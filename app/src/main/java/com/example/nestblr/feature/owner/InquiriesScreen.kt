package com.example.nestblr.feature.owner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ContactPhone
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
import com.example.nestblr.data.remote.dto.InquirySummaryDto
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InquiriesScreen(
    onBack: () -> Unit,
    viewModel: InquiriesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inquiries") },
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
                // Empty/error states are wrapped in a verticalScroll (CenteredScrollable)
                // so the pull-to-refresh gesture registers even with no scrollable list.
                state.error != null -> CenteredScrollable {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.ContactPhone,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Couldn't load inquiries", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            state.error!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = viewModel::load) { Text("Retry") }
                    }
                }
                state.items.isEmpty() -> CenteredScrollable {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.ContactPhone,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No inquiries yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "When tenants tap Call owner on your listings, you'll see them here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp,
                            top = 12.dp, bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(state.items, key = { it.listingId }) { item ->
                            InquiryCard(item)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Centers short content but stays vertically scrollable, so the PullToRefreshBox
 * parent receives the nested-scroll events it needs to detect the pull gesture
 * (a plain centered Column emits none — empty/error states wouldn't refresh).
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
private fun InquiryCard(item: InquirySummaryDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.listingTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Last inquiry ${relativeInquiryTime(item.lastInquiryAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            InquirerPill(item.uniqueInquirerCount)
        }
    }
}

@Composable
private fun InquirerPill(count: Int) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            text = if (count == 1) "1 inquirer" else "$count inquirers",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/**
 * Parses the timestamp the backend sends for lastInquiryAt. It is a raw Postgres
 * timestamptz string — e.g. "2026-06-13 07:45:55.403238+05:30": a SPACE separator
 * (not 'T') and a numeric offset (not 'Z'), so strict Instant.parse fails on it.
 * We normalize the space to 'T' and parse as an offset date-time, with Instant.parse
 * fallbacks in case the format ever tightens to canonical ISO-8601 ("…Z").
 */
private fun parseTimestamp(raw: String): Instant? {
    val normalized = raw.replaceFirst(' ', 'T')
    return runCatching { OffsetDateTime.parse(normalized).toInstant() }.getOrNull()
        ?: runCatching { Instant.parse(normalized) }.getOrNull()
        ?: runCatching { Instant.parse(raw) }.getOrNull()
}

/**
 * Human-readable "time since". Falls back to a neutral phrase if the timestamp is
 * somehow unparseable rather than crashing the row.
 */
private fun relativeInquiryTime(iso: String): String {
    val then = parseTimestamp(iso) ?: return "recently"
    val minutes = Duration.between(then, Instant.now()).toMinutes()
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> if (minutes == 1L) "1 minute ago" else "$minutes minutes ago"
        minutes < 60 * 24 -> (minutes / 60).let { if (it == 1L) "1 hour ago" else "$it hours ago" }
        minutes < 60 * 24 * 7 -> (minutes / (60 * 24)).let { if (it == 1L) "1 day ago" else "$it days ago" }
        else -> "on " + then.atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
    }
}
