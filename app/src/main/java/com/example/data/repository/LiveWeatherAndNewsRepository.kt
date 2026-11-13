package com.example.data.repository

import android.util.Log
import android.util.Xml
import com.example.data.model.CityFloodForecast
import com.example.data.model.FloodRiskLevel
import com.example.data.model.GeoCityResult
import com.example.data.model.HourlyRainPoint
import com.example.data.model.LiveDisasterEvent
import com.example.data.model.LocalFloodNewsItem
import com.example.data.model.NwsAlert
import com.example.data.model.SavedMonitoredProperty
import com.example.data.model.StreetFloodRiskSummary
import com.example.data.model.WaterReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class LiveWeatherAndNewsRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun searchCities(query: String): Result<List<GeoCityResult>> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext Result.success(emptyList())
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=6&language=en&format=json"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("City search failed (${response.code})"))
                }
                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)
                val results = json.optJSONArray("results") ?: return@withContext Result.success(emptyList())
                val list = buildList {
                    for (i in 0 until results.length()) {
                        val item = results.getJSONObject(i)
                        add(
                            GeoCityResult(
                                name = item.optString("name", ""),
                                admin1 = item.optString("admin1", ""),
                                country = item.optString("country", ""),
                                latitude = item.optDouble("latitude", 0.0),
                                longitude = item.optDouble("longitude", 0.0)
                            )
                        )
                    }
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Log.e(TAG, "City search error", e)
            Result.failure(e)
        }
    }

    suspend fun fetchCityForecast(
        city: String,
        latitude: Double,
        longitude: Double
    ): Result<CityFloodForecast> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$latitude&longitude=$longitude" +
                "&hourly=precipitation_probability,precipitation,soil_moisture_0_to_1cm,soil_moisture_3_to_9cm" +
                "&forecast_days=2&timezone=auto"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("Weather forecast unavailable (${response.code})"))
                }
                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)
                val hourly = json.getJSONObject("hourly")
                val times = hourly.getJSONArray("time")
                val probs = hourly.getJSONArray("precipitation_probability")
                val precips = hourly.getJSONArray("precipitation")
                val surfMoist = hourly.optJSONArray("soil_moisture_0_to_1cm")
                val deepMoist = hourly.optJSONArray("soil_moisture_3_to_9cm")

                val points = mutableListOf<HourlyRainPoint>()
                val limit = minOf(times.length(), 24)
                for (i in 0 until limit) {
                    points.add(
                        HourlyRainPoint(
                            timeIso = times.optString(i, ""),
                            precipProbability = probs.optInt(i, 0),
                            precipMm = precips.optDouble(i, 0.0),
                            surfaceSoilMoisture = surfMoist?.optDouble(i, 0.25) ?: 0.25,
                            deepSoilMoisture = deepMoist?.optDouble(i, 0.25) ?: 0.25
                        )
                    )
                }

                val maxProb = points.maxOfOrNull { it.precipProbability } ?: 0
                val totalMm = points.sumOf { it.precipMm }
                val peakPoint = points.maxByOrNull { it.precipMm * 10 + it.precipProbability }
                val avgSoil = points.take(6).map { (it.surfaceSoilMoisture + it.deepSoilMoisture) / 2.0 }.average()
                // Typical volumetric soil moisture spans ~0.05 (dry) to ~0.48 (saturated)
                val saturationPct = ((avgSoil / 0.45) * 100.0).toInt().coerceIn(5, 100)

                Result.success(
                    CityFloodForecast(
                        city = city,
                        latitude = latitude,
                        longitude = longitude,
                        maxRainProbNext24h = maxProb,
                        totalRainMmNext24h = totalMm,
                        peakHourIso = peakPoint?.timeIso ?: "",
                        soilSaturationPercent = saturationPct,
                        hourlyPoints = points
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Forecast fetch failed", e)
            Result.failure(e)
        }
    }

    suspend fun fetchNwsAlerts(latitude: Double, longitude: Double): Result<List<NwsAlert>> =
        withContext(Dispatchers.IO) {
            try {
                val latRounded = String.format(java.util.Locale.US, "%.4f", latitude)
                val lonRounded = String.format(java.util.Locale.US, "%.4f", longitude)
                val url = "https://api.weather.gov/alerts/active?point=$latRounded,$lonRounded"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "WaterTableApp/1.0 (hydro-community-alert)")
                    .header("Accept", "application/geo+json")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        // Outside US or temporary NWS issue -> return empty list gracefully
                        return@withContext Result.success(emptyList())
                    }
                    val body = response.body?.string().orEmpty()
                    val json = JSONObject(body)
                    val features = json.optJSONArray("features") ?: return@withContext Result.success(emptyList())
                    val alerts = buildList {
                        for (i in 0 until minOf(features.length(), 8)) {
                            val feat = features.getJSONObject(i)
                            val props = feat.optJSONObject("properties") ?: continue
                            add(
                                NwsAlert(
                                    id = feat.optString("id", "alert_$i"),
                                    event = props.optString("event", "Weather Advisory"),
                                    severity = props.optString("severity", "Moderate"),
                                    headline = props.optString("headline", ""),
                                    areaDesc = props.optString("areaDesc", ""),
                                    instruction = props.optString("instruction", "")
                                )
                            )
                        }
                    }
                    Result.success(alerts)
                }
            } catch (e: Exception) {
                Log.w(TAG, "NWS alerts lookup skipped/failed: ${e.message}")
                Result.success(emptyList())
            }
        }

    suspend fun fetchLocalFloodNews(city: String): Result<List<LocalFloodNewsItem>> =
        withContext(Dispatchers.IO) {
            try {
                val cleanCity = city.trim().ifBlank { "flooding" }
                val query = URLEncoder.encode("$cleanCity flooding OR wildfire OR water table OR storm", "UTF-8")
                val url = "https://news.google.com/rss/search?q=$query&hl=en-US&gl=US&ceid=US:en"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "WaterTableApp/1.0")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(IllegalStateException("News feed unavailable (${response.code})"))
                    }
                    val xml = response.body?.string().orEmpty()
                    Result.success(parseRssFeed(xml))
                }
            } catch (e: Exception) {
                Log.e(TAG, "News RSS fetch failed", e)
                Result.failure(e)
            }
        }

    private fun parseRssFeed(xml: String): List<LocalFloodNewsItem> {
        if (xml.isBlank()) return emptyList()
        val items = mutableListOf<LocalFloodNewsItem>()
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xml))

        var insideItem = false
        var currentTag = ""
        var title = ""
        var link = ""
        var pubDate = ""
        var source = "Local News Channel"

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT && items.size < 8) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name.orEmpty()
                    if (currentTag == "item") {
                        insideItem = true
                        title = ""
                        link = ""
                        pubDate = ""
                        source = "Local News Channel"
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideItem) {
                        val text = parser.text.orEmpty().trim()
                        when (currentTag) {
                            "title" -> if (text.isNotEmpty()) title += text
                            "link" -> if (text.isNotEmpty()) link += text
                            "pubDate" -> if (text.isNotEmpty()) pubDate += text
                            "source" -> if (text.isNotEmpty()) source = text
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "item" && insideItem) {
                        if (title.isNotBlank() && link.isNotBlank()) {
                            items.add(
                                LocalFloodNewsItem(
                                    title = title,
                                    source = source,
                                    pubDate = pubDate.take(22),
                                    link = link
                                )
                            )
                        }
                        insideItem = false
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }
        return items
    }

    fun computePreRainStreetFloodRisk(
        city: String,
        forecast: CityFloodForecast?,
        alerts: List<NwsAlert>,
        reports: List<WaterReport>,
        latitude: Double,
        longitude: Double
    ): StreetFloodRiskSummary {
        val nearbyOrCityReports = reports.filter { rep ->
            rep.city.equals(city, ignoreCase = true) ||
                (abs(rep.latitude - latitude) < 0.35 && abs(rep.longitude - longitude) < 0.35)
        }
        val shallowReports = nearbyOrCityReports.count {
            it.depthFeet <= 6.0 || it.streetFloodRisk == "high" || it.streetFloodRisk == "critical"
        }
        val floodAlertsCount = alerts.count {
            it.event.contains("flood", ignoreCase = true) ||
                it.event.contains("storm", ignoreCase = true) ||
                it.event.contains("rain", ignoreCase = true)
        }

        val rainMm = forecast?.totalRainMmNext24h ?: 0.0
        val rainProb = forecast?.maxRainProbNext24h ?: 0
        val soilSat = forecast?.soilSaturationPercent ?: 35

        var score = 0
        score += (rainMm * 1.8).toInt().coerceAtMost(35)
        score += (rainProb * 0.2).toInt().coerceAtMost(20)
        score += ((soilSat - 40).coerceAtLeast(0) * 0.45).toInt().coerceAtMost(22)
        score += (shallowReports * 7).coerceAtMost(20)
        score += (floodAlertsCount * 18).coerceAtMost(25)
        score = score.coerceIn(5, 100)

        val level = when {
            score >= 75 -> FloodRiskLevel.CRITICAL
            score >= 52 -> FloodRiskLevel.HIGH
            score >= 28 -> FloodRiskLevel.MODERATE
            else -> FloodRiskLevel.LOW
        }

        val carAdvisory = when (level) {
            FloodRiskLevel.CRITICAL -> "MOVE VEHICLES NOW: Shallow water table + heavy forecast rain (${String.format(java.util.Locale.US, "%.1f", rainMm)} mm) & active warnings put curb lanes at imminent flood risk."
            FloodRiskLevel.HIGH -> "PRE-RAIN CAR WARNING: Move parked cars out of low-lying streets & underpasses before rain arrives (${rainProb}% chance, soil $soilSat% saturated)."
            FloodRiskLevel.MODERATE -> "WATCH ADVISORY: Moderate soil saturation ($soilSat%) with $shallowReports shallow-water reports nearby. Avoid storm-drain depressions."
            FloodRiskLevel.LOW -> "STREETS CLEAR: Groundwater absorption capacity looks healthy ahead of next 24h weather window."
        }

        val headline = when {
            floodAlertsCount > 0 -> "${alerts.first().event} active near $city"
            rainProb >= 60 -> "Storm window (${rainProb}% chance) approaching $city · Peak ${forecast?.peakHourIso?.takeLast(5).orEmpty()}"
            shallowReports > 0 -> "$shallowReports shallow water table zones logged in $city"
            else -> "Live hydrology & street watch active for $city"
        }

        return StreetFloodRiskSummary(
            city = city,
            riskLevel = level,
            riskScore = score,
            carAdvisory = carAdvisory,
            preRainHeadline = headline,
            shallowWaterTableReportsCount = shallowReports,
            activeAlertCount = alerts.size,
            upcomingRainMm = rainMm,
            soilSaturationPct = soilSat
        )
    }

    suspend fun fetchLiveDisasters(
        centerLat: Double,
        centerLon: Double,
        city: String,
        alerts: List<NwsAlert>,
        monitoredProperties: List<SavedMonitoredProperty>
    ): List<LiveDisasterEvent> = withContext(Dispatchers.IO) {
        val rawEvents = mutableListOf<LiveDisasterEvent>()

        // 1. Live NWS Flood / Fire / Storm Warnings converted to localized disaster pins
        alerts.forEachIndexed { idx, alert ->
            val evLower = alert.event.lowercase()
            val isFire = evLower.contains("fire") || evLower.contains("red flag") || evLower.contains("smoke")
            val isFlood = evLower.contains("flood") || evLower.contains("rain") || evLower.contains("storm") || evLower.contains("tsunami")
            if (isFire || isFlood) {
                rawEvents.add(
                    LiveDisasterEvent(
                        id = "nws_${idx}_${alert.id.takeLast(8)}",
                        type = if (isFire) "wildfire" else "flood",
                        title = "${alert.event} (${alert.areaDesc.take(40)})",
                        severity = if (alert.severity.equals("Extreme", true) || alert.severity.equals("Severe", true)) "Critical" else "High",
                        latitude = centerLat,
                        longitude = centerLon,
                        source = "NWS Live Alert",
                        details = alert.headline.ifBlank { alert.instruction }.take(240),
                        updatedAtLabel = "Live Now",
                        link = "https://www.weather.gov/"
                    )
                )
            }
        }

        // 2. Live CAL FIRE Active Incidents GeoJSON (accurate SoCal & CA active wildfires)
        runCatching {
            val calReq = Request.Builder()
                .url("https://incidents.fire.ca.gov/umbraco/api/IncidentApi/GeoJsonList?inactive=false")
                .header("User-Agent", "WaterTableApp/1.0")
                .build()
            client.newCall(calReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val json = JSONObject(resp.body?.string().orEmpty())
                    val feats = json.optJSONArray("features")
                    if (feats != null) {
                        for (i in 0 until minOf(feats.length(), 15)) {
                            val feat = feats.getJSONObject(i)
                            val geom = feat.optJSONObject("geometry") ?: continue
                            val coords = geom.optJSONArray("coordinates") ?: continue
                            if (coords.length() < 2) continue
                            val lon = coords.optDouble(0, 0.0)
                            val lat = coords.optDouble(1, 0.0)
                            if (lat == 0.0 && lon == 0.0) continue
                            val props = feat.optJSONObject("properties") ?: continue
                            val name = props.optString("Name", "Active Wildfire")
                            val county = props.optString("County", "CA")
                            val acres = props.optInt("AcresBurned", 0)
                            val pct = props.optInt("PercentContained", -1)
                            val url = props.optString("Url", "https://www.fire.ca.gov/incidents")
                            rawEvents.add(
                                LiveDisasterEvent(
                                    id = "calfire_${i}_${name.hashCode()}",
                                    type = "wildfire",
                                    title = "$name ($county)",
                                    severity = if (acres >= 500 || pct in 0..30) "Critical" else "High",
                                    latitude = lat,
                                    longitude = lon,
                                    source = "CAL FIRE Live",
                                    details = "${if (acres > 0) "$acres acres burned" else "Active wildfire"}${if (pct >= 0) " · $pct% contained" else ""}",
                                    updatedAtLabel = props.optString("Updated", "Live").take(16),
                                    link = if (url.startsWith("http")) url else "https://www.fire.ca.gov$url",
                                    acresBurned = acres,
                                    containmentPct = pct
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3. NASA EONET v3 Real-Time Wildfires & Floods
        runCatching {
            val eonetReq = Request.Builder()
                .url("https://eonet.gsfc.nasa.gov/api/v3/events?category=wildfires,floods&status=open&limit=20")
                .header("User-Agent", "WaterTableApp/1.0")
                .build()
            client.newCall(eonetReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val json = JSONObject(resp.body?.string().orEmpty())
                    val events = json.optJSONArray("events")
                    if (events != null) {
                        for (i in 0 until events.length()) {
                            val ev = events.getJSONObject(i)
                            val evId = ev.optString("id", "eonet_$i")
                            val title = ev.optString("title", "Active Disaster Event")
                            val cats = ev.optJSONArray("categories")
                            val catId = if (cats != null && cats.length() > 0) cats.getJSONObject(0).optString("id", "wildfires") else "wildfires"
                            val geoms = ev.optJSONArray("geometry") ?: continue
                            if (geoms.length() == 0) continue
                            val lastGeom = geoms.getJSONObject(geoms.length() - 1)
                            val coords = lastGeom.optJSONArray("coordinates") ?: continue
                            if (coords.length() < 2) continue
                            val lon = coords.optDouble(0, 0.0)
                            val lat = coords.optDouble(1, 0.0)
                            val dateStr = lastGeom.optString("date", "").take(16)
                            val isFlood = catId == "floods"
                            rawEvents.add(
                                LiveDisasterEvent(
                                    id = "eonet_$evId",
                                    type = if (isFlood) "flood" else "wildfire",
                                    title = title,
                                    severity = if (isFlood) "High" else "Critical",
                                    latitude = lat,
                                    longitude = lon,
                                    source = "NASA EONET Satellite",
                                    details = "Active live ${if (isFlood) "flooding" else "wildfire"} thermal/satellite track",
                                    updatedAtLabel = dateStr.ifBlank { "Live" },
                                    link = ev.optString("link", "https://eonet.gsfc.nasa.gov/")
                                )
                            )
                        }
                    }
                }
            }
        }

        // 4. USGS Live River/Creek Flood Gauge Check within ~0.35 deg box
        runCatching {
            val minLon = String.format(Locale.US, "%.3f", centerLon - 0.35)
            val minLat = String.format(Locale.US, "%.3f", centerLat - 0.35)
            val maxLon = String.format(Locale.US, "%.3f", centerLon + 0.35)
            val maxLat = String.format(Locale.US, "%.3f", centerLat + 0.35)
            val usgsUrl = "https://waterservices.usgs.gov/nwis/iv/?format=json&bBox=$minLon,$minLat,$maxLon,$maxLat&parameterCd=00065&siteStatus=active"
            val req = Request.Builder().url(usgsUrl).header("User-Agent", "WaterTableApp/1.0").build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val json = JSONObject(resp.body?.string().orEmpty())
                    val tsArr = json.optJSONObject("value")?.optJSONArray("timeSeries")
                    if (tsArr != null) {
                        for (i in 0 until minOf(tsArr.length(), 4)) {
                            val ts = tsArr.getJSONObject(i)
                            val srcInfo = ts.optJSONObject("sourceInfo") ?: continue
                            val siteName = srcInfo.optString("siteName", "USGS Water Gauge")
                            val geoLoc = srcInfo.optJSONObject("geoLocation")?.optJSONObject("geogLocation") ?: continue
                            val gLat = geoLoc.optDouble("latitude", centerLat)
                            val gLon = geoLoc.optDouble("longitude", centerLon)
                            val values = ts.optJSONArray("values")?.optJSONObject(0)?.optJSONArray("value")
                            val gageVal = if (values != null && values.length() > 0) values.getJSONObject(0).optString("value", "N/A") else "N/A"
                            val gaugeFt = gageVal.toDoubleOrNull() ?: 0.0
                            rawEvents.add(
                                LiveDisasterEvent(
                                    id = "usgs_${i}_${siteName.hashCode()}",
                                    type = "flood",
                                    title = "USGS Gauge: ${siteName.take(38)}",
                                    severity = if (gaugeFt >= 15.0) "High" else "Moderate",
                                    latitude = gLat,
                                    longitude = gLon,
                                    source = "USGS Live Water",
                                    details = "Current live water stage: $gageVal ft",
                                    updatedAtLabel = "Live Gauge",
                                    link = "https://waterdata.usgs.gov/nwis/rt"
                                )
                            )
                        }
                    }
                }
            }
        }

        // Enrich every event with exact GPS distance ratio to user's saved houses/businesses (or active city GPS) + Cleanse Protocol
        val anchors = monitoredProperties.map { Triple(it.label.ifBlank { it.address }, it.latitude, it.longitude) }
            .ifEmpty { listOf(Triple("$city Location", centerLat, centerLon)) }

        rawEvents.map { ev ->
            val nearest = anchors.minByOrNull { (_, lat, lon) -> haversineMiles(lat, lon, ev.latitude, ev.longitude) }
                ?: Triple(city, centerLat, centerLon)
            val dist = haversineMiles(nearest.second, nearest.third, ev.latitude, ev.longitude)
            val roundedDist = (dist * 10.0).toInt() / 10.0
            val ratioLabel = when {
                roundedDist <= 5.0 -> "IMMEDIATE ZONE (${roundedDist} mi from ${nearest.first} · 1:1 Direct Threat)"
                roundedDist <= 15.0 -> "HIGH PROXIMITY (${roundedDist} mi from ${nearest.first} · <15mi Safety Buffer)"
                roundedDist <= 45.0 -> "REGIONAL ALERT (${roundedDist} mi from ${nearest.first} · Ash/Watershed Radius)"
                else -> "${roundedDist.toInt()} mi from ${nearest.first} (Extended Tracking)"
            }
            val (cleanse, specs) = when (ev.type) {
                "wildfire" -> "Wildfire Smoke, Ash & Soot Cleanse: Run HEPA AFD air scrubbers, seal HVAC returns, thermal/hydroxyl deodorization, damp HEPA micro-wipe & attic insulation ash inspection (ANSI/IICRC S700)." to
                    listOf("smoke_fire_restoration", "full_service_restoration", "iicrc_inspector")
                else -> "Flood & Cat-3 Cleanse Protocol: Pump standing water, 24-in drywall flood-cut, EPA-registered botanical antimicrobial cleanse, LGR dehumidifiers & sub-slab drainage check (ANSI/IICRC S500)." to
                    listOf("water_mitigation", "mold_remediation", "drainage_contractor", "soil_engineer", "full_service_restoration")
            }
            ev.copy(
                distanceToHomeMiles = roundedDist,
                distanceRatioLabel = ratioLabel,
                nearestPropertyLabel = nearest.first,
                recommendedCleanseProtocol = cleanse,
                matchedContractorSpecialties = specs
            )
        }.sortedBy { it.distanceToHomeMiles }.take(18)
    }

    /**
     * Resolves any house or business street address to GPS coordinates and queries official FEMA NFHL Flood Zone
     * plus Fire/Water Hazard baselines.
     */
    suspend fun lookupAddressAndFemaFloodZone(
        addressQuery: String,
        fallbackLat: Double,
        fallbackLon: Double,
        fallbackCity: String,
        fallbackState: String
    ): Triple<GeoCityResult, String, String> = withContext(Dispatchers.IO) {
        var lat = fallbackLat
        var lon = fallbackLon
        var resolvedCity = fallbackCity
        var resolvedState = fallbackState

        // 1. Geocode street address or city via Nominatim or Open-Meteo fallback
        if (addressQuery.trim().length >= 4) {
            var found = false
            runCatching {
                val enc = URLEncoder.encode(addressQuery.trim(), "UTF-8")
                val nomUrl = "https://nominatim.openstreetmap.org/search?q=$enc&format=json&limit=1&addressdetails=1"
                val req = Request.Builder().url(nomUrl).header("User-Agent", "WaterTableApp/1.0").build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val arr = org.json.JSONArray(resp.body?.string().orEmpty())
                        if (arr.length() > 0) {
                            val obj = arr.getJSONObject(0)
                            lat = obj.optString("lat").toDoubleOrNull() ?: lat
                            lon = obj.optString("lon").toDoubleOrNull() ?: lon
                            val addr = obj.optJSONObject("address")
                            resolvedCity = addr?.optString("city")
                                ?.ifBlank { addr.optString("town") }
                                ?.ifBlank { addr.optString("county") }
                                ?.ifBlank { fallbackCity } ?: fallbackCity
                            resolvedState = addr?.optString("state")?.ifBlank { fallbackState } ?: fallbackState
                            found = true
                        }
                    }
                }
            }
            if (!found) {
                searchCities(addressQuery).getOrNull()?.firstOrNull()?.let { geo ->
                    lat = geo.latitude
                    lon = geo.longitude
                    resolvedCity = geo.name.ifBlank { fallbackCity }
                    resolvedState = geo.admin1.ifBlank { fallbackState }
                }
            }
        }

        // 2. Official FEMA National Flood Hazard Layer (NFHL) point-in-polygon REST lookup
        var femaZone = "Zone X (Low-to-Moderate Standard Area)"
        var floodRiskDesc = "Outside FEMA 100-year Special Flood Hazard Area; verify local shallow water table & drainage."
        runCatching {
            val url = "https://hazards.fema.gov/arcgis/rest/services/public/NFHL/MapServer/28/query" +
                "?geometry=$lon,$lat&geometryType=esriGeometryPoint&inSR=4326" +
                "&spatialRel=esriSpatialRelIntersects&outFields=FLD_ZONE,ZONE_SUBTY,SFHA_TF&returnGeometry=false&f=json"
            val req = Request.Builder().url(url).header("User-Agent", "WaterTableApp/1.0").build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val json = JSONObject(resp.body?.string().orEmpty())
                    val feats = json.optJSONArray("features")
                    if (feats != null && feats.length() > 0) {
                        val attrs = feats.getJSONObject(0).optJSONObject("attributes")
                        val fldZone = attrs?.optString("FLD_ZONE", "X").orEmpty().ifBlank { "X" }
                        val subty = attrs?.optString("ZONE_SUBTY", "").orEmpty()
                        val sfha = attrs?.optString("SFHA_TF", "F").orEmpty()
                        femaZone = if (subty.isNotBlank()) "FEMA Zone $fldZone ($subty)" else "FEMA Zone $fldZone"
                        floodRiskDesc = if (sfha.equals("T", true) || fldZone.startsWith("A") || fldZone.startsWith("V")) {
                            "HIGH FLOOD HAZARD (SFHA): Mandatory flood insurance typically required; 1% annual (100-yr) flood risk."
                        } else {
                            "FEMA $femaZone: Moderate-to-minimal mapped riverine flood zone; check shallow groundwater & storm drainage."
                        }
                    }
                }
            }
        }

        Triple(
            GeoCityResult(name = resolvedCity, admin1 = resolvedState, country = "US", latitude = lat, longitude = lon),
            femaZone,
            floodRiskDesc
        )
    }

    /**
     * Fetches USGS/SRTM terrain elevation (feet) from Open-Elevation and 5-year historical
     * extreme precipitation/wind storm event counts from Open-Meteo archive API.
     */
    suspend fun fetchElevationAndMotherNatureHistory(
        latitude: Double,
        longitude: Double
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        var elevationFt = 0
        var extremeStormDays = 0

        runCatching {
            val elevUrl = "https://api.open-elevation.com/api/v1/lookup?locations=${String.format(Locale.US, "%.5f", latitude)},${String.format(Locale.US, "%.5f", longitude)}"
            val req = Request.Builder().url(elevUrl).header("User-Agent", "WaterTableApp/1.0").build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val json = JSONObject(resp.body?.string().orEmpty())
                    val results = json.optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        val meters = results.getJSONObject(0).optDouble("elevation", 0.0)
                        elevationFt = (meters * 3.28084).toInt()
                    }
                }
            }
        }

        runCatching {
            val end = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date(System.currentTimeMillis() - 86_400_000L * 5))
            val cal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.YEAR, -5) }
            val start = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
            val archUrl = "https://archive-api.open-meteo.com/v1/archive" +
                "?latitude=${String.format(Locale.US, "%.4f", latitude)}" +
                "&longitude=${String.format(Locale.US, "%.4f", longitude)}" +
                "&start_date=$start&end_date=$end" +
                "&daily=precipitation_sum,wind_speed_10m_max&timezone=auto"
            val req = Request.Builder().url(archUrl).header("User-Agent", "WaterTableApp/1.0").build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val daily = JSONObject(resp.body?.string().orEmpty()).optJSONObject("daily")
                    val precip = daily?.optJSONArray("precipitation_sum")
                    val wind = daily?.optJSONArray("wind_speed_10m_max")
                    if (precip != null) {
                        for (i in 0 until precip.length()) {
                            val pMm = precip.optDouble(i, 0.0)
                            val wKmH = wind?.optDouble(i, 0.0) ?: 0.0
                            if (pMm >= 25.0 || wKmH >= 65.0) {
                                extremeStormDays++
                            }
                        }
                    }
                }
            }
        }

        elevationFt to extremeStormDays
    }

    fun haversineMiles(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 3958.8
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private companion object {
        const val TAG = "LiveWeatherNewsRepo"
    }
}
