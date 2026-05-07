// port-lint: source tests/test_buf.rs
package io.github.kotlinmania.bytes.buf

import io.github.kotlinmania.bytes.U128
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// A random 64-byte ascii string, with the first 8 bytes altered to give valid representations
// of f32 and f64 (making them easier to compare) and negative signed numbers when interpreting
// as big endian (testing sign-extension for `getInt` and `getIntNe`).
internal val INPUT: ByteArray =
    byteArrayOf(
        0xff.toByte(), 'F'.code.toByte(), 'q'.code.toByte(), 'r'.code.toByte(),
        'j'.code.toByte(), 'r'.code.toByte(), 'D'.code.toByte(), 'q'.code.toByte(),
    ) + "PhvTc45vvq33f6bJrUtyHESuTeklWKgYd64xgzxJwvAkpYYnpNJyZSRn".encodeToByteArray()

abstract class BufTestSuite {
    /** Construct a fresh [Buf] over the given input bytes. */
    abstract fun makeInput(bytes: ByteArray): Buf

    @Test
    fun emptyState() {
        val buf = makeInput(byteArrayOf())
        assertEquals(0, buf.remaining())
        assertFalse(buf.hasRemaining())
        assertTrue(buf.chunk().isEmpty())
    }

    @Test
    fun freshState() {
        val buf = makeInput(INPUT)
        assertEquals(64, buf.remaining())
        assertTrue(buf.hasRemaining())

        val chunk = buf.chunk()
        assertTrue(chunk.size <= 64)
        assertTrue(INPUT.copyOfRange(0, chunk.size).contentEquals(chunk))
    }

    @Test
    fun advance() {
        val buf = makeInput(INPUT)
        buf.advance(8)
        assertEquals(64 - 8, buf.remaining())
        assertTrue(buf.hasRemaining())

        val chunk = buf.chunk()
        assertTrue(chunk.size <= 64 - 8)
        assertTrue(INPUT.copyOfRange(8, 8 + chunk.size).contentEquals(chunk))
    }

    @Test
    fun advanceToEnd() {
        val buf = makeInput(INPUT)
        buf.advance(64)
        assertEquals(0, buf.remaining())
        assertFalse(buf.hasRemaining())
        assertTrue(buf.chunk().isEmpty())
    }

    @Test
    fun advancePastEnd() {
        val buf = makeInput(INPUT)
        assertFails { buf.advance(65) }
    }

    @Test
    fun copyToSlice() {
        val buf = makeInput(INPUT)
        val chunk = ByteArray(8)
        buf.copyToSlice(chunk)
        assertEquals(64 - 8, buf.remaining())
        assertTrue(buf.hasRemaining())
        assertContentEquals(INPUT.copyOfRange(0, 8), chunk)
    }

    @Test
    fun copyToSliceBig() {
        val buf = makeInput(INPUT)
        val chunk = ByteArray(56)
        buf.copyToSlice(chunk)
        assertEquals(64 - 56, buf.remaining())
        assertTrue(buf.hasRemaining())
        assertContentEquals(INPUT.copyOfRange(0, 56), chunk)
    }

    @Test
    fun copyToSliceToEnd() {
        val buf = makeInput(INPUT)
        val chunk = ByteArray(64)
        buf.copyToSlice(chunk)
        assertEquals(0, buf.remaining())
        assertFalse(buf.hasRemaining())
        assertContentEquals(INPUT, chunk)
        assertTrue(buf.chunk().isEmpty())
    }

    @Test
    fun copyToSliceOverflow() {
        val buf = makeInput(INPUT)
        val chunk = ByteArray(65)
        assertFails { buf.copyToSlice(chunk) }
    }

    @Test
    fun copyToBytes() {
        val buf = makeInput(INPUT)
        val chunk = buf.copyToBytes(8)
        assertEquals(64 - 8, buf.remaining())
        assertTrue(buf.hasRemaining())
        assertContentEquals(INPUT.copyOfRange(0, 8), chunk.asSlice())
    }

    @Test
    fun copyToBytesBig() {
        val buf = makeInput(INPUT)
        val chunk = buf.copyToBytes(56)
        assertEquals(64 - 56, buf.remaining())
        assertTrue(buf.hasRemaining())
        assertContentEquals(INPUT.copyOfRange(0, 56), chunk.asSlice())
    }

    @Test
    fun copyToBytesToEnd() {
        val buf = makeInput(INPUT)
        val chunk = buf.copyToBytes(64)
        assertEquals(0, buf.remaining())
        assertFalse(buf.hasRemaining())
        assertContentEquals(INPUT, chunk.asSlice())
        assertTrue(buf.chunk().isEmpty())
    }

    @Test
    fun copyToBytesOverflow() {
        val buf = makeInput(INPUT)
        assertFails { buf.copyToBytes(65) }
    }

    // === fixed-width number readers ===

    @Test
    fun getU8Ok() {
        val buf = makeInput(INPUT)
        val value = buf.getU8()
        assertEquals(64 - 1, buf.remaining())
        assertEquals(0xffu.toUByte(), value)
    }

    @Test
    fun getU8Overflow() {
        val buf = makeInput(byteArrayOf())
        assertFails { buf.getU8() }
    }

    @Test
    fun getI8Ok() {
        val buf = makeInput(INPUT)
        val value = buf.getI8()
        assertEquals(64 - 1, buf.remaining())
        assertEquals(0xff.toByte(), value)
    }

    @Test
    fun getI8Overflow() {
        val buf = makeInput(byteArrayOf())
        assertFails { buf.getI8() }
    }

    @Test
    fun getU16Be() {
        val buf = makeInput(INPUT)
        val value = buf.getU16()
        assertEquals(64 - 2, buf.remaining())
        assertEquals(0xff46u.toUShort(), value)
    }

    @Test
    fun getU16BeOverflow() {
        val buf = makeInput(byteArrayOf())
        assertFails { buf.getU16() }
    }

    @Test
    fun getU16Le() {
        val buf = makeInput(INPUT)
        val value = buf.getU16Le()
        assertEquals(64 - 2, buf.remaining())
        assertEquals(0x46ffu.toUShort(), value)
    }

    @Test
    fun getU16LeOverflow() {
        val buf = makeInput(byteArrayOf())
        assertFails { buf.getU16Le() }
    }

    @Test
    fun getU16Ne() {
        val buf = makeInput(INPUT)
        val value = buf.getU16Ne()
        assertEquals(64 - 2, buf.remaining())
        // KMP target platforms in this port are all little-endian.
        assertEquals(0x46ffu.toUShort(), value)
    }

    @Test
    fun getI16Be() {
        val buf = makeInput(INPUT)
        val value = buf.getI16()
        assertEquals(64 - 2, buf.remaining())
        assertEquals(0xff46u.toShort(), value)
    }

    @Test
    fun getI16Le() {
        val buf = makeInput(INPUT)
        val value = buf.getI16Le()
        assertEquals(64 - 2, buf.remaining())
        assertEquals(0x46ff.toShort(), value)
    }

    @Test
    fun getU32Be() {
        val buf = makeInput(INPUT)
        val value = buf.getU32()
        assertEquals(64 - 4, buf.remaining())
        assertEquals(0xff467172u, value)
    }

    @Test
    fun getU32BeOverflow() {
        val buf = makeInput(byteArrayOf())
        assertFails { buf.getU32() }
    }

    @Test
    fun getU32Le() {
        val buf = makeInput(INPUT)
        val value = buf.getU32Le()
        assertEquals(64 - 4, buf.remaining())
        assertEquals(0x727146ffu, value)
    }

    @Test
    fun getI32Be() {
        val buf = makeInput(INPUT)
        val value = buf.getI32()
        assertEquals(64 - 4, buf.remaining())
        assertEquals(0xff467172u.toInt(), value)
    }

    @Test
    fun getI32Le() {
        val buf = makeInput(INPUT)
        val value = buf.getI32Le()
        assertEquals(64 - 4, buf.remaining())
        assertEquals(0x727146ff, value)
    }

    @Test
    fun getU64Be() {
        val buf = makeInput(INPUT)
        val value = buf.getU64()
        assertEquals(64 - 8, buf.remaining())
        assertEquals(0xff4671726a724471uL, value)
    }

    @Test
    fun getU64BeOverflow() {
        val buf = makeInput(byteArrayOf())
        assertFails { buf.getU64() }
    }

    @Test
    fun getU64Le() {
        val buf = makeInput(INPUT)
        val value = buf.getU64Le()
        assertEquals(64 - 8, buf.remaining())
        assertEquals(0x7144726a727146ffuL, value)
    }

    @Test
    fun getI64Be() {
        val buf = makeInput(INPUT)
        val value = buf.getI64()
        assertEquals(64 - 8, buf.remaining())
        assertEquals(0xff4671726a724471uL.toLong(), value)
    }

    @Test
    fun getI64Le() {
        val buf = makeInput(INPUT)
        val value = buf.getI64Le()
        assertEquals(64 - 8, buf.remaining())
        assertEquals(0x7144726a727146ffuL.toLong(), value)
    }

    @Test
    fun getU128Be() {
        val buf = makeInput(INPUT)
        val value = buf.getU128()
        assertEquals(64 - 16, buf.remaining())
        // Upstream constant: 0xff4671726a7244715068765463343576
        assertEquals(U128(0xff4671726a724471uL, 0x5068765463343576uL), value)
    }

    @Test
    fun getU128Le() {
        val buf = makeInput(INPUT)
        val value = buf.getU128Le()
        assertEquals(64 - 16, buf.remaining())
        // Upstream constant: 0x76353463547668507144726a727146ff
        assertEquals(U128(0x7635346354766850uL, 0x7144726a727146ffuL), value)
    }

    @Test
    fun getF32Be() {
        val buf = makeInput(INPUT)
        val value = buf.getF32()
        assertEquals(64 - 4, buf.remaining())
        assertEquals(Float.fromBits(0xff467172u.toInt()), value)
    }

    @Test
    fun getF32Le() {
        val buf = makeInput(INPUT)
        val value = buf.getF32Le()
        assertEquals(64 - 4, buf.remaining())
        assertEquals(Float.fromBits(0x727146ff), value)
    }

    @Test
    fun getF64Be() {
        val buf = makeInput(INPUT)
        val value = buf.getF64()
        assertEquals(64 - 8, buf.remaining())
        assertEquals(Double.fromBits(0xff4671726a724471uL.toLong()), value)
    }

    @Test
    fun getF64Le() {
        val buf = makeInput(INPUT)
        val value = buf.getF64Le()
        assertEquals(64 - 8, buf.remaining())
        assertEquals(Double.fromBits(0x7144726a727146ffuL.toLong()), value)
    }

    // === variable-width number readers ===

    @Test
    fun getUintBe() {
        val buf = makeInput(INPUT)
        val value = buf.getUint(3)
        assertEquals(64 - 3, buf.remaining())
        assertEquals(0xff4671uL, value)
    }

    @Test
    fun getUintBeOverflow() {
        val buf = makeInput(byteArrayOf())
        assertFails { buf.getUint(3) }
    }

    @Test
    fun getUintLe() {
        val buf = makeInput(INPUT)
        val value = buf.getUintLe(3)
        assertEquals(64 - 3, buf.remaining())
        assertEquals(0x7146ffuL, value)
    }

    @Test
    fun getIntBe() {
        val buf = makeInput(INPUT)
        val value = buf.getInt(3)
        assertEquals(64 - 3, buf.remaining())
        // Sign-extended from 0xff4671 (high byte 0xff is the sign byte).
        assertEquals(0xffffffffffff4671uL.toLong(), value)
    }

    @Test
    fun getIntLe() {
        val buf = makeInput(INPUT)
        val value = buf.getIntLe(3)
        assertEquals(64 - 3, buf.remaining())
        assertEquals(0x7146ffL, value)
    }
}

// === Concrete factories ===

class ByteArrayBufBufTest : BufTestSuite() {
    override fun makeInput(bytes: ByteArray): Buf = ByteArrayBuf(bytes)
}

class ChainBufTest : BufTestSuite() {
    override fun makeInput(bytes: ByteArray): Buf {
        val mid = bytes.size / 2
        val a = ByteArrayBuf(bytes.copyOfRange(0, mid))
        val b = ByteArrayBuf(bytes.copyOfRange(mid, bytes.size))
        return a.chain(b)
    }
}

class ChainSmallBigBufTest : BufTestSuite() {
    override fun makeInput(bytes: ByteArray): Buf {
        val mid = if (bytes.isNotEmpty()) 1 else 0
        val a = ByteArrayBuf(bytes.copyOfRange(0, mid))
        val b = ByteArrayBuf(bytes.copyOfRange(mid, bytes.size))
        return a.chain(b)
    }
}
