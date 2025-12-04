/**
 * Permission handler for location permissions in the GeoAssist application.
 *
 * This file provides utility functions for checking and requesting location permissions.
 * It simplifies permission handling for Compose-based UI components.
 *
 * @author Gustavo Sanchez
 * @since 1.0
 */
package com.gustavo.geoassist.domain.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Utility object for handling location permissions.
 *
 * Provides methods to check if location permissions are granted and determine
 * if the app has the necessary permissions to access user location.
 */
object PermissionHandler {
    
    /**
     * Required location permissions for the application.
     */
    val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    
    /**
     * Checks if all required location permissions are granted.
     *
     * @param context Application or activity context
     * @return true if all permissions are granted, false otherwise
     */
    fun hasLocationPermissions(context: Context): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
