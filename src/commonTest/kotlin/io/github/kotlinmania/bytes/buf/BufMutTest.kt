// port-lint: source tests/test_buf_mut.rs
package io.github.kotlinmania.bytes.buf

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails

class BufMutTest {
    @Test
    fun testPutU8() {
        val storage = ByteArray(8)
        val buf = ByteArrayBufMut(storage)
        buf.putU8(33u)
        assertEquals(0x21.toByte(), storage[0])
    }

    @Test
    fun testPutU16() {
        val storage = ByteArray(8)
        val buf = ByteArrayBufMut(storage)
        buf.putU16(8532u)
        assertEquals(0x21.toByte(), storage[0])
        assertEquals(0x54.toByte(), storage[1])
    }

    @Test
    fun testPutU16Le() {
        val storage = ByteArray(8)
        val buf = ByteArrayBufMut(storage)
        buf.putU16Le(8532u)
        assertEquals(0x54.toByte(), storage[0])
        assertEquals(0x21.toByte(), storage[1])
    }

    @Test
    fun testPutInt() {
        val storage = ByteArray(8)
        val buf = ByteArrayBufMut(storage)
        buf.putInt(0x1020304050607080L, 3)
        assertEquals(0x60.toByte(), storage[0])
        assertEquals(0x70.toByte(), storage[1])
        assertEquals(0x80.toByte(), storage[2])
    }

    @Test
    fun testPutIntNbytesOverflow() {
        val storage = ByteArray(8)
        val buf = ByteArrayBufMut(storage)
        assertFails { buf.putInt(0x1020304050607080L, 9) }
    }

    @Test
    fun testPutIntLe() {
        val storage = ByteArray(8)
        val buf = ByteArrayBufMut(storage)
        buf.putIntLe(0x1020304050607080L, 3)
        assertEquals(0x80.toByte(), storage[0])
        assertEquals(0x70.toByte(), storage[1])
        assertEquals(0x60.toByte(), storage[2])
    }

    @Test
    fun testPutIntLeNbytesOverflow() {
        val storage = ByteArray(8)
        val buf = ByteArrayBufMut(storage)
        assertFails { buf.putIntLe(0x1020304050607080L, 9) }
    }

    @Test
    fun testSliceBufMutSmall() {
        // Combined put_bytes / put_u8 / put_slice round.
        val storage = ByteArray(8) { 'X'.code.toByte() }
        val slice = ByteArrayBufMut(storage)
        slice.putBytes('A'.code.toByte(), 2)
        slice.putU8('B'.code.toUByte())
        slice.putSlice("BCC".encodeToByteArray())
        assertEquals(2, slice.remainingMut())
        assertContentEquals("AABBCCXX".encodeToByteArray(), storage)

        // Big-endian put_u32 round overwrites first 4 bytes.
        val storage2 = ByteArray(8) { 'X'.code.toByte() }
        val slice2 = ByteArrayBufMut(storage2)
        slice2.putU32(0x61626364u)
        assertEquals(4, slice2.remainingMut())
        assertContentEquals("abcdXXXX".encodeToByteArray(), storage2)

        // Little-endian put_u32 round.
        val storage3 = ByteArray(8) { 'X'.code.toByte() }
        val slice3 = ByteArrayBufMut(storage3)
        slice3.putU32Le(0x30313233u)
        assertEquals(4, slice3.remainingMut())
        assertContentEquals("3210XXXX".encodeToByteArray(), storage3)
    }

    @Test
    fun testSliceBufMutLarge() {
        // Mirrors upstream's exhaustive size-coverage loop: for every length and every fill size,
        // putSlice + putBytes leave the head filled and the tail untouched.
        val len = 100
        val fill = ByteArray(len) { 'Y'.code.toByte() }

        for (bufLen in 0 until len) {
            for (fillLen in 0..bufLen) {
                run {
                    val storage = ByteArray(bufLen) { 'X'.code.toByte() }
                    val slice = ByteArrayBufMut(storage)
                    slice.putSlice(fill.copyOfRange(0, fillLen))
                    assertEquals(bufLen - fillLen, slice.remainingMut())
                    val (head, tail) = storage.copyOfRange(0, fillLen) to storage.copyOfRange(fillLen, bufLen)
                    assertContentEquals(fill.copyOfRange(0, fillLen), head)
                    assertEquals(true, tail.all { it == 'X'.code.toByte() })
                }
                run {
                    val storage = ByteArray(bufLen) { 'X'.code.toByte() }
                    val slice = ByteArrayBufMut(storage)
                    slice.putBytes(fill[0], fillLen)
                    assertEquals(bufLen - fillLen, slice.remainingMut())
                    val (head, tail) = storage.copyOfRange(0, fillLen) to storage.copyOfRange(fillLen, bufLen)
                    assertContentEquals(fill.copyOfRange(0, fillLen), head)
                    assertEquals(true, tail.all { it == 'X'.code.toByte() })
                }
            }
        }
    }

    @Test
    fun testSliceBufMutPutSliceOverflow() {
        val storage = ByteArray(4) { 'X'.code.toByte() }
        val slice = ByteArrayBufMut(storage)
        assertFails { slice.putSlice("12345".encodeToByteArray()) }
    }

    @Test
    fun testSliceBufMutPutBytesOverflow() {
        val storage = ByteArray(4) { 'X'.code.toByte() }
        val slice = ByteArrayBufMut(storage)
        assertFails { slice.putBytes('1'.code.toByte(), 5) }
    }

    @Test
    fun writeBytePanicsIfOutOfBounds() {
        val data = byteArrayOf('b'.code.toByte(), 'a'.code.toByte(), 'r'.code.toByte())
        val slice = UninitSlice.fromRawPartsMut(data, 3)
        assertFails { slice.writeByte(4, 'f'.code.toByte()) }
    }

    @Test
    fun copyFromSlicePanicsIfDifferentLength1() {
        val data = byteArrayOf('b'.code.toByte(), 'a'.code.toByte(), 'r'.code.toByte())
        val slice = UninitSlice.fromRawPartsMut(data, 3)
        assertFails { slice.copyFromSlice("a".encodeToByteArray()) }
    }

    @Test
    fun copyFromSlicePanicsIfDifferentLength2() {
        val data = byteArrayOf('b'.code.toByte(), 'a'.code.toByte(), 'r'.code.toByte())
        val slice = UninitSlice.fromRawPartsMut(data, 3)
        assertFails { slice.copyFromSlice("abcd".encodeToByteArray()) }
    }
}
