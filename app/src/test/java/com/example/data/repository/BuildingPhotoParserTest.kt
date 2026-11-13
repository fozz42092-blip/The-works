package com.example.data.repository

import kotlinx.coroutines.test.runTest
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BuildingPhotoParserTest {

    @Test
    fun `parseBuildingPhotoAnalysisJson parses drainage roofing foundation and portion risks accurately`() {
        val sampleJson = """
            ```json
            {
              "overallCondition": "Negative exterior grading and aging roof flashing require attention",
              "overallRiskBadge": "Elevated",
              "summary": "The exterior elevation shows soil sloping toward the slab edge and lifted valley flashing above the garage.",
              "visualHighlightsSummary": "Visible splashback staining 14 inches up stucco base, short downspout elbow, and curled shingle tabs.",
              "drainage": {
                "riskLevel": "Urgent",
                "visualIndicatorsFound": [
                  "Downspout terminates 6 inches from slab footing",
                  "Soil mounded above stucco weep screed"
                ],
                "detailedAnalysis": "Roof runoff discharges directly into clay soil at the foundation perimeter.",
                "realLifeProblemsThatCanOccur": [
                  "Sub-slab hydrostatic uplift and slab crack moisture",
                  "Wicking dampness into interior bottom plates and hidden mold"
                ],
                "recommendedActions": [
                  "Add 8-foot solid downspout extension or buried drain line",
                  "Lower soil grade 4-6 inches below weep screed"
                ],
                "recommendedTradeKey": "drainage_contractor"
              },
              "roofing": {
                "riskLevel": "Moderate",
                "visualIndicatorsFound": [
                  "Curling asphalt shingle corners along eaves",
                  "Dark streaking below chimney step flashing"
                ],
                "detailedAnalysis": "Aging shingles and oxidized flashing increase wind-driven rain intrusion risk.",
                "realLifeProblemsThatCanOccur": [
                  "Attic sheathing rot and ceiling drywall staining during winter storms"
                ],
                "recommendedActions": [
                  "Reseal chimney step flashing and replace cracked vent boots"
                ],
                "recommendedTradeKey": "roofer"
              },
              "foundation": {
                "riskLevel": "Moderate",
                "visualIndicatorsFound": [
                  "White efflorescence band along stem wall",
                  "Hairline diagonal crack at garage corner"
                ],
                "detailedAnalysis": "Efflorescence confirms moisture vapor migration through porous concrete.",
                "realLifeProblemsThatCanOccur": [
                  "Rebar spalling and widening differential settlement crack"
                ],
                "recommendedActions": [
                  "Monitor crack width with gauge and redirect perimeter runoff"
                ],
                "recommendedTradeKey": "foundation_specialist"
              },
              "additionalPortionFindings": {
                "riskLevel": "Low",
                "visualIndicatorsFound": [
                  "Hairline stucco shrinkage crack near upper window corner"
                ],
                "detailedAnalysis": "Perimeter window caulking is drying out and due for elastomeric touch-up.",
                "realLifeProblemsThatCanOccur": [
                  "Water intrusion behind stucco lath during driving rain"
                ],
                "recommendedActions": [
                  "Seal hairline stucco cracks with textured elastomeric sealant"
                ],
                "recommendedTradeKey": "general_contractor"
              },
              "realLifeRisksIfIgnored": [
                "Interior baseboard swelling and mold growth",
                "Progressive foundation settlement and higher insurance renewal risk"
              ],
              "actionableSuggestions": [
                "Redirect every downspout at least 8 ft away from the slab",
                "Clear mulch/soil below the stucco weep screed",
                "Schedule a roofer flashing tune-up before next heavy rain"
              ],
              "estimatedRepairRangeUsd": "$950 – $3,400",
              "topRecommendedSpecialtyKey": "drainage_contractor"
            }
            ```
        """.trimIndent()

        val parsed = AiGroundwaterRepository.parseBuildingPhotoAnalysisJson(
            jsonText = sampleJson,
            inspectedPortion = "Full House / Building Exterior",
            buildingType = "Residential House"
        )

        assertEquals("Elevated", parsed.overallRiskBadge)
        assertEquals("Full House / Building Exterior", parsed.inspectedPortion)
        assertEquals("Residential House", parsed.buildingType)
        assertEquals("Urgent", parsed.drainage.riskLevel)
        assertEquals(2, parsed.drainage.visualIndicatorsFound.size)
        assertEquals("drainage_contractor", parsed.drainage.recommendedTradeKey)
        assertEquals("Moderate", parsed.roofing.riskLevel)
        assertEquals("roofer", parsed.roofing.recommendedTradeKey)
        assertEquals("Moderate", parsed.foundation.riskLevel)
        assertEquals("foundation_specialist", parsed.foundation.recommendedTradeKey)
        assertEquals("Low", parsed.additionalPortionFindings.riskLevel)
        assertEquals(2, parsed.realLifeRisksIfIgnored.size)
        assertEquals(3, parsed.actionableSuggestions.size)
        assertEquals("$950 – $3,400", parsed.estimatedRepairRangeUsd)
    }

    @Test
    fun `parseBuildingPhotoAnalysisJson handles omitted categories cleanly`() {
        val minimalJson = """
            {
              "overallCondition": "Interior ceiling stain inspected",
              "overallRiskBadge": "Moderate",
              "summary": "Brown ring stain visible around bathroom ceiling vent.",
              "visualHighlightsSummary": "Circular tan moisture ring near exhaust fan.",
              "additionalPortionFindings": {
                "riskLevel": "Elevated",
                "visualIndicatorsFound": ["Tea-colored water ring around ceiling fan grille"],
                "detailedAnalysis": "Likely roof vent condensation or flashing drip above ceiling.",
                "realLifeProblemsThatCanOccur": ["Drywall sagging and hidden attic mold growth"],
                "recommendedActions": ["Inspect attic above fan and check roof vent boot"],
                "recommendedTradeKey": "roofer"
              },
              "realLifeRisksIfIgnored": ["Ceiling drywall collapse and attic framing mold"],
              "actionableSuggestions": ["Place bucket/moisture meter check and inspect roof boot"],
              "estimatedRepairRangeUsd": "$350 – $1,200",
              "topRecommendedSpecialtyKey": "roofer"
            }
        """.trimIndent()

        val parsed = AiGroundwaterRepository.parseBuildingPhotoAnalysisJson(
            jsonText = minimalJson,
            inspectedPortion = "Interior Wall / Ceiling",
            buildingType = "Residential House"
        )

        assertEquals("None Observed", parsed.drainage.riskLevel)
        assertEquals("None Observed", parsed.roofing.riskLevel)
        assertEquals("None Observed", parsed.foundation.riskLevel)
        assertEquals("Elevated", parsed.additionalPortionFindings.riskLevel)
        assertEquals("roofer", parsed.topRecommendedSpecialtyKey)
    }

    @Test(expected = JSONException::class)
    fun `parseBuildingPhotoAnalysisJson rejects malformed json`() {
        AiGroundwaterRepository.parseBuildingPhotoAnalysisJson("not valid json")
    }

    @Test
    fun `analyzeBuildingOrExteriorPhotos fails cleanly when no bitmaps provided`() = runTest {
        val repo = AiGroundwaterRepository()
        val result = repo.analyzeBuildingOrExteriorPhotos(
            city = "Sacramento",
            state = "CA",
            elevationFeet = 30,
            buildingType = "Residential House",
            buildingPortion = "Full House / Building Exterior",
            notes = "Check exterior walls",
            bitmaps = emptyList()
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("photo", ignoreCase = true) == true)
    }
}
