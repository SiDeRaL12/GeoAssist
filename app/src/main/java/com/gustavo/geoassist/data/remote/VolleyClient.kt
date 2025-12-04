/**
 * Volley HTTP client for network operations in the GeoAssist application.
 *
 * This file defines the VolleyClient singleton class which handles all network requests
 * using the Volley library. It provides methods to fetch place data from a remote API
 * or local JSON resources.
 *
 * The client implements the Singleton pattern to share a single RequestQueue across
 * the application, improving performance and resource management.
 *
 * @author Gustavo Sanchez
 * @since 1.0
 */
package com.gustavo.geoassist.data.remote

import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.gustavo.geoassist.R
import com.gustavo.geoassist.data.local.PlaceEntity
import org.json.JSONArray
import org.json.JSONException

/**
 * Singleton HTTP client using Volley for network operations.
 *
 * This class manages HTTP requests for fetching place data. It uses Volley's RequestQueue
 * to handle requests efficiently and provides callback-based API for asynchronous operations.
 *
 * Design Decisions:
 * - Singleton pattern for shared RequestQueue
 * - Callback-based API for async operations
 * - Support for both network and local JSON data
 * - Automatic JSON parsing to PlaceEntity objects
 * - Comprehensive error handling with typed callbacks
 *
 * Thread Safety:
 * Volley handles threading internally. Callbacks are executed on the main thread,
 * so database operations within callbacks should be dispatched to background threads.
 *
 * Usage Example:
 * ```
 * VolleyClient.getInstance(context).fetchPlaces(
 *     onSuccess = { places ->
 *         // Save to database on background thread
 *         viewModelScope.launch(Dispatchers.IO) {
 *             placeDao.insertAll(places)
 *         }
 *     },
 *     onError = { error ->
 *         Log.e(TAG, "Failed to fetch places: $error")
 *     }
 * )
 * ```
 */
class VolleyClient private constructor(context: Context) {
    
    /**
     * Volley RequestQueue instance.
     *
     * The queue manages the execution of network requests,handling threading,
     * caching, and retry logic automatically.
     */
    private val requestQueue: RequestQueue = Volley.newRequestQueue(context.applicationContext)
    
    /**
     * Application context for accessing resources.
     *
     * Stored to access local JSON files when network is unavailable.
     */
    private val applicationContext: Context = context.applicationContext
    
    companion object {
        /**
         * Volatile instance variable for singleton pattern.
         */
        @Volatile
        private var INSTANCE: VolleyClient? = null
        
        /**
         * Gets the singleton instance of VolleyClient.
         *
         * Implements thread-safe lazy initialization using double-checked locking.
         * Creates the client only when first accessed.
         *
         * @param context Application context for initializing Volley
         * @return Singleton instance of VolleyClient
         *
         * Thread Safety: Thread-safe, can be called from multiple threads
         */
        fun getInstance(context: Context): VolleyClient {
            return INSTANCE ?: synchronized(this) {
                val instance = INSTANCE ?: VolleyClient(context)
                INSTANCE = instance
                instance
            }
        }
    }
    
    /**
     * Fetches places from local JSON resource.
     *
     * This method loads place data from a local JSON file included in the app resources.
     * It's used as a fallback when the network is unavailable or for initial app setup.
     *
     * The method reads the JSON, parses it, and invokes the appropriate callback on
     * the main thread.
     *
     * @param onSuccess Callback invoked with parsed List of PlaceEntity on success
     * @param onError Callback invoked with error message String on failure
     *
     * Thread Safety: Callbacks are invoked on the main thread
     *
     * JSON Format Expected:
     * ```json
     * [
     *   {
     *     "id": 1,
     *     "name": "Central Hospital",
     *     "category": "Hospital",
     *     "latitude": 40.7128,
     *     "longitude": -74.0060,
     *     "address": "123 Main St"
     *   }
     * ]
     * ```
     *
     * Example:
     * ```
     * client.fetchPlacesFromLocal(
     *     onSuccess = { places -> displayPlaces(places) },
     *     onError = { error -> showError(error) }
     * )
     * ```
     */
    fun fetchPlacesFromLocal(
        onSuccess: (List<PlaceEntity>) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val inputStream = applicationContext.resources.openRawResource(R.raw.places)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            
            val places = parsePlacesFromJson(jsonArray)
            onSuccess(places)
        } catch (e: Exception) {
            onError("Failed to load local data: ${e.message}")
        }
    }
    
    /**
     * Fetches places from a remote HTTP API.
     *
     * Makes an HTTP GET request to the specified URL and parses the JSON response
     * into a list of PlaceEntity objects. Uses Volley for async network operations.
     *
     * The request is added to the queue and executed asynchronously. Callbacks are
     * invoked on the main thread when the request completes.
     *
     * @param url The HTTP URL to fetch place data from
     * @param onSuccess Callback invoked with parsed List of PlaceEntity on success
     * @param onError Callback invoked with error message String on failure
     *
     * Thread Safety: Callbacks are invoked on the main thread. Perform database
     *                 operations in a background coroutine.
     *
     * Error Handling:
     * - Network errors (no connectivity, timeout, etc.)
     * - HTTP errors (404, 500, etc.)
     * - JSON parsing errors
     *
     * Example:
     * ```
     * client.fetchPlacesFromNetwork(
     *     url = "https://api.example.com/places",
     *     onSuccess = { places ->
     *         viewModelScope.launch(Dispatchers.IO) {
     *             placeDao.insertAll(places)
     *         }
     *     },
     *     onError = { error ->
     *         Log.e(TAG, "Network error: $error")
     *         // Fall back to local data
     *         fetchPlacesFromLocal(...)
     *     }
     * )
     * ```
     */
    fun fetchPlacesFromNetwork(
        url: String,
        onSuccess: (List<PlaceEntity>) -> Unit,
        onError: (String) -> Unit
    ) {
        val request = JsonArrayRequest(
            Request.Method.GET,
            url,
            null,
            { response ->
                try {
                    val places = parsePlacesFromJson(response)
                    onSuccess(places)
                } catch (e: JSONException) {
                    onError("JSON parsing error: ${e.message}")
                }
            },
            { error ->
                onError("Network error: ${error.message ?: "Unknown error"}")
            }
        )
        
        requestQueue.add(request)
    }
    
    /**
     * Parses a JSONArray into a List of PlaceEntity objects.
     *
     * Iterates through the JSON array and extracts place data, creating PlaceEntity
     * objects for each valid entry. Skips entries with missing or invalid fields.
     *
     * @param jsonArray JSONArray containing place data
     * @return List of parsed PlaceEntity objects
     * @throws JSONException if JSON structure is invalid
     *
     * Expected JSON Structure:
     * Each object in the array should have: id, name, category, latitude, longitude, address
     *
     * Example:
     * ```
     * val jsonArray = JSONArray(jsonString)
     * val places = parsePlacesFromJson(jsonArray)
     * ```
     */
    private fun parsePlacesFromJson(jsonArray: JSONArray): List<PlaceEntity> {
        val places = mutableListOf<PlaceEntity>()
        
        for (i in 0 until jsonArray.length()) {
            try {
                val jsonObject = jsonArray.getJSONObject(i)
                val place = PlaceEntity(
                    id = jsonObject.getInt("id"),
                    name = jsonObject.getString("name"),
                    category = jsonObject.getString("category"),
                    latitude = jsonObject.getDouble("latitude"),
                    longitude = jsonObject.getDouble("longitude"),
                    address = jsonObject.getString("address")
                )
                places.add(place)
            } catch (e: JSONException) {
                // Skip invalid entries and continue parsing
                continue
            }
        }
        
        return places
    }
}
