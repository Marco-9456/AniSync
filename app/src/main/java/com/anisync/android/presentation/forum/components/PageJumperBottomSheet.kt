package com.anisync.android.presentation.forum.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.components.AppModalBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageJumperBottomSheet(
    currentPage: Int,
    lastPage: Int,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onJumpTo: (Int) -> Unit,
    onJumpFirst: () -> Unit,
    onJumpLatest: () -> Unit,
) {
    val safeLast = lastPage.coerceAtLeast(1)
    val safeCurrent = currentPage.coerceIn(1, safeLast)

    var sliderPage by remember(safeCurrent, safeLast) {
        mutableFloatStateOf(safeCurrent.toFloat())
    }
    val pickedPage = sliderPage.toInt().coerceIn(1, safeLast)

    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(
            topStart = 32.dp,
            topEnd = 32.dp
        ) // MD3 Expressive Bottom Sheet Arc
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.forum_jump_to_page),
                style = MaterialTheme.typography.headlineSmall, // Expressive Header
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.forum_page_of, pickedPage, safeLast),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(24.dp))

            Slider(
                value = sliderPage,
                onValueChange = { sliderPage = it },
                valueRange = 1f..safeLast.toFloat(),
                steps = (safeLast - 2).coerceAtLeast(0),
                enabled = safeLast > 1,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "1",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = safeLast.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onJumpFirst,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        stringResource(R.string.forum_first_page),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                FilledTonalButton(
                    onClick = onJumpLatest,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        stringResource(R.string.forum_latest_page),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { onJumpTo(pickedPage) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp), // Tall, expressive prominent button
                enabled = pickedPage != safeCurrent,
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = stringResource(R.string.forum_jump),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(16.dp)) // Extra padding beneath for breathing room
        }
    }
}
