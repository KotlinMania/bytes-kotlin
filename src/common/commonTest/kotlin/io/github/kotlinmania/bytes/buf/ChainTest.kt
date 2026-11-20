// port-lint: source tests/test_chain.rs
package io.github.kotlinmania.bytes.buf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChainTest {
    @Test
    fun collectTwoBufs() {
        val a = ByteArrayBuf("hello".encodeToByteArray())
        val b = ByteArrayBuf("world".encodeToByteArray())

        val res = a.chain(b).copyToBytes(10)
        assertTrue(res.asSlice().contentEquals("helloworld".encodeToByteArray()))
    }

    @Test
    fun writingChained() {
        val a = ByteArray(64)
        val b = ByteArray(64)

        val buf = ByteArrayBufMut(a).chainMut(ByteArrayBufMut(b))
        for (i in 0 until 128) {
            buf.putU8(i.toUByte())
        }

        for (i in 0 until 64) {
            val expect = i.toByte()
            assertEquals(expect, a[i])
            assertEquals((expect.toInt() + 64).toByte(), b[i])
        }
    }

    @Test
    fun iteratingTwoBufs() {
        val a = ByteArrayBuf("hello".encodeToByteArray())
        val b = ByteArrayBuf("world".encodeToByteArray())

        val collected = mutableListOf<Byte>()
        val iter = IntoIter(a.chain(b))
        while (iter.hasNext()) {
            collected.add(iter.next())
        }
        assertTrue(collected.toByteArray().contentEquals("helloworld".encodeToByteArray()))
    }

    @Test
    fun chainGetBytes() {
        val ab = ByteArrayBuf("ab".encodeToByteArray())
        val cd = ByteArrayBuf("cd".encodeToByteArray())
        val chain = ab.chain(cd)
        val a = chain.copyToBytes(1)
        val bc = chain.copyToBytes(2)
        val d = chain.copyToBytes(1)

        assertTrue(a.asSlice().contentEquals("a".encodeToByteArray()))
        assertTrue(bc.asSlice().contentEquals("bc".encodeToByteArray()))
        assertTrue(d.asSlice().contentEquals("d".encodeToByteArray()))
        // The upstream test additionally asserts pointer-equality to confirm zero-copy behaviour
        // through the SharedBytes vtable. Kotlin has no raw pointers; the content comparison
        // above is the strongest invariant we can exercise.
    }
}
