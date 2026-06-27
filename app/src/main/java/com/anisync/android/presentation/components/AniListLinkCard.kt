package com.anisync.android.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.anisync.android.domain.LinkPreview
import com.anisync.android.domain.parser.RichTextBlock
import com.anisync.android.ui.theme.emphasis

// Bounded so the card never stretches edge-to-edge on wide screens; on a phone it is ~full width.
private val MediaCardMaxWidth = 420.dp
private val MediaCoverWidth = 60.dp
private val MediaCoverHeight = 90.dp        // 2:3, matches AniList covers (no crop needed)
private val ProfileCardWidth = 132.dp
private val ProfileAvatarSize = 76.dp
private val CardShape = RoundedCornerShape(12.dp)
private val PosterShape = RoundedCornerShape(10.dp)

private fun typeColor(type: String): Color = when (type.lowercase()) {
    "anime" -> Color(0xFF3DB4F2)
    "manga" -> Color(0xFFF2A33D)
    "character" -> Color(0xFFE03D51)
    "staff" -> Color(0xFF8F56C0)
    "user" -> Color(0xFF4CAF50)
    "activity" -> Color(0xFF7E57C2)
    else -> Color(0xFF3DB4F2)
}

private fun String.parseHexColor(): Color? =
    runCatching { Color(android.graphics.Color.parseColor(this)) }.getOrNull()

private fun typeLabel(type: String): String = type.replaceFirstChar { it.uppercase() }

/**
 * Renders an AniList deep link as a rich preview card. Three shapes:
 * - anime/manga: a bounded horizontal card with a 2:3 cover, title and `format · year`.
 * - character/staff/user: a compact vertical card (poster/avatar over the name).
 * - activity: a compact chip — activities are heterogeneous (status/text/message) and have no clean
 *   title/cover, so a small "View activity" affordance reads better than an empty poster card.
 */
@Composable
fun AniListLinkCard(
    block: RichTextBlock.AnilistLink,
    preview: LinkPreview?,
    style: TextStyle,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    when (block.type.lowercase()) {
        "character", "staff", "user" -> ProfileLinkCard(block, preview, onLinkClick, modifier)
        "activity" -> ActivityLinkChip(block, onLinkClick, modifier)
        else -> MediaLinkCard(block, preview, onLinkClick, modifier)
    }
}

@Composable
private fun MediaLinkCard(
    block: RichTextBlock.AnilistLink,
    preview: LinkPreview?,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = typeColor(block.type)
    val title = preview?.title ?: block.displayTitle
    val coverColor = preview?.coverColor?.parseHexColor()
    val container = coverColor
        ?.copy(alpha = 0.22f)
        ?.compositeOver(MaterialTheme.colorScheme.surfaceContainerHigh)
        ?: MaterialTheme.colorScheme.surfaceContainerLowest
    val outline = (coverColor ?: accent).copy(alpha = 0.45f)

    Row(
        modifier = modifier
            .widthIn(max = MediaCardMaxWidth)
            .fillMaxWidth()
            .clip(CardShape)
            .background(container)
            .border(1.dp, outline, CardShape)
            .clickable { onLinkClick(block.url) }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CoverImage(
            url = preview?.imageUrl,
            type = block.type,
            accent = accent,
            modifier = Modifier
                .size(MediaCoverWidth, MediaCoverHeight)
                .clip(PosterShape)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.emphasis(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            preview?.subtitle?.let { sub ->
                Spacer(Modifier.height(2.dp))
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = typeLabel(block.type),
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ProfileLinkCard(
    block: RichTextBlock.AnilistLink,
    preview: LinkPreview?,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = typeColor(block.type)
    val title = preview?.title ?: block.displayTitle
    val isUser = block.type.equals("user", ignoreCase = true)

    Column(
        modifier = modifier
            .width(ProfileCardWidth)
            .clip(CardShape)
            .background(accent.copy(alpha = 0.10f).compositeOver(MaterialTheme.colorScheme.surfaceContainerLowest))
            .border(1.dp, accent.copy(alpha = 0.40f), CardShape)
            .clickable { onLinkClick(block.url) }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isUser) {
            val context = LocalContext.current
            val request = remember(preview?.imageUrl) {
                ImageRequest.Builder(context)
                    .data(preview?.imageUrl)
                    .allowHardware(false) // animated GIF/WebP avatars
                    .crossfade(true)
                    .build()
            }
            UserAvatar(
                contentDescription = title,
                size = ProfileAvatarSize,
                model = if (preview?.imageUrl != null) request else null
            )
        } else {
            CoverImage(
                url = preview?.imageUrl,
                type = block.type,
                accent = accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(PosterShape)
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = typeLabel(block.type),
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ActivityLinkChip(
    block: RichTextBlock.AnilistLink,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = typeColor("activity")
    Row(
        modifier = modifier
            .clip(CardShape)
            .background(accent.copy(alpha = 0.12f).compositeOver(MaterialTheme.colorScheme.surfaceContainerLowest))
            .border(1.dp, accent.copy(alpha = 0.40f), CardShape)
            .clickable { onLinkClick(block.url) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Comment,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = "View activity",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CoverImage(
    url: String?,
    type: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(accent.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        if (url != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                // Fit (not Crop) so the cover is never trimmed and its aspect ratio is preserved.
                contentScale = ContentScale.Fit,
                error = { LetterGlyph(type, accent) },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LetterGlyph(type, accent)
        }
    }
}

@Composable
private fun LetterGlyph(type: String, accent: Color) {
    Text(
        text = type.take(1).uppercase(),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = accent
    )
}

private fun fakeBlock(type: String, id: Int, slug: String?) =
    RichTextBlock.AnilistLink(type = type, id = id, url = "https://anilist.co/$type/$id", slug = slug)

@Preview(showBackground = true, backgroundColor = 0xFF101014)
@Composable
private fun MediaLinkCardPreview() {
    MaterialTheme {
        Surface(color = Color(0xFF101014)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AniListLinkCard(
                    block = fakeBlock("anime", 101347, "Mushoku-Tensei"),
                    preview = LinkPreview("Mushoku Tensei: Jobless Reincarnation", null, "#5C8AA6", "TV · 2021"),
                    style = MaterialTheme.typography.bodyMedium,
                    onLinkClick = {}
                )
                AniListLinkCard(
                    block = fakeBlock("activity", 1085098694, null),
                    preview = null,
                    style = MaterialTheme.typography.bodyMedium,
                    onLinkClick = {}
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF101014)
@Composable
private fun ProfileLinkCardsPreview() {
    MaterialTheme {
        Surface(color = Color(0xFF101014)) {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AniListLinkCard(
                    block = fakeBlock("character", 40, "Lelouch-Lamperouge"),
                    preview = LinkPreview("Lelouch Lamperouge", null),
                    style = MaterialTheme.typography.bodyMedium,
                    onLinkClick = {}
                )
                AniListLinkCard(
                    block = fakeBlock("user", 1, "Goldiizz"),
                    preview = LinkPreview("Goldiizz", null),
                    style = MaterialTheme.typography.bodyMedium,
                    onLinkClick = {}
                )
            }
        }
    }
}
