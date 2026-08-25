// port-lint: source tests/test_bytes.rs
package io.github.kotlinmania.bytes

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BytesTest {
    @Test
    fun bytesCloningVec() {
        val a = Bytes.from("abcdefgh")

        // Test that Bytes.clone can share the same immutable byte storage.
        val a1 = a.clone()
        val a2 = a1.clone()

        val b = a1.clone()
        assertEquals(a.asSlice().toList(), b.asSlice().toList())
        assertTrue(b.eq(a2))
    }

    @Test
    fun fromSliceAndEquality() {
        val bytes = "abcdefgh".encodeToByteArray()
        val a = Bytes.from(bytes)
        assertTrue(a.eq(bytes))
        assertTrue(a.eq("abcdefgh"))
        assertContentEquals(bytes, a.asSlice())
        assertEquals(8, a.len())
        assertFalse(a.isEmpty())

        val empty = Bytes.from(ByteArray(0))
        assertTrue(empty.isEmpty())
        assertEquals(0, empty.len())
    }

    @Test
    fun fromStaticAndCopyFromSlice() {
        val long = "mary had a little lamb, little lamb, little lamb".encodeToByteArray()
        val short = "hello world".encodeToByteArray()

        val staticBytes = Bytes.fromStatic(short)
        assertTrue(staticBytes.eq(short))

        val copied = Bytes.copyFromSlice(long)
        assertTrue(copied.eq(long))
    }

    @Test
    fun lenAndEmpty() {
        val a = Bytes.from("abcdefg")
        assertEquals(7, a.len())
        assertFalse(a.isEmpty())

        val b = Bytes.from("")
        assertEquals(0, b.len())
        assertTrue(b.isEmpty())

        val c = Bytes.new()
        assertEquals(0, c.len())
        assertTrue(c.isEmpty())
    }

    @Test
    fun indexing() {
        val a = Bytes.from("hello world")
        assertEquals('h'.code.toByte(), a[0])
        assertEquals('e'.code.toByte(), a[1])
        assertEquals('d'.code.toByte(), a[10])
    }

    @Test
    fun slicing() {
        val a = Bytes.from("hello world")

        val b = a.slice(3, 5)
        assertTrue(b.eq("lo"))

        val c = a.slice(0, 0)
        assertTrue(c.eq(""))

        val d = a.slice(3, 3)
        assertTrue(d.eq(""))

        val e = a.slice(a.len(), a.len())
        assertTrue(e.eq(""))

        val f = a.slice(0, 5)
        assertTrue(f.eq("hello"))

        val g = a.slice(3, a.len())
        assertTrue(g.eq("lo world"))
    }

    @Test
    fun sliceOutOfBoundsPanics() {
        val a = Bytes.from("hello world")
        assertFails { a.slice(5, 44) }
        assertFails { a.slice(44, 49) }
        assertFails { a.slice(5, 3) }
    }

    @Test
    fun splitOff() {
        val hello = Bytes.from("helloworld")
        val world = hello.splitOff(5)

        assertTrue(hello.eq("hello"))
        assertTrue(world.eq("world"))
    }

    @Test
    fun splitOffOutOfBoundsPanics() {
        val hello = Bytes.from("helloworld")
        assertFails { hello.splitOff(44) }
    }

    @Test
    fun splitTo() {
        val short = "hello world".encodeToByteArray()
        val a = Bytes.fromStatic(short)
        val b = a.splitTo(4)

        assertTrue(a.eq(short.copyOfRange(4, short.size)))
        assertTrue(b.eq(short.copyOfRange(0, 4)))

        val long = "mary had a little lamb, little lamb, little lamb".encodeToByteArray()
        val c = Bytes.copyFromSlice(long)
        val d = c.splitTo(4)

        assertTrue(c.eq(long.copyOfRange(4, long.size)))
        assertTrue(d.eq(long.copyOfRange(0, 4)))
    }

    @Test
    fun splitToOutOfBoundsPanics() {
        val hello = Bytes.from("helloworld")
        assertFails { hello.splitTo(33) }
    }

    @Test
    fun splitOffToLoop() {
        val text = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val s = text.encodeToByteArray()

        for (i in 0..s.size) {
            val bytes1 = Bytes.from(s)
            val off1 = bytes1.splitOff(i)
            assertEquals(i, bytes1.len())
            val sum1 = bytes1.asSlice() + off1.asSlice()
            assertContentEquals(s, sum1)

            val bytes2 = Bytes.from(s)
            val off2 = bytes2.splitTo(i)
            assertEquals(i, off2.len())
            val sum2 = off2.asSlice() + bytes2.asSlice()
            assertContentEquals(s, sum2)
        }
    }

    @Test
    fun truncate() {
        val s = "helloworld".encodeToByteArray()
        val hello = Bytes.from(s)
        hello.truncate(15)
        assertTrue(hello.eq(s))
        hello.truncate(10)
        assertTrue(hello.eq(s))
        hello.truncate(5)
        assertTrue(hello.eq("hello"))
    }

    @Test
    fun freezeAndClones() {
        val s = "abcdefgh".encodeToByteArray()
        val b = BytesMut.from(s).freeze()
        assertTrue(b.eq(s))
        val c = b.clone()
        assertTrue(c.eq(s))
    }

    @Test
    fun freezeAfterAdvance() {
        val s = "abcdefgh".encodeToByteArray()
        val b = BytesMut.from(s)
        b.advance(1)
        assertContentEquals("bcdefgh".encodeToByteArray(), b.asRef())
        val frozen = b.freeze()
        assertTrue(frozen.eq("bcdefgh"))
    }

    @Test
    fun freezeAfterTruncate() {
        val s = "abcdefgh".encodeToByteArray()
        val b = BytesMut.from(s)
        b.truncate(7)
        assertContentEquals("abcdefg".encodeToByteArray(), b.asRef())
        val frozen = b.freeze()
        assertTrue(frozen.eq("abcdefg"))
    }

    @Test
    fun freezeAfterSplitOff() {
        val s = "abcdefgh".encodeToByteArray()
        val b = BytesMut.from(s)
        val unused = b.splitOff(7)
        assertContentEquals("abcdefg".encodeToByteArray(), b.asRef())
        val frozen = b.freeze()
        assertTrue(frozen.eq("abcdefg"))
    }

    @Test
    fun comparisonAndHashing() {
        val a = Bytes.from("apple")
        val b = Bytes.from("banana")
        val a2 = Bytes.from("apple")

        assertTrue(a < b)
        assertTrue(b > a)
        assertEquals(a, a2)
        assertEquals(a.hashCode(), a2.hashCode())
        assertNotEquals(a, b)
    }

    @Test
    fun iteration() {
        val bytes = Bytes.from("xyz")
        val collected = mutableListOf<Byte>()
        for (b in bytes) {
            collected.add(b)
        }
        assertContentEquals("xyz".encodeToByteArray(), collected.toByteArray())
    }
}

