package io.github.kotlinmania.bytes

internal fun fmt(error: TryGetError): String =
    error.message.orEmpty()

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
internal fun panicAdvance(errorInfo: TryGetError): Nothing = throw IndexOutOfBoundsException(
    "advance out of bounds: the len is ${errorInfo.available} but advancing by ${errorInfo.requested}",
)

internal fun panicDoesNotFit(size: Int, nbytes: Int): Nothing = throw IllegalArgumentException(
    "size too large: the integer type can fit $size bytes, but nbytes is $nbytes",
)
