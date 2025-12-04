/**
 * ViewModel for the map screen in the GeoAssist application.
 *
 * This ViewModel manages the state and business logic for the main map screen,
 * coordinating between the UI layer and the data layer (Repository).
 *
 * @author Gustavo Sanchez
 * @since 1.0
 */
package com.gustavo.geoassist.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gustavo.geoassist.data.local.PlaceEntity
import com.gustavo.geoassist.data.repository.PlaceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for managing map screen state and data.
 *
 * Handles place data observation, category filtering, network connectivity monitoring,
 * and data refresh operations. Follows MVVM architecture pattern with reactive state
 * management using StateFlow.
 *
 * @property application Application context for Repository initialization
 */
class MapViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = PlaceRepository(application)
    
    // Category filter states
    private val _showHospitals = MutableStateFlow(true)
    val showHospitals: StateFlow<Boolean> = _showHospitals.asStateFlow()
    
    private val _showPolice = MutableStateFlow(true)
    val showPolice: StateFlow<Boolean> = _showPolice.asStateFlow()
    
    private val _showLibraries = MutableStateFlow(true)
    val showLibraries: StateFlow<Boolean> = _showLibraries.asStateFlow()
    
    // All places from repository
    private val allPlaces: StateFlow<List<PlaceEntity>> = repository.getPlaces()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Filtered places based on category toggles
    val filteredPlaces: StateFlow<List<PlaceEntity>> = combine(
        allPlaces,
        _showHospitals,
        _showPolice,
        _showLibraries
    ) { places, hospitals, police, libraries ->
        places.filter { place ->
            when (place.category) {
                "Hospital" -> hospitals
                "Police" -> police
                "Library" -> libraries
                else -> true
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    // User location (nullable until permission granted and location obtained)
    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val userLocation: StateFlow<Pair<Double, Double>?> = _userLocation.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Network connectivity state
    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()
    
    // Error message state for user feedback
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // Location accuracy in meters
    private val _locationAccuracy = MutableStateFlow<Float?>(null)
    val locationAccuracy: StateFlow<Float?> = _locationAccuracy.asStateFlow()
    
    // Network callback for monitoring connectivity
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOffline.value = false
        }
        
        override fun onLost(network: Network) {
            _isOffline.value = true
        }
        
        override fun onUnavailable() {
            _isOffline.value = true
        }
    }
    
    init {
        // Register network callback
        registerNetworkCallback()
        // Check initial network state
        checkNetworkState()
        // Load initial data
        refreshPlaces()
    }
    
    /**
     * Registers a callback to monitor network connectivity changes.
     */
    private fun registerNetworkCallback() {
        val connectivityManager = getApplication<Application>()
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        } catch (e: Exception) {
            // Handle case where callback registration fails
            _isOffline.value = true
        }
    }
    
    /**
     * Checks the current network connectivity state.
     */
    private fun checkNetworkState() {
        val connectivityManager = getApplication<Application>()
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        _isOffline.value = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) != true
    }
    
    /**
     * Toggles the Hospital category filter.
     */
    fun toggleHospitals() {
        _showHospitals.value = !_showHospitals.value
    }
    
    /**
     * Toggles the Police category filter.
     */
    fun togglePolice() {
        _showPolice.value = !_showPolice.value
    }
    
    /**
     * Toggles the Library category filter.
     */
    fun toggleLibraries() {
        _showLibraries.value = !_showLibraries.value
    }
    
    /**
     * Updates the user's current location with accuracy.
     *
     * @param latitude User's latitude
     * @param longitude User's longitude
     * @param accuracy Optional location accuracy in meters
     */
    fun updateUserLocation(latitude: Double, longitude: Double, accuracy: Float? = null) {
        _userLocation.value = Pair(latitude, longitude)
        _locationAccuracy.value = accuracy
    }
    
    /**
     * Clears the current error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Refreshes place data from the repository.
     */
    fun refreshPlaces() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            repository.refreshPlaces(
                onSuccess = { 
                    _isLoading.value = false
                },
                onError = { error ->
                    _isLoading.value = false
                    _errorMessage.value = if (_isOffline.value) {
                        "Working offline - showing cached data"
                    } else {
                        "Failed to load places: $error"
                    }
                }
            )
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Unregister network callback
        try {
            val connectivityManager = getApplication<Application>()
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Ignore if already unregistered
        }
    }
}
