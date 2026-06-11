package com.example.nestblr.feature.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nestblr.data.remote.dto.ReviewDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewSheet(
    listingId: String,
    existingReview: ReviewDto?,
    onDismiss: () -> Unit,
    onComplete: (ReviewDto?) -> Unit
) {
    val viewModel = hiltViewModel<ReviewViewModel, ReviewViewModel.Factory>(key = listingId) {
        factory -> factory.create(listingId)
    }
    val state by viewModel.state.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    val isEditing = existingReview != null

    // Reset to the passed review each time the sheet opens (the VM is retained).
    LaunchedEffect(Unit) {
        viewModel.setInitial(existingReview?.rating ?: 0, existingReview?.comment.orEmpty())
    }

    // One-shot completion → hand the result back and dismiss.
    LaunchedEffect(Unit) {
        viewModel.submitComplete.collect { result -> onComplete(result) }
    }

    // Auto-focus the comment field the first time the user picks a rating (create
    // flow only) — not on open, so the cursor doesn't land in the comment first.
    val commentFocus = remember { FocusRequester() }
    var didAutoFocus by remember { mutableStateOf(isEditing) }
    LaunchedEffect(state.rating) {
        if (state.rating > 0 && !didAutoFocus) {
            didAutoFocus = true
            commentFocus.requestFocus()
        }
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    val busy = state.isSubmitting || state.isDeleting

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                if (isEditing) "Edit your review" else "Write a review",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            // Star selector
            Row(verticalAlignment = Alignment.CenterVertically) {
                (1..5).forEach { star ->
                    val filled = star <= state.rating
                    Icon(
                        imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "$star star${if (star > 1) "s" else ""}",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .size(40.dp)
                            .clickable(enabled = !busy) { viewModel.onRatingChange(star) }
                            .padding(4.dp)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (state.rating == 0) "Tap to rate" else "${state.rating} of 5 stars",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = state.comment,
                onValueChange = viewModel::onCommentChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(commentFocus),
                label = { Text("Your review") },
                minLines = 3,
                maxLines = 6,
                enabled = !busy,
                supportingText = {
                    Text(
                        "${state.comment.length}/${ReviewViewModel.MAX_COMMENT}",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
            )

            state.error?.let { msg ->
                Text(
                    msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(8.dp))

            // Enabled whenever not busy (not gated on rating/comment) so tapping it
            // with an incomplete form surfaces the inline validation messages —
            // the test plan exercises the rating==0 error path explicitly.
            Button(
                onClick = viewModel::submit,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                enabled = !busy
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (isEditing) "Update review" else "Submit review")
                }
            }

            if (isEditing) {
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    if (state.isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text("Delete review")
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete your review?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
