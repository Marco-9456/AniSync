package com.anisync.android.domain.parser

internal object RichTextPostProcessor {
    fun groupInlineBlocks(blocks: List<RichTextBlock>): List<RichTextBlock> {
        val result = mutableListOf<RichTextBlock>()
        var index = 0

        while (index < blocks.size) {
            val block = blocks[index]
            if (isInlineGroupCandidate(block)) {
                val group = mutableListOf<RichTextBlock>()
                val align = block.align

                while (index < blocks.size && isInlineGroupCandidate(blocks[index]) && blocks[index].align == align) {
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

            result.add(recursivelyGroupChildren(block))
            index++
        }

        return result
    }

    fun extractImageUrls(blocks: List<RichTextBlock>): List<String> {
        val urls = mutableListOf<String>()
        collectImageUrls(blocks, urls)
        return urls
    }

    private fun isInlineGroupCandidate(block: RichTextBlock): Boolean =
        block is RichTextBlock.Text || block is RichTextBlock.Image || block is RichTextBlock.AnilistLink

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
