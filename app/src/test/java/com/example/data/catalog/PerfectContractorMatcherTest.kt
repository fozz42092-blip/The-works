package com.example.data.catalog

import com.example.data.model.ContractorPortfolioItem
import com.example.data.model.ContractorProfile
import com.example.data.model.ContractorReview
import com.example.data.model.ProblemMatchQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PerfectContractorMatcherTest {

    private val soilPro = ContractorProfile(
        id = "c1", userId = "u_soil", displayName = "Elena Soil, PE", companyName = "Delta Geo",
        specialty = "soil_engineer", licenseNumber = "PE-100", licensingState = "CA",
        primaryCity = "Sacramento", latitude = 38.58, longitude = -121.49,
        offersOnSiteWork = false, helpfulContributionsCount = 8
    )

    private val moldPro = ContractorProfile(
        id = "c2", userId = "u_mold", displayName = "Marcus Mold Pro", companyName = "PureAir",
        specialty = "mold_remediation", licenseNumber = "CMRC-22", licensingState = "CA",
        primaryCity = "Sacramento", latitude = 38.582, longitude = -121.492,
        offersOnSiteWork = true, helpfulContributionsCount = 5
    )

    private val blacklistedWaterPro = ContractorProfile(
        id = "c3", userId = "u_bad", displayName = "CutCorner DryOut", companyName = "CutCorner",
        specialty = "water_mitigation", licenseNumber = "W-99", licensingState = "CA",
        primaryCity = "Sacramento", latitude = 38.581, longitude = -121.495,
        offersOnSiteWork = true, helpfulContributionsCount = 0
    )

    @Test
    fun matchesMoldContractorForMoldProblem_andDisqualifiesBlacklisted() {
        val reviews = listOf(
            ContractorReview(
                id = "r1", userId = "client1", contractorUserId = "u_bad", contractorName = "CutCorner DryOut",
                overallRating = 1, communicationRating = 1, wouldRecommend = false, flagShoddyWork = true, hasProof = true
            ),
            ContractorReview(
                id = "r2", userId = "client2", contractorUserId = "u_bad", contractorName = "CutCorner DryOut",
                overallRating = 1, communicationRating = 1, wouldRecommend = false, flagShoddyWork = true, hasProof = true
            ),
            ContractorReview(
                id = "r3", userId = "client3", contractorUserId = "u_mold", contractorName = "Marcus Mold Pro",
                overallRating = 5, communicationRating = 5, wouldRecommend = true, flagShoddyWork = false, hasProof = true
            )
        )
        val portfolio = listOf(
            ContractorPortfolioItem(
                id = "p1", userId = "u_mold", lossCategory = "mold_mitigation", title = "Attic HEPA Remediation"
            )
        )
        val pros = listOf(soilPro, moldPro, blacklistedWaterPro)
        val reps = WaterTableCatalogs.computeContractorReputations(pros, reviews, 38.5816, -121.4944)

        val ranked = WaterTableCatalogs.matchPerfectContractorsForProblem(
            query = ProblemMatchQuery(
                problemCategory = "auto",
                problemText = "Found black mold spots and musty spores behind bathroom drywall after water intrusion.",
                constructionStage = "after_remodel",
                depthFeet = 0.0,
                city = "Sacramento",
                needsOnSiteCrew = true
            ),
            contractors = pros,
            reputations = reps,
            portfolioItems = portfolio
        )

        assertEquals("u_mold", ranked.first().contractor.userId)
        assertTrue(ranked.first().isPerfectMatch)

        val badResult = ranked.first { it.contractor.userId == "u_bad" }
        assertFalse(badResult.isPerfectMatch)
        assertEquals(0, badResult.matchScorePercent)
    }

    @Test
    fun matchesSoilEngineerForInProcessShallowWaterTable() {
        val pros = listOf(soilPro, moldPro)
        val reps = WaterTableCatalogs.computeContractorReputations(pros, emptyList(), 38.5816, -121.4944)

        val ranked = WaterTableCatalogs.matchPerfectContractorsForProblem(
            query = ProblemMatchQuery(
                problemCategory = "auto",
                problemText = "Excavating basement footing and hit groundwater table at 4 ft depth before contractor finishes slab.",
                constructionStage = "in_process",
                depthFeet = 4.0,
                city = "Sacramento",
                needsOnSiteCrew = false
            ),
            contractors = pros,
            reputations = reps,
            portfolioItems = emptyList()
        )

        assertEquals("u_soil", ranked.first().contractor.userId)
        assertTrue(ranked.first().isPerfectMatch)
    }

    @Test
    fun matchesAzRoofingPlumbingAndStructuralTrades_withExperienceGuaranteeAndSafetyBonuses() {
        val rooferPro = ContractorProfile(
            id = "c_roof", userId = "u_roof", displayName = "Apex Roof & Flashing", companyName = "Apex",
            specialty = "roofer", licenseNumber = "CSLB-774411", licensingState = "CA",
            cslbBoardStatus = "cslb_verified", liveScanStatus = "verified_cleared",
            primaryCity = "Sacramento", latitude = 38.58, longitude = -121.49,
            offersOnSiteWork = true, completedReviewsCount = 5
        )
        val structuralPro = ContractorProfile(
            id = "c_struct", userId = "u_struct", displayName = "Vance Structural PE", companyName = "Vance SE",
            specialty = "structural_engineer", licenseNumber = "SE-4012", licensingState = "CA",
            cslbBoardStatus = "cslb_verified", liveScanStatus = "verified_cleared",
            primaryCity = "Sacramento", latitude = 38.58, longitude = -121.49,
            offersOnSiteWork = false, completedReviewsCount = 4
        )
        val reps = WaterTableCatalogs.computeContractorReputations(listOf(rooferPro, structuralPro), emptyList(), 38.5816, -121.4944)

        val roofRanked = WaterTableCatalogs.matchPerfectContractorsForProblem(
            query = ProblemMatchQuery(
                problemCategory = "auto",
                problemText = "Attic leak around skylight and damaged flashing on roof shingles",
                city = "Sacramento",
                needsOnSiteCrew = true
            ),
            contractors = listOf(rooferPro, structuralPro),
            reputations = reps,
            portfolioItems = emptyList()
        )
        assertEquals("u_roof", roofRanked.first().contractor.userId)
        assertTrue(roofRanked.first().guaranteeEligible)
        assertTrue(roofRanked.first().guaranteeBadgeLabel.contains("Verified Experience Guarantee"))

        val structRanked = WaterTableCatalogs.matchPerfectContractorsForProblem(
            query = ProblemMatchQuery(
                problemCategory = "auto",
                problemText = "Major settlement crack along load bearing beam and retaining wall",
                city = "Sacramento",
                needsOnSiteCrew = false
            ),
            contractors = listOf(rooferPro, structuralPro),
            reputations = reps,
            portfolioItems = emptyList()
        )
        assertEquals("u_struct", structRanked.first().contractor.userId)
    }

    @Test
    fun soilBottleCalculator_classifiesClaySandSiltAndLoam() {
        val clay = WaterTableCatalogs.evaluateSoilBottleTest(sandRaw = 15, siltRaw = 25, clayRaw = 60)
        assertEquals("clay", clay.mappedSoilTypeKey)
        assertTrue(clay.usdaTextureClass.contains("Clay"))

        val sand = WaterTableCatalogs.evaluateSoilBottleTest(sandRaw = 78, siltRaw = 14, clayRaw = 8)
        assertEquals("sand", sand.mappedSoilTypeKey)

        val silt = WaterTableCatalogs.evaluateSoilBottleTest(sandRaw = 20, siltRaw = 60, clayRaw = 20)
        assertEquals("silt", silt.mappedSoilTypeKey)

        val loam = WaterTableCatalogs.evaluateSoilBottleTest(sandRaw = 40, siltRaw = 35, clayRaw = 25)
        assertEquals("loam", loam.mappedSoilTypeKey)
    }

    @Test
    fun telemetryDiscountAndScoutLeaderboard_computeExpectedRewards() {
        val discount = WaterTableCatalogs.computeTelemetryInsuranceDiscount(
            verifiedPropertiesCount = 2,
            reportsCount = 3,
            outcomesCount = 2,
            mitigationScoreBonus = 3
        )
        assertEquals(25, discount)

        val cappedDiscount = WaterTableCatalogs.computeTelemetryInsuranceDiscount(
            verifiedPropertiesCount = 5,
            reportsCount = 10,
            outcomesCount = 10,
            mitigationScoreBonus = 10
        )
        assertEquals(28, cappedDiscount)

        val reports = listOf(
            com.example.data.model.WaterReport(
                id = "r1", userId = "u1", authorName = "Top Scout", city = "Sacramento",
                moderationStatus = "approved", engineerVerified = true,
                mediaType = "photo", mediaPreviewBase64 = "abc"
            ),
            com.example.data.model.WaterReport(
                id = "r2", userId = "u1", authorName = "Top Scout", city = "Sacramento",
                moderationStatus = "approved", engineerVerified = false,
                mediaType = "video", mediaPreviewBase64 = "def"
            )
        )
        val outcomes = listOf(
            com.example.data.model.SolutionOutcome(id = "o1", userId = "u1", worked = true)
        )
        val board = WaterTableCatalogs.computeFieldScoutLeaderboard(reports, outcomes)
        assertEquals(1, board.size)
        assertEquals("u1", board.first().userId)
        assertEquals(120, board.first().points)
        assertTrue(board.first().freeSubscriptionEarned)
        assertTrue(board.first().paidScoutEligible)
    }
}

