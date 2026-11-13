package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.AdaptiveSolutionStat
import com.example.data.model.AiMatchConciergeVerdict
import com.example.data.model.ConsultationRequest
import com.example.data.model.ContractorCopilotAdvice
import com.example.data.model.ContractorMatchResult
import com.example.data.model.GroundedCreditLink
import com.example.data.model.GroundwaterAiDiagnosis
import com.example.data.model.IicrcAuditRecord
import com.example.data.validation.AiModerationVerdict
import com.example.data.model.JobKitRecommendation
import com.example.data.model.LiveDisasterEvent
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
        liveDisasters: List<LiveDisasterEvent>
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
                - Mapped FEMA Flood Zone: $femaFloodZone ($floodRiskDesc)
                - WaterTable Community Shallow Depth Logs: $gwSummary
                - Nearest Active Wildfire Distance: ${nearestFire?.let { "$it miles" } ?: "None within 50 miles"}
                - Nearest Active Flood Alert/Gauge: ${nearestFlood?.let { "$it miles" } ?: "None active right now"}

                Use Google Search to check historical flood risk, wildfire hazard severity zones (WUI/CAL FIRE/FEMA), and regional groundwater/soil drainage history in $city, $state.
                Provide:
                [FIRE_HAZARD_ZONE]
                Short label (e.g., High WUI / Moderate Fire Zone / Low Urban Fire Zone)
                [/FIRE_HAZARD_ZONE]
                [HISTORICAL_DOSSIER]
                Concise pre-purchase / owner historical risk brief covering flood history, fire history, groundwater seepage risk, insurance considerations, and what cleanses/mitigations contractors advise BEFORE closing or remodeling.
                [/HISTORICAL_DOSSIER]
                [CHECKLIST]
                4 short pipe-separated pre-buy or cleanse action items (e.g., Request CLUE report|Camera sewer & French drain scope|Moisture meter slab check|HEPA/vent brush clearance)
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
                ?.take(5)
                .orEmpty()

            val fireZone = extractTaggedBlock(raw, "FIRE_HAZARD_ZONE").trim()
                .ifBlank { if ((nearestFire ?: 999.0) <= 25.0) "Elevated Wildfire Proximity" else "Standard Fire Hazard Zone" }
            val dossier = extractTaggedBlock(raw, "HISTORICAL_DOSSIER").trim().ifBlank { raw.take(1400) }
            val checklistRaw = extractTaggedBlock(raw, "CHECKLIST").trim()
            val checklist = checklistRaw.split("|", "\n")
                .map { it.trim().trimStart('-', '•', ' ') }
                .filter { it.length >= 4 }
                .take(5)
                .ifEmpty {
                    listOf(
                        "Check CLUE insurance loss history & seller disclosures",
                        "Inspect sub-slab / crawlspace moisture & groundwater depth logs",
                        "Confirm FEMA flood zone & drainage slope away from foundation",
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
                    creditLinks = links
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

    private companion object {
        const val TAG = "AiGroundwaterRepo"
    }
}
