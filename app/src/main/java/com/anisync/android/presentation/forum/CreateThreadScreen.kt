package com.anisync.android.presentation.forum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateThreadScreen(
    onBackClick: () -> Unit,
    onThreadCreated: () -> Unit,
    viewModel: CreateThreadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.actions.collectLatest { action ->
            when (action) {
                is CreateThreadAction.NavigateUp -> onThreadCreated()
                else -> {}
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.forum_create_thread)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
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
                modifier = Modifier.fillMaxWidth()
            )

            // Body field
            OutlinedTextField(
                value = uiState.body,
                onValueChange = { viewModel.onAction(CreateThreadAction.OnBodyChange(it)) },
                label = { Text(stringResource(R.string.forum_thread_body)) },
                placeholder = { Text(stringResource(R.string.forum_thread_body_hint)) },
                isError = uiState.bodyError != null,
                supportingText = uiState.bodyError?.let { { Text(it) } },
                minLines = 6,
                maxLines = 20,
                modifier = Modifier.fillMaxWidth()
            )

            // Category selection
            Text(
                text = stringResource(R.string.forum_select_categories),
                style = MaterialTheme.typography.labelLarge,
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
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                uiState.availableCategories.forEach { category ->
                    val isSelected = category.id in uiState.selectedCategoryIds
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onAction(CreateThreadAction.ToggleCategory(category.id)) },
                        label = { Text(category.name) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null
                                )
                            }
                        } else null
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.onAction(CreateThreadAction.Submit) },
                enabled = uiState.isValid && !uiState.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(stringResource(R.string.forum_post_thread))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
