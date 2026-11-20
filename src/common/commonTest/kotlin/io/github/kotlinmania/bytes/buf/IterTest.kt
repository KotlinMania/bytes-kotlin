// port-lint: source tests/test_iter.rs
package io.github.kotlinmania.bytes.buf

import io.github.kotlinmania.bytes.Bytes
import kotlin.test.Test
import kotlin.test.assertEquals

class IterTest {
    @Test
    fun iterLen() {
        val buf = Bytes.fromStatic("hello world".encodeToByteArray())
        val iter = IntoIter(ByteArrayBuf(buf.asSlice()))

        assertEquals(11 to 11, iter.sizeHint())
    }

    @Test
    fun emptyIterLen() {
        val buf = Bytes.new()
        val iter = IntoIter(ByteArrayBuf(buf.asSlice()))

        assertEquals(0 to 0, iter.sizeHint())
    }
}
