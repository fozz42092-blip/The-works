package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ROUTE_ATLAS
import com.example.ROUTE_CONTRACTORS
import com.example.ROUTE_DIAGNOSE
import com.example.ROUTE_DIY_LEAK
import com.example.ROUTE_PROFILE
import com.example.ROUTE_VIDEO_STUDIO
import com.example.ROUTE_WHOLESALE
import com.example.ui.GuideChatMessage
import com.example.ui.theme.SafeEmerald

private data class TutorialStep(
    val id: String,
    val title: String,
    val subtitle: String,
    val targetRoute: String
)

private val TUTORIAL_STEPS = listOf(
    TutorialStep(
        id = "report_map",
        title = "1. Explore Leaflet Depth Map",
        subtitle = "Tap any GPS pin to inspect dig depth (<=5ft shallow warning) & construction advice.",
        targetRoute = ROUTE_ATLAS
    ),
    TutorialStep(
        id = "diagnose",
        title = "2. AI Site Diagnose & Stage Plan",
        subtitle = "Log groundwater, mold, smoke, or fire damage + construction in-process vs post-remodel.",
        targetRoute = ROUTE_DIAGNOSE
    ),
    TutorialStep(
        id = "diy_leak",
        title = "3. Run DIY Leak Check",
        subtitle = "Rule out pressurized plumbing leaks vs rising water table in 15 minutes.",
        targetRoute = ROUTE_DIY_LEAK
    ),
    TutorialStep(
        id = "pros",
        title = "4. Nearest Pros & Glass Wall",
        subtitle = "Find nearby helpful contractors, verify portfolios, or post honest proof reviews.",
        targetRoute = ROUTE_CONTRACTORS
    ),
    TutorialStep(
        id = "supply",
        title = "5. Wholesale French Drain & Sump Store",
        subtitle = "Describe your drainage job to build a bill of materials cheaper than big-box retail.",
        targetRoute = ROUTE_WHOLESALE
    ),
    TutorialStep(
        id = "video_studio",
        title = "6. AI Video & Photo Animator Studio",
        subtitle = "Turn scripts, blog posts, or product photos into shareable MP4 video clips.",
        targetRoute = ROUTE_VIDEO_STUDIO
    ),
    TutorialStep(
        id = "profile_mod",
        title = "7. Profile, Submissions & Moderation",
        subtitle = "Track pending/approved reports or help screen community photos.",
        targetRoute = ROUTE_PROFILE
    )
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiTutorialGuideSheet(
    currentRoute: String,
    completedSteps: Set<String>,
    messages: List<GuideChatMessage>,
    isStreaming: Boolean,
    onAskQuestion: (String) -> Unit,
    onNavigateToStep: (stepId: String, route: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var inputText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(
                            text = "AI Personal Assistant & Tutorial",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${completedSteps.size}/${TUTORIAL_STEPS.size} walkthrough steps completed",
                            style = MaterialTheme.typography.labelSmall,
                            color = SafeEmerald
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close assistant")
                }
            }

            LinearProgressIndicator(
                progress = { completedSteps.size.toFloat() / TUTORIAL_STEPS.size.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )

            // Interactive Onboarding Checklist
            Text("Interactive Walkthrough (Tap to Jump)", style = MaterialTheme.typography.labelLarge)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 185.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(TUTORIAL_STEPS, key = { it.id }) { step ->
                    val done = completedSteps.contains(step.id)
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (done) Color(0xFFECFDF5) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onNavigateToStep(step.id, step.targetRoute)
                                onDismiss()
                            }
                            .testTag("tutorial_step_${step.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (done) SafeEmerald else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(step.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                Text(step.subtitle, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            // Quick AI Tutorial Questions
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "How do I report shallow groundwater?",
                    "How does Glass Wall blacklist work?",
                    "How to make an AI product video?"
                ).forEach { q ->
                    AssistChip(
                        onClick = { onAskQuestion(q) },
                        leadingIcon = {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        label = { Text(q, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Streaming Q&A Chat History
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 210.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            color = if (msg.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(0.88f)
                        ) {
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (msg.isUser) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask AI Assistant how to use any feature...") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("guide_chat_input")
                )
                FilledIconButton(
                    onClick = {
                        val q = inputText.trim()
                        if (q.isNotEmpty()) {
                            inputText = ""
                            onAskQuestion(q)
                        }
                    },
                    enabled = inputText.trim().isNotEmpty() && !isStreaming,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("guide_send_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send question to AI assistant")
                }
            }
        }
    }
}
