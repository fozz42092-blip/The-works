package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.catalog.WaterTableCatalogs
import com.example.data.export.WorkAuthorizationPdfGenerator
import com.example.data.model.AdaptiveSolutionStat
import com.example.data.model.AiMatchConciergeVerdict
import com.example.data.model.AppRevisionSuggestion
import com.example.data.model.BuildingPhotoAiAnalysis
import com.example.data.model.CartItem
import com.example.data.model.CityFloodForecast
import com.example.data.model.ConsultationRequest
import com.example.data.model.ContinuousLearningSnapshot
import com.example.data.model.ContractorComplaint
import com.example.data.model.ContractorCopilotAdvice
import com.example.data.model.ContractorMatchResult
import com.example.data.model.ContractorPortfolioItem
import com.example.data.model.ProblemMatchQuery
import com.example.data.model.ContractorProfile
import com.example.data.model.ContractorProductReview
import com.example.data.model.ContractorReputation
import com.example.data.model.ContractorReview
import com.example.data.model.CslbVerificationResult
import com.example.data.model.EquipmentRentalItem
import com.example.data.model.ExternalContractorGlassWallGroup
import com.example.data.model.FieldScoutApplication
import com.example.data.model.FieldScoutRankEntry
import com.example.data.model.GeneratedVideoClip
import com.example.data.model.GeoCityResult
import com.example.data.model.GeoLocationPoint
import com.example.data.model.GroundedCreditLink
import com.example.data.model.GroundwaterAiDiagnosis
import com.example.data.model.HomeAzAiInspection
import com.example.data.model.HygienistSampleRequest
import com.example.data.model.IicrcAuditRecord
import com.example.data.model.InsuranceCarrierProfile
import com.example.data.model.InsuranceClaimThread
import com.example.data.model.InsurancePolicyOffer
import com.example.data.model.JobKitRecommendation
import com.example.data.model.LiveDisasterEvent
import com.example.data.model.LocalFloodNewsItem
import com.example.data.model.ModerationTicket
import com.example.data.model.NwsAlert
import com.example.data.model.OnlineVerifiedContractorLead
import com.example.data.model.PestControlAiAnalysis
import com.example.data.model.PropertyHazardHistory
import com.example.data.model.SavedMonitoredProperty
import com.example.data.model.SolutionOutcome
import com.example.data.model.StreetFloodRiskSummary
import com.example.data.model.SupplyOrder
import com.example.data.model.UserProfile
import com.example.data.model.WaterReport
import com.example.data.model.WholesaleProduct
import com.example.data.repository.AiGroundwaterRepository
import com.example.data.repository.AiVideoStudioRepository
import com.example.data.repository.CslbBoardRepository
import com.example.data.repository.LiveWeatherAndNewsRepository
import com.example.data.repository.LocationRepository
import com.example.data.repository.WaterTableRepository
import com.example.data.validation.AiModerationVerdict
import com.example.data.validation.SubmissionValidator
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class MediaAttachment(
    val mediaType: String, // "photo" | "video"
    val previewBitmap: Bitmap?,
    val previewBase64: String,
    val videoDurationLabel: String,
    val localUriString: String
)

data class WeatherNewsUiState(
    val isLoading: Boolean = false,
    val selectedCity: String = "Sacramento",
    val selectedState: String = "CA",
    val latitude: Double = 38.5816,
    val longitude: Double = -121.4944,
    val isLiveGps: Boolean = false,
    val addressHint: String = "Downtown / Midtown",
    val forecast: CityFloodForecast? = null,
    val alerts: List<NwsAlert> = emptyList(),
    val newsItems: List<LocalFloodNewsItem> = emptyList(),
    val citySearchResults: List<GeoCityResult> = emptyList(),
    val errorMessage: String? = null
)

data class GuideChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String
)

class WaterTableViewModel(
    private val repository: WaterTableRepository,
    private val weatherNewsRepo: LiveWeatherAndNewsRepository,
    private val aiRepo: AiGroundwaterRepository,
    private val videoRepo: AiVideoStudioRepository,
    private val locationRepo: LocationRepository,
    val currentUserId: String,
    private val cslbRepo: CslbBoardRepository = CslbBoardRepository()
) : ViewModel() {

    private fun unwrapFlowError(error: Throwable, label: String): UiState.Error {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w(TAG, "Error observing $label", root)
        return UiState.Error(root.message ?: "Failed to load $label")
    }

    val reportsState: StateFlow<UiState<List<WaterReport>>> = repository.observeCommunityReports()
        .map<List<WaterReport>, UiState<List<WaterReport>>> { UiState.Success(it) }
        .catch { emit(unwrapFlowError(it, "water reports")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val myReportsState: StateFlow<UiState<List<WaterReport>>> = repository.observeMyReports()
        .map<List<WaterReport>, UiState<List<WaterReport>>> { UiState.Success(it) }
        .catch { emit(unwrapFlowError(it, "my submissions")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val pendingModerationState: StateFlow<UiState<List<ModerationTicket>>> = repository.observePendingModerationQueue()
        .map<List<ModerationTicket>, UiState<List<ModerationTicket>>> { UiState.Success(it) }
        .catch { emit(unwrapFlowError(it, "moderation queue")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val outcomesState: StateFlow<UiState<List<SolutionOutcome>>> = repository.observeSolutionOutcomes()
        .map<List<SolutionOutcome>, UiState<List<SolutionOutcome>>> { UiState.Success(it) }
        .catch { emit(unwrapFlowError(it, "solution outcomes")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val contractorsState: StateFlow<UiState<List<ContractorProfile>>> = repository.observeActiveContractors()
        .map<List<ContractorProfile>, UiState<List<ContractorProfile>>> { UiState.Success(it) }
        .catch { emit(unwrapFlowError(it, "contractors")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val contractorReviewsState: StateFlow<UiState<List<ContractorReview>>> = repository.observeContractorReviews()
        .map<List<ContractorReview>, UiState<List<ContractorReview>>> { UiState.Success(it) }
        .catch { emit(unwrapFlowError(it, "glass wall reviews")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val contractorPortfolioState: StateFlow<UiState<List<ContractorPortfolioItem>>> = repository.observeContractorPortfolio()
        .map<List<ContractorPortfolioItem>, UiState<List<ContractorPortfolioItem>>> { UiState.Success(it) }
        .catch { emit(unwrapFlowError(it, "contractor portfolio")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val clientConsultationsState: StateFlow<UiState<List<ConsultationRequest>>> =
        repository.observeMyClientConsultations()
            .map<List<ConsultationRequest>, UiState<List<ConsultationRequest>>> { UiState.Success(it) }
            .catch { emit(unwrapFlowError(it, "consultations")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val contractorInboxState: StateFlow<UiState<List<ConsultationRequest>>> =
        repository.observeContractorInbox()
            .map<List<ConsultationRequest>, UiState<List<ConsultationRequest>>> { UiState.Success(it) }
            .catch { emit(unwrapFlowError(it, "contractor inbox")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val supplyOrdersState: StateFlow<UiState<List<SupplyOrder>>> =
        repository.observeMySupplyOrders()
            .map<List<SupplyOrder>, UiState<List<SupplyOrder>>> { UiState.Success(it) }
            .catch { emit(unwrapFlowError(it, "supply orders")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val iicrcAuditsState: StateFlow<UiState<List<IicrcAuditRecord>>> =
        repository.observeIicrcAudits()
            .map<List<IicrcAuditRecord>, UiState<List<IicrcAuditRecord>>> { UiState.Success(it) }
            .catch { emit(unwrapFlowError(it, "IICRC audits")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val complaintsState: StateFlow<UiState<List<ContractorComplaint>>> =
        repository.observeContractorComplaints()
            .map<List<ContractorComplaint>, UiState<List<ContractorComplaint>>> { UiState.Success(it) }
            .catch { emit(unwrapFlowError(it, "contractor complaints")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val insuranceCarriersState: StateFlow<UiState<List<InsuranceCarrierProfile>>> =
        repository.observeActiveInsuranceCarriers()
            .map<List<InsuranceCarrierProfile>, UiState<List<InsuranceCarrierProfile>>> { UiState.Success(it) }
            .catch { emit(unwrapFlowError(it, "insurance carriers")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val clientClaimsState: StateFlow<UiState<List<InsuranceClaimThread>>> =
        repository.observeMyClientClaims()
            .map<List<InsuranceClaimThread>, UiState<List<InsuranceClaimThread>>> { UiState.Success(it) }
            .catch { emit(unwrapFlowError(it, "client insurance claims")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val contractorClaimsState: StateFlow<UiState<List<InsuranceClaimThread>>> =
        repository.observeContractorClaims()
            .map<List<InsuranceClaimThread>, UiState<List<InsuranceClaimThread>>> { UiState.Success(it) }
            .catch { emit(unwrapFlowError(it, "contractor insurance claims")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val insurerClaimsState: StateFlow<UiState<List<InsuranceClaimThread>>> =
        repository.observeInsurerClaims()
            .map<List<InsuranceClaimThread>, UiState<List<InsuranceClaimThread>>> { UiState.Success(it) }
            .catch { emit(unwrapFlowError(it, "insurer claim inbox")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val savedPropertiesState: StateFlow<UiState<List<SavedMonitoredProperty>>> =
        repository.observeMySavedProperties()
            .map<List<SavedMonitoredProperty>, UiState<List<SavedMonitoredProperty>>> { UiState.Success(it) }
            .catch { emit(unwrapFlowError(it, "saved properties")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val productReviewsState: StateFlow<UiState<List<ContractorProductReview>>> =
        repository.observeContractorProductReviews()
            .map<List<ContractorProductReview>, UiState<List<ContractorProductReview>>> { UiState.Success(it) }
            .catch { emit(unwrapFlowError(it, "contractor product field reviews")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val equipmentRentalsState: StateFlow<UiState<List<EquipmentRentalItem>>> =
        repository.observeEquipmentRentals()
            .map<List<EquipmentRentalItem>, UiState<List<EquipmentRentalItem>>> { UiState.Success(it) }
            .catch { emit(unwrapFlowError(it, "equipment rentals")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val appSuggestionsState: StateFlow<UiState<List<AppRevisionSuggestion>>> =
        repository.observeAppRevisionSuggestions()
            .map<List<AppRevisionSuggestion>, UiState<List<AppRevisionSuggestion>>> { UiState.Success(it) }
            .catch { emit(unwrapFlowError(it, "app revision suggestions")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val myHygienistRequestsState: StateFlow<UiState<List<HygienistSampleRequest>>> =
        repository.observeMyHygienistRequests()
            .map<List<HygienistSampleRequest>, UiState<List<HygienistSampleRequest>>> { UiState.Success(it) }
            .catch { emit(unwrapFlowError(it, "hygienist requests")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val openHygienistRequestsState: StateFlow<UiState<List<HygienistSampleRequest>>> =
        repository.observeOpenHygienistMarketplace()
            .map<List<HygienistSampleRequest>, UiState<List<HygienistSampleRequest>>> { UiState.Success(it) }
            .catch { emit(unwrapFlowError(it, "hygienist marketplace")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val assignedHygienistRequestsState: StateFlow<UiState<List<HygienistSampleRequest>>> =
        repository.observeAssignedHygienistRequests()
            .map<List<HygienistSampleRequest>, UiState<List<HygienistSampleRequest>>> { UiState.Success(it) }
            .catch { emit(unwrapFlowError(it, "assigned hygienist jobs")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val myPolicyOffersState: StateFlow<UiState<List<InsurancePolicyOffer>>> =
        repository.observeMyClientPolicyOffers()
            .map<List<InsurancePolicyOffer>, UiState<List<InsurancePolicyOffer>>> { UiState.Success(it) }
            .catch { emit(unwrapFlowError(it, "my policy offers")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val openPolicyQuotesState: StateFlow<UiState<List<InsurancePolicyOffer>>> =
        repository.observeOpenInsuranceQuotes()
            .map<List<InsurancePolicyOffer>, UiState<List<InsurancePolicyOffer>>> { UiState.Success(it) }
            .catch { emit(unwrapFlowError(it, "open insurance quotes")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val insurerPolicyOffersState: StateFlow<UiState<List<InsurancePolicyOffer>>> =
        repository.observeInsurerIssuedPolicies()
            .map<List<InsurancePolicyOffer>, UiState<List<InsurancePolicyOffer>>> { UiState.Success(it) }
            .catch { emit(unwrapFlowError(it, "insurer policies")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val fieldScoutAppsState: StateFlow<UiState<List<FieldScoutApplication>>> =
        repository.observeMyFieldScoutApplications()
            .map<List<FieldScoutApplication>, UiState<List<FieldScoutApplication>>> { UiState.Success(it) }
            .catch { emit(unwrapFlowError(it, "field scout applications")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _liveDisastersState = MutableStateFlow<UiState<List<LiveDisasterEvent>>>(UiState.Loading)
    val liveDisastersState: StateFlow<UiState<List<LiveDisasterEvent>>> = _liveDisastersState.asStateFlow()

    private val _propertyHistoryState = MutableStateFlow<UiState<PropertyHazardHistory>?>(null)
    val propertyHistoryState: StateFlow<UiState<PropertyHazardHistory>?> = _propertyHistoryState.asStateFlow()

    private val _azHomeInspectionState = MutableStateFlow<UiState<HomeAzAiInspection>?>(null)
    val azHomeInspectionState: StateFlow<UiState<HomeAzAiInspection>?> = _azHomeInspectionState.asStateFlow()

    private val _onlineContractorLeadsState = MutableStateFlow<UiState<List<OnlineVerifiedContractorLead>>?>(null)
    val onlineContractorLeadsState: StateFlow<UiState<List<OnlineVerifiedContractorLead>>?> = _onlineContractorLeadsState.asStateFlow()

    private val _selectedSoilFromBottleTest = MutableStateFlow<String?>(null)
    val selectedSoilFromBottleTest: StateFlow<String?> = _selectedSoilFromBottleTest.asStateFlow()

    val fieldScoutLeaderboard: StateFlow<List<FieldScoutRankEntry>> =
        combine(reportsState, outcomesState) { repUi, outUi ->
            WaterTableCatalogs.computeFieldScoutLeaderboard(
                reports = (repUi as? UiState.Success)?.data.orEmpty(),
                outcomes = (outUi as? UiState.Success)?.data.orEmpty()
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val userTelemetryDiscountPercent: StateFlow<Int> =
        combine(savedPropertiesState, myReportsState, outcomesState) { propsUi, myRepUi, outUi ->
            val props = (propsUi as? UiState.Success)?.data.orEmpty()
            val myReps = (myRepUi as? UiState.Success)?.data.orEmpty()
            val outs = (outUi as? UiState.Success)?.data.orEmpty().filter { it.userId == currentUserId }
            WaterTableCatalogs.computeTelemetryInsuranceDiscount(
                verifiedPropertiesCount = props.size,
                reportsCount = myReps.size,
                outcomesCount = outs.size,
                mitigationScoreBonus = if (myReps.any { it.solutionWorked }) 4 else 0
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    private val _weatherNewsState = MutableStateFlow(WeatherNewsUiState())
    val weatherNewsState: StateFlow<WeatherNewsUiState> = _weatherNewsState.asStateFlow()

    val contractorReputations: StateFlow<Map<String, ContractorReputation>> =
        combine(contractorsState, contractorReviewsState, _weatherNewsState) { prosUi, revUi, weather ->
            val pros = (prosUi as? UiState.Success)?.data.orEmpty()
            val reviews = (revUi as? UiState.Success)?.data.orEmpty()
            WaterTableCatalogs.computeContractorReputations(
                contractors = pros,
                reviews = reviews,
                userLat = weather.latitude,
                userLon = weather.longitude
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    val sortedContractors: StateFlow<List<ContractorProfile>> =
        combine(contractorsState, contractorReputations) { prosUi, reps ->
            val pros = (prosUi as? UiState.Success)?.data.orEmpty()
            pros.sortedWith(
                compareBy<ContractorProfile> { reps[it.userId]?.isBlacklisted == true }
                    .thenByDescending { reps[it.userId]?.helpfulRankScore ?: 0.0 }
                    .thenBy { reps[it.userId]?.distanceMiles ?: 999.0 }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val externalGlassWallGroups: StateFlow<List<ExternalContractorGlassWallGroup>> =
        contractorReviewsState.map { revUi ->
            val reviews = (revUi as? UiState.Success)?.data.orEmpty()
            WaterTableCatalogs.buildExternalGlassWallGroups(reviews)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adaptiveSolutions: StateFlow<List<AdaptiveSolutionStat>> =
        combine(outcomesState, reportsState) { outcomesUi, reportsUi ->
            val outcomes = (outcomesUi as? UiState.Success)?.data.orEmpty()
            val reports = (reportsUi as? UiState.Success)?.data.orEmpty()
            WaterTableCatalogs.computeAdaptiveSolutionStats(outcomes, reports)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val streetFloodRiskSummary: StateFlow<StreetFloodRiskSummary> =
        combine(_weatherNewsState, reportsState) { weather, reportsUi ->
            val reports = (reportsUi as? UiState.Success)?.data.orEmpty()
            weatherNewsRepo.computePreRainStreetFloodRisk(
                city = weather.selectedCity,
                forecast = weather.forecast,
                alerts = weather.alerts,
                reports = reports,
                latitude = weather.latitude,
                longitude = weather.longitude
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            weatherNewsRepo.computePreRainStreetFloodRisk(
                "Sacramento", null, emptyList(), emptyList(), 38.5816, -121.4944
            )
        )

    val continuousLearningSnapshot: StateFlow<ContinuousLearningSnapshot> =
        combine(
            reportsState,
            outcomesState,
            contractorReviewsState,
            iicrcAuditsState,
            adaptiveSolutions
        ) { repUi, outUi, revUi, audUi, adaptive ->
            WaterTableCatalogs.computeContinuousLearningSnapshot(
                reports = (repUi as? UiState.Success)?.data.orEmpty(),
                outcomes = (outUi as? UiState.Success)?.data.orEmpty(),
                reviews = (revUi as? UiState.Success)?.data.orEmpty(),
                audits = (audUi as? UiState.Success)?.data.orEmpty(),
                adaptiveSolutions = adaptive
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            ContinuousLearningSnapshot(0, 0, 0, 0, "Syncing...", "Syncing...", "Live")
        )

    private val _problemMatchQuery = MutableStateFlow(ProblemMatchQuery())
    val problemMatchQuery: StateFlow<ProblemMatchQuery> = _problemMatchQuery.asStateFlow()

    val matchedContractors: StateFlow<List<ContractorMatchResult>> =
        combine(
            _problemMatchQuery,
            contractorsState,
            contractorReputations,
            contractorPortfolioState,
            iicrcAuditsState
        ) { query, prosUi, reps, portUi, audUi ->
            val pros = (prosUi as? UiState.Success)?.data.orEmpty()
            val portfolio = (portUi as? UiState.Success)?.data.orEmpty()
            val audits = (audUi as? UiState.Success)?.data.orEmpty()
            val weatherCity = _weatherNewsState.value.selectedCity
            val effectiveQuery = if (query.city.isBlank()) query.copy(city = weatherCity) else query
            WaterTableCatalogs.matchPerfectContractorsForProblem(
                query = effectiveQuery,
                contractors = pros,
                reputations = reps,
                portfolioItems = portfolio,
                iicrcAudits = audits
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _aiMatchConciergeState = MutableStateFlow<UiState<AiMatchConciergeVerdict>?>(null)
    val aiMatchConciergeState: StateFlow<UiState<AiMatchConciergeVerdict>?> = _aiMatchConciergeState.asStateFlow()

    private val _copilotAdviceState = MutableStateFlow<UiState<ContractorCopilotAdvice>?>(null)
    val copilotAdviceState: StateFlow<UiState<ContractorCopilotAdvice>?> = _copilotAdviceState.asStateFlow()

    private val _cslbCheckState = MutableStateFlow<UiState<CslbVerificationResult>?>(null)
    val cslbCheckState: StateFlow<UiState<CslbVerificationResult>?> = _cslbCheckState.asStateFlow()

    private val _iicrcAiCheckState = MutableStateFlow<Map<String, UiState<Triple<String, String, List<GroundedCreditLink>>>>>(emptyMap())
    val iicrcAiCheckState: StateFlow<Map<String, UiState<Triple<String, String, List<GroundedCreditLink>>>>> = _iicrcAiCheckState.asStateFlow()

    private val _liveScanProofAttachment = MutableStateFlow<MediaAttachment?>(null)
    val liveScanProofAttachment: StateFlow<MediaAttachment?> = _liveScanProofAttachment.asStateFlow()

    private val _complaintProofAttachment = MutableStateFlow<MediaAttachment?>(null)
    val complaintProofAttachment: StateFlow<MediaAttachment?> = _complaintProofAttachment.asStateFlow()

    private val _latestFiledComplaint = MutableStateFlow<ContractorComplaint?>(null)
    val latestFiledComplaint: StateFlow<ContractorComplaint?> = _latestFiledComplaint.asStateFlow()

    private val _userProfileState = MutableStateFlow<UserProfile?>(null)
    val userProfileState: StateFlow<UserProfile?> = _userProfileState.asStateFlow()

    private val _aiDiagnosisState = MutableStateFlow<UiState<GroundwaterAiDiagnosis>?>(null)
    val aiDiagnosisState: StateFlow<UiState<GroundwaterAiDiagnosis>?> = _aiDiagnosisState.asStateFlow()

    private val _mediaAttachment = MutableStateFlow<MediaAttachment?>(null)
    val mediaAttachment: StateFlow<MediaAttachment?> = _mediaAttachment.asStateFlow()

    private val _extraPhotos = MutableStateFlow<List<MediaAttachment>>(emptyList())
    val extraPhotos: StateFlow<List<MediaAttachment>> = _extraPhotos.asStateFlow()

    private val _buildingPhotoAttachment = MutableStateFlow<MediaAttachment?>(null)
    val buildingPhotoAttachment: StateFlow<MediaAttachment?> = _buildingPhotoAttachment.asStateFlow()

    private val _buildingExtraPhotos = MutableStateFlow<List<MediaAttachment>>(emptyList())
    val buildingExtraPhotos: StateFlow<List<MediaAttachment>> = _buildingExtraPhotos.asStateFlow()

    private val _buildingPhotoAnalysisState = MutableStateFlow<UiState<BuildingPhotoAiAnalysis>?>(null)
    val buildingPhotoAnalysisState: StateFlow<UiState<BuildingPhotoAiAnalysis>?> = _buildingPhotoAnalysisState.asStateFlow()

    private val _pestPhotoAttachment = MutableStateFlow<MediaAttachment?>(null)
    val pestPhotoAttachment: StateFlow<MediaAttachment?> = _pestPhotoAttachment.asStateFlow()

    private val _pestExtraPhotos = MutableStateFlow<List<MediaAttachment>>(emptyList())
    val pestExtraPhotos: StateFlow<List<MediaAttachment>> = _pestExtraPhotos.asStateFlow()

    private val _pestAnalysisState = MutableStateFlow<UiState<PestControlAiAnalysis>?>(null)
    val pestAnalysisState: StateFlow<UiState<PestControlAiAnalysis>?> = _pestAnalysisState.asStateFlow()

    private val _reviewProofAttachment = MutableStateFlow<MediaAttachment?>(null)
    val reviewProofAttachment: StateFlow<MediaAttachment?> = _reviewProofAttachment.asStateFlow()

    private val _portfolioAttachment = MutableStateFlow<MediaAttachment?>(null)
    val portfolioAttachment: StateFlow<MediaAttachment?> = _portfolioAttachment.asStateFlow()

    private val _videoReferenceAttachment = MutableStateFlow<MediaAttachment?>(null)
    val videoReferenceAttachment: StateFlow<MediaAttachment?> = _videoReferenceAttachment.asStateFlow()

    private val _videoStudioState = MutableStateFlow<UiState<GeneratedVideoClip>?>(null)
    val videoStudioState: StateFlow<UiState<GeneratedVideoClip>?> = _videoStudioState.asStateFlow()

    private val _videoStudioProgress = MutableStateFlow("")
    val videoStudioProgress: StateFlow<String> = _videoStudioProgress.asStateFlow()

    private val _reportValidationErrors = MutableStateFlow<List<String>>(emptyList())
    val reportValidationErrors: StateFlow<List<String>> = _reportValidationErrors.asStateFlow()

    private val _aiModerationVerdicts = MutableStateFlow<Map<String, AiModerationVerdict>>(emptyMap())
    val aiModerationVerdicts: StateFlow<Map<String, AiModerationVerdict>> = _aiModerationVerdicts.asStateFlow()

    private val _moderatingTicketId = MutableStateFlow<String?>(null)
    val moderatingTicketId: StateFlow<String?> = _moderatingTicketId.asStateFlow()

    private val _diyTestResults = MutableStateFlow<Map<String, String>>(emptyMap())
    val diyTestResults: StateFlow<Map<String, String>> = _diyTestResults.asStateFlow()

    private val _kitRecommendationState = MutableStateFlow<UiState<JobKitRecommendation>?>(null)
    val kitRecommendationState: StateFlow<UiState<JobKitRecommendation>?> = _kitRecommendationState.asStateFlow()

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    // --- AI Personal Assistant Tutorial Guide ---
    private val _guideMessages = MutableStateFlow(
        listOf(
            GuideChatMessage(
                id = "welcome_1",
                isUser = false,
                text = "Hi! I'm your WaterTable AI Personal Assistant. Tap any tutorial step below to jump straight there, or ask me how to map a shallow water table, log construction-in-process checks, verify contractors on the Glass Wall, or create an AI video clip!"
            )
        )
    )
    val guideMessages: StateFlow<List<GuideChatMessage>> = _guideMessages.asStateFlow()

    private val _isGuideStreaming = MutableStateFlow(false)
    val isGuideStreaming: StateFlow<Boolean> = _isGuideStreaming.asStateFlow()

    private val _completedTutorialSteps = MutableStateFlow<Set<String>>(emptySet())
    val completedTutorialSteps: StateFlow<Set<String>> = _completedTutorialSteps.asStateFlow()

    init {
        refreshCityWeatherAndNews("Sacramento", "CA", 38.5816, -121.4944, isGps = false, addressHint = "Downtown / Midtown")
        loadUserProfile()
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun setStatusBanner(message: String) {
        _statusBanner.value = message
    }

    fun markTutorialStepDone(stepId: String) {
        _completedTutorialSteps.update { it + stepId }
    }

    fun askGuideAssistant(currentRoute: String, question: String) {
        val cleanQ = question.trim()
        if (cleanQ.isEmpty() || _isGuideStreaming.value) return
        viewModelScope.launch {
            _isGuideStreaming.value = true
            val userMsgId = "u_${System.currentTimeMillis()}"
            val aiMsgId = "a_${System.currentTimeMillis() + 1}"
            val historySnapshot = _guideMessages.value.map {
                (if (it.isUser) "User" else "Assistant") to it.text
            }
            _guideMessages.update {
                it + GuideChatMessage(userMsgId, isUser = true, text = cleanQ) +
                    GuideChatMessage(aiMsgId, isUser = false, text = "Thinking...")
            }
            try {
                aiRepo.streamTutorialReply(currentRoute, historySnapshot, cleanQ).collect { accumulated ->
                    _guideMessages.update { list ->
                        list.map { msg -> if (msg.id == aiMsgId) msg.copy(text = accumulated) else msg }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Guide assistant failed", e)
                _guideMessages.update { list ->
                    list.map { msg ->
                        if (msg.id == aiMsgId) msg.copy(text = "Assistant error: ${e.localizedMessage ?: "Unable to reply right now."}")
                        else msg
                    }
                }
            } finally {
                _isGuideStreaming.value = false
            }
        }
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            repository.getUserProfile().onSuccess { profile ->
                _userProfileState.value = profile
            }
        }
    }

    fun saveUserProfile(preferredCity: String, rolePreference: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            repository.upsertUserProfile(preferredCity, rolePreference)
                .onSuccess {
                    loadUserProfile()
                    _statusBanner.value = "Profile preferences updated."
                }
                .onFailure {
                    _statusBanner.value = it.localizedMessage ?: "Failed to update profile."
                }
            _isSubmitting.value = false
        }
    }

    fun searchCities(query: String) {
        viewModelScope.launch {
            if (query.trim().length < 2) {
                _weatherNewsState.update { it.copy(citySearchResults = emptyList()) }
                return@launch
            }
            weatherNewsRepo.searchCities(query).onSuccess { cities ->
                _weatherNewsState.update { it.copy(citySearchResults = cities) }
            }
        }
    }

    fun selectCity(city: GeoCityResult) {
        _weatherNewsState.update { it.copy(citySearchResults = emptyList()) }
        refreshCityWeatherAndNews(
            city = city.name,
            state = city.admin1,
            lat = city.latitude,
            lon = city.longitude,
            isGps = false,
            addressHint = "${city.name}, ${city.admin1}".trimEnd(',', ' ')
        )
    }

    fun refreshWithLiveGps() {
        viewModelScope.launch {
            _weatherNewsState.update { it.copy(isLoading = true, errorMessage = null) }
            locationRepo.getCurrentGpsLocation()
                .onSuccess { point: GeoLocationPoint ->
                    refreshCityWeatherAndNews(
                        city = point.city,
                        state = point.stateOrRegion,
                        lat = point.latitude,
                        lon = point.longitude,
                        isGps = true,
                        addressHint = point.addressHint
                    )
                    _statusBanner.value = "GPS stamped: ${point.city} (${String.format(Locale.US, "%.4f, %.4f", point.latitude, point.longitude)})"
                }
                .onFailure { err ->
                    _weatherNewsState.update {
                        it.copy(isLoading = false, errorMessage = err.localizedMessage ?: "GPS unavailable")
                    }
                }
        }
    }

    fun refreshCityWeatherAndNews(
        city: String,
        state: String,
        lat: Double,
        lon: Double,
        isGps: Boolean,
        addressHint: String
    ) {
        viewModelScope.launch {
            _weatherNewsState.update {
                it.copy(
                    isLoading = true,
                    selectedCity = city,
                    selectedState = state,
                    latitude = lat,
                    longitude = lon,
                    isLiveGps = isGps,
                    addressHint = addressHint,
                    errorMessage = null
                )
            }
            val forecastDeferred = async { weatherNewsRepo.fetchCityForecast(city, lat, lon) }
            val alertsDeferred = async { weatherNewsRepo.fetchNwsAlerts(lat, lon) }
            val newsDeferred = async { weatherNewsRepo.fetchLocalFloodNews(city) }

            val forecastRes = forecastDeferred.await()
            val alertsRes = alertsDeferred.await()
            val newsRes = newsDeferred.await()
            val resolvedAlerts = alertsRes.getOrNull() ?: emptyList()
            _weatherNewsState.update { curr ->
                curr.copy(
                    isLoading = false,
                    forecast = forecastRes.getOrNull() ?: curr.forecast,
                    alerts = resolvedAlerts,
                    newsItems = newsRes.getOrNull() ?: emptyList(),
                    errorMessage = forecastRes.exceptionOrNull()?.localizedMessage
                )
            }
            refreshLiveDisastersInternal(city, lat, lon, resolvedAlerts)
        }
    }

    private suspend fun refreshLiveDisastersInternal(
        city: String,
        lat: Double,
        lon: Double,
        alerts: List<NwsAlert>
    ) {
        val props = (savedPropertiesState.value as? UiState.Success)?.data.orEmpty()
        runCatching {
            weatherNewsRepo.fetchLiveDisasters(
                centerLat = lat,
                centerLon = lon,
                city = city,
                alerts = alerts,
                monitoredProperties = props
            )
        }.onSuccess { events ->
            _liveDisastersState.value = UiState.Success(events)
        }.onFailure { err ->
            _liveDisastersState.value = UiState.Error(err.localizedMessage ?: "Live disasters feed unavailable")
        }
    }

    // --- Media Attachments ---

    fun attachMediaFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val mimeType = context.contentResolver.getType(uri).orEmpty()
            val isVideo = mimeType.startsWith("video/")
            val result = withContext(Dispatchers.IO) {
                if (isVideo) extractVideoThumbnail(context, uri) else extractImagePreview(context, uri, 960, 480)
            }
            if (result != null) {
                _mediaAttachment.value = result
            } else {
                _statusBanner.value = "Could not load selected media preview."
            }
        }
    }

    fun addExtraPhotoFromUri(context: Context, uri: Uri) {
        if (_extraPhotos.value.size >= 3) {
            _statusBanner.value = "Maximum of 3 extra damage photos reached."
            return
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { extractImagePreview(context, uri, 720, 360) }
            if (result != null) {
                _extraPhotos.update { (it + result).take(3) }
            }
        }
    }

    fun removeExtraPhotoAt(index: Int) {
        _extraPhotos.update { list -> list.filterIndexed { i, _ -> i != index } }
    }

    fun clearMediaAttachment() {
        _mediaAttachment.value = null
    }

    fun attachBuildingPhotoFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { extractImagePreview(context, uri, 960, 480) }
            if (result != null) {
                _buildingPhotoAttachment.value = result
            } else {
                _statusBanner.value = "Could not load selected building photo."
            }
        }
    }

    fun addBuildingExtraPhotoFromUri(context: Context, uri: Uri) {
        if (_buildingExtraPhotos.value.size >= 3) {
            _statusBanner.value = "Maximum of 4 building photos (1 primary + 3 angles) reached."
            return
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { extractImagePreview(context, uri, 720, 360) }
            if (result != null) {
                _buildingExtraPhotos.update { (it + result).take(3) }
            }
        }
    }

    fun removeBuildingExtraPhotoAt(index: Int) {
        _buildingExtraPhotos.update { list -> list.filterIndexed { i, _ -> i != index } }
    }

    fun clearBuildingPrimaryPhoto() {
        _buildingPhotoAttachment.value = null
        _buildingPhotoAnalysisState.value = null
    }

    fun clearAllBuildingPhotos() {
        _buildingPhotoAttachment.value = null
        _buildingExtraPhotos.value = emptyList()
        _buildingPhotoAnalysisState.value = null
    }

    fun attachPestPhotoFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { extractImagePreview(context, uri, 960, 480) }
            if (result != null) {
                _pestPhotoAttachment.value = result
            } else {
                _statusBanner.value = "Could not load selected pest/entry photo."
            }
        }
    }

    fun addPestExtraPhotoFromUri(context: Context, uri: Uri) {
        if (_pestExtraPhotos.value.size >= 3) {
            _statusBanner.value = "Maximum of 4 pest photos (1 primary + 3 angles) reached."
            return
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { extractImagePreview(context, uri, 720, 360) }
            if (result != null) {
                _pestExtraPhotos.update { (it + result).take(3) }
            }
        }
    }

    fun removePestExtraPhotoAt(index: Int) {
        _pestExtraPhotos.update { list -> list.filterIndexed { i, _ -> i != index } }
    }

    fun clearPestPrimaryPhoto() {
        _pestPhotoAttachment.value = null
    }

    fun clearAllPestPhotos() {
        _pestPhotoAttachment.value = null
        _pestExtraPhotos.value = emptyList()
        _pestAnalysisState.value = null
    }

    fun attachReviewProofFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { extractImagePreview(context, uri, 720, 460) }
            if (result != null) _reviewProofAttachment.value = result
        }
    }

    fun clearReviewProof() {
        _reviewProofAttachment.value = null
    }

    fun attachPortfolioPhotoFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { extractImagePreview(context, uri, 800, 460) }
            if (result != null) _portfolioAttachment.value = result
        }
    }

    fun clearPortfolioPhoto() {
        _portfolioAttachment.value = null
    }

    fun attachLiveScanProofFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { extractImagePreview(context, uri, 800, 420) }
            if (result != null) _liveScanProofAttachment.value = result
        }
    }

    fun clearLiveScanProof() {
        _liveScanProofAttachment.value = null
    }

    fun attachVideoReferencePhotoFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { extractImagePreview(context, uri, 960, 480) }
            if (result != null) _videoReferenceAttachment.value = result
        }
    }

    fun clearVideoReferencePhoto() {
        _videoReferenceAttachment.value = null
    }

    private fun extractImagePreview(context: Context, uri: Uri, previewDim: Int, thumbDim: Int): MediaAttachment? {
        return try {
            val raw = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                ?: return null
            val scaled = scaleBitmap(raw, maxDim = previewDim)
            val thumb = scaleBitmap(raw, maxDim = thumbDim)
            val baos = ByteArrayOutputStream()
            thumb.compress(Bitmap.CompressFormat.JPEG, 65, baos)
            val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            MediaAttachment(
                mediaType = "photo",
                previewBitmap = scaled,
                previewBase64 = base64,
                videoDurationLabel = "",
                localUriString = uri.toString()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode image", e)
            null
        }
    }

    private fun extractVideoThumbnail(context: Context, uri: Uri): MediaAttachment? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val secs = (durationMs / 1000L).coerceAtLeast(1L)
            val durationLabel = String.format(Locale.US, "Video · %02d:%02d", secs / 60, secs % 60)
            val frame = retriever.getFrameAtTime(0L)
            val scaled = frame?.let { scaleBitmap(it, maxDim = 720) }
            val base64 = if (scaled != null) {
                val thumb = scaleBitmap(scaled, maxDim = 480)
                val baos = ByteArrayOutputStream()
                thumb.compress(Bitmap.CompressFormat.JPEG, 65, baos)
                Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            } else ""
            MediaAttachment(
                mediaType = "video",
                previewBitmap = scaled,
                previewBase64 = base64,
                videoDurationLabel = durationLabel,
                localUriString = uri.toString()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode video frame", e)
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun scaleBitmap(src: Bitmap, maxDim: Int): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= maxDim && h <= maxDim) return src
        val ratio = maxDim.toFloat() / maxOf(w, h).toFloat()
        val newW = (w * ratio).toInt().coerceAtLeast(1)
        val newH = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, newW, newH, true)
    }

    // --- AI Diagnosis, Validation & Submission ---

    fun runAiGroundwaterDiagnosis(
        city: String,
        soilType: String,
        constructionStage: String,
        lossCategory: String,
        situationType: String,
        depthFeet: Double,
        lossDescription: String,
        notes: String
    ) {
        viewModelScope.launch {
            _aiDiagnosisState.value = UiState.Loading
            val topStats = adaptiveSolutions.value.filter {
                it.soilType == soilType || it.soilType == "unknown" || soilType == "unknown"
            }.ifEmpty { adaptiveSolutions.value }

            val bitmaps = buildList {
                _mediaAttachment.value?.previewBitmap?.let { add(it) }
                _extraPhotos.value.forEach { extra -> extra.previewBitmap?.let { add(it) } }
            }

            val result = aiRepo.analyzeSiteSituation(
                city = city.ifBlank { _weatherNewsState.value.selectedCity },
                soilType = soilType,
                constructionStage = constructionStage,
                lossCategory = lossCategory,
                situationType = situationType,
                observedDepthFeet = depthFeet,
                lossDescription = lossDescription,
                notes = notes,
                bitmaps = bitmaps,
                topCommunitySolutions = topStats
            )
            result.onSuccess { diag ->
                _aiDiagnosisState.value = UiState.Success(diag)
                val cat = when (lossCategory) {
                    "water_restoration" -> "water_restoration"
                    "mold_mitigation" -> "mold_mitigation"
                    "smoke_damage", "fire_restoration" -> "smoke_fire"
                    else -> if (diag.verdict == "plumbing_leak") "plumbing_leak" else "groundwater"
                }
                _problemMatchQuery.value = ProblemMatchQuery(
                    problemCategory = cat,
                    problemText = lossDescription.ifBlank { diag.summary },
                    constructionStage = constructionStage,
                    depthFeet = diag.estimatedDepthFeet,
                    city = city.ifBlank { _weatherNewsState.value.selectedCity },
                    needsOnSiteCrew = true
                )
                markTutorialStepDone("diagnose")
            }.onFailure { err ->
                _aiDiagnosisState.value = UiState.Error(err.localizedMessage ?: "AI assessment failed")
            }
        }
    }

    fun clearValidationErrors() {
        _reportValidationErrors.value = emptyList()
    }

    fun publishWaterReport(
        city: String,
        stateOrRegion: String,
        addressHint: String,
        latitude: Double,
        longitude: Double,
        depthFeet: Double,
        soilType: String,
        constructionStage: String,
        lossCategory: String,
        situationType: String,
        lossDescription: String,
        notes: String,
        authorRole: String,
        onPublishedSuccess: () -> Unit
    ) {
        val resolvedCity = city.ifBlank { _weatherNewsState.value.selectedCity }
        val resolvedState = stateOrRegion.ifBlank { _weatherNewsState.value.selectedState }
        val resolvedAddress = addressHint.ifBlank { _weatherNewsState.value.addressHint }
        val media = _mediaAttachment.value
        val extras = _extraPhotos.value

        val validation = SubmissionValidator.validateWaterOrLossReport(
            city = resolvedCity,
            stateOrRegion = resolvedState,
            addressHint = resolvedAddress,
            latitude = latitude,
            longitude = longitude,
            depthFeet = depthFeet,
            lossCategory = lossCategory,
            situationType = situationType,
            lossDescription = lossDescription,
            notes = notes,
            hasPrimaryMedia = media != null,
            extraPhotoCount = extras.size
        )

        if (!validation.isValid) {
            _reportValidationErrors.value = validation.completenessErrors
            _statusBanner.value = validation.completenessErrors.first()
            return
        }
        _reportValidationErrors.value = emptyList()

        viewModelScope.launch {
            _isSubmitting.value = true
            val diag = (_aiDiagnosisState.value as? UiState.Success)?.data
            val combinedScope = buildString {
                if (diag?.restorationScope?.isNotBlank() == true) append(diag.restorationScope)
                if (diag?.constructionStageAdvice?.isNotBlank() == true) {
                    if (isNotEmpty()) append("\n")
                    append("Stage Plan: ").append(diag.constructionStageAdvice)
                }
            }
            val result = repository.createWaterReport(
                city = resolvedCity,
                stateOrRegion = resolvedState,
                addressHint = resolvedAddress,
                latitude = latitude,
                longitude = longitude,
                depthFeet = diag?.estimatedDepthFeet ?: depthFeet,
                soilType = soilType,
                constructionStage = constructionStage,
                lossCategory = lossCategory,
                situationType = situationType,
                waterSourceVerdict = diag?.verdict ?: when (lossCategory) {
                    "water_restoration" -> "water_loss_mitigation"
                    "mold_mitigation" -> "mold_mitigation"
                    "smoke_damage", "fire_restoration" -> "smoke_fire_mitigation"
                    else -> "uncertain"
                },
                aiConfidence = diag?.confidencePercent ?: 0,
                aiSummary = diag?.summary ?: "Resident/Contractor field observation.",
                lossDescription = lossDescription,
                restorationScope = combinedScope,
                recommendedSolutions = diag?.recommendedSolutions ?: emptyList(),
                notes = notes,
                mediaType = media?.mediaType ?: "none",
                mediaPreviewBase64 = media?.previewBase64 ?: "",
                extraPhotosBase64 = extras.map { it.previewBase64 },
                videoDurationLabel = media?.videoDurationLabel ?: "",
                streetFloodRisk = diag?.streetFloodRisk ?: when {
                    situationType == "street_flooding" -> "high"
                    depthFeet <= 4.0 -> "high"
                    else -> "moderate"
                },
                authorRole = authorRole,
                requiresModeration = validation.requiresModeration,
                plausibilityScore = validation.plausibilityScore,
                completenessIssues = validation.plausibilityWarnings,
                moderationNotes = validation.moderationReason
            )
            _isSubmitting.value = false
            result.onSuccess {
                _mediaAttachment.value = null
                _extraPhotos.value = emptyList()
                _aiDiagnosisState.value = null
                markTutorialStepDone("report_map")
                _statusBanner.value = if (validation.requiresModeration) {
                    "Saved! Photo/detailed reports enter Moderation Queue (viewable in Profile) before going public."
                } else {
                    "GPS-stamped observation published directly to the Leaflet Map!"
                }
                onPublishedSuccess()
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to publish report"
            }
        }
    }

    fun runAiModerationForTicket(ticket: ModerationTicket) {
        viewModelScope.launch {
            _moderatingTicketId.value = ticket.reportId
            val bmp = if (ticket.photoBase64.isNotBlank()) {
                runCatching {
                    val bytes = Base64.decode(ticket.photoBase64, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }.getOrNull()
            } else null

            aiRepo.moderateSubmission(
                city = ticket.city,
                lossCategory = ticket.lossCategory,
                depthFeet = 4.0,
                lossDescription = ticket.previewSummary,
                notes = ticket.aiModerationNotes,
                bitmaps = listOfNotNull(bmp)
            ).onSuccess { verdict ->
                _aiModerationVerdicts.update { it + (ticket.reportId to verdict) }
                _statusBanner.value = if (verdict.isSafeAndRelevant) {
                    "AI screen passed (${verdict.plausibilityScore}% plausible). Ready to approve."
                } else {
                    "AI screen flagged concern: ${verdict.flaggedReasons.firstOrNull() ?: verdict.summary}"
                }
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "AI moderation screening failed"
            }
            _moderatingTicketId.value = null
        }
    }

    fun approveOrFlagModerationTicket(ticket: ModerationTicket, approved: Boolean, notes: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            repository.approveModerationTicket(ticket, approved, notes)
                .onSuccess {
                    _statusBanner.value = if (approved) {
                        "Report approved & released to the public Leaflet Map!"
                    } else {
                        "Report flagged and kept off public Atlas."
                    }
                }
                .onFailure { err ->
                    _statusBanner.value = err.localizedMessage ?: "Only registered contractors can peer-moderate reports."
                }
            _isSubmitting.value = false
        }
    }

    fun deleteMyReport(reportId: String) {
        viewModelScope.launch {
            repository.deleteWaterReport(reportId)
                .onSuccess { _statusBanner.value = "Submission removed." }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Could not delete submission" }
        }
    }

    fun upvoteReport(report: WaterReport) {
        viewModelScope.launch {
            repository.upvoteReport(report)
                .onSuccess { _statusBanner.value = "Confirmed report on the map." }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Could not upvote report" }
        }
    }

    fun logSolutionOutcome(
        report: WaterReport?,
        city: String,
        soilType: String,
        waterSource: String,
        solutionName: String,
        worked: Boolean,
        score: Int,
        costUsd: Int,
        notes: String
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            repository.logSolutionOutcome(
                report = report,
                city = city,
                soilType = soilType,
                waterSource = waterSource,
                solutionName = solutionName,
                worked = worked,
                effectivenessScore = score,
                estimatedCostUsd = costUsd,
                notes = notes
            ).onSuccess {
                _statusBanner.value = "Outcome saved! Adaptive algorithm updated best solutions."
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Failed to save outcome"
            }
            _isSubmitting.value = false
        }
    }

    fun setDiyTestAnswer(stepId: String, value: String) {
        _diyTestResults.update { it + (stepId to value) }
        markTutorialStepDone("diy_leak")
    }

    // --- Problem-Matched Perfect Contractor Finder ---

    fun updateProblemMatchQuery(
        problemCategory: String = _problemMatchQuery.value.problemCategory,
        problemText: String = _problemMatchQuery.value.problemText,
        constructionStage: String = _problemMatchQuery.value.constructionStage,
        depthFeet: Double = _problemMatchQuery.value.depthFeet,
        needsOnSiteCrew: Boolean = _problemMatchQuery.value.needsOnSiteCrew
    ) {
        _problemMatchQuery.update {
            it.copy(
                problemCategory = problemCategory,
                problemText = problemText,
                constructionStage = constructionStage,
                depthFeet = depthFeet,
                city = _weatherNewsState.value.selectedCity,
                needsOnSiteCrew = needsOnSiteCrew
            )
        }
    }

    fun applyProblemMatchFromReport(report: WaterReport) {
        val cat = when (report.lossCategory) {
            "water_restoration" -> "water_restoration"
            "mold_mitigation" -> "mold_mitigation"
            "smoke_damage", "fire_restoration" -> "smoke_fire"
            else -> "groundwater"
        }
        val text = report.lossDescription.ifBlank { report.notes }.ifBlank { report.aiSummary }
        _problemMatchQuery.value = ProblemMatchQuery(
            problemCategory = cat,
            problemText = text,
            constructionStage = report.constructionStage,
            depthFeet = report.depthFeet,
            city = report.city.ifBlank { _weatherNewsState.value.selectedCity },
            needsOnSiteCrew = true
        )
        _aiMatchConciergeState.value = null
    }

    fun applyProblemMatchFromDiy(headline: String, summary: String) {
        val lower = headline.lowercase()
        val cat = when {
            lower.contains("plumbing") -> "plumbing_leak"
            lower.contains("drain") -> "drainage_french_drain"
            else -> "groundwater"
        }
        _problemMatchQuery.value = ProblemMatchQuery(
            problemCategory = cat,
            problemText = "$headline: $summary",
            constructionStage = "none",
            depthFeet = 4.0,
            city = _weatherNewsState.value.selectedCity,
            needsOnSiteCrew = true
        )
        _aiMatchConciergeState.value = null
    }

    fun runAiBestMatchConcierge() {
        val currentQuery = _problemMatchQuery.value.let {
            if (it.city.isBlank()) it.copy(city = _weatherNewsState.value.selectedCity) else it
        }
        val ranked = matchedContractors.value
        viewModelScope.launch {
            _aiMatchConciergeState.value = UiState.Loading
            aiRepo.explainBestContractorMatch(
                problemCategory = currentQuery.problemCategory,
                problemText = currentQuery.problemText.ifBlank { "General water / drainage inspection needed." },
                constructionStage = currentQuery.constructionStage,
                depthFeet = currentQuery.depthFeet,
                city = currentQuery.city,
                rankedCandidates = ranked
            ).onSuccess { verdict ->
                _aiMatchConciergeState.value = UiState.Success(verdict)
                markTutorialStepDone("pros")
            }.onFailure { err ->
                _aiMatchConciergeState.value = UiState.Error(err.localizedMessage ?: "AI Matchmaker unavailable")
            }
        }
    }

    // --- Contractors, Glass Wall & Portfolios ---

    fun checkCslbLicense(licenseNumber: String, licensingState: String) {
        viewModelScope.launch {
            _cslbCheckState.value = UiState.Loading
            val res = cslbRepo.verifyLicenseWithBoard(licenseNumber, licensingState)
            _cslbCheckState.value = UiState.Success(res)
            _statusBanner.value = res.summary
        }
    }

    fun registerContractor(
        displayName: String,
        companyName: String,
        specialty: String,
        licenseNumber: String,
        licensingState: String,
        primaryCity: String,
        serviceCitiesRaw: String,
        feeUsd: Int,
        offersOnSiteWork: Boolean,
        bio: String,
        cslbBoardStatus: String = "format_checked",
        cslbLookupUrl: String = WaterTableCatalogs.buildCslbLookupUrl(licenseNumber),
        iicrcCertNumber: String = "",
        isIicrcGovernor: Boolean = (specialty == "iicrc_inspector"),
        liveScanStatus: String = "not_submitted",
        liveScanAtiRef: String = "",
        liveScanAgency: String = "",
        liveScanClearedDate: String = "",
        existingLiveScanProofBase64: String = ""
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val cities = serviceCitiesRaw.split(",").map { it.trim() }.filter { it.isNotBlank() }
            val weather = _weatherNewsState.value
            val proofB64 = _liveScanProofAttachment.value?.previewBase64?.takeIf { it.isNotBlank() }
                ?: existingLiveScanProofBase64
            val effectiveCslbStatus = (_cslbCheckState.value as? UiState.Success)?.data
                ?.takeIf { it.licenseNumber == licenseNumber.filter { c -> c.isDigit() } || it.licenseNumber == licenseNumber.trim() }
                ?.status ?: cslbBoardStatus

            repository.registerOrUpdateContractorProfile(
                displayName = displayName,
                companyName = companyName,
                specialty = specialty,
                licenseNumber = licenseNumber,
                licensingState = licensingState,
                primaryCity = primaryCity.ifBlank { weather.selectedCity },
                latitude = weather.latitude,
                longitude = weather.longitude,
                serviceCities = cities,
                consultationFeeUsd = feeUsd,
                offersOnSiteWork = offersOnSiteWork,
                bio = bio,
                cslbBoardStatus = effectiveCslbStatus,
                cslbLookupUrl = cslbLookupUrl.ifBlank { WaterTableCatalogs.buildCslbLookupUrl(licenseNumber) },
                iicrcCertNumber = iicrcCertNumber,
                isIicrcGovernor = isIicrcGovernor || specialty == "iicrc_inspector",
                liveScanStatus = liveScanStatus,
                liveScanAtiRef = liveScanAtiRef,
                liveScanAgency = liveScanAgency,
                liveScanClearedDate = liveScanClearedDate,
                liveScanProofBase64 = proofB64
            ).onSuccess {
                _liveScanProofAttachment.value = null
                _statusBanner.value = if (specialty == "iicrc_inspector" || isIicrcGovernor) {
                    "IICRC Technical Governor profile published!"
                } else {
                    "Transparent contractor profile published with CSLB & Live Scan badges!"
                }
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Registration failed"
            }
            _isSubmitting.value = false
        }
    }

    fun requestContractorConsultation(
        contractor: ContractorProfile,
        report: WaterReport,
        question: String,
        serviceMode: String = "remote_advice"
    ) {
        val rep = contractorReputations.value[contractor.userId]
        if (rep?.isBlacklisted == true) {
            _statusBanner.value = "Blocked: This contractor is currently blacklisted on the Glass Wall (${rep.blacklistReason})."
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            repository.requestConsultation(contractor, report, question, serviceMode)
                .onSuccess {
                    markTutorialStepDone("pros")
                    _statusBanner.value = if (serviceMode == "restoration_work") {
                        "Restoration work dispatch sent to ${contractor.displayName}!"
                    } else {
                        "Remote review sent to ${contractor.displayName}!"
                    }
                }.onFailure {
                    _statusBanner.value = it.localizedMessage ?: "Could not submit consultation"
                }
            _isSubmitting.value = false
        }
    }

    fun runContractorCopilot(consultation: ConsultationRequest) {
        viewModelScope.launch {
            _copilotAdviceState.value = UiState.Loading
            val report = (reportsState.value as? UiState.Success)?.data
                ?.firstOrNull { it.id == consultation.reportId }
                ?: (myReportsState.value as? UiState.Success)?.data
                    ?.firstOrNull { it.id == consultation.reportId }
            val topStats = adaptiveSolutions.value.take(5)
            aiRepo.generateContractorClientAnswerAndStrategy(
                consultation = consultation,
                report = report,
                topCommunitySolutions = topStats
            ).onSuccess { advice ->
                _copilotAdviceState.value = UiState.Success(advice)
                _statusBanner.value = "AI + Web Co-Pilot generated answer, strategy & ${advice.creditLinks.size} credit links."
            }.onFailure { err ->
                _copilotAdviceState.value = UiState.Error(err.localizedMessage ?: "AI Co-Pilot failed")
            }
        }
    }

    fun clearCopilotAdvice() {
        _copilotAdviceState.value = null
    }

    fun submitContractorReview(
        consultation: ConsultationRequest,
        verdict: String,
        recommendation: String,
        proposedWorkEstimateUsd: Int = 0,
        contractorStrategy: String = "",
        aiCreditLinks: List<String> = emptyList(),
        workAuthTerms: String = "",
        paymentScheduleSummary: String = ""
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            repository.submitContractorReview(
                consultation = consultation,
                verdict = verdict,
                recommendation = recommendation,
                proposedWorkEstimateUsd = proposedWorkEstimateUsd,
                contractorStrategy = contractorStrategy,
                aiCreditLinks = aiCreditLinks,
                workAuthTerms = workAuthTerms,
                paymentScheduleSummary = paymentScheduleSummary
            ).onSuccess {
                _copilotAdviceState.value = null
                _statusBanner.value = if (workAuthTerms.isNotBlank()) {
                    "Official plan, strategy & Work Authorization sent to client!"
                } else {
                    "Official verdict & restoration scope sent to resident!"
                }
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Could not submit review"
            }
            _isSubmitting.value = false
        }
    }

    fun signWorkAuthorization(consultationId: String, clientSignatureName: String) {
        if (clientSignatureName.trim().length < 2) {
            _statusBanner.value = "Enter your full legal name to sign the Work Authorization."
            return
        }
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        viewModelScope.launch {
            _isSubmitting.value = true
            repository.signWorkAuthorization(consultationId, clientSignatureName, stamp)
                .onSuccess {
                    _statusBanner.value = "Work Authorization signed & certified payment schedule active!"
                }
                .onFailure {
                    _statusBanner.value = it.localizedMessage ?: "Failed to sign work authorization"
                }
            _isSubmitting.value = false
        }
    }

    fun exportWorkAuthorizationPdf(
        context: Context,
        consultation: ConsultationRequest,
        contractor: ContractorProfile?
    ): File? {
        return runCatching {
            WorkAuthorizationPdfGenerator.exportWorkAuthPdf(context, consultation, contractor)
        }.onFailure { err ->
            _statusBanner.value = err.localizedMessage ?: "Failed to generate Work Authorization PDF"
        }.getOrNull()
    }

    fun runIicrcAiVerification(audit: IicrcAuditRecord) {
        viewModelScope.launch {
            _iicrcAiCheckState.update { it + (audit.id to UiState.Loading) }
            aiRepo.verifyIicrcStandardsCompliance(audit)
                .onSuccess { verdict ->
                    _iicrcAiCheckState.update { it + (audit.id to UiState.Success(verdict)) }
                }
                .onFailure { err ->
                    _iicrcAiCheckState.update {
                        it + (audit.id to UiState.Error(err.localizedMessage ?: "IICRC verification failed"))
                    }
                }
        }
    }

    fun submitIicrcGovernanceDecision(
        auditId: String,
        iicrcStatus: String,
        iicrcStandardRef: String,
        iicrcGovernorNotes: String,
        iicrcSources: List<String>
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            repository.governIicrcAudit(
                auditId = auditId,
                iicrcStatus = iicrcStatus,
                iicrcStandardRef = iicrcStandardRef,
                iicrcGovernorNotes = iicrcGovernorNotes,
                iicrcSources = iicrcSources
            ).onSuccess {
                _statusBanner.value = "IICRC Governance decision recorded ($iicrcStatus)!"
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Only registered IICRC Inspectors/Governors can audit others' plans."
            }
            _isSubmitting.value = false
        }
    }

    fun completeAndRateConsultation(consultationId: String, rating: Int) {
        viewModelScope.launch {
            repository.completeAndRateConsultation(consultationId, rating)
                .onSuccess { _statusBanner.value = "Thank you for rating this consultation!" }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to rate" }
        }
    }

    fun submitGlassWallReview(
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
        flagShoddyWork: Boolean,
        onSubmitted: () -> Unit
    ) {
        val proof = _reviewProofAttachment.value
        val validation = SubmissionValidator.validateGlassWallReview(
            contractorName = contractorName,
            city = city,
            overallRating = overallRating,
            communicationRating = communicationRating,
            honestExperience = honestExperience,
            hasProofPhoto = proof != null,
            proofDescription = proofDescription
        )
        if (!validation.isValid) {
            _statusBanner.value = validation.completenessErrors.first()
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            repository.createContractorReview(
                contractorUserId = contractorUserId,
                contractorName = contractorName,
                companyOrLicenseRef = companyOrLicenseRef,
                city = city,
                workCategory = workCategory,
                overallRating = overallRating,
                communicationRating = communicationRating,
                honestExperience = honestExperience,
                proofDescription = proofDescription,
                proofPhotoBase64 = proof?.previewBase64.orEmpty(),
                wouldRecommend = wouldRecommend,
                flagShoddyWork = flagShoddyWork
            ).onSuccess {
                _reviewProofAttachment.value = null
                _statusBanner.value = if (flagShoddyWork) {
                    "Honest Glass Wall warning & proof posted! Blacklist threshold updated."
                } else {
                    "Honest Glass Wall review posted!"
                }
                onSubmitted()
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Failed to post Glass Wall review"
            }
            _isSubmitting.value = false
        }
    }

    fun uploadPortfolioOrReferral(
        contractorName: String,
        entryType: String,
        title: String,
        lossCategory: String,
        city: String,
        clientReferenceName: String,
        description: String,
        completionYear: String,
        onUploaded: () -> Unit
    ) {
        val photo = _portfolioAttachment.value
        viewModelScope.launch {
            _isSubmitting.value = true
            repository.createPortfolioOrReferralItem(
                contractorName = contractorName,
                entryType = entryType,
                title = title,
                lossCategory = lossCategory,
                city = city,
                clientReferenceName = clientReferenceName,
                description = description,
                completionYear = completionYear,
                photoBase64 = photo?.previewBase64.orEmpty()
            ).onSuccess {
                _portfolioAttachment.value = null
                _statusBanner.value = if (entryType == "referral") "Past-client referral added!" else "Portfolio project added!"
                onUploaded()
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Register as a contractor first to add portfolio/referrals."
            }
            _isSubmitting.value = false
        }
    }

    fun deletePortfolioItem(itemId: String) {
        viewModelScope.launch {
            repository.deletePortfolioItem(itemId)
                .onSuccess { _statusBanner.value = "Portfolio item deleted." }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Could not delete portfolio item" }
        }
    }

    // --- AI Video Studio ---

    fun generateAiVideoClip(context: Context, sourceType: String, promptText: String) {
        if (promptText.trim().length < 8) {
            _statusBanner.value = "Enter a script, blog post, or product description (at least 8 characters)."
            return
        }
        viewModelScope.launch {
            _videoStudioState.value = UiState.Loading
            _videoStudioProgress.value = "Initializing Gemini 3.8 Flash & Nano Banana 2.1..."
            val refBmp = _videoReferenceAttachment.value?.previewBitmap
            val result = videoRepo.generateVideoClip(
                context = context.applicationContext,
                sourceType = sourceType,
                promptText = promptText.trim(),
                referencePhoto = refBmp,
                onProgress = { msg -> _videoStudioProgress.value = msg }
            )
            result.onSuccess { clip ->
                _videoStudioState.value = UiState.Success(clip)
                _videoStudioProgress.value = ""
                markTutorialStepDone("video_studio")
            }.onFailure { err ->
                _videoStudioProgress.value = ""
                _videoStudioState.value = UiState.Error(err.localizedMessage ?: "AI video generation failed")
            }
        }
    }

    // --- Wholesale Store & AI Kit Builder ---

    fun generateWholesaleJobKit(jobDescription: String) {
        viewModelScope.launch {
            _kitRecommendationState.value = UiState.Loading
            aiRepo.buildWholesaleJobKit(jobDescription, WaterTableCatalogs.wholesaleProducts)
                .onSuccess { rec ->
                    _kitRecommendationState.value = UiState.Success(rec)
                    markTutorialStepDone("supply")
                }
                .onFailure { err ->
                    _kitRecommendationState.value = UiState.Error(err.localizedMessage ?: "Could not build kit")
                }
        }
    }

    fun addProductToCart(product: WholesaleProduct, quantity: Int = 1) {
        _cartItems.update { list ->
            val existing = list.find { it.product.sku == product.sku }
            if (existing != null) {
                list.map {
                    if (it.product.sku == product.sku) it.copy(quantity = it.quantity + quantity) else it
                }
            } else {
                list + CartItem(product, quantity)
            }
        }
        _statusBanner.value = "Added ${product.name} to wholesale cart"
    }

    fun addRecommendedKitToCart(rec: JobKitRecommendation) {
        val map = WaterTableCatalogs.wholesaleProducts.associateBy { it.sku }
        rec.recommendedSkus.forEach { (sku, qty) ->
            map[sku]?.let { product -> addProductToCart(product, qty) }
        }
        _statusBanner.value = "Full wholesale job kit added to cart!"
    }

    fun updateCartQuantity(sku: String, delta: Int) {
        _cartItems.update { list ->
            list.mapNotNull { item ->
                if (item.product.sku != sku) item
                else {
                    val nextQty = item.quantity + delta
                    if (nextQty <= 0) null else item.copy(quantity = nextQty)
                }
            }
        }
    }

    fun placeWholesaleSupplyOrder(
        jobDescription: String,
        shippingCity: String,
        shippingAddress: String
    ) {
        val items = _cartItems.value
        if (items.isEmpty()) {
            _statusBanner.value = "Your cart is empty."
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            val wholesaleTotal = items.sumOf { it.product.wholesalePriceUsd * it.quantity }
            val retailTotal = items.sumOf { it.product.retailBenchmarkUsd * it.quantity }
            val platformFee = (wholesaleTotal * 0.05)
            val summaries = items.map { "${it.product.name} x${it.quantity} ($${String.format(Locale.US, "%.2f", it.product.wholesalePriceUsd * it.quantity)})" }

            repository.createSupplyOrder(
                jobDescription = jobDescription,
                shippingCity = shippingCity,
                shippingAddress = shippingAddress,
                itemSummaries = summaries,
                wholesaleTotalUsd = wholesaleTotal,
                retailBenchmarkUsd = retailTotal,
                platformFeeUsd = platformFee
            ).onSuccess {
                _cartItems.value = emptyList()
                _statusBanner.value = "Wholesale drop-ship order submitted! Check Order History."
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to submit order"
            }
            _isSubmitting.value = false
        }
    }

    // --- Formal SoCal CSLB + IICRC Contractor Complaints ---

    fun attachComplaintProofFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { extractImagePreview(context, uri, 800, 420) }
            if (result != null) _complaintProofAttachment.value = result
        }
    }

    fun clearComplaintProof() {
        _complaintProofAttachment.value = null
    }

    fun clearLatestFiledComplaint() {
        _latestFiledComplaint.value = null
    }

    fun fileContractorComplaint(
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
        onFiled: (ContractorComplaint) -> Unit = {}
    ) {
        if (contractorName.trim().length < 2 || summary.trim().length < 10) {
            _statusBanner.value = "Provide contractor name and detailed factual summary (min 10 chars)."
            return
        }
        val proof = _complaintProofAttachment.value?.previewBase64.orEmpty()
        viewModelScope.launch {
            _isSubmitting.value = true
            repository.createContractorComplaint(
                contractorUserId = contractorUserId,
                contractorName = contractorName,
                companyName = companyName,
                licenseNumber = licenseNumber,
                iicrcCertNumber = iicrcCertNumber,
                isRestorationCompany = isRestorationCompany,
                city = city.ifBlank { _weatherNewsState.value.selectedCity },
                complaintCategory = complaintCategory,
                summary = summary,
                evidenceNotes = evidenceNotes,
                proofPhotoBase64 = proof
            ).onSuccess { saved ->
                _complaintProofAttachment.value = null
                _latestFiledComplaint.value = saved
                _statusBanner.value = if (saved.routedToIicrc) {
                    "Complaint recorded & ready for instant dispatch to SoCal CSLB + IICRC!"
                } else {
                    "Complaint recorded & ready for instant dispatch to SoCal CSLB!"
                }
                onFiled(saved)
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to submit complaint"
            }
            _isSubmitting.value = false
        }
    }

    fun markComplaintBoardStatus(complaintId: String, nextStatus: String, notes: String? = null) {
        viewModelScope.launch {
            repository.updateComplaintStatus(complaintId, nextStatus, notes)
                .onSuccess {
                    _statusBanner.value = "Complaint status updated ($nextStatus)."
                }
                .onFailure {
                    _statusBanner.value = it.localizedMessage ?: "Could not update complaint status."
                }
        }
    }

    fun exportComplaintPacketPdf(context: Context, complaint: ContractorComplaint): File? {
        return runCatching {
            WorkAuthorizationPdfGenerator.exportComplaintPdf(context, complaint)
        }.onFailure { err ->
            _statusBanner.value = err.localizedMessage ?: "Failed to build Complaint PDF"
        }.getOrNull()
    }

    // --- Insurance Carrier Profiles, Client Policy Linking & Claim Coordination ---

    fun linkClientInsurancePolicy(
        carrierUid: String,
        carrierName: String,
        policyRef: String,
        claimNumber: String,
        deductibleUsd: Int
    ) {
        if (carrierName.trim().length < 2) {
            _statusBanner.value = "Enter or select an insurance company name."
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            repository.linkClientInsurance(
                carrierUid = carrierUid,
                carrierName = carrierName,
                policyRef = policyRef,
                claimNumber = claimNumber,
                deductibleUsd = deductibleUsd,
                fallbackCity = _weatherNewsState.value.selectedCity
            ).onSuccess {
                loadUserProfile()
                _statusBanner.value = "Insurance ($carrierName) linked to your profile & ready for contractor streamlining!"
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Failed to link insurance policy"
            }
            _isSubmitting.value = false
        }
    }

    fun registerInsuranceCarrier(
        carrierName: String,
        adjusterName: String,
        licenseOrNaicCode: String,
        claimsEmail: String,
        claimsPhone: String,
        claimsPortalUrl: String,
        serviceRegionsRaw: String,
        supportsDirectRestorationBilling: Boolean,
        avgTurnaroundHours: Int,
        bio: String
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val regions = serviceRegionsRaw.split(",").map { it.trim() }.filter { it.isNotBlank() }
            repository.registerOrUpdateInsuranceCarrier(
                carrierName = carrierName,
                adjusterName = adjusterName,
                licenseOrNaicCode = licenseOrNaicCode,
                claimsEmail = claimsEmail,
                claimsPhone = claimsPhone,
                claimsPortalUrl = claimsPortalUrl,
                serviceRegions = regions,
                supportsDirectRestorationBilling = supportsDirectRestorationBilling,
                avgTurnaroundHours = avgTurnaroundHours,
                bio = bio
            ).onSuccess {
                _statusBanner.value = "Insurance Company / Adjuster profile published!"
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Failed to publish insurance carrier profile"
            }
            _isSubmitting.value = false
        }
    }

    fun openInsuranceClaimThread(
        contractor: ContractorProfile?,
        insurerUid: String,
        insurerName: String,
        claimNumber: String,
        policyType: String,
        deductibleUsd: Int,
        lossCategory: String,
        reportId: String = "",
        consultationId: String = "",
        contractorSuggestion: String = "",
        estimatedScopeUsd: Int = 0,
        initialNote: String = ""
    ) {
        if (insurerName.trim().length < 2 || claimNumber.trim().length < 2) {
            _statusBanner.value = "Enter Insurance Company and Claim / Policy Reference."
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            repository.createOrLinkInsuranceClaim(
                contractorUserId = contractor?.userId.orEmpty(),
                contractorName = contractor?.displayName.orEmpty(),
                insurerUserId = insurerUid.ifBlank { "external_carrier" },
                insurerName = insurerName,
                claimNumber = claimNumber,
                policyType = policyType,
                deductibleUsd = deductibleUsd,
                reportId = reportId,
                consultationId = consultationId,
                city = _weatherNewsState.value.selectedCity,
                lossCategory = lossCategory,
                contractorSuggestion = contractorSuggestion,
                estimatedScopeUsd = estimatedScopeUsd,
                initialNote = initialNote
            ).onSuccess {
                _statusBanner.value = "Streamlined Claim Thread opened for Client + Contractor + $insurerName!"
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Could not open insurance claim thread"
            }
            _isSubmitting.value = false
        }
    }

    fun sendConsultationToLinkedInsurance(consultation: ConsultationRequest) {
        val profile = _userProfileState.value
        val carrierName = profile?.linkedInsuranceCarrierName?.takeIf { it.isNotBlank() }
        if (carrierName == null) {
            _statusBanner.value = "Link your insurance first in the Insurance tab or Profile to sync this consultation!"
            return
        }
        val carrierUid = profile.linkedInsuranceCarrierUid.ifBlank { "external_carrier" }
        val claimNo = profile.linkedClaimNumber.ifBlank {
            profile.linkedPolicyRef.ifBlank { "CLM-${consultation.id.takeLast(6).uppercase()}" }
        }
        openInsuranceClaimThread(
            contractor = ContractorProfile(
                userId = consultation.contractorUserId,
                displayName = consultation.contractorName
            ),
            insurerUid = carrierUid,
            insurerName = carrierName,
            claimNumber = claimNo,
            policyType = "homeowners_ho3",
            deductibleUsd = profile.linkedDeductibleUsd,
            lossCategory = consultation.lossCategory,
            reportId = consultation.reportId,
            consultationId = consultation.id,
            contractorSuggestion = consultation.engineerRecommendation,
            estimatedScopeUsd = consultation.proposedWorkEstimateUsd,
            initialNote = "Synced contractor scope & Work Authorization for direct insurance streamlining."
        )
    }

    fun postInsuranceClaimMessage(
        claim: InsuranceClaimThread,
        senderRole: String,
        message: String,
        nextStatus: String? = null,
        updatedSuggestion: String? = null,
        updatedEstimateUsd: Int? = null,
        approvedCoverageUsd: Int? = null,
        assignContractor: ContractorProfile? = null
    ) {
        if (message.trim().length < 2 && nextStatus == null && approvedCoverageUsd == null) {
            _statusBanner.value = "Enter a message or update."
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            repository.postInsuranceClaimUpdate(
                claim = claim,
                senderRole = senderRole,
                message = message.ifBlank { "Status updated to ${nextStatus ?: claim.claimStatus}" },
                nextStatus = nextStatus,
                updatedSuggestion = updatedSuggestion,
                updatedEstimateUsd = updatedEstimateUsd,
                approvedCoverageUsd = approvedCoverageUsd,
                assignContractorUid = assignContractor?.userId,
                assignContractorName = assignContractor?.displayName
            ).onSuccess {
                _statusBanner.value = "Claim thread updated for Client, Contractor & Insurer!"
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Failed to update claim thread"
            }
            _isSubmitting.value = false
        }
    }

    // --- Live Disasters, House/Land History, Elevation & Saved Property Watch ---

    fun refreshLiveDisastersNow() {
        val w = _weatherNewsState.value
        viewModelScope.launch {
            _liveDisastersState.value = UiState.Loading
            refreshLiveDisastersInternal(w.selectedCity, w.latitude, w.longitude, w.alerts)
        }
    }

    fun lookupAddressAndGenerateHistory(
        label: String,
        propertyType: String,
        addressQuery: String
    ) {
        viewModelScope.launch {
            _propertyHistoryState.value = UiState.Loading
            val w = _weatherNewsState.value
            val (geo, femaZone, floodDesc) = weatherNewsRepo.lookupAddressAndFemaFloodZone(
                addressQuery = addressQuery.ifBlank { "${w.selectedCity}, ${w.selectedState}" },
                fallbackLat = w.latitude,
                fallbackLon = w.longitude,
                fallbackCity = w.selectedCity,
                fallbackState = w.selectedState
            )
            val (elevationFt, stormDays) = weatherNewsRepo.fetchElevationAndMotherNatureHistory(
                latitude = geo.latitude,
                longitude = geo.longitude
            )
            val reports = (reportsState.value as? UiState.Success)?.data.orEmpty()
            val disasters = (_liveDisastersState.value as? UiState.Success)?.data.orEmpty()
            val nearby = reports.filter {
                WaterTableCatalogs.distanceMiles(geo.latitude, geo.longitude, it.latitude, it.longitude) <= 25.0 ||
                    it.city.equals(geo.name, ignoreCase = true)
            }
            aiRepo.generatePropertyHazardHistoryDossier(
                label = label.ifBlank { addressQuery.ifBlank { geo.name } },
                propertyType = propertyType,
                address = addressQuery.ifBlank { "${geo.name}, ${geo.admin1}" },
                city = geo.name,
                state = geo.admin1,
                latitude = geo.latitude,
                longitude = geo.longitude,
                femaFloodZone = femaZone,
                floodRiskDesc = floodDesc,
                nearbyReports = nearby,
                liveDisasters = disasters,
                elevationFeet = elevationFt,
                historicalStormDays = stormDays
            ).onSuccess { history ->
                _propertyHistoryState.value = UiState.Success(history)
            }.onFailure { err ->
                _propertyHistoryState.value = UiState.Error(err.localizedMessage ?: "Hazard lookup failed")
            }
        }
    }

    fun saveMonitoredPropertyFromHistory(history: PropertyHazardHistory) {
        viewModelScope.launch {
            _isSubmitting.value = true
            repository.saveMonitoredProperty(
                label = history.label,
                propertyType = history.propertyType,
                address = history.address,
                city = history.city,
                state = history.state,
                latitude = history.latitude,
                longitude = history.longitude,
                femaFloodZone = history.femaFloodZone,
                fireHazardZone = history.fireHazardZone,
                waterTableDepthFt = history.nearestReportDepthFt ?: 0.0,
                hazardSummary = "${history.elevationFeet} ft elev · ${history.landBeforeHouseBuilt.ifBlank { history.aiHistoricalDossier }}".take(750),
                notifyPriorityAlerts = true
            ).onSuccess {
                _statusBanner.value = "Property '${history.label}' saved for priority hazard monitoring!"
                refreshLiveDisastersNow()
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Failed to save property"
            }
            _isSubmitting.value = false
        }
    }

    fun toggleSavedPropertyAlert(propertyId: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.togglePropertyAlert(propertyId, enabled)
                .onSuccess { _statusBanner.value = if (enabled) "Priority alerts enabled for property." else "Property alerts paused." }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Could not update property alerts" }
        }
    }

    fun deleteSavedProperty(propertyId: String) {
        viewModelScope.launch {
            repository.deleteSavedProperty(propertyId)
                .onSuccess { _statusBanner.value = "Removed monitored property." }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Could not remove property" }
        }
    }

    // --- Calming A-Z Home AI, Multi-Frame Exterior Video Walkaround, & Online Pro Search ---

    fun setSelectedSoilTypeFromBottleTest(soilTypeKey: String) {
        _selectedSoilFromBottleTest.value = soilTypeKey
        _statusBanner.value = "Applied soil bottle result ($soilTypeKey) to Diagnose & Problem Matcher!"
    }

    fun runAzHomeOrExteriorVideoAi(
        context: Context,
        mode: String,
        problemArea: String,
        userDescription: String
    ) {
        viewModelScope.launch {
            _azHomeInspectionState.value = UiState.Loading
            val w = _weatherNewsState.value
            val bitmaps = withContext(Dispatchers.IO) {
                val list = mutableListOf<Bitmap>()
                val primary = _mediaAttachment.value
                if (primary != null && primary.mediaType == "video" && primary.localUriString.isNotBlank()) {
                    val uri = runCatching { Uri.parse(primary.localUriString) }.getOrNull()
                    if (uri != null) {
                        val retriever = MediaMetadataRetriever()
                        runCatching {
                            retriever.setDataSource(context, uri)
                            val durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                ?.toLongOrNull()?.coerceAtLeast(1000L) ?: 4000L
                            listOf(0.1, 0.35, 0.65, 0.9).forEach { pct ->
                                val frame = retriever.getFrameAtTime((durMs * pct).toLong() * 1000L)
                                if (frame != null) list.add(scaleBitmap(frame, 720))
                            }
                        }
                        runCatching { retriever.release() }
                    }
                }
                if (list.isEmpty()) {
                    primary?.previewBitmap?.let { list.add(it) }
                    _extraPhotos.value.forEach { it.previewBitmap?.let { bmp -> list.add(bmp) } }
                }
                list.take(5)
            }
            val elevPair = weatherNewsRepo.fetchElevationAndMotherNatureHistory(w.latitude, w.longitude)
            aiRepo.analyzeAzHouseOrExteriorVideoProblem(
                mode = mode,
                city = w.selectedCity,
                state = w.selectedState,
                elevationFeet = elevPair.first,
                problemArea = problemArea,
                userDescription = userDescription,
                bitmaps = bitmaps
            ).onSuccess { inspection ->
                _azHomeInspectionState.value = UiState.Success(inspection)
                _problemMatchQuery.value = ProblemMatchQuery(
                    problemCategory = inspection.recommendedSpecialtyKey,
                    problemText = "${inspection.detectedIssueTitle}: ${inspection.scopeOfWork}",
                    constructionStage = "none",
                    depthFeet = 0.0,
                    city = w.selectedCity,
                    needsOnSiteCrew = true
                )
            }.onFailure { err ->
                _azHomeInspectionState.value = UiState.Error(err.localizedMessage ?: "AI house inspector unavailable")
            }
        }
    }

    fun routeAzProblemToPros(specialtyKey: String, summary: String) {
        val w = _weatherNewsState.value
        _problemMatchQuery.value = ProblemMatchQuery(
            problemCategory = specialtyKey,
            problemText = summary,
            constructionStage = "none",
            depthFeet = 0.0,
            city = w.selectedCity,
            needsOnSiteCrew = true
        )
        _aiMatchConciergeState.value = null
    }

    fun runBuildingPhotoAiAnalysis(
        buildingType: String,
        buildingPortion: String,
        notes: String
    ) {
        val bitmaps = buildList {
            _buildingPhotoAttachment.value?.previewBitmap?.let { add(it) }
            _buildingExtraPhotos.value.forEach { extra -> extra.previewBitmap?.let { add(it) } }
        }
        if (bitmaps.isEmpty()) {
            _buildingPhotoAnalysisState.value = UiState.Error(
                "Please take or upload at least one photo of any house or building portion first."
            )
            return
        }
        viewModelScope.launch {
            _buildingPhotoAnalysisState.value = UiState.Loading
            val w = _weatherNewsState.value
            val elevPair = weatherNewsRepo.fetchElevationAndMotherNatureHistory(w.latitude, w.longitude)
            aiRepo.analyzeBuildingOrExteriorPhotos(
                city = w.selectedCity,
                state = w.selectedState,
                elevationFeet = elevPair.first,
                buildingType = buildingType,
                buildingPortion = buildingPortion,
                notes = notes,
                bitmaps = bitmaps
            ).onSuccess { analysis ->
                _buildingPhotoAnalysisState.value = UiState.Success(analysis)
                _problemMatchQuery.value = ProblemMatchQuery(
                    problemCategory = analysis.topRecommendedSpecialtyKey,
                    problemText = "${analysis.inspectedPortion} (${analysis.overallRiskBadge}): ${analysis.summary}",
                    constructionStage = "none",
                    depthFeet = 0.0,
                    city = w.selectedCity,
                    needsOnSiteCrew = true
                )
            }.onFailure { err ->
                _buildingPhotoAnalysisState.value = UiState.Error(
                    err.localizedMessage ?: "Building photo AI inspection failed. Please retry."
                )
            }
        }
    }

    fun routeBuildingIssueToPros(specialtyKey: String, summary: String) {
        val w = _weatherNewsState.value
        _problemMatchQuery.value = ProblemMatchQuery(
            problemCategory = specialtyKey,
            problemText = summary,
            constructionStage = "none",
            depthFeet = 0.0,
            city = w.selectedCity,
            needsOnSiteCrew = true
        )
        _aiMatchConciergeState.value = null
    }

    fun runPestControlAiAnalysis(
        focusType: String,
        userConcern: String
    ) {
        val bitmaps = buildList {
            _pestPhotoAttachment.value?.previewBitmap?.let { add(it) }
            _pestExtraPhotos.value.forEach { extra -> extra.previewBitmap?.let { add(it) } }
        }
        if (bitmaps.isEmpty() && userConcern.isBlank()) {
            _pestAnalysisState.value = UiState.Error(
                "Take or upload a photo of a possible rodent/pest entry point or describe your pest concern first."
            )
            return
        }
        viewModelScope.launch {
            _pestAnalysisState.value = UiState.Loading
            val w = _weatherNewsState.value
            aiRepo.analyzePestOrRodentEntryProblem(
                city = w.selectedCity,
                state = w.selectedState,
                focusType = focusType,
                userConcern = userConcern,
                bitmaps = bitmaps
            ).onSuccess { analysis ->
                _pestAnalysisState.value = UiState.Success(analysis)
                _problemMatchQuery.value = ProblemMatchQuery(
                    problemCategory = "pest_control",
                    problemText = "${analysis.suspectedCurrentProblem}: ${analysis.confidenceSummary}",
                    constructionStage = "none",
                    depthFeet = 0.0,
                    city = w.selectedCity,
                    needsOnSiteCrew = true
                )
            }.onFailure { err ->
                _pestAnalysisState.value = UiState.Error(
                    err.localizedMessage ?: "Pest AI inspection failed. Please retry."
                )
            }
        }
    }

    fun routePestIssueToPros(summary: String) {
        val w = _weatherNewsState.value
        _problemMatchQuery.value = ProblemMatchQuery(
            problemCategory = "pest_control",
            problemText = summary,
            constructionStage = "none",
            depthFeet = 0.0,
            city = w.selectedCity,
            needsOnSiteCrew = true
        )
        _aiMatchConciergeState.value = null
    }

    fun searchOnlineContractors(tradeLabel: String, issueDescription: String) {
        val w = _weatherNewsState.value
        viewModelScope.launch {
            _onlineContractorLeadsState.value = UiState.Loading
            aiRepo.findOnlineReputableContractors(
                tradeLabel = tradeLabel,
                city = w.selectedCity,
                state = w.selectedState,
                issueDescription = issueDescription
            ).onSuccess { leads ->
                _onlineContractorLeadsState.value = UiState.Success(leads)
            }.onFailure { err ->
                _onlineContractorLeadsState.value = UiState.Error(err.localizedMessage ?: "Online contractor search failed")
            }
        }
    }

    // --- Real Contractor Field Product Reviews ---

    fun createContractorProductReview(
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
        wouldUseOnInsuredJob: Boolean,
        onSubmitted: () -> Unit = {}
    ) {
        if (productName.trim().length < 2 || honestReview.trim().length < 5) {
            _statusBanner.value = "Enter product name and honest contractor field review (min 5 chars)."
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            repository.createContractorProductReview(
                contractorName = contractorName,
                companyName = companyName,
                licenseNumber = licenseNumber,
                productSku = productSku,
                productName = productName,
                brandModel = brandModel,
                category = category,
                disasterUseCase = disasterUseCase,
                rating = rating,
                jobsUsedCount = jobsUsedCount,
                pros = pros,
                cons = cons,
                honestReview = honestReview,
                wouldUseOnInsuredJob = wouldUseOnInsuredJob
            ).onSuccess {
                _statusBanner.value = "Contractor field product review published!"
                onSubmitted()
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Could not post product review"
            }
            _isSubmitting.value = false
        }
    }

    // --- Nearby Remediation Equipment Rentals ---

    fun listEquipmentForRent(
        ownerType: String,
        companyName: String,
        equipmentName: String,
        category: String,
        city: String,
        dailyRateUsd: Int,
        depositUsd: Int,
        specs: String,
        contactPhoneOrNote: String,
        onListed: () -> Unit = {}
    ) {
        if (equipmentName.trim().length < 2) {
            _statusBanner.value = "Enter equipment name (e.g., Commercial LGR Dehumidifier)."
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            repository.createEquipmentRental(
                ownerType = ownerType,
                companyName = companyName,
                equipmentName = equipmentName,
                category = category,
                city = city.ifBlank { _weatherNewsState.value.selectedCity },
                dailyRateUsd = dailyRateUsd,
                depositUsd = depositUsd,
                specs = specs,
                contactPhoneOrNote = contactPhoneOrNote
            ).onSuccess {
                _statusBanner.value = "Remediation equipment listed for nearby pickup!"
                onListed()
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Failed to list equipment"
            }
            _isSubmitting.value = false
        }
    }

    fun toggleEquipmentRentalAvailability(rentalId: String, available: Boolean) {
        viewModelScope.launch {
            repository.toggleEquipmentAvailability(rentalId, available)
                .onSuccess { _statusBanner.value = if (available) "Marked equipment Available." else "Marked equipment Currently Rented Out." }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to update rental availability" }
        }
    }

    fun deleteEquipmentRental(rentalId: String) {
        viewModelScope.launch {
            repository.deleteEquipmentRental(rentalId)
                .onSuccess { _statusBanner.value = "Equipment rental removed." }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to delete listing" }
        }
    }

    // --- Contractor & Community App Revision Suggestions ---

    fun submitAppRevisionSuggestion(
        authorRole: String,
        tradeOrSpecialty: String,
        title: String,
        category: String,
        description: String,
        onSubmitted: () -> Unit = {}
    ) {
        if (title.trim().length < 3 || description.trim().length < 5) {
            _statusBanner.value = "Provide a concise revision title & description."
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            repository.submitAppRevisionSuggestion(authorRole, tradeOrSpecialty, title, category, description)
                .onSuccess {
                    _statusBanner.value = "Revision suggestion published for everyone to vote on!"
                    onSubmitted()
                }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to post suggestion" }
            _isSubmitting.value = false
        }
    }

    fun upvoteAppRevisionSuggestion(suggestionId: String, currentVotes: Int) {
        viewModelScope.launch {
            repository.upvoteAppRevisionSuggestion(suggestionId, currentVotes)
                .onSuccess { _statusBanner.value = "Voted for app revision!" }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Vote failed" }
        }
    }

    // --- Industrial Hygienist Air & Tape Sampling ---

    fun submitHygienistSampleRequest(
        city: String,
        propertyAddressHint: String,
        sampleTypes: List<String>,
        reasonNotes: String,
        onSubmitted: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            repository.createHygienistSampleRequest(
                city = city.ifBlank { _weatherNewsState.value.selectedCity },
                propertyAddressHint = propertyAddressHint,
                sampleTypes = sampleTypes,
                reasonNotes = reasonNotes
            ).onSuccess {
                _statusBanner.value = "Air & Tape Sample request posted to local Industrial Hygienists!"
                onSubmitted()
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Failed to request hygienist sample"
            }
            _isSubmitting.value = false
        }
    }

    fun updateHygienistLabResult(
        requestId: String,
        hygienistName: String,
        status: String,
        quotedFeeUsd: Int,
        labFindingsSummary: String
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            repository.claimOrUpdateHygienistRequest(
                requestId = requestId,
                hygienistName = hygienistName,
                status = status,
                quotedFeeUsd = quotedFeeUsd,
                labFindingsSummary = labFindingsSummary
            ).onSuccess {
                _statusBanner.value = "Hygienist sample request updated ($status)."
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Only Industrial Hygienists / IICRC pros can claim sampling requests."
            }
            _isSubmitting.value = false
        }
    }

    // --- Direct In-App Insurance Policy Quotes & Telemetry Rate Discounts ---

    fun requestOrIssueInsurancePolicy(
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
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            repository.createOrRequestInsurancePolicy(
                isCarrierIssuingQuote = isCarrierIssuingQuote,
                insurerUserId = insurerUserId,
                carrierName = carrierName,
                targetClientUserId = targetClientUserId,
                targetClientName = targetClientName,
                propertyLabel = propertyLabel,
                city = city.ifBlank { _weatherNewsState.value.selectedCity },
                coverageType = coverageType,
                coverageLimitUsd = coverageLimitUsd,
                deductibleUsd = deductibleUsd,
                baseMonthlyPremiumUsd = baseMonthlyPremiumUsd,
                telemetryDiscountPercent = telemetryDiscountPercent,
                policyTermsSummary = policyTermsSummary
            ).onSuccess {
                _statusBanner.value = if (isCarrierIssuingQuote) {
                    "Direct in-app policy quote published with $telemetryDiscountPercent% telemetry discount!"
                } else {
                    "Discounted policy quote request sent to $carrierName!"
                }
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Policy request/quote failed"
            }
            _isSubmitting.value = false
        }
    }

    fun respondToInsurancePolicyOffer(
        policyId: String,
        newStatus: String,
        asClientAccepting: Boolean
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            repository.respondToInsurancePolicyOffer(policyId, newStatus, asClientAccepting)
                .onSuccess {
                    _statusBanner.value = "Insurance policy status updated to $newStatus!"
                }
                .onFailure {
                    _statusBanner.value = it.localizedMessage ?: "Failed to update policy offer"
                }
            _isSubmitting.value = false
        }
    }

    // --- Top User Field Scout Application ---

    fun submitFieldScoutApplication(
        applicantName: String,
        city: String,
        scoutScore: Int,
        verifiedReportsCount: Int,
        availabilitySummary: String,
        coverageNotes: String
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            repository.submitFieldScoutApplication(
                applicantName = applicantName,
                city = city.ifBlank { _weatherNewsState.value.selectedCity },
                scoutScore = scoutScore,
                verifiedReportsCount = verifiedReportsCount,
                availabilitySummary = availabilitySummary,
                coverageNotes = coverageNotes
            ).onSuccess {
                _statusBanner.value = "Paid Town Field Scout application submitted! Free subscription earned on approval."
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Failed to submit application"
            }
            _isSubmitting.value = false
        }
    }

    private companion object {
        const val TAG = "WaterTableVM"
    }
}
