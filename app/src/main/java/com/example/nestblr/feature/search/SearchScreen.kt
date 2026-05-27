package com.example.nestblr.feature.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NestBLR — PGs in Bengaluru") }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
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
                    Text(
                        "No PGs found in this area",
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
                            ListingCard(listing)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListingCard(listing: ListingSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cover image placeholder
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