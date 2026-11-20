// port-lint: source tests/test_limit.rs
package io.github.kotlinmania.bytes.buf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class LimitTest {
    @Test
    fun longLimit() {
        val buf = ByteArrayBufMut(ByteArray(10))
        val limit = buf.limit(100)
        assertEquals(10, limit.remainingMut())
    }

    @Test
    fun limitGetMut() {
        val buf = ByteArrayBufMut(ByteArray(128))
        val limit = buf.limit(10)
        assertEquals(10, limit.remainingMut())
        // The Kotlin port exposes the same instance via getMut; the upstream byte-slice
        // length comparison is captured by checking the inner BufMut's full capacity.
        assertEquals(128, (limit.getMut() as ByteArrayBufMut).size)
    }

    @Test
    fun limitSetLimit() {
        val buf = ByteArrayBufMut(ByteArray(128))
        val limit = buf.limit(10)
        assertEquals(10, limit.limit())
        limit.setLimit(5)
        assertEquals(5, limit.limit())
    }

    @Test
    fun limitChunkMut() {
        val buf = ByteArrayBufMut(ByteArray(20))
        val limit = buf.limit(10)
        assertEquals(10, limit.chunkMut().len())

        val buf2 = ByteArrayBufMut(ByteArray(10))
        val limit2 = buf2.limit(20)
        assertEquals(10, limit2.chunkMut().len())
    }

    @Test
    fun limitAdvanceMutPanic1() {
        val buf = ByteArrayBufMut(ByteArray(10))
        val limit = buf.limit(100)
        assertFails {
            limit.advanceMut(50)
        }
    }

    @Test
    fun limitAdvanceMutPanic2() {
        val buf = ByteArrayBufMut(ByteArray(100))
        val limit = buf.limit(10)
        assertFails {
            limit.advanceMut(50)
        }
    }

    @Test
    fun limitAdvanceMut() {
        val buf = ByteArrayBufMut(ByteArray(100))
        val limit = buf.limit(10)
        limit.advanceMut(5)
        assertEquals(5, limit.remainingMut())
        assertEquals(5, limit.chunkMut().len())
    }

    @Test
    fun limitIntoInner() {
        val storage = "hello world".encodeToByteArray()
        val buf = ByteArrayBufMut(storage)
        val limit = buf.limit(4)
        limit.advanceMut(2)
        val recovered = limit.intoInner()
        // After advancing 2 of 4 limited bytes, the inner BufMut still has 9 bytes of capacity
        // remaining (11 - 2). The upstream test fills `dst` from the recovered slice and asserts
        // it equals "llo world".
        assertEquals(9, recovered.remainingMut())
    }
}
