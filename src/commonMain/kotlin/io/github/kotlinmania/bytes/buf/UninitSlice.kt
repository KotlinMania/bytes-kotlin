// port-lint: source buf/uninit_slice.rs
package io.github.kotlinmania.bytes.buf

/**
 * Uninitialized byte slice.
 *
 * Returned by `BufMut.chunkMut()`, the referenced byte slice may be
 * uninitialized. The wrapper provides safe access without introducing
 * undefined behavior.
 *
 * The safety invariants of this wrapper are:
 *
 * 1. Reading from an `UninitSlice` is undefined behavior.
 * 2. Writing uninitialized bytes to an `UninitSlice` is undefined behavior.
 *
 * The difference between `UninitSlice` and a mutable list of maybe-uninitialized bytes is
 * that it is possible in safe code to write uninitialized bytes to the latter, which this type
 * prohibits.
 */
public class UninitSlice private constructor(
    private val bytes: ByteArray,
    private val start: Int,
    private val end: Int,
) {
    public companion object {
        /**
         * Creates an `UninitSlice` wrapping a slice of initialised memory.
         *
         * # Examples
         *
         * ```
         * val buffer = ByteArray(64)
         * val slice = UninitSlice.new(buffer)
         * ```
         */
        public fun new(slice: ByteArray): UninitSlice =
            UninitSlice(slice, 0, slice.size)

        /**
         * Creates an `UninitSlice` wrapping a slice of uninitialised memory.
         *
         * # Examples
         *
         * ```
         * val buffer = ByteArray(64)
         * val slice = UninitSlice.uninit(buffer)
         *
         * val vec = ByteArray(1024)
         * val spare: UninitSlice = UninitSlice.uninit(vec)
         * ```
         */
        public fun uninit(slice: ByteArray): UninitSlice =
            UninitSlice(slice, 0, slice.size)

        internal fun uninitRef(slice: ByteArray): UninitSlice =
            UninitSlice(slice, 0, slice.size)

        /**
         * Creates an `UninitSlice` that references a sub-range of the given byte array
         * **without copying**. Writes via [writeByte] or [copyFromSlice] mutate `bytes` directly.
         *
         * Used by [BufMut] implementations to expose a writable window over their internal
         * storage. `bytes` is captured by reference; the returned slice retains a view over it
         * while the parent [BufMut] is still in use.
         */
        internal fun rangeRef(bytes: ByteArray, start: Int, end: Int): UninitSlice {
            require(start in 0..bytes.size)
            require(end in start..bytes.size)
            return UninitSlice(bytes, start, end)
        }

        /**
         * Create an `UninitSlice` from a buffer and a length.
         *
         * # Safety
         *
         * The caller must ensure that `bytes` is caller-owned storage while the returned
         * wrapper is in use.
         *
         * # Examples
         *
         * ```
         * val bytes = "hello world".encodeToByteArray()
         * val len = bytes.size
         *
         * val slice = UninitSlice.fromRawPartsMut(bytes, len)
         * ```
         */
        public fun fromRawPartsMut(bytes: ByteArray, len: Int): UninitSlice {
            require(len in 0..bytes.size)
            return UninitSlice(bytes, 0, len)
        }

        public fun from(slice: ByteArray): UninitSlice =
            new(slice)
    }

    /**
     * Write a single byte at the specified offset.
     *
     * # Panics
     *
     * The function panics if `index` is out of bounds.
     *
     * # Examples
     *
     * ```
     * val data = "foo".encodeToByteArray()
     * val slice = UninitSlice.fromRawPartsMut(data, 3)
     *
     * slice.writeByte(0, 98)
     *
     * check("boo" == data.decodeToString())
     * ```
     */
    public fun writeByte(index: Int, byte: Byte) {
        require(index in 0 until len())
        bytes[start + index] = byte
    }

    /**
     * Copies bytes from `src` into `this`.
     *
     * The length of `src` must be the same as `this`.
     *
     * # Panics
     *
     * The function panics if `src` has a different length than `this`.
     *
     * # Examples
     *
     * ```
     * val data = "foo".encodeToByteArray()
     * val slice = UninitSlice.fromRawPartsMut(data, 3)
     *
     * slice.copyFromSlice("bar".encodeToByteArray())
     *
     * check("bar" == data.decodeToString())
     * ```
     */
    public fun copyFromSlice(src: ByteArray) {
        require(len() == src.size)
        src.copyInto(bytes, start, 0, src.size)
    }

    /**
     * Return the backing buffer for the slice.
     *
     * # Safety
     *
     * The caller **must not** read from the referenced memory and **must not**
     * write **uninitialized** bytes to the slice either.
     *
     * # Examples
     *
     * ```
     * val data = byteArrayOf(0, 1, 2)
     * val slice = UninitSlice.new(data)
     * val ptr = slice.asMutPtr()
     * ```
     */
    public fun asMutPtr(): ByteArray = bytes

    /**
     * Return the mutable byte storage for this slice buffer.
     *
     * # Safety
     *
     * The caller **must not** read from the referenced memory and **must not** write
     * **uninitialized** bytes to the slice either. This is because `BufMut` implementation
     * that created the `UninitSlice` knows which parts are initialized. Writing uninitialized
     * bytes to the slice may cause the `BufMut` to read those bytes and trigger undefined
     * behavior.
     *
     * # Examples
     *
     * ```
     * val data = byteArrayOf(0, 1, 2)
     * val uninitSlice = UninitSlice.new(data).asUninitSliceMut()
     * ```
     */
    public fun asUninitSliceMut(): ByteArray = bytes

    /**
     * Returns the number of bytes in the slice.
     *
     * # Examples
     *
     * ```
     * val data = byteArrayOf(0, 1, 2)
     * val slice = UninitSlice.new(data)
     * val len = slice.len()
     *
     * check(len == 3)
     * ```
     */
    public fun len(): Int = end - start

    override fun toString(): String = "UninitSlice[...]"

    public fun fmt(): String = toString()

    public operator fun get(index: IntRange): UninitSlice {
        val boundedStart = start + index.first
        val boundedEnd = start + index.last + 1
        require(boundedStart >= start)
        require(boundedEnd <= end)
        return UninitSlice(bytes, boundedStart, boundedEnd)
    }
}
