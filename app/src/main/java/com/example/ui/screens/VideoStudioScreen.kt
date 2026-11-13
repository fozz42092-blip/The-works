package com.example.ui.screens

import android.content.Intent
import android.media.MediaPlayer
import android.widget.VideoView
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.example.data.model.GeneratedVideoClip
import com.example.ui.MediaAttachment
import com.example.ui.UiState
import com.example.ui.WaterTableViewModel
import com.example.ui.theme.HydroNavyDark
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoStudioScreen(
    viewModel: WaterTableViewModel,
    initialPromptText: String,
    initialSourceType: String,
    videoReferencePhoto: MediaAttachment?,
    videoStudioState: UiState<GeneratedVideoClip>?,
    videoStudioProgress: String,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var sourceType by remember(initialSourceType) {
        mutableStateOf(initialSourceType.ifBlank { "product_ad" })
    }
    var promptText by remember(initialPromptText) {
        mutableStateOf(initialPromptText)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.attachVideoReferencePhotoFromUri(context, uri)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("video_studio_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column {
                Text(
                    text = "AI Video Studio & Photo Animator",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Text-to-Video & Image-to-Video MP4 Generator",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Explanation Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Movie, contentDescription = null, tint = Color.White)
                            Text(
                                text = "Bring Scripts & Product Photos to Life",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Paste a blog post, contractor script, or wholesale sump/French drain description — or attach a product/site photo — and generate a 9-second animated H.264 MP4 video clip using Gemini 3.8 Flash storyboarding + Nano Banana 2.1 motion keyframes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.92f)
                        )
                    }
                }
            }

            // Input & Reference Photo Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("1. Content Format", style = MaterialTheme.typography.titleSmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "product_ad" to "Product Video Ad",
                                "blog_post" to "Blog Post Reel",
                                "script" to "Video Script",
                                "site_report" to "Property Walkthrough"
                            ).forEach { (key, label) ->
                                FilterChip(
                                    selected = sourceType == key,
                                    onClick = { sourceType = key },
                                    label = { Text(label) }
                                )
                            }
                        }

                        // Quick templates
                        Text("Quick Prompts", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    sourceType = "product_ad"
                                    promptText = "Dual-battery cast-iron 1/2 HP sump pump kit with vertical float switch & silent check valve — 32% cheaper than big-box retail, built for shallow basement water tables."
                                }
                            ) {
                                Text("Sump Pump Ad", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = {
                                    sourceType = "blog_post"
                                    promptText = "Why shallow 4ft water tables flood basement slabs during construction — and how installing perimeter French drains before backfill saves homeowners $18,000 in post-remodel repairs."
                                }
                            ) {
                                Text("Water Table Blog", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        OutlinedTextField(
                            value = promptText,
                            onValueChange = { promptText = it },
                            label = { Text("Blog post, script, or product description") },
                            minLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("video_studio_prompt_input")
                        )

                        Text("2. Optional Photo to Animate (Image-to-Video)", style = MaterialTheme.typography.titleSmall)
                        if (videoReferencePhoto?.previewBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            ) {
                                Image(
                                    bitmap = videoReferencePhoto.previewBitmap.asImageBitmap(),
                                    contentDescription = "Reference photo to animate",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = { viewModel.clearVideoReferencePhoto() },
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove photo", tint = Color.White)
                                }
                            }
                        } else {
                            FilledTonalButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("attach_video_photo_button")
                            ) {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload Product or Site Photo to Animate")
                            }
                        }

                        Button(
                            onClick = { viewModel.generateAiVideoClip(context, sourceType, promptText) },
                            enabled = promptText.trim().length >= 8 && videoStudioState !is UiState.Loading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("generate_ai_video_button")
                        ) {
                            if (videoStudioState is UiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Rendering Video Clip...")
                            } else {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generate Short MP4 Video")
                            }
                        }
                    }
                }
            }

            // Progress / Output / Error Area
            when (val state = videoStudioState) {
                UiState.Loading -> {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = videoStudioProgress.ifBlank { "Generating storyboard & motion frames..." },
                                    style = MaterialTheme.typography.titleSmall
                                )
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }

                is UiState.Error -> {
                    item {
                        Surface(
                            color = Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Video Generation Unavailable / Failed",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFFB91C1C),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF7F1D1D)
                                )
                                Text(
                                    text = "Note: Nano Banana 2.1 keyframe synthesis requires Firebase Blaze billing & App Check provisioning.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF991B1B)
                                )
                            }
                        }
                    }
                }

                is UiState.Success -> {
                    item {
                        GeneratedVideoPlayerCard(clip = state.data)
                    }
                }

                null -> Unit
            }
        }
    }
}

@Composable
private fun GeneratedVideoPlayerCard(clip: GeneratedVideoClip) {
    val context = LocalContext.current
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(true) }

    Card(
        shape = RoundedCornerShape(18.dp),
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
                    Text(clip.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${clip.durationSeconds}s H.264 MP4 · ${clip.scenes.size} AI Scenes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                FilledTonalButton(
                    onClick = {
                        val file = File(clip.mp4FilePath)
                        if (file.exists()) {
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "video/mp4"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, clip.title)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share AI Video Clip"))
                        }
                    },
                    modifier = Modifier.testTag("share_generated_video_button")
                ) {
                    Icon(Icons.Filled.Share, contentDescription = "Share MP4 video", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share MP4")
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(HydroNavyDark)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoPath(clip.mp4FilePath)
                            setOnPreparedListener { mp: MediaPlayer ->
                                mp.isLooping = true
                                start()
                                isPlaying = true
                            }
                            setOnCompletionListener {
                                isPlaying = false
                            }
                            videoViewRef = this
                        }
                    },
                    update = { view ->
                        if (view.tag != clip.mp4FilePath) {
                            view.tag = clip.mp4FilePath
                            view.setVideoPath(clip.mp4FilePath)
                            view.start()
                            isPlaying = true
                        }
                    }
                )
            }

            DisposableEffect(clip.mp4FilePath) {
                onDispose {
                    videoViewRef?.stopPlayback()
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val vv = videoViewRef ?: return@OutlinedButton
                        if (vv.isPlaying) {
                            vv.pause()
                            isPlaying = false
                        } else {
                            vv.start()
                            isPlaying = true
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isPlaying) "Pause" else "Play")
                }
                OutlinedButton(
                    onClick = {
                        videoViewRef?.let { vv ->
                            vv.seekTo(0)
                            vv.start()
                            isPlaying = true
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Replay")
                }
            }

            if (clip.scriptSummary.isNotBlank()) {
                Text(clip.scriptSummary, style = MaterialTheme.typography.bodySmall)
            }

            Text("Storyboard Scenes", style = MaterialTheme.typography.titleSmall)
            clip.scenes.forEach { scene ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Scene ${scene.sceneNumber}: ${scene.headline} (${scene.cameraMotion})",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(scene.voiceoverCaption, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
