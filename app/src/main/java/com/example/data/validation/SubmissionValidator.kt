package com.example.data.validation

data class SubmissionValidationResult(
    val isValid: Boolean,
    val completenessErrors: List<String>,
    val plausibilityWarnings: List<String>,
    val plausibilityScore: Int,
    val requiresModeration: Boolean,
    val moderationReason: String
)

data class AiModerationVerdict(
    val isSafeAndRelevant: Boolean,
    val plausibilityScore: Int,
    val summary: String,
    val flaggedReasons: List<String>
)

object SubmissionValidator {

    fun validateWaterOrLossReport(
        city: String,
        stateOrRegion: String,
        addressHint: String,
        latitude: Double,
        longitude: Double,
        depthFeet: Double,
        lossCategory: String,
        situationType: String,
        lossDescription: String,
        notes: String,
        hasPrimaryMedia: Boolean,
        extraPhotoCount: Int
    ): SubmissionValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        var score = 100

        // 1. Completeness Checks
        val cleanCity = city.trim()
        if (cleanCity.length < 2) {
            errors += "City or town name is required (at least 2 characters)."
            score -= 25
        } else if (cleanCity.any { it.isDigit() }) {
            warnings += "City name contains numbers — double-check spelling."
            score -= 10
        }

        if (stateOrRegion.trim().isEmpty()) {
            warnings += "State or region is blank; adding it helps regional water table mapping."
            score -= 5
        }

        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
            errors += "GPS coordinates are outside valid global latitude/longitude bounds."
            score -= 40
        } else if (latitude == 0.0 && longitude == 0.0) {
            errors += "Please stamp GPS or select a city so the report maps to a real location."
            score -= 30
        }

        val combinedNarrative = "${lossDescription.trim()} ${notes.trim()}".trim()
        val isDetailed = lossDescription.trim().length >= 50 || combinedNarrative.length >= 70 || extraPhotoCount > 0

        if (lossCategory != "groundwater" && lossDescription.trim().length < 20) {
            errors += "Please provide a full description of the water, mold, smoke, or fire loss (at least 20 characters) for restoration contractors."
            score -= 25
        } else if (combinedNarrative.length < 10) {
            errors += "Add brief field notes (at least 10 characters) describing what you observe."
            score -= 15
        }

        // Gibberish / spam repetition check
        if (combinedNarrative.length >= 10 && isLikelyGibberish(combinedNarrative)) {
            errors += "Field description looks repetitive or unclear. Please describe the actual site condition."
            score -= 35
        }

        // 2. Plausibility Checks
        if (depthFeet < 0.0 || depthFeet > 200.0) {
            errors += "Water table depth must be between 0.0 and 200.0 feet."
            score -= 30
        } else {
            if (situationType == "subsurface_test" && depthFeet == 0.0) {
                warnings += "Subsurface bore/dig reports normally have a depth > 0 ft."
                score -= 12
            }
            if (depthFeet > 90.0 && (situationType == "street_flooding" || situationType == "basement_intrusion")) {
                warnings += "Depth > 90 ft is unusually deep for street flooding or basement seepage — verify measurement."
                score -= 18
            }
            if (lossCategory == "groundwater" && depthFeet > 140.0) {
                warnings += "Reported groundwater depth (${depthFeet} ft) exceeds typical residential impact depth."
                score -= 10
            }
        }

        if (addressHint.trim().isEmpty()) {
            warnings += "No street or cross-street entered; nearby drivers benefit from an address hint."
            score -= 5
        }

        // 3. Moderation Trigger Rule:
        // All new photo/video uploads AND detailed reports hold in pending_review before public listing
        val requiresModeration = hasPrimaryMedia || extraPhotoCount > 0 || isDetailed
        val reason = when {
            (hasPrimaryMedia || extraPhotoCount > 0) && isDetailed ->
                "Photo upload & detailed loss report routed to pre-publication moderation queue."
            hasPrimaryMedia || extraPhotoCount > 0 ->
                "New photo/video upload held in moderation queue before public visibility."
            isDetailed ->
                "Detailed multi-sentence report held in moderation queue for plausibility verification."
            else ->
                "Standard GPS observation cleared completeness & plausibility checks."
        }

        return SubmissionValidationResult(
            isValid = errors.isEmpty(),
            completenessErrors = errors,
            plausibilityWarnings = warnings,
            plausibilityScore = score.coerceIn(0, 100),
            requiresModeration = requiresModeration,
            moderationReason = reason
        )
    }

    fun validateGlassWallReview(
        contractorName: String,
        city: String,
        overallRating: Int,
        communicationRating: Int,
        honestExperience: String,
        hasProofPhoto: Boolean,
        proofDescription: String
    ): SubmissionValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        var score = 100

        if (contractorName.trim().length < 2) {
            errors += "Contractor or company name is required."
            score -= 25
        }
        if (city.trim().length < 2) {
            errors += "City where the work happened is required."
            score -= 15
        }
        if (overallRating !in 1..5 || communicationRating !in 1..5) {
            errors += "Overall and communication ratings must be between 1 and 5."
            score -= 25
        }
        if (honestExperience.trim().length < 15) {
            errors += "Please describe your honest experience in at least 15 characters so others can see how they treated you."
            score -= 25
        }
        if (hasProofPhoto && proofDescription.trim().length < 5) {
            errors += "Add a short caption explaining what your attached proof photo shows."
            score -= 15
        }
        if (!hasProofPhoto) {
            warnings += "Tip: Attaching a photo or invoice screenshot boosts Glass Wall trust."
            score -= 5
        }
        if (isLikelyGibberish(honestExperience)) {
            errors += "Review text appears invalid or repetitive."
            score -= 30
        }

        return SubmissionValidationResult(
            isValid = errors.isEmpty(),
            completenessErrors = errors,
            plausibilityWarnings = warnings,
            plausibilityScore = score.coerceIn(0, 100),
            requiresModeration = false,
            moderationReason = ""
        )
    }

    private fun isLikelyGibberish(text: String): Boolean {
        val cleaned = text.trim()
        if (cleaned.length < 8) return false
        val uniqueChars = cleaned.lowercase().filter { it.isLetterOrDigit() }.toSet()
        return uniqueChars.size <= 3
    }
}
