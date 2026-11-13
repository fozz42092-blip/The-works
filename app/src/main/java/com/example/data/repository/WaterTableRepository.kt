package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.catalog.WaterTableCatalogs
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AppRevisionSuggestion
import com.example.data.model.ConsultationRequest
import com.example.data.model.ContractorComplaint
import com.example.data.model.ContractorPortfolioItem
import com.example.data.model.ContractorProfile
import com.example.data.model.ContractorProductReview
import com.example.data.model.ContractorReview
import com.example.data.model.EquipmentRentalItem
import com.example.data.model.FieldScoutApplication
import com.example.data.model.HygienistSampleRequest
import com.example.data.model.IicrcAuditRecord
import com.example.data.model.InsuranceCarrierProfile
import com.example.data.model.InsuranceClaimThread
import com.example.data.model.InsurancePolicyOffer
import com.example.data.model.ModerationTicket
import com.example.data.model.SavedMonitoredProperty
import com.example.data.model.SolutionOutcome
import com.example.data.model.SupplyOrder
import com.example.data.model.UserProfile
import com.example.data.model.WaterReport
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.math.roundToInt

class WaterTableRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun handleFlowError(error: Throwable, operationType: OperationType, path: String, expectedUid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }

    // --- Community Water Table, Flood & Multi-Peril Restoration Reports ---

    fun observeCommunityReports(): Flow<List<WaterReport>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_REPORTS
        emitAll(
            db.collection(COLLECTION_REPORTS)
                .whereEqualTo("visibility", "community")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WaterReport::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeMyReports(): Flow<List<WaterReport>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_REPORTS
        emitAll(
            db.collection(COLLECTION_REPORTS)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WaterReport::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun createWaterReport(
        city: String,
        stateOrRegion: String,
        addressHint: String,
        latitude: Double,
        longitude: Double,
        depthFeet: Double,
        soilType: String,
        constructionStage: String = "none",
        lossCategory: String = "groundwater",
        situationType: String,
        waterSourceVerdict: String,
        aiConfidence: Int,
        aiSummary: String,
        lossDescription: String = "",
        restorationScope: String = "",
        recommendedSolutions: List<String>,
        notes: String,
        mediaType: String,
        mediaPreviewBase64: String,
        extraPhotosBase64: List<String> = emptyList(),
        videoDurationLabel: String,
        streetFloodRisk: String,
        authorRole: String = "resident",
        requiresModeration: Boolean = false,
        plausibilityScore: Int = 90,
        completenessIssues: List<String> = emptyList(),
        moderationNotes: String = "Cleared automated completeness & plausibility checks."
    ): Result<String> {
        val uid = requireUserId()
        val authorName = (auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Community Member").take(60)
        val reportId = "rep_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection(COLLECTION_REPORTS).document(reportId)
        val cleanExtras = extraPhotosBase64.filter { it.isNotBlank() }.take(3).map { it.take(115000) }
        val visibility = if (requiresModeration) "pending_review" else "community"
        val modStatus = if (requiresModeration) "pending_review" else "approved"

        val payload = mapOf(
            "userId" to uid,
            "authorName" to authorName,
            "authorRole" to authorRole,
            "visibility" to visibility,
            "moderationStatus" to modStatus,
            "moderationNotes" to moderationNotes.take(600),
            "plausibilityScore" to plausibilityScore.coerceIn(0, 100),
            "city" to city.trim().take(80),
            "stateOrRegion" to stateOrRegion.trim().take(60),
            "addressHint" to addressHint.trim().take(120),
            "latitude" to latitude.coerceIn(-90.0, 90.0),
            "longitude" to longitude.coerceIn(-180.0, 180.0),
            "depthFeet" to depthFeet.coerceIn(0.0, 200.0),
            "soilType" to soilType,
            "constructionStage" to constructionStage,
            "lossCategory" to lossCategory,
            "situationType" to situationType,
            "waterSourceVerdict" to waterSourceVerdict,
            "aiConfidence" to aiConfidence.coerceIn(0, 100),
            "aiSummary" to aiSummary.take(1500),
            "lossDescription" to lossDescription.trim().take(2500),
            "restorationScope" to restorationScope.trim().take(1500),
            "recommendedSolutions" to recommendedSolutions.take(8).map { it.take(160) },
            "notes" to notes.trim().take(1500),
            "mediaType" to mediaType,
            "mediaPreviewBase64" to mediaPreviewBase64.take(195000),
            "extraPhotosBase64" to cleanExtras,
            "videoDurationLabel" to videoDurationLabel.take(40),
            "streetFloodRisk" to streetFloodRisk,
            "upvotesCount" to 0,
            "solutionApplied" to "",
            "solutionWorked" to false,
            "engineerVerified" to false,
            "verifiedByContractorId" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            if (requiresModeration) {
                val ticketRef = db.collection(COLLECTION_MODERATION_QUEUE).document(reportId)
                val ticketPayload = mapOf(
                    "userId" to uid,
                    "authorName" to authorName,
                    "reportId" to reportId,
                    "city" to city.trim().take(80),
                    "lossCategory" to lossCategory,
                    "hasMedia" to (mediaType != "none" || cleanExtras.isNotEmpty()),
                    "isDetailedReport" to (lossDescription.trim().length >= 50 || notes.trim().length >= 70),
                    "plausibilityScore" to plausibilityScore.coerceIn(0, 100),
                    "completenessIssues" to completenessIssues.take(10).map { it.take(160) },
                    "queueStatus" to "pending",
                    "aiModerationNotes" to moderationNotes.take(600),
                    "reviewedByUid" to "",
                    "previewSummary" to (lossDescription.ifBlank { notes }.ifBlank { aiSummary }).take(400),
                    "photoBase64" to (mediaPreviewBase64.ifBlank { cleanExtras.firstOrNull().orEmpty() }).take(195000),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ticketRef.set(ticketPayload).await()
            }
            Result.success(reportId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observePendingModerationQueue(): Flow<List<ModerationTicket>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_MODERATION_QUEUE
        emitAll(
            db.collection(COLLECTION_MODERATION_QUEUE)
                .whereEqualTo("queueStatus", "pending")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ModerationTicket::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun approveModerationTicket(
        ticket: ModerationTicket,
        approved: Boolean,
        moderatorNotes: String
    ): Result<Unit> {
        val uid = requireUserId()
        val reportRef = db.collection(COLLECTION_REPORTS).document(ticket.reportId)
        val ticketRef = db.collection(COLLECTION_MODERATION_QUEUE).document(ticket.id.ifBlank { ticket.reportId })
        return try {
            val nextVisibility = if (approved) "community" else "flagged"
            val nextStatus = if (approved) "approved" else "flagged"
            reportRef.update(
                mapOf(
                    "visibility" to nextVisibility,
                    "moderationStatus" to nextStatus,
                    "moderationNotes" to moderatorNotes.take(600),
                    "verifiedByContractorId" to uid,
                    "engineerVerified" to approved,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            ticketRef.update(
                mapOf(
                    "queueStatus" to if (approved) "approved" else "flagged",
                    "reviewedByUid" to uid,
                    "aiModerationNotes" to moderatorNotes.take(600),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (approved) {
                db.collection(COLLECTION_CONTRACTORS).document(uid).update(
                    mapOf(
                        "helpfulContributionsCount" to FieldValue.increment(1),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, reportRef.path)
            Result.failure(e)
        }
    }

    suspend fun getWaterReport(reportId: String): Result<WaterReport?> {
        requireUserId()
        val docRef = db.collection(COLLECTION_REPORTS).document(reportId)
        return try {
            val snap = docRef.get().await()
            val report = snap.toObject(
                WaterReport::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(report)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun upvoteReport(report: WaterReport): Result<Unit> {
        requireUserId()
        require(report.id.isNotBlank()) { "Report ID is required" }
        val docRef = db.collection(COLLECTION_REPORTS).document(report.id)
        return try {
            docRef.update(
                mapOf(
                    "upvotesCount" to FieldValue.increment(1),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteWaterReport(reportId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection(COLLECTION_REPORTS).document(reportId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Adaptive Solution Learning Outcomes ---

    fun observeSolutionOutcomes(): Flow<List<SolutionOutcome>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_OUTCOMES
        emitAll(
            db.collection(COLLECTION_OUTCOMES)
                .whereEqualTo("isPublic", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SolutionOutcome::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun logSolutionOutcome(
        report: WaterReport?,
        city: String,
        soilType: String,
        waterSource: String,
        solutionName: String,
        worked: Boolean,
        effectivenessScore: Int,
        estimatedCostUsd: Int,
        notes: String
    ): Result<String> {
        val uid = requireUserId()
        val outcomeId = "out_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection(COLLECTION_OUTCOMES).document(outcomeId)
        val payload = mapOf(
            "userId" to uid,
            "reportId" to (report?.id ?: ""),
            "isPublic" to true,
            "city" to city.trim().take(80).ifBlank { "General" },
            "soilType" to soilType,
            "waterSource" to waterSource,
            "solutionName" to solutionName.trim().take(120),
            "worked" to worked,
            "effectivenessScore" to effectivenessScore.coerceIn(1, 5),
            "estimatedCostUsd" to estimatedCostUsd.coerceIn(0, 500000),
            "notes" to notes.trim().take(600),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            if (report != null && report.id.isNotBlank() && report.userId == uid) {
                db.collection(COLLECTION_REPORTS).document(report.id).update(
                    mapOf(
                        "solutionApplied" to solutionName.trim().take(120),
                        "solutionWorked" to worked,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(outcomeId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Licensed Engineers & Restoration Contractors Directory ---

    fun observeActiveContractors(): Flow<List<ContractorProfile>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_CONTRACTORS
        emitAll(
            db.collection(COLLECTION_CONTRACTORS)
                .whereEqualTo("isActive", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ContractorProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.helpfulContributionsCount + it.completedReviewsCount }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun registerOrUpdateContractorProfile(
        displayName: String,
        companyName: String,
        specialty: String,
        licenseNumber: String,
        licensingState: String,
        primaryCity: String,
        latitude: Double,
        longitude: Double,
        serviceCities: List<String>,
        consultationFeeUsd: Int,
        offersOnSiteWork: Boolean,
        bio: String,
        cslbBoardStatus: String = "unverified",
        cslbLookupUrl: String = "",
        iicrcCertNumber: String = "",
        isIicrcGovernor: Boolean = (specialty == "iicrc_inspector"),
        liveScanStatus: String = "not_submitted",
        liveScanAtiRef: String = "",
        liveScanAgency: String = "",
        liveScanClearedDate: String = "",
        liveScanProofBase64: String = ""
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection(COLLECTION_CONTRACTORS).document(uid)
        val cleanCities = serviceCities.map { it.trim() }.filter { it.isNotBlank() }.take(10)
            .ifEmpty { listOf(primaryCity.ifBlank { "Regional" }) }
        val effectiveGovernor = isIicrcGovernor || specialty == "iicrc_inspector"
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "displayName" to displayName.trim().take(60),
                        "companyName" to companyName.trim().take(80),
                        "specialty" to specialty,
                        "licenseNumber" to licenseNumber.trim().take(40),
                        "licensingState" to licensingState.trim().take(30),
                        "cslbBoardStatus" to cslbBoardStatus,
                        "cslbLookupUrl" to cslbLookupUrl.trim().take(300),
                        "iicrcCertNumber" to iicrcCertNumber.trim().take(40),
                        "isIicrcGovernor" to effectiveGovernor,
                        "liveScanStatus" to liveScanStatus,
                        "liveScanAtiRef" to liveScanAtiRef.trim().take(40),
                        "liveScanAgency" to liveScanAgency.trim().take(100),
                        "liveScanClearedDate" to liveScanClearedDate.trim().take(30),
                        "liveScanProofBase64" to liveScanProofBase64.take(150000),
                        "primaryCity" to primaryCity.trim().take(80).ifBlank { cleanCities.first() },
                        "latitude" to latitude.coerceIn(-90.0, 90.0),
                        "longitude" to longitude.coerceIn(-180.0, 180.0),
                        "serviceCities" to cleanCities,
                        "consultationFeeUsd" to consultationFeeUsd.coerceIn(5, 500),
                        "offersOnSiteWork" to offersOnSiteWork,
                        "bio" to bio.trim().take(600),
                        "isActive" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to displayName.trim().take(60),
                    "companyName" to companyName.trim().take(80),
                    "specialty" to specialty,
                    "licenseNumber" to licenseNumber.trim().take(40),
                    "licensingState" to licensingState.trim().take(30),
                    "cslbBoardStatus" to cslbBoardStatus,
                    "cslbLookupUrl" to cslbLookupUrl.trim().take(300),
                    "iicrcCertNumber" to iicrcCertNumber.trim().take(40),
                    "isIicrcGovernor" to effectiveGovernor,
                    "liveScanStatus" to liveScanStatus,
                    "liveScanAtiRef" to liveScanAtiRef.trim().take(40),
                    "liveScanAgency" to liveScanAgency.trim().take(100),
                    "liveScanClearedDate" to liveScanClearedDate.trim().take(30),
                    "liveScanProofBase64" to liveScanProofBase64.take(150000),
                    "primaryCity" to primaryCity.trim().take(80).ifBlank { cleanCities.first() },
                    "latitude" to latitude.coerceIn(-90.0, 90.0),
                    "longitude" to longitude.coerceIn(-180.0, 180.0),
                    "serviceCities" to cleanCities,
                    "consultationFeeUsd" to consultationFeeUsd.coerceIn(5, 500),
                    "platformFeePercent" to PLATFORM_FEE_PERCENT,
                    "offersOnSiteWork" to offersOnSiteWork,
                    "bio" to bio.trim().take(600),
                    "completedReviewsCount" to 0,
                    "helpfulContributionsCount" to 0,
                    "isActive" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Remote Consultations & Restoration Work Dispatches ---

    fun observeMyClientConsultations(): Flow<List<ConsultationRequest>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_CONSULTATIONS
        emitAll(
            db.collection(COLLECTION_CONSULTATIONS)
                .whereEqualTo("clientUserId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ConsultationRequest::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeContractorInbox(): Flow<List<ConsultationRequest>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_CONSULTATIONS
        emitAll(
            db.collection(COLLECTION_CONSULTATIONS)
                .whereEqualTo("contractorUserId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ConsultationRequest::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun requestConsultation(
        contractor: ContractorProfile,
        report: WaterReport,
        question: String,
        serviceMode: String = "remote_advice"
    ): Result<String> {
        val uid = requireUserId()
        val clientName = (auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Resident").take(60)
        val consultationId = "con_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection(COLLECTION_CONSULTATIONS).document(consultationId)

        val contractorFee = contractor.consultationFeeUsd.coerceIn(5, 500)
        val platformFee = ((contractorFee * PLATFORM_FEE_PERCENT) / 100.0).roundToInt().coerceIn(1, 100)
        val totalFee = contractorFee + platformFee

        val payload = mapOf(
            "clientUserId" to uid,
            "clientName" to clientName,
            "contractorUserId" to contractor.userId,
            "contractorName" to contractor.displayName.take(60),
            "reportId" to report.id,
            "city" to report.city.ifBlank { "Local" }.take(80),
            "serviceMode" to serviceMode,
            "lossCategory" to report.lossCategory.ifBlank { "groundwater" },
            "question" to question.trim().take(1000),
            "status" to "requested",
            "engineerVerdict" to "pending",
            "engineerRecommendation" to "",
            "contractorStrategy" to "",
            "aiCreditLinks" to emptyList<String>(),
            "proposedWorkEstimateUsd" to 0,
            "contractorFeeUsd" to contractorFee,
            "platformFeeUsd" to platformFee,
            "totalFeeUsd" to totalFee,
            "clientRating" to 0,
            "workAuthStatus" to "none",
            "workAuthTerms" to "",
            "paymentScheduleSummary" to "",
            "clientSignatureName" to "",
            "clientAuthorizedAtLabel" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(consultationId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun submitContractorReview(
        consultation: ConsultationRequest,
        verdict: String,
        recommendation: String,
        proposedWorkEstimateUsd: Int = 0,
        contractorStrategy: String = "",
        aiCreditLinks: List<String> = emptyList(),
        workAuthTerms: String = "",
        paymentScheduleSummary: String = ""
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection(COLLECTION_CONSULTATIONS).document(consultation.id)
        val cleanLinks = aiCreditLinks.map { it.trim().take(340) }.filter { it.isNotBlank() }.take(10)
        val cleanWorkAuth = workAuthTerms.trim().take(3500)
        val nextWorkAuthStatus = if (cleanWorkAuth.isNotBlank()) "drafted" else consultation.workAuthStatus.ifBlank { "none" }
        return try {
            docRef.update(
                mapOf(
                    "status" to "reviewed",
                    "engineerVerdict" to verdict,
                    "engineerRecommendation" to recommendation.trim().take(2000),
                    "contractorStrategy" to contractorStrategy.trim().take(2000),
                    "aiCreditLinks" to cleanLinks,
                    "proposedWorkEstimateUsd" to proposedWorkEstimateUsd.coerceIn(0, 250000),
                    "workAuthStatus" to nextWorkAuthStatus,
                    "workAuthTerms" to cleanWorkAuth,
                    "paymentScheduleSummary" to paymentScheduleSummary.trim().take(600),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (consultation.reportId.isNotBlank()) {
                val reportVerdict = when (verdict) {
                    "needs_site_boring" -> "uncertain"
                    "water_mitigation_approved" -> "water_loss_mitigation"
                    "mold_remediation_approved" -> "mold_mitigation"
                    "smoke_fire_approved" -> "smoke_fire_mitigation"
                    else -> verdict
                }
                db.collection(COLLECTION_REPORTS).document(consultation.reportId).update(
                    mapOf(
                        "engineerVerified" to true,
                        "verifiedByContractorId" to uid,
                        "waterSourceVerdict" to reportVerdict,
                        "restorationScope" to recommendation.trim().take(1500),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Submit restoration suggestion for IICRC Technical Governance oversight
            val isRestorationWork = consultation.lossCategory in listOf(
                "water_restoration", "mold_mitigation", "smoke_damage", "fire_restoration"
            ) || verdict in listOf("water_mitigation_approved", "mold_remediation_approved", "smoke_fire_approved")
            if (isRestorationWork && recommendation.trim().length >= 5) {
                val auditRef = db.collection(COLLECTION_IICRC_AUDITS).document(consultation.id)
                val existingAudit = auditRef.get().await()
                if (!existingAudit.exists()) {
                    val stdRef = when (consultation.lossCategory) {
                        "mold_mitigation" -> "IICRC S520 (Mold)"
                        "smoke_damage", "fire_restoration" -> "IICRC S700 (Fire & Smoke)"
                        else -> "IICRC S500 (Water)"
                    }
                    auditRef.set(
                        mapOf(
                            "consultationId" to consultation.id,
                            "reportId" to consultation.reportId,
                            "contractorUserId" to uid,
                            "contractorName" to consultation.contractorName.ifBlank { "Licensed Pro" }.take(60),
                            "city" to consultation.city.ifBlank { "Local" }.take(80),
                            "lossCategory" to consultation.lossCategory.ifBlank { "water_restoration" },
                            "proposedVerdict" to verdict.take(60),
                            "contractorSuggestion" to recommendation.trim().take(2000),
                            "contractorStrategy" to contractorStrategy.trim().take(2000),
                            "aiCreditLinks" to cleanLinks,
                            "proposedWorkEstimateUsd" to proposedWorkEstimateUsd.coerceIn(0, 250000),
                            "isPublic" to true,
                            "iicrcStatus" to "pending_iicrc",
                            "iicrcStandardRef" to stdRef,
                            "iicrcInspectorUid" to "",
                            "iicrcInspectorName" to "",
                            "iicrcGovernorNotes" to "",
                            "iicrcSources" to emptyList<String>(),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            db.collection(COLLECTION_CONTRACTORS).document(uid).update(
                mapOf(
                    "completedReviewsCount" to FieldValue.increment(1),
                    "helpfulContributionsCount" to FieldValue.increment(1),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun signWorkAuthorization(
        consultationId: String,
        clientSignatureName: String,
        signedAtLabel: String
    ): Result<Unit> {
        requireUserId()
        val docRef = db.collection(COLLECTION_CONSULTATIONS).document(consultationId)
        return try {
            docRef.update(
                mapOf(
                    "workAuthStatus" to "client_authorized",
                    "clientSignatureName" to clientSignatureName.trim().take(80),
                    "clientAuthorizedAtLabel" to signedAtLabel.trim().take(40),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeIicrcAudits(): Flow<List<IicrcAuditRecord>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_IICRC_AUDITS
        emitAll(
            db.collection(COLLECTION_IICRC_AUDITS)
                .whereEqualTo("isPublic", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        IicrcAuditRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun governIicrcAudit(
        auditId: String,
        iicrcStatus: String,
        iicrcStandardRef: String,
        iicrcGovernorNotes: String,
        iicrcSources: List<String>
    ): Result<Unit> {
        val uid = requireUserId()
        val inspectorName = (auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "IICRC Governor").take(60)
        val docRef = db.collection(COLLECTION_IICRC_AUDITS).document(auditId)
        return try {
            docRef.update(
                mapOf(
                    "iicrcStatus" to iicrcStatus,
                    "iicrcStandardRef" to iicrcStandardRef.trim().take(120),
                    "iicrcInspectorUid" to uid,
                    "iicrcInspectorName" to inspectorName,
                    "iicrcGovernorNotes" to iicrcGovernorNotes.trim().take(2000),
                    "iicrcSources" to iicrcSources.map { it.trim().take(340) }.filter { it.isNotBlank() }.take(10),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun completeAndRateConsultation(
        consultationId: String,
        rating: Int
    ): Result<Unit> {
        requireUserId()
        val docRef = db.collection(COLLECTION_CONSULTATIONS).document(consultationId)
        return try {
            docRef.update(
                mapOf(
                    "status" to "completed",
                    "clientRating" to rating.coerceIn(1, 5),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Glass Wall Honest Reviews & Proof (Blacklist Engine) ---

    fun observeContractorReviews(): Flow<List<ContractorReview>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_CONTRACTOR_REVIEWS
        emitAll(
            db.collection(COLLECTION_CONTRACTOR_REVIEWS)
                .whereEqualTo("isPublic", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ContractorReview::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun createContractorReview(
        contractorUserId: String,
        contractorName: String,
        companyOrLicenseRef: String,
        city: String,
        workCategory: String,
        overallRating: Int,
        communicationRating: Int,
        honestExperience: String,
        proofDescription: String,
        proofPhotoBase64: String,
        wouldRecommend: Boolean,
        flagShoddyWork: Boolean
    ): Result<String> {
        val uid = requireUserId()
        val reviewerName = (auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Verified Client").take(60)
        val reviewId = "rev_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection(COLLECTION_CONTRACTOR_REVIEWS).document(reviewId)
        val cleanPhoto = proofPhotoBase64.take(195000)
        val cleanProofDesc = proofDescription.trim().take(300)
        val payload = mapOf(
            "userId" to uid,
            "reviewerName" to reviewerName,
            "contractorUserId" to contractorUserId,
            "contractorName" to contractorName.trim().take(80),
            "companyOrLicenseRef" to companyOrLicenseRef.trim().take(100),
            "city" to city.trim().take(80).ifBlank { "Local" },
            "isPublic" to true,
            "workCategory" to workCategory,
            "overallRating" to overallRating.coerceIn(1, 5),
            "communicationRating" to communicationRating.coerceIn(1, 5),
            "honestExperience" to honestExperience.trim().take(1500),
            "proofDescription" to cleanProofDesc,
            "proofPhotoBase64" to cleanPhoto,
            "hasProof" to (cleanPhoto.isNotBlank() || cleanProofDesc.isNotBlank()),
            "wouldRecommend" to wouldRecommend,
            "flagShoddyWork" to flagShoddyWork,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(reviewId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Contractor Portfolio & Past-Client Referrals ---

    fun observeContractorPortfolio(): Flow<List<ContractorPortfolioItem>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_CONTRACTOR_PORTFOLIO
        emitAll(
            db.collection(COLLECTION_CONTRACTOR_PORTFOLIO)
                .whereEqualTo("isPublic", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ContractorPortfolioItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun createPortfolioOrReferralItem(
        contractorName: String,
        entryType: String,
        title: String,
        lossCategory: String,
        city: String,
        clientReferenceName: String,
        description: String,
        completionYear: String,
        photoBase64: String
    ): Result<String> {
        val uid = requireUserId()
        val itemId = "prt_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection(COLLECTION_CONTRACTOR_PORTFOLIO).document(itemId)
        val payload = mapOf(
            "userId" to uid,
            "contractorName" to contractorName.trim().take(60).ifBlank { "Licensed Pro" },
            "entryType" to entryType,
            "isPublic" to true,
            "title" to title.trim().take(100),
            "lossCategory" to lossCategory,
            "city" to city.trim().take(80).ifBlank { "Local" },
            "clientReferenceName" to clientReferenceName.trim().take(80),
            "description" to description.trim().take(1500),
            "completionYear" to completionYear.trim().take(10),
            "photoBase64" to photoBase64.take(195000),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(itemId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deletePortfolioItem(itemId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection(COLLECTION_CONTRACTOR_PORTFOLIO).document(itemId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- User Profile ---

    suspend fun upsertUserProfile(preferredCity: String, rolePreference: String): Result<Unit> {
        val uid = requireUserId()
        val displayName = (auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "WaterTable Member").take(60)
        val docRef = db.collection(COLLECTION_USERS).document(uid)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "displayName" to displayName,
                        "preferredCity" to preferredCity.trim().take(80),
                        "rolePreference" to rolePreference,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to displayName,
                        "preferredCity" to preferredCity.trim().take(80),
                        "rolePreference" to rolePreference,
                        "linkedInsuranceCarrierUid" to "",
                        "linkedInsuranceCarrierName" to "",
                        "linkedPolicyRef" to "",
                        "linkedClaimNumber" to "",
                        "linkedDeductibleUsd" to 0,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun linkClientInsurance(
        carrierUid: String,
        carrierName: String,
        policyRef: String,
        claimNumber: String,
        deductibleUsd: Int,
        fallbackCity: String = "Local"
    ): Result<Unit> {
        val uid = requireUserId()
        val displayName = (auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "WaterTable Member").take(60)
        val docRef = db.collection(COLLECTION_USERS).document(uid)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "linkedInsuranceCarrierUid" to carrierUid.trim().take(128),
                        "linkedInsuranceCarrierName" to carrierName.trim().take(100),
                        "linkedPolicyRef" to policyRef.trim().take(60),
                        "linkedClaimNumber" to claimNumber.trim().take(60),
                        "linkedDeductibleUsd" to deductibleUsd.coerceIn(0, 100000),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to displayName,
                        "preferredCity" to fallbackCity.trim().take(80).ifBlank { "Local" },
                        "rolePreference" to "resident",
                        "linkedInsuranceCarrierUid" to carrierUid.trim().take(128),
                        "linkedInsuranceCarrierName" to carrierName.trim().take(100),
                        "linkedPolicyRef" to policyRef.trim().take(60),
                        "linkedClaimNumber" to claimNumber.trim().take(60),
                        "linkedDeductibleUsd" to deductibleUsd.coerceIn(0, 100000),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(): Result<UserProfile?> {
        val uid = requireUserId()
        val docRef = db.collection(COLLECTION_USERS).document(uid)
        return try {
            val snap = docRef.get().await()
            Result.success(
                snap.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    private suspend fun fetchOrCreateUserProfile(): UserProfile {
        val uid = requireUserId()
        val existing = getUserProfile().getOrNull()
        if (existing != null) return existing
        return UserProfile(
            id = uid,
            userId = uid,
            displayName = (auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Community Member").take(60),
            preferredCity = "Sacramento"
        )
    }

    // --- Wholesale Drop-Ship Supply Orders ---

    fun observeMySupplyOrders(): Flow<List<SupplyOrder>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_SUPPLY_ORDERS
        emitAll(
            db.collection(COLLECTION_SUPPLY_ORDERS)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SupplyOrder::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun createSupplyOrder(
        jobDescription: String,
        shippingCity: String,
        shippingAddress: String,
        itemSummaries: List<String>,
        wholesaleTotalUsd: Double,
        retailBenchmarkUsd: Double,
        platformFeeUsd: Double
    ): Result<String> {
        val uid = requireUserId()
        val customerName = (auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "WaterTable Buyer").take(60)
        val orderId = "ord_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection(COLLECTION_SUPPLY_ORDERS).document(orderId)
        val cleanItems = itemSummaries.take(30).map { it.take(180) }
        val savings = (retailBenchmarkUsd - wholesaleTotalUsd).coerceAtLeast(0.0)
        val payload = mapOf(
            "userId" to uid,
            "customerName" to customerName,
            "jobDescription" to jobDescription.trim().take(1000).ifBlank { "Groundwater drainage job kit" },
            "shippingCity" to shippingCity.trim().take(80).ifBlank { "Local" },
            "shippingAddress" to shippingAddress.trim().take(200),
            "itemSummaries" to cleanItems,
            "wholesaleTotalUsd" to wholesaleTotalUsd.coerceAtLeast(1.0),
            "retailBenchmarkUsd" to retailBenchmarkUsd.coerceAtLeast(1.0),
            "totalSavingsUsd" to savings,
            "platformFeeUsd" to platformFeeUsd.coerceAtLeast(0.0),
            "status" to "submitted",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(orderId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Formal Complaints (Southern California CSLB + IICRC Restoration Oversight) ---

    fun observeContractorComplaints(): Flow<List<ContractorComplaint>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_CONTRACTOR_COMPLAINTS
        emitAll(
            db.collection(COLLECTION_CONTRACTOR_COMPLAINTS)
                .whereEqualTo("isPublic", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ContractorComplaint::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun createContractorComplaint(
        contractorUserId: String,
        contractorName: String,
        companyName: String,
        licenseNumber: String,
        iicrcCertNumber: String,
        isRestorationCompany: Boolean,
        city: String,
        complaintCategory: String,
        summary: String,
        evidenceNotes: String,
        proofPhotoBase64: String
    ): Result<ContractorComplaint> {
        val uid = requireUserId()
        val complainantName = (auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Homeowner").take(60)
        val complaintId = "cmp_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection(COLLECTION_CONTRACTOR_COMPLAINTS).document(complaintId)
        val cleanPhoto = proofPhotoBase64.take(160000)
        val payload = mapOf(
            "userId" to uid,
            "complainantName" to complainantName,
            "contractorUserId" to contractorUserId.ifBlank { "external" },
            "contractorName" to contractorName.trim().take(80),
            "companyName" to companyName.trim().take(80),
            "licenseNumber" to licenseNumber.trim().take(40),
            "iicrcCertNumber" to iicrcCertNumber.trim().take(40),
            "isRestorationCompany" to isRestorationCompany,
            "city" to city.trim().take(80).ifBlank { "Los Angeles" },
            "complaintCategory" to complaintCategory,
            "summary" to summary.trim().take(2000),
            "evidenceNotes" to evidenceNotes.trim().take(600),
            "proofPhotoBase64" to cleanPhoto,
            "routedToSocalCslb" to true,
            "routedToIicrc" to isRestorationCompany,
            "cslbIntakeUrl" to WaterTableCatalogs.CSLB_COMPLAINT_URL,
            "iicrcIntakeUrl" to WaterTableCatalogs.IICRC_COMPLAINT_URL,
            "status" to "filed",
            "isPublic" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            val saved = docRef.get().await().toObject(
                ContractorComplaint::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: ContractorComplaint(
                id = complaintId,
                userId = uid,
                complainantName = complainantName,
                contractorUserId = contractorUserId.ifBlank { "external" },
                contractorName = contractorName.trim(),
                companyName = companyName.trim(),
                licenseNumber = licenseNumber.trim(),
                iicrcCertNumber = iicrcCertNumber.trim(),
                isRestorationCompany = isRestorationCompany,
                city = city.trim().ifBlank { "Los Angeles" },
                complaintCategory = complaintCategory,
                summary = summary.trim(),
                evidenceNotes = evidenceNotes.trim(),
                proofPhotoBase64 = cleanPhoto,
                routedToSocalCslb = true,
                routedToIicrc = isRestorationCompany,
                cslbIntakeUrl = WaterTableCatalogs.CSLB_COMPLAINT_URL,
                iicrcIntakeUrl = WaterTableCatalogs.IICRC_COMPLAINT_URL,
                status = "filed",
                isPublic = true
            )
            Result.success(saved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateComplaintStatus(complaintId: String, nextStatus: String, evidenceNotes: String? = null): Result<Unit> {
        requireUserId()
        val docRef = db.collection(COLLECTION_CONTRACTOR_COMPLAINTS).document(complaintId)
        val updates = mutableMapOf<String, Any>(
            "status" to nextStatus,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (evidenceNotes != null) {
            updates["evidenceNotes"] = evidenceNotes.trim().take(600)
        }
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Insurance Carrier Profiles & Streamlined Claims Coordination ---

    fun observeActiveInsuranceCarriers(): Flow<List<InsuranceCarrierProfile>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_INSURANCE_CARRIERS
        emitAll(
            db.collection(COLLECTION_INSURANCE_CARRIERS)
                .whereEqualTo("isActive", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        InsuranceCarrierProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.carrierName }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun registerOrUpdateInsuranceCarrier(
        carrierName: String,
        adjusterName: String,
        licenseOrNaicCode: String,
        claimsEmail: String,
        claimsPhone: String,
        claimsPortalUrl: String,
        serviceRegions: List<String>,
        supportsDirectRestorationBilling: Boolean,
        avgTurnaroundHours: Int,
        bio: String
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection(COLLECTION_INSURANCE_CARRIERS).document(uid)
        val cleanRegions = serviceRegions.map { it.trim() }.filter { it.isNotBlank() }.take(10)
            .ifEmpty { listOf("Southern California") }
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "carrierName" to carrierName.trim().take(100),
                        "adjusterName" to adjusterName.trim().take(60),
                        "licenseOrNaicCode" to licenseOrNaicCode.trim().take(40),
                        "claimsEmail" to claimsEmail.trim().take(100),
                        "claimsPhone" to claimsPhone.trim().take(30),
                        "claimsPortalUrl" to claimsPortalUrl.trim().take(250),
                        "serviceRegions" to cleanRegions,
                        "supportsDirectRestorationBilling" to supportsDirectRestorationBilling,
                        "avgTurnaroundHours" to avgTurnaroundHours.coerceIn(1, 336),
                        "bio" to bio.trim().take(600),
                        "isActive" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "carrierName" to carrierName.trim().take(100),
                        "adjusterName" to adjusterName.trim().take(60),
                        "licenseOrNaicCode" to licenseOrNaicCode.trim().take(40),
                        "claimsEmail" to claimsEmail.trim().take(100),
                        "claimsPhone" to claimsPhone.trim().take(30),
                        "claimsPortalUrl" to claimsPortalUrl.trim().take(250),
                        "serviceRegions" to cleanRegions,
                        "supportsDirectRestorationBilling" to supportsDirectRestorationBilling,
                        "avgTurnaroundHours" to avgTurnaroundHours.coerceIn(1, 336),
                        "bio" to bio.trim().take(600),
                        "isActive" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeMyClientClaims(): Flow<List<InsuranceClaimThread>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_INSURANCE_CLAIMS
        emitAll(
            db.collection(COLLECTION_INSURANCE_CLAIMS)
                .whereEqualTo("clientUserId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        InsuranceClaimThread::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeContractorClaims(): Flow<List<InsuranceClaimThread>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_INSURANCE_CLAIMS
        emitAll(
            db.collection(COLLECTION_INSURANCE_CLAIMS)
                .whereEqualTo("contractorUserId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        InsuranceClaimThread::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeInsurerClaims(): Flow<List<InsuranceClaimThread>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_INSURANCE_CLAIMS
        emitAll(
            db.collection(COLLECTION_INSURANCE_CLAIMS)
                .whereEqualTo("insurerUserId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        InsuranceClaimThread::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun createOrLinkInsuranceClaim(
        contractorUserId: String,
        contractorName: String,
        insurerUserId: String,
        insurerName: String,
        claimNumber: String,
        policyType: String,
        deductibleUsd: Int,
        reportId: String,
        consultationId: String,
        city: String,
        lossCategory: String,
        contractorSuggestion: String,
        estimatedScopeUsd: Int,
        initialNote: String
    ): Result<String> {
        val uid = requireUserId()
        val clientName = (auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Policyholder").take(60)
        val claimId = "clm_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection(COLLECTION_INSURANCE_CLAIMS).document(claimId)
        val starterStatus = if (contractorSuggestion.isNotBlank()) "contractor_scoped" else "linked"
        val logEntries = listOfNotNull(
            "Client linked insurance (${insurerName.trim()}, Claim #${claimNumber.trim()})",
            initialNote.trim().takeIf { it.isNotBlank() }?.let { "Client Note: ${it.take(260)}" }
        )
        val payload = mapOf(
            "clientUserId" to uid,
            "clientName" to clientName,
            "contractorUserId" to contractorUserId.trim().take(128),
            "contractorName" to contractorName.trim().take(60),
            "insurerUserId" to insurerUserId.trim().take(128).ifBlank { "external_carrier" },
            "insurerName" to insurerName.trim().take(100),
            "claimNumber" to claimNumber.trim().take(60),
            "policyType" to policyType,
            "deductibleUsd" to deductibleUsd.coerceIn(0, 100000),
            "reportId" to reportId.trim().take(128),
            "consultationId" to consultationId.trim().take(128),
            "city" to city.trim().take(80).ifBlank { "Local" },
            "lossCategory" to lossCategory.ifBlank { "water_restoration" },
            "contractorSuggestion" to contractorSuggestion.trim().take(2000),
            "estimatedScopeUsd" to estimatedScopeUsd.coerceIn(0, 250000),
            "approvedCoverageUsd" to 0,
            "claimStatus" to starterStatus,
            "lastMessageFromRole" to "client",
            "communicationLog" to logEntries,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(claimId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun postInsuranceClaimUpdate(
        claim: InsuranceClaimThread,
        senderRole: String,
        message: String,
        nextStatus: String? = null,
        updatedSuggestion: String? = null,
        updatedEstimateUsd: Int? = null,
        approvedCoverageUsd: Int? = null,
        assignContractorUid: String? = null,
        assignContractorName: String? = null
    ): Result<Unit> {
        requireUserId()
        val docRef = db.collection(COLLECTION_INSURANCE_CLAIMS).document(claim.id)
        val cleanMsg = message.trim().take(320)
        val tag = when (senderRole) {
            "contractor" -> "Contractor"
            "insurer" -> "Insurer"
            else -> "Client"
        }
        val nextLog = (claim.communicationLog + "$tag: $cleanMsg").takeLast(25)
        val updates = mutableMapOf<String, Any>(
            "lastMessageFromRole" to senderRole,
            "communicationLog" to nextLog,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (nextStatus != null) updates["claimStatus"] = nextStatus
        if (updatedSuggestion != null) updates["contractorSuggestion"] = updatedSuggestion.trim().take(2000)
        if (updatedEstimateUsd != null) updates["estimatedScopeUsd"] = updatedEstimateUsd.coerceIn(0, 250000)
        if (approvedCoverageUsd != null) updates["approvedCoverageUsd"] = approvedCoverageUsd.coerceIn(0, 250000)
        if (assignContractorUid != null) updates["contractorUserId"] = assignContractorUid.trim().take(128)
        if (assignContractorName != null) updates["contractorName"] = assignContractorName.trim().take(60)
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Saved Monitored Houses & Businesses (Zone History & Priority Alerts) ---

    fun observeMySavedProperties(): Flow<List<SavedMonitoredProperty>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_SAVED_PROPERTIES
        emitAll(
            db.collection(COLLECTION_SAVED_PROPERTIES)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SavedMonitoredProperty::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun saveMonitoredProperty(
        label: String,
        propertyType: String,
        address: String,
        city: String,
        state: String,
        latitude: Double,
        longitude: Double,
        femaFloodZone: String,
        fireHazardZone: String,
        waterTableDepthFt: Double,
        hazardSummary: String,
        notifyPriorityAlerts: Boolean
    ): Result<String> {
        val uid = requireUserId()
        val propId = "prop_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection(COLLECTION_SAVED_PROPERTIES).document(propId)
        val payload = mapOf(
            "userId" to uid,
            "label" to label.trim().take(60).ifBlank { "My Property" },
            "propertyType" to if (propertyType == "business") "business" else "house",
            "address" to address.trim().take(160),
            "city" to city.trim().take(80).ifBlank { "Local" },
            "state" to state.trim().take(40),
            "latitude" to latitude.coerceIn(-90.0, 90.0),
            "longitude" to longitude.coerceIn(-180.0, 180.0),
            "femaFloodZone" to femaFloodZone.trim().take(120).ifBlank { "Unknown" },
            "fireHazardZone" to fireHazardZone.trim().take(120).ifBlank { "Standard" },
            "waterTableDepthFt" to waterTableDepthFt.coerceIn(0.0, 200.0),
            "hazardSummary" to hazardSummary.trim().take(800),
            "notifyPriorityAlerts" to notifyPriorityAlerts,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(propId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun togglePropertyAlert(propertyId: String, enabled: Boolean): Result<Unit> {
        requireUserId()
        val docRef = db.collection(COLLECTION_SAVED_PROPERTIES).document(propertyId)
        return try {
            docRef.update(
                mapOf(
                    "notifyPriorityAlerts" to enabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteSavedProperty(propertyId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection(COLLECTION_SAVED_PROPERTIES).document(propertyId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Contractor Real Field Product Reviews ---

    fun observeContractorProductReviews(): Flow<List<ContractorProductReview>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_PRODUCT_REVIEWS
        emitAll(
            db.collection(COLLECTION_PRODUCT_REVIEWS)
                .whereEqualTo("isPublic", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ContractorProductReview::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun createContractorProductReview(
        contractorName: String,
        companyName: String,
        licenseNumber: String,
        productSku: String,
        productName: String,
        brandModel: String,
        category: String,
        disasterUseCase: String,
        rating: Int,
        jobsUsedCount: Int,
        pros: String,
        cons: String,
        honestReview: String,
        wouldUseOnInsuredJob: Boolean
    ): Result<String> {
        val uid = requireUserId()
        val revId = "prv_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection(COLLECTION_PRODUCT_REVIEWS).document(revId)
        val payload = mapOf(
            "userId" to uid,
            "contractorName" to contractorName.trim().take(60).ifBlank { "Licensed Contractor" },
            "companyName" to companyName.trim().take(80),
            "licenseNumber" to licenseNumber.trim().take(40),
            "productSku" to productSku.trim().take(60).ifBlank { "custom" },
            "productName" to productName.trim().take(120),
            "brandModel" to brandModel.trim().take(80),
            "category" to category.trim().take(60),
            "disasterUseCase" to disasterUseCase,
            "rating" to rating.coerceIn(1, 5),
            "jobsUsedCount" to jobsUsedCount.coerceIn(1, 5000),
            "pros" to pros.trim().take(400),
            "cons" to cons.trim().take(400),
            "honestReview" to honestReview.trim().take(1500),
            "wouldUseOnInsuredJob" to wouldUseOnInsuredJob,
            "isPublic" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(revId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Remediation Equipment Rentals (Restoration Companies & Residents) ---

    fun observeEquipmentRentals(): Flow<List<EquipmentRentalItem>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_EQUIPMENT_RENTALS
        emitAll(
            db.collection(COLLECTION_EQUIPMENT_RENTALS)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        EquipmentRentalItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<EquipmentRentalItem> { it.isAvailable }
                            .thenByDescending { it.createdAt?.seconds ?: 0L }
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun createEquipmentRental(
        ownerType: String,
        companyName: String,
        equipmentName: String,
        category: String,
        city: String,
        dailyRateUsd: Int,
        depositUsd: Int,
        specs: String,
        contactPhoneOrNote: String
    ): Result<String> {
        val uid = requireUserId()
        val profile = fetchOrCreateUserProfile()
        val rentalId = "rnt_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection(COLLECTION_EQUIPMENT_RENTALS).document(rentalId)
        val payload = mapOf(
            "ownerUserId" to uid,
            "ownerName" to profile.displayName.ifBlank { "Community Member" }.take(60),
            "ownerType" to if (ownerType == "restoration_company") "restoration_company" else "resident",
            "companyName" to companyName.trim().take(80),
            "equipmentName" to equipmentName.trim().take(100),
            "category" to category.trim().take(40).ifBlank { "Dehumidifier" },
            "city" to city.trim().take(80).ifBlank { profile.preferredCity.ifBlank { "Sacramento" } },
            "dailyRateUsd" to dailyRateUsd.coerceIn(5, 2500),
            "depositUsd" to depositUsd.coerceIn(0, 10000),
            "specs" to specs.trim().take(500),
            "contactPhoneOrNote" to contactPhoneOrNote.trim().take(120),
            "isAvailable" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(rentalId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleEquipmentAvailability(rentalId: String, available: Boolean): Result<Unit> {
        requireUserId()
        val docRef = db.collection(COLLECTION_EQUIPMENT_RENTALS).document(rentalId)
        return try {
            docRef.update(
                mapOf(
                    "isAvailable" to available,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteEquipmentRental(rentalId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection(COLLECTION_EQUIPMENT_RENTALS).document(rentalId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Contractor & Community App Revision Suggestions ---

    fun observeAppRevisionSuggestions(): Flow<List<AppRevisionSuggestion>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_APP_SUGGESTIONS
        emitAll(
            db.collection(COLLECTION_APP_SUGGESTIONS)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AppRevisionSuggestion::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<AppRevisionSuggestion> { it.upvotesCount }
                            .thenByDescending { it.createdAt?.seconds ?: 0L }
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun submitAppRevisionSuggestion(
        authorRole: String,
        tradeOrSpecialty: String,
        title: String,
        category: String,
        description: String
    ): Result<String> {
        val uid = requireUserId()
        val profile = fetchOrCreateUserProfile()
        val sugId = "sug_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection(COLLECTION_APP_SUGGESTIONS).document(sugId)
        val validCategory = if (category in listOf("Workflow", "AI Diagnostics", "Safety & Licensing", "Insurance", "Equipment & DIY")) {
            category
        } else "Workflow"
        val validRole = if (authorRole in listOf("contractor", "resident", "insurer", "iicrc_inspector")) {
            authorRole
        } else "resident"
        val payload = mapOf(
            "userId" to uid,
            "authorName" to profile.displayName.ifBlank { "Community Member" }.take(60),
            "authorRole" to validRole,
            "tradeOrSpecialty" to tradeOrSpecialty.trim().take(60),
            "title" to title.trim().take(120),
            "category" to validCategory,
            "description" to description.trim().take(1200),
            "upvotesCount" to 0,
            "status" to "submitted",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(sugId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun upvoteAppRevisionSuggestion(suggestionId: String, currentVotes: Int): Result<Unit> {
        requireUserId()
        val docRef = db.collection(COLLECTION_APP_SUGGESTIONS).document(suggestionId)
        return try {
            docRef.update(
                mapOf(
                    "upvotesCount" to (currentVotes + 1),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Industrial Hygienist Air & Tape Sampling Requests ---

    fun observeMyHygienistRequests(): Flow<List<HygienistSampleRequest>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_HYGIENIST_REQUESTS
        emitAll(
            db.collection(COLLECTION_HYGIENIST_REQUESTS)
                .whereEqualTo("clientUserId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        HygienistSampleRequest::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeOpenHygienistMarketplace(): Flow<List<HygienistSampleRequest>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_HYGIENIST_REQUESTS
        emitAll(
            db.collection(COLLECTION_HYGIENIST_REQUESTS)
                .whereEqualTo("hygienistUserId", "open_marketplace")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        HygienistSampleRequest::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeAssignedHygienistRequests(): Flow<List<HygienistSampleRequest>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_HYGIENIST_REQUESTS
        emitAll(
            db.collection(COLLECTION_HYGIENIST_REQUESTS)
                .whereEqualTo("hygienistUserId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        HygienistSampleRequest::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun createHygienistSampleRequest(
        city: String,
        propertyAddressHint: String,
        sampleTypes: List<String>,
        reasonNotes: String
    ): Result<String> {
        val uid = requireUserId()
        val profile = fetchOrCreateUserProfile()
        val reqId = "hyg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection(COLLECTION_HYGIENIST_REQUESTS).document(reqId)
        val cleanSamples = sampleTypes.map { it.trim() }.filter { it.isNotBlank() }.take(6)
            .ifEmpty { listOf("Air Spore Trap", "Surface Tape Lift") }
        val payload = mapOf(
            "clientUserId" to uid,
            "clientName" to profile.displayName.ifBlank { "Homeowner" }.take(60),
            "hygienistUserId" to "open_marketplace",
            "hygienistName" to "",
            "city" to city.trim().take(80).ifBlank { profile.preferredCity.ifBlank { "Sacramento" } },
            "propertyAddressHint" to propertyAddressHint.trim().take(160),
            "sampleTypes" to cleanSamples,
            "reasonNotes" to reasonNotes.trim().take(800),
            "status" to "requested",
            "quotedFeeUsd" to 0,
            "labFindingsSummary" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(reqId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun claimOrUpdateHygienistRequest(
        requestId: String,
        hygienistName: String,
        status: String,
        quotedFeeUsd: Int,
        labFindingsSummary: String
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection(COLLECTION_HYGIENIST_REQUESTS).document(requestId)
        val validStatus = if (status in listOf("accepted", "sampled", "lab_report_ready")) status else "accepted"
        return try {
            docRef.update(
                mapOf(
                    "hygienistUserId" to uid,
                    "hygienistName" to hygienistName.trim().take(60).ifBlank { "Certified Industrial Hygienist" },
                    "status" to validStatus,
                    "quotedFeeUsd" to quotedFeeUsd.coerceIn(0, 15000),
                    "labFindingsSummary" to labFindingsSummary.trim().take(1500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Direct In-App Insurance Policies & Telemetry Rate Discounts ---

    fun observeMyClientPolicyOffers(): Flow<List<InsurancePolicyOffer>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_INSURANCE_POLICIES
        emitAll(
            db.collection(COLLECTION_INSURANCE_POLICIES)
                .whereEqualTo("clientUserId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        InsurancePolicyOffer::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeOpenInsuranceQuotes(): Flow<List<InsurancePolicyOffer>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_INSURANCE_POLICIES
        emitAll(
            db.collection(COLLECTION_INSURANCE_POLICIES)
                .whereEqualTo("clientUserId", "open_quote")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        InsurancePolicyOffer::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeInsurerIssuedPolicies(): Flow<List<InsurancePolicyOffer>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_INSURANCE_POLICIES
        emitAll(
            db.collection(COLLECTION_INSURANCE_POLICIES)
                .whereEqualTo("insurerUserId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        InsurancePolicyOffer::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun createOrRequestInsurancePolicy(
        isCarrierIssuingQuote: Boolean,
        insurerUserId: String,
        carrierName: String,
        targetClientUserId: String,
        targetClientName: String,
        propertyLabel: String,
        city: String,
        coverageType: String,
        coverageLimitUsd: Int,
        deductibleUsd: Int,
        baseMonthlyPremiumUsd: Int,
        telemetryDiscountPercent: Int,
        policyTermsSummary: String
    ): Result<String> {
        val uid = requireUserId()
        val profile = fetchOrCreateUserProfile()
        val polId = "pol_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection(COLLECTION_INSURANCE_POLICIES).document(polId)
        val safeBase = baseMonthlyPremiumUsd.coerceIn(10, 25000)
        val safeDisc = telemetryDiscountPercent.coerceIn(0, 45)
        val discounted = ((safeBase * (100 - safeDisc)) / 100).coerceAtLeast(10)
        val validCov = if (coverageType in listOf("homeowners_ho3", "flood_nfip", "wildfire_dic", "landlord")) {
            coverageType
        } else "homeowners_ho3"
        val payload = mapOf(
            "insurerUserId" to if (isCarrierIssuingQuote) uid else insurerUserId.ifBlank { "external_carrier" },
            "carrierName" to carrierName.trim().take(100).ifBlank { "Partner Carrier" },
            "clientUserId" to if (isCarrierIssuingQuote) targetClientUserId.ifBlank { "open_quote" } else uid,
            "clientName" to if (isCarrierIssuingQuote) targetClientName.trim().take(60) else profile.displayName.ifBlank { "Homeowner" }.take(60),
            "propertyLabel" to propertyLabel.trim().take(120).ifBlank { "Primary Residence" },
            "city" to city.trim().take(80).ifBlank { profile.preferredCity.ifBlank { "Sacramento" } },
            "coverageType" to validCov,
            "coverageLimitUsd" to coverageLimitUsd.coerceIn(10000, 10000000),
            "deductibleUsd" to deductibleUsd.coerceIn(0, 100000),
            "baseMonthlyPremiumUsd" to safeBase,
            "telemetryDiscountPercent" to safeDisc,
            "discountedMonthlyPremiumUsd" to discounted,
            "policyTermsSummary" to policyTermsSummary.trim().take(1000),
            "status" to if (isCarrierIssuingQuote) "quoted" else "requested",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(polId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun respondToInsurancePolicyOffer(
        policyId: String,
        newStatus: String,
        asClientAccepting: Boolean
    ): Result<Unit> {
        val uid = requireUserId()
        val profile = fetchOrCreateUserProfile()
        val docRef = db.collection(COLLECTION_INSURANCE_POLICIES).document(policyId)
        val updates = if (asClientAccepting) {
            mapOf(
                "clientUserId" to uid,
                "clientName" to profile.displayName.ifBlank { "Homeowner" }.take(60),
                "status" to if (newStatus == "client_accepted") "client_accepted" else "declined",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        } else {
            mapOf(
                "status" to if (newStatus in listOf("quoted", "bound", "declined")) newStatus else "bound",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        }
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Top User Paid Town Field Scout Applications ---

    fun observeMyFieldScoutApplications(): Flow<List<FieldScoutApplication>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_FIELD_SCOUT_APPS
        emitAll(
            db.collection(COLLECTION_FIELD_SCOUT_APPS)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FieldScoutApplication::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun submitFieldScoutApplication(
        applicantName: String,
        city: String,
        scoutScore: Int,
        verifiedReportsCount: Int,
        availabilitySummary: String,
        coverageNotes: String
    ): Result<String> {
        val uid = requireUserId()
        val profile = fetchOrCreateUserProfile()
        val appId = "sct_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection(COLLECTION_FIELD_SCOUT_APPS).document(appId)
        val payload = mapOf(
            "userId" to uid,
            "applicantName" to applicantName.trim().take(60).ifBlank { profile.displayName.ifBlank { "Field Scout" } },
            "city" to city.trim().take(80).ifBlank { profile.preferredCity.ifBlank { "Sacramento" } },
            "scoutScore" to scoutScore.coerceIn(0, 1000000),
            "verifiedReportsCount" to verifiedReportsCount.coerceIn(0, 100000),
            "availabilitySummary" to availabilitySummary.trim().take(300).ifBlank { "Weekdays & weekends" },
            "coverageNotes" to coverageNotes.trim().take(800),
            "status" to "applied",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(appId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_REPORTS = "water_reports"
        const val COLLECTION_OUTCOMES = "solution_outcomes"
        const val COLLECTION_CONTRACTORS = "contractors"
        const val COLLECTION_CONSULTATIONS = "consultations"
        const val COLLECTION_CONTRACTOR_REVIEWS = "contractor_reviews"
        const val COLLECTION_CONTRACTOR_PORTFOLIO = "contractor_portfolio"
        const val COLLECTION_MODERATION_QUEUE = "moderation_queue"
        const val COLLECTION_SUPPLY_ORDERS = "supply_orders"
        const val COLLECTION_IICRC_AUDITS = "iicrc_audits"
        const val COLLECTION_CONTRACTOR_COMPLAINTS = "contractor_complaints"
        const val COLLECTION_INSURANCE_CARRIERS = "insurance_carriers"
        const val COLLECTION_INSURANCE_CLAIMS = "insurance_claims"
        const val COLLECTION_SAVED_PROPERTIES = "saved_properties"
        const val COLLECTION_PRODUCT_REVIEWS = "contractor_product_reviews"
        const val COLLECTION_EQUIPMENT_RENTALS = "equipment_rentals"
        const val COLLECTION_APP_SUGGESTIONS = "app_revision_suggestions"
        const val COLLECTION_HYGIENIST_REQUESTS = "hygienist_requests"
        const val COLLECTION_INSURANCE_POLICIES = "insurance_policies"
        const val COLLECTION_FIELD_SCOUT_APPS = "field_scout_applications"
        const val PLATFORM_FEE_PERCENT = 10
    }
}
