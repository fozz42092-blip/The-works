package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.PestControl
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.catalog.DiyEvaluation
import com.example.data.catalog.WaterTableCatalogs
import com.example.data.model.PestControlAiAnalysis
import com.example.ui.MediaAttachment
import com.example.ui.UiState
import com.example.ui.WaterTableViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiyLeakScreen(
    diyResults: Map<String, String>,
    onAnswerChange: (stepId: String, value: String) -> Unit,
    onNavigateToContractors: (DiyEvaluation) -> Unit,
    onNavigateToWholesale: () -> Unit,
    onApplySoilBottleToDiagnose: (String) -> Unit = {},
    onOpenVideoStudioWithPrompt: (String) -> Unit = {},
    viewModel: WaterTableViewModel? = null,
    city: String = "Local",
    pestPhotoAttachment: MediaAttachment? = null,
    pestExtraPhotos: List<MediaAttachment> = emptyList(),
    pestAnalysisState: UiState<PestControlAiAnalysis>? = null,
    onNavigateToPestContractors: () -> Unit = {}
) {
    val context = LocalContext.current
    val evaluation = remember(diyResults) {
        WaterTableCatalogs.evaluateDiyTestResults(diyResults)
    }

    var activeSection by remember { mutableStateOf("all") } // "all" | "leak" | "soil" | "shutoff" | "videos"
    var sandPct by remember { mutableFloatStateOf(30f) }
    var siltPct by remember { mutableFloatStateOf(35f) }
    var clayPct by remember { mutableFloatStateOf(35f) }
    val soilResult = remember(sandPct, siltPct, clayPct) {
        WaterTableCatalogs.evaluateSoilBottleTest(
            sandPct.roundToInt(),
            siltPct.roundToInt(),
            clayPct.roundToInt()
        )
    }

    var videoQuery by remember { mutableStateOf("") }
    var videoCatFilter by remember { mutableStateOf("all") }
    val filteredVideos = remember(videoQuery, videoCatFilter) {
        WaterTableCatalogs.howToVideoGuides.filter { guide ->
            val catOk = videoCatFilter == "all" || guide.category == videoCatFilter
            val qOk = videoQuery.isBlank() ||
                guide.title.contains(videoQuery, ignoreCase = true) ||
                guide.summary.contains(videoQuery, ignoreCase = true)
            catOk && qOk
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "DIY Homeowner & Field Toolkits",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Run soil shake tests, shut off water at the meter, inspect leak causes yourself, and follow step-by-step A–Z how-to & mold remediation videos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "all" to "All DIY Tools",
                        "pest" to "Pest & Termite",
                        "soil" to "Soil Bottle Test",
                        "shutoff" to "Water Shutoff",
                        "videos" to "How-To Videos",
                        "leak" to "5 Leak Tests"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = activeSection == key,
                            onClick = { activeSection = key },
                            label = { Text(label) },
                            modifier = Modifier.testTag("diy_filter_$key")
                        )
                    }
                }
            }
        }

        // Pest, Rodent Entry Photo AI & Termite Residual Science Section
        if (viewModel != null && activeSection == "pest") {
            item {
                PestControlAiContentColumn(
                    viewModel = viewModel,
                    city = city,
                    primaryPhoto = pestPhotoAttachment,
                    extraPhotos = pestExtraPhotos,
                    analysisState = pestAnalysisState,
                    onNavigateToContractors = onNavigateToPestContractors
                )
            }
        } else if (viewModel != null && activeSection == "all") {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.PestControl, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "DIY Pest, Rodent Seal-Up & Termite AI",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Photo possible rodent entry points to spot obvious openings, get seal-up steps with DoMyOwn.com supply links, and learn why Tenting leaves zero residual vs Hot Spot + Bait Stations.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        FilledTonalButton(
                            onClick = { activeSection = "pest" },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Open Pest, Rodent Entry & Termite Guide")
                        }
                    }
                }
            }
        }

        // Water-Bottle Soil Shake Test Calculator
        if (activeSection == "all" || activeSection == "soil") {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.testTag("diy_soil_bottle_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Science, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Water-Bottle Soil Shake Test",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Fill a clear water bottle 1/3 with yard soil (6–12\" deep) + 2/3 water + 1 drop soap. Shake 2 min & read layer thicknesses:",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Bottom Layer (1 min): Sand (${sandPct.roundToInt()} parts)",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Slider(value = sandPct, onValueChange = { sandPct = it }, valueRange = 0f..100f)

                        Text(
                            text = "• Middle Layer (2 hrs): Silt (${siltPct.roundToInt()} parts)",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Slider(value = siltPct, onValueChange = { siltPct = it }, valueRange = 0f..100f)

                        Text(
                            text = "• Top Cloudy Layer (24 hrs): Clay (${clayPct.roundToInt()} parts)",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Slider(value = clayPct, onValueChange = { clayPct = it }, valueRange = 0f..100f)

                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "${soilResult.usdaTextureClass} (${soilResult.sandPercent}% Sand · ${soilResult.siltPercent}% Silt · ${soilResult.clayPercent}% Clay)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Drainage: ${soilResult.drainageRating}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = soilResult.foundationAdvisory,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onApplySoilBottleToDiagnose(soilResult.mappedSoilTypeKey) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("apply_soil_bottle_button")
                            ) {
                                Text("Apply Soil (${soilResult.mappedSoilTypeKey}) to Diagnose")
                            }
                        }
                    }
                }
            }
        }

        // Emergency Water Shutoff Guide (House Main + Street Curb Meter)
        if (activeSection == "all" || activeSection == "shutoff") {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)),
                    modifier = Modifier.testTag("diy_water_shutoff_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text(
                                text = "Emergency Water Shutoff: House & Curb Meter",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        WaterTableCatalogs.emergencyWaterShutoffSteps.forEach { (title, body) ->
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(text = title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    Text(text = body, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                val guide = WaterTableCatalogs.howToVideoGuides.first { it.id == "vid_shutoff_meter" }
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(guide.videoSearchUrl)))
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("watch_shutoff_video_button")
                        ) {
                            Icon(Icons.Filled.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Watch Street Curb Meter & Main Valve Shutoff Video")
                        }
                    }
                }
            }
        }

        // Searchable How-To & Mold Remediation Video Library
        if (activeSection == "all" || activeSection == "videos") {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.testTag("diy_video_library_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.VideoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "How-To & Mold Remediation Video Guides",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedTextField(
                            value = videoQuery,
                            onValueChange = { videoQuery = it },
                            label = { Text("Search house problem (mold, soil, shutoff, roof, sink, copper)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                "all" to "All",
                                "mold" to "Mold S520",
                                "soil_water" to "Soil Test",
                                "shutoff" to "Shutoff",
                                "plumbing_roof" to "Roof & Pipes",
                                "drainage" to "Exterior Runoff"
                            ).forEach { (k, lbl) ->
                                FilterChip(
                                    selected = videoCatFilter == k,
                                    onClick = { videoCatFilter = k },
                                    label = { Text(lbl) }
                                )
                            }
                        }
                        filteredVideos.forEach { guide ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = guide.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
                                            Text(
                                                text = guide.durationLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    Text(text = guide.summary, style = MaterialTheme.typography.bodySmall)
                                    guide.stepByStepChecklist.forEach { tip ->
                                        Text("• $tip", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilledTonalButton(
                                            onClick = {
                                                runCatching {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(guide.videoSearchUrl)))
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                        ) {
                                            Icon(Icons.Filled.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Watch Tutorials")
                                        }
                                        OutlinedButton(
                                            onClick = { onOpenVideoStudioWithPrompt("${guide.title}: ${guide.videoPromptHint}") },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                        ) {
                                            Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("AI Clip Guide")
                                        }
                                    }
                                }
                            }
                        }
                        Text(
                            text = "Need commercial HEPA air scrubbers or LGR dehumidifiers for mold/water remediation? Rent directly from nearby pros or neighbors in the Supply & Rentals tab!",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Live Diagnostic Verdict Card (5 Leak Tests)
        if (activeSection == "all" || activeSection == "leak") {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = evaluation.confidenceBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = evaluation.headline,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = evaluation.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        evaluation.nextSteps.forEach { step ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onNavigateToContractors(evaluation) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("diy_contact_contractor_button")
                            ) {
                                Icon(Icons.Filled.Engineering, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Contact Pro")
                            }
                            FilledTonalButton(
                                onClick = onNavigateToWholesale,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("diy_open_wholesale_button")
                            ) {
                                Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Supply & Rentals")
                            }
                        }
                    }
                }
            }

            // Interactive 5 Leak Test Steps
            items(WaterTableCatalogs.diyLeakProtocols, key = { it.id }) { step ->
                val current = diyResults[step.id] ?: "untested"
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = step.title,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = step.durationLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = step.instructions,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("• Positive (+): ${step.positiveMeans}", style = MaterialTheme.typography.bodySmall)
                                Text("• Negative (-): ${step.negativeMeans}", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = current == "yes",
                                onClick = { onAnswerChange(step.id, "yes") },
                                label = { Text("Positive (+)", fontWeight = FontWeight.SemiBold) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = current == "no",
                                onClick = { onAnswerChange(step.id, "no") },
                                label = { Text("Negative (-)", fontWeight = FontWeight.SemiBold) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = current == "untested",
                                onClick = { onAnswerChange(step.id, "untested") },
                                label = { Text("Untested") }
                            )
                        }
                    }
                }
            }
        }
    }
}
