package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.FirestoreDataRepository
import com.example.data.repository.GpsCoordinates
import com.example.data.repository.LocationAndPlacesRepository
import com.example.data.repository.PlacesSearchOutcome
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MainDashboardState(
    val userProfile: UserProfile? = null,
    val privateInfo: UserPrivateInfo? = null,
    val isAdmin: Boolean = false,
    val searchQuery: String = "",
    val selectedCityFilter: String = "ALL",
    val selectedCategoryFilter: String = "ALL",
    val selectedCountryFilter: String = "EG",
    val gpsLocation: GpsCoordinates? = null,
    val gpsStatusMessage: String = "",
    val placesOutcome: PlacesSearchOutcome? = null,
    val isSearchingPlaces: Boolean = false,
    val actionMessage: String? = null,
    val isErrorAction: Boolean = false
)

class MainViewModel(
    private val firestoreRepo: FirestoreDataRepository,
    private val locationRepo: LocationAndPlacesRepository,
    val currentUserId: String
) : ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
        private const val STOP_TIMEOUT_MS = 5000L
    }

    private val _uiMeta = MutableStateFlow(MainDashboardState())
    val uiMeta: StateFlow<MainDashboardState> = _uiMeta.asStateFlow()

    val isOnline: Boolean
        get() = locationRepo.isNetworkOnline()

    val isGoogleMapsKeyConfigured: Boolean
        get() = locationRepo.isGoogleMapsKeyConfigured()

    init {
        viewModelScope.launch {
            val admin = firestoreRepo.checkIsAdmin()
            _uiMeta.update { it.copy(isAdmin = admin) }
        }
        viewModelScope.launch {
            firestoreRepo.observeUserProfile(currentUserId)
                .catch { handleStreamError(it, "profile") }
                .collect { profile -> _uiMeta.update { it.copy(userProfile = profile) } }
        }
        viewModelScope.launch {
            firestoreRepo.observeUserPrivateInfo(currentUserId)
                .catch { handleStreamError(it, "privateInfo") }
                .collect { priv -> _uiMeta.update { it.copy(privateInfo = priv) } }
        }
    }

    private fun <T> Flow<T>.toUiState(): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                val root = fsErr ?: error
                Log.w(TAG, "Stream error", root)
                emit(UiState.Error(root.localizedMessage ?: "Failed to load data"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    private fun handleStreamError(error: Throwable, label: String) {
        val fsErr = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (fsErr == null && error is CancellationException) throw error
        Log.w(TAG, "Error observing $label", fsErr ?: error)
    }

    val destinationsState: StateFlow<UiState<List<Destination>>> =
        firestoreRepo.observeApprovedDestinations().toUiState()

    val activitiesState: StateFlow<UiState<List<ActivityListing>>> =
        firestoreRepo.observeApprovedActivities().toUiState()

    val providersState: StateFlow<UiState<List<ProviderProfile>>> =
        firestoreRepo.observeApprovedProviders().toUiState()

    val myProviderProfileState: StateFlow<UiState<ProviderProfile?>> =
        firestoreRepo.observeMyProviderProfile().toUiState()

    val myActivitiesState: StateFlow<UiState<List<ActivityListing>>> =
        firestoreRepo.observeMyActivities().toUiState()

    val travelerBookingsState: StateFlow<UiState<List<BookingRecord>>> =
        firestoreRepo.observeTravelerBookings().toUiState()

    val providerBookingsState: StateFlow<UiState<List<BookingRecord>>> =
        firestoreRepo.observeProviderBookings().toUiState()

    val favoritesState: StateFlow<UiState<List<FavoriteItem>>> =
        firestoreRepo.observeFavorites().toUiState()

    val travelNotesState: StateFlow<UiState<List<TravelNote>>> =
        firestoreRepo.observeTravelNotes().toUiState()

    val notificationsState: StateFlow<UiState<List<AppNotification>>> =
        firestoreRepo.observeNotifications().toUiState()

    val myRoomsState: StateFlow<UiState<List<ConversationRoom>>> =
        firestoreRepo.observeMyConversationRooms().toUiState()

    // Admin streams (collected only when AdminDashboard is composed by verified admin)
    val adminDestinationsFlow by lazy { firestoreRepo.observeAdminAllDestinations().toUiState() }
    val adminActivitiesFlow by lazy { firestoreRepo.observeAdminAllActivities().toUiState() }
    val adminProvidersFlow by lazy { firestoreRepo.observeAdminAllProviders().toUiState() }
    val adminBookingsFlow by lazy { firestoreRepo.observeAdminAllBookings().toUiState() }
    val adminReportsFlow by lazy { firestoreRepo.observeAdminReports().toUiState() }
    val adminUsersFlow by lazy { firestoreRepo.observeAdminUsers().toUiState() }
    val dataSourcesFlow by lazy { firestoreRepo.observeDataSources().toUiState() }
    val auditLogsFlow by lazy { firestoreRepo.observeAuditLogs().toUiState() }

    fun setSearchQuery(q: String) {
        _uiMeta.update { it.copy(searchQuery = q) }
    }

    fun setCityFilter(city: String) {
        _uiMeta.update { it.copy(selectedCityFilter = city) }
    }

    fun setCategoryFilter(category: String) {
        _uiMeta.update { it.copy(selectedCategoryFilter = category) }
    }

    fun setCountryFilter(code: String) {
        _uiMeta.update { it.copy(selectedCountryFilter = code) }
    }

    fun clearActionMessage() {
        _uiMeta.update { it.copy(actionMessage = null, isErrorAction = false) }
    }

    private fun reportResult(res: Result<*>, successMsg: String) {
        res.onSuccess {
            _uiMeta.update { it.copy(actionMessage = successMsg, isErrorAction = false) }
        }.onFailure { err ->
            _uiMeta.update {
                it.copy(actionMessage = err.localizedMessage ?: "Operation failed", isErrorAction = true)
            }
        }
    }

    fun saveProfile(
        displayName: String,
        accountMode: String,
        preferredLanguage: String,
        country: String,
        photoUri: String,
        bio: String,
        privacyConsent: Boolean,
        retainHistory: Boolean,
        phone: String,
        emergencyContact: String,
        city: String = "",
        nativeLanguage: String = preferredLanguage,
        spokenLanguages: List<String> = listOf(nativeLanguage.ifBlank { preferredLanguage }),
        learningLanguages: List<String> = emptyList(),
        interests: List<String> = emptyList(),
        profileVisibility: String = "PUBLIC",
        openToLanguageExchange: Boolean = true,
        phoneCountryCode: String = "+20",
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = firestoreRepo.saveUserProfile(
                displayName = displayName,
                accountMode = accountMode,
                preferredLanguage = preferredLanguage,
                country = country,
                photoUri = photoUri,
                bio = bio,
                privacyConsent = privacyConsent,
                retainTranslationHistory = retainHistory,
                phone = phone,
                emergencyContact = emergencyContact,
                city = city,
                nativeLanguage = nativeLanguage,
                spokenLanguages = spokenLanguages,
                learningLanguages = learningLanguages,
                interests = interests,
                profileVisibility = profileVisibility,
                openToLanguageExchange = openToLanguageExchange,
                phoneCountryCode = phoneCountryCode
            )
            reportResult(res, "Profile saved securely.")
            if (res.isSuccess) onComplete()
        }
    }

    fun deleteAccountData(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val res = firestoreRepo.deleteAccountData()
            reportResult(res, "User account data removed.")
            if (res.isSuccess) onDeleted()
        }
    }

    fun toggleFavorite(targetId: String, targetType: String, title: String, cityName: String, category: String, currentlySaved: Boolean) {
        viewModelScope.launch {
            val res = firestoreRepo.toggleFavorite(targetId, targetType, title, cityName, category, currentlySaved)
            reportResult(res, if (currentlySaved) "Removed from Saved Places" else "Saved to My Journey")
        }
    }

    fun saveTravelNote(title: String, content: String, cityName: String, tripDate: String) {
        viewModelScope.launch {
            val res = firestoreRepo.saveTravelNote(title, content, cityName, tripDate)
            reportResult(res, "Travel note saved.")
        }
    }

    fun deleteTravelNote(noteId: String) {
        viewModelScope.launch {
            val res = firestoreRepo.deleteTravelNote(noteId)
            reportResult(res, "Travel note deleted.")
        }
    }

    fun submitProviderApplication(profile: ProviderProfile) {
        viewModelScope.launch {
            val res = firestoreRepo.submitProviderApplication(profile)
            reportResult(res, "Application submitted for administrator verification.")
        }
    }

    fun submitDestination(dest: Destination, asAdminApproved: Boolean = false) {
        viewModelScope.launch {
            val res = firestoreRepo.submitDestination(dest, asAdminApproved)
            reportResult(res, if (asAdminApproved) "Verified destination published." else "Submitted for verification.")
        }
    }

    fun submitActivity(activity: ActivityListing, asAdminApproved: Boolean = false) {
        viewModelScope.launch {
            val res = firestoreRepo.submitActivity(activity, asAdminApproved)
            reportResult(res, if (asAdminApproved) "Activity published." else "Activity submitted for review.")
        }
    }

    fun createBookingRequest(
        activity: ActivityListing,
        requestedDate: String,
        participantsCount: Int,
        notes: String,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val name = _uiMeta.value.userProfile?.displayName ?: "Traveler"
            val res = firestoreRepo.createBookingRequest(activity, name, requestedDate, participantsCount, notes)
            reportResult(res, "Booking request submitted. Awaiting provider confirmation.")
            res.onSuccess { onCreated(it) }
        }
    }

    fun updateBookingStatus(bookingId: String, status: String, reason: String = "") {
        viewModelScope.launch {
            val res = firestoreRepo.updateBookingStatus(bookingId, status, reason)
            reportResult(res, "Booking status updated to $status.")
        }
    }

    fun markNotificationRead(notifId: String) {
        viewModelScope.launch {
            firestoreRepo.markNotificationRead(notifId)
        }
    }

    fun submitReport(targetType: String, targetId: String, reason: String, details: String) {
        viewModelScope.launch {
            val res = firestoreRepo.submitReport(targetType, targetId, reason, details)
            reportResult(res, "Report submitted to NILORA moderation.")
        }
    }

    // Admin actions
    fun moderateDestination(id: String, status: String) {
        viewModelScope.launch {
            reportResult(firestoreRepo.moderateDestination(id, status), "Destination $status")
        }
    }

    fun moderateActivity(id: String, status: String) {
        viewModelScope.launch {
            reportResult(firestoreRepo.moderateActivity(id, status), "Activity $status")
        }
    }

    fun moderateProvider(id: String, status: String, reason: String = "") {
        viewModelScope.launch {
            reportResult(firestoreRepo.moderateProvider(id, status, reason), "Provider $status")
        }
    }

    fun resolveReport(id: String, status: String) {
        viewModelScope.launch {
            reportResult(firestoreRepo.resolveReport(id, status), "Report marked $status")
        }
    }

    // GPS & Places
    fun refreshGpsLocation() {
        viewModelScope.launch {
            _uiMeta.update { it.copy(gpsStatusMessage = "Acquiring GPS coordinates...") }
            val res = locationRepo.getCurrentGpsLocation()
            res.onSuccess { coords ->
                _uiMeta.update {
                    it.copy(
                        gpsLocation = coords,
                        gpsStatusMessage = "Live GPS active (${String.format("%.4f", coords.latitude)}, ${String.format("%.4f", coords.longitude)})"
                    )
                }
            }.onFailure { err ->
                _uiMeta.update {
                    it.copy(gpsStatusMessage = err.localizedMessage ?: "Location unavailable.")
                }
            }
        }
    }

    fun searchLivePlaces(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiMeta.update { it.copy(isSearchingPlaces = true) }
            val outcome = locationRepo.searchVerifiedGooglePlaces(query)
            _uiMeta.update { it.copy(placesOutcome = outcome, isSearchingPlaces = false) }
        }
    }
}
