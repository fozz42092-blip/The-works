package com.example.ui.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.model.AdaptiveSolutionStat
import com.example.data.model.BuildingPhotoAiAnalysis
import com.example.data.model.GroundwaterAiDiagnosis
import com.example.data.model.HomeAzAiInspection
import com.example.data.model.PestControlAiAnalysis
import com.example.data.validation.SubmissionValidator
import com.example.ui.MediaAttachment
import com.example.ui.UiState
import com.example.ui.WaterTableViewModel
import com.example.ui.WeatherNewsUiState
import com.example.ui.components.VerdictBadge
import com.example.ui.theme.FloodAmber
import com.example.ui.theme.FloodCrimson
import com.example.ui.theme.SafeEmerald
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiagnoseReportScreen(
    viewModel: WaterTableViewModel,
    weatherState: WeatherNewsUiState,
    mediaAttachment: MediaAttachment?,
    extraPhotos: List<MediaAttachment>,
    buildingPhotoAttachment: MediaAttachment? = null,
    buildingExtraPhotos: List<MediaAttachment> = emptyList(),
    pestPhotoAttachment: MediaAttachment? = null,
    pestExtraPhotos: List<MediaAttachment> = emptyList(),
    aiDiagnosisState: UiState<GroundwaterAiDiagnosis>?,
    azHomeInspectionState: UiState<HomeAzAiInspection>? = null,
    buildingPhotoAnalysisState: UiState<BuildingPhotoAiAnalysis>? = null,
    pestAnalysisState: UiState<PestControlAiAnalysis>? = null,
    selectedSoilFromBottleTest: String? = null,
    adaptiveSolutions: List<AdaptiveSolutionStat>,
    validationErrors: List<String>,
    isSubmitting: Boolean,
    onNavigateToContractors: () -> Unit,
    onOpenVideoStudioWithText: (String) -> Unit,
    onOpenDiyVideos: () -> Unit = {},
    onOpenRentals: () -> Unit = {},
    onPublished: () -> Unit
) {
    val context = LocalContext.current

    var diagnoseModeTab by remember { mutableIntStateOf(0) } // 0 = Any House/Building Photo AI, 1 = Hydro/Loss Map Pin, 2 = Calming A-Z Video / Pre-Buy AI

    var city by remember(weatherState.selectedCity) { mutableStateOf(weatherState.selectedCity) }
    var stateRegion by remember(weatherState.selectedState) { mutableStateOf(weatherState.selectedState) }
    var addressHint by remember(weatherState.addressHint) { mutableStateOf(weatherState.addressHint) }
    var depthText by remember { mutableStateOf("4.5") }
    var soilType by remember { mutableStateOf(selectedSoilFromBottleTest ?: "clay") }
    var constructionStage by remember { mutableStateOf("in_process") }
    var lossCategory by remember { mutableStateOf("groundwater") }
    var situationType by remember { mutableStateOf("groundwater_seepage") }
    var authorRole by remember { mutableStateOf("resident") }
    var lossDescription by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    LaunchedEffect(selectedSoilFromBottleTest) {
        if (!selectedSoilFromBottleTest.isNullOrBlank()) {
            soilType = selectedSoilFromBottleTest
        }
    }

    // Calming A-Z House / Exterior Video / Future Home AI states
    var azMode by remember { mutableStateOf("az_photo") } // "az_photo" | "exterior_video" | "future_home"
    var azProblemArea by remember { mutableStateOf("Roof & Attic Leak") }
    var azDescription by remember { mutableStateOf("") }
    var showHygienistDialog by remember { mutableStateOf(false) }

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.attachMediaFromUri(context, uri)
    }

    val extraPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.addExtraPhotoFromUri(context, uri)
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) viewModel.attachMediaFromUri(context, uri)
    }

    val soilOptions = listOf("clay", "sand", "silt", "loam", "gravel", "bedrock", "unknown")
    val lossCategoryOptions = listOf(
        "groundwater" to "Groundwater",
        "water_restoration" to "Water Loss",
        "mold_mitigation" to "Mold",
        "smoke_damage" to "Smoke",
        "fire_restoration" to "Fire"
    )
    val constructionOptions = listOf(
        "none" to "Standard / Pre-Dig",
        "in_process" to "Construction In Process",
        "after_remodel" to "After Full Remodel"
    )
    val situationOptions = listOf(
        "groundwater_seepage" to "Groundwater Seepage",
        "street_flooding" to "Street Flooding",
        "basement_intrusion" to "Basement / Slab",
        "subsurface_test" to "Test Bore / Dig",
        "drainage_backup" to "Drain Backup",
        "water_loss" to "Pipe / Interior Flood",
        "mold_growth" to "Mold Colonization",
        "smoke_soot" to "Smoke & Soot",
        "fire_char" to "Fire / Suppression"
    )
    val azProblemAreas = listOf(
        "Roof & Attic Leak",
        "Under-Sink Drain & Trap",
        "Exterior Copper Pipe Corrosion",
        "Slab Cracks & Foundation",
        "Yard Grading & Exterior Runoff",
        "Mold / Musty Drywall",
        "Future House Exterior & Interior",
        "Electrical / HVAC Moisture"
    )

    val gpsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val ok = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) viewModel.refreshWithLiveGps()
    }

    val depthValue = depthText.toDoubleOrNull() ?: 4.5
    val previewValidation = remember(
        city, stateRegion, addressHint, weatherState.latitude, weatherState.longitude,
        depthValue, lossCategory, situationType, lossDescription, notes, mediaAttachment, extraPhotos.size
    ) {
        SubmissionValidator.validateWaterOrLossReport(
            city = city,
            stateOrRegion = stateRegion,
            addressHint = addressHint,
            latitude = weatherState.latitude,
            longitude = weatherState.longitude,
            depthFeet = depthValue,
            lossCategory = lossCategory,
            situationType = situationType,
            lossDescription = lossDescription,
            notes = notes,
            hasPrimaryMedia = mediaAttachment != null,
            extraPhotoCount = extraPhotos.size
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = diagnoseModeTab) {
            Tab(
                selected = diagnoseModeTab == 0,
                onClick = { diagnoseModeTab = 0 },
                text = { Text("Photo AI") },
                modifier = Modifier.testTag("diagnose_tab_photo_ai")
            )
            Tab(
                selected = diagnoseModeTab == 1,
                onClick = { diagnoseModeTab = 1 },
                text = { Text("Hydro Log") },
                modifier = Modifier.testTag("diagnose_tab_hydro")
            )
            Tab(
                selected = diagnoseModeTab == 2,
                onClick = { diagnoseModeTab = 2 },
                text = { Text("A–Z & Video", maxLines = 1) },
                modifier = Modifier.testTag("diagnose_tab_az_home")
            )
            Tab(
                selected = diagnoseModeTab == 3,
                onClick = { diagnoseModeTab = 3 },
                text = { Text("Pest AI", maxLines = 1) },
                modifier = Modifier.testTag("diagnose_tab_pest_ai")
            )
        }

        if (diagnoseModeTab == 0) {
            BuildingPhotoInspectorTab(
                viewModel = viewModel,
                city = city,
                primaryPhoto = buildingPhotoAttachment,
                extraPhotos = buildingExtraPhotos,
                analysisState = buildingPhotoAnalysisState,
                onNavigateToContractors = onNavigateToContractors,
                onOpenDiyVideos = onOpenDiyVideos,
                onRequestHygienist = { showHygienistDialog = true },
                onOpenVideoStudioWithText = onOpenVideoStudioWithText
            )
        } else if (diagnoseModeTab == 3) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    PestControlAiContentColumn(
                        viewModel = viewModel,
                        city = city,
                        primaryPhoto = pestPhotoAttachment,
                        extraPhotos = pestExtraPhotos,
                        analysisState = pestAnalysisState,
                        onNavigateToContractors = onNavigateToContractors
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Shared Evidence Attachment Card on Hydro & A-Z tabs
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
                                text = if (diagnoseModeTab == 1) {
                                    "Attach Site Photos / Video (Up to 4 Images)"
                                } else {
                                    "Attach House Photo, Exterior Walkaround Video, or Future Home Pics"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )

                        if (mediaAttachment?.previewBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.Black)
                            ) {
                                Image(
                                    bitmap = mediaAttachment.previewBitmap.asImageBitmap(),
                                    contentDescription = "Primary site evidence photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (mediaAttachment.mediaType == "video") {
                                    Surface(
                                        color = Color(0xCC06253A),
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = "Multi-frame AI scan ready (${mediaAttachment.videoDurationLabel})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.clearMediaAttachment() },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .background(Color(0xAA000000), RoundedCornerShape(50))
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove media", tint = Color.White)
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
                                                contentDescription = "Extra site photo ${idx + 1}",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            IconButton(
                                                onClick = { viewModel.removeExtraPhotoAt(idx) },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(24.dp)
                                                    .background(Color(0xAA000000), RoundedCornerShape(50))
                                            ) {
                                                Icon(Icons.Filled.Close, contentDescription = "Remove photo", tint = Color.White, modifier = Modifier.size(14.dp))
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
                            OutlinedButton(
                                onClick = {
                                    mediaPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pick_media_button")
                            ) {
                                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Photo/Video", maxLines = 1)
                            }

                            FilledTonalButton(
                                onClick = {
                                    val dir = File(context.cacheDir, "captures").apply { mkdirs() }
                                    val file = File(dir, "site_${System.currentTimeMillis()}.jpg")
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                    pendingCameraUri = uri
                                    takePictureLauncher.launch(uri)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("capture_photo_button")
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Camera", maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = {
                                    extraPhotoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                enabled = extraPhotos.size < 3,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+Photo (${extraPhotos.size}/3)", maxLines = 1)
                            }
                        }
                    }
                }
            }

            if (diagnoseModeTab == 1) {
                // --- MODE 1: HYDRO / WATER TABLE / LOSS MAP LOG ---
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("1. Incident Category & Construction Stage", style = MaterialTheme.typography.titleMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                lossCategoryOptions.forEach { (key, label) ->
                                    FilterChip(
                                        selected = lossCategory == key,
                                        onClick = {
                                            lossCategory = key
                                            situationType = when (key) {
                                                "water_restoration" -> "water_loss"
                                                "mold_mitigation" -> "mold_growth"
                                                "smoke_damage" -> "smoke_soot"
                                                "fire_restoration" -> "fire_char"
                                                else -> "groundwater_seepage"
                                            }
                                        },
                                        label = { Text(label) }
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.Construction, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Text("Construction / Remodel Stage", style = MaterialTheme.typography.labelLarge)
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                constructionOptions.forEach { (key, label) ->
                                    FilterChip(
                                        selected = constructionStage == key,
                                        onClick = { constructionStage = key },
                                        label = { Text(label) }
                                    )
                                }
                            }
                            Text(
                                text = when (constructionStage) {
                                    "in_process" -> "AI will tell you what waterproofing & drainage steps MUST happen BEFORE contractors finish the property."
                                    "after_remodel" -> "AI will recommend targeted, minimally invasive repairs to fix moisture after a completed remodel."
                                    else -> "Standard groundwater dig test, landscaper trench reading, or neighborhood observation."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

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
                                Text("2. GPS, Dig Depth & Field Log", style = MaterialTheme.typography.titleMedium)
                                FilledTonalButton(
                                    onClick = {
                                        gpsPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                                Manifest.permission.ACCESS_FINE_LOCATION
                                            )
                                        )
                                    }
                                ) {
                                    Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Stamp GPS")
                                }
                            }

                            Text(
                                text = String.format(
                                    java.util.Locale.US,
                                    "GPS: %.4f°, %.4f° (%s)",
                                    weatherState.latitude,
                                    weatherState.longitude,
                                    if (weatherState.isLiveGps) "Live GPS Lock" else "City Center"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = city,
                                    onValueChange = { city = it },
                                    label = { Text("City / Town") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.4f)
                                )
                                OutlinedTextField(
                                    value = stateRegion,
                                    onValueChange = { stateRegion = it },
                                    label = { Text("State") },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.7f)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = addressHint,
                                    onValueChange = { addressHint = it },
                                    label = { Text("Street / Cross-Street") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.4f)
                                )
                                OutlinedTextField(
                                    value = depthText,
                                    onValueChange = { depthText = it.filter { c -> c.isDigit() || c == '.' } },
                                    label = { Text("Dig Depth (ft)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.8f)
                                )
                            }

                            if (depthValue in 0.1..5.0) {
                                Surface(
                                    color = FloodCrimson.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Shallow Water Table Alert (${depthValue} ft): Elevated hydrostatic uplift risk for footings, slabs, & trenches.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = FloodCrimson,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            Text("Soil Type (auto-synced if Soil Bottle Test run)", style = MaterialTheme.typography.labelLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                soilOptions.forEach { soil ->
                                    FilterChip(
                                        selected = soilType == soil,
                                        onClick = { soilType = soil },
                                        label = { Text(soil.replaceFirstChar { it.uppercase() }) }
                                    )
                                }
                            }

                            Text("Situation", style = MaterialTheme.typography.labelLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                situationOptions.forEach { (key, label) ->
                                    FilterChip(
                                        selected = situationType == key,
                                        onClick = { situationType = key },
                                        label = { Text(label) }
                                    )
                                }
                            }

                            Text("Posting As (Neighbors, Landscapers & Pros)", style = MaterialTheme.typography.labelLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    "resident" to "Resident",
                                    "contractor" to "Contractor / Landscaper",
                                    "engineer" to "Engineer"
                                ).forEach { (role, label) ->
                                    FilterChip(
                                        selected = authorRole == role,
                                        onClick = { authorRole = role },
                                        label = { Text(label) }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = lossDescription,
                                onValueChange = {
                                    lossDescription = it
                                    viewModel.clearValidationErrors()
                                },
                                label = { Text("Full loss, trench/dig depth or landscaper water-table observation") },
                                minLines = 3,
                                modifier = Modifier.fillMaxWidth().testTag("loss_description_input")
                            )

                            OutlinedTextField(
                                value = notes,
                                onValueChange = {
                                    notes = it
                                    viewModel.clearValidationErrors()
                                },
                                label = { Text("Field notes (rises after rain, finish status, impacted areas)") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth().testTag("report_notes_input")
                            )

                            Surface(
                                color = if (previewValidation.requiresModeration) FloodAmber.copy(alpha = 0.14f) else MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Plausibility Score: ${previewValidation.plausibilityScore}/100 · ${if (previewValidation.requiresModeration) "Moderation Queue" else "Instant Atlas"}",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                        Text(
                                            text = previewValidation.moderationReason,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }

                            if (validationErrors.isNotEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        validationErrors.forEach { err ->
                                            Text("• $err", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.runAiGroundwaterDiagnosis(
                                        city = city,
                                        soilType = soilType,
                                        constructionStage = constructionStage,
                                        lossCategory = lossCategory,
                                        situationType = situationType,
                                        depthFeet = depthValue,
                                        lossDescription = lossDescription,
                                        notes = notes.ifBlank { "Site observation near $addressHint" }
                                    )
                                },
                                enabled = aiDiagnosisState !is UiState.Loading,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("run_ai_triage_button")
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Run AI Triage & Construction Plan")
                            }
                        }
                    }
                }

                when (aiDiagnosisState) {
                    UiState.Loading -> {
                        item {
                            Card(shape = RoundedCornerShape(18.dp)) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Analyzing photos, dig depth, & construction stage...", style = MaterialTheme.typography.titleSmall)
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                }
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
                                    text = aiDiagnosisState.message,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }
                    is UiState.Success -> {
                        val diag = aiDiagnosisState.data
                        item {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
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
                                        VerdictBadge(diag.verdict)
                                        Text(
                                            text = "${diag.confidencePercent}% Confidence",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text(diag.summary, style = MaterialTheme.typography.titleMedium)
                                    Text(diag.engineeringReasoning, style = MaterialTheme.typography.bodySmall)

                                    if (diag.constructionStageAdvice.isNotBlank()) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text = if (constructionStage == "after_remodel") "Post-Remodel Repair Strategy:" else "Before Contractors Finish Property:",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(diag.constructionStageAdvice, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }

                                    if (diag.restorationScope.isNotBlank()) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text("Restoration & Mitigation Scope:", style = MaterialTheme.typography.labelLarge)
                                                Text(diag.restorationScope, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }

                                    if (diag.recommendedSolutions.isNotEmpty()) {
                                        Text("Best Solutions (Adaptive Priority):", style = MaterialTheme.typography.titleSmall)
                                        diag.recommendedSolutions.forEach { sol ->
                                            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                                Text(sol, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (diag.shouldEscalateToEngineer) {
                                            OutlinedButton(
                                                onClick = onNavigateToContractors,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Filled.Engineering, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Nearest Pro")
                                            }
                                        }
                                        FilledTonalButton(
                                            onClick = {
                                                onOpenVideoStudioWithText(
                                                    "${city} ${lossCategory.replace('_', ' ')} at ${depthValue}ft: ${diag.summary}. ${diag.constructionStageAdvice}"
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Filled.VideoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Make Video")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    null -> Unit
                }

                item {
                    Button(
                        onClick = {
                            viewModel.publishWaterReport(
                                city = city,
                                stateOrRegion = stateRegion,
                                addressHint = addressHint,
                                latitude = weatherState.latitude,
                                longitude = weatherState.longitude,
                                depthFeet = depthValue,
                                soilType = soilType,
                                constructionStage = constructionStage,
                                lossCategory = lossCategory,
                                situationType = situationType,
                                lossDescription = lossDescription,
                                notes = notes,
                                authorRole = authorRole,
                                onPublishedSuccess = onPublished
                            )
                        },
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("publish_report_button")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Publish, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Submit GPS Map Stamp & Loss Log", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (adaptiveSolutions.isNotEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                    Text("Algorithm-Learned Best Solutions", style = MaterialTheme.typography.titleMedium)
                                }
                                adaptiveSolutions.take(5).forEach { stat ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(stat.solutionName, style = MaterialTheme.typography.titleSmall)
                                                Text(
                                                    text = "${stat.soilType} soil · ${stat.sampleSize} verified reports",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(50)
                                            ) {
                                                Text(
                                                    text = "${stat.successRatePercent}% worked",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // --- MODE 1: CALMING A-Z HOUSE / EXTERIOR VIDEO / FUTURE HOME AI ---
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.HealthAndSafety, contentDescription = null, tint = SafeEmerald)
                                Text("Calming A–Z Home & Future-Buy AI Advisor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SafeEmerald)
                            }
                            Text(
                                text = "Stressed about a leak, crack, or weird stain? Snap a photo or record a slow exterior video—or upload photos of a house you want to buy. AI gives a calm game plan, Scope of Work, USD price check, life expectancy forecast & top vetted pros.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                item {
                    Card(shape = RoundedCornerShape(18.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Select Inspection Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(
                                    "az_photo" to "A–Z House Photo",
                                    "exterior_video" to "Exterior Runoff Video",
                                    "future_home" to "Future Home Pre-Buy AI"
                                ).forEach { (key, label) ->
                                    FilterChip(
                                        selected = azMode == key,
                                        onClick = { azMode = key },
                                        label = { Text(label) }
                                    )
                                }
                            }

                            Text("Focus Area (Roof to Slab & Pipes)", style = MaterialTheme.typography.labelLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                azProblemAreas.forEach { area ->
                                    FilterChip(
                                        selected = azProblemArea == area,
                                        onClick = { azProblemArea = area },
                                        label = { Text(area) }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = azDescription,
                                onValueChange = { azDescription = it },
                                label = {
                                    Text(
                                        when (azMode) {
                                            "exterior_video" -> "What does your exterior video show? (grading, gutters, puddles, weep screed)"
                                            "future_home" -> "Describe future home age, lot slope & any photos attached"
                                            else -> "Describe the house problem (roof leak, under-sink drain, copper green crust, slab crack)"
                                        }
                                    )
                                },
                                minLines = 3,
                                modifier = Modifier.fillMaxWidth().testTag("az_home_description_input")
                            )

                            Button(
                                onClick = {
                                    viewModel.runAzHomeOrExteriorVideoAi(
                                        context = context,
                                        mode = azMode,
                                        problemArea = azProblemArea,
                                        userDescription = azDescription.ifBlank { "$azProblemArea inspection in $city" }
                                    )
                                },
                                enabled = azHomeInspectionState !is UiState.Loading,
                                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("run_az_home_ai_button")
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Run Calming A–Z AI Inspection & Price Check")
                            }
                        }
                    }
                }

                when (azHomeInspectionState) {
                    UiState.Loading -> {
                        item {
                            Card(shape = RoundedCornerShape(18.dp)) {
                                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Scanning multi-frame video, elevation geology, pipe corrosion & costs...", style = MaterialTheme.typography.titleSmall)
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                    is UiState.Error -> {
                        item {
                            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                                Text(azHomeInspectionState.message, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(14.dp))
                            }
                        }
                    }
                    is UiState.Success -> {
                        val ins = azHomeInspectionState.data
                        item {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // Calming Reassurance Header
                                    Surface(color = Color(0xFFD1FAE5), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("Peace of Mind Plan", style = MaterialTheme.typography.labelLarge, color = SafeEmerald, fontWeight = FontWeight.Bold)
                                            Text(ins.calmingReassurance, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(ins.detectedIssueTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        Surface(
                                            color = if (ins.severityLabel == "Urgent") Color(0xFFFEE2E2) else MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = ins.severityLabel.uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (ins.severityLabel == "Urgent") FloodCrimson else MaterialTheme.colorScheme.onSecondaryContainer,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    // Fair Price Check Pill
                                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column {
                                                Text("Estimated Fair Price Check", style = MaterialTheme.typography.labelMedium)
                                                Text("Best Trade: ${ins.recommendedTrade}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                            }
                                            Text(
                                                text = "$${ins.estimatedPriceLowUsd} – $${ins.estimatedPriceHighUsd}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Text("Scope of Work (To Hand Contractors or Insurer)", style = MaterialTheme.typography.labelLarge)
                                    Text(ins.scopeOfWork, style = MaterialTheme.typography.bodySmall)

                                    HorizontalDivider()

                                    Text("Drainage, Runoff & Erosion Check", style = MaterialTheme.typography.labelLarge)
                                    Text(ins.drainageAndErosionFindings, style = MaterialTheme.typography.bodySmall)

                                    Text("Under-Sink Drain & Exterior Copper Pipe Corrosion Check", style = MaterialTheme.typography.labelLarge)
                                    Text(ins.plumbingAndPipeCorrosionFindings, style = MaterialTheme.typography.bodySmall)

                                    Text("Geology, Elevation & Component Life Expectancy", style = MaterialTheme.typography.labelLarge)
                                    Text("${ins.geologicalAndElevationNotes}\n\nLife Expectancy Forecast: ${ins.lifeExpectancyForecast}", style = MaterialTheme.typography.bodySmall)

                                    if (ins.diyImmediateSteps.isNotEmpty()) {
                                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text("Do Today While Scheduling:", style = MaterialTheme.typography.labelLarge)
                                                ins.diyImmediateSteps.forEach { step ->
                                                    Text("• $step", style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                    }

                                    // 1-Tap Routing Actions
                                    Button(
                                        onClick = {
                                            viewModel.routeAzProblemToPros(
                                                specialtyKey = ins.recommendedSpecialtyKey,
                                                summary = "${ins.detectedIssueTitle}: ${ins.scopeOfWork}"
                                            )
                                            onNavigateToContractors()
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    ) {
                                        Icon(Icons.Filled.Engineering, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Find Vetted ${ins.recommendedTrade} & Structural/Geo Pros")
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (ins.needsHygienistAirOrTapeTest || ins.relatedVideoCategory == "mold") {
                                            OutlinedButton(
                                                onClick = { showHygienistDialog = true },
                                                modifier = Modifier.weight(1f).height(46.dp)
                                            ) {
                                                Icon(Icons.Filled.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Air / Tape Test", maxLines = 1)
                                            }
                                        }
                                        if (ins.needsEquipmentRental) {
                                            FilledTonalButton(
                                                onClick = onOpenRentals,
                                                modifier = Modifier.weight(1f).height(46.dp)
                                            ) {
                                                Text("Rent Gear Nearby", maxLines = 1)
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = onOpenDiyVideos,
                                            modifier = Modifier.weight(1f).height(46.dp)
                                        ) {
                                            Icon(Icons.Filled.VideoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("How-To Videos", maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    null -> Unit
                }

                // Quick Hygienist trigger card even before running AI
                item {
                    OutlinedButton(
                        onClick = { showHygienistDialog = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Filled.Science, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Need Air Samples or Mold Tape Samples? Request Hygienist")
                    }
                }
            }
        }
        }
    }

    if (showHygienistDialog) {
        HygienistSampleRequestDialog(
            defaultCity = city,
            defaultAddress = addressHint,
            isSubmitting = isSubmitting,
            onDismiss = { showHygienistDialog = false },
            onSubmit = { c, addr, types, notesTxt ->
                viewModel.submitHygienistSampleRequest(c, addr, types, notesTxt) {
                    showHygienistDialog = false
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HygienistSampleRequestDialog(
    defaultCity: String,
    defaultAddress: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (city: String, address: String, sampleTypes: List<String>, reasonNotes: String) -> Unit
) {
    var city by remember { mutableStateOf(defaultCity) }
    var address by remember { mutableStateOf(defaultAddress) }
    var airSpore by remember { mutableStateOf(true) }
    var tapeLift by remember { mutableStateOf(true) }
    var asbestosLead by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.HomeWork, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Order Industrial Hygienist Lab Samples")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Request independent certified Industrial Hygienists for Air Spore Traps, Mold Tape Lifts, or Asbestos/Lead testing before or after remediation.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City / Town") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Property Area / Cross-Street") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = airSpore, onClick = { airSpore = !airSpore }, label = { Text("Air Spore Trap") })
                    FilterChip(selected = tapeLift, onClick = { tapeLift = !tapeLift }, label = { Text("Mold Tape Lift") })
                    FilterChip(selected = asbestosLead, onClick = { asbestosLead = !asbestosLead }, label = { Text("Asbestos / Lead") })
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Rooms impacted, visible growth, or insurance claim notes") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            val selectedTypes = buildList {
                if (airSpore) add("Air Spore Trap")
                if (tapeLift) add("Surface Tape Lift")
                if (asbestosLead) add("Asbestos/Lead Bulk")
            }.ifEmpty { listOf("Air Spore Trap") }
            Button(
                onClick = { onSubmit(city, address, selectedTypes, notes.ifBlank { "Independent mold & air clearance sample requested." }) },
                enabled = !isSubmitting && city.isNotBlank()
            ) {
                Text("Send to Hygienists")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
