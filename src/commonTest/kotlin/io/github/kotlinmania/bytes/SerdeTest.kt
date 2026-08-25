// port-lint: source tests/test_serde.rs
package io.github.kotlinmania.bytes

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.SeqAccess
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.ser.Impossible
import io.github.kotlinmania.serdecore.ser.SerializeMap
import io.github.kotlinmania.serdecore.ser.SerializeSeq
import io.github.kotlinmania.serdecore.ser.SerializeStruct
import io.github.kotlinmania.serdecore.ser.SerializeStructVariant
import io.github.kotlinmania.serdecore.ser.SerializeTuple
import io.github.kotlinmania.serdecore.ser.SerializeTupleStruct
import io.github.kotlinmania.serdecore.ser.SerializeTupleVariant
import io.github.kotlinmania.serdecore.ser.Serializer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SerdeTest {
    private class TestByteSerializer : Serializer<ByteArray> {
        override fun serializeBool(v: Boolean): SerdeResult<ByteArray> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun serializeI8(v: Byte): SerdeResult<ByteArray> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun serializeI16(v: Short): SerdeResult<ByteArray> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun serializeI32(v: Int): SerdeResult<ByteArray> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun serializeI64(v: Long): SerdeResult<ByteArray> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun serializeU8(v: UByte): SerdeResult<ByteArray> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun serializeU16(v: UShort): SerdeResult<ByteArray> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun serializeU32(v: UInt): SerdeResult<ByteArray> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun serializeU64(v: ULong): SerdeResult<ByteArray> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun serializeF32(v: Float): SerdeResult<ByteArray> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun serializeF64(v: Double): SerdeResult<ByteArray> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun serializeChar(v: Char): SerdeResult<ByteArray> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun serializeStr(v: String): SerdeResult<ByteArray> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun serializeBytes(v: ByteArray): SerdeResult<ByteArray> = SerdeResult.success(v.copyOf())
        override fun serializeNone(): SerdeResult<ByteArray> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun <T> serializeSome(value: T): SerdeResult<ByteArray> where T : io.github.kotlinmania.serdecore.ser.Serialize = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun serializeUnit(): SerdeResult<ByteArray> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun serializeUnitStruct(name: String): SerdeResult<ByteArray> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun serializeUnitVariant(name: String, variantIndex: UInt, variant: String): SerdeResult<ByteArray> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun <T> serializeNewtypeStruct(name: String, value: T): SerdeResult<ByteArray> where T : io.github.kotlinmania.serdecore.ser.Serialize = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun <T> serializeNewtypeVariant(name: String, variantIndex: UInt, variant: String, value: T): SerdeResult<ByteArray> where T : io.github.kotlinmania.serdecore.ser.Serialize = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun serializeSeq(len: Int?): SerdeResult<SerializeSeq<ByteArray>> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun serializeTuple(len: Int): SerdeResult<SerializeTuple<ByteArray>> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun serializeTupleStruct(name: String, len: Int): SerdeResult<SerializeTupleStruct<ByteArray>> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun serializeTupleVariant(name: String, variantIndex: UInt, variant: String, len: Int): SerdeResult<SerializeTupleVariant<ByteArray>> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun serializeMap(len: Int?): SerdeResult<SerializeMap<ByteArray>> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun serializeStruct(name: String, len: Int): SerdeResult<SerializeStruct<ByteArray>> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun serializeStructVariant(name: String, variantIndex: UInt, variant: String, len: Int): SerdeResult<SerializeStructVariant<ByteArray>> = SerdeResult.failure(SerdeError.custom("unsupported"))
    }

    private class TestByteDeserializer(private val data: ByteArray) : Deserializer {
        override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = deserializeBytes(visitor)
        override fun <V> deserializeBool(visitor: Visitor<V>): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun <V> deserializeI8(visitor: Visitor<V>): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun <V> deserializeI16(visitor: Visitor<V>): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun <V> deserializeI32(visitor: Visitor<V>): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun <V> deserializeI64(visitor: Visitor<V>): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun <V> deserializeU8(visitor: Visitor<V>): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun <V> deserializeU16(visitor: Visitor<V>): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun <V> deserializeU32(visitor: Visitor<V>): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun <V> deserializeU64(visitor: Visitor<V>): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun <V> deserializeF32(visitor: Visitor<V>): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun <V> deserializeF64(visitor: Visitor<V>): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun <V> deserializeChar(visitor: Visitor<V>): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun <V> deserializeStr(visitor: Visitor<V>): SerdeResult<V> = visitor.visitStr(data.decodeToString())
        override fun <V> deserializeString(visitor: Visitor<V>): SerdeResult<V> = visitor.visitString(data.decodeToString())
        override fun <V> deserializeBytes(visitor: Visitor<V>): SerdeResult<V> = visitor.visitBytes(data)
        override fun <V> deserializeByteBuf(visitor: Visitor<V>): SerdeResult<V> = visitor.visitByteBuf(data)
        override fun <V> deserializeOption(visitor: Visitor<V>): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun <V> deserializeUnit(visitor: Visitor<V>): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun <V> deserializeSeq(visitor: Visitor<V>): SerdeResult<V> {
            var index = 0
            val seqAccess = object : SeqAccess {
                override fun <T> nextElementSeed(seed: io.github.kotlinmania.serdecore.de.DeserializeSeed<T>): SerdeResult<T?> {
                    if (index >= data.size) return SerdeResult.success(null)
                    val b = data[index++]
                    val byteDeserializer = object : Deserializer {
                        override fun <V2> deserializeAny(visitor: Visitor<V2>): SerdeResult<V2> = deserializeI8(visitor)
                        override fun <V2> deserializeBool(visitor: Visitor<V2>): SerdeResult<V2> = SerdeResult.failure(SerdeError.custom("unsupported"))
                        override fun <V2> deserializeI8(visitor: Visitor<V2>): SerdeResult<V2> = visitor.visitI8(b)
                        override fun <V2> deserializeI16(visitor: Visitor<V2>): SerdeResult<V2> = visitor.visitI16(b.toShort())
                        override fun <V2> deserializeI32(visitor: Visitor<V2>): SerdeResult<V2> = visitor.visitI32(b.toInt())
                        override fun <V2> deserializeI64(visitor: Visitor<V2>): SerdeResult<V2> = visitor.visitI64(b.toLong())
                        override fun <V2> deserializeU8(visitor: Visitor<V2>): SerdeResult<V2> = visitor.visitU8(b.toUByte())
                        override fun <V2> deserializeU16(visitor: Visitor<V2>): SerdeResult<V2> = visitor.visitU16(b.toUShort())
                        override fun <V2> deserializeU32(visitor: Visitor<V2>): SerdeResult<V2> = visitor.visitU32(b.toUInt())
                        override fun <V2> deserializeU64(visitor: Visitor<V2>): SerdeResult<V2> = visitor.visitU64(b.toULong())
                        override fun <V2> deserializeF32(visitor: Visitor<V2>): SerdeResult<V2> = SerdeResult.failure(SerdeError.custom("unsupported"))
                        override fun <V2> deserializeF64(visitor: Visitor<V2>): SerdeResult<V2> = SerdeResult.failure(SerdeError.custom("unsupported"))
                        override fun <V2> deserializeChar(visitor: Visitor<V2>): SerdeResult<V2> = SerdeResult.failure(SerdeError.custom("unsupported"))
                        override fun <V2> deserializeStr(visitor: Visitor<V2>): SerdeResult<V2> = SerdeResult.failure(SerdeError.custom("unsupported"))
                        override fun <V2> deserializeString(visitor: Visitor<V2>): SerdeResult<V2> = SerdeResult.failure(SerdeError.custom("unsupported"))
                        override fun <V2> deserializeBytes(visitor: Visitor<V2>): SerdeResult<V2> = SerdeResult.failure(SerdeError.custom("unsupported"))
                        override fun <V2> deserializeByteBuf(visitor: Visitor<V2>): SerdeResult<V2> = SerdeResult.failure(SerdeError.custom("unsupported"))
                        override fun <V2> deserializeOption(visitor: Visitor<V2>): SerdeResult<V2> = SerdeResult.failure(SerdeError.custom("unsupported"))
                        override fun <V2> deserializeUnit(visitor: Visitor<V2>): SerdeResult<V2> = SerdeResult.failure(SerdeError.custom("unsupported"))
                        override fun <V2> deserializeUnitStruct(name: String, visitor: Visitor<V2>): SerdeResult<V2> = SerdeResult.failure(SerdeError.custom("unsupported"))
                        override fun <V2> deserializeNewtypeStruct(name: String, visitor: Visitor<V2>): SerdeResult<V2> = SerdeResult.failure(SerdeError.custom("unsupported"))
                        override fun <V2> deserializeSeq(visitor: Visitor<V2>): SerdeResult<V2> = SerdeResult.failure(SerdeError.custom("unsupported"))
                        override fun <V2> deserializeTuple(len: Int, visitor: Visitor<V2>): SerdeResult<V2> = SerdeResult.failure(SerdeError.custom("unsupported"))
                        override fun <V2> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V2>): SerdeResult<V2> = SerdeResult.failure(SerdeError.custom("unsupported"))
                        override fun <V2> deserializeMap(visitor: Visitor<V2>): SerdeResult<V2> = SerdeResult.failure(SerdeError.custom("unsupported"))
                        override fun <V2> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V2>): SerdeResult<V2> = SerdeResult.failure(SerdeError.custom("unsupported"))
                        override fun <V2> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V2>): SerdeResult<V2> = SerdeResult.failure(SerdeError.custom("unsupported"))
                        override fun <V2> deserializeIdentifier(visitor: Visitor<V2>): SerdeResult<V2> = SerdeResult.failure(SerdeError.custom("unsupported"))
                        override fun <V2> deserializeIgnoredAny(visitor: Visitor<V2>): SerdeResult<V2> = SerdeResult.failure(SerdeError.custom("unsupported"))
                    }
                    return seed.deserialize(byteDeserializer).map { it }
                }
            }
            return visitor.visitSeq(seqAccess)
        }
        override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun <V> deserializeMap(visitor: Visitor<V>): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun <V> deserializeIdentifier(visitor: Visitor<V>): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("unsupported"))
        override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): SerdeResult<V> = SerdeResult.failure(SerdeError.custom("unsupported"))
    }

    @Test
    fun testSerDeEmpty() {
        val b = Bytes.new()
        val serResult = b.serialize(TestByteSerializer())
        assertTrue(serResult.isSuccess)
        assertContentEquals(ByteArray(0), serResult.getOrThrow())

        val deResult = BytesDeserialize.deserialize(TestByteDeserializer(ByteArray(0)))
        assertTrue(deResult.isSuccess)
        assertEquals(0, deResult.getOrThrow().len())

        val bm = BytesMut.withCapacity(0)
        val serBmResult = bm.serialize(TestByteSerializer())
        assertTrue(serBmResult.isSuccess)
        assertContentEquals(ByteArray(0), serBmResult.getOrThrow())

        val deBmResult = BytesMutDeserialize.deserialize(TestByteDeserializer(ByteArray(0)))
        assertTrue(deBmResult.isSuccess)
        assertEquals(0, deBmResult.getOrThrow().len())
    }

    @Test
    fun testSerDe() {
        val raw = "bytes".encodeToByteArray()
        val b = Bytes.from(raw)
        val serResult = b.serialize(TestByteSerializer())
        assertTrue(serResult.isSuccess)
        assertContentEquals(raw, serResult.getOrThrow())

        val deResult = BytesDeserialize.deserialize(TestByteDeserializer(raw))
        assertTrue(deResult.isSuccess)
        assertTrue(deResult.getOrThrow().eq("bytes"))

        val bm = BytesMut.from(raw)
        val serBmResult = bm.serialize(TestByteSerializer())
        assertTrue(serBmResult.isSuccess)
        assertContentEquals(raw, serBmResult.getOrThrow())

        val deBmResult = BytesMutDeserialize.deserialize(TestByteDeserializer(raw))
        assertTrue(deBmResult.isSuccess)
        assertContentEquals(raw, deBmResult.getOrThrow().asRef())
    }
}

