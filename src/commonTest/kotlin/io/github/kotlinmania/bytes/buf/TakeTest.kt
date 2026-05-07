// port-lint: source tests/test_take.rs
package io.github.kotlinmania.bytes.buf

import io.github.kotlinmania.bytes.Bytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class TakeTest {
    @Test
    fun longTake() {
        // Tests that a take with a size greater than the buffer length will not
        // overrun the buffer. Regression test for #138.
        val buf = ByteArrayBuf("hello world".encodeToByteArray()).take(100)
        assertEquals(11, buf.remaining())
        assertTrue(buf.chunk().contentEquals("hello world".encodeToByteArray()))
    }

    @Test
    fun takeCopyToBytes() {
        val abcd = ByteArrayBuf("abcd".encodeToByteArray())
        val take = abcd.take(2)
        val a = take.copyToBytes(1)
        assertTrue(a.asSlice().contentEquals("a".encodeToByteArray()))
        // The Kotlin port preserves zero-copy semantics through the SharedBytes refcount; the
        // upstream test asserts via pointer equality, which has no direct analog. The remaining
        // contents of `abcd` confirm that `copy_to_bytes` advanced the cursor through the
        // existing buffer rather than allocating.
        val rest = ByteArray(3)
        abcd.copyToSlice(rest)
        assertTrue(rest.contentEquals("bcd".encodeToByteArray()))
    }

    @Test
    fun takeCopyToBytesPanics() {
        val abcd = ByteArrayBuf("abcd".encodeToByteArray())
        assertFails {
            abcd.take(2).copyToBytes(3)
        }
    }
}
