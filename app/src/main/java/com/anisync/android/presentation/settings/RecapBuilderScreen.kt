package com.anisync.android.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.domain.RecapPreview
import com.anisync.android.domain.StoredRecap
import com.anisync.android.domain.formatExactCount
import com.anisync.android.domain.formatHours
import com.anisync.android.domain.formatOneDecimal
import com.anisync.android.domain.recap.RecapPeriod
import com.anisync.android.presentation.navigation.RecapStory
import androidx.compose.runtime.LaunchedEffect
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/** Developer Tools: builds recaps on demand so their numbers can be checked against the frames. */
@Composable
fun RecapBuilderScreen(
    onBackClick: () -> Unit,
    onOpenStory: (RecapStory) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecapBuilderViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val opening by viewModel.openStory.collectAsStateWithLifecycle()
    LaunchedEffect(opening) {
        opening?.let {
            onOpenStory(it)
            viewModel.onAction(RecapBuilderAction.StoryOpened)
        }
    }
    SettingsScreenScaffold(
        title = stringResource(R.string.dev_recap_builder),
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        BuilderCard(state = state, onAction = viewModel::onAction)
        Spacer(modifier = Modifier.height(8.dp))
        StoredCard(state = state, onAction = viewModel::onAction)
    }
}

@Composable
private fun BuilderCard(state: RecapBuilderUiState, onAction: (RecapBuilderAction) -> Unit) {
    // Last month's and last year's keys, the two shapes the field reads.
    val examples = remember {
        val lastMonth = YearMonth.now().minusMonths(1)
        RecapPeriod.Month(lastMonth).key to RecapPeriod.Year(lastMonth.year - 1).key
    }
    DevCard(
        title = stringResource(R.string.dev_recap_builder),
        subtitle = stringResource(R.string.dev_recap_builder_subtitle),
    ) {
        OutlinedTextField(
            value = state.userId,
            onValueChange = { onAction(RecapBuilderAction.SetUserId(it)) },
            label = { Text(stringResource(R.string.dev_recap_user_id)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.period,
            onValueChange = { onAction(RecapBuilderAction.SetPeriod(it)) },
            label = { Text(stringResource(R.string.dev_recap_period, examples.first, examples.second)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.zone,
            onValueChange = { onAction(RecapBuilderAction.SetZone(it)) },
            label = { Text(stringResource(R.string.dev_recap_zone)) },
            singleLine = true,
            isError = state.inputError,
            supportingText = if (state.inputError) {
                { Text(stringResource(R.string.dev_recap_invalid_input)) }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = state.previewAdult,
                    role = Role.Switch,
                    onValueChange = { onAction(RecapBuilderAction.SetPreviewAdult(it)) },
                ),
        ) {
            Text(
                stringResource(R.string.recap_settings_show_adult),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = state.previewAdult, onCheckedChange = null, colors = appSwitchColors())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilledTonalButton(
                onClick = { onAction(RecapBuilderAction.Build) },
                enabled = !state.isBuilding,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(if (state.isBuilding) R.string.dev_recap_building else R.string.dev_recap_build))
            }
            FilledTonalButton(
                onClick = { onAction(RecapBuilderAction.OpenMine) },
                enabled = !state.isBuilding,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.dev_recap_open_mine), textAlign = TextAlign.Center)
            }
        }
        state.buildError?.let { error ->
            Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        state.preview?.let { preview ->
            if (preview.recap != null) {
                FilledTonalButton(onClick = { onAction(RecapBuilderAction.OpenPreviewStory) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.dev_recap_open_story))
                }
            }
            PreviewReadout(preview)
        }
        state.opened?.let { RecapNumbers(it, LocalConfiguration.current.locales[0]) }
        if (state.openedNothing) {
            Text(stringResource(R.string.dev_recap_nothing), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PreviewReadout(preview: RecapPreview) {
    val locale = LocalConfiguration.current.locales[0]
    Readout(stringResource(R.string.dev_recap_activities_read), formatExactCount(preview.activities, locale))
    Readout(stringResource(R.string.dev_recap_requests), formatExactCount(preview.requests, locale))
    Readout(
        stringResource(R.string.dev_recap_took),
        stringResource(R.string.dev_recap_duration_ms, formatExactCount(preview.millis.toInt(), locale)),
    )
    Readout(
        stringResource(R.string.dev_recap_stored_size),
        stringResource(R.string.dev_recap_kilobytes, formatOneDecimal(preview.payloadBytes / 1024.0, locale)),
    )
    val recap = preview.recap
    if (recap == null) {
        Text(stringResource(R.string.dev_recap_nothing), style = MaterialTheme.typography.bodyMedium)
    } else {
        RecapNumbers(recap, locale)
    }
}

/** Every figure the story pages show, in the order they show them. */
@Composable
private fun RecapNumbers(stored: StoredRecap, locale: Locale) {
    val recap = stored.recap
    val dates = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    val hidden = stringResource(R.string.dev_recap_hidden_title)
    fun title(id: Int?): String = id?.let { stored.media[it]?.title } ?: hidden
    fun count(value: Int) = formatExactCount(value, locale)
    val valueCount = R.string.dev_recap_value_count

    Readout(stringResource(R.string.dev_recap_episodes), count(recap.episodes))
    Readout(
        stringResource(R.string.dev_recap_time),
        stringResource(R.string.share_recap_hours_minutes, count(recap.minutes / 60), count(recap.minutes % 60)),
    )
    Readout(stringResource(R.string.dev_recap_titles), count(recap.titles))
    Readout(stringResource(R.string.dev_recap_finished), count(recap.finished))
    Readout(
        stringResource(R.string.dev_recap_finished_titles),
        recap.finishedTitles.map { title(it) }.joinToString(NEW_LINE),
    )
    Readout(stringResource(R.string.dev_recap_started), count(recap.started))
    Readout(stringResource(R.string.dev_recap_active_days), count(recap.activeDays))
    Readout(stringResource(R.string.dev_recap_streak), count(recap.streak.days))
    Readout(
        stringResource(R.string.dev_recap_streak_dates),
        stringResource(R.string.dev_recap_date_range, recap.streak.start.format(dates), recap.streak.end.format(dates)),
    )
    Readout(
        stringResource(R.string.dev_recap_weekday),
        stringResource(
            valueCount,
            recap.busiestWeekday.getDisplayName(TextStyle.FULL, locale),
            count(recap.episodesByWeekday.getValue(recap.busiestWeekday)),
        ),
    )
    Readout(
        stringResource(R.string.dev_recap_biggest_day),
        stringResource(valueCount, recap.biggestDay.date.format(dates), count(recap.biggestDay.episodes)),
    )
    Readout(
        stringResource(R.string.dev_recap_biggest_day_lead),
        stringResource(
            valueCount,
            recap.biggestDay.leaders.map { title(it) }.ifEmpty { listOf(hidden) }.joinToString(NEW_LINE),
            count(recap.biggestDay.leadingEpisodes),
        ),
    )
    Readout(
        stringResource(R.string.dev_recap_top_titles),
        recap.ranked.take(5).map { ranked ->
            stringResource(
                R.string.dev_recap_title_line,
                title(ranked.mediaId),
                stringResource(R.string.share_recap_hours_minutes, count(ranked.minutes / 60), count(ranked.minutes % 60)),
                count(ranked.episodes),
            )
        }.joinToString(NEW_LINE),
    )
    Readout(
        stringResource(R.string.dev_recap_genres),
        recap.genres.take(6).map {
            stringResource(R.string.dev_recap_name_hours, it.name, formatHours(it.minutes, locale))
        }.joinToString(NEW_LINE),
    )
    Readout(
        stringResource(R.string.dev_recap_studios),
        recap.studios.take(3).map {
            stringResource(R.string.dev_recap_name_hours, it.name, formatHours(it.minutes, locale))
        }.joinToString(NEW_LINE),
    )
    recap.franchise?.let { franchise ->
        Readout(
            stringResource(R.string.dev_recap_franchise),
            stringResource(valueCount, franchise.name ?: title(franchise.mediaIds.first()), count(franchise.mediaIds.size)),
        )
    }
    recap.manga?.let { manga ->
        Readout(stringResource(R.string.dev_recap_manga_chapters), count(manga.chapters))
        Readout(stringResource(R.string.dev_recap_manga_titles), count(manga.titles))
        Readout(stringResource(R.string.dev_recap_manga_top), stringResource(valueCount, title(manga.topMediaId), count(manga.topChapters)))
    }
    recap.episodesByMonth?.let { months ->
        Readout(
            stringResource(R.string.dev_recap_months),
            months.entries.map { (month, episodes) ->
                stringResource(valueCount, month.getDisplayName(TextStyle.FULL, locale), count(episodes))
            }.joinToString(NEW_LINE),
        )
        recap.busiestMonth?.let { month ->
            Readout(
                stringResource(R.string.dev_recap_busiest_month),
                stringResource(valueCount, month.getDisplayName(TextStyle.FULL, locale), count(months.getValue(month))),
            )
        }
        recap.quietestMonth?.let { month ->
            Readout(
                stringResource(R.string.dev_recap_quietest_month),
                stringResource(valueCount, month.getDisplayName(TextStyle.FULL, locale), count(months.getValue(month))),
            )
        }
    }
    recap.keptUpWhileAiring?.let { Readout(stringResource(R.string.dev_recap_kept_up), count(it)) }
}

@Composable
private fun StoredCard(state: RecapBuilderUiState, onAction: (RecapBuilderAction) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    DevCard(
        title = stringResource(R.string.dev_recap_stored),
        subtitle = stringResource(R.string.dev_recap_stored_subtitle),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilledTonalButton(onClick = { onAction(RecapBuilderAction.RunJob) }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.dev_recap_run_job), textAlign = TextAlign.Center)
            }
            FilledTonalButton(onClick = { onAction(RecapBuilderAction.ClearStored) }) {
                Text(stringResource(R.string.clear))
            }
        }
        if (state.stored.isEmpty()) {
            Text(
                stringResource(R.string.dev_recap_stored_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.stored.forEach { stored ->
            val status = stringResource(
                when {
                    !stored.isFinal -> R.string.dev_recap_state_in_progress
                    stored.seenAt == null -> R.string.dev_recap_state_new
                    else -> R.string.dev_recap_state_final
                }
            )
            Readout(
                periodName(stored.period, locale),
                stringResource(R.string.dev_recap_value_count, status, formatExactCount(stored.recap.episodes, locale)),
            )
        }
    }
    SettingsGroup {
        SwitchSettingsItem(
            title = stringResource(R.string.recap_settings_include_manga),
            checked = state.includeManga,
            onCheckedChange = { onAction(RecapBuilderAction.SetIncludeManga(it)) },
        )
        SwitchSettingsItem(
            title = stringResource(R.string.recap_settings_count_repeats),
            checked = state.countRepeats,
            onCheckedChange = { onAction(RecapBuilderAction.SetCountRepeats(it)) },
        )
    }
}

private fun periodName(period: RecapPeriod, locale: Locale): String = when (period) {
    is RecapPeriod.Month -> LocalDate.of(period.month.year, period.month.month, 1)
        .format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
    is RecapPeriod.Year -> period.year.toString()
}

/** The card the other Developer Tools sections use. */
@Composable
private fun DevCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
        }
    }
}

private const val NEW_LINE = "\n"

/** One label and its value, the value wrapping under the label when it runs long. */
@Composable
private fun Readout(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.6f),
        )
    }
}
