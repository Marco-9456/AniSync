package com.anisync.android.presentation.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.domain.MediaTheme
import com.anisync.android.domain.ThemeType
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import com.anisync.android.presentation.components.CollapsingTopBarScaffold
import com.anisync.android.presentation.components.SegmentedTabGroup
import com.anisync.android.presentation.details.components.ThemeDetail
import com.anisync.android.presentation.details.components.ThemeRow
import com.anisync.android.presentation.details.components.ThemeSheet
import com.anisync.android.presentation.navigation.DetailPanePlaceholder
import com.anisync.android.presentation.navigation.TwoPaneListDetailScaffold
import com.anisync.android.presentation.util.LocalAdaptiveInfo
import com.anisync.android.presentation.util.LocalPaneIsRoot
import com.anisync.android.presentation.util.bouncyClickable

private enum class ThemeFilter { All, Openings, Endings }

/**
 * Every opening and ending for a title, stacked so the bars share one scale.
 *
 * This is the screen behind the section arrow. The rail on the media page is deliberately
 * short and horizontal, which is fine for four themes and useless for seventy, so the
 * comparison view lives here where the rows can align and the filter can cut the list down.
 *
 * Compact and medium widths open a tapped theme in a sheet over the list. Expanded widths use the
 * shared two-pane [TwoPaneListDetailScaffold] — the list as the permanent pane, the theme in the
 * resizable detail pane beside it — so the list keeps its scroll position and its selection while a
 * theme plays.
 *
 * @param forceSinglePane set when this screen is itself drilled into a detail pane, which already
 * spends part of the window on the list beside it. Splitting again there would leave both halves too
 * narrow, so the pane keeps the sheet layout however wide the window is.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaThemesScreen(
    mediaTitle: String,
    totalEpisodes: Int?,
    coverUrl: String?,
    onBackClick: () -> Unit,
    forceSinglePane: Boolean = false,
    viewModel: MediaThemesViewModel = hiltViewModel()
) {
    val themesState by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.start(isAnime = true) }

    var filter by rememberSaveable { mutableStateOf(ThemeFilter.All) }
    var openTheme by remember { mutableStateOf<MediaTheme?>(null) }

    val openings = remember(themesState.themes) { themesState.themes.filter { it.type == ThemeType.OP } }
    val endings = remember(themesState.themes) { themesState.themes.filter { it.type == ThemeType.ED } }

    val list: @Composable (selectedThemeId: Int?, onThemeClick: (Int) -> Unit) -> Unit =
        { selectedThemeId, onThemeClick ->
            ThemeListPane(
                mediaTitle = mediaTitle,
                totalEpisodes = totalEpisodes,
                coverUrl = coverUrl,
                animeSlug = themesState.animeSlug,
                openings = openings,
                endings = endings,
                isLoading = themesState.isLoading || !themesState.hasLoaded,
                filter = filter,
                onFilterChange = { filter = it },
                selectedThemeId = selectedThemeId,
                onThemeClick = onThemeClick,
                onBackClick = onBackClick
            )
        }

    if (forceSinglePane || !LocalAdaptiveInfo.current.supportsTwoPane) {
        list(null) { id -> openTheme = themesState.themes.firstOrNull { it.id == id } }

        openTheme?.let { theme ->
            ThemeSheet(
                theme = theme,
                totalEpisodes = totalEpisodes,
                animeSlug = themesState.animeSlug,
                onDismiss = { openTheme = null }
            )
        }
        return
    }

    TwoPaneListDetailScaffold(
        // A pushed route, so there is no navigation rail for the list pane to sit flush against.
        gutterPadding = PaddingValues(16.dp),
        placeholderPane = {
            DetailPanePlaceholder(
                icon = Icons.Outlined.MusicNote,
                text = stringResource(R.string.pane_placeholder_theme)
            )
        },
        listPane = { selectedThemeId, onItemClick -> list(selectedThemeId, onItemClick) },
        detailPane = { themeId, onClose ->
            themesState.themes.firstOrNull { it.id == themeId }?.let { theme ->
                // One theme per pane and no drilling, so the pane is always its own root: the detail
                // shows the trailing close instead of a leading back arrow.
                CompositionLocalProvider(LocalPaneIsRoot provides true) {
                    ThemeDetailPane(
                        theme = theme,
                        totalEpisodes = totalEpisodes,
                        animeSlug = themesState.animeSlug,
                        onClose = onClose
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeListPane(
    mediaTitle: String,
    totalEpisodes: Int?,
    coverUrl: String?,
    animeSlug: String?,
    openings: List<MediaTheme>,
    endings: List<MediaTheme>,
    isLoading: Boolean,
    filter: ThemeFilter,
    onFilterChange: (ThemeFilter) -> Unit,
    selectedThemeId: Int?,
    onThemeClick: (Int) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val openingsLabel = stringResource(R.string.themes_group_openings)
    val endingsLabel = stringResource(R.string.themes_group_endings)
    val scaleLabel = totalEpisodes?.let { stringResource(R.string.themes_episode_scale, it) }

    CollapsingTopBarScaffold(
        title = stringResource(R.string.section_themes),
        onBackClick = onBackClick,
        scrollableState = listState
    ) { topContentPadding ->
        if (openings.isEmpty() && endings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (isLoading) {
                    AppCircularProgressIndicator()
                } else {
                    Text(
                        text = stringResource(R.string.themes_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@CollapsingTopBarScaffold
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = topContentPadding,
                bottom = dimensionResource(R.dimen.spacing_extra_large),
                start = dimensionResource(R.dimen.spacing_large),
                end = dimensionResource(R.dimen.spacing_large)
            ),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
        ) {
            item(key = "subtitle") {
                Text(
                    text = mediaTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item(key = "filter") {
                ThemeFilterRow(
                    selected = filter,
                    hasOpenings = openings.isNotEmpty(),
                    hasEndings = endings.isNotEmpty(),
                    onSelect = onFilterChange
                )
            }

            if (filter != ThemeFilter.Endings && openings.isNotEmpty()) {
                themeGroup(
                    label = openingsLabel,
                    scaleLabel = scaleLabel,
                    themes = openings,
                    totalEpisodes = totalEpisodes,
                    coverUrl = coverUrl,
                    selectedThemeId = selectedThemeId,
                    onThemeClick = onThemeClick
                )
            }

            if (filter != ThemeFilter.Openings && endings.isNotEmpty()) {
                themeGroup(
                    label = endingsLabel,
                    scaleLabel = scaleLabel,
                    themes = endings,
                    totalEpisodes = totalEpisodes,
                    coverUrl = coverUrl,
                    selectedThemeId = selectedThemeId,
                    onThemeClick = onThemeClick
                )
            }

            item(key = "footer") {
                Spacer(Modifier.height(dimensionResource(R.dimen.spacing_small)))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .bouncyClickable(
                            onClick = {
                                val url = if (animeSlug != null) {
                                    "https://animethemes.moe/anime/$animeSlug"
                                } else {
                                    "https://animethemes.moe"
                                }
                                runCatching {
                                    context.startActivity(
                                        android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse(url)
                                        )
                                    )
                                }
                            },
                            clipShape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(dimensionResource(R.dimen.spacing_medium))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(dimensionResource(R.dimen.spacing_normal)))
                        Text(
                            text = stringResource(R.string.themes_open_source),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.themes_attribution),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * The selected theme, filling a detail pane. It carries its own collapsing bar so the pane reads
 * like every other detail pane in the app, with the song title in the bar and the close affordance
 * where [LocalPaneIsRoot] puts it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeDetailPane(
    theme: MediaTheme,
    totalEpisodes: Int?,
    animeSlug: String?,
    onClose: () -> Unit
) {
    val scrollState = rememberScrollState()
    CollapsingTopBarScaffold(
        title = theme.songTitle ?: theme.slug,
        onBackClick = onClose,
        scrollableState = scrollState
    ) { topContentPadding ->
        ThemeDetail(
            theme = theme,
            totalEpisodes = totalEpisodes,
            animeSlug = animeSlug,
            twoColumn = true,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(top = topContentPadding)
                .padding(
                    start = dimensionResource(R.dimen.spacing_large),
                    end = dimensionResource(R.dimen.spacing_large),
                    bottom = dimensionResource(R.dimen.spacing_large)
                )
        )
    }
}

private fun LazyListScope.themeGroup(
    label: String,
    scaleLabel: String?,
    themes: List<MediaTheme>,
    totalEpisodes: Int?,
    coverUrl: String?,
    selectedThemeId: Int?,
    onThemeClick: (Int) -> Unit
) {
    item(key = "label_$label") {
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            // Every bar below shares this scale, which is what makes the stack readable.
            if (scaleLabel != null) {
                Text(
                    text = scaleLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
    items(themes, key = { it.id }) { theme ->
        ThemeRow(
            theme = theme,
            coverUrl = coverUrl,
            totalEpisodes = totalEpisodes,
            selected = theme.id == selectedThemeId,
            onClick = { onThemeClick(theme.id) }
        )
    }
}

@Composable
private fun ThemeFilterRow(
    selected: ThemeFilter,
    hasOpenings: Boolean,
    hasEndings: Boolean,
    onSelect: (ThemeFilter) -> Unit
) {
    if (!hasOpenings || !hasEndings) return

    SegmentedTabGroup(
        options = ThemeFilter.entries,
        selected = selected,
        onSelect = onSelect,
        label = {
            when (it) {
                ThemeFilter.All -> stringResource(R.string.themes_filter_all)
                ThemeFilter.Openings -> stringResource(R.string.themes_group_openings)
                ThemeFilter.Endings -> stringResource(R.string.themes_group_endings)
            }
        },
        fillEqually = true
    )
}
