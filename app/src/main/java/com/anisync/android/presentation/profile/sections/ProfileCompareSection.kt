package com.anisync.android.presentation.profile.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.data.WhatsNewItem
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import com.anisync.android.presentation.components.EmptyState
import com.anisync.android.presentation.components.MediaPosterCard
import com.anisync.android.presentation.components.MediaPosterCardDefaults
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.presentation.components.WhatsNewPill
import com.anisync.android.presentation.components.rememberActiveWhatsNew
import com.anisync.android.presentation.profile.ProfileTab
import com.anisync.android.presentation.profile.ProfileUiState
import com.anisync.android.presentation.profile.components.ProfileListRow
import com.anisync.android.presentation.profile.lists.ListComparison
import com.anisync.android.presentation.profile.lists.ScoreVerdict
import com.anisync.android.presentation.profile.lists.SharedTitle
import com.anisync.android.presentation.profile.lists.compareLists
import com.anisync.android.presentation.util.formatAndYear
import com.anisync.android.presentation.util.pluralStringResourceOrDefault
import com.anisync.android.type.MediaType
import com.anisync.android.ui.theme.LocalExpressiveTypography
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun OverviewCompareCard(
    comparison: ListComparison,
    ownerName: String,
    yourAvatar: String?,
    theirAvatar: String?,
    onCompare: () -> Unit,
    modifier: Modifier = Modifier
) {
    CompareCard(stringResource(R.string.profile_compare_title, ownerName), yourAvatar, theirAvatar, modifier) {
        Spacer(modifier = Modifier.height(14.dp))
        CompareHero(comparison, ownerName, MediaType.ANIME)
        comparison.sameScore?.let {
            CompareFact(Icons.Outlined.CheckCircle, stringResource(R.string.profile_compare_same_score, it.theirs.titleUserPreferred))
        }
        if (comparison.theirTensNotYours.isNotEmpty()) {
            val count = comparison.theirTensNotYours.size
            CompareFact(
                Icons.Default.Star,
                pluralStringResourceOrDefault(R.plurals.profile_compare_their_tens, count, count.toString(), ownerName)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        FilledTonalButton(onClick = onCompare) {
            Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.profile_compare_open))
            if (WhatsNewItem.PROFILE_COMPARE in rememberActiveWhatsNew()) {
                Spacer(modifier = Modifier.width(8.dp))
                WhatsNewPill(WhatsNewItem.Kind.NEW)
            }
        }
    }
}

/** Holds the card's place while their list loads, so Overview does not jump when it arrives. */
@Composable
internal fun OverviewCompareCardLoading(
    ownerName: String,
    yourAvatar: String?,
    theirAvatar: String?,
    modifier: Modifier = Modifier
) {
    CompareCard(stringResource(R.string.profile_compare_title, ownerName), yourAvatar, theirAvatar, modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
        ) {
            AppCircularProgressIndicator()
        }
    }
}

@Composable
private fun CompareCard(
    eyebrow: String,
    yourAvatar: String?,
    theirAvatar: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            CompareEyebrow(eyebrow, yourAvatar, theirAvatar)
            content()
        }
    }
}

@Composable
private fun CompareEyebrow(text: String, yourAvatar: String?, theirAvatar: String?) {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box {
            UserAvatar(contentDescription = null, size = 28.dp, url = yourAvatar)
            UserAvatar(contentDescription = null, size = 28.dp, url = theirAvatar, modifier = Modifier.offset(x = 20.dp))
        }
        Spacer(modifier = Modifier.width(30.dp))
        Text(
            text = text.uppercase(locale),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun CompareHero(comparison: ListComparison, ownerName: String, type: MediaType, too: Boolean = false) {
    val numbers = remember { NumberFormat.getIntegerInstance() }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = numbers.format(comparison.sharedCount),
            style = LocalExpressiveTypography.current.heroWide.style(64.sp, 64.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(14.dp))
        val sentence = when {
            type == MediaType.MANGA && too -> R.plurals.profile_compare_shared_manga_too
            type == MediaType.MANGA -> R.plurals.profile_compare_shared_manga
            too -> R.plurals.profile_compare_shared_anime_too
            else -> R.plurals.profile_compare_shared_anime
        }
        Text(
            text = pluralStringResourceOrDefault(
                sentence,
                comparison.sharedCount,
                numbers.format(comparison.yourCount),
                ownerName
            ),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CompareFact(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(top = 10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun LazyListScope.profileComparePage(
    comparison: ListComparison?,
    isLoading: Boolean,
    type: MediaType,
    ownerName: String,
    yourAvatar: String?,
    theirAvatar: String?,
    yourScoreFormat: ScoreFormat,
    titleLanguage: TitleLanguage,
    onMediaClick: (Int) -> Unit,
    expanded: Set<String> = emptySet(),
    onExpand: (String) -> Unit = {}
) {
    if (comparison == null || isLoading) {
        item(key = "compare_loading") {
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp), contentAlignment = Alignment.Center) {
                AppCircularProgressIndicator()
            }
        }
        return
    }
    if (comparison.yourCount == 0) {
        item(key = "compare_empty") {
            EmptyState(
                icon = Icons.Default.Groups,
                title = stringResource(R.string.profile_compare_nothing),
                description = stringResource(R.string.profile_compare_nothing_body, ownerName),
                modifier = Modifier.height(360.dp)
            )
        }
        return
    }
    item(key = "compare_summary") {
        CompareSummary(comparison, type, ownerName, yourAvatar, theirAvatar)
    }
    sharedSection("compare_both", R.string.profile_compare_both_scored, comparison.bothScored, yourScoreFormat, titleLanguage, type, onMediaClick, "compare_both" in expanded, onExpand, yourAvatar, theirAvatar, ownerName)
    sharedSection("compare_also", R.string.profile_compare_also_shared, comparison.alsoShared, yourScoreFormat, titleLanguage, type, onMediaClick, "compare_also" in expanded, onExpand, yourAvatar, theirAvatar, ownerName)
    if (comparison.theirTensNotYours.isNotEmpty()) {
        item(key = "compare_tens") {
            PosterShelf(
                title = stringResource(R.string.profile_compare_their_tens_title, ownerName),
                items = comparison.theirTensNotYours,
                titleLanguage = titleLanguage,
                onMediaClick = onMediaClick,
                prefix = "compare_tens"
            )
        }
    }
    if (comparison.onlyYours.isNotEmpty()) {
        item(key = "compare_only_yours") {
            PosterShelf(
                title = stringResource(R.string.profile_compare_only_yours),
                items = comparison.onlyYours,
                titleLanguage = titleLanguage,
                onMediaClick = onMediaClick,
                prefix = "compare_only"
            )
        }
    }
}

@Composable
private fun CompareSummary(comparison: ListComparison, type: MediaType, ownerName: String, yourAvatar: String?, theirAvatar: String?) {
    val numbers = remember { NumberFormat.getIntegerInstance() }
    CompareCard(
        eyebrow = stringResource(if (type == MediaType.MANGA) R.string.profile_compare_both_manga else R.string.profile_compare_both_anime),
        yourAvatar = yourAvatar,
        theirAvatar = theirAvatar,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))
        CompareHero(comparison, ownerName, type, too = true)
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MeanBox(comparison.yourMean, stringResource(R.string.profile_compare_your_mean), Modifier.weight(1f))
            MeanBox(comparison.theirMean, stringResource(R.string.profile_compare_their_mean, ownerName), Modifier.weight(1f))
        }
        if (comparison.theirsNotYours > 0) {
            CompareFact(
                Icons.Outlined.PersonAdd,
                pluralStringResourceOrDefault(
                    if (type == MediaType.MANGA) R.plurals.profile_compare_theirs_not_yours_manga else R.plurals.profile_compare_theirs_not_yours_anime,
                    comparison.theirsNotYours,
                    ownerName,
                    numbers.format(comparison.theirsNotYours)
                )
            )
        }
        if (comparison.onlyYours.isNotEmpty()) {
            CompareFact(
                Icons.Outlined.Person,
                pluralStringResourceOrDefault(
                    R.plurals.profile_compare_yours_not_theirs,
                    comparison.onlyYours.size,
                    numbers.format(comparison.onlyYours.size),
                    ownerName
                )
            )
        }
    }
}

@Composable
private fun MeanBox(mean: Double?, label: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = mean?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "-",
                style = LocalExpressiveTypography.current.titleWide.style(28.sp, 34.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun LazyListScope.sharedSection(
    key: String,
    titleRes: Int,
    titles: List<SharedTitle>,
    yourScoreFormat: ScoreFormat,
    titleLanguage: TitleLanguage,
    type: MediaType,
    onMediaClick: (Int) -> Unit,
    expanded: Boolean,
    onExpand: (String) -> Unit,
    yourAvatar: String?,
    theirAvatar: String?,
    ownerName: String
) {
    if (titles.isEmpty()) return
    item(key = "${key}_header") {
        OverviewHeader(
            title = stringResource(titleRes),
            onAction = null,
            subtitle = pluralStringResourceOrDefault(R.plurals.stats_titles, titles.size, titles.size.toString())
        )
    }
    val shown = if (expanded) titles else titles.take(COMPARE_PREVIEW)
    items(shown, key = { "${key}_${it.theirs.mediaId}" }) { shared ->
        ProfileListRow(
            entry = shared.theirs,
            mediaType = type,
            ownerAvatar = theirAvatar,
            onClick = { onMediaClick(shared.theirs.mediaId) },
            titleLanguage = titleLanguage,
            yours = shared.yours,
            yourAvatar = yourAvatar,
            yourScoreFormat = yourScoreFormat,
            factsLead = when (shared.verdict) {
                ScoreVerdict.SAME -> stringResource(R.string.profile_compare_verdict_same)
                ScoreVerdict.YOURS_HIGHER -> stringResource(R.string.profile_compare_verdict_yours)
                ScoreVerdict.THEIRS_HIGHER -> stringResource(R.string.profile_compare_verdict_theirs, ownerName)
                null -> null
            },
            dateSort = null,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
    if (shown.size < titles.size) {
        item(key = "${key}_more") {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                FilledTonalButton(onClick = { onExpand(key) }, modifier = Modifier.padding(top = 8.dp)) {
                    Text(stringResource(R.string.profile_compare_show_all, titles.size))
                }
            }
        }
    }
}

private const val COMPARE_PREVIEW = 10

@Composable
private fun PosterShelf(
    title: String,
    items: List<LibraryEntry>,
    titleLanguage: TitleLanguage,
    onMediaClick: (Int) -> Unit,
    prefix: String
) {
    Column {
        OverviewHeader(
            title = title,
            onAction = null,
            subtitle = pluralStringResourceOrDefault(R.plurals.stats_titles, items.size, items.size.toString())
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.mediaId }) { entry ->
                val facts = entry.formatAndYear()
                MediaPosterCard(
                    item = entry,
                    onClick = { onMediaClick(entry.mediaId) },
                    titleLanguage = titleLanguage,
                    transitionPrefix = prefix,
                    listStatus = null,
                    modifier = Modifier.width(112.dp),
                    meta = { if (facts != null) MediaPosterCardDefaults.MetaText(facts) }
                )
            }
        }
    }
}

@Composable
internal fun rememberListComparison(uiState: ProfileUiState, isOwnProfile: Boolean): ListComparison? {
    val type = if (uiState.selectedTab == ProfileTab.COMPARE) uiState.compareType else MediaType.ANIME
    val enabled = !isOwnProfile && (uiState.selectedTab == ProfileTab.OVERVIEW || uiState.selectedTab == ProfileTab.COMPARE)
    val theirs = if (type == MediaType.MANGA) uiState.userMangaList else uiState.userAnimeList
    return remember(theirs, uiState.viewerEntries, uiState.viewerScoreFormat, type, enabled) {
        if (!enabled || theirs.isEmpty()) null else compareLists(theirs, uiState.viewerEntries.values, type, uiState.viewerScoreFormat)
    }
}
