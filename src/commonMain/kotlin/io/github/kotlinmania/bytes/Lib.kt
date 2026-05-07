// port-lint: source src/lib.rs
package io.github.kotlinmania.bytes

/**
 * Provides abstractions for working with bytes.
 *
 * The `bytes` crate provides an efficient byte buffer structure
 * (`Bytes`) and traits for working with buffer implementations
 * (`Buf`, `BufMut`).
 *
 * # `Bytes`
 *
 * `Bytes` is an efficient container for storing and operating on contiguous
 * slices of memory. It is intended for use primarily in networking code, but
 * could have applications elsewhere as well.
 *
 * `Bytes` values facilitate zero-copy network programming by allowing multiple
 * `Bytes` objects to point to the same underlying memory. This is managed by
 * using a reference count to track when the memory is no longer needed and can
 * be freed.
 *
 * A `Bytes` handle can be created directly from an existing byte store (such as a
 * `ByteArray` or `MutableList<Byte>`), but usually a `BytesMut` is used first and
 * written to.
 *
 * In the upstream example, only a single buffer of 1024 is allocated. The handles
 * `a` and `b` will share the underlying buffer and maintain indices tracking
 * the view into the buffer represented by the handle.
 *
 * See the struct docs for `Bytes` for more details.
 *
 * # `Buf`, `BufMut`
 *
 * These two traits provide read and write access to buffers. The underlying
 * storage may or may not be in contiguous memory. For example, `Bytes` is a
 * buffer that guarantees contiguous memory, but a [rope] stores the bytes in
 * disjoint chunks. `Buf` and `BufMut` maintain cursors tracking the current
 * position in the underlying byte storage. When bytes are read or written, the
 * cursor is advanced.
 *
 * Rope reference: https://en.wikipedia.org/wiki/Rope
 *
 * ## Relation with `Read` and `Write`
 *
 * At first glance, it may seem that `Buf` and `BufMut` overlap in
 * functionality with `Read` and `Write`. However, they serve different purposes.
 * A buffer is the value that is provided as an argument to `Read.read` and
 * `Write.write`. `Read` and `Write` may then perform a syscall, which has the
 * potential of failing. Operations on `Buf` and `BufMut` are infallible.
 */

// The upstream crate root enables allocation support, conditionally enables standard-library
// support, declares the buffer, bytes, mutable-bytes, formatting, and loom modules, and exposes
// the main buffer and byte container names from their owning modules. Kotlin callers should
// import those owning ported symbols directly instead of using root package type aliases.

// Optional Serde support belongs to the Kotlin port of the upstream serde module.

internal fun abort(): Nothing {
    class Abort {
        fun drop(): Nothing {
            throw IllegalStateException("abort")
        }
    }

    val aborter = Abort()
    try {
        throw IllegalStateException("abort")
    } finally {
        aborter.drop()
    }
}

internal fun saturatingSubUsizeU64(a: Int, b: ULong): Int {
    if (b > Int.MAX_VALUE.toULong()) {
        return 0
    }
    return (a - b.toInt()).coerceAtLeast(0)
}

internal fun minU64Usize(a: ULong, b: Int): Int =
    if (a > Int.MAX_VALUE.toULong()) {
        b
    } else {
        minOf(a.toInt(), b)
    }

internal fun fmt(error: TryGetError): String =
    error.message ?: ""

internal fun from(error: TryGetError): Exception =
    error

/**
 * Error type for the `tryGet` methods of `Buf`.
 *
 * Indicates that there were not enough remaining bytes in the buffer while attempting
 * to get a value from a `Buf` with one of the `tryGet` methods.
 */
public data class TryGetError(
    /**
     * The number of bytes necessary to get the value
     */
    public val requested: Int,
    /**
     * The number of bytes available in the buffer
     */
    public val available: Int,
) : Exception(
    "Not enough bytes remaining in buffer to read value (requested $requested but only $available available)",
)

/**
 * Panic with a nice error message.
 */
internal fun panicAdvance(errorInfo: TryGetError): Nothing {
    throw IndexOutOfBoundsException(
        "advance out of bounds: the len is ${errorInfo.available} but advancing by ${errorInfo.requested}",
    )
}

internal fun panicDoesNotFit(size: Int, nbytes: Int): Nothing {
    throw IllegalArgumentException(
        "size too large: the integer type can fit $size bytes, but nbytes is $nbytes",
    )
}
