package com.anisync.android.presentation.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anisync.android.domain.StaffStat
import com.anisync.android.domain.VoiceActorStat

internal val StatPersonCardHeight = 196.dp
internal val StatPersonAvatarSize = 96.dp

@Composable
fun VoiceActorCardModern(va: VoiceActorStat) {
    PersonCard(
        name = va.name,
        imageUrl = va.imageUrl,
        countLabel = "${va.count} roles"
    )
}

@Composable
fun StaffCardModern(staff: StaffStat) {
    PersonCard(
        name = staff.name,
        imageUrl = staff.imageUrl,
        countLabel = "${staff.count} works"
    )
}

@Composable
private fun PersonCard(name: String, imageUrl: String?, countLabel: String) {
    Card(
        modifier = Modifier
            .width(140.dp)
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
                    .size(StatPersonAvatarSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(StatPersonAvatarSize)
                            .clip(CircleShape)
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
                text = name,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = countLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
