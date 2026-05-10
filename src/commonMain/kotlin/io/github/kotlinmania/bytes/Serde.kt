// port-lint: source serde.rs
package io.github.kotlinmania.bytes

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * The upstream Rust file defines a `serde_impl!` macro that emits, for a given type,
 * a `Serialize` impl, a `Visitor` struct, and a `Deserialize` impl. The macro is
 * parameterised by a slice constructor and a vec constructor, and is invoked twice:
 *
 *  - `serde_impl!(Bytes,    BytesVisitor,    copyFromSlice, from)`
 *  - `serde_impl!(BytesMut, BytesMutVisitor, from,          fromVec)`
 *
 * Kotlin has no macros, so each invocation is translated as an explicit
 * [KSerializer] singleton below. The visitor's accept-everything posture
 * (`visitSeq`, `visitBytes`, `visitByteBuf`, `visitStr`, `visitString`) collapses
 * to a delegation to the kotlinx-serialization built-in [ByteArraySerializer],
 * which surfaces the format's native byte payload to the format itself.
 */

/**
 * `Serialize` / `Deserialize` for [Bytes].
 *
 * The upstream `Serialize` impl forwards to `serializer.serialize_bytes(&self)`. The
 * `Deserialize` impl runs `deserializer.deserialize_byte_buf(BytesVisitor)`, where
 * the visitor accepts a `seq` of `u8`, a `&[u8]`, a `Vec<u8>`, a `&str`, or a
 * `String`, copying the borrowed forms via `Bytes::copy_from_slice` and adopting
 * the owned forms via `Bytes::from`. In Kotlin the byte payload is read through
 * [ByteArraySerializer] and wrapped via [Bytes.copyFromSlice].
 */
public object BytesSerializer : KSerializer<Bytes> {
    private val delegate = ByteArraySerializer()

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: Bytes) {
        delegate.serialize(encoder, value.asSlice())
    }

    override fun deserialize(decoder: Decoder): Bytes {
        return Bytes.copyFromSlice(delegate.deserialize(decoder))
    }
}

/**
 * `Serialize` / `Deserialize` for [BytesMut].
 *
 * Mirrors [BytesSerializer]. Upstream the visitor copies borrowed input via
 * `BytesMut::from` and adopts owned input via `BytesMut::from_vec`. In Kotlin
 * every constructor takes a [ByteArray], so both upstream paths collapse to
 * [BytesMut.from].
 */
public object BytesMutSerializer : KSerializer<BytesMut> {
    private val delegate = ByteArraySerializer()

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: BytesMut) {
        delegate.serialize(encoder, value.asRef())
    }

    override fun deserialize(decoder: Decoder): BytesMut {
        return BytesMut.from(delegate.deserialize(decoder))
    }
}
