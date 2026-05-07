// port-lint: source src/buf/mod.rs
package io.github.kotlinmania.bytes.buf

/**
 * Utilities for working with buffers.
 *
 * A buffer is any structure that contains a sequence of bytes. The bytes may
 * or may not be stored in contiguous memory. This module contains traits used
 * to abstract over buffers as well as utilities for working with buffer types.
 *
 * # `Buf`, `BufMut`
 *
 * These are the two foundational traits for abstractly working with buffers.
 * They can be thought as iterators for byte structures. They offer additional
 * performance over `Iterator` by providing an API optimized for byte slices.
 *
 * See `Buf` and `BufMut` for more details.
 *
 * Rope reference: https://en.wikipedia.org/wiki/Rope
 */

// The upstream module declares the buffer implementation, mutable-buffer implementation, chained
// buffers, iterators, limits, readers, takes, uninitialized slices, deque support, and writers.
// It also exposes the public buffer traits and adapter types from those owning modules. Per the
// workspace re-export workflow, this tracking file does not introduce central aliases for those
// names. Kotlin callers should import the original ported symbols directly from their owning
// files and use Kotlin import aliasing when a faithful translation needs to preserve an upstream
// re-exported identifier.

// Upstream re-export lines:
// - `pub use self::buf_impl::Buf;` exports `self::buf_impl::Buf` as `Buf`.
// - `pub use self::buf_mut::BufMut;` exports `self::buf_mut::BufMut` as `BufMut`.
// - `pub use self::chain::Chain;` exports `self::chain::Chain` as `Chain`.
// - `pub use self::iter::IntoIter;` exports `self::iter::IntoIter` as `IntoIter`.
// - `pub use self::limit::Limit;` exports `self::limit::Limit` as `Limit`.
// - `pub use self::take::Take;` exports `self::take::Take` as `Take`.
// - `pub use self::uninit_slice::UninitSlice;` exports `self::uninit_slice::UninitSlice` as
//   `UninitSlice`.
// - `pub use self::{reader::Reader, writer::Writer};` exports `self::reader::Reader` as `Reader`
//   and `self::writer::Writer` as `Writer`.

// Callers migrated:
// - none
//
// Kotlin caller scan:
// - no direct imports, wildcard imports, or fully-qualified references to this package's
//   re-exported names were found in bytes-dependent sibling ports.
// - the relevant Rust caller repos named by `RUST_CALLERS.md` currently have no non-temporary
//   Kotlin source files corresponding to those Rust caller files, so there are no concrete
//   callers to rewrite yet.
//
// Projected callers (Rust):
// - `prost-kotlin`, `rama-core-kotlin`, `reqwest-kotlin`, `rmcp-kotlin`, `sse-stream-kotlin`,
//   `tokio-kotlin`, `tokio-util-kotlin`, `tonic-kotlin`, `tonic-prost-kotlin`, and
//   `tungstenite-kotlin` import the root `Buf` re-export.
// - `aws-sigv4-kotlin`, `axum-kotlin`, `http-kotlin`, `libwebrtc-kotlin`, `prost-kotlin`,
//   `rama-core-kotlin`, `reqwest-kotlin`, `tokio-kotlin`, `tokio-util-kotlin`, `tonic-kotlin`,
//   and `tonic-prost-kotlin` import the root `BufMut` re-export.
// - `tonic-kotlin` imports the `bytes::buf::UninitSlice` re-export.
//
// (From `/Volumes/stuff/Projects/kotlinmania/bytes-kotlin/RUST_CALLERS.md`.)
