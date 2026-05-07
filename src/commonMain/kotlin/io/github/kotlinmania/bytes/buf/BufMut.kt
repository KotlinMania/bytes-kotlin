// port-lint: source buf/buf_mut.rs
package io.github.kotlinmania.bytes.buf

import io.github.kotlinmania.bytes.I128
import io.github.kotlinmania.bytes.TryGetError
import io.github.kotlinmania.bytes.U128
import io.github.kotlinmania.bytes.panicAdvance
import io.github.kotlinmania.bytes.panicDoesNotFit

// === Endianness model ===
// Identical to Buf.kt: every supported KMP target is little-endian, so the `*Ne` (native-endian)
// methods on BufMut delegate to the little-endian variant. The constant is duplicated rather
// than shared to keep each file self-contained per the workspace's one-Rust-file → one-Kotlin-file
// rule.
private const val NATIVE_IS_BIG_ENDIAN: Boolean = false

// === Byte-encoding helpers (correspond to inline fixed-size writes in upstream put_* methods).

private fun shortToBytesBe(n: Short, dst: ByteArray, offset: Int) {
    val value = n.toInt() and 0xffff
    dst[offset] = ((value ushr 8) and 0xff).toByte()
    dst[offset + 1] = (value and 0xff).toByte()
}

private fun shortToBytesLe(n: Short, dst: ByteArray, offset: Int) {
    val value = n.toInt() and 0xffff
    dst[offset] = (value and 0xff).toByte()
    dst[offset + 1] = ((value ushr 8) and 0xff).toByte()
}

private fun intToBytesBe(n: Int, dst: ByteArray, offset: Int) {
    dst[offset] = ((n ushr 24) and 0xff).toByte()
    dst[offset + 1] = ((n ushr 16) and 0xff).toByte()
    dst[offset + 2] = ((n ushr 8) and 0xff).toByte()
    dst[offset + 3] = (n and 0xff).toByte()
}

private fun intToBytesLe(n: Int, dst: ByteArray, offset: Int) {
    dst[offset] = (n and 0xff).toByte()
    dst[offset + 1] = ((n ushr 8) and 0xff).toByte()
    dst[offset + 2] = ((n ushr 16) and 0xff).toByte()
    dst[offset + 3] = ((n ushr 24) and 0xff).toByte()
}

private fun longToBytesBe(n: Long, dst: ByteArray, offset: Int) {
    for (index in 0 until 8) {
        dst[offset + index] = ((n ushr ((7 - index) * 8)) and 0xff).toByte()
    }
}

private fun longToBytesLe(n: Long, dst: ByteArray, offset: Int) {
    for (index in 0 until 8) {
        dst[offset + index] = ((n ushr (index * 8)) and 0xff).toByte()
    }
}

private fun writeFixedSize(buf: BufMut, src: ByteArray) {
    val size = src.size
    if (buf.remainingMut() < size) {
        panicAdvance(TryGetError(size, buf.remainingMut()))
    }
    var written = 0
    while (written < size) {
        val dst = buf.chunkMut()
        val cnt = minOf(size - written, dst.len())
        for (index in 0 until cnt) {
            dst.writeByte(index, src[written + index])
        }
        buf.advanceMut(cnt)
        written += cnt
    }
}

/**
 * A trait for values that provide sequential write access to bytes.
 *
 * Write bytes to a buffer.
 *
 * A buffer stores bytes in memory such that write operations are infallible.
 * The underlying storage may or may not be in contiguous memory. A `BufMut`
 * value is a cursor into the buffer. Writing to `BufMut` advances the cursor
 * position.
 *
 * The simplest `BufMut` is a [ByteArrayBufMut] wrapping a [ByteArray].
 *
 * ```
 * val dst = ByteArray(11)
 * val buf = ByteArrayBufMut(dst)
 *
 * buf.putSlice("hello world".encodeToByteArray())
 * check(dst.contentEquals("hello world".encodeToByteArray()))
 * ```
 */
public interface BufMut {
    /**
     * Returns the number of bytes that can be written from the current
     * position until the end of the buffer is reached.
     *
     * This value is greater than or equal to the length of the slice returned
     * by [chunkMut].
     *
     * Writing to a `BufMut` may involve allocating more memory on the fly.
     * Implementations may fail before reaching the number of bytes indicated
     * by this method if they encounter an allocation failure.
     *
     * # Implementer notes
     *
     * Implementations of `remainingMut` should ensure that the return value
     * does not change unless a call is made to [advanceMut] or any other
     * function that is documented to change the `BufMut`'s current position.
     *
     * # Note
     *
     * `remainingMut` may return a value smaller than the actual available
     * space.
     */
    public fun remainingMut(): Int

    /**
     * Advance the internal cursor of the [BufMut].
     *
     * The next call to [chunkMut] will return a slice starting `cnt` bytes
     * further into the underlying buffer.
     *
     * # Safety
     *
     * The caller must ensure that the next `cnt` bytes of [chunkMut] are
     * initialized. The Kotlin port preserves this contract — [advanceMut]
     * remains the cursor-advancing primitive that callers couple with
     * [chunkMut] writes. Kotlin lacks the upstream Rust safety qualifier on
     * trait methods, so the signature is plain. Misuse manifests as a logic
     * bug rather than an undefined-behavior crash.
     *
     * # Panics
     *
     * This function **may** panic if `cnt > remainingMut()`.
     *
     * # Implementer notes
     *
     * It is recommended for implementations of `advanceMut` to panic if
     * `cnt > remainingMut()`. If the implementation does not panic, the call
     * must behave as if `cnt == remainingMut()`.
     *
     * A call with `cnt == 0` should never panic and be a no-op.
     */
    public fun advanceMut(cnt: Int)

    /**
     * Returns true if there is space in `self` for more bytes.
     *
     * This is equivalent to `remainingMut() != 0`.
     */
    public fun hasRemainingMut(): Boolean = remainingMut() > 0

    /**
     * Returns a mutable slice starting at the current `BufMut` position and of
     * length between 0 and `remainingMut()`. Note that this *can* be shorter
     * than the whole remainder of the buffer (this allows non-continuous
     * implementation).
     *
     * This is a lower level function. Most operations are done with other
     * functions.
     *
     * The returned byte slice may represent uninitialized memory.
     *
     * # Implementer notes
     *
     * This function should never panic. `chunkMut()` should return an empty
     * slice **if and only if** `remainingMut()` returns 0. In other words,
     * `chunkMut()` returning an empty slice implies that `remainingMut()` will
     * return 0 and `remainingMut()` returning 0 implies that `chunkMut()` will
     * return an empty slice.
     *
     * This function may trigger an out-of-memory abort if it tries to allocate
     * memory and fails to do so.
     */
    public fun chunkMut(): UninitSlice

    /**
     * Transfer bytes into `self` from `src` and advance the cursor by the
     * number of bytes written.
     *
     * # Examples
     *
     * ```
     * val dst = ByteArray(11)
     * val buf = ByteArrayBufMut(dst)
     *
     * buf.putU8(0x68u.toUByte())
     * buf.put(ByteArrayBuf("ello".encodeToByteArray()))
     * buf.put(ByteArrayBuf(" world".encodeToByteArray()))
     *
     * check(dst.contentEquals("hello world".encodeToByteArray()))
     * ```
     *
     * # Panics
     *
     * Panics if `self` does not have enough capacity to contain `src`.
     */
    public fun put(src: Buf) {
        if (remainingMut() < src.remaining()) {
            panicAdvance(TryGetError(src.remaining(), remainingMut()))
        }
        while (src.hasRemaining()) {
            val s = src.chunk()
            val d = chunkMut()
            val cnt = minOf(s.size, d.len())
            for (index in 0 until cnt) {
                d.writeByte(index, s[index])
            }
            advanceMut(cnt)
            src.advance(cnt)
        }
    }

    /**
     * Transfer bytes into `self` from `src` and advance the cursor by the
     * number of bytes written.
     *
     * `self` must have enough remaining capacity to contain all of `src`.
     */
    public fun putSlice(src: ByteArray) {
        if (remainingMut() < src.size) {
            panicAdvance(TryGetError(src.size, remainingMut()))
        }
        var consumed = 0
        while (consumed < src.size) {
            val dst = chunkMut()
            val cnt = minOf(src.size - consumed, dst.len())
            for (index in 0 until cnt) {
                dst.writeByte(index, src[consumed + index])
            }
            advanceMut(cnt)
            consumed += cnt
        }
    }

    /**
     * Put `cnt` bytes `value` into `self`.
     *
     * Logically equivalent to calling `putU8(value)` `cnt` times, but may work
     * faster.
     *
     * `self` must have at least `cnt` remaining capacity.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining capacity in
     * `self`.
     */
    public fun putBytes(value: Byte, cnt: Int) {
        if (remainingMut() < cnt) {
            panicAdvance(TryGetError(cnt, remainingMut()))
        }
        var written = 0
        while (written < cnt) {
            val dst = chunkMut()
            val step = minOf(cnt - written, dst.len())
            for (index in 0 until step) {
                dst.writeByte(index, value)
            }
            advanceMut(step)
            written += step
        }
    }

    /**
     * Writes an unsigned 8 bit integer to `self`.
     *
     * The current position is advanced by 1.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining capacity in `self`.
     */
    public fun putU8(n: UByte) {
        putBytes(n.toByte(), 1)
    }

    /**
     * Writes a signed 8 bit integer to `self`.
     *
     * The current position is advanced by 1.
     *
     * # Panics
     *
     * This function panics if there is not enough remaining capacity in `self`.
     */
    public fun putI8(n: Byte) {
        putBytes(n, 1)
    }

    /** Writes an unsigned 16 bit integer to `self` in big-endian byte order. */
    public fun putU16(n: UShort) {
        val buf = ByteArray(2)
        shortToBytesBe(n.toShort(), buf, 0)
        writeFixedSize(this, buf)
    }

    /** Writes an unsigned 16 bit integer to `self` in little-endian byte order. */
    public fun putU16Le(n: UShort) {
        val buf = ByteArray(2)
        shortToBytesLe(n.toShort(), buf, 0)
        writeFixedSize(this, buf)
    }

    /** Writes an unsigned 16 bit integer to `self` in native-endian byte order. */
    public fun putU16Ne(n: UShort) {
        if (NATIVE_IS_BIG_ENDIAN) putU16(n) else putU16Le(n)
    }

    /** Writes a signed 16 bit integer to `self` in big-endian byte order. */
    public fun putI16(n: Short) {
        val buf = ByteArray(2)
        shortToBytesBe(n, buf, 0)
        writeFixedSize(this, buf)
    }

    /** Writes a signed 16 bit integer to `self` in little-endian byte order. */
    public fun putI16Le(n: Short) {
        val buf = ByteArray(2)
        shortToBytesLe(n, buf, 0)
        writeFixedSize(this, buf)
    }

    /** Writes a signed 16 bit integer to `self` in native-endian byte order. */
    public fun putI16Ne(n: Short) {
        if (NATIVE_IS_BIG_ENDIAN) putI16(n) else putI16Le(n)
    }

    /** Writes an unsigned 32 bit integer to `self` in big-endian byte order. */
    public fun putU32(n: UInt) {
        val buf = ByteArray(4)
        intToBytesBe(n.toInt(), buf, 0)
        writeFixedSize(this, buf)
    }

    /** Writes an unsigned 32 bit integer to `self` in little-endian byte order. */
    public fun putU32Le(n: UInt) {
        val buf = ByteArray(4)
        intToBytesLe(n.toInt(), buf, 0)
        writeFixedSize(this, buf)
    }

    /** Writes an unsigned 32 bit integer to `self` in native-endian byte order. */
    public fun putU32Ne(n: UInt) {
        if (NATIVE_IS_BIG_ENDIAN) putU32(n) else putU32Le(n)
    }

    /** Writes a signed 32 bit integer to `self` in big-endian byte order. */
    public fun putI32(n: Int) {
        val buf = ByteArray(4)
        intToBytesBe(n, buf, 0)
        writeFixedSize(this, buf)
    }

    /** Writes a signed 32 bit integer to `self` in little-endian byte order. */
    public fun putI32Le(n: Int) {
        val buf = ByteArray(4)
        intToBytesLe(n, buf, 0)
        writeFixedSize(this, buf)
    }

    /** Writes a signed 32 bit integer to `self` in native-endian byte order. */
    public fun putI32Ne(n: Int) {
        if (NATIVE_IS_BIG_ENDIAN) putI32(n) else putI32Le(n)
    }

    /** Writes an unsigned 64 bit integer to `self` in big-endian byte order. */
    public fun putU64(n: ULong) {
        val buf = ByteArray(8)
        longToBytesBe(n.toLong(), buf, 0)
        writeFixedSize(this, buf)
    }

    /** Writes an unsigned 64 bit integer to `self` in little-endian byte order. */
    public fun putU64Le(n: ULong) {
        val buf = ByteArray(8)
        longToBytesLe(n.toLong(), buf, 0)
        writeFixedSize(this, buf)
    }

    /** Writes an unsigned 64 bit integer to `self` in native-endian byte order. */
    public fun putU64Ne(n: ULong) {
        if (NATIVE_IS_BIG_ENDIAN) putU64(n) else putU64Le(n)
    }

    /** Writes a signed 64 bit integer to `self` in big-endian byte order. */
    public fun putI64(n: Long) {
        val buf = ByteArray(8)
        longToBytesBe(n, buf, 0)
        writeFixedSize(this, buf)
    }

    /** Writes a signed 64 bit integer to `self` in little-endian byte order. */
    public fun putI64Le(n: Long) {
        val buf = ByteArray(8)
        longToBytesLe(n, buf, 0)
        writeFixedSize(this, buf)
    }

    /** Writes a signed 64 bit integer to `self` in native-endian byte order. */
    public fun putI64Ne(n: Long) {
        if (NATIVE_IS_BIG_ENDIAN) putI64(n) else putI64Le(n)
    }

    /** Writes an unsigned 128 bit integer to `self` in big-endian byte order. */
    public fun putU128(n: U128) {
        writeFixedSize(this, n.toBigEndianBytes())
    }

    /** Writes an unsigned 128 bit integer to `self` in little-endian byte order. */
    public fun putU128Le(n: U128) {
        writeFixedSize(this, n.toLittleEndianBytes())
    }

    /** Writes an unsigned 128 bit integer to `self` in native-endian byte order. */
    public fun putU128Ne(n: U128) {
        if (NATIVE_IS_BIG_ENDIAN) putU128(n) else putU128Le(n)
    }

    /** Writes a signed 128 bit integer to `self` in big-endian byte order. */
    public fun putI128(n: I128) {
        writeFixedSize(this, n.toBigEndianBytes())
    }

    /** Writes a signed 128 bit integer to `self` in little-endian byte order. */
    public fun putI128Le(n: I128) {
        writeFixedSize(this, n.toLittleEndianBytes())
    }

    /** Writes a signed 128 bit integer to `self` in native-endian byte order. */
    public fun putI128Ne(n: I128) {
        if (NATIVE_IS_BIG_ENDIAN) putI128(n) else putI128Le(n)
    }

    /**
     * Writes an unsigned n-byte integer to `self` in big-endian byte order.
     *
     * # Panics
     *
     * Panics if there is not enough remaining capacity in `self`, or if
     * `nbytes` is greater than 8.
     */
    public fun putUint(n: ULong, nbytes: Int) {
        if (nbytes > 8) {
            panicDoesNotFit(8, nbytes)
        }
        if (remainingMut() < nbytes) {
            panicAdvance(TryGetError(nbytes, remainingMut()))
        }
        val full = ByteArray(8)
        longToBytesBe(n.toLong(), full, 0)
        writeFixedSize(this, full.copyOfRange(8 - nbytes, 8))
    }

    /**
     * Writes an unsigned n-byte integer to `self` in little-endian byte order.
     *
     * # Panics
     *
     * Panics if there is not enough remaining capacity in `self`, or if
     * `nbytes` is greater than 8.
     */
    public fun putUintLe(n: ULong, nbytes: Int) {
        if (nbytes > 8) {
            panicDoesNotFit(8, nbytes)
        }
        if (remainingMut() < nbytes) {
            panicAdvance(TryGetError(nbytes, remainingMut()))
        }
        val full = ByteArray(8)
        longToBytesLe(n.toLong(), full, 0)
        writeFixedSize(this, full.copyOfRange(0, nbytes))
    }

    /**
     * Writes an unsigned n-byte integer to `self` in native-endian byte order.
     *
     * # Panics
     *
     * Panics if there is not enough remaining capacity in `self`, or if
     * `nbytes` is greater than 8.
     */
    public fun putUintNe(n: ULong, nbytes: Int) {
        if (NATIVE_IS_BIG_ENDIAN) putUint(n, nbytes) else putUintLe(n, nbytes)
    }

    /**
     * Writes a signed n-byte integer to `self` in big-endian byte order.
     *
     * # Panics
     *
     * Panics if there is not enough remaining capacity in `self`, or if
     * `nbytes` is greater than 8.
     */
    public fun putInt(n: Long, nbytes: Int) {
        putUint(n.toULong(), nbytes)
    }

    /**
     * Writes a signed n-byte integer to `self` in little-endian byte order.
     *
     * # Panics
     *
     * Panics if there is not enough remaining capacity in `self`, or if
     * `nbytes` is greater than 8.
     */
    public fun putIntLe(n: Long, nbytes: Int) {
        putUintLe(n.toULong(), nbytes)
    }

    /**
     * Writes a signed n-byte integer to `self` in native-endian byte order.
     *
     * # Panics
     *
     * Panics if there is not enough remaining capacity in `self`, or if
     * `nbytes` is greater than 8.
     */
    public fun putIntNe(n: Long, nbytes: Int) {
        if (NATIVE_IS_BIG_ENDIAN) putInt(n, nbytes) else putIntLe(n, nbytes)
    }

    /**
     * Writes an IEEE754 single-precision (4 bytes) floating point number to
     * `self` in big-endian byte order.
     */
    public fun putF32(n: Float) {
        putU32(n.toRawBits().toUInt())
    }

    /**
     * Writes an IEEE754 single-precision (4 bytes) floating point number to
     * `self` in little-endian byte order.
     */
    public fun putF32Le(n: Float) {
        putU32Le(n.toRawBits().toUInt())
    }

    /**
     * Writes an IEEE754 single-precision (4 bytes) floating point number to
     * `self` in native-endian byte order.
     */
    public fun putF32Ne(n: Float) {
        putU32Ne(n.toRawBits().toUInt())
    }

    /**
     * Writes an IEEE754 double-precision (8 bytes) floating point number to
     * `self` in big-endian byte order.
     */
    public fun putF64(n: Double) {
        putU64(n.toRawBits().toULong())
    }

    /**
     * Writes an IEEE754 double-precision (8 bytes) floating point number to
     * `self` in little-endian byte order.
     */
    public fun putF64Le(n: Double) {
        putU64Le(n.toRawBits().toULong())
    }

    /**
     * Writes an IEEE754 double-precision (8 bytes) floating point number to
     * `self` in native-endian byte order.
     */
    public fun putF64Ne(n: Double) {
        putU64Ne(n.toRawBits().toULong())
    }

    /**
     * Creates an adaptor which can write at most `limit` bytes to `self`.
     *
     * # Examples
     *
     * ```
     * val dst = ByteArray(20)
     * val limited = ByteArrayBufMut(dst).limit(10)
     *
     * limited.putSlice("hello world".encodeToByteArray().copyOfRange(0, 10))
     * ```
     */
    public fun limit(limit: Int): Limit = Limit(this, limit)

    /**
     * Creates an adaptor which exposes the [BufMut] as a stream of bytes for
     * sequential write access.
     */
    public fun writer(): Writer = Writer(this)

    /**
     * Creates an adapter which will chain this buffer with another. The
     * returned [BufMut] instance will first fill `self` and only then write
     * into `next` once `self` is full.
     */
    public fun chainMut(next: BufMut): Chain = Chain(this, next)
}

/**
 * A [BufMut] backed by a [ByteArray]. This is the canonical analog of
 * `impl BufMut for &mut [u8]` in upstream Rust: a cursored, write-only view
 * over a contiguous slice of bytes that advances on write.
 *
 * The cursor starts at `0` and is advanced by every write. Allocation is not
 * performed; writes that would exceed [size] panic.
 */
public class ByteArrayBufMut(
    private val data: ByteArray,
) : BufMut {
    private var cursor: Int = 0

    /** Number of bytes in the backing array. */
    public val size: Int get() = data.size

    override fun remainingMut(): Int = data.size - cursor

    override fun chunkMut(): UninitSlice = UninitSlice.rangeRef(data, cursor, data.size)

    override fun advanceMut(cnt: Int) {
        if (data.size - cursor < cnt) {
            panicAdvance(TryGetError(cnt, data.size - cursor))
        }
        cursor += cnt
    }

    override fun putSlice(src: ByteArray) {
        if (data.size - cursor < src.size) {
            panicAdvance(TryGetError(src.size, data.size - cursor))
        }
        src.copyInto(data, cursor, 0, src.size)
        cursor += src.size
    }

    override fun putBytes(value: Byte, cnt: Int) {
        if (data.size - cursor < cnt) {
            panicAdvance(TryGetError(cnt, data.size - cursor))
        }
        for (index in 0 until cnt) {
            data[cursor + index] = value
        }
        cursor += cnt
    }
}
