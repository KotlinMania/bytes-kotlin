// port-lint: source fmt/debug.rs
package io.github.kotlinmania.bytes.fmt

/**
 * Alternative implementation of `Debug` for byte slice.
 *
 * Standard `Debug` implementation for `ByteArray` is comma separated
 * list of numbers. Since large amount of byte strings are in fact
 * ASCII strings or contain a lot of ASCII strings (e. g. HTTP),
 * it is convenient to print strings as ASCII when possible.
 */
internal fun debugBytesRef(bytesRef: BytesRef): String {
    val out = StringBuilder()
    out.append("b\"")
    for (byte in bytesRef.bytes) {
        val unsignedByte = byte.toInt() and 0xff
        // https://doc.rust-lang.org/reference/tokens.html#byte-escapes
        when {
            unsignedByte == '\n'.code -> out.append("\\n")
            unsignedByte == '\r'.code -> out.append("\\r")
            unsignedByte == '\t'.code -> out.append("\\t")
            unsignedByte == '\\'.code || unsignedByte == '"'.code -> {
                out.append('\\')
                out.append(unsignedByte.toChar())
            }
            unsignedByte == 0 -> out.append("\\0")
            // ASCII printable
            unsignedByte in 0x20 until 0x7f -> out.append(unsignedByte.toChar())
            else -> {
                out.append("\\x")
                out.append(unsignedByte.toString(16).padStart(2, '0'))
            }
        }
    }
    out.append('"')
    return out.toString()
}

internal fun fmt(bytesRef: BytesRef): String =
    debugBytesRef(bytesRef)

// The upstream module applies this formatter to both immutable and mutable byte containers.
