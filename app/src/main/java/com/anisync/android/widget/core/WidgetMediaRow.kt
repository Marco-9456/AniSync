package com.anisync.android.widget.core

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.widget.RemoteViews
import com.anisync.android.R

/**
 * Builds the shared media row.
 *
 * Up Next, Airing Today and the Weekly Calendar all list a series with a time against it, so they
 * build the same row here rather than each assembling its own copy. What differs between them is
 * only what goes in the trailing pill and whether the bookmark applies.
 */
object WidgetMediaRow {

    fun build(
        context: Context,
        title: String,
        subtitle: String,
        trailing: String,
        mediaId: Int,
        cover: Bitmap?,
        onMyList: Boolean = false,
        contentDescription: String
    ): RemoteViews = RemoteViews(context.packageName, R.layout.widget_item_media).apply {
        setTextViewText(R.id.item_title, title)
        setTextViewText(R.id.item_episode, subtitle)
        setTextViewText(R.id.item_time, trailing)
        setViewVisibility(R.id.item_bookmark, if (onMyList) View.VISIBLE else View.GONE)
        cover?.let { setImageViewBitmap(R.id.item_cover, it) }
        setContentDescription(R.id.item_root, contentDescription)
        setOnClickFillInIntent(R.id.item_root, WidgetIntents.openMediaFillIn(mediaId))
    }
}
