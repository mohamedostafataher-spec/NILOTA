const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_mohamed";
const ADMIN_EMAIL = "mohamedostafataher@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user: denied all reads and writes", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("destinations").where("verificationStatus", "==", "APPROVED").get());
  await assertFails(unauth.collection("bookings").where("travelerId", "==", ALICE_UID).get());
  await assertFails(unauth.collection("conversation_rooms").doc("NLR100").get());
});

test("User profile & PII isolation: owner can create/update own profile; cross-user denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Traveler",
      accountMode: "TRAVELER",
      preferredLanguage: "en",
      country: "Egypt",
      privacyConsent: true,
      retainTranslationHistory: false,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Ghost field rejection (Shadow Update Test)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob cannot read or modify Alice's profile or private PII
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("private").doc("info").get());
});

test("My Journey: user favorites and travel notes enforce owner isolation and createdAt immutability", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("favorites").doc("fav_1").set({
      userId: ALICE_UID,
      targetId: "dest_giza",
      targetType: "DESTINATION",
      title: "Giza Plateau",
      cityName: "Giza",
      category: "Historical Attractions",
      createdAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("travel_notes").doc("note_1").set({
      userId: ALICE_UID,
      title: "Flight & Museum",
      content: "Verify opening times before visiting.",
      cityName: "Cairo",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("travel_notes").doc("note_1").update({
      content: "Updated verified notes.",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // CreatedAt immutability check
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("travel_notes").doc("note_1").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob cannot list Alice's notes
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("travel_notes").get());
});

test("Provider verification integrity: applicant cannot self-approve; verified admin can approve", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  const past = new Date(Date.now() - 5000);

  // Cannot self-create as APPROVED
  await assertFails(
    aliceDb.collection("providers").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Guide",
      serviceType: "TOUR_GUIDE",
      languages: ["en", "ar"],
      citiesCovered: ["Cairo", "Giza"],
      bio: "Licensed Egyptology guide.",
      documentSummary: "Ministry Tourism License #123",
      verificationStatus: "APPROVED",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Must create as PENDING
  await assertSucceeds(
    aliceDb.collection("providers").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Guide",
      serviceType: "TOUR_GUIDE",
      languages: ["en", "ar"],
      citiesCovered: ["Cairo", "Giza"],
      bio: "Licensed Egyptology guide.",
      documentSummary: "Ministry Tourism License #123",
      verificationStatus: "PENDING",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Unverified email spoof cannot approve
  await assertFails(
    spoofedAdminDb.collection("providers").doc(ALICE_UID).update({
      verificationStatus: "APPROVED",
      reviewedBy: "spoof_uid",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Verified admin approves
  await assertSucceeds(
    adminDb.collection("providers").doc(ALICE_UID).update({
      verificationStatus: "APPROVED",
      reviewedBy: ADMIN_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Booking integrity: traveler cannot self-confirm; provider can confirm; terminal state locked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  // Traveler cannot create booking already CONFIRMED
  await assertFails(
    aliceDb.collection("bookings").doc("bk_fake").set({
      bookingReference: "NLR-BK-001",
      travelerId: ALICE_UID,
      travelerName: "Alice",
      providerId: BOB_UID,
      providerName: "Bob Tours",
      activityId: "act_1",
      activityTitle: "Red Sea Boat",
      requestedDate: "2026-12-01",
      participantsCount: 2,
      status: "CONFIRMED",
      paymentStatus: "NOT_CONFIGURED",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Valid booking request submission
  await assertSucceeds(
    aliceDb.collection("bookings").doc("bk_1").set({
      bookingReference: "NLR-BK-001",
      travelerId: ALICE_UID,
      travelerName: "Alice",
      providerId: BOB_UID,
      providerName: "Bob Tours",
      activityId: "act_1",
      activityTitle: "Red Sea Boat",
      requestedDate: "2026-12-01",
      participantsCount: 2,
      status: "REQUEST_SUBMITTED",
      paymentStatus: "NOT_CONFIGURED",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Traveler cannot mark own booking CONFIRMED
  await assertFails(
    aliceDb.collection("bookings").doc("bk_1").update({
      status: "CONFIRMED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Provider confirms booking
  await assertSucceeds(
    bobDb.collection("bookings").doc("bk_1").update({
      status: "CONFIRMED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Traveler cancels, locking terminal state
  await assertSucceeds(
    aliceDb.collection("bookings").doc("bk_1").update({
      status: "CANCELLED",
      cancellationReason: "Schedule changed",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Non-admin cannot re-open cancelled booking
  await assertFails(
    bobDb.collection("bookings").doc("bk_1").update({
      status: "CONFIRMED",
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("Two-phone translation room: host creates, guest joins via roomCode, messages isolated to members", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext("charlie_789").firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("conversation_rooms").doc("NLR48291").set({
      roomCode: "NLR48291",
      hostId: ALICE_UID,
      hostName: "Alice",
      hostLanguage: "en",
      memberUids: [ALICE_UID],
      status: "WAITING",
      retainTranscript: false,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Bob reads waiting room by code & joins
  await assertSucceeds(bobDb.collection("conversation_rooms").doc("NLR48291").get());
  await assertSucceeds(
    bobDb.collection("conversation_rooms").doc("NLR48291").update({
      guestId: BOB_UID,
      guestName: "Bob",
      guestLanguage: "ar",
      memberUids: [ALICE_UID, BOB_UID],
      status: "ACTIVE",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Outsider Charlie cannot read active room or send messages
  await assertFails(charlieDb.collection("conversation_rooms").doc("NLR48291").get());

  // Bob sends translated message
  await assertSucceeds(
    bobDb.collection("conversation_rooms").doc("NLR48291").collection("messages").doc("msg_1").set({
      roomCode: "NLR48291",
      memberUids: [ALICE_UID, BOB_UID],
      senderId: BOB_UID,
      senderName: "Bob",
      sourceLanguage: "ar",
      targetLanguage: "en",
      originalText: "أهلاً بك في مصر",
      translatedText: "Welcome to Egypt",
      createdAt: new Date(Date.now() - 500),
    })
  );

  // Unfiltered query on rooms rejected; memberUids query succeeds
  await assertFails(aliceDb.collection("conversation_rooms").get());
  await assertSucceeds(
    aliceDb.collection("conversation_rooms").where("memberUids", "array-contains", ALICE_UID).get()
  );
});

test("Destinations query alignment: approved query succeeds; unfiltered query fails for non-admin", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("destinations").where("verificationStatus", "==", "APPROVED").get()
  );
  await assertFails(aliceDb.collection("destinations").get());
});

test("Public profiles visibility & blocking isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext("charlie_789").firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("public_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      country: "Egypt",
      city: "Cairo",
      nativeLanguage: "ar-EG",
      spokenLanguages: ["ar-EG", "en"],
      learningLanguages: ["ru"],
      interests: ["Culture", "History"],
      bio: "Egyptian host & language partner",
      profileVisibility: "PUBLIC",
      openToLanguageExchange: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    bobDb.collection("public_profiles").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob Private",
      country: "Russia",
      nativeLanguage: "ru",
      spokenLanguages: ["ru", "en"],
      learningLanguages: ["ar-EG"],
      interests: ["Travel"],
      bio: "Learning Egyptian Arabic",
      profileVisibility: "PRIVATE",
      openToLanguageExchange: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Public query succeeds; unfiltered fails; PRIVATE cannot be read by others
  await assertSucceeds(
    bobDb.collection("public_profiles").where("profileVisibility", "==", "PUBLIC").get()
  );
  await assertFails(bobDb.collection("public_profiles").get());
  await assertFails(aliceDb.collection("public_profiles").doc(BOB_UID).get());
  await assertSucceeds(aliceDb.collection("public_profiles").doc(ALICE_UID).get());

  // Alice blocks Charlie -> Charlie cannot read Alice's public profile
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("blocked").doc("charlie_789").set({
      blockerId: ALICE_UID,
      blockedUserId: "charlie_789",
      blockedUserName: "Charlie",
      reason: "Spam",
      createdAt: past,
    })
  );
  await assertFails(charlieDb.collection("public_profiles").doc(ALICE_UID).get());
});

test("Connection requests & 1:1 direct messaging enforce pair rules, block prevention, immutable originalText, and recipient-only read receipts", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext("charlie_789").firestore();
  const past = new Date(Date.now() - 5000);
  const convId = ALICE_UID < BOB_UID ? `${ALICE_UID}__${BOB_UID}` : `${BOB_UID}__${ALICE_UID}`;

  // Connection request: Alice -> Bob
  await assertSucceeds(
    aliceDb.collection("connection_requests").doc(convId).set({
      senderId: ALICE_UID,
      senderName: "Alice",
      senderCountry: "Egypt",
      senderNativeLanguage: "ar-EG",
      receiverId: BOB_UID,
      receiverName: "Bob",
      receiverCountry: "France",
      receiverNativeLanguage: "fr",
      memberUids: [ALICE_UID, BOB_UID],
      introMessage: "Let's practice Arabic and French!",
      status: "PENDING",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Sender cannot self-accept; receiver can accept
  await assertFails(
    aliceDb.collection("connection_requests").doc(convId).update({
      status: "ACCEPTED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    bobDb.collection("connection_requests").doc(convId).update({
      status: "ACCEPTED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Create direct conversation using canonical pair ID
  await assertSucceeds(
    aliceDb.collection("direct_conversations").doc(convId).set({
      participantIds: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      participantLanguages: { [ALICE_UID]: "ar-EG", [BOB_UID]: "fr" },
      lastMessagePreview: "",
      lastMessageSenderId: "",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Charlie cannot read conversation or messages
  await assertFails(charlieDb.collection("direct_conversations").doc(convId).get());

  // Alice sends a message
  await assertSucceeds(
    aliceDb.collection("direct_conversations").doc(convId).collection("messages").doc("m1").set({
      conversationId: convId,
      participantIds: [ALICE_UID, BOB_UID],
      senderId: ALICE_UID,
      senderName: "Alice",
      messageType: "TEXT",
      originalText: "ازيك يا بوب؟ عامل ايه؟",
      sourceLanguage: "ar-EG",
      translations: { fr: "Comment vas-tu Bob ?" },
      translationConfidence: "MEDIUM",
      translationNote: "Egyptian colloquial Arabic",
      audioBase64: "",
      audioDurationMs: 0,
      audioMimeType: "",
      deliveryStatus: "SENT",
      createdAt: past,
    })
  );

  // Sender Alice CANNOT mark her own message READ; recipient Bob CAN
  await assertFails(
    aliceDb.collection("direct_conversations").doc(convId).collection("messages").doc("m1").update({
      deliveryStatus: "READ",
    })
  );
  await assertSucceeds(
    bobDb.collection("direct_conversations").doc(convId).collection("messages").doc("m1").update({
      deliveryStatus: "READ",
    })
  );

  // originalText is IMMUTABLE even during translation caching
  await assertFails(
    bobDb.collection("direct_conversations").doc(convId).collection("messages").doc("m1").update({
      originalText: "Modified",
      translations: { fr: "Salut", en: "Modified" },
    })
  );
  await assertSucceeds(
    bobDb.collection("direct_conversations").doc(convId).collection("messages").doc("m1").update({
      translations: { fr: "Comment vas-tu Bob ?", en: "How are you Bob?" },
    })
  );

  // If Bob blocks Alice, Alice can no longer send messages
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("blocked").doc(ALICE_UID).set({
      blockerId: BOB_UID,
      blockedUserId: ALICE_UID,
      blockedUserName: "Alice",
      reason: "Muted",
      createdAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("direct_conversations").doc(convId).collection("messages").doc("m2").set({
      conversationId: convId,
      participantIds: [ALICE_UID, BOB_UID],
      senderId: ALICE_UID,
      senderName: "Alice",
      messageType: "TEXT",
      originalText: "Are you there?",
      sourceLanguage: "en",
      translations: {},
      translationConfidence: "LOW",
      translationNote: "",
      audioBase64: "",
      audioDurationMs: 0,
      audioMimeType: "",
      deliveryStatus: "SENT",
      createdAt: new Date(Date.now() - 100),
    })
  );
});

test("Multilingual groups: open join, approval requests, owner/moderator controls, ban enforcement, and reports", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext("charlie_789").firestore();
  const past = new Date(Date.now() - 5000);

  // Alice creates OPEN group
  await assertSucceeds(
    aliceDb.collection("groups").doc("grp_egypt_lang").set({
      name: "Cairo Language Cafe",
      description: "Daily Egyptian Arabic, Russian, French, and English practice",
      ownerId: ALICE_UID,
      moderatorIds: [ALICE_UID],
      memberIds: [ALICE_UID],
      bannedIds: [],
      primaryLanguages: ["ar-EG", "en", "ru", "fr"],
      joinPolicy: "OPEN",
      autoTranslateDefault: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Non-member Bob cannot post messages yet
  await assertFails(
    bobDb.collection("groups").doc("grp_egypt_lang").collection("messages").doc("gm1").set({
      groupId: "grp_egypt_lang",
      senderId: BOB_UID,
      senderName: "Bob",
      originalText: "Bonjour!",
      sourceLanguage: "fr",
      translations: {},
      translationNote: "",
      createdAt: past,
    })
  );

  // Bob joins OPEN group cleanly (only adding himself)
  await assertSucceeds(
    bobDb.collection("groups").doc("grp_egypt_lang").update({
      memberIds: [ALICE_UID, BOB_UID],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Now Bob can send a message
  await assertSucceeds(
    bobDb.collection("groups").doc("grp_egypt_lang").collection("messages").doc("gm1").set({
      groupId: "grp_egypt_lang",
      senderId: BOB_UID,
      senderName: "Bob",
      originalText: "Bonjour tout le monde!",
      sourceLanguage: "fr",
      translations: { "ar-EG": "أهلاً بالكل!" },
      translationNote: "Egyptian Arabic preview",
      createdAt: new Date(Date.now() - 900),
    })
  );

  // Owner Alice bans Charlie and removes Bob
  await assertSucceeds(
    aliceDb.collection("groups").doc("grp_egypt_lang").update({
      memberIds: [ALICE_UID],
      bannedIds: ["charlie_789"],
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Banned Charlie cannot read or join the group
  await assertFails(charlieDb.collection("groups").doc("grp_egypt_lang").get());
  await assertFails(
    charlieDb.collection("groups").doc("grp_egypt_lang").update({
      memberIds: [ALICE_UID, "charlie_789"],
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Group message report succeeds
  await assertSucceeds(
    bobDb.collection("reports").doc("rep_phase2").set({
      reporterId: BOB_UID,
      targetType: "GROUP_MESSAGE",
      targetId: "grp_egypt_lang/gm1",
      reason: "Harassment",
      details: "Abusive remark in room.",
      status: "OPEN",
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("World Postcards & multi-country contributions preserve source text, honor blocks, and update chain", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext("charlie_789").firestore();
  const past = new Date(Date.now() - 5000);

  // Alice creates a World Postcard from Egypt
  await assertSucceeds(
    aliceDb.collection("world_postcards").doc("pc_breakfast").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      country: "Egypt",
      city: "Cairo",
      languageCode: "ar-EG",
      category: "FOOD",
      title: "Morning Ful & Taameya",
      storyText: "What does morning breakfast feel like where you live?",
      localExpression: "صباح الفل",
      questionPrompt: "Share your morning ritual!",
      photoBase64: "",
      audioBase64: "",
      audioDurationMs: 0,
      translations: {},
      translationNote: "",
      participantIds: [ALICE_UID],
      countriesChain: ["Egypt"],
      contributionsCount: 0,
      allowReplies: true,
      allowPrivateConnect: true,
      visibility: "PUBLIC",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Bob from Japan contributes a response and extends countriesChain
  await assertSucceeds(
    bobDb.collection("world_postcards").doc("pc_breakfast").collection("contributions").doc("c_jp").set({
      postcardId: "pc_breakfast",
      authorId: BOB_UID,
      authorName: "Kenji",
      country: "Japan",
      city: "Kyoto",
      languageCode: "ja",
      responseText: "Grilled fish, miso soup, and steamed rice!",
      localExpression: "おはようございます",
      photoBase64: "",
      audioBase64: "",
      audioDurationMs: 0,
      translations: {},
      translationNote: "",
      allowPrivateConnect: true,
      createdAt: new Date(Date.now() - 2000),
    })
  );

  await assertSucceeds(
    bobDb.collection("world_postcards").doc("pc_breakfast").update({
      participantIds: [ALICE_UID, BOB_UID],
      countriesChain: ["Egypt", "Japan"],
      contributionsCount: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Contribution text is immutable even if translations cache updates
  await assertFails(
    bobDb.collection("world_postcards").doc("pc_breakfast").collection("contributions").doc("c_jp").update({
      responseText: "Tampered text",
    })
  );

  // If Alice blocks Charlie, Charlie cannot read or contribute
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("blocked").doc("charlie_789").set({
      blockerId: ALICE_UID,
      blockedUserId: "charlie_789",
      blockedUserName: "Charlie",
      reason: "Block test",
      createdAt: past,
    })
  );
  await assertFails(charlieDb.collection("world_postcards").doc("pc_breakfast").get());
  await assertFails(
    charlieDb.collection("world_postcards").doc("pc_breakfast").collection("contributions").doc("c_blocked").set({
      postcardId: "pc_breakfast",
      authorId: "charlie_789",
      authorName: "Charlie",
      country: "Brazil",
      city: "",
      languageCode: "pt",
      responseText: "Should fail",
      localExpression: "",
      photoBase64: "",
      audioBase64: "",
      audioDurationMs: 0,
      translations: {},
      translationNote: "",
      allowPrivateConnect: true,
      createdAt: new Date(Date.now() - 500),
    })
  );
});

test("Culture Swap & World Quest enforce real objective completion and disallow fake progress", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  // Culture Swap
  await assertSucceeds(
    aliceDb.collection("culture_swaps").doc("swap_1").set({
      creatorId: ALICE_UID,
      creatorName: "Alice",
      creatorCountry: "Egypt",
      creatorLanguage: "ar-EG",
      promptCategory: "TEACH_PHRASE",
      promptText: "Teach me one warm everyday phrase from your home.",
      participationMode: "TWO_PERSON",
      maxParticipants: 2,
      participantIds: [ALICE_UID],
      participantNames: { [ALICE_UID]: "Alice" },
      participantCountries: { [ALICE_UID]: "Egypt" },
      entriesCount: 1,
      status: "OPEN",
      allowPrivateConnect: true,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    bobDb.collection("culture_swaps").doc("swap_1").update({
      participantIds: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      participantCountries: { [ALICE_UID]: "Egypt", [BOB_UID]: "France" },
      entriesCount: 2,
      status: "EXCHANGED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // World Quest with targetGoal = 2
  await assertSucceeds(
    aliceDb.collection("world_quests").doc("q_expressions").set({
      creatorId: ALICE_UID,
      creatorName: "Alice",
      missionType: "THREE_EXPRESSIONS",
      title: "Exchange Everyday Greetings",
      objective: "Share two authentic local greetings across countries.",
      targetGoal: 2,
      participantIds: [ALICE_UID],
      participantCountries: ["Egypt"],
      contributionsCount: 0,
      status: "ACTIVE",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Fake progress: cannot mark COMPLETED when contributionsCount < targetGoal (0 < 2)
  await assertFails(
    aliceDb.collection("world_quests").doc("q_expressions").update({
      status: "COMPLETED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Genuine sequential contributions reach targetGoal -> COMPLETED succeeds
  await assertSucceeds(
    aliceDb.collection("world_quests").doc("q_expressions").update({
      contributionsCount: 1,
      updatedAt: new Date(Date.now() - 400),
    })
  );
  await assertSucceeds(
    bobDb.collection("world_quests").doc("q_expressions").update({
      participantIds: [ALICE_UID, BOB_UID],
      participantCountries: ["Egypt", "France"],
      contributionsCount: 2,
      status: "COMPLETED",
      updatedAt: new Date(Date.now() - 300),
    })
  );
});

test("NILORA Memory enforces private/shared/public visibility and audio retention consent", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext("charlie_789").firestore();
  const past = new Date(Date.now() - 5000);

  // Cannot save raw audio if retainAudioConsent is false
  await assertFails(
    aliceDb.collection("nilora_memories").doc("mem_bad_audio").set({
      ownerId: ALICE_UID,
      ownerName: "Alice",
      memoryType: "VOICE_MEMORY",
      sourceId: "",
      title: "Voice greeting",
      summaryText: "A short memory.",
      originalLanguage: "ar-EG",
      translations: {},
      countries: ["Egypt"],
      partnerIds: [],
      partnerNames: [],
      placeLabel: "Cairo",
      photoBase64: "",
      audioBase64: "AAAA",
      audioDurationMs: 2000,
      retainAudioConsent: false,
      visibility: "PRIVATE",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Shared memory visible ONLY to Alice and partner Bob, NOT Charlie
  await assertSucceeds(
    aliceDb.collection("nilora_memories").doc("mem_shared").set({
      ownerId: ALICE_UID,
      ownerName: "Alice",
      memoryType: "CULTURE_SWAP",
      sourceId: "swap_1",
      title: "Egyptian & French Phrases",
      summaryText: "We exchanged morning greetings.",
      originalLanguage: "ar-EG",
      translations: {},
      countries: ["Egypt", "France"],
      partnerIds: [BOB_UID],
      partnerNames: ["Bob"],
      placeLabel: "",
      photoBase64: "",
      audioBase64: "",
      audioDurationMs: 0,
      retainAudioConsent: false,
      visibility: "SHARED_WITH_PARTICIPANTS",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(bobDb.collection("nilora_memories").doc("mem_shared").get());
  await assertFails(charlieDb.collection("nilora_memories").doc("mem_shared").get());
});

test("Live Adventure Sessions enforce participant membership for presence, chat, and joining", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  // Alice opens an open JOIN_OTHERS Red Sea adventure session
  await assertSucceeds(
    aliceDb.collection("adventure_sessions").doc("sess_sharm_1").set({
      hostId: ALICE_UID,
      hostName: "Alice",
      adventureId: "sharm_reef_dive",
      adventureTitle: "Red Sea — Reef Exploration",
      destinationName: "Sharm El Sheikh",
      mode: "JOIN_OTHERS",
      status: "ACTIVE",
      maxParticipants: 6,
      participantIds: [ALICE_UID],
      participantNames: { [ALICE_UID]: "Alice" },
      participantCountries: { [ALICE_UID]: "Egypt" },
      participantLanguages: { [ALICE_UID]: "ar-EG" },
      voicePreferred: true,
      experienceLevel: "INTERMEDIATE",
      durationMinutes: 30,
      availabilitySlot: "Now",
      crewId: "",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Non-participant Bob cannot write presence or chat before joining
  await assertFails(
    bobDb.collection("adventure_sessions").doc("sess_sharm_1").collection("participants").doc(BOB_UID).set({
      sessionId: "sess_sharm_1",
      userId: BOB_UID,
      displayName: "Bob",
      country: "Russia",
      languageCode: "ru",
      posX: 0,
      posY: -4,
      posZ: 2,
      yawDeg: 0,
      pointedPoiId: "",
      pointedX: 0,
      pointedY: 0,
      pointedZ: 0,
      voiceMuted: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob joins the session (adding only self)
  await assertSucceeds(
    bobDb.collection("adventure_sessions").doc("sess_sharm_1").update({
      participantIds: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      participantCountries: { [ALICE_UID]: "Egypt", [BOB_UID]: "Russia" },
      participantLanguages: { [ALICE_UID]: "ar-EG", [BOB_UID]: "ru" },
      updatedAt: new Date(Date.now() - 800),
    })
  );

  // Now Bob can broadcast 3D spatial presence & send translated adventure chat
  await assertSucceeds(
    bobDb.collection("adventure_sessions").doc("sess_sharm_1").collection("participants").doc(BOB_UID).set({
      sessionId: "sess_sharm_1",
      userId: BOB_UID,
      displayName: "Bob",
      country: "Russia",
      languageCode: "ru",
      posX: 1.5,
      posY: -5.0,
      posZ: -3.2,
      yawDeg: 45,
      pointedPoiId: "poi_shark_reef",
      pointedX: 4.0,
      pointedY: -6.0,
      pointedZ: -12.0,
      voiceMuted: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(
    bobDb.collection("adventure_sessions").doc("sess_sharm_1").collection("messages").doc("msg_1").set({
      sessionId: "sess_sharm_1",
      senderId: BOB_UID,
      senderName: "Bob",
      originalText: "Посмотри на коралловый риф здесь!",
      sourceLanguage: "ru",
      translations: { "ar-EG": "بص على الشعاب المرجانية هنا!" },
      translationNote: "NILORA LIVE AI",
      discoveryPoiId: "poi_shark_reef",
      createdAt: new Date(Date.now() - 400),
    })
  );
});

test("Adventure Matchmaking & NILORA Crews enforce owner isolation and crew membership", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext("charlie_789").firestore();
  const past = new Date(Date.now() - 4000);

  // Bob cannot write Alice's matchmaking profile
  await assertFails(
    bobDb.collection("adventure_match_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      country: "Egypt",
      targetAdventureId: "sharm_reef_dive",
      targetDestination: "Sharm El Sheikh",
      interests: ["Scuba diving"],
      preferredLanguage: "ar-EG",
      spokenLanguages: ["ar-EG", "en"],
      experienceLevel: "INTERMEDIATE",
      preferredDurationMinutes: 30,
      availableTimeSlot: "Evenings UTC+2",
      voicePreferred: true,
      isAvailable: true,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("adventure_match_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      country: "Egypt",
      targetAdventureId: "sharm_reef_dive",
      targetDestination: "Sharm El Sheikh",
      interests: ["Scuba diving", "Coral reefs"],
      preferredLanguage: "ar-EG",
      spokenLanguages: ["ar-EG", "en"],
      experienceLevel: "INTERMEDIATE",
      preferredDurationMinutes: 30,
      availableTimeSlot: "Evenings UTC+2",
      voicePreferred: true,
      isAvailable: true,
      updatedAt: past,
    })
  );

  // Alice creates a persistent NILORA Crew
  await assertSucceeds(
    aliceDb.collection("nilora_crews").doc("crew_red_sea").set({
      name: "Red Sea Coral Explorers",
      description: "Diving and reef conservation crew.",
      leaderId: ALICE_UID,
      memberIds: [ALICE_UID],
      memberNames: { [ALICE_UID]: "Alice" },
      memberCountries: { [ALICE_UID]: "Egypt" },
      sharedInterests: ["Coral reefs", "Egypt"],
      focusDestination: "Sharm El Sheikh",
      plannedAdventureIds: ["sharm_reef_dive"],
      adventureHistoryTitles: [],
      joinPolicy: "OPEN",
      voiceChannelEnabled: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Non-member Charlie cannot read or write crew messages
  await assertFails(
    charlieDb.collection("nilora_crews").doc("crew_red_sea").collection("messages").doc("c_msg_1").set({
      crewId: "crew_red_sea",
      senderId: "charlie_789",
      senderName: "Charlie",
      originalText: "Hello!",
      sourceLanguage: "en",
      translations: {},
      createdAt: past,
    })
  );

  // Bob joins OPEN crew, then sends crew message
  await assertSucceeds(
    bobDb.collection("nilora_crews").doc("crew_red_sea").update({
      memberIds: [ALICE_UID, BOB_UID],
      memberNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      memberCountries: { [ALICE_UID]: "Egypt", [BOB_UID]: "Russia" },
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(
    bobDb.collection("nilora_crews").doc("crew_red_sea").collection("messages").doc("c_msg_1").set({
      crewId: "crew_red_sea",
      senderId: BOB_UID,
      senderName: "Bob",
      originalText: "Ready for Ras Mohamed tomorrow!",
      sourceLanguage: "en",
      translations: { "ar-EG": "جاهز لراس محمد بكرة!" },
      createdAt: new Date(Date.now() - 300),
    })
  );
});

test("Reality Events enforce non-admin verification restriction, approval join, and participant chat", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv.authenticatedContext("admin_uid", {
    email: "mohamedostafataher@gmail.com",
    email_verified: true,
  }).firestore();
  const past = new Date(Date.now() - 4000);

  // Non-admin cannot self-publish ADMIN_VERIFIED event
  await assertFails(
    aliceDb.collection("reality_events").doc("evt_fake_verified").set({
      creatorId: ALICE_UID,
      creatorName: "Alice",
      title: "Cairo Book Fair Meetup",
      category: "CULTURAL",
      description: "Meetup at the cultural exhibition halls.",
      countryCode: "EG",
      countryName: "Egypt",
      cityName: "Cairo",
      venueName: "Egypt International Exhibition Center",
      hasVerifiedCoordinates: false,
      startDateIso: "2026-12-10",
      endDateIso: "2026-12-10",
      timeWindowLabel: "14:00 - 17:00",
      freeVerified: false,
      priceNote: "Check official site",
      organizerName: "Alice",
      organizerType: "USER_ORGANIZER",
      officialUrl: "",
      conditionsNote: "Public meetup",
      safetyNote: "Stay inside exhibition halls",
      ageMinimum: 16,
      verificationTier: "ADMIN_VERIFIED",
      lastVerifiedLabel: "",
      status: "UPCOMING",
      maxParticipants: 20,
      participantIds: [ALICE_UID],
      participantNames: { [ALICE_UID]: "Alice" },
      pendingRequestIds: [],
      joinPolicy: "OPEN",
      linkedAdventureId: "cairo_historic_citadel",
      createdAt: past,
      updatedAt: past,
    })
  );

  // User creates UNVERIFIED_USER_SUBMITTED event cleanly
  await assertSucceeds(
    aliceDb.collection("reality_events").doc("evt_cairo_1").set({
      creatorId: ALICE_UID,
      creatorName: "Alice",
      title: "Cairo Cultural Walk & Museum Meetup",
      category: "CULTURAL",
      description: "Public cultural walk starting at Tahrir Square.",
      countryCode: "EG",
      countryName: "Egypt",
      cityName: "Cairo",
      venueName: "Tahrir Square Main Gate",
      hasVerifiedCoordinates: true,
      latitude: 30.0478,
      longitude: 31.2336,
      startDateIso: "2026-12-10",
      endDateIso: "2026-12-10",
      timeWindowLabel: "15:00 - 17:30",
      freeVerified: true,
      priceNote: "Free public walk",
      organizerName: "Alice",
      organizerType: "USER_ORGANIZER",
      officialUrl: "",
      conditionsNote: "Bring walking shoes",
      safetyNote: "Public space meetup only",
      ageMinimum: 18,
      verificationTier: "UNVERIFIED_USER_SUBMITTED",
      lastVerifiedLabel: "",
      status: "UPCOMING",
      maxParticipants: 15,
      participantIds: [ALICE_UID],
      participantNames: { [ALICE_UID]: "Alice" },
      pendingRequestIds: [],
      joinPolicy: "OPEN",
      linkedAdventureId: "cairo_historic_citadel",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Bob cannot promote event to ADMIN_VERIFIED
  await assertFails(
    bobDb.collection("reality_events").doc("evt_cairo_1").update({
      verificationTier: "ADMIN_VERIFIED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Admin promotes event to ADMIN_VERIFIED
  await assertSucceeds(
    adminDb.collection("reality_events").doc("evt_cairo_1").update({
      verificationTier: "ADMIN_VERIFIED",
      lastVerifiedLabel: "Verified by NILORA Admin",
      updatedAt: new Date(Date.now() - 900),
    })
  );

  // Non-participant Bob cannot post event chat message yet
  await assertFails(
    bobDb.collection("reality_events").doc("evt_cairo_1").collection("messages").doc("em_1").set({
      eventId: "evt_cairo_1",
      senderId: BOB_UID,
      senderName: "Bob",
      originalText: "Can I join?",
      sourceLanguage: "en",
      translations: {},
      createdAt: new Date(Date.now() - 700),
    })
  );

  // Bob joins OPEN event, then posts chat message
  await assertSucceeds(
    bobDb.collection("reality_events").doc("evt_cairo_1").update({
      participantIds: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      updatedAt: new Date(Date.now() - 600),
    })
  );
  await assertSucceeds(
    bobDb.collection("reality_events").doc("evt_cairo_1").collection("messages").doc("em_1").set({
      eventId: "evt_cairo_1",
      senderId: BOB_UID,
      senderName: "Bob",
      originalText: "See everyone at the gate!",
      sourceLanguage: "en",
      translations: { "ar-EG": "أشوفكم عند البوابة!" },
      createdAt: new Date(Date.now() - 500),
    })
  );
});

test("Go/Play/Team-Up & Community Impact enforce block checks, non-admin verification lock, and member-only chat", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 3500);

  // Non-admin cannot mark impactVerifiedByAdmin = true
  await assertFails(
    aliceDb.collection("reality_activities").doc("act_cleanup_bad").set({
      creatorId: ALICE_UID,
      creatorName: "Alice",
      domain: "COMMUNITY_IMPACT",
      subCategory: "BEACH_CLEANUP",
      title: "Hurghada Coast Cleanup",
      description: "Public shoreline litter pickup.",
      countryName: "Egypt",
      cityName: "Hurghada",
      publicMeetingSpot: "Public Marina Promenade",
      scheduledDateLabel: "Saturday 09:00",
      skillLevel: "ALL_LEVELS",
      equipmentNote: "Gloves & bags",
      safetyRules: "Stay in daylight supervised coastal zones.",
      ageMinimum: 16,
      joinPolicy: "PUBLIC",
      teamStatus: "LOOKING_FOR_PARTICIPANTS",
      maxParticipants: 20,
      participantIds: [ALICE_UID],
      participantNames: { [ALICE_UID]: "Alice" },
      pendingRequestIds: [],
      invitedUserIds: [],
      impactOrganizationName: "Red Sea Volunteers",
      impactVerifiedByAdmin: true,
      linkedAdventureId: "",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Alice creates a Padel Team-Up with REQUEST_APPROVAL
  await assertSucceeds(
    aliceDb.collection("reality_activities").doc("act_padel_1").set({
      creatorId: ALICE_UID,
      creatorName: "Alice",
      domain: "PLAY_SPORTS",
      subCategory: "PADEL",
      title: "Maadi Padel Doubles — Need 2 Players",
      description: "Friendly evening doubles match.",
      countryName: "Egypt",
      cityName: "Cairo",
      publicMeetingSpot: "Maadi Sports Courts Reception",
      latitude: 29.9602,
      longitude: 31.2569,
      scheduledDateLabel: "Friday 19:00",
      skillLevel: "INTERMEDIATE",
      equipmentNote: "Bring racket or rent at reception",
      safetyRules: "Public sports venue only. Hydrate well.",
      ageMinimum: 18,
      joinPolicy: "REQUEST_APPROVAL",
      teamStatus: "LOOKING_FOR_PARTICIPANTS",
      maxParticipants: 4,
      participantIds: [ALICE_UID],
      participantNames: { [ALICE_UID]: "Alice" },
      pendingRequestIds: [],
      invitedUserIds: [],
      impactOrganizationName: "",
      impactVerifiedByAdmin: false,
      linkedAdventureId: "",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Bob requests to join
  await assertSucceeds(
    bobDb.collection("reality_activities").doc("act_padel_1").update({
      pendingRequestIds: [BOB_UID],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob cannot read activity group chat while still pending
  await assertFails(
    bobDb.collection("reality_activities").doc("act_padel_1").collection("messages").doc("am_1").set({
      activityId: "act_padel_1",
      senderId: BOB_UID,
      senderName: "Bob",
      originalText: "Sneaking in",
      sourceLanguage: "en",
      translations: {},
      createdAt: new Date(Date.now() - 800),
    })
  );

  // Alice approves Bob
  await assertSucceeds(
    aliceDb.collection("reality_activities").doc("act_padel_1").update({
      participantIds: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      pendingRequestIds: [],
      teamStatus: "GROUP_FORMING",
      updatedAt: new Date(Date.now() - 600),
    })
  );

  // Now Bob can post in group chat
  await assertSucceeds(
    bobDb.collection("reality_activities").doc("act_padel_1").collection("messages").doc("am_1").set({
      activityId: "act_padel_1",
      senderId: BOB_UID,
      senderName: "Bob",
      originalText: "Bringing extra padel balls!",
      sourceLanguage: "en",
      translations: {},
      createdAt: new Date(Date.now() - 400),
    })
  );
});

test("Real World Missions vs Virtual separation, Global Challenges fairness, and Reality Bridge owner isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 3500);

  // Virtual mission forbids REAL_WORLD_LOG submissions
  await assertSucceeds(
    aliceDb.collection("reality_missions").doc("mis_virtual_1").set({
      creatorId: ALICE_UID,
      creatorName: "Alice",
      templateType: "CULTURE_CONNECT",
      executionMode: "VIRTUAL",
      title: "Virtual Nile Heritage Study",
      objective: "Explore virtual world landmarks and share notes.",
      cityName: "Luxor",
      countryName: "Egypt",
      safetyAndEcoRules: "Respect heritage education guidelines.",
      targetSteps: 3,
      participantIds: [ALICE_UID],
      submissionsCount: 0,
      status: "ACTIVE",
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertFails(
    aliceDb.collection("reality_missions").doc("mis_virtual_1").collection("submissions").doc("sub_bad").set({
      missionId: "mis_virtual_1",
      authorId: ALICE_UID,
      authorName: "Alice",
      submissionMode: "REAL_WORLD_LOG",
      reflectionText: "Claiming physical presence from virtual mode",
      placeLabel: "Luxor Temple",
      ecoSafetyConfirmed: true,
      createdAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    aliceDb.collection("reality_missions").doc("mis_virtual_1").collection("submissions").doc("sub_ok").set({
      missionId: "mis_virtual_1",
      authorId: ALICE_UID,
      authorName: "Alice",
      submissionMode: "VIRTUAL_PROGRESS",
      reflectionText: "Completed virtual pylon exploration notes.",
      placeLabel: "Luxor Virtual 3D",
      ecoSafetyConfirmed: true,
      createdAt: new Date(Date.now() - 900),
    })
  );

  // Global Challenge non-admin cannot forge ADMIN_VERIFIED entry
  await assertSucceeds(
    aliceDb.collection("global_challenges").doc("chg_1").set({
      creatorId: ALICE_UID,
      creatorName: "Alice",
      challengeScope: "INTERNATIONAL",
      category: "COMMUNITY_IMPACT",
      title: "Mediterranean & Red Sea Clean Shore Sprint",
      description: "Document verified coastal cleanup bags across cities.",
      targetGoal: 50,
      participantIds: [ALICE_UID],
      participantCountries: ["Egypt"],
      entriesCount: 0,
      cityName: "Alexandria",
      countryName: "Egypt",
      status: "ACTIVE",
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertFails(
    bobDb.collection("global_challenges").doc("chg_1").collection("entries").doc("ent_bad").set({
      challengeId: "chg_1",
      authorId: BOB_UID,
      authorName: "Bob",
      countryName: "Russia",
      teamName: "Team North",
      summaryText: "Self-forged admin verified entry",
      pointsClaimed: 5,
      verificationLabel: "ADMIN_VERIFIED",
      createdAt: new Date(Date.now() - 800),
    })
  );
  await assertSucceeds(
    bobDb.collection("global_challenges").doc("chg_1").collection("entries").doc("ent_ok").set({
      challengeId: "chg_1",
      authorId: BOB_UID,
      authorName: "Bob",
      countryName: "Russia",
      teamName: "Alexandria Visitors",
      summaryText: "Collected 2 bags along Stanley Corniche.",
      pointsClaimed: 2,
      verificationLabel: "USER_SUBMITTED",
      createdAt: new Date(Date.now() - 700),
    })
  );

  // Reality Bridge owner isolation
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("reality_bridge").doc("br_1").set({
      userId: ALICE_UID,
      virtualAdventureId: "sharm_reef_dive",
      virtualAdventureTitle: "Sharm El Sheikh — Shark & Yolanda Reef",
      destinationName: "Sharm El Sheikh",
      targetType: "REALITY_EVENT",
      targetId: "evt_cairo_1",
      targetTitle: "Cairo Cultural Walk & Museum Meetup",
      stage: "PLANNED",
      notes: "Bridging from Red Sea exploration to Cairo walk.",
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("reality_bridge").doc("br_1").get()
  );
});




