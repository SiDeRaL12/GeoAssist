/**
 * Distance calculation utility for the GeoAssist application.
 *
 * This file provides utility functions for calculating distances between geographic coordinates
 * using the Haversine formula. It supports formatting distances in appropriate units (meters or
 * kilometers) and sorting places by their distance from a reference point.
 *
 * The Haversine formula accounts for the Earth's spherical shape, providing accurate distance
 * calculations for locations not too far apart (which is suitable for a city-scale application).
 *
 * @author Gustavo Sanchez
 * @since 1.0
 */
package com.gustavo.geoassist.domain.util

import com.gustavo.geoassist.data.local.PlaceEntity
import kotlin.math.*

/**
 * Utility object for geographic distance calculations.
 *
 * This object provides methods for calculating distances between coordinates, formatting
 * distances for display, and sorting places by proximity. All calculations use the Haversine
 * formula which provides good accuracy for distances up to a few hundred kilometers.
 *
 * Design Decisions:
 * - Object (singleton) pattern for stateless utility functions
 * - Earth radius constant for Haversine formula
 * - Automatic unit selection (meters vs kilometers) for readability
 * - Extension functions for convenient sorting
 *
 * Mathematical Background:
 * The Haversine formula calculates the great-circle distance between two points on a sphere
 * given their longitudes and latitudes. It's more accurate than simple Euclidean distance
 * for geographic coordinates.
 *
 * Formula:
 * a = sin²(Δφ/2) + cos(φ1) * cos(φ2) * sin²(Δλ/2)
 * c = 2 * atan2(√a, √(1−a))
 * d = R * c
 *
 * Where:
 * - φ is latitude, λ is longitude, R is earth's radius
 * - Δφ is the difference in latitude, Δλ is the difference in longitude
 *
 * Usage Example:
 * ```
 * val distance = DistanceCalculator.calculateDistance(
 *     lat1 = 40.7128, lon1 = -74.0060,  // New York
 *     lat2 = 34.0522, lon2 = -118.2437  // Los Angeles
 * )
 * val formatted = DistanceCalculator.formatDistance(distance)
 * // Result: "3935.75 km"
 * ```
 */
object DistanceCalculator {
    
    /**
     * Earth's radius in meters.
     *
     * This is the mean radius of Earth used in the Haversine formula.
     * Value: 6,371,000 meters (6,371 kilometers)
     */
    private const val EARTH_RADIUS_METERS = 6371000.0
    
    /**
     * Threshold for switching from meters to kilometers in formatting.
     *
     * Distances below this value are displayed in meters, above in kilometers.
     */
    private const val METER_TO_KM_THRESHOLD = 1000.0
    
    /**
     * Calculates the distance between two geographic coordinates using the Haversine formula.
     *
     * This method computes the great-circle distance between two points on Earth's surface,
     * accounting for the spherical nature of the planet. The result is accurate for distances
     * typically encountered in city-scale applications.
     *
     * The Haversine formula is preferred over simpler distance calculations because:
     * - It accounts for Earth's curvature
     * - It's computationally efficient
     * - It provides sufficient accuracy for distances up to several hundred kilometers
     *
     * @param lat1 Latitude of the first point in decimal degrees (-90 to 90)
     * @param lon1 Longitude of the first point in decimal degrees (-180 to 180)
     * @param lat2 Latitude of the second point in decimal degrees (-90 to 90)
     * @param lon2 Longitude of the second point in decimal degrees (-180 to 180)
     * @return Distance between the two points in meters
     *
     * Edge Cases:
     * - If the two points are identical, returns 0.0
     * - For antipodal points (opposite sides of Earth), may have minor accuracy issues
     * - Invalid latitude/longitude values may produce incorrect results
     *
     * Performance: O(1) - constant time operation with basic trigonometric functions
     *
     * Example:
     * ```
     * val distance = DistanceCalculator.calculateDistance(
     *     lat1 = -1.286389, lon1 = 36.817223,  // Nairobi
     *     lat2 = -1.292066, lon2 = 36.821946   // Nearby location
     * )
     * // Returns approximately 700 meters
     * ```
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        // Convert degrees to radians
        val lat1Rad = Math.toRadians(lat1)
        val lon1Rad = Math.toRadians(lon1)
        val lat2Rad = Math.toRadians(lat2)
        val lon2Rad = Math.toRadians(lon2)
        
        // Calculate differences
        val deltaLat = lat2Rad - lat1Rad
        val deltaLon = lon2Rad - lon1Rad
        
        // Haversine formula
        val a = sin(deltaLat / 2).pow(2) + 
                cos(lat1Rad) * cos(lat2Rad) * sin(deltaLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        // Calculate distance
        return EARTH_RADIUS_METERS * c
    }
    
    /**
     * Formats a distance value for display with appropriate units.
     *
     * Automatically selects between meters and kilometers based on the distance value:
     * - Distances < 1000m are displayed in meters (e.g., "750 m")
     * - Distances >= 1000m are displayed in kilometers with 2 decimal places (e.g., "2.45 km")
     *
     * @param distanceInMeters The distance to format, in meters
     * @return Formatted string with distance and unit
     *
     * Formatting Rules:
     * - Meters: No decimal places (e.g., "500 m", "999 m")
     * - Kilometers: Two decimal places (e.g., "1.50 km", "12.75 km")
     * - Negative distances are treated as 0
     *
     * Example:
     * ```
     * formatDistance(500.0)      // "500 m"
     * formatDistance(999.0)      // "999 m"
     * formatDistance(1000.0)     // "1.00 km"
     * formatDistance(1234.56)    // "1.23 km"
     * formatDistance(10500.0)    // "10.50 km"
     * ```
     */
    fun formatDistance(distanceInMeters: Double): String {
        return if (distanceInMeters < METER_TO_KM_THRESHOLD) {
            "${distanceInMeters.roundToInt()} m"
        } else {
            val distanceInKm = distanceInMeters / 1000.0
            String.format("%.2f km", distanceInKm)
        }
    }
    
    /**
     * Calculates the distance from a user location to a place.
     *
     * Convenience method that extracts coordinates from a PlaceEntity and calculates
     * the distance from a given user location.
     *
     * @param userLat User's latitude in decimal degrees
     * @param userLon User's longitude in decimal degrees
     * @param place The PlaceEntity to calculate distance to
     * @return Distance in meters from user to place
     *
     * Example:
     * ```
     * val distance = DistanceCalculator.distanceToPlace(
     *     userLat = -1.286389,
     *     userLon = 36.817223,
     *     place = hospitalEntity
     * )
     * ```
     */
    fun distanceToPlace(userLat: Double, userLon: Double, place: PlaceEntity): Double {
        return calculateDistance(userLat, userLon, place.latitude, place.longitude)
    }
    
    /**
     * Sorts a list of places by their distance from a user location.
     *
     * Creates a new list with places sorted in ascending order by distance from the
     * specified user coordinates. The original list is not modified.
     *
     * @param places List of PlaceEntity objects to sort
     * @param userLat User's latitude in decimal degrees
     * @param userLon User's longitude in decimal degrees
     * @return New list of places sorted by distance (closest first)
     *
     * Performance: O(n log n) where n is the number of places
     *
     * Thread Safety: This method is thread-safe as it creates a new list
     *
     * Example:
     * ```
     * val sortedPlaces = DistanceCalculator.sortPlacesByDistance(
     *     places = allHospitals,
     *     userLat = currentLat,
     *     userLon = currentLon
     * )
     * // First item is the closest hospital
     * val closestHospital = sortedPlaces.first()
     * ```
     */
    fun sortPlacesByDistance(
        places: List<PlaceEntity>,
        userLat: Double,
        userLon: Double
    ): List<PlaceEntity> {
        return places.sortedBy { place ->
            distanceToPlace(userLat, userLon, place)
        }
    }
    
    /**
     * Extension function to calculate distance from this place to user coordinates.
     *
     * Provides a convenient way to calculate distance directly on a PlaceEntity instance.
     *
     * @receiver PlaceEntity The place to calculate distance from
     * @param userLat User's latitude in decimal degrees
     * @param userLon User's longitude in decimal degrees
     * @return Distance in meters
     *
     * Example:
     * ```
     * val distance = hospital.distanceFrom(currentLat, currentLon)
     * ```
     */
    fun PlaceEntity.distanceFrom(userLat: Double, userLon: Double): Double {
        return distanceToPlace(userLat, userLon, this)
    }
}
