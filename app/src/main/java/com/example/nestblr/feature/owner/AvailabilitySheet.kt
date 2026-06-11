package com.example.nestblr.feature.owner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailabilitySheet(
    listingId: String,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val viewModel = hiltViewModel<AvailabilityViewModel, AvailabilityViewModel.Factory>(
        key = listingId
    ) { factory -> factory.create(listingId) }

    val state by viewModel.state.collectAsState()
    val sheetState = rememberModalBottomSheetState()

    // (Re)load fresh room data every time the sheet opens. The ViewModel is
    // retained (scoped to OwnerHome), so we can't rely on init for this.
    LaunchedEffect(Unit) { viewModel.load() }

    // When all changes save, refresh the owner list then dismiss.
    LaunchedEffect(Unit) {
        viewModel.saveComplete.collect {
            onSaved()
            onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                "Update availability",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.rooms.isEmpty() -> {
                    Text(
                        "No rooms to update",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                }

                else -> {
                    state.rooms.forEach { room ->
                        RoomStepperRow(
                            room = room,
                            onDecrement = { viewModel.decrement(room.roomId) },
                            onIncrement = { viewModel.increment(room.roomId) }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            state.error?.let { message ->
                Spacer(Modifier.height(4.dp))
                Text(
                    "Save failed: $message",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.hasChanges && !state.isSaving
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save changes")
                }
            }
        }
    }
}

@Composable
private fun RoomStepperRow(
    room: AvailabilityRoomItem,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${room.sharingType} sharing",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        FilledTonalIconButton(
            onClick = onDecrement,
            enabled = room.currentAvailable > 0
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease")
        }
        Text(
            "${room.currentAvailable} / ${room.totalBeds}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .widthIn(min = 44.dp)
                .padding(horizontal = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        FilledTonalIconButton(
            onClick = onIncrement,
            enabled = room.currentAvailable < room.totalBeds
        ) {
            Icon(Icons.Default.Add, contentDescription = "Increase")
        }
    }
}
