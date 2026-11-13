package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.FloodRiskLevel
import com.example.data.model.LocalFloodNewsItem
import com.example.data.model.NwsAlert
import com.example.data.model.WaterReport
import com.example.ui.UiState
import com.example.ui.WaterTableViewModel
import com.example.ui.components.Base64MediaPreview
import com.example.ui.components.ConstructionStageBadge
import com.example.ui.components.InteractiveLeafletHydroMap
import com.example.ui.components.LossCategoryBadge
import com.example.ui.components.RainForecastBarChart
import com.example.ui.components.RiskLevelBadge
import com.example.ui.components.VerdictBadge
import com.example.ui.theme.FloodAmber
import com.example.ui.theme.FloodCrimson

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AtlasMapScreen(
    viewModel: WaterTableViewModel,
    reportsUi: UiState<List<WaterReport>>,
    onNavigateToDiagnose: () -> Unit,
    onAskContractorForReport: (WaterReport) -> Unit,
    weatherState: com.example.ui.WeatherNewsUiState,
    riskSummary: com.example.data.model.StreetFloodRiskSummary
) {
    val context = LocalContext.current
    var citySearchQuery by remember { mutableStateOf("") }
    var showCitySearchDialog by remember { mutableStateOf(false) }
    var selectedReport by remember { mutableStateOf<WaterReport?>(null) }
    var outcomeTargetReport by remember { mutableStateOf<WaterReport?>(null) }
    var cityFilterOnly by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.refreshWithLiveGps()
        } else {
            viewModel.setStatusBanner("Location permission denied. Search any city/town manually.")
        }
    }

    val allReports = (reportsUi as? UiState.Success)?.data.orEmpty()
    val filteredReports = remember(allReports, cityFilterOnly, weatherState.selectedCity) {
        if (!cityFilterOnly) allReports
        else allReports.filter { it.city.contains(weatherState.selectedCity, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // City & GPS Top Controls
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showCitySearchDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("search_city_button")
                ) {
                    Icon(Icons.Filled.Search, contentDescription = "Search city or town", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${weatherState.selectedCity}${if (weatherState.selectedState.isNotBlank()) ", ${weatherState.selectedState}" else ""}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledTonalButton(
                    onClick = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("gps_locate_button")
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = "Use current GPS location", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("GPS")
                }

                IconButton(
                    onClick = {
                        viewModel.refreshCityWeatherAndNews(
                            weatherState.selectedCity,
                            weatherState.selectedState,
                            weatherState.latitude,
                            weatherState.longitude,
                            weatherState.isLiveGps,
                            weatherState.addressHint
                        )
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("refresh_weather_news_button")
                ) {
                    if (weatherState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh live forecast and news")
                    }
                }
            }
        }

        // Pre-Rain Car & Street Flood Watch Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(126.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_flood_alert),
                            contentDescription = "Pre-rain street drainage flood risk illustration",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0x3306253A), Color(0xCC06253A))
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RiskLevelBadge(riskSummary.riskLevel)
                                Text(
                                    text = "Score ${riskSummary.riskScore}/100",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = riskSummary.preRainHeadline,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DirectionsCar,
                                contentDescription = "Car flood safety advisory",
                                tint = when (riskSummary.riskLevel) {
                                    FloodRiskLevel.CRITICAL -> FloodCrimson
                                    FloodRiskLevel.HIGH -> FloodAmber
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = riskSummary.carAdvisory,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricPill("24h Rain", "${String.format(java.util.Locale.US, "%.1f", riskSummary.upcomingRainMm)} mm")
                            MetricPill("Soil Moisture", "${riskSummary.soilSaturationPct}%")
                            MetricPill("Shallow Zones", "${riskSummary.shallowWaterTableReportsCount}")
                            MetricPill("NWS Alerts", "${riskSummary.activeAlertCount}")
                        }

                        weatherState.forecast?.let { fc ->
                            RainForecastBarChart(points = fc.hourlyPoints)
                        }
                    }
                }
            }
        }

        // Interactive Leaflet GPS Map + Clickable Dig Depth & Construction Stage Sheet
        item {
            InteractiveLeafletHydroMap(
                centerLat = weatherState.latitude,
                centerLon = weatherState.longitude,
                cityName = weatherState.selectedCity,
                reports = filteredReports,
                selectedReportId = selectedReport?.id,
                riskLevel = riskSummary.riskLevel,
                onSelectReport = { selectedReport = it }
            )

            selectedReport?.let { pinned ->
                Spacer(modifier = Modifier.height(10.dp))
                PinnedReportDetailsCard(
                    report = pinned,
                    onClose = { selectedReport = null },
                    onAskContractor = { onAskContractorForReport(pinned) }
                )
            }
        }

        // Live NWS Alerts + Local News Channels
        if (weatherState.alerts.isNotEmpty() || weatherState.newsItems.isNotEmpty()) {
            item {
                LiveNewsAndAlertsSection(
                    city = weatherState.selectedCity,
                    alerts = weatherState.alerts,
                    newsItems = weatherState.newsItems,
                    onOpenUrl = { url ->
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    }
                )
            }
        }

        // Community Reports Header & Filter
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Community Map Pins (${filteredReports.size})",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = if (cityFilterOnly) "Showing ${weatherState.selectedCity} logs" else "Tap any map pin or card for dig depth & remodel advice",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextButton(
                        onClick = { cityFilterOnly = !cityFilterOnly },
                        modifier = Modifier.testTag("toggle_city_filter_button")
                    ) {
                        Text(if (cityFilterOnly) "All" else "City")
                    }
                    FilledTonalButton(
                        onClick = onNavigateToDiagnose,
                        modifier = Modifier.testTag("quick_add_report_button")
                    ) {
                        Icon(Icons.Filled.AddLocationAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log")
                    }
                }
            }
        }

        when (reportsUi) {
            UiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = reportsUi.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
            is UiState.Success -> {
                if (filteredReports.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                                Text("No Groundwater Reports Here Yet", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Log a dig depth, construction stage, or restoration photo for ${weatherState.selectedCity}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(onClick = onNavigateToDiagnose) { Text("Log Photo & GPS Stamp") }
                            }
                        }
                    }
                } else {
                    items(filteredReports, key = { it.id }) { report ->
                        CommunityReportCard(
                            report = report,
                            onSelectOnMap = { selectedReport = report },
                            onUpvote = { viewModel.upvoteReport(report) },
                            onLogOutcome = { outcomeTargetReport = report },
                            onAskEngineer = { onAskContractorForReport(report) }
                        )
                    }
                }
            }
        }
    }

    if (showCitySearchDialog) {
        AlertDialog(
            onDismissRequest = { showCitySearchDialog = false },
            title = { Text("Select City or Town") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = citySearchQuery,
                        onValueChange = {
                            citySearchQuery = it
                            viewModel.searchCities(it)
                        },
                        label = { Text("City or town name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("city_search_input")
                    )
                    weatherState.citySearchResults.forEach { result ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectCity(result)
                                    showCitySearchDialog = false
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(result.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    listOf(result.admin1, result.country).filter { it.isNotBlank() }.joinToString(", "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCitySearchDialog = false }) { Text("Close") }
            }
        )
    }

    outcomeTargetReport?.let { target ->
        LogOutcomeDialog(
            report = target,
            onDismiss = { outcomeTargetReport = null },
            onSubmit = { solution, worked, score, cost, notes ->
                viewModel.logSolutionOutcome(
                    report = target,
                    city = target.city,
                    soilType = target.soilType,
                    waterSource = target.waterSourceVerdict,
                    solutionName = solution,
                    worked = worked,
                    score = score,
                    costUsd = cost,
                    notes = notes
                )
                outcomeTargetReport = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PinnedReportDetailsCard(
    report: WaterReport,
    onClose: () -> Unit,
    onAskContractor: () -> Unit
) {
    val shallowSeverity = when {
        report.depthFeet in 0.1..5.0 -> "SHALLOW WATER TABLE (${report.depthFeet} ft) — High Hydrostatic Pressure!"
        report.depthFeet <= 12.0 -> "MODERATE DEPTH (${report.depthFeet} ft) — Seasonal Table Contact Risk"
        else -> "DEEPER TABLE (${report.depthFeet} ft)"
    }
    val stagePlanTitle = when (report.constructionStage) {
        "in_process" -> "Before Contractors Finish Property:"
        "after_remodel" -> "After Full Remodel Repair Guidance:"
        else -> "Recommended Site Action:"
    }
    val fallbackAdvice = when (report.constructionStage) {
        "in_process" -> "Install sub-slab perforated French drain with washed #57 gravel + geotextile fabric, vapor barrier, and sump sleeve BEFORE pouring concrete or closing drywall."
        "after_remodel" -> "Protect finished remodel with an exterior perimeter French drain cut outside foundations, core-drilled relief sump in utility/crawl space, and baseboard micro-channel retrofit."
        else -> report.recommendedSolutions.firstOrNull() ?: "Verify seasonal water table with a 24h test bore or consult a licensed pro."
    }

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("pinned_report_details_card")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${report.city} · ${report.addressHint.ifBlank { "GPS Pin" }}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onClose) { Text("Close") }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LossCategoryBadge(report.lossCategory)
                ConstructionStageBadge(report.constructionStage)
                VerdictBadge(report.waterSourceVerdict, report.engineerVerified)
            }

            Surface(
                color = if (report.depthFeet <= 5.0) FloodCrimson.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Dig Depth: $shallowSeverity · Soil: ${report.soilType}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (report.depthFeet <= 5.0) FloodCrimson else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Construction, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(stagePlanTitle, style = MaterialTheme.typography.labelLarge)
                    }
                    Text(
                        text = report.restorationScope.ifBlank { fallbackAdvice },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            FilledTonalButton(onClick = onAskContractor, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Engineering, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ask Nearest Licensed Pro About This Spot")
            }
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LiveNewsAndAlertsSection(
    city: String,
    alerts: List<NwsAlert>,
    newsItems: List<LocalFloodNewsItem>,
    onOpenUrl: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Newspaper, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(text = "Live Alerts & Local News ($city)", style = MaterialTheme.typography.titleMedium)
            }

            alerts.forEach { alert ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "${alert.event} (${alert.severity})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        if (alert.headline.isNotBlank()) {
                            Text(text = alert.headline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            newsItems.take(4).forEachIndexed { idx, news ->
                if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenUrl(news.link) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = news.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(text = "${news.source} · ${news.pubDate}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open news article", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CommunityReportCard(
    report: WaterReport,
    onSelectOnMap: () -> Unit,
    onUpvote: () -> Unit,
    onLogOutcome: () -> Unit,
    onAskEngineer: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.clickable { onSelectOnMap() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${report.city}${if (report.stateOrRegion.isNotBlank()) ", ${report.stateOrRegion}" else ""}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${report.addressHint.ifBlank { "GPS Verified Zone" }} · ${report.authorName} (${report.authorRole})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = if (report.depthFeet in 0.1..5.0) FloodCrimson.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "${report.depthFeet} ft",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (report.depthFeet in 0.1..5.0) FloodCrimson else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LossCategoryBadge(report.lossCategory)
                ConstructionStageBadge(report.constructionStage)
                VerdictBadge(report.waterSourceVerdict, report.engineerVerified)
            }

            if (report.mediaPreviewBase64.isNotBlank()) {
                Base64MediaPreview(
                    base64 = report.mediaPreviewBase64,
                    mediaType = report.mediaType,
                    videoDurationLabel = report.videoDurationLabel,
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                )
            }

            if (report.extraPhotosBase64.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(report.extraPhotosBase64) { extraB64 ->
                        Base64MediaPreview(
                            base64 = extraB64,
                            mediaType = "photo",
                            videoDurationLabel = "",
                            modifier = Modifier.size(84.dp)
                        )
                    }
                }
            }

            if (report.lossDescription.isNotBlank()) {
                Text(text = report.lossDescription, style = MaterialTheme.typography.bodyMedium)
            }
            if (report.aiSummary.isNotBlank()) {
                Text(text = report.aiSummary, style = MaterialTheme.typography.bodySmall)
            }
            if (report.restorationScope.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Scope & Repair Plan: ${report.restorationScope}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
            if (report.notes.isNotBlank()) {
                Text(
                    text = "Field Note: ${report.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = String.format(
                    java.util.Locale.US,
                    "GPS %.4f, %.4f · Soil: %s · Street Risk: %s",
                    report.latitude, report.longitude, report.soilType, report.streetFloodRisk.uppercase()
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onUpvote, modifier = Modifier.height(44.dp)) {
                    Icon(Icons.Filled.ThumbUp, contentDescription = "Confirm report", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${report.upvotesCount}")
                }
                OutlinedButton(onClick = onLogOutcome, modifier = Modifier.weight(1f).height(44.dp)) {
                    Text("Log Fix Result", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                FilledTonalButton(onClick = onAskEngineer, modifier = Modifier.height(44.dp)) {
                    Icon(Icons.Filled.Engineering, contentDescription = "Ask licensed pro", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pro")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogOutcomeDialog(
    report: WaterReport,
    onDismiss: () -> Unit,
    onSubmit: (solution: String, worked: Boolean, score: Int, costUsd: Int, notes: String) -> Unit
) {
    val presetSolutions = listOf(
        "Perimeter French Drain + Geotextile",
        "Cast-Iron Sump Pump + Battery Backup",
        "Hydrostatic Relief Valve + Hydraulic Cement",
        "LGR Dehumidification + 24in Flood Cut + Antimicrobial",
        "HEPA Negative Air Containment + Mold Remediation",
        "HEPA Soot Cleaning + Thermal Deodorization"
    )
    var selectedSolution by remember { mutableStateOf(report.recommendedSolutions.firstOrNull() ?: presetSolutions.first()) }
    var expanded by remember { mutableStateOf(false) }
    var worked by remember { mutableStateOf(true) }
    var score by remember { mutableIntStateOf(5) }
    var costText by remember { mutableStateOf("450") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Train Best Solutions Algorithm") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Share what solution worked for ${report.city} (${report.soilType} soil).",
                    style = MaterialTheme.typography.bodySmall
                )
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedSolution,
                        onValueChange = { selectedSolution = it },
                        label = { Text("Solution Applied") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        presetSolutions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedSolution = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Did this solve the issue?", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = worked, onCheckedChange = { worked = it })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..5).forEach { star ->
                        FilledTonalButton(
                            onClick = { score = star },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(if (score == star) "$star★" else "$star")
                        }
                    }
                }
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Approx. Cost (USD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes for neighbors") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        selectedSolution.ifBlank { presetSolutions.first() },
                        worked,
                        score,
                        costText.toIntOrNull() ?: 0,
                        notes
                    )
                }
            ) { Text("Save Outcome") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
