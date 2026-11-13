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
}
