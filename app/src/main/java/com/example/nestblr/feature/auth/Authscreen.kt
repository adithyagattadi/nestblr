package com.example.nestblr.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nestblr.ui.theme.brandFilterChipColors
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val dobDisplayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val isSignup = state.mode == AuthMode.SIGNUP

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var genderExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.isAuthenticated) {
        if (state.isAuthenticated) onAuthenticated()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        // Coral logo dot
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "N",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "NestBLR",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Find your PG in Bengaluru",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(48.dp))

        Text(
            if (isSignup) "Create your account" else "Welcome back",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (isSignup)
                "Just a couple of details to get started"
            else
                "Log in to continue",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(Modifier.height(24.dp))

        // ── Signup-only: Full Name + Phone (above email) ──
        if (isSignup) {
            OutlinedTextField(
                value = state.fullName,
                onValueChange = viewModel::onFullNameChange,
                label = { Text("Full Name") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                isError = state.fullNameError != null,
                supportingText = state.fullNameError?.let { { ErrorText(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.phone,
                onValueChange = viewModel::onPhoneChange,
                label = { Text("Phone") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                isError = state.phoneError != null,
                supportingText = state.phoneError?.let { { ErrorText(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))
        }

        // ── Email (shared) ──
        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text("Email") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            isError = isSignup && state.emailError != null,
            supportingText = (state.emailError.takeIf { isSignup })?.let { { ErrorText(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // ── Password (shared) ──
        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text("Password") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isError = isSignup && state.passwordError != null,
            // In signup, persistently show the strength rule (or the error in its place).
            supportingText = if (isSignup) {
                {
                    Text(
                        state.passwordError ?: "At least 8 characters, with a letter and a digit",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.passwordError != null)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else null,
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        // ── Login-only: Forgot password link ──
        if (!isSignup) {
            TextButton(
                onClick = onForgotPasswordClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    "Forgot password?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // ── Signup-only: Confirm Password + DOB + Gender + Role ──
        if (isSignup) {
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                label = { Text("Confirm Password") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                isError = state.confirmPasswordError != null,
                supportingText = state.confirmPasswordError?.let { { ErrorText(it) } },
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // DOB — readOnly field with a transparent clickable overlay. A bare
            // Modifier.clickable on a readOnly OutlinedTextField is swallowed by the
            // field's internal focus handling, so the overlay guarantees the tap.
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.dob?.format(dobDisplayFormat) ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date of Birth") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    isError = state.dobError != null,
                    supportingText = state.dobError?.let { { ErrorText(it) } },
                    trailingIcon = {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Pick date")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showDatePicker = true }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Gender dropdown
            ExposedDropdownMenuBox(
                expanded = genderExpanded,
                onExpandedChange = { genderExpanded = it }
            ) {
                OutlinedTextField(
                    value = state.gender.display,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Gender (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = genderExpanded,
                    onDismissRequest = { genderExpanded = false }
                ) {
                    Gender.entries.forEach { gender ->
                        DropdownMenuItem(
                            text = { Text(gender.display) },
                            onClick = {
                                viewModel.onGenderChange(gender)
                                genderExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "I am a…",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RoleChip(
                    label = "Looking for a PG",
                    selected = state.role == "TENANT",
                    onClick = { viewModel.onRoleChange("TENANT") },
                    modifier = Modifier.weight(1f)
                )
                RoleChip(
                    label = "PG Owner",
                    selected = state.role == "OWNER",
                    onClick = { viewModel.onRoleChange("OWNER") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.Start)
            )
        }

        Spacer(Modifier.height(28.dp))

        val signupReady = state.fullName.isNotBlank() && state.phone.isNotBlank() &&
            state.email.isNotBlank() && state.password.isNotBlank() &&
            state.confirmPassword.isNotBlank() && state.dob != null
        val canSubmit = !state.isLoading && (!isSignup || signupReady)

        Button(
            onClick = viewModel::submit,
            enabled = canSubmit,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    if (isSignup) "Sign up" else "Log in",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        TextButton(onClick = viewModel::toggleMode) {
            Text(
                if (isSignup)
                    "Already have an account? Log in"
                else
                    "New here? Create an account",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(Modifier.height(24.dp))
    }

    // DatePicker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.dob?.toEpochDay()?.times(86_400_000L)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        viewModel.onDobChange(date)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ErrorText(message: String) {
    Text(
        message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                label,
                maxLines = 2,
                style = MaterialTheme.typography.labelLarge
            )
        },
        shape = RoundedCornerShape(12.dp),
        colors = brandFilterChipColors(),
        modifier = modifier.height(48.dp)
    )
}
