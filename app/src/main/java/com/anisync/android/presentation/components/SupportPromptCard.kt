package com.anisync.android.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anisync.android.BuildConfig
import com.anisync.android.R
import com.anisync.android.ui.theme.ExpressiveShapes

/** Wide enough for two lines of body text on a phone, short of filling a tablet. */
private val MaxWidth = 460.dp

/**
 * The tip nudge that floats over the app on the first launch after an update: the one moment the
 * app has earned the right to ask. It sits above the navigation bar rather than in a screen, so it
 * reaches whichever tab the launch opened on, and it stays until it is answered. [bottomInset] is
 * what the host has to clear beneath it: the navigation bar on a phone, the system inset on a rail.
 *
 * Both answers are final for this version. "Buy a coffee" opens the Donate screen and dismissing
 * marks the build seen, so neither one leaves the card waiting to come back on the next launch.
 */
@Composable
fun SupportPromptCard(
    visible: Boolean,
    onDonate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .padding(bottom = bottomInset)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .widthIn(max = MaxWidth)
            ) {
                Column(modifier = Modifier.padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_kofi),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            // Without the flavor suffix: a preview or debug build asks about
                            // "3.2.0", not "3.2.0-preview".
                            text = stringResource(
                                R.string.support_card_title,
                                BuildConfig.VERSION_NAME.substringBefore('-')
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.cd_support_card_close),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = stringResource(R.string.support_card_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss, shape = ExpressiveShapes.pill) {
                            Text(
                                text = stringResource(R.string.support_card_dismiss),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        FilledTonalButton(onClick = onDonate, shape = ExpressiveShapes.pill) {
                            Icon(
                                painter = painterResource(R.drawable.ic_kofi),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.sponsors_become_button),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
