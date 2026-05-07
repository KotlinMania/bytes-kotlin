// port-lint: source bytes.rs
package io.github.kotlinmania.bytes

import kotlin.test.Test
import kotlin.test.assertEquals
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
}
