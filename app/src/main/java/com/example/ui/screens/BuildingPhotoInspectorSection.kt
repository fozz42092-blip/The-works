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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Foundation
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Roofing
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WaterDrop
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.catalog.WaterTableCatalogs
import com.example.data.model.BuildingPhotoAiAnalysis
import com.example.data.model.BuildingProblemCategoryAssessment
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
fun BuildingPhotoInspectorTab(
    viewModel: WaterTableViewModel,
    city: String,
    primaryPhoto: MediaAttachment?,
    extraPhotos: List<MediaAttachment>,
    analysisState: UiState<BuildingPhotoAiAnalysis>?,
    onNavigateToContractors: () -> Unit,
    onOpenDiyVideos: () -> Unit,
    onRequestHygienist: () -> Unit,
    onOpenVideoStudioWithText: (String) -> Unit
) {
    val context = LocalContext.current

    var buildingType by remember { mutableStateOf("Residential House") }
    var selectedPortion by remember { mutableStateOf("Full House / Building Exterior") }
    var customPortionText by remember { mutableStateOf("") }
    var inspectorNotes by remember { mutableStateOf("") }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var cameraCaptureTargetIsPrimary by remember { mutableStateOf(true) }

    val primaryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.attachBuildingPhotoFromUri(context, uri)
    }

    val extraPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.addBuildingExtraPhotoFromUri(context, uri)
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            if (cameraCaptureTargetIsPrimary) {
                viewModel.attachBuildingPhotoFromUri(context, uri)
            } else {
                viewModel.addBuildingExtraPhotoFromUri(context, uri)
            }
        }
    }

    fun launchCameraCapture(isPrimary: Boolean) {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        val file = File(dir, "bldg_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        cameraCaptureTargetIsPrimary = isPrimary
        pendingCameraUri = uri
        takePictureLauncher.launch(uri)
    }

    val buildingTypes = listOf(
        "Residential House",
        "Commercial / Multi-Unit",
        "Future Property (Pre-Buy)"
    )

    val portionOptions = listOf(
        "Full House / Building Exterior",
        "Yard Slope & Drainage",
        "Roof, Gutters & Flashing",
        "Foundation, Slab & Footing",
        "Basement / Crawlspace",
        "Interior Wall / Ceiling",
        "Plumbing / Under-Sink / Pipes",
        "Siding, Stucco & Balcony",
        "Electrical / HVAC / Garage",
        "Other Building Portion"
    )

    val effectivePortion = if (selectedPortion == "Other Building Portion" && customPortionText.isNotBlank()) {
        customPortionText.trim()
    } else {
        selectedPortion
    }
    val totalPhotosCount = (if (primaryPhoto != null) 1 else 0) + extraPhotos.size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Explainer Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Apartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "AI House & Building Photo Inspector",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = "Snap or upload photos of ANY portion of a house or building—exterior walls, yard grading, roof, foundation, basement, plumbing, or interior rooms. Gemini AI highlights visual Drainage, Roofing & Foundation indicators plus real-life problems that can occur and clear next steps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Photo Upload & Camera Card
        item {
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
                            text = "1. Take or Upload Photo(s) ($totalPhotosCount/4)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (totalPhotosCount > 0) {
                            OutlinedButton(
                                onClick = { viewModel.clearAllBuildingPhotos() },
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Clear All", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    if (primaryPhoto?.previewBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Image(
                                bitmap = primaryPhoto.previewBitmap.asImageBitmap(),
                                contentDescription = "Uploaded house or building portion photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.72f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "Primary Photo · Ready for Gemini Vision",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.clearBuildingPrimaryPhoto() },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(48.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Remove primary building photo",
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
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "No building photo attached yet",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Tip: For full exterior scans, step back 15–25 ft to show the roofline, gutters, siding & ground slope. Or snap a close-up of any crack, stain, pipe, attic, or room.",
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
                                            .size(88.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    ) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "Additional building angle ${idx + 2}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        IconButton(
                                            onClick = { viewModel.removeBuildingExtraPhotoAt(idx) },
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
                                                        contentDescription = "Remove extra building photo",
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
                            onClick = { launchCameraCapture(isPrimary = primaryPhoto == null) },
                            enabled = totalPhotosCount < 4,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("exterior_take_photo_button")
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (primaryPhoto == null) "Take Photo" else "+Camera Angle", maxLines = 1)
                        }

                        FilledTonalButton(
                            onClick = {
                                if (primaryPhoto == null) {
                                    primaryPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                } else {
                                    extraPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            },
                            enabled = totalPhotosCount < 4,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("exterior_pick_photo_button")
                        ) {
                            Icon(
                                imageVector = if (primaryPhoto == null) Icons.Filled.PhotoLibrary else Icons.Filled.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (primaryPhoto == null) "Upload Photo" else "+Gallery (${extraPhotos.size}/3)", maxLines = 1)
                        }
                    }
                }
            }
        }

        // Portion Selector & Notes Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "2. Select Building Type & Portion",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        buildingTypes.forEach { type ->
                            FilterChip(
                                selected = buildingType == type,
                                onClick = { buildingType = type },
                                label = { Text(type) }
                            )
                        }
                    }

                    Text("Portion of House / Building Photographed", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        portionOptions.forEach { portion ->
                            FilterChip(
                                selected = selectedPortion == portion,
                                onClick = { selectedPortion = portion },
                                label = { Text(portion) }
                            )
                        }
                    }

                    if (selectedPortion == "Other Building Portion") {
                        OutlinedTextField(
                            value = customPortionText,
                            onValueChange = { customPortionText = it },
                            label = { Text("Specify portion (e.g., retaining wall, parapet roof, garage header)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = inspectorNotes,
                        onValueChange = { inspectorNotes = it },
                        label = { Text("Optional notes (age of building, recent rain, odors, hairline crack history)") },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("exterior_notes_input")
                    )

                    Button(
                        onClick = {
                            viewModel.runBuildingPhotoAiAnalysis(
                                buildingType = buildingType,
                                buildingPortion = effectivePortion,
                                notes = inspectorNotes
                            )
                        },
                        enabled = analysisState !is UiState.Loading,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("exterior_analyze_button")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyze Photo(s) with Gemini AI", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Results Section
        when (analysisState) {
            UiState.Loading -> {
                item {
                    Card(shape = RoundedCornerShape(18.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Scanning photo(s) in $city for Drainage, Roofing, Foundation & real-life structural risks...",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = analysisState.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (primaryPhoto != null) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.runBuildingPhotoAiAnalysis(
                                            buildingType = buildingType,
                                            buildingPortion = effectivePortion,
                                            notes = inspectorNotes
                                        )
                                    },
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry AI Inspection")
                                }
                            }
                        }
                    }
                }
            }
            is UiState.Success -> {
                val res = analysisState.data

                // Summary Card
                item {
                    BuildingSummaryHighlightCard(res = res)
                }

                // Category 1: Drainage Visual Indicators
                item {
                    CategoryAssessmentHighlightCard(
                        icon = Icons.Filled.WaterDrop,
                        assessment = res.drainage,
                        onFindPro = {
                            viewModel.routeBuildingIssueToPros(
                                specialtyKey = res.drainage.recommendedTradeKey,
                                summary = "Drainage Photo Findings ($city): ${res.drainage.detailedAnalysis}"
                            )
                            onNavigateToContractors()
                        }
                    )
                }

                // Category 2: Roofing Visual Indicators
                item {
                    CategoryAssessmentHighlightCard(
                        icon = Icons.Filled.Roofing,
                        assessment = res.roofing,
                        onFindPro = {
                            viewModel.routeBuildingIssueToPros(
                                specialtyKey = res.roofing.recommendedTradeKey,
                                summary = "Roofing Photo Findings ($city): ${res.roofing.detailedAnalysis}"
                            )
                            onNavigateToContractors()
                        }
                    )
                }

                // Category 3: Foundation Visual Indicators
                item {
                    CategoryAssessmentHighlightCard(
                        icon = Icons.Filled.Foundation,
                        assessment = res.foundation,
                        onFindPro = {
                            viewModel.routeBuildingIssueToPros(
                                specialtyKey = res.foundation.recommendedTradeKey,
                                summary = "Foundation Photo Findings ($city): ${res.foundation.detailedAnalysis}"
                            )
                            onNavigateToContractors()
                        }
                    )
                }

                // Category 4: Portion-Specific & Building-Wide Risks (Interior, Plumbing, Stucco, Commercial, etc.)
                if (res.additionalPortionFindings.detailedAnalysis.isNotBlank() ||
                    res.additionalPortionFindings.visualIndicatorsFound.isNotEmpty()
                ) {
                    item {
                        CategoryAssessmentHighlightCard(
                            icon = Icons.Filled.Build,
                            assessment = res.additionalPortionFindings,
                            onFindPro = {
                                viewModel.routeBuildingIssueToPros(
                                    specialtyKey = res.additionalPortionFindings.recommendedTradeKey,
                                    summary = "${res.inspectedPortion} Photo Findings ($city): ${res.additionalPortionFindings.detailedAnalysis}"
                                )
                                onNavigateToContractors()
                            }
                        )
                    }
                }

                // Real-Life Problems That Can Occur + AI Suggestions + Routing Actions
                item {
                    RealLifeRisksAndSuggestionsCard(
                        analysis = res,
                        city = city,
                        onFindTopPro = {
                            viewModel.routeBuildingIssueToPros(
                                specialtyKey = res.topRecommendedSpecialtyKey,
                                summary = "${res.inspectedPortion} AI Inspection ($city): ${res.summary}"
                            )
                            onNavigateToContractors()
                        },
                        onOpenDiyVideos = onOpenDiyVideos,
                        onRequestHygienist = onRequestHygienist,
                        onCreateVideoSummary = {
                            onOpenVideoStudioWithText(
                                "${res.buildingType} (${res.inspectedPortion}) in $city: ${res.overallCondition}. ${res.summary}"
                            )
                        }
                    )
                }
            }
            null -> Unit
        }
    }
}

@Composable
private fun riskPalette(riskLevel: String): Pair<Color, Color> {
    return when (riskLevel.lowercase()) {
        "urgent", "critical" -> FloodCrimsonContainer to FloodCrimson
        "elevated", "moderate" -> FloodAmberContainer to FloodAmber
        "low" -> SafeEmeraldContainer to SafeEmerald
        else -> AquiferTealLight to AquiferTeal
    }
}

@Composable
private fun BuildingSummaryHighlightCard(res: BuildingPhotoAiAnalysis) {
    val (badgeBg, badgeFg) = riskPalette(res.overallRiskBadge)
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
                        text = "${res.buildingType} · ${res.inspectedPortion}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = res.overallCondition,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(color = badgeBg, shape = RoundedCornerShape(50)) {
                    Text(
                        text = res.overallRiskBadge.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeFg,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Text(res.summary, style = MaterialTheme.typography.bodyMedium)

            if (res.visualHighlightsSummary.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Visual Cues Spotted in Photo",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(res.visualHighlightsSummary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Estimated Market Cost Range", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = "Top Match: ${WaterTableCatalogs.specialtyLabel(res.topRecommendedSpecialtyKey)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = res.estimatedRepairRangeUsd,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryAssessmentHighlightCard(
    icon: ImageVector,
    assessment: BuildingProblemCategoryAssessment,
    onFindPro: () -> Unit
) {
    val (badgeBg, badgeFg) = riskPalette(assessment.riskLevel)
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, badgeFg.copy(alpha = 0.28f))
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeFg,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = assessment.category,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(color = badgeBg, shape = RoundedCornerShape(50)) {
                    Text(
                        text = assessment.riskLevel,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeFg,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (assessment.visualIndicatorsFound.isNotEmpty()) {
                Text(
                    text = "Visual Indicators Highlighted:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                assessment.visualIndicatorsFound.forEach { indicator ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = badgeFg,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(15.dp)
                        )
                        Text(indicator, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (assessment.detailedAnalysis.isNotBlank()) {
                Text(assessment.detailedAnalysis, style = MaterialTheme.typography.bodySmall)
            }

            if (assessment.realLifeProblemsThatCanOccur.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "Real-Life Problems That Can Occur:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        assessment.realLifeProblemsThatCanOccur.forEach { risk ->
                            Text("• $risk", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            if (assessment.recommendedActions.isNotEmpty()) {
                Text("AI Fix & Maintenance Suggestions:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                assessment.recommendedActions.forEach { act ->
                    Text("→ $act", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (!assessment.riskLevel.equals("None Observed", ignoreCase = true)) {
                OutlinedButton(
                    onClick = onFindPro,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Filled.Engineering, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Match ${WaterTableCatalogs.specialtyLabel(assessment.recommendedTradeKey)}")
                }
            }
        }
    }
}

@Composable
private fun RealLifeRisksAndSuggestionsCard(
    analysis: BuildingPhotoAiAnalysis,
    city: String,
    onFindTopPro: () -> Unit,
    onOpenDiyVideos: () -> Unit,
    onRequestHygienist: () -> Unit,
    onCreateVideoSummary: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (analysis.realLifeRisksIfIgnored.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = FloodAmber)
                    Text(
                        text = "What Can Happen in Real Life If Ignored",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                analysis.realLifeRisksIfIgnored.forEach { risk ->
                    Text("• $risk", style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider()
            }

            if (analysis.actionableSuggestions.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = SafeEmerald)
                    Text(
                        text = "AI Suggestions & Smart Next Steps",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                analysis.actionableSuggestions.forEachIndexed { index, suggestion ->
                    Text("${index + 1}. $suggestion", style = MaterialTheme.typography.bodySmall)
                }
            }

            Button(
                onClick = onFindTopPro,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Filled.Engineering, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Find Licensed ${WaterTableCatalogs.specialtyLabel(analysis.topRecommendedSpecialtyKey)} in $city")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenDiyVideos,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Filled.VideoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("DIY Videos", maxLines = 1)
                }
                FilledTonalButton(
                    onClick = onRequestHygienist,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Filled.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hygienist", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onCreateVideoSummary,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Make Video", maxLines = 1)
                }
            }
        }
    }
}
