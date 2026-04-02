package com.anisync.android.domain

import com.anisync.android.domain.parser.RichTextBlock

data class LinkPreview(
    val title: String,
    val imageUrl: String?
)

interface LinkPreviewProvider {
    suspend fun getPreviews(links: List<RichTextBlock.AnilistLink>): Map<Int, LinkPreview>
}
