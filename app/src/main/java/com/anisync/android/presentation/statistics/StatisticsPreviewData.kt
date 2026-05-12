package com.anisync.android.presentation.statistics

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.anisync.android.domain.CountryStat
import com.anisync.android.ui.theme.PreviewTheme
import com.anisync.android.domain.FormatStat
import com.anisync.android.domain.GenreStat
import com.anisync.android.domain.StaffStat
import com.anisync.android.domain.StudioStat
import com.anisync.android.domain.TagStat
import com.anisync.android.domain.VoiceActorStat
import com.anisync.android.presentation.profile.LengthUiModel
import com.anisync.android.presentation.profile.ScoreUiModel
import com.anisync.android.presentation.profile.StatusUiModel
import com.anisync.android.presentation.profile.YearUiModel

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

internal val previewScores: List<ScoreUiModel> = listOf(
    ScoreUiModel(score = 10, label = "10", normalizedScore = 0.10f, count = 2, heightFraction = 0.10f),
    ScoreUiModel(score = 20, label = "20", normalizedScore = 0.20f, count = 1, heightFraction = 0.05f),
    ScoreUiModel(score = 30, label = "30", normalizedScore = 0.30f, count = 0, heightFraction = 0f),
    ScoreUiModel(score = 40, label = "40", normalizedScore = 0.40f, count = 3, heightFraction = 0.15f),
    ScoreUiModel(score = 50, label = "50", normalizedScore = 0.50f, count = 6, heightFraction = 0.30f),
    ScoreUiModel(score = 60, label = "60", normalizedScore = 0.60f, count = 9, heightFraction = 0.45f),
    ScoreUiModel(score = 70, label = "70", normalizedScore = 0.70f, count = 14, heightFraction = 0.70f),
    ScoreUiModel(score = 80, label = "80", normalizedScore = 0.80f, count = 20, heightFraction = 1.00f),
    ScoreUiModel(score = 90, label = "90", normalizedScore = 0.90f, count = 12, heightFraction = 0.60f),
    ScoreUiModel(score = 100, label = "100", normalizedScore = 1.00f, count = 4, heightFraction = 0.20f)
)

internal val previewScoresAllZero: List<ScoreUiModel> = previewScores.map { it.copy(count = 0, heightFraction = 0f) }

internal val previewYears: List<YearUiModel> = listOf(
    YearUiModel(year = 2018, count = 4, heightFraction = 0.20f),
    YearUiModel(year = 2019, count = 7, heightFraction = 0.35f),
    YearUiModel(year = 2020, count = 12, heightFraction = 0.60f),
    YearUiModel(year = 2021, count = 20, heightFraction = 1.00f),
    YearUiModel(year = 2022, count = 15, heightFraction = 0.75f),
    YearUiModel(year = 2023, count = 9, heightFraction = 0.45f),
    YearUiModel(year = 2024, count = 6, heightFraction = 0.30f),
    YearUiModel(year = 2025, count = 2, heightFraction = 0.10f)
)

internal val previewStartYears: List<YearUiModel> = listOf(
    YearUiModel(year = 2020, count = 8, heightFraction = 0.45f),
    YearUiModel(year = 2021, count = 18, heightFraction = 1.00f),
    YearUiModel(year = 2022, count = 14, heightFraction = 0.78f),
    YearUiModel(year = 2023, count = 11, heightFraction = 0.61f),
    YearUiModel(year = 2024, count = 5, heightFraction = 0.28f)
)

internal val previewSingleYear: List<YearUiModel> = listOf(
    YearUiModel(year = 2024, count = 25, heightFraction = 1.0f)
)

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

internal val previewTags: List<TagStat> = listOf(
    TagStat(id = 1, name = "Magic", count = 18, meanScore = 8.0f, hoursWatched = 60f),
    TagStat(id = 2, name = "School", count = 14, meanScore = 7.4f, hoursWatched = 40f),
    TagStat(id = 3, name = "Time Travel", count = 6, meanScore = 8.6f, hoursWatched = 20f),
    TagStat(id = 4, name = "Demons", count = 11, meanScore = 7.9f, hoursWatched = 30f),
    TagStat(id = 5, name = "Coming of Age", count = 4, meanScore = 7.0f, hoursWatched = 15f),
    TagStat(id = 6, name = "Found Family", count = 9, meanScore = 8.2f, hoursWatched = 25f),
    TagStat(id = 7, name = "Tournament", count = 3, meanScore = 6.8f, hoursWatched = 10f)
)

internal val previewFormats: List<FormatStat> = listOf(
    FormatStat(format = "TV", count = 56, meanScore = 7.9f, hoursWatched = 280f),
    FormatStat(format = "MOVIE", count = 14, meanScore = 8.3f, hoursWatched = 30f),
    FormatStat(format = "OVA", count = 6, meanScore = 7.1f, hoursWatched = 8f),
    FormatStat(format = "ONA", count = 4, meanScore = 7.5f, hoursWatched = 6f),
    FormatStat(format = "SPECIAL", count = 3, meanScore = 6.8f, hoursWatched = 2f),
    FormatStat(format = "MUSIC", count = 2, meanScore = 0f, hoursWatched = 0.5f),
    FormatStat(format = "MANGA", count = 22, meanScore = 8.0f, hoursWatched = 0f)
)

internal val previewStudios: List<StudioStat> = listOf(
    StudioStat(id = 569, studioName = "Mappa", count = 14, meanScore = 8.2f, hoursWatched = 70f),
    StudioStat(id = 2, studioName = "Kyoto Animation", count = 11, meanScore = 8.7f, hoursWatched = 55f),
    StudioStat(id = 9999, studioName = "Z", count = 1, meanScore = 6.0f, hoursWatched = 2f),
    StudioStat(id = 9998, studioName = "An Extremely Long Studio Name That Will Surely Truncate", count = 3, meanScore = 7.3f, hoursWatched = 12f)
)

internal val previewVAs: List<VoiceActorStat> = listOf(
    VoiceActorStat(id = 1, name = "Megumi Hayashibara", imageUrl = null, count = 18, meanScore = 8.1f, hoursWatched = 70f),
    VoiceActorStat(id = 2, name = "Aoi Yuuki", imageUrl = null, count = 12, meanScore = 7.9f, hoursWatched = 45f),
    VoiceActorStat(id = 3, name = "Yuuki Kaji", imageUrl = null, count = 9, meanScore = 7.7f, hoursWatched = 30f)
)

internal val previewStaff: List<StaffStat> = listOf(
    StaffStat(id = 1, name = "Hayao Miyazaki", imageUrl = null, count = 11, meanScore = 9.0f, hoursWatched = 22f),
    StaffStat(id = 2, name = "Mamoru Hosoda", imageUrl = null, count = 6, meanScore = 8.4f, hoursWatched = 12f),
    StaffStat(id = 3, name = "Satoshi Kon", imageUrl = null, count = 4, meanScore = 8.9f, hoursWatched = 8f)
)

internal val previewCountries: List<CountryStat> = listOf(
    CountryStat(countryCode = "JP", count = 84, meanScore = 7.9f, hoursWatched = 300f),
    CountryStat(countryCode = "KR", count = 12, meanScore = 7.6f, hoursWatched = 40f),
    CountryStat(countryCode = "CN", count = 6, meanScore = 7.2f, hoursWatched = 18f),
    CountryStat(countryCode = "TW", count = 2, meanScore = 7.0f, hoursWatched = 5f),
    CountryStat(countryCode = "US", count = 1, meanScore = 6.5f, hoursWatched = 2f)
)

internal val previewCountriesMany: List<CountryStat> = listOf(
    "JP", "KR", "CN", "TW", "US", "FR", "DE", "GB", "IT", "ES", "BR", "AR", "RU"
).mapIndexed { i, c -> CountryStat(countryCode = c, count = (20 - i).coerceAtLeast(1), meanScore = 7f, hoursWatched = 5f) }

internal val previewLengths: List<LengthUiModel> = listOf(
    LengthUiModel(label = "<10m", count = 2, heightFraction = 0.10f),
    LengthUiModel(label = "10-20m", count = 8, heightFraction = 0.30f),
    LengthUiModel(label = "20-30m", count = 24, heightFraction = 1.00f),
    LengthUiModel(label = "30-45m", count = 6, heightFraction = 0.25f),
    LengthUiModel(label = "45m+", count = 4, heightFraction = 0.16f)
)

internal val previewLengthsShort: List<LengthUiModel> = listOf(
    LengthUiModel(label = "1 vol", count = 6, heightFraction = 0.4f),
    LengthUiModel(label = "2+ vol", count = 14, heightFraction = 1.0f)
)
