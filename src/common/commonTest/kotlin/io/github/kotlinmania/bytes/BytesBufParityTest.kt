package io.github.kotlinmania.bytes

import io.github.kotlinmania.bytes.buf.Buf
import io.github.kotlinmania.bytes.buf.IntoIter
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BytesBufParityTest {
    @Test
    fun bytesConformsToBufAndSupportsEndianNumericReads() {
        val raw =
            byteArrayOf(
                0x7f,
                0x12,
                0x34,
                0x34,
                0x12,
                0xff.toByte(),
                0xfe.toByte(),
                0xfd.toByte(),
                0xfc.toByte(),
                0xfc.toByte(),
                0xfd.toByte(),
                0xfe.toByte(),
                0xff.toByte(),
                0x01,
                0x02,
                0x03,
                0x04,
                0x05,
                0x06,
                0x07,
                0x08,
                0xaa.toByte(),
                0xbb.toByte(),
                0xcc.toByte(),
            )
        val bytes = Bytes.from(raw)
        val buf: Buf = assertIs<Buf>(bytes)
        assertEquals(24, countRemainingAsBuf(bytes))

        assertEquals(24, buf.remaining())
        assertTrue(bytes.hasRemaining())
        assertContentEquals(raw, bytes.chunk())

        assertEquals(0x7fu.toUByte(), bytes.getU8())
        assertEquals(0x1234u.toUShort(), bytes.getU16())
        assertEquals(0x1234u.toUShort(), bytes.getU16Le())
        assertEquals(-0x010204, bytes.getI32())
        assertEquals(-0x010204, bytes.getI32Le())
        assertEquals(0x0102030405060708uL, buf.getU64())
        assertEquals(0xaabbccuL, bytes.getUint(3))

        assertEquals(0, bytes.len())
        assertEquals(0, buf.remaining())
        assertFalse(bytes.hasRemaining())
        assertTrue(bytes.chunk().isEmpty())
    }

    @Test
    fun tryGetInsufficientDataPreservesBytesCursor() {
        val initial = byteArrayOf(0x10, 0x20, 0x30)
        val bytes = Bytes.from(initial)
        val buf: Buf = bytes

        val underU32 = bytes.tryGetU32()
        assertTrue(underU32.isFailure)
        assertEquals(TryGetError(requested = 4, available = 3), underU32.exceptionOrNull())
        assertEquals(3, bytes.len())
        assertEquals(3, buf.remaining())
        assertContentEquals(initial, bytes.asSlice())

        val underI64 = buf.tryGetI64Le()
        assertTrue(underI64.isFailure)
        assertEquals(TryGetError(requested = 8, available = 3), underI64.exceptionOrNull())
        assertEquals(3, bytes.remaining())
        assertContentEquals(initial, bytes.chunk())

        assertEquals(0x1020u.toUShort(), bytes.tryGetU16().getOrThrow())
        assertEquals(1, bytes.len())
        assertContentEquals(byteArrayOf(0x30), bytes.asSlice())

        val underTailU16 = bytes.tryGetU16Le()
        assertTrue(underTailU16.isFailure)
        assertEquals(TryGetError(requested = 2, available = 1), underTailU16.exceptionOrNull())
        assertEquals(1, bytes.len())
        assertContentEquals(byteArrayOf(0x30), bytes.asSlice())
    }

    @Test
    fun bufAdaptersReaderTakeChainAndIntoIterOperateOnBytes() {
        val head = Bytes.from("hello ")
        val tail = Bytes.from("world!")

        val limited = head.clone().take(5)
        assertEquals(5, limited.remaining())
        assertTrue(limited.copyToBytes(5).eq("hello"))
        assertEquals(0, limited.remaining())
        assertEquals(1, limited.intoInner().remaining())

        val chained = head.chain(tail)
        assertEquals(12, chained.remaining())
        val sink = ByteArray(8)
        val reader = chained.reader()
        assertEquals(8, reader.read(sink))
        assertContentEquals("hello wo".encodeToByteArray(), sink)
        assertEquals(0, head.len())
        assertTrue(tail.eq("rld!"))

        val dstMut = BytesMut.withCapacity(8)
        dstMut.put(tail)
        assertTrue(dstMut.freeze().eq("rld!"))
        assertTrue(tail.isEmpty())

        val iterSource = Bytes.fromStatic("xyz")
        val iter = IntoIter(iterSource)
        assertEquals(3 to 3, iter.sizeHint())
        assertEquals('x'.code.toByte(), iter.next())
        assertEquals(2, iterSource.len())
        assertContentEquals("yz".encodeToByteArray(), iter.asSequence().toList().toByteArray())
        assertTrue(iterSource.isEmpty())
    }

    @Test
    fun copyToBytesUsesSplitSemanticsOnBytes() {
        val bytes = Bytes.from("abcdefgh")
        val buf: Buf = bytes

        val prefix = buf.copyToBytes(3)
        assertTrue(prefix.eq("abc"))
        assertTrue(bytes.eq("defgh"))
        assertEquals(5, buf.remaining())

        assertFailsWith<IllegalArgumentException> {
            buf.copyToBytes(6)
        }
        assertTrue(bytes.eq("defgh"))
        assertEquals(5, buf.remaining())
    }

    @Test
    fun clonedAndSlicedHandlesKeepIndependentBufCursors() {
        val original = Bytes.from("0123456789")
        val cloned = original.clone()
        val middle = original.slice(2, 8)

        val originalBuf: Buf = original
        val clonedBuf: Buf = cloned
        val middleBuf: Buf = middle

        assertEquals(0x3031u.toUShort(), original.getU16())
        originalBuf.advance(3)
        assertTrue(original.eq("56789"))
        assertEquals(5, originalBuf.remaining())

        assertEquals('2'.code.toUByte(), middle.getU8())
        val midPrefix = middleBuf.copyToBytes(2)
        assertTrue(midPrefix.eq("34"))
        assertTrue(middle.eq("567"))
        assertEquals(3, middleBuf.remaining())

        assertTrue(cloned.eq("0123456789"))
        assertEquals(10, clonedBuf.remaining())
    }

    private fun countRemainingAsBuf(buf: Buf): Int = buf.remaining()
}
