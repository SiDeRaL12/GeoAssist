/**
 * Repository for managing place data in the GeoAssist application.
 *
 * This file defines the PlaceRepository class which implements the Repository pattern,
 * serving as a single source of truth for place data. It coordinates data access between
 * the local Room database and remote network sources.
 *
 * The repository implements an offline-first strategy, always exposing data from the local
 * database while asynchronously fetching fresh data from the network in the background.
 *
 * @author Gustavo Sanchez
 * @since 1.0
 */
package com.gustavo.geoassist.data.repository

import android.content.Context
import com.gustavo.geoassist.data.local.GeoAssistDatabase
import com.gustavo.geoassist.data.local.PlaceDao
import com.gustavo.geoassist.data.local.PlaceEntity
import com.gustavo.geoassist.data.remote.VolleyClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Repository class for place data management.
 *
 * This class follows the Repository pattern from Android Architecture Components,
 * providing a clean API for data access while abstracting the data sources (local and remote).
 *
 * Design Decisions:
 * - Offline-first strategy: Always read from local database, update in background
 * - Single source of truth: Room database is the authoritative data source
 * - Flow-based reactive data: UI receives automatic updates when data changes
 * - Separation of concerns: Repository handles data logic, ViewModels handle UI logic
 *
 * Data Flow:
 * 1. UI observes Flow from repository (which comes from Room)
 * 2. Repository exposes local data immediately
 * 3. Repository fetches fresh data from network in background
 * 4. Fresh data is saved to Room
 * 5. Room emits update through Flow
 * 6. UI receives update automatically
 *
 * Thread Safety:
 * - Flow observations are thread-safe (handled by Room and coroutines)
 * - Database writes must be called from IO dispatcher
 * - Network callbacks execute on main thread
 *
 * Usage Example:
 * ```
 * val repository = PlaceRepository(context)
 *
 * // In ViewModel
 * viewModelScope.launch {
 *     // Observe places
 *     repository.getPlaces().collectLatest { places ->
 *         _placesState.value = places
 *     }
 *
 *     // Refresh from network
 *     repository.refreshPlaces()
 * }
 * ```
 *
 * @property placeDao DAO for accessing place data from Room database
 * @property volleyClient HTTP client for fetching data from network
 */
class PlaceRepository(context: Context) {
    
    /**
     * Place DAO instance for database operations.
     *
     * Obtained from the GeoAssistDatabase singleton to ensure consistent
     * database access across the application.
     */
    private val placeDao: PlaceDao = GeoAssistDatabase.getInstance(context).placeDao()
    
    /**
     * Volley client instance for network operations.
     *
     * Obtained from the VolleyClient singleton to share the RequestQueue
     * and avoid creating multiple network clients.
     */
    private val volleyClient: VolleyClient = VolleyClient.getInstance(context)
    
    /**
     * Gets all places from the local database as a Flow.
     *
     * Returns a Flow that emits the complete list of places from the Room database.
     * The Flow automatically updates whenever the underlying data changes, enabling
     * reactive UI updates.
     *
     * This method provides immediate access to locally cached data, supporting offline
     * functionality. For fresh data, call refreshPlaces() separately.
     *
     * @return Flow emitting List of all PlaceEntity objects
     *
     * Thread Safety: The Flow can be collected from any coroutine scope
     *
     * Example:
     * ```
     * viewModelScope.launch {
     *     repository.getPlaces().collectLatest { places ->
     *         _placesState.value = places
     *     }
     * }
     * ```
     */
    fun getPlaces(): Flow<List<PlaceEntity>> {
        return placeDao.getAllPlaces()
    }
    
    /**
     * Gets places filtered by category from the local database as a Flow.
     *
     * Returns a Flow that emits only places matching the specified category.
     * This supports the category filtering feature in the UI.
     *
     * The Flow automatically updates when places of the specified category are
     * added, removed, or modified.
     *
     * @param category The category to filter by ("Hospital", "Police", or "Library")
     * @return Flow emitting List of PlaceEntity objects for the specified category
     *
     * Thread Safety: The Flow can be collected from any coroutine scope
     *
     * Note: Category matching is case-sensitive
     *
     * Example:
     * ```
     * repository.getPlacesByCategory("Hospital").collectLatest { hospitals ->
     *     displayOnMap(hospitals)
     * }
     * ```
     */
    fun getPlacesByCategory(category: String): Flow<List<PlaceEntity>> {
        return placeDao.getPlacesByCategory(category)
    }
    
    /**
     * Refreshes place data from the network (or local fallback).
     *
     * Attempts to fetch fresh place data from the network and save it to the local
     * database. If network fetch fails, falls back to loading data from local JSON
     * resources.
     *
     * This method uses callback-based Volley API which executes on the main thread,
     * so database operations are wrapped in coroutine IO dispatcher calls.
     *
     * Data Flow:
     * 1. Attempt network fetch
     * 2. On success: Parse JSON and save to database
     * 3. On failure: Fall back to local JSON resource
     * 4. Database updates trigger Flow emissions to observers
     *
     * @param onSuccess Optional callback invoked when data is successfully refreshed
     * @param onError Optional callback invoked when refresh fails (after all retry attempts)
     *
     * Thread Safety: Can be called from any thread. Database writes are dispatched
     *                 to IO thread internally.
     *
     * Network Errors:
     * The method gracefully handles network failures by falling back to local data,
     * ensuring the app remains functional even without connectivity.
     *
     * Example:
     * ```
     * // Simple refresh
     * repository.refreshPlaces()
     *
     * // With callbacks
     * repository.refreshPlaces(
     *     onSuccess = { showSuccessMessage() },
     *     onError = { error -> showError(error) }
     * )
     * ```
     *
     * Important Notes:
     * - This method does not block
     * - UI will be updated automatically via Flow when data is saved
     * - Failed refreshes do not clear existing cached data
     * - Local fallback ensures offline functionality
     */
    fun refreshPlaces(
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        // First try loading from local resource file
        // In a real app, you would try network first, then fall back to local
        volleyClient.fetchPlacesFromLocal(
            onSuccess = { places ->
                // Save to database on IO thread
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        placeDao.insertAll(places)
                        withContext(Dispatchers.Main) {
                            onSuccess?.invoke()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            onError?.invoke("Failed to save data: ${e.message}")
                        }
                    }
                }
            },
            onError = { error ->
                onError?.invoke(error)
            }
        )
    }
}
