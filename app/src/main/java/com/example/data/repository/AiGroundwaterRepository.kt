package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.data.catalog.WaterTableCatalogs
import com.example.data.model.AdaptiveSolutionStat
import com.example.data.model.AiMatchConciergeVerdict
import com.example.data.model.BuildingPhotoAiAnalysis
import com.example.data.model.BuildingProblemCategoryAssessment
import com.example.data.model.ConsultationRequest
import com.example.data.model.ContractorCopilotAdvice
import com.example.data.model.ContractorMatchResult
import com.example.data.model.DiyPestSupplyItem
import com.example.data.model.GroundedCreditLink
import com.example.data.model.GroundwaterAiDiagnosis
import com.example.data.model.HomeAzAiInspection
import com.example.data.model.IicrcAuditRecord
import com.example.data.validation.AiModerationVerdict
import com.example.data.model.JobKitRecommendation
import com.example.data.model.LiveDisasterEvent
import com.example.data.model.OnlineVerifiedContractorLead
import com.example.data.model.PestControlAiAnalysis
import com.example.data.model.PropertyHazardHistory
import com.example.data.model.WaterReport
import com.example.data.model.WholesaleProduct
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class AiGroundwaterRepository {

    private val diagnosisSchema = Schema.obj(
        properties = mapOf(
            "verdict" to Schema.enumeration(
                values = listOf(
                    "ground_water_table",
                    "surface_runoff",
                    "plumbing_leak",
                    "storm_drain_backup",
                    "water_loss_mitigation",
                    "mold_mitigation",
                    "smoke_fire_mitigation",
                    "uncertain"
                ),
                description = "Primary classification of the groundwater or restoration loss"
            ),
            "confidencePercent" to Schema.integer(description = "Confidence score from 0 to 100"),
            "summary" to Schema.string(description = "Clear yes/no determination or restoration severity assessment"),
            "engineeringReasoning" to Schema.string(description = "Hydrological, moisture, mold, smoke, or fire indicators observed"),
            "restorationScope" to Schema.string(description = "Step-by-step water/mold/smoke/fire restoration and dry-out protocol for contractors or residents"),
            "constructionStageAdvice" to Schema.string(
                description = "Specific guidance for what needs to be done BEFORE contractors finish property (when construction is in process) OR how to repair non-destructively AFTER a full remodel is done."
            ),
            "recommendedSolutions" to Schema.array(
                items = Schema.string(),
                description = "Ordered list of 3-5 repair, dry-out, or mitigation solutions prioritized by proven effectiveness"
            ),
            "immediateActions" to Schema.array(
                items = Schema.string(),
                description = "Immediate stabilization steps homeowner or restoration crew should take today"
            ),
            "streetFloodRisk" to Schema.enumeration(
                values = listOf("low", "moderate", "high", "critical"),
                description = "Street/car flooding or structural hazard risk level"
            ),
            "estimatedDepthFeet" to Schema.double(description = "Estimated water table depth in feet if applicable"),
            "shouldEscalateToEngineer" to Schema.boolean(description = "True if a licensed engineer or restoration contractor review is advised")
        )
    )

    private val moderationSchema = Schema.obj(
        properties = mapOf(
            "isSafeAndRelevant" to Schema.boolean(description = "True if photos and report describe genuine groundwater, flood, water loss, mold, smoke, or fire restoration without spam/abuse"),
            "plausibilityScore" to Schema.integer(description = "0 to 100 score of hydrological/restoration plausibility"),
            "summary" to Schema.string(description = "Short moderation decision note explaining why approved or flagged"),
            "flaggedReasons" to Schema.array(
                items = Schema.string(),
                description = "Empty if safe and plausible; otherwise list specific concerns"
            )
        )
    )

    private val kitSchema = Schema.obj(
        properties = mapOf(
            "kitSummary" to Schema.string(description = "Short explanation of the recommended wholesale materials kit"),
            "recommendedItems" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "sku" to Schema.string(description = "Exact SKU from the provided catalog"),
                        "quantity" to Schema.integer(description = "Recommended quantity >= 1")
                    )
                ),
                description = "List of catalog SKUs and quantities needed for the job"
            ),
            "installationNotes" to Schema.array(
                items = Schema.string(),
                description = "3-4 practical step-by-step installation tips"
            )
        )
    )

    private val diagnosisModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = diagnosisSchema
            }
        )
    }

    private val moderationModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = moderationSchema
            }
        )
    }

    private val kitModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = kitSchema
            }
        )
    }

    private val matchConciergeSchema = Schema.obj(
        properties = mapOf(
            "topRecommendedContractorName" to Schema.string(description = "Name of the single best-matched contractor from the provided list"),
            "reasoning" to Schema.string(description = "2-3 sentence tailored explanation of why this contractor is the perfect match for the client's specific problem and construction stage"),
            "recommendedServiceMode" to Schema.enumeration(
                values = listOf("restoration_work", "remote_advice"),
                description = "Whether client should request on-site work dispatch or remote engineering triage first"
            ),
            "checklistToShareWithPro" to Schema.array(
                items = Schema.string(),
                description = "3 specific photos, measurements, or details the homeowner should attach when messaging this pro"
            )
        )
    )

    private val matchConciergeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = matchConciergeSchema
            }
        )
    }

    private val homeAzSchema = Schema.obj(
        properties = mapOf(
            "calmingReassurance" to Schema.string(description = "Warm, calming, anxiety-reducing 2-3 sentence opening reassuring the homeowner this problem has clear, fixable steps and explaining why not to panic"),
            "detectedIssueTitle" to Schema.string(description = "Clear plain-English title of the detected A-Z house problem"),
            "severityLabel" to Schema.enumeration(
                values = listOf("Low", "Moderate", "Urgent"),
                description = "Realistic urgency level"
            ),
            "recommendedTrade" to Schema.string(description = "Best A-Z licensed contractor trade label"),
            "recommendedSpecialtyKey" to Schema.enumeration(
                values = listOf(
                    "soil_engineer", "structural_engineer", "general_engineer", "industrial_hygienist",
                    "landscaper", "drainage_contractor", "leak_specialist", "water_mitigation",
                    "mold_remediation", "smoke_fire_restoration", "full_service_restoration",
                    "iicrc_inspector", "plumber", "roofer", "foundation_specialist",
                    "electrician", "hvac", "general_contractor"
                ),
                description = "Exact specialty key to route the homeowner to"
            ),
            "scopeOfWork" to Schema.string(description = "Step-by-step Scope of Work contractors will perform so homeowner knows every step and isn't overcharged"),
            "estimatedPriceLowUsd" to Schema.integer(description = "Realistic fair low-end market repair cost in USD"),
            "estimatedPriceHighUsd" to Schema.integer(description = "Realistic fair high-end market repair cost in USD"),
            "geologicalAndElevationNotes" to Schema.string(description = "Geological, lot grading, elevation, or slab water-table notes observed"),
            "lifeExpectancyForecast" to Schema.string(description = "AI remaining useful life expectancy estimate for the inspected roof, pipes, copper lines, slab, or exterior"),
            "plumbingAndPipeCorrosionFindings" to Schema.string(description = "Specific check for under-sink drain leaks, supply valves, or exterior copper pipe green/blue patina & pinhole corrosion risk"),
            "drainageAndErosionFindings" to Schema.string(description = "Exterior lot slope, soil erosion, weep screed height, and downspout runoff findings"),
            "needsHygienistAirOrTapeTest" to Schema.boolean(description = "True if mold/air spore traps or tape lift sampling by an Industrial Hygienist is advised"),
            "needsEquipmentRental" to Schema.boolean(description = "True if homeowner or restoration crew can speed drying with dehumidifier/HEPA air scrubber rental"),
            "diyImmediateSteps" to Schema.array(
                items = Schema.string(),
                description = "3 calm, safe steps the homeowner can take today"
            ),
            "relatedVideoCategory" to Schema.enumeration(
                values = listOf("soil_water", "mold", "shutoff", "plumbing_roof", "drainage"),
                description = "Closest how-to video category"
            )
        )
    )

    private val homeAzModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = homeAzSchema
            }
        )
    }

    private fun categoryAssessmentSchema(categoryDescription: String): Schema = Schema.obj(
        properties = mapOf(
            "riskLevel" to Schema.enumeration(
                values = listOf("None Observed", "Low", "Moderate", "Elevated", "Urgent"),
                description = "Observed visual risk severity for $categoryDescription"
            ),
            "visualIndicatorsFound" to Schema.array(
                items = Schema.string(),
                description = "Concrete visual indicators observed in the uploaded photo(s), or clearly state if angle did not show this area"
            ),
            "detailedAnalysis" to Schema.string(
                description = "Plain-English engineering/contractor explanation of what is visible for $categoryDescription"
            ),
            "realLifeProblemsThatCanOccur" to Schema.array(
                items = Schema.string(),
                description = "Real-life structural, water, mold, safety, or financial damage that can occur if these indicators progress"
            ),
            "recommendedActions" to Schema.array(
                items = Schema.string(),
                description = "Prioritized practical repair, inspection, or preventive suggestions"
            ),
            "recommendedTradeKey" to Schema.enumeration(
                values = listOf(
                    "soil_engineer", "structural_engineer", "general_engineer", "industrial_hygienist",
                    "landscaper", "drainage_contractor", "leak_specialist", "water_mitigation",
                    "mold_remediation", "smoke_fire_restoration", "full_service_restoration",
                    "iicrc_inspector", "plumber", "roofer", "foundation_specialist",
                    "electrician", "hvac", "general_contractor"
                ),
                description = "Best contractor trade key for this category"
            )
        )
    )

    private val buildingPhotoAnalysisSchema = Schema.obj(
        properties = mapOf(
            "overallCondition" to Schema.string(description = "Concise 1-line summary of the visible condition of the photographed portion/building"),
            "overallRiskBadge" to Schema.enumeration(
                values = listOf("Low", "Moderate", "Elevated", "Urgent"),
                description = "Overall inspection priority badge"
            ),
            "summary" to Schema.string(description = "2-3 sentence clear assessment of the photographed portion of the house or building"),
            "visualHighlightsSummary" to Schema.string(description = "Key visual cues spotted directly in the uploaded image(s)"),
            "drainage" to categoryAssessmentSchema("Lot grading, surface runoff, downspouts, efflorescence, erosion, and moisture drainage"),
            "roofing" to categoryAssessmentSchema("Shingles/tiles, flashing, valleys, fascia, gutters, vents, and roof moisture staining"),
            "foundation" to categoryAssessmentSchema("Slab cracks, stair-step cracks, spalling, settlement gaps, damp base bands, and footing integrity"),
            "additionalPortionFindings" to categoryAssessmentSchema("Portion-specific findings (interior framing, plumbing, copper pipes, HVAC, electrical, stucco/siding, balcony, basement, or commercial envelope)"),
            "realLifeRisksIfIgnored" to Schema.array(
                items = Schema.string(),
                description = "3-5 concrete real-life problems that can occur over time if the observed issues or vulnerabilities go unaddressed"
            ),
            "actionableSuggestions" to Schema.array(
                items = Schema.string(),
                description = "4-6 practical AI suggestions covering DIY maintenance, professional verification, and preventative steps"
            ),
            "estimatedRepairRangeUsd" to Schema.string(description = "Realistic market USD cost range e.g. '\$350 – \$1,800' or '\$0 – \$250 DIY'"),
            "topRecommendedSpecialtyKey" to Schema.enumeration(
                values = listOf(
                    "soil_engineer", "structural_engineer", "general_engineer", "industrial_hygienist",
                    "landscaper", "drainage_contractor", "leak_specialist", "water_mitigation",
                    "mold_remediation", "smoke_fire_restoration", "full_service_restoration",
                    "iicrc_inspector", "plumber", "roofer", "foundation_specialist",
                    "electrician", "hvac", "general_contractor"
                ),
                description = "Single highest-priority licensed trade key"
            )
        )
    )

    private val buildingPhotoModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = buildingPhotoAnalysisSchema
            }
        )
    }

    private val pestControlSchema = Schema.obj(
        properties = mapOf(
            "suspectedCurrentProblem" to Schema.string(description = "Clear diagnosis of the current pest, rodent entry, or termite problem at the property"),
            "pestCategory" to Schema.enumeration(
                values = listOf("rodent_entry", "termite", "insect_crawling", "moisture_pest", "wildlife_burrow", "general_pest"),
                description = "Primary pest category"
            ),
            "urgencyLevel" to Schema.enumeration(
                values = listOf("Low", "Moderate", "Elevated", "Urgent"),
                description = "Infestation or structural risk urgency"
            ),
            "confidenceSummary" to Schema.string(description = "1-2 sentence summary explaining why the uploaded photos and/or described signs indicate this problem"),
            "obviousSignsDetected" to Schema.array(
                items = Schema.string(),
                description = "Specific visual entry signs (chew marks, sebum rub marks, gaps >= 1/4 inch, torn vent screens, droppings, mud tubes, frass pellets, wings) or key symptom matches"
            ),
            "howToSealAndExcludeSteps" to Schema.array(
                items = Schema.string(),
                description = "Actionable step-by-step instructions to physically seal entry points or exclude pests (use copper/stainless mesh, hardware cloth, metal flashing, polyurethane sealant — warn against plain expanding foam)"
            ),
            "treatmentAndResidualInsight" to Schema.string(description = "Practical explanation of treatment options, emphasizing non-residual knockdowns vs long-lasting residual pesticides & baiting strategies"),
            "termiteStrategyNote" to Schema.string(description = "Termite-specific insight when termites are mentioned or suspected: fumigation/tenting has ZERO residual allowing immediate re-entry unless paired with interior Hot Spot residual pesticides + exterior ground bait stations; otherwise note how moisture/wood access affects pests"),
            "diySuppliesNeeded" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "name" to Schema.string(description = "Specific DIY pest control product type needed (e.g. Copper Exclusion Mesh, 1/4 inch Hardware Cloth, Fipronil Residual Concentrate, In-Ground Termite Bait Stations)"),
                        "purpose" to Schema.string(description = "Why and how the homeowner uses this item"),
                        "searchKeyword" to Schema.string(description = "Short search query for DoMyOwn.com DIY pest control supply store"),
                        "category" to Schema.string(description = "Short category label e.g. Exclusion Seal-Up, Traps, Residual Barrier, Termite Bait")
                    )
                ),
                description = "3 to 5 specific DIY items needed so the homeowner can buy supplies and do the work themselves"
            ),
            "shouldCallLicensedPro" to Schema.boolean(description = "True if heavy structural damage, attic remediation, widespread drywood termites, or electrical chewing hazards warrant a licensed pest pro"),
            "recommendedProReason" to Schema.string(description = "When to escalate vs handle DIY safely")
        )
    )

    private val pestControlModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = pestControlSchema
            }
        )
    }

    private val webGroundedCopilotModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            tools = listOf(Tool.googleSearch())
        )
    }

    private val guideAssistantModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    You are the friendly, practical WaterTable AI Personal Assistant & New-User Tutorial Guide.
                    Help new users navigate and succeed with every feature in WaterTable:
                    1) Atlas Tab (Leaflet Map & Street Flood Watch): Shows GPS groundwater pins color-coded by dig depth (<= 5 ft shallow crimson alert, 5-12 ft amber, > 12 ft teal), pre-rain car/street flood risk from NWS & live weather, and clickable pins revealing construction in-process vs post-remodel repair plans.
                    2) Diagnose Tab: Upload up to 4 site photos or video, stamp GPS, log dig depth & construction stage (Pre-Finish Construction In Process vs After Full Remodel), select Groundwater / Water Restoration / Mold / Smoke / Fire loss, run AI Ground Water & Restoration Triage, and submit to the map (photos & detailed reports pass completeness/plausibility checks & Moderation Queue).
                    3) DIY Leak Tab: Run the 5-step Meter Spin, Chlorine Strip, Sub-Slab Plastic Sheet, Dye Trace, and Post-Rain Lag tests before digging.
                    4) Pros Tab (Nearest Pros, Glass Wall & Pro Hub): Connect with nearest helpful non-blacklisted Soil Engineers or Water/Mold/Smoke/Fire Mitigation Pros, view portfolios & past-client referrals, and post/read honest Glass Wall reviews with photo proof & community blacklist flags.
                    5) Supply Tab: Build wholesale drop-ship French drain & sump pump kits cheaper than big-box retail.
                    6) Video Studio (top bar): Turn scripts, blog posts, product descriptions, or uploaded product/site photos into playable MP4 video clips.
                    7) Profile Tab (top bar): View all past submissions (including pending moderation reports), update city/role, and run Peer & AI Moderation screening.
                    Keep answers concise, actionable, numbered when giving tutorial steps, and under 180 words unless asked for deep engineering detail.
                    """.trimIndent()
                )
            }
        )
    }

    fun streamTutorialReply(
        currentScreenRoute: String,
        historyPairs: List<Pair<String, String>>,
        userQuestion: String
    ): Flow<String> = flow {
        val recentHistory = historyPairs.takeLast(6).joinToString("\n") { (role, msg) -> "$role: $msg" }
        val prompt = """
            Current app screen: $currentScreenRoute
            Recent conversation:
            $recentHistory

            User question: $userQuestion
        """.trimIndent()
        val buffer = StringBuilder()
        withTimeout(60_000L) {
            guideAssistantModel.generateContentStream(prompt).collect { chunk ->
                val part = chunk.text.orEmpty()
                if (part.isNotEmpty()) {
                    buffer.append(part)
                    emit(buffer.toString())
                }
            }
        }
        if (buffer.isBlank()) {
            throw IllegalStateException("Assistant returned an empty response.")
        }
    }

    suspend fun analyzeSiteSituation(
        city: String,
        soilType: String,
        constructionStage: String,
        lossCategory: String,
        situationType: String,
        observedDepthFeet: Double,
        lossDescription: String,
        notes: String,
        bitmaps: List<Bitmap>,
        topCommunitySolutions: List<AdaptiveSolutionStat>
    ): Result<GroundwaterAiDiagnosis> {
        return try {
            val communityLearningContext = if (topCommunitySolutions.isEmpty()) {
                "No prior logged community outcomes yet for this filter."
            } else {
                topCommunitySolutions.take(5).joinToString("\n") { stat ->
                    "- ${stat.solutionName}: ${stat.successRatePercent}% success rate (${stat.sampleSize} verified reports, avg rating ${stat.avgRating}/5)"
                }
            }

            val domainGuidance = when (lossCategory) {
                "water_restoration" -> "Focus on IICRC S500 water damage category (Cat 1/2/3), extraction, LGR dehumidifiers, air movers, flood cuts, and antimicrobial treatment alongside checking if groundwater caused it."
                "mold_mitigation" -> "Focus on IICRC S520 mold containment barriers, negative air HEPA scrubbers, moisture-source elimination, porous vs. non-porous material removal, and PPE safety."
                "smoke_damage" -> "Focus on dry vs. wet soot chemistry, HEPA vacuuming, chemical sponges, hydroxyl/thermal deodorization, HVAC duct cleaning, and surface sealing."
                "fire_restoration" -> "Focus on structural board-up, char removal, water-from-firefighting dry-out, odor encapsulation, and safe pack-out steps."
                else -> "Determine clearly whether standing or seeping water is from the natural Ground Water Table or surface runoff, pressurized pipe leak, or storm drain backup."
            }

            val stageContext = when (constructionStage) {
                "in_process" -> "CONSTRUCTION IS IN PROCESS: Detail critical waterproofing, sub-slab drainage, sump sleeves, vapor barriers, and hydrostatic relief steps that MUST be completed BEFORE contractors pour concrete, close framing, or finish drywall."
                "after_remodel" -> "FULL REMODEL IS ALREADY COMPLETE: Detail minimally invasive post-remodel repair methods (exterior perimeter French drains, targeted micro-channel baseboard retrofits, core-drilled relief sumps, smart sensors) to protect new finishes without unnecessary demolition."
                else -> "Standard site evaluation: note shallow table depth risks and practical pre-finish or drainage stabilization advice."
            }

            val promptText = """
                You are a senior geotechnical drainage engineer and IICRC-certified water, mold, smoke, and fire restoration specialist.
                $domainGuidance
                $stageContext

                Site Context:
                - City/Town: $city
                - Loss Category: $lossCategory
                - Construction Stage: $constructionStage
                - Situation: $situationType
                - Soil Type: $soilType
                - Observed Dig / Standing Water Depth (ft): $observedDepthFeet (<= 5 ft = very shallow water table hazard)
                - Full Loss Description: $lossDescription
                - Additional Field Notes: $notes

                Adaptive Learning Data (Verified by local residents & contractors):
                $communityLearningContext

                Analyze all attached photos (${bitmaps.size} images) and site context. Provide a decisive assessment, practical restoration scope, specific constructionStageAdvice for before-contractors-finish or post-remodel repairs, and prioritized repair solutions.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                diagnosisModel.generateContent(
                    content {
                        bitmaps.take(4).forEach { bmp -> image(bmp) }
                        text(promptText)
                    }
                )
            }

            val jsonText = response.text ?: throw IllegalStateException("AI returned an empty diagnosis.")
            val json = JSONObject(jsonText)

            val recsArray = json.optJSONArray("recommendedSolutions")
            val recs = buildList {
                if (recsArray != null) {
                    for (i in 0 until recsArray.length()) add(recsArray.getString(i))
                }
            }
            val actionsArray = json.optJSONArray("immediateActions")
            val actions = buildList {
                if (actionsArray != null) {
                    for (i in 0 until actionsArray.length()) add(actionsArray.getString(i))
                }
            }

            Result.success(
                GroundwaterAiDiagnosis(
                    lossCategory = lossCategory,
                    verdict = json.optString("verdict", "uncertain"),
                    confidencePercent = json.optInt("confidencePercent", 75).coerceIn(0, 100),
                    summary = json.optString("summary", ""),
                    engineeringReasoning = json.optString("engineeringReasoning", ""),
                    restorationScope = json.optString("restorationScope", ""),
                    constructionStageAdvice = json.optString("constructionStageAdvice", ""),
                    recommendedSolutions = recs,
                    immediateActions = actions,
                    streetFloodRisk = json.optString("streetFloodRisk", "moderate"),
                    estimatedDepthFeet = json.optDouble("estimatedDepthFeet", observedDepthFeet),
                    shouldEscalateToEngineer = json.optBoolean("shouldEscalateToEngineer", true)
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI diagnosis timed out", e)
            Result.failure(IllegalStateException("AI analysis timed out after 60s. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI diagnosis failed", e)
            Result.failure(e)
        }
    }

    suspend fun moderateSubmission(
        city: String,
        lossCategory: String,
        depthFeet: Double,
        lossDescription: String,
        notes: String,
        bitmaps: List<Bitmap>
    ): Result<AiModerationVerdict> {
        return try {
            val prompt = """
                You are a content and technical plausibility moderator for a community groundwater & property restoration app.
                Review this submission before it is published to the public city atlas:
                - City: $city
                - Category: $lossCategory
                - Depth (ft): $depthFeet
                - Loss Description: $lossDescription
                - Field Notes: $notes
                - Attached Photos: ${bitmaps.size}

                Evaluate:
                1) Are the photos (if provided) and text genuinely related to groundwater, flooding, drainage, water damage, mold, smoke, or fire restoration (and free of spam, ads, abusive content, or unrelated selfies/memes)?
                2) Are the depth and damage details physically plausible?
            """.trimIndent()

            val response = withTimeout(45_000L) {
                moderationModel.generateContent(
                    content {
                        bitmaps.take(3).forEach { bmp -> image(bmp) }
                        text(prompt)
                    }
                )
            }
            val json = JSONObject(response.text ?: throw IllegalStateException("Empty moderation response"))
            val flagsArray = json.optJSONArray("flaggedReasons")
            val flags = buildList {
                if (flagsArray != null) {
                    for (i in 0 until flagsArray.length()) add(flagsArray.getString(i))
                }
            }
            Result.success(
                AiModerationVerdict(
                    isSafeAndRelevant = json.optBoolean("isSafeAndRelevant", false),
                    plausibilityScore = json.optInt("plausibilityScore", 70).coerceIn(0, 100),
                    summary = json.optString("summary", "Moderation screening completed."),
                    flaggedReasons = flags
                )
            )
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("AI moderation timed out after 45s."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI moderation failed", e)
            Result.failure(e)
        }
    }

    suspend fun buildWholesaleJobKit(
        jobDescription: String,
        catalog: List<WholesaleProduct>
    ): Result<JobKitRecommendation> {
        return try {
            val catalogLines = catalog.joinToString("\n") { p ->
                "${p.sku} | ${p.name} | ${p.unitLabel} | Wholesale $${p.wholesalePriceUsd} (Retail $${p.retailBenchmarkUsd}) | ${p.specs}"
            }
            val prompt = """
                A resident or contractor needs a complete drop-ship bill of materials from our wholesale groundwater supply catalog.
                Job Description: "$jobDescription"

                Available Wholesale Catalog SKUs (ONLY select SKUs from this list):
                $catalogLines

                Select the exact SKUs and realistic quantities needed to finish this job end-to-end (including pipe, filter fabric, sump pump/backup, check valve, catch basin, and waterproofing/leak test items as needed).
            """.trimIndent()

            val response = withTimeout(60_000L) {
                kitModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("AI returned an empty kit plan.")
            val json = JSONObject(jsonText)
            val catalogMap = catalog.associateBy { it.sku }

            val itemsArray = json.optJSONArray("recommendedItems")
            val validPairs = mutableListOf<Pair<String, Int>>()
            var savings = 0.0
            if (itemsArray != null) {
                for (i in 0 until itemsArray.length()) {
                    val obj = itemsArray.getJSONObject(i)
                    val sku = obj.optString("sku", "")
                    val qty = obj.optInt("quantity", 1).coerceIn(1, 50)
                    val product = catalogMap[sku]
                    if (product != null) {
                        validPairs.add(sku to qty)
                        savings += product.savingsUsd * qty
                    }
                }
            }

            val notesArray = json.optJSONArray("installationNotes")
            val notes = buildList {
                if (notesArray != null) {
                    for (i in 0 until notesArray.length()) add(notesArray.getString(i))
                }
            }

            Result.success(
                JobKitRecommendation(
                    kitSummary = json.optString("kitSummary", "Custom wholesale drainage & waterproofing kit."),
                    estimatedSavingsVsRetailUsd = savings,
                    recommendedSkus = validPairs,
                    installationNotes = notes
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI kit builder timed out", e)
            Result.failure(IllegalStateException("Kit builder timed out after 60s. Please retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI kit builder failed", e)
            Result.failure(e)
        }
    }

    suspend fun explainBestContractorMatch(
        problemCategory: String,
        problemText: String,
        constructionStage: String,
        depthFeet: Double,
        city: String,
        rankedCandidates: List<ContractorMatchResult>
    ): Result<AiMatchConciergeVerdict> {
        val eligible = rankedCandidates.filter { it.reputation?.isBlacklisted != true }.take(4)
        if (eligible.isEmpty()) {
            return Result.failure(IllegalStateException("No active non-blacklisted contractors available to match."))
        }
        return try {
            val candidateSummary = eligible.joinToString("\n") { m ->
                val c = m.contractor
                val ratingStr = m.reputation?.let {
                    if (it.reviewCount > 0) "${it.avgOverallRating}★ (${it.reviewCount} reviews), ${it.distanceMiles} mi"
                    else "${it.distanceMiles} mi, ${c.helpfulContributionsCount} helped"
                } ?: "${c.helpfulContributionsCount} helped"
                "- ${c.displayName} | ${c.companyName} | Specialty=${c.specialty} | Lic=${c.licenseNumber} | MatchScore=${m.matchScorePercent}% | OnSite=${c.offersOnSiteWork} | $ratingStr | Reasons=${m.matchReasons.joinToString("; ")}"
            }
            val prompt = """
                You are a restoration & geotechnical concierge matching a homeowner with their PERFECT licensed contractor.
                Client Problem:
                - City: $city
                - Category: $problemCategory
                - Construction Stage: $constructionStage
                - Dig / Water Depth (ft): $depthFeet
                - Problem Details: "$problemText"

                Top Non-Blacklisted Candidates:
                $candidateSummary

                Select the single best contractor for this exact problem, explain why they are the ideal fit, advise remote advice vs on-site restoration work, and list 3 measurements/photos the client should include.
            """.trimIndent()

            val response = withTimeout(45_000L) { matchConciergeModel.generateContent(prompt) }
            val json = JSONObject(response.text ?: throw IllegalStateException("Empty AI match response"))
            val checklistArr = json.optJSONArray("checklistToShareWithPro")
            val checklist = buildList {
                if (checklistArr != null) {
                    for (i in 0 until checklistArr.length()) add(checklistArr.getString(i))
                }
            }
            Result.success(
                AiMatchConciergeVerdict(
                    topRecommendedContractorName = json.optString("topRecommendedContractorName", eligible.first().contractor.displayName),
                    reasoning = json.optString("reasoning", "Best matched by specialty, proximity, and proven client ratings."),
                    recommendedServiceMode = json.optString("recommendedServiceMode", eligible.first().recommendedServiceMode),
                    checklistToShareWithPro = checklist
                )
            )
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("AI Matchmaker timed out after 45s."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI Matchmaker failed", e)
            Result.failure(e)
        }
    }

    /**
     * AI + Live Web Search Co-Pilot assisting contractors to answer all client questions accurately
     * with field execution strategies and verifiable web credit links (IICRC / ASTM / EPA / CSLB).
     */
    suspend fun generateContractorClientAnswerAndStrategy(
        consultation: ConsultationRequest,
        report: WaterReport?,
        topCommunitySolutions: List<AdaptiveSolutionStat>
    ): Result<ContractorCopilotAdvice> {
        return try {
            val communityLearning = if (topCommunitySolutions.isEmpty()) {
                "No prior logged outcomes yet for this category."
            } else {
                topCommunitySolutions.take(5).joinToString("\n") { s ->
                    "- ${s.solutionName}: ${s.successRatePercent}% client success (${s.sampleSize} verified reports, avg $${s.avgCostUsd})"
                }
            }
            val siteDetails = report?.let {
                """
                Site Telemetry:
                - City: ${it.city}, ${it.stateOrRegion} | Depth: ${it.depthFeet} ft | Soil: ${it.soilType}
                - Construction Stage: ${it.constructionStage} (in_process = before contractor finishes; after_remodel = completed remodel)
                - Loss Category: ${it.lossCategory} | Situation: ${it.situationType}
                - Client Loss Description: ${it.lossDescription}
                - Field Notes: ${it.notes}
                """.trimIndent()
            } ?: "City: ${consultation.city} | Loss Category: ${consultation.lossCategory}"

            val prompt = """
                You are an expert Technical Restoration & Geotechnical Engineering Co-Pilot assisting a licensed contractor answer a client's inquiry accurately using verified current industry standards (IICRC S500 Water Damage, IICRC S520 Mold Remediation, IICRC S700 Fire & Smoke, EPA mold guidance, ASTM waterproofing/drainage standards).
                Use Google Search to ground technical claims in authoritative public standards and return accurate, up-to-date citations.

                Client Request (${consultation.serviceMode}):
                "${consultation.question}"

                $siteDetails

                Continuous App Learning (what actually worked in similar community jobs):
                $communityLearning

                Format your response using these exact section headers:
                [VERDICT]
                One of: water_mitigation_approved, mold_remediation_approved, smoke_fire_approved, ground_water_table, surface_runoff, plumbing_leak, needs_site_boring
                [/VERDICT]
                [ESTIMATE_USD]
                Single integer USD estimate for on-site mitigation/repair (or 0 if remote advice only)
                [/ESTIMATE_USD]
                [CLIENT_ANSWER]
                Clear, honest, client-friendly answer addressing every question raised, explaining moisture physics, safety, and construction-stage steps.
                [/CLIENT_ANSWER]
                [CONTRACTOR_STRATEGY]
                Actionable step-by-step contractor execution strategy: inspection readings (moisture/psychrometrics), containment/PPE, equipment sizing, pre-finish or non-destructive post-remodel tactics, and QA sign-off checkpoints.
                [/CONTRACTOR_STRATEGY]
            """.trimIndent()

            val response = withTimeout(60_000L) {
                webGroundedCopilotModel.generateContent(prompt)
            }
            val raw = response.text ?: throw IllegalStateException("AI Co-Pilot returned empty response.")
            val groundingMeta = response.candidates.firstOrNull()?.groundingMetadata
            val webChunks = groundingMeta?.groundingChunks
                ?.mapNotNull { chunk -> chunk.web }
                ?.mapNotNull { web ->
                    val uri = web.uri?.trim().orEmpty()
                    if (uri.startsWith("http")) {
                        GroundedCreditLink(
                            title = web.title?.takeIf { it.isNotBlank() } ?: (web.domain ?: "Reference Source"),
                            url = uri,
                            domain = web.domain.orEmpty()
                        )
                    } else null
                }
                ?.distinctBy { it.url }
                ?.take(6)
                .orEmpty()

            val searchQueries = groundingMeta?.webSearchQueries.orEmpty().take(4)

            val parsedVerdict = extractTaggedBlock(raw, "VERDICT").trim()
                .takeIf {
                    it in listOf(
                        "water_mitigation_approved", "mold_remediation_approved", "smoke_fire_approved",
                        "ground_water_table", "surface_runoff", "plumbing_leak", "needs_site_boring"
                    )
                } ?: when (consultation.lossCategory) {
                "mold_mitigation" -> "mold_remediation_approved"
                "smoke_damage", "fire_restoration" -> "smoke_fire_approved"
                "water_restoration" -> "water_mitigation_approved"
                else -> "ground_water_table"
            }
            val parsedEstimate = extractTaggedBlock(raw, "ESTIMATE_USD").filter { it.isDigit() }
                .toIntOrNull()?.coerceIn(0, 250_000) ?: 0
            val clientAnswer = extractTaggedBlock(raw, "CLIENT_ANSWER").ifBlank { raw.take(1500) }
            val strategy = extractTaggedBlock(raw, "CONTRACTOR_STRATEGY").ifBlank {
                "Document pre-work moisture baseline, verify source shutoff, follow IICRC/ASTM containment, and record daily dry-out logs."
            }

            Result.success(
                ContractorCopilotAdvice(
                    clientAnswer = clientAnswer.trim(),
                    contractorStrategy = strategy.trim(),
                    recommendedVerdict = parsedVerdict,
                    suggestedEstimateUsd = parsedEstimate,
                    creditLinks = webChunks,
                    searchQueriesUsed = searchQueries
                )
            )
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("AI + Web Co-Pilot timed out after 60s. Please retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI Co-Pilot failed", e)
            Result.failure(e)
        }
    }

    /**
     * IICRC Governance Verifier: allows an IICRC Inspector / Governor to verify a contractor's
     * restoration suggestion against current IICRC S500 / S520 / S700 standards via Google Search.
     */
    suspend fun verifyIicrcStandardsCompliance(
        audit: IicrcAuditRecord
    ): Result<Triple<String, String, List<GroundedCreditLink>>> {
        return try {
            val prompt = """
                You are assisting an IICRC Master Technical Governor auditing a restoration contractor's proposed scope for accuracy and compliance with the latest published ANSI/IICRC Standards (S500 Water Damage, S520 Mold Remediation, S700 Fire & Smoke Restoration).
                Use Google Search to verify current ANSI/IICRC standard terminology, categories/classes, containment, psychrometrics, and clearance criteria.

                Audit Subject:
                - City: ${audit.city}
                - Loss Category: ${audit.lossCategory}
                - Contractor: ${audit.contractorName}
                - Proposed Scope: "${audit.contractorSuggestion}"
                - Proposed Strategy: "${audit.contractorStrategy}"

                Format with exact tags:
                [RECOMMENDED_STATUS]
                One of: endorsed, revision_requested, flagged_outdated
                [/RECOMMENDED_STATUS]
                [STANDARD_REF]
                Exact ANSI/IICRC standard (e.g. ANSI/IICRC S500 5th Ed. or S520 3rd Ed. or S700)
                [/STANDARD_REF]
                [GOVERNANCE_REVIEW]
                Concise technical verification noting what meets current IICRC standard and any missing required controls (e.g., negative air, flood-cut height, antimicrobial limits, clearance testing).
                [/GOVERNANCE_REVIEW]
            """.trimIndent()

            val response = withTimeout(60_000L) {
                webGroundedCopilotModel.generateContent(prompt)
            }
            val raw = response.text ?: throw IllegalStateException("IICRC verification returned empty text.")
            val links = response.candidates.firstOrNull()?.groundingMetadata?.groundingChunks
                ?.mapNotNull { it.web }
                ?.mapNotNull { web ->
                    val uri = web.uri?.trim().orEmpty()
                    if (uri.startsWith("http")) {
                        GroundedCreditLink(
                            title = web.title?.takeIf { it.isNotBlank() } ?: (web.domain ?: "IICRC Source"),
                            url = uri,
                            domain = web.domain.orEmpty()
                        )
                    } else null
                }
                ?.distinctBy { it.url }
                ?.take(5)
                .orEmpty()

            val status = extractTaggedBlock(raw, "RECOMMENDED_STATUS").trim()
                .takeIf { it in listOf("endorsed", "revision_requested", "flagged_outdated") }
                ?: "endorsed"
            val stdRef = extractTaggedBlock(raw, "STANDARD_REF").trim().ifBlank { "ANSI/IICRC S500 / S520 / S700" }
            val reviewText = extractTaggedBlock(raw, "GOVERNANCE_REVIEW").trim().ifBlank { raw.take(1500) }
            Result.success(Triple(status, "$stdRef — $reviewText", links))
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("IICRC standards verification timed out after 60s."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "IICRC verification failed", e)
            Result.failure(e)
        }
    }

    suspend fun generatePropertyHazardHistoryDossier(
        label: String,
        propertyType: String,
        address: String,
        city: String,
        state: String,
        latitude: Double,
        longitude: Double,
        femaFloodZone: String,
        floodRiskDesc: String,
        nearbyReports: List<WaterReport>,
        liveDisasters: List<LiveDisasterEvent>,
        elevationFeet: Int = 0,
        historicalStormDays: Int = 0
    ): Result<PropertyHazardHistory> {
        val shallowReports = nearbyReports.filter { it.depthFeet in 0.1..6.5 }
        val nearestDepth = nearbyReports.minByOrNull { it.depthFeet }?.depthFeet
        val nearestFire = liveDisasters.filter { it.type == "wildfire" }.minOfOrNull { it.distanceToHomeMiles }
        val nearestFlood = liveDisasters.filter { it.type == "flood" }.minOfOrNull { it.distanceToHomeMiles }

        val gwSummary = when {
            shallowReports.isNotEmpty() -> "${shallowReports.size} shallow groundwater logs (<=6.5 ft, nearest ${nearestDepth ?: 4.0} ft) near $city."
            nearbyReports.isNotEmpty() -> "${nearbyReports.size} groundwater logs in area (shallowest ${nearestDepth ?: 10.0} ft)."
            else -> "No prior WaterTable digs logged within immediate block; verify sub-slab moisture before buying/remodeling."
        }

        return try {
            val prompt = """
                A homebuyer or property owner is evaluating this ${if (propertyType == "business") "commercial property" else "house"}:
                - Address / Label: $address ($label)
                - City/State: $city, $state (GPS $latitude, $longitude)
                - Measured Terrain Elevation: $elevationFeet ft above sea level
                - Past 5-Year Heavy Storm/Wind Events (Open-Meteo Archive): $historicalStormDays severe days
                - Mapped FEMA Flood Zone: $femaFloodZone ($floodRiskDesc)
                - WaterTable Community Shallow Depth Logs: $gwSummary
                - Nearest Active Wildfire Distance: ${nearestFire?.let { "$it miles" } ?: "None within 50 miles"}
                - Nearest Active Flood Alert/Gauge: ${nearestFlood?.let { "$it miles" } ?: "None active right now"}

                Use Google Search to research public records and history of $city, $state (near $address):
                1) What was that land used for BEFORE neighborhoods/houses were built there (wetland, citrus grove, orchard, floodplain, oil field, wash, ranchland, marsh, creek bed,chaparral hillside)?
                2) Neighborhood development era & soil/erosion comparison notes.
                3) Past Mother Nature experiences in this neighborhood (major floods, debris flows, wildfires, earthquakes, atmospheric rivers).
                Provide:
                [FIRE_HAZARD_ZONE]
                Short label (e.g., High WUI / Moderate Fire Zone / Low Urban Fire Zone)
                [/FIRE_HAZARD_ZONE]
                [LAND_BEFORE_HOUSE]
                2-3 sentences explaining what this exact land/watershed was before houses were built here (orchards, seasonal creek wash, marsh/peat, grazing ranch, hillside chaparral) and how that impacts slab/drainage today.
                [/LAND_BEFORE_HOUSE]
                [NEIGHBORHOOD_ERA]
                Short development era & construction note (e.g., 1960s-1970s tract homes on expansive alluvial clay or post-2000 master-planned slab).
                [/NEIGHBORHOOD_ERA]
                [MOTHER_NATURE_HISTORY]
                Public-data summary of notable Mother Nature events in $city / surrounding watershed (major flood years, fires, windstorms, atmospheric rivers).
                [/MOTHER_NATURE_HISTORY]
                [EROSION_NOTES]
                Practical satellite/elevation comparison note for comparing older vs newer aerial drainage swales, hillside creep, and insurance underwriting.
                [/EROSION_NOTES]
                [HISTORICAL_DOSSIER]
                Concise pre-purchase / owner historical risk brief covering flood history, fire history, groundwater seepage risk, insurance considerations, and what cleanses/mitigations contractors advise BEFORE closing or remodeling.
                [/HISTORICAL_DOSSIER]
                [CHECKLIST]
                4 short pipe-separated pre-buy or cleanse action items (e.g., Request CLUE report|Camera sewer & French drain scope|Moisture meter slab check|Compare Google Earth historic aerial erosion)
                [/CHECKLIST]
            """.trimIndent()

            val response = withTimeout(60_000L) {
                webGroundedCopilotModel.generateContent(prompt)
            }
            val raw = response.text ?: throw IllegalStateException("Empty property hazard history response.")
            val links = response.candidates.firstOrNull()?.groundingMetadata?.groundingChunks
                ?.mapNotNull { it.web }
                ?.mapNotNull { web ->
                    val uri = web.uri?.trim().orEmpty()
                    if (uri.startsWith("http")) {
                        GroundedCreditLink(
                            title = web.title?.takeIf { it.isNotBlank() } ?: (web.domain ?: "Hazard Source"),
                            url = uri,
                            domain = web.domain.orEmpty()
                        )
                    } else null
                }
                ?.distinctBy { it.url }
                ?.take(6)
                .orEmpty()

            val fireZone = extractTaggedBlock(raw, "FIRE_HAZARD_ZONE").trim()
                .ifBlank { if ((nearestFire ?: 999.0) <= 25.0) "Elevated Wildfire Proximity" else "Standard Fire Hazard Zone" }
            val landBefore = extractTaggedBlock(raw, "LAND_BEFORE_HOUSE").trim()
            val era = extractTaggedBlock(raw, "NEIGHBORHOOD_ERA").trim()
            val motherNature = extractTaggedBlock(raw, "MOTHER_NATURE_HISTORY").trim()
            val erosionNotes = extractTaggedBlock(raw, "EROSION_NOTES").trim()
            val dossier = extractTaggedBlock(raw, "HISTORICAL_DOSSIER").trim().ifBlank { raw.take(1400) }
            val checklistRaw = extractTaggedBlock(raw, "CHECKLIST").trim()
            val checklist = checklistRaw.split("|", "\n")
                .map { it.trim().trimStart('-', '•', ' ') }
                .filter { it.length >= 4 }
                .take(5)
                .ifEmpty {
                    listOf(
                        "Check CLUE insurance loss history & seller disclosures",
                        "Compare Google Maps aerials over years for yard/hillside erosion",
                        "Inspect sub-slab / crawlspace moisture & exterior copper lines",
                        "Verify defensible space & HVAC/attic smoke filter readiness"
                    )
                }

            Result.success(
                PropertyHazardHistory(
                    label = label.ifBlank { address },
                    propertyType = propertyType,
                    address = address,
                    city = city,
                    state = state,
                    latitude = latitude,
                    longitude = longitude,
                    femaFloodZone = femaFloodZone,
                    floodZoneRisk = floodRiskDesc,
                    fireHazardZone = fireZone,
                    groundwaterTableSummary = gwSummary,
                    nearbyShallowReportsCount = shallowReports.size,
                    nearestReportDepthFt = nearestDepth,
                    nearestActiveFireMiles = nearestFire,
                    nearestActiveFloodMiles = nearestFlood,
                    aiHistoricalDossier = dossier,
                    preBuyOrCleanseChecklist = checklist,
                    creditLinks = links,
                    elevationFeet = elevationFeet,
                    landBeforeHouseBuilt = landBefore,
                    neighborhoodEra = era,
                    motherNatureHistory = motherNature,
                    erosionComparisonNotes = erosionNotes,
                    historicalStormCount = historicalStormDays
                )
            )
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("Property hazard dossier timed out after 60s."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Property hazard dossier failed", e)
            Result.failure(e)
        }
    }

    /**
     * Calming multimodal AI house inspector covering:
     * 1) "az_photo": Roof leaks to slab cracks, under-sink drains, exterior copper corrosion, price check, scope of work.
     * 2) "exterior_video": Multi-frame exterior walkaround spotting negative grading & foundation water runoff risks.
     * 3) "future_home": Pre-buy geological/elevation assessment, erosion comparison cues, & component life expectancy.
     */
    suspend fun analyzeAzHouseOrExteriorVideoProblem(
        mode: String,
        city: String,
        state: String,
        elevationFeet: Int,
        problemArea: String,
        userDescription: String,
        bitmaps: List<Bitmap>
    ): Result<HomeAzAiInspection> {
        return try {
            val modePrompt = when (mode) {
                "exterior_video" -> "Inspect these sequential exterior video walkaround frames carefully for negative soil grading sloping toward the foundation, buried weep screed, ponding runoff, gutter downspouts dumping at the slab, hillside erosion, and exterior copper pipe/hose-bib corrosion."
                "future_home" -> "Perform a pre-purchase / future-home interior & exterior inspection: evaluate geological & elevation drainage risks ($elevationFeet ft elevation in $city, $state), signs of past flooding or foundation settlement, under-sink drain leaks, exterior copper pipe green/blue corrosion, roof wear, and estimate remaining useful life expectancy."
                else -> "Inspect this homeowner's house issue photo(s) from roof leaks to slab cracks, under-sink plumbing drains, exterior copper corrosion, mold spots, or moisture staining. Be reassuring, give a transparent fair Scope of Work, and provide realistic market Price Check USD estimates."
            }

            val input = content {
                bitmaps.take(5).forEach { bmp -> image(bmp) }
                text(
                    """
                    You are a calm, empathetic, deeply knowledgeable Master Building Inspector & Restoration Advisor in $city, $state.
                    The homeowner may be stressed or worried about their house. Start with warm reassurance that home issues are fixable and step-by-step scope protects them from overpaying.

                    Mode: $mode ($modePrompt)
                    Property City/State: $city, $state (Elevation: $elevationFeet ft)
                    Focus Area Selected: $problemArea
                    Homeowner Notes / Questions: ${userDescription.ifBlank { "Please inspect attached photo/video frames and advise calm next steps, fair price check, and scope of work." }}
                    """.trimIndent()
                )
            }

            val response = withTimeout(60_000L) {
                homeAzModel.generateContent(input)
            }
            val raw = response.text ?: throw IllegalStateException("Empty AI home inspection response.")
            val json = JSONObject(raw)
            val stepsArr = json.optJSONArray("diyImmediateSteps")
            val steps = buildList {
                if (stepsArr != null) {
                    for (i in 0 until stepsArr.length()) {
                        stepsArr.optString(i).takeIf { it.isNotBlank() }?.let { add(it) }
                    }
                }
            }.ifEmpty {
                listOf("Take a deep breath and document date-stamped photos.", "Check shutoff valves if active water is dripping.", "Request 2 licensed quotes with the Scope of Work below.")
            }

            Result.success(
                HomeAzAiInspection(
                    mode = mode,
                    calmingReassurance = json.optString("calmingReassurance", "Take a deep breath — almost every home leak or moisture issue has a clear, manageable fix once diagnosed step by step."),
                    detectedIssueTitle = json.optString("detectedIssueTitle", problemArea.ifBlank { "Home Inspection Findings" }),
                    severityLabel = json.optString("severityLabel", "Moderate"),
                    recommendedTrade = json.optString("recommendedTrade", "General Contractor (B)"),
                    recommendedSpecialtyKey = json.optString("recommendedSpecialtyKey", "general_contractor"),
                    scopeOfWork = json.optString("scopeOfWork", "On-site moisture/visual confirmation followed by targeted component repair."),
                    estimatedPriceLowUsd = json.optInt("estimatedPriceLowUsd", 250),
                    estimatedPriceHighUsd = json.optInt("estimatedPriceHighUsd", 1200),
                    geologicalAndElevationNotes = json.optString("geologicalAndElevationNotes", "Site at $elevationFeet ft elevation; keep positive drainage 6 inches away over first 10 ft."),
                    lifeExpectancyForecast = json.optString("lifeExpectancyForecast", "Confirm age of roof flashing, under-sink supply lines, and copper piping on site."),
                    plumbingAndPipeCorrosionFindings = json.optString("plumbingAndPipeCorrosionFindings", "Inspect under-sink P-traps and exterior copper risers for green patina/verdigris."),
                    drainageAndErosionFindings = json.optString("drainageAndErosionFindings", "Verify soil slopes away from slab and downspouts extend 8-10 ft clear."),
                    needsHygienistAirOrTapeTest = json.optBoolean("needsHygienistAirOrTapeTest", false),
                    needsEquipmentRental = json.optBoolean("needsEquipmentRental", false),
                    diyImmediateSteps = steps,
                    relatedVideoCategory = json.optString("relatedVideoCategory", "plumbing_roof")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("A-Z Home AI inspector timed out after 60s."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "A-Z Home AI inspection failed", e)
            Result.failure(e)
        }
    }

    /**
     * Uses Gemini 3.8 Flash + Google Search Grounding to locate reputable licensed A-Z contractors online
     * near the user's city with verification links (CSLB, official contractor websites, Better Business / Yelp).
     */
    suspend fun findOnlineReputableContractors(
        tradeLabel: String,
        city: String,
        state: String,
        issueDescription: String
    ): Result<List<OnlineVerifiedContractorLead>> {
        return try {
            val prompt = """
                Search online for reputable, well-reviewed, licensed contractors near $city, $state for:
                Trade Needed: $tradeLabel
                Homeowner Issue: ${issueDescription.ifBlank { "$tradeLabel repair & inspection" }}

                Return 3 to 5 specific real local contractor businesses or official licensing directories found via Google Search.
                Output each lead cleanly inside repeating blocks like:
                [LEAD]
                COMPANY: Company Name
                TRADE: Specific Trade / Services
                LICENSE_OR_RATING: CSLB license info or public rating summary found
                URL: Exact https:// URL from search
                REASON: Why reputable & strong match for this issue
                [/LEAD]
            """.trimIndent()

            val response = withTimeout(60_000L) {
                webGroundedCopilotModel.generateContent(prompt)
            }
            val raw = response.text ?: throw IllegalStateException("Empty contractor search response.")
            val groundingWeb = response.candidates.firstOrNull()?.groundingMetadata?.groundingChunks
                ?.mapNotNull { it.web }
                .orEmpty()

            val parsedLeads = mutableListOf<OnlineVerifiedContractorLead>()
            val blocks = raw.split("[LEAD]")
            for (blk in blocks.drop(1)) {
                val inner = blk.substringBefore("[/LEAD]").trim()
                fun field(name: String): String =
                    inner.lines().firstOrNull { it.trim().startsWith("$name:", ignoreCase = true) }
                        ?.substringAfter(":")?.trim().orEmpty()

                val name = field("COMPANY")
                if (name.isNotBlank()) {
                    val urlRaw = field("URL").takeIf { it.startsWith("http") }
                        ?: groundingWeb.firstOrNull { it.title?.contains(name.take(8), ignoreCase = true) == true }?.uri
                        ?: "https://www.cslb.ca.gov/OnlineServices/CheckLicenseII/CheckLicense.aspx"
                    val domain = runCatching { java.net.URI(urlRaw).host?.removePrefix("www.").orEmpty() }.getOrDefault("")
                    parsedLeads.add(
                        OnlineVerifiedContractorLead(
                            companyName = name,
                            tradeSummary = field("TRADE").ifBlank { tradeLabel },
                            licenseOrRatingHint = field("LICENSE_OR_RATING").ifBlank { "Verify active license on state board" },
                            verificationUrl = urlRaw,
                            domain = domain,
                            reasonMatched = field("REASON").ifBlank { "Matched online for $tradeLabel near $city" }
                        )
                    )
                }
            }

            if (parsedLeads.isEmpty()) {
                groundingWeb.filter { !it.uri.isNullOrBlank() }.take(4).forEach { web ->
                    parsedLeads.add(
                        OnlineVerifiedContractorLead(
                            companyName = web.title?.take(60) ?: (web.domain ?: "$tradeLabel Directory"),
                            tradeSummary = tradeLabel,
                            licenseOrRatingHint = "Verified Search Result · Confirm CSLB Active Status",
                            verificationUrl = web.uri!!,
                            domain = web.domain.orEmpty(),
                            reasonMatched = "Grounded web match near $city, $state"
                        )
                    )
                }
            }

            Result.success(parsedLeads.take(5))
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("Online contractor search timed out after 60s."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Online contractor search failed", e)
            Result.failure(e)
        }
    }

    /**
     * Multimodal Building & House Exterior Photo AI Inspector.
     * Supports any photo(s) of any portion of a house or building (exterior walls, yard grading, roof,
     * foundation, basement, crawlspace, plumbing, electrical, interior, or commercial building),
     * highlighting visual indicators for Drainage, Roofing, Foundation, and portion-specific
     * real-life problems that can occur.
     */
    suspend fun analyzeBuildingOrExteriorPhotos(
        city: String,
        state: String,
        elevationFeet: Int,
        buildingType: String,
        buildingPortion: String,
        notes: String,
        bitmaps: List<Bitmap>
    ): Result<BuildingPhotoAiAnalysis> {
        if (bitmaps.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please take or upload at least one photo of the house or building first."))
        }
        return try {
            val promptText = """
                You are a senior Master Building Inspector, Structural Forensics Advisor, and Restoration Specialist evaluating photo(s) in $city, $state (terrain elevation ~$elevationFeet ft).
                Building Type: $buildingType
                User-Selected Building Portion / Area: $buildingPortion
                User Notes: ${notes.ifBlank { "Analyze all visible conditions, subtle warning signs, and real-life problems that can occur." }}

                Carefully inspect all ${bitmaps.take(4).size} attached photo(s):
                1. Highlight concrete VISUAL INDICATORS actually visible in the image(s). Be truthful: if a specific system (e.g., roof surface or foundation base) is NOT visible in the provided camera angle, state "Not clearly visible in this camera angle" in visualIndicatorsFound and set its riskLevel to "None Observed", while still mentioning what to check from that angle.
                2. Explicitly evaluate these core building vulnerability systems based on common visual indicators:
                   - DRAINAGE: negative lot slope sloping toward slab/building, disconnected or short downspouts discharging next to foundation, efflorescence/white salt deposits, buried weep screed, soil erosion/gullies, standing puddles, splashback mud/staining on lower siding, clogged swales or area drains.
                   - ROOFING: missing, curled, cracked, or granule-bare shingles/tiles; rusted, lifted, or missing step/valley flashing; cracked neoprene plumbing vent boots; sagging fascia/soffit boards; pulling or clogged gutters; moss, algae streaks, or roof-to-wall water staining; flat-roof blistering or ponding rings.
                   - FOUNDATION: horizontal, diagonal, or stair-step mortar/concrete cracks; spalling or flaking concrete; exposed or corroded rebar; differential settlement gaps around windows/doors/garage; damp lower perimeter band or rising wicking moisture; footing undermining.
                   - ADDITIONAL PORTION FINDINGS ($buildingPortion): Analyze the exact photographed building portion—such as interior walls/ceilings (water rings, blistering paint, bubbling drywall tape, hidden mold potential), plumbing/under-sink/copper pipes (green/blue verdigris corrosion, weeping compression valves, P-trap leaks), basement/crawlspace (joist staining, efflorescence, high-humidity condensation), stucco/siding/decks (hairline stucco cracks, failed caulking, ledger flashing gaps, wood rot), HVAC/electrical (rusted condensate pan, corroded breaker exterior, unsealed penetrations), or commercial envelope/parapet/parking slab.
                3. Clearly list REAL-LIFE PROBLEMS THAT CAN OCCUR if these conditions continue (e.g., sub-slab hydrostatic heave, hidden wall-cavity mold colonization, roof deck rot & ceiling collapse, pinhole pipe bursts, termite attraction to damp wood, insurance non-renewal or costly claim denial).
                4. Provide practical, prioritized AI SUGGESTIONS (immediate homeowner safety/maintenance checks, what to document, and which licensed specialty trade to consult).
            """.trimIndent()

            val response = withTimeout(60_000L) {
                buildingPhotoModel.generateContent(
                    content {
                        bitmaps.take(4).forEach { bmp -> image(bmp) }
                        text(promptText)
                    }
                )
            }
            val rawJson = response.text ?: throw IllegalStateException("Gemini AI returned an empty photo analysis.")
            Result.success(
                parseBuildingPhotoAnalysisJson(
                    jsonText = rawJson,
                    inspectedPortion = buildingPortion,
                    buildingType = buildingType
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Building photo AI analysis timed out", e)
            Result.failure(IllegalStateException("Photo AI inspection timed out after 60s. Please retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Building photo AI analysis failed", e)
            Result.failure(e)
        }
    }

    /**
     * Multimodal & Symptom-Driven Pest, Rodent Entry, and Termite AI Advisor.
     * Detects rodent entry points & obvious entrance signs in photos, gives step-by-step exclusion/seal-up
     * instructions, links DIY DoMyOwn.com items needed, and diagnoses any pest/termite concerns
     * including Tenting (zero residual) vs Hot Spot Residual + Exterior Ground Bait Stations.
     */
    suspend fun analyzePestOrRodentEntryProblem(
        city: String,
        state: String,
        focusType: String,
        userConcern: String,
        bitmaps: List<Bitmap>
    ): Result<PestControlAiAnalysis> {
        if (bitmaps.isEmpty() && userConcern.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Please take/upload a photo of a possible entry point or describe your pest concern first.")
            )
        }
        return try {
            val promptText = """
                You are a veteran Master Pest Control Inspector, Rodent Exclusion Specialist, and Structural Termite Strategist serving $city, $state.
                User Selected Focus: $focusType
                Homeowner Concerns & Symptoms: ${userConcern.ifBlank { "Analyze the attached photo(s) carefully for rodent entry points, structural gaps, pest evidence, and DIY fix steps." }}
                Attached Photos: ${bitmaps.take(4).size}

                Instructions:
                1. Suggest the exact CURRENT PEST PROBLEM at the property based on the photos and/or described concerns (mice, roof/Norway rats, drywood termites, subterranean termites, ants, cockroaches, carpenter bees/ants, moisture pests, gophers/burrows, etc.).
                2. If photos are provided, point out EVERY OBVIOUS SIGN OF ENTRANCE OR ACTIVITY visible (gaps >= 1/4" under siding/pipe chases/garage corners, chewed weatherstripping, dark greasy rub marks, chewed wood/foam, torn attic/crawlspace vent screens, droppings, mud tubes, frass pellets, wings, damp wood).
                3. Provide exact STEP-BY-STEP HOW TO SEAL UP & EXCLUDE instructions (always pair copper/stainless steel exclusion wool like Xcluder/Stuf-Fit with polyurethane caulk or galvanized 1/4" hardware cloth + metal flashing; explicitly warn NEVER to use expanding spray foam alone because rodents chew right through plain foam).
                4. Address RESIDUAL vs NON-RESIDUAL treatments truthfully:
                   - For Termites: explain how Termite Testing / Tenting (sulfuryl fluoride fumigation) kills active termites inside the wood at that moment but leaves ZERO chemical residual—allowing new swarming termites back into the house almost immediately once the tent airs out. Compare pros/cons of Tenting vs Hot Spot Treatments (fipronil/borate residual pesticides that stay active for years and work better than testing/tenting alone for accessible colonies), and recommend the complete 3-part defense when tenting is needed: Tent + Interior Hot Spot Residual + Exterior Ground Bait Stations.
                5. List 3-5 concrete DIY items needed for DoMyOwn.com so the homeowner can buy exact exclusion mesh, hardware cloth, traps, residual treatments, or exterior bait stations and do the work themselves.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                pestControlModel.generateContent(
                    content {
                        bitmaps.take(4).forEach { bmp -> image(bmp) }
                        text(promptText)
                    }
                )
            }
            val rawJson = response.text ?: throw IllegalStateException("Gemini AI returned an empty pest analysis.")
            Result.success(parsePestControlAnalysisJson(rawJson))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Pest AI analysis timed out", e)
            Result.failure(IllegalStateException("Pest AI analysis timed out after 60s. Please retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Pest AI analysis failed", e)
            Result.failure(e)
        }
    }

    private fun extractTaggedBlock(source: String, tag: String): String {
        val startToken = "[$tag]"
        val endToken = "[/$tag]"
        val start = source.indexOf(startToken)
        if (start == -1) return ""
        val afterStart = start + startToken.length
        val end = source.indexOf(endToken, afterStart)
        return if (end == -1) source.substring(afterStart).trim()
        else source.substring(afterStart, end).trim()
    }

    companion object {
        private const val TAG = "AiGroundwaterRepo"

        internal fun parseBuildingPhotoAnalysisJson(
            jsonText: String,
            inspectedPortion: String = "Exterior Overview",
            buildingType: String = "Residential House"
        ): BuildingPhotoAiAnalysis {
            val cleaned = jsonText.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val root = JSONObject(cleaned)

            fun JSONObject.stringList(key: String): List<String> {
                val arr = optJSONArray(key) ?: return emptyList()
                return buildList {
                    for (i in 0 until arr.length()) {
                        arr.optString(i).trim().takeIf { it.isNotBlank() }?.let { add(it) }
                    }
                }
            }

            fun parseCategory(
                obj: JSONObject?,
                title: String,
                defaultTradeKey: String
            ): BuildingProblemCategoryAssessment {
                if (obj == null) {
                    return BuildingProblemCategoryAssessment(
                        category = title,
                        riskLevel = "None Observed",
                        visualIndicatorsFound = listOf("Not clearly visible in this camera angle."),
                        detailedAnalysis = "Capture a dedicated angle of this area if you would like deeper AI visual verification.",
                        realLifeProblemsThatCanOccur = emptyList(),
                        recommendedActions = emptyList(),
                        recommendedTradeKey = defaultTradeKey
                    )
                }
                return BuildingProblemCategoryAssessment(
                    category = title,
                    riskLevel = obj.optString("riskLevel", "Low").ifBlank { "Low" },
                    visualIndicatorsFound = obj.stringList("visualIndicatorsFound"),
                    detailedAnalysis = obj.optString("detailedAnalysis", "").trim(),
                    realLifeProblemsThatCanOccur = obj.stringList("realLifeProblemsThatCanOccur"),
                    recommendedActions = obj.stringList("recommendedActions"),
                    recommendedTradeKey = obj.optString("recommendedTradeKey", defaultTradeKey).ifBlank { defaultTradeKey }
                )
            }

            return BuildingPhotoAiAnalysis(
                inspectedPortion = inspectedPortion,
                buildingType = buildingType,
                overallCondition = root.optString("overallCondition", "Inspection Complete").ifBlank { "Inspection Complete" },
                overallRiskBadge = root.optString("overallRiskBadge", "Moderate").ifBlank { "Moderate" },
                summary = root.optString("summary", "").trim(),
                visualHighlightsSummary = root.optString("visualHighlightsSummary", "").trim(),
                drainage = parseCategory(root.optJSONObject("drainage"), "Drainage & Lot Runoff", "drainage_contractor"),
                roofing = parseCategory(root.optJSONObject("roofing"), "Roofing & Flashing", "roofer"),
                foundation = parseCategory(root.optJSONObject("foundation"), "Foundation & Slab", "foundation_specialist"),
                additionalPortionFindings = parseCategory(
                    root.optJSONObject("additionalPortionFindings"),
                    "Portion-Specific: $inspectedPortion",
                    "general_contractor"
                ),
                realLifeRisksIfIgnored = root.stringList("realLifeRisksIfIgnored"),
                actionableSuggestions = root.stringList("actionableSuggestions"),
                estimatedRepairRangeUsd = root.optString("estimatedRepairRangeUsd", "Verify with 2 licensed bids").ifBlank { "Verify with 2 licensed bids" },
                topRecommendedSpecialtyKey = root.optString("topRecommendedSpecialtyKey", "general_contractor").ifBlank { "general_contractor" }
            )
        }

        internal fun parsePestControlAnalysisJson(jsonText: String): PestControlAiAnalysis {
            val cleaned = jsonText.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val root = JSONObject(cleaned)

            fun JSONObject.stringList(key: String): List<String> {
                val arr = optJSONArray(key) ?: return emptyList()
                return buildList {
                    for (i in 0 until arr.length()) {
                        arr.optString(i).trim().takeIf { it.isNotBlank() }?.let { add(it) }
                    }
                }
            }

            val suppliesArr = root.optJSONArray("diySuppliesNeeded")
            val supplies = buildList {
                if (suppliesArr != null) {
                    for (i in 0 until suppliesArr.length()) {
                        val itemObj = suppliesArr.optJSONObject(i) ?: continue
                        val name = itemObj.optString("name", "").trim()
                        if (name.isNotBlank()) {
                            val kw = itemObj.optString("searchKeyword", name).ifBlank { name }
                            add(
                                DiyPestSupplyItem(
                                    name = name,
                                    purpose = itemObj.optString("purpose", "DIY pest exclusion & treatment item").trim(),
                                    websiteUrl = WaterTableCatalogs.buildDoMyOwnSearchUrl(kw),
                                    category = itemObj.optString("category", "DIY Supply").ifBlank { "DIY Supply" }
                                )
                            )
                        }
                    }
                }
            }.ifEmpty {
                WaterTableCatalogs.diyPestSupplyCatalog.take(3)
            }

            return PestControlAiAnalysis(
                suspectedCurrentProblem = root.optString("suspectedCurrentProblem", "Pest Entry & Activity Assessment").ifBlank { "Pest Entry & Activity Assessment" },
                pestCategory = root.optString("pestCategory", "general_pest").ifBlank { "general_pest" },
                urgencyLevel = root.optString("urgencyLevel", "Moderate").ifBlank { "Moderate" },
                confidenceSummary = root.optString("confidenceSummary", "").trim(),
                obviousSignsDetected = root.stringList("obviousSignsDetected"),
                howToSealAndExcludeSteps = root.stringList("howToSealAndExcludeSteps"),
                treatmentAndResidualInsight = root.optString("treatmentAndResidualInsight", "").trim(),
                termiteStrategyNote = root.optString("termiteStrategyNote", "").trim(),
                diySuppliesNeeded = supplies,
                shouldCallLicensedPro = root.optBoolean("shouldCallLicensedPro", false),
                recommendedProReason = root.optString("recommendedProReason", "").trim()
            )
        }
    }
}
