// port-lint: source tests/test_reader.rs
package io.github.kotlinmania.bytes.buf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReaderTest {
    @Test
    fun read() {
        val buf1 = ByteArrayBuf("hello ".encodeToByteArray())
        val buf2 = ByteArrayBuf("world".encodeToByteArray())
        val buf = buf1.chain(buf2)
        val sink = ByteArray(11)
        val read = buf.reader().read(sink)
        assertEquals(11, read)
        assertTrue(sink.contentEquals("hello world".encodeToByteArray()))
    }

    @Test
    fun bufRead() {
        // The upstream `read_line` reads until newline; the Kotlin port surfaces fillBuf and
        // consume so callers can compose an equivalent line reader. This test exercises that
        // composition.
        val buf1 = ByteArrayBuf("hell".encodeToByteArray())
        val buf2 = ByteArrayBuf("o\nworld".encodeToByteArray())
        val reader = buf1.chain(buf2).reader()

        // First read: consume bytes until the first '\n', inclusive.
        var line = StringBuilder()
        var done = false
        while (!done) {
            val available = reader.fillBuf()
            if (available.isEmpty()) {
                break
            }
            val newlineIndex = available.indexOf('\n'.code.toByte())
            val take = if (newlineIndex >= 0) newlineIndex + 1 else available.size
            line.append(available.copyOfRange(0, take).decodeToString())
            reader.consume(take)
            if (newlineIndex >= 0) {
                done = true
            }
        }
        assertEquals("hello\n", line.toString())

        // Second read: drain the rest.
        line = StringBuilder()
        while (true) {
            val available = reader.fillBuf()
            if (available.isEmpty()) {
                break
            }
            line.append(available.decodeToString())
            reader.consume(available.size)
        }
        assertEquals("world", line.toString())
    }

    @Test
    fun getMut() {
        val buf = ByteArrayBuf("hello world".encodeToByteArray())
        val reader = buf.reader()
        val bufMut = reader.getMut()
        assertEquals(11, bufMut.remaining())
        assertTrue(bufMut.chunk().contentEquals("hello world".encodeToByteArray()))
    }
}
