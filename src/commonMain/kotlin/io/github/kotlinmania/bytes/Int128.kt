// port-lint: ignore
// Kotlin has no native 128-bit integer type. This file provides U128 and I128 wrapper classes
// holding two ULong halves (high and low). They satisfy the Rust upstream's get_u128 / get_i128
// surface area on the `Buf` trait without pulling in a BigInteger dependency. Endianness is
// handled explicitly in conversions to and from byte arrays.
package io.github.kotlinmania.bytes

/**
 * An unsigned 128-bit integer composed of a high and low 64-bit half.
 *
 * The Kotlin standard library does not define a 128-bit primitive type. [U128] is the byte-faithful
 * wrapper used by the buffer trait surface so that the `getU128` / `tryGetU128` family of methods
 * on [io.github.kotlinmania.bytes.buf.Buf] preserve the upstream `bytes` crate semantics.
 *
 * Internally [high] holds the most significant 64 bits and [low] holds the least significant 64
 * bits. Equality is bitwise; arithmetic is intentionally not provided here because the only
 * use site for [U128] inside this port is reading and writing 16 raw bytes.
 */
public data class U128(
    /** The most significant 64 bits. */
    public val high: ULong,
    /** The least significant 64 bits. */
    public val low: ULong,
) {
    public companion object {
        /** The zero value. */
        public val ZERO: U128 = U128(0u, 0u)

        /**
         * Read a [U128] from 16 bytes encoded in big-endian order. Byte 0 is the most significant
         * byte of [high]; byte 15 is the least significant byte of [low].
         */
        public fun fromBigEndianBytes(bytes: ByteArray): U128 {
            require(bytes.size >= 16) { "U128 requires 16 bytes, got ${bytes.size}" }
            return U128(
                high = bytesToULongBe(bytes, 0),
                low = bytesToULongBe(bytes, 8),
            )
        }

        /**
         * Read a [U128] from 16 bytes encoded in little-endian order. Byte 0 is the least
         * significant byte of [low]; byte 15 is the most significant byte of [high].
         */
        public fun fromLittleEndianBytes(bytes: ByteArray): U128 {
            require(bytes.size >= 16) { "U128 requires 16 bytes, got ${bytes.size}" }
            return U128(
                high = bytesToULongLe(bytes, 8),
                low = bytesToULongLe(bytes, 0),
            )
        }
    }

    /** Encode this [U128] as 16 bytes in big-endian order. */
    public fun toBigEndianBytes(): ByteArray {
        val out = ByteArray(16)
        uLongToBytesBe(high, out, 0)
        uLongToBytesBe(low, out, 8)
        return out
    }

    /** Encode this [U128] as 16 bytes in little-endian order. */
    public fun toLittleEndianBytes(): ByteArray {
        val out = ByteArray(16)
        uLongToBytesLe(low, out, 0)
        uLongToBytesLe(high, out, 8)
        return out
    }

    /** Reinterpret the bits of this [U128] as an [I128] without changing the byte representation. */
    public fun toI128(): I128 = I128(high.toLong(), low)
}

/**
 * A signed 128-bit integer composed of a signed high and an unsigned low 64-bit half.
 *
 * The most significant bit of [high] is the sign bit. Conversions to and from byte arrays
 * mirror [U128] on identical byte content; sign handling is performed via the most significant
 * bit only.
 */
public data class I128(
    /** The most significant 64 bits, including the sign bit. */
    public val high: Long,
    /** The least significant 64 bits, treated as unsigned. */
    public val low: ULong,
) {
    public companion object {
        /** The zero value. */
        public val ZERO: I128 = I128(0L, 0u)

        /**
         * Read an [I128] from 16 bytes encoded in big-endian order. Byte 0 carries the sign bit
         * in its top bit.
         */
        public fun fromBigEndianBytes(bytes: ByteArray): I128 = U128.fromBigEndianBytes(bytes).toI128()

        /**
         * Read an [I128] from 16 bytes encoded in little-endian order. Byte 15 carries the sign
         * bit in its top bit.
         */
        public fun fromLittleEndianBytes(bytes: ByteArray): I128 = U128.fromLittleEndianBytes(bytes).toI128()
    }

    /** Encode this [I128] as 16 bytes in big-endian order, sign-extended. */
    public fun toBigEndianBytes(): ByteArray = toU128().toBigEndianBytes()

    /** Encode this [I128] as 16 bytes in little-endian order, sign-extended. */
    public fun toLittleEndianBytes(): ByteArray = toU128().toLittleEndianBytes()

    /** Reinterpret the bits of this [I128] as a [U128] without changing the byte representation. */
    public fun toU128(): U128 = U128(high.toULong(), low)
}

private fun bytesToULongBe(bytes: ByteArray, offset: Int): ULong {
    var value = 0uL
    for (index in 0 until 8) {
        value = (value shl 8) or (bytes[offset + index].toULong() and 0xffu)
    }
    return value
}

private fun bytesToULongLe(bytes: ByteArray, offset: Int): ULong {
    var value = 0uL
    for (index in 7 downTo 0) {
        value = (value shl 8) or (bytes[offset + index].toULong() and 0xffu)
    }
    return value
}

private fun uLongToBytesBe(value: ULong, dst: ByteArray, offset: Int) {
    for (index in 0 until 8) {
        dst[offset + index] = ((value shr ((7 - index) * 8)) and 0xffu).toByte()
    }
}

private fun uLongToBytesLe(value: ULong, dst: ByteArray, offset: Int) {
    for (index in 0 until 8) {
        dst[offset + index] = ((value shr (index * 8)) and 0xffu).toByte()
    }
}
