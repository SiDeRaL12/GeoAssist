/**
 * OSMdroid map view composable for the GeoAssist application.
 *
 * This file provides a Compose wrapper around the OSMdroid MapView to display
 * an interactive map with place markers. It handles map initialization, marker
 * management, and user interaction.
 *
 * @author Gustavo Sanchez
 * @since 1.0
 */
package com.gustavo.geoassist.ui.components

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.gustavo.geoassist.data.local.PlaceEntity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Composable that displays an OSMdroid map with place markers.
 *
 * This composable wraps the OSMdroid MapView in AndroidView to make it compatible
 * with Jetpack Compose. It handles map configuration, marker placement, and
 * automatic updates when places or user location changes.
 *
 * Design Decisions:
 * - Uses AndroidView for OSMdroid integration
 * - Configures map with default MetroPoint coordinates
 * - Color-codes markers by category
 * - Automatically centers on user location when available
 * - Handles marker click events
 *
 * @param places List of places to display as markers
 * @param userLocation Optional user location as Pair of (latitude, longitude)
 * @param onMarkerClick Callback invoked when a marker is clicked
 * @param modifier Modifier for the composable
 */
@Composable
fun OsmMapView(
    places: List<PlaceEntity>,
    userLocation: Pair<Double, Double>?,
    onMarkerClick: (PlaceEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Initialize OSMdroid configuration once
    LaunchedEffect(Unit) {
        initializeOsmConfig(context)
    }
    
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            MapView(ctx).apply {
                // Configure map
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                
                // Set initial view to MetroPoint (Nairobi area)
                controller.setZoom(13.0)
                controller.setCenter(GeoPoint(-1.286389, 36.817223))
                
                // Enable zoom controls
                zoomController.setVisibility(
                    org.osmdroid.views.CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT
                )
            }
        },
        update = { mapView ->
            // Clear existing markers
            mapView.overlays.clear()
            
            // Add place markers
            places.forEach { place ->
                val marker = Marker(mapView).apply {
                    position = GeoPoint(place.latitude, place.longitude)
                    title = place.name
                    snippet = "${place.category} - ${place.address}"
                    
                    // Set marker click listener
                    setOnMarkerClickListener { _, _ ->
                        onMarkerClick(place)
                        true
                    }
                }
                mapView.overlays.add(marker)
            }
            
            // Add user location marker if available
            userLocation?.let { (lat, lon) ->
                val userMarker = Marker(mapView).apply {
                    position = GeoPoint(lat, lon)
                    title = "Your Location"
                    snippet = "Current position"
                }
                mapView.overlays.add(userMarker)
                
                // Center on user location
                mapView.controller.animateTo(GeoPoint(lat, lon))
            }
            
            // Refresh map
            mapView.invalidate()
        }
    )
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            // OSMdroid cleanup happens automatically
        }
    }
}

/**
 * Initializes OSMdroid configuration.
 *
 * Sets up the user agent and cache directory for OSMdroid.
 * Must be called before creating any MapView instances.
 *
 * @param context Application context
 */
private fun initializeOsmConfig(context: Context) {
    Configuration.getInstance().apply {
        userAgentValue = context.packageName
        osmdroidBasePath = context.filesDir
        osmdroidTileCache = context.cacheDir
    }
}
