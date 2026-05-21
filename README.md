# bytes-kotlin in Kotlin

[![GitHub link](https://img.shields.io/badge/GitHub-KotlinMania%2Fbytes--kotlin-blue.svg)](https://github.com/KotlinMania/bytes-kotlin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kotlinmania/bytes-kotlin)](https://central.sonatype.com/artifact/io.github.kotlinmania/bytes-kotlin)
[![Build status](https://img.shields.io/github/actions/workflow/status/KotlinMania/bytes-kotlin/ci.yml?branch=main)](https://github.com/KotlinMania/bytes-kotlin/actions)

This is a Kotlin Multiplatform line-by-line transliteration port of [`tokio-rs/bytes`](https://github.com/tokio-rs/bytes).

**Original Project:** This port is based on [`tokio-rs/bytes`](https://github.com/tokio-rs/bytes). All design credit and project intent belong to the upstream authors; this repository is a faithful port to Kotlin Multiplatform with no behavioural changes intended.

The upstream README and license text are treated as upstream-authored source documents. This repository adds Kotlin-port wrapper sections, absolute-link edits, and port-specific notices while keeping upstream authorship attached to the original text.

### Porting status

This is an **in-progress port**. The goal is feature parity with the upstream Rust crate while providing a native Kotlin Multiplatform API. Every Kotlin file carries a `// port-lint: source <path>` header naming its upstream Rust counterpart so the AST-distance tool can track provenance.

---

## Upstream README — `tokio-rs/bytes`

> The text below is reproduced and lightly edited from [`https://github.com/tokio-rs/bytes`](https://github.com/tokio-rs/bytes). It is the upstream project's own description and remains under the upstream authors' authorship; links have been rewritten to absolute upstream URLs so they continue to resolve from this repository.

## Bytes

A utility library for working with bytes.

[![Crates.io][crates-badge]][crates-url]
[![Build Status][ci-badge]][ci-url]

[crates-badge]: https://img.shields.io/crates/v/bytes.svg
[crates-url]: https://crates.io/crates/bytes
[ci-badge]: https://github.com/tokio-rs/bytes/workflows/CI/badge.svg
[ci-url]: https://github.com/tokio-rs/bytes/actions

[Documentation](https://docs.rs/bytes)

## Usage

To use `bytes`, first add this to your `Cargo.toml`:

```toml
[dependencies]
bytes = "1"
```

Next, add this to your crate:

```rust
use bytes::{Bytes, BytesMut, Buf, BufMut};
```

## no_std support

To use `bytes` with no_std environment, disable the (enabled by default) `std` feature.

```toml
[dependencies]
bytes = { version = "1", default-features = false }
```

To use `bytes` with no_std environment without atomic CAS, such as thumbv6m, you also need to enable
the `extra-platforms` feature. See the [documentation for the `portable-atomic`
crate](https://docs.rs/portable-atomic) for more information.

The MSRV when `extra-platforms` feature is enabled depends on the MSRV of `portable-atomic`.

## Serde support

Serde support is optional and disabled by default. To enable use the feature `serde`.

```toml
[dependencies]
bytes = { version = "1", features = ["serde"] }
```

The MSRV when `serde` feature is enabled depends on the MSRV of `serde`.

## Building documentation

When building the `bytes` documentation the `docsrs` option should be used, otherwise
feature gates will not be shown. This requires a nightly toolchain:

```
RUSTDOCFLAGS="--cfg docsrs" cargo +nightly doc
```

## License

This project is licensed under the [MIT license](https://github.com/tokio-rs/bytes/blob/HEAD/LICENSE).

### Contribution

Unless you explicitly state otherwise, any contribution intentionally submitted
for inclusion in `bytes` by you, shall be licensed as MIT, without any additional
terms or conditions.

---

## About this Kotlin port

### Installation

```kotlin
dependencies {
    implementation("io.github.kotlinmania:bytes-kotlin:0.2.1")
}
```

### Kotlin usage

The Rust crate exposes four central names — `Bytes`, `BytesMut`, `Buf`, and `BufMut`. The
Kotlin port keeps those names and the surrounding adapter types; the Rust *trait* `Buf` becomes
a Kotlin **interface** with the same set of read methods, and likewise for `BufMut`. The Rust
`&[u8]: Buf` and `&mut [u8]: BufMut` conformances become concrete classes [`ByteArrayBuf`] and
[`ByteArrayBufMut`] that wrap a `ByteArray` with a cursor.

```kotlin
import io.github.kotlinmania.bytes.Bytes
import io.github.kotlinmania.bytes.buf.ByteArrayBuf
import io.github.kotlinmania.bytes.buf.ByteArrayBufMut

// Immutable, cheaply-cloneable byte container — slicing, splitting, comparison.
val mem = Bytes.from("Hello world")
val a = mem.slice(0, 5)
check(a.eq("Hello"))

val b = mem.splitTo(6)
check(mem.eq("world"))
check(b.eq("Hello "))

// Cursored read view over a ByteArray.
val read = ByteArrayBuf("hello world".encodeToByteArray())
check(read.getU8() == 'h'.code.toUByte())
check(read.remaining() == 10)
val rest = ByteArray(10)
read.copyToSlice(rest)
check(rest.decodeToString() == "ello world")

// Cursored write view over a ByteArray.
val dst = ByteArray(11)
val write = ByteArrayBufMut(dst)
write.putSlice("hello ".encodeToByteArray())
write.putU8('w'.code.toUByte())
write.putSlice("orld".encodeToByteArray())
check(dst.decodeToString() == "hello world")
```

#### Reading typed numbers

`Buf` provides the same family of `getU8`, `getU16`, `getU32`, `getU64`, `getI*`, `getF32`,
`getF64`, `getUint(nbytes)`, `getInt(nbytes)` methods as upstream — both big-endian and
little-endian variants (`getU16Le`, etc.) and a native-endian alias (`getU16Ne`) that delegates
to the little-endian path on every platform supported by this port. There is no native 128-bit
primitive in Kotlin, so `getU128` / `getI128` return [`U128`] / [`I128`] wrapper data classes
that hold two `ULong` halves.

```kotlin
val buf = ByteArrayBuf(byteArrayOf(0x08, 0x09, 'h'.code.toByte(), 'i'.code.toByte()))
check(buf.getU16() == 0x0809u.toUShort())   // big-endian
```

The `tryGet*` family returns `kotlin.Result<T>` and never throws on under-read:

```kotlin
val short = ByteArrayBuf(byteArrayOf())
check(short.tryGetU8().isFailure)
```

#### Writing typed numbers

`BufMut` provides the matching `putU8`, `putU16(Le|Ne)`, `putU32`, `putU64`, `putI*`, `putF32`,
`putF64`, `putUint(value, nbytes)`, `putInt(value, nbytes)` methods, plus `putSlice(ByteArray)`
and `putBytes(value, count)`.

#### Adapters

The same adapter types from upstream are available in `io.github.kotlinmania.bytes.buf`:

| Type | What it does |
|---|---|
| `Chain`     | sequences two `Buf`s (or two `BufMut`s) into one continuous view |
| `Take`      | limits a `Buf` to read at most N more bytes |
| `Limit`     | limits a `BufMut` to write at most N more bytes |
| `Reader`    | exposes a `Buf` as a stream-of-bytes reader (`read`, `fillBuf`, `consume`) |
| `Writer`    | exposes a `BufMut` as a stream-of-bytes writer (`write`, `flush`) |
| `IntoIter`  | iterates the bytes of a `Buf` one at a time (Kotlin `Iterator<Byte>`) |
| `VecDequeBuf` | wraps a Kotlin `ArrayDeque<Byte>` so it can be read as a `Buf` |
| `UninitSlice` | the writable window returned by `BufMut.chunkMut()` |

```kotlin
val chain = ByteArrayBuf("hello ".encodeToByteArray())
    .chain(ByteArrayBuf("world".encodeToByteArray()))
val full = chain.copyToBytes(11)
check(full.asSlice().contentEquals("hello world".encodeToByteArray()))
```

#### Error handling

Underflow / over-advance panics in upstream Rust are translated to thrown
[`TryGetError`] exceptions in Kotlin. Use the `tryGet*` and `tryCopyToSlice`
variants to receive them as `Result.failure` instead of throwing.

#### Differences from upstream worth knowing

- **No `BytesMut` yet.** The mutable byte buffer (growable storage with `freeze()` to convert
  back to `Bytes`) is being ported in a follow-up release. For 0.2.1, mutable cursored writes
  go through `ByteArrayBufMut` against a pre-sized `ByteArray`.
- **No `serde` integration yet.** The upstream `serde` feature port is on the roadmap and will
  arrive once [`serde-kotlin`](https://github.com/KotlinMania/serde-kotlin) is published.
- **No raw-pointer zero-copy assertions.** Kotlin has no raw pointers, so behavior tests use
  content equality where upstream uses pointer equality. Reference-counted sharing is preserved
  internally via the `Bytes` cursor / shared-slice machinery.
- **Native-endian methods** (`*Ne`) delegate to the little-endian variant on every platform
  this port targets (macOS arm64, Linux x64, MinGW x64, iOS, Android, JS, Wasm-JS — all LE).
  If you need big-endian native delegation for a future big-endian platform, override the
  `NATIVE_IS_BIG_ENDIAN` constant in the port.

### Maintainer

Sydney Renee <sydney@solace.ofharmony.ai> (GitHub: [@sydneyrenee](https://github.com/sydneyrenee)) maintains this Kotlin port. Sydney Renee is the founder of The Solace Project.

### Building

```bash
./gradlew build
./gradlew test
```

### Targets

- macOS arm64
- Linux x64
- Windows mingw-x64
- iOS arm64 / simulator-arm64 (Swift export + XCFramework)
- JS (browser + Node.js)
- Wasm-JS (browser + Node.js)
- Android (API 24+)

### Porting guidelines

See [AGENTS.md](AGENTS.md) and [CLAUDE.md](CLAUDE.md) for translator discipline, port-lint header convention, and Rust → Kotlin idiom mapping.

### License

This Kotlin port is distributed under the same MIT license as the upstream [`tokio-rs/bytes`](https://github.com/tokio-rs/bytes). See [LICENSE](LICENSE) for the upstream license text and [NOTICE](NOTICE) for the Kotlin port notice.

Original work copyrighted by the bytes authors.  
Kotlin port: Copyright (c) 2026 Sydney Renee and The Solace Project.
Byline: Sydney Renee <sydney@solace.ofharmony.ai>, founder of The Solace Project.

### Acknowledgments

Thanks to the [`tokio-rs/bytes`](https://github.com/tokio-rs/bytes) maintainers and contributors for the original Rust implementation. This port reproduces their work in Kotlin Multiplatform; bug reports about upstream design or behavior should go to the upstream repository.
