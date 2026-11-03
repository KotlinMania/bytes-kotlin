package io.github.kotlinmania.bytes.buf

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class BufAdapterEmptyDispatchTest {
    private class ReadBuf(
        private val limit: Int,
        private val failure: Throwable? = null,
    ) : Buf {
        private val bytes = byteArrayOf(1, 2, 3)
        var position = 0
            private set
        val copies = mutableListOf<Int>()

        override fun remaining(): Int = limit - position

        override fun chunk(): ByteArray = bytes.copyOfRange(position, limit)

        override fun advance(cnt: Int) {
            check(cnt <= remaining())
            position += cnt
        }

        override fun copyToSlice(dst: ByteArray) {
            copies.add(dst.size)
            failure?.let { throw it }
            check(dst.size <= remaining())
            bytes.copyInto(dst, startIndex = position, endIndex = position + dst.size)
            position += dst.size
        }
    }

    private class WriteBuf(
        private val limit: Int,
        private val failure: Throwable? = null,
    ) : BufMut {
        val bytes = ByteArray(4) { 0xa5.toByte() }
        var position = 0
            private set
        val puts = mutableListOf<Int>()

        override fun remainingMut(): Int = limit - position

        override fun chunkMut(): UninitSlice = UninitSlice.rangeRef(bytes, position, limit)

        override fun advanceMut(cnt: Int) {
            check(cnt <= remainingMut())
            position += cnt
        }

        override fun putSlice(src: ByteArray) {
            puts.add(src.size)
            failure?.let { throw it }
            check(src.size <= remainingMut())
            src.copyInto(bytes, destinationOffset = position)
            position += src.size
        }
    }

    @Test
    fun emptyReaderDestinationStillCallsCopy() {
        val buf = ReadBuf(3)
        assertEquals(0, buf.reader().read(ByteArray(0)))
        assertEquals(listOf(0), buf.copies)
        assertEquals(0, buf.position)
    }

    @Test
    fun readerAtEofStillCallsCopyWithoutChangingDestination() {
        val buf = ReadBuf(0)
        val dst = ByteArray(4) { 0xa5.toByte() }
        assertEquals(0, buf.reader().read(dst))
        assertEquals(listOf(0), buf.copies)
        assertContentEquals(ByteArray(4) { 0xa5.toByte() }, dst)
        assertEquals(0, buf.position)
    }

    @Test
    fun emptyReaderCopyPanicsPropagate() {
        val marker = IllegalStateException("copy marker")
        val buf = ReadBuf(0, marker)
        assertSame(marker, assertFailsWith<IllegalStateException> { buf.reader().read(ByteArray(1)) })
        assertEquals(listOf(0), buf.copies)
        assertEquals(0, buf.position)
    }

    @Test
    fun zeroCapacityWriterStillCallsPut() {
        val buf = WriteBuf(0)
        assertEquals(0, buf.writer().write(byteArrayOf(1, 2)))
        assertEquals(listOf(0), buf.puts)
        assertEquals(0, buf.position)
    }

    @Test
    fun emptyWriterInputStillCallsPut() {
        val buf = WriteBuf(3)
        assertEquals(0, buf.writer().write(ByteArray(0)))
        assertEquals(listOf(0), buf.puts)
        assertEquals(0, buf.position)
    }

    @Test
    fun emptyWriterPutPanicsPropagate() {
        val marker = IllegalStateException("put marker")
        val buf = WriteBuf(0, marker)
        assertSame(marker, assertFailsWith<IllegalStateException> { buf.writer().write(byteArrayOf(1)) })
        assertEquals(listOf(0), buf.puts)
        assertEquals(0, buf.position)
    }

    @Test
    fun partialReaderWritesOnlyTheAvailablePrefix() {
        val buf = ReadBuf(3)
        val dst = ByteArray(5) { 0xa5.toByte() }
        assertEquals(3, buf.reader().read(dst))
        assertEquals(listOf(3), buf.copies)
        assertEquals(3, buf.position)
        assertContentEquals(byteArrayOf(1, 2, 3, 0xa5.toByte(), 0xa5.toByte()), dst)
    }

    @Test
    fun partialWriterUsesOnlyTheAvailableCapacity() {
        val buf = WriteBuf(3)
        assertEquals(3, buf.writer().write(byteArrayOf(1, 2, 3, 4, 5)))
        assertEquals(listOf(3), buf.puts)
        assertEquals(3, buf.position)
        assertContentEquals(byteArrayOf(1, 2, 3, 0xa5.toByte()), buf.bytes)
    }
}
