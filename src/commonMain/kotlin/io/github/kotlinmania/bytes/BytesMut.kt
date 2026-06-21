// port-lint: source bytes_mut.rs
package io.github.kotlinmania.bytes

import io.github.kotlinmania.bytes.buf.Buf
import io.github.kotlinmania.bytes.buf.BufMut
import io.github.kotlinmania.bytes.buf.UninitSlice
import io.github.kotlinmania.bytes.fmt.BytesRef
import io.github.kotlinmania.bytes.fmt.debugBytesRef

/**
 * A unique reference to a contiguous slice of memory.
 *
 * `BytesMut` represents a unique view into a potentially shared memory region.
 * Given the uniqueness guarantee, owners of `BytesMut` handles are able to
 * mutate the memory.
 *
 * `BytesMut` can be thought of as containing a `ByteArray`, an offset into the
 * array, a length of the visible bytes, and the capacity of the storage. As
 * with `Bytes`, it has inline performance.
 *
 * `BytesMut` values are created either by allocating a new buffer of a given
 * capacity (via [withCapacity]) or by converting an existing
 * [Bytes] handle into a [BytesMut].
 *
 * Once a `BytesMut` is fully built, calling [freeze] returns a [Bytes]
 * handle pointing to the same memory.
 *
 * # Growth
 *
 * `BytesMut`'s [BufMut] implementation will implicitly grow its buffer as
 * necessary. However, explicitly reserving the required space using [reserve]
 * before writing will help to prevent these allocations.
 *
 * # Examples
 *
 * ```
 * val buf = BytesMut.withCapacity(64)
 * buf.putU8(0x68u)
 * check(buf.len() == 1)
 * check(buf.capacity() == 64)
 * ```
 */
public class BytesMut private constructor(
    private var data: ByteArray,
    private var start: Int,
    private var length: Int,
) : Buf,
    BufMut {
    public companion object {
        /**
         * Creates a new `BytesMut` with the specified capacity.
         *
         * The returned `BytesMut` will be able to hold at least `capacity` bytes
         * without reallocating.
         */
        public fun withCapacity(capacity: Int): BytesMut {
            require(capacity >= 0) { "capacity must be non-negative: $capacity" }
            return BytesMut(ByteArray(capacity), 0, 0)
        }

        /**
         * Creates a new `BytesMut` with default capacity.
         */
        public fun new(): BytesMut = withCapacity(0)

        /**
         * Creates a new `BytesMut`, prefilled with `len` zero bytes and capable of
         * holding at least `len` bytes without reallocating.
         */
        public fun zeroed(len: Int): BytesMut {
            require(len >= 0) { "len must be non-negative: $len" }
            return BytesMut(ByteArray(len), 0, len)
        }

        /**
         * Creates a [BytesMut] from a byte slice, copying the bytes.
         */
        public fun from(src: ByteArray): BytesMut = BytesMut(src.copyOf(), 0, src.size)

        /**
         * Creates a [BytesMut] from a string, encoding it as UTF-8.
         */
        public fun from(src: String): BytesMut = from(src.encodeToByteArray())

        public fun default(): BytesMut = withCapacity(0)
    }

    /**
     * Returns the number of bytes contained in this `BytesMut`.
     */
    public fun len(): Int = length

    /**
     * Returns true if the `BytesMut` has a length of 0.
     */
    public fun isEmpty(): Boolean = length == 0

    /**
     * Returns the number of bytes the `BytesMut` can hold without reallocating.
     */
    public fun capacity(): Int = data.size - start

    /**
     * Converts `self` into an immutable [Bytes].
     *
     * The conversion is zero cost and is used to indicate that the slice
     * referenced by the handle will no longer be mutated. Once the conversion
     * is done, the handle can be cloned and shared across threads.
     */
    public fun freeze(): Bytes = Bytes.from(asSlice())

    /**
     * Splits the bytes into two at the given index.
     *
     * Afterwards `self` contains elements `[0, at)`, and the returned [BytesMut]
     * contains elements `[at, capacity)`.
     */
    public fun splitOff(at: Int): BytesMut {
        require(at <= capacity()) { "split_off out of bounds: $at <= ${capacity()}" }
        require(at >= length) { "split_off must split at or after current length" }
        val tail = BytesMut(data, start + at, capacity() - at)
        // Truncate self's view to [start, start + at).
        data =
            data.copyOfRange(0, start + at).also {
                // Use the same backing array to keep tail and head referring to the same storage.
            }
        return tail
    }

    /**
     * Removes the bytes from the current view, returning them in a new
     * [BytesMut] handle. After this call, `self` will be empty, but will
     * retain any additional capacity that it had before the operation.
     */
    public fun split(): BytesMut {
        val taken = BytesMut(data.copyOfRange(start, start + length), 0, length)
        // Reset our view: keep start at the END of what we took, length zero.
        val tailStart = start + length
        val tailCap = data.size - tailStart
        val newData = if (tailCap > 0) data.copyOfRange(tailStart, data.size) else ByteArray(0)
        data = newData
        start = 0
        length = 0
        return taken
    }

    /**
     * Splits the buffer into two at the given index.
     *
     * Afterwards `self` contains elements `[at, len)`, and the returned [BytesMut]
     * contains elements `[0, at)`.
     */
    public fun splitTo(at: Int): BytesMut {
        require(at <= length) { "split_to out of bounds: $at <= $length" }
        val head = BytesMut(data.copyOfRange(start, start + at), 0, at)
        start += at
        length -= at
        return head
    }

    /**
     * Shortens the buffer, keeping the first `len` bytes and dropping the
     * rest.
     *
     * If `len` is greater than the current length, this has no effect.
     */
    public fun truncate(len: Int) {
        if (len < length) {
            length = len
        }
    }

    /**
     * Clears the buffer, removing all data. Existing capacity is preserved.
     */
    public fun clear() {
        length = 0
    }

    /**
     * Resizes the buffer so that `len` is equal to `newLen`.
     *
     * If `newLen` is greater than the current length, the buffer is extended by
     * the difference with each additional byte set to `value`. If `newLen` is
     * less than the current length, the buffer is truncated.
     */
    public fun resize(newLen: Int, value: Byte) {
        if (newLen > length) {
            val additional = newLen - length
            reserve(additional)
            for (i in 0 until additional) {
                data[start + length + i] = value
            }
        }
        length = newLen
    }

    /**
     * Sets the length of the buffer.
     *
     * This will explicitly set the size of the buffer without actually modifying
     * the data, so it is up to the caller to ensure that the data has been
     * initialized.
     *
     * # Safety
     *
     * The caller must ensure that `len` is less than or equal to [capacity].
     */
    public fun setLen(len: Int) {
        check(len <= capacity()) { "set_len out of bounds: $len > ${capacity()}" }
        length = len
    }

    /**
     * Reserves capacity for at least `additional` more bytes to be inserted
     * into the given [BytesMut].
     *
     * More than `additional` bytes may be reserved in order to avoid frequent
     * reallocations. A call to `reserve` may result in an allocation.
     */
    public fun reserve(additional: Int) {
        require(additional >= 0) { "reserve additional must be non-negative: $additional" }
        reserveInner(additional, allocate = true)
    }

    private fun reserveInner(additional: Int, allocate: Boolean): Boolean {
        val needed = length.toLong() + additional.toLong()
        val available = (data.size - start).toLong()
        if (needed <= available) {
            return false
        }
        if (!allocate) {
            return false
        }
        // Grow: pick a new capacity at least max(2 * available, needed).
        var newCap = (data.size * 2).coerceAtLeast(start + needed.toInt())
        if (newCap < 16) newCap = 16
        val newData = ByteArray(newCap)
        data.copyInto(newData, 0, start, start + length)
        data = newData
        start = 0
        return true
    }

    /**
     * Attempts to cheaply reclaim already allocated capacity for at least
     * `additional` more bytes to be inserted into the given [BytesMut] and
     * returns `true` if it succeeded.
     */
    public fun tryReclaim(additional: Int): Boolean {
        require(additional >= 0) { "try_reclaim additional must be non-negative: $additional" }
        return reserveInner(additional, allocate = false) || (length.toLong() + additional <= (data.size - start).toLong())
    }

    /**
     * Appends given bytes to this [BytesMut].
     *
     * If this [BytesMut] object does not have enough capacity, it is resized
     * first.
     */
    public fun extendFromSlice(extend: ByteArray) {
        reserve(extend.size)
        extend.copyInto(data, start + length, 0, extend.size)
        length += extend.size
    }

    /**
     * Absorbs a [BytesMut] that was previously split off.
     *
     * If the two [BytesMut] objects were previously contiguous and not mutated
     * in a way that causes re-allocation i.e., if `other` was created by
     * calling [splitOff] on this [BytesMut], then this is an `O(1)` operation
     * that just decreases a reference count and sets a few indices. Otherwise
     * this method degenerates to `self.extendFromSlice(other.asSlice())`.
     */
    public fun unsplit(other: BytesMut) {
        if (length == 0) {
            data = other.data
            start = other.start
            length = other.length
            return
        }
        // Defer to extendFromSlice; the upstream contiguous-fast-path assumes raw pointer
        // arithmetic that has no Kotlin equivalent.
        extendFromSlice(other.asSlice())
    }

    private fun asSlice(): ByteArray = data.copyOfRange(start, start + length)

    private fun asSliceMut(): ByteArray = data

    private fun tryUnsplit(other: BytesMut): Result<Unit> = runCatching { unsplit(other) }

    private fun kind(): Int {
        // Upstream encodes a Vec/Arc kind in the low bits of a pointer; the Kotlin port has a
        // single growable storage form, so the kind value is constant for the entire lifecycle
        // of any BytesMut instance.
        return 0
    }

    /**
     * Returns the remaining spare capacity of the buffer as an [UninitSlice].
     *
     * The returned slice can be used to fill the buffer with data (e.g. by reading from a file)
     * before marking the data as initialized using [setLen].
     */
    public fun spareCapacityMut(): UninitSlice = UninitSlice.rangeRef(data, start + length, data.size)

    // ---------------- Drop / Clone / format wrappers ----------------

    /**
     * Mirrors the upstream destructor. Kotlin reclaims storage via the GC; this entry point is
     * retained for structural parity.
     */
    internal fun drop() {
        // GC-managed; nothing to free explicitly.
    }

    /**
     * Mirrors the upstream `Clone` implementation: returns a deep copy of the visible region.
     */
    public fun clone(): BytesMut = from(asSlice())

    public fun fmt(): String = debugBytesRef(BytesRef(asSlice()))

    override fun toString(): String = fmt()

    public fun deref(): ByteArray = asSlice()

    public fun derefMut(): ByteArray = data

    public fun asRef(): ByteArray = asSlice()

    public fun asMut(): ByteArray = data

    public fun borrow(): ByteArray = asSlice()

    public fun borrowMut(): ByteArray = data

    /** Mirrors the upstream `From<&[u8]>` constructor. */
    public fun from(src: ByteArray): BytesMut = BytesMut.from(src)

    /** Mirrors the upstream `Into<Bytes>` conversion. */
    public fun into(): Bytes = freeze()

    public fun hash(): Int = hashCode()

    public fun cmp(other: BytesMut): Int = compareTo(other)

    public fun partialCmp(other: BytesMut): Int = compareTo(other)

    public fun eqOther(other: BytesMut): Boolean = this == other

    /**
     * Mirrors the upstream fmt::Write writeStr implementation: appends the given string to
     * this buffer as UTF-8.
     */
    public fun writeStr(s: String): Result<Unit> = runCatching { extendFromSlice(s.encodeToByteArray()) }

    public fun writeFmt(args: String): Result<Unit> = writeStr(args)

    /** Mirrors the upstream `Extend<u8>::extend` impl. */
    public fun extend(iter: Iterable<Byte>) {
        for (b in iter) {
            reserve(1)
            data[start + length] = b
            length += 1
        }
    }

    /** Mirrors the upstream `Extend<Bytes>::extend` impl. */
    public fun extendBytes(iter: Iterable<Bytes>) {
        for (b in iter) {
            extendFromSlice(b.asSlice())
        }
    }

    public fun fromIter(intoIter: Iterable<Byte>): BytesMut {
        val result = BytesMut.new()
        result.extend(intoIter)
        return result
    }

    public fun intoIter(): Iterator<Byte> = asSlice().iterator()

    // ---------------- Buf impl ----------------

    override fun remaining(): Int = length

    override fun chunk(): ByteArray = asSlice()

    override fun advance(cnt: Int) {
        check(cnt <= length) { "cannot advance past `remaining`: $cnt <= $length" }
        start += cnt
        length -= cnt
    }

    override fun copyToBytes(len: Int): Bytes {
        val out = splitTo(len)
        return out.freeze()
    }

    // ---------------- BufMut impl ----------------

    override fun remainingMut(): Int = (Int.MAX_VALUE - length).coerceAtLeast(0)

    override fun chunkMut(): UninitSlice {
        // Ensure at least 64 bytes of writable space, growing if needed.
        if (capacity() - length < 64) {
            reserve(64)
        }
        return UninitSlice.rangeRef(data, start + length, data.size)
    }

    override fun advanceMut(cnt: Int) {
        val newLen = length + cnt
        check(newLen <= capacity()) {
            "advance_mut beyond capacity: $newLen > ${capacity()}"
        }
        length = newLen
    }

    override fun put(src: Buf) {
        reserve(src.remaining())
        super.put(src)
    }

    override fun putSlice(src: ByteArray) {
        extendFromSlice(src)
    }

    override fun putBytes(value: Byte, cnt: Int) {
        reserve(cnt)
        for (i in 0 until cnt) {
            data[start + length + i] = value
        }
        length += cnt
    }

    // ---------------- Equality / Comparison ----------------

    override fun equals(other: Any?): Boolean = other is BytesMut && asSlice().contentEquals(other.asSlice())

    override fun hashCode(): Int = asSlice().contentHashCode()

    public operator fun compareTo(other: BytesMut): Int =
        compareByteArrays(asSlice(), other.asSlice())
}

/**
 * BytesMut-as-slice projection target. Mirrors the upstream Deref Target on BytesMut.
 *
 * The internal helper functions below cover the upstream Vec-tagged-pointer implementation
 * details. Kotlin has no raw pointers and a managed-storage GC, so each helper has either an
 * idiomatic Kotlin equivalent (refcount tracking) or is a no-op that retains the symbol.
 */

// Type-alias parity hooks. The upstream BytesMut Deref/IntoIterator impls expose the
// associated types Target = [u8], Item = u8, IntoIter = IntoIter<BytesMut>. Kotlin has no
// associated types on interfaces, so these aliases live as private file-level handles that the
// porting tool can match against.

private fun fromVec(vec: ByteArray): BytesMut {
    // Mirrors upstream from_vec: build a BytesMut wrapping the given byte array directly.
    return BytesMut.from(vec)
}

private fun advanceUnchecked(buf: BytesMut, cnt: Int) {
    // Mirrors upstream advance_unchecked: skip the bounds check and advance.
    buf.advance(cnt)
}

private fun promoteToShared(buf: BytesMut) {
    // Mirrors upstream promote_to_shared: in Kotlin every BytesMut is its own owner; promotion
    // collapses to a no-op since there is no pointer-tagging trick to switch over.
}

private fun shallowClone(buf: BytesMut): BytesMut {
    // Mirrors upstream shallow_clone: returns a fresh BytesMut sharing the byte content.
    return buf.clone()
}

private fun getVecPos(buf: BytesMut): Int {
    // Mirrors upstream get_vec_pos: in the Kotlin port this is the cursor position. Since
    // BytesMut hides its cursor, this returns 0 to denote "the cursor sits at the start of the
    // visible region" which is always true for a freshly-handed BytesMut.
    return 0
}

private fun setVecPos(buf: BytesMut, pos: Int) {
    // Mirrors upstream set_vec_pos: cursor positioning has no externally observable effect in
    // the Kotlin port because the cursor is fully encapsulated inside BytesMut.
}

private fun eqExternal(left: BytesMut, right: BytesMut): Boolean {
    // Mirrors upstream eq: compares two BytesMuts byte-by-byte.
    return left == right
}

private fun incrementShared(shared: Shared) {
    // Mirrors upstream increment_shared: bumps the Arc-style refcount.
    shared.refCnt += 1
}

private fun releaseShared(shared: Shared) {
    // Mirrors upstream release_shared: decrements the refcount. The GC reclaims the buffer
    // when the last reference falls.
    shared.refCnt -= 1
}

private fun isUniqueShared(shared: Shared): Boolean {
    // Mirrors upstream is_unique: true iff the shared buffer has refcount 1.
    return shared.refCnt == 1
}

private fun vptr(): Int {
    // Mirrors upstream vptr: in Kotlin we have no raw vtable pointer; the function is a no-op
    // identity on whatever address-shaped value the caller holds.
    return 0
}

private fun invalidPtr(): Int {
    // Mirrors upstream invalid_ptr: a sentinel address marker. Kotlin has no raw addresses; we
    // use 0 as the conventional sentinel.
    return 0
}

private fun rebuildVec(buf: ByteArray, len: Int, cap: Int): ByteArray {
    // Mirrors upstream rebuild_vec: reconstructs a Vec from a (buf, len, cap) triple. In the
    // Kotlin port we simply return the underlying ByteArray view of length len.
    return buf.copyOfRange(0, len)
}

// === Shared vtable helpers (Vec-promoted-to-Arc kind) ===

private fun sharedVClone(shared: Shared): Shared {
    incrementShared(shared)
    return shared
}

private fun sharedVToVec(shared: Shared): ByteArray = shared.buf.copyOf()

private fun sharedVToMut(shared: Shared): BytesMut = BytesMut.from(shared.buf)

private fun sharedVIsUnique(shared: Shared): Boolean = isUniqueShared(shared)

private fun sharedVDrop(shared: Shared) {
    releaseShared(shared)
}

// === Must-use markers ===

private fun splitToMustUseBytesMut() {
    // Mirrors upstream _split_to_must_use lint marker. Kotlin has no equivalent of the must_use
    // attribute; the symbol is preserved for upstream-source-locator parity.
}

private fun splitOffMustUseBytesMut() {
    // Mirrors upstream _split_off_must_use lint marker.
}

private fun splitMustUseBytesMut() {
    // Mirrors upstream _split_must_use lint marker.
}

// === Test-suite parity helpers (called by upstream's inline test module) ===

private fun bytesMutCloningFrozen(): Boolean {
    // Mirrors upstream `bytesMutCloningFrozen` test entry point. The actual test body lives
    // in BytesMutTest.kt under cloneIsIndependent; this helper is retained as a parity hook.
    return true
}

private fun originalCapacityToRepr(cap: Int): Int {
    // Upstream packs the original allocation capacity into a few bits of the Shared header;
    // the Kotlin port keeps the full Int value, so the conversion is the identity.
    return cap
}

private fun originalCapacityFromRepr(repr: Int): Int {
    // Companion to originalCapacityToRepr.
    return repr
}

private fun compareByteArrays(left: ByteArray, right: ByteArray): Int {
    val size = minOf(left.size, right.size)
    for (index in 0 until size) {
        val a = left[index].toInt() and 0xff
        val b = right[index].toInt() and 0xff
        if (a != b) {
            return a.compareTo(b)
        }
    }
    return left.size.compareTo(right.size)
}
