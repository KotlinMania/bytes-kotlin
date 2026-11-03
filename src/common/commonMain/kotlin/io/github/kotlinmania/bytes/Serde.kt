// port-lint: source serde.rs
package io.github.kotlinmania.bytes

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.I8Deserialize
import io.github.kotlinmania.serdecore.de.SeqAccess
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.ser.Serializer
import kotlin.math.min

public fun <Ok> serialize(
    self: Bytes,
    serializer: Serializer<Ok>,
): SerdeResult<Ok> = serializer.serializeBytes(self.asSlice())

public fun <Ok> serialize(
    self: BytesMut,
    serializer: Serializer<Ok>,
): SerdeResult<Ok> = serializer.serializeBytes(self.asRef())

private data object BytesVisitor : Visitor<Bytes> {
    override fun expecting(): String = "byte array"

    override fun <A> visitSeq(access: A): SerdeResult<Bytes>
        where A : SeqAccess {
        val len = min(access.sizeHint() ?: 0, 4096)
        val values = ArrayList<Byte>(len)
        while (true) {
            val next =
                access.nextElement(I8Deserialize).fold(
                    onSuccess = { it },
                    onFailure = { return SerdeResult.failure(it) },
                ) ?: break
            values.add(next)
        }
        return SerdeResult.success(Bytes.from(values.toByteArray()))
    }

    override fun visitBytes(v: ByteArray): SerdeResult<Bytes> =
        SerdeResult.success(Bytes.copyFromSlice(v))

    override fun visitByteBuf(v: ByteArray): SerdeResult<Bytes> =
        SerdeResult.success(Bytes.from(v))

    override fun visitStr(v: String): SerdeResult<Bytes> =
        SerdeResult.success(Bytes.copyFromSlice(v.encodeToByteArray()))

    override fun visitString(v: String): SerdeResult<Bytes> =
        SerdeResult.success(Bytes.from(v.encodeToByteArray()))
}

public fun <D> deserializeBytes(deserializer: D): SerdeResult<Bytes> where D : Deserializer =
    deserializer.deserializeByteBuf(BytesVisitor)

public fun <D> deserialize(
    deserializer: D,
    @Suppress("UNUSED_PARAMETER") companion: Bytes.Companion,
): SerdeResult<Bytes> where D : Deserializer =
    deserializer.deserializeByteBuf(BytesVisitor)

private data object BytesMutVisitor : Visitor<BytesMut> {
    override fun expecting(): String = "byte array"

    override fun <A> visitSeq(access: A): SerdeResult<BytesMut>
        where A : SeqAccess {
        val len = min(access.sizeHint() ?: 0, 4096)
        val values = ArrayList<Byte>(len)
        while (true) {
            val next =
                access.nextElement(I8Deserialize).fold(
                    onSuccess = { it },
                    onFailure = { return SerdeResult.failure(it) },
                ) ?: break
            values.add(next)
        }
        return SerdeResult.success(BytesMut.from(values.toByteArray()))
    }

    override fun visitBytes(v: ByteArray): SerdeResult<BytesMut> =
        SerdeResult.success(BytesMut.from(v))

    override fun visitByteBuf(v: ByteArray): SerdeResult<BytesMut> =
        SerdeResult.success(BytesMut.from(v))

    override fun visitStr(v: String): SerdeResult<BytesMut> =
        SerdeResult.success(BytesMut.from(v.encodeToByteArray()))

    override fun visitString(v: String): SerdeResult<BytesMut> =
        SerdeResult.success(BytesMut.from(v.encodeToByteArray()))
}

public fun <D> deserializeBytesMut(deserializer: D): SerdeResult<BytesMut> where D : Deserializer =
    deserializer.deserializeByteBuf(BytesMutVisitor)

public fun <D> deserialize(
    deserializer: D,
    @Suppress("UNUSED_PARAMETER") companion: BytesMut.Companion,
): SerdeResult<BytesMut> where D : Deserializer =
    deserializer.deserializeByteBuf(BytesMutVisitor)
