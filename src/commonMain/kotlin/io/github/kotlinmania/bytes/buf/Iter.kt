// port-lint: source buf/iter.rs
package io.github.kotlinmania.bytes.buf

/**
 * Iterator over the bytes contained by the buffer.
 *
 * # Examples
 *
 * Basic usage:
 *
 * ```
 * val buf = Bytes.from("abc")
 * val iter = IntoIter(ByteArrayBuf(buf.asSlice()))
 *
 * check(iter.next() == 0x61.toByte())
 * check(iter.next() == 0x62.toByte())
 * check(iter.next() == 0x63.toByte())
 * check(!iter.hasNext())
 * ```
 */
public class IntoIter<T : Buf>(
    private var inner: T,
) : Iterator<Byte> {
    public companion object {
        /** Creates an iterator over the bytes contained by the buffer. Mirrors upstream `IntoIter::new`. */
        public fun <T : Buf> new(inner: T): IntoIter<T> = IntoIter(inner)
    }

    /**
     * Consumes this `IntoIter`, returning the underlying value.
     *
     * # Examples
     *
     * ```
     * val buf = ByteArrayBuf("abc".encodeToByteArray())
     * val iter = IntoIter(buf)
     *
     * check(iter.next() == 0x61.toByte())
     *
     * val recovered = iter.intoInner()
     * check(recovered.remaining() == 2)
     * ```
     */
    public fun intoInner(): T = inner

    /**
     * Gets a reference to the underlying [Buf].
     *
     * It is inadvisable to directly read from the underlying [Buf].
     */
    public fun getRef(): T = inner

    /**
     * Gets a mutable reference to the underlying [Buf].
     *
     * In Kotlin the same instance is returned and may be mutated by the caller. The upstream
     * Rust signature `&mut T` is collapsed because Kotlin lacks distinct shared and exclusive
     * reference forms.
     */
    public fun getMut(): T = inner

    override fun hasNext(): Boolean = inner.hasRemaining()

    override fun next(): Byte {
        if (!inner.hasRemaining()) {
            throw NoSuchElementException("IntoIter exhausted")
        }
        val b = inner.chunk()[0]
        inner.advance(1)
        return b
    }

    /**
     * Returns the number of bytes still in the iterator. Mirrors the upstream
     * `ExactSizeIterator` impl: the lower and upper hint are both equal to `inner.remaining()`.
     */
    public fun sizeHint(): Pair<Int, Int> {
        val rem = inner.remaining()
        return rem to rem
    }
}
