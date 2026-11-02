package io.github.kotlinmania.bytes.buf

import io.github.kotlinmania.bytes.TryGetError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

class BufNumericCopyDispatchTest {
    private class FixedRead(
        val name: String,
        val size: Int,
        val read: (Buf) -> Result<*>,
    )

    private class VariableRead(
        val name: String,
        val read: (Buf) -> Result<*>,
    )

    private class RecordingBuf(
        private val bytes: ByteArray,
        private val chunkLimit: Int,
        private val tryFailure: Throwable? = null,
        private val copyFailure: Throwable? = null,
    ) : Buf {
        var position = 0
            private set
        var chunkCalls = 0
            private set
        var tryCopyCalls = 0
            private set
        val copySizes = mutableListOf<Int>()
        val tryCopySizes = mutableListOf<Int>()

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
            copySizes.add(dst.size)
            copyFailure?.let { throw it }
            bytes.copyInto(dst, startIndex = position, endIndex = position + dst.size)
            position += dst.size
        }

        override fun tryCopyToSlice(dst: ByteArray): Result<Unit> {
            tryCopyCalls++
            tryCopySizes.add(dst.size)
            tryFailure?.let { return Result.failure(it) }
            bytes.copyInto(dst, startIndex = position, endIndex = position + dst.size)
            position += dst.size
            return Result.success(Unit)
        }
    }

    private fun fixedReads(): List<FixedRead> =
        listOf(
            FixedRead("tryGetU16", 2) { it.tryGetU16() },
            FixedRead("tryGetU16Le", 2) { it.tryGetU16Le() },
            FixedRead("tryGetU16Ne", 2) { it.tryGetU16Ne() },
            FixedRead("tryGetI16", 2) { it.tryGetI16() },
            FixedRead("tryGetI16Le", 2) { it.tryGetI16Le() },
            FixedRead("tryGetI16Ne", 2) { it.tryGetI16Ne() },
            FixedRead("tryGetU32", 4) { it.tryGetU32() },
            FixedRead("tryGetU32Le", 4) { it.tryGetU32Le() },
            FixedRead("tryGetU32Ne", 4) { it.tryGetU32Ne() },
            FixedRead("tryGetI32", 4) { it.tryGetI32() },
            FixedRead("tryGetI32Le", 4) { it.tryGetI32Le() },
            FixedRead("tryGetI32Ne", 4) { it.tryGetI32Ne() },
            FixedRead("tryGetU64", 8) { it.tryGetU64() },
            FixedRead("tryGetU64Le", 8) { it.tryGetU64Le() },
            FixedRead("tryGetU64Ne", 8) { it.tryGetU64Ne() },
            FixedRead("tryGetI64", 8) { it.tryGetI64() },
            FixedRead("tryGetI64Le", 8) { it.tryGetI64Le() },
            FixedRead("tryGetI64Ne", 8) { it.tryGetI64Ne() },
            FixedRead("tryGetU128", 16) { it.tryGetU128() },
            FixedRead("tryGetU128Le", 16) { it.tryGetU128Le() },
            FixedRead("tryGetU128Ne", 16) { it.tryGetU128Ne() },
            FixedRead("tryGetI128", 16) { it.tryGetI128() },
            FixedRead("tryGetI128Le", 16) { it.tryGetI128Le() },
            FixedRead("tryGetI128Ne", 16) { it.tryGetI128Ne() },
            FixedRead("tryGetF32", 4) { it.tryGetF32() },
            FixedRead("tryGetF32Le", 4) { it.tryGetF32Le() },
            FixedRead("tryGetF32Ne", 4) { it.tryGetF32Ne() },
            FixedRead("tryGetF64", 8) { it.tryGetF64() },
            FixedRead("tryGetF64Le", 8) { it.tryGetF64Le() },
            FixedRead("tryGetF64Ne", 8) { it.tryGetF64Ne() },
        )

    private fun variableReads(): List<VariableRead> =
        listOf(
            VariableRead("tryGetUint") { it.tryGetUint(3) },
            VariableRead("tryGetUintLe") { it.tryGetUintLe(3) },
            VariableRead("tryGetUintNe") { it.tryGetUintNe(3) },
            VariableRead("tryGetInt") { it.tryGetInt(3) },
            VariableRead("tryGetIntLe") { it.tryGetIntLe(3) },
            VariableRead("tryGetIntNe") { it.tryGetIntNe(3) },
        )

    @Test
    fun fragmentedFixedWidthReadsDispatchThroughCopyToSlice() {
        val bytes = ByteArray(16) { (it + 1).toByte() }
        for (case in fixedReads()) {
            val buf = RecordingBuf(bytes, 1, tryFailure = TryGetError(case.size, 0))
            val actual = case.read(buf).getOrThrow()
            val expected = case.read(ByteArrayBuf(bytes)).getOrThrow()
            assertEquals(expected, actual, case.name)
            assertEquals(listOf(case.size), buf.copySizes, case.name)
            assertEquals(0, buf.tryCopyCalls, case.name)
            assertEquals(case.size, buf.position, case.name)
        }
    }

    @Test
    fun contiguousFixedWidthReadsUseTheDirectChunk() {
        val bytes = ByteArray(16) { (it + 1).toByte() }
        for (case in fixedReads()) {
            val buf = RecordingBuf(bytes, bytes.size)
            assertEquals(case.read(ByteArrayBuf(bytes)).getOrThrow(), case.read(buf).getOrThrow(), case.name)
            assertEquals(1, buf.chunkCalls, case.name)
            assertEquals(emptyList(), buf.copySizes, case.name)
            assertEquals(0, buf.tryCopyCalls, case.name)
            assertEquals(case.size, buf.position, case.name)
        }
    }

    @Test
    fun shortFixedWidthReadsFailBeforeAnyCopyOrChunk() {
        for (case in fixedReads()) {
            val buf = RecordingBuf(ByteArray(case.size - 1), 1)
            val error = assertIs<TryGetError>(case.read(buf).exceptionOrNull())
            assertEquals(case.size, error.requested, case.name)
            assertEquals(case.size - 1, error.available, case.name)
            assertEquals(0, buf.chunkCalls, case.name)
            assertEquals(emptyList(), buf.copySizes, case.name)
            assertEquals(0, buf.tryCopyCalls, case.name)
            assertEquals(0, buf.position, case.name)
        }
    }

    @Test
    fun fixedWidthCopyPanicsPropagateInsteadOfBecomingReadErrors() {
        val marker = IllegalStateException("copy marker")
        val buf = RecordingBuf(ByteArray(4), 1, copyFailure = marker)
        assertSame(marker, assertFailsWith<IllegalStateException> { buf.tryGetU32() })
        assertEquals(listOf(4), buf.copySizes)
        assertEquals(0, buf.tryCopyCalls)
        assertEquals(0, buf.position)
    }

    @Test
    fun variableWidthReadsAlwaysDispatchThroughTryCopyToSlice() {
        val bytes = ByteArray(16) { (it + 1).toByte() }
        for (case in variableReads()) {
            for (chunkLimit in listOf(1, bytes.size)) {
                val buf = RecordingBuf(bytes, chunkLimit)
                assertEquals(case.read(ByteArrayBuf(bytes)).getOrThrow(), case.read(buf).getOrThrow(), case.name)
                assertEquals(listOf(3), buf.tryCopySizes, case.name)
                assertEquals(emptyList(), buf.copySizes, case.name)
                assertEquals(0, buf.chunkCalls, case.name)
                assertEquals(3, buf.position, case.name)
            }
        }
    }

    @Test
    fun variableWidthReadsPreserveTheOverrideErrorWithoutPrecheckingRemaining() {
        val marker = TryGetError(3, 2)
        for (case in variableReads()) {
            for (available in listOf(2, 16)) {
                val buf = RecordingBuf(ByteArray(available), available, tryFailure = marker)
                assertSame(marker, case.read(buf).exceptionOrNull(), case.name)
                assertEquals(listOf(3), buf.tryCopySizes, case.name)
                assertEquals(0, buf.chunkCalls, case.name)
                assertEquals(0, buf.position, case.name)
            }
        }
    }

    @Test
    fun zeroWidthUnsignedReadsStillDispatchToTheOverride() {
        val marker = TryGetError(0, 0)
        val buf = RecordingBuf(ByteArray(0), 0, tryFailure = marker)
        assertSame(marker, buf.tryGetUint(0).exceptionOrNull())
        assertEquals(listOf(0), buf.tryCopySizes)
        assertEquals(0, buf.chunkCalls)
        assertEquals(0, buf.position)
    }

    @Test
    fun oversizedVariableReadsPanicBeforeCopying() {
        val buf = RecordingBuf(ByteArray(16), 16)
        assertFailsWith<IllegalArgumentException> { buf.tryGetUint(9) }
        assertEquals(0, buf.tryCopyCalls)
        assertEquals(0, buf.chunkCalls)
        assertEquals(0, buf.position)
    }
}
