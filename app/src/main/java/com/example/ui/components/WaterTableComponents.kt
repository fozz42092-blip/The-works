package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.FloodRiskLevel
import com.example.data.model.HourlyRainPoint
import com.example.data.model.WaterReport
import com.example.ui.theme.AquiferTeal
import com.example.ui.theme.FloodAmber
import com.example.ui.theme.FloodCrimson
import com.example.ui.theme.HydroCyanBright
import com.example.ui.theme.HydroNavy
import com.example.ui.theme.HydroNavyDark
import com.example.ui.theme.SafeEmerald
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.hypot

@Composable
fun VerdictBadge(verdict: String, engineerVerified: Boolean = false) {
    val (label, bg, fg) = when (verdict) {
        "ground_water_table" -> Triple("Ground Water Table", Color(0xFFCCFBF1), Color(0xFF0F766E))
        "surface_runoff" -> Triple("Surface Runoff", Color(0xFFE0F2FE), Color(0xFF0369A1))
        "plumbing_leak" -> Triple("Plumbing / Pipe Leak", Color(0xFFFEF3C7), Color(0xFFB45309))
        "storm_drain_backup" -> Triple("Drain Backup", Color(0xFFFEE2E2), Color(0xFFB91C1C))
        "water_loss_mitigation" -> Triple("Water Mitigation", Color(0xFFDBEAFE), Color(0xFF1D4ED8))
        "mold_mitigation" -> Triple("Mold Remediation", Color(0xFFECFCCB), Color(0xFF3F6212))
        "smoke_fire_mitigation" -> Triple("Smoke & Fire Loss", Color(0xFFFFEDD5), Color(0xFFC2410C))
        else -> Triple("Needs Triage", Color(0xFFE2E8F0), Color(0xFF334155))
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(color = bg, shape = RoundedCornerShape(50)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        if (engineerVerified) {
            Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(50)) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = "Licensed pro verified",
                        tint = SafeEmerald,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Pro Verified",
                        style = MaterialTheme.typography.labelSmall,
                        color = SafeEmerald,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun LossCategoryBadge(category: String) {
    val (label, bg, fg) = when (category) {
        "water_restoration" -> Triple("Water Loss", Color(0xFFDBEAFE), Color(0xFF1E40AF))
        "mold_mitigation" -> Triple("Mold Mitigation", Color(0xFFECFCCB), Color(0xFF3F6212))
        "smoke_damage" -> Triple("Smoke Damage", Color(0xFFF3F4F6), Color(0xFF374151))
        "fire_restoration" -> Triple("Fire Restoration", Color(0xFFFFEDD5), Color(0xFF9A3412))
        else -> Triple("Groundwater", Color(0xFFCCFBF1), Color(0xFF0F766E))
    }
    Surface(color = bg, shape = RoundedCornerShape(50)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun ConstructionStageBadge(stage: String) {
    if (stage == "none" || stage.isBlank()) return
    val (label, bg, fg) = when (stage) {
        "in_process" -> Triple("Construction In Process", Color(0xFFFEF3C7), Color(0xFF92400E))
        "after_remodel" -> Triple("After Full Remodel", Color(0xFFE0E7FF), Color(0xFF3730A3))
        else -> return
    }
    Surface(color = bg, shape = RoundedCornerShape(50)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun ModerationStatusBadge(status: String) {
    val (label, bg, fg) = when (status) {
        "approved" -> Triple("Public Atlas", Color(0xFFD1FAE5), SafeEmerald)
        "ai_verified" -> Triple("AI Screened", Color(0xFFE0F2FE), Color(0xFF0369A1))
        "flagged" -> Triple("Flagged", Color(0xFFFEE2E2), FloodCrimson)
        else -> Triple("Pending Moderation", Color(0xFFFEF3C7), Color(0xFFB45309))
    }
    Surface(color = bg, shape = RoundedCornerShape(50)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun BlacklistBadge(reason: String) {
    Surface(
        color = Color(0xFFFEE2E2),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Block,
                contentDescription = "Blacklisted contractor warning",
                tint = FloodCrimson,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "BLACKLISTED · $reason",
                style = MaterialTheme.typography.labelSmall,
                color = FloodCrimson,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun RatingStarsRow(label: String, rating: Double) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = FloodAmber,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "$label ${String.format(java.util.Locale.US, "%.1f", rating)}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun RiskLevelBadge(level: FloodRiskLevel) {
    val (bg, fg) = when (level) {
        FloodRiskLevel.CRITICAL -> Color(0xFFFEE2E2) to FloodCrimson
        FloodRiskLevel.HIGH -> Color(0xFFFEF3C7) to Color(0xFFB45309)
        FloodRiskLevel.MODERATE -> Color(0xFFE0F2FE) to Color(0xFF0369A1)
        FloodRiskLevel.LOW -> Color(0xFFD1FAE5) to SafeEmerald
    }
    Surface(color = bg, shape = RoundedCornerShape(50)) {
        Text(
            text = level.label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun Base64MediaPreview(
    base64: String,
    mediaType: String,
    videoDurationLabel: String,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(base64) {
        if (base64.isBlank()) null
        else try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
    if (bitmap != null) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(14.dp))
                .background(HydroNavyDark)
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = if (mediaType == "video") "Site video frame preview" else "Site photo preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (mediaType == "video") {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xAA06253A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayCircle,
                        contentDescription = "Video attachment indicator",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                if (videoDurationLabel.isNotBlank()) {
                    Surface(
                        color = Color(0xCC06253A),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = videoDurationLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive Leaflet.js Map backed by OpenStreetMap / OpenTopoMap / Esri Satellite tiles,
 * displaying user-submitted GPS points color-coded by dig depth (shallow water table alerts)
 * and construction status (in-process vs post-remodel), with native tap callbacks + fallback grid view.
 */
@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun InteractiveLeafletHydroMap(
    centerLat: Double,
    centerLon: Double,
    cityName: String,
    reports: List<WaterReport>,
    selectedReportId: String?,
    riskLevel: FloodRiskLevel,
    onSelectReport: (WaterReport) -> Unit,
    modifier: Modifier = Modifier
) {
    var useLeafletWebMap by remember { mutableStateOf(true) }
    var tileStyle by remember { mutableStateOf("streets") } // "streets" | "topo" | "satellite"

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text(
                    text = if (useLeafletWebMap) "Leaflet Live GPS Map ($cityName)" else "Hydro Grid Radar ($cityName)",
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (useLeafletWebMap) {
                    FilterChip(
                        selected = tileStyle == "streets",
                        onClick = { tileStyle = "streets" },
                        label = { Text("Street") }
                    )
                    FilterChip(
                        selected = tileStyle == "topo",
                        onClick = { tileStyle = "topo" },
                        label = { Text("Topo") }
                    )
                }
                FilterChip(
                    selected = !useLeafletWebMap,
                    onClick = { useLeafletWebMap = !useLeafletWebMap },
                    label = { Text(if (useLeafletWebMap) "Radar" else "Leaflet") }
                )
            }
        }

        if (!useLeafletWebMap) {
            InteractiveHydroMapCanvas(
                centerLat = centerLat,
                centerLon = centerLon,
                cityName = cityName,
                reports = reports,
                selectedReportId = selectedReportId,
                riskLevel = riskLevel,
                onSelectReport = onSelectReport
            )
        } else {
            val context = LocalContext.current
            val mainHandler = remember { Handler(Looper.getMainLooper()) }
            val reportsJson = remember(reports) {
                val arr = JSONArray()
                reports.forEach { r ->
                    arr.put(
                        JSONObject().apply {
                            put("id", r.id)
                            put("lat", r.latitude)
                            put("lon", r.longitude)
                            put("city", r.city)
                            put("addr", r.addressHint.ifBlank { "GPS Pin" })
                            put("depth", r.depthFeet)
                            put("stage", r.constructionStage)
                            put("verdict", r.waterSourceVerdict)
                            put("risk", r.streetFloodRisk)
                        }
                    )
                }
                arr.toString()
            }

            var webViewRef by remember { mutableStateOf<WebView?>(null) }
            var mapLoaded by remember { mutableStateOf(false) }

            val bridge = remember(reports, onSelectReport) {
                object {
                    @JavascriptInterface
                    fun selectPin(reportId: String) {
                        val match = reports.firstOrNull { it.id == reportId } ?: return
                        mainHandler.post { onSelectReport(match) }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(275.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(HydroNavyDark)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            addJavascriptInterface(bridge, "WaterTableBridge")
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    mapLoaded = true
                                    view?.evaluateJavascript(
                                        "window.updateHydroMap($centerLat, $centerLon, $reportsJson, ${JSONObject.quote(selectedReportId ?: "")}, ${JSONObject.quote(tileStyle)});",
                                        null
                                    )
                                }
                            }
                            loadDataWithBaseURL(
                                "https://watertable.local/",
                                LEAFLET_HTML,
                                "text/html",
                                "UTF-8",
                                null
                            )
                            webViewRef = this
                        }
                    },
                    update = { view ->
                        if (mapLoaded) {
                            view.evaluateJavascript(
                                "window.updateHydroMap($centerLat, $centerLon, $reportsJson, ${JSONObject.quote(selectedReportId ?: "")}, ${JSONObject.quote(tileStyle)});",
                                null
                            )
                        }
                    }
                )

                // Depth Legend Overlay
                Surface(
                    color = Color(0xCC06253A),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendDot(color = FloodCrimson, label = "Shallow ≤5ft")
                        LegendDot(color = FloodAmber, label = "5–12ft")
                        LegendDot(color = AquiferTeal, label = ">12ft")
                    }
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    webViewRef?.destroy()
                }
            }
        }
    }
}

@Composable
fun InteractiveHydroMapCanvas(
    centerLat: Double,
    centerLon: Double,
    cityName: String,
    reports: List<WaterReport>,
    selectedReportId: String?,
    riskLevel: FloodRiskLevel,
    onSelectReport: (WaterReport) -> Unit,
    modifier: Modifier = Modifier
) {
    val spanDegrees = 0.22
    val pinPositions = remember(centerLat, centerLon, reports) {
        reports.map { report ->
            val dx = ((report.longitude - centerLon) / spanDegrees).toFloat().coerceIn(-0.44f, 0.44f)
            val dy = (-(report.latitude - centerLat) / spanDegrees).toFloat().coerceIn(-0.42f, 0.42f)
            Triple(report, 0.5f + dx, 0.5f + dy)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(255.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF072338), Color(0xFF0B3C5D), Color(0xFF0D4E6E))
                )
            )
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(pinPositions) {
                    detectTapGestures { tap ->
                        val hit = pinPositions.minByOrNull { (_, nx, ny) ->
                            val px = nx * size.width
                            val py = ny * size.height
                            hypot(tap.x - px, tap.y - py)
                        }
                        if (hit != null) {
                            val px = hit.second * size.width
                            val py = hit.third * size.height
                            if (hypot(tap.x - px, tap.y - py) <= 64f) {
                                onSelectReport(hit.first)
                            }
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val gridColor = Color.White.copy(alpha = 0.09f)

            for (i in 1..5) {
                val x = w * (i / 6f)
                drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1.5f)
            }
            for (j in 1..4) {
                val y = h * (j / 5f)
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.5f)
            }

            val ringColor = when (riskLevel) {
                FloodRiskLevel.CRITICAL -> FloodCrimson
                FloodRiskLevel.HIGH -> FloodAmber
                FloodRiskLevel.MODERATE -> HydroCyanBright
                FloodRiskLevel.LOW -> SafeEmerald
            }
            drawCircle(
                color = ringColor.copy(alpha = 0.14f),
                radius = minOf(w, h) * 0.38f,
                center = Offset(w * 0.5f, h * 0.5f)
            )
            drawCircle(
                color = ringColor.copy(alpha = 0.35f),
                radius = minOf(w, h) * 0.24f,
                center = Offset(w * 0.5f, h * 0.5f),
                style = Stroke(width = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)))
            )

            pinPositions.forEach { (report, nx, ny) ->
                val cx = nx * w
                val cy = ny * h
                val isSelected = report.id == selectedReportId
                val pinColor = when {
                    report.depthFeet in 0.1..5.0 -> FloodCrimson
                    report.depthFeet <= 12.0 -> FloodAmber
                    report.waterSourceVerdict == "ground_water_table" -> HydroCyanBright
                    else -> AquiferTeal
                }
                if (isSelected) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.32f),
                        radius = 24f,
                        center = Offset(cx, cy)
                    )
                }
                drawCircle(color = Color.White, radius = if (isSelected) 13f else 10f, center = Offset(cx, cy))
                drawCircle(color = pinColor, radius = if (isSelected) 9.5f else 7.5f, center = Offset(cx, cy))
            }

            drawCircle(
                color = Color.White,
                radius = 7f,
                center = Offset(w * 0.5f, h * 0.5f),
                style = Stroke(width = 2.5f)
            )
        }

        Surface(
            color = Color(0xB806253A),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text(
                    text = "$cityName Hydro Grid",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = String.format(java.util.Locale.US, "%.4f°, %.4f° · %d GPS pins", centerLat, centerLon, reports.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = HydroCyanBright
                )
            }
        }

        Surface(
            color = Color(0xB806253A),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(color = FloodCrimson, label = "Shallow ≤5ft")
                LegendDot(color = FloodAmber, label = "5–12ft")
                LegendDot(color = AquiferTeal, label = ">12ft")
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}

@Composable
fun RainForecastBarChart(
    points: List<HourlyRainPoint>,
    modifier: Modifier = Modifier
) {
    val slice = points.take(12)
    if (slice.isEmpty()) return
    val maxMm = (slice.maxOfOrNull { it.precipMm } ?: 1.0).coerceAtLeast(2.0)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
        ) {
            val count = slice.size
            val slotW = size.width / count
            val barW = (slotW * 0.58f).coerceAtLeast(6f)
            slice.forEachIndexed { idx, pt ->
                val x = idx * slotW + (slotW - barW) / 2f
                val probH = (pt.precipProbability / 100f) * (size.height * 0.85f)
                val mmH = ((pt.precipMm / maxMm).toFloat() * (size.height * 0.85f))
                val barH = maxOf(probH, mmH, 5f)
                val barColor = when {
                    pt.precipMm >= 4.0 || pt.precipProbability >= 75 -> FloodAmber
                    pt.precipProbability >= 45 -> HydroCyanBright
                    else -> HydroNavy.copy(alpha = 0.35f)
                }
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, size.height - barH),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Now (${slice.first().timeIso.takeLast(5)})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "+6h",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "+12h (${slice.last().timeIso.takeLast(5)})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private const val LEAFLET_HTML = """
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
  <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
  <style>
    html, body, #map { margin: 0; padding: 0; width: 100%; height: 100%; background: #072338; }
    .wt-popup { font-family: sans-serif; font-size: 12px; line-height: 1.35; }
    .wt-shallow { color: #dc2626; font-weight: bold; }
  </style>
</head>
<body>
  <div id="map"></div>
  <script>
    var map = null;
    var markerLayer = null;
    var streetLayer = null;
    var topoLayer = null;
    var currentStyle = 'streets';
    var lastCenter = '';

    function ensureMap(lat, lon) {
      if (map) return;
      map = L.map('map', { zoomControl: true, attributionControl: false }).setView([lat, lon], 12);
      streetLayer = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 });
      topoLayer = L.tileLayer('https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png', { maxZoom: 17 });
      streetLayer.addTo(map);
      markerLayer = L.layerGroup().addTo(map);
    }

    window.updateHydroMap = function(lat, lon, reports, selectedId, tileStyle) {
      if (typeof L === 'undefined') return;
      ensureMap(lat, lon);
      var nextCenter = lat.toFixed(3) + ',' + lon.toFixed(3);
      if (lastCenter !== nextCenter) {
        map.setView([lat, lon], 12);
        lastCenter = nextCenter;
      }
      if (tileStyle !== currentStyle) {
        if (tileStyle === 'topo') {
          if (map.hasLayer(streetLayer)) map.removeLayer(streetLayer);
          if (!map.hasLayer(topoLayer)) topoLayer.addTo(map);
        } else {
          if (map.hasLayer(topoLayer)) map.removeLayer(topoLayer);
          if (!map.hasLayer(streetLayer)) streetLayer.addTo(map);
        }
        currentStyle = tileStyle;
      }

      markerLayer.clearLayers();
      // City GPS center dot
      L.circleMarker([lat, lon], {
        radius: 6, color: '#ffffff', fillColor: '#0284c7', fillOpacity: 0.9, weight: 2
      }).addTo(markerLayer);

      reports.forEach(function(r) {
        var isShallow = r.depth > 0 && r.depth <= 5.0;
        var color = isShallow ? '#dc2626' : (r.depth <= 12.0 ? '#d97706' : '#0d9488');
        var isSel = r.id === selectedId;
        var marker = L.circleMarker([r.lat, r.lon], {
          radius: isSel ? 12 : 9,
          color: '#ffffff',
          weight: isSel ? 3 : 2,
          fillColor: color,
          fillOpacity: 0.92
        }).addTo(markerLayer);

        var stageLabel = r.stage === 'in_process' ? 'Construction In Process' : (r.stage === 'after_remodel' ? 'After Full Remodel' : 'Site Observation');
        var depthText = isShallow ? ('<span class="wt-shallow">' + r.depth + ' ft (SHALLOW TABLE!)</span>') : (r.depth + ' ft');
        marker.bindPopup('<div class="wt-popup"><b>' + r.addr + '</b><br/>Dig Depth: ' + depthText + '<br/>Stage: ' + stageLabel + '</div>');
        marker.on('click', function() {
          if (window.WaterTableBridge && window.WaterTableBridge.selectPin) {
            window.WaterTableBridge.selectPin(r.id);
          }
        });
        if (isSel) { marker.openPopup(); }
      });
    };
  </script>
</body>
</html>
"""
