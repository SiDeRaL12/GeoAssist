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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gustavo.geoassist.data.local.PlaceEntity
import com.gustavo.geoassist.data.repository.PlaceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for managing map screen state and data.
 *
 * Handles place data observation, category filtering, and data refresh operations.
 * Follows MVVM architecture pattern with reactive state management using StateFlow.
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
    
    init {
        // Load initial data
        refreshPlaces()
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
     * Updates the user's current location.
     *
     * @param latitude User's latitude
     * @param longitude User's longitude
     */
    fun updateUserLocation(latitude: Double, longitude: Double) {
        _userLocation.value = Pair(latitude, longitude)
    }
    
    /**
     * Refreshes place data from the repository.
     */
    fun refreshPlaces() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.refreshPlaces(
                onSuccess = { _isLoading.value = false },
                onError = { _isLoading.value = false }
            )
        }
    }
}
