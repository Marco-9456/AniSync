package com.anisync.android.presentation.components.filtersheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.anisync.android.R

/** One tag the picker can offer. */
@Immutable
data class TagChoice(
    val name: String,
    val category: String?,
    val isAdult: Boolean = false
)

/**
 * Search field over [tags], grouped by AniList category with one group open at a time.
 *
 * Edits apply live through [onSelectionChange], the same way every other filter sheet works.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagPickerSheet(
    tags: List<TagChoice>,
    included: Set<String>,
    excluded: Set<String>,
    showAdult: Boolean,
    onSelectionChange: (included: Set<String>, excluded: Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var expandedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    val otherLabel = stringResource(R.string.tag_picker_other)
    val visibleTags = remember(tags, showAdult, query) {
        val pool = if (showAdult) tags else tags.filterNot { it.isAdult }
        if (query.isBlank()) pool
        else pool.filter { it.name.contains(query, ignoreCase = true) }
    }
    val grouped = remember(visibleTags, otherLabel) {
        visibleTags.groupBy { it.category ?: otherLabel }.toSortedMap()
    }

    FilterSheetScaffold(
        title = stringResource(R.string.tag_picker_title, included.size, excluded.size),
        onDismiss = onDismiss,
        onReset = { onSelectionChange(emptySet(), emptySet()) },
        resetEnabled = included.isNotEmpty() || excluded.isNotEmpty(),
        scrollableBody = false
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.search_tags_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            grouped.forEach { (category, categoryTags) ->
                item(key = "group-$category") {
                    TagGroup(
                        label = category,
                        summary = groupSummary(categoryTags, included, excluded),
                        tags = categoryTags,
                        included = included,
                        excluded = excluded,
                        expanded = expandedCategory == category,
                        onToggle = {
                            expandedCategory = if (expandedCategory == category) null else category
                        },
                        onSelectionChange = onSelectionChange
                    )
                }
            }
            if (grouped.isEmpty()) {
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
            modifier = Modifier.padding(horizontal = 12.dp),
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
private fun groupSummary(tags: List<TagChoice>, included: Set<String>, excluded: Set<String>): String {
    val includedCount = tags.count { it.name in included }
    val excludedCount = tags.count { it.name in excluded }
    return when {
        includedCount == 0 && excludedCount == 0 ->
            pluralStringResource(R.plurals.tag_picker_count, tags.size, tags.size)
        excludedCount == 0 -> stringResource(R.string.tag_picker_included, includedCount)
        includedCount == 0 -> stringResource(R.string.tag_picker_excluded, excludedCount)
        else -> stringResource(R.string.tag_picker_included_excluded, includedCount, excludedCount)
    }
}
