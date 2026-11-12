import Testing
import Bytes

// Exercise concrete exported buffer behavior, rather than module import alone.
@Suite struct BytesExportTests {
    @Test func testBytesFactoriesSlicesAndIndependentCursors() {
        let factory = ExportedKotlinPackages.io.github.kotlinmania.bytes.Bytes.Companion.shared
        let empty = factory.new()
        #expect(empty.isEmpty())
        #expect(empty.len() == 0)

        let bytes = factory.from(slice: "Hello world")
        #expect(bytes.len() == 11)
        #expect(!bytes.isEmpty())
        #expect(bytes.eq(other: "Hello world"))
        #expect(bytes.asString() == "Hello world")

        let slice = bytes.slice(begin: 0, end: 5)
        #expect(slice.eq(other: "Hello"))
        #expect(bytes.eq(other: "Hello world"))

        let prefix = bytes.splitTo(at: 6)
        #expect(prefix.len() == 6)
        #expect(prefix.eq(other: "Hello "))
        #expect(bytes.len() == 5)
        #expect(bytes.eq(other: "world"))
        #expect(slice.eq(other: "Hello"))

        prefix.advance(cnt: 1)
        #expect(prefix.eq(other: "ello "))
        #expect(bytes.eq(other: "world"))
        #expect(slice.eq(other: "Hello"))
    }

    @Test func testBytesMutWritesIntegerEndiannessAndFreezesContents() {
        let bytes = ExportedKotlinPackages.io.github.kotlinmania.bytes.BytesMut.Companion.shared
            .withCapacity(capacity: 8)
        #expect(bytes.isEmpty())
        bytes.putU16(n: 0x1234)
        bytes.putU16Le(n: 0x5678)
        bytes.putU32(n: 0x9ABCDEF0)
        #expect(bytes.len() == 8)

        let frozen = bytes.freeze()
        let expected: [UInt8] = [0x12, 0x34, 0x78, 0x56, 0x9A, 0xBC, 0xDE, 0xF0]
        #expect(frozen.len() == 8)
        for index in expected.indices {
            #expect(UInt8(bitPattern: frozen[Int32(index)]) == expected[index])
        }
    }

    @Test func testBytesMutReadsIntegerEndianness() {
        let bytes = ExportedKotlinPackages.io.github.kotlinmania.bytes.BytesMut.Companion.shared
            .from(src: "\u{12}4xVABCD")
        #expect(bytes.remaining() == 8)
        let bigEndian16 = bytes.getU16()
        let littleEndian16 = bytes.getU16Le()
        let bigEndian32 = bytes.getU32()
        #expect(bigEndian16 == 0x1234)
        #expect(littleEndian16 == 0x5678)
        #expect(bigEndian32 == 0x41424344)
        #expect(bytes.remaining() == 0)
        #expect(bytes.isEmpty())
    }

    @Test func testU128FromSwift() {
        let u128 = ExportedKotlinPackages.io.github.kotlinmania.bytes.U128(
            high: 10,
            low: 20
        )
        #expect(u128.high == 10)
        #expect(u128.low == 20)

        let zero = ExportedKotlinPackages.io.github.kotlinmania.bytes.U128.Companion.shared.ZERO
        #expect(zero.high == 0)
        #expect(zero.low == 0)

        #expect(!u128.equals(other: zero))
    }
}

