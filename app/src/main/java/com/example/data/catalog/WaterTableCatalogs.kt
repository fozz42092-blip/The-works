package com.example.data.catalog

import com.example.data.model.AdaptiveSolutionStat
import com.example.data.model.ContinuousLearningSnapshot
import com.example.data.model.ContractorMatchResult
import com.example.data.model.ContractorPortfolioItem
import com.example.data.model.ContractorProfile
import com.example.data.model.ContractorReputation
import com.example.data.model.ContractorReview
import com.example.data.model.DiyLeakTestStep
import com.example.data.model.ExternalContractorGlassWallGroup
import com.example.data.model.IicrcAuditRecord
import com.example.data.model.ProblemMatchQuery
import com.example.data.model.SolutionOutcome
import com.example.data.model.WaterReport
import com.example.data.model.WholesaleProduct
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object WaterTableCatalogs {

    val diyLeakProtocols: List<DiyLeakTestStep> = listOf(
        DiyLeakTestStep(
            id = "meter_spin",
            title = "1. Water Meter Spin & Shutoff Test",
            durationLabel = "15–30 min",
            instructions = "Turn off all indoor faucets, ice makers, and irrigation. Record your water meter's low-flow leak triangle/dial, wait 20 minutes, and check if it moved.",
            positiveMeans = "Meter moved -> Pressurized municipal/well plumbing leak (NOT groundwater table).",
            negativeMeans = "Meter stayed 100% still -> Supply pipes are tight; water is groundwater, rain, or drain."
        ),
        DiyLeakTestStep(
            id = "chlorine_strip",
            title = "2. Pool Chlorine Strip Test",
            durationLabel = "2 min",
            instructions = "Dip a standard pool/spa free-chlorine test strip into the standing or seeping water. City tap water typically shows 0.5–2.0 ppm free chlorine.",
            positiveMeans = "Chlorine detected -> Treated municipal supply pipe or pool line leak.",
            negativeMeans = "0 ppm chlorine -> Consistent with natural groundwater table, rain runoff, or septic."
        ),
        DiyLeakTestStep(
            id = "plastic_dome",
            title = "3. Sub-Slab Plastic Sheet Moisture Test",
            durationLabel = "12–24 hrs",
            instructions = "Tape an 18x18 inch clear heavy plastic square tightly on all 4 edges onto dry concrete near the wet spot.",
            positiveMeans = "Condensation/droplets form UNDER the plastic -> Upward hydrostatic groundwater pressure.",
            negativeMeans = "Water forms only on TOP of plastic -> Humid indoor air condensation, not groundwater."
        ),
        DiyLeakTestStep(
            id = "dye_trace",
            title = "4. Drain & Gutter Dye Trace",
            durationLabel = "30 min",
            instructions = "Pour bright plumbing tracer dye (or food coloring) into nearby storm drains, sump discharge, or downspouts with a garden hose running.",
            positiveMeans = "Colored water appears at seepage spot -> Cracked downspout leader or lateral drain pipe.",
            negativeMeans = "Seepage stays clear -> Likely natural ground water table or distant subsurface flow."
        ),
        DiyLeakTestStep(
            id = "rain_lag",
            title = "5. Post-Rain 24h Lag Observation",
            durationLabel = "24–48 hrs",
            instructions = "Note when water appears relative to rainfall. Surface runoff peaks during rain; water table intrusion often rises 12–48 hours AFTER rain stops.",
            positiveMeans = "Water rises 12–48h after rain stops & stays for days -> Classic elevated ground water table.",
            negativeMeans = "Appears only during downpour and dries fast -> Surface grading or gutter overflow issue."
        )
    )

    val wholesaleProducts: List<WholesaleProduct> = listOf(
        WholesaleProduct(
            sku = "WT-FD-100",
            name = "4\" Perforated French Drain Pipe (100 ft)",
            category = "French Drain",
            unitLabel = "100 ft Coil",
            wholesalePriceUsd = 68.0,
            retailBenchmarkUsd = 104.0,
            specs = "HDPE single-wall slotted pipe with high crush rating for perimeter & yard trenches.",
            shipsFrom = "Regional Pipe Depot · 2-3 days"
        ),
        WholesaleProduct(
            sku = "WT-TW-50",
            name = "4\" Triple-Wall Rigid Perforated Pipe (50 ft)",
            category = "French Drain",
            unitLabel = "5 x 10 ft Bundle",
            wholesalePriceUsd = 79.0,
            retailBenchmarkUsd = 118.0,
            specs = "Smooth interior wall for fast flow & snake cleanout under driveways and lawns.",
            shipsFrom = "Regional Pipe Depot · 2-3 days"
        ),
        WholesaleProduct(
            sku = "WT-GEO-300",
            name = "Non-Woven Geotextile Trench Fabric (3' x 100')",
            category = "Fabric & Gravel",
            unitLabel = "300 sq ft Roll",
            wholesalePriceUsd = 44.0,
            retailBenchmarkUsd = 69.0,
            specs = "4 oz needle-punched polypropylene stops clay/silt clogging while passing water fast.",
            shipsFrom = "GeoSupply Hub · 1-2 days"
        ),
        WholesaleProduct(
            sku = "WT-GRV-500",
            name = "Washed #57 Drainage Aggregate (10 x 50 lb Bags)",
            category = "Fabric & Gravel",
            unitLabel = "500 lb Pallet Pack",
            wholesalePriceUsd = 64.0,
            retailBenchmarkUsd = 92.0,
            specs = "Clean angular crushed stone for maximum void space around perforated French drains.",
            shipsFrom = "Local Quarry Dispatch · 2-4 days"
        ),
        WholesaleProduct(
            sku = "WT-PUMP-050",
            name = "1/2 HP Cast-Iron Submersible Sump Pump",
            category = "Sump Pumps",
            unitLabel = "1 Unit (4,300 GPH)",
            wholesalePriceUsd = 129.0,
            retailBenchmarkUsd = 189.0,
            specs = "Thermal-protected cast iron housing with vertical mechanical float switch.",
            shipsFrom = "Pump Direct Warehouse · 1-2 days"
        ),
        WholesaleProduct(
            sku = "WT-BKUP-12V",
            name = "12V Battery Backup Sump System + Smart Charger",
            category = "Sump Pumps",
            unitLabel = "Backup Pump + Controller",
            wholesalePriceUsd = 198.0,
            retailBenchmarkUsd = 289.0,
            specs = "Auto-switches during storm power outages; 2,400 GPH auxiliary capacity + alarm.",
            shipsFrom = "Pump Direct Warehouse · 1-2 days"
        ),
        WholesaleProduct(
            sku = "WT-BSN-18",
            name = "18\" x 22\" Perforated Sump Basin & Sealed Lid",
            category = "Sump Pumps",
            unitLabel = "Basin + Gasket Lid",
            wholesalePriceUsd = 54.0,
            retailBenchmarkUsd = 82.0,
            specs = "Heavy-duty structural basin with dual 2\" knockouts for discharge & vent.",
            shipsFrom = "Regional Pipe Depot · 2-3 days"
        ),
        WholesaleProduct(
            sku = "WT-CHK-15",
            name = "Silent Spring Check Valve + 1-1/2\" Fittings Kit",
            category = "Fittings",
            unitLabel = "Valve + Couplers + Clamps",
            wholesalePriceUsd = 21.0,
            retailBenchmarkUsd = 34.0,
            specs = "Eliminates water-hammer slam and prevents discharge backflow into sump pit.",
            shipsFrom = "Pump Direct Warehouse · 1-2 days"
        ),
        WholesaleProduct(
            sku = "WT-CB-12",
            name = "12\" Catch Basin + Atrium Grate + 2 Adapters",
            category = "Fittings",
            unitLabel = "Complete Basin Kit",
            wholesalePriceUsd = 36.0,
            retailBenchmarkUsd = 56.0,
            specs = "Captures surface & low-spot yard water before it saturates foundations.",
            shipsFrom = "Regional Pipe Depot · 2-3 days"
        ),
        WholesaleProduct(
            sku = "WT-HYD-20",
            name = "Hydrostatic Relief Valve + Hydraulic Cement (20 lb)",
            category = "Waterproofing",
            unitLabel = "2 Valves + 20 lb Pail",
            wholesalePriceUsd = 42.0,
            retailBenchmarkUsd = 65.0,
            specs = "Sets in 3 minutes against active water seepage; relieves sub-slab uplift pressure.",
            shipsFrom = "GeoSupply Hub · 1-2 days"
        ),
        WholesaleProduct(
            sku = "WT-LEAK-KIT",
            name = "DIY Groundwater vs. Leak Detection Test Kit",
            category = "Waterproofing",
            unitLabel = "50 Chlorine Strips + Dye + Film",
            wholesalePriceUsd = 19.0,
            retailBenchmarkUsd = 31.0,
            specs = "Includes free-chlorine strips, 6 plumbing tracer dye tablets, and sub-slab vapor film.",
            shipsFrom = "GeoSupply Hub · 1-2 days"
        ),
        WholesaleProduct(
            sku = "WT-TRN-12",
            name = "Driveway & Garage Trench Channel Drain (12 ft Kit)",
            category = "French Drain",
            unitLabel = "4 x 3 ft Channels + Grates",
            wholesalePriceUsd = 112.0,
            retailBenchmarkUsd = 168.0,
            specs = "Vehicle-rated Class B galvanized steel grate intercepts street & driveway runoff.",
            shipsFrom = "Regional Pipe Depot · 2-3 days"
        )
    )

    fun distanceMiles(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        if (lat1 == 0.0 && lon1 == 0.0) return 999.0
        if (lat2 == 0.0 && lon2 == 0.0) return 999.0
        val earthRadiusMiles = 3958.8
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return ((earthRadiusMiles * c) * 10.0).roundToInt() / 10.0
    }

    fun computeContractorReputations(
        contractors: List<ContractorProfile>,
        reviews: List<ContractorReview>,
        userLat: Double,
        userLon: Double
    ): Map<String, ContractorReputation> {
        val reviewsByContractor = reviews.groupBy { it.contractorUserId }
        return contractors.associate { pro ->
            val proReviews = reviewsByContractor[pro.userId].orEmpty()
            val count = proReviews.size
            val avgOverall = if (count > 0) ((proReviews.map { it.overallRating }.average()) * 10.0).roundToInt() / 10.0 else 0.0
            val avgComm = if (count > 0) ((proReviews.map { it.communicationRating }.average()) * 10.0).roundToInt() / 10.0 else 0.0
            val pos = proReviews.count { it.overallRating >= 4 && it.wouldRecommend && !it.flagShoddyWork }
            val neg = proReviews.count { it.overallRating <= 2 || !it.wouldRecommend }
            val shoddy = proReviews.count { it.flagShoddyWork }
            val proofCount = proReviews.count { it.hasProof || it.proofPhotoBase64.isNotBlank() }

            val blacklisted = shoddy >= 2 ||
                (count >= 2 && avgOverall <= 2.2) ||
                (count >= 2 && neg >= 2 && avgComm <= 2.3)

            val reason = when {
                shoddy >= 2 -> "Blocked: $shoddy verified shoddy-work / abandonment flags on the Glass Wall."
                count >= 2 && avgOverall <= 2.2 -> "Blocked: Low honest rating ($avgOverall★ across $count client reviews)."
                count >= 2 && neg >= 2 && avgComm <= 2.3 -> "Blocked: Poor client communication ($avgComm★ avg communication)."
                else -> ""
            }

            val distMiles = distanceMiles(userLat, userLon, pro.latitude, pro.longitude)
            // Prioritize nearest contractors who are actively helping (helpfulContributions + completedReviews + positive rating)
            val helpfulPoints = (pro.helpfulContributionsCount * 3.0) + (pro.completedReviewsCount * 2.0) + (avgOverall * 4.0) - (shoddy * 50.0)
            val distancePenalty = distMiles.coerceAtMost(500.0) * 0.65
            val matchScore = helpfulPoints - distancePenalty

            pro.userId to ContractorReputation(
                contractorUserId = pro.userId,
                avgOverallRating = avgOverall,
                avgCommunicationRating = avgComm,
                reviewCount = count,
                positiveCount = pos,
                negativeCount = neg,
                shoddyWorkFlags = shoddy,
                proofReviewCount = proofCount,
                isBlacklisted = blacklisted,
                blacklistReason = reason,
                distanceMiles = distMiles,
                helpfulRankScore = matchScore
            )
        }
    }

    const val CSLB_SEARCH_URL = "https://www.cslb.ca.gov/OnlineServices/CheckLicenseII/CheckLicense.aspx"
    const val CSLB_COMPLAINT_URL = "https://www.cslb.ca.gov/Consumers/Filing_A_Complaint/"
    const val DOJ_LIVE_SCAN_STATUS_URL = "https://applicantstatus.doj.ca.gov/"
    const val IICRC_LOCATOR_URL = "https://iicrc.org/iicrclocator/"
    const val IICRC_COMPLAINT_URL = "https://iicrc.org/consumers/"
    const val IICRC_STANDARDS_URL = "https://iicrc.org/standards/"

    fun buildCslbLookupUrl(licenseNumber: String): String {
        val digits = licenseNumber.filter { it.isDigit() }
        return if (digits.length in 5..8) {
            "https://www.cslb.ca.gov/OnlineServices/CheckLicenseII/LicenseDetail.aspx?LicNum=$digits"
        } else {
            CSLB_SEARCH_URL
        }
    }

    fun computeContinuousLearningSnapshot(
        reports: List<WaterReport>,
        outcomes: List<SolutionOutcome>,
        reviews: List<ContractorReview>,
        audits: List<IicrcAuditRecord>,
        adaptiveSolutions: List<AdaptiveSolutionStat>
    ): ContinuousLearningSnapshot {
        val topCity = reports.groupingBy { it.city.ifBlank { "Regional" } }
            .eachCount()
            .maxByOrNull { it.value }
            ?.let { "${it.key} (${it.value} field logs)" }
            ?: "Awaiting first community pins"

        val topSolution = adaptiveSolutions.firstOrNull()?.let {
            "${it.solutionName}: ${it.successRatePercent}% worked (${it.sampleSize} client reports)"
        } ?: "Gathering solution outcomes"

        val completedJobs = outcomes.count { it.worked } + audits.count { it.iicrcStatus == "endorsed" }
        val timeStr = SimpleDateFormat("HH:mm", Locale.US).format(Date())

        return ContinuousLearningSnapshot(
            totalFieldReports = reports.size,
            verifiedOutcomesCount = outcomes.size,
            completedJobsCount = completedJobs,
            honestReviewsCount = reviews.size,
            topCityInsight = topCity,
            topSolutionHeadline = topSolution,
            updatedTimestampLabel = "Live Sync · $timeStr"
        )
    }

    /**
     * Scores and ranks every active contractor against the client's exact problem (peril, keywords,
     * dig depth, construction stage, city/GPS proximity, matching portfolio/referrals, proven success
     * rate guarantee, CSLB & Live Scan transparency, IICRC audits, and Glass Wall honesty),
     * disqualifying blacklisted contractors so clients get the best-fit non-blacklisted pro at #1.
     */
    fun matchPerfectContractorsForProblem(
        query: ProblemMatchQuery,
        contractors: List<ContractorProfile>,
        reputations: Map<String, ContractorReputation>,
        portfolioItems: List<ContractorPortfolioItem>,
        iicrcAudits: List<IicrcAuditRecord> = emptyList()
    ): List<ContractorMatchResult> {
        val lowerText = query.problemText.lowercase()
        val inferredCategory = when {
            query.problemCategory != "auto" -> query.problemCategory
            listOf("mold", "mildew", "spore", "musty", "black spots", "hepa").any { lowerText.contains(it) } -> "mold_mitigation"
            listOf("smoke", "fire", "soot", "char", "burnt", "ash", "odor").any { lowerText.contains(it) } -> "smoke_fire"
            listOf("flood", "drywall", "carpet", "standing water", "burst", "extract", "dehumid").any { lowerText.contains(it) } -> "water_restoration"
            listOf("french drain", "trench", "gravel", "catch basin", "yard", "runoff", "downspout", "curtain drain").any { lowerText.contains(it) } -> "drainage_french_drain"
            listOf("chlorine", "meter", "pipe", "plumb", "slab leak", "pressure").any { lowerText.contains(it) } -> "plumbing_leak"
            listOf("remodel", "before finish", "footing", "excavat", "water table", "hydrostatic", "soil", "perched").any { lowerText.contains(it) } ||
                query.constructionStage != "none" || query.depthFeet in 0.1..8.0 -> "groundwater"
            else -> "water_restoration"
        }

        val desiredSpecialties = when (inferredCategory) {
            "mold_mitigation" -> listOf("mold_remediation", "full_service_restoration", "water_mitigation", "iicrc_inspector")
            "smoke_fire", "smoke_damage", "fire_restoration" -> listOf("smoke_fire_restoration", "full_service_restoration", "iicrc_inspector")
            "water_restoration" -> listOf("water_mitigation", "full_service_restoration", "drainage_contractor", "soil_engineer")
            "drainage_french_drain" -> listOf("drainage_contractor", "soil_engineer", "water_mitigation", "full_service_restoration")
            "construction_waterproofing", "groundwater" -> listOf("soil_engineer", "drainage_contractor", "general_engineer", "water_mitigation", "full_service_restoration")
            "plumbing_leak" -> listOf("water_mitigation", "full_service_restoration", "general_engineer")
            else -> listOf("full_service_restoration", "water_mitigation", "soil_engineer")
        }

        val targetPortfolioCategory = when (inferredCategory) {
            "mold_mitigation" -> "mold_mitigation"
            "smoke_fire", "smoke_damage", "fire_restoration" -> "fire_restoration"
            "water_restoration" -> "water_restoration"
            else -> "groundwater"
        }

        val results = contractors.map { pro ->
            val rep = reputations[pro.userId]
            val proPortfolio = portfolioItems.filter { it.userId == pro.userId }
            val matchingPortfolio = proPortfolio.count {
                it.lossCategory == targetPortfolioCategory ||
                    (inferredCategory == "smoke_fire" && it.lossCategory == "smoke_damage")
            }
            val proAudits = iicrcAudits.filter { it.contractorUserId == pro.userId }
            val iicrcEndorsedCount = proAudits.count { it.iicrcStatus == "endorsed" }
            val iicrcFlaggedCount = proAudits.count { it.iicrcStatus == "flagged_outdated" }

            // Compute Proven Job Experience & Client-Verified Success Rate
            val positiveReviewJobs = rep?.positiveCount ?: 0
            val totalCompletedSignals = matchingPortfolio + pro.completedReviewsCount + iicrcEndorsedCount
            val totalEvaluated = (rep?.reviewCount ?: 0) + matchingPortfolio
            val verifiedSuccessPct = when {
                totalEvaluated > 0 -> (((positiveReviewJobs + matchingPortfolio).toDouble() / totalEvaluated.toDouble()) * 100.0)
                    .roundToInt().coerceIn(0, 100)
                pro.completedReviewsCount >= 1 -> 92
                else -> 0
            }
            val guaranteeEligible = (rep?.isBlacklisted != true) &&
                (rep?.shoddyWorkFlags ?: 0) == 0 &&
                iicrcFlaggedCount == 0 &&
                (matchingPortfolio >= 1 || positiveReviewJobs >= 2 || pro.completedReviewsCount >= 2) &&
                (verifiedSuccessPct >= 80 || pro.completedReviewsCount >= 2)

            val guaranteeLabel = if (guaranteeEligible) {
                "Verified Experience Guarantee · ${totalCompletedSignals.coerceAtLeast(1)} Jobs (${verifiedSuccessPct.coerceAtLeast(85)}% Client Success)"
            } else ""

            if (rep?.isBlacklisted == true) {
                return@map ContractorMatchResult(
                    contractor = pro,
                    reputation = rep,
                    matchScorePercent = 0,
                    isPerfectMatch = false,
                    matchReasons = listOf("Disqualified: Blacklisted on Glass Wall (${rep.blacklistReason})"),
                    recommendedServiceMode = "remote_advice",
                    portfolioMatchCount = matchingPortfolio,
                    provenSimilarJobsCount = totalCompletedSignals,
                    verifiedSuccessRatePercent = verifiedSuccessPct,
                    guaranteeEligible = false,
                    guaranteeBadgeLabel = ""
                )
            }

            var score = 16.0
            val reasons = mutableListOf<String>()

            // 1. Specialty precision (up to +34 pts)
            val primarySpec = desiredSpecialties.firstOrNull()
            when {
                pro.specialty == primarySpec -> {
                    score += 34.0
                    reasons.add("Exact specialty match for ${categoryFriendlyName(inferredCategory)}")
                }
                pro.specialty == "full_service_restoration" &&
                    inferredCategory in listOf("water_restoration", "mold_mitigation", "smoke_fire", "smoke_damage", "fire_restoration") -> {
                    score += 29.0
                    reasons.add("Full-service multi-peril restoration licensed")
                }
                pro.specialty in desiredSpecialties -> {
                    score += 20.0
                    reasons.add("Qualified secondary specialty for this issue")
                }
                else -> score += 4.0
            }

            // 2. Construction stage & shallow water table bonus (up to +12 pts)
            if (query.constructionStage == "in_process" && pro.specialty in listOf("soil_engineer", "drainage_contractor", "general_engineer")) {
                score += 9.0
                reasons.add("Pre-finish drainage & slab engineering fit")
            } else if (query.constructionStage == "after_remodel" && pro.specialty in listOf("water_mitigation", "mold_remediation", "full_service_restoration")) {
                score += 9.0
                reasons.add("Post-remodel non-destructive dry-out & containment")
            }
            if (query.depthFeet in 0.1..5.0 && pro.specialty in listOf("soil_engineer", "drainage_contractor")) {
                score += 4.0
                reasons.add("Shallow water table (${query.depthFeet} ft) relief experience")
            }

            // 3. Portfolio, referrals & track-record guarantee (up to +14 pts)
            if (matchingPortfolio > 0) {
                score += (matchingPortfolio * 4.0).coerceAtMost(12.0)
                reasons.add("$matchingPortfolio proven job(s)/referrals in ${categoryFriendlyName(inferredCategory)}")
            } else if (proPortfolio.isNotEmpty()) {
                score += 4.0
            }
            if (guaranteeEligible) {
                score += 6.0
                reasons.add("$verifiedSuccessPct% verified client success across past jobs")
            }

            // 4. Safety & Governance Trust: Live Scan + CSLB + IICRC (up to +14 pts)
            if (pro.liveScanStatus == "verified_cleared") {
                score += 7.0
                reasons.add("DOJ/FBI Live Scan Cleared · Safe for Home/Business Entry")
            }
            if (pro.cslbBoardStatus == "cslb_verified") {
                score += 5.0
                reasons.add("CSLB Southern CA / State Board Checked")
            }
            if (iicrcEndorsedCount > 0) {
                score += 5.0
                reasons.add("$iicrcEndorsedCount IICRC S500/S520/S700-endorsed plan(s)")
            }
            if (iicrcFlaggedCount > 0) {
                score -= 12.0
            }

            // 5. Proximity & service city fit (up to +14 pts)
            val dist = rep?.distanceMiles ?: 999.0
            val servesCity = query.city.isNotBlank() &&
                (pro.primaryCity.equals(query.city, ignoreCase = true) ||
                    pro.serviceCities.any { it.equals(query.city, ignoreCase = true) })
            when {
                dist <= 15.0 -> {
                    score += 14.0
                    reasons.add("${dist} mi away · Nearest responder")
                }
                servesCity -> {
                    score += 12.0
                    reasons.add("Serves ${query.city}")
                }
                dist <= 45.0 -> {
                    score += 8.0
                    reasons.add("${dist} mi regional coverage")
                }
                dist <= 120.0 -> score += 4.0
            }

            // 6. Glass Wall client trust & responsiveness
            if (rep != null && rep.reviewCount > 0) {
                if (rep.avgOverallRating >= 4.3 && rep.avgCommunicationRating >= 4.0) {
                    score += 10.0
                    reasons.add("${rep.avgOverallRating}★ honest Glass Wall & communication rating")
                } else if (rep.avgOverallRating >= 3.5) {
                    score += 5.0
                } else {
                    score -= 10.0
                }
            } else if (pro.helpfulContributionsCount >= 2 || pro.completedReviewsCount >= 2) {
                score += 6.0
                reasons.add("${pro.helpfulContributionsCount} community problems solved")
            }

            // 7. Bio/keyword bonus & On-site work alignment
            if (lowerText.isNotBlank()) {
                val bioLower = pro.bio.lowercase()
                val tokens = lowerText.split(" ", ",", ".").filter { it.length >= 4 }.take(8)
                if (tokens.any { bioLower.contains(it) }) {
                    score += 5.0
                }
            }
            if (query.needsOnSiteCrew && pro.offersOnSiteWork) {
                score += 4.0
                reasons.add("On-site mitigation crew available")
            }

            val finalPct = score.roundToInt().coerceIn(15, 99)
            val serviceMode = if (pro.offersOnSiteWork && inferredCategory in listOf("water_restoration", "mold_mitigation", "smoke_fire", "smoke_damage", "fire_restoration", "drainage_french_drain")) {
                "restoration_work"
            } else "remote_advice"

            ContractorMatchResult(
                contractor = pro,
                reputation = rep,
                matchScorePercent = finalPct,
                isPerfectMatch = false,
                matchReasons = reasons.take(5).ifEmpty { listOf("Active licensed professional") },
                recommendedServiceMode = serviceMode,
                portfolioMatchCount = matchingPortfolio,
                provenSimilarJobsCount = totalCompletedSignals,
                verifiedSuccessRatePercent = verifiedSuccessPct,
                guaranteeEligible = guaranteeEligible,
                guaranteeBadgeLabel = guaranteeLabel
            )
        }.sortedWith(
            compareBy<ContractorMatchResult> { it.reputation?.isBlacklisted == true }
                .thenByDescending { it.matchScorePercent }
                .thenBy { it.reputation?.distanceMiles ?: 999.0 }
        )

        val topScore = results.firstOrNull { it.reputation?.isBlacklisted != true }?.matchScorePercent ?: 0
        return results.mapIndexed { idx, res ->
            val qualifyPerfect = (itNotBlacklisted(res)) &&
                ((idx == 0 && res.matchScorePercent >= 55) || (res.matchScorePercent >= 82 && res.matchScorePercent >= topScore - 5))
            res.copy(isPerfectMatch = qualifyPerfect)
        }
    }

    private fun itNotBlacklisted(res: ContractorMatchResult): Boolean = res.reputation?.isBlacklisted != true

    private fun categoryFriendlyName(cat: String): String = when (cat) {
        "mold_mitigation" -> "Mold Mitigation"
        "smoke_fire", "smoke_damage", "fire_restoration" -> "Smoke & Fire Restoration"
        "water_restoration" -> "Water Mitigation"
        "drainage_french_drain" -> "French Drain & Surface Drainage"
        "plumbing_leak" -> "Leak Detection & Dry-Out"
        else -> "Groundwater & Engineering"
    }

    fun buildExternalGlassWallGroups(reviews: List<ContractorReview>): List<ExternalContractorGlassWallGroup> {
        return reviews.filter { it.contractorUserId == "external" }
            .groupBy { it.contractorName.trim().lowercase() }
            .mapNotNull { (_, group) ->
                val first = group.firstOrNull() ?: return@mapNotNull null
                val avgOverall = ((group.map { it.overallRating }.average()) * 10.0).roundToInt() / 10.0
                val avgComm = ((group.map { it.communicationRating }.average()) * 10.0).roundToInt() / 10.0
                val shoddy = group.count { it.flagShoddyWork }
                val neg = group.count { it.overallRating <= 2 || !it.wouldRecommend }
                val blacklisted = shoddy >= 2 || (group.size >= 2 && avgOverall <= 2.2) || (group.size >= 1 && shoddy >= 1 && avgOverall <= 2.0)
                ExternalContractorGlassWallGroup(
                    contractorName = first.contractorName,
                    companyOrLicenseRef = group.firstOrNull { it.companyOrLicenseRef.isNotBlank() }?.companyOrLicenseRef.orEmpty(),
                    city = first.city,
                    avgOverallRating = avgOverall,
                    avgCommunicationRating = avgComm,
                    shoddyWorkFlags = shoddy,
                    isBlacklisted = blacklisted,
                    reviews = group.sortedByDescending { it.createdAt?.seconds ?: 0L }
                )
            }
            .sortedWith(compareByDescending<ExternalContractorGlassWallGroup> { it.isBlacklisted }
                .thenByDescending { it.reviews.size })
    }

    fun computeAdaptiveSolutionStats(
        outcomes: List<SolutionOutcome>,
        reports: List<WaterReport>
    ): List<AdaptiveSolutionStat> {
        val grouped = outcomes.groupBy { it.solutionName.trim().lowercase() }
        val statsFromOutcomes = grouped.mapNotNull { (_, items) ->
            val name = items.firstOrNull()?.solutionName ?: return@mapNotNull null
            val workedCount = items.count { it.worked }
            val successPct = ((workedCount.toDouble() / items.size.toDouble()) * 100.0).roundToInt()
            val avgRating = items.map { it.effectivenessScore }.average()
            val avgCost = items.map { it.estimatedCostUsd }.filter { it > 0 }.average()
                .let { if (it.isNaN()) 0 else it.roundToInt() }
            val topSoil = items.groupingBy { it.soilType }.eachCount().maxByOrNull { it.value }?.key ?: "unknown"
            val topSource = items.groupingBy { it.waterSource }.eachCount().maxByOrNull { it.value }?.key ?: "ground_water_table"
            AdaptiveSolutionStat(
                solutionName = name,
                waterSource = topSource,
                soilType = topSoil,
                successRatePercent = successPct,
                avgRating = (avgRating * 10.0).roundToInt() / 10.0,
                sampleSize = items.size,
                avgCostUsd = avgCost
            )
        }

        val reportOutcomes = reports.filter { it.solutionApplied.isNotBlank() }
            .groupBy { it.solutionApplied.trim().lowercase() }
            .mapNotNull { (key, list) ->
                if (grouped.containsKey(key)) return@mapNotNull null
                val name = list.first().solutionApplied
                val workedCount = list.count { it.solutionWorked }
                AdaptiveSolutionStat(
                    solutionName = name,
                    waterSource = list.first().waterSourceVerdict,
                    soilType = list.first().soilType,
                    successRatePercent = ((workedCount.toDouble() / list.size) * 100.0).roundToInt(),
                    avgRating = if (workedCount > 0) 4.4 else 2.2,
                    sampleSize = list.size,
                    avgCostUsd = 0
                )
            }

        return (statsFromOutcomes + reportOutcomes)
            .sortedWith(compareByDescending<AdaptiveSolutionStat> { it.successRatePercent }
                .thenByDescending { it.sampleSize })
    }

    fun evaluateDiyTestResults(results: Map<String, String>): DiyEvaluation {
        val meter = results["meter_spin"]
        val chlorine = results["chlorine_strip"]
        val dome = results["plastic_dome"]
        val dye = results["dye_trace"]
        val rainLag = results["rain_lag"]

        return when {
            meter == "yes" || chlorine == "yes" -> DiyEvaluation(
                headline = "Likely Pressurized Plumbing / Supply Leak",
                confidenceBadge = "High Plumbing Signal",
                summary = "Meter movement or residual chlorine points strongly to a treated water line leak rather than natural ground water table.",
                nextSteps = listOf(
                    "Shut off main valve or irrigation zone to isolate the leaking branch.",
                    "Avoid pouring concrete or installing a French drain until the pressurized pipe is repaired.",
                    "Contact a Leak Detection Specialist or Water Mitigation Pro from the Pros tab."
                ),
                recommendContractor = true,
                recommendWholesaleKit = false
            )
            dome == "yes" || rainLag == "yes" -> DiyEvaluation(
                headline = "Consistent with Elevated Ground Water Table",
                confidenceBadge = "Hydrostatic Groundwater Signal",
                summary = "Sub-slab vapor pressure and post-rain 12–48h lag indicate upward ground water table intrusion rather than a leaking pipe.",
                nextSteps = listOf(
                    "Log this spot on the WaterTable Map so neighbors & drivers know the depth.",
                    "Relieve hydrostatic pressure using a perimeter French drain with geotextile fabric and a battery-backed sump pump.",
                    "Request a remote Soil Engineer or Water Mitigation review before trenching."
                ),
                recommendContractor = true,
                recommendWholesaleKit = true
            )
            dye == "yes" -> DiyEvaluation(
                headline = "Surface Drain or Downspout Lateral Break",
                confidenceBadge = "Drainage Lateral Signal",
                summary = "Tracer dye migration confirms water is escaping a cracked storm lateral, gutter pipe, or catch basin.",
                nextSteps = listOf(
                    "Extend downspouts at least 10 ft away from foundation walls.",
                    "Replace cracked corrugated drain sections with rigid triple-wall pipe & catch basins.",
                    "Check Wholesale Supply for pipe, catch basins, and trench fabric."
                ),
                recommendContractor = false,
                recommendWholesaleKit = true
            )
            else -> DiyEvaluation(
                headline = "Complete 2+ Tests Above for Instant Triage",
                confidenceBadge = "DIY Triage Ready",
                summary = "Run the 2-minute Chlorine Strip and 20-minute Meter Spin tests first—they immediately separate costly pipe leaks from natural groundwater.",
                nextSteps = listOf(
                    "Start with Test #1 (Water Meter) and Test #2 (Chlorine Strip).",
                    "Take photos of the wet area and run AI Triage in the Diagnose tab."
                ),
                recommendContractor = false,
                recommendWholesaleKit = false
            )
        }
    }
}

data class DiyEvaluation(
    val headline: String,
    val confidenceBadge: String,
    val summary: String,
    val nextSteps: List<String>,
    val recommendContractor: Boolean,
    val recommendWholesaleKit: Boolean
)
