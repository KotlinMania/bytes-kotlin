// port-lint: source tests/test_bytes.rs (BytesMut portions) + src/bytes_mut.rs inline tests
package io.github.kotlinmania.bytes

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BytesMutTest {
    @Test
    fun withCapacityAllocatesRequestedSpace() {
        val buf = BytesMut.withCapacity(64)
        assertEquals(0, buf.len())
        assertEquals(64, buf.capacity())
        assertTrue(buf.isEmpty())
    }

    @Test
    fun newIsEmptyAndZeroCapacity() {
        val buf = BytesMut.new()
        assertEquals(0, buf.len())
        assertEquals(0, buf.capacity())
        assertTrue(buf.isEmpty())
    }

    @Test
    fun zeroedReturnsZeroFilledBuffer() {
        val buf = BytesMut.zeroed(5)
        assertEquals(5, buf.len())
        assertEquals(5, buf.capacity())
        assertContentEquals(ByteArray(5), buf.asRef())
    }

    @Test
    fun fromCopiesSliceContents() {
        val src = "hello".encodeToByteArray()
        val buf = BytesMut.from(src)
        assertEquals(5, buf.len())
        assertContentEquals(src, buf.asRef())
        // Mutate the source and check that the BytesMut copy is independent.
        src[0] = 'X'.code.toByte()
        assertEquals('h'.code.toByte(), buf.asRef()[0])
    }

    @Test
    fun fromStringEncodesUtf8() {
        val buf = BytesMut.from("hello world")
        assertContentEquals("hello world".encodeToByteArray(), buf.asRef())
    }

    @Test
    fun freezeReturnsImmutableBytesWithSameContents() {
        val mut = BytesMut.from("hello")
        val frozen = mut.freeze()
        assertContentEquals("hello".encodeToByteArray(), frozen.asSlice())
    }

    @Test
    fun splitOffYieldsTrailingPortionAsCapacity() {
        val buf = BytesMut.from("hello world")
        val tail = buf.splitOff(buf.len())
        // Length stays at original; tail captures whatever spare capacity exists past length.
        assertEquals(11, buf.len())
        assertEquals(0, tail.len())
    }

    @Test
    fun splitDrainsCurrentLengthIntoNewBuffer() {
        val buf = BytesMut.from("hello world")
        val taken = buf.split()
        assertEquals(0, buf.len())
        assertEquals(11, taken.len())
        assertContentEquals("hello world".encodeToByteArray(), taken.asRef())
    }

    @Test
    fun splitToYieldsLeadingPortion() {
        val buf = BytesMut.from("hello world")
        val head = buf.splitTo(5)
        assertContentEquals("hello".encodeToByteArray(), head.asRef())
        assertContentEquals(" world".encodeToByteArray(), buf.asRef())
    }

    @Test
    fun splitToOutOfBoundsPanics() {
        val buf = BytesMut.from("hi")
        assertFails { buf.splitTo(10) }
    }

    @Test
    fun truncateShortensBuffer() {
        val buf = BytesMut.from("hello world")
        buf.truncate(5)
        assertEquals(5, buf.len())
        assertContentEquals("hello".encodeToByteArray(), buf.asRef())
    }

    @Test
    fun truncateLongerLengthIsNoOp() {
        val buf = BytesMut.from("hello")
        buf.truncate(100)
        assertEquals(5, buf.len())
        assertContentEquals("hello".encodeToByteArray(), buf.asRef())
    }

    @Test
    fun clearEmptiesPreservingCapacity() {
        val buf = BytesMut.from("hello world")
        val originalCapacity = buf.capacity()
        buf.clear()
        assertEquals(0, buf.len())
        assertTrue(buf.isEmpty())
        assertTrue(buf.capacity() >= originalCapacity)
    }

    @Test
    fun resizeExtendsWithFillByte() {
        val buf = BytesMut.from("hi")
        buf.resize(5, 'X'.code.toByte())
        assertEquals(5, buf.len())
        assertContentEquals("hiXXX".encodeToByteArray(), buf.asRef())
    }

    @Test
    fun resizeShrinksWithoutZeroing() {
        val buf = BytesMut.from("hello world")
        buf.resize(5, 0.toByte())
        assertEquals(5, buf.len())
        assertContentEquals("hello".encodeToByteArray(), buf.asRef())
    }

    @Test
    fun setLenWithinCapacitySucceeds() {
        val buf = BytesMut.withCapacity(16)
        // Manually fill bytes through the underlying storage exposed by spareCapacityMut.
        val spare = buf.spareCapacityMut()
        for (i in 0 until 4) {
            spare.writeByte(i, 'A'.code.toByte())
        }
        buf.setLen(4)
        assertEquals(4, buf.len())
        assertContentEquals("AAAA".encodeToByteArray(), buf.asRef())
    }

    @Test
    fun setLenBeyondCapacityPanics() {
        val buf = BytesMut.withCapacity(4)
        assertFails { buf.setLen(8) }
    }

    @Test
    fun reserveGrowsBackingStorage() {
        val buf = BytesMut.from("hi")
        val originalCapacity = buf.capacity()
        buf.reserve(100)
        assertTrue(buf.capacity() >= 102)
        // Existing contents preserved.
        assertContentEquals("hi".encodeToByteArray(), buf.asRef())
        assertNotEquals(originalCapacity, buf.capacity())
    }

    @Test
    fun tryReclaimSucceedsWhenAlreadyAvailable() {
        val buf = BytesMut.withCapacity(16)
        assertTrue(buf.tryReclaim(8))
    }

    @Test
    fun extendFromSliceAppendsBytes() {
        val buf = BytesMut.from("hello")
        buf.extendFromSlice(" world".encodeToByteArray())
        assertContentEquals("hello world".encodeToByteArray(), buf.asRef())
    }

    @Test
    fun unsplitConcatenatesBuffers() {
        val a = BytesMut.from("hello ")
        val b = BytesMut.from("world")
        a.unsplit(b)
        assertContentEquals("hello world".encodeToByteArray(), a.asRef())
    }

    @Test
    fun spareCapacityMutExposesUninitTail() {
        val buf = BytesMut.withCapacity(16)
        buf.extendFromSlice("hi".encodeToByteArray())
        val spare = buf.spareCapacityMut()
        assertTrue(spare.len() >= 14)
    }

    // === Buf interface conformance ===

    @Test
    fun bufRemainingMatchesLen() {
        val buf = BytesMut.from("hello world")
        assertEquals(11, buf.remaining())
        assertTrue(buf.hasRemaining())
    }

    @Test
    fun bufChunkReturnsVisibleBytes() {
        val buf = BytesMut.from("hello world")
        assertContentEquals("hello world".encodeToByteArray(), buf.chunk())
    }

    @Test
    fun bufAdvanceMovesCursorForward() {
        val buf = BytesMut.from("hello world")
        buf.advance(6)
        assertEquals(5, buf.remaining())
        assertContentEquals("world".encodeToByteArray(), buf.chunk())
    }

    @Test
    fun bufAdvancePastRemainingPanics() {
        val buf = BytesMut.from("hi")
        assertFails { buf.advance(100) }
    }

    @Test
    fun bufCopyToBytesYieldsImmutableSlice() {
        val buf = BytesMut.from("hello world")
        val taken = buf.copyToBytes(5)
        assertContentEquals("hello".encodeToByteArray(), taken.asSlice())
        assertContentEquals(" world".encodeToByteArray(), buf.asRef())
    }

    @Test
    fun bufGetU8AdvancesCursor() {
        val buf = BytesMut.from("hello")
        assertEquals('h'.code.toUByte(), buf.getU8())
        assertEquals(4, buf.remaining())
    }

    @Test
    fun bufGetU16BigEndian() {
        val buf = BytesMut.from(byteArrayOf(0x12, 0x34, 0x56))
        assertEquals(0x1234u.toUShort(), buf.getU16())
        assertEquals(1, buf.remaining())
    }

    // === BufMut interface conformance ===

    @Test
    fun bufMutPutSliceAppends() {
        val buf = BytesMut.new()
        buf.putSlice("hello".encodeToByteArray())
        buf.putSlice(" world".encodeToByteArray())
        assertContentEquals("hello world".encodeToByteArray(), buf.asRef())
    }

    @Test
    fun bufMutPutBytesFillsRange() {
        val buf = BytesMut.new()
        buf.putBytes('A'.code.toByte(), 4)
        assertContentEquals("AAAA".encodeToByteArray(), buf.asRef())
    }

    @Test
    fun bufMutPutU8Appends() {
        val buf = BytesMut.new()
        buf.putU8('a'.code.toUByte())
        buf.putU8('b'.code.toUByte())
        assertContentEquals("ab".encodeToByteArray(), buf.asRef())
    }

    @Test
    fun bufMutPutU16BigEndian() {
        val buf = BytesMut.new()
        buf.putU16(0x1234u)
        assertContentEquals(byteArrayOf(0x12, 0x34), buf.asRef())
    }

    @Test
    fun bufMutPutTransfersFromBuf() {
        val src = BytesMut.from("source")
        val dst = BytesMut.new()
        dst.put(src)
        assertContentEquals("source".encodeToByteArray(), dst.asRef())
    }

    @Test
    fun bufMutChunkMutGrowsOnDemand() {
        val buf = BytesMut.new()
        val chunk = buf.chunkMut()
        assertTrue(chunk.len() >= 64)
    }

    // === Equality / hashing / compareTo ===

    @Test
    fun equalContentsAreEqual() {
        val a = BytesMut.from("hello")
        val b = BytesMut.from("hello")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun differentContentsAreNotEqual() {
        val a = BytesMut.from("hello")
        val b = BytesMut.from("world")
        assertNotEquals(a, b)
    }

    @Test
    fun compareToMatchesByteArrayOrdering() {
        val a = BytesMut.from("apple")
        val b = BytesMut.from("banana")
        assertTrue(a < b)
        assertTrue(b > a)
        assertTrue(a == BytesMut.from("apple"))
    }

    // === Cloning ===

    @Test
    fun cloneIsIndependent() {
        val a = BytesMut.from("hello")
        val b = a.clone()
        a.putSlice(" world".encodeToByteArray())
        // After mutating a, b should not be affected.
        assertContentEquals("hello world".encodeToByteArray(), a.asRef())
        assertContentEquals("hello".encodeToByteArray(), b.asRef())
    }

    // === Iteration ===

    @Test
    fun intoIterYieldsAllBytes() {
        val buf = BytesMut.from("abc")
        val collected = mutableListOf<Byte>()
        val iter = buf.intoIter()
        while (iter.hasNext()) {
            collected.add(iter.next())
        }
        assertContentEquals("abc".encodeToByteArray(), collected.toByteArray())
    }

    // === Extend ===

    @Test
    fun extendFromIterAppends() {
        val buf = BytesMut.from("ab")
        buf.extend(listOf('c'.code.toByte(), 'd'.code.toByte()))
        assertContentEquals("abcd".encodeToByteArray(), buf.asRef())
    }

    // === fmt::Write integration ===

    @Test
    fun writeStrAppendsUtf8() {
        val buf = BytesMut.new()
        val result = buf.writeStr("hello world")
        assertTrue(result.isSuccess)
        assertContentEquals("hello world".encodeToByteArray(), buf.asRef())
    }

    // === bytesMutReuse: tests upstream `test_bytes_mut_reuse` ===

    @Test
    fun bytesMutReuseEmptyAndPopulated() {
        val empty = BytesMut.new()
        empty.put(BytesMut.from(byteArrayOf()))
        val populated = BytesMut.new()
        populated.put(BytesMut.from(byteArrayOf(1, 2, 3)))
        assertContentEquals(byteArrayOf(1, 2, 3), populated.asRef())
    }
}
