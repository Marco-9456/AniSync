package com.anisync.android.presentation.discover.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.GroupedSearchResults
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.presentation.discover.ResultCategory
import com.anisync.android.type.MediaFormat
import com.anisync.android.ui.theme.LocalAvatarShape
import com.anisync.android.util.getTitle

/** Items listed inside a category panel before its "View all" link (the AniList-style overview). */
private const val PanelItemCap = 8

/** A panel's natural minimum width; the overview tiles up to three of them across (3 × 2 = six types). */
private val PanelMinWidth = 320.dp
private val PanelSpacing = 12.dp

/**
 * The shared search-results board used on **every** width so phone and tablet read identically: when no
 * category is active it shows the [SearchOverviewPanels] (one compact panel per type), otherwise the
 * single active category's full results. Item taps are routed by type so the caller can pane-select on a
 * tablet or push full screen on a phone.
 */
@Composable
fun SearchResultsPanels(
    activeCategory: ResultCategory,
    searchAnime: List<LibraryEntry>,
    searchManga: List<LibraryEntry>,
    groupedResults: GroupedSearchResults,
    titleLanguage: TitleLanguage,
    onShowAll: (ResultCategory) -> Unit,
    onMediaClick: (Int) -> Unit,
    onCharacterClick: (Int) -> Unit,
    onStaffClick: (Int) -> Unit,
    onStudioClick: (Int) -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (activeCategory == ResultCategory.ALL) {
        SearchOverviewPanels(
            searchAnime, searchManga, groupedResults, titleLanguage,
            onShowAll, onMediaClick, onCharacterClick, onStaffClick, onStudioClick, onUserClick, modifier,
        )
    } else {
        SearchCategoryResults(
            activeCategory, searchAnime, searchManga, groupedResults, titleLanguage,
            onMediaClick, onCharacterClick, onStaffClick, onStudioClick, onUserClick, modifier,
        )
    }
}

/**
 * The search **overview**: one compact panel per non-empty result type (anime · manga · characters ·
 * staff · studios · users), tiled up to three across so the six types read as a 3 × 2 board. Each panel
 * lists its first [PanelItemCap] results with a "View all N results" link that opens that single
 * category. Columns adapt to the available width, so the same board reflows from three columns on a
 * tablet down to one on a phone (or in a shrunken list pane) without losing the panel structure.
 */
@Composable
fun SearchOverviewPanels(
    searchAnime: List<LibraryEntry>,
    searchManga: List<LibraryEntry>,
    groupedResults: GroupedSearchResults,
    titleLanguage: TitleLanguage,
    onShowAll: (ResultCategory) -> Unit,
    onMediaClick: (Int) -> Unit,
    onCharacterClick: (Int) -> Unit,
    onStaffClick: (Int) -> Unit,
    onStudioClick: (Int) -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val avatarShape = LocalAvatarShape.current
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = (((maxWidth + PanelSpacing) / (PanelMinWidth + PanelSpacing)).toInt())
            .coerceIn(1, 3)

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(PanelSpacing),
            verticalArrangement = Arrangement.spacedBy(PanelSpacing),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (searchAnime.isNotEmpty()) {
                item(key = "panel_anime") {
                    SearchCategoryPanel(title = "Anime", total = searchAnime.size, onViewAll = { onShowAll(ResultCategory.ANIME) }) {
                        searchAnime.take(PanelItemCap).forEach { entry -> MediaResultRow(entry, titleLanguage) { onMediaClick(entry.mediaId) } }
                    }
                }
            }
            if (searchManga.isNotEmpty()) {
                item(key = "panel_manga") {
                    SearchCategoryPanel(title = "Manga", total = searchManga.size, onViewAll = { onShowAll(ResultCategory.MANGA) }) {
                        searchManga.take(PanelItemCap).forEach { entry -> MediaResultRow(entry, titleLanguage) { onMediaClick(entry.mediaId) } }
                    }
                }
            }
            if (groupedResults.characters.isNotEmpty()) {
                item(key = "panel_characters") {
                    SearchCategoryPanel(
                        title = stringResource(R.string.search_header_characters),
                        total = groupedResults.characters.size,
                        onViewAll = { onShowAll(ResultCategory.CHARACTERS) },
                    ) {
                        groupedResults.characters.take(PanelItemCap).forEach { character ->
                            SearchResultRow(
                                title = character.displayName,
                                subtitle = character.nativeName,
                                imageUrl = character.imageUrl,
                                imageShape = avatarShape,
                                fallbackIcon = Icons.Outlined.Person,
                                onClick = { onCharacterClick(character.id) },
                            )
                        }
                    }
                }
            }
            if (groupedResults.staff.isNotEmpty()) {
                item(key = "panel_staff") {
                    SearchCategoryPanel(
                        title = stringResource(R.string.search_header_staff),
                        total = groupedResults.staff.size,
                        onViewAll = { onShowAll(ResultCategory.STAFF) },
                    ) {
                        groupedResults.staff.take(PanelItemCap).forEach { staff ->
                            SearchResultRow(
                                title = staff.displayName,
                                subtitle = staff.primaryOccupations.firstOrNull() ?: staff.nativeName,
                                imageUrl = staff.imageUrl,
                                imageShape = avatarShape,
                                fallbackIcon = Icons.Outlined.Person,
                                onClick = { onStaffClick(staff.id) },
                            )
                        }
                    }
                }
            }
            if (groupedResults.studios.isNotEmpty()) {
                item(key = "panel_studios") {
                    SearchCategoryPanel(
                        title = stringResource(R.string.search_header_studios),
                        total = groupedResults.studios.size,
                        onViewAll = { onShowAll(ResultCategory.STUDIOS) },
                    ) {
                        groupedResults.studios.take(PanelItemCap).forEach { studio ->
                            SearchResultRow(
                                title = studio.displayName,
                                subtitle = studio.favourites?.let { stringResource(R.string.search_favourites_count, it) },
                                imageUrl = null,
                                imageShape = RoundedCornerShape(10.dp),
                                fallbackIcon = Icons.Outlined.Apartment,
                                onClick = { onStudioClick(studio.id) },
                            )
                        }
                    }
                }
            }
            if (groupedResults.users.isNotEmpty()) {
                item(key = "panel_users") {
                    SearchCategoryPanel(
                        title = stringResource(R.string.search_header_users),
                        total = groupedResults.users.size,
                        onViewAll = { onShowAll(ResultCategory.USERS) },
                    ) {
                        groupedResults.users.take(PanelItemCap).forEach { user ->
                            SearchResultRow(
                                title = user.displayName,
                                subtitle = null,
                                imageUrl = user.imageUrl,
                                imageShape = avatarShape,
                                fallbackIcon = Icons.Outlined.Person,
                                onClick = { onUserClick(user.displayName) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A single category's **full** results (shown after "View all"): the same compact rows as the overview,
 * each wrapped in its own card, tiled into as many columns as the width allows. The active-category
 * chips above stay the way back to the overview.
 */
@Composable
fun SearchCategoryResults(
    activeCategory: ResultCategory,
    searchAnime: List<LibraryEntry>,
    searchManga: List<LibraryEntry>,
    groupedResults: GroupedSearchResults,
    titleLanguage: TitleLanguage,
    onMediaClick: (Int) -> Unit,
    onCharacterClick: (Int) -> Unit,
    onStaffClick: (Int) -> Unit,
    onStudioClick: (Int) -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val avatarShape = LocalAvatarShape.current
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = (((maxWidth + PanelSpacing) / (PanelMinWidth + PanelSpacing)).toInt())
            .coerceIn(1, 3)

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(PanelSpacing),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            when (activeCategory) {
                ResultCategory.ANIME -> items(searchAnime, key = { "a_${it.mediaId}" }) { entry ->
                    PanelCardRow { MediaResultRow(entry, titleLanguage) { onMediaClick(entry.mediaId) } }
                }
                ResultCategory.MANGA -> items(searchManga, key = { "m_${it.mediaId}" }) { entry ->
                    PanelCardRow { MediaResultRow(entry, titleLanguage) { onMediaClick(entry.mediaId) } }
                }
                ResultCategory.CHARACTERS -> items(groupedResults.characters, key = { "c_${it.id}" }) { c ->
                    PanelCardRow {
                        SearchResultRow(c.displayName, c.nativeName, c.imageUrl, avatarShape, Icons.Outlined.Person) {
                            onCharacterClick(c.id)
                        }
                    }
                }
                ResultCategory.STAFF -> items(groupedResults.staff, key = { "s_${it.id}" }) { s ->
                    PanelCardRow {
                        SearchResultRow(s.displayName, s.primaryOccupations.firstOrNull() ?: s.nativeName, s.imageUrl, avatarShape, Icons.Outlined.Person) {
                            onStaffClick(s.id)
                        }
                    }
                }
                ResultCategory.STUDIOS -> items(groupedResults.studios, key = { "st_${it.id}" }) { st ->
                    PanelCardRow {
                        SearchResultRow(st.displayName, st.favourites?.let { stringResource(R.string.search_favourites_count, it) }, null, RoundedCornerShape(10.dp), Icons.Outlined.Apartment) {
                            onStudioClick(st.id)
                        }
                    }
                }
                ResultCategory.USERS -> items(groupedResults.users, key = { "u_${it.id}" }) { u ->
                    PanelCardRow {
                        SearchResultRow(u.displayName, null, u.imageUrl, avatarShape, Icons.Outlined.Person) {
                            onUserClick(u.displayName)
                        }
                    }
                }
                ResultCategory.ALL -> Unit
            }
        }
    }
}

/** A rounded panel: a colored type header, its result rows, then a "View all" link when there are more. */
@Composable
private fun SearchCategoryPanel(
    title: String,
    total: Int,
    onViewAll: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(vertical = 10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
            )
            content()
            if (total > PanelItemCap) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onViewAll)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.search_view_all_results, total),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

/** Wraps a single-category row in the same panel card tone so the full list keeps the overview look. */
@Composable
private fun PanelCardRow(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) { content() }
    }
}

@Composable
private fun MediaResultRow(
    entry: LibraryEntry,
    titleLanguage: TitleLanguage,
    onClick: () -> Unit,
) {
    SearchResultRow(
        title = entry.getTitle(titleLanguage),
        subtitle = entry.format?.label(),
        imageUrl = entry.coverUrl,
        imageShape = RoundedCornerShape(6.dp),
        fallbackIcon = Icons.Outlined.Movie,
        imageWidth = 38.dp,
        imageHeight = 52.dp,
        onClick = onClick,
    )
}

/**
 * One compact result row: a small thumbnail (cover, avatar, or an icon placeholder) + a one-line title
 * and an optional muted subtitle. Avatars use the user-selected [LocalAvatarShape]; covers and studio
 * placeholders keep their own rounded shapes. Shared by the overview panels and the full lists.
 */
@Composable
private fun SearchResultRow(
    title: String,
    subtitle: String?,
    imageUrl: String?,
    imageShape: Shape,
    fallbackIcon: ImageVector,
    imageWidth: Dp = 44.dp,
    imageHeight: Dp = 44.dp,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .width(imageWidth)
                .height(imageHeight)
                .clip(imageShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = fallbackIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun MediaFormat.label(): String? = when (this) {
    MediaFormat.TV -> "TV"
    MediaFormat.TV_SHORT -> "TV Short"
    MediaFormat.MOVIE -> "Movie"
    MediaFormat.SPECIAL -> "Special"
    MediaFormat.OVA -> "OVA"
    MediaFormat.ONA -> "ONA"
    MediaFormat.MUSIC -> "Music"
    MediaFormat.MANGA -> "Manga"
    MediaFormat.NOVEL -> "Novel"
    MediaFormat.ONE_SHOT -> "One-shot"
    else -> null
}
