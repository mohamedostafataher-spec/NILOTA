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

class FirestoreDataRepository(
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
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun currentEmail(): String = auth.currentUser?.email.orEmpty()

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

    // --- ADMIN ROLE CHECK ---
    suspend fun checkIsAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        if (user.isEmailVerified && user.email.equals("mohamedostafataher@gmail.com", ignoreCase = true)) {
            return true
        }
        return try {
            val snap = db.collection("admin_users").document(user.uid).get().await()
            snap.exists()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "admin_users/${user.uid}")
            false
        }
    }

    // --- USER PROFILE & PRIVATE INFO ---
    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.GET, path, uid)
        )
    }

    fun observeUserPrivateInfo(userId: String): Flow<UserPrivateInfo?> = flow {
        val uid = requireUserId()
        val path = "users/$userId/private/info"
        emitAll(
            db.collection("users").document(userId).collection("private").document("info")
                .snapshots()
                .map { it.toObject(UserPrivateInfo::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.GET, path, uid)
        )
    }

    suspend fun saveUserProfile(
        displayName: String,
        accountMode: String,
        preferredLanguage: String,
        country: String,
        photoUri: String,
        bio: String,
        privacyConsent: Boolean,
        retainTranslationHistory: Boolean,
        phone: String = "",
        emergencyContact: String = "",
        city: String = "",
        nativeLanguage: String = preferredLanguage,
        spokenLanguages: List<String> = listOf(nativeLanguage.ifBlank { preferredLanguage }),
        learningLanguages: List<String> = emptyList(),
        interests: List<String> = emptyList(),
        profileVisibility: String = "PUBLIC",
        openToLanguageExchange: Boolean = true,
        phoneCountryCode: String = "+20"
    ): Result<Unit> {
        var docPath = "users/unknown"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val existing = docRef.get().await()
            val cleanName = displayName.trim().take(80).ifBlank { "NILORA Member" }
            val cleanMode = when (accountMode) {
                "LOCAL_PROVIDER", "TRAVELER", "GLOBAL_MEMBER" -> accountMode
                else -> "GLOBAL_MEMBER"
            }
            val cleanLang = NiloraLanguage.fromCode(preferredLanguage).code
            val cleanNativeLang = nativeLanguage.trim().ifBlank { cleanLang }.take(16)
            val cleanSpoken = spokenLanguages.map { it.trim().take(20) }.filter { it.isNotBlank() }.distinct().take(15)
                .ifEmpty { listOf(cleanNativeLang) }
            val cleanLearning = learningLanguages.map { it.trim().take(20) }.filter { it.isNotBlank() }.distinct().take(15)
            val cleanInterests = interests.map { it.trim().take(40) }.filter { it.isNotBlank() }.distinct().take(15)
            val cleanCountry = country.trim().take(80).ifBlank { "Egypt" }
            val cleanCity = city.trim().take(80)
            val cleanBio = bio.trim().take(500)
            val cleanPhoto = photoUri.take(1000)
            val cleanVisibility = when (profileVisibility) {
                "PUBLIC", "CONNECTIONS_ONLY", "PRIVATE" -> profileVisibility
                else -> "PUBLIC"
            }

            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to cleanName,
                    "accountMode" to cleanMode,
                    "preferredLanguage" to cleanLang,
                    "nativeLanguage" to cleanNativeLang,
                    "spokenLanguages" to cleanSpoken,
                    "learningLanguages" to cleanLearning,
                    "interests" to cleanInterests,
                    "country" to cleanCountry,
                    "city" to cleanCity,
                    "photoUri" to cleanPhoto,
                    "bio" to cleanBio,
                    "profileVisibility" to cleanVisibility,
                    "openToLanguageExchange" to openToLanguageExchange,
                    "privacyConsent" to privacyConsent,
                    "retainTranslationHistory" to retainTranslationHistory,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "displayName" to cleanName,
                        "accountMode" to cleanMode,
                        "preferredLanguage" to cleanLang,
                        "nativeLanguage" to cleanNativeLang,
                        "spokenLanguages" to cleanSpoken,
                        "learningLanguages" to cleanLearning,
                        "interests" to cleanInterests,
                        "country" to cleanCountry,
                        "city" to cleanCity,
                        "photoUri" to cleanPhoto,
                        "bio" to cleanBio,
                        "profileVisibility" to cleanVisibility,
                        "openToLanguageExchange" to openToLanguageExchange,
                        "privacyConsent" to privacyConsent,
                        "retainTranslationHistory" to retainTranslationHistory,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val publicRef = db.collection("public_profiles").document(uid)
            val existingPublic = publicRef.get().await()
            if (!existingPublic.exists()) {
                publicRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to cleanName,
                        "country" to cleanCountry,
                        "city" to cleanCity,
                        "nativeLanguage" to cleanNativeLang,
                        "spokenLanguages" to cleanSpoken,
                        "learningLanguages" to cleanLearning,
                        "interests" to cleanInterests,
                        "photoUri" to cleanPhoto,
                        "bio" to cleanBio,
                        "profileVisibility" to cleanVisibility,
                        "openToLanguageExchange" to openToLanguageExchange,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                publicRef.update(
                    mapOf(
                        "displayName" to cleanName,
                        "country" to cleanCountry,
                        "city" to cleanCity,
                        "nativeLanguage" to cleanNativeLang,
                        "spokenLanguages" to cleanSpoken,
                        "learningLanguages" to cleanLearning,
                        "interests" to cleanInterests,
                        "photoUri" to cleanPhoto,
                        "bio" to cleanBio,
                        "profileVisibility" to cleanVisibility,
                        "openToLanguageExchange" to openToLanguageExchange,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val privateRef = docRef.collection("private").document("info")
            privateRef.set(
                mapOf(
                    "userId" to uid,
                    "email" to currentEmail().take(200),
                    "phoneCountryCode" to phoneCountryCode.trim().ifBlank { "+20" }.take(10),
                    "phone" to phone.trim().take(40),
                    "emergencyContact" to emergencyContact.trim().take(160),
                    "updatedAt" to FieldValue.serverTimestamp()
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

    suspend fun deleteAccountData(): Result<Unit> {
        var docPath = "users/unknown"
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            docPath = userRef.path
            for (sub in listOf("favorites", "travel_notes", "notifications", "private", "blocked")) {
                val snap = userRef.collection(sub).get().await()
                for (doc in snap.documents) {
                    doc.reference.delete().await()
                }
            }
            try {
                db.collection("public_profiles").document(uid).delete().await()
            } catch (_: Exception) {
            }
            for (colInfo in listOf("nilora_memories" to "ownerId", "world_postcards" to "authorId")) {
                try {
                    val ownDocs = db.collection(colInfo.first).whereEqualTo(colInfo.second, uid).get().await()
                    for (doc in ownDocs.documents) {
                        doc.reference.delete().await()
                    }
                } catch (_: Exception) {
                }
            }
            userRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- DESTINATIONS ---
    fun observeApprovedDestinations(): Flow<List<Destination>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("destinations")
                .whereEqualTo("verificationStatus", "APPROVED")
                .snapshots()
                .map { snap -> snap.toObjects(Destination::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "destinations", uid)
        )
    }

    fun observeAdminAllDestinations(): Flow<List<Destination>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("destinations")
                .snapshots()
                .map { snap -> snap.toObjects(Destination::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "destinations", uid)
        )
    }

    suspend fun submitDestination(dest: Destination, asAdminApproved: Boolean = false): Result<String> {
        var docPath = "destinations"
        return try {
            val uid = requireUserId()
            val ref = db.collection("destinations").document()
            docPath = ref.path
            val status = if (asAdminApproved) "APPROVED" else "PENDING"
            val payload = mutableMapOf<String, Any>(
                "title" to dest.title.trim().take(140),
                "countryCode" to dest.countryCode.ifBlank { "EG" }.take(10),
                "cityName" to dest.cityName.trim().take(80),
                "category" to dest.category.ifBlank { "HISTORICAL" }.take(60),
                "description" to dest.description.trim().take(2000),
                "verifiedCoordinates" to (dest.latitude != null && dest.longitude != null && dest.verifiedCoordinates),
                "facilities" to dest.facilities.take(30),
                "operatingHours" to dest.operatingHours.trim().take(200),
                "priceInfo" to dest.priceInfo.trim().take(200),
                "safetyWarnings" to dest.safetyWarnings.trim().take(500),
                "accessibilityInfo" to dest.accessibilityInfo.trim().take(300),
                "officialWebsite" to dest.officialWebsite.trim().take(300),
                "contactInfo" to dest.contactInfo.trim().take(200),
                "photoUrl" to dest.photoUrl.trim().take(500),
                "sourceName" to dest.sourceName.trim().ifBlank { "Verified Admin Entry" }.take(200),
                "sourceUrl" to dest.sourceUrl.trim().take(500),
                "lastVerifiedAt" to dest.lastVerifiedAt.ifBlank { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()) },
                "verificationStatus" to status,
                "submittedBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (dest.latitude != null && dest.longitude != null) {
                payload["latitude"] = dest.latitude
                payload["longitude"] = dest.longitude
            }
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun moderateDestination(destinationId: String, newStatus: String): Result<Unit> {
        val docPath = "destinations/$destinationId"
        return try {
            val uid = requireUserId()
            db.collection("destinations").document(destinationId)
                .update(mapOf("verificationStatus" to newStatus, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            writeAuditLogInternal(uid, "MODERATE_DESTINATION", "destinations", destinationId, "Destination $destinationId marked $newStatus", "INFO")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- ACTIVITIES ---
    fun observeApprovedActivities(): Flow<List<ActivityListing>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("activities")
                .whereEqualTo("verificationStatus", "APPROVED")
                .snapshots()
                .map { snap -> snap.toObjects(ActivityListing::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "activities", uid)
        )
    }

    fun observeMyActivities(): Flow<List<ActivityListing>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("activities")
                .whereEqualTo("providerId", uid)
                .snapshots()
                .map { snap -> snap.toObjects(ActivityListing::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "activities", uid)
        )
    }

    fun observeAdminAllActivities(): Flow<List<ActivityListing>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("activities")
                .snapshots()
                .map { snap -> snap.toObjects(ActivityListing::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "activities", uid)
        )
    }

    suspend fun submitActivity(activity: ActivityListing, asAdminApproved: Boolean = false): Result<String> {
        var docPath = "activities"
        return try {
            val uid = requireUserId()
            val ref = db.collection("activities").document()
            docPath = ref.path
            val status = if (asAdminApproved) "APPROVED" else "PENDING"
            val payload = mutableMapOf<String, Any>(
                "title" to activity.title.trim().take(140),
                "providerId" to uid,
                "providerName" to activity.providerName.trim().ifBlank { "Verified Provider" }.take(100),
                "countryCode" to activity.countryCode.ifBlank { "EG" }.take(10),
                "cityName" to activity.cityName.trim().take(80),
                "category" to activity.category.ifBlank { "ADVENTURE" }.take(60),
                "description" to activity.description.trim().take(2000),
                "durationLabel" to activity.durationLabel.trim().ifBlank { "Duration on request" }.take(80),
                "currency" to activity.currency.ifBlank { "EGP" }.take(10),
                "priceOnRequest" to activity.priceOnRequest,
                "availabilityStatus" to activity.availabilityStatus,
                "includedServices" to activity.includedServices.take(25),
                "exclusions" to activity.exclusions.take(25),
                "cancellationPolicy" to activity.cancellationPolicy.trim().ifBlank { "Free cancellation up to 24h prior" }.take(500),
                "safetyRequirements" to activity.safetyRequirements.trim().take(500),
                "verifiedCoordinates" to (activity.latitude != null && activity.longitude != null && activity.verifiedCoordinates),
                "sourceName" to activity.sourceName.ifBlank { "Provider Submission" }.take(200),
                "verificationStatus" to status,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (activity.priceAmount != null && activity.priceAmount >= 0.0) {
                payload["priceAmount"] = activity.priceAmount
            }
            if (activity.latitude != null && activity.longitude != null) {
                payload["latitude"] = activity.latitude
                payload["longitude"] = activity.longitude
            }
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun moderateActivity(activityId: String, newStatus: String): Result<Unit> {
        val docPath = "activities/$activityId"
        return try {
            val uid = requireUserId()
            db.collection("activities").document(activityId)
                .update(mapOf("verificationStatus" to newStatus, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            writeAuditLogInternal(uid, "MODERATE_ACTIVITY", "activities", activityId, "Activity $activityId marked $newStatus", "INFO")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- PROVIDERS (LOCAL CONNECT) ---
    fun observeApprovedProviders(): Flow<List<ProviderProfile>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("providers")
                .whereEqualTo("verificationStatus", "APPROVED")
                .snapshots()
                .map { snap -> snap.toObjects(ProviderProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "providers", uid)
        )
    }

    fun observeMyProviderProfile(): Flow<ProviderProfile?> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("providers").document(uid)
                .snapshots()
                .map { it.toObject(ProviderProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.GET, "providers/$uid", uid)
        )
    }

    fun observeAdminAllProviders(): Flow<List<ProviderProfile>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("providers")
                .snapshots()
                .map { snap -> snap.toObjects(ProviderProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "providers", uid)
        )
    }

    suspend fun submitProviderApplication(profile: ProviderProfile): Result<Unit> {
        var docPath = "providers/unknown"
        return try {
            val uid = requireUserId()
            val ref = db.collection("providers").document(uid)
            docPath = ref.path
            val existing = ref.get().await()
            val payload = mapOf(
                "userId" to uid,
                "fullName" to profile.fullName.trim().take(100),
                "serviceType" to profile.serviceType,
                "languages" to profile.languages.take(15),
                "citiesCovered" to profile.citiesCovered.take(20),
                "bio" to profile.bio.trim().take(1000),
                "licenseNumber" to profile.licenseNumber.trim().take(100),
                "documentSummary" to profile.documentSummary.trim().take(400),
                "documentRefUri" to profile.documentRefUri.take(1000),
                "verificationStatus" to "PENDING",
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!existing.exists()) {
                ref.set(payload + ("createdAt" to FieldValue.serverTimestamp())).await()
            } else {
                ref.update(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun moderateProvider(providerId: String, newStatus: String, reason: String = ""): Result<Unit> {
        val docPath = "providers/$providerId"
        return try {
            val uid = requireUserId()
            db.collection("providers").document(providerId)
                .update(
                    mapOf(
                        "verificationStatus" to newStatus,
                        "reviewedBy" to uid,
                        "rejectionReason" to reason.take(300),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            writeAuditLogInternal(uid, "MODERATE_PROVIDER", "providers", providerId, "Provider $providerId -> $newStatus", "INFO")
            sendNotification(providerId, "Provider Application $newStatus", "Your Local Connect verification status is now $newStatus.", "VERIFICATION")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- BOOKINGS ---
    fun observeTravelerBookings(): Flow<List<BookingRecord>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("bookings")
                .whereEqualTo("travelerId", uid)
                .snapshots()
                .map { snap -> snap.toObjects(BookingRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "bookings", uid)
        )
    }

    fun observeProviderBookings(): Flow<List<BookingRecord>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("bookings")
                .whereEqualTo("providerId", uid)
                .snapshots()
                .map { snap -> snap.toObjects(BookingRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "bookings", uid)
        )
    }

    fun observeAdminAllBookings(): Flow<List<BookingRecord>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("bookings")
                .snapshots()
                .map { snap -> snap.toObjects(BookingRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "bookings", uid)
        )
    }

    suspend fun createBookingRequest(
        activity: ActivityListing,
        travelerName: String,
        requestedDate: String,
        participantsCount: Int,
        notes: String
    ): Result<String> {
        var docPath = "bookings"
        return try {
            val uid = requireUserId()
            val ref = db.collection("bookings").document()
            docPath = ref.path
            val reference = "NLR-" + UUID.randomUUID().toString().take(8).uppercase()
            val payload = mapOf(
                "bookingReference" to reference,
                "travelerId" to uid,
                "travelerName" to travelerName.trim().ifBlank { "Traveler" }.take(100),
                "providerId" to activity.providerId,
                "providerName" to activity.providerName,
                "activityId" to activity.id,
                "activityTitle" to activity.title.take(140),
                "requestedDate" to requestedDate.trim().take(40),
                "participantsCount" to participantsCount.coerceIn(1, 50),
                "notes" to notes.trim().take(500),
                "status" to "AWAITING_PROVIDER_CONFIRMATION",
                "paymentStatus" to "NOT_CONFIGURED",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            sendNotification(
                uid,
                "Booking Request Submitted",
                "Request $reference for ${activity.title} is awaiting provider confirmation.",
                "BOOKING"
            )
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateBookingStatus(bookingId: String, newStatus: String, reason: String = ""): Result<Unit> {
        val docPath = "bookings/$bookingId"
        return try {
            requireUserId()
            db.collection("bookings").document(bookingId)
                .update(
                    mapOf(
                        "status" to newStatus,
                        "cancellationReason" to reason.trim().take(300),
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

    // --- MY JOURNEY: FAVORITES, NOTES, NOTIFICATIONS ---
    fun observeFavorites(): Flow<List<FavoriteItem>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("users").document(uid).collection("favorites")
                .snapshots()
                .map { snap -> snap.toObjects(FavoriteItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "users/$uid/favorites", uid)
        )
    }

    suspend fun toggleFavorite(targetId: String, targetType: String, title: String, cityName: String, category: String, currentlySaved: Boolean): Result<Unit> {
        var docPath = "users/unknown/favorites/$targetId"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("favorites").document(targetId)
            docPath = ref.path
            if (currentlySaved) {
                ref.delete().await()
            } else {
                ref.set(
                    mapOf(
                        "userId" to uid,
                        "targetId" to targetId,
                        "targetType" to targetType,
                        "title" to title.take(140),
                        "cityName" to cityName.take(80),
                        "category" to category.take(80),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun observeTravelNotes(): Flow<List<TravelNote>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("users").document(uid).collection("travel_notes")
                .snapshots()
                .map { snap -> snap.toObjects(TravelNote::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "users/$uid/travel_notes", uid)
        )
    }

    suspend fun saveTravelNote(title: String, content: String, cityName: String, tripDate: String): Result<Unit> {
        var docPath = "users/unknown/travel_notes"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("travel_notes").document()
            docPath = ref.path
            ref.set(
                mapOf(
                    "userId" to uid,
                    "title" to title.trim().take(120),
                    "content" to content.trim().take(2000),
                    "cityName" to cityName.trim().take(80),
                    "tripDate" to tripDate.trim().take(40),
                    "createdAt" to FieldValue.serverTimestamp(),
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

    suspend fun deleteTravelNote(noteId: String): Result<Unit> {
        var docPath = "users/unknown/travel_notes/$noteId"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("travel_notes").document(noteId)
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

    fun observeNotifications(): Flow<List<AppNotification>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("users").document(uid).collection("notifications")
                .snapshots()
                .map { snap -> snap.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "users/$uid/notifications", uid)
        )
    }

    suspend fun markNotificationRead(notifId: String): Result<Unit> {
        var docPath = "users/unknown/notifications/$notifId"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("notifications").document(notifId)
            docPath = ref.path
            ref.update("readStatus", true).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun sendNotification(targetUserId: String, title: String, message: String, category: String): Result<Unit> {
        val docPath = "users/$targetUserId/notifications"
        return try {
            requireUserId()
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
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    // --- TWO-PHONE CONVERSATION ROOMS ---
    fun observeMyConversationRooms(): Flow<List<ConversationRoom>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("conversation_rooms")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snap -> snap.toObjects(ConversationRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "conversation_rooms", uid)
        )
    }

    fun observeRoom(roomCode: String): Flow<ConversationRoom?> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("conversation_rooms").document(roomCode)
                .snapshots()
                .map { it.toObject(ConversationRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.GET, "conversation_rooms/$roomCode", uid)
        )
    }

    fun observeRoomMessages(roomCode: String): Flow<List<ConversationMessage>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("conversation_rooms").document(roomCode).collection("messages")
                .whereArrayContains("memberUids", uid)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snap -> snap.toObjects(ConversationMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "conversation_rooms/$roomCode/messages", uid)
        )
    }

    suspend fun createConversationRoom(
        hostName: String,
        hostLanguage: String,
        retainTranscript: Boolean
    ): Result<String> {
        val code = "NLR" + (10000..99999).random().toString()
        val docPath = "conversation_rooms/$code"
        return try {
            val uid = requireUserId()
            db.collection("conversation_rooms").document(code)
                .set(
                    mapOf(
                        "roomCode" to code,
                        "hostId" to uid,
                        "hostName" to hostName.trim().ifBlank { "Host" }.take(80),
                        "hostLanguage" to NiloraLanguage.fromCode(hostLanguage).code,
                        "memberUids" to listOf(uid),
                        "status" to "WAITING",
                        "retainTranscript" to retainTranscript,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(code)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun joinConversationRoom(roomCode: String, guestName: String, guestLanguage: String): Result<ConversationRoom> {
        val cleanCode = roomCode.trim().uppercase()
        val docPath = "conversation_rooms/$cleanCode"
        return try {
            val uid = requireUserId()
            require(cleanCode.matches(Regex("^[a-zA-Z0-9_\\-]+$"))) { "Invalid room code format." }
            val ref = db.collection("conversation_rooms").document(cleanCode)
            val snap = ref.get().await()
            val room = snap.toObject(ConversationRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return Result.failure(IllegalStateException("Room $cleanCode not found."))
            if (uid in room.memberUids) {
                return Result.success(room)
            }
            if (room.status != "WAITING" || room.memberUids.size >= 2) {
                return Result.failure(IllegalStateException("Room $cleanCode is no longer available."))
            }
            val updatedMembers = room.memberUids + uid
            ref.update(
                mapOf(
                    "guestId" to uid,
                    "guestName" to guestName.trim().ifBlank { "Guest" }.take(80),
                    "guestLanguage" to NiloraLanguage.fromCode(guestLanguage).code,
                    "memberUids" to updatedMembers,
                    "status" to "ACTIVE",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            val refreshed = ref.get().await().toObject(ConversationRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: room.copy(guestId = uid, guestName = guestName, guestLanguage = guestLanguage, memberUids = updatedMembers, status = "ACTIVE")
            Result.success(refreshed)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun sendRoomMessage(
        room: ConversationRoom,
        senderName: String,
        sourceLanguage: String,
        targetLanguage: String,
        originalText: String,
        translatedText: String,
        confidenceNote: String
    ): Result<Unit> {
        val docPath = "conversation_rooms/${room.roomCode}/messages"
        return try {
            val uid = requireUserId()
            db.collection("conversation_rooms").document(room.roomCode)
                .collection("messages").document()
                .set(
                    mapOf(
                        "roomCode" to room.roomCode,
                        "memberUids" to room.memberUids,
                        "senderId" to uid,
                        "senderName" to senderName.trim().ifBlank { "Speaker" }.take(80),
                        "sourceLanguage" to sourceLanguage,
                        "targetLanguage" to targetLanguage,
                        "originalText" to originalText.trim().take(1500),
                        "translatedText" to translatedText.trim().take(1500),
                        "confidenceNote" to confidenceNote.trim().take(200),
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

    suspend fun updateRoomLanguage(roomCode: String, isHost: Boolean, newLang: String): Result<Unit> {
        val docPath = "conversation_rooms/$roomCode"
        return try {
            requireUserId()
            val field = if (isHost) "hostLanguage" else "guestLanguage"
            db.collection("conversation_rooms").document(roomCode)
                .update(mapOf(field to NiloraLanguage.fromCode(newLang).code, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun endConversationRoom(roomCode: String, deleteMessagesIfTemporary: Boolean): Result<Unit> {
        val docPath = "conversation_rooms/$roomCode"
        return try {
            val uid = requireUserId()
            val ref = db.collection("conversation_rooms").document(roomCode)
            ref.update(mapOf("status" to "ENDED", "updatedAt" to FieldValue.serverTimestamp())).await()
            if (deleteMessagesIfTemporary) {
                val msgs = ref.collection("messages").whereArrayContains("memberUids", uid).get().await()
                for (doc in msgs.documents) {
                    doc.reference.delete().await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- REPORTS, DATA SOURCES, AUDIT LOGS (ADMIN) ---
    suspend fun submitReport(targetType: String, targetId: String, reason: String, details: String): Result<Unit> {
        var docPath = "reports"
        return try {
            val uid = requireUserId()
            val ref = db.collection("reports").document()
            docPath = ref.path
            ref.set(
                mapOf(
                    "reporterId" to uid,
                    "targetType" to targetType,
                    "targetId" to targetId.take(128),
                    "reason" to reason.trim().take(160),
                    "details" to details.trim().take(1000),
                    "status" to "OPEN",
                    "createdAt" to FieldValue.serverTimestamp(),
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

    fun observeAdminReports(): Flow<List<ContentReport>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("reports")
                .snapshots()
                .map { snap -> snap.toObjects(ContentReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "reports", uid)
        )
    }

    suspend fun resolveReport(reportId: String, status: String): Result<Unit> {
        val docPath = "reports/$reportId"
        return try {
            val uid = requireUserId()
            db.collection("reports").document(reportId)
                .update(mapOf("status" to status, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            writeAuditLogInternal(uid, "RESOLVE_REPORT", "reports", reportId, "Report $reportId marked $status", "INFO")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    fun observeAdminUsers(): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("users")
                .snapshots()
                .map { snap -> snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "users", uid)
        )
    }

    fun observeDataSources(): Flow<List<DataSourceRecord>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("data_sources")
                .snapshots()
                .map { snap -> snap.toObjects(DataSourceRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "data_sources", uid)
        )
    }

    fun observeAuditLogs(): Flow<List<AuditLog>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("audit_logs")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .snapshots()
                .map { snap -> snap.toObjects(AuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logAndRethrow(OperationType.LIST, "audit_logs", uid)
        )
    }

    private suspend fun writeAuditLogInternal(
        actorId: String,
        action: String,
        targetCollection: String,
        targetId: String,
        summary: String,
        severity: String
    ) {
        try {
            db.collection("audit_logs").document().set(
                mapOf(
                    "actorId" to actorId,
                    "actorEmail" to currentEmail().take(200),
                    "action" to action.take(100),
                    "targetCollection" to targetCollection.take(80),
                    "targetId" to targetId.take(128),
                    "summary" to summary.take(500),
                    "severity" to severity,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "audit_logs")
        }
    }
}
