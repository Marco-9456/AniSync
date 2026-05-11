package com.anisync.android.presentation.components

import com.anisync.android.ui.theme.emphasis
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A standard info card used to display a metric or attribute with an icon.
 * Used in Details screens (e.g. Media Details).
 */
@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    iconTint: Color,
    iconContentDescription: String? = null,
    isStatus: Boolean = false
) {
    InfoCardContent(
        modifier = modifier,
        label = label,
        value = value,
        iconTint = iconTint,
        isStatus = isStatus
    ) {
        Icon(
            imageVector = icon, 
            contentDescription = iconContentDescription,
            tint = iconTint, 
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * InfoCard variant that accepts a drawable resource ID for the icon.
 */
@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    @DrawableRes iconResId: Int,
    label: String,
    value: String,
    iconTint: Color,
    iconContentDescription: String? = null,
    isStatus: Boolean = false
) {
    InfoCardContent(
        modifier = modifier,
        label = label,
        value = value,
        iconTint = iconTint,
        isStatus = isStatus
    ) {
        Icon(
            painter = painterResource(id = iconResId), 
            contentDescription = iconContentDescription,
            tint = iconTint, 
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun InfoCardContent(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    iconTint: Color,
    isStatus: Boolean = false,
    iconContent: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
         Row(
             modifier = Modifier.padding(12.dp),
             verticalAlignment = Alignment.CenterVertically,
             horizontalArrangement = Arrangement.spacedBy(12.dp)
         ) {
             Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                         if (isStatus) iconTint.copy(alpha=0.2f) else iconTint.copy(alpha=0.1f), 
                         CircleShape
                    ),
                contentAlignment = Alignment.Center
             ) {
                 iconContent()
             }
             
             Column {
                 Text(
                     text = label, 
                     style = MaterialTheme.typography.labelSmall, 
                     color = MaterialTheme.colorScheme.onSurfaceVariant
                 )
                 Text(
                     text = value, 
                     style = MaterialTheme.typography.labelLarge.emphasis(),
                     maxLines = 1,
                     overflow = TextOverflow.Ellipsis
                 )
             }
         }
    }
}

@Preview
@Composable
private fun InfoCardPreview() {
    MaterialTheme {
        InfoCard(
            icon = Icons.Default.Info,
            label = "Format",
            value = "TV Show",
            iconTint = MaterialTheme.colorScheme.primary
        )
    }
}
