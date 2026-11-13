package com.example.ui.components

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.catalog.WaterTableCatalogs
import com.example.data.model.ContractorComplaint
import com.example.data.model.ContractorProfile
import com.example.data.model.GroundedCreditLink
import com.example.data.model.IicrcAuditRecord
import com.example.data.model.InsuranceCarrierProfile
import com.example.data.model.InsuranceClaimThread
import com.example.data.model.InsurancePolicyOffer
import com.example.data.model.UserProfile
import com.example.ui.UiState
import com.example.ui.WaterTableViewModel
import com.example.ui.theme.FloodAmber
import com.example.ui.theme.FloodCrimson
import com.example.ui.theme.SafeEmerald
import kotlin.math.roundToInt

@Composable
fun IicrcGovernanceBoardSection(
    viewModel: WaterTableViewModel,
    audits: List<IicrcAuditRecord>,
    complaints: List<ContractorComplaint>,
    contractors: List<ContractorProfile>,
    iicrcAiCheckState: Map<String, UiState<Triple<String, String, List<GroundedCreditLink>>>>,
    isSubmitting: Boolean,
    onJumpToJoinIicrc: () -> Unit,
    onOpenComplaintDialog: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val myProProfile = contractors.find { it.userId == viewModel.currentUserId }
    val amIicrcGovernor = myProProfile?.let { it.isIicrcGovernor || it.specialty == "iicrc_inspector" } == true
    val restorationComplaints = complaints.filter { it.isRestorationCompany }

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
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("IICRC Restoration Governance & Board Desk", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "IICRC Master Inspectors/Governors audit restoration scopes (S500 Water, S520 Mold, S700 Fire & Smoke) using live AI + Google Search standards verification, and review escalated restoration complaints alongside SoCal CSLB.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { runCatching { uriHandler.openUri(WaterTableCatalogs.IICRC_LOCATOR_URL) } },
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("IICRC Locator", maxLines = 1)
                        }
                        if (!amIicrcGovernor) {
                            Button(
                                onClick = onJumpToJoinIicrc,
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Text("Join as IICRC", maxLines = 1)
                            }
                        } else {
                            Button(
                                onClick = onOpenComplaintDialog,
                                colors = ButtonDefaults.buttonColors(containerColor = FloodCrimson),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Text("File Complaint", maxLines = 1)
                            }
                        }
                    }
                    if (!amIicrcGovernor) {
                        OutlinedButton(
                            onClick = onOpenComplaintDialog,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = FloodCrimson),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Icon(Icons.Filled.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("File Instant Complaint -> SoCal CSLB & IICRC")
                        }
                    }
                }
            }
        }

        // Restoration Scopes Under IICRC Technical Governance
        item {
            Text("Restoration Scopes Under IICRC Governance (${audits.size})", style = MaterialTheme.typography.titleMedium)
        }
        if (audits.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Text(
                        text = "No restoration proposals in the IICRC governance queue yet. Water, Mold & Fire restoration plans submitted by pros appear here automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(audits, key = { it.id }) { audit ->
                val aiCheck = iicrcAiCheckState[audit.id]
                var governorNoteInput by remember(audit.id) { mutableStateOf(audit.iicrcGovernorNotes) }
                var stdRefInput by remember(audit.id) {
                    mutableStateOf(audit.iicrcStandardRef.ifBlank { "ANSI/IICRC S500" })
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${audit.contractorName} · ${audit.city}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(audit.iicrcStandardRef.ifBlank { audit.lossCategory }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Surface(
                                color = when (audit.iicrcStatus) {
                                    "endorsed" -> Color(0xFFD1FAE5)
                                    "flagged_outdated" -> Color(0xFFFEE2E2)
                                    "revision_requested" -> Color(0xFFFEF3C7)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = audit.iicrcStatus.replace('_', ' ').uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when (audit.iicrcStatus) {
                                        "endorsed" -> SafeEmerald
                                        "flagged_outdated" -> FloodCrimson
                                        "revision_requested" -> FloodAmber
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text("Proposed Scope: ${audit.contractorSuggestion}", style = MaterialTheme.typography.bodySmall)
                        if (audit.contractorStrategy.isNotBlank()) {
                            Text("Strategy: ${audit.contractorStrategy}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (audit.iicrcGovernorNotes.isNotBlank()) {
                            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(10.dp)) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("IICRC Ruling (${audit.iicrcInspectorName}):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    Text(audit.iicrcGovernorNotes, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        FilledTonalButton(
                            onClick = { viewModel.runIicrcAiVerification(audit) },
                            enabled = aiCheck !is UiState.Loading,
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verify Up-to-Date IICRC Standard (AI + Web)")
                        }

                        when (aiCheck) {
                            UiState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            is UiState.Error -> Text(aiCheck.message, color = FloodCrimson, style = MaterialTheme.typography.bodySmall)
                            is UiState.Success -> {
                                val (recStatus, recNotes, recLinks) = aiCheck.data
                                Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f), shape = RoundedCornerShape(10.dp)) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("AI Web Verification ($recStatus):", style = MaterialTheme.typography.labelLarge, color = SafeEmerald)
                                        Text(recNotes, style = MaterialTheme.typography.bodySmall)
                                        recLinks.forEach { link ->
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
                            null -> Unit
                        }

                        if (amIicrcGovernor && audit.contractorUserId != viewModel.currentUserId) {
                            OutlinedTextField(
                                value = stdRefInput,
                                onValueChange = { stdRefInput = it },
                                label = { Text("IICRC Standard Cited (e.g., ANSI/IICRC S500 5th Ed.)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = governorNoteInput,
                                onValueChange = { governorNoteInput = it },
                                label = { Text("Governor Audit Notes") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                            val linksForSubmit = (aiCheck as? UiState.Success)?.data?.third?.map { "${it.title} | ${it.url}" } ?: audit.iicrcSources
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.submitIicrcGovernanceDecision(
                                            auditId = audit.id,
                                            iicrcStatus = "endorsed",
                                            iicrcStandardRef = stdRefInput,
                                            iicrcGovernorNotes = governorNoteInput.ifBlank { (aiCheck as? UiState.Success)?.data?.second ?: "Meets current IICRC standard." },
                                            iicrcSources = linksForSubmit
                                        )
                                    },
                                    enabled = !isSubmitting,
                                    modifier = Modifier.weight(1f)
                                ) { Text("Endorse") }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.submitIicrcGovernanceDecision(
                                            auditId = audit.id,
                                            iicrcStatus = "revision_requested",
                                            iicrcStandardRef = stdRefInput,
                                            iicrcGovernorNotes = governorNoteInput.ifBlank { "Update containment/dry-out plan per current IICRC." },
                                            iicrcSources = linksForSubmit
                                        )
                                    },
                                    enabled = !isSubmitting,
                                    modifier = Modifier.weight(1f)
                                ) { Text("Revise") }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.submitIicrcGovernanceDecision(
                                            auditId = audit.id,
                                            iicrcStatus = "flagged_outdated",
                                            iicrcStandardRef = stdRefInput,
                                            iicrcGovernorNotes = governorNoteInput.ifBlank { "Flagged non-compliant with current IICRC standard." },
                                            iicrcSources = linksForSubmit
                                        )
                                    },
                                    enabled = !isSubmitting,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FloodCrimson),
                                    modifier = Modifier.weight(1f)
                                ) { Text("Flag") }
                            }
                        }
                    }
                }
            }
        }

        // Formal Complaints Routed to SoCal CSLB + IICRC
        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text("Board Complaints Feed (${complaints.size} Total · ${restorationComplaints.size} IICRC Restoration)", style = MaterialTheme.typography.titleMedium)
        }
        items(complaints, key = { "board_${it.id}" }) { cmp ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${cmp.contractorName} (${cmp.city})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Lic #${cmp.licenseNumber.ifBlank { "Unverified" }}${if (cmp.routedToIicrc) " · IICRC Copy Sent" else ""}", style = MaterialTheme.typography.labelSmall, color = FloodCrimson)
                        }
                        Surface(color = Color(0xFFFEE2E2), shape = RoundedCornerShape(50)) {
                            Text(
                                text = cmp.status.replace('_', ' ').uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = FloodCrimson,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(cmp.summary, style = MaterialTheme.typography.bodySmall)
                    if (cmp.proofPhotoBase64.isNotBlank()) {
                        Base64MediaPreview(cmp.proofPhotoBase64, "photo", "", Modifier.fillMaxWidth().height(110.dp))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { runCatching { uriHandler.openUri(cmp.cslbIntakeUrl.ifBlank { WaterTableCatalogs.CSLB_COMPLAINT_URL }) } },
                            modifier = Modifier.weight(1f)
                        ) { Text("SoCal CSLB", maxLines = 1) }
                        if (cmp.routedToIicrc) {
                            OutlinedButton(
                                onClick = { runCatching { uriHandler.openUri(cmp.iicrcIntakeUrl.ifBlank { WaterTableCatalogs.IICRC_COMPLAINT_URL }) } },
                                modifier = Modifier.weight(1f)
                            ) { Text("IICRC Portal", maxLines = 1) }
                        }
                        FilledTonalButton(
                            onClick = {
                                val pdf = viewModel.exportComplaintPacketPdf(context, cmp) ?: return@FilledTonalButton
                                sharePdfFile(context, pdf, "Complaint Dossier - ${cmp.contractorName}")
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("PDF", maxLines = 1) }
                    }
                    if (amIicrcGovernor && cmp.isRestorationCompany && cmp.contractorUserId != viewModel.currentUserId && cmp.status != "resolved") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.markComplaintBoardStatus(cmp.id, "iicrc_reviewing") },
                                modifier = Modifier.weight(1f)
                            ) { Text("IICRC Reviewing") }
                            OutlinedButton(
                                onClick = { viewModel.markComplaintBoardStatus(cmp.id, "resolved") },
                                modifier = Modifier.weight(1f)
                            ) { Text("Mark Resolved") }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InsuranceHubSection(
    viewModel: WaterTableViewModel,
    userProfile: UserProfile?,
    carriers: List<InsuranceCarrierProfile>,
    claims: List<InsuranceClaimThread>,
    contractors: List<ContractorProfile>,
    telemetryDiscountPercent: Int = 0,
    myPolicyOffers: List<InsurancePolicyOffer> = emptyList(),
    openPolicyQuotes: List<InsurancePolicyOffer> = emptyList(),
    insurerPolicyOffers: List<InsurancePolicyOffer> = emptyList(),
    defaultCity: String,
    isSubmitting: Boolean
) {
    val myCarrierProfile = carriers.find { it.userId == viewModel.currentUserId }
    val uriHandler = LocalUriHandler.current

    // Client Insurance Policy Link state
    var selectedCarrier by remember(carriers, userProfile?.linkedInsuranceCarrierUid) {
        mutableStateOf(carriers.find { it.userId == userProfile?.linkedInsuranceCarrierUid })
    }
    var customCarrierName by remember(userProfile?.linkedInsuranceCarrierName) {
        mutableStateOf(userProfile?.linkedInsuranceCarrierName ?: "")
    }
    var policyRef by remember(userProfile?.linkedPolicyRef) { mutableStateOf(userProfile?.linkedPolicyRef ?: "") }
    var claimNumber by remember(userProfile?.linkedClaimNumber) { mutableStateOf(userProfile?.linkedClaimNumber ?: "") }
    var deductibleText by remember(userProfile?.linkedDeductibleUsd) {
        mutableStateOf((userProfile?.linkedDeductibleUsd ?: 1000).toString())
    }

    // Direct Policy Quote & Telemetry Discount state
    var quoteCoverageType by remember { mutableStateOf("homeowners_ho3") }
    var quoteLimitText by remember { mutableStateOf("450000") }
    var quoteBasePremiumText by remember { mutableStateOf("185") }
    var quotePropertyLabel by remember { mutableStateOf("Primary Home") }
    var quoteTerms by remember { mutableStateOf("") }

    // Open New Claim Thread state
    var claimContractor by remember(contractors) { mutableStateOf(contractors.firstOrNull()) }
    var claimLossCat by remember { mutableStateOf("water_restoration") }
    var claimPolicyType by remember { mutableStateOf("homeowners_ho3") }
    var claimNote by remember { mutableStateOf("") }

    // Insurance Company Registration state
    var insName by remember(myCarrierProfile) { mutableStateOf(myCarrierProfile?.carrierName ?: "") }
    var insAdjuster by remember(myCarrierProfile) { mutableStateOf(myCarrierProfile?.adjusterName ?: "") }
    var insNaic by remember(myCarrierProfile) { mutableStateOf(myCarrierProfile?.licenseOrNaicCode ?: "") }
    var insEmail by remember(myCarrierProfile) { mutableStateOf(myCarrierProfile?.claimsEmail ?: "") }
    var insPhone by remember(myCarrierProfile) { mutableStateOf(myCarrierProfile?.claimsPhone ?: "") }
    var insPortal by remember(myCarrierProfile) { mutableStateOf(myCarrierProfile?.claimsPortalUrl ?: "") }
    var insRegions by remember(myCarrierProfile, defaultCity) {
        mutableStateOf(myCarrierProfile?.serviceRegions?.joinToString(", ") ?: "Southern California, $defaultCity")
    }
    var insDirectBilling by remember(myCarrierProfile) { mutableStateOf(myCarrierProfile?.supportsDirectRestorationBilling ?: true) }
    var insTurnaround by remember(myCarrierProfile) { mutableStateOf((myCarrierProfile?.avgTurnaroundHours ?: 24).toString()) }
    var insBio by remember(myCarrierProfile) { mutableStateOf(myCarrierProfile?.bio ?: "") }

    val mergedPolicies = remember(myPolicyOffers, openPolicyQuotes, insurerPolicyOffers) {
        (myPolicyOffers + insurerPolicyOffers + openPolicyQuotes).distinctBy { it.id }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 0. Telemetry Rate Discount & Direct In-App Policies
        item {
            val basePrem = quoteBasePremiumText.toIntOrNull()?.coerceAtLeast(30) ?: 185
            val discountedPrem = (basePrem * (100 - telemetryDiscountPercent) / 100.0).roundToInt().coerceAtLeast(25)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Telemetry Rate Discount & Direct Policies", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SafeEmerald)
                        Surface(color = Color(0xFFD1FAE5), shape = RoundedCornerShape(50)) {
                            Text(
                                text = "-$telemetryDiscountPercent% Earned",
                                style = MaterialTheme.typography.labelLarge,
                                color = SafeEmerald,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = "Your verified property updates, field pins & mitigation outcomes earn insurers live risk clarity—lowering estimated premiums from $$basePrem/mo to $$discountedPrem/mo.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "homeowners_ho3" to "Homeowners HO-3",
                            "flood_nfip" to "Flood Policy",
                            "wildfire_dic" to "Wildfire / DIC",
                            "landlord" to "Landlord"
                        ).forEach { (k, label) ->
                            FilterChip(selected = quoteCoverageType == k, onClick = { quoteCoverageType = k }, label = { Text(label) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = quotePropertyLabel, onValueChange = { quotePropertyLabel = it }, label = { Text("Property") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = quoteLimitText, onValueChange = { quoteLimitText = it.filter { c -> c.isDigit() } }, label = { Text("Limit $") }, singleLine = true, modifier = Modifier.weight(0.9f))
                        OutlinedTextField(value = quoteBasePremiumText, onValueChange = { quoteBasePremiumText = it.filter { c -> c.isDigit() } }, label = { Text("Base $/mo") }, singleLine = true, modifier = Modifier.weight(0.75f))
                    }
                    OutlinedTextField(
                        value = quoteTerms,
                        onValueChange = { quoteTerms = it },
                        label = { Text("Property updates, roof/pipe mitigation or policy notes") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val isCarrier = myCarrierProfile != null
                            viewModel.requestOrIssueInsurancePolicy(
                                isCarrierIssuingQuote = isCarrier,
                                insurerUserId = myCarrierProfile?.userId ?: (selectedCarrier?.userId ?: "open_quote"),
                                carrierName = myCarrierProfile?.carrierName ?: customCarrierName.ifBlank { selectedCarrier?.carrierName ?: "Verified Carrier Marketplace" },
                                targetClientUserId = if (isCarrier) "open_quote" else viewModel.currentUserId,
                                targetClientName = if (isCarrier) "Open Discounted Homeowner" else "",
                                propertyLabel = quotePropertyLabel.ifBlank { "Primary Property" },
                                city = defaultCity,
                                coverageType = quoteCoverageType,
                                coverageLimitUsd = quoteLimitText.toIntOrNull() ?: 450000,
                                deductibleUsd = deductibleText.toIntOrNull() ?: 1000,
                                baseMonthlyPremiumUsd = basePrem,
                                telemetryDiscountPercent = telemetryDiscountPercent,
                                policyTermsSummary = quoteTerms.ifBlank { "Live property updates verified on Insights ($telemetryDiscountPercent% telemetry discount applied)." }
                            )
                            quoteTerms = ""
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("request_or_issue_policy_button")
                    ) {
                        Text(if (myCarrierProfile != null) "Issue Direct Policy Quote ($$discountedPrem/mo)" else "Request -$telemetryDiscountPercent% Discounted In-App Quote")
                    }

                    if (mergedPolicies.isNotEmpty()) {
                        HorizontalDivider()
                        Text("Active Policy Quotes & Bound Policies (${mergedPolicies.size})", style = MaterialTheme.typography.labelLarge)
                        mergedPolicies.take(6).forEach { offer ->
                            val isClientOwner = offer.clientUserId == viewModel.currentUserId || offer.clientUserId == "open_quote"
                            val isInsurerOwner = offer.insurerUserId == viewModel.currentUserId
                            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${offer.carrierName} · ${offer.coverageType.uppercase()}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(offer.status.replace('_', ' ').uppercase(), style = MaterialTheme.typography.labelSmall, color = SafeEmerald, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = "${offer.propertyLabel} (${offer.city}) · $${offer.discountedMonthlyPremiumUsd}/mo (was $${offer.baseMonthlyPremiumUsd}/mo · -${offer.telemetryDiscountPercent}%)",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (offer.policyTermsSummary.isNotBlank()) {
                                        Text(offer.policyTermsSummary, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (isClientOwner && offer.status == "quoted") {
                                            Button(
                                                onClick = { viewModel.respondToInsurancePolicyOffer(offer.id, "client_accepted", asClientAccepting = true) },
                                                modifier = Modifier.weight(1f)
                                            ) { Text("Accept Policy") }
                                        }
                                        if (isInsurerOwner && (offer.status == "quoted" || offer.status == "client_accepted")) {
                                            FilledTonalButton(
                                                onClick = { viewModel.respondToInsurancePolicyOffer(offer.id, "bound", asClientAccepting = false) },
                                                modifier = Modifier.weight(1f)
                                            ) { Text("Bind Policy") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1. Client Link Policy Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Policy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Link Your Homeowners / Property Insurance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Connect your insurance carrier & claim reference once so contractors, IICRC plans, and your adjuster stay synced to do the job right the first time.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (carriers.isNotEmpty()) {
                        Text("Pick Registered Insurer (or type below)", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            carriers.forEach { c ->
                                FilterChip(
                                    selected = selectedCarrier?.userId == c.userId,
                                    onClick = {
                                        selectedCarrier = c
                                        customCarrierName = c.carrierName
                                    },
                                    label = { Text(c.carrierName) }
                                )
                            }
                            FilterChip(
                                selected = selectedCarrier == null,
                                onClick = { selectedCarrier = null },
                                label = { Text("Other Carrier") }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = customCarrierName,
                        onValueChange = { customCarrierName = it },
                        label = { Text("Insurance Company Name (e.g. State Farm / Pacific Shield)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("link_insurer_name_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = policyRef,
                            onValueChange = { policyRef = it },
                            label = { Text("Masked Policy Ref") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = claimNumber,
                            onValueChange = { claimNumber = it },
                            label = { Text("Claim #") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = deductibleText,
                            onValueChange = { deductibleText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Ded. ($)") },
                            singleLine = true,
                            modifier = Modifier.weight(0.75f)
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.linkClientInsurancePolicy(
                                carrierUid = selectedCarrier?.userId ?: "external_carrier",
                                carrierName = customCarrierName,
                                policyRef = policyRef,
                                claimNumber = claimNumber,
                                deductibleUsd = deductibleText.toIntOrNull() ?: 0
                            )
                        },
                        enabled = !isSubmitting && customCarrierName.trim().length >= 2,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_insurance_link_button")
                    ) {
                        Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Link Insurance to My Profile")
                    }
                }
            }
        }

        // 2. Start Shared Claim Coordination Thread (Client + Contractor + Insurer)
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Streamlined 3-Way Claim Workspace", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Open a shared claim bridge linking you, your chosen Contractor, and your Insurance Carrier to approve scopes fast.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (contractors.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            contractors.take(4).forEach { pro ->
                                FilterChip(
                                    selected = claimContractor?.userId == pro.userId,
                                    onClick = { claimContractor = pro },
                                    label = { Text(pro.displayName) }
                                )
                            }
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "water_restoration" to "Water Loss",
                            "mold_mitigation" to "Mold",
                            "smoke_damage" to "Smoke/Fire",
                            "groundwater" to "Groundwater"
                        ).forEach { (cat, label) ->
                            FilterChip(selected = claimLossCat == cat, onClick = { claimLossCat = cat }, label = { Text(label) })
                        }
                    }
                    OutlinedTextField(
                        value = claimNote,
                        onValueChange = { claimNote = it },
                        label = { Text("Loss details & message for Contractor + Adjuster") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    FilledTonalButton(
                        onClick = {
                            viewModel.openInsuranceClaimThread(
                                contractor = claimContractor,
                                insurerUid = selectedCarrier?.userId ?: (userProfile?.linkedInsuranceCarrierUid?.ifBlank { "external_carrier" } ?: "external_carrier"),
                                insurerName = customCarrierName.ifBlank { userProfile?.linkedInsuranceCarrierName.orEmpty() },
                                claimNumber = claimNumber.ifBlank { policyRef.ifBlank { "NEW-CLAIM" } },
                                policyType = claimPolicyType,
                                deductibleUsd = deductibleText.toIntOrNull() ?: 1000,
                                lossCategory = claimLossCat,
                                initialNote = claimNote
                            )
                            claimNote = ""
                        },
                        enabled = !isSubmitting && customCarrierName.trim().length >= 2,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Streamlined 3-Way Claim")
                    }
                }
            }
        }

        // 3. Active Claim Threads
        item {
            Text("My Streamlined Insurance Claims (${claims.size})", style = MaterialTheme.typography.titleMedium)
        }
        items(claims, key = { it.id }) { claim ->
            var replyText by remember(claim.id) { mutableStateOf("") }
            var scopeUpdateText by remember(claim.id) { mutableStateOf(claim.contractorSuggestion) }
            var estUsdText by remember(claim.id) { mutableStateOf(claim.estimatedScopeUsd.toString()) }
            var approveUsdText by remember(claim.id) { mutableStateOf(claim.estimatedScopeUsd.coerceAtLeast(claim.approvedCoverageUsd).toString()) }

            val myUid = viewModel.currentUserId
            val senderRole = when (myUid) {
                claim.insurerUserId -> "insurer"
                claim.contractorUserId -> "contractor"
                else -> "client"
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${claim.insurerName} · Claim #${claim.claimNumber}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Client: ${claim.clientName} · Pro: ${claim.contractorName.ifBlank { "Unassigned" }} · Ded: $${claim.deductibleUsd}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = if (claim.claimStatus == "approved_direct_work") Color(0xFFD1FAE5) else MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = claim.claimStatus.replace('_', ' ').uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (claim.claimStatus == "approved_direct_work") SafeEmerald else MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (claim.contractorSuggestion.isNotBlank() || claim.estimatedScopeUsd > 0 || claim.approvedCoverageUsd > 0) {
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (claim.contractorSuggestion.isNotBlank()) {
                                    Text("Contractor Suggestion & Scope: ${claim.contractorSuggestion}", style = MaterialTheme.typography.bodySmall)
                                }
                                Text(
                                    text = "Contractor Estimate: $${claim.estimatedScopeUsd} · Insurer Approved Coverage: $${claim.approvedCoverageUsd}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (claim.approvedCoverageUsd > 0) SafeEmerald else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    claim.communicationLog.forEach { msg ->
                        Text("• $msg", style = MaterialTheme.typography.bodySmall)
                    }

                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        label = { Text("Message ($senderRole)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (senderRole == "contractor") {
                        OutlinedTextField(
                            value = scopeUpdateText,
                            onValueChange = { scopeUpdateText = it },
                            label = { Text("Push Updated IICRC Scope / Suggestion to Insurer") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = estUsdText,
                                onValueChange = { estUsdText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Est. ($)") },
                                singleLine = true,
                                modifier = Modifier.weight(0.6f)
                            )
                            Button(
                                onClick = {
                                    viewModel.postInsuranceClaimMessage(
                                        claim = claim,
                                        senderRole = "contractor",
                                        message = replyText.ifBlank { "Updated restoration scope ($${estUsdText.toIntOrNull() ?: claim.estimatedScopeUsd})." },
                                        nextStatus = "contractor_scoped",
                                        updatedSuggestion = scopeUpdateText,
                                        updatedEstimateUsd = estUsdText.toIntOrNull() ?: claim.estimatedScopeUsd
                                    )
                                    replyText = ""
                                },
                                enabled = !isSubmitting,
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) { Text("Send Scope to Insurer") }
                        }
                    } else if (senderRole == "insurer") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = approveUsdText,
                                onValueChange = { approveUsdText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Approve ($)") },
                                singleLine = true,
                                modifier = Modifier.weight(0.6f)
                            )
                            Button(
                                onClick = {
                                    val amt = approveUsdText.toIntOrNull() ?: claim.estimatedScopeUsd
                                    viewModel.postInsuranceClaimMessage(
                                        claim = claim,
                                        senderRole = "insurer",
                                        message = replyText.ifBlank { "Approved $$amt direct restoration work." },
                                        nextStatus = "approved_direct_work",
                                        approvedCoverageUsd = amt
                                    )
                                    replyText = ""
                                },
                                enabled = !isSubmitting,
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) { Text("Approve Coverage") }
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.postInsuranceClaimMessage(
                                    claim = claim,
                                    senderRole = senderRole,
                                    message = replyText
                                )
                                replyText = ""
                            },
                            enabled = !isSubmitting && replyText.trim().length >= 2,
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) { Text("Send Update") }
                    }
                }
            }
        }

        // 4. Registered Insurance Companies + Adjuster Onboarding Form
        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text("Registered Insurance Companies (${carriers.size})", style = MaterialTheme.typography.titleMedium)
        }
        items(carriers, key = { "car_${it.userId}" }) { car ->
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(car.carrierName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (car.supportsDirectRestorationBilling) {
                            Surface(color = Color(0xFFD1FAE5), shape = RoundedCornerShape(50)) {
                                Text("DIRECT BILLING · ${car.avgTurnaroundHours}h SLA", style = MaterialTheme.typography.labelSmall, color = SafeEmerald, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }
                    }
                    Text("Adjuster: ${car.adjusterName} · NAIC/Lic #${car.licenseOrNaicCode}", style = MaterialTheme.typography.labelSmall)
                    if (car.bio.isNotBlank()) Text(car.bio, style = MaterialTheme.typography.bodySmall)
                    if (car.claimsPortalUrl.startsWith("http")) {
                        Text(
                            text = "↗ Claims Portal: ${car.claimsPortalUrl}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable { runCatching { uriHandler.openUri(car.claimsPortalUrl) } }
                        )
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = if (myCarrierProfile == null) "Join as Insurance Company / Claims Adjuster" else "Update Insurance Carrier Profile",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedTextField(value = insName, onValueChange = { insName = it }, label = { Text("Insurance Company Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = insAdjuster, onValueChange = { insAdjuster = it }, label = { Text("Adjuster Name") }, singleLine = true, modifier = Modifier.weight(1.2f))
                        OutlinedTextField(value = insNaic, onValueChange = { insNaic = it }, label = { Text("NAIC / CA Lic #") }, singleLine = true, modifier = Modifier.weight(0.8f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = insEmail, onValueChange = { insEmail = it }, label = { Text("Claims Email") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = insPhone, onValueChange = { insPhone = it }, label = { Text("Claims Phone") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = insPortal, onValueChange = { insPortal = it }, label = { Text("Claims Portal URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = insRegions, onValueChange = { insRegions = it }, label = { Text("Regions (comma sep.)") }, singleLine = true, modifier = Modifier.weight(1.3f))
                        OutlinedTextField(value = insTurnaround, onValueChange = { insTurnaround = it.filter { c -> c.isDigit() } }, label = { Text("SLA Hrs") }, singleLine = true, modifier = Modifier.weight(0.7f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Supports Direct Restoration Billing", style = MaterialTheme.typography.bodySmall)
                        Switch(checked = insDirectBilling, onCheckedChange = { insDirectBilling = it })
                    }
                    OutlinedTextField(value = insBio, onValueChange = { insBio = it }, label = { Text("Claims guidelines & IICRC direct approval policy") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            viewModel.registerInsuranceCarrier(
                                carrierName = insName,
                                adjusterName = insAdjuster,
                                licenseOrNaicCode = insNaic,
                                claimsEmail = insEmail,
                                claimsPhone = insPhone,
                                claimsPortalUrl = insPortal,
                                serviceRegionsRaw = insRegions,
                                supportsDirectRestorationBilling = insDirectBilling,
                                avgTurnaroundHours = insTurnaround.toIntOrNull() ?: 24,
                                bio = insBio
                            )
                        },
                        enabled = !isSubmitting && insName.trim().length >= 2 && insAdjuster.trim().length >= 2,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(if (myCarrierProfile == null) "Publish Insurance Company Profile" else "Save Insurance Profile")
                    }
                }
            }
        }
    }
}
