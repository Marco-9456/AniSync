package com.anisync.android.presentation.forum

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.presentation.components.AsyncRichTextRenderer
import com.anisync.android.presentation.forum.components.ForumCategoryChip
import kotlinx.coroutines.flow.collectLatest

private const val MAX_BODY_LENGTH = 10000

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateThreadScreen(
    onBackClick: () -> Unit,
    onThreadCreated: () -> Unit,
    viewModel: CreateThreadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val focusManager = LocalFocusManager.current
    var showDiscardDialog by remember { mutableStateOf(false) }

    // Handle unsaved changes on back press
    BackHandler(enabled = uiState.hasUnsavedChanges) {
        showDiscardDialog = true
    }

    LaunchedEffect(Unit) {
        viewModel.actions.collectLatest { action ->
            when (action) {
                is CreateThreadAction.NavigateUp -> onThreadCreated()
                else -> {}
            }
        }
    }

    // Discard changes confirmation dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard draft?", fontWeight = FontWeight.Bold) },
            text = { Text("You have unsaved changes. Are you sure you want to leave?") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onBackClick()
                }) {
                    Text("Discard", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep editing", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.forum_create_thread),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.hasUnsavedChanges) showDiscardDialog = true else onBackClick()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    // Preview toggle
                    IconButton(
                        onClick = { viewModel.onAction(CreateThreadAction.TogglePreview) },
                        enabled = uiState.body.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Preview,
                            contentDescription = if (uiState.isPreviewMode) "Edit" else "Preview",
                            tint = if (uiState.isPreviewMode) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (uiState.isSubmitting) {
                        Box(modifier = Modifier.padding(end = 16.dp)) {
                            CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                        }
                    } else {
                        IconButton(
                            onClick = { viewModel.onAction(CreateThreadAction.Submit) },
                            enabled = uiState.isValid
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.forum_post_thread)
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Title field
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.onAction(CreateThreadAction.OnTitleChange(it)) },
                label = { Text(stringResource(R.string.forum_thread_title)) },
                placeholder = { Text(stringResource(R.string.forum_thread_title_hint)) },
                isError = uiState.titleError != null,
                supportingText = uiState.titleError?.let { { Text(it) } },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Body field with character counter
            if (uiState.isPreviewMode) {
                // Markdown preview
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Preview",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    AsyncRichTextRenderer(
                        html = uiState.body,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Column {
                    OutlinedTextField(
                        value = uiState.body,
                        onValueChange = {
                            if (it.length <= MAX_BODY_LENGTH)
                                viewModel.onAction(CreateThreadAction.OnBodyChange(it))
                        },
                        label = { Text(stringResource(R.string.forum_thread_body)) },
                        placeholder = { Text(stringResource(R.string.forum_thread_body_hint)) },
                        isError = uiState.bodyError != null,
                        supportingText = uiState.bodyError?.let { { Text(it) } },
                        minLines = 6,
                        maxLines = 20,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Character counter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "${uiState.body.length} / $MAX_BODY_LENGTH",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (uiState.body.length > MAX_BODY_LENGTH * 0.9)
                                MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, end = 4.dp)
                        )
                    }
                }
            }

            // Category selection
            Text(
                text = stringResource(R.string.forum_select_categories),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (uiState.categoryError != null) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface
            )
            if (uiState.categoryError != null) {
                Text(
                    text = uiState.categoryError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                uiState.availableCategories.forEach { category ->
                    val isSelected = category.id in uiState.selectedCategoryIds
                    ForumCategoryChip(
                        category = category,
                        selected = isSelected,
                        onClick = { viewModel.onAction(CreateThreadAction.ToggleCategory(category.id)) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.onAction(CreateThreadAction.Submit) },
                enabled = uiState.isValid && !uiState.isSubmitting,
                shape = RoundedCornerShape(percent = 50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.forum_post_thread),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
