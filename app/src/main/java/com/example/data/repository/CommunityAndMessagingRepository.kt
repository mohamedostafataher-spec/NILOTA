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

/**
 * Firestore repository for NILORA Phase 2:
 * - Discoverable global profiles (public_profiles)
 * - Blocking (users/{uid}/blocked)
 * - Connection / language-exchange requests (connection_requests)
 * - 1:1 Direct conversations & voice/text messages (direct_conversations)
 * - Multilingual group conversations & moderation (groups, join_requests, messages)
 */
class CommunityAndMessagingRepository(
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
            ?: throw IllegalStateException("Please sign in with Google to access NILORA global messaging and community.")

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

    // --- GLOBAL DISCOVERY ---
    fun observePublicProfiles(): Flow<List<PublicProfile>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("public_profiles")
                .whereEqualTo("profileVisibility", "PUBLIC")
                .snapshots()
                .map { snap ->
                    snap.toObjects(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .logAndRethrow(OperationType.LIST, "public_profiles", uid)
        )
    }

    suspend fun getPublicProfile(userId: String): Result<PublicProfile?> = try {
        requireUserId()
        val snap = db.collection("public_profiles").document(userId).get().await()
        Result.success(snap.toObject(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.GET, "public_profiles/$userId")
        Result.failure(e)
    }

    // --- BLOCKING ---
    fun observeBlockedUsers(): Flow<List<BlockedUserRecord>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("users").document(uid).collection("blocked")
                .snapshots()
                .map { snap -> snap.toObjects(BlockedUserRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "users/$uid/blocked", uid)
        )
    }

    suspend fun blockUser(blockedUserId: String, blockedUserName: String, reason: String = "User blocked"): Result<Unit> {
        var docPath = "users/unknown/blocked/$blockedUserId"
        return try {
            val uid = requireUserId()
            require(uid != blockedUserId) { "You cannot block yourself." }
            val ref = db.collection("users").document(uid).collection("blocked").document(blockedUserId)
            docPath = ref.path
            ref.set(
                mapOf(
                    "blockerId" to uid,
                    "blockedUserId" to blockedUserId,
                    "blockedUserName" to blockedUserName.trim().take(80),
                    "reason" to reason.trim().take(200),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun unblockUser(blockedUserId: String): Result<Unit> {
        var docPath = "users/unknown/blocked/$blockedUserId"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("blocked").document(blockedUserId)
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

    // --- CONNECTION REQUESTS ---
    fun observeConnectionRequests(): Flow<List<ConnectionRequest>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("connection_requests")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ConnectionRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .logAndRethrow(OperationType.LIST, "connection_requests", uid)
        )
    }

    suspend fun sendConnectionRequest(
        target: PublicProfile,
        myProfile: UserProfile?,
        introMessage: String
    ): Result<Unit> {
        var docPath = "connection_requests"
        return try {
            val uid = requireUserId()
            require(uid != target.userId) { "Cannot send a connection request to yourself." }
            val reqId = pairDocId(uid, target.userId)
            val ref = db.collection("connection_requests").document(reqId)
            docPath = ref.path
            val existing = ref.get().await()
            if (existing.exists()) {
                val curStatus = existing.getString("status").orEmpty()
                if (curStatus == "ACCEPTED") {
                    return Result.success(Unit)
                }
                if (curStatus == "PENDING") {
                    return Result.failure(IllegalStateException("A connection request is already pending."))
                }
                ref.delete().await()
            }
            val senderName = myProfile?.displayName?.ifBlank { auth.currentUser?.displayName ?: "NILORA Member" }
                ?: auth.currentUser?.displayName ?: "NILORA Member"
            ref.set(
                mapOf(
                    "senderId" to uid,
                    "senderName" to senderName.take(80),
                    "senderCountry" to (myProfile?.country ?: "Egypt").take(80),
                    "senderNativeLanguage" to (myProfile?.nativeLanguage?.ifBlank { myProfile.preferredLanguage } ?: "en").take(16),
                    "receiverId" to target.userId,
                    "receiverName" to target.displayName.ifBlank { "Member" }.take(80),
                    "receiverCountry" to target.country.take(80),
                    "receiverNativeLanguage" to target.nativeLanguage.ifBlank { "en" }.take(16),
                    "memberUids" to listOf(uid, target.userId).sorted(),
                    "introMessage" to introMessage.trim().take(300),
                    "status" to "PENDING",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            sendCommunityNotificationSafely(
                targetUserId = target.userId,
                title = "New NILORA Connection Request",
                message = "$senderName wants to connect with you.",
                category = "COMMUNITY"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun respondToConnectionRequest(
        request: ConnectionRequest,
        newStatus: String,
        myProfile: UserProfile?
    ): Result<String?> {
        val docPath = "connection_requests/${request.id}"
        return try {
            val uid = requireUserId()
            db.collection("connection_requests").document(request.id)
                .update(
                    mapOf(
                        "status" to newStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

            var conversationId: String? = null
            if (newStatus == "ACCEPTED") {
                val otherId = if (request.senderId == uid) request.receiverId else request.senderId
                val otherName = if (request.senderId == uid) request.receiverName else request.senderName
                val otherLang = if (request.senderId == uid) request.receiverNativeLanguage else request.senderNativeLanguage
                conversationId = ensureDirectConversationInternal(
                    myUid = uid,
                    myName = myProfile?.displayName?.ifBlank { "NILORA Member" } ?: "NILORA Member",
                    myLang = myProfile?.nativeLanguage?.ifBlank { myProfile.preferredLanguage } ?: "en",
                    partnerId = otherId,
                    partnerName = otherName,
                    partnerLang = otherLang
                )
                sendCommunityNotificationSafely(
                    targetUserId = request.senderId,
                    title = "Connection Accepted",
                    message = "${request.receiverName} accepted your connection request.",
                    category = "COMMUNITY"
                )
            }
            Result.success(conversationId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- 1:1 DIRECT MESSAGING ---
    fun observeMyDirectConversations(): Flow<List<DirectConversation>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("direct_conversations")
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(DirectConversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .logAndRethrow(OperationType.LIST, "direct_conversations", uid)
        )
    }

    fun observeDirectMessages(conversationId: String): Flow<List<DirectMessage>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("direct_conversations").document(conversationId).collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snap -> snap.toObjects(DirectMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "direct_conversations/$conversationId/messages", uid)
        )
    }

    suspend fun openOrCreateDirectConversation(partner: PublicProfile, myProfile: UserProfile?): Result<String> {
        var docPath = "direct_conversations"
        return try {
            val uid = requireUserId()
            require(uid != partner.userId) { "Cannot start a 1:1 chat with yourself." }
            docPath = "direct_conversations/${pairDocId(uid, partner.userId)}"
            val convId = ensureDirectConversationInternal(
                myUid = uid,
                myName = myProfile?.displayName?.ifBlank { auth.currentUser?.displayName ?: "NILORA Member" } ?: "NILORA Member",
                myLang = myProfile?.nativeLanguage?.ifBlank { myProfile.preferredLanguage } ?: "en",
                partnerId = partner.userId,
                partnerName = partner.displayName.ifBlank { "Member" },
                partnerLang = partner.nativeLanguage.ifBlank { "en" }
            )
            Result.success(convId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    private suspend fun ensureDirectConversationInternal(
        myUid: String,
        myName: String,
        myLang: String,
        partnerId: String,
        partnerName: String,
        partnerLang: String
    ): String {
        val convId = pairDocId(myUid, partnerId)
        val ref = db.collection("direct_conversations").document(convId)
        val snap = ref.get().await()
        val sortedIds = listOf(myUid, partnerId).sorted()
        val namesMap = mapOf(myUid to myName.take(80), partnerId to partnerName.take(80))
        val langsMap = mapOf(myUid to myLang.take(16), partnerId to partnerLang.take(16))
        if (!snap.exists()) {
            ref.set(
                mapOf(
                    "participantIds" to sortedIds,
                    "participantNames" to namesMap,
                    "participantLanguages" to langsMap,
                    "lastMessagePreview" to "",
                    "lastMessageSenderId" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            ref.update(
                mapOf(
                    "participantNames" to namesMap,
                    "participantLanguages" to langsMap,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        return convId
    }

    suspend fun sendDirectMessage(
        conversation: DirectConversation,
        senderName: String,
        originalText: String,
        sourceLanguage: String,
        initialTranslations: Map<String, String> = emptyMap(),
        translationConfidence: String = "",
        translationNote: String = "",
        messageType: String = "TEXT",
        audioBase64: String = "",
        audioDurationMs: Long = 0L,
        audioMimeType: String = ""
    ): Result<String> {
        val docPath = "direct_conversations/${conversation.id}/messages"
        return try {
            val uid = requireUserId()
            val cleanText = originalText.trim().take(2000)
            require(cleanText.isNotBlank()) { "Message text cannot be empty." }
            val convRef = db.collection("direct_conversations").document(conversation.id)
            val msgRef = convRef.collection("messages").document()

            val payload = mapOf(
                "conversationId" to conversation.id,
                "participantIds" to conversation.participantIds,
                "senderId" to uid,
                "senderName" to senderName.trim().ifBlank { "Member" }.take(80),
                "messageType" to messageType,
                "originalText" to cleanText,
                "sourceLanguage" to sourceLanguage.ifBlank { "en" }.take(16),
                "translations" to initialTranslations.entries.take(20).associate { it.key.take(16) to it.value.take(2000) },
                "translationConfidence" to translationConfidence.take(16),
                "translationNote" to translationNote.take(240),
                "audioBase64" to audioBase64.take(95000),
                "audioDurationMs" to audioDurationMs.coerceIn(0L, 30000L),
                "audioMimeType" to audioMimeType.take(40),
                "deliveryStatus" to "SENT",
                "createdAt" to FieldValue.serverTimestamp()
            )
            msgRef.set(payload).await()

            val preview = if (messageType == "VOICE_NOTE") "[Voice Note] $cleanText" else cleanText
            convRef.update(
                mapOf(
                    "lastMessagePreview" to preview.take(300),
                    "lastMessageSenderId" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val partnerId = conversation.partnerId(uid)
            if (partnerId.isNotBlank()) {
                sendCommunityNotificationSafely(
                    targetUserId = partnerId,
                    title = "Message from ${senderName.ifBlank { "NILORA Member" }}",
                    message = preview.take(200),
                    category = "COMMUNITY"
                )
            }
            Result.success(msgRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun cacheDirectMessageTranslation(
        conversationId: String,
        messageId: String,
        mergedTranslations: Map<String, String>,
        confidence: String,
        note: String
    ): Result<Unit> {
        val docPath = "direct_conversations/$conversationId/messages/$messageId"
        return try {
            requireUserId()
            db.collection("direct_conversations").document(conversationId)
                .collection("messages").document(messageId)
                .update(
                    mapOf(
                        "translations" to mergedTranslations.entries.take(20).associate { it.key.take(16) to it.value.take(2000) },
                        "translationConfidence" to confidence.take(16),
                        "translationNote" to note.take(240)
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

    suspend fun markDirectMessageRead(conversationId: String, messageId: String): Result<Unit> {
        val docPath = "direct_conversations/$conversationId/messages/$messageId"
        return try {
            requireUserId()
            db.collection("direct_conversations").document(conversationId)
                .collection("messages").document(messageId)
                .update("deliveryStatus", "READ")
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Best-effort read receipt update
            Result.success(Unit)
        }
    }

    // --- MULTILINGUAL GROUP CONVERSATIONS ---
    fun observeDiscoverableGroups(): Flow<List<GroupRoom>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("groups")
                .whereIn("joinPolicy", listOf("OPEN", "REQUEST_APPROVAL"))
                .snapshots()
                .map { snap ->
                    snap.toObjects(GroupRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { uid !in it.bannedIds }
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .logAndRethrow(OperationType.LIST, "groups", uid)
        )
    }

    fun observeMyGroups(): Flow<List<GroupRoom>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("groups")
                .whereArrayContains("memberIds", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(GroupRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .logAndRethrow(OperationType.LIST, "groups", uid)
        )
    }

    fun observeGroup(groupId: String): Flow<GroupRoom?> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("groups").document(groupId)
                .snapshots()
                .map { it.toObject(GroupRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.GET, "groups/$groupId", uid)
        )
    }

    fun observeGroupMessages(groupId: String): Flow<List<GroupMessage>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("groups").document(groupId).collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snap -> snap.toObjects(GroupMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "groups/$groupId/messages", uid)
        )
    }

    fun observeGroupJoinRequests(groupId: String): Flow<List<GroupJoinRequest>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("groups").document(groupId).collection("join_requests")
                .snapshots()
                .map { snap -> snap.toObjects(GroupJoinRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "groups/$groupId/join_requests", uid)
        )
    }

    suspend fun createGroup(
        name: String,
        description: String,
        primaryLanguages: List<String>,
        joinPolicy: String,
        autoTranslateDefault: Boolean
    ): Result<String> {
        var docPath = "groups"
        return try {
            val uid = requireUserId()
            val cleanName = name.trim().take(80)
            require(cleanName.length >= 2) { "Group name must be at least 2 characters." }
            val ref = db.collection("groups").document()
            docPath = ref.path
            val policy = if (joinPolicy in listOf("OPEN", "REQUEST_APPROVAL", "INVITE_ONLY")) joinPolicy else "OPEN"
            val langs = primaryLanguages.map { it.trim().take(16) }.filter { it.isNotBlank() }.distinct().take(10)
                .ifEmpty { listOf("en", "ar-EG") }
            ref.set(
                mapOf(
                    "name" to cleanName,
                    "description" to description.trim().take(400),
                    "ownerId" to uid,
                    "moderatorIds" to listOf(uid),
                    "memberIds" to listOf(uid),
                    "bannedIds" to emptyList<String>(),
                    "primaryLanguages" to langs,
                    "joinPolicy" to policy,
                    "autoTranslateDefault" to autoTranslateDefault,
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

    suspend fun joinOpenGroup(group: GroupRoom): Result<Unit> {
        val docPath = "groups/${group.id}"
        return try {
            val uid = requireUserId()
            if (uid in group.memberIds) return Result.success(Unit)
            require(uid !in group.bannedIds) { "You cannot join this group because you were removed by a moderator." }
            require(group.joinPolicy == "OPEN") { "This group requires approval or an invite." }
            db.collection("groups").document(group.id)
                .update(
                    mapOf(
                        "memberIds" to (group.memberIds + uid).distinct(),
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

    suspend fun requestJoinGroup(group: GroupRoom, myProfile: UserProfile?, message: String): Result<Unit> {
        var docPath = "groups/${group.id}/join_requests"
        return try {
            val uid = requireUserId()
            val ref = db.collection("groups").document(group.id).collection("join_requests").document(uid)
            docPath = ref.path
            val existing = ref.get().await()
            if (existing.exists()) {
                val st = existing.getString("status").orEmpty()
                if (st == "PENDING") {
                    return Result.failure(IllegalStateException("Your join request is already pending moderator review."))
                }
                ref.delete().await()
            }
            val senderName = myProfile?.displayName?.ifBlank { auth.currentUser?.displayName ?: "Member" } ?: "Member"
            ref.set(
                mapOf(
                    "groupId" to group.id,
                    "userId" to uid,
                    "userName" to senderName.take(80),
                    "userCountry" to (myProfile?.country ?: "Egypt").take(80),
                    "nativeLanguage" to (myProfile?.nativeLanguage?.ifBlank { myProfile.preferredLanguage } ?: "en").take(16),
                    "message" to message.trim().take(300),
                    "requestType" to "JOIN_REQUEST",
                    "status" to "PENDING",
                    "invitedBy" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            sendCommunityNotificationSafely(
                targetUserId = group.ownerId,
                title = "Group Join Request",
                message = "$senderName requested to join ${group.name}.",
                category = "GROUP"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun inviteUserToGroup(group: GroupRoom, target: PublicProfile): Result<Unit> {
        var docPath = "groups/${group.id}/join_requests"
        return try {
            val uid = requireUserId()
            val ref = db.collection("groups").document(group.id).collection("join_requests").document(target.userId)
            docPath = ref.path
            val existing = ref.get().await()
            if (existing.exists()) {
                ref.delete().await()
            }
            ref.set(
                mapOf(
                    "groupId" to group.id,
                    "userId" to target.userId,
                    "userName" to target.displayName.ifBlank { "Member" }.take(80),
                    "userCountry" to target.country.take(80),
                    "nativeLanguage" to target.nativeLanguage.ifBlank { "en" }.take(16),
                    "message" to "Invited to join ${group.name}",
                    "requestType" to "INVITE",
                    "status" to "PENDING",
                    "invitedBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            // Add user directly if owner/mod invited them so they can enter immediately cleanly
            if (target.userId !in group.memberIds && target.userId !in group.bannedIds) {
                db.collection("groups").document(group.id)
                    .update(
                        mapOf(
                            "memberIds" to (group.memberIds + target.userId).distinct(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                ref.update(mapOf("status" to "APPROVED", "updatedAt" to FieldValue.serverTimestamp())).await()
            }
            sendCommunityNotificationSafely(
                targetUserId = target.userId,
                title = "Invited to ${group.name}",
                message = "You have been added to multilingual group '${group.name}'.",
                category = "GROUP"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun reviewGroupJoinRequest(group: GroupRoom, req: GroupJoinRequest, approve: Boolean): Result<Unit> {
        val docPath = "groups/${group.id}/join_requests/${req.userId}"
        return try {
            requireUserId()
            db.collection("groups").document(group.id).collection("join_requests").document(req.userId)
                .update(
                    mapOf(
                        "status" to if (approve) "APPROVED" else "DECLINED",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            if (approve && req.userId !in group.memberIds && req.userId !in group.bannedIds) {
                db.collection("groups").document(group.id)
                    .update(
                        mapOf(
                            "memberIds" to (group.memberIds + req.userId).distinct(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                sendCommunityNotificationSafely(
                    targetUserId = req.userId,
                    title = "Group Request Approved",
                    message = "You are now a member of ${group.name}.",
                    category = "GROUP"
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun promoteModerator(group: GroupRoom, targetUserId: String): Result<Unit> {
        val docPath = "groups/${group.id}"
        return try {
            requireUserId()
            db.collection("groups").document(group.id)
                .update(
                    mapOf(
                        "moderatorIds" to (group.moderatorIds + targetUserId).distinct(),
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

    suspend fun removeOrBanMember(group: GroupRoom, targetUserId: String, banUser: Boolean): Result<Unit> {
        val docPath = "groups/${group.id}"
        return try {
            requireUserId()
            require(targetUserId != group.ownerId) { "The group owner cannot be removed or banned." }
            val updatedMembers = group.memberIds.filter { it != targetUserId }
            val updatedBanned = if (banUser) (group.bannedIds + targetUserId).distinct() else group.bannedIds
            val updates = mutableMapOf<String, Any>(
                "memberIds" to updatedMembers,
                "bannedIds" to updatedBanned,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            // Owner can also strip moderator role
            if (auth.currentUser?.uid == group.ownerId) {
                updates["moderatorIds"] = group.moderatorIds.filter { it != targetUserId }
            }
            db.collection("groups").document(group.id).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun leaveGroup(group: GroupRoom): Result<Unit> {
        val docPath = "groups/${group.id}"
        return try {
            val uid = requireUserId()
            require(uid != group.ownerId) { "As the group owner, delete the room instead of leaving." }
            db.collection("groups").document(group.id)
                .update(
                    mapOf(
                        "memberIds" to group.memberIds.filter { it != uid },
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

    suspend fun sendGroupMessage(
        groupId: String,
        senderName: String,
        originalText: String,
        sourceLanguage: String,
        initialTranslations: Map<String, String> = emptyMap(),
        translationNote: String = ""
    ): Result<String> {
        val docPath = "groups/$groupId/messages"
        return try {
            val uid = requireUserId()
            val cleanText = originalText.trim().take(2000)
            require(cleanText.isNotBlank()) { "Message text cannot be empty." }
            val ref = db.collection("groups").document(groupId).collection("messages").document()
            ref.set(
                mapOf(
                    "groupId" to groupId,
                    "senderId" to uid,
                    "senderName" to senderName.trim().ifBlank { "Member" }.take(80),
                    "originalText" to cleanText,
                    "sourceLanguage" to sourceLanguage.ifBlank { "en" }.take(16),
                    "translations" to initialTranslations.entries.take(20).associate { it.key.take(16) to it.value.take(2000) },
                    "translationNote" to translationNote.take(240),
                    "createdAt" to FieldValue.serverTimestamp()
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

    suspend fun cacheGroupMessageTranslation(
        groupId: String,
        messageId: String,
        mergedTranslations: Map<String, String>,
        note: String
    ): Result<Unit> {
        val docPath = "groups/$groupId/messages/$messageId"
        return try {
            requireUserId()
            db.collection("groups").document(groupId).collection("messages").document(messageId)
                .update(
                    mapOf(
                        "translations" to mergedTranslations.entries.take(20).associate { it.key.take(16) to it.value.take(2000) },
                        "translationNote" to note.take(240)
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

    suspend fun deleteGroupMessage(groupId: String, messageId: String): Result<Unit> {
        val docPath = "groups/$groupId/messages/$messageId"
        return try {
            requireUserId()
            db.collection("groups").document(groupId).collection("messages").document(messageId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private suspend fun sendCommunityNotificationSafely(
        targetUserId: String,
        title: String,
        message: String,
        category: String
    ) {
        try {
            db.collection("users").document(targetUserId).collection("notifications").document()
                .set(
                    mapOf(
                        "userId" to targetUserId,
                        "title" to title.take(120),
                        "message" to message.take(500),
                        "category" to category,
                        "readStatus" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        } catch (_: Exception) {
            // Best-effort notification: blocked users or offline state should not break the caller cleanly.
        }
    }
}
