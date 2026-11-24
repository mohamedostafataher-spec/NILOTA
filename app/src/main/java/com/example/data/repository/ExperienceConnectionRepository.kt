package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.*
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Firestore repository for NILORA Next-Gen Human Connection experiences:
 * - World Postcard & living multi-country contributions
 * - Culture Swap prompts & structured exchanges
 * - World Quests with genuine goal-gated progress
 * - NILORA Memory collections with strict privacy & audio retention consent
 *
 * Every query predicate is aligned 1:1 with Firestore Security Rules.
 */
class ExperienceConnectionRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Please sign in with Google to participate in NILORA experiences.")

    fun pairDocId(uidA: String, uidB: String): String =
        if (uidA < uidB) "${uidA}__${uidB}" else "${uidB}__${uidA}"

    private fun <T> Flow<T>.logAndRethrow(op: OperationType, path: String, uid: String): Flow<T> =
        catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val toLog = firestoreError ?: (error as? Exception)
            if (toLog != null && auth.currentUser?.uid == uid) {
                handleFirestoreError(toLog, op, path)
            }
            throw error
        }

    // --- WORLD POSTCARDS ---

    fun observeWorldPostcards(): Flow<List<WorldPostcard>> = flow {
        val uid = requireUserId()
        val publicFlow = db.collection("world_postcards")
            .whereEqualTo("visibility", "PUBLIC")
            .snapshots()
            .map { it.toObjects(WorldPostcard::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .logAndRethrow(OperationType.LIST, "world_postcards(PUBLIC)", uid)

        val ownFlow = db.collection("world_postcards")
            .whereEqualTo("authorId", uid)
            .snapshots()
            .map { it.toObjects(WorldPostcard::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .logAndRethrow(OperationType.LIST, "world_postcards(OWN)", uid)

        emitAll(
            combine(publicFlow, ownFlow) { pub, mine ->
                (mine + pub)
                    .distinctBy { it.id }
                    .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
            }
        )
    }

    fun observePostcard(postcardId: String): Flow<WorldPostcard?> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("world_postcards").document(postcardId)
                .snapshots()
                .map { it.toObject(WorldPostcard::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.GET, "world_postcards/$postcardId", uid)
        )
    }

    fun observePostcardContributions(postcardId: String): Flow<List<PostcardContribution>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("world_postcards").document(postcardId).collection("contributions")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { it.toObjects(PostcardContribution::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "world_postcards/$postcardId/contributions", uid)
        )
    }

    suspend fun createWorldPostcard(
        myProfile: UserProfile?,
        category: PostcardCategory,
        title: String,
        storyText: String,
        localExpression: String,
        questionPrompt: String,
        country: String,
        city: String,
        languageCode: String,
        photoBase64: String,
        audioBase64: String,
        audioDurationMs: Long,
        allowReplies: Boolean,
        allowPrivateConnect: Boolean,
        visibility: String
    ): Result<String> {
        var docPath = "world_postcards"
        return try {
            val uid = requireUserId()
            val ref = db.collection("world_postcards").document()
            docPath = ref.path
            val authorName = myProfile?.displayName?.ifBlank { auth.currentUser?.displayName ?: "NILORA Member" }
                ?: auth.currentUser?.displayName ?: "NILORA Member"
            val cleanCountry = country.trim().ifBlank { myProfile?.country ?: "Egypt" }.take(80)
            val safeVisibility = if (visibility in listOf("PUBLIC", "CONNECTIONS_ONLY", "PRIVATE")) visibility else "PUBLIC"

            ref.set(
                mapOf(
                    "authorId" to uid,
                    "authorName" to authorName.take(80),
                    "country" to cleanCountry,
                    "city" to city.trim().take(80),
                    "languageCode" to languageCode.ifBlank { "en" }.take(16),
                    "category" to category.key,
                    "title" to title.trim().take(120),
                    "storyText" to storyText.trim().take(1500),
                    "localExpression" to localExpression.trim().take(300),
                    "questionPrompt" to questionPrompt.trim().take(300),
                    "photoBase64" to photoBase64.take(220000),
                    "audioBase64" to audioBase64.take(100000),
                    "audioDurationMs" to audioDurationMs.coerceIn(0L, 30000L),
                    "translations" to emptyMap<String, String>(),
                    "translationNote" to "",
                    "participantIds" to listOf(uid),
                    "countriesChain" to listOf(cleanCountry),
                    "contributionsCount" to 0,
                    "allowReplies" to allowReplies,
                    "allowPrivateConnect" to allowPrivateConnect,
                    "visibility" to safeVisibility,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addPostcardContribution(
        postcard: WorldPostcard,
        myProfile: UserProfile?,
        responseText: String,
        localExpression: String,
        country: String,
        city: String,
        languageCode: String,
        photoBase64: String,
        audioBase64: String,
        audioDurationMs: Long,
        allowPrivateConnect: Boolean
    ): Result<String> {
        val cardRef = db.collection("world_postcards").document(postcard.id)
        val contribRef = cardRef.collection("contributions").document()
        return try {
            val uid = requireUserId()
            val authorName = myProfile?.displayName?.ifBlank { auth.currentUser?.displayName ?: "NILORA Member" }
                ?: auth.currentUser?.displayName ?: "NILORA Member"
            val cleanCountry = country.trim().ifBlank { myProfile?.country ?: "Egypt" }.take(80)

            contribRef.set(
                mapOf(
                    "postcardId" to postcard.id,
                    "authorId" to uid,
                    "authorName" to authorName.take(80),
                    "country" to cleanCountry,
                    "city" to city.trim().take(80),
                    "languageCode" to languageCode.ifBlank { "en" }.take(16),
                    "responseText" to responseText.trim().take(1500),
                    "localExpression" to localExpression.trim().take(300),
                    "photoBase64" to photoBase64.take(220000),
                    "audioBase64" to audioBase64.take(100000),
                    "audioDurationMs" to audioDurationMs.coerceIn(0L, 30000L),
                    "translations" to emptyMap<String, String>(),
                    "translationNote" to "",
                    "allowPrivateConnect" to allowPrivateConnect,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val snap = cardRef.get().await()
            val curParticipants = (snap.get("participantIds") as? List<*>)
                ?.filterIsInstance<String>().orEmpty()
            val curCountries = (snap.get("countriesChain") as? List<*>)
                ?.filterIsInstance<String>().orEmpty()
            val curCount = snap.getLong("contributionsCount") ?: postcard.contributionsCount
            val nextParticipants = (curParticipants + uid).distinct().take(50)
            val nextCountries = if (curCountries.any { it.equals(cleanCountry, ignoreCase = true) }) {
                curCountries
            } else {
                (curCountries + cleanCountry).take(50)
            }

            cardRef.update(
                mapOf(
                    "participantIds" to nextParticipants,
                    "countriesChain" to nextCountries,
                    "contributionsCount" to curCount + 1,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (postcard.authorId != uid) {
                sendExperienceNotificationSafely(
                    targetUserId = postcard.authorId,
                    title = "New World Postcard Reply ($cleanCountry)",
                    message = "$authorName added to your postcard '${postcard.title.take(50)}'."
                )
            }
            Result.success(contribRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, contribRef.path)
            Result.failure(e)
        }
    }

    suspend fun cachePostcardTranslation(
        postcardId: String,
        mergedTranslations: Map<String, String>,
        note: String
    ): Result<Unit> {
        val path = "world_postcards/$postcardId"
        return try {
            requireUserId()
            db.collection("world_postcards").document(postcardId)
                .update(
                    mapOf(
                        "translations" to mergedTranslations.entries.take(20).associate { it.key.take(16) to it.value.take(1500) },
                        "translationNote" to note.take(240)
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun cacheContributionTranslation(
        postcardId: String,
        contributionId: String,
        mergedTranslations: Map<String, String>,
        note: String
    ): Result<Unit> {
        val path = "world_postcards/$postcardId/contributions/$contributionId"
        return try {
            requireUserId()
            db.collection("world_postcards").document(postcardId)
                .collection("contributions").document(contributionId)
                .update(
                    mapOf(
                        "translations" to mergedTranslations.entries.take(20).associate { it.key.take(16) to it.value.take(1500) },
                        "translationNote" to note.take(240)
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- CULTURE SWAPS ---

    fun observeCultureSwaps(): Flow<List<CultureSwap>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("culture_swaps")
                .whereIn("status", listOf("OPEN", "EXCHANGED", "COMPLETED"))
                .snapshots()
                .map { snap ->
                    snap.toObjects(CultureSwap::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrow(OperationType.LIST, "culture_swaps", uid)
        )
    }

    fun observeCultureSwap(swapId: String): Flow<CultureSwap?> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("culture_swaps").document(swapId)
                .snapshots()
                .map { it.toObject(CultureSwap::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.GET, "culture_swaps/$swapId", uid)
        )
    }

    fun observeCultureSwapEntries(swapId: String): Flow<List<CultureSwapEntry>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("culture_swaps").document(swapId).collection("entries")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { it.toObjects(CultureSwapEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "culture_swaps/$swapId/entries", uid)
        )
    }

    suspend fun createCultureSwap(
        myProfile: UserProfile?,
        promptCategory: SwapPromptCategory,
        promptText: String,
        participationMode: String,
        maxParticipants: Long,
        initialResponseText: String,
        expressionSample: String,
        languageCode: String,
        photoBase64: String,
        audioBase64: String,
        audioDurationMs: Long,
        allowPrivateConnect: Boolean
    ): Result<String> {
        var docPath = "culture_swaps"
        return try {
            val uid = requireUserId()
            val swapRef = db.collection("culture_swaps").document()
            docPath = swapRef.path
            val creatorName = myProfile?.displayName?.ifBlank { auth.currentUser?.displayName ?: "NILORA Member" }
                ?: auth.currentUser?.displayName ?: "NILORA Member"
            val creatorCountry = myProfile?.country?.ifBlank { "Egypt" } ?: "Egypt"
            val mode = if (participationMode == "GROUP") "GROUP" else "TWO_PERSON"
            val cappedMax = if (mode == "TWO_PERSON") 2L else maxParticipants.coerceIn(3L, 20L)
            val hasInitial = initialResponseText.isNotBlank()

            swapRef.set(
                mapOf(
                    "creatorId" to uid,
                    "creatorName" to creatorName.take(80),
                    "creatorCountry" to creatorCountry.take(80),
                    "creatorLanguage" to languageCode.ifBlank { "en" }.take(16),
                    "promptCategory" to promptCategory.key,
                    "promptText" to promptText.trim().take(400),
                    "participationMode" to mode,
                    "maxParticipants" to cappedMax,
                    "participantIds" to listOf(uid),
                    "participantNames" to mapOf(uid to creatorName.take(80)),
                    "participantCountries" to mapOf(uid to creatorCountry.take(80)),
                    "entriesCount" to if (hasInitial) 1L else 0L,
                    "status" to "OPEN",
                    "allowPrivateConnect" to allowPrivateConnect,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (hasInitial) {
                val entryRef = swapRef.collection("entries").document()
                entryRef.set(
                    mapOf(
                        "swapId" to swapRef.id,
                        "authorId" to uid,
                        "authorName" to creatorName.take(80),
                        "country" to creatorCountry.take(80),
                        "languageCode" to languageCode.ifBlank { "en" }.take(16),
                        "contentText" to initialResponseText.trim().take(1500),
                        "expressionSample" to expressionSample.trim().take(300),
                        "photoBase64" to photoBase64.take(220000),
                        "audioBase64" to audioBase64.take(100000),
                        "audioDurationMs" to audioDurationMs.coerceIn(0L, 30000L),
                        "translations" to emptyMap<String, String>(),
                        "translationNote" to "",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(swapRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addCultureSwapEntry(
        swap: CultureSwap,
        myProfile: UserProfile?,
        contentText: String,
        expressionSample: String,
        country: String,
        languageCode: String,
        photoBase64: String,
        audioBase64: String,
        audioDurationMs: Long
    ): Result<String> {
        val swapRef = db.collection("culture_swaps").document(swap.id)
        val entryRef = swapRef.collection("entries").document()
        return try {
            val uid = requireUserId()
            val authorName = myProfile?.displayName?.ifBlank { auth.currentUser?.displayName ?: "NILORA Member" }
                ?: auth.currentUser?.displayName ?: "NILORA Member"
            val cleanCountry = country.trim().ifBlank { myProfile?.country ?: "Egypt" }.take(80)

            entryRef.set(
                mapOf(
                    "swapId" to swap.id,
                    "authorId" to uid,
                    "authorName" to authorName.take(80),
                    "country" to cleanCountry,
                    "languageCode" to languageCode.ifBlank { "en" }.take(16),
                    "contentText" to contentText.trim().take(1500),
                    "expressionSample" to expressionSample.trim().take(300),
                    "photoBase64" to photoBase64.take(220000),
                    "audioBase64" to audioBase64.take(100000),
                    "audioDurationMs" to audioDurationMs.coerceIn(0L, 30000L),
                    "translations" to emptyMap<String, String>(),
                    "translationNote" to "",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val snap = swapRef.get().await()
            val curIds = (snap.get("participantIds") as? List<*>)?.filterIsInstance<String>().orEmpty()
            val curNames = (snap.get("participantNames") as? Map<*, *>)
                ?.entries?.associate { it.key.toString() to it.value.toString() } ?: emptyMap()
            val curCountries = (snap.get("participantCountries") as? Map<*, *>)
                ?.entries?.associate { it.key.toString() to it.value.toString() } ?: emptyMap()
            val curCount = snap.getLong("entriesCount") ?: swap.entriesCount

            val nextIds = (curIds + uid).distinct().take(swap.maxParticipants.toInt().coerceIn(2, 20))
            val nextNames = curNames + (uid to authorName.take(80))
            val nextCountries = curCountries + (uid to cleanCountry)
            val nextStatus = if (nextIds.size >= 2) "EXCHANGED" else "OPEN"

            swapRef.update(
                mapOf(
                    "participantIds" to nextIds,
                    "participantNames" to nextNames,
                    "participantCountries" to nextCountries,
                    "entriesCount" to curCount + 1,
                    "status" to nextStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (swap.creatorId != uid) {
                sendExperienceNotificationSafely(
                    targetUserId = swap.creatorId,
                    title = "Culture Swap Reply ($cleanCountry)",
                    message = "$authorName shared a response to your Culture Swap."
                )
            }
            Result.success(entryRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, entryRef.path)
            Result.failure(e)
        }
    }

    suspend fun cacheSwapEntryTranslation(
        swapId: String,
        entryId: String,
        mergedTranslations: Map<String, String>,
        note: String
    ): Result<Unit> {
        val path = "culture_swaps/$swapId/entries/$entryId"
        return try {
            requireUserId()
            db.collection("culture_swaps").document(swapId)
                .collection("entries").document(entryId)
                .update(
                    mapOf(
                        "translations" to mergedTranslations.entries.take(20).associate { it.key.take(16) to it.value.take(1500) },
                        "translationNote" to note.take(240)
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- WORLD QUESTS ---

    fun observeWorldQuests(): Flow<List<WorldQuest>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("world_quests")
                .whereIn("status", listOf("ACTIVE", "COMPLETED"))
                .snapshots()
                .map { snap ->
                    snap.toObjects(WorldQuest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrow(OperationType.LIST, "world_quests", uid)
        )
    }

    fun observeWorldQuest(questId: String): Flow<WorldQuest?> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("world_quests").document(questId)
                .snapshots()
                .map { it.toObject(WorldQuest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.GET, "world_quests/$questId", uid)
        )
    }

    fun observeWorldQuestContributions(questId: String): Flow<List<WorldQuestContribution>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("world_quests").document(questId).collection("contributions")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { it.toObjects(WorldQuestContribution::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "world_quests/$questId/contributions", uid)
        )
    }

    suspend fun createWorldQuest(
        myProfile: UserProfile?,
        missionType: QuestMissionType,
        title: String,
        objective: String,
        targetGoal: Long
    ): Result<String> {
        var docPath = "world_quests"
        return try {
            val uid = requireUserId()
            val ref = db.collection("world_quests").document()
            docPath = ref.path
            val creatorName = myProfile?.displayName?.ifBlank { auth.currentUser?.displayName ?: "NILORA Member" }
                ?: auth.currentUser?.displayName ?: "NILORA Member"
            val creatorCountry = myProfile?.country?.ifBlank { "Egypt" } ?: "Egypt"

            ref.set(
                mapOf(
                    "creatorId" to uid,
                    "creatorName" to creatorName.take(80),
                    "missionType" to missionType.key,
                    "title" to title.trim().take(120),
                    "objective" to objective.trim().take(500),
                    "targetGoal" to targetGoal.coerceIn(2L, 50L),
                    "participantIds" to listOf(uid),
                    "participantCountries" to listOf(creatorCountry.take(80)),
                    "contributionsCount" to 0L,
                    "status" to "ACTIVE",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addWorldQuestContribution(
        quest: WorldQuest,
        myProfile: UserProfile?,
        contentText: String,
        country: String,
        languageCode: String,
        photoBase64: String,
        audioBase64: String,
        audioDurationMs: Long
    ): Result<String> {
        val questRef = db.collection("world_quests").document(quest.id)
        val contribRef = questRef.collection("contributions").document()
        return try {
            val uid = requireUserId()
            val authorName = myProfile?.displayName?.ifBlank { auth.currentUser?.displayName ?: "NILORA Member" }
                ?: auth.currentUser?.displayName ?: "NILORA Member"
            val cleanCountry = country.trim().ifBlank { myProfile?.country ?: "Egypt" }.take(80)

            contribRef.set(
                mapOf(
                    "questId" to quest.id,
                    "authorId" to uid,
                    "authorName" to authorName.take(80),
                    "country" to cleanCountry,
                    "languageCode" to languageCode.ifBlank { "en" }.take(16),
                    "contentText" to contentText.trim().take(1500),
                    "photoBase64" to photoBase64.take(220000),
                    "audioBase64" to audioBase64.take(100000),
                    "audioDurationMs" to audioDurationMs.coerceIn(0L, 30000L),
                    "translations" to emptyMap<String, String>(),
                    "translationNote" to "",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val snap = questRef.get().await()
            val curIds = (snap.get("participantIds") as? List<*>)?.filterIsInstance<String>().orEmpty()
            val curCountries = (snap.get("participantCountries") as? List<*>)?.filterIsInstance<String>().orEmpty()
            val curCount = snap.getLong("contributionsCount") ?: quest.contributionsCount
            val nextCount = curCount + 1
            val nextIds = (curIds + uid).distinct().take(50)
            val nextCountries = if (curCountries.any { it.equals(cleanCountry, ignoreCase = true) }) {
                curCountries
            } else {
                (curCountries + cleanCountry).take(50)
            }
            val nextStatus = if (nextCount >= quest.targetGoal) "COMPLETED" else "ACTIVE"

            questRef.update(
                mapOf(
                    "participantIds" to nextIds,
                    "participantCountries" to nextCountries,
                    "contributionsCount" to nextCount,
                    "status" to nextStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (quest.creatorId != uid) {
                sendExperienceNotificationSafely(
                    targetUserId = quest.creatorId,
                    title = if (nextStatus == "COMPLETED") "World Quest Completed!" else "New World Quest Contribution",
                    message = "$authorName ($cleanCountry) contributed to '${quest.title.take(50)}'."
                )
            }
            Result.success(contribRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, contribRef.path)
            Result.failure(e)
        }
    }

    suspend fun cacheQuestContributionTranslation(
        questId: String,
        contributionId: String,
        mergedTranslations: Map<String, String>,
        note: String
    ): Result<Unit> {
        val path = "world_quests/$questId/contributions/$contributionId"
        return try {
            requireUserId()
            db.collection("world_quests").document(questId)
                .collection("contributions").document(contributionId)
                .update(
                    mapOf(
                        "translations" to mergedTranslations.entries.take(20).associate { it.key.take(16) to it.value.take(1500) },
                        "translationNote" to note.take(240)
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- NILORA MEMORY ---

    fun observeAccessibleMemories(): Flow<List<NiloraMemory>> = flow {
        val uid = requireUserId()
        val ownFlow = db.collection("nilora_memories")
            .whereEqualTo("ownerId", uid)
            .snapshots()
            .map { it.toObjects(NiloraMemory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .logAndRethrow(OperationType.LIST, "nilora_memories(OWN)", uid)

        val sharedFlow = db.collection("nilora_memories")
            .whereEqualTo("visibility", "SHARED_WITH_PARTICIPANTS")
            .whereArrayContains("partnerIds", uid)
            .snapshots()
            .map { it.toObjects(NiloraMemory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .logAndRethrow(OperationType.LIST, "nilora_memories(SHARED)", uid)

        val publicFlow = db.collection("nilora_memories")
            .whereEqualTo("visibility", "PUBLIC")
            .snapshots()
            .map { it.toObjects(NiloraMemory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .logAndRethrow(OperationType.LIST, "nilora_memories(PUBLIC)", uid)

        emitAll(
            combine(ownFlow, sharedFlow, publicFlow) { own, shared, pub ->
                (own + shared + pub)
                    .distinctBy { it.id }
                    .sortedByDescending { it.createdAt?.seconds ?: it.updatedAt?.seconds ?: 0L }
            }
        )
    }

    suspend fun saveNiloraMemory(
        myProfile: UserProfile?,
        memoryType: MemoryType,
        sourceId: String,
        title: String,
        summaryText: String,
        originalLanguage: String,
        translations: Map<String, String>,
        countries: List<String>,
        partnerIds: List<String>,
        partnerNames: List<String>,
        placeLabel: String,
        photoBase64: String,
        audioBase64: String,
        audioDurationMs: Long,
        retainAudioConsent: Boolean,
        visibility: MemoryVisibility
    ): Result<String> {
        var docPath = "nilora_memories"
        return try {
            val uid = requireUserId()
            val ref = db.collection("nilora_memories").document()
            docPath = ref.path
            val ownerName = myProfile?.displayName?.ifBlank { auth.currentUser?.displayName ?: "NILORA Member" }
                ?: auth.currentUser?.displayName ?: "NILORA Member"
            val effectiveAudio = if (retainAudioConsent) audioBase64.take(100000) else ""
            val effectiveDuration = if (retainAudioConsent && effectiveAudio.isNotBlank()) {
                audioDurationMs.coerceIn(0L, 30000L)
            } else {
                0L
            }

            ref.set(
                mapOf(
                    "ownerId" to uid,
                    "ownerName" to ownerName.take(80),
                    "memoryType" to memoryType.key,
                    "sourceId" to sourceId.take(128),
                    "title" to title.trim().take(120),
                    "summaryText" to summaryText.trim().take(2000),
                    "originalLanguage" to originalLanguage.ifBlank { "en" }.take(16),
                    "translations" to translations.entries.take(20).associate { it.key.take(16) to it.value.take(1500) },
                    "countries" to countries.filter { it.isNotBlank() }.distinct().take(50).map { it.take(80) },
                    "partnerIds" to partnerIds.filter { it.isNotBlank() && it != uid }.distinct().take(50),
                    "partnerNames" to partnerNames.filter { it.isNotBlank() }.distinct().take(50).map { it.take(80) },
                    "placeLabel" to placeLabel.trim().take(120),
                    "photoBase64" to photoBase64.take(220000),
                    "audioBase64" to effectiveAudio,
                    "audioDurationMs" to effectiveDuration,
                    "retainAudioConsent" to retainAudioConsent,
                    "visibility" to visibility.key,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateMemoryVisibility(memoryId: String, visibility: MemoryVisibility): Result<Unit> {
        val path = "nilora_memories/$memoryId"
        return try {
            requireUserId()
            db.collection("nilora_memories").document(memoryId)
                .update(
                    mapOf(
                        "visibility" to visibility.key,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun revokeMemoryAudio(memoryId: String): Result<Unit> {
        val path = "nilora_memories/$memoryId"
        return try {
            requireUserId()
            db.collection("nilora_memories").document(memoryId)
                .update(
                    mapOf(
                        "audioBase64" to "",
                        "audioDurationMs" to 0L,
                        "retainAudioConsent" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteMemory(memoryId: String): Result<Unit> {
        val path = "nilora_memories/$memoryId"
        return try {
            requireUserId()
            db.collection("nilora_memories").document(memoryId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- HELPER: EXPERIENCE -> PRIVATE CONTINUATION (ONLY WITH MUTUAL CONSENT) ---

    suspend fun openPrivateContinuation(
        myProfile: UserProfile?,
        partnerId: String,
        partnerName: String,
        partnerCountry: String,
        partnerLanguage: String,
        contextOriginTitle: String
    ): Result<String> {
        var path = "direct_conversations"
        return try {
            val uid = requireUserId()
            require(uid != partnerId) { "Cannot open a private continuation with yourself." }
            val convId = pairDocId(uid, partnerId)
            path = "direct_conversations/$convId"
            val ref = db.collection("direct_conversations").document(convId)
            val snap = ref.get().await()
            val myName = myProfile?.displayName?.ifBlank { auth.currentUser?.displayName ?: "NILORA Member" }
                ?: auth.currentUser?.displayName ?: "NILORA Member"
            val myLang = myProfile?.nativeLanguage?.ifBlank { myProfile.preferredLanguage } ?: "en"
            val sortedIds = listOf(uid, partnerId).sorted()
            val namesMap = mapOf(uid to myName.take(80), partnerId to partnerName.ifBlank { "Member" }.take(80))
            val langsMap = mapOf(uid to myLang.take(16), partnerId to partnerLanguage.ifBlank { "en" }.take(16))

            if (!snap.exists()) {
                ref.set(
                    mapOf(
                        "participantIds" to sortedIds,
                        "participantNames" to namesMap,
                        "participantLanguages" to langsMap,
                        "lastMessagePreview" to "Connected from: ${contextOriginTitle.take(60)}",
                        "lastMessageSenderId" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                sendExperienceNotificationSafely(
                    targetUserId = partnerId,
                    title = "Continued From Experience",
                    message = "$myName ($partnerCountry exchange) continued from '$contextOriginTitle'."
                )
            }
            Result.success(convId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private suspend fun sendExperienceNotificationSafely(
        targetUserId: String,
        title: String,
        message: String
    ) {
        if (targetUserId.isBlank()) return
        try {
            db.collection("notifications").add(
                mapOf(
                    "userId" to targetUserId,
                    "title" to title.take(120),
                    "message" to message.take(500),
                    "category" to "EXPERIENCE",
                    "readStatus" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (_: Exception) {
            // Non-blocking side notification
        }
    }
}
