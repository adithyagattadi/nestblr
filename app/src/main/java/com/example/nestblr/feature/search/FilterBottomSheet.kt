package com.example.nestblr.feature.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    initial: FilterState,
    onDismiss: () -> Unit,
    onApply: (FilterState) -> Unit,
    onClearAll: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Local draft state — only applied when user taps Apply
    var draft by remember { mutableStateOf(initial) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Filters",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = {
                    draft = FilterState()
                    onClearAll()
                }) {
                    Text("Clear all")
                }
            }

            Spacer(Modifier.height(8.dp))

            // Gender
            SectionLabel("Looking for")
            ChipRow(
                options = listOf(
                    "MALE" to "Men",
                    "FEMALE" to "Women",
                    "COED" to "Co-ed"
                ),
                selected = draft.gender,
                onSelect = { draft = draft.copy(gender = it) }
            )

            Spacer(Modifier.height(16.dp))

            // PG type
            SectionLabel("Type")
            ChipRow(
                options = listOf(
                    "PG" to "PG",
                    "HOSTEL" to "Hostel",
                    "COLIVING" to "Coliving"
                ),
                selected = draft.pgType,
                onSelect = { draft = draft.copy(pgType = it) }
            )

            Spacer(Modifier.height(16.dp))

            // Food
            SectionLabel("Food")
            ChipRow(
                options = listOf(
                    "VEG" to "Veg",
                    "NON_VEG" to "Non-veg",
                    "BOTH" to "Both"
                ),
                selected = draft.food,
                onSelect = { draft = draft.copy(food = it) }
            )

            Spacer(Modifier.height(20.dp))

            // Rent range
            SectionLabel("Monthly rent")
            Text(
                "₹${"%,d".format(draft.minRent)} – ₹${"%,d".format(draft.maxRent)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            RangeSlider(
                value = draft.minRent.toFloat()..draft.maxRent.toFloat(),
                onValueChange = { range ->
                    // Round to nearest 500 for cleaner UX
                    val min = (range.start.toInt() / 500) * 500
                    val max = (range.endInclusive.toInt() / 500) * 500
                    draft = draft.copy(
                        minRent = min.coerceAtLeast(FilterState.MIN_RENT),
                        maxRent = max.coerceAtMost(FilterState.MAX_RENT)
                    )
                },
                valueRange = FilterState.MIN_RENT.toFloat()..FilterState.MAX_RENT.toFloat(),
                steps = 95   // (50000-2000)/500 - 1
            )

            Spacer(Modifier.height(24.dp))

            // Apply button
            Button(
                onClick = { onApply(draft) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Show results")
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun ChipRow(
    options: List<Pair<String, String>>,   // value to label
    selected: String?,
    onSelect: (String?) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            val isSelected = selected == value
            FilterChip(
                selected = isSelected,
                onClick = {
                    // Tap selected chip again to clear it
                    onSelect(if (isSelected) null else value)
                },
                label = { Text(label) }
            )
        }
    }
}