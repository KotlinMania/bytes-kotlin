// port-lint: source buf/take.rs
package io.github.kotlinmania.bytes.buf

import io.github.kotlinmania.bytes.Bytes

/**
 * A [Buf] adapter which limits the bytes read from an underlying buffer.
 *
 * This struct is generally created by calling [Buf.take] on [Buf]. See
 * documentation of [Buf.take] for more details.
 */
public class Take(
    private var inner: Buf,
    private var limitValue: Int,
) : Buf {
    public companion object {
        /** Creates a new `Take` adapter. Mirrors upstream `take::new`. */
        public fun new(inner: Buf, limit: Int): Take = Take(inner, limit)
    }


    /**
     * Consumes this `Take`, returning the underlying value.
     *
     * In Kotlin the inner [Buf] is shared rather than moved out; the caller may continue to read
     * from it after this call returns. Mirrors the upstream consume-and-return semantics.
     *
     * # Examples
     *
     * ```
     * val take = ByteArrayBuf("hello world".encodeToByteArray()).take(2)
     * val dst = ByteArray(2)
     * take.copyToSlice(dst)
     * check(dst.contentEquals("he".encodeToByteArray()))
     *
     * val recovered = take.intoInner()
     * val rest = ByteArray(9)
     * recovered.copyToSlice(rest)
     * check(rest.contentEquals("llo world".encodeToByteArray()))
     * ```
     */
    public fun intoInner(): Buf = inner

    /**
     * Gets a reference to the underlying [Buf].
     *
     * It is inadvisable to directly read from the underlying [Buf].
     *
     * # Examples
     *
     * ```
     * val take = ByteArrayBuf("hello world".encodeToByteArray()).take(2)
     * check(take.getRef().remaining() == 11)
     * ```
     */
    public fun getRef(): Buf = inner

    /**
     * Gets a mutable reference to the underlying [Buf].
     *
     * It is inadvisable to directly read from the underlying [Buf].
     */
    public fun getMut(): Buf = inner

    /**
     * Returns the maximum number of bytes that can be read.
     *
     * # Note
     *
     * If the inner [Buf] has fewer bytes than indicated by this method then
     * that is the actual number of available bytes.
     *
     * # Examples
     *
     * ```
     * val take = ByteArrayBuf("hello world".encodeToByteArray()).take(2)
     * check(take.limit() == 2)
     * check(take.getU8() == 0x68u.toUByte())
     * check(take.limit() == 1)
     * ```
     */
    public fun limit(): Int = limitValue

    /**
     * Sets the maximum number of bytes that can be read.
     *
     * # Note
     *
     * If the inner [Buf] has fewer bytes than `lim` then that is the actual
     * number of available bytes.
     *
     * # Examples
     *
     * ```
     * val take = ByteArrayBuf("hello world".encodeToByteArray()).take(2)
     * val dst = ByteArray(2)
     * take.copyToSlice(dst)
     * check(dst.contentEquals("he".encodeToByteArray()))
     *
     * take.setLimit(3)
     * val next = ByteArray(3)
     * take.copyToSlice(next)
     * check(next.contentEquals("llo".encodeToByteArray()))
     * ```
     */
    public fun setLimit(lim: Int) {
        limitValue = lim
    }

    override fun remaining(): Int = minOf(inner.remaining(), limitValue)

    override fun chunk(): ByteArray {
        val bytes = inner.chunk()
        val cap = minOf(bytes.size, limitValue)
        return if (cap == bytes.size) bytes else bytes.copyOfRange(0, cap)
    }

    override fun advance(cnt: Int) {
        check(cnt <= limitValue) { "advance past limit: $cnt > $limitValue" }
        inner.advance(cnt)
        limitValue -= cnt
    }

    override fun copyToBytes(len: Int): Bytes {
        check(len <= remaining()) { "`len` greater than remaining" }
        val result = inner.copyToBytes(len)
        limitValue -= len
        return result
    }
}
