// port-lint: source tests/test_serde.rs
package io.github.kotlinmania.bytes

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Translated from `tests/test_serde.rs`. The upstream tests run `serde_test::assert_tokens`
 * to verify both `Serialize` and `Deserialize` against a fixed `Token::Bytes(...)` token
 * sequence. kotlinx-serialization has no equivalent token-tape harness, so each test below
 * runs a JSON encode/decode roundtrip via the [BytesSerializer] / [BytesMutSerializer]
 * objects from [Serde] and asserts that the decoded value equals the original.
 */
class SerdeTest {
    private val json = Json

    @Test
    fun testSerDeEmpty() {
        run {
            val b = Bytes.new()
            val encoded = json.encodeToString(BytesSerializer, b)
            assertEquals(b, json.decodeFromString(BytesSerializer, encoded))
        }
        run {
            val b = BytesMut.withCapacity(0)
            val encoded = json.encodeToString(BytesMutSerializer, b)
            assertEquals(b, json.decodeFromString(BytesMutSerializer, encoded))
        }
    }

    @Test
    fun testSerDe() {
        run {
            val b = Bytes.from("bytes".encodeToByteArray())
            val encoded = json.encodeToString(BytesSerializer, b)
            assertEquals(b, json.decodeFromString(BytesSerializer, encoded))
        }
        run {
            val b = BytesMut.from("bytes".encodeToByteArray())
            val encoded = json.encodeToString(BytesMutSerializer, b)
            assertEquals(b, json.decodeFromString(BytesMutSerializer, encoded))
        }
    }
}
