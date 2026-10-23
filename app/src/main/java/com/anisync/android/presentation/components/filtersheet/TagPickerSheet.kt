package com.anisync.android.presentation.components.filtersheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.components.SearchField
import com.anisync.android.presentation.util.pluralStringResourceOrDefault

/**
 * One tag the picker can offer.
 *
 * A [isSpoiler] tag is listed in a group of its own rather than under its category, and that group
 * stays shut until it is opened, so the name is never on screen unasked.
 */
@Immutable
data class TagChoice(
    val name: String,
    val category: String?,
    val isAdult: Boolean = false,
    val isSpoiler: Boolean = false
)

private const val SPOILER_GROUP = "spoilers"

/**
 * Search field over [tags], grouped by AniList category with one group open at a time.
 *
 * Edits apply live through [onSelectionChange], the same way every other filter sheet works. How
 * many are picked leads the hint rather than the title, where arrows read as a sort.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagPickerSheet(
    tags: List<TagChoice>,
    included: Set<String>,
    excluded: Set<String>,
    showAdult: Boolean,
    onSelectionChange: (included: Set<String>, excluded: Set<String>) -> Unit,
    onDismiss: () -> Unit,
    footer: (@Composable () -> Unit)? = null
) {
    var query by rememberSaveable { mutableStateOf("") }
    var expandedGroup by rememberSaveable { mutableStateOf<String?>(null) }
    val otherLabel = stringResource(R.string.tag_picker_other)
    val visibleTags = remember(tags, showAdult, query) {
        val pool = if (showAdult) tags else tags.filterNot { it.isAdult }
        if (query.isBlank()) pool
        else pool.filter { it.name.contains(query, ignoreCase = true) }
    }
    val grouped = remember(visibleTags, otherLabel) {
        visibleTags.filterNot { it.isSpoiler }.groupBy { it.category ?: otherLabel }.toSortedMap()
    }
    val spoilers = remember(visibleTags) { visibleTags.filter { it.isSpoiler } }

    FilterSheetScaffold(
        title = stringResource(R.string.library_filter_tags),
        onDismiss = onDismiss,
        onReset = { onSelectionChange(emptySet(), emptySet()) },
        resetEnabled = included.isNotEmpty() || excluded.isNotEmpty(),
        scrollableBody = false,
        footer = footer
    ) {
        SearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = stringResource(R.string.search_tags_hint),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        IncludeExcludeHint(
            counts = selectionSummary(included.size, excluded.size),
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 8.dp)
        )

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            grouped.forEach { (category, categoryTags) ->
                val key = "group-$category"
                item(key = key) {
                    TagGroup(
                        label = category,
                        summary = groupSummary(categoryTags, included, excluded),
                        tags = categoryTags,
                        included = included,
                        excluded = excluded,
                        expanded = expandedGroup == key,
                        onToggle = { expandedGroup = if (expandedGroup == key) null else key },
                        onSelectionChange = onSelectionChange
                    )
                }
            }
            if (spoilers.isNotEmpty()) {
                item(key = SPOILER_GROUP) {
                    val expanded = expandedGroup == SPOILER_GROUP
                    val untouched = spoilers.none { it.name in included || it.name in excluded }
                    TagGroup(
                        label = stringResource(R.string.tag_picker_spoilers),
                        summary = if (untouched && !expanded) {
                            pluralStringResourceOrDefault(R.plurals.tags_spoiler_hidden, spoilers.size, spoilers.size)
                        } else {
                            groupSummary(spoilers, included, excluded)
                        },
                        tags = spoilers,
                        included = included,
                        excluded = excluded,
                        expanded = expanded,
                        onToggle = { expandedGroup = if (expanded) null else SPOILER_GROUP },
                        onSelectionChange = onSelectionChange
                    )
                }
            }
            if (grouped.isEmpty() && spoilers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (query.isBlank()) {
                                stringResource(R.string.tag_picker_empty)
                            } else {
                                stringResource(R.string.tag_picker_no_match, query)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagGroup(
    label: String,
    summary: String,
    tags: List<TagChoice>,
    included: Set<String>,
    excluded: Set<String>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSelectionChange: (included: Set<String>, excluded: Set<String>) -> Unit
) {
    FilterOptionGroup(
        label = label,
        summary = summary,
        expanded = expanded,
        onToggle = onToggle
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { tag ->
                val state = when (tag.name) {
                    in included -> TriState.INCLUDED
                    in excluded -> TriState.EXCLUDED
                    else -> TriState.OFF
                }
                IncludeExcludeChip(
                    label = tag.name,
                    state = state,
                    onStateChange = { newState ->
                        onSelectionChange(
                            if (newState == TriState.INCLUDED) included + tag.name else included - tag.name,
                            if (newState == TriState.EXCLUDED) excluded + tag.name else excluded - tag.name
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun groupSummary(tags: List<TagChoice>, included: Set<String>, excluded: Set<String>): String =
    selectionSummary(tags.count { it.name in included }, tags.count { it.name in excluded })
        ?: pluralStringResourceOrDefault(R.plurals.tag_picker_count, tags.size, tags.size)

/** "2 included", "1 excluded", or both. Null while nothing is picked. */
@Composable
private fun selectionSummary(includedCount: Int, excludedCount: Int): String? = when {
    includedCount == 0 && excludedCount == 0 -> null
    excludedCount == 0 -> stringResource(R.string.tag_picker_included, includedCount)
    includedCount == 0 -> stringResource(R.string.tag_picker_excluded, excludedCount)
    else -> stringResource(R.string.tag_picker_included_excluded, includedCount, excludedCount)
}
