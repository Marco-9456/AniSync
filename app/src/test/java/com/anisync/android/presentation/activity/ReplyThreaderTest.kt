package com.anisync.android.presentation.activity

import com.anisync.android.domain.ActivityReply
import com.anisync.android.domain.CommentNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ReplyThreaderTest {

    private var seq = 1_000_000L

    private fun reply(
        id: Int,
        author: String,
        body: String,
        createdAt: Long = seq++
    ) = ActivityReply(
        id = id,
        body = body,
        likeCount = 0,
        isLiked = false,
        authorId = author.hashCode(),
        authorName = author,
        authorAvatarUrl = null,
        createdAt = createdAt
    )

    /** Depth-first lookup of [id]'s parent node, or null when [id] is a root. */
    private fun parentOf(roots: List<CommentNode>, id: Int): CommentNode? {
        fun search(node: CommentNode): CommentNode? {
            for (child in node.childComments) {
                if (child.id == id) return node
                search(child)?.let { return it }
            }
            return null
        }
        return roots.firstNotNullOfOrNull { search(it) }
    }

    private fun findNode(roots: List<CommentNode>, id: Int): CommentNode? {
        fun search(node: CommentNode): CommentNode? {
            if (node.id == id) return node
            for (child in node.childComments) search(child)?.let { return it }
            return null
        }
        return roots.firstNotNullOfOrNull { search(it) }
    }

    /**
     * Real-world repro from activity 1093723421: Krynn replied to both Marco9456 and Hash2k.
     * When Marco9456 then replies to Krynn, the new reply must land under Krynn's reply that was
     * addressed to Marco9456 — not under Krynn's newer reply that sits in Hash2k's branch.
     * Regression guard for the "reply shows under the wrong node" bug.
     */
    @Test
    fun `reply to an author who posted in two branches lands in the branch addressed to the replier`() {
        val marcoRoot = reply(17004339, "Marco9456", "<p>Try upscaling it</p>")
        val hashRoot = reply(17004384, "Hash2k", "<p>if it has bad resolution it's unfixable</p>")
        val krynnToMarco = reply(
            17004404, "Krynn",
            "<p><a href='x'>@Marco9456</a> oh hey ur the guy i think</p><p>does that use generative ai?</p>"
        )
        val krynnToHash = reply(
            17004411, "Krynn",
            "<p><a href='x'>@Hash2k</a> not about my banner BUT my banner is terrible</p>"
        )
        // The reply the user just posted (tapped Reply on Krynn's @Marco9456 message).
        val newMarcoToKrynn = reply(17004500, "Marco9456", "<p>@Krynn yeah it does</p>")

        val roots = ReplyThreader.build(
            listOf(marcoRoot, hashRoot, krynnToMarco, krynnToHash, newMarcoToKrynn)
        )

        // Krynn's two replies sit under the authors they addressed.
        assertEquals(17004339, parentOf(roots, krynnToMarco.id)?.id)
        assertEquals(17004384, parentOf(roots, krynnToHash.id)?.id)

        // The fix: the new reply nests under Krynn's @Marco9456 reply (Marco's branch),
        // NOT under Krynn's @Hash2k reply (Hash2k's branch).
        assertEquals(
            "new reply landed in the wrong branch",
            17004404,
            parentOf(roots, newMarcoToKrynn.id)?.id
        )
    }

    @Test
    fun `mention nests under the mentioned author's latest reply when there is no pairing`() {
        val alice = reply(1, "Alice", "<p>first thought</p>")
        val aliceLater = reply(2, "Alice", "<p>second thought</p>")
        val bob = reply(3, "Bob", "<p>@Alice good point</p>")

        val roots = ReplyThreader.build(listOf(alice, aliceLater, bob))

        // No back-and-forth signal -> fall back to Alice's most recent reply.
        assertEquals(2, parentOf(roots, bob.id)?.id)
    }

    @Test
    fun `reply with no leading mention is a root`() {
        val a = reply(1, "Alice", "<p>hello everyone</p>")
        val b = reply(2, "Bob", "<p>nice weather</p>")

        val roots = ReplyThreader.build(listOf(a, b))

        assertEquals(2, roots.size)
        assertNull(parentOf(roots, a.id))
        assertNull(parentOf(roots, b.id))
    }

    @Test
    fun `mentioning an author who has not yet replied stays a root`() {
        val bob = reply(1, "Bob", "<p>@Ghost are you there?</p>")

        val roots = ReplyThreader.build(listOf(bob))

        assertEquals(1, roots.size)
        assertEquals(bob.id, roots.single().id)
    }

    @Test
    fun `every input reply appears exactly once in the tree`() {
        val replies = listOf(
            reply(1, "A", "<p>root</p>"),
            reply(2, "B", "<p>@A hi</p>"),
            reply(3, "A", "<p>@B back</p>"),
            reply(4, "C", "<p>@A and @B noise</p>")
        )

        val roots = ReplyThreader.build(replies)

        replies.forEach { assertNotNull("missing ${it.id}", findNode(roots, it.id)) }
        val flattened = mutableListOf<Int>()
        fun walk(n: CommentNode) { flattened += n.id; n.childComments.forEach(::walk) }
        roots.forEach(::walk)
        assertEquals(replies.size, flattened.size)
        assertEquals(replies.map { it.id }.toSet(), flattened.toSet())
    }
}
