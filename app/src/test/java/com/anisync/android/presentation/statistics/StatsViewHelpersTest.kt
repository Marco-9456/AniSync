package com.anisync.android.presentation.statistics

import androidx.compose.ui.unit.dp
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.presentation.profile.ScoreUiModel
import com.anisync.android.presentation.profile.StatusUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsViewHelpersTest {

    @Test
    fun `a monogram takes the first letters of the first two words`() {
        assertEquals("KA", monogram("Kyoto Animation"))
        assertEquals("A1", monogram("A-1 Pictures"))
        assertEquals("TE", monogram("TMS Entertainment"))
        assertEquals("M", monogram("MAPPA"))
    }

    @Test
    fun `segments fill the bar and keep a small list visible`() {
        val widths = segmentWidths(listOf(1616, 1143, 11, 17, 24), 368.dp)
        assertEquals(368f, widths.sumOf { it.value.toDouble() }.toFloat(), 0.01f)
        assertTrue(widths.all { it >= 8.dp })
        assertTrue(widths[0] > widths[1])
    }

    @Test
    fun `the teaser keeps the top ten steps, scaled to their own tallest`() {
        val scores = (1..20).map { step ->
            val score = step * 5
            ScoreUiModel(score = score, label = score.toString(), normalizedScore = score / 100f, count = if (score == 85) 107 else step, heightFraction = 0f)
        }
        val teaser = scores.teaserScores(mostGiven = 85)
        assertEquals((11..20).map { it * 5 }, teaser.map { it.score })
        assertEquals(1f, teaser.single { it.score == 85 }.heightFraction, 0.0001f)
        assertEquals(11f / 107f, teaser.first().heightFraction, 0.0001f)
    }

    @Test
    fun `hero lists keep only the statuses the app knows`() {
        val statuses = listOf(
            StatusUiModel(status = "COMPLETED", count = 211, fraction = 0f, colorRoleIndex = 0),
            StatusUiModel(status = "PLANNING", count = 122, fraction = 0f, colorRoleIndex = 1),
            StatusUiModel(status = "Unknown", count = 3, fraction = 0f, colorRoleIndex = 2),
        )
        assertEquals(listOf(LibraryStatus.COMPLETED, LibraryStatus.PLANNING), statuses.heroLists().map { it.status })
        assertEquals(122, statuses.countOf(LibraryStatus.PLANNING))
        assertEquals(0, statuses.countOf(LibraryStatus.DROPPED))
    }
}
