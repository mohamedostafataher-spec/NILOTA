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

class RealityUniverseRepository(
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
            ?: throw IllegalStateException("Google Sign-In required to access NILORA Reality Connected Universe.")

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

    // --- 1. REALITY EVENTS ---

    fun observeRealityEvents(): Flow<List<RealityEvent>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("reality_events")
                .snapshots()
                .map { snap ->
                    snap.toObjects(RealityEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrow(OperationType.LIST, "reality_events", uid)
        )
    }

    suspend fun createRealityEvent(
        creatorName: String,
        title: String,
        category: RealityEventCategory,
        description: String,
        countryCode: String,
        countryName: String,
        cityName: String,
        venueName: String,
        latitude: Double?,
        longitude: Double?,
        startDateIso: String,
        endDateIso: String,
        timeWindowLabel: String,
        freeVerified: Boolean,
        priceNote: String,
        organizerName: String,
        organizerType: EventOrganizerType,
        officialUrl: String,
        conditionsNote: String,
        safetyNote: String,
        ageMinimum: Int,
        maxParticipants: Int,
        joinPolicy: String,
        linkedAdventureId: String = "",
        isAdmin: Boolean = false,
        adminVerifiedImmediately: Boolean = false
    ): Result<String> {
        var docPath = "reality_events"
        return try {
            val uid = requireUserId()
            val ref = db.collection("reality_events").document()
            docPath = ref.path
            val cleanCreator = creatorName.trim().ifBlank { "NILORA Member" }.take(80)
            val cleanTitle = title.trim().take(140)
            require(cleanTitle.length >= 2) { "Event title must be at least 2 characters." }
            val cleanDesc = description.trim().take(2000)
            require(cleanDesc.length >= 2) { "Event description is required." }
            val cleanVenue = venueName.trim().take(140)
            require(cleanVenue.isNotEmpty()) { "Public venue name is required." }

            val hasCoords = latitude != null && longitude != null &&
                latitude in -90.0..90.0 && longitude in -180.0..180.0
            val tier = if (isAdmin && adminVerifiedImmediately) {
                EventVerificationTier.ADMIN_VERIFIED.key
            } else {
                EventVerificationTier.UNVERIFIED_USER_SUBMITTED.key
            }
            val cleanPolicy = if (joinPolicy == "REQUEST_APPROVAL") "REQUEST_APPROVAL" else "OPEN"

            val payload = mutableMapOf<String, Any?>(
                "creatorId" to uid,
                "creatorName" to cleanCreator,
                "title" to cleanTitle,
                "category" to category.key,
                "description" to cleanDesc,
                "countryCode" to countryCode.trim().ifBlank { "EG" }.take(10),
                "countryName" to countryName.trim().ifBlank { "Egypt" }.take(80),
                "cityName" to cityName.trim().ifBlank { "Cairo" }.take(80),
                "venueName" to cleanVenue,
                "hasVerifiedCoordinates" to hasCoords,
                "latitude" to if (hasCoords) latitude else null,
                "longitude" to if (hasCoords) longitude else null,
                "startDateIso" to startDateIso.trim().ifBlank { "Upcoming Schedule" }.take(40),
                "endDateIso" to endDateIso.trim().take(40),
                "timeWindowLabel" to timeWindowLabel.trim().take(80),
                "freeVerified" to freeVerified,
                "priceNote" to priceNote.trim().ifBlank { if (freeVerified) "Free entry" else "Check official organizer site" }.take(160),
                "organizerName" to organizerName.trim().ifBlank { cleanCreator }.take(120),
                "organizerType" to organizerType.key,
                "officialUrl" to officialUrl.trim().take(400),
                "conditionsNote" to conditionsNote.trim().take(400),
                "safetyNote" to safetyNote.trim().ifBlank { "Public venue meetup — verify local schedule before attending." }.take(400),
                "ageMinimum" to ageMinimum.coerceIn(0, 99),
                "verificationTier" to tier,
                "lastVerifiedLabel" to if (tier == EventVerificationTier.ADMIN_VERIFIED.key) "Admin verified" else "Pending verification",
                "status" to "UPCOMING",
                "maxParticipants" to maxParticipants.coerceIn(2, 500),
                "participantIds" to listOf(uid),
                "participantNames" to mapOf(uid to cleanCreator),
                "pendingRequestIds" to emptyList<String>(),
                "joinPolicy" to cleanPolicy,
                "linkedAdventureId" to linkedAdventureId.trim().take(80),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            @Suppress("UNCHECKED_CAST")
            ref.set(payload.filterValues { it != null } as Map<String, Any>).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun joinOrRequestEvent(event: RealityEvent, myName: String): Result<String> {
        val docPath = "reality_events/${event.id}"
        return try {
            val uid = requireUserId()
            if (uid in event.participantIds) return Result.success("JOINED")
            if (event.status !in setOf("UPCOMING", "ACTIVE")) {
                return Result.failure(IllegalStateException("This event is ${event.status.lowercase()}."))
            }
            val cleanName = myName.trim().ifBlank { "Participant" }.take(80)
            val ref = db.collection("reality_events").document(event.id)
            if (event.joinPolicy == "REQUEST_APPROVAL") {
                if (uid in event.pendingRequestIds) return Result.success("REQUESTED")
                val updatedPending = (event.pendingRequestIds + uid).distinct()
                ref.update(
                    mapOf(
                        "pendingRequestIds" to updatedPending,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                sendRealityNotification(
                    targetUserId = event.creatorId,
                    title = "Event Join Request",
                    message = "$cleanName requested to join ${event.title}.",
                    category = "REALITY"
                )
                Result.success("REQUESTED")
            } else {
                if (event.participantIds.size >= event.maxParticipants) {
                    return Result.failure(IllegalStateException("Event attendee limit reached."))
                }
                val updatedIds = (event.participantIds + uid).distinct()
                val updatedNames = event.participantNames.toMutableMap().apply { put(uid, cleanName) }
                ref.update(
                    mapOf(
                        "participantIds" to updatedIds,
                        "participantNames" to updatedNames,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success("JOINED")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun leaveEvent(event: RealityEvent): Result<Unit> {
        val docPath = "reality_events/${event.id}"
        return try {
            val uid = requireUserId()
            if (uid == event.creatorId) {
                return Result.failure(IllegalStateException("Event creator cannot leave; update status to CANCELLED instead."))
            }
            val updatedIds = event.participantIds.filter { it != uid }
            val updatedNames = event.participantNames.toMutableMap().apply { remove(uid) }
            db.collection("reality_events").document(event.id)
                .update(
                    mapOf(
                        "participantIds" to updatedIds,
                        "participantNames" to updatedNames,
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

    suspend fun approveEventRequest(
        event: RealityEvent,
        requesterId: String,
        requesterName: String,
        approve: Boolean
    ): Result<Unit> {
        val docPath = "reality_events/${event.id}"
        return try {
            requireUserId()
            val updatedPending = event.pendingRequestIds.filter { it != requesterId }
            val updatedIds = if (approve && event.participantIds.size < event.maxParticipants) {
                (event.participantIds + requesterId).distinct()
            } else {
                event.participantIds
            }
            val updatedNames = event.participantNames.toMutableMap().apply {
                if (approve) put(requesterId, requesterName.ifBlank { "Participant" }.take(80))
            }
            db.collection("reality_events").document(event.id)
                .update(
                    mapOf(
                        "pendingRequestIds" to updatedPending,
                        "participantIds" to updatedIds,
                        "participantNames" to updatedNames,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            sendRealityNotification(
                targetUserId = requesterId,
                title = if (approve) "Event Request Approved" else "Event Request Declined",
                message = "Your request for ${event.title} was ${if (approve) "approved" else "declined"}.",
                category = "REALITY"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateRealityEventStatus(eventId: String, newStatus: String): Result<Unit> {
        val docPath = "reality_events/$eventId"
        return try {
            requireUserId()
            val clean = if (newStatus in setOf("UPCOMING", "ACTIVE", "CANCELLED", "COMPLETED")) newStatus else "ACTIVE"
            db.collection("reality_events").document(eventId)
                .update(mapOf("status" to clean, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun moderateRealityEventVerification(
        eventId: String,
        verificationTier: EventVerificationTier,
        lastVerifiedLabel: String
    ): Result<Unit> {
        val docPath = "reality_events/$eventId"
        return try {
            val uid = requireUserId()
            db.collection("reality_events").document(eventId)
                .update(
                    mapOf(
                        "verificationTier" to verificationTier.key,
                        "lastVerifiedLabel" to lastVerifiedLabel.trim().take(80),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            writeAuditInternal(uid, "MODERATE_REALITY_EVENT", "reality_events", eventId, "Set tier ${verificationTier.key}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    fun observeEventMessages(eventId: String): Flow<List<RealityEventMessage>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("reality_events").document(eventId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snap -> snap.toObjects(RealityEventMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "reality_events/$eventId/messages", uid)
        )
    }

    suspend fun sendEventMessage(
        eventId: String,
        senderName: String,
        text: String,
        sourceLang: String,
        translations: Map<String, String> = emptyMap()
    ): Result<Unit> {
        val docPath = "reality_events/$eventId/messages"
        return try {
            val uid = requireUserId()
            val cleanText = text.trim().take(1500)
            require(cleanText.isNotEmpty()) { "Message cannot be empty." }
            db.collection("reality_events").document(eventId)
                .collection("messages").document()
                .set(
                    mapOf(
                        "eventId" to eventId,
                        "senderId" to uid,
                        "senderName" to senderName.trim().ifBlank { "Participant" }.take(80),
                        "originalText" to cleanText,
                        "sourceLanguage" to sourceLang.ifBlank { "en" }.take(16),
                        "translations" to translations.entries.take(20).associate { it.key.take(16) to it.value.take(1500) },
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun cacheEventMessageTranslation(
        eventId: String,
        messageId: String,
        existingTranslations: Map<String, String>,
        targetLangKey: String,
        translatedText: String
    ): Result<Unit> {
        val docPath = "reality_events/$eventId/messages/$messageId"
        return try {
            requireUserId()
            val merged = existingTranslations.toMutableMap().apply {
                put(targetLangKey.take(16), translatedText.take(1500))
            }
            db.collection("reality_events").document(eventId)
                .collection("messages").document(messageId)
                .update(mapOf("translations" to merged))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- 2. GO / PLAY / TEAM-UP & COMMUNITY IMPACT ACTIVITIES ---

    fun observeTeamActivities(): Flow<List<TeamActivityPost>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("reality_activities")
                .snapshots()
                .map { snap ->
                    snap.toObjects(TeamActivityPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrow(OperationType.LIST, "reality_activities", uid)
        )
    }

    suspend fun createTeamActivity(
        creatorName: String,
        domain: ActivityDomain,
        subCategory: ActivitySubCategory,
        title: String,
        description: String,
        countryName: String,
        cityName: String,
        publicMeetingSpot: String,
        latitude: Double?,
        longitude: Double?,
        scheduledDateLabel: String,
        skillLevel: ActivitySkillLevel,
        equipmentNote: String,
        safetyRules: String,
        ageMinimum: Int,
        joinPolicy: String,
        maxParticipants: Int,
        impactOrganizationName: String = "",
        linkedAdventureId: String = ""
    ): Result<String> {
        var docPath = "reality_activities"
        return try {
            val uid = requireUserId()
            val ref = db.collection("reality_activities").document()
            docPath = ref.path
            val cleanCreator = creatorName.trim().ifBlank { "NILORA Member" }.take(80)
            val cleanTitle = title.trim().take(140)
            val cleanDesc = description.trim().take(2000)
            val cleanSpot = publicMeetingSpot.trim().take(140)
            require(cleanTitle.length >= 2) { "Activity title must be at least 2 characters." }
            require(cleanDesc.length >= 2) { "Activity description is required." }
            require(cleanSpot.length >= 2) { "Public meeting landmark is required." }

            val hasCoords = latitude != null && longitude != null &&
                latitude in -90.0..90.0 && longitude in -180.0..180.0
            val cleanPolicy = if (joinPolicy in setOf("PUBLIC", "REQUEST_APPROVAL", "INVITE_ONLY")) joinPolicy else "PUBLIC"

            val payload = mutableMapOf<String, Any?>(
                "creatorId" to uid,
                "creatorName" to cleanCreator,
                "domain" to domain.key,
                "subCategory" to subCategory.key,
                "title" to cleanTitle,
                "description" to cleanDesc,
                "countryName" to countryName.trim().ifBlank { "Egypt" }.take(80),
                "cityName" to cityName.trim().ifBlank { "Cairo" }.take(80),
                "publicMeetingSpot" to cleanSpot,
                "latitude" to if (hasCoords) latitude else null,
                "longitude" to if (hasCoords) longitude else null,
                "scheduledDateLabel" to scheduledDateLabel.trim().ifBlank { "Flexible this week" }.take(60),
                "skillLevel" to skillLevel.key,
                "equipmentNote" to equipmentNote.trim().take(300),
                "safetyRules" to safetyRules.trim().ifBlank {
                    "Meet in a well-lit public venue only. Never share private home addresses."
                }.take(400),
                "ageMinimum" to ageMinimum.coerceIn(13, 99),
                "joinPolicy" to cleanPolicy,
                "teamStatus" to TeamUpStatus.LOOKING_FOR_PARTICIPANTS.key,
                "maxParticipants" to maxParticipants.coerceIn(2, 50),
                "participantIds" to listOf(uid),
                "participantNames" to mapOf(uid to cleanCreator),
                "pendingRequestIds" to emptyList<String>(),
                "invitedUserIds" to emptyList<String>(),
                "impactOrganizationName" to impactOrganizationName.trim().take(120),
                "impactVerifiedByAdmin" to false,
                "linkedAdventureId" to linkedAdventureId.trim().take(80),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            @Suppress("UNCHECKED_CAST")
            ref.set(payload.filterValues { it != null } as Map<String, Any>).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun joinOrRequestActivity(activity: TeamActivityPost, myName: String): Result<String> {
        val docPath = "reality_activities/${activity.id}"
        return try {
            val uid = requireUserId()
            if (uid in activity.participantIds) return Result.success("JOINED")
            if (activity.teamStatus !in setOf(TeamUpStatus.LOOKING_FOR_PARTICIPANTS.key, TeamUpStatus.GROUP_FORMING.key)) {
                return Result.failure(IllegalStateException("This activity is ${activity.teamStatus.lowercase().replace('_', ' ')}."))
            }
            val cleanName = myName.trim().ifBlank { "Participant" }.take(80)
            val ref = db.collection("reality_activities").document(activity.id)
            if (activity.joinPolicy == "REQUEST_APPROVAL") {
                if (uid in activity.pendingRequestIds) return Result.success("REQUESTED")
                val updatedPending = (activity.pendingRequestIds + uid).distinct()
                ref.update(
                    mapOf(
                        "pendingRequestIds" to updatedPending,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                sendRealityNotification(
                    targetUserId = activity.creatorId,
                    title = "Team-Up Join Request",
                    message = "$cleanName wants to join ${activity.title}.",
                    category = "REALITY"
                )
                Result.success("REQUESTED")
            } else {
                if (activity.joinPolicy == "INVITE_ONLY" && uid !in activity.invitedUserIds) {
                    return Result.failure(IllegalStateException("This activity is invite-only."))
                }
                if (activity.participantIds.size >= activity.maxParticipants) {
                    return Result.failure(IllegalStateException("Group is already full."))
                }
                val updatedIds = (activity.participantIds + uid).distinct()
                val updatedNames = activity.participantNames.toMutableMap().apply { put(uid, cleanName) }
                val nextStatus = if (updatedIds.size >= activity.maxParticipants) {
                    TeamUpStatus.GROUP_FULL.key
                } else {
                    TeamUpStatus.GROUP_FORMING.key
                }
                ref.update(
                    mapOf(
                        "participantIds" to updatedIds,
                        "participantNames" to updatedNames,
                        "teamStatus" to nextStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success("JOINED")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun leaveActivity(activity: TeamActivityPost): Result<Unit> {
        val docPath = "reality_activities/${activity.id}"
        return try {
            val uid = requireUserId()
            if (uid == activity.creatorId) {
                return Result.failure(IllegalStateException("Organizer cannot leave; set status to Cancelled instead."))
            }
            val updatedIds = activity.participantIds.filter { it != uid }
            val updatedNames = activity.participantNames.toMutableMap().apply { remove(uid) }
            val nextStatus = when {
                activity.teamStatus in setOf(TeamUpStatus.ACTIVITY_CONFIRMED.key, TeamUpStatus.ACTIVITY_COMPLETED.key, TeamUpStatus.ACTIVITY_CANCELLED.key) -> activity.teamStatus
                updatedIds.size <= 1 -> TeamUpStatus.LOOKING_FOR_PARTICIPANTS.key
                else -> TeamUpStatus.GROUP_FORMING.key
            }
            db.collection("reality_activities").document(activity.id)
                .update(
                    mapOf(
                        "participantIds" to updatedIds,
                        "participantNames" to updatedNames,
                        "teamStatus" to nextStatus,
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

    suspend fun approveActivityRequest(
        activity: TeamActivityPost,
        requesterId: String,
        requesterName: String,
        approve: Boolean
    ): Result<Unit> {
        val docPath = "reality_activities/${activity.id}"
        return try {
            requireUserId()
            val updatedPending = activity.pendingRequestIds.filter { it != requesterId }
            val updatedIds = if (approve && activity.participantIds.size < activity.maxParticipants) {
                (activity.participantIds + requesterId).distinct()
            } else {
                activity.participantIds
            }
            val updatedNames = activity.participantNames.toMutableMap().apply {
                if (approve) put(requesterId, requesterName.ifBlank { "Teammate" }.take(80))
            }
            val nextStatus = when {
                updatedIds.size >= activity.maxParticipants -> TeamUpStatus.GROUP_FULL.key
                updatedIds.size > 1 -> TeamUpStatus.GROUP_FORMING.key
                else -> activity.teamStatus
            }
            db.collection("reality_activities").document(activity.id)
                .update(
                    mapOf(
                        "pendingRequestIds" to updatedPending,
                        "participantIds" to updatedIds,
                        "participantNames" to updatedNames,
                        "teamStatus" to nextStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            sendRealityNotification(
                targetUserId = requesterId,
                title = if (approve) "Team-Up Accepted" else "Team-Up Request Declined",
                message = "Your request for ${activity.title} was ${if (approve) "accepted" else "declined"}.",
                category = "REALITY"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateActivityStatus(activityId: String, status: TeamUpStatus): Result<Unit> {
        val docPath = "reality_activities/$activityId"
        return try {
            requireUserId()
            db.collection("reality_activities").document(activityId)
                .update(mapOf("teamStatus" to status.key, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun moderateImpactVerification(activityId: String, verified: Boolean): Result<Unit> {
        val docPath = "reality_activities/$activityId"
        return try {
            val uid = requireUserId()
            db.collection("reality_activities").document(activityId)
                .update(
                    mapOf(
                        "impactVerifiedByAdmin" to verified,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            writeAuditInternal(uid, "MODERATE_IMPACT_ACTIVITY", "reality_activities", activityId, "impactVerifiedByAdmin=$verified")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    fun observeActivityMessages(activityId: String): Flow<List<ActivityGroupMessage>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("reality_activities").document(activityId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snap -> snap.toObjects(ActivityGroupMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "reality_activities/$activityId/messages", uid)
        )
    }

    suspend fun sendActivityMessage(
        activityId: String,
        senderName: String,
        text: String,
        sourceLang: String,
        translations: Map<String, String> = emptyMap()
    ): Result<Unit> {
        val docPath = "reality_activities/$activityId/messages"
        return try {
            val uid = requireUserId()
            val cleanText = text.trim().take(1500)
            require(cleanText.isNotEmpty()) { "Message cannot be empty." }
            db.collection("reality_activities").document(activityId)
                .collection("messages").document()
                .set(
                    mapOf(
                        "activityId" to activityId,
                        "senderId" to uid,
                        "senderName" to senderName.trim().ifBlank { "Member" }.take(80),
                        "originalText" to cleanText,
                        "sourceLanguage" to sourceLang.ifBlank { "en" }.take(16),
                        "translations" to translations.entries.take(20).associate { it.key.take(16) to it.value.take(1500) },
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun cacheActivityMessageTranslation(
        activityId: String,
        messageId: String,
        existingTranslations: Map<String, String>,
        targetLangKey: String,
        translatedText: String
    ): Result<Unit> {
        val docPath = "reality_activities/$activityId/messages/$messageId"
        return try {
            requireUserId()
            val merged = existingTranslations.toMutableMap().apply {
                put(targetLangKey.take(16), translatedText.take(1500))
            }
            db.collection("reality_activities").document(activityId)
                .collection("messages").document(messageId)
                .update(mapOf("translations" to merged))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- 3. REAL-WORLD MISSIONS (STRICTLY SEPARATED FROM VIRTUAL EXPLORATION) ---

    fun observeMissions(): Flow<List<RealityMission>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("reality_missions")
                .snapshots()
                .map { snap ->
                    snap.toObjects(RealityMission::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrow(OperationType.LIST, "reality_missions", uid)
        )
    }

    suspend fun createMission(
        creatorName: String,
        templateType: String,
        executionMode: MissionExecutionMode,
        title: String,
        objective: String,
        cityName: String,
        countryName: String,
        latitude: Double?,
        longitude: Double?,
        safetyAndEcoRules: String,
        targetSteps: Int
    ): Result<String> {
        var docPath = "reality_missions"
        return try {
            val uid = requireUserId()
            val ref = db.collection("reality_missions").document()
            docPath = ref.path
            val cleanTitle = title.trim().take(120)
            val cleanObjective = objective.trim().take(500)
            require(cleanTitle.length >= 2) { "Mission title must be at least 2 characters." }
            require(cleanObjective.length >= 4) { "Mission objective must be at least 4 characters." }
            val hasCoords = latitude != null && longitude != null &&
                latitude in -90.0..90.0 && longitude in -180.0..180.0

            val payload = mutableMapOf<String, Any?>(
                "creatorId" to uid,
                "creatorName" to creatorName.trim().ifBlank { "Explorer" }.take(80),
                "templateType" to templateType.trim().ifBlank { "CUSTOM" }.take(48),
                "executionMode" to executionMode.key,
                "title" to cleanTitle,
                "objective" to cleanObjective,
                "cityName" to cityName.trim().ifBlank { "Cairo" }.take(80),
                "countryName" to countryName.trim().ifBlank { "Egypt" }.take(80),
                "latitude" to if (hasCoords) latitude else null,
                "longitude" to if (hasCoords) longitude else null,
                "safetyAndEcoRules" to safetyAndEcoRules.trim().ifBlank {
                    "Stay in public spaces, respect local customs, and pack out all litter."
                }.take(500),
                "targetSteps" to targetSteps.coerceIn(1, 30),
                "participantIds" to listOf(uid),
                "submissionsCount" to 0,
                "status" to "ACTIVE",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            @Suppress("UNCHECKED_CAST")
            ref.set(payload.filterValues { it != null } as Map<String, Any>).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    fun observeMissionSubmissions(missionId: String): Flow<List<MissionSubmission>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("reality_missions").document(missionId)
                .collection("submissions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap -> snap.toObjects(MissionSubmission::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "reality_missions/$missionId/submissions", uid)
        )
    }

    suspend fun submitMissionEvidence(
        mission: RealityMission,
        authorName: String,
        reflectionText: String,
        placeLabel: String,
        photoBase64: String,
        ecoSafetyConfirmed: Boolean
    ): Result<Unit> {
        val docPath = "reality_missions/${mission.id}/submissions"
        return try {
            val uid = requireUserId()
            require(ecoSafetyConfirmed) { "Please confirm that you followed public safety and environmental rules." }
            val cleanReflection = reflectionText.trim().take(1500)
            val cleanPlace = placeLabel.trim().take(120)
            require(cleanReflection.isNotEmpty()) { "Observation or reflection is required." }
            require(cleanPlace.isNotEmpty()) { "Public place label is required." }

            // Enforce rule separation: VIRTUAL missions never record REAL_WORLD_LOG
            val effectiveMode = if (mission.executionMode == MissionExecutionMode.VIRTUAL.key) {
                "VIRTUAL_PROGRESS"
            } else {
                "REAL_WORLD_LOG"
            }

            val missionRef = db.collection("reality_missions").document(mission.id)
            val subRef = missionRef.collection("submissions").document()
            subRef.set(
                mapOf(
                    "missionId" to mission.id,
                    "authorId" to uid,
                    "authorName" to authorName.trim().ifBlank { "Explorer" }.take(80),
                    "submissionMode" to effectiveMode,
                    "reflectionText" to cleanReflection,
                    "placeLabel" to cleanPlace,
                    "photoBase64" to photoBase64.take(220000),
                    "ecoSafetyConfirmed" to true,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val updatedParticipants = (mission.participantIds + uid).distinct()
            val nextCount = (mission.submissionsCount + 1).coerceAtMost(500)
            val nextStatus = if (nextCount >= mission.targetSteps) "COMPLETED" else "ACTIVE"
            missionRef.update(
                mapOf(
                    "participantIds" to updatedParticipants,
                    "submissionsCount" to nextCount,
                    "status" to nextStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    // --- 4. GLOBAL CHALLENGES ---

    fun observeChallenges(): Flow<List<GlobalChallenge>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("global_challenges")
                .snapshots()
                .map { snap ->
                    snap.toObjects(GlobalChallenge::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrow(OperationType.LIST, "global_challenges", uid)
        )
    }

    suspend fun createChallenge(
        creatorName: String,
        creatorCountry: String,
        scope: ChallengeScope,
        category: String,
        title: String,
        description: String,
        targetGoal: Int,
        cityName: String,
        countryName: String,
        latitude: Double?,
        longitude: Double?
    ): Result<String> {
        var docPath = "global_challenges"
        return try {
            val uid = requireUserId()
            val ref = db.collection("global_challenges").document()
            docPath = ref.path
            val cleanTitle = title.trim().take(120)
            val cleanDesc = description.trim().take(600)
            require(cleanTitle.length >= 2) { "Challenge title is required." }
            require(cleanDesc.length >= 4) { "Challenge description must be at least 4 characters." }
            val hasCoords = latitude != null && longitude != null &&
                latitude in -90.0..90.0 && longitude in -180.0..180.0

            val payload = mutableMapOf<String, Any?>(
                "creatorId" to uid,
                "creatorName" to creatorName.trim().ifBlank { "Challenger" }.take(80),
                "challengeScope" to scope.key,
                "category" to category.trim().ifBlank { "COMMUNITY_IMPACT" }.take(48),
                "title" to cleanTitle,
                "description" to cleanDesc,
                "targetGoal" to targetGoal.coerceIn(1, 500),
                "participantIds" to listOf(uid),
                "participantCountries" to listOf(creatorCountry.trim().ifBlank { "Egypt" }.take(80)),
                "entriesCount" to 0,
                "cityName" to cityName.trim().ifBlank { "Cairo" }.take(80),
                "countryName" to countryName.trim().ifBlank { "Egypt" }.take(80),
                "latitude" to if (hasCoords) latitude else null,
                "longitude" to if (hasCoords) longitude else null,
                "status" to "ACTIVE",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            @Suppress("UNCHECKED_CAST")
            ref.set(payload.filterValues { it != null } as Map<String, Any>).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    fun observeChallengeEntries(challengeId: String): Flow<List<ChallengeEntry>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("global_challenges").document(challengeId)
                .collection("entries")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap -> snap.toObjects(ChallengeEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "global_challenges/$challengeId/entries", uid)
        )
    }

    suspend fun submitChallengeEntry(
        challenge: GlobalChallenge,
        authorName: String,
        countryName: String,
        teamName: String,
        summaryText: String,
        pointsClaimed: Int
    ): Result<Unit> {
        val docPath = "global_challenges/${challenge.id}/entries"
        return try {
            val uid = requireUserId()
            val cleanSummary = summaryText.trim().take(1000)
            require(cleanSummary.isNotEmpty()) { "Challenge entry summary is required." }
            val cleanCountry = countryName.trim().ifBlank { "Egypt" }.take(80)

            val chalRef = db.collection("global_challenges").document(challenge.id)
            chalRef.collection("entries").document()
                .set(
                    mapOf(
                        "challengeId" to challenge.id,
                        "authorId" to uid,
                        "authorName" to authorName.trim().ifBlank { "Member" }.take(80),
                        "countryName" to cleanCountry,
                        "teamName" to teamName.trim().take(80),
                        "summaryText" to cleanSummary,
                        "pointsClaimed" to pointsClaimed.coerceIn(1, 10),
                        "verificationLabel" to "USER_SUBMITTED",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()

            val nextParticipants = (challenge.participantIds + uid).distinct()
            val nextCountries = (challenge.participantCountries + cleanCountry).distinct().take(100)
            val nextEntries = (challenge.entriesCount + 1).coerceAtMost(1000)
            val nextStatus = if (nextEntries >= challenge.targetGoal) "COMPLETED" else "ACTIVE"
            chalRef.update(
                mapOf(
                    "participantIds" to nextParticipants,
                    "participantCountries" to nextCountries,
                    "entriesCount" to nextEntries,
                    "status" to nextStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    // --- 5. REALITY BRIDGE (VIRTUAL -> PLANNED -> REGISTERED -> CONFIRMED -> COMPLETED) ---

    fun observeMyRealityBridge(userId: String): Flow<List<RealityBridgeLink>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("users").document(userId)
                .collection("reality_bridge")
                .snapshots()
                .map { snap ->
                    snap.toObjects(RealityBridgeLink::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrow(OperationType.LIST, "users/$userId/reality_bridge", uid)
        )
    }

    suspend fun upsertRealityBridge(
        virtualAdventureId: String,
        virtualAdventureTitle: String,
        destinationName: String,
        targetType: String,
        targetId: String,
        targetTitle: String,
        stage: RealityBridgeStage,
        notes: String = ""
    ): Result<String> {
        var docPath = "users/unknown/reality_bridge"
        return try {
            val uid = requireUserId()
            val validTargetType = if (targetType in setOf("REALITY_EVENT", "TEAM_ACTIVITY", "BOOKING_ACTIVITY", "REAL_WORLD_MISSION", "NILORA_MEMORY")) {
                targetType
            } else {
                "REALITY_EVENT"
            }
            val safeId = "${virtualAdventureId.take(32)}_${validTargetType.take(16)}_${targetId.take(40)}"
                .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            val ref = db.collection("users").document(uid)
                .collection("reality_bridge").document(safeId)
            docPath = ref.path
            val existing = ref.get().await()
            if (!existing.exists()) {
                ref.set(
                    mapOf(
                        "userId" to uid,
                        "virtualAdventureId" to virtualAdventureId.ifBlank { "custom_world" }.take(80),
                        "virtualAdventureTitle" to virtualAdventureTitle.ifBlank { "NILORA World" }.take(120),
                        "destinationName" to destinationName.ifBlank { "Cairo" }.take(100),
                        "targetType" to validTargetType,
                        "targetId" to targetId.ifBlank { safeId }.take(128),
                        "targetTitle" to targetTitle.ifBlank { "Real-World Plan" }.take(140),
                        "stage" to stage.key,
                        "notes" to notes.trim().take(500),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.update(
                    mapOf(
                        "stage" to stage.key,
                        "targetTitle" to targetTitle.ifBlank { "Real-World Plan" }.take(140),
                        "notes" to notes.trim().take(500),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateBridgeStage(bridgeId: String, newStage: RealityBridgeStage, notes: String? = null): Result<Unit> {
        var docPath = "users/unknown/reality_bridge/$bridgeId"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("reality_bridge").document(bridgeId)
            docPath = ref.path
            val updates = mutableMapOf<String, Any>(
                "stage" to newStage.key,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (notes != null) updates["notes"] = notes.trim().take(500)
            ref.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteBridgeItem(bridgeId: String): Result<Unit> {
        var docPath = "users/unknown/reality_bridge/$bridgeId"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("reality_bridge").document(bridgeId)
            docPath = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- SHARED HELPERS ---

    suspend fun sendRealityNotification(
        targetUserId: String,
        title: String,
        message: String,
        category: String = "REALITY"
    ) {
        try {
            val uid = auth.currentUser?.uid ?: return
            if (targetUserId.isBlank() || targetUserId == uid) return
            db.collection("users").document(targetUserId)
                .collection("notifications").document()
                .set(
                    mapOf(
                        "userId" to targetUserId,
                        "title" to title.trim().take(120),
                        "message" to message.trim().take(500),
                        "category" to category,
                        "readStatus" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Non-blocking notification failure should not break primary workflow
        }
    }

    private suspend fun writeAuditInternal(
        actorId: String,
        action: String,
        targetCollection: String,
        targetId: String,
        summary: String
    ) {
        try {
            db.collection("audit_logs").document().set(
                mapOf(
                    "actorId" to actorId,
                    "actorEmail" to (auth.currentUser?.email ?: "").take(200),
                    "action" to action.take(100),
                    "targetCollection" to targetCollection.take(80),
                    "targetId" to targetId.take(128),
                    "summary" to summary.take(500),
                    "severity" to "INFO",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }
}
