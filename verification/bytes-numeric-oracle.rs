// Compile the unchanged pinned bytes crate before running this fixture:
// mkdir -p build/rust-kotlin-buf-dispatch
// rustc --crate-name bytes --crate-type rlib --edition 2021 --cfg 'feature="std"' tmp/bytes/src/lib.rs -o build/rust-kotlin-buf-dispatch/libbytes_upstream.rlib
// rustc --test --edition 2021 --extern bytes=build/rust-kotlin-buf-dispatch/libbytes_upstream.rlib verification/bytes-numeric-oracle.rs -o build/rust-kotlin-buf-dispatch/numeric-oracle
// build/rust-kotlin-buf-dispatch/numeric-oracle

use bytes::{Buf, TryGetError};
use std::cell::Cell;
use std::panic::{catch_unwind, AssertUnwindSafe};

struct RecordingBuf {
    bytes: Vec<u8>,
    position: usize,
    chunk_limit: usize,
    chunk_calls: Cell<usize>,
    copy_calls: usize,
    try_copy_calls: usize,
    failure: Option<TryGetError>,
}

impl RecordingBuf {
    fn new(len: usize, chunk_limit: usize) -> Self {
        Self {
            bytes: (0..len).map(|i| (i + 1) as u8).collect(),
            position: 0,
            chunk_limit,
            chunk_calls: Cell::new(0),
            copy_calls: 0,
            try_copy_calls: 0,
            failure: None,
        }
    }
}

impl Buf for RecordingBuf {
    fn remaining(&self) -> usize {
        self.bytes.len() - self.position
    }

    fn chunk(&self) -> &[u8] {
        self.chunk_calls.set(self.chunk_calls.get() + 1);
        let end = self.bytes.len().min(self.position + self.chunk_limit);
        &self.bytes[self.position..end]
    }

    fn advance(&mut self, count: usize) {
        assert!(count <= self.remaining());
        self.position += count;
    }

    fn try_get_uint(&mut self, _width: usize) -> Result<u64, TryGetError> {
        panic!("unsigned fallible getter override marker");
    }

    fn try_get_uint_le(&mut self, _width: usize) -> Result<u64, TryGetError> {
        panic!("unsigned fallible getter override marker");
    }

    fn copy_to_slice(&mut self, _dst: &mut [u8]) {
        self.copy_calls += 1;
        panic!("copy_to_slice marker");
    }

    fn try_copy_to_slice(&mut self, dst: &mut [u8]) -> Result<(), TryGetError> {
        self.try_copy_calls += 1;
        if let Some(error) = self.failure.take() {
            return Err(error);
        }
        dst.copy_from_slice(&self.bytes[self.position..self.position + dst.len()]);
        self.position += dst.len();
        Ok(())
    }
}

#[test]
fn infallible_variable_reads_use_try_copy_for_contiguous_and_fragmented_storage() {
    let readers: [fn(&mut RecordingBuf) -> u64; 6] = [
        |buf| buf.get_uint(3),
        |buf| buf.get_uint_le(3),
        |buf| buf.get_uint_ne(3),
        |buf| buf.get_int(3) as u64,
        |buf| buf.get_int_le(3) as u64,
        |buf| buf.get_int_ne(3) as u64,
    ];
    let native = if cfg!(target_endian = "big") {
        0x010203
    } else {
        0x030201
    };
    let expected = [0x010203, 0x030201, native, 0x010203, 0x030201, native];
    for (index, read) in readers.into_iter().enumerate() {
        for chunk_limit in [1, 16] {
            let mut buf = RecordingBuf::new(16, chunk_limit);
            assert_eq!(read(&mut buf), expected[index]);
            assert_eq!(buf.try_copy_calls, 1);
            assert_eq!(buf.copy_calls, 0);
            assert_eq!(buf.chunk_calls.get(), 0);
            assert_eq!(buf.position, 3);
        }
    }
}

#[test]
fn infallible_variable_read_errors_panic_with_the_returned_error_fields() {
    let mut buf = RecordingBuf::new(16, 16);
    buf.failure = Some(TryGetError {
        requested: 3,
        available: 2,
    });
    let panic = catch_unwind(AssertUnwindSafe(|| buf.get_uint(3))).unwrap_err();
    let message = panic
        .downcast_ref::<String>()
        .map(String::as_str)
        .or_else(|| panic.downcast_ref::<&str>().copied())
        .unwrap();
    assert_eq!(
        message,
        "advance out of bounds: the len is 2 but advancing by 3"
    );
    assert_eq!(buf.try_copy_calls, 1);
    assert_eq!(buf.copy_calls, 0);
    assert_eq!(buf.chunk_calls.get(), 0);
    assert_eq!(buf.position, 0);
}

#[test]
fn fallible_signed_reads_zero_pad_reduced_width_values() {
    for width in 0..=8 {
        let data = [0xff; 8];
        let expected = match width {
            0 => 0,
            8 => -1,
            _ => ((1u64 << (width * 8)) - 1) as i64,
        };
        assert_eq!(data.as_slice().try_get_int(width).unwrap(), expected);
        assert_eq!(data.as_slice().try_get_int_le(width).unwrap(), expected);
        assert_eq!(data.as_slice().try_get_int_ne(width).unwrap(), expected);
        if width != 0 {
            assert_eq!(data.as_slice().get_int(width), -1);
            assert_eq!(data.as_slice().get_int_le(width), -1);
            assert_eq!(data.as_slice().get_int_ne(width), -1);
        }
    }
}

#[test]
fn zero_width_unsigned_reads_still_call_try_copy() {
    let mut buf = RecordingBuf::new(0, 0);
    assert_eq!(buf.get_uint(0), 0);
    assert_eq!(buf.try_copy_calls, 1);
    assert_eq!(buf.chunk_calls.get(), 0);
    assert_eq!(buf.position, 0);
}

#[test]
fn oversized_unsigned_reads_panic_before_copying() {
    let mut buf = RecordingBuf::new(16, 16);
    assert!(catch_unwind(AssertUnwindSafe(|| buf.get_uint(9))).is_err());
    assert_eq!(buf.try_copy_calls, 0);
    assert_eq!(buf.copy_calls, 0);
    assert_eq!(buf.chunk_calls.get(), 0);
}

#[test]
fn fallible_signed_reads_do_not_dispatch_to_unsigned_getter_overrides() {
    let readers: [fn(&mut RecordingBuf) -> Result<i64, TryGetError>; 3] = [
        |buf| buf.try_get_int(3),
        |buf| buf.try_get_int_le(3),
        |buf| buf.try_get_int_ne(3),
    ];
    for read in readers {
        let mut buf = RecordingBuf::new(16, 16);
        assert!(read(&mut buf).is_ok());
        assert_eq!(buf.try_copy_calls, 1);
        assert_eq!(buf.chunk_calls.get(), 0);
    }
}
