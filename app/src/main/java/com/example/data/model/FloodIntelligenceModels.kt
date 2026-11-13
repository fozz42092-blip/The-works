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
    val creditLinks: List<GroundedCreditLink> = emptyList(),
    val elevationFeet: Int = 0,
    val landBeforeHouseBuilt: String = "",
    val neighborhoodEra: String = "",
    val motherNatureHistory: String = "",
    val erosionComparisonNotes: String = "",
    val historicalStormCount: Int = 0
)

data class SoilBottleResult(
    val usdaTextureClass: String,
    val drainageRating: String,
    val mappedSoilTypeKey: String,
    val clayPercent: Int,
    val siltPercent: Int,
    val sandPercent: Int,
    val foundationAdvisory: String
)

data class HowToVideoGuide(
    val id: String,
    val title: String,
    val category: String, // "soil_water" | "mold" | "shutoff" | "plumbing_roof" | "drainage"
    val durationLabel: String,
    val summary: String,
    val stepByStepChecklist: List<String>,
    val videoSearchUrl: String,
    val videoPromptHint: String
)

data class HomeAzAiInspection(
    val mode: String, // "az_photo" | "exterior_video" | "future_home"
    val calmingReassurance: String,
    val detectedIssueTitle: String,
    val severityLabel: String, // "Low" | "Moderate" | "Urgent"
    val recommendedTrade: String,
    val recommendedSpecialtyKey: String,
    val scopeOfWork: String,
    val estimatedPriceLowUsd: Int,
    val estimatedPriceHighUsd: Int,
    val geologicalAndElevationNotes: String,
    val lifeExpectancyForecast: String,
    val plumbingAndPipeCorrosionFindings: String,
    val drainageAndErosionFindings: String,
    val needsHygienistAirOrTapeTest: Boolean,
    val needsEquipmentRental: Boolean,
    val diyImmediateSteps: List<String>,
    val relatedVideoCategory: String
)

data class OnlineVerifiedContractorLead(
    val companyName: String,
    val tradeSummary: String,
    val licenseOrRatingHint: String,
    val verificationUrl: String,
    val domain: String,
    val reasonMatched: String
)

data class FieldScoutRankEntry(
    val rank: Int,
    val userId: String,
    val displayName: String,
    val city: String,
    val verifiedReportsCount: Int,
    val mediaReportsCount: Int,
    val outcomesLoggedCount: Int,
    val points: Int,
    val freeSubscriptionEarned: Boolean,
    val paidScoutEligible: Boolean
)

data class BuildingProblemCategoryAssessment(
    val category: String,
    val riskLevel: String, // "None Observed" | "Low" | "Moderate" | "Elevated" | "Urgent"
    val visualIndicatorsFound: List<String>,
    val detailedAnalysis: String,
    val realLifeProblemsThatCanOccur: List<String>,
    val recommendedActions: List<String>,
    val recommendedTradeKey: String
)

data class BuildingPhotoAiAnalysis(
    val inspectedPortion: String,
    val buildingType: String,
    val overallCondition: String,
    val overallRiskBadge: String, // "Low" | "Moderate" | "Elevated" | "Urgent"
    val summary: String,
    val visualHighlightsSummary: String,
    val drainage: BuildingProblemCategoryAssessment,
    val roofing: BuildingProblemCategoryAssessment,
    val foundation: BuildingProblemCategoryAssessment,
    val additionalPortionFindings: BuildingProblemCategoryAssessment,
    val realLifeRisksIfIgnored: List<String>,
    val actionableSuggestions: List<String>,
    val estimatedRepairRangeUsd: String,
    val topRecommendedSpecialtyKey: String
)

data class DiyPestSupplyItem(
    val name: String,
    val purpose: String,
    val websiteUrl: String,
    val category: String
)

data class TermiteStrategyComparison(
    val methodTitle: String,
    val residualProtection: String,
    val pros: String,
    val cons: String,
    val verdict: String
)

data class PestControlAiAnalysis(
    val suspectedCurrentProblem: String,
    val pestCategory: String, // "rodent_entry" | "termite" | "insect_crawling" | "moisture_pest" | "wildlife_burrow" | "general_pest"
    val urgencyLevel: String, // "Low" | "Moderate" | "Elevated" | "Urgent"
    val confidenceSummary: String,
    val obviousSignsDetected: List<String>,
    val howToSealAndExcludeSteps: List<String>,
    val treatmentAndResidualInsight: String,
    val termiteStrategyNote: String,
    val diySuppliesNeeded: List<DiyPestSupplyItem>,
    val shouldCallLicensedPro: Boolean,
    val recommendedProReason: String
)


