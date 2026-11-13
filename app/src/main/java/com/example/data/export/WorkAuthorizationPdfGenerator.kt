package com.example.data.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.model.ConsultationRequest
import com.example.data.model.ContractorComplaint
import com.example.data.model.ContractorProfile
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WorkAuthorizationPdfGenerator {

    fun buildDefaultAuthorizationText(
        consultation: ConsultationRequest,
        contractor: ContractorProfile?,
        scopeText: String,
        strategyText: String,
        estimateUsd: Int,
        paymentScheduleSummary: String
    ): String {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val licenseLine = contractor?.let {
            "Lic #${it.licenseNumber} (${it.licensingState}) · CSLB: ${it.cslbBoardStatus} · Live Scan: ${it.liveScanStatus}"
        } ?: "Licensed Contractor #${consultation.contractorUserId.take(8)}"

        return """
            GENERAL WORK AUTHORIZATION & CERTIFIED PAYMENT SCHEDULE
            Date Issued: $dateStr
            Location / City: ${consultation.city} | Case Ref: ${consultation.id.ifBlank { consultation.reportId }}
            Client / Property Owner: ${consultation.clientName}
            Contractor: ${consultation.contractorName} ${contractor?.companyName?.takeIf { it.isNotBlank() }?.let { "($it)" }.orEmpty()}
            Verification: $licenseLine

            1. AUTHORIZED SCOPE OF WORK (${consultation.lossCategory.uppercase()}):
            ${scopeText.ifBlank { consultation.engineerRecommendation }}

            2. EXECUTION & SAFETY STRATEGY:
            ${strategyText.ifBlank { "Follow ANSI/IICRC & engineering standards, document daily moisture/depth readings, and photograph progress." }}

            3. CERTIFIED MILESTONE PAYMENT SCHEDULE (Est. Work Total: $${estimateUsd}):
            $paymentScheduleSummary
            (Remote Platform Advice Fee already itemized: $${consultation.contractorFeeUsd} Pro + $${consultation.platformFeeUsd} Platform = $${consultation.totalFeeUsd})

            4. TRANSPARENCY & HONESTY TERMS:
            Change orders require written client consent. Final certified payment is released only after walkthrough sign-off and moisture/drainage verification.
        """.trimIndent()
    }

    fun buildDefaultPaymentSchedule(estimateUsd: Int): String {
        val workTotal = estimateUsd.coerceAtLeast(0)
        if (workTotal <= 0) {
            return "Remote Advice Only ($0 On-Site Work) — Certified upon scope delivery."
        }
        val deposit = (workTotal * 0.20).toInt()
        val mid = (workTotal * 0.40).toInt()
        val finalPay = (workTotal - deposit - mid).coerceAtLeast(0)
        return "Milestone 1 (20% Mobilization Deposit): $$deposit | Milestone 2 (40% Containment/Drainage Rough-In): $$mid | Milestone 3 (40% Final Sign-Off & Clearance): $$finalPay"
    }

    fun exportWorkAuthPdf(
        context: Context,
        consultation: ConsultationRequest,
        contractor: ContractorProfile?
    ): File {
        val dir = File(context.cacheDir, "work_authorizations").apply { mkdirs() }
        val safeId = consultation.id.filter { it.isLetterOrDigit() || it == '_' }.ifBlank { "auth" }
        val outFile = File(dir, "work_auth_$safeId.pdf")

        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(612, 792, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(12, 74, 110)
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val headingPaint = Paint().apply {
            color = Color.rgb(15, 118, 110)
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 10f
            isAntiAlias = true
        }

        var y = 48f
        canvas.drawText("WATERTABLE · GENERAL WORK AUTHORIZATION", 42f, y, titlePaint)
        y += 20f
        canvas.drawLine(42f, y, 570f, y, headingPaint)
        y += 20f

        val fullBody = buildString {
            if (consultation.workAuthTerms.isNotBlank()) {
                append(consultation.workAuthTerms)
            } else {
                append(
                    buildDefaultAuthorizationText(
                        consultation = consultation,
                        contractor = contractor,
                        scopeText = consultation.engineerRecommendation,
                        strategyText = consultation.contractorStrategy,
                        estimateUsd = consultation.proposedWorkEstimateUsd,
                        paymentScheduleSummary = consultation.paymentScheduleSummary
                            .ifBlank { buildDefaultPaymentSchedule(consultation.proposedWorkEstimateUsd) }
                    )
                )
            }
            if (consultation.clientSignatureName.isNotBlank()) {
                append("\n\n5. CLIENT AUTHORIZATION SIGNATURE:\nSigned by: ${consultation.clientSignatureName}\nAuthorized At: ${consultation.clientAuthorizedAtLabel}")
            } else {
                append("\n\n5. CLIENT AUTHORIZATION SIGNATURE:\n[Pending Client Signature]")
            }
        }

        fullBody.lines().forEach { rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) {
                y += 8f
                return@forEach
            }
            val isHeader = trimmed.startsWith("1.") || trimmed.startsWith("2.") ||
                trimmed.startsWith("3.") || trimmed.startsWith("4.") || trimmed.startsWith("5.")
            val paintToUse = if (isHeader) headingPaint else bodyPaint
            val wrapped = wrapLine(trimmed, 82)
            wrapped.forEach { wLine ->
                if (y < 755f) {
                    canvas.drawText(wLine, 42f, y, paintToUse)
                    y += if (isHeader) 15f else 13f
                }
            }
        }

        pdf.finishPage(page)
        FileOutputStream(outFile).use { fos ->
            pdf.writeTo(fos)
        }
        pdf.close()
        return outFile
    }

    private fun wrapLine(text: String, maxChars: Int): List<String> {
        if (text.length <= maxChars) return listOf(text)
        val out = mutableListOf<String>()
        var remaining = text
        while (remaining.length > maxChars) {
            val splitAt = remaining.lastIndexOf(' ', maxChars).let { if (it <= 10) maxChars else it }
            out.add(remaining.substring(0, splitAt).trim())
            remaining = remaining.substring(splitAt).trim()
        }
        if (remaining.isNotEmpty()) out.add(remaining)
        return out
    }

    fun buildComplaintDossierText(complaint: ContractorComplaint): String {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        return """
            FORMAL CONTRACTOR COMPLAINT PACKET
            Filing Date: $dateStr | Ref: ${complaint.id.ifBlank { "DRAFT" }}
            Complainant: ${complaint.complainantName}
            Jurisdiction: Southern California / California CSLB (${complaint.city})
            ${if (complaint.routedToIicrc) "Restoration Ethics Copy: IICRC Consumer & Certification Review (${complaint.iicrcCertNumber.ifBlank { "Restoration Co." }})" else ""}

            1. SUBJECT CONTRACTOR / COMPANY:
            Name: ${complaint.contractorName}
            Company: ${complaint.companyName.ifBlank { "Not listed" }}
            License #: ${complaint.licenseNumber.ifBlank { "Unverified / Unlicensed" }}
            Restoration Company: ${if (complaint.isRestorationCompany) "YES (IICRC Governed)" else "No"}

            2. COMPLAINT CATEGORY:
            ${complaint.complaintCategory.replace('_', ' ').uppercase()}

            3. FACTUAL STATEMENT & WORK CHRONOLOGY:
            ${complaint.summary}

            4. EVIDENCE & DOCUMENTATION ATTACHED:
            ${complaint.evidenceNotes.ifBlank { "In-app report, photo logs, and Work Authorization/Review history." }}

            5. ROUTING & CONSUMER NOTICE:
            Independent community filing dossier prepared via WaterTable. Submit directly via CA CSLB Portal (${complaint.cslbIntakeUrl}) and IICRC (${complaint.iicrcIntakeUrl}).
        """.trimIndent()
    }

    fun exportComplaintPdf(context: Context, complaint: ContractorComplaint): File {
        val dir = File(context.cacheDir, "work_authorizations").apply { mkdirs() }
        val safeId = complaint.id.filter { it.isLetterOrDigit() || it == '_' }.ifBlank { "complaint" }
        val outFile = File(dir, "complaint_$safeId.pdf")

        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(612, 792, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(153, 27, 27)
            textSize = 14.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val headingPaint = Paint().apply {
            color = Color.rgb(30, 58, 138)
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 10f
            isAntiAlias = true
        }

        var y = 48f
        canvas.drawText("CSLB / IICRC FORMAL COMPLAINT DOSSIER", 42f, y, titlePaint)
        y += 20f
        canvas.drawLine(42f, y, 570f, y, headingPaint)
        y += 20f

        buildComplaintDossierText(complaint).lines().forEach { rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) {
                y += 8f
                return@forEach
            }
            val isHeader = trimmed.startsWith("1.") || trimmed.startsWith("2.") ||
                trimmed.startsWith("3.") || trimmed.startsWith("4.") || trimmed.startsWith("5.")
            val paintToUse = if (isHeader) headingPaint else bodyPaint
            wrapLine(trimmed, 82).forEach { wLine ->
                if (y < 755f) {
                    canvas.drawText(wLine, 42f, y, paintToUse)
                    y += if (isHeader) 15f else 13f
                }
            }
        }

        pdf.finishPage(page)
        FileOutputStream(outFile).use { fos -> pdf.writeTo(fos) }
        pdf.close()
        return outFile
    }
}
