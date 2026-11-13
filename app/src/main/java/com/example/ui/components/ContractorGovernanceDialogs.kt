package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.export.WorkAuthorizationPdfGenerator
import com.example.data.model.ConsultationRequest
import com.example.data.model.ContractorComplaint
import com.example.data.model.ContractorMatchResult
import com.example.data.model.ContractorPortfolioItem
import com.example.data.model.ContractorProfile
import com.example.data.model.ContractorReview
import com.example.ui.MediaAttachment
import com.example.ui.theme.FloodAmber
import com.example.ui.theme.FloodCrimson
import com.example.ui.theme.SafeEmerald
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContractorTransparentProfileDialog(
    pro: ContractorProfile,
    match: ContractorMatchResult?,
    portfolioItems: List<ContractorPortfolioItem>,
    reviews: List<ContractorReview>,
    complaints: List<ContractorComplaint>,
    onFileComplaint: () -> Unit,
    onPostHonestReview: () -> Unit,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val proComplaints = complaints.filter {
        it.contractorUserId == pro.userId || it.contractorName.equals(pro.displayName, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("${pro.displayName} · Transparent Dossier", style = MaterialTheme.typography.titleMedium)
                if (pro.companyName.isNotBlank()) {
                    Text(pro.companyName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.height(440.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Proven Experience & Guarantee Banner
                if (match != null && (match.guaranteeEligible || match.provenSimilarJobsCount > 0)) {
                    item {
                        Surface(
                            color = if (match.guaranteeEligible) Color(0xFFD1FAE5) else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = match.guaranteeBadgeLabel.ifBlank { "Verified Job History" },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (match.guaranteeEligible) SafeEmerald else MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${match.provenSimilarJobsCount} verified field jobs · ${match.verifiedSuccessRatePercent}% success rate",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Southern California / CA CSLB Board Verification
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Text("CSLB License & Board Check", style = MaterialTheme.typography.labelLarge)
                            }
                            Text(
                                text = "Lic #${pro.licenseNumber} (${pro.licensingState}) · Board Status: ${pro.cslbBoardStatus.replace('_', ' ').uppercase()}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            val lookupUrl = pro.cslbLookupUrl.ifBlank { WaterTableCatalogs.buildCslbLookupUrl(pro.licenseNumber) }
                            OutlinedButton(
                                onClick = { runCatching { uriHandler.openUri(lookupUrl) } },
                                modifier = Modifier.fillMaxWidth().height(44.dp)
                            ) {
                                Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Inspect Live on CA CSLB Board")
                            }
                        }
                    }
                }

                // Live Scan Safety & Home-Entry Clearance
                item {
                    val isCleared = pro.liveScanStatus == "verified_cleared"
                    val isPending = pro.liveScanStatus == "submitted_ati"
                    Surface(
                        color = when {
                            isCleared -> Color(0xFFD1FAE5)
                            isPending -> Color(0xFFFEF3C7)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    Icons.Filled.Security,
                                    contentDescription = null,
                                    tint = if (isCleared) SafeEmerald else FloodAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Live Scan Home-Safe Clearance: ${pro.liveScanStatus.replace('_', ' ').uppercase()}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isCleared) SafeEmerald else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (pro.liveScanAtiRef.isNotBlank() || pro.liveScanAgency.isNotBlank()) {
                                Text(
                                    text = "ATI Ref: ${pro.liveScanAtiRef.ifBlank { "N/A" }} · Agency: ${pro.liveScanAgency.ifBlank { "CA DOJ Applicant" }} ${if (pro.liveScanClearedDate.isNotBlank()) "(${pro.liveScanClearedDate})" else ""}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                Text(
                                    text = "No DOJ Live Scan ATI clearance uploaded yet.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (pro.liveScanProofBase64.isNotBlank()) {
                                Base64MediaPreview(
                                    base64 = pro.liveScanProofBase64,
                                    mediaType = "photo",
                                    videoDurationLabel = "",
                                    modifier = Modifier.fillMaxWidth().height(110.dp)
                                )
                            }
                            OutlinedButton(
                                onClick = { runCatching { uriHandler.openUri(WaterTableCatalogs.DOJ_LIVE_SCAN_STATUS_URL) } },
                                modifier = Modifier.fillMaxWidth().height(44.dp)
                            ) {
                                Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Verify ATI on CA DOJ Live Scan")
                            }
                        }
                    }
                }

                // IICRC Credential Section
                if (pro.iicrcCertNumber.isNotBlank() || pro.isIicrcGovernor) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (pro.isIicrcGovernor) "IICRC Technical Governor · Cert #${pro.iicrcCertNumber}" else "IICRC Certified Restoration · #${pro.iicrcCertNumber}",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = "Verify active IICRC certification directly on the IICRC Global Locator.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                OutlinedButton(
                                    onClick = { runCatching { uriHandler.openUri(WaterTableCatalogs.IICRC_LOCATOR_URL) } },
                                    modifier = Modifier.fillMaxWidth().height(42.dp)
                                ) {
                                    Text("Open IICRC Locator")
                                }
                            }
                        }
                    }
                }

                // Portfolio & Referrals
                item {
                    Text("Work Portfolio & Client Referrals (${portfolioItems.size})", style = MaterialTheme.typography.titleSmall)
                }
                if (portfolioItems.isEmpty()) {
                    item { Text("No portfolio photos uploaded yet.", style = MaterialTheme.typography.bodySmall) }
                } else {
                    items(portfolioItems, key = { it.id }) { item ->
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${item.entryType.uppercase()}: ${item.title}", style = MaterialTheme.typography.labelLarge)
                                if (item.photoBase64.isNotBlank()) {
                                    Base64MediaPreview(item.photoBase64, "photo", "", Modifier.fillMaxWidth().height(110.dp))
                                }
                                Text(item.description, style = MaterialTheme.typography.bodySmall)
                                if (item.clientReferenceName.isNotBlank()) {
                                    Text("Ref: ${item.clientReferenceName} · ${item.city} (${item.completionYear})", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                // Honest Reviews + Formal Board Complaints
                item {
                    Text("Glass Wall Reviews (${reviews.size}) & Board Complaints (${proComplaints.size})", style = MaterialTheme.typography.titleSmall)
                }
                items(reviews, key = { "rev_${it.id}" }) { rev ->
                    Text(
                        text = "• ${rev.overallRating}★ Overall / ${rev.communicationRating}★ Comm: ${rev.honestExperience}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                items(proComplaints, key = { "cmp_${it.id}" }) { cmp ->
                    Surface(color = Color(0xFFFEE2E2), shape = RoundedCornerShape(10.dp)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "CSLB${if (cmp.routedToIicrc) " + IICRC" else ""} Complaint · ${cmp.complaintCategory.replace('_', ' ').uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = FloodCrimson,
                                fontWeight = FontWeight.Bold
                            )
                            Text(cmp.summary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onFileComplaint,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FloodCrimson)
                ) {
                    Text("File Complaint")
                }
                Button(onClick = onPostHonestReview) {
                    Text("Post Review")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun WorkAuthorizationDialog(
    consultation: ConsultationRequest,
    contractor: ContractorProfile?,
    isClient: Boolean,
    isSubmitting: Boolean,
    onSign: (String) -> Unit,
    onExportPdf: () -> File?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var signatureInput by remember { mutableStateOf("") }

    val authText = remember(consultation, contractor) {
        if (consultation.workAuthTerms.isNotBlank()) {
            consultation.workAuthTerms
        } else {
            WorkAuthorizationPdfGenerator.buildDefaultAuthorizationText(
                consultation = consultation,
                contractor = contractor,
                scopeText = consultation.engineerRecommendation,
                strategyText = consultation.contractorStrategy,
                estimateUsd = consultation.proposedWorkEstimateUsd,
                paymentScheduleSummary = consultation.paymentScheduleSummary.ifBlank {
                    WorkAuthorizationPdfGenerator.buildDefaultPaymentSchedule(consultation.proposedWorkEstimateUsd)
                }
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("General Work Auth & Payments", style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.height(400.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    val authorized = consultation.workAuthStatus == "client_authorized"
                    Surface(
                        color = if (authorized) Color(0xFFD1FAE5) else Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (authorized) {
                                "CLIENT AUTHORIZED · Signed by ${consultation.clientSignatureName} (${consultation.clientAuthorizedAtLabel})"
                            } else {
                                "STATUS: ${consultation.workAuthStatus.replace('_', ' ').uppercase()}"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (authorized) SafeEmerald else FloodAmber,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = authText,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                if (isClient && consultation.workAuthStatus == "drafted") {
                    item {
                        OutlinedTextField(
                            value = signatureInput,
                            onValueChange = { signatureInput = it },
                            label = { Text("Type Your Full Legal Name to Authorize Work") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("work_auth_signature_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onSign(signatureInput) },
                            enabled = !isSubmitting && signatureInput.trim().length >= 2,
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("work_auth_sign_button")
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Authorize Work & Certified Milestones")
                        }
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    val file = onExportPdf() ?: return@FilledTonalButton
                    sharePdfFile(context, file, "General Work Authorization - ${consultation.city}")
                }
            ) {
                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share / Save PDF")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FileComplaintDialog(
    preselectedPro: ContractorProfile?,
    defaultCity: String,
    proofAttachment: MediaAttachment?,
    latestFiledComplaint: ContractorComplaint?,
    isSubmitting: Boolean,
    onPickProof: (android.net.Uri, Context) -> Unit,
    onClearProof: () -> Unit,
    onExportComplaintPdf: (ContractorComplaint) -> File?,
    onSubmitComplaint: (
        contractorUid: String,
        contractorName: String,
        companyName: String,
        licenseNumber: String,
        iicrcCertNumber: String,
        isRestorationCompany: Boolean,
        city: String,
        category: String,
        summary: String,
        evidenceNotes: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    var contractorName by remember(preselectedPro) { mutableStateOf(preselectedPro?.displayName ?: "") }
    var companyName by remember(preselectedPro) { mutableStateOf(preselectedPro?.companyName ?: "") }
    var licenseNumber by remember(preselectedPro) { mutableStateOf(preselectedPro?.licenseNumber ?: "") }
    var iicrcCertNumber by remember(preselectedPro) { mutableStateOf(preselectedPro?.iicrcCertNumber ?: "") }
    var isRestorationCompany by remember(preselectedPro) {
        mutableStateOf(
            preselectedPro?.specialty in listOf(
                "water_mitigation", "mold_remediation", "smoke_fire_restoration", "full_service_restoration"
            ) || preselectedPro?.iicrcCertNumber?.isNotBlank() == true
        )
    }
    var city by remember(defaultCity) { mutableStateOf(defaultCity.ifBlank { "Los Angeles" }) }
    var complaintCategory by remember { mutableStateOf("poor_workmanship") }
    var summary by remember { mutableStateOf("") }
    var evidenceNotes by remember { mutableStateOf("") }

    val proofLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) onPickProof(uri, context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Gavel, contentDescription = null, tint = FloodCrimson)
                Text(
                    if (latestFiledComplaint != null) "Instant Dispatch: SoCal CSLB & IICRC" else "File Contractor Complaint",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            if (latestFiledComplaint != null) {
                val dossier = remember(latestFiledComplaint) {
                    WorkAuthorizationPdfGenerator.buildComplaintDossierText(latestFiledComplaint)
                }
                LazyColumn(
                    modifier = Modifier.height(410.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Surface(color = Color(0xFFD1FAE5), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Complaint Dossier Logged & Escalated", style = MaterialTheme.typography.labelLarge, color = SafeEmerald, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Prepared for Southern California CSLB${if (latestFiledComplaint.routedToIicrc) " AND forwarded to IICRC Restoration Governance" else ""}. Tap below to submit straight into official intake.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    item {
                        OutlinedButton(
                            onClick = {
                                copyTextToClipboard(context, "CSLB Complaint Dossier", dossier)
                            },
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Full Dossier to Paste in Portal")
                        }
                    }
                    item {
                        Button(
                            onClick = {
                                copyTextToClipboard(context, "CSLB Complaint Dossier", dossier)
                                runCatching { uriHandler.openUri(WaterTableCatalogs.CSLB_COMPLAINT_URL) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FloodCrimson),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open SoCal CA CSLB Complaint Portal")
                        }
                    }
                    if (latestFiledComplaint.routedToIicrc) {
                        item {
                            Button(
                                onClick = {
                                    copyTextToClipboard(context, "IICRC Complaint Dossier", dossier)
                                    runCatching { uriHandler.openUri(WaterTableCatalogs.IICRC_COMPLAINT_URL) }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open IICRC Restoration Complaint Portal")
                            }
                        }
                    }
                    item {
                        FilledTonalButton(
                            onClick = {
                                val file = onExportComplaintPdf(latestFiledComplaint) ?: return@FilledTonalButton
                                sharePdfFile(context, file, "CSLB & IICRC Complaint - ${latestFiledComplaint.contractorName}")
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send / Email Formal Complaint PDF")
                        }
                    }
                    item {
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)) {
                            Text(dossier, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.height(410.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "Generates a formal complaint dossier routed to the Southern California CSLB portal — plus IICRC Oversight if it is a restoration company.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = contractorName,
                            onValueChange = { contractorName = it },
                            label = { Text("Contractor Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = companyName,
                            onValueChange = { companyName = it },
                            label = { Text("Company Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = licenseNumber,
                                onValueChange = { licenseNumber = it },
                                label = { Text("CSLB Lic #") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = city,
                                onValueChange = { city = it },
                                label = { Text("SoCal / City") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Restoration Co.? (Copy to IICRC)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            Switch(checked = isRestorationCompany, onCheckedChange = { isRestorationCompany = it })
                        }
                        if (isRestorationCompany) {
                            OutlinedTextField(
                                value = iicrcCertNumber,
                                onValueChange = { iicrcCertNumber = it },
                                label = { Text("IICRC Cert # (if known)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Text("Violation Category", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                "poor_workmanship" to "Shoddy Work",
                                "abandonment" to "Abandonment",
                                "unlicensed_work" to "Unlicensed",
                                "billing_fraud" to "Billing Fraud",
                                "iicrc_violation" to "IICRC Violation",
                                "safety_hazard" to "Safety Hazard"
                            ).forEach { (key, label) ->
                                FilterChip(
                                    selected = complaintCategory == key,
                                    onClick = { complaintCategory = key },
                                    label = { Text(label) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = summary,
                            onValueChange = { summary = it },
                            label = { Text("Detailed factual complaint (dates, contract violations, damage)") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = evidenceNotes,
                            onValueChange = { evidenceNotes = it },
                            label = { Text("Invoices, permits, messages, moisture log notes") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        proofAttachment?.previewBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Complaint proof photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(10.dp))
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { proofLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (proofAttachment == null) "Attach Photo Proof" else "Change Photo")
                            }
                            if (proofAttachment != null) {
                                TextButton(onClick = onClearProof) { Text("Clear") }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (latestFiledComplaint == null) {
                Button(
                    onClick = {
                        onSubmitComplaint(
                            preselectedPro?.userId ?: "external",
                            contractorName,
                            companyName,
                            licenseNumber,
                            iicrcCertNumber,
                            isRestorationCompany,
                            city,
                            complaintCategory,
                            summary,
                            evidenceNotes
                        )
                    },
                    enabled = !isSubmitting && contractorName.trim().length >= 2 && summary.trim().length >= 10,
                    colors = ButtonDefaults.buttonColors(containerColor = FloodCrimson)
                ) {
                    Text("Submit & Route to Boards")
                }
            } else {
                Button(onClick = onDismiss) { Text("Done") }
            }
        },
        dismissButton = {
            if (latestFiledComplaint == null) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

fun sharePdfFile(context: Context, file: File, title: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, title))
}

private fun copyTextToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
}
