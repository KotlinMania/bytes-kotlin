// port-lint: source bytes_mut.rs
package io.github.kotlinmania.bytes

import io.github.kotlinmania.bytes.buf.Buf
import io.github.kotlinmania.bytes.buf.BufMut
import io.github.kotlinmania.bytes.buf.UninitSlice
import io.github.kotlinmania.bytes.fmt.BytesRef
import io.github.kotlinmania.bytes.fmt.debugBytesRef
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.Serializer

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
internal const val KIND_ARC: Int = 0b0
internal const val KIND_VEC: Int = 0b1

private const val MAX_ORIGINAL_CAPACITY_WIDTH: Int = 17
private const val MIN_ORIGINAL_CAPACITY_WIDTH: Int = 10

public class BytesMut private constructor(
    private var data: ByteArray,
    private var start: Int,
    private var length: Int,
    private var kindTag: Int = KIND_VEC,
    private var sharedState: Shared? = null,
    private var originalCapRepr: Int = originalCapacityToRepr(data.size),
) : Buf,
    BufMut,
    Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serialize(this, serializer)

    public companion object : Deserialize<BytesMut> {
        override fun <D> deserialize(deserializer: D): SerdeResult<BytesMut> where D : Deserializer =
            deserializeBytesMut(deserializer)

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
         * Creates a [BytesMut] directly from an existing [vec] buffer without copying.
         */
        internal fun fromVec(vec: ByteArray): BytesMut =
            BytesMut(
                data = vec,
                start = 0,
                length = vec.size,
                kindTag = KIND_VEC,
                sharedState = null,
                originalCapRepr = originalCapacityToRepr(vec.size),
            )

        /**
         * Creates a [BytesMut] from a byte slice, copying the bytes.
         */
        public fun from(src: ByteArray): BytesMut = fromVec(src.copyOf())

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
        val otherStart = start + at
        val otherLength = if (length > at) length - at else 0
        val otherCapacity = capacity() - at
        val tail = BytesMut(data.copyOfRange(otherStart, otherStart + otherCapacity), 0, otherLength)
        length = minOf(length, at)
        data = data.copyOfRange(start, start + at)
        start = 0
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
        kindTag = KIND_VEC
        sharedState = null
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
        if (tryUnsplit(other).isFailure) {
            extendFromSlice(other.asSlice())
        }
    }

    internal fun tryUnsplit(other: BytesMut): Result<Unit> {
        if (other.capacity() == 0) {
            return Result.success(Unit)
        }
        if (length == 0 && kind() == KIND_VEC) {
            data = other.data
            start = other.start
            length = other.length
            kindTag = other.kindTag
            sharedState = other.sharedState
            originalCapRepr = other.originalCapRepr
            return Result.success(Unit)
        }
        val currentShared = sharedState
        val otherShared = other.sharedState
        if (kind() == KIND_ARC &&
            other.kind() == KIND_ARC &&
            currentShared != null &&
            currentShared === otherShared &&
            start + length == other.start
        ) {
            length += other.length
            return Result.success(Unit)
        }
        return Result.failure(IllegalStateException("BytesMut buffers cannot be unsplit in O(1)"))
    }

    internal fun kind(): Int = kindTag

    internal fun getVecPos(): Int = start

    internal fun setVecPos(pos: Int) {
        start = pos
    }

    internal fun promoteToShared(refCnt: Int = 1) {
        require(refCnt == 1 || refCnt == 2) { "invalid refCnt for promoteToShared: $refCnt" }
        val currentShared = sharedState
        if (kind() == KIND_ARC && currentShared != null) {
            return
        }
        val rebuilt = rebuildVec(data, start + length, originalCapacityFromRepr(originalCapRepr).coerceAtLeast(capacity()))
        val shared = Shared(rebuilt).apply { this.refCnt = refCnt }
        data = rebuilt
        sharedState = shared
        kindTag = KIND_ARC
    }

    internal fun shallowClone(): BytesMut {
        if (kind() == KIND_ARC) {
            incrementShared(checkNotNull(sharedState))
        } else {
            promoteToShared(refCnt = 2)
        }
        return BytesMut(
            data = data,
            start = start,
            length = length,
            kindTag = kindTag,
            sharedState = sharedState,
            originalCapRepr = originalCapRepr,
        )
    }

    internal fun advanceUnchecked(cnt: Int) {
        if (cnt == 0) {
            return
        }
        if (kind() == KIND_VEC) {
            setVecPos(getVecPos() + cnt)
        } else {
            start += cnt
        }
        length = (length - cnt).coerceAtLeast(0)
    }

    private fun asSlice(): ByteArray = data.copyOfRange(start, start + length)

    internal fun asSliceMut(): ByteArray = data

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

    public fun derefMut(): ByteArray = asSliceMut()

    public fun asRef(): ByteArray = asSlice()

    public operator fun get(index: Int): Byte {
        require(index in 0 until length) { "index out of bounds: $index" }
        return data[start + index]
    }

    public operator fun set(index: Int, value: Byte) {
        require(index in 0 until length) { "index out of bounds: $index" }
        data[start + index] = value
    }

    public fun asMut(): ByteArray = asSliceMut()

    public fun borrow(): ByteArray = asSlice()

    public fun borrowMut(): ByteArray = asSliceMut()

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
        advanceUnchecked(cnt)
    }

    override fun copyToBytes(len: Int): Bytes {
        val out = splitTo(len)
        return out.freeze()
    }

    // ---------------- BufMut impl ----------------

    override fun remainingMut(): Int = (Int.MAX_VALUE - length).coerceAtLeast(0)

    override fun chunkMut(): UninitSlice {
        // Reserve 64 bytes when capacity is exhausted, otherwise expose existing spare capacity.
        if (capacity() == len()) {
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

internal fun incrementShared(shared: Shared) {
    shared.refCnt += 1
}

private fun releaseShared(shared: Shared) {
    shared.refCnt -= 1
}

internal fun isUniqueShared(shared: Shared): Boolean = shared.refCnt == 1

internal fun originalCapacityToRepr(cap: Int): Int {
    if (cap <= 0) {
        return 0
    }
    val width = 32 - (cap.toUInt() shr MIN_ORIGINAL_CAPACITY_WIDTH).countLeadingZeroBits()
    return minOf(width, MAX_ORIGINAL_CAPACITY_WIDTH - MIN_ORIGINAL_CAPACITY_WIDTH)
}

internal fun originalCapacityFromRepr(repr: Int): Int {
    if (repr == 0) {
        return 0
    }
    return 1 shl (repr + (MIN_ORIGINAL_CAPACITY_WIDTH - 1))
}

internal fun rebuildVec(
    buf: ByteArray,
    len: Int,
    cap: Int,
): ByteArray {
    val targetSize = maxOf(buf.size, len, cap)
    return if (targetSize == buf.size) buf.copyOf() else buf.copyOf(targetSize)
}

private fun sharedVClone(
    data: Any?,
    bytes: ByteArray,
    start: Int,
    len: Int,
): Bytes {
    val shared = data as Shared
    incrementShared(shared)
    return Bytes.withVtable(bytes, start, len, shared, SHARED_VTABLE)
}

private fun sharedVToVec(
    data: Any?,
    bytes: ByteArray,
    start: Int,
    len: Int,
): ByteArray {
    val shared = data as Shared
    val out = shared.buf.copyOfRange(start, start + len)
    releaseShared(shared)
    return out
}

private fun sharedVToMut(
    data: Any?,
    bytes: ByteArray,
    start: Int,
    len: Int,
): BytesMut {
    val shared = data as Shared
    if (isUniqueShared(shared)) {
        return BytesMut.fromVec(shared.buf.copyOfRange(start, start + len))
    }
    val copied = shared.buf.copyOfRange(start, start + len)
    releaseShared(shared)
    return BytesMut.fromVec(copied)
}

private fun sharedVIsUnique(data: Any?): Boolean {
    val shared = data as? Shared ?: return false
    return isUniqueShared(shared)
}

private fun sharedVDrop(data: Any?) {
    val shared = data as? Shared ?: return
    releaseShared(shared)
}

/** Shared vtable for frozen [BytesMut] storage backed by [Shared]. */
internal val SHARED_BYTES_MUT_VTABLE: Vtable =
    Vtable(
        clone = ::sharedVClone,
        intoVec = ::sharedVToVec,
        intoMut = ::sharedVToMut,
        isUnique = ::sharedVIsUnique,
        drop = ::sharedVDrop,
    )
