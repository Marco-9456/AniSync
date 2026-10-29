package com.anisync.android.presentation.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.type.MediaType
import com.anisync.android.ui.theme.ListIndicatorKind
import com.anisync.android.ui.theme.LocalExpressiveTypography
import com.anisync.android.ui.theme.listIndicatorArtColor

/** One list on the hero's bar and legend. */
@Immutable
data class HeroList(val status: LibraryStatus, val count: Int)

/**
 * The dashboard's lead card: what is counted, the count on the wide axis, the time behind it, and
 * where the lists stand. [staleNote] says when AniList has not counted the latest changes yet.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HeroDashboard(
    eyebrow: String,
    value: String,
    unit: String,
    lead: String?,
    lists: List<HeroList>,
    mediaType: MediaType,
    note: String?,
    staleNote: String?,
    modifier: Modifier = Modifier
) {
    val expressive = LocalExpressiveTypography.current
    val content = MaterialTheme.colorScheme.onPrimaryContainer
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(24.dp)
    ) {
        val unitStyle = MaterialTheme.typography.headlineSmall
        val heroStyle = fittedHeroStyle(value, maxWidth - measuredWidth(unit, unitStyle) - 10.dp)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = eyebrow.uppercase(), style = expressive.statLabel, color = content.copy(alpha = 0.7f))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = value, style = heroStyle, color = content, maxLines = 1)
                Text(
                    text = unit,
                    style = unitStyle,
                    color = content.copy(alpha = 0.7f),
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }
            if (lead != null) {
                Text(text = lead, style = expressive.editorialLead, color = content.copy(alpha = 0.85f))
            }
            if (lists.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                ListStatusBar(lists)
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    lists.sortedBy { LegendOrder.indexOf(it.status) }.forEach { list ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(list.status.barColor())
                            )
                            Text(text = list.status.toLabel(mediaType), style = MaterialTheme.typography.labelLarge, color = content)
                            Text(
                                text = list.count.toString(),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
                                color = content.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }
            if (note != null) {
                Spacer(Modifier.height(4.dp))
                Text(text = note, style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.7f))
            }
            if (staleNote != null) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = content.copy(alpha = 0.75f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(text = staleNote, style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.75f))
                }
            }
        }
    }
}

/** The bar orders lists by size, each at least [MinSegment] wide so a small list stays visible. */
@Composable
private fun ListStatusBar(lists: List<HeroList>) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val sorted = lists.filter { it.count > 0 }.sortedByDescending { it.count }
        val widths = segmentWidths(sorted.map { it.count }, maxWidth - SegmentGap * (sorted.size - 1).coerceAtLeast(0))
        Row(horizontalArrangement = Arrangement.spacedBy(SegmentGap)) {
            sorted.forEachIndexed { index, list ->
                Box(
                    Modifier
                        .width(widths[index])
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(list.status.barColor())
                )
            }
        }
    }
}

/** Shares of [available] by count, lifting small ones to [MinSegment] at the expense of the rest. */
internal fun segmentWidths(counts: List<Int>, available: Dp): List<Dp> {
    val total = counts.sum().coerceAtLeast(1)
    val raw = counts.map { available * (it.toFloat() / total) }
    val lifted = raw.map { maxOf(it, MinSegment) }
    val surplus = lifted.zip(raw).sumOf { (lift, share) -> (lift - share).value.toDouble() }.toFloat()
    val roomy = raw.filter { it > MinSegment }
    val roomyTotal = roomy.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f)
    return raw.map { share ->
        if (share > MinSegment) share - Dp(surplus * share.value / roomyTotal) else MinSegment
    }
}

private val SegmentGap = 3.dp
private val MinSegment = 8.dp

private val LegendOrder = listOf(
    LibraryStatus.COMPLETED,
    LibraryStatus.PLANNING,
    LibraryStatus.CURRENT,
    LibraryStatus.PAUSED,
    LibraryStatus.DROPPED,
    LibraryStatus.REPEATING,
    LibraryStatus.UNKNOWN,
)

/** The pale tint of each list, the same in both themes, as the frames draw the bar. */
private fun LibraryStatus.barColor() = listIndicatorArtColor(
    when (this) {
        LibraryStatus.CURRENT -> ListIndicatorKind.WATCHING
        LibraryStatus.REPEATING -> ListIndicatorKind.REPEATING
        LibraryStatus.PLANNING -> ListIndicatorKind.PLANNING
        LibraryStatus.PAUSED -> ListIndicatorKind.PAUSED
        LibraryStatus.COMPLETED -> ListIndicatorKind.COMPLETED
        LibraryStatus.DROPPED -> ListIndicatorKind.DROPPED
        LibraryStatus.UNKNOWN -> ListIndicatorKind.CUSTOM
    }
).container

/** Width [text] needs on one line in [style]. */
@Composable
internal fun measuredWidth(text: String, style: TextStyle): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(text, style, density, measurer) {
        with(density) { measurer.measure(text, style).size.width.toDp() }
    }
}

/** heroWide at 80/84, stepped down until [value] fits [maxWidth], never below 40. */
@Composable
private fun fittedHeroStyle(value: String, maxWidth: Dp): TextStyle {
    val role = LocalExpressiveTypography.current.heroWide
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(value, maxWidth, role, density, measurer) {
        var size = HeroSize
        var style = role.style(HeroSize.sp, HeroLineHeight.sp, HeroTracking.sp)
        while (size > HeroMinSize && with(density) { measurer.measure(value, style).size.width.toDp() } > maxWidth) {
            size -= 2f
            val scale = size / HeroSize
            style = role.style(size.sp, (HeroLineHeight * scale).sp, (HeroTracking * scale).sp)
        }
        style
    }
}

private const val HeroSize = 80f
private const val HeroLineHeight = 84f
private const val HeroTracking = -2f
private const val HeroMinSize = 40f

// region Previews

@Preview(showBackground = true, name = "HeroDashboard, a large list", widthDp = 416)
@Composable
private fun HeroDashboardLargePreview() {
    StatPreviewSurface(isDark = true) {
        HeroDashboard(
            eyebrow = "Anime · all time",
            value = "1,668",
            unit = "anime",
            lead = "168 days and 9 hours of watching",
            lists = listOf(
                HeroList(LibraryStatus.COMPLETED, 1616),
                HeroList(LibraryStatus.PLANNING, 1143),
                HeroList(LibraryStatus.CURRENT, 11),
                HeroList(LibraryStatus.PAUSED, 17),
                HeroList(LibraryStatus.DROPPED, 24),
            ),
            mediaType = MediaType.ANIME,
            note = "2,811 on your lists. Planning stays on the list but is not counted in the 1,668.",
            staleNote = null
        )
    }
}

@Preview(showBackground = true, name = "HeroDashboard, a new list", widthDp = 416)
@Composable
private fun HeroDashboardSmallPreview() {
    StatPreviewSurface(isDark = false) {
        HeroDashboard(
            eyebrow = "Anime · all time",
            value = "8",
            unit = "anime",
            lead = "21 days and 2 hours of watching",
            lists = listOf(HeroList(LibraryStatus.CURRENT, 6)),
            mediaType = MediaType.ANIME,
            note = null,
            staleNote = "AniList recounts these about every two days, so 2 changes from this week are not in yet."
        )
    }
}

// endregion
