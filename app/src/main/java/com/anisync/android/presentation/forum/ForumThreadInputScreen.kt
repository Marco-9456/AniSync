package com.anisync.android.presentation.forum

import com.anisync.android.presentation.components.AppModalBottomSheet
import com.anisync.android.ui.theme.emphasis
import com.anisync.android.domain.url

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.ContentLimits
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.presentation.components.alert.LocalToastManager
import com.anisync.android.presentation.components.alert.OverlayToastHost
import com.anisync.android.presentation.components.richtext.RichTextScaffold
import com.anisync.android.presentation.components.richtext.rememberRichTextInsertController
import com.anisync.android.presentation.forum.components.ForumCategoryChip
import com.anisync.android.type.MediaType
import kotlinx.coroutines.flow.collectLatest

private val TitleBounds = ContentLimits.ThreadTitle
private val BodyBounds = ContentLimits.ThreadBody

/**
 * Forum thread create screen. Wraps [RichTextScaffold] with a header slot
 * carrying the title field, category chips, related-media chips, and inline
 * error texts. Also owns the categories + AniList media search bottom sheet —
 * selecting a media result attaches it to the thread's `mediaCategories`
 * (a separate signal from forum [com.anisync.android.domain.ForumCategory]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumThreadInputScreen(
    onBackClick: () -> Unit,
    onThreadCreated: () -> Unit,
    viewModel: CreateThreadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val insertController = rememberRichTextInsertController()

    var showCategorySheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.actions.collectLatest { action ->
            if (action is CreateThreadAction.NavigateUp) onThreadCreated()
        }
    }

    if (showCategorySheet) {
        CategoriesAndMediaSheet(
            uiState = uiState,
            onToggleCategory = { viewModel.onAction(CreateThreadAction.ToggleCategory(it)) },
            onSearchQueryChange = { viewModel.onAction(CreateThreadAction.OnMediaSearchQueryChange(it)) },
            onSearchTypeChange = { viewModel.onAction(CreateThreadAction.OnMediaSearchTypeChange(it)) },
            onSelectMedia = { entry ->
                // Keep the sheet open so the user can attach a second media (cap is
                // MAX_THREAD_MEDIA_CATEGORIES) without reopening it each time.
                viewModel.onAction(CreateThreadAction.AddMediaCategory(entry))
            },
            onRemoveMedia = { mediaId ->
                viewModel.onAction(CreateThreadAction.RemoveMediaCategory(mediaId))
            },
            onDismiss = { showCategorySheet = false }
        )
    }

    // Dirty = differs from the as-opened form: a media pre-attached by the "start discussion"
    // entry point doesn't count, so backing out untouched skips the discard prompt.
    val externalUnsaved = uiState.title.isNotBlank() ||
        uiState.selectedCategoryIds.isNotEmpty() ||
        uiState.selectedMediaCategories.map { it.mediaId } != uiState.prefilledMediaCategoryIds
    val externallyValid = TitleBounds.isValid(uiState.title.trim().length) &&
        uiState.selectedCategoryIds.isNotEmpty()

    RichTextScaffold(
        title = stringResource(R.string.forum_create_thread),
        initialBody = uiState.body,
        placeholder = stringResource(R.string.forum_thread_body_hint),
        submitLabel = stringResource(R.string.post),
        isSubmitting = uiState.isSubmitting,
        onSubmit = { viewModel.onAction(CreateThreadAction.Submit) },
        onDismiss = onBackClick,
        onBodyChange = { viewModel.onAction(CreateThreadAction.OnBodyChange(it)) },
        isExternallyValid = externallyValid,
        hasExternalUnsavedChanges = externalUnsaved,
        autoFocusBody = false,
        minLength = BodyBounds.min,
        maxLength = BodyBounds.max,
        insertController = insertController,
        headerSlot = {
            ThreadMetaHeader(
                title = uiState.title,
                titleError = uiState.titleError,
                categoryError = uiState.categoryError,
                bodyError = uiState.bodyError,
                selectedCategoryIds = uiState.selectedCategoryIds,
                availableCategories = uiState.availableCategories,
                selectedMediaCategories = uiState.selectedMediaCategories,
                onTitleChange = { viewModel.onAction(CreateThreadAction.OnTitleChange(it)) },
                onTitleImeNext = { focusManager.moveFocus(FocusDirection.Down) },
                onToggleCategory = { viewModel.onAction(CreateThreadAction.ToggleCategory(it)) },
                onRemoveMediaCategory = { viewModel.onAction(CreateThreadAction.RemoveMediaCategory(it)) },
                onOpenCategorySheet = { showCategorySheet = true }
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThreadMetaHeader(
    title: String,
    titleError: String?,
    categoryError: String?,
    bodyError: String?,
    selectedCategoryIds: Set<Int>,
    availableCategories: List<com.anisync.android.domain.ForumCategory>,
    selectedMediaCategories: List<LibraryEntry>,
    onTitleChange: (String) -> Unit,
    onTitleImeNext: () -> Unit,
    onToggleCategory: (Int) -> Unit,
    onRemoveMediaCategory: (Int) -> Unit,
    onOpenCategorySheet: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TextField(
            value = title,
            onValueChange = { new ->
                // Clamp at the AniList-enforced max so the field can't run past
                // the limit; counter tops out instead of accepting and rejecting on submit.
                onTitleChange(if (new.length <= TitleBounds.max) new else new.take(TitleBounds.max))
            },
            placeholder = {
                Text(
                    text = stringResource(R.string.forum_thread_title_hint),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            textStyle = MaterialTheme.typography.titleLarge.emphasis(),
            singleLine = true,
            isError = titleError != null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { onTitleImeNext() }),
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
            text = "${title.trim().length} / ${TitleBounds.max}",
            style = MaterialTheme.typography.labelSmall,
            color = if (titleError != null) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp)
        )

        if (titleError != null) {
            Text(
                text = titleError,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val hasSelection = selectedCategoryIds.isNotEmpty()

            Surface(
                onClick = onOpenCategorySheet,
                shape = RoundedCornerShape(percent = 50),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = if (hasSelection) 10.dp else 16.dp,
                        vertical = 8.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.forum_add_categories),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    if (!hasSelection) {
                        Text(
                            text = stringResource(R.string.forum_categories_title),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            availableCategories
                .filter { it.id in selectedCategoryIds }
                .forEach { category ->
                    ForumCategoryChip(
                        category = category,
                        selected = true,
                        onClick = { onToggleCategory(category.id) }
                    )
                }

            selectedMediaCategories.forEach { entry ->
                MediaCategoryChip(
                    entry = entry,
                    onRemove = { onRemoveMediaCategory(entry.mediaId) }
                )
            }
        }

        if (categoryError != null) {
            Text(
                text = categoryError,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
        }

        if (bodyError != null) {
            Text(
                text = bodyError,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CategoriesAndMediaSheet(
    uiState: CreateThreadUiState,
    onToggleCategory: (Int) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchTypeChange: (MediaType) -> Unit,
    onSelectMedia: (LibraryEntry) -> Unit,
    onRemoveMedia: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        // Hosted inside the sheet so limit toasts render above the sheet's scrim
        // (the global host sits in the app window, behind it).
        OverlayToastHost(toastManager = LocalToastManager.current)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.forum_categories_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

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
                        enabled = uiState.isCategoryEnabled(category.id),
                        onClick = { onToggleCategory(category.id) }
                    )
                }
            }

            // AniList Apps threads don't carry related media — hide the whole picker.
            if (!uiState.isMediaPickerHidden) {
                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                MediaSearchSection(
                    query = uiState.mediaSearchQuery,
                    searchType = uiState.mediaSearchType,
                    results = uiState.mediaSearchResults,
                    isSearching = uiState.isMediaSearching,
                    error = uiState.mediaSearchError,
                    selectedMediaIds = uiState.selectedMediaCategories.map { it.mediaId }.toSet(),
                    onQueryChange = onSearchQueryChange,
                    onTypeChange = onSearchTypeChange,
                    onResultClick = { entry ->
                        val isAlreadySelected = uiState.selectedMediaCategories.any { it.mediaId == entry.mediaId }
                        if (isAlreadySelected) onRemoveMedia(entry.mediaId) else onSelectMedia(entry)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaSearchSection(
    query: String,
    searchType: MediaType,
    results: List<LibraryEntry>,
    isSearching: Boolean,
    error: String?,
    selectedMediaIds: Set<Int>,
    onQueryChange: (String) -> Unit,
    onTypeChange: (MediaType) -> Unit,
    onResultClick: (LibraryEntry) -> Unit
) {
    Text(
        text = stringResource(R.string.forum_link_media_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(8.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = searchType == MediaType.ANIME,
            onClick = { onTypeChange(MediaType.ANIME) },
            label = { Text(stringResource(R.string.media_type_anime)) }
        )
        FilterChip(
            selected = searchType == MediaType.MANGA,
            onClick = { onTypeChange(MediaType.MANGA) },
            label = { Text(stringResource(R.string.media_type_manga)) }
        )
    }
    Spacer(Modifier.height(8.dp))

    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.forum_media_search_hint)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = if (isSearching) {
            {
                AppCircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }
        } else null,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )

    Spacer(Modifier.height(12.dp))

    when {
        error != null -> {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        query.isNotBlank() && results.isEmpty() && !isSearching -> {
            Text(
                text = stringResource(R.string.forum_media_no_results, query),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        results.isNotEmpty() -> {
            Text(
                text = stringResource(R.string.forum_search_results_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())
            ) {
                results.forEach { entry ->
                    MediaResultRow(
                        entry = entry,
                        isSelected = entry.mediaId in selectedMediaIds,
                        onClick = { onResultClick(entry) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaResultRow(entry: LibraryEntry, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = entry.cover.url() ?: entry.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 48.dp, height = 64.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.titleUserPreferred,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2
                )
                val metaParts = listOfNotNull(
                    entry.format?.name?.replace('_', ' '),
                    entry.startedAt?.let {
                        java.text.SimpleDateFormat("yyyy", java.util.Locale.US)
                            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                            .format(java.util.Date(it))
                    }
                )
                if (metaParts.isNotEmpty()) {
                    Text(
                        text = metaParts.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaCategoryChip(
    entry: LibraryEntry,
    onRemove: () -> Unit
) {
    Surface(
        onClick = onRemove,
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = entry.titleUserPreferred,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                maxLines = 1
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.forum_remove_media_category),
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}
