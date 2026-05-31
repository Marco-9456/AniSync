package com.anisync.android.domain.parser

internal object RichTextPostProcessor {
    // An absolute width at/above this is treated as a full-width banner/divider rather than a
    // grid thumbnail, so it gets its own row instead of being packed into an image grid.
    private const val FULL_WIDTH_IMAGE_THRESHOLD = 400

    fun groupInlineBlocks(blocks: List<RichTextBlock>): List<RichTextBlock> {
        val result = mutableListOf<RichTextBlock>()
        var index = 0

        while (index < blocks.size) {
            val block = blocks[index]

            // Group contiguous Images to prevent buggy text wrapping, but keep full-width images
            // (banners/dividers) on their own row — packing a wide divider into a FlowRow with small
            // thumbnails (e.g. 20% "Day N" tiles) shoves the grid out of alignment.
            if (block is RichTextBlock.Image) {
                val align = block.align
                val run = mutableListOf<RichTextBlock.Image>()
                while (index < blocks.size && blocks[index] is RichTextBlock.Image && blocks[index].align == align) {
                    run.add(blocks[index] as RichTextBlock.Image)
                    index++
                }

                val pending = mutableListOf<RichTextBlock.Image>()
                fun flushPending() {
                    when (pending.size) {
                        0 -> Unit
                        1 -> result.add(pending.first())
                        else -> result.add(RichTextBlock.InlineGroup(pending.toList(), align))
                    }
                    pending.clear()
                }
                for (img in run) {
                    if (img.isFullWidthImage()) {
                        flushPending()
                        result.add(img)
                    } else {
                        pending.add(img)
                    }
                }
                flushPending()
                continue
            }

            // Merge contiguous lists into one. AniList markup can split a single logical
            // list across blocks — e.g. a stranded markdown bullet followed by a real <ul> —
            // which would otherwise render as two lists with a gap between them.
            if (block is RichTextBlock.ListBlock) {
                val align = block.align
                val mergedItems = mutableListOf<ListItem>()
                while (index < blocks.size &&
                    blocks[index].let { it is RichTextBlock.ListBlock && it.align == align }
                ) {
                    mergedItems.addAll((blocks[index] as RichTextBlock.ListBlock).items)
                    index++
                }
                result.add(recursivelyGroupChildren(RichTextBlock.ListBlock(mergedItems, align)))
                continue
            }

            // Group contiguous AnilistLinks together
            if (block is RichTextBlock.AnilistLink) {
                val group = mutableListOf<RichTextBlock>()
                val align = block.align
                while (index < blocks.size && blocks[index] is RichTextBlock.AnilistLink && blocks[index].align == align) {
                    group.add(blocks[index])
                    index++
                }
                if (group.size > 1) {
                    result.add(RichTextBlock.InlineGroup(group, align))
                } else {
                    result.add(group.first())
                }
                continue
            }

            // Text blocks are NO LONGER grouped here to ensure explicit block-level vertical alignment
            result.add(recursivelyGroupChildren(block))
            index++
        }

        return result
    }

    /**
     * Whether an image should occupy a full row rather than join a thumbnail grid: a percent width
     * near 100, no explicit width (renders fillMaxWidth), or a large absolute width.
     */
    private fun RichTextBlock.Image.isFullWidthImage(): Boolean = when {
        isPercent -> (width ?: 100) >= 90
        width == null -> true
        else -> width >= FULL_WIDTH_IMAGE_THRESHOLD
    }

    fun extractImageUrls(blocks: List<RichTextBlock>): List<String> {
        val urls = mutableListOf<String>()
        collectImageUrls(blocks, urls)
        return urls
    }

    private fun recursivelyGroupChildren(block: RichTextBlock): RichTextBlock = when (block) {
        is RichTextBlock.Spoiler -> block.copy(children = groupInlineBlocks(block.children))
        is RichTextBlock.Blockquote -> block.copy(children = groupInlineBlocks(block.children))
        is RichTextBlock.Table -> block.copy(
            rows = block.rows.map { row ->
                TableRow(
                    row.cells.map { cell ->
                        cell.copy(children = groupInlineBlocks(cell.children))
                    }
                )
            }
        )

        is RichTextBlock.ListBlock -> block.copy(
            items = block.items.map { item ->
                ListItem(
                    children = groupInlineBlocks(item.children),
                    bullet = item.bullet
                )
            }
        )

        is RichTextBlock.InlineGroup -> block.copy(
            children = groupInlineBlocks(block.children)
        )

        else -> block
    }

    private fun collectImageUrls(blocks: List<RichTextBlock>, output: MutableList<String>) {
        for (block in blocks) {
            when (block) {
                is RichTextBlock.Image -> output.add(block.url)
                is RichTextBlock.InlineGroup -> {
                    for (child in block.children) {
                        if (child is RichTextBlock.Image) {
                            output.add(child.url)
                        }
                    }
                }

                is RichTextBlock.Spoiler -> collectImageUrls(block.children, output)
                is RichTextBlock.Blockquote -> collectImageUrls(block.children, output)
                is RichTextBlock.ListBlock -> {
                    for (item in block.items) {
                        collectImageUrls(item.children, output)
                    }
                }

                is RichTextBlock.Table -> {
                    for (row in block.rows) {
                        for (cell in row.cells) {
                            collectImageUrls(cell.children, output)
                        }
                    }
                }

                else -> Unit
            }
        }
    }
}