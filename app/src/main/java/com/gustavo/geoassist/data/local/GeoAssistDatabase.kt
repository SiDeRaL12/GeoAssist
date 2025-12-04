/**
 * Room Database class for the GeoAssist application.
 *
 * This file defines the GeoAssistDatabase class which serves as the main access point
 * to the application's persisted relational data. It uses Room, a persistence library
 * that provides an abstraction layer over SQLite for database operations.
 *
 * The database implements the Singleton pattern to ensure only one instance exists
 * throughout the application lifecycle, preventing multiple database connections and
 * potential data inconsistencies.
 *
 * @author Gustavo Sanchez
 * @since 1.0
 */
package com.gustavo.geoassist.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Abstract Room Database class for GeoAssist.
 *
 * This class defines the database configuration and serves as the main access point
 * for the underlying SQLite database. Room uses this class to generate the database
 * implementation at compile time.
 *
 * Design Decisions:
 * - Singleton pattern ensures single database instance across the app
 * - Double-checked locking for thread-safe singleton initialization
 * - Database version 1 as initial implementation
 * - Export schema disabled for development (can be enabled for production)
 *
 * Database Configuration:
 * - Database Name: "geoassist_database"
 * - Version: 1
 * - Entities: PlaceEntity
 * - Export Schema: false (set to true for production releases)
 *
 * Thread Safety:
 * The getInstance method uses synchronized double-checked locking to ensure
 * thread-safe initialization of the singleton instance.
 *
 * Usage Example:
 * ```
 * // Get database instance
 * val database = GeoAssistDatabase.getInstance(context)
 *
 * // Access DAO
 * val placeDao = database.placeDao()
 *
 * // Perform database operations
 * placeDao.getAllPlaces().collect { places ->
 *     // Use places
 * }
 * ```
 *
 * Migration Strategy:
 * When the database schema changes:
 * 1. Increment the version number
 * 2. Provide Migration objects to Room.databaseBuilder()
 * 3. Enable exportSchema and store schema files in version control
 * 4. Test migrations thoroughly before release
 *
 * @property placeDao Abstract method providing access to PlaceDao
 */
@Database(
    entities = [PlaceEntity::class],
    version = 1,
    exportSchema = false
)
abstract class GeoAssistDatabase : RoomDatabase() {
    
    /**
     * Provides access to the PlaceDao for database operations.
     *
     * Room generates the implementation of this method at compile time,
     * returning a concrete instance of the PlaceDao interface.
     *
     * @return PlaceDao instance for accessing place data
     */
    abstract fun placeDao(): PlaceDao
    
    companion object {
        /**
         * Volatile instance variable for singleton pattern.
         *
         * The Volatile annotation ensures that writes to this variable are immediately
         * visible to other threads, preventing issues with multiple threads accessing
         * the singleton.
         */
        @Volatile
        private var INSTANCE: GeoAssistDatabase? = null
        
        /**
         * Gets the singleton instance of the GeoAssistDatabase.
         *
         * This method implements thread-safe lazy initialization using double-checked
         * locking. It creates the database only when first accessed and ensures only
         * one instance exists even when called from multiple threads simultaneously.
         *
         * Implementation Details:
         * - First check: Avoids unnecessary synchronization after initialization
         * - Synchronized block: Ensures thread safety during initialization
         * - Second check: Prevents race condition where multiple threads
         *   could create instances
         *
         * The database is created with fallback to destructive migration for development.
         * For production, proper migration strategies should be implemented.
         *
         * @param context Application context used to create the database.
         *                Should use applicationContext to avoid memory leaks.
         * @return Singleton instance of GeoAssistDatabase
         *
         * Thread Safety: This method is thread-safe and can be called from multiple
         *                threads concurrently.
         *
         * Example:
         * ```
         * // In Application class or dependency injection
         * val database = GeoAssistDatabase.getInstance(applicationContext)
         *
         * // In Repository
         * class PlaceRepository(context: Context) {
         *     private val database = GeoAssistDatabase.getInstance(context)
         *     private val placeDao = database.placeDao()
         * }
         * ```
         *
         * Important Notes:
         * - Always pass applicationContext to avoid memory leaks
         * - The instance persists for the lifetime of the application
         * - Database operations should be performed on background threads
         */
        fun getInstance(context: Context): GeoAssistDatabase {
            // First check without synchronization for performance
            return INSTANCE ?: synchronized(this) {
                // Second check within synchronized block
                val instance = INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GeoAssistDatabase::class.java,
                    "geoassist_database"
                )
                    .fallbackToDestructiveMigration() // For development only
                    .build()
                
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Clears the database instance.
         *
         * This method is primarily for testing purposes to reset the singleton
         * state between test cases. It should NOT be called in production code.
         *
         * Important: This only clears the reference. The database connection
         * should be properly closed before calling this method.
         *
         * @suppress This method is internal and should only be used for testing
         */
        @androidx.annotation.VisibleForTesting
        fun clearInstance() {
            INSTANCE = null
        }
    }
}
