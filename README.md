# bytes-kotlin in Kotlin

[![GitHub link](https://img.shields.io/badge/GitHub-KotlinMania%2Fbytes--kotlin-blue.svg)](https://github.com/KotlinMania/bytes-kotlin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kotlinmania/bytes-kotlin)](https://central.sonatype.com/artifact/io.github.kotlinmania/bytes-kotlin)
[![Build status](https://img.shields.io/github/actions/workflow/status/KotlinMania/bytes-kotlin/ci.yml?branch=main)](https://github.com/KotlinMania/bytes-kotlin/actions)

This is a Kotlin Multiplatform line-by-line transliteration port of [`tokio-rs/bytes`](https://github.com/tokio-rs/bytes).

**Original Project:** This port is based on [`tokio-rs/bytes`](https://github.com/tokio-rs/bytes). All design credit and project intent belong to the upstream authors; this repository is a faithful port to Kotlin Multiplatform with no behavioural changes intended.

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
    implementation("io.github.kotlinmania:bytes-kotlin:0.1.0-SNAPSHOT")
}
```

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

This Kotlin port is distributed under the same MIT license as the upstream [`tokio-rs/bytes`](https://github.com/tokio-rs/bytes). See [LICENSE](LICENSE) (and any sibling `LICENSE-*` / `NOTICE` files mirrored from upstream) for the full text.

Original work copyrighted by the bytes authors.  
Kotlin port: Copyright (c) 2026 Sydney Renee and The Solace Project.

### Acknowledgments

Thanks to the [`tokio-rs/bytes`](https://github.com/tokio-rs/bytes) maintainers and contributors for the original Rust implementation. This port reproduces their work in Kotlin Multiplatform; bug reports about upstream design or behavior should go to the upstream repository.
