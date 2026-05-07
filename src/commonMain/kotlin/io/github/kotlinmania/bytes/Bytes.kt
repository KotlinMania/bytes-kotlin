// port-lint: source bytes.rs
package io.github.kotlinmania.bytes

import io.github.kotlinmania.bytes.fmt.BytesRef
import io.github.kotlinmania.bytes.fmt.debugBytesRef

/**
 * A cheaply cloneable and sliceable chunk of contiguous memory.
 *
 * `Bytes` is an efficient container for storing and operating on contiguous
 * slices of memory. It is intended for use primarily in networking code, but
 * could have applications elsewhere as well.
 *
 * `Bytes` values facilitate zero-copy network programming by allowing multiple
 * `Bytes` objects to point to the same underlying memory.
 *
 * `Bytes` does not have a single implementation. It is an interface, whose
 * exact behavior is implemented through dynamic dispatch in several underlying
 * implementations of `Bytes`.
 *
 * All `Bytes` implementations must fulfill the following requirements:
 * - They are cheaply cloneable and thereby shareable between an unlimited amount
 *   of components, for example by modifying a reference count.
 * - Instances can be sliced to refer to a subset of the original buffer.
 *
 * ```
 * val mem = Bytes.from("Hello world")
 * val a = mem.slice(0, 5)
 *
 * check(a.eq("Hello"))
 *
 * val b = mem.splitTo(6)
 *
 * check(mem.eq("world"))
 * check(b.eq("Hello "))
 * ```
 *
 * # Memory layout
 *
 * The upstream `Bytes` struct itself is fairly small, limited to 4 integer-sized
 * fields used to track information about which segment of the underlying memory
 * the `Bytes` handle has access to.
 *
 * `Bytes` keeps both a pointer to the shared state containing the full memory
 * slice and a pointer to the start of the region visible by the handle.
 * `Bytes` also tracks the length of its view into the memory.
 *
 * # Sharing
 *
 * `Bytes` contains a vtable, which allows implementations of `Bytes` to define
 * how sharing/cloning is implemented in detail.
 * When `Bytes.clone()` is called, `Bytes` will call the vtable function for
 * cloning the backing storage in order to share it behind multiple `Bytes`
 * instances.
 *
 * For `Bytes` implementations which refer to constant memory, the cloning
 * implementation will be a no-op.
 *
 * For `Bytes` implementations which point to a reference counted shared storage,
 * sharing will be implemented by increasing the reference count.
 *
 * Due to this mechanism, multiple `Bytes` instances may point to the same
 * shared memory region. Each `Bytes` instance can point to different sections
 * within that memory region, and `Bytes` instances may or may not have
 * overlapping views into the memory.
 */
public class Bytes private constructor(
    private val shared: SharedBytes,
    private var start: Int,
    private var length: Int,
) : Iterable<Byte>,
    Comparable<Bytes> {
    public companion object {
        /**
         * Creates a new empty `Bytes`.
         *
         * This will not allocate and the returned `Bytes` handle will be empty.
         *
         * # Examples
         *
         * ```
         * val b = Bytes.new()
         * check(b.asSlice().isEmpty())
         * ```
         */
        public fun new(): Bytes {
            val empty = ByteArray(0)
            return fromStatic(empty)
        }

        /**
         * Creates a new `Bytes` from a static slice.
         *
         * The returned `Bytes` will point directly to the static slice. There is
         * no allocating or copying.
         *
         * # Examples
         *
         * ```
         * val b = Bytes.fromStatic("hello".encodeToByteArray())
         * check(b.eq("hello"))
         * ```
         */
        public fun fromStatic(bytes: ByteArray): Bytes {
            return Bytes(SharedBytes(bytes.copyOf(), isStatic = true, owner = false), 0, bytes.size)
        }

        /**
         * Creates a new `Bytes` from a static string.
         */
        public fun fromStatic(bytes: String): Bytes {
            return fromStatic(bytes.encodeToByteArray())
        }

        /**
         * Creates a new `Bytes` with length zero and the given pointer as the address.
         */
        private fun newEmptyWithPtr(index: Int): Bytes {
            require(index >= 0)
            return Bytes(SharedBytes(ByteArray(0), isStatic = true, owner = false), 0, 0)
        }

        /**
         * Create `Bytes` with a buffer whose lifespan is controlled via an explicit owner.
         *
         * The owner will be transferred to the constructed `Bytes` object, which
         * will ensure it is dropped once all remaining clones of the constructed
         * object are dropped. Kotlin owns the copied immutable byte storage
         * directly.
         */
        public fun fromOwner(owner: ByteArray): Bytes {
            return Bytes(SharedBytes(owner.copyOf(), isStatic = false, owner = true), 0, owner.size)
        }

        /**
         * Create `Bytes` with string owner data.
         */
        public fun fromOwner(owner: String): Bytes {
            return fromOwner(owner.encodeToByteArray())
        }

        /**
         * Creates `Bytes` instance from slice, by copying it.
         */
        public fun copyFromSlice(data: ByteArray): Bytes {
            return from(data)
        }

        public fun from(slice: ByteArray): Bytes {
            return Bytes(SharedBytes(slice.copyOf(), isStatic = false, owner = false), 0, slice.size)
        }

        public fun from(slice: String): Bytes {
            return from(slice.encodeToByteArray())
        }

        public fun from(slice: Iterable<Byte>): Bytes {
            return from(slice.toList().toByteArray())
        }

        public fun fromIter(intoIter: Iterable<Byte>): Bytes {
            return from(intoIter)
        }

        public fun default(): Bytes {
            return new()
        }
    }

    /**
     * Returns the number of bytes contained in this `Bytes`.
     *
     * # Examples
     *
     * ```
     * val b = Bytes.from("hello")
     * check(b.len() == 5)
     * ```
     */
    public fun len(): Int {
        return length
    }

    /**
     * Returns true if the `Bytes` has a length of 0.
     *
     * # Examples
     *
     * ```
     * val b = Bytes.new()
     * check(b.isEmpty())
     * ```
     */
    public fun isEmpty(): Boolean {
        return length == 0
    }

    /**
     * Returns true if this is the only reference to the data and conversion to
     * mutable bytes would avoid cloning the underlying buffer.
     *
     * Always returns false if the data is backed by a static slice or an owner.
     */
    public fun isUnique(): Boolean {
        return !shared.isStatic && !shared.owner && shared.refCount == 1
    }

    /**
     * Returns a slice of self for the provided range.
     *
     * This will increment the reference count for the underlying memory and
     * return a new `Bytes` handle set to the slice.
     *
     * This operation is `O(1)`.
     *
     * # Panics
     *
     * Requires that `begin <= end` and `end <= len()`, otherwise slicing
     * will panic.
     */
    public fun slice(begin: Int = 0, end: Int = len()): Bytes {
        require(begin <= end) {
            "range start must not be greater than end: $begin <= $end"
        }
        require(end <= len()) {
            "range end out of bounds: $end <= ${len()}"
        }

        if (end == begin) {
            return newEmptyWithPtr(start + begin)
        }

        val ret = clone()
        ret.length = end - begin
        ret.start += begin
        return ret
    }

    public fun slice(range: IntRange): Bytes {
        if (range.isEmpty()) {
            return slice(range.first, range.first)
        }
        return slice(range.first, range.last + 1)
    }

    /**
     * Returns a slice of self that is equivalent to the given `subset`.
     *
     * This function turns that byte slice into another `Bytes`, as if one had
     * called `slice()` with the offsets that correspond to `subset`.
     *
     * # Panics
     *
     * Requires that the given `sub` slice is in fact contained within the
     * `Bytes` buffer; otherwise this function will panic.
     */
    public fun sliceRef(subset: ByteArray): Bytes {
        if (subset.isEmpty()) {
            return new()
        }

        val bytes = asSlice()
        val offset = bytes.indexOfSubslice(subset)
        require(offset >= 0) {
            "subset is out of bounds"
        }
        return slice(offset, offset + subset.size)
    }

    /**
     * Splits the bytes into two at the given index.
     *
     * Afterwards `self` contains elements `[0, at)`, and the returned `Bytes`
     * contains elements `[at, len)`.
     *
     * This is an `O(1)` operation that just increases the reference count and
     * sets a few indices.
     *
     * # Panics
     *
     * Panics if `at > len`.
     */
    public fun splitOff(at: Int): Bytes {
        if (at == len()) {
            return newEmptyWithPtr(start + at)
        }

        if (at == 0) {
            val ret = clone()
            clear()
            return ret
        }

        require(at <= len()) {
            "split_off out of bounds: $at <= ${len()}"
        }

        val ret = clone()
        length = at
        ret.incStart(at)
        return ret
    }

    /**
     * Splits the bytes into two at the given index.
     *
     * Afterwards `self` contains elements `[at, len)`, and the returned
     * `Bytes` contains elements `[0, at)`.
     *
     * This is an `O(1)` operation that just increases the reference count and
     * sets a few indices.
     *
     * # Panics
     *
     * Panics if `at > len`.
     */
    public fun splitTo(at: Int): Bytes {
        if (at == len()) {
            val ret = clone()
            clear()
            return ret
        }

        if (at == 0) {
            return newEmptyWithPtr(start)
        }

        require(at <= len()) {
            "split_to out of bounds: $at <= ${len()}"
        }

        val ret = clone()
        incStart(at)
        ret.length = at
        return ret
    }

    /**
     * Shortens the buffer, keeping the first `len` bytes and dropping the rest.
     *
     * If `len` is greater than the buffer's current length, this has no effect.
     */
    public fun truncate(len: Int) {
        if (len < length) {
            length = len
        }
    }

    /**
     * Clears the buffer, removing all data.
     */
    public fun clear() {
        truncate(0)
    }

    /**
     * Returns the immutable visible byte slice.
     */
    public fun asSlice(): ByteArray {
        return shared.bytes.copyOfRange(start, start + length)
    }

    public fun asRef(): ByteArray {
        return asSlice()
    }

    public fun asString(): String {
        return asSlice().decodeToString()
    }

    public fun remaining(): Int {
        return len()
    }

    public fun chunk(): ByteArray {
        return asSlice()
    }

    public fun advance(cnt: Int) {
        require(cnt <= len()) {
            "cannot advance past `remaining`: $cnt <= ${len()}"
        }
        incStart(cnt)
    }

    public fun copyToBytes(len: Int): Bytes {
        return splitTo(len)
    }

    public fun clone(): Bytes {
        shared.refCount += 1
        return Bytes(shared, start, length)
    }

    private fun incStart(by: Int) {
        check(length >= by) {
            "internal: inc_start out of bounds"
        }
        length -= by
        start += by
    }

    override fun iterator(): Iterator<Byte> {
        return asSlice().iterator()
    }

    public fun intoIter(): Iterator<Byte> {
        return iterator()
    }

    public fun eq(other: Bytes): Boolean {
        return asSlice().contentEquals(other.asSlice())
    }

    public fun eq(other: ByteArray): Boolean {
        return asSlice().contentEquals(other)
    }

    public fun eq(other: String): Boolean {
        return asSlice().contentEquals(other.encodeToByteArray())
    }

    override fun equals(other: Any?): Boolean {
        return other is Bytes && eq(other)
    }

    override fun hashCode(): Int {
        return asSlice().contentHashCode()
    }

    override fun compareTo(other: Bytes): Int {
        return compareByteArrays(asSlice(), other.asSlice())
    }

    public fun partialCmp(other: Bytes): Int {
        return compareTo(other)
    }

    public fun partialCmp(other: ByteArray): Int {
        return compareByteArrays(asSlice(), other)
    }

    public fun partialCmp(other: String): Int {
        return compareByteArrays(asSlice(), other.encodeToByteArray())
    }

    public fun borrow(): ByteArray {
        return asSlice()
    }

    public fun fmt(): String {
        return debugBytesRef(BytesRef(asSlice()))
    }

    override fun toString(): String {
        return fmt()
    }
}

private class SharedBytes(
    val bytes: ByteArray,
    val isStatic: Boolean,
    val owner: Boolean,
) {
    var refCount: Int = 1
}

private fun Iterable<Byte>.toByteArray(): ByteArray {
    val bytes = toList()
    val out = ByteArray(bytes.size)
    for (index in bytes.indices) {
        out[index] = bytes[index]
    }
    return out
}

private fun ByteArray.indexOfSubslice(subset: ByteArray): Int {
    if (subset.isEmpty()) {
        return 0
    }
    if (subset.size > size) {
        return -1
    }
    for (candidate in 0..(size - subset.size)) {
        var matched = true
        for (index in subset.indices) {
            if (this[candidate + index] != subset[index]) {
                matched = false
                break
            }
        }
        if (matched) {
            return candidate
        }
    }
    return -1
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
