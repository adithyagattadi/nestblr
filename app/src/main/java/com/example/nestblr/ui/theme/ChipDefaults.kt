package com.example.nestblr.ui.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.runtime.Composable

/**
 * Brand-coloured FilterChip palette: selected state pulls from primary (coral),
 * not Material 3's default secondaryContainer (which would be teal in our theme
 * and is reserved for verified/available semantics).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun brandFilterChipColors(): SelectableChipColors = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
)
