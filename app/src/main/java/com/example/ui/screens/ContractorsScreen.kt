package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AiMatchConciergeVerdict
import com.example.data.model.ConsultationRequest
import com.example.data.model.ContractorMatchResult
import com.example.data.model.ContractorPortfolioItem
import com.example.data.model.ContractorProfile
import com.example.data.model.ContractorReputation
import com.example.data.model.ContractorReview
import com.example.data.model.ExternalContractorGlassWallGroup
import com.example.data.model.ProblemMatchQuery
import com.example.data.model.WaterReport
import com.example.data.repository.WaterTableRepository
import com.example.ui.MediaAttachment
import com.example.ui.UiState
import com.example.ui.WaterTableViewModel
import com.example.ui.components.Base64MediaPreview
import com.example.ui.components.BlacklistBadge
import com.example.ui.components.LossCategoryBadge
import com.example.ui.components.RatingStarsRow
import com.example.ui.components.VerdictBadge
import com.example.ui.theme.FloodAmber
import com.example.ui.theme.FloodCrimson
import com.example.ui.theme.SafeEmerald
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ContractorsScreen(
    viewModel: WaterTableViewModel,
    matchedContractors: List<ContractorMatchResult>,
    sortedContractors: List<ContractorProfile>,
    contractorsUi: UiState<List<ContractorProfile>>,
    reputations: Map<String, ContractorReputation>,
    reviewsUi: UiState<List<ContractorReview>>,
    externalGroups: List<ExternalContractorGlassWallGroup>,
    portfolioUi: UiState<List<ContractorPortfolioItem>>,
    reportsUi: UiState<List<WaterReport>>,
    clientConsultationsUi: UiState<List<ConsultationRequest>>,
    contractorInboxUi: UiState<List<ConsultationRequest>>,
    problemMatchQuery: ProblemMatchQuery,
    aiMatchConciergeState: UiState<AiMatchConciergeVerdict>?,
    reviewProofAttachment: MediaAttachment?,
    portfolioAttachment: MediaAttachment?,
    defaultCity: String,
    preselectedReport: WaterReport?,
    onClearPreselectedReport: () -> Unit,
    isSubmitting: Boolean
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    var specialtyFilter by remember { mutableStateOf("all") }
    var consultTargetMatch by remember { mutableStateOf<ContractorMatchResult?>(null) }
    var reviewTargetConsultation by remember { mutableStateOf<ConsultationRequest?>(null) }
    var portfolioViewerContractor by remember { mutableStateOf<ContractorProfile?>(null) }
    var showGlassWallReviewDialog by remember { mutableStateOf(false) }
    var reviewTargetProForDialog by remember { mutableStateOf<ContractorProfile?>(null) }

    val filteredMatches = remember(matchedContractors, specialtyFilter) {
        if (specialtyFilter == "all") matchedContractors
        else matchedContractors.filter { it.contractor.specialty == specialtyFilter }
    }
    val allReports = (reportsUi as? UiState.Success)?.data.orEmpty()
    val myReports = allReports.filter { it.userId == viewModel.currentUserId }.ifEmpty { allReports }
    val allPortfolio = (portfolioUi as? UiState.Success)?.data.orEmpty()
    val allReviews = (reviewsUi as? UiState.Success)?.data.orEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedSubTab) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("Best Match") },
                modifier = Modifier.testTag("contractors_tab_find")
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("Glass Wall") },
                modifier = Modifier.testTag("contractors_tab_glass_wall")
            )
            Tab(
                selected = selectedSubTab == 2,
                onClick = { selectedSubTab = 2 },
                text = { Text("Requests") },
                modifier = Modifier.testTag("contractors_tab_requests")
            )
            Tab(
                selected = selectedSubTab == 3,
                onClick = { selectedSubTab = 3 },
                text = { Text("Pro Hub") },
                modifier = Modifier.testTag("contractors_tab_hub")
            )
        }

        when (selectedSubTab) {
            // 0. Perfect Problem-Matched Non-Blacklisted Pros
            0 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Interactive Problem-to-Perfect-Contractor Matchmaker Card
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Perfect Contractor Matchmaker",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tell us what problem you have—or auto-fill from any report—and we score every non-blacklisted pro by exact license specialty, portfolio track record, construction stage, Glass Wall honesty, and distance.",
                                    style = MaterialTheme.typography.bodySmall
                                )

                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(
                                        "auto" to "Auto-Detect",
                                        "groundwater" to "Water Table",
                                        "water_restoration" to "Water Loss",
                                        "mold_mitigation" to "Mold",
                                        "smoke_fire" to "Smoke & Fire",
                                        "drainage_french_drain" to "French Drain",
                                        "plumbing_leak" to "Pipe Leak"
                                    ).forEach { (key, label) ->
                                        FilterChip(
                                            selected = problemMatchQuery.problemCategory == key,
                                            onClick = { viewModel.updateProblemMatchQuery(problemCategory = key) },
                                            label = { Text(label) }
                                        )
                                    }
                                }

                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(
                                        "none" to "Standard / Repair",
                                        "in_process" to "Pre-Finish Construction",
                                        "after_remodel" to "After Full Remodel"
                                    ).forEach { (stage, label) ->
                                        FilterChip(
                                            selected = problemMatchQuery.constructionStage == stage,
                                            onClick = { viewModel.updateProblemMatchQuery(constructionStage = stage) },
                                            label = { Text(label) }
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = problemMatchQuery.problemText,
                                    onValueChange = { viewModel.updateProblemMatchQuery(problemText = it) },
                                    label = { Text("Describe your exact problem (e.g., basement seepage, mold drywall, smoke odor)") },
                                    minLines = 2,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("matchmaker_problem_input")
                                )

                                if (allReports.isNotEmpty()) {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        myReports.take(2).forEach { r ->
                                            OutlinedButton(
                                                onClick = { viewModel.applyProblemMatchFromReport(r) }
                                            ) {
                                                Text("Match Report: ${r.city} (${r.depthFeet}ft)", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }

                                Button(
                                    onClick = { viewModel.runAiBestMatchConcierge() },
                                    enabled = matchedContractors.any { it.reputation?.isBlacklisted != true } &&
                                        aiMatchConciergeState !is UiState.Loading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("find_perfect_match_button")
                                ) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ask AI Concierge Who Is Best")
                                }

                                when (aiMatchConciergeState) {
                                    UiState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    is UiState.Error -> Text(aiMatchConciergeState.message, color = FloodCrimson, style = MaterialTheme.typography.bodySmall)
                                    is UiState.Success -> {
                                        val v = aiMatchConciergeState.data
                                        Surface(
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = "AI Concierge Pick: ${v.topRecommendedContractorName}",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = SafeEmerald,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(v.reasoning, style = MaterialTheme.typography.bodySmall)
                                                if (v.checklistToShareWithPro.isNotEmpty()) {
                                                    Text("Attach When Requesting:", style = MaterialTheme.typography.labelLarge)
                                                    v.checklistToShareWithPro.forEach { tip ->
                                                        Text("• $tip", style = MaterialTheme.typography.bodySmall)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    null -> Unit
                                }
                            }
                        }
                    }

                    if (preselectedReport != null) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Attaching report: ${preselectedReport.city} · ${preselectedReport.depthFeet} ft",
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = onClearPreselectedReport) { Text("Clear") }
                                }
                            }
                        }
                    }

                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                "all" to "All Ranked",
                                "water_mitigation" to "Water Mitigation",
                                "mold_remediation" to "Mold Remediation",
                                "smoke_fire_restoration" to "Smoke & Fire",
                                "full_service_restoration" to "Full Restoration",
                                "soil_engineer" to "Soil Engineers",
                                "general_engineer" to "General Engineers",
                                "drainage_contractor" to "Drainage"
                            ).forEach { (key, label) ->
                                FilterChip(
                                    selected = specialtyFilter == key,
                                    onClick = { specialtyFilter = key },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    when (contractorsUi) {
                        UiState.Loading -> item { CircularProgressIndicator() }
                        is UiState.Error -> item { Text(contractorsUi.message, color = MaterialTheme.colorScheme.error) }
                        is UiState.Success -> {
                            if (filteredMatches.isEmpty()) {
                                item {
                                    Card(shape = RoundedCornerShape(18.dp)) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text("No Matching Pros Registered Yet", style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                text = "Licensed Water/Mold/Fire Mitigation Companies & Soil Engineers can join in 'Pro Hub' to receive local dispatches.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Button(onClick = { selectedSubTab = 3 }) {
                                                Text("Join as Licensed Pro")
                                            }
                                        }
                                    }
                                }
                            } else {
                                items(filteredMatches, key = { it.contractor.id }) { match ->
                                    val pro = match.contractor
                                    val rep = match.reputation
                                    val platformFee = ((pro.consultationFeeUsd * WaterTableRepository.PLATFORM_FEE_PERCENT) / 100.0).roundToInt().coerceAtLeast(1)
                                    val totalFee = pro.consultationFeeUsd + platformFee
                                    val portfolioCount = allPortfolio.count { it.userId == pro.userId }

                                    Card(
                                        shape = RoundedCornerShape(18.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (match.isPerfectMatch) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surface
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = if (match.isPerfectMatch) 4.dp else 2.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (rep?.isBlacklisted == true) {
                                                BlacklistBadge(rep.blacklistReason)
                                            } else if (match.isPerfectMatch) {
                                                Surface(color = Color(0xFFD1FAE5), shape = RoundedCornerShape(50)) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SafeEmerald, modifier = Modifier.size(15.dp))
                                                        Text(
                                                            text = "PERFECT MATCH · ${match.matchScorePercent}% Fit",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = SafeEmerald,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            } else {
                                                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(50)) {
                                                    Text(
                                                        text = "${match.matchScorePercent}% Problem Match",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(pro.displayName, style = MaterialTheme.typography.titleMedium)
                                                    Text(
                                                        text = "${specialtyDisplay(pro.specialty)}${if (pro.companyName.isNotBlank()) " · ${pro.companyName}" else ""}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                Surface(
                                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Text(
                                                        text = "$$totalFee advice",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                    )
                                                }
                                            }

                                            // Why This Contractor Fits the Client's Problem
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                    match.matchReasons.forEach { reason ->
                                                        Text(
                                                            text = "• $reason",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (rep?.isBlacklisted == true) FloodCrimson else MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                                                Text(
                                                    text = "${pro.primaryCity} (~${rep?.distanceMiles ?: 0.0} mi)",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                                if (rep != null && rep.reviewCount > 0) {
                                                    RatingStarsRow("Overall", rep.avgOverallRating)
                                                    RatingStarsRow("Comm", rep.avgCommunicationRating)
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.Filled.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                                Text(
                                                    text = "Lic #${pro.licenseNumber} (${pro.licensingState}) · ${pro.helpfulContributionsCount} helped · ${if (pro.offersOnSiteWork) "On-Site Crew Available" else "Remote Only"}",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }

                                            if (pro.bio.isNotBlank()) {
                                                Text(pro.bio, style = MaterialTheme.typography.bodySmall)
                                            }

                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(
                                                    onClick = { portfolioViewerContractor = pro },
                                                    modifier = Modifier.weight(1f).height(44.dp)
                                                ) {
                                                    Icon(Icons.Filled.WorkHistory, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Portfolio ($portfolioCount)", maxLines = 1)
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        reviewTargetProForDialog = pro
                                                        showGlassWallReviewDialog = true
                                                    },
                                                    modifier = Modifier.weight(1f).height(44.dp)
                                                ) {
                                                    Text("Review Honest")
                                                }
                                            }

                                            Button(
                                                onClick = { consultTargetMatch = match },
                                                enabled = myReports.isNotEmpty() && rep?.isBlacklisted != true,
                                                colors = if (rep?.isBlacklisted == true) {
                                                    ButtonDefaults.buttonColors(containerColor = FloodCrimson)
                                                } else ButtonDefaults.buttonColors(),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(48.dp)
                                                    .testTag("request_pro_button_${pro.userId}")
                                            ) {
                                                Icon(Icons.Filled.RateReview, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    when {
                                                        rep?.isBlacklisted == true -> "Hiring Blocked (Blacklisted)"
                                                        myReports.isEmpty() -> "Log a Report First to Request"
                                                        match.isPerfectMatch -> "Request Perfect Match ($$totalFee)"
                                                        pro.offersOnSiteWork -> "Request Advice / Work ($$totalFee)"
                                                        else -> "Request Remote Review ($$totalFee)"
                                                    }
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

            // 1. Glass Wall Honest Reviews + Proof + Community Blacklist
            1 -> {
                GlassWallSection(
                    allReviews = allReviews,
                    externalGroups = externalGroups,
                    contractors = sortedContractors,
                    reputations = reputations,
                    onOpenPostReview = {
                        reviewTargetProForDialog = null
                        showGlassWallReviewDialog = true
                    }
                )
            }

            // 2. Client's Submitted Requests
            2 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val itemsList = (clientConsultationsUi as? UiState.Success)?.data.orEmpty()
                    if (itemsList.isEmpty()) {
                        item {
                            Card(shape = RoundedCornerShape(18.dp)) {
                                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("No Requests Yet", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "Pick the nearest non-blacklisted pro to request remote engineering triage or full water/mold/smoke/fire restoration work.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(itemsList, key = { it.id }) { req ->
                            ConsultationCard(
                                req = req,
                                isContractorView = false,
                                onSubmitReview = {},
                                onRate = { rating -> viewModel.completeAndRateConsultation(req.id, rating) }
                            )
                        }
                    }
                }
            }

            // 3. Licensed Pro Hub (Register/Update, Incoming Jobs, Portfolio & Referrals Upload)
            3 -> {
                ContractorHubSection(
                    viewModel = viewModel,
                    contractors = sortedContractors,
                    inboxUi = contractorInboxUi,
                    allPortfolio = allPortfolio,
                    portfolioAttachment = portfolioAttachment,
                    defaultCity = defaultCity,
                    isSubmitting = isSubmitting,
                    onOpenReviewDialog = { reviewTargetConsultation = it }
                )
            }
        }
    }

    consultTargetMatch?.let { match ->
        val pro = match.contractor
        RequestConsultationDialog(
            contractor = pro,
            reports = myReports,
            initialReport = preselectedReport ?: myReports.firstOrNull(),
            initialQuestion = problemMatchQuery.problemText.ifBlank {
                "Please review my attached photos & GPS log, confirm restoration scope/steps, and quote work if needed."
            },
            initialServiceMode = match.recommendedServiceMode,
            onDismiss = { consultTargetMatch = null },
            onSubmit = { report, question, serviceMode ->
                viewModel.requestContractorConsultation(pro, report, question, serviceMode)
                consultTargetMatch = null
                onClearPreselectedReport()
                selectedSubTab = 2
            }
        )
    }

    reviewTargetConsultation?.let { req ->
        SubmitEngineerReviewDialog(
            consultation = req,
            onDismiss = { reviewTargetConsultation = null },
            onSubmit = { verdict, recommendation, workEstimate ->
                viewModel.submitContractorReview(req, verdict, recommendation, workEstimate)
                reviewTargetConsultation = null
            }
        )
    }

    if (showGlassWallReviewDialog) {
        PostGlassWallReviewDialog(
            preselectedPro = reviewTargetProForDialog,
            contractors = sortedContractors,
            defaultCity = defaultCity,
            proofAttachment = reviewProofAttachment,
            isSubmitting = isSubmitting,
            onPickProof = { uri, ctx -> viewModel.attachReviewProofFromUri(ctx, uri) },
            onClearProof = { viewModel.clearReviewProof() },
            onDismiss = {
                showGlassWallReviewDialog = false
                reviewTargetProForDialog = null
            },
            onSubmit = { cUid, cName, compRef, city, workCat, overall, comm, exp, proofDesc, recommend, shoddy ->
                viewModel.submitGlassWallReview(
                    contractorUserId = cUid,
                    contractorName = cName,
                    companyOrLicenseRef = compRef,
                    city = city,
                    workCategory = workCat,
                    overallRating = overall,
                    communicationRating = comm,
                    honestExperience = exp,
                    proofDescription = proofDesc,
                    wouldRecommend = recommend,
                    flagShoddyWork = shoddy
                ) {
                    showGlassWallReviewDialog = false
                    reviewTargetProForDialog = null
                    selectedSubTab = 1
                }
            }
        )
    }

    portfolioViewerContractor?.let { pro ->
        val items = allPortfolio.filter { it.userId == pro.userId }
        val proReviews = allReviews.filter { it.contractorUserId == pro.userId }
        AlertDialog(
            onDismissRequest = { portfolioViewerContractor = null },
            title = { Text("${pro.displayName} · Portfolio & Glass Wall") },
            text = {
                LazyColumn(
                    modifier = Modifier.height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (items.isEmpty()) {
                        item {
                            Text("No portfolio projects or client referrals uploaded yet.", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        items(items, key = { it.id }) { item ->
                            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "${if (item.entryType == "referral") "Client Referral" else "Work Showcase"}: ${item.title}",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    if (item.photoBase64.isNotBlank()) {
                                        Base64MediaPreview(item.photoBase64, "photo", "", Modifier.fillMaxWidth().height(120.dp))
                                    }
                                    Text(item.description, style = MaterialTheme.typography.bodySmall)
                                    if (item.clientReferenceName.isNotBlank()) {
                                        Text("Ref: ${item.clientReferenceName} · ${item.city} (${item.completionYear})", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                    if (proReviews.isNotEmpty()) {
                        item {
                            Text("Honest Glass Wall Feedback (${proReviews.size})", style = MaterialTheme.typography.titleSmall)
                        }
                        items(proReviews, key = { it.id }) { r ->
                            Text(
                                text = "• ${r.overallRating}★ Overall / ${r.communicationRating}★ Comm — ${r.honestExperience}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { portfolioViewerContractor = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun GlassWallSection(
    allReviews: List<ContractorReview>,
    externalGroups: List<ExternalContractorGlassWallGroup>,
    contractors: List<ContractorProfile>,
    reputations: Map<String, ContractorReputation>,
    onOpenPostReview: () -> Unit
) {
    val blacklistedPros = contractors.filter { reputations[it.userId]?.isBlacklisted == true }
    val blacklistedExternals = externalGroups.filter { it.isBlacklisted }

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
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Glass Wall Contractor Transparency", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "See who contractors really are before hiring. Clients post honest good or bad reviews with photo proof—and contractors CANNOT delete negative reviews.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = onOpenPostReview,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("post_glass_wall_review_button")
                    ) {
                        Text("Log Honest Contractor Experience + Proof")
                    }
                }
            }
        }

        if (blacklistedPros.isNotEmpty() || blacklistedExternals.isNotEmpty()) {
            item {
                Text("Community Blacklist Alerts", style = MaterialTheme.typography.titleMedium, color = FloodCrimson)
            }
            items(blacklistedPros, key = { "bl_${it.userId}" }) { pro ->
                val reason = reputations[pro.userId]?.blacklistReason.orEmpty()
                BlacklistBadge("${pro.displayName} (${pro.companyName}): $reason")
            }
            items(blacklistedExternals, key = { "ext_bl_${it.contractorName}" }) { ext ->
                BlacklistBadge("${ext.contractorName} (${ext.city}): ${ext.shoddyWorkFlags} shoddy-work reports (${ext.avgOverallRating}★ avg)")
            }
        }

        item {
            Text("All Glass Wall Client Experiences (${allReviews.size})", style = MaterialTheme.typography.titleMedium)
        }

        if (allReviews.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Text(
                        text = "No contractor reviews yet. Share a verified past experience (good or bad) with photo proof to protect neighbors.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(allReviews, key = { it.id }) { rev ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (rev.flagShoddyWork) {
                            BlacklistBadge("FLAGGED SHODDY WORK / POOR CONDUCT")
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rev.contractorName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${rev.city} · ${rev.companyOrLicenseRef.ifBlank { "Past Contractor" }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            LossCategoryBadge(rev.workCategory)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            RatingStarsRow("Overall", rev.overallRating.toDouble())
                            RatingStarsRow("Communication", rev.communicationRating.toDouble())
                        }
                        Text(rev.honestExperience, style = MaterialTheme.typography.bodyMedium)
                        if (rev.proofPhotoBase64.isNotBlank()) {
                            Base64MediaPreview(
                                base64 = rev.proofPhotoBase64,
                                mediaType = "photo",
                                videoDurationLabel = "",
                                modifier = Modifier.fillMaxWidth().height(150.dp)
                            )
                        }
                        if (rev.proofDescription.isNotBlank()) {
                            Text(
                                text = "Client Proof: ${rev.proofDescription}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Posted by ${rev.reviewerName} · ${if (rev.wouldRecommend) "Would hire again" else "Do NOT recommend"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (rev.wouldRecommend) SafeEmerald else FloodCrimson
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsultationCard(
    req: ConsultationRequest,
    isContractorView: Boolean,
    onSubmitReview: () -> Unit,
    onRate: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isContractorView) "Client: ${req.clientName} (${req.city})" else "Pro: ${req.contractorName}",
                    style = MaterialTheme.typography.titleMedium
                )
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
                    Text(
                        text = "${req.serviceMode.replace('_', ' ').uppercase()} · ${req.status.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Text("Scope / Question: ${req.question}", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Advice Fee: Pro $${req.contractorFeeUsd} + Platform $${req.platformFeeUsd} = $${req.totalFeeUsd} total${if (req.proposedWorkEstimateUsd > 0) " · Restoration Estimate: $${req.proposedWorkEstimateUsd}" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (req.engineerRecommendation.isNotBlank()) {
                VerdictBadge(req.engineerVerdict, engineerVerified = true)
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Pro Recommendation:\n${req.engineerRecommendation}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (isContractorView && req.status == "requested") {
                Button(onClick = onSubmitReview, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("Submit Plan / Work Estimate & Earn $${req.contractorFeeUsd}")
                }
            } else if (!isContractorView && req.status == "reviewed") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Rate & Complete:", style = MaterialTheme.typography.labelMedium)
                    (3..5).forEach { stars ->
                        FilledTonalButton(onClick = { onRate(stars) }) { Text("$stars★") }
                    }
                }
            } else if (req.clientRating > 0) {
                Text(
                    text = "Completed · Client Rating: ${req.clientRating}/5 ★",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContractorHubSection(
    viewModel: WaterTableViewModel,
    contractors: List<ContractorProfile>,
    inboxUi: UiState<List<ConsultationRequest>>,
    allPortfolio: List<ContractorPortfolioItem>,
    portfolioAttachment: MediaAttachment?,
    defaultCity: String,
    isSubmitting: Boolean,
    onOpenReviewDialog: (ConsultationRequest) -> Unit
) {
    val context = LocalContext.current
    val existingProfile = contractors.find { it.userId == viewModel.currentUserId }
    val myPortfolio = allPortfolio.filter { it.userId == viewModel.currentUserId }

    var displayName by remember(existingProfile) { mutableStateOf(existingProfile?.displayName ?: "") }
    var companyName by remember(existingProfile) { mutableStateOf(existingProfile?.companyName ?: "") }
    var specialty by remember(existingProfile) { mutableStateOf(existingProfile?.specialty ?: "water_mitigation") }
    var licenseNumber by remember(existingProfile) { mutableStateOf(existingProfile?.licenseNumber ?: "") }
    var licensingState by remember(existingProfile) { mutableStateOf(existingProfile?.licensingState ?: "CA") }
    var primaryCity by remember(existingProfile, defaultCity) { mutableStateOf(existingProfile?.primaryCity ?: defaultCity) }
    var serviceCities by remember(existingProfile) { mutableStateOf(existingProfile?.serviceCities?.joinToString(", ") ?: defaultCity) }
    var feeText by remember(existingProfile) { mutableStateOf((existingProfile?.consultationFeeUsd ?: 45).toString()) }
    var offersOnSiteWork by remember(existingProfile) { mutableStateOf(existingProfile?.offersOnSiteWork ?: true) }
    var bio by remember(existingProfile) { mutableStateOf(existingProfile?.bio ?: "") }

    // Portfolio & Referral Form
    var entryType by remember { mutableStateOf("portfolio") }
    var portTitle by remember { mutableStateOf("") }
    var portCategory by remember { mutableStateOf("water_restoration") }
    var portCity by remember(defaultCity) { mutableStateOf(defaultCity) }
    var portClientRef by remember { mutableStateOf("") }
    var portYear by remember { mutableStateOf("2026") }
    var portDesc by remember { mutableStateOf("") }

    val portfolioPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.attachPortfolioPhotoFromUri(context, uri)
    }

    val inbox = (inboxUi as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (inbox.isNotEmpty()) {
            item {
                Text("Incoming Dispatches & Leads (${inbox.size})", style = MaterialTheme.typography.titleLarge)
            }
            items(inbox, key = { it.id }) { req ->
                ConsultationCard(
                    req = req,
                    isContractorView = true,
                    onSubmitReview = { onOpenReviewDialog(req) },
                    onRate = {}
                )
            }
        }

        // Contractor Profile Form
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Engineering, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = if (existingProfile == null) "Join as Licensed Mitigation Co. / Engineer" else "Update Your Licensed Pro Profile",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = "Water/Mold/Smoke/Fire Mitigation Pros & Soil Engineers earn remote review fees AND receive local restoration dispatches. (${WaterTableRepository.PLATFORM_FEE_PERCENT}% platform maintenance fee).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Lead Name & Credentials (e.g., Jane Doe, WRT/PE)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("pro_name_input")
                    )
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("Mitigation Company / Engineering Firm") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Specialty", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "water_mitigation" to "Water Mitigation",
                            "mold_remediation" to "Mold Remediation",
                            "smoke_fire_restoration" to "Smoke & Fire",
                            "full_service_restoration" to "Full Restoration",
                            "soil_engineer" to "Soil Engineer",
                            "general_engineer" to "General Engineer",
                            "drainage_contractor" to "Drainage Pro",
                            "leak_specialist" to "Leak Specialist"
                        ).forEach { (key, label) ->
                            FilterChip(selected = specialty == key, onClick = { specialty = key }, label = { Text(label) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = licenseNumber,
                            onValueChange = { licenseNumber = it },
                            label = { Text("License / IICRC #") },
                            singleLine = true,
                            modifier = Modifier.weight(1.4f).testTag("pro_license_input")
                        )
                        OutlinedTextField(
                            value = licensingState,
                            onValueChange = { licensingState = it },
                            label = { Text("State") },
                            singleLine = true,
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = primaryCity,
                            onValueChange = { primaryCity = it },
                            label = { Text("Dispatch City") },
                            singleLine = true,
                            modifier = Modifier.weight(1.3f)
                        )
                        OutlinedTextField(
                            value = feeText,
                            onValueChange = { feeText = it.filter { c -> c.isDigit() } },
                            label = { Text("Advice Fee ($)") },
                            singleLine = true,
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                    OutlinedTextField(
                        value = serviceCities,
                        onValueChange = { serviceCities = it },
                        label = { Text("Service Cities (comma separated)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Accept On-Site Restoration Work Leads", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = offersOnSiteWork, onCheckedChange = { offersOnSiteWork = it })
                    }
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Equipment, certifications & mitigation background") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            viewModel.registerContractor(
                                displayName = displayName,
                                companyName = companyName,
                                specialty = specialty,
                                licenseNumber = licenseNumber,
                                licensingState = licensingState,
                                primaryCity = primaryCity,
                                serviceCitiesRaw = serviceCities,
                                feeUsd = feeText.toIntOrNull()?.coerceIn(5, 500) ?: 45,
                                offersOnSiteWork = offersOnSiteWork,
                                bio = bio
                            )
                        },
                        enabled = !isSubmitting && displayName.length >= 2 && licenseNumber.length >= 3,
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("register_contractor_button")
                    ) {
                        Text(if (existingProfile == null) "Publish Licensed Profile" else "Save Profile Changes")
                    }
                }
            }
        }

        // Contractor Portfolio & Past-Client Referrals
        if (existingProfile != null) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Portfolio & Past-Client Referrals Showcase", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = entryType == "portfolio", onClick = { entryType = "portfolio" }, label = { Text("Past Work Photo") })
                            FilterChip(selected = entryType == "referral", onClick = { entryType = "referral" }, label = { Text("Client Referral") })
                        }
                        OutlinedTextField(value = portTitle, onValueChange = { portTitle = it }, label = { Text("Project / Referral Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = portClientRef, onValueChange = { portClientRef = it }, label = { Text("Past Client Ref (Initials/Name)") }, singleLine = true, modifier = Modifier.weight(1.3f))
                            OutlinedTextField(value = portYear, onValueChange = { portYear = it }, label = { Text("Year") }, singleLine = true, modifier = Modifier.weight(0.7f))
                        }
                        OutlinedTextField(value = portDesc, onValueChange = { portDesc = it }, label = { Text("Real work completed / client quote") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                        portfolioAttachment?.previewBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Portfolio work preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(12.dp))
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { portfolioPhotoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Photo")
                            }
                            Button(
                                onClick = {
                                    viewModel.uploadPortfolioOrReferral(
                                        contractorName = existingProfile.displayName,
                                        entryType = entryType,
                                        title = portTitle,
                                        lossCategory = portCategory,
                                        city = portCity,
                                        clientReferenceName = portClientRef,
                                        description = portDesc,
                                        completionYear = portYear
                                    ) {
                                        portTitle = ""
                                        portDesc = ""
                                    }
                                },
                                enabled = !isSubmitting && portTitle.length >= 2 && portDesc.length >= 5,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Upload")
                            }
                        }

                        if (myPortfolio.isNotEmpty()) {
                            HorizontalDivider()
                            Text("My Uploaded Items (${myPortfolio.size})", style = MaterialTheme.typography.labelLarge)
                            myPortfolio.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${item.entryType.uppercase()}: ${item.title}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    TextButton(onClick = { viewModel.deletePortfolioItem(item.id) }) { Text("Remove", color = FloodCrimson) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PostGlassWallReviewDialog(
    preselectedPro: ContractorProfile?,
    contractors: List<ContractorProfile>,
    defaultCity: String,
    proofAttachment: MediaAttachment?,
    isSubmitting: Boolean,
    onPickProof: (android.net.Uri, android.content.Context) -> Unit,
    onClearProof: () -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (
        contractorUserId: String,
        contractorName: String,
        companyOrLicenseRef: String,
        city: String,
        workCategory: String,
        overallRating: Int,
        communicationRating: Int,
        honestExperience: String,
        proofDescription: String,
        wouldRecommend: Boolean,
        flagShoddyWork: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    var isExternalPastContractor by remember(preselectedPro) { mutableStateOf(preselectedPro == null) }
    var contractorName by remember(preselectedPro) { mutableStateOf(preselectedPro?.displayName ?: "") }
    var companyRef by remember(preselectedPro) { mutableStateOf(preselectedPro?.companyName ?: "") }
    var city by remember(defaultCity) { mutableStateOf(defaultCity) }
    var workCategory by remember { mutableStateOf("water_restoration") }
    var overallRating by remember { mutableIntStateOf(5) }
    var communicationRating by remember { mutableIntStateOf(5) }
    var honestExperience by remember { mutableStateOf("") }
    var proofDescription by remember { mutableStateOf("") }
    var wouldRecommend by remember { mutableStateOf(true) }
    var flagShoddyWork by remember { mutableStateOf(false) }

    val proofLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) onPickProof(uri, context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Glass Wall Honest Review + Proof") },
        text = {
            LazyColumn(
                modifier = Modifier.height(410.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !isExternalPastContractor && preselectedPro != null,
                            onClick = {
                                if (preselectedPro != null) {
                                    isExternalPastContractor = false
                                    contractorName = preselectedPro.displayName
                                    companyRef = preselectedPro.companyName
                                }
                            },
                            enabled = preselectedPro != null,
                            label = { Text("Registered Pro") }
                        )
                        FilterChip(
                            selected = isExternalPastContractor,
                            onClick = { isExternalPastContractor = true },
                            label = { Text("Past / External Pro") }
                        )
                    }
                    OutlinedTextField(
                        value = contractorName,
                        onValueChange = { contractorName = it },
                        label = { Text("Contractor / Company Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = companyRef,
                        onValueChange = { companyRef = it },
                        label = { Text("License / Company Ref (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Overall Rating ($overallRating★)", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..5).forEach { s ->
                            FilledTonalButton(onClick = { overallRating = s }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) {
                                Text(if (overallRating == s) "$s★" else "$s")
                            }
                        }
                    }
                    Text("Communication ($communicationRating★)", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..5).forEach { s ->
                            FilledTonalButton(onClick = { communicationRating = s }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) {
                                Text(if (communicationRating == s) "$s★" else "$s")
                            }
                        }
                    }
                    OutlinedTextField(
                        value = honestExperience,
                        onValueChange = { honestExperience = it },
                        label = { Text("Honest experience (workmanship, communication, pricing)") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Flag Shoddy Work / Blacklist", style = MaterialTheme.typography.bodySmall, color = FloodCrimson)
                        Switch(checked = flagShoddyWork, onCheckedChange = {
                            flagShoddyWork = it
                            if (it) wouldRecommend = false
                        })
                    }
                    OutlinedTextField(
                        value = proofDescription,
                        onValueChange = { proofDescription = it },
                        label = { Text("Proof caption (invoice, unfinished drywall, text screenshot)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    proofAttachment?.previewBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Proof photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(10.dp))
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { proofLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (proofAttachment == null) "Attach Proof" else "Change Proof")
                        }
                        if (proofAttachment != null) {
                            TextButton(onClick = onClearProof) { Text("Clear") }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val targetUid = if (!isExternalPastContractor && preselectedPro != null) preselectedPro.userId else "external"
                    onSubmit(
                        targetUid, contractorName, companyRef, city, workCategory,
                        overallRating, communicationRating, honestExperience, proofDescription,
                        wouldRecommend, flagShoddyWork
                    )
                },
                enabled = !isSubmitting && contractorName.length >= 2 && honestExperience.length >= 15
            ) {
                Text("Post Honest Review")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestConsultationDialog(
    contractor: ContractorProfile,
    reports: List<WaterReport>,
    initialReport: WaterReport?,
    initialQuestion: String,
    initialServiceMode: String,
    onDismiss: () -> Unit,
    onSubmit: (WaterReport, String, String) -> Unit
) {
    var selectedReport by remember { mutableStateOf(initialReport ?: reports.first()) }
    var expanded by remember { mutableStateOf(false) }
    var serviceMode by remember {
        mutableStateOf(
            if (contractor.offersOnSiteWork) initialServiceMode else "remote_advice"
        )
    }
    var question by remember { mutableStateOf(initialQuestion) }
    val platformFee = ((contractor.consultationFeeUsd * WaterTableRepository.PLATFORM_FEE_PERCENT) / 100.0).roundToInt().coerceAtLeast(1)
    val totalFee = contractor.consultationFeeUsd + platformFee

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connect with ${contractor.displayName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = serviceMode == "remote_advice",
                        onClick = { serviceMode = "remote_advice" },
                        label = { Text("Remote Advice") }
                    )
                    if (contractor.offersOnSiteWork) {
                        FilterChip(
                            selected = serviceMode == "restoration_work",
                            onClick = { serviceMode = "restoration_work" },
                            label = { Text("Restoration Work") }
                        )
                    }
                }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = "${selectedReport.city} · ${selectedReport.depthFeet} ft (${selectedReport.lossCategory})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Attach Documented Report") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        reports.forEach { rep ->
                            DropdownMenuItem(
                                text = { Text("${rep.city} · ${rep.depthFeet} ft · ${rep.lossCategory}") },
                                onClick = {
                                    selectedReport = rep
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Loss details & request") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Advice / Dispatch Fee: $${contractor.consultationFeeUsd} + 10% Platform Fee ($$platformFee) = $$totalFee.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedReport, question, serviceMode) },
                enabled = question.trim().length >= 5
            ) {
                Text("Send ($$totalFee)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubmitEngineerReviewDialog(
    consultation: ConsultationRequest,
    onDismiss: () -> Unit,
    onSubmit: (verdict: String, recommendation: String, workEstimateUsd: Int) -> Unit
) {
    var verdict by remember { mutableStateOf("ground_water_table") }
    var recommendation by remember { mutableStateOf("") }
    var workEstimateText by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Submit Official Input & Estimate") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "ground_water_table" to "Water Table",
                        "water_mitigation_approved" to "Water Mitigation",
                        "mold_remediation_approved" to "Mold Remediation",
                        "smoke_fire_approved" to "Smoke/Fire Loss",
                        "plumbing_leak" to "Pipe Leak",
                        "needs_site_boring" to "Needs Boring"
                    ).forEach { (key, label) ->
                        FilterChip(selected = verdict == key, onClick = { verdict = key }, label = { Text(label) })
                    }
                }
                OutlinedTextField(
                    value = recommendation,
                    onValueChange = { recommendation = it },
                    label = { Text("Scope of work, dry-out plan & recommendations") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = workEstimateText,
                    onValueChange = { workEstimateText = it.filter { c -> c.isDigit() } },
                    label = { Text("On-Site Work Estimate USD (optional, 0 if advice only)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(verdict, recommendation, workEstimateText.toIntOrNull() ?: 0) },
                enabled = recommendation.isNotBlank()
            ) { Text("Submit Plan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun specialtyDisplay(code: String): String = when (code) {
    "water_mitigation" -> "Licensed Water Mitigation Co."
    "mold_remediation" -> "Certified Mold Remediation"
    "smoke_fire_restoration" -> "Smoke & Fire Restoration"
    "full_service_restoration" -> "Full-Service Water/Mold/Fire Restoration"
    "soil_engineer" -> "Licensed Soil Engineer"
    "general_engineer" -> "General Civil Engineer"
    "drainage_contractor" -> "Drainage & Waterproofing Contractor"
    "leak_specialist" -> "Leak Detection Specialist"
    else -> "Licensed Professional"
}
