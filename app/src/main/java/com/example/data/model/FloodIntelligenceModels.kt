package com.example.data.model

data class GeoLocationPoint(
    val latitude: Double,
    val longitude: Double,
    val city: String,
    val stateOrRegion: String,
    val addressHint: String,
    val isLiveGps: Boolean
)

data class GeoCityResult(
    val name: String,
    val admin1: String,
    val country: String,
    val latitude: Double,
    val longitude: Double
)

data class HourlyRainPoint(
    val timeIso: String,
    val precipProbability: Int,
    val precipMm: Double,
    val surfaceSoilMoisture: Double,
    val deepSoilMoisture: Double
)

data class CityFloodForecast(
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val maxRainProbNext24h: Int,
    val totalRainMmNext24h: Double,
    val peakHourIso: String,
    val soilSaturationPercent: Int,
    val hourlyPoints: List<HourlyRainPoint>
)

data class NwsAlert(
    val id: String,
    val event: String,
    val severity: String,
    val headline: String,
    val areaDesc: String,
    val instruction: String
)

data class LocalFloodNewsItem(
    val title: String,
    val source: String,
    val pubDate: String,
    val link: String
)

enum class FloodRiskLevel(val label: String) {
    LOW("Low Risk"),
    MODERATE("Watch"),
    HIGH("Move Cars · High"),
    CRITICAL("Critical Flood Risk")
}

data class StreetFloodRiskSummary(
    val city: String,
    val riskLevel: FloodRiskLevel,
    val riskScore: Int,
    val carAdvisory: String,
    val preRainHeadline: String,
    val shallowWaterTableReportsCount: Int,
    val activeAlertCount: Int,
    val upcomingRainMm: Double,
    val soilSaturationPct: Int
)

data class GroundwaterAiDiagnosis(
    val lossCategory: String,
    val verdict: String,
    val confidencePercent: Int,
    val summary: String,
    val engineeringReasoning: String,
    val restorationScope: String,
    val constructionStageAdvice: String = "",
    val recommendedSolutions: List<String>,
    val immediateActions: List<String>,
    val streetFloodRisk: String,
    val estimatedDepthFeet: Double,
    val shouldEscalateToEngineer: Boolean
)

data class VideoScenePlan(
    val sceneNumber: Int,
    val headline: String,
    val voiceoverCaption: String,
    val visualPrompt: String,
    val cameraMotion: String
)

data class GeneratedVideoClip(
    val title: String,
    val sourceType: String,
    val mp4FilePath: String,
    val durationSeconds: Int,
    val scenes: List<VideoScenePlan>,
    val scriptSummary: String
)

data class AdaptiveSolutionStat(
    val solutionName: String,
    val waterSource: String,
    val soilType: String,
    val successRatePercent: Int,
    val avgRating: Double,
    val sampleSize: Int,
    val avgCostUsd: Int
)

data class ContractorReputation(
    val contractorUserId: String,
    val avgOverallRating: Double,
    val avgCommunicationRating: Double,
    val reviewCount: Int,
    val positiveCount: Int,
    val negativeCount: Int,
    val shoddyWorkFlags: Int,
    val proofReviewCount: Int,
    val isBlacklisted: Boolean,
    val blacklistReason: String,
    val distanceMiles: Double,
    val helpfulRankScore: Double
)

data class ExternalContractorGlassWallGroup(
    val contractorName: String,
    val companyOrLicenseRef: String,
    val city: String,
    val avgOverallRating: Double,
    val avgCommunicationRating: Double,
    val shoddyWorkFlags: Int,
    val isBlacklisted: Boolean,
    val reviews: List<ContractorReview>
)

data class DiyLeakTestStep(
    val id: String,
    val title: String,
    val durationLabel: String,
    val instructions: String,
    val positiveMeans: String,
    val negativeMeans: String
)

data class WholesaleProduct(
    val sku: String,
    val name: String,
    val category: String,
    val unitLabel: String,
    val wholesalePriceUsd: Double,
    val retailBenchmarkUsd: Double,
    val specs: String,
    val shipsFrom: String
) {
    val savingsUsd: Double get() = (retailBenchmarkUsd - wholesalePriceUsd).coerceAtLeast(0.0)
    val savingsPercent: Int
        get() = if (retailBenchmarkUsd > 0) {
            (((retailBenchmarkUsd - wholesalePriceUsd) / retailBenchmarkUsd) * 100.0).toInt().coerceIn(0, 95)
        } else 0
}

data class CartItem(
    val product: WholesaleProduct,
    val quantity: Int
)

data class JobKitRecommendation(
    val kitSummary: String,
    val estimatedSavingsVsRetailUsd: Double,
    val recommendedSkus: List<Pair<String, Int>>,
    val installationNotes: List<String>
)

data class ProblemMatchQuery(
    val problemCategory: String = "auto",
    val problemText: String = "",
    val constructionStage: String = "none",
    val depthFeet: Double = 0.0,
    val city: String = "",
    val needsOnSiteCrew: Boolean = true
)

data class ContractorMatchResult(
    val contractor: ContractorProfile,
    val reputation: ContractorReputation?,
    val matchScorePercent: Int,
    val isPerfectMatch: Boolean,
    val matchReasons: List<String>,
    val recommendedServiceMode: String,
    val portfolioMatchCount: Int,
    val provenSimilarJobsCount: Int = 0,
    val verifiedSuccessRatePercent: Int = 0,
    val guaranteeEligible: Boolean = false,
    val guaranteeBadgeLabel: String = ""
)

data class AiMatchConciergeVerdict(
    val topRecommendedContractorName: String,
    val reasoning: String,
    val recommendedServiceMode: String,
    val checklistToShareWithPro: List<String>
)

data class GroundedCreditLink(
    val title: String,
    val url: String,
    val domain: String
)

data class ContractorCopilotAdvice(
    val clientAnswer: String,
    val contractorStrategy: String,
    val recommendedVerdict: String,
    val suggestedEstimateUsd: Int,
    val creditLinks: List<GroundedCreditLink>,
    val searchQueriesUsed: List<String>
)

data class ContinuousLearningSnapshot(
    val totalFieldReports: Int,
    val verifiedOutcomesCount: Int,
    val completedJobsCount: Int,
    val honestReviewsCount: Int,
    val topCityInsight: String,
    val topSolutionHeadline: String,
    val updatedTimestampLabel: String
)

data class CslbVerificationResult(
    val licenseNumber: String,
    val status: String, // "cslb_verified", "format_checked", "unverified", "flagged"
    val summary: String,
    val classification: String,
    val lookupUrl: String
)

data class LiveDisasterEvent(
    val id: String,
    val type: String, // "wildfire" | "flood" | "storm"
    val title: String,
    val severity: String, // "Critical" | "High" | "Moderate"
    val latitude: Double,
    val longitude: Double,
    val source: String,
    val details: String,
    val updatedAtLabel: String,
    val link: String = "",
    val acresBurned: Int = 0,
    val containmentPct: Int = -1,
    val distanceToHomeMiles: Double = 0.0,
    val distanceRatioLabel: String = "",
    val nearestPropertyLabel: String = "",
    val recommendedCleanseProtocol: String = "",
    val matchedContractorSpecialties: List<String> = emptyList()
)

data class PropertyHazardHistory(
    val label: String,
    val propertyType: String, // "house" | "business"
    val address: String,
    val city: String,
    val state: String,
    val latitude: Double,
    val longitude: Double,
    val femaFloodZone: String,
    val floodZoneRisk: String,
    val fireHazardZone: String,
    val groundwaterTableSummary: String,
    val nearbyShallowReportsCount: Int,
    val nearestReportDepthFt: Double?,
    val nearestActiveFireMiles: Double?,
    val nearestActiveFloodMiles: Double?,
    val aiHistoricalDossier: String,
    val preBuyOrCleanseChecklist: List<String> = emptyList(),
    val creditLinks: List<GroundedCreditLink> = emptyList()
)


