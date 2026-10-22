// port-lint: source tests/test_serde.rs
package io.github.kotlinmania.bytes

import io.github.kotlinmania.serdetest.Token
import kotlin.test.Test
import io.github.kotlinmania.serdetest.assertTokens as serdeAssertTokens

class SerdeTest {
    private fun assertTokens(
        value: Bytes,
        tokens: List<Token>,
    ) {
        serdeAssertTokens(value, Bytes, tokens)
    }

    private fun assertTokens(
        value: BytesMut,
        tokens: List<Token>,
    ) {
        serdeAssertTokens(value, BytesMut, tokens)
    }

    @Test
    fun testSerDeEmpty() {
        val b = Bytes.new()
        assertTokens(b, listOf(Token.Bytes(byteArrayOf())))
        val bMut = BytesMut.withCapacity(0)
        assertTokens(bMut, listOf(Token.Bytes(byteArrayOf())))
    }

    @Test
    fun testSerDe() {
        val b = Bytes.from("bytes".encodeToByteArray())
        assertTokens(b, listOf(Token.Bytes("bytes".encodeToByteArray())))
        val bMut = BytesMut.from("bytes".encodeToByteArray())
        assertTokens(bMut, listOf(Token.Bytes("bytes".encodeToByteArray())))
    }
}
