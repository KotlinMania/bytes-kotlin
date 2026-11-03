// port-lint: source fmt/hex.rs
package io.github.kotlinmania.bytes.fmt

internal fun lowerHexBytesRef(bytesRef: BytesRef): String {
    val out = StringBuilder(bytesRef.bytes.size * 2)
    for (byte in bytesRef.bytes) {
        out.append((byte.toInt() and 0xff).toString(16).padStart(2, '0'))
    }
    return out.toString()
}

internal fun upperHexBytesRef(bytesRef: BytesRef): String {
    val out = StringBuilder(bytesRef.bytes.size * 2)
    for (byte in bytesRef.bytes) {
        out.append((byte.toInt() and 0xff).toString(16).padStart(2, '0').uppercase())
    }
    return out.toString()
}

internal fun fmt(bytesRef: BytesRef, uppercase: Boolean): String =
    if (uppercase) {
        upperHexBytesRef(bytesRef)
    } else {
        lowerHexBytesRef(bytesRef)
    }

// The upstream module applies lower- and upper-hex formatting to both immutable and mutable byte
// containers.
