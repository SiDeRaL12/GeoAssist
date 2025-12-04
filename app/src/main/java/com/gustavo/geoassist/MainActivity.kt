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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.gustavo.geoassist.ui.theme.AppTheme
import com.gustavo.geoassist.ui.viewmodel.MapViewModel

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
 * Displays a list of places with category filtering functionality.
 *
 * @param viewModel The MapViewModel managing application state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeoAssistApp(viewModel: MapViewModel) {
    val places by viewModel.filteredPlaces.collectAsState()
    val showHospitals by viewModel.showHospitals.collectAsState()
    val showPolice by viewModel.showPolice.collectAsState()
    val showLibraries by viewModel.showLibraries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("GeoAssist") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = showHospitals,
                    onClick = { viewModel.toggleHospitals() },
                    label = { Text("Hospitals") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_hospital),
                            contentDescription = "Hospital"
                        )
                    }
                )
                FilterChip(
                    selected = showPolice,
                    onClick = { viewModel.togglePolice() },
                    label = { Text("Police") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_police),
                            contentDescription = "Police"
                        )
                    }
                )
                FilterChip(
                    selected = showLibraries,
                    onClick = { viewModel.toggleLibraries() },
                    label = { Text("Libraries") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_library),
                            contentDescription = "Library"
                        )
                    }
                )
            }
            
            // Loading indicator
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Places list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(places) { place ->
                    PlaceCard(place = place)
                }
            }
        }
    }
}

/**
 * Card composable displaying place information.
 *
 * @param place The PlaceEntity to display
 */
@Composable
fun PlaceCard(place: com.gustavo.geoassist.data.local.PlaceEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = place.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = place.category,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = place.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}