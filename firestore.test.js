const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_resident_1";
const BOB_UID = "bob_engineer_2";
const CAROL_UID = "carol_other_3";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validReportPayload(uid) {
  const now = new Date();
  return {
    userId: uid,
    authorName: "Alice R.",
    authorRole: "resident",
    visibility: "community",
    moderationStatus: "approved",
    moderationNotes: "Auto-verified completeness & plausibility.",
    plausibilityScore: 92,
    city: "Sacramento",
    stateOrRegion: "CA",
    addressHint: "Oak Ave & 14th St",
    latitude: 38.5816,
    longitude: -121.4944,
    depthFeet: 4.5,
    soilType: "clay",
    constructionStage: "in_process",
    lossCategory: "water_restoration",
    situationType: "water_loss",
    waterSourceVerdict: "water_loss_mitigation",
    aiConfidence: 88,
    aiSummary: "Category 2/3 basement water loss requiring extraction and LGR dehumidification.",
    lossDescription: "2 inches standing water in finished basement and utility closet; drywall wet 18 inches up.",
    restorationScope: "Extract standing water, 24-inch flood cut on drywall, HEPA air scrubber + 2 LGR dehumidifiers.",
    recommendedSolutions: ["Water extraction + LGR dehumidifiers", "24-inch drywall flood cut + antimicrobial"],
    notes: "Started after main basement sump failed during storm.",
    mediaType: "photo",
    mediaPreviewBase64: "abc123preview",
    extraPhotosBase64: ["extra1", "extra2"],
    videoDurationLabel: "",
    streetFloodRisk: "high",
    upvotesCount: 0,
    solutionApplied: "",
    solutionWorked: false,
    engineerVerified: false,
    verifiedByContractorId: "",
    createdAt: now,
    updatedAt: now,
  };
}

function validContractorPayload(uid) {
  const now = new Date();
  return {
    userId: uid,
    displayName: "Bob Vance, WRT",
    companyName: "Sierra Water, Fire & Mold Mitigation",
    specialty: "full_service_restoration",
    licenseNumber: "IICRC-88412",
    licensingState: "CA",
    cslbBoardStatus: "cslb_verified",
    cslbLookupUrl: "https://www.cslb.ca.gov/OnlineServices/CheckLicenseII/LicenseDetail.aspx?LicNum=88412",
    iicrcCertNumber: "WRT-88412",
    isIicrcGovernor: false,
    liveScanStatus: "verified_cleared",
    liveScanAtiRef: "ATI-A123456",
    liveScanAgency: "CA DOJ / Certifix",
    liveScanClearedDate: "2026-10-15",
    liveScanProofBase64: "",
    primaryCity: "Sacramento",
    latitude: 38.5816,
    longitude: -121.4944,
    serviceCities: ["Sacramento", "Davis"],
    consultationFeeUsd: 45,
    platformFeePercent: 10,
    offersOnSiteWork: true,
    bio: "Licensed water, mold, smoke & fire restoration contractor with 24/7 dry-out dispatch.",
    completedReviewsCount: 0,
    helpfulContributionsCount: 0,
    isActive: true,
    createdAt: now,
    updatedAt: now,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or list protected collections", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("water_reports").where("visibility", "==", "community").get());
  await assertFails(unauth.collection("contractors").where("isActive", "==", true).get());
  await assertFails(unauth.collection("contractor_reviews").where("isPublic", "==", true).get());
  await assertFails(unauth.collection("contractor_portfolio").where("isPublic", "==", true).get());
  await assertFails(unauth.collection("consultations").where("clientUserId", "==", ALICE_UID).get());
});

test("Authenticated user can create and query multi-peril water_reports with extra photos", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(alice.collection("water_reports").doc("report_1").set(validReportPayload(ALICE_UID)));
  await assertSucceeds(alice.collection("water_reports").where("visibility", "==", "community").get());
});

test("Authenticated user cannot list water_reports without visibility filter", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(alice.collection("water_reports").get());
});

test("User cannot spoof another userId or inject ghost field on water_reports", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(alice.collection("water_reports").doc("report_spoof").set(validReportPayload(BOB_UID)));
  const ghost = { ...validReportPayload(ALICE_UID), isAdminVerified: true };
  await assertFails(alice.collection("water_reports").doc("report_ghost").set(ghost));
});

test("Cross-user cannot edit or delete someone else's water_report, but can upvote +1", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(alice.collection("water_reports").doc("report_1").set(validReportPayload(ALICE_UID)));

  const carol = testEnv.authenticatedContext(CAROL_UID).firestore();
  await assertFails(carol.collection("water_reports").doc("report_1").update({ notes: "Hacked" }));
  await assertFails(carol.collection("water_reports").doc("report_1").delete());
  await assertSucceeds(carol.collection("water_reports").doc("report_1").update({
    upvotesCount: 1,
    updatedAt: new Date(),
  }));
});

test("Contractor can register mitigation profile & upload portfolio/referrals; non-contractors cannot", async () => {
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Unregistered Alice cannot upload contractor portfolio item
  const now = new Date();
  const fakePortfolio = {
    userId: ALICE_UID,
    contractorName: "Alice R.",
    entryType: "portfolio",
    isPublic: true,
    title: "Fake Job",
    lossCategory: "water_restoration",
    city: "Sacramento",
    clientReferenceName: "Nobody",
    description: "Trying to upload without contractor registration.",
    completionYear: "2026",
    photoBase64: "abc",
    createdAt: now,
    updatedAt: now,
  };
  await assertFails(alice.collection("contractor_portfolio").doc("port_fake").set(fakePortfolio));

  // Bob registers as licensed restoration contractor
  await assertSucceeds(bob.collection("contractors").doc(BOB_UID).set(validContractorPayload(BOB_UID)));
  await assertFails(bob.collection("contractors").doc(ALICE_UID).set(validContractorPayload(BOB_UID)));

  // Registered Bob can upload portfolio showcase & past client referral
  await assertSucceeds(bob.collection("contractor_portfolio").doc("port_1").set({
    userId: BOB_UID,
    contractorName: "Bob Vance, WRT",
    entryType: "portfolio",
    isPublic: true,
    title: "Smoke & Fire Mitigation — Oak Park Residence",
    lossCategory: "fire_restoration",
    city: "Sacramento",
    clientReferenceName: "M. Ramirez (Homeowner)",
    description: "HEPA air scrubbing, char sealing, and thermal deodorization completed in 5 days.",
    completionYear: "2026",
    photoBase64: "photo_preview_1",
    createdAt: now,
    updatedAt: now,
  }));
  await assertSucceeds(bob.collection("contractor_portfolio").where("isPublic", "==", true).get());
});

test("Glass Wall honest reviews with proof: self-review blocked, and contractor cannot delete honest bad review", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  await assertSucceeds(bob.collection("contractors").doc(BOB_UID).set(validContractorPayload(BOB_UID)));

  // Bob cannot post a fake 5-star review for himself
  await assertFails(bob.collection("contractor_reviews").doc("rev_self").set({
    userId: BOB_UID,
    reviewerName: "Bob Vance",
    contractorUserId: BOB_UID,
    contractorName: "Bob Vance, WRT",
    companyOrLicenseRef: "Sierra Mitigation",
    city: "Sacramento",
    isPublic: true,
    workCategory: "water_restoration",
    overallRating: 5,
    communicationRating: 5,
    honestExperience: "I am amazing and deserve 5 stars.",
    proofDescription: "Self proof",
    proofPhotoBase64: "abc",
    hasProof: true,
    wouldRecommend: true,
    flagShoddyWork: false,
    createdAt: now,
    updatedAt: now,
  }));

  // Alice posts an honest bad review + photo proof + shoddy work flag
  await assertSucceeds(alice.collection("contractor_reviews").doc("rev_honest_1").set({
    userId: ALICE_UID,
    reviewerName: "Alice R.",
    contractorUserId: BOB_UID,
    contractorName: "Bob Vance, WRT",
    companyOrLicenseRef: "Sierra Mitigation · IICRC-88412",
    city: "Sacramento",
    isPublic: true,
    workCategory: "mold_mitigation",
    overallRating: 1,
    communicationRating: 1,
    honestExperience: "Stopped returning calls mid-job and left containment unsealed.",
    proofDescription: "Photo of ripped containment poly and wet drywall left behind.",
    proofPhotoBase64: "proof_photo_base64_stub",
    hasProof: true,
    wouldRecommend: false,
    flagShoddyWork: true,
    createdAt: now,
    updatedAt: now,
  }));

  // Alice can also log a past contractor experience with proof for an unregistered company
  await assertSucceeds(alice.collection("contractor_reviews").doc("rev_external_1").set({
    userId: ALICE_UID,
    reviewerName: "Alice R.",
    contractorUserId: "external",
    contractorName: "Shoddy DryOut Bros",
    companyOrLicenseRef: "Unverified #9901",
    city: "Sacramento",
    isPublic: true,
    workCategory: "water_restoration",
    overallRating: 1,
    communicationRating: 2,
    honestExperience: "Collected deposit and never brought dehumidifiers.",
    proofDescription: "Receipt photo + text message screenshot",
    proofPhotoBase64: "receipt_photo_stub",
    hasProof: true,
    wouldRecommend: false,
    flagShoddyWork: true,
    createdAt: now,
    updatedAt: now,
  }));

  // Bob CANNOT delete or alter Alice's honest review on the Glass Wall
  await assertFails(bob.collection("contractor_reviews").doc("rev_honest_1").delete());
  await assertFails(bob.collection("contractor_reviews").doc("rev_honest_1").update({
    overallRating: 5,
    flagShoddyWork: false,
    updatedAt: new Date(),
  }));

  await assertSucceeds(alice.collection("contractor_reviews").where("isPublic", "==", true).get());
});

test("Consultation & restoration work dispatch isolates private data and supports work estimate", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const carol = testEnv.authenticatedContext(CAROL_UID).firestore();

  await assertSucceeds(alice.collection("water_reports").doc("rep_1").set(validReportPayload(ALICE_UID)));
  await assertSucceeds(bob.collection("contractors").doc(BOB_UID).set(validContractorPayload(BOB_UID)));

  const now = new Date();
  const consultPayload = {
    clientUserId: ALICE_UID,
    clientName: "Alice R.",
    contractorUserId: BOB_UID,
    contractorName: "Bob Vance, WRT",
    reportId: "rep_1",
    city: "Sacramento",
    serviceMode: "restoration_work",
    lossCategory: "water_restoration",
    question: "Basement flooded 2 inches; please give dry-out scope and estimate to do the restoration work.",
    status: "requested",
    engineerVerdict: "pending",
    engineerRecommendation: "",
    contractorStrategy: "",
    aiCreditLinks: [],
    proposedWorkEstimateUsd: 0,
    contractorFeeUsd: 45,
    platformFeeUsd: 5,
    totalFeeUsd: 50,
    clientRating: 0,
    workAuthStatus: "none",
    workAuthTerms: "",
    paymentScheduleSummary: "",
    clientSignatureName: "",
    clientAuthorizedAtLabel: "",
    createdAt: now,
    updatedAt: now,
  };

  await assertSucceeds(alice.collection("consultations").doc("cons_1").set(consultPayload));
  await assertFails(carol.collection("consultations").doc("cons_1").get());

  await assertSucceeds(bob.collection("consultations").doc("cons_1").update({
    status: "reviewed",
    engineerVerdict: "water_mitigation_approved",
    engineerRecommendation: "24-hour extraction + 3-day LGR dehumidification + antimicrobial treatment.",
    contractorStrategy: "Contain hallway, extract standing water, set 2 LGR dehumidifiers per IICRC S500.",
    aiCreditLinks: ["IICRC S500 Standard | https://iicrc.org/s500/"],
    proposedWorkEstimateUsd: 1850,
    workAuthStatus: "drafted",
    workAuthTerms: "GENERAL WORK AUTHORIZATION: Extraction & LGR dry-out at Oak Ave.",
    paymentScheduleSummary: "20% Deposit ($370) · 40% Dry-Out Start ($740) · 40% Final Sign-Off ($740)",
    updatedAt: new Date(),
  }));

  // Contractor Bob cannot forge client signature
  await assertFails(bob.collection("consultations").doc("cons_1").update({
    workAuthStatus: "client_authorized",
    clientSignatureName: "Alice Forged",
    clientAuthorizedAtLabel: "2026-11-12",
    updatedAt: new Date(),
  }));

  // Client Alice authorizes the Work Authorization Letter
  await assertSucceeds(alice.collection("consultations").doc("cons_1").update({
    workAuthStatus: "client_authorized",
    clientSignatureName: "Alice Resident",
    clientAuthorizedAtLabel: "2026-11-12 23:25",
    updatedAt: new Date(),
  }));

  await assertSucceeds(alice.collection("consultations").doc("cons_1").update({
    status: "completed",
    clientRating: 5,
    updatedAt: new Date(),
  }));

  await assertFails(bob.collection("consultations").doc("cons_1").update({
    engineerRecommendation: "Changed after completion",
    updatedAt: new Date(),
  }));
});

test("Moderation workflow holds photo/detailed reports in pending_review until approved by peer moderator", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  await assertSucceeds(bob.collection("contractors").doc(BOB_UID).set(validContractorPayload(BOB_UID)));

  const pendingReport = {
    ...validReportPayload(ALICE_UID),
    visibility: "pending_review",
    moderationStatus: "pending_review",
    moderationNotes: "Held for photo & detailed report moderation.",
    plausibilityScore: 86,
  };
  await assertSucceeds(alice.collection("water_reports").doc("rep_mod_1").set(pendingReport));

  // Unrelated Bob cannot see Alice's pending_review report on community query
  const communitySnap = await assertSucceeds(bob.collection("water_reports").where("visibility", "==", "community").get());
  if (communitySnap.docs.some((d) => d.id === "rep_mod_1")) {
    throw new Error("Pending report should not be visible on community query before approval");
  }

  // Alice creates matching moderation ticket
  await assertSucceeds(alice.collection("moderation_queue").doc("rep_mod_1").set({
    userId: ALICE_UID,
    authorName: "Alice R.",
    reportId: "rep_mod_1",
    city: "Sacramento",
    lossCategory: "water_restoration",
    hasMedia: true,
    isDetailedReport: true,
    plausibilityScore: 86,
    completenessIssues: [],
    queueStatus: "pending",
    aiModerationNotes: "Photo & GPS plausible; awaiting moderator check.",
    reviewedByUid: "",
    previewSummary: "2 inches standing water in finished basement.",
    photoBase64: "abc123preview",
    createdAt: now,
    updatedAt: now,
  }));

  // Registered contractor Bob moderates and approves report to public community
  await assertSucceeds(bob.collection("water_reports").doc("rep_mod_1").update({
    visibility: "community",
    moderationStatus: "approved",
    moderationNotes: "Verified by licensed restoration moderator.",
    verifiedByContractorId: BOB_UID,
    engineerVerified: true,
    updatedAt: new Date(),
  }));

  await assertSucceeds(bob.collection("moderation_queue").doc("rep_mod_1").update({
    queueStatus: "approved",
    reviewedByUid: BOB_UID,
    aiModerationNotes: "Approved for public feed.",
    updatedAt: new Date(),
  }));
});

test("water_reports constructionStage enforces valid enum values (in_process, after_remodel, none)", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(alice.collection("water_reports").doc("rep_stage_inproc").set({
    ...validReportPayload(ALICE_UID),
    constructionStage: "in_process",
  }));
  await assertSucceeds(alice.collection("water_reports").doc("rep_stage_after").set({
    ...validReportPayload(ALICE_UID),
    constructionStage: "after_remodel",
  }));
  await assertFails(alice.collection("water_reports").doc("rep_stage_bad").set({
    ...validReportPayload(ALICE_UID),
    constructionStage: "demolished_invalid",
  }));
});

test("supply_orders enforces owner-only isolation, initial submitted status, and cancel transition", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();
  const orderPayload = {
    userId: ALICE_UID,
    customerName: "Alice R.",
    jobDescription: "60ft perimeter French drain + battery sump kit",
    shippingCity: "Sacramento",
    shippingAddress: "100 Main St, Sacramento, CA",
    itemSummaries: ["4-in Perforated French Drain Pipe x1"],
    wholesaleTotalUsd: 185.0,
    retailBenchmarkUsd: 265.0,
    totalSavingsUsd: 80.0,
    platformFeeUsd: 9.25,
    status: "submitted",
    createdAt: now,
    updatedAt: now,
  };

  await assertSucceeds(alice.collection("supply_orders").doc("ord_test_1").set(orderPayload));
  await assertFails(bob.collection("supply_orders").doc("ord_test_1").get());
  await assertFails(bob.collection("supply_orders").where("userId", "==", ALICE_UID).get());
  await assertFails(alice.collection("supply_orders").doc("ord_spoof_status").set({
    ...orderPayload,
    status: "shipped",
  }));
  await assertSucceeds(alice.collection("supply_orders").doc("ord_test_1").update({
    status: "cancelled",
    updatedAt: new Date(),
  }));
  await assertFails(alice.collection("supply_orders").doc("ord_test_1").delete());
});

test("IICRC inspector governance enforces non-self endorsement and blocks non-IICRC users", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const carol = testEnv.authenticatedContext(CAROL_UID).firestore();
  const now = new Date();

  await assertSucceeds(bob.collection("contractors").doc(BOB_UID).set(validContractorPayload(BOB_UID)));
  await assertSucceeds(carol.collection("contractors").doc(CAROL_UID).set({
    ...validContractorPayload(CAROL_UID),
    displayName: "Carol Vance, IICRC Master",
    specialty: "iicrc_inspector",
    iicrcCertNumber: "MASTER-77210",
    isIicrcGovernor: true,
  }));

  await assertSucceeds(bob.collection("iicrc_audits").doc("cons_iicrc_1").set({
    consultationId: "cons_iicrc_1",
    reportId: "rep_1",
    contractorUserId: BOB_UID,
    contractorName: "Bob Vance, WRT",
    city: "Sacramento",
    lossCategory: "water_restoration",
    proposedVerdict: "water_mitigation_approved",
    contractorSuggestion: "Extract standing water, 24-inch drywall flood cut, run 2 LGR dehumidifiers.",
    contractorStrategy: "Contain affected hallway and track daily psychrometric grains/lb.",
    aiCreditLinks: ["IICRC S500 | https://iicrc.org/s500/"],
    proposedWorkEstimateUsd: 1850,
    isPublic: true,
    iicrcStatus: "pending_iicrc",
    iicrcStandardRef: "IICRC S500",
    iicrcInspectorUid: "",
    iicrcInspectorName: "",
    iicrcGovernorNotes: "",
    iicrcSources: [],
    createdAt: now,
    updatedAt: now,
  }));

  // Non-contractor Alice cannot govern IICRC audit
  await assertFails(alice.collection("iicrc_audits").doc("cons_iicrc_1").update({
    iicrcStatus: "endorsed",
    iicrcStandardRef: "IICRC S500",
    iicrcInspectorUid: ALICE_UID,
    iicrcInspectorName: "Alice",
    iicrcGovernorNotes: "Self-approved",
    iicrcSources: [],
    updatedAt: new Date(),
  }));

  // Submitting contractor Bob cannot self-endorse
  await assertFails(bob.collection("iicrc_audits").doc("cons_iicrc_1").update({
    iicrcStatus: "endorsed",
    iicrcStandardRef: "IICRC S500",
    iicrcInspectorUid: BOB_UID,
    iicrcInspectorName: "Bob",
    iicrcGovernorNotes: "Self-approved",
    iicrcSources: [],
    updatedAt: new Date(),
  }));

  // Registered IICRC Inspector Carol CAN endorse and cite current S500 standard
  await assertSucceeds(carol.collection("iicrc_audits").doc("cons_iicrc_1").update({
    iicrcStatus: "endorsed",
    iicrcStandardRef: "IICRC S500 (5th Ed.)",
    iicrcInspectorUid: CAROL_UID,
    iicrcInspectorName: "Carol Vance, IICRC Master",
    iicrcGovernorNotes: "Confirmed accurate Cat-2/3 S500 flood-cut & LGR psychrometric plan.",
    iicrcSources: ["IICRC S500 Water Damage | https://iicrc.org/s500/"],
    updatedAt: new Date(),
  }));
});

test("Client contractor complaints route to SoCal CSLB and IICRC for restoration companies", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const carol = testEnv.authenticatedContext(CAROL_UID).firestore();
  const now = new Date();

  await assertSucceeds(bob.collection("contractors").doc(BOB_UID).set(validContractorPayload(BOB_UID)));
  await assertSucceeds(carol.collection("contractors").doc(CAROL_UID).set({
    ...validContractorPayload(CAROL_UID),
    displayName: "Carol Vance, IICRC Master",
    specialty: "iicrc_inspector",
    iicrcCertNumber: "MASTER-77210",
    isIicrcGovernor: true,
  }));

  await assertSucceeds(alice.collection("contractor_complaints").doc("cmp_1").set({
    userId: ALICE_UID,
    complainantName: "Alice R.",
    contractorUserId: BOB_UID,
    contractorName: "Bob Vance, WRT",
    companyName: "Sierra Water, Fire & Mold Mitigation",
    licenseNumber: "CSLB-901122",
    iicrcCertNumber: "WRT-88412",
    isRestorationCompany: true,
    city: "Los Angeles",
    complaintCategory: "iicrc_violation",
    summary: "Removed wet drywall without containment or negative air scrubber during Cat-3 sewage dry-out.",
    evidenceNotes: "Attached meter photos and dry-out log",
    proofPhotoBase64: "",
    routedToSocalCslb: true,
    routedToIicrc: true,
    cslbIntakeUrl: "https://www.cslb.ca.gov/Consumers/Filing_A_Complaint/",
    iicrcIntakeUrl: "https://iicrc.org/consumers/",
    status: "filed",
    isPublic: true,
    createdAt: now,
    updatedAt: now,
  }));

  // Contractor Bob cannot delete Alice's complaint
  await assertFails(bob.collection("contractor_complaints").doc("cmp_1").delete());

  // IICRC Inspector Carol can review & mark restoration complaint iicrc_reviewing
  await assertSucceeds(carol.collection("contractor_complaints").doc("cmp_1").update({
    status: "iicrc_reviewing",
    evidenceNotes: "Escalated to IICRC S500 compliance committee.",
    updatedAt: new Date(),
  }));
});

test("Insurance carriers register profiles and streamline shared claims with client and contractor", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const carol = testEnv.authenticatedContext(CAROL_UID).firestore();
  const now = new Date();

  await assertSucceeds(bob.collection("contractors").doc(BOB_UID).set(validContractorPayload(BOB_UID)));

  // Carol registers as an Insurance Company Claims Adjuster
  await assertSucceeds(carol.collection("insurance_carriers").doc(CAROL_UID).set({
    userId: CAROL_UID,
    carrierName: "Pacific Shield Homeowners Insurance",
    adjusterName: "Carol M. (Senior Property Adjuster)",
    licenseOrNaicCode: "NAIC-44910",
    claimsEmail: "claims@pacificshield.example.com",
    claimsPhone: "800-555-0199",
    claimsPortalUrl: "https://pacificshield.example.com/claims",
    serviceRegions: ["Los Angeles", "San Diego", "Orange County"],
    supportsDirectRestorationBilling: true,
    avgTurnaroundHours: 12,
    bio: "Fast-track direct restoration billing for IICRC-governed water & mold losses.",
    isActive: true,
    createdAt: now,
    updatedAt: now,
  }));

  // Client Alice links her insurance & opens a shared claim thread with Contractor Bob + Carrier Carol
  await assertSucceeds(alice.collection("insurance_claims").doc("clm_1").set({
    clientUserId: ALICE_UID,
    clientName: "Alice R.",
    contractorUserId: BOB_UID,
    contractorName: "Bob Vance, WRT",
    insurerUserId: CAROL_UID,
    insurerName: "Pacific Shield Homeowners Insurance",
    claimNumber: "PS-2026-8841",
    policyType: "homeowners_ho3",
    deductibleUsd: 1000,
    reportId: "rep_1",
    consultationId: "cons_1",
    city: "Los Angeles",
    lossCategory: "water_restoration",
    contractorSuggestion: "",
    estimatedScopeUsd: 0,
    approvedCoverageUsd: 0,
    claimStatus: "linked",
    lastMessageFromRole: "client",
    communicationLog: ["Client linked insurance (Pacific Shield, Claim #PS-2026-8841)"],
    createdAt: now,
    updatedAt: now,
  }));

  // Contractor Bob pushes S500 dry-out scope & estimate straight to Insurer Carol
  await assertSucceeds(bob.collection("insurance_claims").doc("clm_1").update({
    contractorSuggestion: "IICRC S500 Class 2 dry-out: 3 LGR dehumidifiers + 24-in flood cut.",
    estimatedScopeUsd: 3400,
    claimStatus: "contractor_scoped",
    lastMessageFromRole: "contractor",
    communicationLog: [
      "Client linked insurance (Pacific Shield, Claim #PS-2026-8841)",
      "Contractor: Uploaded S500 scope ($3400) for adjuster review."
    ],
    updatedAt: new Date(),
  }));

  // Contractor Bob CANNOT forge insurer approval amount
  await assertFails(bob.collection("insurance_claims").doc("clm_1").update({
    approvedCoverageUsd: 3400,
    claimStatus: "approved_direct_work",
    lastMessageFromRole: "contractor",
    communicationLog: ["Fake approval"],
    updatedAt: new Date(),
  }));

  // Insurance Adjuster Carol approves direct work & coverage
  await assertSucceeds(carol.collection("insurance_claims").doc("clm_1").update({
    approvedCoverageUsd: 3400,
    claimStatus: "approved_direct_work",
    lastMessageFromRole: "insurer",
    communicationLog: [
      "Client linked insurance (Pacific Shield, Claim #PS-2026-8841)",
      "Contractor: Uploaded S500 scope ($3400) for adjuster review.",
      "Insurer: Approved $3400 direct restoration scope."
    ],
    updatedAt: new Date(),
  }));
});



