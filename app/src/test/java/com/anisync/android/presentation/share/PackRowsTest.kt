package com.anisync.android.presentation.share

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer

class PackRowsTest {

    private fun ByteBuffer.bytes() = ByteArray(remaining()).also { get(it) }

    /** Rows of [rowBytes] whose bytes count up from 1, each followed by [padding] bytes of 0xFF. */
    private fun paddedRows(rowBytes: Int, padding: Int, rows: Int, padLastRow: Boolean = true): ByteBuffer {
        val out = ByteBuffer.allocate((rowBytes + padding) * rows - if (padLastRow) 0 else padding)
        var value = 1
        repeat(rows) { row ->
            repeat(rowBytes) { out.put((value++).toByte()) }
            if (padLastRow || row < rows - 1) repeat(padding) { out.put(0xFF.toByte()) }
        }
        out.flip()
        return out
    }

    private fun expected(rowBytes: Int, rows: Int) = ByteArray(rowBytes * rows) { (it + 1).toByte() }

    @Test
    fun `padding after each row is dropped`() {
        val packed = paddedRows(rowBytes = 12, padding = 4, rows = 3).packRows(12, 16, 3)
        assertArrayEquals(expected(12, 3), packed.bytes())
    }

    @Test
    fun `last row without padding still reads`() {
        val packed = paddedRows(rowBytes = 12, padding = 4, rows = 3, padLastRow = false).packRows(12, 16, 3)
        assertArrayEquals(expected(12, 3), packed.bytes())
    }

    @Test
    fun `tightly packed buffer passes through`() {
        val source = paddedRows(rowBytes = 12, padding = 0, rows = 3)
        assertArrayEquals(expected(12, 3), source.packRows(12, 12, 3).bytes())
    }

    @Test
    fun `source position is left alone`() {
        val source = paddedRows(rowBytes = 12, padding = 4, rows = 2)
        source.packRows(12, 16, 2)
        assertEquals(0, source.position())
    }

    @Test
    fun `export width on a mali stride keeps every row aligned`() {
        val rowBytes = 1080 * 4
        val stride = 1088 * 4
        val rows = 5
        val source = ByteBuffer.allocate(stride * rows)
        repeat(rows) { row ->
            source.put(row * stride, row.toByte())
            source.put(row * stride + rowBytes - 1, row.toByte())
        }
        val packed = source.packRows(rowBytes, stride, rows).bytes()
        repeat(rows) { row ->
            assertEquals(row.toByte(), packed[row * rowBytes])
            assertEquals(row.toByte(), packed[row * rowBytes + rowBytes - 1])
        }
    }
}
