package io.github.kotlinmania.bytes.buf

import io.github.kotlinmania.bytes.TryGetError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BufVariableReadParityTest {
    private class ReadCase(
        val name: String,
        val expected: ULong,
        val read: (Buf) -> ULong,
    )

    private class RecordingBuf(
        private val bytes: ByteArray,
        private val chunkLimit: Int,
        private val failure: TryGetError? = null,
    ) : Buf {
        var position = 0
            private set
        var chunkCalls = 0
            private set
        var copyCalls = 0
            private set
        var tryCopyCalls = 0
            private set

        override fun remaining(): Int = bytes.size - position

        override fun chunk(): ByteArray {
            chunkCalls++
            return bytes.copyOfRange(position, minOf(bytes.size, position + chunkLimit))
        }

        override fun advance(cnt: Int) {
            check(cnt <= remaining())
            position += cnt
        }

        override fun copyToSlice(dst: ByteArray) {
            copyCalls++
            error("copy method marker")
        }

        override fun tryCopyToSlice(dst: ByteArray): Result<Unit> {
            tryCopyCalls++
            failure?.let { return Result.failure(it) }
            bytes.copyInto(dst, startIndex = position, endIndex = position + dst.size)
            position += dst.size
            return Result.success(Unit)
        }

        override fun tryGetUint(nbytes: Int): Result<ULong> = error("unsigned getter override marker")

        override fun tryGetUintLe(nbytes: Int): Result<ULong> = error("unsigned getter override marker")
    }

    private fun readers(): List<ReadCase> =
        listOf(
            ReadCase("getUint", 0x010203uL) { it.getUint(3) },
            ReadCase("getUintLe", 0x030201uL) { it.getUintLe(3) },
            ReadCase("getUintNe", 0x030201uL) { it.getUintNe(3) },
            ReadCase("getInt", 0x010203uL) { it.getInt(3).toULong() },
            ReadCase("getIntLe", 0x030201uL) { it.getIntLe(3).toULong() },
            ReadCase("getIntNe", 0x030201uL) { it.getIntNe(3).toULong() },
        )

    @Test
    fun infallibleVariableReadsUseTryCopyForEveryStorageShape() {
        val bytes = ByteArray(16) { (it + 1).toByte() }
        for (case in readers()) {
            for (chunkLimit in listOf(1, bytes.size)) {
                val buf = RecordingBuf(bytes, chunkLimit)
                assertEquals(case.expected, case.read(buf), case.name)
                assertEquals(1, buf.tryCopyCalls, case.name)
                assertEquals(0, buf.copyCalls, case.name)
                assertEquals(0, buf.chunkCalls, case.name)
                assertEquals(3, buf.position, case.name)
            }
        }
    }

    @Test
    fun infallibleVariableReadErrorsUseTheReturnedFieldsInTheirPanic() {
        for (case in readers()) {
            val buf = RecordingBuf(ByteArray(16), 16, TryGetError(3, 2))
            val error = assertFailsWith<IndexOutOfBoundsException>(case.name) { case.read(buf) }
            assertEquals("advance out of bounds: the len is 2 but advancing by 3", error.message)
            assertEquals(1, buf.tryCopyCalls, case.name)
            assertEquals(0, buf.copyCalls, case.name)
            assertEquals(0, buf.chunkCalls, case.name)
            assertEquals(0, buf.position, case.name)
        }
    }

    @Test
    fun zeroWidthUnsignedReadsStillCallTryCopy() {
        val reads: List<(Buf) -> ULong> = listOf({ it.getUint(0) }, { it.getUintLe(0) }, { it.getUintNe(0) })
        for (read in reads) {
            val buf = RecordingBuf(ByteArray(0), 0)
            assertEquals(0uL, read(buf))
            assertEquals(1, buf.tryCopyCalls)
            assertEquals(0, buf.chunkCalls)
            assertEquals(0, buf.position)
        }
    }

    @Test
    fun signedFallibleReadsZeroPadReducedWidthsLikeTheUpstreamCrate() {
        for (width in 0..8) {
            val expected =
                when (width) {
                    0 -> 0L
                    8 -> -1L
                    else -> ((1uL shl (width * 8)) - 1uL).toLong()
                }
            val bytes = ByteArray(8) { 0xff.toByte() }
            assertEquals(expected, ByteArrayBuf(bytes).tryGetInt(width).getOrThrow())
            assertEquals(expected, ByteArrayBuf(bytes).tryGetIntLe(width).getOrThrow())
            assertEquals(expected, ByteArrayBuf(bytes).tryGetIntNe(width).getOrThrow())
        }
    }

    @Test
    fun signedInfallibleReadsStillSignExtendReducedWidths() {
        for (width in 1..8) {
            val bytes = ByteArray(8) { 0xff.toByte() }
            assertEquals(-1L, ByteArrayBuf(bytes).getInt(width))
            assertEquals(-1L, ByteArrayBuf(bytes).getIntLe(width))
            assertEquals(-1L, ByteArrayBuf(bytes).getIntNe(width))
        }
    }

    @Test
    fun signedFallibleReadsBypassUnsignedGetterOverrides() {
        val bytes = ByteArray(16) { (it + 1).toByte() }
        val big = RecordingBuf(bytes, 16)
        val little = RecordingBuf(bytes, 16)
        val native = RecordingBuf(bytes, 16)
        assertEquals(0x010203L, big.tryGetInt(3).getOrThrow())
        assertEquals(0x030201L, little.tryGetIntLe(3).getOrThrow())
        assertEquals(0x030201L, native.tryGetIntNe(3).getOrThrow())
        for (buf in listOf(big, little, native)) {
            assertEquals(1, buf.tryCopyCalls)
            assertEquals(0, buf.chunkCalls)
        }
    }

    @Test
    fun oversizedReadsPanicBeforeCopying() {
        val buf = RecordingBuf(ByteArray(16), 16)
        assertFailsWith<IllegalArgumentException> { buf.getUint(9) }
        assertFailsWith<IllegalArgumentException> { buf.tryGetInt(9) }
        assertEquals(0, buf.tryCopyCalls)
        assertEquals(0, buf.copyCalls)
        assertEquals(0, buf.chunkCalls)
    }
}
