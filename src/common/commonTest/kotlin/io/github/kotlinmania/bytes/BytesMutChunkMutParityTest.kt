package io.github.kotlinmania.bytes

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BytesMutChunkMutParityTest {
    @Test
    fun withCapacitySmallExposesExactSpareAndRepeatedChunkMutNeitherGrowsNorChangesLen() {
        val buf = BytesMut.withCapacity(16)
        assertEquals(16, buf.capacity())
        assertEquals(0, buf.len())

        val first = buf.chunkMut()
        assertEquals(16, first.len())
        assertEquals(16, buf.capacity())
        assertEquals(0, buf.len())

        val second = buf.chunkMut()
        assertEquals(16, second.len())
        assertEquals(16, buf.capacity())
        assertEquals(0, buf.len())
    }

    @Test
    fun partiallyFilledSub64SpareRetainsCapacityAndCommitsWritesAfterAdvanceMut() {
        val buf = BytesMut.withCapacity(20)
        buf.putSlice("hello".encodeToByteArray())
        assertEquals(20, buf.capacity())
        assertEquals(5, buf.len())

        val spare = buf.chunkMut()
        assertEquals(15, spare.len())
        assertEquals(20, buf.capacity())
        assertEquals(5, buf.len())
        assertContentEquals("hello".encodeToByteArray(), buf.asRef())

        spare[0 until 6].copyFromSlice(" world".encodeToByteArray())
        spare.writeByte(6, '!'.code.toByte())
        assertEquals(5, buf.len())
        assertContentEquals("hello".encodeToByteArray(), buf.asRef())

        buf.advanceMut(7)
        assertEquals(12, buf.len())
        assertEquals(20, buf.capacity())
        assertContentEquals("hello world!".encodeToByteArray(), buf.asRef())
        assertEquals(8, buf.chunkMut().len())
        assertEquals(20, buf.capacity())
    }

    @Test
    fun advancedAndSplitBufferWithNonzeroStartReturnsActualTailSpare() {
        val buf = BytesMut.withCapacity(24)
        buf.putSlice("0123456789".encodeToByteArray())
        buf.advance(4)
        val head = buf.splitTo(2)

        assertContentEquals("45".encodeToByteArray(), head.asRef())
        assertEquals(18, buf.capacity())
        assertEquals(4, buf.len())
        assertContentEquals("6789".encodeToByteArray(), buf.asRef())

        val tailSpare = buf.chunkMut()
        assertEquals(14, tailSpare.len())
        assertEquals(18, buf.capacity())
        assertEquals(4, buf.len())

        tailSpare[0 until 4].copyFromSlice("ABCD".encodeToByteArray())
        buf.advanceMut(4)
        assertEquals(8, buf.len())
        assertEquals(18, buf.capacity())
        assertContentEquals("6789ABCD".encodeToByteArray(), buf.asRef())
        assertEquals(10, buf.chunkMut().len())
    }

    @Test
    fun exhaustedDefaultAndZeroedBuffersGrowAtLeast64WithoutLosingContent() {
        val defaultBuf = BytesMut.default()
        assertEquals(0, defaultBuf.capacity())
        assertEquals(0, defaultBuf.len())
        val defaultChunk = defaultBuf.chunkMut()
        assertTrue(defaultChunk.len() >= 64)
        assertTrue(defaultBuf.capacity() >= 64)
        assertEquals(0, defaultBuf.len())

        val zeroed = BytesMut.zeroed(10)
        assertEquals(10, zeroed.capacity())
        assertEquals(10, zeroed.len())
        val zeroedChunk = zeroed.chunkMut()
        assertTrue(zeroedChunk.len() >= 64)
        assertTrue(zeroed.capacity() >= 74)
        assertEquals(10, zeroed.len())
        assertContentEquals(ByteArray(10), zeroed.asRef())

        val exhausted = BytesMut.withCapacity(8)
        exhausted.putSlice("abcdefgh".encodeToByteArray())
        assertEquals(8, exhausted.capacity())
        assertEquals(8, exhausted.len())
        val grownChunk = exhausted.chunkMut()
        assertTrue(grownChunk.len() >= 64)
        assertTrue(exhausted.capacity() >= 72)
        assertEquals(8, exhausted.len())
        assertContentEquals("abcdefgh".encodeToByteArray(), exhausted.asRef())

        grownChunk.writeByte(0, '!'.code.toByte())
        exhausted.advanceMut(1)
        assertContentEquals("abcdefgh!".encodeToByteArray(), exhausted.asRef())
    }

    @Test
    fun bufMutNumericWritersAndLimitAdapterUseSub64SpareAndGrowAcrossBoundary() {
        val buf = BytesMut.withCapacity(12)
        buf.putU32(0x01020304u)
        buf.putI16Le(0x0506)
        assertEquals(6, buf.len())
        assertEquals(12, buf.capacity())

        val limited = buf.limit(10)
        assertEquals(6, limited.chunkMut().len())
        assertEquals(12, buf.capacity())

        val written = limited.writer().write(byteArrayOf(0x07, 0x08, 0x09, 0x0a))
        assertEquals(4, written)
        assertEquals(10, buf.len())
        assertEquals(12, buf.capacity())
        assertEquals(2, limited.chunkMut().len())

        buf.putU32(0x0b0c0d0eu)
        assertEquals(14, buf.len())
        assertTrue(buf.capacity() >= buf.len())
        assertContentEquals(
            byteArrayOf(1, 2, 3, 4, 6, 5, 7, 8, 9, 10, 11, 12, 13, 14),
            buf.asRef(),
        )
    }
}
