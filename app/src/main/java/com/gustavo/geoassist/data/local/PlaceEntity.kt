/**
 * Room Entity representing a public service place in the GeoAssist application.
 *
 * This file defines the PlaceEntity class which serves as the data model for storing
 * information about public service locations (hospitals, police stations, libraries) in
 * the local Room database. Each place contains essential information including its name,
 * category, geographic coordinates, and address.
 *
 * The entity is designed to support offline functionality by caching data fetched from
 * the network, allowing the application to function without an internet connection.
 *
 * @author Gustavo Sanchez
 * @since 1.0
 */
package com.gustavo.geoassist.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity class representing a public service location.
 *
 * This class is used by Room to create and manage the places table in the local database.
 * It stores all necessary information about each public service location that users can
 * find on the map.
 *
 * Design Decisions:
 * - Uses Integer primary key for simple identification and efficient indexing
 * - Category field is String to allow flexible categorization
 * - Coordinates stored as Double for precision required by mapping libraries
 * - All fields are non-null to ensure data integrity
 *
 * Usage:
 * ```
 * val hospital = PlaceEntity(
 *     id = 1,
 *     name = "MetroPoint General Hospital",
 *     category = "Hospital",
 *     latitude = 40.7128,
 *     longitude = -74.0060,
 *     address = "123 Main St, MetroPoint"
 * )
 * ```
 *
 * @property id Unique identifier for the place (Primary Key)
 * @property name Display name of the public service location
 * @property category Type of service (Hospital, Police, or Library)
 * @property latitude Geographic latitude coordinate in decimal degrees
 * @property longitude Geographic longitude coordinate in decimal degrees
 * @property address Physical street address of the location
 */
@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey
    val id: Int,
    
    /**
     * Name of the public service location.
     * Examples: "Central Hospital", "Police Station 5", "Main Library"
     */
    val name: String,
    
    /**
     * Category of the public service.
     * Must be one of: "Hospital", "Police", or "Library"
     */
    val category: String,
    
    /**
     * Latitude coordinate in decimal degrees.
     * Range: -90.0 to 90.0
     */
    val latitude: Double,
    
    /**
     * Longitude coordinate in decimal degrees.
     * Range: -180.0 to 180.0
     */
    val longitude: Double,
    
    /**
     * Physical street address of the location.
     * Should include street, city, and any other relevant address information.
     */
    val address: String
)
