package com.anisync.android.presentation.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.url
import com.anisync.android.ui.theme.LocalExpressiveTypography

/**
 * A "poster wall" share card: the viewer's top favourites as a 3-wide cover grid under a banner
 * header. Built for the profile Favourites tab (anime / manga), it caps at six covers so the
 * exported image stays a clean, legible tile — not a wall of thumbnails.
 */
@Composable
fun FavouritesShareCard(
    heading: String,
    eyebrow: String,
    entries: List<LibraryEntry>,
    bannerUrl: String? = null,
    handle: String? = null,
    modifier: Modifier = Modifier,
) {
    val expressive = LocalExpressiveTypography.current
    val covers = entries.take(6)

    ShareCardScaffold(modifier = modifier, handle = handle) {
        ShareCardBannerBox(bannerUrl = bannerUrl, height = 92.dp, scrimAlpha = 0.78f) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = eyebrow.uppercase(),
                    style = expressive.statLabel,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = heading,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            covers.chunked(3).forEach { rowEntries ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowEntries.forEach { entry ->
                        CoverTile(entry = entry, modifier = Modifier.weight(1f))
                    }
                    // Pad the final row so a lone cover keeps the same tile width.
                    repeat(3 - rowEntries.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun CoverTile(entry: LibraryEntry, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val coverUrl = entry.coverUrl ?: entry.cover.url()
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = entry.titleUserPreferred,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
