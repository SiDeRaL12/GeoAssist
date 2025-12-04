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
import android.graphics.drawable.Drawable
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.gustavo.geoassist.R
import com.gustavo.geoassist.data.local.PlaceEntity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay

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
 * - Color-codes markers by category with distinct icons
 * - Automatically centers on user location when available
 * - Handles marker click events
 * - Preserves compass, scale bar, and rotation overlays
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
    
    // Remember the map view and persistent overlays
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var compassOverlay by remember { mutableStateOf<CompassOverlay?>(null) }
    var scaleBarOverlay by remember { mutableStateOf<ScaleBarOverlay?>(null) }
    var rotationGestureOverlay by remember { mutableStateOf<RotationGestureOverlay?>(null) }
    
    // Track if we should animate to user location (only first time)
    var hasAnimatedToUser by remember { mutableStateOf(false) }
    
    // Initialize OSMdroid configuration once
    LaunchedEffect(Unit) {
        initializeOsmConfig(context)
    }
    
    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    mapViewRef = this
                    
                    // Configure map
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    
                    // Set initial view to MetroPoint (Nairobi area)
                    controller.setZoom(13.0)
                    controller.setCenter(GeoPoint(-1.286389, 36.817223))
                    
                    // Enable zoom controls with modern styling
                    zoomController.setVisibility(
                        org.osmdroid.views.CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT
                    )
                    
                    // Create and add compass overlay
                    compassOverlay = CompassOverlay(
                        ctx,
                        InternalCompassOrientationProvider(ctx),
                        this
                    ).apply {
                        enableCompass()
                    }
                    overlays.add(compassOverlay)
                    
                    // Create and add scale bar overlay with better positioning
                    scaleBarOverlay = ScaleBarOverlay(this).apply {
                        setCentred(false)
                        setScaleBarOffset(
                            (resources.displayMetrics.widthPixels / 2) - 100,
                            20
                        )
                    }
                    overlays.add(scaleBarOverlay)
                    
                    // Create and add rotation gesture overlay
                    rotationGestureOverlay = RotationGestureOverlay(this).apply {
                        isEnabled = true
                    }
                    overlays.add(rotationGestureOverlay)
                }
            },
            update = { mapView ->
                // Remove only marker overlays, preserve compass/scale/rotation
                val overlaysToRemove = mutableListOf<Overlay>()
                mapView.overlays.forEach { overlay ->
                    if (overlay is Marker) {
                        overlaysToRemove.add(overlay)
                    }
                }
                mapView.overlays.removeAll(overlaysToRemove)
                
                // Re-add persistent overlays if they were somehow removed
                compassOverlay?.let { compass ->
                    if (!mapView.overlays.contains(compass)) {
                        mapView.overlays.add(compass)
                    }
                }
                scaleBarOverlay?.let { scale ->
                    if (!mapView.overlays.contains(scale)) {
                        mapView.overlays.add(scale)
                    }
                }
                rotationGestureOverlay?.let { rotation ->
                    if (!mapView.overlays.contains(rotation)) {
                        mapView.overlays.add(rotation)
                    }
                }
                
                // Add place markers with category-specific colored icons
                places.forEach { place ->
                    val marker = Marker(mapView).apply {
                        position = GeoPoint(place.latitude, place.longitude)
                        title = place.name
                        snippet = "${place.category} - ${place.address}"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        
                        // Set category-specific icon
                        icon = when (place.category) {
                            "Hospital" -> ContextCompat.getDrawable(context, R.drawable.ic_hospital)
                            "Police" -> ContextCompat.getDrawable(context, R.drawable.ic_police)
                            "Library" -> ContextCompat.getDrawable(context, R.drawable.ic_library)
                            else -> ContextCompat.getDrawable(context, R.drawable.ic_location)
                        }
                        
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
                        icon = ContextCompat.getDrawable(context, R.drawable.ic_location_user)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    }
                    mapView.overlays.add(userMarker)
                    
                    // Animate to user location only once
                    if (!hasAnimatedToUser) {
                        mapView.controller.animateTo(GeoPoint(lat, lon), 15.0, 1000L)
                        hasAnimatedToUser = true
                    }
                }
                
                // Refresh map
                mapView.invalidate()
            }
        )
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            mapViewRef?.onDetach()
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
        // Enable tile caching for offline support
        expirationOverrideDuration = 1000L * 60 * 60 * 24 * 7 // 7 days
    }
}
