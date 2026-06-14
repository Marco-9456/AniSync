package com.anisync.android.presentation.activity

import com.anisync.android.domain.ActivityReply
import com.anisync.android.domain.CommentNode
import org.jsoup.Jsoup

/**
 * Emulates threading on AniList's flat reply list by parsing a leading @username mention.
 *
 * A reply starting with "@Alice" nests under one of Alice's earlier replies. When Alice has
 * posted in several branches, we pick her most recent earlier reply that was itself addressed
 * back to this replier (a back-and-forth), so the reply lands in the correct branch instead of
 * always snapping to Alice's newest reply. Falls back to Alice's latest reply when there is no
 * such pairing, and to a root node when the mentioned author hasn't replied yet.
 *
 * AniList stores no parent on replies, so this heuristic also governs the view after a server
 * refresh — there is no explicit reply-target to fall back on.
 */
object ReplyThreader {

    private val mentionRegex = Regex("^@([A-Za-z0-9_]+)")

    fun build(replies: List<ActivityReply>): List<CommentNode> {
        if (replies.isEmpty()) return emptyList()
        val sorted = replies.sortedBy { it.createdAt }
        val byId = sorted.associateBy { it.id }

        // Leading @mention per reply (lowercased, null when absent), parsed once up front.
        val leadingMention = HashMap<Int, String?>(sorted.size)
        for (reply in sorted) {
            leadingMention[reply.id] = extractLeadingMention(reply.body)?.lowercase()
        }

        val parentOf = HashMap<Int, Int>(sorted.size)
        // All earlier replies per lowercased author, accumulated in time order (oldest-first).
        val repliesByAuthor = HashMap<String, MutableList<ActivityReply>>()

        for (reply in sorted) {
            val mention = leadingMention[reply.id]
            if (mention != null) {
                val candidates = repliesByAuthor[mention]
                if (!candidates.isNullOrEmpty()) {
                    val replier = reply.authorName.lowercase()
                    // Prefer the mentioned author's most recent earlier reply that was itself
                    // addressed to this replier (a back-and-forth), so an author who posted in
                    // multiple branches resolves to the right one. Otherwise use their latest.
                    val parent = candidates.lastOrNull { leadingMention[it.id] == replier }
                        ?: candidates.last()
                    if (parent.id != reply.id) {
                        parentOf[reply.id] = parent.id
                    }
                }
            }
            repliesByAuthor.getOrPut(reply.authorName.lowercase()) { mutableListOf() }.add(reply)
        }

        // Accumulate children per parent (preserving time order), then materialize roots.
        val childLists = HashMap<Int, MutableList<ActivityReply>>()
        for (reply in sorted) {
            val parentId = parentOf[reply.id]
            if (parentId != null && byId.containsKey(parentId)) {
                childLists.getOrPut(parentId) { mutableListOf() }.add(reply)
            }
        }

        fun build(reply: ActivityReply): CommentNode {
            val kids = childLists[reply.id].orEmpty().map { build(it) }
            return toNode(reply, kids)
        }

        return sorted
            .filter { parentOf[it.id] == null }
            .map { build(it) }
    }

    private fun toNode(reply: ActivityReply, children: List<CommentNode>) = CommentNode(
        id = reply.id,
        body = reply.body,
        likeCount = reply.likeCount,
        isLiked = reply.isLiked,
        authorId = reply.authorId,
        authorName = reply.authorName,
        authorAvatarUrl = reply.authorAvatarUrl,
        createdAt = reply.createdAt,
        childComments = children
    )

    private fun extractLeadingMention(html: String): String? {
        if (html.isBlank()) return null
        val text = try {
            Jsoup.parse(html).text().trimStart()
        } catch (_: Exception) {
            html.trimStart()
        }
        val match = mentionRegex.find(text) ?: return null
        return match.groupValues[1]
    }
}
