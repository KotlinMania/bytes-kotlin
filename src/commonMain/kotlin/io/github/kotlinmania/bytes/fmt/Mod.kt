// port-lint: source src/fmt/mod.rs
package io.github.kotlinmania.bytes.fmt

// The upstream macro implements each formatting trait for both immutable and mutable byte
// containers by delegating to `BytesRef`. Kotlin represents that delegation as helper functions
// in the debug and hexadecimal formatting files.

// The upstream module declares debug and hexadecimal formatting submodules.

// Upstream re-export lines:
// - none
//
// Callers migrated:
// - none

/**
 * `BytesRef` is not a part of public API of bytes crate.
 */
internal class BytesRef(
    internal val bytes: ByteArray,
)
