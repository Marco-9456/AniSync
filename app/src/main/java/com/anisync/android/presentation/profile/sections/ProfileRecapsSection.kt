package com.anisync.android.presentation.profile.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import com.anisync.android.presentation.components.EmptyState
import com.anisync.android.presentation.recap.RecapEntry
import com.anisync.android.presentation.recap.RecapInProgressCard
import com.anisync.android.presentation.recap.RecapMonthTile
import com.anisync.android.presentation.recap.RecapRunningYearTile
import com.anisync.android.presentation.recap.RecapShelfCard
import com.anisync.android.presentation.recap.RecapThisMonthCard
import com.anisync.android.presentation.recap.RecapYearTile
import com.anisync.android.presentation.recap.RecapsUiModel
import com.anisync.android.presentation.statistics.StatsSectionHeader
import com.anisync.android.presentation.statistics.statsBodySmall

/**
 * Overview's recaps, above the hero on the viewer's own profile: the recaps waiting to be opened,
 * or the month in progress when none are. A tablet pane drops the header and fills its width with
 * two cards, as ST4 draws them.
 */
internal fun LazyListScope.recapShelf(recaps: RecapsUiModel, columns: Int, onOpenRecap: (String) -> Unit) {
    val shelf = recaps.shelf
    val progress = recaps.inProgress
    if (!recaps.building && shelf.isEmpty() && progress == null) return
    val wide = columns > 1
    if (!wide) {
        item(key = "recap_shelf_header") { StatsSectionHeader(title = stringResource(R.string.recap_shelf_title), top = 0.dp) }
    }
    item(key = "recap_shelf", contentType = "recap_shelf") {
        when {
            recaps.building -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                AppCircularProgressIndicator()
            }
            shelf.isNotEmpty() && wide -> Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                shelf.take(2).forEach { entry ->
                    RecapShelfCard(entry, onClick = { onOpenRecap(entry.key) }, modifier = Modifier.weight(1f), wide = true)
                }
                if (shelf.size == 1) Spacer(Modifier.weight(1f))
            }
            shelf.isNotEmpty() -> LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(shelf, key = { it.key }) { entry ->
                    RecapShelfCard(entry, onClick = { onOpenRecap(entry.key) }, modifier = Modifier.width(300.dp))
                }
            }
            progress != null -> RecapInProgressCard(progress, Modifier.padding(horizontal = 16.dp))
        }
    }
    item(key = "recap_shelf_gap") { Spacer(Modifier.height(if (wide) 16.dp else 24.dp)) }
}

/**
 * ST5: the month in progress, every kept month and every kept year, each tile opening its story.
 * The grid takes two columns on a phone and four in a tablet's pane.
 */
internal fun LazyListScope.statsRecaps(recaps: RecapsUiModel, columns: Int, onOpenRecap: (String) -> Unit) {
    val perRow = if (columns > 1) 4 else 2
    if (recaps.building && recaps.entries.isEmpty()) {
        item(key = "recaps_building") {
            Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) { AppCircularProgressIndicator() }
        }
        return
    }
    if (recaps.entries.isEmpty() && recaps.inProgress == null) {
        item(key = "recaps_none") {
            EmptyState(
                icon = Icons.Default.AutoAwesomeMotion,
                title = stringResource(R.string.recap_none_title),
                description = stringResource(R.string.recap_none_body),
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
            )
        }
        return
    }
    recaps.inProgress?.let { progress ->
        item(key = "recaps_this_month_header") { StatsSectionHeader(title = stringResource(R.string.recap_this_month), top = 0.dp) }
        item(key = "recaps_this_month") { RecapThisMonthCard(progress, Modifier.padding(horizontal = 16.dp)) }
    }
    val months = recaps.months
    if (months.isNotEmpty()) {
        item(key = "recaps_months_header") {
            StatsSectionHeader(title = stringResource(R.string.recap_monthly_recaps), top = if (recaps.inProgress == null) 0.dp else 16.dp)
        }
        tileRows("recaps_month", months, perRow) { entry, modifier -> RecapMonthTile(entry, { onOpenRecap(entry.key) }, modifier) }
    }
    val years = recaps.years
    if (years.isNotEmpty() || recaps.runningYear != null) {
        item(key = "recaps_years_header") { StatsSectionHeader(title = stringResource(R.string.recap_yearly_recaps), top = 8.dp) }
        item(key = "recaps_years") {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                recaps.runningYear?.let { RecapRunningYearTile(it, Modifier.weight(1f)) }
                years.take(perRow - if (recaps.runningYear != null) 1 else 0).forEach { entry ->
                    RecapYearTile(entry, { onOpenRecap(entry.key) }, Modifier.weight(1f))
                }
                val used = years.size.coerceAtMost(perRow) + if (recaps.runningYear != null) 1 else 0
                repeat((perRow - used).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    item(key = "recaps_note") {
        Text(
            text = stringResource(R.string.recap_view_note),
            style = statsBodySmall(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp),
        )
    }
}

private fun LazyListScope.tileRows(
    key: String,
    entries: List<RecapEntry>,
    perRow: Int,
    tile: @androidx.compose.runtime.Composable (RecapEntry, Modifier) -> Unit,
) {
    entries.chunked(perRow).forEachIndexed { index, row ->
        item(key = "${key}_$index", contentType = "recap_tiles") {
            Row(
                Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { entry -> tile(entry, Modifier.weight(1f)) }
                repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
