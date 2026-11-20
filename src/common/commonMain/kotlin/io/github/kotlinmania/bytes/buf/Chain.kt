// port-lint: source buf/chain.rs
package io.github.kotlinmania.bytes.buf

import io.github.kotlinmania.bytes.Bytes

/**
 * A `Chain` sequences two buffers.
 *
 * `Chain` is an adapter that links two underlying buffers and provides a
 * continuous view across both buffers. It is able to sequence either
 * immutable buffers ([Buf] values) or mutable buffers ([BufMut] values).
 *
 * Whether the chain behaves as a [Buf], a [BufMut], or both depends on which
 * trait implementations the two underlying values satisfy. In Kotlin both
 * conformances are exposed unconditionally; calling [chunkMut] or
 * [advanceMut] on a chain whose halves are not [BufMut] will fail with a
 * cast error at runtime. This mirrors how upstream Rust's `impl<T, U> BufMut
 * for Chain<T, U>` is gated by `T: BufMut, U: BufMut` at compile time.
 *
 * This struct is generally created by calling [Buf.chain] / [BufMut.chainMut].
 *
 * # Examples
 *
 * ```
 * val chain = ByteArrayBuf("hello ".encodeToByteArray())
 *     .chain(ByteArrayBuf("world".encodeToByteArray()))
 *
 * val full = chain.copyToBytes(11)
 * check(full.asSlice().contentEquals("hello world".encodeToByteArray()))
 * ```
 */
public class Chain(
    private val a: Any,
    private val b: Any,
) : Buf,
    BufMut {
    public companion object {
        /** Creates a new `Chain` sequencing the provided values. Mirrors upstream `Chain::new`. */
        public fun new(a: Any, b: Any): Chain = Chain(a, b)
    }

    /** Gets a reference to the first underlying value. */
    public fun firstRef(): Any = a

    /** Gets a mutable reference to the first underlying value. */
    public fun firstMut(): Any = a

    /** Gets a reference to the last underlying value. */
    public fun lastRef(): Any = b

    /** Gets a mutable reference to the last underlying value. */
    public fun lastMut(): Any = b

    /** Consumes this `Chain`, returning the underlying values. */
    public fun intoInner(): Pair<Any, Any> = a to b

    private fun firstBuf(): Buf = a as? Buf ?: error("Chain.first is not a Buf")

    private fun lastBuf(): Buf = b as? Buf ?: error("Chain.last is not a Buf")

    private fun firstBufMut(): BufMut = a as? BufMut ?: error("Chain.first is not a BufMut")

    private fun lastBufMut(): BufMut = b as? BufMut ?: error("Chain.last is not a BufMut")

    override fun remaining(): Int {
        val sum = firstBuf().remaining().toLong() + lastBuf().remaining().toLong()
        return if (sum > Int.MAX_VALUE.toLong()) Int.MAX_VALUE else sum.toInt()
    }

    override fun chunk(): ByteArray =
        if (firstBuf().hasRemaining()) firstBuf().chunk() else lastBuf().chunk()

    override fun advance(cnt: Int) {
        var remaining = cnt
        val first = firstBuf()
        val aRem = first.remaining()
        if (aRem != 0) {
            if (aRem >= remaining) {
                first.advance(remaining)
                return
            }
            first.advance(aRem)
            remaining -= aRem
        }
        lastBuf().advance(remaining)
    }

    override fun copyToBytes(len: Int): Bytes {
        val first = firstBuf()
        val aRem = first.remaining()
        if (aRem >= len) {
            return first.copyToBytes(len)
        }
        if (aRem == 0) {
            return lastBuf().copyToBytes(len)
        }
        val last = lastBuf()
        check(len - aRem <= last.remaining()) { "`len` greater than remaining" }
        val result = ByteArray(len)
        val head = ByteArray(aRem)
        first.copyToSlice(head)
        head.copyInto(result, 0, 0, aRem)
        val tail = ByteArray(len - aRem)
        last.copyToSlice(tail)
        tail.copyInto(result, aRem, 0, tail.size)
        return Bytes.from(result)
    }

    override fun remainingMut(): Int {
        val sum = firstBufMut().remainingMut().toLong() + lastBufMut().remainingMut().toLong()
        return if (sum > Int.MAX_VALUE.toLong()) Int.MAX_VALUE else sum.toInt()
    }

    override fun chunkMut(): UninitSlice =
        if (firstBufMut().hasRemainingMut()) firstBufMut().chunkMut() else lastBufMut().chunkMut()

    override fun advanceMut(cnt: Int) {
        var remaining = cnt
        val first = firstBufMut()
        val aRem = first.remainingMut()
        if (aRem != 0) {
            if (aRem >= remaining) {
                first.advanceMut(remaining)
                return
            }
            first.advanceMut(aRem)
            remaining -= aRem
        }
        lastBufMut().advanceMut(remaining)
    }
}
