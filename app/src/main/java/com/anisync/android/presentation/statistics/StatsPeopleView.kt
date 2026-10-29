package com.anisync.android.presentation.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.formatExactCount
import com.anisync.android.domain.formatOneDecimal
import com.anisync.android.presentation.util.pluralStringResourceOrDefault
import com.anisync.android.ui.theme.LocalAvatarShape
import com.anisync.android.ui.theme.LocalExpressiveTypography
import com.anisync.android.ui.theme.lineBox
import kotlin.math.roundToInt

/** One person or studio in a People list. [count] is roles for voice actors and titles otherwise. */
@Immutable
data class PersonRow(
    val id: Int,
    val name: String,
    val imageUrl: String?,
    val count: Int,
    val hours: Float,
    val meanScore: Float,
)

/** The person who turns up most, large, with how often and whether they are a favourite too. */
@Composable
fun PeopleSpotlight(
    person: PersonRow,
    eyebrow: String,
    detail: String,
    isFavourite: Boolean,
    isOwnProfile: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val content = scheme.onPrimary
    val nameStyle = LocalExpressiveTypography.current.titleWide.style(26.sp, 32.sp, (-0.5).sp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(scheme.primary)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = person.imageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(96.dp)
                .clip(LocalAvatarShape.current)
                .background(scheme.surfaceVariant)
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = eyebrow.uppercase(), style = LocalExpressiveTypography.current.statLabel, color = content.copy(alpha = 0.75f))
            Text(text = person.name, style = nameStyle, color = content, modifier = Modifier.lineBox(nameStyle))
            Text(text = detail, style = MaterialTheme.typography.bodyMedium, color = content.copy(alpha = 0.85f))
            if (isFavourite) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(scheme.onPrimaryContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
                    Text(
                        text = stringResource(if (isOwnProfile) R.string.stats_people_favourite_own else R.string.stats_people_favourite_other),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
                        color = content
                    )
                }
            }
        }
    }
}

/**
 * Ranked people or studios, the first rank lit. Each row opens its page. Studios draw a monogram
 * in place of a picture.
 */
@Composable
fun PeopleListCard(
    rows: List<PersonRow>,
    countLabel: @Composable (Int) -> String,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    monogram: Boolean = false,
) {
    if (rows.isEmpty()) return
    val locale = LocalConfiguration.current.locales[0]
    val scheme = MaterialTheme.colorScheme
    val rankStyle = LocalExpressiveTypography.current.titleWide.style(16.sp, 22.sp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(scheme.surfaceContainerLow)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        rows.forEachIndexed { index, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(role = Role.Button) { onClick(row.id) }
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = (index + 1).toString(),
                    style = rankStyle,
                    color = if (index == 0) scheme.primary else scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .width(22.dp)
                        .lineBox(rankStyle)
                )
                if (monogram) {
                    StudioMonogram(row.name, size = 48.dp, textSize = MONOGRAM_TEXT)
                } else {
                    AsyncImage(
                        model = row.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(LocalAvatarShape.current)
                            .background(scheme.surfaceVariant)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = row.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(
                            R.string.stats_people_line,
                            countLabel(row.count),
                            stringResource(R.string.stats_genre_hours, formatExactCount(row.hours.roundToInt(), locale))
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                if (row.meanScore > 0f) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(scheme.surfaceContainerHigh)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(14.dp))
                        Text(
                            text = formatOneDecimal(row.meanScore.toDouble(), locale),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.sp
                            ),
                            color = scheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/** A 48dp monogram carries its initials at this size, as the frame draws it. */
private const val MONOGRAM_TEXT = 14.5f

/** "51 roles" or "89 titles", for a row's count. */
@Composable
fun rolesLabel(count: Int): String = pluralStringResourceOrDefault(R.plurals.stats_roles, count, count)

@Composable
fun titlesLabel(count: Int): String {
    val locale = LocalConfiguration.current.locales[0]
    return pluralStringResourceOrDefault(R.plurals.stats_titles, count, formatExactCount(count, locale))
}
