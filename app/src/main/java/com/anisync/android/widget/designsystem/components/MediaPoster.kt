package com.anisync.android.widget.designsystem.components

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.width
import com.anisync.android.widget.core.SizeClass
import com.anisync.android.widget.designsystem.tokens.WidgetDimensions

/**
 * A media poster image container with rounded corners and placeholder.
 *
 * @param bitmap The poster image bitmap (nullable for placeholder)
 * @param width Width of the poster
 * @param height Height of the poster
 * @param modifier Optional GlanceModifier
 * @param cornerRadius Corner radius for the poster
 */
@Composable
fun MediaPoster(
    bitmap: Bitmap?,
    width: Dp,
    height: Dp,
    modifier: GlanceModifier = GlanceModifier,
    cornerRadius: Dp = WidgetDimensions.cornerRadiusSmall
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .cornerRadius(cornerRadius)
            .background(GlanceTheme.colors.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        // When bitmap is null, the surfaceVariant background acts as placeholder
    }
}

/**
 * Creates a poster sized appropriately for the given SizeClass.
 */
@Composable
fun MediaPosterForSize(
    bitmap: Bitmap?,
    sizeClass: SizeClass,
    modifier: GlanceModifier = GlanceModifier
) {
    val (width, height) = when (sizeClass) {
        SizeClass.COMPACT -> WidgetDimensions.Poster.widthCompact to WidgetDimensions.Poster.heightCompact
        SizeClass.MEDIUM -> WidgetDimensions.Poster.widthMedium to WidgetDimensions.Poster.heightMedium
        SizeClass.EXPANDED -> WidgetDimensions.Poster.widthExpanded to WidgetDimensions.Poster.heightExpanded
    }

    MediaPoster(
        bitmap = bitmap,
        width = width,
        height = height,
        modifier = modifier
    )
}
