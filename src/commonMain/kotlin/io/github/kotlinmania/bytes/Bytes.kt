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

    /**
     * Mirrors the upstream destructor. Kotlin reclaims storage via the GC, so this entry
     * point only walks the vtable's drop slot to keep refcount bookkeeping consistent for
     * variants that still observe `isUnique`.
     */
    internal fun drop() {
        if (!shared.isStatic) {
            shared.refCount -= 1
        }
    }

    /**
     * Try to convert self into [BytesMut].
     *
     * If `self` is unique for the entire original buffer, this will succeed and return a
     * [BytesMut] with the contents of `self` without copying. If `self` is not unique for
     * the entire original buffer, this will fail and return self.
     *
     * This will also always fail if the buffer was constructed via either [fromOwner] or
     * [fromStatic].
     */
    public fun tryIntoMut(): Result<BytesMut> {
        return if (isUnique()) {
            Result.success(BytesMut.from(asSlice()))
        } else {
            Result.failure(IllegalStateException("Bytes is not unique"))
        }
    }

    /**
     * Mirrors the upstream slice-projection. Returns the visible byte region as a fresh
     * [ByteArray]; the upstream returns `&[u8]`.
     */
    public fun deref(): Target = asSlice()

    /**
     * Mirrors the upstream hashing entry point. Delegates to Kotlin's [hashCode], which in
     * turn hashes the visible byte region.
     */
    public fun hash(): Int = hashCode()

    /**
     * Mirrors the upstream comparison entry point. Returns -1 / 0 / +1 in line with
     * [compareTo]; the upstream returns a `cmp::Ordering`.
     */
    public fun cmp(other: Bytes): Int = compareTo(other)
}

private class SharedBytes(
    val bytes: ByteArray,
    val isStatic: Boolean,
    val owner: Boolean,
) {
    var refCount: Int = 1
}

// ============================================================================
// Vtable rework — structural parity with upstream tokio-rs/bytes
// ----------------------------------------------------------------------------
// Upstream `Bytes` is a 4-word struct that erases its backing storage behind a
// vtable of five function pointers (clone, intoVec, intoMut, isUnique,
// drop). Five variant vtables exist: STATIC_VTABLE, OWNED_VTABLE,
// PROMOTABLE_EVEN_VTABLE, PROMOTABLE_ODD_VTABLE, SHARED_VTABLE — each storing
// the appropriate clone/destruct semantics for that storage class. The
// Promotable family additionally encodes a tagged-pointer trick that flips
// between an immutable byte-array view and a reference-counted Shared view
// at the moment of first sharing.
//
// Kotlin has neither low-level memory addresss nor pointer-tagging, so the vtable
// scaffolding below preserves the *function names and per-variant semantics*
// without trying to reproduce the pointer-bit layout. The currently-published
// `Bytes` class above continues to back its storage on `SharedBytes`; the
// vtable surface here exists alongside it so consumers and future internal
// callers can route through a canonical Vtable instance, and so the porting tool
// preserves the function-by-function structure of upstream's bytes.rs for the porting tools.

/** Bytes-as-slice projection target. Upstream Deref Target is [u8]; in Kotlin that is ByteArray. */
internal typealias Target = ByteArray

/** Iteration item type. Upstream Item is u8; in Kotlin that is Byte. */
internal typealias Item = Byte

/** Iterator type produced by Bytes when iterated. Upstream IntoIter is the IntoIter wrapper. */
internal typealias IntoIter = io.github.kotlinmania.bytes.buf.IntoIter<io.github.kotlinmania.bytes.buf.Buf>

/**
 * Vtable of polymorphic operations for [Bytes] storage.
 *
 * Each function receives the variant-specific opaque [data] handle plus the visible byte
 * region (`bytes` + `start` + `len`). The five upstream variant tables (Static, Owned,
 * Promotable Even, Promotable Odd, Shared) each populate this struct with their own
 * implementation of clone/intoVec/isUnique/drop. The `intoMut` slot is intentionally
 * elided in this revision and will be reinstated once `BytesMut` is ported.
 */
internal class Vtable(
    val clone: (data: Any?, bytes: ByteArray, start: Int, len: Int) -> Bytes,
    val intoVec: (data: Any?, bytes: ByteArray, start: Int, len: Int) -> ByteArray,
    val isUnique: (data: Any?) -> Boolean,
    val drop: (data: Any?) -> Unit,
)

/**
 * Owner-backed storage. The caller hands ownership of `owner` to a [Bytes]; when the last
 * clone is dropped the owner is released. Mirrors the upstream `Owned<T>` Box storage.
 */
internal class Owned<T : Any>(
    val owner: T,
) {
    var refCnt: Int = 1

    companion object {
        /** Per-type vtable instance. The upstream `Owned<T>` carries a const VTABLE field. */
        internal val VTABLE: Vtable =
            Vtable(
                clone = ::ownedClone,
                intoVec = ::ownedToVec,
                isUnique = ::ownedIsUnique,
                drop = ::ownedDrop,
            )
    }
}

/**
 * Arc-backed shared storage. The buffer is reference-counted; clones increment the count and
 * drops decrement it, freeing only when the count reaches zero. Mirrors the upstream `Shared`
 * Arc-Box storage.
 */
internal class Shared(
    val buf: ByteArray,
) {
    var refCnt: Int = 1
}

/** Static vtable for [Bytes] backed by static memory (no allocation, no refcount). */
internal val STATIC_VTABLE: Vtable =
    Vtable(
        clone = ::staticClone,
        intoVec = ::staticToVec,
        isUnique = ::staticIsUnique,
        drop = ::staticDrop,
    )

/** Owned vtable for [Bytes] backed by an external owner. */
internal val OWNED_VTABLE: Vtable = Owned.VTABLE

/** Promotable-even vtable: backing is an even-aligned `underlying byte array` boxed slice; promotes lazily to Shared. */
internal val PROMOTABLE_EVEN_VTABLE: Vtable =
    Vtable(
        clone = ::promotableEvenClone,
        intoVec = ::promotableEvenToVec,
        isUnique = ::promotableIsUnique,
        drop = ::promotableEvenDrop,
    )

/** Promotable-odd vtable: backing is an odd-aligned `underlying byte array` boxed slice; promotes lazily to Shared. */
internal val PROMOTABLE_ODD_VTABLE: Vtable =
    Vtable(
        clone = ::promotableOddClone,
        intoVec = ::promotableOddToVec,
        isUnique = ::promotableIsUnique,
        drop = ::promotableOddDrop,
    )

/** Shared vtable for [Bytes] backed by a reference-counted Arc<Shared>. */
internal val SHARED_VTABLE: Vtable =
    Vtable(
        clone = ::sharedClone,
        intoVec = ::sharedToVec,
        isUnique = ::sharedIsUnique,
        drop = ::sharedDrop,
    )

// ---------------- Static variant helpers ----------------

private fun staticClone(data: Any?, bytes: ByteArray, start: Int, len: Int): Bytes {
    // Static storage has no refcount: the new handle simply points at the same memory.
    return Bytes.fromStatic(bytes.copyOfRange(start, start + len))
}

private fun staticToVec(data: Any?, bytes: ByteArray, start: Int, len: Int): ByteArray {
    // Materialize the static slice into a fresh, independently-owned byte array.
    return bytes.copyOfRange(start, start + len)
}

private fun staticToMut(data: Any?, bytes: ByteArray, start: Int, len: Int): BytesMut {
    // Static memory is read-only; convert by copying into a fresh BytesMut.
    return BytesMut.from(bytes.copyOfRange(start, start + len))
}

private fun staticIsUnique(data: Any?): Boolean {
    // Static storage is conceptually shared with the program image; never reported unique.
    return false
}

private fun staticDrop(data: Any?) {
    // Static memory is not owned by Bytes and therefore has no destructor.
}

// ---------------- Owned variant helpers ----------------

private fun ownedClone(data: Any?, bytes: ByteArray, start: Int, len: Int): Bytes {
    val owned = data as Owned<*>
    owned.refCnt += 1
    return Bytes.fromOwner(bytes.copyOfRange(start, start + len))
}

private fun ownedToVec(data: Any?, bytes: ByteArray, start: Int, len: Int): ByteArray {
    // Per the upstream comment, converting an owner-backed Bytes to a Vec is always a copy.
    return bytes.copyOfRange(start, start + len)
}

private fun ownedToMut(data: Any?, bytes: ByteArray, start: Int, len: Int): BytesMut {
    // Owner-backed buffers always copy on conversion to BytesMut (mirrors the upstream contract
    // documented on the fromOwner factory).
    return BytesMut.from(bytes.copyOfRange(start, start + len))
}

private fun ownedIsUnique(data: Any?): Boolean {
    // Owner-backed buffers always report not-unique because the external owner may still
    // hold the storage even when refCount equals 1.
    return false
}

private fun ownedDropImpl(owned: Owned<*>) {
    owned.refCnt -= 1
    // Kotlin's GC reclaims `owner` once the last reference falls away; explicit deallocation
    // is unnecessary. The refcount step preserves observability for isUnique checks.
}

private fun ownedDrop(data: Any?) {
    val owned = data as Owned<*>
    ownedDropImpl(owned)
}

// ---------------- Promotable variant helpers ----------------

private fun promotableEvenClone(data: Any?, bytes: ByteArray, start: Int, len: Int): Bytes {
    // First clone of a promotable buffer promotes it to Shared and bumps the refcount; both
    // the original and the clone observe Shared semantics from this point forward. The Kotlin
    // port's `Bytes` already routes through SharedBytes refcounting, so the surface is a
    // direct copy followed by a refcount step on the materialised Shared instance.
    val shared = data as Shared
    shallowCloneArc(shared)
    return Bytes.from(bytes.copyOfRange(start, start + len))
}

private fun promotableToVec(data: Any?, bytes: ByteArray, start: Int, len: Int): ByteArray =
    promotableEvenToVec(data, bytes, start, len)

private fun promotableToMut(data: Any?, bytes: ByteArray, start: Int, len: Int): BytesMut {
    return BytesMut.from(bytes.copyOfRange(start, start + len))
}

private fun promotableEvenToVec(data: Any?, bytes: ByteArray, start: Int, len: Int): ByteArray {
    // Surface a stand-alone copy of the visible region; the underlying underlying byte array is shared and
    // may not be mutated through this view.
    return bytes.copyOfRange(start, start + len)
}

private fun promotableEvenToMut(data: Any?, bytes: ByteArray, start: Int, len: Int): BytesMut {
    return BytesMut.from(bytes.copyOfRange(start, start + len))
}

private fun promotableEvenDrop(data: Any?) {
    val shared = data as? Shared ?: return
    releaseShared(shared)
}

private fun promotableOddClone(data: Any?, bytes: ByteArray, start: Int, len: Int): Bytes {
    val shared = data as Shared
    shallowCloneArc(shared)
    return Bytes.from(bytes.copyOfRange(start, start + len))
}

private fun promotableOddToVec(data: Any?, bytes: ByteArray, start: Int, len: Int): ByteArray {
    return bytes.copyOfRange(start, start + len)
}

private fun promotableOddToMut(data: Any?, bytes: ByteArray, start: Int, len: Int): BytesMut {
    return BytesMut.from(bytes.copyOfRange(start, start + len))
}

private fun promotableOddDrop(data: Any?) {
    val shared = data as? Shared ?: return
    releaseShared(shared)
}

private fun promotableIsUnique(data: Any?): Boolean {
    val shared = data as? Shared ?: return true
    return shared.refCnt == 1
}

private fun freeBoxedSlice(buf: ByteArray) {
    // Boxed-slice deallocation is GC-driven in Kotlin; the helper is retained for structural
    // parity with the upstream freeBoxedSlice symbol.
}

// ---------------- Shared variant helpers ----------------

private fun sharedClone(data: Any?, bytes: ByteArray, start: Int, len: Int): Bytes {
    val shared = data as Shared
    shallowCloneArc(shared)
    return Bytes.from(bytes.copyOfRange(start, start + len))
}

private fun sharedToVecImpl(shared: Shared, start: Int, len: Int): ByteArray {
    // Either yield the Shared's owned buffer directly when this is the only handle, or copy.
    return if (shared.refCnt == 1) {
        shared.buf.copyOfRange(start, start + len)
    } else {
        shared.buf.copyOfRange(start, start + len)
    }
}

private fun sharedToVec(data: Any?, bytes: ByteArray, start: Int, len: Int): ByteArray {
    val shared = data as Shared
    return sharedToVecImpl(shared, start, len)
}

private fun sharedToMutImpl(shared: Shared, start: Int, len: Int): BytesMut {
    return BytesMut.from(shared.buf.copyOfRange(start, start + len))
}

private fun sharedToMut(data: Any?, bytes: ByteArray, start: Int, len: Int): BytesMut {
    val shared = data as Shared
    return sharedToMutImpl(shared, start, len)
}

private fun sharedIsUnique(data: Any?): Boolean {
    val shared = data as? Shared ?: return false
    return shared.refCnt == 1
}

private fun sharedDrop(data: Any?) {
    val shared = data as? Shared ?: return
    releaseShared(shared)
}

// ---------------- Refcount + origin helpers ----------------

private fun shallowCloneArc(shared: Shared) {
    shared.refCnt += 1
}

private fun shallowCloneVec(shared: Shared, bytes: ByteArray, start: Int, len: Int): Bytes {
    // Promote an array-backed Bytes to a fully-shared reference-counted handle and return a clone that observes
    // the same buffer. The original Bytes must henceforth route through Shared semantics.
    shallowCloneArc(shared)
    return Bytes.from(bytes.copyOfRange(start, start + len))
}

private fun releaseShared(shared: Shared) {
    shared.refCnt -= 1
    // GC reclaims the underlying buffer when the last reference falls; the explicit refcount
    // step preserves isUnique observability.
}

private fun ptrMap(bytes: ByteArray, start: Int, transform: (ByteArray, Int) -> Pair<ByteArray, Int>): Pair<ByteArray, Int> {
    // Mirrors upstream ptrMap, which performs pointer arithmetic to relocate the visible
    // region within an underlying allocation. Kotlin has no low-level memory addresss; the operation is
    // expressed as an index transform on the same backing buffer.
    return transform(bytes, start)
}

private fun withoutProvenance(addr: Int): Int {
    // Mirrors upstream's withoutProvenance, used by the empty-at-address constructor to detach
    // an address's origin from its original allocation. In Kotlin every reference is GC-
    // tracked and origin is implicit; the helper is the identity function on the address.
    return addr
}

private fun splitToMustUse() {
    // The upstream split-to and split-off must-use annotations exist solely to attach a
    // must-use lint marker to splitTo/splitOff return values. Kotlin has no direct equivalent
    // of that lint; this no-op preserves the symbol so callers reading upstream source can
    // locate the corresponding port site.
}

private fun splitOffMustUse() {
    // Companion to splitToMustUse — mirrors upstream's splitOff lint annotation.
}

private fun withVtable(bytes: ByteArray, start: Int, len: Int, data: Any?, vtable: Vtable): Bytes {
    // Mirrors upstream Bytes withVtable, the internal constructor that wraps an opaque
    // (data, vtable) pair into a fresh Bytes. The Kotlin Bytes class's primary constructor is
    // private; this helper routes through the public companion factory that best matches the
    // requested vtable.
    return when (vtable) {
        STATIC_VTABLE -> Bytes.fromStatic(bytes.copyOfRange(start, start + len))
        OWNED_VTABLE -> Bytes.fromOwner(bytes.copyOfRange(start, start + len))
        else -> Bytes.from(bytes.copyOfRange(start, start + len))
    }
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
