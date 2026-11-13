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
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.model.AdaptiveSolutionStat
import com.example.data.model.GroundwaterAiDiagnosis
import com.example.data.validation.SubmissionValidator
import com.example.ui.MediaAttachment
import com.example.ui.UiState
import com.example.ui.WaterTableViewModel
import com.example.ui.WeatherNewsUiState
import com.example.ui.components.VerdictBadge
import com.example.ui.theme.FloodAmber
import com.example.ui.theme.FloodCrimson
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiagnoseReportScreen(
    viewModel: WaterTableViewModel,
    weatherState: WeatherNewsUiState,
    mediaAttachment: MediaAttachment?,
    extraPhotos: List<MediaAttachment>,
    aiDiagnosisState: UiState<GroundwaterAiDiagnosis>?,
    adaptiveSolutions: List<AdaptiveSolutionStat>,
    validationErrors: List<String>,
    isSubmitting: Boolean,
    onNavigateToContractors: () -> Unit,
    onOpenVideoStudioWithText: (String) -> Unit,
    onPublished: () -> Unit
) {
    val context = LocalContext.current

    var city by remember(weatherState.selectedCity) { mutableStateOf(weatherState.selectedCity) }
    var stateRegion by remember(weatherState.selectedState) { mutableStateOf(weatherState.selectedState) }
    var addressHint by remember(weatherState.addressHint) { mutableStateOf(weatherState.addressHint) }
    var depthText by remember { mutableStateOf("4.5") }
    var soilType by remember { mutableStateOf("clay") }
    var constructionStage by remember { mutableStateOf("in_process") }
    var lossCategory by remember { mutableStateOf("groundwater") }
    var situationType by remember { mutableStateOf("groundwater_seepage") }
    var authorRole by remember { mutableStateOf("resident") }
    var lossDescription by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

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

    val gpsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val ok = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) viewModel.refreshWithLiveGps()
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "AI Hydro, Construction & Loss Documentation",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Document dig depth, construction stage (in-process vs. after remodel), and water, mold, smoke, or fire damage with multi-photo GPS stamps.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Loss Type & Construction Stage
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
                            else -> "Standard groundwater dig test or neighborhood observation."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Multi-Photo & Video Evidence Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("2. Attach Site Photos / Video (Up to 4 Images)", style = MaterialTheme.typography.titleMedium)

                    if (mediaAttachment?.previewBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.Black)
                        ) {
                            Image(
                                bitmap = mediaAttachment.previewBitmap.asImageBitmap(),
                                contentDescription = "Primary site evidence photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
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
                                            .size(86.dp)
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
                            Text("Main Media", maxLines = 1)
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

        // GPS, Dig Depth & Detailed Description
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
                        Text("3. GPS Coordinates, Dig Depth & Details", style = MaterialTheme.typography.titleMedium)
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

                    Text("Soil Type", style = MaterialTheme.typography.labelLarge)
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

                    Text("Posting As", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("resident" to "Resident", "contractor" to "Contractor", "engineer" to "Engineer").forEach { (role, label) ->
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
                        label = { Text("Full loss, trench/dig depth & construction status description") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("loss_description_input")
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = {
                            notes = it
                            viewModel.clearValidationErrors()
                        },
                        label = { Text("Field notes (rises after rain, contractor finish status, impacted rooms)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("report_notes_input")
                    )

                    // Plausibility & Moderation Preview Pill
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

        // AI Diagnosis Result
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

        // Publish Button
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

        // Adaptive Solutions Leaderboard
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
    }
}
