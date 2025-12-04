/**
 * Data Access Object (DAO) for Place entities in the GeoAssist application.
 *
 * This file defines the PlaceDao interface which provides methods for accessing and
 * manipulating place data in the Room database. It follows the DAO pattern to abstract
 * database operations and provide a clean API for data access.
 *
 * The DAO uses Flow for reactive data observation, allowing the UI to automatically
 * update when the underlying data changes. All database operations are designed to be
 * called from background threads (coroutines) to avoid blocking the main thread.
 *
 * @author Gustavo Sanchez
 * @since 1.0
 */
package com.gustavo.geoassist.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO interface for accessing place data in the Room database.
 *
 * This interface defines all database operations related to places. Room automatically
 * generates the implementation at compile time using annotation processing (KSP).
 *
 * Design Decisions:
 * - Uses Flow for reactive queries that emit updates when data changes
 * - OnConflict REPLACE strategy allows easy data refresh from network
 * - Suspend functions for write operations to work with coroutines
 * - Filter methods support category-based filtering for the UI
 *
 * Thread Safety:
 * All methods are designed to be called from coroutines. Query methods returning Flow
 * can be collected from any coroutine, and suspend functions should be called from
 * background dispatchers (IO or Default).
 *
 * Usage Example:
 * ```
 * // Observe all places
 * placeDao.getAllPlaces().collect { places ->
 *     // Update UI with places
 * }
 *
 * // Insert places from network
 * viewModelScope.launch(Dispatchers.IO) {
 *     placeDao.insertAll(networkPlaces)
 * }
 *
 * // Filter by category
 * placeDao.getPlacesByCategory("Hospital").collect { hospitals ->
 *     // Show only hospitals
 * }
 * ```
 */
@Dao
interface PlaceDao {
    
    /**
     * Inserts or replaces multiple places in the database.
     *
     * This method is used to populate or refresh the database with places fetched from
     * the network. The REPLACE conflict strategy ensures that if a place with the same
     * ID already exists, it will be updated with the new data.
     *
     * This is a suspend function and must be called from a coroutine, typically with
     * an IO dispatcher to avoid blocking the main thread.
     *
     * @param places List of PlaceEntity objects to insert or update
     *
     * Thread Safety: Must be called from a background thread (coroutine with IO dispatcher)
     *
     * Example:
     * ```
     * viewModelScope.launch(Dispatchers.IO) {
     *     val placesFromNetwork = fetchPlacesFromApi()
     *     placeDao.insertAll(placesFromNetwork)
     * }
     * ```
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(places: List<PlaceEntity>)
    
    /**
     * Retrieves all places from the database as a Flow.
     *
     * Returns a Flow that emits the complete list of places whenever the data changes
     * in the database. This enables reactive UI updates without manual refresh logic.
     *
     * The Flow will immediately emit the current data and then emit again whenever
     * any place is inserted, updated, or deleted.
     *
     * @return Flow emitting List of all PlaceEntity objects in the database
     *
     * Thread Safety: The Flow can be collected from any coroutine scope. Room handles
     *                 background thread execution internally.
     *
     * Example:
     * ```
     * placeDao.getAllPlaces()
     *     .collectLatest { places ->
     *         _placesState.value = places
     *     }
     * ```
     */
    @Query("SELECT * FROM places")
    fun getAllPlaces(): Flow<List<PlaceEntity>>
    
    /**
     * Retrieves places filtered by category from the database as a Flow.
     *
     * Returns a Flow that emits only places matching the specified category. This is
     * used to implement category filtering in the UI (e.g., show only hospitals).
     *
     * The Flow will automatically update when places are added,removed, or updated
     * that match the specified category.
     *
     * @param category The category to filter by (e.g., "Hospital", "Police", "Library")
     * @return Flow emitting List of PlaceEntity objects matching the category
     *
     * Thread Safety: The Flow can be collected from any coroutine scope. Room handles
     *                 background thread execution internally.
     *
     * Note: Category matching is case-sensitive. Ensure the category parameter matches
     *       the exact case used when storing places.
     *
     * Example:
     * ```
     * placeDao.getPlacesByCategory("Hospital")
     *     .collectLatest { hospitals ->
     *         displayHospitalsOnMap(hospitals)
     *     }
     * ```
     */
    @Query("SELECT * FROM places WHERE category = :category")
    fun getPlacesByCategory(category: String): Flow<List<PlaceEntity>>
    
    /**
     * Deletes all places from the database.
     *
     * This method is used to clear the cache, typically when refreshing data from the
     * network or when the user explicitly requests a data reset.
     *
     * This is a suspend function and must be called from a coroutine, typically with
     * an IO dispatcher.
     *
     * Thread Safety: Must be called from a background thread (coroutine with IO dispatcher)
     *
     * Example:
     * ```
     * viewModelScope.launch(Dispatchers.IO) {
     *     placeDao.deleteAll()
     *     // Then fetch and insert fresh data
     * }
     * ```
     */
    @Query("DELETE FROM places")
    suspend fun deleteAll()
    
    /**
     * Gets the total count of places in the database.
     *
     * This method is useful for checking if the database has been populated or is empty,
     * which can determine whether to fetch data from the network or use cached data.
     *
     * This is a suspend function and must be called from a coroutine.
     *
     * @return The number of places currently stored in the database
     *
     * Thread Safety: Must be called from a background thread (coroutine with IO dispatcher)
     *
     * Example:
     * ```
     * val count = withContext(Dispatchers.IO) {
     *     placeDao.getPlaceCount()
     * }
     * if (count == 0) {
     *     fetchDataFromNetwork()
     * }
     * ```
     */
    @Query("SELECT COUNT(*) FROM places")
    suspend fun getPlaceCount(): Int
}
