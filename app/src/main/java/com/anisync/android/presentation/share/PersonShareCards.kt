package com.anisync.android.presentation.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.CharacterDetails
import com.anisync.android.domain.StaffDetails
import com.anisync.android.presentation.util.formatCompactNumber
import com.anisync.android.ui.theme.LocalExpressiveTypography

/**
 * Shareable card for a character: portrait, name (+ native name), favourites, quick facts, and a
 * "known for" strip of their top series covers. [displayName] arrives pre-resolved through the
 * viewer's title-language setting.
 */
@Composable
fun CharacterShareCard(
    details: CharacterDetails,
    displayName: String,
    handle: String? = null,
    modifier: Modifier = Modifier,
) {
    PersonShareCard(
        modifier = modifier,
        handle = handle,
        eyebrow = stringResource(R.string.share_person_character),
        portraitUrl = details.imageUrl,
        name = displayName,
        nativeName = details.nativeName?.takeIf { it != displayName },
        favourites = details.favourites,
        facts = listOfNotNull(details.gender, details.age, details.dateOfBirth),
        knownFor = details.media.take(3).map { KnownForItem(it.coverUrl, it.titleUserPreferred) },
    )
}

/**
 * Shareable card for a staff member: portrait, name, favourites, occupations, and a "known for"
 * strip — their top voiced characters when they act, else their top production covers.
 */
@Composable
fun StaffShareCard(
    details: StaffDetails,
    displayName: String,
    handle: String? = null,
    modifier: Modifier = Modifier,
) {
    val knownFor = if (details.voicedCharacters.isNotEmpty()) {
        details.voicedCharacters.take(3).map {
            KnownForItem(it.characterImageUrl, it.characterNameUserPreferred)
        }
    } else {
        details.productionMedia.take(3).map { KnownForItem(it.coverUrl, it.titleUserPreferred) }
    }
    PersonShareCard(
        modifier = modifier,
        handle = handle,
        eyebrow = stringResource(R.string.share_person_staff),
        portraitUrl = details.imageUrl,
        name = displayName,
        nativeName = details.nativeName?.takeIf { it != displayName },
        favourites = details.favourites,
        facts = details.primaryOccupations.take(3),
        knownFor = knownFor,
    )
}

private data class KnownForItem(val imageUrl: String?, val label: String)

/** Common layout for the character/staff cards: identity block, fact chips, known-for covers. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PersonShareCard(
    modifier: Modifier,
    handle: String?,
    eyebrow: String,
    portraitUrl: String?,
    name: String,
    nativeName: String?,
    favourites: Int?,
    facts: List<String>,
    knownFor: List<KnownForItem>,
) {
    val expressive = LocalExpressiveTypography.current

    ShareCardScaffold(modifier = modifier, handle = handle) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row {
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(140.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (portraitUrl != null) {
                        AsyncImage(
                            model = portraitUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = eyebrow.uppercase(),
                        style = expressive.statLabel,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (nativeName != null) {
                        Text(
                            text = nativeName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    favourites?.takeIf { it > 0 }?.let {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = formatCompactNumber(it),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = stringResource(R.string.share_media_favourites).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (facts.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    facts.forEach { fact -> ShareChip(fact) }
                }
            }

            if (knownFor.isNotEmpty()) {
                Column {
                    Text(
                        text = stringResource(R.string.share_known_for).uppercase(),
                        style = expressive.statLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        knownFor.forEach { item ->
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(2f / 3f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    if (item.imageUrl != null) {
                                        AsyncImage(
                                            model = item.imageUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        // Pad so 1–2 items keep the same tile width as a full row.
                        repeat(3 - knownFor.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}
