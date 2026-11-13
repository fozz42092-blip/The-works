package com.example.data.repository

import com.example.data.catalog.WaterTableCatalogs
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PestControlParserTest {

    @Test
    fun `parsePestControlAnalysisJson parses rodent entry signs seal steps and DoMyOwn supply links`() {
        val sampleJson = """
            ```json
            {
              "suspectedCurrentProblem": "Roof Rat & House Mouse Exterior Entry",
              "pestCategory": "rodent_entry",
              "urgencyLevel": "Elevated",
              "confidenceSummary": "1/2-inch gap around AC refrigerant line penetration shows dark sebum rub marks and chewed foam.",
              "obviousSignsDetected": [
                "Unsealed 1/2-inch gap around exterior copper HVAC line set",
                "Chewed polyurethane foam scraps and dark grease rub marks along stucco edge"
              ],
              "howToSealAndExcludeSteps": [
                "Scrape out chewed plain foam completely.",
                "Pack gap tightly with copper or stainless steel exclusion mesh (Stuf-Fit / Xcluder).",
                "Cap over mesh with high-grade polyurethane sealant and metal escutcheon plate."
              ],
              "treatmentAndResidualInsight": "Physical metal exclusion stops new rodents permanently; pair with snap traps inside until zero activity.",
              "termiteStrategyNote": "Keep mulch and wood-to-soil contact 6 inches below weep screed to also block subterranean termites.",
              "diySuppliesNeeded": [
                {
                  "name": "Stuf-Fit Copper Exclusion Mesh",
                  "purpose": "Pack into pipe chase gaps so rodents cannot chew back through.",
                  "searchKeyword": "copper exclusion mesh",
                  "category": "Exclusion Seal-Up"
                },
                {
                  "name": "1/4 inch Galvanized Hardware Cloth",
                  "purpose": "Cover crawlspace & attic vent louvers.",
                  "searchKeyword": "hardware cloth",
                  "category": "Vent Screening"
                }
              ],
              "shouldCallLicensedPro": false,
              "recommendedProReason": "Accessible ground-level gap can be safely sealed DIY using copper mesh and metal flashing."
            }
            ```
        """.trimIndent()

        val parsed = AiGroundwaterRepository.parsePestControlAnalysisJson(sampleJson)

        assertEquals("Roof Rat & House Mouse Exterior Entry", parsed.suspectedCurrentProblem)
        assertEquals("rodent_entry", parsed.pestCategory)
        assertEquals("Elevated", parsed.urgencyLevel)
        assertEquals(2, parsed.obviousSignsDetected.size)
        assertEquals(3, parsed.howToSealAndExcludeSteps.size)
        assertEquals(2, parsed.diySuppliesNeeded.size)
        assertTrue(parsed.diySuppliesNeeded[0].websiteUrl.startsWith("https://www.domyown.com/search?w="))
        assertTrue(parsed.diySuppliesNeeded[0].websiteUrl.contains("copper"))
    }

    @Test
    fun `termiteComparisonGuide covers zero-residual tenting vs residual hot spot and exterior bait stations`() {
        val titlesAndDetails = WaterTableCatalogs.termiteComparisonGuide.joinToString("\n") {
            "${it.methodTitle} ${it.residualProtection} ${it.pros} ${it.cons} ${it.verdict}"
        }
        assertTrue(titlesAndDetails.contains("ZERO RESIDUAL", ignoreCase = true))
        assertTrue(titlesAndDetails.contains("Hot Spot", ignoreCase = true))
        assertTrue(titlesAndDetails.contains("bait station", ignoreCase = true))
    }

    @Test
    fun `analyzePestOrRodentEntryProblem fails cleanly when neither photo nor concern is given`() = runTest {
        val repo = AiGroundwaterRepository()
        val result = repo.analyzePestOrRodentEntryProblem(
            city = "Sacramento",
            state = "CA",
            focusType = "Rodent Entry & Sealing",
            userConcern = "   ",
            bitmaps = emptyList()
        )
        assertTrue(result.isFailure)
    }
}
