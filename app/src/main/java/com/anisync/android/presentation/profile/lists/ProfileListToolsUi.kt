package com.anisync.android.presentation.profile.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.domain.formatScore
import com.anisync.android.domain.max
import com.anisync.android.presentation.components.filtersheet.FilterSheetScaffold
import com.anisync.android.presentation.components.filtersheet.IncludeExcludeChip
import com.anisync.android.presentation.components.filtersheet.OptionPillGrid
import com.anisync.android.presentation.components.filtersheet.SheetFooterButton
import com.anisync.android.presentation.components.filtersheet.SheetSectionLabel
import com.anisync.android.presentation.components.filtersheet.SortSheet
import com.anisync.android.presentation.components.navigation.BackTrailButton
import com.anisync.android.presentation.util.pluralStringResourceOrDefault
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.type.MediaFormat
import java.text.NumberFormat

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProfileListTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    view: ProfileListView,
    onToggleView: () -> Unit,
    onOpenSort: () -> Unit,
    filtersActive: Boolean,
    onOpenFilters: () -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    // Local text: the strip is drawn twice, and the round trip through the ViewModel drops fast keys.
    var text by rememberSaveable { mutableStateOf(query) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(query) { if (!focused && query != text) text = query }
    val focusManager = LocalFocusManager.current
    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(imeVisible) { if (!imeVisible && focused) focusManager.clearFocus() }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = CircleShape,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(56.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            if (onBack != null) {
                BackTrailButton(onClick = onBack)
            } else {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp, end = 8.dp)
                )
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (text.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        onQueryChange(it)
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focused = it.isFocused }
                )
            }
            // Only while typing: once the query is set the bar goes back to its three actions.
            if (text.isNotEmpty() && focused) {
                IconButton(onClick = {
                    text = ""
                    onQueryChange("")
                }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.profile_list_clear_search))
                }
            }
            IconButton(onClick = onToggleView) {
                Icon(
                    imageVector = if (view == ProfileListView.LIST) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                    contentDescription = stringResource(
                        if (view == ProfileListView.LIST) R.string.profile_list_show_grid else R.string.profile_list_show_rows
                    )
                )
            }
            IconButton(onClick = onOpenSort) {
                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.profile_list_sort))
            }
            IconButton(onClick = onOpenFilters) {
                BadgedBox(badge = { if (filtersActive) Badge() }) {
                    Icon(Icons.Default.Tune, contentDescription = stringResource(R.string.profile_list_filters))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProfileListSummary(
    results: ProfileListResults,
    tools: ProfileListTools,
    isOwnProfile: Boolean,
    onRemoveFilter: (ProfileListFilters) -> Unit,
    modifier: Modifier = Modifier
) {
    val numbers = remember { NumberFormat.getIntegerInstance() }
    val count = results.items.size
    val query = tools.query.trim()
    val amount = when {
        query.isNotEmpty() -> pluralStringResourceOrDefault(R.plurals.profile_list_results_for, count, numbers.format(count), query)
        tools.filters.isActive -> stringResource(R.string.profile_list_count_of, numbers.format(count), numbers.format(results.inScope))
        else -> pluralStringResourceOrDefault(R.plurals.profile_list_titles, count, numbers.format(count))
    }
    val text = stringResource(R.string.profile_people_summary, amount, orderPhrase(tools, isOwnProfile))
    val filters = tools.filters
    FlowRow(
        verticalArrangement = Arrangement.Center,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        if (filters.yourList != YourListFilter.ANY) {
            RemovableChip(stringResource(filters.yourList.labelRes)) { onRemoveFilter(filters.copy(yourList = YourListFilter.ANY)) }
        }
        filters.formats.forEach { format ->
            RemovableChip(format.toLabel()) { onRemoveFilter(filters.copy(formats = filters.formats - format)) }
        }
        filters.genres.forEach { genre ->
            RemovableChip(genre) { onRemoveFilter(filters.copy(genres = filters.genres - genre)) }
        }
        filters.excludedGenres.forEach { genre ->
            RemovableChip(genre, excluded = true) { onRemoveFilter(filters.copy(excludedGenres = filters.excludedGenres - genre)) }
        }
        filters.scoreFloor?.let { floor ->
            RemovableChip(floorChipLabel(floor)) { onRemoveFilter(filters.copy(scoreFloor = null)) }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RemovableChip(label: String, excluded: Boolean = false, onRemove: () -> Unit) {
    val container = if (excluded) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
    val content = if (excluded) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
    InputChip(
        selected = true,
        onClick = onRemove,
        label = { Text(label, maxLines = 1) },
        leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp)) },
        shape = CircleShape,
        colors = InputChipDefaults.inputChipColors(
            selectedContainerColor = container,
            selectedLabelColor = content,
            selectedLeadingIconColor = content
        ),
        border = null,
        modifier = if (excluded) {
            val state = stringResource(R.string.a11y_filter_excluded)
            Modifier.semantics { stateDescription = state }
        } else {
            Modifier
        }
    )
}

@Composable
private fun floorChipLabel(floor: Int): String = when (floor) {
    ProfileListFilters.UNSCORED -> stringResource(R.string.profile_list_not_scored)
    else -> stringResource(R.string.profile_list_score_floor_chip, floor)
}

@Composable
private fun orderPhrase(tools: ProfileListTools, isOwnProfile: Boolean): String {
    val sort = tools.sort
    if (sort == ProfileListSort.TITLE) {
        return stringResource(if (tools.ascending) R.string.profile_order_title_az else R.string.profile_order_title_za)
    }
    val phrase = stringResource(
        when (sort) {
            ProfileListSort.UPDATED -> R.string.profile_order_updated
            ProfileListSort.THEIR_SCORE -> if (isOwnProfile) R.string.profile_order_score else R.string.profile_order_their_score
            ProfileListSort.YOUR_SCORE -> R.string.profile_order_your_score
            ProfileListSort.AVERAGE_SCORE -> R.string.profile_order_average
            ProfileListSort.POPULARITY -> R.string.profile_order_popularity
            ProfileListSort.RELEASE_DATE -> R.string.profile_order_release
            ProfileListSort.ADDED -> R.string.profile_order_added
            ProfileListSort.STARTED -> R.string.profile_order_started
            ProfileListSort.FINISHED -> R.string.profile_order_finished
            ProfileListSort.PROGRESS -> R.string.profile_order_progress
            ProfileListSort.REWATCHES -> R.string.profile_order_rewatches
            ProfileListSort.TITLE -> R.string.profile_order_title_az
        }
    )
    return if (tools.ascending) stringResource(R.string.profile_order_reversed, phrase) else phrase
}

@Composable
internal fun sortLabel(sort: ProfileListSort, isOwnProfile: Boolean): String =
    stringResource(if (isOwnProfile && sort == ProfileListSort.THEIR_SCORE) R.string.profile_list_sort_score else sort.labelRes)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileListSortSheet(
    tools: ProfileListTools,
    isOwnProfile: Boolean,
    onSortChange: (ProfileListSort, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val columns = if (isOwnProfile) ProfileListSort.entries - ProfileListSort.YOUR_SCORE else ProfileListSort.entries
    SortSheet(
        columns = columns,
        label = { sortLabel(it, isOwnProfile) },
        selected = tools.sort,
        isAscending = tools.ascending,
        hasDirection = true,
        onColumnPick = { onSortChange(it, it == ProfileListSort.TITLE) },
        onDirectionChange = { onSortChange(tools.sort, it) },
        onReset = { onSortChange(ProfileListSort.UPDATED, false) },
        resetEnabled = tools.sort != ProfileListSort.UPDATED || tools.ascending,
        onDismiss = onDismiss
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileListFilterSheet(
    entries: List<LibraryEntry>,
    status: LibraryStatus?,
    tools: ProfileListTools,
    viewer: Map<Int, LibraryEntry>,
    titleLanguage: TitleLanguage,
    ownerScoreFormat: ScoreFormat,
    isOwnProfile: Boolean,
    onFiltersChange: (ProfileListFilters) -> Unit,
    onDismiss: () -> Unit
) {
    val filters = tools.filters
    val matching = remember(entries, status, tools, viewer, titleLanguage) {
        applyListTools(entries, status, tools, viewer, titleLanguage).items.size
    }
    val formats = remember(entries) {
        entries.mapNotNull { it.format }.filter { it != MediaFormat.UNKNOWN__ }
            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key }
    }
    val genres = remember(entries) { entries.flatMap { it.genres }.distinct().sorted() }
    // What each choice would leave with the other filters kept.
    val yourListCounts = remember(entries, status, tools, viewer, titleLanguage, isOwnProfile) {
        if (isOwnProfile) {
            emptyMap()
        } else {
            listOf(YourListFilter.ON_YOUR_LIST, YourListFilter.NOT_ON_YOUR_LIST).associateWith { choice ->
                val narrowed = tools.copy(filters = filters.copy(yourList = choice))
                applyListTools(entries, status, narrowed, viewer, titleLanguage).items.size
            }
        }
    }
    val numbers = remember { NumberFormat.getIntegerInstance() }
    FilterSheetScaffold(
        title = stringResource(R.string.profile_list_filters),
        onDismiss = onDismiss,
        onReset = { onFiltersChange(ProfileListFilters()) },
        resetEnabled = filters.isActive,
        footer = {
            SheetFooterButton(
                text = pluralStringResourceOrDefault(R.plurals.library_show_results, matching, matching),
                onClick = onDismiss
            )
        }
    ) {
        if (!isOwnProfile) {
            SheetSectionLabel(stringResource(R.string.profile_list_your_list))
            Spacer(Modifier.height(8.dp))
            OptionPillGrid(
                options = YourListFilter.entries,
                label = { stringResource(it.labelRes) },
                isSelected = { it == filters.yourList },
                onClick = { onFiltersChange(filters.copy(yourList = it)) },
                count = { choice -> yourListCounts[choice]?.let(numbers::format) }
            )
            Spacer(Modifier.height(12.dp))
        }
        if (formats.size > 1) {
            SheetSectionLabel(stringResource(R.string.library_filter_format))
            Spacer(Modifier.height(8.dp))
            OptionPillGrid(
                options = formats,
                label = { it.toLabel() },
                isSelected = { it in filters.formats },
                onClick = { format -> onFiltersChange(filters.copy(formats = filters.formats.toggled(format))) },
                role = Role.Checkbox
            )
            Spacer(Modifier.height(12.dp))
        }
        if (genres.isNotEmpty()) {
            SheetSectionLabel(stringResource(R.string.filter_genres))
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                genres.forEach { genre ->
                    IncludeExcludeChip(
                        label = genre,
                        state = filters.genreState(genre),
                        onStateChange = { onFiltersChange(filters.withGenre(genre, it)) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        SheetSectionLabel(stringResource(if (isOwnProfile) R.string.profile_list_sort_score else R.string.profile_list_sort_their_score))
        Spacer(Modifier.height(8.dp))
        OptionPillGrid(
            options = listOf<Int?>(null) + scoreFloors(ownerScoreFormat) + ProfileListFilters.UNSCORED,
            label = { floorLabel(it, ownerScoreFormat) },
            isSelected = { it == filters.scoreFloor },
            onClick = { onFiltersChange(filters.copy(scoreFloor = it)) }
        )
    }
}

@Composable
private fun floorLabel(floor: Int?, format: ScoreFormat): String = when (floor) {
    null -> stringResource(R.string.filter_any)
    ProfileListFilters.UNSCORED -> stringResource(R.string.profile_list_not_scored)
    100 -> formatScore(format.max, format)
    else -> stringResource(R.string.profile_list_score_and_up, formatScore(floor / 100.0 * format.max, format))
}

private fun <T> Set<T>.toggled(item: T): Set<T> = if (item in this) this - item else this + item
