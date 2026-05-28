package com.example.nestblr.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.nestblr.domain.model.Gender
import com.example.nestblr.domain.model.ListingSummary
import com.example.nestblr.domain.model.PgType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onListingClick: (String) -> Unit,
    onSignOut: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showFilters by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NestBLR — PGs in Bengaluru") },
                actions = {
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
                    state.listings.isEmpty() -> {
                        EmptyState(
                            hasFilters = state.filters.hasActiveFilters,
                            onClearFilters = viewModel::clearFilters,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    "${state.listings.size} PGs near Koramangala",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            items(state.listings, key = { it.id }) { listing ->
                                ListingCard(
                                    listing = listing,
                                    onClick = { onListingClick(listing.id) }
                                )
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

@Composable
private fun ListingCard(
    listing: ListingSummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (listing.coverPhotoUrl != null) {
                AsyncImage(
                    model = listing.coverPhotoUrl,
                    contentDescription = listing.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
                Spacer(Modifier.height(12.dp))
            }

            Text(
                text = listing.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${listing.locality} · ${"%.1f".format(listing.distanceKm)} km away",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "${listing.avgRating} (${listing.reviewCount})",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.width(12.dp))
                Tag(text = genderLabel(listing.genderPreference))
                Spacer(Modifier.width(8.dp))
                Tag(text = pgTypeLabel(listing.pgType))
            }

            Spacer(Modifier.height(12.dp))

            listing.minRent?.let { rent ->
                Text(
                    text = "₹${"%,d".format(rent)}/month onwards",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun Tag(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer
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
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (hasFilters) "No PGs match your filters" else "No PGs in this area",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (hasFilters) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Try removing some filters",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onClearFilters) { Text("Clear filters") }
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
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Couldn't load PGs",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Retry") }
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