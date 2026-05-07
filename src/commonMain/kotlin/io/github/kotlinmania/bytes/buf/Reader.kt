// port-lint: source buf/reader.rs
package io.github.kotlinmania.bytes.buf

/**
 * A [Buf] adapter which exposes the inner value as a stream of bytes.
 *
 * Upstream Rust implements `std::io::Read` and `std::io::BufRead` here so callers can plug a
 * [Buf] into anything that consumes the standard reader trait. The Kotlin port surfaces the
 * same operations as plain methods because Kotlin Multiplatform has no analog of
 * `std::io::Read` in commonMain. The result is a small, infallible facade with the same
 * read-then-advance semantics as the upstream impl.
 *
 * This struct is generally created by calling [Buf.reader] on [Buf]. See
 * documentation of [Buf.reader] for more details.
 */
public class Reader(
    private var buf: Buf,
) {
    public companion object {
        /** Creates a new `Reader` adapter. Mirrors upstream `reader::new`. */
        public fun new(buf: Buf): Reader = Reader(buf)
    }


    /**
     * Gets a reference to the underlying [Buf].
     *
     * It is inadvisable to directly read from the underlying [Buf].
     *
     * # Examples
     *
     * ```
     * val reader = ByteArrayBuf("hello world".encodeToByteArray()).reader()
     * check(reader.getRef().remaining() == 11)
     * ```
     */
    public fun getRef(): Buf = buf

    /**
     * Gets a mutable reference to the underlying [Buf].
     *
     * It is inadvisable to directly read from the underlying [Buf].
     */
    public fun getMut(): Buf = buf

    /**
     * Consumes this `Reader`, returning the underlying value.
     *
     * # Examples
     *
     * ```
     * val reader = ByteArrayBuf("hello world".encodeToByteArray()).reader()
     * val sink = ByteArray(11)
     * reader.read(sink)
     * check(reader.intoInner().remaining() == 0)
     * ```
     */
    public fun intoInner(): Buf = buf

    /**
     * Reads bytes from this reader into [dst], advancing the inner [Buf] and returning the
     * number of bytes consumed. The return value is always `min(buf.remaining(), dst.size)`,
     * matching the upstream `io::Read::read` contract.
     */
    public fun read(dst: ByteArray): Int {
        val len = minOf(buf.remaining(), dst.size)
        if (len == 0) {
            return 0
        }
        val slice = if (len == dst.size) dst else ByteArray(len)
        buf.copyToSlice(slice)
        if (slice !== dst) {
            slice.copyInto(dst, 0, 0, len)
        }
        return len
    }

    /**
     * Returns a slice of the inner buffer's currently visible bytes without advancing the
     * cursor. Mirrors the upstream buffered-read fill behavior on the `BufRead` trait.
     */
    public fun fillBuf(): ByteArray = buf.chunk()

    /**
     * Advances the inner buffer by [amt] bytes. Mirrors the upstream `io::BufRead::consume`
     * impl.
     */
    public fun consume(amt: Int) {
        buf.advance(amt)
    }
}
