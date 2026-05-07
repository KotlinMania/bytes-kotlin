// port-lint: source buf/limit.rs
package io.github.kotlinmania.bytes.buf

/**
 * A [BufMut] adapter which limits the amount of bytes that can be written to
 * an underlying buffer.
 */
public class Limit(
    private var inner: BufMut,
    private var limitValue: Int,
) : BufMut {
    public companion object {
        /** Creates a new `Limit` adapter. Mirrors upstream `limit::new`. */
        public fun new(inner: BufMut, limit: Int): Limit = Limit(inner, limit)
    }


    /** Consumes this `Limit`, returning the underlying value. */
    public fun intoInner(): BufMut = inner

    /**
     * Gets a reference to the underlying [BufMut].
     *
     * It is inadvisable to directly write to the underlying [BufMut].
     */
    public fun getRef(): BufMut = inner

    /**
     * Gets a mutable reference to the underlying [BufMut].
     *
     * It is inadvisable to directly write to the underlying [BufMut].
     */
    public fun getMut(): BufMut = inner

    /**
     * Returns the maximum number of bytes that can be written.
     *
     * # Note
     *
     * If the inner [BufMut] has fewer bytes than indicated by this method then
     * that is the actual number of available bytes.
     */
    public fun limit(): Int = limitValue

    /**
     * Sets the maximum number of bytes that can be written.
     *
     * # Note
     *
     * If the inner [BufMut] has fewer bytes than `lim` then that is the actual
     * number of available bytes.
     */
    public fun setLimit(lim: Int) {
        limitValue = lim
    }

    override fun remainingMut(): Int = minOf(inner.remainingMut(), limitValue)

    override fun chunkMut(): UninitSlice {
        val bytes = inner.chunkMut()
        val end = minOf(bytes.len(), limitValue)
        return if (end == bytes.len()) bytes else bytes[0 until end]
    }

    override fun advanceMut(cnt: Int) {
        check(cnt <= limitValue) { "advance past limit: $cnt > $limitValue" }
        inner.advanceMut(cnt)
        limitValue -= cnt
    }
}
