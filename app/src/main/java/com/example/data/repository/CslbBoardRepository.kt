package com.example.data.repository

import com.example.data.catalog.WaterTableCatalogs
import com.example.data.model.CslbVerificationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class CslbBoardRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Checks a California / Southern California contractor license against the public
     * Contractors State License Board (CSLB) lookup endpoint and returns the direct official URL.
     */
    suspend fun verifyLicenseWithBoard(
        licenseNumber: String,
        licensingState: String
    ): CslbVerificationResult = withContext(Dispatchers.IO) {
        val trimmed = licenseNumber.trim()
        val digits = trimmed.filter { it.isDigit() }
        val stateUpper = licensingState.trim().uppercase()
        val lookupUrl = WaterTableCatalogs.buildCslbLookupUrl(trimmed)

        if (digits.length < 5) {
            return@withContext CslbVerificationResult(
                licenseNumber = trimmed,
                status = "unverified",
                summary = "Enter a 6–8 digit California CSLB or valid state contractor/engineering license # to verify.",
                classification = "",
                lookupUrl = WaterTableCatalogs.CSLB_SEARCH_URL
            )
        }

        if (stateUpper != "CA" && stateUpper != "CALIFORNIA" && stateUpper.isNotBlank()) {
            return@withContext CslbVerificationResult(
                licenseNumber = trimmed,
                status = "format_checked",
                summary = "Out-of-state credential ($stateUpper #$trimmed). Use CSLB link if licensed in Southern California.",
                classification = stateUpper,
                lookupUrl = lookupUrl
            )
        }

        return@withContext runCatching {
            val request = Request.Builder()
                .url(lookupUrl)
                .header("User-Agent", "WaterTableTransparencyApp/1.0")
                .build()
            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                val upperBody = body.uppercase()
                when {
                    resp.isSuccessful && upperBody.contains("THIS LICENSE IS CURRENT AND ACTIVE") -> {
                        val clsMatch = Regex("""\b(A|B|C-\d{1,2}|HAZ)\b""").find(upperBody)?.value.orEmpty()
                        CslbVerificationResult(
                            licenseNumber = digits,
                            status = "cslb_verified",
                            summary = "Live CSLB Check: Active & in good standing on CA Contractors State License Board.",
                            classification = clsMatch,
                            lookupUrl = lookupUrl
                        )
                    }
                    resp.isSuccessful && (upperBody.contains("REVOKED") || upperBody.contains("SUSPENDED") || upperBody.contains("EXPIRED")) -> {
                        CslbVerificationResult(
                            licenseNumber = digits,
                            status = "flagged",
                            summary = "CSLB Record Warning: License returned Expired/Suspended/Revoked notice — tap CSLB Link to inspect.",
                            classification = "",
                            lookupUrl = lookupUrl
                        )
                    }
                    else -> {
                        CslbVerificationResult(
                            licenseNumber = digits,
                            status = "format_checked",
                            summary = "CA CSLB # format valid ($digits). Tap 'Open CSLB Board' to inspect live bond/workers-comp record.",
                            classification = "CA",
                            lookupUrl = lookupUrl
                        )
                    }
                }
            }
        }.getOrElse {
            CslbVerificationResult(
                licenseNumber = digits,
                status = "format_checked",
                summary = "CSLB check link ready for #$digits. Tap to inspect official CA Contractors State License Board record.",
                classification = "CA",
                lookupUrl = lookupUrl
            )
        }
    }
}
