// port-lint: source buf/vec_deque.rs
package io.github.kotlinmania.bytes.buf

/**
 * Adapter exposing a Kotlin [ArrayDeque] of [Byte] as a [Buf].
 *
 * The upstream Rust file provides a Buf conformance for VecDeque directly; in Kotlin
 * extension-style impls on stdlib types do not satisfy interface conformance, so the analog is
 * a small wrapper class that you construct with the bytes you want to read out of. Reads pull
 * from the front of the deque, mirroring the upstream slice-views and drain semantics.
 *
 * The public constructor takes an immutable [List] of [Byte] rather than a mutable [ArrayDeque]:
 * Swift Export's bridge generator erases a public mutable generic collection to `ArrayDeque<Any?>`
 * and then fails to cast it back to `ArrayDeque<Byte>`, so the Swift Export compile breaks. An
 * `ArrayDeque<Byte>` is itself a `List<Byte>`, so callers holding a deque pass it unchanged; the
 * draining copy is kept internal.
 */
public class VecDequeBuf(
    bytes: List<Byte>,
) : Buf {
    private val deque: ArrayDeque<Byte> = ArrayDeque(bytes)

    override fun remaining(): Int = deque.size

    override fun chunk(): ByteArray {
        if (deque.isEmpty()) return ByteArray(0)
        // ArrayDeque does not expose its two internal segments, so the chunk is a snapshot of the
        // entire deque. Successive [chunk] calls after [advance] still return non-empty slices.
        val out = ByteArray(deque.size)
        for (index in deque.indices) {
            out[index] = deque[index]
        }
        return out
    }

    override fun advance(cnt: Int) {
        require(cnt <= deque.size) { "advance past remaining: $cnt > ${deque.size}" }
        for (index in 0 until cnt) {
            deque.removeFirst()
        }
    }
}
