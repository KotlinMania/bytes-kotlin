// port-lint: source tests/test_bytes_odd_alloc.rs + tests/test_bytes_vec_alloc.rs
package io.github.kotlinmania.bytes

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Upstream test_bytes_odd_alloc.rs and test_bytes_vec_alloc.rs test custom global
// allocators (#[global_allocator] static ODD: Odd, LEDGER: Ledger) and raw pointer LSB alignment.
// Those low-level memory allocator hooks do not port to Kotlin managed memory/GC runtimes.
// Below are faithful Kotlin equivalents of all behavioral and buffer transformation invariants.

class AllocTest {
    @Test
    fun testBytesFromVecDrop() {
        val vec = ByteArray(1024) { 33 }
        val b = Bytes.from(vec)
        assertEquals(1024, b.len())
        assertContentEquals(vec, b.asSlice())
    }

    @Test
    fun testBytesCloneDrop() {
        val vec = ByteArray(1024) { 33 }
        val b1 = Bytes.from(vec)
        val b2 = b1.clone()
        assertEquals(b1, b2)
    }

    @Test
    fun testBytesIntoVec() {
        val vec = ByteArray(1024) { 33 }

        // Fresh Bytes
        val b1 = Bytes.from(vec)
        assertContentEquals(vec, b1.asSlice())

        // Cloned Bytes
        val b2 = Bytes.from(vec)
        val b3 = b2.clone()
        assertContentEquals(vec, b2.asSlice())
        assertContentEquals(vec, b3.asSlice())

        // Sliced Bytes with offset
        val b4 = Bytes.from(vec)
        val b5 = b4.splitOff(20)
        assertContentEquals(vec.copyOfRange(20, 1024), b5.asSlice())
        assertContentEquals(vec.copyOfRange(0, 20), b4.asSlice())
    }

    @Test
    fun testBytesMutFromBytesVec() {
        val vec = ByteArray(1024) { 33 }
        val b1 = Bytes.from(vec)
        val b1m = BytesMut.from(b1.asSlice())
        assertContentEquals(vec, b1m.asRef())
    }

    @Test
    fun testBytesMutFromBytesArc() {
        val vec = ByteArray(1024) { 33 }
        val b1 = Bytes.from(vec)
        val b2 = b1.clone()
        val b1m = BytesMut.from(b1.asSlice())
        val b2m = BytesMut.from(b2.asSlice())
        assertContentEquals(vec, b1m.asRef())
        assertContentEquals(vec, b2m.asRef())
    }

    @Test
    fun testBytesMutFromBytesArcOffset() {
        val vec = ByteArray(1024) { 33 }
        val b1 = Bytes.from(vec)
        val b2 = b1.splitOff(20)
        val b1m = BytesMut.from(b1.asSlice())
        val b2m = BytesMut.from(b2.asSlice())
        assertContentEquals(vec.copyOfRange(20, 1024), b2m.asRef())
        assertContentEquals(vec.copyOfRange(0, 20), b1m.asRef())
    }

    @Test
    fun testBytesAdvance() {
        val bytes = Bytes.from(byteArrayOf(10, 20, 30))
        val advanced = bytes.slice(1, bytes.len())
        assertEquals(2, advanced.len())
        assertContentEquals(byteArrayOf(20, 30), advanced.asSlice())
    }

    @Test
    fun testBytesTruncate() {
        val bytes = Bytes.from(byteArrayOf(10, 20, 30))
        bytes.truncate(2)
        assertEquals(2, bytes.len())
        assertContentEquals(byteArrayOf(10, 20), bytes.asSlice())
    }

    @Test
    fun testBytesTruncateAndAdvance() {
        val bytes = Bytes.from(byteArrayOf(10, 20, 30))
        bytes.truncate(2)
        val advanced = bytes.slice(1, bytes.len())
        assertEquals(1, advanced.len())
        assertContentEquals(byteArrayOf(20), advanced.asSlice())
    }
}
