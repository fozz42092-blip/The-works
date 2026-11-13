package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.firebase.configureAppCheck
import com.example.data.model.WaterReport
import com.example.data.repository.AiGroundwaterRepository
import com.example.data.repository.AiVideoStudioRepository
import com.example.data.repository.LiveWeatherAndNewsRepository
import com.example.data.repository.LocationRepository
import com.example.data.repository.WaterTableRepository
import com.example.ui.UiState
import com.example.ui.WaterTableViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOut
import com.example.ui.components.AiTutorialGuideSheet
import com.example.ui.screens.AtlasMapScreen
import com.example.ui.screens.ContractorsScreen
import com.example.ui.screens.DiagnoseReportScreen
import com.example.ui.screens.DiyLeakScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.VideoStudioScreen
import com.example.ui.screens.WholesaleStoreScreen
import com.example.ui.theme.WaterTableTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_ATLAS = "atlas"
const val ROUTE_DIAGNOSE = "diagnose"
const val ROUTE_DIY_LEAK = "diy_leak"
const val ROUTE_CONTRACTORS = "contractors"
const val ROUTE_WHOLESALE = "wholesale"
const val ROUTE_PROFILE = "profile"
const val ROUTE_VIDEO_STUDIO = "video_studio"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory Step 1: Configure App Check with launch intent before UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            WaterTableTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        val uid = user.uid
        key(uid) {
            WaterTableMainShell(currentUserId = uid)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterTableMainShell(currentUserId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: WaterTableViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                WaterTableViewModel(
                    repository = WaterTableRepository(db),
                    weatherNewsRepo = LiveWeatherAndNewsRepository(),
                    aiRepo = AiGroundwaterRepository(),
                    videoRepo = AiVideoStudioRepository(),
                    locationRepo = LocationRepository(app),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_ATLAS

    val reportsUi by viewModel.reportsState.collectAsStateWithLifecycle()
    val myReportsUi by viewModel.myReportsState.collectAsStateWithLifecycle()
    val pendingModerationUi by viewModel.pendingModerationState.collectAsStateWithLifecycle()
    val contractorsUi by viewModel.contractorsState.collectAsStateWithLifecycle()
    val sortedContractors by viewModel.sortedContractors.collectAsStateWithLifecycle()
    val matchedContractors by viewModel.matchedContractors.collectAsStateWithLifecycle()
    val problemMatchQuery by viewModel.problemMatchQuery.collectAsStateWithLifecycle()
    val aiMatchConciergeState by viewModel.aiMatchConciergeState.collectAsStateWithLifecycle()
    val reputations by viewModel.contractorReputations.collectAsStateWithLifecycle()
    val reviewsUi by viewModel.contractorReviewsState.collectAsStateWithLifecycle()
    val externalGroups by viewModel.externalGlassWallGroups.collectAsStateWithLifecycle()
    val portfolioUi by viewModel.contractorPortfolioState.collectAsStateWithLifecycle()
    val clientConsultationsUi by viewModel.clientConsultationsState.collectAsStateWithLifecycle()
    val contractorInboxUi by viewModel.contractorInboxState.collectAsStateWithLifecycle()
    val supplyOrdersUi by viewModel.supplyOrdersState.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfileState.collectAsStateWithLifecycle()
    val adaptiveSolutions by viewModel.adaptiveSolutions.collectAsStateWithLifecycle()
    val weatherState by viewModel.weatherNewsState.collectAsStateWithLifecycle()
    val riskSummary by viewModel.streetFloodRiskSummary.collectAsStateWithLifecycle()
    val mediaAttachment by viewModel.mediaAttachment.collectAsStateWithLifecycle()
    val extraPhotos by viewModel.extraPhotos.collectAsStateWithLifecycle()
    val reviewProofAttachment by viewModel.reviewProofAttachment.collectAsStateWithLifecycle()
    val portfolioAttachment by viewModel.portfolioAttachment.collectAsStateWithLifecycle()
    val videoReferencePhoto by viewModel.videoReferenceAttachment.collectAsStateWithLifecycle()
    val videoStudioState by viewModel.videoStudioState.collectAsStateWithLifecycle()
    val videoStudioProgress by viewModel.videoStudioProgress.collectAsStateWithLifecycle()
    val aiDiagnosisState by viewModel.aiDiagnosisState.collectAsStateWithLifecycle()
    val validationErrors by viewModel.reportValidationErrors.collectAsStateWithLifecycle()
    val aiModerationVerdicts by viewModel.aiModerationVerdicts.collectAsStateWithLifecycle()
    val moderatingTicketId by viewModel.moderatingTicketId.collectAsStateWithLifecycle()
    val diyResults by viewModel.diyTestResults.collectAsStateWithLifecycle()
    val kitRecState by viewModel.kitRecommendationState.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val guideMessages by viewModel.guideMessages.collectAsStateWithLifecycle()
    val isGuideStreaming by viewModel.isGuideStreaming.collectAsStateWithLifecycle()
    val completedTutorialSteps by viewModel.completedTutorialSteps.collectAsStateWithLifecycle()

    var preselectedReportForPro by remember { mutableStateOf<WaterReport?>(null) }
    var showGuideSheet by remember { mutableStateOf(false) }
    var videoStudioInitialPrompt by remember { mutableStateOf("") }
    var videoStudioInitialSourceType by remember { mutableStateOf("product_ad") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    fun navigateTab(route: String) {
        navController.navigate(route) {
            popUpTo(ROUTE_ATLAS) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openVideoStudio(prompt: String, sourceType: String) {
        videoStudioInitialPrompt = prompt
        videoStudioInitialSourceType = sourceType
        navController.navigate(ROUTE_VIDEO_STUDIO) {
            launchSingleTop = true
        }
    }

    val pendingCount = (pendingModerationUi as? UiState.Success)?.data?.size ?: 0

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${weatherState.selectedCity} · Hydro & Restoration Watch",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showGuideSheet = true },
                        modifier = Modifier.testTag("open_ai_guide_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SmartToy,
                            contentDescription = "AI Personal Assistant & Tutorial",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { openVideoStudio("", "product_ad") },
                        modifier = Modifier.testTag("open_video_studio_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Movie,
                            contentDescription = "AI Video & Photo Studio"
                        )
                    }
                    IconButton(
                        onClick = {
                            navController.navigate(ROUTE_PROFILE) { launchSingleTop = true }
                        },
                        modifier = Modifier.testTag("open_profile_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (pendingCount > 0) Badge { Text("$pendingCount") }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = "My Profile, Submissions & Moderation"
                            )
                        }
                    }
                    IconButton(
                        onClick = { signOut(credentialManager, scope) },
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign out"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (currentRoute != ROUTE_VIDEO_STUDIO && currentRoute != ROUTE_PROFILE) {
                ExtendedFloatingActionButton(
                    onClick = { showGuideSheet = true },
                    icon = { Icon(Icons.Filled.SmartToy, contentDescription = null) },
                    text = { Text("AI Guide") },
                    modifier = Modifier.testTag("fab_ai_guide")
                )
            }
        },
        bottomBar = {
            if (currentRoute != ROUTE_PROFILE && currentRoute != ROUTE_VIDEO_STUDIO) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == ROUTE_ATLAS,
                        onClick = { navigateTab(ROUTE_ATLAS) },
                        icon = { Icon(Icons.Filled.Map, contentDescription = "Leaflet Map and flood watch") },
                        label = { Text("Atlas") },
                        modifier = Modifier.testTag("nav_atlas")
                    )
                    NavigationBarItem(
                        selected = currentRoute == ROUTE_DIAGNOSE,
                        onClick = { navigateTab(ROUTE_DIAGNOSE) },
                        icon = { Icon(Icons.Filled.AddAPhoto, contentDescription = "AI diagnose and report") },
                        label = { Text("Diagnose") },
                        modifier = Modifier.testTag("nav_diagnose")
                    )
                    NavigationBarItem(
                        selected = currentRoute == ROUTE_DIY_LEAK,
                        onClick = { navigateTab(ROUTE_DIY_LEAK) },
                        icon = { Icon(Icons.Filled.Build, contentDescription = "DIY leak detection") },
                        label = { Text("DIY Leak") },
                        modifier = Modifier.testTag("nav_diy")
                    )
                    NavigationBarItem(
                        selected = currentRoute == ROUTE_CONTRACTORS,
                        onClick = { navigateTab(ROUTE_CONTRACTORS) },
                        icon = { Icon(Icons.Filled.Engineering, contentDescription = "Licensed contractors") },
                        label = { Text("Pros") },
                        modifier = Modifier.testTag("nav_pros")
                    )
                    NavigationBarItem(
                        selected = currentRoute == ROUTE_WHOLESALE,
                        onClick = { navigateTab(ROUTE_WHOLESALE) },
                        icon = {
                            val count = cartItems.sumOf { it.quantity }
                            BadgedBox(
                                badge = {
                                    if (count > 0) Badge { Text("$count") }
                                }
                            ) {
                                Icon(Icons.Filled.LocalShipping, contentDescription = "Wholesale store")
                            }
                        },
                        label = { Text("Supply") },
                        modifier = Modifier.testTag("nav_supply")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            NavHost(
                navController = navController,
                startDestination = ROUTE_ATLAS,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
            ) {
                composable(ROUTE_ATLAS) {
                    AtlasMapScreen(
                        viewModel = viewModel,
                        reportsUi = reportsUi,
                        onNavigateToDiagnose = { navigateTab(ROUTE_DIAGNOSE) },
                        onAskContractorForReport = { report ->
                            preselectedReportForPro = report
                            viewModel.applyProblemMatchFromReport(report)
                            navigateTab(ROUTE_CONTRACTORS)
                        },
                        weatherState = weatherState,
                        riskSummary = riskSummary
                    )
                }
                composable(ROUTE_DIAGNOSE) {
                    DiagnoseReportScreen(
                        viewModel = viewModel,
                        weatherState = weatherState,
                        mediaAttachment = mediaAttachment,
                        extraPhotos = extraPhotos,
                        aiDiagnosisState = aiDiagnosisState,
                        adaptiveSolutions = adaptiveSolutions,
                        validationErrors = validationErrors,
                        isSubmitting = isSubmitting,
                        onNavigateToContractors = { navigateTab(ROUTE_CONTRACTORS) },
                        onOpenVideoStudioWithText = { siteText ->
                            openVideoStudio(siteText, "site_report")
                        },
                        onPublished = { navigateTab(ROUTE_ATLAS) }
                    )
                }
                composable(ROUTE_DIY_LEAK) {
                    DiyLeakScreen(
                        diyResults = diyResults,
                        onAnswerChange = { stepId, ans -> viewModel.setDiyTestAnswer(stepId, ans) },
                        onNavigateToContractors = { eval ->
                            viewModel.applyProblemMatchFromDiy(eval.headline, eval.summary)
                            navigateTab(ROUTE_CONTRACTORS)
                        },
                        onNavigateToWholesale = { navigateTab(ROUTE_WHOLESALE) }
                    )
                }
                composable(ROUTE_CONTRACTORS) {
                    ContractorsScreen(
                        viewModel = viewModel,
                        matchedContractors = matchedContractors,
                        sortedContractors = sortedContractors,
                        contractorsUi = contractorsUi,
                        reputations = reputations,
                        reviewsUi = reviewsUi,
                        externalGroups = externalGroups,
                        portfolioUi = portfolioUi,
                        reportsUi = reportsUi,
                        clientConsultationsUi = clientConsultationsUi,
                        contractorInboxUi = contractorInboxUi,
                        problemMatchQuery = problemMatchQuery,
                        aiMatchConciergeState = aiMatchConciergeState,
                        reviewProofAttachment = reviewProofAttachment,
                        portfolioAttachment = portfolioAttachment,
                        defaultCity = weatherState.selectedCity,
                        preselectedReport = preselectedReportForPro,
                        onClearPreselectedReport = { preselectedReportForPro = null },
                        isSubmitting = isSubmitting
                    )
                }
                composable(ROUTE_WHOLESALE) {
                    WholesaleStoreScreen(
                        viewModel = viewModel,
                        kitRecommendationState = kitRecState,
                        cartItems = cartItems,
                        supplyOrdersUi = supplyOrdersUi,
                        defaultCity = weatherState.selectedCity,
                        isSubmitting = isSubmitting,
                        onOpenVideoStudioWithProduct = { productCopy ->
                            openVideoStudio(productCopy, "product_ad")
                        }
                    )
                }
                composable(ROUTE_PROFILE) {
                    ProfileScreen(
                        viewModel = viewModel,
                        userProfile = userProfile,
                        myReportsUi = myReportsUi,
                        clientConsultationsUi = clientConsultationsUi,
                        reviewsUi = reviewsUi,
                        supplyOrdersUi = supplyOrdersUi,
                        pendingModerationUi = pendingModerationUi,
                        aiModerationVerdicts = aiModerationVerdicts,
                        moderatingTicketId = moderatingTicketId,
                        defaultCity = weatherState.selectedCity,
                        isSubmitting = isSubmitting,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(ROUTE_VIDEO_STUDIO) {
                    VideoStudioScreen(
                        viewModel = viewModel,
                        initialPromptText = videoStudioInitialPrompt,
                        initialSourceType = videoStudioInitialSourceType,
                        videoReferencePhoto = videoReferencePhoto,
                        videoStudioState = videoStudioState,
                        videoStudioProgress = videoStudioProgress,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }

    if (showGuideSheet) {
        AiTutorialGuideSheet(
            currentRoute = currentRoute,
            completedSteps = completedTutorialSteps,
            messages = guideMessages,
            isStreaming = isGuideStreaming,
            onAskQuestion = { q -> viewModel.askGuideAssistant(currentRoute, q) },
            onNavigateToStep = { stepId, targetRoute ->
                viewModel.markTutorialStepDone(stepId)
                when (targetRoute) {
                    ROUTE_PROFILE, ROUTE_VIDEO_STUDIO -> navController.navigate(targetRoute) { launchSingleTop = true }
                    else -> navigateTab(targetRoute)
                }
            },
            onDismiss = { showGuideSheet = false }
        )
    }
}
