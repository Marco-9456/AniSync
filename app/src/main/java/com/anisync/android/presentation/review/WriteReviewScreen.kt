package com.anisync.android.presentation.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.domain.ContentLimits
import com.anisync.android.presentation.components.richtext.RichTextScaffold
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

private val SummaryBounds = ContentLimits.ReviewSummary
private val BodyBounds = ContentLimits.ReviewBody

/**
 * Review editor. Wraps [RichTextScaffold] (the same rich-text editor used for
 * creating a forum thread) with a header carrying the review summary, score, and
 * private toggle, plus a delete action when editing the viewer's existing review.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteReviewScreen(
    onBackClick: () -> Unit,
    onReviewSaved: () -> Unit,
    viewModel: WriteReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is WriteReviewEvent.Saved, is WriteReviewEvent.Deleted -> onReviewSaved()
            }
        }
    }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            AppCircularProgressIndicator()
        }
        return
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.review_delete_confirm_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text(stringResource(R.string.review_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.onAction(WriteReviewAction.Delete)
                }) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.editor_discard_keep))
                }
            }
        )
    }

    val summaryValid = SummaryBounds.isValid(uiState.summary.trim().length)
    val scoreValid = uiState.score in 0..100
    // Dirty = differs from the loaded values, so opening an existing review and backing out
    // untouched doesn't trip the discard prompt.
    val externalUnsaved = uiState.summary != uiState.initialSummary ||
        uiState.isPrivate != uiState.initialIsPrivate ||
        uiState.score != uiState.initialScore

    RichTextScaffold(
        title = stringResource(
            if (uiState.isEditing) R.string.edit_review_title else R.string.write_review_title
        ),
        initialBody = uiState.initialBody,
        placeholder = stringResource(R.string.review_body_hint),
        submitLabel = stringResource(R.string.review_publish),
        isSubmitting = uiState.isSubmitting || uiState.isDeleting,
        onSubmit = { viewModel.onAction(WriteReviewAction.Submit) },
        onDismiss = onBackClick,
        onBodyChange = { viewModel.onAction(WriteReviewAction.OnBodyChange(it)) },
        isExternallyValid = summaryValid && scoreValid,
        hasExternalUnsavedChanges = externalUnsaved,
        autoFocusBody = false,
        minLength = BodyBounds.min,
        maxLength = BodyBounds.max,
        headerSlot = {
            ReviewMetaHeader(
                summary = uiState.summary,
                summaryError = uiState.summaryError,
                bodyError = uiState.bodyError,
                scoreError = uiState.scoreError,
                score = uiState.score,
                isPrivate = uiState.isPrivate,
                isEditing = uiState.isEditing,
                onSummaryChange = { viewModel.onAction(WriteReviewAction.OnSummaryChange(it)) },
                onScoreChange = { viewModel.onAction(WriteReviewAction.OnScoreChange(it)) },
                onPrivateChange = { viewModel.onAction(WriteReviewAction.OnPrivateChange(it)) },
                onDeleteClick = { showDeleteDialog = true }
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewMetaHeader(
    summary: String,
    summaryError: String?,
    bodyError: String?,
    scoreError: String?,
    score: Int,
    isPrivate: Boolean,
    isEditing: Boolean,
    onSummaryChange: (String) -> Unit,
    onScoreChange: (Int) -> Unit,
    onPrivateChange: (Boolean) -> Unit,
    onDeleteClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TextField(
            value = summary,
            onValueChange = { new ->
                onSummaryChange(if (new.length <= SummaryBounds.max) new else new.take(SummaryBounds.max))
            },
            placeholder = { Text(stringResource(R.string.review_summary_hint)) },
            isError = summaryError != null,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                errorContainerColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "${summary.trim().length} / ${SummaryBounds.max}",
            style = MaterialTheme.typography.labelSmall,
            color = if (summaryError != null) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp)
        )
        if (summaryError != null) {
            Text(
                text = stringResource(R.string.review_summary_error, SummaryBounds.min, SummaryBounds.max),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        // Score
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.review_score_label),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "$score",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = score.toFloat(),
            onValueChange = { onScoreChange(it.roundToInt()) },
            valueRange = 0f..100f,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (scoreError != null) {
            Text(
                text = stringResource(R.string.review_score_error, 0, 100),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Private toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.review_private_label),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Switch(checked = isPrivate, onCheckedChange = onPrivateChange)
        }

        if (bodyError != null) {
            Text(
                text = stringResource(R.string.review_body_error, BodyBounds.min),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        if (isEditing) {
            OutlinedButton(
                onClick = onDeleteClick,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.review_delete),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
    }
}
