package io.github.kotlinmania.bytes.buf

import io.github.kotlinmania.bytes.BytesMut
import io.github.kotlinmania.bytes.I128
import io.github.kotlinmania.bytes.U128
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BufMutPutSliceDispatchParityTest {
    private class SentinelSliceException : RuntimeException("putSlice sentinel")

    private open class RecordingPutSliceBufMut : BufMut {
        val slices: MutableList<ByteArray> = mutableListOf()

        override fun putSlice(src: ByteArray) {
            slices += src.copyOf()
        }

        override fun remainingMut(): Int = throw AssertionError("unexpected remainingMut()")

        override fun chunkMut(): UninitSlice = throw AssertionError("unexpected chunkMut()")

        override fun advanceMut(cnt: Int): Nothing = throw AssertionError("unexpected advanceMut($cnt)")

        override fun putBytes(value: Byte, cnt: Int): Nothing =
            throw AssertionError("unexpected putBytes($value, $cnt)")
    }

    @Test
    fun recordingBufMutReceivesSingleExactPutSliceAcrossAllNumericFamilies() {
        val recorder = RecordingPutSliceBufMut()
        val u128Val = U128(0x0102030405060708uL, 0x090a0b0c0d0e0f10uL)
        val i128Val = I128(-0x0102030405060708L, 0x1112131415161718uL)
        val rawF32Nan = Float.fromBits(0x7fe12345)
        val rawF64Nan = Double.fromBits(0x7ff4_0102_0304_0506L)

        val cases: List<Pair<() -> Unit, ByteArray>> =
            listOf(
                ({ recorder.putU8(0xabu) }) to byteArrayOf(0xab.toByte()),
                ({ recorder.putI8(-0x12) }) to byteArrayOf(0xee.toByte()),
                ({ recorder.putU16(0x1234u) }) to byteArrayOf(0x12, 0x34),
                ({ recorder.putU16Le(0x1234u) }) to byteArrayOf(0x34, 0x12),
                ({ recorder.putU16Ne(0x1234u) }) to byteArrayOf(0x34, 0x12),
                ({ recorder.putI16(-0x0103) }) to byteArrayOf(0xfe.toByte(), 0xfd.toByte()),
                ({ recorder.putI16Le(-0x0103) }) to byteArrayOf(0xfd.toByte(), 0xfe.toByte()),
                ({ recorder.putI16Ne(-0x0103) }) to byteArrayOf(0xfd.toByte(), 0xfe.toByte()),
                ({ recorder.putU32(0x01020304u) }) to byteArrayOf(1, 2, 3, 4),
                ({ recorder.putU32Le(0x01020304u) }) to byteArrayOf(4, 3, 2, 1),
                ({ recorder.putU32Ne(0x01020304u) }) to byteArrayOf(4, 3, 2, 1),
                ({ recorder.putI32(-0x01020305) }) to
                    byteArrayOf(0xfe.toByte(), 0xfd.toByte(), 0xfc.toByte(), 0xfb.toByte()),
                ({ recorder.putI32Le(-0x01020305) }) to
                    byteArrayOf(0xfb.toByte(), 0xfc.toByte(), 0xfd.toByte(), 0xfe.toByte()),
                ({ recorder.putI32Ne(-0x01020305) }) to
                    byteArrayOf(0xfb.toByte(), 0xfc.toByte(), 0xfd.toByte(), 0xfe.toByte()),
                ({ recorder.putU64(0x0102030405060708uL) }) to byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8),
                ({ recorder.putU64Le(0x0102030405060708uL) }) to byteArrayOf(8, 7, 6, 5, 4, 3, 2, 1),
                ({ recorder.putU64Ne(0x0102030405060708uL) }) to byteArrayOf(8, 7, 6, 5, 4, 3, 2, 1),
                ({ recorder.putI64(-0x0102030405060709L) }) to
                    byteArrayOf(
                        0xfe.toByte(),
                        0xfd.toByte(),
                        0xfc.toByte(),
                        0xfb.toByte(),
                        0xfa.toByte(),
                        0xf9.toByte(),
                        0xf8.toByte(),
                        0xf7.toByte(),
                    ),
                ({ recorder.putI64Le(-0x0102030405060709L) }) to
                    byteArrayOf(
                        0xf7.toByte(),
                        0xf8.toByte(),
                        0xf9.toByte(),
                        0xfa.toByte(),
                        0xfb.toByte(),
                        0xfc.toByte(),
                        0xfd.toByte(),
                        0xfe.toByte(),
                    ),
                ({ recorder.putI64Ne(-0x0102030405060709L) }) to
                    byteArrayOf(
                        0xf7.toByte(),
                        0xf8.toByte(),
                        0xf9.toByte(),
                        0xfa.toByte(),
                        0xfb.toByte(),
                        0xfc.toByte(),
                        0xfd.toByte(),
                        0xfe.toByte(),
                    ),
                ({ recorder.putU128(u128Val) }) to u128Val.toBigEndianBytes(),
                ({ recorder.putU128Le(u128Val) }) to u128Val.toLittleEndianBytes(),
                ({ recorder.putU128Ne(u128Val) }) to u128Val.toLittleEndianBytes(),
                ({ recorder.putI128(i128Val) }) to i128Val.toBigEndianBytes(),
                ({ recorder.putI128Le(i128Val) }) to i128Val.toLittleEndianBytes(),
                ({ recorder.putI128Ne(i128Val) }) to i128Val.toLittleEndianBytes(),
                ({ recorder.putUint(0xaa010203uL, 3) }) to byteArrayOf(1, 2, 3),
                ({ recorder.putUintLe(0xaa010203uL, 3) }) to byteArrayOf(3, 2, 1),
                ({ recorder.putUintNe(0xaa010203uL, 3) }) to byteArrayOf(3, 2, 1),
                ({ recorder.putInt(0x0504010203L, 3) }) to byteArrayOf(1, 2, 3),
                ({ recorder.putIntLe(0x0504010203L, 3) }) to byteArrayOf(3, 2, 1),
                ({ recorder.putIntNe(-0x010204L, 3) }) to
                    byteArrayOf(0xfc.toByte(), 0xfd.toByte(), 0xfe.toByte()),
                ({ recorder.putF32(rawF32Nan) }) to byteArrayOf(0x7f, 0xe1.toByte(), 0x23, 0x45),
                ({ recorder.putF32Le(rawF32Nan) }) to byteArrayOf(0x45, 0x23, 0xe1.toByte(), 0x7f),
                ({ recorder.putF32Ne(rawF32Nan) }) to byteArrayOf(0x45, 0x23, 0xe1.toByte(), 0x7f),
                ({ recorder.putF64(rawF64Nan) }) to
                    byteArrayOf(0x7f, 0xf4.toByte(), 0x01, 0x02, 0x03, 0x04, 0x05, 0x06),
                ({ recorder.putF64Le(rawF64Nan) }) to
                    byteArrayOf(0x06, 0x05, 0x04, 0x03, 0x02, 0x01, 0xf4.toByte(), 0x7f),
                ({ recorder.putF64Ne(rawF64Nan) }) to
                    byteArrayOf(0x06, 0x05, 0x04, 0x03, 0x02, 0x01, 0xf4.toByte(), 0x7f),
            )

        cases.forEachIndexed { index, (invoke, expected) ->
            invoke()
            assertEquals(index + 1, recorder.slices.size)
            assertContentEquals(expected, recorder.slices[index])
        }
    }

    @Test
    fun variableWidthZeroEmitsEmptySliceAndInvalidWidthRejectsBeforeSlice() {
        val recorder = RecordingPutSliceBufMut()

        recorder.putUint(0xdeadbeefuL, 0)
        recorder.putUintLe(0xdeadbeefuL, 0)
        recorder.putInt(-1L, 0)
        recorder.putIntLe(-1L, 0)
        assertEquals(4, recorder.slices.size)
        assertTrue(recorder.slices.all { it.isEmpty() })

        assertFailsWith<IllegalArgumentException> { recorder.putUint(1uL, 9) }
        assertFailsWith<IllegalArgumentException> { recorder.putUintLe(1uL, 9) }
        assertFailsWith<IllegalArgumentException> { recorder.putUintNe(1uL, 9) }
        assertFailsWith<IllegalArgumentException> { recorder.putInt(1L, 9) }
        assertFailsWith<IllegalArgumentException> { recorder.putIntLe(1L, 9) }
        assertFailsWith<IllegalArgumentException> { recorder.putIntNe(1L, 9) }
        assertFailsWith<IllegalArgumentException> { recorder.putUint(1uL, -1) }
        assertFailsWith<IllegalArgumentException> { recorder.putInt(1L, -1) }
        assertEquals(4, recorder.slices.size)

        val forbidUintOverride =
            object : RecordingPutSliceBufMut() {
                override fun putUint(n: ULong, nbytes: Int): Nothing =
                    throw AssertionError("putInt must not delegate through putUint")

                override fun putUintLe(n: ULong, nbytes: Int): Nothing =
                    throw AssertionError("putIntLe must not delegate through putUintLe")
            }
        forbidUintOverride.putInt(-0x010204L, 3)
        forbidUintOverride.putIntLe(-0x010204L, 3)
        assertEquals(2, forbidUintOverride.slices.size)
        assertContentEquals(byteArrayOf(0xfe.toByte(), 0xfd.toByte(), 0xfc.toByte()), forbidUintOverride.slices[0])
        assertContentEquals(byteArrayOf(0xfc.toByte(), 0xfd.toByte(), 0xfe.toByte()), forbidUintOverride.slices[1])

        val fixedNeTrap =
            object : RecordingPutSliceBufMut() {
                override fun putU16(n: UShort): Nothing = throw AssertionError("unexpected putU16")

                override fun putU16Le(n: UShort): Nothing = throw AssertionError("unexpected putU16Le")

                override fun putI16(n: Short): Nothing = throw AssertionError("unexpected putI16")

                override fun putI16Le(n: Short): Nothing = throw AssertionError("unexpected putI16Le")

                override fun putU32(n: UInt): Nothing = throw AssertionError("unexpected putU32")

                override fun putU32Le(n: UInt): Nothing = throw AssertionError("unexpected putU32Le")

                override fun putI32(n: Int): Nothing = throw AssertionError("unexpected putI32")

                override fun putI32Le(n: Int): Nothing = throw AssertionError("unexpected putI32Le")

                override fun putU64(n: ULong): Nothing = throw AssertionError("unexpected putU64")

                override fun putU64Le(n: ULong): Nothing = throw AssertionError("unexpected putU64Le")

                override fun putI64(n: Long): Nothing = throw AssertionError("unexpected putI64")

                override fun putI64Le(n: Long): Nothing = throw AssertionError("unexpected putI64Le")

                override fun putU128(n: U128): Nothing = throw AssertionError("unexpected putU128")

                override fun putU128Le(n: U128): Nothing = throw AssertionError("unexpected putU128Le")

                override fun putI128(n: I128): Nothing = throw AssertionError("unexpected putI128")

                override fun putI128Le(n: I128): Nothing = throw AssertionError("unexpected putI128Le")
            }
        val u128Sample = U128(0x0102030405060708uL, 0x090a0b0c0d0e0f10uL)
        val i128Sample = I128(-0x0102030405060708L, 0x1112131415161718uL)
        fixedNeTrap.putU16Ne(0x1234u)
        fixedNeTrap.putI16Ne(-0x0103)
        fixedNeTrap.putU32Ne(0x01020304u)
        fixedNeTrap.putI32Ne(-0x01020305)
        fixedNeTrap.putU64Ne(0x0102030405060708uL)
        fixedNeTrap.putI64Ne(-0x0102030405060709L)
        fixedNeTrap.putU128Ne(u128Sample)
        fixedNeTrap.putI128Ne(i128Sample)
        assertEquals(8, fixedNeTrap.slices.size)
        assertContentEquals(byteArrayOf(0x34, 0x12), fixedNeTrap.slices[0])
        assertContentEquals(byteArrayOf(0xfd.toByte(), 0xfe.toByte()), fixedNeTrap.slices[1])
        assertContentEquals(byteArrayOf(4, 3, 2, 1), fixedNeTrap.slices[2])
        assertContentEquals(
            byteArrayOf(0xfb.toByte(), 0xfc.toByte(), 0xfd.toByte(), 0xfe.toByte()),
            fixedNeTrap.slices[3],
        )
        assertContentEquals(byteArrayOf(8, 7, 6, 5, 4, 3, 2, 1), fixedNeTrap.slices[4])
        assertContentEquals(
            byteArrayOf(
                0xf7.toByte(),
                0xf8.toByte(),
                0xf9.toByte(),
                0xfa.toByte(),
                0xfb.toByte(),
                0xfc.toByte(),
                0xfd.toByte(),
                0xfe.toByte(),
            ),
            fixedNeTrap.slices[5],
        )
        assertContentEquals(u128Sample.toLittleEndianBytes(), fixedNeTrap.slices[6])
        assertContentEquals(i128Sample.toLittleEndianBytes(), fixedNeTrap.slices[7])

        val varNeCalls = mutableListOf<String>()
        val varNeTrap =
            object : RecordingPutSliceBufMut() {
                override fun putSlice(src: ByteArray): Nothing =
                    throw AssertionError("putUintNe/putIntNe should delegate to putUintLe/putIntLe")

                override fun putUintLe(n: ULong, nbytes: Int) {
                    varNeCalls += "uint_le:$n:$nbytes"
                }

                override fun putIntLe(n: Long, nbytes: Int) {
                    varNeCalls += "int_le:$n:$nbytes"
                }
            }
        varNeTrap.putUintNe(0x123456uL, 3)
        varNeTrap.putIntNe(-42L, 2)
        assertEquals(listOf("uint_le:1193046:3", "int_le:-42:2"), varNeCalls)
    }

    @Test
    fun sentinelPutSliceExceptionPropagatesUnwrappedAndFloatsHonorIntegerOverrides() {
        val sentinel = SentinelSliceException()
        val throwingSink =
            object : RecordingPutSliceBufMut() {
                override fun putSlice(src: ByteArray): Nothing = throw sentinel
            }

        assertSame(sentinel, assertFailsWith<SentinelSliceException> { throwingSink.putU8(1u) })
        assertSame(sentinel, assertFailsWith<SentinelSliceException> { throwingSink.putI32Le(-5) })
        assertSame(sentinel, assertFailsWith<SentinelSliceException> { throwingSink.putU128(U128.ZERO) })
        assertSame(sentinel, assertFailsWith<SentinelSliceException> { throwingSink.putUint(7uL, 2) })
        assertSame(sentinel, assertFailsWith<SentinelSliceException> { throwingSink.putIntLe(-7L, 2) })
        assertSame(sentinel, assertFailsWith<SentinelSliceException> { throwingSink.putF64(1.25) })

        val recordedIntCalls = mutableListOf<Pair<String, ULong>>()
        val intOverrideSink =
            object : RecordingPutSliceBufMut() {
                override fun putSlice(src: ByteArray): Nothing =
                    throw AssertionError("float writers should delegate to integer overrides")

                override fun putU32(n: UInt) {
                    recordedIntCalls += "u32_be" to n.toULong()
                }

                override fun putU32Le(n: UInt) {
                    recordedIntCalls += "u32_le" to n.toULong()
                }

                override fun putU32Ne(n: UInt) {
                    recordedIntCalls += "u32_ne" to n.toULong()
                }

                override fun putU64(n: ULong) {
                    recordedIntCalls += "u64_be" to n
                }

                override fun putU64Le(n: ULong) {
                    recordedIntCalls += "u64_le" to n
                }

                override fun putU64Ne(n: ULong) {
                    recordedIntCalls += "u64_ne" to n
                }
            }

        val nan32 = Float.fromBits(0x7fe12345)
        val nan64 = Double.fromBits(0x7ff4_0102_0304_0506L)
        intOverrideSink.putF32(nan32)
        intOverrideSink.putF32Le(nan32)
        intOverrideSink.putF32Ne(nan32)
        intOverrideSink.putF64(nan64)
        intOverrideSink.putF64Le(nan64)
        intOverrideSink.putF64Ne(nan64)

        assertEquals(
            listOf(
                "u32_be" to 0x7fe12345uL,
                "u32_le" to 0x7fe12345uL,
                "u32_ne" to 0x7fe12345uL,
                "u64_be" to 0x7ff4_0102_0304_0506uL,
                "u64_le" to 0x7ff4_0102_0304_0506uL,
                "u64_ne" to 0x7ff4_0102_0304_0506uL,
            ),
            recordedIntCalls,
        )
    }

    @Test
    fun defaultPrimitiveBufMutFallbackEmitsExpectedBytesAndEnforcesUnderflow() {
        val backing = ByteArray(14)
        var pos = 0
        val primitive =
            object : BufMut {
                override fun remainingMut(): Int = backing.size - pos

                override fun chunkMut(): UninitSlice {
                    val step = minOf(3, backing.size - pos)
                    return UninitSlice.rangeRef(backing, pos, pos + step)
                }

                override fun advanceMut(cnt: Int) {
                    check(cnt <= backing.size - pos)
                    pos += cnt
                }
            }

        primitive.putU8(0x11u)
        primitive.putI16(-0x0103)
        primitive.putU32Le(0x21222324u)
        primitive.putInt(0x99aabbccL, 3)
        primitive.putF32(Float.fromBits(0x3f800000))

        assertEquals(0, primitive.remainingMut())
        assertContentEquals(
            byteArrayOf(
                0x11,
                0xfe.toByte(),
                0xfd.toByte(),
                0x24,
                0x23,
                0x22,
                0x21,
                0xaa.toByte(),
                0xbb.toByte(),
                0xcc.toByte(),
                0x3f,
                0x80.toByte(),
                0x00,
                0x00,
            ),
            backing,
        )

        primitive.putUint(0x1234uL, 0)
        assertFailsWith<IllegalArgumentException> { primitive.putUint(1uL, 9) }
        assertFailsWith<IndexOutOfBoundsException> { primitive.putU8(1u) }
        assertFailsWith<IndexOutOfBoundsException> { primitive.putU16(1u) }
        assertFailsWith<IndexOutOfBoundsException> { primitive.putUint(1uL, 1) }
    }

    @Test
    fun bytesMutNumericWritesPreserveSmallSpareAndGrowCleanlyPastCapacity() {
        val buf = BytesMut.withCapacity(10)
        buf.putU8(0x01u)
        buf.putI8(0x02)
        buf.putU16(0x0304u)
        buf.putI32Le(0x08070605)
        buf.putUint(0x090auL, 2)

        assertEquals(10, buf.len())
        assertEquals(10, buf.capacity())

        buf.putU32(0x0b0c0d0eu)
        assertEquals(14, buf.len())
        assertTrue(buf.capacity() >= buf.len())
        assertContentEquals(
            byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14),
            buf.asRef(),
        )
    }
}
