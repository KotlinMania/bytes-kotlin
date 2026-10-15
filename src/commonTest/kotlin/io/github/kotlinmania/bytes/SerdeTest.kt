// port-lint: source tests/test_serde.rs
package io.github.kotlinmania.bytes

import io.github.kotlinmania.serdetest.Token
import io.github.kotlinmania.serdetest.assertTokens
import kotlin.test.Test

class SerdeTest {
    @Test
    fun testSerDeEmpty() {
        val b = Bytes.new()
        assertTokens(b, BytesDeserialize, listOf(Token.Bytes(byteArrayOf())))
        val bm = BytesMut.withCapacity(0)
        assertTokens(bm, BytesMutDeserialize, listOf(Token.Bytes(byteArrayOf())))
    }

    @Test
    fun testSerDe() {
        val b = Bytes.from("bytes".encodeToByteArray())
        assertTokens(b, BytesDeserialize, listOf(Token.Bytes("bytes".encodeToByteArray())))
        val bm = BytesMut.from("bytes".encodeToByteArray())
        assertTokens(bm, BytesMutDeserialize, listOf(Token.Bytes("bytes".encodeToByteArray())))
    }
}
