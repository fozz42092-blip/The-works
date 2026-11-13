package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.catalog.WaterTableCatalogs
import com.example.data.export.WorkAuthorizationPdfGenerator
import com.example.data.model.AiMatchConciergeVerdict
import com.example.data.model.ConsultationRequest
import com.example.data.model.ContinuousLearningSnapshot
import com.example.data.model.ContractorComplaint
import com.example.data.model.ContractorCopilotAdvice
import com.example.data.model.ContractorMatchResult
import com.example.data.model.ContractorPortfolioItem
import com.example.data.model.ContractorProfile
import com.example.data.model.ContractorReputation
import com.example.data.model.ContractorReview
import com.example.data.model.CslbVerificationResult
import com.example.data.model.ExternalContractorGlassWallGroup
import com.example.data.model.GroundedCreditLink
import com.example.data.model.HygienistSampleRequest
import com.example.data.model.IicrcAuditRecord
import com.example.data.model.InsuranceCarrierProfile
import com.example.data.model.InsuranceClaimThread
import com.example.data.model.InsurancePolicyOffer
import com.example.data.model.OnlineVerifiedContractorLead
import com.example.data.model.ProblemMatchQuery
import com.example.data.model.UserProfile
import com.example.data.model.WaterReport
import com.example.data.repository.WaterTableRepository
import com.example.ui.MediaAttachment
import com.example.ui.UiState
import com.example.ui.WaterTableViewModel
import com.example.ui.components.Base64MediaPreview
import com.example.ui.components.BlacklistBadge
import com.example.ui.components.ContractorTransparentProfileDialog
import com.example.ui.components.FileComplaintDialog
import com.example.ui.components.IicrcGovernanceBoardSection
import com.example.ui.components.InsuranceHubSection
import com.example.ui.components.LossCategoryBadge
import com.example.ui.components.RatingStarsRow
import com.example.ui.components.VerdictBadge
import com.example.ui.components.WorkAuthorizationDialog
import com.example.ui.components.sharePdfFile
import com.example.ui.theme.FloodAmber
import com.example.ui.theme.FloodCrimson
import com.example.ui.theme.SafeEmerald
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ContractorsScreen(
    viewModel: WaterTableViewModel,
    userProfile: UserProfile? = null,
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
    continuousSnapshot: ContinuousLearningSnapshot = ContinuousLearningSnapshot(
        0, 0, 0, 0, "Syncing...", "Syncing...", "Live"
    ),
    iicrcAudits: List<IicrcAuditRecord> = emptyList(),
    complaints: List<ContractorComplaint> = emptyList(),
    insuranceCarriers: List<InsuranceCarrierProfile> = emptyList(),
    mergedClaims: List<InsuranceClaimThread> = emptyList(),
    myPolicyOffers: List<InsurancePolicyOffer> = emptyList(),
    openPolicyQuotes: List<InsurancePolicyOffer> = emptyList(),
    insurerPolicyOffers: List<InsurancePolicyOffer> = emptyList(),
    telemetryDiscountPercent: Int = 0,
    myHygienistRequests: List<HygienistSampleRequest> = emptyList(),
    openHygienistRequests: List<HygienistSampleRequest> = emptyList(),
    assignedHygienistRequests: List<HygienistSampleRequest> = emptyList(),
    onlineContractorLeadsState: UiState<List<OnlineVerifiedContractorLead>>? = null,
    cslbCheckState: UiState<CslbVerificationResult>? = null,
    copilotAdviceState: UiState<ContractorCopilotAdvice>? = null,
    iicrcAiCheckState: Map<String, UiState<Triple<String, String, List<GroundedCreditLink>>>> = emptyMap(),
    liveScanProofAttachment: MediaAttachment? = null,
    complaintProofAttachment: MediaAttachment? = null,
    latestFiledComplaint: ContractorComplaint? = null,
    defaultCity: String,
    preselectedReport: WaterReport?,
    onClearPreselectedReport: () -> Unit,
    onNavigateToDiagnose: () -> Unit = {},
    isSubmitting: Boolean
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    var selectedSubTab by remember { mutableIntStateOf(0) }
    var specialtyFilter by remember { mutableStateOf("all") }
    var onlineTradeKey by remember { mutableStateOf("general_contractor") }
    var onlineSearchNote by remember { mutableStateOf("") }

    var consultTargetMatch by remember { mutableStateOf<ContractorMatchResult?>(null) }
    var reviewTargetConsultation by remember { mutableStateOf<ConsultationRequest?>(null) }
    var dossierTargetMatch by remember { mutableStateOf<ContractorMatchResult?>(null) }
    var workAuthTarget by remember { mutableStateOf<ConsultationRequest?>(null) }
    var showComplaintDialog by remember { mutableStateOf(false) }
    var complaintTargetPro by remember { mutableStateOf<ContractorProfile?>(null) }
    var showGlassWallReviewDialog by remember { mutableStateOf(false) }
    var reviewTargetProForDialog by remember { mutableStateOf<ContractorProfile?>(null) }
    var showHygienistDialog by remember { mutableStateOf(false) }
    var showSuggestRevisionDialog by remember { mutableStateOf(false) }

    val filteredMatches = remember(matchedContractors, specialtyFilter) {
        if (specialtyFilter == "all") matchedContractors
        else matchedContractors.filter { it.contractor.specialty == specialtyFilter }
    }
    val allReports = (reportsUi as? UiState.Success)?.data.orEmpty()
    val myReports = allReports.filter { it.userId == viewModel.currentUserId }.ifEmpty { allReports }
    val allPortfolio = (portfolioUi as? UiState.Success)?.data.orEmpty()
    val allReviews = (reviewsUi as? UiState.Success)?.data.orEmpty()
    val myProProfile = sortedContractors.find { it.userId == viewModel.currentUserId }
    val isHygienistOrIicrc = myProProfile?.specialty in listOf("industrial_hygienist", "iicrc_inspector") ||
        myProProfile?.isIicrcGovernor == true ||
        userProfile?.rolePreference == "hygienist"

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedSubTab,
            edgePadding = 8.dp
        ) {
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
            Tab(
                selected = selectedSubTab == 4,
                onClick = { selectedSubTab = 4 },
                text = { Text("IICRC (${iicrcAudits.size})") },
                modifier = Modifier.testTag("contractors_tab_iicrc")
            )
            Tab(
                selected = selectedSubTab == 5,
                onClick = { selectedSubTab = 5 },
                text = { Text("Insurance") },
                modifier = Modifier.testTag("contractors_tab_insurance")
            )
        }

        when (selectedSubTab) {
            // 0. Perfect Problem-Matched Pros + Continuous Learning + Online A-Z Search
            0 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Continuous Learning Snapshot Card
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Live Continuous Learning Engine",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = continuousSnapshot.updatedTimestampLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SafeEmerald,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "${continuousSnapshot.totalFieldReports} logs · ${continuousSnapshot.verifiedOutcomesCount} outcomes · ${continuousSnapshot.completedJobsCount} verified jobs · ${continuousSnapshot.honestReviewsCount} Glass Wall reviews",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = "Top City: ${continuousSnapshot.topCityInsight} · ${continuousSnapshot.topSolutionHeadline}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Interactive Problem-to-Perfect-Contractor Matchmaker Card
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Perfect Contractor Matchmaker (A–Z)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Matches your exact issue against non-blacklisted pros using client-proven job success, CSLB license check, DOJ Live Scan safety clearance, IICRC standards, and distance.",
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
                                        "roofer" to "Roof Leak",
                                        "plumber" to "Pipe / Drain",
                                        "structural_engineer" to "Structural PE",
                                        "industrial_hygienist" to "Air / Tape Test",
                                        "landscaper" to "Landscaping"
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
                                    label = { Text("Describe exact problem (roof leak, slab heave, copper corrosion, mold, etc.)") },
                                    minLines = 2,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("matchmaker_problem_input")
                                )

                                if (allReports.isNotEmpty()) {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        myReports.take(2).forEach { r ->
                                            OutlinedButton(onClick = { viewModel.applyProblemMatchFromReport(r) }) {
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

                    // Online Licensed Contractor Finder (A-Z Web Search + Google Grounding)
                    item {
                        Card(shape = RoundedCornerShape(18.dp)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text("Find Licensed & Reputable Pros Online (A–Z)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "Need a specialized licensed trade near $defaultCity right away? Search live web + CA CSLB/BBB/IICRC directories with direct verification links.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(
                                        "roofer" to "Roofer",
                                        "plumber" to "Plumber",
                                        "structural_engineer" to "Structural Eng.",
                                        "foundation_specialist" to "Foundation/Slab",
                                        "industrial_hygienist" to "Hygienist",
                                        "landscaper" to "Landscaper",
                                        "mold_remediation" to "Mold/IICRC",
                                        "electrician" to "Electrician",
                                        "hvac" to "HVAC",
                                        "general_contractor" to "General (B)"
                                    ).forEach { (k, label) ->
                                        FilterChip(
                                            selected = onlineTradeKey == k,
                                            onClick = { onlineTradeKey = k },
                                            label = { Text(label) }
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = onlineSearchNote,
                                    onValueChange = { onlineSearchNote = it },
                                    label = { Text("Issue context (optional, uses Matchmaker text if blank)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                FilledTonalButton(
                                    onClick = {
                                        val tradeLabel = WaterTableCatalogs.specialtyLabel(onlineTradeKey)
                                        val issue = onlineSearchNote.ifBlank { problemMatchQuery.problemText }
                                            .ifBlank { "Licensed $tradeLabel for home inspection & repair in $defaultCity" }
                                        viewModel.searchOnlineContractors(tradeLabel, issue)
                                    },
                                    enabled = onlineContractorLeadsState !is UiState.Loading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("search_online_pros_button")
                                ) {
                                    Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Search Licensed Pros Online")
                                }

                                when (onlineContractorLeadsState) {
                                    UiState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    is UiState.Error -> Text(onlineContractorLeadsState.message, color = FloodCrimson, style = MaterialTheme.typography.bodySmall)
                                    is UiState.Success -> {
                                        val leads = onlineContractorLeadsState.data
                                        if (leads.isEmpty()) {
                                            Text("No external directory leads returned yet — try refining your trade or city.", style = MaterialTheme.typography.bodySmall)
                                        } else {
                                            leads.forEach { lead ->
                                                Surface(
                                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Text(lead.companyName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                        Text("${lead.tradeSummary} · ${lead.licenseOrRatingHint}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                                        Text(lead.reasonMatched, style = MaterialTheme.typography.bodySmall)
                                                        if (lead.verificationUrl.startsWith("http")) {
                                                            Text(
                                                                text = "↗ Verify on ${lead.domain.ifBlank { "Web" }}",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                textDecoration = TextDecoration.Underline,
                                                                modifier = Modifier.clickable {
                                                                    runCatching { uriHandler.openUri(lead.verificationUrl) }
                                                                }
                                                            )
                                                        }
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
                            FilterChip(
                                selected = specialtyFilter == "all",
                                onClick = { specialtyFilter = "all" },
                                label = { Text("All Ranked") }
                            )
                            WaterTableCatalogs.allContractorSpecialties.forEach { (key, label) ->
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
                                                text = "Any licensed contractor (Restoration, Structural Eng., Hygienist, Landscaper, Roofer, Plumber, General B) can join in 'Pro Hub' — or search online pros right above.",
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

                                            // Verified Experience Guarantee + Trust Badges
                                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                if (match.guaranteeEligible && match.guaranteeBadgeLabel.isNotBlank()) {
                                                    Surface(color = Color(0xFFD1FAE5), shape = RoundedCornerShape(50)) {
                                                        Text(
                                                            text = match.guaranteeBadgeLabel,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = SafeEmerald,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        )
                                                    }
                                                }
                                                if (pro.cslbBoardStatus == "cslb_verified") {
                                                    Surface(color = Color(0xFFE0F2FE), shape = RoundedCornerShape(50)) {
                                                        Text(
                                                            text = "CA CSLB Checked",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color(0xFF0369A1),
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        )
                                                    }
                                                }
                                                if (pro.liveScanStatus == "verified_cleared") {
                                                    Surface(color = Color(0xFFD1FAE5), shape = RoundedCornerShape(50)) {
                                                        Text(
                                                            text = "Live Scan Home-Safe",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = SafeEmerald,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        )
                                                    }
                                                } else if (pro.liveScanStatus == "submitted_ati") {
                                                    Surface(color = Color(0xFFFEF3C7), shape = RoundedCornerShape(50)) {
                                                        Text(
                                                            text = "Live Scan ATI Pending",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = FloodAmber,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        )
                                                    }
                                                }
                                                if (pro.iicrcCertNumber.isNotBlank() || pro.isIicrcGovernor) {
                                                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(50)) {
                                                        Text(
                                                            text = if (pro.isIicrcGovernor) "IICRC Governor" else "IICRC Certified",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        )
                                                    }
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
                                                        text = "${WaterTableCatalogs.specialtyLabel(pro.specialty)}${if (pro.companyName.isNotBlank()) " · ${pro.companyName}" else ""}",
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
                                                FilledTonalButton(
                                                    onClick = { dossierTargetMatch = match },
                                                    modifier = Modifier.weight(1f).height(46.dp)
                                                ) {
                                                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Dossier ($portfolioCount)", maxLines = 1)
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        reviewTargetProForDialog = pro
                                                        showGlassWallReviewDialog = true
                                                    },
                                                    modifier = Modifier.weight(1f).height(46.dp)
                                                ) {
                                                    Text("Review Honest", maxLines = 1)
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        complaintTargetPro = pro
                                                        viewModel.clearLatestFiledComplaint()
                                                        showComplaintDialog = true
                                                    },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FloodCrimson),
                                                    modifier = Modifier.height(46.dp)
                                                ) {
                                                    Icon(Icons.Filled.Gavel, contentDescription = "File Board Complaint", modifier = Modifier.size(16.dp))
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

            // 1. Glass Wall Honest Reviews + Proof + Community Blacklist + Formal Complaints
            1 -> {
                GlassWallSection(
                    viewModel = viewModel,
                    allReviews = allReviews,
                    complaints = complaints,
                    externalGroups = externalGroups,
                    contractors = sortedContractors,
                    reputations = reputations,
                    onOpenPostReview = {
                        reviewTargetProForDialog = null
                        showGlassWallReviewDialog = true
                    },
                    onOpenComplaint = {
                        complaintTargetPro = null
                        viewModel.clearLatestFiledComplaint()
                        showComplaintDialog = true
                    }
                )
            }

            // 2. Client's Submitted Consultations + Work Auth + Hygienist Lab Sampling
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
                                    Text("No Contractor Requests Yet", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "Pick the nearest non-blacklisted pro to request remote engineering triage, General Work Authorizations, or full restoration work.",
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
                                onRate = { rating -> viewModel.completeAndRateConsultation(req.id, rating) },
                                onOpenWorkAuth = { workAuthTarget = req },
                                onSendToInsurance = { viewModel.sendConsultationToLinkedInsurance(req) }
                            )
                        }
                    }

                    // Industrial Hygienist Air & Tape Sampling Section
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Filled.Science, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text("Industrial Hygienist Air & Tape Samples", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "Order independent lab air-spore traps or mold surface tape lifts before or after remediation—and let certified hygienists quote and post lab results here.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Button(
                                    onClick = { showHygienistDialog = true },
                                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("open_hygienist_request_button")
                                ) {
                                    Text("Request Air / Mold Tape Samples")
                                }
                            }
                        }
                    }

                    if (myHygienistRequests.isNotEmpty()) {
                        item {
                            Text("My Hygienist Sample Requests (${myHygienistRequests.size})", style = MaterialTheme.typography.titleMedium)
                        }
                        items(myHygienistRequests, key = { "my_hyg_${it.id}" }) { req ->
                            HygienistRequestCard(
                                req = req,
                                canManageAsHygienist = false,
                                isSubmitting = isSubmitting,
                                onClaimOrUpdate = { _, _, _ -> }
                            )
                        }
                    }

                    if (isHygienistOrIicrc && (openHygienistRequests.isNotEmpty() || assignedHygienistRequests.isNotEmpty())) {
                        item {
                            Text("Open Hygienist Dispatch Marketplace (${openHygienistRequests.size + assignedHygienistRequests.size})", style = MaterialTheme.typography.titleMedium, color = SafeEmerald)
                        }
                        items((assignedHygienistRequests + openHygienistRequests).distinctBy { it.id }, key = { "hyg_mkt_${it.id}" }) { req ->
                            HygienistRequestCard(
                                req = req,
                                canManageAsHygienist = true,
                                isSubmitting = isSubmitting,
                                onClaimOrUpdate = { status, fee, summary ->
                                    viewModel.updateHygienistLabResult(
                                        requestId = req.id,
                                        hygienistName = myProProfile?.displayName ?: "Certified Hygienist",
                                        status = status,
                                        quotedFeeUsd = fee,
                                        labFindingsSummary = summary
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // 3. Licensed Pro Hub (All 18 A-Z Trades, CSLB Live Verify, DOJ Live Scan, IICRC Governor, Incoming Inbox, Landscaper Note, Suggest Revision)
            3 -> {
                ContractorHubSection(
                    viewModel = viewModel,
                    contractors = sortedContractors,
                    inboxUi = contractorInboxUi,
                    allPortfolio = allPortfolio,
                    portfolioAttachment = portfolioAttachment,
                    liveScanProofAttachment = liveScanProofAttachment,
                    cslbCheckState = cslbCheckState,
                    defaultCity = defaultCity,
                    isSubmitting = isSubmitting,
                    onOpenReviewDialog = {
                        viewModel.clearCopilotAdvice()
                        reviewTargetConsultation = it
                    },
                    onOpenWorkAuth = { workAuthTarget = it },
                    onNavigateToDiagnose = onNavigateToDiagnose,
                    onOpenSuggestRevision = { showSuggestRevisionDialog = true }
                )
            }

            // 4. IICRC Restoration Governance Board Desk
            4 -> {
                IicrcGovernanceBoardSection(
                    viewModel = viewModel,
                    audits = iicrcAudits,
                    complaints = complaints,
                    contractors = sortedContractors,
                    iicrcAiCheckState = iicrcAiCheckState,
                    isSubmitting = isSubmitting,
                    onJumpToJoinIicrc = { selectedSubTab = 3 },
                    onOpenComplaintDialog = {
                        complaintTargetPro = null
                        viewModel.clearLatestFiledComplaint()
                        showComplaintDialog = true
                    }
                )
            }

            // 5. Insurance Hub & Direct Telemetry Policies
            5 -> {
                InsuranceHubSection(
                    viewModel = viewModel,
                    userProfile = userProfile,
                    carriers = insuranceCarriers,
                    claims = mergedClaims,
                    contractors = sortedContractors,
                    telemetryDiscountPercent = telemetryDiscountPercent,
                    myPolicyOffers = myPolicyOffers,
                    openPolicyQuotes = openPolicyQuotes,
                    insurerPolicyOffers = insurerPolicyOffers,
                    defaultCity = defaultCity,
                    isSubmitting = isSubmitting
                )
            }
        }
    }

    // Transparent Dossier Dialog (CSLB + Live Scan + IICRC + Portfolio + Reviews + Complaints)
    dossierTargetMatch?.let { match ->
        val pro = match.contractor
        val proPortfolio = allPortfolio.filter { it.userId == pro.userId }
        val proReviews = allReviews.filter { it.contractorUserId == pro.userId }
        ContractorTransparentProfileDialog(
            pro = pro,
            match = match,
            portfolioItems = proPortfolio,
            reviews = proReviews,
            complaints = complaints,
            onFileComplaint = {
                dossierTargetMatch = null
                complaintTargetPro = pro
                viewModel.clearLatestFiledComplaint()
                showComplaintDialog = true
            },
            onPostHonestReview = {
                dossierTargetMatch = null
                reviewTargetProForDialog = pro
                showGlassWallReviewDialog = true
            },
            onDismiss = { dossierTargetMatch = null }
        )
    }

    // Formal Complaint Dialog -> SoCal CSLB & IICRC
    if (showComplaintDialog) {
        FileComplaintDialog(
            preselectedPro = complaintTargetPro,
            defaultCity = defaultCity,
            proofAttachment = complaintProofAttachment,
            latestFiledComplaint = latestFiledComplaint,
            isSubmitting = isSubmitting,
            onPickProof = { uri, ctx -> viewModel.attachComplaintProofFromUri(ctx, uri) },
            onClearProof = { viewModel.clearComplaintProof() },
            onExportComplaintPdf = { cmp -> viewModel.exportComplaintPacketPdf(context, cmp) },
            onSubmitComplaint = { uid, name, comp, lic, iicrc, isRest, c, cat, sum, ev ->
                viewModel.fileContractorComplaint(
                    contractorUserId = uid,
                    contractorName = name,
                    companyName = comp,
                    licenseNumber = lic,
                    iicrcCertNumber = iicrc,
                    isRestorationCompany = isRest,
                    city = c,
                    complaintCategory = cat,
                    summary = sum,
                    evidenceNotes = ev
                )
            },
            onDismiss = {
                showComplaintDialog = false
                complaintTargetPro = null
                viewModel.clearLatestFiledComplaint()
            }
        )
    }

    // Work Authorization & Certified Payment Schedule Dialog
    workAuthTarget?.let { req ->
        val matchingPro = sortedContractors.find { it.userId == req.contractorUserId }
        WorkAuthorizationDialog(
            consultation = req,
            contractor = matchingPro,
            isClient = req.clientUserId == viewModel.currentUserId,
            isSubmitting = isSubmitting,
            onSign = { signature -> viewModel.signWorkAuthorization(req.id, signature) },
            onExportPdf = { viewModel.exportWorkAuthorizationPdf(context, req, matchingPro) },
            onDismiss = { workAuthTarget = null }
        )
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
            copilotAdviceState = copilotAdviceState,
            onRunCopilot = { viewModel.runContractorCopilot(req) },
            onDismiss = {
                viewModel.clearCopilotAdvice()
                reviewTargetConsultation = null
            },
            onSubmit = { verdict, recommendation, workEstimate, strategy, links, authTerms, paymentSched ->
                viewModel.submitContractorReview(
                    consultation = req,
                    verdict = verdict,
                    recommendation = recommendation,
                    proposedWorkEstimateUsd = workEstimate,
                    contractorStrategy = strategy,
                    aiCreditLinks = links,
                    workAuthTerms = authTerms,
                    paymentScheduleSummary = paymentSched
                )
                reviewTargetConsultation = null
            }
        )
    }

    if (showGlassWallReviewDialog) {
        PostGlassWallReviewDialog(
            preselectedPro = reviewTargetProForDialog,
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

    if (showHygienistDialog) {
        HygienistSampleRequestDialog(
            defaultCity = defaultCity,
            defaultAddress = "",
            isSubmitting = isSubmitting,
            onDismiss = { showHygienistDialog = false },
            onSubmit = { c, addr, types, notesTxt ->
                viewModel.submitHygienistSampleRequest(c, addr, types, notesTxt) {
                    showHygienistDialog = false
                }
            }
        )
    }

    if (showSuggestRevisionDialog) {
        SuggestAppRevisionDialog(
            initialTrade = myProProfile?.specialty ?: "",
            isSubmitting = isSubmitting,
            onDismiss = { showSuggestRevisionDialog = false },
            onSubmit = { role, trade, title, cat, desc ->
                viewModel.submitAppRevisionSuggestion(role, trade, title, cat, desc) {
                    showSuggestRevisionDialog = false
                }
            }
        )
    }
}

@Composable
private fun GlassWallSection(
    viewModel: WaterTableViewModel,
    allReviews: List<ContractorReview>,
    complaints: List<ContractorComplaint>,
    externalGroups: List<ExternalContractorGlassWallGroup>,
    contractors: List<ContractorProfile>,
    reputations: Map<String, ContractorReputation>,
    onOpenPostReview: () -> Unit,
    onOpenComplaint: () -> Unit
) {
    val context = LocalContext.current
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
                        text = "See who contractors really are before hiring. Clients post honest reviews with photo proof (impossible to delete), plus formal complaints sent directly to SoCal CSLB & IICRC.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = onOpenPostReview,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("post_glass_wall_review_button")
                    ) {
                        Text("Log Honest Contractor Experience + Proof")
                    }
                    OutlinedButton(
                        onClick = onOpenComplaint,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FloodCrimson),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("open_complaint_dialog_button")
                    ) {
                        Icon(Icons.Filled.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("File Instant Complaint -> SoCal CSLB & IICRC")
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

        if (complaints.isNotEmpty()) {
            item {
                Text("Formal Board Complaints (${complaints.size})", style = MaterialTheme.typography.titleMedium)
            }
            items(complaints.take(8), key = { "gw_cmp_${it.id}" }) { cmp ->
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${cmp.contractorName} · ${cmp.city}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = "CSLB${if (cmp.routedToIicrc) " + IICRC" else ""} · ${cmp.status.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = FloodCrimson,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(cmp.summary, style = MaterialTheme.typography.bodySmall)
                        FilledTonalButton(
                            onClick = {
                                val file = viewModel.exportComplaintPacketPdf(context, cmp) ?: return@FilledTonalButton
                                sharePdfFile(context, file, "Board Complaint - ${cmp.contractorName}")
                            }
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Complaint Dossier PDF")
                        }
                    }
                }
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
    onRate: (Int) -> Unit,
    onOpenWorkAuth: () -> Unit,
    onSendToInsurance: (() -> Unit)? = null
) {
    val uriHandler = LocalUriHandler.current
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
                text = "Advice Fee: Pro $${req.contractorFeeUsd} + Platform $${req.platformFeeUsd} = $${req.totalFeeUsd} total${if (req.proposedWorkEstimateUsd > 0) " · Work Estimate: $${req.proposedWorkEstimateUsd}" else ""}",
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
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Pro Recommendation:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text(req.engineerRecommendation, style = MaterialTheme.typography.bodySmall)
                        if (req.contractorStrategy.isNotBlank()) {
                            Text("Strategy: ${req.contractorStrategy}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        if (req.aiCreditLinks.isNotEmpty()) {
                            req.aiCreditLinks.forEach { raw ->
                                val parts = raw.split("|", limit = 2)
                                val label = parts.firstOrNull()?.trim().orEmpty()
                                val url = parts.getOrNull(1)?.trim().orEmpty().ifBlank { label }
                                if (url.startsWith("http")) {
                                    Text(
                                        text = "↗ Source: $label",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        textDecoration = TextDecoration.Underline,
                                        modifier = Modifier.clickable { runCatching { uriHandler.openUri(url) } }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (req.workAuthStatus != "none" && req.workAuthStatus.isNotBlank()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onOpenWorkAuth,
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Icon(Icons.Filled.Assignment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (req.workAuthStatus == "client_authorized") "Work Auth Signed" else "Review Work Auth",
                            maxLines = 1
                        )
                    }
                    if (onSendToInsurance != null) {
                        OutlinedButton(
                            onClick = onSendToInsurance,
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.Filled.Policy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync Insurance", maxLines = 1)
                        }
                    }
                }
            } else if (onSendToInsurance != null && req.engineerRecommendation.isNotBlank()) {
                OutlinedButton(
                    onClick = onSendToInsurance,
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(Icons.Filled.Policy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send Scope to My Linked Insurance")
                }
            }

            if (isContractorView && req.status == "requested") {
                Button(onClick = onSubmitReview, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("Submit Plan, AI Credit Links & Work Auth (Earn $${req.contractorFeeUsd})")
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

@Composable
private fun HygienistRequestCard(
    req: HygienistSampleRequest,
    canManageAsHygienist: Boolean,
    isSubmitting: Boolean,
    onClaimOrUpdate: (status: String, feeUsd: Int, labSummary: String) -> Unit
) {
    var feeText by remember(req.id) { mutableStateOf((req.quotedFeeUsd.takeIf { it > 0 } ?: 195).toString()) }
    var findingsInput by remember(req.id) { mutableStateOf(req.labFindingsSummary) }

    Card(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${req.city} · ${req.sampleTypes.joinToString(", ")}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(req.status.replace('_', ' ').uppercase(), style = MaterialTheme.typography.labelSmall, color = SafeEmerald, fontWeight = FontWeight.Bold)
            }
            Text("Area: ${req.propertyAddressHint.ifBlank { req.city }} · Client: ${req.clientName}", style = MaterialTheme.typography.labelSmall)
            Text(req.reasonNotes, style = MaterialTheme.typography.bodySmall)
            if (req.hygienistName.isNotBlank()) {
                Text("Assigned Hygienist: ${req.hygienistName} · Fee $${req.quotedFeeUsd}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            if (req.labFindingsSummary.isNotBlank()) {
                Surface(color = Color(0xFFECFDF5), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("Lab Findings: ${req.labFindingsSummary}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
                }
            }
            if (canManageAsHygienist && req.status != "lab_report_ready") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = feeText,
                        onValueChange = { feeText = it.filter { c -> c.isDigit() } },
                        label = { Text("Sample Fee $") },
                        singleLine = true,
                        modifier = Modifier.weight(0.55f)
                    )
                    OutlinedTextField(
                        value = findingsInput,
                        onValueChange = { findingsInput = it },
                        label = { Text("Lab clearance / spore count findings") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (req.status == "requested") {
                        OutlinedButton(
                            onClick = { onClaimOrUpdate("accepted", feeText.toIntOrNull() ?: 195, findingsInput) },
                            enabled = !isSubmitting,
                            modifier = Modifier.weight(1f)
                        ) { Text("Accept Job") }
                    }
                    Button(
                        onClick = {
                            onClaimOrUpdate(
                                "lab_report_ready",
                                feeText.toIntOrNull() ?: 195,
                                findingsInput.ifBlank { "Spore & tape sample collected and clearance findings posted." }
                            )
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier.weight(1f)
                    ) { Text("Post Lab Results") }
                }
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
    liveScanProofAttachment: MediaAttachment?,
    cslbCheckState: UiState<CslbVerificationResult>?,
    defaultCity: String,
    isSubmitting: Boolean,
    onOpenReviewDialog: (ConsultationRequest) -> Unit,
    onOpenWorkAuth: (ConsultationRequest) -> Unit,
    onNavigateToDiagnose: () -> Unit,
    onOpenSuggestRevision: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val existingProfile = contractors.find { it.userId == viewModel.currentUserId }
    val myPortfolio = allPortfolio.filter { it.userId == viewModel.currentUserId }

    var displayName by remember(existingProfile) { mutableStateOf(existingProfile?.displayName ?: "") }
    var companyName by remember(existingProfile) { mutableStateOf(existingProfile?.companyName ?: "") }
    var specialty by remember(existingProfile) { mutableStateOf(existingProfile?.specialty ?: "water_mitigation") }
    var licenseNumber by remember(existingProfile) { mutableStateOf(existingProfile?.licenseNumber ?: "") }
    var licensingState by remember(existingProfile) { mutableStateOf(existingProfile?.licensingState ?: "CA") }
    var iicrcCertNumber by remember(existingProfile) { mutableStateOf(existingProfile?.iicrcCertNumber ?: "") }
    var isIicrcGovernor by remember(existingProfile) { mutableStateOf(existingProfile?.isIicrcGovernor ?: false) }

    var liveScanStatus by remember(existingProfile) { mutableStateOf(existingProfile?.liveScanStatus ?: "not_submitted") }
    var liveScanAtiRef by remember(existingProfile) { mutableStateOf(existingProfile?.liveScanAtiRef ?: "") }
    var liveScanAgency by remember(existingProfile) { mutableStateOf(existingProfile?.liveScanAgency ?: "") }
    var liveScanClearedDate by remember(existingProfile) { mutableStateOf(existingProfile?.liveScanClearedDate ?: "") }

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
    ) { uri -> if (uri != null) viewModel.attachPortfolioPhotoFromUri(context, uri) }

    val liveScanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) viewModel.attachLiveScanProofFromUri(context, uri) }

    val inbox = (inboxUi as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Landscaper Water-Table Shortcut + App Revision Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Field Community Tools for Every Trade (A–Z)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onNavigateToDiagnose,
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.Filled.Landscape, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Log Water Table", maxLines = 1)
                        }
                        Button(
                            onClick = onOpenSuggestRevision,
                            modifier = Modifier.weight(1f).height(46.dp).testTag("suggest_app_revision_button")
                        ) {
                            Icon(Icons.Filled.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Suggest Revision", maxLines = 1)
                        }
                    }
                }
            }
        }

        if (inbox.isNotEmpty()) {
            item {
                Text("Incoming Dispatches & Leads (${inbox.size})", style = MaterialTheme.typography.titleLarge)
            }
            items(inbox, key = { it.id }) { req ->
                ConsultationCard(
                    req = req,
                    isContractorView = true,
                    onSubmitReview = { onOpenReviewDialog(req) },
                    onRate = {},
                    onOpenWorkAuth = { onOpenWorkAuth(req) }
                )
            }
        }

        // Contractor Profile Form (All 18 A-Z Trades + CA CSLB + Live Scan + IICRC)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Engineering, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = if (existingProfile == null) "Join as Any Licensed Pro / Hygienist / IICRC" else "Update Transparent Pro Profile",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = "Open to Restoration, Structural Engineers, Industrial Hygienists, Landscapers, Roofers, Plumbers, Electricians, HVAC, Foundation & General B pros (${WaterTableRepository.PLATFORM_FEE_PERCENT}% platform maintenance fee).",
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
                        label = { Text("Company / Engineering / Hygienist Firm") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Select Specialty (All 18 A–Z Trades)", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WaterTableCatalogs.allContractorSpecialties.forEach { (key, label) ->
                            FilterChip(
                                selected = specialty == key,
                                onClick = {
                                    specialty = key
                                    if (key == "iicrc_inspector") isIicrcGovernor = true
                                },
                                label = { Text(label) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = licenseNumber,
                            onValueChange = { licenseNumber = it },
                            label = { Text("CA CSLB / State Lic #") },
                            singleLine = true,
                            modifier = Modifier.weight(1.4f).testTag("pro_license_input")
                        )
                        OutlinedTextField(
                            value = licensingState,
                            onValueChange = { licensingState = it },
                            label = { Text("State") },
                            singleLine = true,
                            modifier = Modifier.weight(0.6f)
                        )
                    }

                    // Live CA CSLB Board License Check
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = { viewModel.checkCslbLicense(licenseNumber, licensingState) },
                            enabled = licenseNumber.trim().length >= 4 && cslbCheckState !is UiState.Loading,
                            modifier = Modifier.weight(1f).height(46.dp).testTag("verify_cslb_button")
                        ) {
                            Icon(Icons.Filled.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verify on CA CSLB Board", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                runCatching { uriHandler.openUri(WaterTableCatalogs.buildCslbLookupUrl(licenseNumber)) }
                            },
                            modifier = Modifier.height(46.dp)
                        ) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = "Open CSLB lookup", modifier = Modifier.size(16.dp))
                        }
                    }

                    when (cslbCheckState) {
                        UiState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        is UiState.Error -> Text(cslbCheckState.message, color = FloodCrimson, style = MaterialTheme.typography.bodySmall)
                        is UiState.Success -> {
                            val res = cslbCheckState.data
                            Surface(
                                color = if (res.status == "cslb_verified") Color(0xFFD1FAE5) else MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${res.summary} (${res.classification})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (res.status == "cslb_verified") SafeEmerald else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                        null -> Unit
                    }

                    // IICRC Certification + Technical Governor Opt-In
                    OutlinedTextField(
                        value = iicrcCertNumber,
                        onValueChange = { iicrcCertNumber = it },
                        label = { Text("IICRC Cert # (WRT, AMRT, FSRT — optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Join as IICRC Restoration Governor", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Switch(checked = isIicrcGovernor, onCheckedChange = { isIicrcGovernor = it })
                    }

                    // DOJ / FBI Live Scan Background Clearance Section
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.Security, contentDescription = null, tint = SafeEmerald, modifier = Modifier.size(18.dp))
                                Text("DOJ / FBI Live Scan Safety Clearance", style = MaterialTheme.typography.labelLarge)
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(
                                    "not_submitted" to "Not Submitted",
                                    "submitted_ati" to "ATI Submitted",
                                    "verified_cleared" to "Cleared Home-Safe"
                                ).forEach { (st, lbl) ->
                                    FilterChip(
                                        selected = liveScanStatus == st,
                                        onClick = { liveScanStatus = st },
                                        label = { Text(lbl) }
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = liveScanAtiRef,
                                    onValueChange = { liveScanAtiRef = it },
                                    label = { Text("ATI #") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = liveScanClearedDate,
                                    onValueChange = { liveScanClearedDate = it },
                                    label = { Text("Date") },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.9f)
                                )
                            }
                            OutlinedTextField(
                                value = liveScanAgency,
                                onValueChange = { liveScanAgency = it },
                                label = { Text("Live Scan Agency / ORI Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            liveScanProofAttachment?.previewBitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Live Scan receipt preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth().height(95.dp).clip(RoundedCornerShape(8.dp))
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { liveScanLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (liveScanProofAttachment == null) "Attach Live Scan Photo" else "Change Photo", maxLines = 1)
                                }
                                if (liveScanProofAttachment != null) {
                                    TextButton(onClick = { viewModel.clearLiveScanProof() }) { Text("Clear") }
                                }
                            }
                        }
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
                        Text("Accept On-Site Work Leads", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = offersOnSiteWork, onCheckedChange = { offersOnSiteWork = it })
                    }
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Equipment, certifications, guarantees & background") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val checkedStatus = (cslbCheckState as? UiState.Success)?.data?.status
                        ?: existingProfile?.cslbBoardStatus
                        ?: "format_checked"

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
                                bio = bio,
                                cslbBoardStatus = checkedStatus,
                                cslbLookupUrl = WaterTableCatalogs.buildCslbLookupUrl(licenseNumber),
                                iicrcCertNumber = iicrcCertNumber,
                                isIicrcGovernor = isIicrcGovernor || specialty == "iicrc_inspector",
                                liveScanStatus = liveScanStatus,
                                liveScanAtiRef = liveScanAtiRef,
                                liveScanAgency = liveScanAgency,
                                liveScanClearedDate = liveScanClearedDate,
                                existingLiveScanProofBase64 = existingProfile?.liveScanProofBase64.orEmpty()
                            )
                        },
                        enabled = !isSubmitting && displayName.length >= 2 && licenseNumber.length >= 3,
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("register_contractor_button")
                    ) {
                        Text(if (existingProfile == null) "Publish Transparent Pro Profile" else "Save Profile Changes")
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
                        OutlinedTextField(value = portDesc, onValueChange = { portDesc = it }, label = { Text("Real work completed / guaranteed outcome") }, minLines = 2, modifier = Modifier.fillMaxWidth())
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
    copilotAdviceState: UiState<ContractorCopilotAdvice>?,
    onRunCopilot: () -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (
        verdict: String,
        recommendation: String,
        workEstimateUsd: Int,
        strategy: String,
        creditLinks: List<String>,
        workAuthTerms: String,
        paymentScheduleSummary: String
    ) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var verdict by remember { mutableStateOf("ground_water_table") }
    var recommendation by remember { mutableStateOf("") }
    var strategyText by remember { mutableStateOf("") }
    var workEstimateText by remember { mutableStateOf("0") }
    var workAuthTermsText by remember { mutableStateOf("") }
    var creditLinksList by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(copilotAdviceState) {
        if (copilotAdviceState is UiState.Success) {
            val adv = copilotAdviceState.data
            if (recommendation.isBlank()) recommendation = adv.clientAnswer
            if (strategyText.isBlank()) strategyText = adv.contractorStrategy
            if (adv.recommendedVerdict.isNotBlank()) verdict = adv.recommendedVerdict
            if (workEstimateText == "0" && adv.suggestedEstimateUsd > 0) {
                workEstimateText = adv.suggestedEstimateUsd.toString()
            }
            creditLinksList = adv.creditLinks.map { "${it.title} | ${it.url}" }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pro Plan, AI+Web Co-Pilot & Work Auth") },
        text = {
            LazyColumn(
                modifier = Modifier.height(430.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilledTonalButton(
                        onClick = onRunCopilot,
                        enabled = copilotAdviceState !is UiState.Loading,
                        modifier = Modifier.fillMaxWidth().height(46.dp).testTag("run_contractor_copilot_button")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI + Web Co-Pilot (Draft Answer & Credit Links)")
                    }
                    when (copilotAdviceState) {
                        UiState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                        is UiState.Error -> Text(copilotAdviceState.message, color = FloodCrimson, style = MaterialTheme.typography.bodySmall)
                        is UiState.Success -> {
                            val adv = copilotAdviceState.data
                            if (adv.creditLinks.isNotEmpty()) {
                                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Grounded Web Citations (${adv.creditLinks.size}):", style = MaterialTheme.typography.labelLarge)
                                        adv.creditLinks.forEach { link ->
                                            Text(
                                                text = "↗ ${link.title} (${link.domain})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                textDecoration = TextDecoration.Underline,
                                                modifier = Modifier.clickable { runCatching { uriHandler.openUri(link.url) } }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        null -> Unit
                    }

                    Spacer(modifier = Modifier.height(6.dp))
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
                        label = { Text("Client Answer & Detailed Scope of Work") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = strategyText,
                        onValueChange = { strategyText = it },
                        label = { Text("Contractor Execution & Sequencing Strategy") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = workEstimateText,
                        onValueChange = { workEstimateText = it.filter { c -> c.isDigit() } },
                        label = { Text("Work Estimate USD (0 = Remote Advice Only)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = workAuthTermsText,
                        onValueChange = { workAuthTermsText = it },
                        label = { Text("Work Auth Clauses (optional — auto-drafted if blank)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val est = workEstimateText.toIntOrNull() ?: 0
                    val paymentSched = WorkAuthorizationPdfGenerator.buildDefaultPaymentSchedule(est)
                    val resolvedTerms = workAuthTermsText.ifBlank {
                        WorkAuthorizationPdfGenerator.buildDefaultAuthorizationText(
                            consultation = consultation,
                            contractor = null,
                            scopeText = recommendation,
                            strategyText = strategyText,
                            estimateUsd = est,
                            paymentScheduleSummary = paymentSched
                        )
                    }
                    onSubmit(verdict, recommendation, est, strategyText, creditLinksList, resolvedTerms, paymentSched)
                },
                enabled = recommendation.isNotBlank()
            ) { Text("Send Plan & Work Auth") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuggestAppRevisionDialog(
    initialTrade: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (role: String, trade: String, title: String, category: String, description: String) -> Unit
) {
    var role by remember { mutableStateOf("contractor") }
    var trade by remember { mutableStateOf(initialTrade) }
    var category by remember { mutableStateOf("Workflow") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Suggest App Revision for Everyone") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Any contractor, engineer, hygienist, landscaper, or homeowner can propose workflow or diagnostic revisions.", style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("contractor" to "Contractor", "resident" to "Homeowner", "insurer" to "Insurer", "iicrc_inspector" to "IICRC").forEach { (r, l) ->
                        FilterChip(selected = role == r, onClick = { role = r }, label = { Text(l) })
                    }
                }
                OutlinedTextField(value = trade, onValueChange = { trade = it }, label = { Text("Your Trade / Specialty") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Workflow", "AI Diagnostics", "Safety & Licensing", "Insurance", "Equipment & DIY").forEach { cat ->
                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Revision Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("How should it improve jobs for clients & pros?") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(role, trade, title, category, description) },
                enabled = !isSubmitting && title.trim().length >= 3 && description.trim().length >= 5
            ) { Text("Post Revision") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
