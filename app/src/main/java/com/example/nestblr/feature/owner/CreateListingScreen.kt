package com.example.nestblr.feature.owner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateListingScreen(
    onBack: () -> Unit,
    onCreated: () -> Unit,
    viewModel: CreateListingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.createdId) {
        if (state.createdId != null) onCreated()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create listing") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Basics
            SectionTitle("Basics")
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitle,
                label = { Text("Title *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescription,
                label = { Text("Description") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            // Location
            SectionTitle("Location")
            LocalityDropdown(
                selected = state.locality,
                onSelect = viewModel::onLocality
            )
            OutlinedTextField(
                value = state.addressLine,
                onValueChange = viewModel::onAddress,
                label = { Text("Address line *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.pincode,
                onValueChange = viewModel::onPincode,
                label = { Text("Pincode") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.contactPhone,
                onValueChange = { viewModel.onContactPhone(it.filter { c -> c.isDigit() || c == '+' }) },
                label = { Text("Contact phone *") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                supportingText = { Text("Tenants will call this number") },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Location is approximate (locality centroid). A map pin picker comes later.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Preferences
            SectionTitle("Preferences")
            LabeledChips("For", listOf("MALE" to "Men", "FEMALE" to "Women", "COED" to "Co-ed"),
                state.genderPreference, viewModel::onGender)
            LabeledChips("Type", listOf("PG" to "PG", "HOSTEL" to "Hostel", "COLIVING" to "Coliving"),
                state.pgType, viewModel::onPgType)
            LabeledChips("Food", listOf("VEG" to "Veg", "NON_VEG" to "Non-veg", "BOTH" to "Both", "NONE" to "None"),
                state.foodType, viewModel::onFoodType)

            // Rooms
            SectionTitle("Room options")
            state.rooms.forEachIndexed { index, room ->
                RoomOptionEditor(
                    index = index,
                    room = room,
                    canRemove = state.rooms.size > 1,
                    onChange = { viewModel.updateRoom(index, it) },
                    onRemove = { viewModel.removeRoom(index) }
                )
            }
            OutlinedButton(
                onClick = viewModel::addRoom,
                modifier = Modifier.fillMaxWidth()
            ) { Text("+ Add another room type") }

            // Amenities
            SectionTitle("Amenities")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AMENITIES.forEach { (id, name) ->
                    FilterChip(
                        selected = id in state.selectedAmenityIds,
                        onClick = { viewModel.toggleAmenity(id) },
                        label = { Text(name) }
                    )
                }
            }

            // Error
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            // Submit
            Button(
                onClick = viewModel::submit,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Create listing")
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalityDropdown(
    selected: Locality,
    onSelect: (Locality) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Locality *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Locality.entries.forEach { loc ->
                DropdownMenuItem(
                    text = { Text(loc.displayName) },
                    onClick = {
                        onSelect(loc)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun LabeledChips(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, text) ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = { Text(text) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoomOptionEditor(
    index: Int,
    room: RoomOptionForm,
    canRemove: Boolean,
    onChange: (RoomOptionForm) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Room ${index + 1}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (canRemove) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Remove room", modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Sharing type chips
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("SINGLE", "DOUBLE", "TRIPLE", "QUAD").forEach { type ->
                    FilterChip(
                        selected = room.sharingType == type,
                        onClick = { onChange(room.copy(sharingType = type)) },
                        label = { Text(type.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = room.monthlyRent,
                    onValueChange = { onChange(room.copy(monthlyRent = it.filter { c -> c.isDigit() })) },
                    label = { Text("Rent ₹") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = room.securityDeposit,
                    onValueChange = { onChange(room.copy(securityDeposit = it.filter { c -> c.isDigit() })) },
                    label = { Text("Deposit ₹") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = room.totalBeds,
                    onValueChange = { onChange(room.copy(totalBeds = it.filter { c -> c.isDigit() })) },
                    label = { Text("Total beds") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = room.availableBeds,
                    onValueChange = { onChange(room.copy(availableBeds = it.filter { c -> c.isDigit() })) },
                    label = { Text("Available") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}