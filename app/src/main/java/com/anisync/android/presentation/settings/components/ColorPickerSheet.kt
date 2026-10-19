package com.anisync.android.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisync.android.R
import com.anisync.android.presentation.components.AppModalBottomSheet
import com.anisync.android.presentation.components.rememberControlledSliderState
import com.anisync.android.ui.theme.paletteRoles
import com.materialkolor.PaletteStyle
import kotlinx.coroutines.launch

/**
 * Picks the seed for a custom theme, either by hue or by an exact hex code, and shows the roles the
 * app would build from it before anything is applied.
 *
 * The hue slider keeps saturation and lightness fixed so every stop makes a usable theme. A typed or
 * pasted hex is taken as is, which is how an AniList profile color can be matched exactly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerSheet(
    currentColor: Color?,
    isDarkMode: Boolean,
    paletteStyle: PaletteStyle,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    var argb by rememberSaveable { mutableIntStateOf((currentColor ?: Color.hsl(0f, 0.7f, 0.5f)).toArgb()) }
    var hue by rememberSaveable { mutableFloatStateOf(extractHue(Color(argb))) }
    var hexText by rememberSaveable { mutableStateOf(Color(argb).toHex()) }
    val selectedColor = Color(argb)

    fun pickHue(newHue: Float) {
        hue = newHue
        argb = Color.hsl(newHue, 0.7f, 0.5f).toArgb()
        hexText = Color(argb).toHex()
    }

    fun pickHex(text: String) {
        hexText = text
        parseHex(text)?.let {
            argb = it.toArgb()
            hue = extractHue(it)
        }
    }

    val rainbowBrush = remember {
        Brush.horizontalGradient(
            colors = (0..360 step 30).map { h -> Color.hsl(h.toFloat(), 0.7f, 0.5f) }
        )
    }
    val clipboard = LocalClipboard.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.custom_color),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.custom_color_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))

            GeneratedRoles(seed = selectedColor, isDarkMode = isDarkMode, paletteStyle = paletteStyle)

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.hue_slider),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(rainbowBrush),
                contentAlignment = Alignment.Center
            ) {
                Slider(
                    state = rememberControlledSliderState(value = hue, valueRange = 0f..360f),
                    onValueChange = { pickHue(it) },
                    colors = SliderDefaults.colors(
                        thumbColor = selectedColor,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            TextField(
                value = hexText,
                onValueChange = { pickHex(it.take(7)) },
                label = { Text(stringResource(R.string.hex_code)) },
                singleLine = true,
                isError = parseHex(hexText) == null,
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(selectedColor)
                    )
                },
                trailingIcon = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val pasted = clipboard.getClipEntry()?.clipData
                                    ?.takeIf { it.itemCount > 0 }
                                    ?.getItemAt(0)?.text?.toString()?.trim()
                                if (pasted != null) pickHex(pasted.take(7))
                            }
                        }
                    ) { Text(stringResource(R.string.paste)) }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    errorContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onColorSelected(selectedColor)
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.apply))
                }
            }
        }
    }
}

/** The seed beside four of the roles generated from it, on the surface they would sit on. */
@Composable
private fun GeneratedRoles(seed: Color, isDarkMode: Boolean, paletteStyle: PaletteStyle) {
    val colors = remember(seed, isDarkMode, paletteStyle) {
        val roles = paletteRoles(seed, isDarkMode, paletteStyle)
        GeneratedRoleColors(
            surfaceContainer = roles.surfaceContainer(),
            surfaceContainerHigh = roles.surfaceContainerHigh(),
            primary = roles.primary(),
            primaryContainer = roles.primaryContainer(),
            tertiary = roles.tertiary(),
            outlineVariant = roles.outlineVariant(),
            onSurfaceVariant = roles.onSurfaceVariant()
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surfaceContainer)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(seed)
        )
        Row(modifier = Modifier.weight(1f)) {
            listOf(
                R.string.color_role_primary to colors.primary,
                R.string.color_role_container to colors.primaryContainer,
                R.string.color_role_tertiary to colors.tertiary,
                R.string.color_role_surface to colors.surfaceContainerHigh,
            ).forEach { (label, color) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(1.dp, colors.outlineVariant, CircleShape)
                    )
                    Text(
                        text = stringResource(label),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        // A quarter of the row can be narrower than the word, so it shrinks rather than breaks.
                        maxLines = 1,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = 8.sp,
                            maxFontSize = MaterialTheme.typography.labelSmall.fontSize
                        )
                    )
                }
            }
        }
    }
}

@Immutable
private data class GeneratedRoleColors(
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val primary: Color,
    val primaryContainer: Color,
    val tertiary: Color,
    val outlineVariant: Color,
    val onSurfaceVariant: Color
)

private fun Color.toHex(): String = "#%06X".format(toArgb() and 0xFFFFFF)

/** Reads `#RRGGBB` or `RRGGBB`. Anything else is not a color yet. */
private fun parseHex(text: String): Color? {
    val digits = text.trim().removePrefix("#")
    if (digits.length != 6) return null
    return digits.toLongOrNull(16)?.let { Color(0xFF000000 or it) }
}

/**
 * Extract hue (0-360) from a Color.
 */
private fun extractHue(color: Color): Float {
    val r = color.red
    val g = color.green
    val b = color.blue

    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min

    if (delta == 0f) return 0f

    val hue = when (max) {
        r -> 60f * (((g - b) / delta) % 6f)
        g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }

    return if (hue < 0) hue + 360f else hue
}
