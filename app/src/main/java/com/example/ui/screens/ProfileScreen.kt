package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ConsultationRequest
import com.example.data.model.ContractorReview
import com.example.data.model.ModerationTicket
import com.example.data.model.SupplyOrder
import com.example.data.model.UserProfile
import com.example.data.model.WaterReport
import com.example.data.validation.AiModerationVerdict
import com.example.ui.UiState
import com.example.ui.WaterTableViewModel
import com.example.ui.components.Base64MediaPreview
import com.example.ui.components.ConstructionStageBadge
import com.example.ui.components.LossCategoryBadge
import com.example.ui.components.ModerationStatusBadge
import com.example.ui.components.RatingStarsRow
import com.example.ui.components.VerdictBadge
import com.example.ui.theme.FloodCrimson
import com.example.ui.theme.SafeEmerald
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    viewModel: WaterTableViewModel,
    userProfile: UserProfile?,
    myReportsUi: UiState<List<WaterReport>>,
    clientConsultationsUi: UiState<List<ConsultationRequest>>,
    reviewsUi: UiState<List<ContractorReview>>,
    supplyOrdersUi: UiState<List<SupplyOrder>>,
    pendingModerationUi: UiState<List<ModerationTicket>>,
    aiModerationVerdicts: Map<String, AiModerationVerdict>,
    moderatingTicketId: String?,
    defaultCity: String,
    isSubmitting: Boolean,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val authUser = Firebase.auth.currentUser
    var selectedSectionTab by remember { mutableIntStateOf(0) } // 0 = My Submissions, 1 = Moderation Queue
    var submissionFilterTab by remember { mutableIntStateOf(0) } // 0=Reports, 1=Consultations, 2=Glass Wall, 3=Orders

    var preferredCity by remember(userProfile?.preferredCity, defaultCity) {
        mutableStateOf(userProfile?.preferredCity?.ifBlank { defaultCity } ?: defaultCity)
    }
    var rolePreference by remember(userProfile?.rolePreference) {
        mutableStateOf(userProfile?.rolePreference ?: "resident")
    }

    val myReports = (myReportsUi as? UiState.Success)?.data.orEmpty()
    val myConsultations = (clientConsultationsUi as? UiState.Success)?.data.orEmpty()
    val myReviews = ((reviewsUi as? UiState.Success)?.data.orEmpty())
        .filter { it.userId == viewModel.currentUserId }
    val myOrders = (supplyOrdersUi as? UiState.Success)?.data.orEmpty()
    val moderationTickets = (pendingModerationUi as? UiState.Success)?.data.orEmpty()

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
                modifier = Modifier.testTag("profile_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to main app")
            }
            Text(
                text = "Account, Submissions & Moderation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        TabRow(selectedTabIndex = selectedSectionTab) {
            Tab(
                selected = selectedSectionTab == 0,
                onClick = { selectedSectionTab = 0 },
                text = { Text("My Profile & History") },
                modifier = Modifier.testTag("tab_profile_history")
            )
            Tab(
                selected = selectedSectionTab == 1,
                onClick = { selectedSectionTab = 1 },
                text = { Text("Moderation (${moderationTickets.size})") },
                modifier = Modifier.testTag("tab_moderation_queue")
            )
        }

        if (selectedSectionTab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Google Auth Account + Role & Preferred City Card
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccountCircle,
                                    contentDescription = "Google account",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Column {
                                    Text(
                                        text = authUser?.displayName?.ifBlank { "WaterTable Member" } ?: "WaterTable Member",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = authUser?.email?.ifBlank { "Signed in with Google" } ?: "Signed in with Google",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                            OutlinedTextField(
                                value = preferredCity,
                                onValueChange = { preferredCity = it },
                                label = { Text("Home / Preferred City") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_city_input")
                            )

                            Text("Account Role", style = MaterialTheme.typography.labelLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    "resident" to "Homeowner / Resident",
                                    "contractor" to "Licensed Contractor",
                                    "engineer" to "Soil / Hydro Engineer"
                                ).forEach { (key, label) ->
                                    FilterChip(
                                        selected = rolePreference == key,
                                        onClick = { rolePreference = key },
                                        label = { Text(label) }
                                    )
                                }
                            }

                            Button(
                                onClick = { viewModel.saveUserProfile(preferredCity, rolePreference) },
                                enabled = !isSubmitting && preferredCity.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("save_profile_button")
                            ) {
                                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Profile Preferences")
                            }
                        }
                    }
                }

                // Activity Summary Counts
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatMiniChip("Reports", myReports.size, Modifier.weight(1f))
                        StatMiniChip("Requests", myConsultations.size, Modifier.weight(1f))
                        StatMiniChip("Glass Wall", myReviews.size, Modifier.weight(1f))
                        StatMiniChip("Orders", myOrders.size, Modifier.weight(1f))
                    }
                }

                // Past Submissions Filter Bar
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            0 to "My Reports (${myReports.size})",
                            1 to "Pro Requests (${myConsultations.size})",
                            2 to "Glass Wall (${myReviews.size})",
                            3 to "Supply Orders (${myOrders.size})"
                        ).forEach { (idx, label) ->
                            FilterChip(
                                selected = submissionFilterTab == idx,
                                onClick = { submissionFilterTab = idx },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                when (submissionFilterTab) {
                    0 -> {
                        if (myReportsUi is UiState.Loading) {
                            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                        } else if (myReports.isEmpty()) {
                            item {
                                Text(
                                    "No reports submitted yet. Use Diagnose to log groundwater depth, flood alerts, or water/mold/fire damage.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(myReports, key = { it.id }) { report ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${report.city} · ${report.addressHint}",
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                                Text(
                                                    text = "Depth: ${report.depthFeet} ft · Plausibility: ${report.plausibilityScore}%",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                ModerationStatusBadge(report.moderationStatus)
                                                IconButton(onClick = { viewModel.deleteMyReport(report.id) }) {
                                                    Icon(
                                                        Icons.Filled.Delete,
                                                        contentDescription = "Delete submission",
                                                        tint = FloodCrimson
                                                    )
                                                }
                                            }
                                        }
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            LossCategoryBadge(report.lossCategory)
                                            ConstructionStageBadge(report.constructionStage)
                                            VerdictBadge(report.waterSourceVerdict, report.engineerVerified)
                                        }
                                        if (report.moderationNotes.isNotBlank()) {
                                            Text(
                                                text = "Validation/Moderation: ${report.moderationNotes}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        Text(
                                            text = report.lossDescription.ifBlank { report.notes },
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        if (report.mediaPreviewBase64.isNotBlank()) {
                                            Base64MediaPreview(
                                                base64 = report.mediaPreviewBase64,
                                                mediaType = report.mediaType,
                                                videoDurationLabel = report.videoDurationLabel,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(140.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        if (myConsultations.isEmpty()) {
                            item {
                                Text(
                                    "No contractor requests sent yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(myConsultations, key = { it.id }) { req ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("${req.contractorName} · ${req.city}", style = MaterialTheme.typography.titleSmall)
                                            Text(req.status.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                        LossCategoryBadge(req.lossCategory)
                                        Text(req.question, style = MaterialTheme.typography.bodySmall)
                                        if (req.engineerRecommendation.isNotBlank()) {
                                            Text(
                                                "Scope/Verdict: ${req.engineerRecommendation}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SafeEmerald
                                            )
                                        }
                                        if (req.proposedWorkEstimateUsd > 0) {
                                            Text(
                                                "Estimated Restoration Work: $${req.proposedWorkEstimateUsd}",
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        if (myReviews.isEmpty()) {
                            item {
                                Text(
                                    "No Glass Wall contractor reviews submitted yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(myReviews, key = { it.id }) { rev ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(rev.contractorName, style = MaterialTheme.typography.titleSmall)
                                            RatingStarsRow("Overall", rev.overallRating.toDouble())
                                        }
                                        Text("${rev.city} · ${rev.companyOrLicenseRef}", style = MaterialTheme.typography.labelSmall)
                                        Text(rev.honestExperience, style = MaterialTheme.typography.bodySmall)
                                        if (rev.flagShoddyWork) {
                                            Text("Flagged Shoddy Work / Blacklist Vote", style = MaterialTheme.typography.labelSmall, color = FloodCrimson)
                                        }
                                        if (rev.proofPhotoBase64.isNotBlank()) {
                                            Base64MediaPreview(
                                                base64 = rev.proofPhotoBase64,
                                                mediaType = "photo",
                                                videoDurationLabel = "",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(120.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        if (myOrders.isEmpty()) {
                            item {
                                Text(
                                    "No wholesale drop-ship supply orders yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(myOrders, key = { it.id }) { order ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Order · ${order.shippingCity}", style = MaterialTheme.typography.titleSmall)
                                            Text(order.status.uppercase(), style = MaterialTheme.typography.labelSmall, color = SafeEmerald)
                                        }
                                        Text(order.jobDescription, style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            text = String.format(
                                                Locale.US,
                                                "Paid $%.2f wholesale (Saved $%.2f vs retail)",
                                                order.wholesaleTotalUsd, order.totalSavingsUsd
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SafeEmerald
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Peer & AI Moderation Queue
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Photo & Detailed Report Moderation Queue", style = MaterialTheme.typography.titleMedium)
                            }
                            Text(
                                "New photo uploads and detailed restoration reports are validated for completeness & plausibility, then held here until Gemini AI screening + a registered licensed contractor/engineer approves them for the public Leaflet Map.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                if (pendingModerationUi is UiState.Loading) {
                    item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                } else if (moderationTickets.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Moderation queue is clear! All submitted photo & detailed reports have been reviewed.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(18.dp)
                            )
                        }
                    }
                } else {
                    items(moderationTickets, key = { it.id }) { ticket ->
                        val aiVerdict = aiModerationVerdicts[ticket.reportId]
                        val isScreening = moderatingTicketId == ticket.reportId
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${ticket.city} · ${ticket.authorName}", style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "Plausibility Score: ${ticket.plausibilityScore}% · ${if (ticket.hasMedia) "Has Photo/Video" else "Text Only"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    LossCategoryBadge(ticket.lossCategory)
                                }

                                Text(ticket.previewSummary, style = MaterialTheme.typography.bodySmall)

                                if (ticket.completenessIssues.isNotEmpty()) {
                                    Surface(color = Color(0xFFFEF3C7), shape = RoundedCornerShape(10.dp)) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text("Plausibility Warnings:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            ticket.completenessIssues.forEach { warn ->
                                                Text("• $warn", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }

                                if (ticket.photoBase64.isNotBlank()) {
                                    Base64MediaPreview(
                                        base64 = ticket.photoBase64,
                                        mediaType = "photo",
                                        videoDurationLabel = "",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                    )
                                }

                                if (aiVerdict != null) {
                                    Surface(
                                        color = if (aiVerdict.isSafeAndRelevant) Color(0xFFD1FAE5) else Color(0xFFFEE2E2),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "Gemini AI Screen (${aiVerdict.plausibilityScore}% plausible): ${if (aiVerdict.isSafeAndRelevant) "PASS" else "CONCERN"}",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (aiVerdict.isSafeAndRelevant) SafeEmerald else FloodCrimson
                                            )
                                            Text(aiVerdict.summary, style = MaterialTheme.typography.bodySmall)
                                            aiVerdict.flaggedReasons.forEach { reason ->
                                                Text("• $reason", style = MaterialTheme.typography.labelSmall, color = FloodCrimson)
                                            }
                                        }
                                    }
                                }

                                FilledTonalButton(
                                    onClick = { viewModel.runAiModerationForTicket(ticket) },
                                    enabled = !isScreening,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isScreening) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Screening with Gemini...")
                                    } else {
                                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Run Gemini AI Plausibility Screen")
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.approveOrFlagModerationTicket(
                                                ticket = ticket,
                                                approved = false,
                                                notes = aiVerdict?.summary ?: "Flagged by contractor moderator for implausible or off-topic content."
                                            )
                                        },
                                        enabled = !isSubmitting,
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FloodCrimson),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                    ) {
                                        Icon(Icons.Filled.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Flag")
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.approveOrFlagModerationTicket(
                                                ticket = ticket,
                                                approved = true,
                                                notes = aiVerdict?.summary ?: "Verified plausible photo/field report by licensed moderator."
                                            )
                                        },
                                        enabled = !isSubmitting,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Approve Public")
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

@Composable
private fun StatMiniChip(label: String, count: Int, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
