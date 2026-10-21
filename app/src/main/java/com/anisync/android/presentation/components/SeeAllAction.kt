package com.anisync.android.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.util.bouncyClickable

/**
 * The labelled way into a section's full list: "See all" and a chevron in primary, with a pill
 * press. Screen readers hear which section it opens, since several sit on one screen.
 */
@Composable
fun SeeAllAction(
    sectionTitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    val description = stringResource(R.string.a11y_action_see_all, sectionTitle)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .bouncyClickable(onClick = onClick, clipShape = RoundedCornerShape(50))
            .semantics {
                role = Role.Button
                contentDescription = description
            }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = label ?: stringResource(R.string.a11y_action_see_all_button),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
    }
}
