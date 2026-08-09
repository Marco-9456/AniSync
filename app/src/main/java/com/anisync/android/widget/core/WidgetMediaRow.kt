package com.anisync.android.widget.core

import android.content.Context
import android.graphics.Bitmap
import android.widget.RemoteViews
import com.anisync.android.R

/**
 * Builds the shared media row.
 *
 * Up Next, Airing Today and the Weekly Calendar all show a series with a time next to it, so they
 * share this instead of each building its own. Only the trailing pill and the bookmark differ.
 */
object WidgetMediaRow {

    fun build(
        context: Context,
        title: String,
        subtitle: String,
        trailing: String,
        mediaId: Int,
        cover: Bitmap?,
        colors: WidgetColors,
        contentDescription: String
    ): RemoteViews = RemoteViews(context.packageName, R.layout.widget_item_media).apply {
        WidgetTheme.card(this, R.id.item_root, colors)
        WidgetTheme.poster(this, R.id.item_cover, colors)
        WidgetTheme.text(this, R.id.item_title, colors.onSurface, colors)
        WidgetTheme.text(this, R.id.item_episode, colors.onSurfaceVariant, colors)
        WidgetTheme.badge(this, R.id.item_time, colors.primaryContainer, colors.onPrimaryContainer, colors)
        setTextViewText(R.id.item_title, title)
        setTextViewText(R.id.item_episode, subtitle)
        setTextViewText(R.id.item_time, trailing)
        cover?.let { setImageViewBitmap(R.id.item_cover, it) }
        setContentDescription(R.id.item_root, contentDescription)
        setOnClickFillInIntent(R.id.item_root, WidgetIntents.openMediaFillIn(mediaId))
    }
}
