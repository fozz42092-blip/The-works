package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.PestControl
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.catalog.WaterTableCatalogs
import com.example.data.model.DiyPestSupplyItem
import com.example.data.model.PestControlAiAnalysis
import com.example.ui.MediaAttachment
import com.example.ui.UiState
import com.example.ui.WaterTableViewModel
import com.example.ui.theme.AquiferTeal
import com.example.ui.theme.AquiferTealLight
import com.example.ui.theme.FloodAmber
import com.example.ui.theme.FloodAmberContainer
import com.example.ui.theme.FloodCrimson
import com.example.ui.theme.FloodCrimsonContainer
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.SafeEmeraldContainer
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PestControlAiContentColumn(
    viewModel: WaterTableViewModel,
    city: String,
    primaryPhoto: MediaAttachment?,
    extraPhotos: List<MediaAttachment>,
    analysisState: UiState<PestControlAiAnalysis>?,
    onNavigateToContractors: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    var selectedFocus by remember { mutableStateOf("Rodent Entry & Sealing") }
    var userConcern by remember { mutableStateOf("") }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var capturePrimary by remember { mutableStateOf(true) }

    val primaryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.attachPestPhotoFromUri(context, uri)
    }

    val extraPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.addPestExtraPhotoFromUri(context, uri)
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            if (capturePrimary) viewModel.attachPestPhotoFromUri(context, uri)
            else viewModel.addPestExtraPhotoFromUri(context, uri)
        }
    }

    fun launchPestCamera(isPrimary: Boolean) {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        val file = File(dir, "pest_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        capturePrimary = isPrimary
        pendingCameraUri = uri
        takePictureLauncher.launch(uri)
    }

    val focusOptions = listOf(
        "Rodent Entry & Sealing",
        "Termites (Tent vs Hot Spot)",
        "Attic / Wall Scratching",
        "Ants / Roaches / Spiders",
        "Moisture & Crawlspace Pests",
        "General Pest Diagnosis"
    )
    val totalPhotos = (if (primaryPhoto != null) 1 else 0) + extraPhotos.size

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Banner
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.PestControl, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "AI Pest, Rodent Entry & Termite Advisor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Snap photos of gaps, vents, garage corners, chewed trim, or describe any pest concern. Gemini AI spots obvious rodent entry points, gives seal-up steps, links exact DIY supplies on DoMyOwn.com, and compares Termite Testing/Tenting vs Residual Hot Spot + Exterior Bait Stations.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Photo Upload Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1. Photo Possible Rodent Entry / Pest Signs ($totalPhotos/4)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (totalPhotos > 0) {
                        OutlinedButton(
                            onClick = { viewModel.clearAllPestPhotos() },
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Clear", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                if (primaryPhoto?.previewBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Image(
                            bitmap = primaryPhoto.previewBitmap.asImageBitmap(),
                            contentDescription = "Suspected rodent or pest entry point photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { viewModel.clearPestPrimaryPhoto() },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(48.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.72f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Remove pest entry photo",
                                        tint = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Best spots to photograph for Rodent & Pest Entry:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "• AC/plumbing pipe wall penetrations, garage door bottom corners, crawlspace & attic screens, roof eaves/gable vents, foundation weep holes, rub smudges, droppings, or termite pellets/mud tubes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (extraPhotos.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(extraPhotos) { idx, item ->
                            item.previewBitmap?.let { bmp ->
                                Box(
                                    modifier = Modifier
                                        .size(84.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                ) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Additional pest photo ${idx + 2}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    IconButton(
                                        onClick = { viewModel.removePestExtraPhotoAt(idx) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(32.dp)
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.72f),
                                            shape = RoundedCornerShape(50),
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = "Remove extra pest photo",
                                                    tint = MaterialTheme.colorScheme.surface,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { launchPestCamera(isPrimary = primaryPhoto == null) },
                        enabled = totalPhotos < 4,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("pest_take_photo_button")
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (primaryPhoto == null) "Take Photo" else "+Camera", maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = {
                            if (primaryPhoto == null) {
                                primaryPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            } else {
                                extraPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        },
                        enabled = totalPhotos < 4,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("pest_pick_photo_button")
                    ) {
                        Icon(
                            imageVector = if (primaryPhoto == null) Icons.Filled.PhotoLibrary else Icons.Filled.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (primaryPhoto == null) "Upload Photo" else "+Angle (${extraPhotos.size}/3)", maxLines = 1)
                    }
                }
            }
        }

        // Concern / Symptom Analyzer Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "2. Describe Pest Concerns or Symptoms",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    focusOptions.forEach { option ->
                        FilterChip(
                            selected = selectedFocus == option,
                            onClick = { selectedFocus = option },
                            label = { Text(option) }
                        )
                    }
                }
                OutlinedTextField(
                    value = userConcern,
                    onValueChange = { userConcern = it },
                    label = {
                        Text("What are you seeing/hearing? (gaps, droppings, scratching, frass pellets, mud tubes, swarmers, ants)")
                    },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pest_concern_input")
                )
                Button(
                    onClick = {
                        viewModel.runPestControlAiAnalysis(
                            focusType = selectedFocus,
                            userConcern = userConcern
                        )
                    },
                    enabled = analysisState !is UiState.Loading,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("pest_analyze_button")
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Diagnose Current Pest Problem & Seal-Up Plan", fontWeight = FontWeight.Bold)
                }
            }
        }

        // AI Output Section
        when (analysisState) {
            UiState.Loading -> {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Inspecting entry points, signs, seal-up steps & residual options...",
                            style = MaterialTheme.typography.titleSmall
                        )
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            is UiState.Error -> {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = analysisState.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedButton(
                            onClick = {
                                viewModel.runPestControlAiAnalysis(
                                    focusType = selectedFocus,
                                    userConcern = userConcern
                                )
                            },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry Analysis")
                        }
                    }
                }
            }
            is UiState.Success -> {
                PestAnalysisResultCard(
                    analysis = analysisState.data,
                    city = city,
                    onOpenUrl = { url -> uriHandler.openUri(url) },
                    onFindPro = {
                        viewModel.routePestIssueToPros(
                            "${analysisState.data.suspectedCurrentProblem}: ${analysisState.data.confidenceSummary}"
                        )
                        onNavigateToContractors()
                    }
                )
            }
            null -> Unit
        }

        // Always-Available Termite Testing vs Tenting vs Hot Spot + Ground Bait Station Science Card
        TermiteScienceAndComparisonCard(
            onOpenUrl = { url -> uriHandler.openUri(url) }
        )

        // Direct DIY Pest Control Store & Essential Gear Card
        DiyPestSupplyDirectoryCard(
            onOpenUrl = { url -> uriHandler.openUri(url) }
        )
    }
}

@Composable
private fun PestAnalysisResultCard(
    analysis: PestControlAiAnalysis,
    city: String,
    onOpenUrl: (String) -> Unit,
    onFindPro: () -> Unit
) {
    val (badgeBg, badgeFg) = when (analysis.urgencyLevel.lowercase()) {
        "urgent", "critical" -> FloodCrimsonContainer to FloodCrimson
        "elevated", "moderate" -> FloodAmberContainer to FloodAmber
        "low" -> SafeEmeraldContainer to SafeEmerald
        else -> AquiferTealLight to AquiferTeal
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Suggested Current Property Problem",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = analysis.suspectedCurrentProblem,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(color = badgeBg, shape = RoundedCornerShape(50)) {
                    Text(
                        text = analysis.urgencyLevel.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeFg,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            if (analysis.confidenceSummary.isNotBlank()) {
                Text(analysis.confidenceSummary, style = MaterialTheme.typography.bodyMedium)
            }

            if (analysis.obviousSignsDetected.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Obvious Signs & Entry Indicators Detected",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        analysis.obviousSignsDetected.forEach { sign ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WarningAmber,
                                    contentDescription = null,
                                    tint = badgeFg,
                                    modifier = Modifier.padding(top = 2.dp).size(15.dp)
                                )
                                Text(sign, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            if (analysis.howToSealAndExcludeSteps.isNotEmpty()) {
                Text(
                    text = "How to Seal Up & Fix Yourself",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                analysis.howToSealAndExcludeSteps.forEachIndexed { idx, step ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = SafeEmerald,
                            modifier = Modifier.padding(top = 2.dp).size(15.dp)
                        )
                        Text("${idx + 1}. $step", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (analysis.treatmentAndResidualInsight.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Residual vs. Non-Residual Treatment Insight",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(analysis.treatmentAndResidualInsight, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (analysis.termiteStrategyNote.isNotBlank()) {
                Surface(
                    color = FloodAmberContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Termite Protection Note",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = FloodAmber
                        )
                        Text(analysis.termiteStrategyNote, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = "DIY Supplies Needed (Direct Links to ${WaterTableCatalogs.DIY_PEST_WEBSITE_NAME})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            analysis.diySuppliesNeeded.forEach { supply ->
                SupplyItemLinkRow(item = supply, onOpenUrl = onOpenUrl)
            }

            if (analysis.recommendedProReason.isNotBlank()) {
                Text(
                    text = if (analysis.shouldCallLicensedPro) "Pro Escalation Advice: ${analysis.recommendedProReason}" else "DIY Outlook: ${analysis.recommendedProReason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onFindPro,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Filled.Engineering, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Match Licensed Pest & Exclusion Pro in $city")
            }
        }
    }
}

@Composable
private fun TermiteScienceAndComparisonCard(
    onOpenUrl: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, FloodAmber.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = FloodAmber)
                Text(
                    text = "Termite Guide: Testing & Tenting vs. Hot Spot Residual + Ground Bait",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(
                color = FloodCrimsonContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Why Termite Testing / Tenting Alone Fails Long-Term (Zero Residual):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = FloodCrimson
                    )
                    Text(
                        text = "A standard termite test/inspection & sulfuryl fluoride tent fumigation only kills termites present inside the house on the day of tenting. Once the tent comes off and vents out, ZERO chemical residual stays in the wood or soil—meaning neighboring swarmers can fly right back into the house almost immediately.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            WaterTableCatalogs.termiteComparisonGuide.forEach { method ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(method.methodTitle, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Residual: ${method.residualProtection}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("✓ Pros: ${method.pros}", style = MaterialTheme.typography.bodySmall)
                        Text("✗ Cons: ${method.cons}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "Bottom Line: ${method.verdict}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = { onOpenUrl("https://www.domyown.com/termites-c-3.html") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("View Residual Termite Liquids, Borates & Bait Stations (DoMyOwn.com)")
            }
        }
    }
}

@Composable
private fun DiyPestSupplyDirectoryCard(
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DIY Pest Control Supply Store Links",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Do the exclusion & treatment yourself with commercial-grade items via ${WaterTableCatalogs.DIY_PEST_WEBSITE_NAME}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            WaterTableCatalogs.diyPestSupplyCatalog.forEach { supply ->
                SupplyItemLinkRow(item = supply, onOpenUrl = onOpenUrl)
            }

            FilledTonalButton(
                onClick = { onOpenUrl(WaterTableCatalogs.DIY_PEST_WEBSITE_HOME) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("open_domyown_website_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Browse All DIY Supplies on DoMyOwn.com")
            }
        }
    }
}

@Composable
private fun SupplyItemLinkRow(
    item: DiyPestSupplyItem,
    onOpenUrl: (String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.category.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(item.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text(item.purpose, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(
                onClick = { onOpenUrl(item.websiteUrl) },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text("Shop", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open DIY product on DoMyOwn.com", modifier = Modifier.size(14.dp))
            }
        }
    }
}
