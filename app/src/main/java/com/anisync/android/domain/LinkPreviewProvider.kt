package com.anisync.android.domain

import com.anisync.android.domain.parser.RichTextBlock
import com.anisync.android.domain.parser.LinkPreviewKey

data class LinkPreview(
    val title: String,
    val imageUrl: String?
)

interface LinkPreviewProvider {
    suspend fun getPreviews(links: List<RichTextBlock.AnilistLink>): Map<LinkPreviewKey, LinkPreview>
}
