package com.anisync.android.presentation.statistics

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.anisync.android.ui.theme.PreviewTheme
import com.anisync.android.domain.GenreStat
import com.anisync.android.presentation.profile.StatusUiModel

/**
 * Sample fixtures + shared seed for `@Preview` blocks across the statistics components.
 * Not used at runtime — referenced only from private @Preview composables, which the R8
 * shrinker drops from release builds.
 */

internal val PreviewSeedColor: Color = Color(0xFF8B5CF6)

/**
 * Preview wrapper used by every statistics @Preview. Installs [PreviewTheme] (which
 * sets the M3 color scheme + expressive typography) AND paints a [Surface] using
 * `colorScheme.background` so [SectionHeader] titles (which sit outside the section
 * card) render on the same canvas they would in the app — keeps dark-mode title
 * contrast correct.
 */
@Composable
internal fun StatPreviewSurface(isDark: Boolean, content: @Composable () -> Unit) {
    PreviewTheme(seedColor = PreviewSeedColor, isDark = isDark) {
        Surface(color = MaterialTheme.colorScheme.background, content = content)
    }
}

internal val previewStatuses: List<StatusUiModel> = listOf(
    StatusUiModel(status = "CURRENT", count = 6, fraction = 0.10f, colorRoleIndex = 0),
    StatusUiModel(status = "COMPLETED", count = 38, fraction = 0.60f, colorRoleIndex = 1),
    StatusUiModel(status = "PLANNING", count = 9, fraction = 0.15f, colorRoleIndex = 2),
    StatusUiModel(status = "PAUSED", count = 4, fraction = 0.07f, colorRoleIndex = 3),
    StatusUiModel(status = "DROPPED", count = 3, fraction = 0.05f, colorRoleIndex = 4),
    StatusUiModel(status = "REPEATING", count = 2, fraction = 0.03f, colorRoleIndex = 0)
)

internal val previewSingleStatus: List<StatusUiModel> = listOf(
    StatusUiModel(status = "COMPLETED", count = 42, fraction = 1f, colorRoleIndex = 1)
)

internal val previewGenres: List<GenreStat> = listOf(
    GenreStat(genre = "Action", count = 42, meanScore = 8.1f, hoursWatched = 120f),
    GenreStat(genre = "Romance", count = 31, meanScore = 7.6f, hoursWatched = 80f),
    GenreStat(genre = "Slice of Life", count = 18, meanScore = 7.9f, hoursWatched = 50f),
    GenreStat(genre = "Mystery / Psychological Thriller", count = 9, meanScore = 0f, hoursWatched = 30f)
)
