package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val preferredCity: String = "",
    val rolePreference: String = "resident",
    val linkedInsuranceCarrierUid: String = "",
    val linkedInsuranceCarrierName: String = "",
    val linkedPolicyRef: String = "",
    val linkedClaimNumber: String = "",
    val linkedDeductibleUsd: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WaterReport(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val authorRole: String = "resident",
    val visibility: String = "community",
    val moderationStatus: String = "approved",
    val moderationNotes: String = "",
    val plausibilityScore: Int = 100,
    val city: String = "",
    val stateOrRegion: String = "",
    val addressHint: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val depthFeet: Double = 0.0,
    val soilType: String = "unknown",
    val constructionStage: String = "none",
    val lossCategory: String = "groundwater",
    val situationType: String = "groundwater_seepage",
    val waterSourceVerdict: String = "uncertain",
    val aiConfidence: Int = 0,
    val aiSummary: String = "",
    val lossDescription: String = "",
    val restorationScope: String = "",
    val recommendedSolutions: List<String> = emptyList(),
    val notes: String = "",
    val mediaType: String = "none",
    val mediaPreviewBase64: String = "",
    val extraPhotosBase64: List<String> = emptyList(),
    val videoDurationLabel: String = "",
    val streetFloodRisk: String = "moderate",
    val upvotesCount: Int = 0,
    val solutionApplied: String = "",
    val solutionWorked: Boolean = false,
    val engineerVerified: Boolean = false,
    val verifiedByContractorId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ModerationTicket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val reportId: String = "",
    val city: String = "",
    val lossCategory: String = "groundwater",
    val hasMedia: Boolean = false,
    val isDetailedReport: Boolean = false,
    val plausibilityScore: Int = 85,
    val completenessIssues: List<String> = emptyList(),
    val queueStatus: String = "pending",
    val aiModerationNotes: String = "",
    val reviewedByUid: String = "",
    val previewSummary: String = "",
    val photoBase64: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SolutionOutcome(
    @DocumentId val id: String = "",
    val userId: String = "",
    val reportId: String = "",
    val isPublic: Boolean = true,
    val city: String = "",
    val soilType: String = "unknown",
    val waterSource: String = "ground_water_table",
    val solutionName: String = "",
    val worked: Boolean = true,
    val effectivenessScore: Int = 5,
    val estimatedCostUsd: Int = 0,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ContractorProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val companyName: String = "",
    val specialty: String = "soil_engineer",
    val licenseNumber: String = "",
    val licensingState: String = "",
    val cslbBoardStatus: String = "unverified",
    val cslbLookupUrl: String = "",
    val iicrcCertNumber: String = "",
    val isIicrcGovernor: Boolean = false,
    val liveScanStatus: String = "not_submitted",
    val liveScanAtiRef: String = "",
    val liveScanAgency: String = "",
    val liveScanClearedDate: String = "",
    val liveScanProofBase64: String = "",
    val primaryCity: String = "Sacramento",
    val latitude: Double = 38.5816,
    val longitude: Double = -121.4944,
    val serviceCities: List<String> = emptyList(),
    val consultationFeeUsd: Int = 45,
    val platformFeePercent: Int = 10,
    val offersOnSiteWork: Boolean = true,
    val bio: String = "",
    val completedReviewsCount: Int = 0,
    val helpfulContributionsCount: Int = 0,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class IicrcAuditRecord(
    @DocumentId val id: String = "",
    val consultationId: String = "",
    val reportId: String = "",
    val contractorUserId: String = "",
    val contractorName: String = "",
    val city: String = "",
    val lossCategory: String = "water_restoration",
    val proposedVerdict: String = "",
    val contractorSuggestion: String = "",
    val contractorStrategy: String = "",
    val aiCreditLinks: List<String> = emptyList(),
    val proposedWorkEstimateUsd: Int = 0,
    val isPublic: Boolean = true,
    val iicrcStatus: String = "pending_iicrc",
    val iicrcStandardRef: String = "",
    val iicrcInspectorUid: String = "",
    val iicrcInspectorName: String = "",
    val iicrcGovernorNotes: String = "",
    val iicrcSources: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ConsultationRequest(
    @DocumentId val id: String = "",
    val clientUserId: String = "",
    val clientName: String = "",
    val contractorUserId: String = "",
    val contractorName: String = "",
    val reportId: String = "",
    val city: String = "",
    val serviceMode: String = "remote_advice",
    val lossCategory: String = "groundwater",
    val question: String = "",
    val status: String = "requested",
    val engineerVerdict: String = "pending",
    val engineerRecommendation: String = "",
    val contractorStrategy: String = "",
    val aiCreditLinks: List<String> = emptyList(),
    val proposedWorkEstimateUsd: Int = 0,
    val contractorFeeUsd: Int = 45,
    val platformFeeUsd: Int = 5,
    val totalFeeUsd: Int = 50,
    val clientRating: Int = 0,
    val workAuthStatus: String = "none",
    val workAuthTerms: String = "",
    val paymentScheduleSummary: String = "",
    val clientSignatureName: String = "",
    val clientAuthorizedAtLabel: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ContractorReview(
    @DocumentId val id: String = "",
    val userId: String = "",
    val reviewerName: String = "",
    val contractorUserId: String = "",
    val contractorName: String = "",
    val companyOrLicenseRef: String = "",
    val city: String = "",
    val isPublic: Boolean = true,
    val workCategory: String = "water_restoration",
    val overallRating: Int = 5,
    val communicationRating: Int = 5,
    val honestExperience: String = "",
    val proofDescription: String = "",
    val proofPhotoBase64: String = "",
    val hasProof: Boolean = false,
    val wouldRecommend: Boolean = true,
    val flagShoddyWork: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ContractorPortfolioItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val contractorName: String = "",
    val entryType: String = "portfolio",
    val isPublic: Boolean = true,
    val title: String = "",
    val lossCategory: String = "water_restoration",
    val city: String = "",
    val clientReferenceName: String = "",
    val description: String = "",
    val completionYear: String = "",
    val photoBase64: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SupplyOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val jobDescription: String = "",
    val shippingCity: String = "",
    val shippingAddress: String = "",
    val itemSummaries: List<String> = emptyList(),
    val wholesaleTotalUsd: Double = 0.0,
    val retailBenchmarkUsd: Double = 0.0,
    val totalSavingsUsd: Double = 0.0,
    val platformFeeUsd: Double = 0.0,
    val status: String = "submitted",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ContractorComplaint(
    @DocumentId val id: String = "",
    val userId: String = "",
    val complainantName: String = "",
    val contractorUserId: String = "external",
    val contractorName: String = "",
    val companyName: String = "",
    val licenseNumber: String = "",
    val iicrcCertNumber: String = "",
    val isRestorationCompany: Boolean = false,
    val city: String = "",
    val complaintCategory: String = "poor_workmanship",
    val summary: String = "",
    val evidenceNotes: String = "",
    val proofPhotoBase64: String = "",
    val routedToSocalCslb: Boolean = true,
    val routedToIicrc: Boolean = false,
    val cslbIntakeUrl: String = "",
    val iicrcIntakeUrl: String = "",
    val status: String = "filed",
    val isPublic: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class InsuranceCarrierProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val carrierName: String = "",
    val adjusterName: String = "",
    val licenseOrNaicCode: String = "",
    val claimsEmail: String = "",
    val claimsPhone: String = "",
    val claimsPortalUrl: String = "",
    val serviceRegions: List<String> = emptyList(),
    val supportsDirectRestorationBilling: Boolean = true,
    val avgTurnaroundHours: Int = 24,
    val bio: String = "",
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class InsuranceClaimThread(
    @DocumentId val id: String = "",
    val clientUserId: String = "",
    val clientName: String = "",
    val contractorUserId: String = "",
    val contractorName: String = "",
    val insurerUserId: String = "external_carrier",
    val insurerName: String = "",
    val claimNumber: String = "",
    val policyType: String = "homeowners_ho3",
    val deductibleUsd: Int = 0,
    val reportId: String = "",
    val consultationId: String = "",
    val city: String = "",
    val lossCategory: String = "water_restoration",
    val contractorSuggestion: String = "",
    val estimatedScopeUsd: Int = 0,
    val approvedCoverageUsd: Int = 0,
    val claimStatus: String = "linked",
    val lastMessageFromRole: String = "client",
    val communicationLog: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SavedMonitoredProperty(
    @DocumentId val id: String = "",
    val userId: String = "",
    val label: String = "",
    val propertyType: String = "house", // "house" | "business"
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val femaFloodZone: String = "Unknown",
    val fireHazardZone: String = "Standard",
    val waterTableDepthFt: Double = 0.0,
    val hazardSummary: String = "",
    val notifyPriorityAlerts: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ContractorProductReview(
    @DocumentId val id: String = "",
    val userId: String = "",
    val contractorName: String = "",
    val companyName: String = "",
    val licenseNumber: String = "",
    val productSku: String = "custom",
    val productName: String = "",
    val brandModel: String = "",
    val category: String = "Waterproofing",
    val disasterUseCase: String = "flooding", // "flooding" | "wildfire_smoke" | "mold_cleanse" | "groundwater"
    val rating: Int = 5,
    val jobsUsedCount: Int = 1,
    val pros: String = "",
    val cons: String = "",
    val honestReview: String = "",
    val wouldUseOnInsuredJob: Boolean = true,
    val isPublic: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
