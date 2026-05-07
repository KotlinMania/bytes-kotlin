// port-lint: source buf/buf_impl.rs
package io.github.kotlinmania.bytes.buf

import io.github.kotlinmania.bytes.Bytes
import io.github.kotlinmania.bytes.I128
import io.github.kotlinmania.bytes.TryGetError
import io.github.kotlinmania.bytes.U128
import io.github.kotlinmania.bytes.panicAdvance
import io.github.kotlinmania.bytes.panicDoesNotFit

// === Endianness model ===
// Every KMP target platform supported by this port (macOS arm64/x64, iOS arm64/x64/simulator,
// Linux x64, MinGW x64, JS, Wasm-JS, Android arm64/x64) executes little-endian. The upstream
// crate's native-endian conditional resolves to little-endian on those same platforms. The
// `Ne` (native-endian) methods on the buffer trait therefore delegate to the little-endian
// variant.
private const val NATIVE_IS_BIG_ENDIAN: Boolean = false

// === Helper: sign extension for arbitrary-byte-length signed integers.
private fun signExtend(value: ULong, nbytes: Int): Long {
    val shift = (8 - nbytes) * 8
    return (value shl shift).toLong() shr shift
}

// === Byte-decoding helpers (correspond to the buffered-get macro family in upstream). Each
// helper reads exactly `size` bytes from `buf` (advancing its cursor) into an internal stack
// buffer, then converts to the requested numeric type. Big-endian and little-endian helpers are
// provided for each fixed-size integer width; the variable-width helpers (`getUint`, `getInt`)
// read up to 8 bytes and assemble manually.

private fun readFixedSize(buf: Buf, size: Int): ByteArray {
    if (buf.remaining() < size) {
        panicAdvance(TryGetError(size, buf.remaining()))
    }
    val out = ByteArray(size)
    val src = buf.chunk()
    if (src.size >= size) {
        src.copyInto(out, 0, 0, size)
        buf.advance(size)
    } else {
        buf.copyToSlice(out)
    }
    return out
}

private fun tryReadFixedSize(buf: Buf, size: Int): Result<ByteArray> {
    if (buf.remaining() < size) {
        return Result.failure(TryGetError(size, buf.remaining()))
    }
    val out = ByteArray(size)
    val src = buf.chunk()
    if (src.size >= size) {
        src.copyInto(out, 0, 0, size)
        buf.advance(size)
        return Result.success(out)
    }
    val copy = buf.tryCopyToSlice(out)
    if (copy.isFailure) {
        return Result.failure(copy.exceptionOrNull() ?: TryGetError(size, buf.remaining()))
    }
    return Result.success(out)
}

private fun bytesToShortBe(b: ByteArray): Short =
    (((b[0].toInt() and 0xff) shl 8) or (b[1].toInt() and 0xff)).toShort()

private fun bytesToShortLe(b: ByteArray): Short =
    (((b[1].toInt() and 0xff) shl 8) or (b[0].toInt() and 0xff)).toShort()

private fun bytesToIntBe(b: ByteArray): Int =
    ((b[0].toInt() and 0xff) shl 24) or
        ((b[1].toInt() and 0xff) shl 16) or
        ((b[2].toInt() and 0xff) shl 8) or
        (b[3].toInt() and 0xff)

private fun bytesToIntLe(b: ByteArray): Int =
    ((b[3].toInt() and 0xff) shl 24) or
        ((b[2].toInt() and 0xff) shl 16) or
        ((b[1].toInt() and 0xff) shl 8) or
        (b[0].toInt() and 0xff)

private fun bytesToLongBe(b: ByteArray): Long {
    var value = 0L
    for (index in 0 until 8) {
        value = (value shl 8) or (b[index].toLong() and 0xffL)
    }
    return value
}

private fun bytesToLongLe(b: ByteArray): Long {
    var value = 0L
    for (index in 7 downTo 0) {
        value = (value shl 8) or (b[index].toLong() and 0xffL)
    }
    return value
}

// Variable-byte-width readers used by `getUint` / `tryGetUint` / and friends. These read
// `nbytes` (1..8) raw bytes from `buf` and assemble a u64 in the requested byte order.
private fun readUintBe(buf: Buf, nbytes: Int): ULong {
    if (nbytes > 8) {
        panicDoesNotFit(8, nbytes)
    }
    val raw = readFixedSize(buf, nbytes)
    var value = 0uL
    for (index in 0 until nbytes) {
        value = (value shl 8) or (raw[index].toULong() and 0xffuL)
    }
    return value
}

private fun readUintLe(buf: Buf, nbytes: Int): ULong {
    if (nbytes > 8) {
        panicDoesNotFit(8, nbytes)
    }
    val raw = readFixedSize(buf, nbytes)
    var value = 0uL
    for (index in nbytes - 1 downTo 0) {
        value = (value shl 8) or (raw[index].toULong() and 0xffuL)
    }
    return value
}

private fun tryReadUintBe(buf: Buf, nbytes: Int): Result<ULong> {
    if (nbytes > 8) {
        panicDoesNotFit(8, nbytes)
    }
    val raw = tryReadFixedSize(buf, nbytes)
    if (raw.isFailure) {
        return Result.failure(raw.exceptionOrNull() ?: TryGetError(nbytes, buf.remaining()))
    }
    val src = raw.getOrThrow()
    var value = 0uL
    for (index in 0 until nbytes) {
        value = (value shl 8) or (src[index].toULong() and 0xffuL)
    }
    return Result.success(value)
}

private fun tryReadUintLe(buf: Buf, nbytes: Int): Result<ULong> {
    if (nbytes > 8) {
        panicDoesNotFit(8, nbytes)
    }
    val raw = tryReadFixedSize(buf, nbytes)
    if (raw.isFailure) {
        return Result.failure(raw.exceptionOrNull() ?: TryGetError(nbytes, buf.remaining()))
    }
    val src = raw.getOrThrow()
    var value = 0uL
    for (index in nbytes - 1 downTo 0) {
        value = (value shl 8) or (src[index].toULong() and 0xffuL)
    }
    return Result.success(value)
}

/**
 * Read bytes from a buffer.
 *
 * A buffer stores bytes in memory such that read operations are infallible.
 * The underlying storage may or may not be in contiguous memory. A `Buf` value
 * is a cursor into the buffer. Reading from `Buf` advances the cursor
 * position. It can be thought of as an efficient `Iterator` for collections of
 * bytes.
 *
 * The simplest `Buf` is a `ByteArray` wrapped via [ByteArrayBuf].
 *
 * ```
 * val buf = ByteArrayBuf("hello world".encodeToByteArray())
 *
 * check(buf.getU8() == 0x68u.toUByte())
 * check(buf.getU8() == 0x65u.toUByte())
 * check(buf.getU8() == 0x6cu.toUByte())
 *
 * val rest = ByteArray(8)
 * buf.copyToSlice(rest)
 *
 * check(rest.decodeToString() == "lo world")
 * ```
 */
public interface Buf {
    /**
     * Returns the number of bytes between the current position and the end of
     * the buffer.
     *
     * This value is greater than or equal to the length of the slice returned
     * by [chunk].
     *
     * # Examples
     *
     * ```
     * val buf = ByteArrayBuf("hello world".encodeToByteArray())
     *
     * check(buf.remaining() == 11)
     *
     * buf.getU8()
     *
     * check(buf.remaining() == 10)
     * ```
     *
     * # Implementer notes
     *
     * Implementations of `remaining` should ensure that the return value does
     * not change unless a call is made to [advance] or any other function that
     * is documented to change the `Buf`'s current position.
     */
    public fun remaining(): Int

    /**
     * Returns a slice starting at the current position and of length between 0
     * and `remaining()`. Note that this *can* return a shorter slice (this
     * allows non-continuous internal representation).
     *
     * This is a lower level function. Most operations are done with other
     * functions.
     *
     * # Examples
     *
     * ```
     * val buf = ByteArrayBuf("hello world".encodeToByteArray())
     *
     * check(buf.chunk().contentEquals("hello world".encodeToByteArray()))
     *
     * buf.advance(6)
     *
     * check(buf.chunk().contentEquals("world".encodeToByteArray()))
     * ```
     *
     * # Implementer notes
     *
     * This function should never panic. `chunk()` should return an empty
     * slice **if and only if** `remaining()` returns 0. In other words,
     * `chunk()` returning an empty slice implies that `remaining()` will
     * return 0 and `remaining()` returning 0 implies that `chunk()` will
     * return an empty slice.
     */
    public fun chunk(): ByteArray

    /**
     * Advance the internal cursor of the Buf
     *
     * The next call to [chunk] will return a slice starting `cnt` bytes
     * further into the underlying buffer.
     *
     * # Examples
     *
     * ```
     * val buf = ByteArrayBuf("hello world".encodeToByteArray())
     *
     * check(buf.chunk().contentEquals("hello world".encodeToByteArray()))
     *
     * buf.advance(6)
     *
     * check(buf.chunk().contentEquals("world".encodeToByteArray()))
     * ```
     *
     * # Panics
     *
     * This function **may** panic if `cnt > remaining()`.
     *
     * # Implementer notes
     *
     * It is recommended for implementations of `advance` to panic if
     * `cnt > remaining()`. If the implementation does not panic, the call must
     * behave as if `cnt == remaining()`.
     *
     * A call with `cnt == 0` should never panic and be a no-op.
     */
    public fun advance(cnt: Int)

    /**
     * Returns true if there are any more bytes to consume
     *
     * This is equivalent to `remaining() != 0`.
     *
     * # Examples
     *
     * ```
     * val buf = ByteArrayBuf("a".encodeToByteArray())
     *
     * check(buf.hasRemaining())
     *
     * buf.getU8()
     *
     * check(!buf.hasRemaining())
     * ```
     */
    public fun hasRemaining(): Boolean = remaining() > 0

    /**
     * Copies bytes from `self` into `dst`.
     *
     * The cursor is advanced by the number of bytes copied. `self` must have
     * enough remaining bytes to fill `dst`.
     *
     * # Examples
     *
     * ```
     * val buf = ByteArrayBuf("hello world".encodeToByteArray())
     * val dst = ByteArray(5)
     *
     * buf.copyToSlice(dst)
     * check(dst.contentEquals("hello".encodeToByteArray()))
     * check(buf.remaining() == 6)
     * ```
     *
     * # Panics
     *
     * This function panics if `remaining() < dst.size`.
     */
    public fun copyToSlice(dst: ByteArray) {
        tryCopyToSlice(dst).onFailure { error ->
            panicAdvance(error as? TryGetError ?: TryGetError(dst.size, remaining()))
        }
    }

    /**
     * Gets an unsigned 8 bit integer from `self`.
     *
     * The current position is advanced by 1.
     *
     * # Examples
     *
     * ```
     * val buf = ByteArrayBuf(byteArrayOf(0x08, 0x20, 0x68.toByte()))
     * check(buf.getU8() == 0x08u.toUByte())
     * ```
     *
     * # Panics
     *
     * This function panics if there is no more remaining data in `self`.
     */
    public fun getU8(): UByte {
        if (remaining() < 1) {
            panicAdvance(TryGetError(1, 0))
        }
        val ret = chunk()[0].toUByte()
        advance(1)
        return ret
    }

    /**
     * Gets a signed 8 bit integer from `self`.
     *
     * The current position is advanced by 1.
     *
     * # Examples
     *
     * ```
     * val buf = ByteArrayBuf(byteArrayOf(0x08, 0x20, 0x68.toByte()))
     * check(buf.getI8() == 0x08.toByte())
     * ```
     *
     * # Panics
     *
     * This function panics if there is no more remaining data in `self`.
     */
    public fun getI8(): Byte {
        if (remaining() < 1) {
            panicAdvance(TryGetError(1, 0))
        }
        val ret = chunk()[0]
        advance(1)
        return ret
    }

    /**
     * Gets an unsigned 16 bit integer from `self` in big-endian byte order.
     *
     * The current position is advanced by 2.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`.
     */
    public fun getU16(): UShort = bytesToShortBe(readFixedSize(this, 2)).toUShort()

    /**
     * Gets an unsigned 16 bit integer from `self` in little-endian byte order.
     *
     * The current position is advanced by 2.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`.
     */
    public fun getU16Le(): UShort = bytesToShortLe(readFixedSize(this, 2)).toUShort()

    /**
     * Gets an unsigned 16 bit integer from `self` in native-endian byte order.
     *
     * The current position is advanced by 2.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`.
     */
    public fun getU16Ne(): UShort = if (NATIVE_IS_BIG_ENDIAN) getU16() else getU16Le()

    /**
     * Gets a signed 16 bit integer from `self` in big-endian byte order.
     *
     * The current position is advanced by 2.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`.
     */
    public fun getI16(): Short = bytesToShortBe(readFixedSize(this, 2))

    /**
     * Gets a signed 16 bit integer from `self` in little-endian byte order.
     *
     * The current position is advanced by 2.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`.
     */
    public fun getI16Le(): Short = bytesToShortLe(readFixedSize(this, 2))

    /**
     * Gets a signed 16 bit integer from `self` in native-endian byte order.
     *
     * The current position is advanced by 2.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`.
     */
    public fun getI16Ne(): Short = if (NATIVE_IS_BIG_ENDIAN) getI16() else getI16Le()

    /**
     * Gets an unsigned 32 bit integer from `self` in the big-endian byte order.
     *
     * The current position is advanced by 4.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`.
     */
    public fun getU32(): UInt = bytesToIntBe(readFixedSize(this, 4)).toUInt()

    /**
     * Gets an unsigned 32 bit integer from `self` in the little-endian byte order.
     *
     * The current position is advanced by 4.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`.
     */
    public fun getU32Le(): UInt = bytesToIntLe(readFixedSize(this, 4)).toUInt()

    /**
     * Gets an unsigned 32 bit integer from `self` in native-endian byte order.
     *
     * The current position is advanced by 4.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`.
     */
    public fun getU32Ne(): UInt = if (NATIVE_IS_BIG_ENDIAN) getU32() else getU32Le()

    /**
     * Gets a signed 32 bit integer from `self` in big-endian byte order.
     *
     * The current position is advanced by 4.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`.
     */
    public fun getI32(): Int = bytesToIntBe(readFixedSize(this, 4))

    /**
     * Gets a signed 32 bit integer from `self` in little-endian byte order.
     *
     * The current position is advanced by 4.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`.
     */
    public fun getI32Le(): Int = bytesToIntLe(readFixedSize(this, 4))

    /**
     * Gets a signed 32 bit integer from `self` in native-endian byte order.
     *
     * The current position is advanced by 4.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`.
     */
    public fun getI32Ne(): Int = if (NATIVE_IS_BIG_ENDIAN) getI32() else getI32Le()

    /**
     * Gets an unsigned 64 bit integer from `self` in big-endian byte order.
     *
     * The current position is advanced by 8.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`.
     */
    public fun getU64(): ULong = bytesToLongBe(readFixedSize(this, 8)).toULong()

    /**
     * Gets an unsigned 64 bit integer from `self` in little-endian byte order.
     *
     * The current position is advanced by 8.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`.
     */
    public fun getU64Le(): ULong = bytesToLongLe(readFixedSize(this, 8)).toULong()

    /**
     * Gets an unsigned 64 bit integer from `self` in native-endian byte order.
     *
     * The current position is advanced by 8.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`.
     */
    public fun getU64Ne(): ULong = if (NATIVE_IS_BIG_ENDIAN) getU64() else getU64Le()

    /**
     * Gets a signed 64 bit integer from `self` in big-endian byte order.
     *
     * The current position is advanced by 8.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`.
     */
    public fun getI64(): Long = bytesToLongBe(readFixedSize(this, 8))

    /**
     * Gets a signed 64 bit integer from `self` in little-endian byte order.
     *
     * The current position is advanced by 8.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`.
     */
    public fun getI64Le(): Long = bytesToLongLe(readFixedSize(this, 8))

    /**
     * Gets a signed 64 bit integer from `self` in native-endian byte order.
     *
     * The current position is advanced by 8.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`.
     */
    public fun getI64Ne(): Long = if (NATIVE_IS_BIG_ENDIAN) getI64() else getI64Le()

    /**
     * Gets an unsigned 128 bit integer from `self` in big-endian byte order.
     *
     * The current position is advanced by 16.
     *
     * Kotlin has no native 128-bit integer; the result is wrapped in [U128].
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`.
     */
    public fun getU128(): U128 = U128.fromBigEndianBytes(readFixedSize(this, 16))

    /**
     * Gets an unsigned 128 bit integer from `self` in little-endian byte order.
     *
     * The current position is advanced by 16.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`.
     */
    public fun getU128Le(): U128 = U128.fromLittleEndianBytes(readFixedSize(this, 16))

    /**
     * Gets an unsigned 128 bit integer from `self` in native-endian byte order.
     *
     * The current position is advanced by 16.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`.
     */
    public fun getU128Ne(): U128 = if (NATIVE_IS_BIG_ENDIAN) getU128() else getU128Le()

    /**
     * Gets a signed 128 bit integer from `self` in big-endian byte order.
     *
     * The current position is advanced by 16.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`.
     */
    public fun getI128(): I128 = I128.fromBigEndianBytes(readFixedSize(this, 16))

    /**
     * Gets a signed 128 bit integer from `self` in little-endian byte order.
     *
     * The current position is advanced by 16.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`.
     */
    public fun getI128Le(): I128 = I128.fromLittleEndianBytes(readFixedSize(this, 16))

    /**
     * Gets a signed 128 bit integer from `self` in native-endian byte order.
     *
     * The current position is advanced by 16.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`.
     */
    public fun getI128Ne(): I128 = if (NATIVE_IS_BIG_ENDIAN) getI128() else getI128Le()

    /**
     * Gets an unsigned n-byte integer from `self` in big-endian byte order.
     *
     * The current position is advanced by `nbytes`.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`, or
     * if `nbytes` is greater than 8.
     */
    public fun getUint(nbytes: Int): ULong = readUintBe(this, nbytes)

    /**
     * Gets an unsigned n-byte integer from `self` in little-endian byte order.
     *
     * The current position is advanced by `nbytes`.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`, or
     * if `nbytes` is greater than 8.
     */
    public fun getUintLe(nbytes: Int): ULong = readUintLe(this, nbytes)

    /**
     * Gets an unsigned n-byte integer from `self` in native-endian byte order.
     *
     * The current position is advanced by `nbytes`.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`, or
     * if `nbytes` is greater than 8.
     */
    public fun getUintNe(nbytes: Int): ULong = if (NATIVE_IS_BIG_ENDIAN) getUint(nbytes) else getUintLe(nbytes)

    /**
     * Gets a signed n-byte integer from `self` in big-endian byte order.
     *
     * The current position is advanced by `nbytes`.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`, or
     * if `nbytes` is greater than 8.
     */
    public fun getInt(nbytes: Int): Long = signExtend(getUint(nbytes), nbytes)

    /**
     * Gets a signed n-byte integer from `self` in little-endian byte order.
     *
     * The current position is advanced by `nbytes`.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`, or
     * if `nbytes` is greater than 8.
     */
    public fun getIntLe(nbytes: Int): Long = signExtend(getUintLe(nbytes), nbytes)

    /**
     * Gets a signed n-byte integer from `self` in native-endian byte order.
     *
     * The current position is advanced by `nbytes`.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`, or
     * if `nbytes` is greater than 8.
     */
    public fun getIntNe(nbytes: Int): Long = if (NATIVE_IS_BIG_ENDIAN) getInt(nbytes) else getIntLe(nbytes)

    /**
     * Gets an IEEE754 single-precision (4 bytes) floating point number from
     * `self` in big-endian byte order.
     *
     * The current position is advanced by 4.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`.
     */
    public fun getF32(): Float = Float.fromBits(getU32().toInt())

    /**
     * Gets an IEEE754 single-precision (4 bytes) floating point number from
     * `self` in little-endian byte order.
     *
     * The current position is advanced by 4.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`.
     */
    public fun getF32Le(): Float = Float.fromBits(getU32Le().toInt())

    /**
     * Gets an IEEE754 single-precision (4 bytes) floating point number from
     * `self` in native-endian byte order.
     *
     * The current position is advanced by 4.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`.
     */
    public fun getF32Ne(): Float = Float.fromBits(getU32Ne().toInt())

    /**
     * Gets an IEEE754 double-precision (8 bytes) floating point number from
     * `self` in big-endian byte order.
     *
     * The current position is advanced by 8.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`.
     */
    public fun getF64(): Double = Double.fromBits(getU64().toLong())

    /**
     * Gets an IEEE754 double-precision (8 bytes) floating point number from
     * `self` in little-endian byte order.
     *
     * The current position is advanced by 8.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`.
     */
    public fun getF64Le(): Double = Double.fromBits(getU64Le().toLong())

    /**
     * Gets an IEEE754 double-precision (8 bytes) floating point number from
     * `self` in native-endian byte order.
     *
     * The current position is advanced by 8.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining data in `self`.
     */
    public fun getF64Ne(): Double = Double.fromBits(getU64Ne().toLong())

    /**
     * Copies bytes from `self` into `dst`.
     *
     * The cursor is advanced by the number of bytes copied. `self` must have
     * enough remaining bytes to fill `dst`.
     *
     * Returns `Result.failure(TryGetError)` when there are not enough remaining
     * bytes to read the value.
     */
    public fun tryCopyToSlice(dst: ByteArray): Result<Unit> {
        if (remaining() < dst.size) {
            return Result.failure(TryGetError(dst.size, remaining()))
        }
        var written = 0
        while (written < dst.size) {
            val src = chunk()
            val cnt = minOf(src.size, dst.size - written)
            src.copyInto(dst, written, 0, cnt)
            written += cnt
            advance(cnt)
        }
        return Result.success(Unit)
    }

    /**
     * Gets an unsigned 8 bit integer from `self`.
     *
     * The current position is advanced by 1.
     *
     * Returns `Result.failure(TryGetError)` when there are not enough remaining
     * bytes to read the value.
     */
    public fun tryGetU8(): Result<UByte> {
        if (remaining() < 1) {
            return Result.failure(TryGetError(1, remaining()))
        }
        val ret = chunk()[0].toUByte()
        advance(1)
        return Result.success(ret)
    }

    /**
     * Gets a signed 8 bit integer from `self`.
     *
     * The current position is advanced by 1.
     */
    public fun tryGetI8(): Result<Byte> {
        if (remaining() < 1) {
            return Result.failure(TryGetError(1, remaining()))
        }
        val ret = chunk()[0]
        advance(1)
        return Result.success(ret)
    }

    /**
     * Gets an unsigned 16 bit integer from `self` in big-endian byte order.
     *
     * The current position is advanced by 2.
     */
    public fun tryGetU16(): Result<UShort> = tryReadFixedSize(this, 2).map { bytesToShortBe(it).toUShort() }

    /** Gets an unsigned 16 bit integer from `self` in little-endian byte order. */
    public fun tryGetU16Le(): Result<UShort> = tryReadFixedSize(this, 2).map { bytesToShortLe(it).toUShort() }

    /** Gets an unsigned 16 bit integer from `self` in native-endian byte order. */
    public fun tryGetU16Ne(): Result<UShort> = if (NATIVE_IS_BIG_ENDIAN) tryGetU16() else tryGetU16Le()

    /** Gets a signed 16 bit integer from `self` in big-endian byte order. */
    public fun tryGetI16(): Result<Short> = tryReadFixedSize(this, 2).map { bytesToShortBe(it) }

    /** Gets a signed 16 bit integer from `self` in little-endian byte order. */
    public fun tryGetI16Le(): Result<Short> = tryReadFixedSize(this, 2).map { bytesToShortLe(it) }

    /** Gets a signed 16 bit integer from `self` in native-endian byte order. */
    public fun tryGetI16Ne(): Result<Short> = if (NATIVE_IS_BIG_ENDIAN) tryGetI16() else tryGetI16Le()

    /** Gets an unsigned 32 bit integer from `self` in big-endian byte order. */
    public fun tryGetU32(): Result<UInt> = tryReadFixedSize(this, 4).map { bytesToIntBe(it).toUInt() }

    /** Gets an unsigned 32 bit integer from `self` in little-endian byte order. */
    public fun tryGetU32Le(): Result<UInt> = tryReadFixedSize(this, 4).map { bytesToIntLe(it).toUInt() }

    /** Gets an unsigned 32 bit integer from `self` in native-endian byte order. */
    public fun tryGetU32Ne(): Result<UInt> = if (NATIVE_IS_BIG_ENDIAN) tryGetU32() else tryGetU32Le()

    /** Gets a signed 32 bit integer from `self` in big-endian byte order. */
    public fun tryGetI32(): Result<Int> = tryReadFixedSize(this, 4).map { bytesToIntBe(it) }

    /** Gets a signed 32 bit integer from `self` in little-endian byte order. */
    public fun tryGetI32Le(): Result<Int> = tryReadFixedSize(this, 4).map { bytesToIntLe(it) }

    /** Gets a signed 32 bit integer from `self` in native-endian byte order. */
    public fun tryGetI32Ne(): Result<Int> = if (NATIVE_IS_BIG_ENDIAN) tryGetI32() else tryGetI32Le()

    /** Gets an unsigned 64 bit integer from `self` in big-endian byte order. */
    public fun tryGetU64(): Result<ULong> = tryReadFixedSize(this, 8).map { bytesToLongBe(it).toULong() }

    /** Gets an unsigned 64 bit integer from `self` in little-endian byte order. */
    public fun tryGetU64Le(): Result<ULong> = tryReadFixedSize(this, 8).map { bytesToLongLe(it).toULong() }

    /** Gets an unsigned 64 bit integer from `self` in native-endian byte order. */
    public fun tryGetU64Ne(): Result<ULong> = if (NATIVE_IS_BIG_ENDIAN) tryGetU64() else tryGetU64Le()

    /** Gets a signed 64 bit integer from `self` in big-endian byte order. */
    public fun tryGetI64(): Result<Long> = tryReadFixedSize(this, 8).map { bytesToLongBe(it) }

    /** Gets a signed 64 bit integer from `self` in little-endian byte order. */
    public fun tryGetI64Le(): Result<Long> = tryReadFixedSize(this, 8).map { bytesToLongLe(it) }

    /** Gets a signed 64 bit integer from `self` in native-endian byte order. */
    public fun tryGetI64Ne(): Result<Long> = if (NATIVE_IS_BIG_ENDIAN) tryGetI64() else tryGetI64Le()

    /** Gets an unsigned 128 bit integer from `self` in big-endian byte order. */
    public fun tryGetU128(): Result<U128> = tryReadFixedSize(this, 16).map { U128.fromBigEndianBytes(it) }

    /** Gets an unsigned 128 bit integer from `self` in little-endian byte order. */
    public fun tryGetU128Le(): Result<U128> = tryReadFixedSize(this, 16).map { U128.fromLittleEndianBytes(it) }

    /** Gets an unsigned 128 bit integer from `self` in native-endian byte order. */
    public fun tryGetU128Ne(): Result<U128> = if (NATIVE_IS_BIG_ENDIAN) tryGetU128() else tryGetU128Le()

    /** Gets a signed 128 bit integer from `self` in big-endian byte order. */
    public fun tryGetI128(): Result<I128> = tryReadFixedSize(this, 16).map { I128.fromBigEndianBytes(it) }

    /** Gets a signed 128 bit integer from `self` in little-endian byte order. */
    public fun tryGetI128Le(): Result<I128> = tryReadFixedSize(this, 16).map { I128.fromLittleEndianBytes(it) }

    /** Gets a signed 128 bit integer from `self` in native-endian byte order. */
    public fun tryGetI128Ne(): Result<I128> = if (NATIVE_IS_BIG_ENDIAN) tryGetI128() else tryGetI128Le()

    /**
     * Gets an unsigned n-byte integer from `self` in big-endian byte order.
     *
     * Panics if `nbytes` is greater than 8.
     */
    public fun tryGetUint(nbytes: Int): Result<ULong> = tryReadUintBe(this, nbytes)

    /**
     * Gets an unsigned n-byte integer from `self` in little-endian byte order.
     *
     * Panics if `nbytes` is greater than 8.
     */
    public fun tryGetUintLe(nbytes: Int): Result<ULong> = tryReadUintLe(this, nbytes)

    /**
     * Gets an unsigned n-byte integer from `self` in native-endian byte order.
     *
     * Panics if `nbytes` is greater than 8.
     */
    public fun tryGetUintNe(nbytes: Int): Result<ULong> = if (NATIVE_IS_BIG_ENDIAN) tryGetUint(nbytes) else tryGetUintLe(nbytes)

    /**
     * Gets a signed n-byte integer from `self` in big-endian byte order.
     *
     * Panics if `nbytes` is greater than 8.
     */
    public fun tryGetInt(nbytes: Int): Result<Long> = tryGetUint(nbytes).map { signExtend(it, nbytes) }

    /**
     * Gets a signed n-byte integer from `self` in little-endian byte order.
     *
     * Panics if `nbytes` is greater than 8.
     */
    public fun tryGetIntLe(nbytes: Int): Result<Long> = tryGetUintLe(nbytes).map { signExtend(it, nbytes) }

    /**
     * Gets a signed n-byte integer from `self` in native-endian byte order.
     *
     * Panics if `nbytes` is greater than 8.
     */
    public fun tryGetIntNe(nbytes: Int): Result<Long> = if (NATIVE_IS_BIG_ENDIAN) tryGetInt(nbytes) else tryGetIntLe(nbytes)

    /** Gets an IEEE754 single-precision (4 bytes) floating point number in big-endian byte order. */
    public fun tryGetF32(): Result<Float> = tryGetU32().map { Float.fromBits(it.toInt()) }

    /** Gets an IEEE754 single-precision (4 bytes) floating point number in little-endian byte order. */
    public fun tryGetF32Le(): Result<Float> = tryGetU32Le().map { Float.fromBits(it.toInt()) }

    /** Gets an IEEE754 single-precision (4 bytes) floating point number in native-endian byte order. */
    public fun tryGetF32Ne(): Result<Float> = tryGetU32Ne().map { Float.fromBits(it.toInt()) }

    /** Gets an IEEE754 double-precision (8 bytes) floating point number in big-endian byte order. */
    public fun tryGetF64(): Result<Double> = tryGetU64().map { Double.fromBits(it.toLong()) }

    /** Gets an IEEE754 double-precision (8 bytes) floating point number in little-endian byte order. */
    public fun tryGetF64Le(): Result<Double> = tryGetU64Le().map { Double.fromBits(it.toLong()) }

    /** Gets an IEEE754 double-precision (8 bytes) floating point number in native-endian byte order. */
    public fun tryGetF64Ne(): Result<Double> = tryGetU64Ne().map { Double.fromBits(it.toLong()) }

    /**
     * Consumes `len` bytes inside self and returns new instance of [Bytes]
     * with this data.
     *
     * This function may be optimized by the underlying type to avoid actual
     * copies. For example, the [Bytes] implementation will do a shallow copy
     * (ref-count increment).
     *
     * # Panics
     *
     * This function panics if `len > remaining()`.
     */
    public fun copyToBytes(len: Int): Bytes {
        if (remaining() < len) {
            panicAdvance(TryGetError(len, remaining()))
        }
        val out = ByteArray(len)
        copyToSlice(out)
        return Bytes.from(out)
    }

    /**
     * Creates an adaptor which will read at most `limit` bytes from `self`.
     *
     * This function returns a new instance of [Buf] which will read at most
     * `limit` bytes.
     *
     * # Examples
     *
     * ```
     * val buf = ByteArrayBuf("hello world".encodeToByteArray()).take(5)
     * check(buf.copyToBytes(5).asSlice().contentEquals("hello".encodeToByteArray()))
     * ```
     */
    public fun take(limit: Int): Take = Take(this, limit)

    /**
     * Creates an adaptor which will chain this buffer with another.
     *
     * The returned [Buf] instance will first consume all bytes from `self`.
     * Afterwards the output is equivalent to the output of `next`.
     *
     * # Examples
     *
     * ```
     * val chain = ByteArrayBuf("hello ".encodeToByteArray())
     *     .chain(ByteArrayBuf("world".encodeToByteArray()))
     *
     * val full = chain.copyToBytes(11)
     * check(full.chunk().contentEquals("hello world".encodeToByteArray()))
     * ```
     */
    public fun chain(next: Buf): Chain = Chain(this, next)

    /**
     * Creates an adaptor which exposes the [Buf] as a stream of bytes.
     *
     * # Examples
     *
     * ```
     * val buf = Bytes.from("hello world")
     * val reader = buf.reader()
     * val dst = ByteArray(1024)
     * val num = reader.read(dst)
     * check(num == 11)
     * ```
     */
    public fun reader(): Reader = Reader(this)
}

/**
 * A [Buf] backed by a [ByteArray]. This is the canonical analog of `impl Buf for &[u8]` in
 * upstream Rust: a cursored view over a contiguous slice of bytes that advances on read.
 *
 * The cursor starts at `0` and is advanced by every read. The instance does not own its backing
 * array — successive calls to [chunk] return a *copy* of the unread tail to preserve immutability
 * at the API boundary, mirroring how `&[u8]` decays to a fresh slice in Rust.
 */
public class ByteArrayBuf(
    private val data: ByteArray,
) : Buf {
    private var cursor: Int = 0

    override fun remaining(): Int = data.size - cursor

    override fun chunk(): ByteArray = data.copyOfRange(cursor, data.size)

    override fun advance(cnt: Int) {
        if (data.size - cursor < cnt) {
            panicAdvance(TryGetError(cnt, data.size - cursor))
        }
        cursor += cnt
    }

    override fun copyToSlice(dst: ByteArray) {
        if (data.size - cursor < dst.size) {
            panicAdvance(TryGetError(dst.size, data.size - cursor))
        }
        data.copyInto(dst, 0, cursor, cursor + dst.size)
        cursor += dst.size
    }
}
