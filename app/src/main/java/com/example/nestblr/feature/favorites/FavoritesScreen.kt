package com.example.nestblr.feature.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nestblr.feature.search.ListingCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onListingClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.favoriteError) {
        state.favoriteError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFavoriteError()
        }
    }

    // Reload on resume so a favorite toggled from a detail screen opened off this
    // list is reflected (e.g. un-favoriting there drops the row here on return).
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Favorites") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                // Wrapped in verticalScroll so pull-to-refresh registers on these
                // non-list states too (PullToRefreshBox needs a scrollable child).
                state.error != null -> CenteredScrollable {
                    ErrorState(message = state.error!!, onRetry = viewModel::load)
                }

                state.listings.isEmpty() -> CenteredScrollable { EmptyState() }

                else -> LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
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

/**
 * Centers short content but stays vertically scrollable, so the PullToRefreshBox
 * parent receives the nested-scroll events it needs to detect the pull gesture.
 */
@Composable
private fun CenteredScrollable(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.FavoriteBorder,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No favorites yet",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Tap the heart on any PG to save it",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Couldn't load favorites",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRetry, shape = RoundedCornerShape(12.dp)) { Text("Retry") }
    }
}
