// port-lint: source buf/writer.rs
package io.github.kotlinmania.bytes.buf

/**
 * A [BufMut] adapter which exposes the inner value as a stream of bytes for
 * sequential write access.
 *
 * Upstream Rust implements `std::io::Write` here so callers can plug a
 * [BufMut] into anything that consumes the standard writer trait. The Kotlin
 * port surfaces the same operations as plain methods because Kotlin
 * Multiplatform has no analog of `std::io::Write` in commonMain. The result is
 * a small, infallible facade with the same write-then-advance semantics as
 * the upstream impl.
 *
 * This struct is generally created by calling [BufMut.writer] on [BufMut].
 */
public class Writer(
    private var buf: BufMut,
) {
    public companion object {
        /** Creates a new `Writer` adapter. Mirrors upstream `writer::new`. */
        public fun new(buf: BufMut): Writer = Writer(buf)
    }


    /**
     * Gets a reference to the underlying [BufMut].
     *
     * It is inadvisable to directly write to the underlying [BufMut].
     */
    public fun getRef(): BufMut = buf

    /**
     * Gets a mutable reference to the underlying [BufMut].
     *
     * It is inadvisable to directly write to the underlying [BufMut].
     */
    public fun getMut(): BufMut = buf

    /**
     * Consumes this `Writer`, returning the underlying value.
     */
    public fun intoInner(): BufMut = buf

    /**
     * Writes bytes from [src] into this writer, advancing the inner [BufMut]
     * and returning the number of bytes written. The return value is always
     * `min(buf.remainingMut(), src.size)`, matching the upstream
     * `io::Write::write` contract.
     */
    public fun write(src: ByteArray): Int {
        val n = minOf(buf.remainingMut(), src.size)
        if (n == src.size) {
            buf.putSlice(src)
        } else if (n > 0) {
            buf.putSlice(src.copyOfRange(0, n))
        }
        return n
    }

    /**
     * No-op flush. Matches upstream `io::Write::flush` for in-memory buffers.
     */
    public fun flush() {
        // The inner buffer is in-memory; no flushing required.
    }
}
