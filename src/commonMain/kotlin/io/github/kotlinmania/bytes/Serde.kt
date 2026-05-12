// port-lint: source serde.rs
package io.github.kotlinmania.bytes

import io.github.kotlinmania.serde.core.de.Deserialize
import io.github.kotlinmania.serde.core.de.DeserializeSeed
import io.github.kotlinmania.serde.core.de.Deserializer
import io.github.kotlinmania.serde.core.de.I8Deserialize
import io.github.kotlinmania.serde.core.de.SeqAccess
import io.github.kotlinmania.serde.core.de.Visitor
import io.github.kotlinmania.serde.core.ser.Error
import io.github.kotlinmania.serde.core.ser.Serializer
import kotlin.math.min

/**
 * The upstream Rust file defines a `serde_impl!` macro that emits, for a given type, a
 * `Serialize` impl, a `Visitor` struct, and a `Deserialize` impl, parameterised by a
 * slice constructor and a vec constructor. The macro is invoked twice:
 *
 *  - `serde_impl!(Bytes,    BytesVisitor,    copyFromSlice, from)`
 *  - `serde_impl!(BytesMut, BytesMutVisitor, from,          fromVec)`
 *
 * Kotlin has no macros, so each invocation is translated as an explicit triple
 * — a `Type.serialize` extension function (matching the `Impls.kt` pattern in
 * `serde-kotlin`), a private `data object` [Visitor], and a public `data object`
 * [Deserialize] singleton. Kotlin cannot bolt a `Serialize` interface onto [Bytes] /
 * [BytesMut] from outside their declaring files, so the receiver-binding lives on the
 * extension instead.
 */

/**
 * `DeserializeSeed` adapter that hands a single `u8` to a [SeqAccess]. The upstream
 * macro calls `seq.next_element()?` with the element type inferred to `u8`; in Kotlin
 * `SeqAccess.nextElement` is parameterised over a `DeserializeSeed`, so we wrap the
 * primitive [I8Deserialize] in one.
 */
private data object ByteSeed : DeserializeSeed<Byte> {
    override fun <D> deserialize(deserializer: D): Result<Byte>
        where D : Deserializer =
        I8Deserialize.deserialize(deserializer)
}

////////////////////////////////////////////////////////////////////////////////

public fun <Ok, E> Bytes.serialize(serializer: Serializer<Ok, E>): Result<Ok>
    where E : Error =
    serializer.serializeBytes(asSlice())

private data object BytesVisitor : Visitor<Bytes> {
    override fun expecting(): String = "byte array"

    override fun <A> visitSeq(seq: A): Result<Bytes>
        where A : SeqAccess {
        val len = min(seq.sizeHint() ?: 0, 4096)
        val values = ArrayList<Byte>(len)
        while (true) {
            val next = seq.nextElement(ByteSeed).fold(
                onSuccess = { it },
                onFailure = { return Result.failure(it) },
            ) ?: break
            values.add(next)
        }
        return Result.success(Bytes.from(values.toByteArray()))
    }

    override fun visitBytes(v: ByteArray): Result<Bytes> =
        Result.success(Bytes.copyFromSlice(v))

    override fun visitByteBuf(v: ByteArray): Result<Bytes> =
        Result.success(Bytes.from(v))

    override fun visitStr(v: String): Result<Bytes> =
        Result.success(Bytes.copyFromSlice(v.encodeToByteArray()))

    override fun visitString(v: String): Result<Bytes> =
        Result.success(Bytes.from(v.encodeToByteArray()))
}

public data object BytesDeserialize : Deserialize<Bytes> {
    override fun <D> deserialize(deserializer: D): Result<Bytes>
        where D : Deserializer =
        deserializer.deserializeByteBuf(BytesVisitor)
}

////////////////////////////////////////////////////////////////////////////////

public fun <Ok, E> BytesMut.serialize(serializer: Serializer<Ok, E>): Result<Ok>
    where E : Error =
    serializer.serializeBytes(asRef())

private data object BytesMutVisitor : Visitor<BytesMut> {
    override fun expecting(): String = "byte array"

    override fun <A> visitSeq(seq: A): Result<BytesMut>
        where A : SeqAccess {
        val len = min(seq.sizeHint() ?: 0, 4096)
        val values = ArrayList<Byte>(len)
        while (true) {
            val next = seq.nextElement(ByteSeed).fold(
                onSuccess = { it },
                onFailure = { return Result.failure(it) },
            ) ?: break
            values.add(next)
        }
        return Result.success(BytesMut.from(values.toByteArray()))
    }

    override fun visitBytes(v: ByteArray): Result<BytesMut> =
        Result.success(BytesMut.from(v))

    override fun visitByteBuf(v: ByteArray): Result<BytesMut> =
        Result.success(BytesMut.from(v))

    override fun visitStr(v: String): Result<BytesMut> =
        Result.success(BytesMut.from(v.encodeToByteArray()))

    override fun visitString(v: String): Result<BytesMut> =
        Result.success(BytesMut.from(v.encodeToByteArray()))
}

public data object BytesMutDeserialize : Deserialize<BytesMut> {
    override fun <D> deserialize(deserializer: D): Result<BytesMut>
        where D : Deserializer =
        deserializer.deserializeByteBuf(BytesMutVisitor)
}
