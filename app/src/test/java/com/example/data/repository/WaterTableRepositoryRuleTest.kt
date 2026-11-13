package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WaterTableRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveWaterReport_withConstructionStage_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repo = WaterTableRepository(firestore, auth)

        val reportId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createWaterReport(
                city = "Sacramento",
                stateOrRegion = "CA",
                addressHint = "12th & J St",
                latitude = 38.5816,
                longitude = -121.4944,
                depthFeet = 4.0,
                soilType = "clay",
                constructionStage = "in_process",
                lossCategory = "groundwater",
                situationType = "groundwater_seepage",
                waterSourceVerdict = "ground_water_table",
                aiConfidence = 90,
                aiSummary = "Shallow alluvial water table detected before backfill.",
                lossDescription = "Trench hit standing groundwater at 4 ft during remodel excavation.",
                restorationScope = "Install perimeter French drain + vapor barrier before slab pour.",
                recommendedSolutions = listOf("Perimeter French drain", "Battery-backed sump"),
                notes = "Clear water seeping upward along footing.",
                mediaType = "none",
                mediaPreviewBase64 = "",
                videoDurationLabel = "",
                streetFloodRisk = "high",
                requiresModeration = false,
                plausibilityScore = 92
            ).getOrThrow()
        }

        assertTrue(reportId.isNotBlank())
        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) { repo.getWaterReport(reportId).getOrThrow() }
        assertNotNull(fetched)
        assertEquals("Sacramento", fetched?.city)
        assertEquals("in_process", fetched?.constructionStage)

        val communityList = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeCommunityReports().first { list -> list.any { it.id == reportId } }
        }
        assertTrue(communityList.any { it.id == reportId })
    }

    @Test
    fun moderationQueueHold_andPeerContractorApproval_enforced() = runBlocking {
        // Bob registers as a licensed restoration pro
        val bobUid = signInTestUser(BOB_EMAIL)
        val bobRepo = WaterTableRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.registerOrUpdateContractorProfile(
                displayName = "Bob Vance, WRT",
                companyName = "Sierra Mitigation",
                specialty = "full_service_restoration",
                licenseNumber = "IICRC-90112",
                licensingState = "CA",
                primaryCity = "Sacramento",
                latitude = 38.5816,
                longitude = -121.4944,
                serviceCities = listOf("Sacramento", "Elk Grove"),
                consultationFeeUsd = 50,
                offersOnSiteWork = true,
                bio = "Water, mold & smoke mitigation."
            ).getOrThrow()
        }

        // Alice creates a photo/detailed report that goes into pending_review moderation
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = WaterTableRepository(firestore, auth)
        val reportId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createWaterReport(
                city = "Sacramento",
                stateOrRegion = "CA",
                addressHint = "Midtown Blvd",
                latitude = 38.575,
                longitude = -121.485,
                depthFeet = 3.2,
                soilType = "silt",
                constructionStage = "after_remodel",
                lossCategory = "water_restoration",
                situationType = "water_loss",
                waterSourceVerdict = "water_loss_mitigation",
                aiConfidence = 85,
                aiSummary = "Post-remodel flooring buckling from sub-slab vapor pressure.",
                lossDescription = "Hardwood buckled 3 weeks after full remodel finished; drywall wick line at 12 inches.",
                restorationScope = "Non-destructive hardwood mat drying + exterior French drain.",
                recommendedSolutions = listOf("Injectidry floor drying mats", "Exterior curtain drain"),
                notes = "Photo proof attached for moderation review.",
                mediaType = "photo",
                mediaPreviewBase64 = "samplebase64",
                extraPhotosBase64 = listOf("extra1"),
                videoDurationLabel = "",
                streetFloodRisk = "high",
                requiresModeration = true,
                plausibilityScore = 88,
                moderationNotes = "Held for photo & detailed report screening."
            ).getOrThrow()
        }

        // Owner sees it in observeMyReports, but public feed must NOT show it yet
        val mine = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeMyReports().first { list -> list.any { it.id == reportId } }
        }
        assertEquals("pending_review", mine.first { it.id == reportId }.visibility)

        signInTestUser(BOB_EMAIL)
        val publicBefore = withTimeout(FLOW_TIMEOUT_MS) { bobRepo.observeCommunityReports().first() }
        assertFalse(publicBefore.any { it.id == reportId })

        val pendingQueue = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observePendingModerationQueue().first { list -> list.any { it.reportId == reportId } }
        }
        val ticket = pendingQueue.first { it.reportId == reportId }
        assertEquals(bobUid.isNotBlank(), true)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.approveModerationTicket(
                ticket = ticket,
                approved = true,
                moderatorNotes = "Approved by licensed restoration contractor."
            ).getOrThrow()
        }

        val publicAfter = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeCommunityReports().first { list -> list.any { it.id == reportId } }
        }
        assertTrue(publicAfter.any { it.id == reportId && it.engineerVerified })
    }

    @Test
    fun contractorRegistrationAndConsultationFlow_crossUserIsolation_enforced() = runBlocking {
        val bobUid = signInTestUser(BOB_EMAIL)
        val bobRepo = WaterTableRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.registerOrUpdateContractorProfile(
                displayName = "Bob Vance, PE",
                companyName = "Sierra Soil & Geo",
                specialty = "soil_engineer",
                licenseNumber = "PE-90112",
                licensingState = "CA",
                primaryCity = "Sacramento",
                latitude = 38.5816,
                longitude = -121.4944,
                serviceCities = listOf("Sacramento", "Elk Grove"),
                consultationFeeUsd = 50,
                offersOnSiteWork = true,
                bio = "15 yrs geotechnical groundwater & drainage engineering."
            ).getOrThrow()
        }

        val contractors = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeActiveContractors().first { list -> list.any { it.userId == bobUid } }
        }
        val bobProfile = contractors.first { it.userId == bobUid }

        signInTestUser(ALICE_EMAIL)
        val aliceRepo = WaterTableRepository(firestore, auth)
        val reportId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createWaterReport(
                city = "Sacramento",
                stateOrRegion = "CA",
                addressHint = "Midtown Alley",
                latitude = 38.57,
                longitude = -121.48,
                depthFeet = 3.5,
                soilType = "silt",
                constructionStage = "in_process",
                lossCategory = "groundwater",
                situationType = "basement_intrusion",
                waterSourceVerdict = "uncertain",
                aiConfidence = 72,
                aiSummary = "Possible perched water table.",
                lossDescription = "Water ponding inside crawlspace excavation trench.",
                restorationScope = "",
                recommendedSolutions = listOf("Interior drain tile"),
                notes = "Need PE confirmation.",
                mediaType = "none",
                mediaPreviewBase64 = "",
                videoDurationLabel = "",
                streetFloodRisk = "moderate",
                requiresModeration = false
            ).getOrThrow()
        }
        val report = withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.getWaterReport(reportId).getOrThrow()!! }
        val consultationId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.requestConsultation(
                contractor = bobProfile,
                report = report,
                question = "Can you confirm if this is perched groundwater before I pour slab?",
                serviceMode = "restoration_work"
            ).getOrThrow()
        }
        assertTrue(consultationId.isNotBlank())

        // Carol cannot delete Alice's report
        signInTestUser(CAROL_EMAIL)
        val carolRepo = WaterTableRepository(firestore, auth)
        val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) { carolRepo.deleteWaterReport(reportId) }
        assertTrue(deleteResult.isFailure)
        val fsEx = deleteResult.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)

        // Bob submits review + estimate
        signInTestUser(BOB_EMAIL)
        val bobConsultations = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeContractorInbox().first { list -> list.any { it.id == consultationId } }
        }
        val consultation = bobConsultations.first { it.id == consultationId }
        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.submitContractorReview(
                consultation = consultation,
                verdict = "ground_water_table",
                recommendation = "Confirmed seasonal perched groundwater. Use vapor barrier + perimeter French drain.",
                proposedWorkEstimateUsd = 2400
            ).getOrThrow()
        }

        // Alice completes and rates
        signInTestUser(ALICE_EMAIL)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.completeAndRateConsultation(consultationId, 5).getOrThrow()
        }
    }

    @Test
    fun glassWallReviewsAndPortfolio_rulesEnforced() = runBlocking {
        val bobUid = signInTestUser(BOB_EMAIL)
        val bobRepo = WaterTableRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.registerOrUpdateContractorProfile(
                displayName = "Bob Vance, WRT",
                companyName = "Sierra Mitigation",
                specialty = "water_mitigation",
                licenseNumber = "IICRC-1122",
                licensingState = "CA",
                primaryCity = "Sacramento",
                latitude = 38.5816,
                longitude = -121.4944,
                serviceCities = listOf("Sacramento"),
                consultationFeeUsd = 45,
                offersOnSiteWork = true,
                bio = "24/7 dry-out specialist."
            ).getOrThrow()
        }

        val itemId = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.createPortfolioOrReferralItem(
                contractorName = "Bob Vance, WRT",
                entryType = "portfolio",
                title = "French Drain + Basement Restoration",
                lossCategory = "water_restoration",
                city = "Sacramento",
                clientReferenceName = "J. Miller",
                description = "Full exterior perimeter drainage & interior dry-out.",
                completionYear = "2026",
                photoBase64 = "portfolio_img"
            ).getOrThrow()
        }
        assertTrue(itemId.isNotBlank())

        // Non-contractor Alice cannot upload portfolio item, but CAN post Glass Wall review on Bob
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = WaterTableRepository(firestore, auth)
        val unauthorizedPort = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createPortfolioOrReferralItem(
                contractorName = "Fake Pro",
                entryType = "portfolio",
                title = "Fake",
                lossCategory = "water_restoration",
                city = "Sacramento",
                clientReferenceName = "None",
                description = "Should fail.",
                completionYear = "2026",
                photoBase64 = "x"
            )
        }
        assertTrue(unauthorizedPort.isFailure)

        val reviewId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createContractorReview(
                contractorUserId = bobUid,
                contractorName = "Bob Vance, WRT",
                companyOrLicenseRef = "IICRC-1122",
                city = "Sacramento",
                workCategory = "water_restoration",
                overallRating = 5,
                communicationRating = 5,
                honestExperience = "Arrived in 45 minutes and showed moisture meter readings honestly.",
                proofDescription = "Before/after moisture log photo",
                proofPhotoBase64 = "proof123",
                wouldRecommend = true,
                flagShoddyWork = false
            ).getOrThrow()
        }
        assertTrue(reviewId.isNotBlank())

        // Self-review by Bob must fail
        signInTestUser(BOB_EMAIL)
        val selfReview = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.createContractorReview(
                contractorUserId = bobUid,
                contractorName = "Bob Vance, WRT",
                companyOrLicenseRef = "IICRC-1122",
                city = "Sacramento",
                workCategory = "water_restoration",
                overallRating = 5,
                communicationRating = 5,
                honestExperience = "Self review attempt",
                proofDescription = "Self proof",
                proofPhotoBase64 = "x",
                wouldRecommend = true,
                flagShoddyWork = false
            )
        }
        assertTrue(selfReview.isFailure)
    }

    @Test
    fun observeCommunityReports_unauthenticatedCaller_failsPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = WaterTableRepository(firestore, auth)
        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) { repo.observeCommunityReports().first() }
        } catch (t: Throwable) {
            caught = t
        }
        assertNotNull(caught)
    }

    @Test
    fun createSupplyOrder_ownerOnlyIsolation_verified() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = WaterTableRepository(firestore, auth)
        val orderId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createSupplyOrder(
                jobDescription = "50ft French drain + sump pump kit",
                shippingCity = "Sacramento",
                shippingAddress = "100 Main St, Sacramento, CA",
                itemSummaries = listOf("4-inch Perforated French Drain Pipe (100 ft) x1"),
                wholesaleTotalUsd = 185.0,
                retailBenchmarkUsd = 265.0,
                platformFeeUsd = 9.25
            ).getOrThrow()
        }
        assertTrue(orderId.isNotBlank())

        val orders = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeMySupplyOrders().first { list -> list.any { it.id == orderId } }
        }
        assertTrue(orders.any { it.id == orderId })
    }

    private companion object {
        const val ALICE_EMAIL = "alice_wt@test.com"
        const val BOB_EMAIL = "bob_wt@test.com"
        const val CAROL_EMAIL = "carol_wt@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
