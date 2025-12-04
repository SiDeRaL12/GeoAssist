/**
 * Main Activity for the GeoAssist application.
 *
 * Entry point for the application that sets up the Compose UI and applies the Material 3 theme.
 *
 * @author Gustavo Sanchez
 * @since 1.0
 */
package com.gustavo.geoassist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import com.gustavo.geoassist.ui.components.OsmMapView
import com.gustavo.geoassist.ui.theme.AppTheme
import com.gustavo.geoassist.ui.viewmodel.MapViewModel
import android.content.Intent
import android.provider.Settings

/**
 * Main Activity implementing the GeoAssist user interface.
 *
 * This activity serves as the entry point and hosts the Compose-based UI.
 * It initializes the MapViewModel and applies the custom Material 3 theme.
 */
class MainActivity : ComponentActivity() {
    
    private val viewModel: MapViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                GeoAssistApp(viewModel)
            }
        }
    }
}

/**
 * Main composable for GeoAssist application.
 *
 * Displays a map with POI markers, category filtering, and enhanced UI components.
 *
 * @param viewModel The MapViewModel managing application state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeoAssistApp(viewModel: MapViewModel) {
    val context = LocalContext.current
    val places by viewModel.filteredPlaces.collectAsState()
    val showHospitals by viewModel.showHospitals.collectAsState()
    val showPolice by viewModel.showPolice.collectAsState()
    val showLibraries by viewModel.showLibraries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val locationAccuracy by viewModel.locationAccuracy.collectAsState()
    
    // Dialog state
    var selectedPlace by remember { mutableStateOf<com.gustavo.geoassist.data.local.PlaceEntity?>(null) }
    
    // Permission state
    var hasLocationPermission by remember {
        mutableStateOf(com.gustavo.geoassist.domain.util.PermissionHandler.hasLocationPermissions(context))
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions.values.all { it }
    }
    
    // Request permissions on first launch
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                com.gustavo.geoassist.domain.util.PermissionHandler.REQUIRED_PERMISSIONS
            )
        }
    }
    
    // Check if GPS is enabled
    val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
    val isGpsEnabled = remember(userLocation) {
        locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
    }
    
    // Category colors for chips
    val hospitalColor = Color(0xFFE53935)
    val policeColor = Color(0xFF1E88E5)
    val libraryColor = Color(0xFF8E24AA)
    
    Scaffold(
        topBar = {
            // Top app bar with gradient and filter chips
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        )
                        .statusBarsPadding()
                ) {
                    // Title row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "GeoAssist",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Municipal Services Finder",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        
                        // Refresh button
                        IconButton(
                            onClick = { viewModel.refreshPlaces() },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                    
                    // Filter chips row - inside app bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        EnhancedFilterChip(
                            selected = showHospitals,
                            onClick = { viewModel.toggleHospitals() },
                            label = "Hospitals",
                            icon = R.drawable.ic_hospital,
                            selectedColor = hospitalColor
                        )
                        EnhancedFilterChip(
                            selected = showPolice,
                            onClick = { viewModel.togglePolice() },
                            label = "Police",
                            icon = R.drawable.ic_police,
                            selectedColor = policeColor
                        )
                        EnhancedFilterChip(
                            selected = showLibraries,
                            onClick = { viewModel.toggleLibraries() },
                            label = "Libraries",
                            icon = R.drawable.ic_library,
                            selectedColor = libraryColor
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        // Map takes the full content area with FAB overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OsmMapView(
                places = places,
                userLocation = userLocation,
                onMarkerClick = { place -> selectedPlace = place },
                modifier = Modifier.fillMaxSize()
            )
            
            // Place count - bottom left
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                shadowElevation = 4.dp
            ) {
                Text(
                    text = "${places.size} places",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            // FAB - bottom right
            FloatingActionButton(
                onClick = { viewModel.refreshPlaces() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 24.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_location),
                    contentDescription = "Center Map",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
    
    // Enhanced place detail dialog
    selectedPlace?.let { place ->
        PlaceDetailDialog(
            place = place,
            userLocation = userLocation,
            onDismiss = { selectedPlace = null }
        )
    }
}

/**
 * Enhanced filter chip with category-specific colors and animations.
 */
@Composable
fun EnhancedFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: Int,
    selectedColor: Color
) {
    val animatedColor by animateColorAsState(
        targetValue = if (selected) selectedColor else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(300),
        label = "chipColor"
    )
    
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(300),
        label = "contentColor"
    )
    
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = animatedColor,
        modifier = Modifier.height(40.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = label,
                modifier = Modifier.size(18.dp),
                tint = contentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor
            )
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    modifier = Modifier.size(16.dp),
                    tint = contentColor
                )
            }
        }
    }
}

/**
 * Error banner with icon and action button.
 */
@Composable
fun ErrorBanner(
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    actionLabel: String,
    onAction: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error // Using error color for icon
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            TextButton(
                onClick = onAction,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(actionLabel)
            }
        }
    }
}

/**
 * Enhanced place detail dialog with visual hierarchy and distance display.
 */
@Composable
fun PlaceDetailDialog(
    place: com.gustavo.geoassist.data.local.PlaceEntity,
    userLocation: Pair<Double, Double>?,
    onDismiss: () -> Unit
) {
    val categoryColor = when (place.category) {
        "Hospital" -> Color(0xFFE53935)
        "Police" -> Color(0xFF1E88E5)
        "Library" -> Color(0xFF8E24AA)
        else -> MaterialTheme.colorScheme.primary
    }
    
    val categoryIcon = when (place.category) {
        "Hospital" -> R.drawable.ic_hospital
        "Police" -> R.drawable.ic_police
        "Library" -> R.drawable.ic_library
        else -> R.drawable.ic_location
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = CircleShape,
                color = categoryColor.copy(alpha = 0.15f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(categoryIcon),
                        contentDescription = place.category,
                        modifier = Modifier.size(32.dp),
                        tint = categoryColor
                    )
                }
            }
        },
        title = {
            Text(
                text = place.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Category chip
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = categoryColor.copy(alpha = 0.15f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = place.category,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = categoryColor,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                HorizontalDivider()
                
                // Address
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = "Address",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = place.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Distance
                userLocation?.let { (userLat, userLon) ->
                    val distance = com.gustavo.geoassist.domain.util.DistanceCalculator.calculateDistance(
                        userLat, userLon, place.latitude, place.longitude
                    )
                    val formattedDistance = com.gustavo.geoassist.domain.util.DistanceCalculator.formatDistance(distance)
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Distance",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = formattedDistance,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                
                // Coordinates
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = "Coordinates",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "${String.format("%.4f", place.latitude)}, ${String.format("%.4f", place.longitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = categoryColor
                )
            ) {
                Text("Close")
            }
        }
    )
}