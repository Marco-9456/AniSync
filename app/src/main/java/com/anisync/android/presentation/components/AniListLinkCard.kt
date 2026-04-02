package com.anisync.android.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.anisync.android.domain.LinkPreview
import com.anisync.android.domain.parser.RichTextBlock

@Composable
fun AniListLinkCard(
    block: RichTextBlock.AnilistLink,
    preview: LinkPreview?,
    style: TextStyle,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val typeColor = when (block.type.lowercase()) {
        "anime" -> Color(0xFF3DB4F2)
        "manga" -> Color(0xFFF2A33D)
        "character" -> Color(0xFFE03D51)
        "staff" -> Color(0xFF8F56C0)
        "user" -> Color(0xFF4CAF50)
        else -> MaterialTheme.colorScheme.primary
    }

    val title = preview?.title ?: block.displayTitle
    val imageUrl = preview?.imageUrl

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .clickable { onLinkClick(block.url) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (imageUrl != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                loading = { LetterFallback(block.type, typeColor, style) },
                error = { LetterFallback(block.type, typeColor, style) },
                modifier = Modifier
                    .width(48.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
        } else {
            LetterFallback(block.type, typeColor, style)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = style.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = block.type.replaceFirstChar { it.uppercase() },
                style = style.copy(
                    fontSize = 12.sp,
                    color = typeColor,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun LetterFallback(type: String, typeColor: Color, style: TextStyle) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(typeColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = type.take(1).uppercase(),
            style = style.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = typeColor
            )
        )
    }
}
