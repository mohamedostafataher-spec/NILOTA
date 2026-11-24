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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AdventureWorldRepository(
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
            ?: throw IllegalStateException("Google Sign-In required before accessing NILORA Live Worlds.")

    private fun <T> Flow<T>.logAndRethrow(op: OperationType, path: String, uid: String): Flow<T> =
        catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            val toLog = fsErr ?: (error as? Exception)
            if (toLog != null && auth.currentUser?.uid == uid) {
                handleFirestoreError(toLog, op, path)
            }
            throw error
        }

    // --- LIVE ADVENTURE SESSIONS ---

    fun observeJoinableSessions(): Flow<List<LiveAdventureSession>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("adventure_sessions")
                .whereEqualTo("status", "ACTIVE")
                .whereEqualTo("mode", AdventureSessionMode.JOIN_OTHERS.key)
                .snapshots()
                .map { snap ->
                    snap.documents.mapNotNull {
                        it.toObject(LiveAdventureSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .logAndRethrow(OperationType.LIST, "adventure_sessions", uid)
        )
    }

    fun observeMySessions(): Flow<List<LiveAdventureSession>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("adventure_sessions")
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snap ->
                    snap.documents.mapNotNull {
                        it.toObject(LiveAdventureSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .logAndRethrow(OperationType.LIST, "adventure_sessions", uid)
        )
    }

    fun observeSession(sessionId: String): Flow<LiveAdventureSession?> = flow {
        val uid = requireUserId()
        val path = "adventure_sessions/$sessionId"
        emitAll(
            db.collection("adventure_sessions").document(sessionId)
                .snapshots()
                .map { it.toObject(LiveAdventureSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.GET, path, uid)
        )
    }

    fun observeSessionParticipants(sessionId: String): Flow<List<SessionParticipantPresence>> = flow {
        val uid = requireUserId()
        val path = "adventure_sessions/$sessionId/participants"
        emitAll(
            db.collection("adventure_sessions").document(sessionId).collection("participants")
                .snapshots()
                .map { snap ->
                    snap.documents.mapNotNull {
                        it.toObject(SessionParticipantPresence::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }
                }
                .logAndRethrow(OperationType.LIST, path, uid)
        )
    }

    fun observeSessionMessages(sessionId: String): Flow<List<SessionChatMessage>> = flow {
        val uid = requireUserId()
        val path = "adventure_sessions/$sessionId/messages"
        emitAll(
            db.collection("adventure_sessions").document(sessionId).collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snap ->
                    snap.documents.mapNotNull {
                        it.toObject(SessionChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }
                }
                .logAndRethrow(OperationType.LIST, path, uid)
        )
    }

    suspend fun createSession(
        adventure: AdventureExperience,
        mode: AdventureSessionMode,
        hostProfile: UserProfile?,
        maxParticipants: Int,
        voicePreferred: Boolean,
        experienceLevel: AdventureLevel,
        durationMinutes: Int,
        availabilitySlot: String,
        crewId: String = ""
    ): Result<String> {
        var docPath = "adventure_sessions"
        return try {
            val uid = requireUserId()
            val sessionId = "adv_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val docRef = db.collection("adventure_sessions").document(sessionId)
            docPath = docRef.path

            val hostName = hostProfile?.displayName?.ifBlank { "NILORA Explorer" } ?: "NILORA Explorer"
            val hostCountry = hostProfile?.country?.ifBlank { "Egypt" } ?: "Egypt"
            val hostLang = hostProfile?.nativeLanguage?.ifBlank { hostProfile.preferredLanguage } ?: "en"

            val payload = mapOf(
                "hostId" to uid,
                "hostName" to hostName.take(80),
                "adventureId" to adventure.id,
                "adventureTitle" to adventure.title.take(120),
                "destinationName" to adventure.destinationName.take(100),
                "mode" to mode.key,
                "status" to "ACTIVE",
                "maxParticipants" to maxParticipants.coerceIn(1, 12),
                "participantIds" to listOf(uid),
                "participantNames" to mapOf(uid to hostName.take(80)),
                "participantCountries" to mapOf(uid to hostCountry.take(80)),
                "participantLanguages" to mapOf(uid to hostLang.take(16)),
                "voicePreferred" to voicePreferred,
                "experienceLevel" to experienceLevel.key,
                "durationMinutes" to durationMinutes.coerceIn(5, 180),
                "availabilitySlot" to availabilitySlot.take(80),
                "crewId" to crewId.take(128),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()

            // Publish initial 3D spatial presence for host
            updateMyPresence(
                sessionId = sessionId,
                profile = hostProfile,
                posX = 0f,
                posY = -3.8f,
                posZ = 3.5f,
                yawDeg = 0f,
                pointedPoiId = "",
                pointedX = 0f,
                pointedY = 0f,
                pointedZ = 0f,
                voiceMuted = !voicePreferred
            )
            Result.success(sessionId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun joinSession(session: LiveAdventureSession, myProfile: UserProfile?): Result<Unit> {
        val docPath = "adventure_sessions/${session.id}"
        return try {
            val uid = requireUserId()
            if (uid !in session.participantIds) {
                val myName = myProfile?.displayName?.ifBlank { "NILORA Explorer" } ?: "NILORA Explorer"
                val myCountry = myProfile?.country?.ifBlank { "Egypt" } ?: "Egypt"
                val myLang = myProfile?.nativeLanguage?.ifBlank { myProfile.preferredLanguage } ?: "en"
                val updatedIds = (session.participantIds + uid).distinct()
                val updatedNames = session.participantNames + (uid to myName.take(80))
                val updatedCountries = session.participantCountries + (uid to myCountry.take(80))
                val updatedLangs = session.participantLanguages + (uid to myLang.take(16))

                db.collection("adventure_sessions").document(session.id).update(
                    mapOf(
                        "participantIds" to updatedIds,
                        "participantNames" to updatedNames,
                        "participantCountries" to updatedCountries,
                        "participantLanguages" to updatedLangs,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            updateMyPresence(
                sessionId = session.id,
                profile = myProfile,
                posX = 1.6f,
                posY = -3.8f,
                posZ = 3.5f,
                yawDeg = 0f,
                pointedPoiId = "",
                pointedX = 0f,
                pointedY = 0f,
                pointedZ = 0f,
                voiceMuted = !session.voicePreferred
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun leaveSession(session: LiveAdventureSession): Result<Unit> {
        val docPath = "adventure_sessions/${session.id}"
        return try {
            val uid = requireUserId()
            runCatching {
                db.collection("adventure_sessions").document(session.id)
                    .collection("participants").document(uid).delete().await()
            }
            if (session.hostId == uid) {
                db.collection("adventure_sessions").document(session.id).update(
                    mapOf(
                        "status" to "ENDED",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else if (uid in session.participantIds) {
                val nextIds = session.participantIds.filter { it != uid }
                val nextNames = session.participantNames.filterKeys { it != uid }
                val nextCountries = session.participantCountries.filterKeys { it != uid }
                val nextLangs = session.participantLanguages.filterKeys { it != uid }
                db.collection("adventure_sessions").document(session.id).update(
                    mapOf(
                        "participantIds" to nextIds,
                        "participantNames" to nextNames,
                        "participantCountries" to nextCountries,
                        "participantLanguages" to nextLangs,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateMyPresence(
        sessionId: String,
        profile: UserProfile?,
        posX: Float,
        posY: Float,
        posZ: Float,
        yawDeg: Float,
        pointedPoiId: String,
        pointedX: Float,
        pointedY: Float,
        pointedZ: Float,
        voiceMuted: Boolean
    ): Result<Unit> {
        val docPath = "adventure_sessions/$sessionId/participants"
        return try {
            val uid = requireUserId()
            val name = profile?.displayName?.ifBlank { "NILORA Explorer" } ?: "NILORA Explorer"
            val country = profile?.country?.ifBlank { "Egypt" } ?: "Egypt"
            val lang = profile?.nativeLanguage?.ifBlank { profile.preferredLanguage } ?: "en"
            val payload = mapOf(
                "sessionId" to sessionId,
                "userId" to uid,
                "displayName" to name.take(80),
                "country" to country.take(80),
                "languageCode" to lang.take(16),
                "posX" to posX.toDouble().coerceIn(-200.0, 200.0),
                "posY" to posY.toDouble().coerceIn(-80.0, 80.0),
                "posZ" to posZ.toDouble().coerceIn(-200.0, 200.0),
                "yawDeg" to (yawDeg % 360f).toDouble(),
                "pointedPoiId" to pointedPoiId.take(80),
                "pointedX" to pointedX.toDouble(),
                "pointedY" to pointedY.toDouble(),
                "pointedZ" to pointedZ.toDouble(),
                "voiceMuted" to voiceMuted,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("adventure_sessions").document(sessionId)
                .collection("participants").document(uid).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun sendSessionMessage(
        sessionId: String,
        senderProfile: UserProfile?,
        originalText: String,
        sourceLanguage: String,
        translations: Map<String, String> = emptyMap(),
        translationNote: String = "",
        discoveryPoiId: String = ""
    ): Result<String> {
        val path = "adventure_sessions/$sessionId/messages"
        return try {
            val uid = requireUserId()
            val msgId = "smsg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val senderName = senderProfile?.displayName?.ifBlank { "NILORA Explorer" } ?: "NILORA Explorer"
            val payload = mapOf(
                "sessionId" to sessionId,
                "senderId" to uid,
                "senderName" to senderName.take(80),
                "originalText" to originalText.trim().take(1500),
                "sourceLanguage" to sourceLanguage.take(16),
                "translations" to translations,
                "translationNote" to translationNote.take(240),
                "discoveryPoiId" to discoveryPoiId.take(80),
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("adventure_sessions").document(sessionId)
                .collection("messages").document(msgId).set(payload).await()
            Result.success(msgId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun cacheSessionMessageTranslation(
        sessionId: String,
        messageId: String,
        updatedTranslations: Map<String, String>,
        note: String
    ): Result<Unit> {
        val path = "adventure_sessions/$sessionId/messages/$messageId"
        return try {
            requireUserId()
            db.collection("adventure_sessions").document(sessionId)
                .collection("messages").document(messageId)
                .update(mapOf("translations" to updatedTranslations, "translationNote" to note.take(240)))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- ADVENTURE MATCHMAKING ---

    fun observeAvailableMatchers(): Flow<List<AdventureMatchProfile>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("adventure_match_profiles")
                .whereEqualTo("isAvailable", true)
                .snapshots()
                .map { snap ->
                    snap.documents.mapNotNull {
                        it.toObject(AdventureMatchProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }
                }
                .logAndRethrow(OperationType.LIST, "adventure_match_profiles", uid)
        )
    }

    suspend fun saveMyMatchProfile(profile: AdventureMatchProfile): Result<Unit> {
        val uid = requireUserId()
        val path = "adventure_match_profiles/$uid"
        return try {
            val payload = mapOf(
                "userId" to uid,
                "displayName" to profile.displayName.trim().ifBlank { "NILORA Explorer" }.take(80),
                "country" to profile.country.trim().ifBlank { "Egypt" }.take(80),
                "targetAdventureId" to profile.targetAdventureId.take(80),
                "targetDestination" to profile.targetDestination.take(100),
                "interests" to profile.interests.take(15).map { it.take(40) },
                "preferredLanguage" to profile.preferredLanguage.take(16),
                "spokenLanguages" to profile.spokenLanguages.ifEmpty { listOf(profile.preferredLanguage) }.take(15),
                "experienceLevel" to profile.experienceLevel,
                "preferredDurationMinutes" to profile.preferredDurationMinutes.coerceIn(5, 180),
                "availableTimeSlot" to profile.availableTimeSlot.trim().ifBlank { "Flexible" }.take(80),
                "voicePreferred" to profile.voicePreferred,
                "isAvailable" to profile.isAvailable,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("adventure_match_profiles").document(uid).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- NILORA CREWS ---

    fun observeMyCrews(): Flow<List<NiloraCrew>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("nilora_crews")
                .whereArrayContains("memberIds", uid)
                .snapshots()
                .map { snap ->
                    snap.documents.mapNotNull {
                        it.toObject(NiloraCrew::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .logAndRethrow(OperationType.LIST, "nilora_crews", uid)
        )
    }

    fun observeOpenCrews(): Flow<List<NiloraCrew>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("nilora_crews")
                .whereEqualTo("joinPolicy", "OPEN")
                .snapshots()
                .map { snap ->
                    snap.documents.mapNotNull {
                        it.toObject(NiloraCrew::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .logAndRethrow(OperationType.LIST, "nilora_crews", uid)
        )
    }

    fun observeCrewDetail(crewId: String): Flow<NiloraCrew?> = flow {
        val uid = requireUserId()
        val path = "nilora_crews/$crewId"
        emitAll(
            db.collection("nilora_crews").document(crewId)
                .snapshots()
                .map { it.toObject(NiloraCrew::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.GET, path, uid)
        )
    }

    fun observeCrewMessages(crewId: String): Flow<List<CrewMessage>> = flow {
        val uid = requireUserId()
        val path = "nilora_crews/$crewId/messages"
        emitAll(
            db.collection("nilora_crews").document(crewId).collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snap ->
                    snap.documents.mapNotNull {
                        it.toObject(CrewMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }
                }
                .logAndRethrow(OperationType.LIST, path, uid)
        )
    }

    suspend fun createCrew(
        name: String,
        description: String,
        focusDestination: String,
        sharedInterests: List<String>,
        plannedAdventureId: String?,
        historyTitle: String?,
        joinPolicy: String,
        voiceChannelEnabled: Boolean,
        leaderProfile: UserProfile?
    ): Result<String> {
        var docPath = "nilora_crews"
        return try {
            val uid = requireUserId()
            val crewId = "crew_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val ref = db.collection("nilora_crews").document(crewId)
            docPath = ref.path
            val leaderName = leaderProfile?.displayName?.ifBlank { "NILORA Explorer" } ?: "NILORA Explorer"
            val leaderCountry = leaderProfile?.country?.ifBlank { "Egypt" } ?: "Egypt"

            val payload = mapOf(
                "name" to name.trim().take(80),
                "description" to description.trim().take(400),
                "leaderId" to uid,
                "memberIds" to listOf(uid),
                "memberNames" to mapOf(uid to leaderName.take(80)),
                "memberCountries" to mapOf(uid to leaderCountry.take(80)),
                "sharedInterests" to sharedInterests.take(15),
                "focusDestination" to focusDestination.trim().ifBlank { "Sharm El Sheikh" }.take(100),
                "plannedAdventureIds" to listOfNotNull(plannedAdventureId?.takeIf { it.isNotBlank() }),
                "adventureHistoryTitles" to listOfNotNull(historyTitle?.takeIf { it.isNotBlank() }),
                "joinPolicy" to if (joinPolicy == "INVITE_ONLY") "INVITE_ONLY" else "OPEN",
                "voiceChannelEnabled" to voiceChannelEnabled,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            Result.success(crewId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun joinOpenCrew(crew: NiloraCrew, myProfile: UserProfile?): Result<Unit> {
        val docPath = "nilora_crews/${crew.id}"
        return try {
            val uid = requireUserId()
            if (uid in crew.memberIds) return Result.success(Unit)
            val myName = myProfile?.displayName?.ifBlank { "NILORA Explorer" } ?: "NILORA Explorer"
            val myCountry = myProfile?.country?.ifBlank { "Egypt" } ?: "Egypt"
            db.collection("nilora_crews").document(crew.id).update(
                mapOf(
                    "memberIds" to (crew.memberIds + uid).distinct(),
                    "memberNames" to (crew.memberNames + (uid to myName.take(80))),
                    "memberCountries" to (crew.memberCountries + (uid to myCountry.take(80))),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun sendCrewMessage(
        crewId: String,
        senderProfile: UserProfile?,
        originalText: String,
        sourceLanguage: String
    ): Result<String> {
        val path = "nilora_crews/$crewId/messages"
        return try {
            val uid = requireUserId()
            val msgId = "cmsg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val senderName = senderProfile?.displayName?.ifBlank { "NILORA Explorer" } ?: "NILORA Explorer"
            val payload = mapOf(
                "crewId" to crewId,
                "senderId" to uid,
                "senderName" to senderName.take(80),
                "originalText" to originalText.trim().take(1500),
                "sourceLanguage" to sourceLanguage.take(16),
                "translations" to emptyMap<String, String>(),
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("nilora_crews").document(crewId).collection("messages").document(msgId).set(payload).await()
            Result.success(msgId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun cacheCrewMessageTranslation(
        crewId: String,
        messageId: String,
        updatedTranslations: Map<String, String>
    ): Result<Unit> {
        val path = "nilora_crews/$crewId/messages/$messageId"
        return try {
            requireUserId()
            db.collection("nilora_crews").document(crewId)
                .collection("messages").document(messageId)
                .update("translations", updatedTranslations)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun planCrewAdventure(crew: NiloraCrew, adventure: AdventureExperience): Result<Unit> {
        val path = "nilora_crews/${crew.id}"
        return try {
            requireUserId()
            val nextPlanned = (crew.plannedAdventureIds + adventure.id).distinct().take(20)
            val nextHistory = (crew.adventureHistoryTitles + "${adventure.destinationName}: ${adventure.title}").distinct().take(30)
            db.collection("nilora_crews").document(crew.id).update(
                mapOf(
                    "plannedAdventureIds" to nextPlanned,
                    "adventureHistoryTitles" to nextHistory,
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
}
