package com.example.nestblr.feature.search

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.example.nestblr.core.util.resolveBackendUrl
import com.example.nestblr.domain.model.Gender
import com.example.nestblr.domain.model.ListingSummary
import com.example.nestblr.domain.model.PgType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onListingClick: (String) -> Unit,
    onFavoritesClick: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showFilters by remember { mutableStateOf(false) }
    var showMap by remember { mutableStateOf(false) }
    var showLocalityPicker by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(state.favoriteError) {
        state.favoriteError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFavoriteError()
        }
    }
    LaunchedEffect(state.locationError) {
        state.locationError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearLocationError()
        }
    }

    // Permission flow lives in the UI, not the ViewModel. We re-check the grant
    // on every FAB tap rather than caching it — the user can revoke it in Settings.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.useCurrentLocation()
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    "Location permission denied. Enable it in app settings to use your location."
                )
            }
        }
    }
    val onLocationFabTapped: () -> Unit = {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.useCurrentLocation()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    // Reload on resume so a favorite toggled from the detail screen is reflected
    // here when the user returns. load() keeps the existing list visible (the
    // full-screen spinner only shows when listings is empty).
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.load()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    // Two-line header. Line 2 is tappable → opens the locality picker.
                    Column(
                        modifier = Modifier.clickable { showLocalityPicker = true }
                    ) {
                        Text(
                            "NestBLR",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "PGs near ${state.locationName}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Choose locality",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                actions = {
                    // List/Map toggle — single button, label flips with mode
                    IconButton(onClick = { showMap = !showMap }) {
                        Icon(
                            imageVector = if (showMap) Icons.AutoMirrored.Filled.List
                                          else Icons.Default.Map,
                            contentDescription = if (showMap) "Show list" else "Show map"
                        )
                    }
                    // Filter button with badge when active
                    BadgedBox(
                        badge = {
                            if (state.filters.activeCount > 0) {
                                Badge { Text("${state.filters.activeCount}") }
                            }
                        }
                    ) {
                        IconButton(onClick = { showFilters = true }) {
                            Icon(Icons.Default.Tune, contentDescription = "Filters")
                        }
                    }
                    IconButton(onClick = onFavoritesClick) {
                        Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Favorites")
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Active filter chips strip — only shown if filters active
            if (state.filters.hasActiveFilters) {
                ActiveFilterChips(
                    filters = state.filters,
                    onClearAll = viewModel::clearFilters,
                    onRemoveGender = { viewModel.applyFilters(state.filters.copy(gender = null)) },
                    onRemoveFood = { viewModel.applyFilters(state.filters.copy(food = null)) },
                    onRemovePgType = { viewModel.applyFilters(state.filters.copy(pgType = null)) },
                    onRemoveRent = {
                        viewModel.applyFilters(
                            state.filters.copy(
                                minRent = FilterState.MIN_RENT,
                                maxRent = FilterState.MAX_RENT
                            )
                        )
                    }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading && state.listings.isEmpty() -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    state.error != null -> {
                        ErrorState(
                            message = state.error!!,
                            onRetry = viewModel::load,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    // In map mode the map always renders — even with zero results it
                    // recenters to the picked locality (markers just empty out).
                    showMap -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            NestBlrMap(
                                centerLat = state.centerLat,
                                centerLng = state.centerLng,
                                listings = state.listings,
                                onMarkerClick = onListingClick,
                                showUserLocation = state.isUsingCurrentLocation,
                                modifier = Modifier.fillMaxSize()
                            )
                            // "Use my location" FAB — surface container + primary
                            // content reads against OSM Mapnik's light tiles; the
                            // FAB's own elevation/shadow separates it from the map.
                            SmallFloatingActionButton(
                                onClick = onLocationFabTapped,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp),
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                if (state.isFetchingLocation) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.MyLocation,
                                        contentDescription = "Use my location"
                                    )
                                }
                            }
                        }
                    }
                    state.listings.isEmpty() -> {
                        EmptyState(
                            hasFilters = state.filters.hasActiveFilters,
                            onClearFilters = viewModel::clearFilters,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        val refreshState = rememberPullToRefreshState()
                        PullToRefreshBox(
                            isRefreshing = state.isRefreshing,
                            onRefresh = viewModel::refresh,
                            state = refreshState,
                            indicator = {
                                PullToRefreshDefaults.Indicator(
                                    state = refreshState,
                                    isRefreshing = state.isRefreshing,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.TopCenter)
                                )
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp, end = 16.dp,
                                top = 12.dp, bottom = 24.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                Column {
                                    Text(
                                        "PGs near you",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "${state.listings.size} options around ${state.locationName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                            items(state.listings, key = { it.id }) { listing ->
                                ListingCard(
                                    listing = listing,
                                    onClick = { onListingClick(listing.id) },
                                    onToggleFavorite = viewModel::toggleFavorite
                                )
                            }
                        }
                        }
                    }
                }
            }
        }

        // Bottom sheet
        if (showFilters) {
            FilterBottomSheet(
                initial = state.filters,
                onDismiss = { showFilters = false },
                onApply = {
                    viewModel.applyFilters(it)
                    showFilters = false
                },
                onClearAll = {
                    viewModel.clearFilters()
                    showFilters = false
                }
            )
        }

        // Locality picker sheet
        if (showLocalityPicker) {
            LocalityPickerSheet(
                // No row highlighted while centered on the user's location.
                selected = if (state.isUsingCurrentLocation) null else state.selectedLocality,
                onDismiss = { showLocalityPicker = false },
                onLocalitySelected = { locality ->
                    viewModel.onLocalityChanged(locality)
                    showLocalityPicker = false
                }
            )
        }
    }
}

@Composable
private fun ActiveFilterChips(
    filters: FilterState,
    onClearAll: () -> Unit,
    onRemoveGender: () -> Unit,
    onRemoveFood: () -> Unit,
    onRemovePgType: () -> Unit,
    onRemoveRent: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        filters.gender?.let { value ->
            DismissibleChip(label = genderLabel(Gender.from(value)), onDismiss = onRemoveGender)
        }
        filters.pgType?.let { value ->
            DismissibleChip(label = pgTypeLabel(PgType.from(value)), onDismiss = onRemovePgType)
        }
        filters.food?.let { value ->
            DismissibleChip(label = foodLabel(value), onDismiss = onRemoveFood)
        }
        if (filters.minRent != FilterState.MIN_RENT || filters.maxRent != FilterState.MAX_RENT) {
            DismissibleChip(
                label = "₹${filters.minRent / 1000}k–₹${filters.maxRent / 1000}k",
                onDismiss = onRemoveRent
            )
        }
        TextButton(onClick = onClearAll) {
            Text("Clear", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun DismissibleChip(label: String, onDismiss: () -> Unit) {
    AssistChip(
        onClick = onDismiss,
        label = { Text(label) },
        trailingIcon = {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove $label",
                modifier = Modifier.size(16.dp)
            )
        }
    )
}

/**
 * Heart overlay for the card cover. Sits on a translucent dark scrim so a white
 * outline (not-favorited) reads on light photos and the coral fill reads on dark
 * ones. The IconButton consumes its own tap, so it does not trigger the card click.
 */
@Composable
private fun FavoriteHeart(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.32f))
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite
                              else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isFavorite) "Remove from favorites"
                                     else "Add to favorites",
                tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Shared listing card — used by both search and favorites. [onToggleFavorite]
 * receives (listingId, currentlyFavorite) and is invoked by the heart overlay.
 */
@Composable
internal fun ListingCard(
    listing: ListingSummary,
    onClick: () -> Unit,
    onToggleFavorite: (listingId: String, currentlyFavorite: Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column {
            // Edge-to-edge photo with a heart overlay in the top-right corner.
            Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                if (listing.coverPhotoUrl != null) {
                    AsyncImage(
                        model = resolveBackendUrl(listing.coverPhotoUrl),
                        contentDescription = listing.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.HomeWork,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                FavoriteHeart(
                    isFavorite = listing.isFavorite,
                    onClick = { onToggleFavorite(listing.id, listing.isFavorite) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }

            Column(modifier = Modifier.padding(14.dp)) {
                // Title row with rating right-aligned
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = listing.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    if (listing.reviewCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                "%.1f".format(listing.avgRating),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(3.dp))
                    val distanceText = listing.distanceKm?.let { "${"%.1f".format(it)} km away" }
                    val supportingLine =
                        listOfNotNull(listing.locality, distanceText).joinToString(" · ")
                    Text(
                        text = supportingLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Tag(text = genderLabel(listing.genderPreference))
                    Spacer(Modifier.width(6.dp))
                    Tag(text = pgTypeLabel(listing.pgType))
                }

                Spacer(Modifier.height(12.dp))

                listing.minRent?.let { rent ->
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "from ₹${"%,d".format(rent)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            " /month",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Tag(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyState(
    hasFilters: Boolean,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.HomeWork,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            if (hasFilters) "No PGs match your filters" else "No PGs in this area",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (hasFilters) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Try removing some filters to broaden the search.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
            OutlinedButton(
                onClick = onClearFilters,
                shape = RoundedCornerShape(12.dp)
            ) { Text("Clear filters") }
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Couldn't load PGs",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp)
        ) { Text("Retry") }
    }
}

private fun genderLabel(g: Gender) = when (g) {
    Gender.MALE -> "Men"
    Gender.FEMALE -> "Women"
    Gender.COED -> "Co-ed"
    Gender.UNKNOWN -> "—"
}

private fun pgTypeLabel(t: PgType) = when (t) {
    PgType.PG -> "PG"
    PgType.HOSTEL -> "Hostel"
    PgType.COLIVING -> "Coliving"
    PgType.UNKNOWN -> "—"
}

private fun foodLabel(value: String) = when (value.uppercase()) {
    "VEG" -> "Veg"
    "NON_VEG" -> "Non-veg"
    "BOTH" -> "Veg & Non-veg"
    else -> value
}