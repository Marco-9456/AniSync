package com.anisync.android.presentation.statistics

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.anisync.android.domain.StaffStat
import com.anisync.android.domain.VoiceActorStat
import com.anisync.android.ui.theme.LocalAvatarShape

internal val StatPersonCardWidth = 152.dp
internal val StatPersonCardHeight = 236.dp

@Composable
fun VoiceActorCardModern(va: VoiceActorStat, onClick: () -> Unit = {}) {
    PersonCard(
        name = va.name,
        imageUrl = va.imageUrl,
        countLabel = "${va.count} roles",
        onClick = onClick
    )
}

@Composable
fun StaffCardModern(staff: StaffStat, onClick: () -> Unit = {}) {
    PersonCard(
        name = staff.name,
        imageUrl = staff.imageUrl,
        countLabel = "${staff.count} works",
        onClick = onClick
    )
}

@Composable
private fun PersonCard(
    name: String,
    imageUrl: String?,
    countLabel: String,
    onClick: () -> Unit = {}
) {
    val imageShape = LocalAvatarShape.current
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(StatPersonCardWidth)
            .height(StatPersonCardHeight),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(imageShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(imageShape)
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = name.uppercase(),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = countLabel,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontStyle = FontStyle.Italic
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// region Previews

@Preview(showBackground = true, name = "VoiceActor — null image fallback")
@Composable
private fun VoiceActorNullImagePreview() {
    StatPreviewSurface(isDark = false) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            previewVAs.forEach { VoiceActorCardModern(it) }
        }
    }
}

@Preview(showBackground = true, name = "Staff — null image fallback")
@Composable
private fun StaffNullImagePreview() {
    StatPreviewSurface(isDark = false) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            previewStaff.forEach { StaffCardModern(it) }
        }
    }
}

@Preview(
    name = "Pixel 10 Pro XL",
    showBackground = true,
    device = "id:pixel_10_pro_xl"
)
@Preview(showBackground = true, name = "PersonCard — long name truncation")
@Composable
private fun PersonCardLongNamePreview() {
    StatPreviewSurface(isDark = false) {
        Row(Modifier.padding(8.dp)) {
            VoiceActorCardModern(VoiceActorStat(
                id = 99,
                name = "An Extremely Long Voice Actor Name That Surely Truncates To Two Lines",
                imageUrl = null, count = 1, meanScore = 7.5f, hoursWatched = 4f
            ))
        }
    }
}

@Preview(showBackground = true, name = "PersonCards — dark")
@Composable
private fun PersonCardsDarkPreview() {
    StatPreviewSurface(isDark = true) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VoiceActorCardModern(previewVAs.first())
            StaffCardModern(previewStaff.first())
        }
    }
}

// endregion
