// Compile and link against the unchanged pinned bytes crate:
// mkdir -p build/rust-kotlin-buf-dispatch
// rustc --crate-name bytes --crate-type rlib --edition 2021 --cfg 'feature="std"' tmp/bytes/src/lib.rs -o build/rust-kotlin-buf-dispatch/libbytes_adapter_oracle.rlib
// rustc --test --edition 2021 --extern bytes=build/rust-kotlin-buf-dispatch/libbytes_adapter_oracle.rlib verification/bytes-adapter-oracle.rs -o build/rust-kotlin-buf-dispatch/adapter-oracle
// build/rust-kotlin-buf-dispatch/adapter-oracle

use bytes::buf::UninitSlice;
use bytes::{Buf, BufMut};
use std::io::{Read, Write};
use std::panic::{catch_unwind, AssertUnwindSafe};

struct ReadBuf {
    bytes: [u8; 3],
    limit: usize,
    position: usize,
    copies: Vec<usize>,
    panic_copy: bool,
}

impl ReadBuf {
    fn new(limit: usize) -> Self {
        Self {
            bytes: [1, 2, 3],
            limit,
            position: 0,
            copies: Vec::new(),
            panic_copy: false,
        }
    }
}

impl Buf for ReadBuf {
    fn remaining(&self) -> usize {
        self.limit - self.position
    }
    fn chunk(&self) -> &[u8] {
        &self.bytes[self.position..self.limit]
    }
    fn advance(&mut self, count: usize) {
        assert!(count <= self.remaining());
        self.position += count;
    }
    fn copy_to_slice(&mut self, dst: &mut [u8]) {
        self.copies.push(dst.len());
        if self.panic_copy {
            panic!("copy marker");
        }
        assert!(dst.len() <= self.remaining());
        dst.copy_from_slice(&self.bytes[self.position..self.position + dst.len()]);
        self.position += dst.len();
    }
}

struct WriteBuf {
    bytes: [u8; 4],
    limit: usize,
    position: usize,
    puts: Vec<usize>,
    panic_put: bool,
}

impl WriteBuf {
    fn new(limit: usize) -> Self {
        Self {
            bytes: [0xa5; 4],
            limit,
            position: 0,
            puts: Vec::new(),
            panic_put: false,
        }
    }
}

unsafe impl BufMut for WriteBuf {
    fn remaining_mut(&self) -> usize {
        self.limit - self.position
    }
    fn chunk_mut(&mut self) -> &mut UninitSlice {
        UninitSlice::new(&mut self.bytes[self.position..self.limit])
    }
    unsafe fn advance_mut(&mut self, count: usize) {
        assert!(count <= self.remaining_mut());
        self.position += count;
    }
    fn put_slice(&mut self, src: &[u8]) {
        self.puts.push(src.len());
        if self.panic_put {
            panic!("put marker");
        }
        assert!(src.len() <= self.remaining_mut());
        self.bytes[self.position..self.position + src.len()].copy_from_slice(src);
        self.position += src.len();
    }
}

#[test]
fn empty_reader_destination_still_calls_copy() {
    let mut reader = ReadBuf::new(3).reader();
    assert_eq!(reader.read(&mut []).unwrap(), 0);
    assert_eq!(reader.get_ref().copies, [0]);
    assert_eq!(reader.get_ref().position, 0);
}

#[test]
fn reader_at_eof_still_calls_copy_without_changing_destination() {
    let mut reader = ReadBuf::new(0).reader();
    let mut dst = [0xa5; 4];
    assert_eq!(reader.read(&mut dst).unwrap(), 0);
    assert_eq!(reader.get_ref().copies, [0]);
    assert_eq!(dst, [0xa5; 4]);
}

#[test]
fn empty_reader_copy_panics_propagate() {
    let mut buf = ReadBuf::new(0);
    buf.panic_copy = true;
    let mut reader = buf.reader();
    assert!(catch_unwind(AssertUnwindSafe(|| reader.read(&mut [0; 1]))).is_err());
    assert_eq!(reader.get_ref().copies, [0]);
}

#[test]
fn zero_capacity_writer_still_calls_put() {
    let mut writer = WriteBuf::new(0).writer();
    assert_eq!(writer.write(&[1, 2]).unwrap(), 0);
    assert_eq!(writer.get_ref().puts, [0]);
    assert_eq!(writer.get_ref().position, 0);
}

#[test]
fn empty_writer_input_still_calls_put() {
    let mut writer = WriteBuf::new(3).writer();
    assert_eq!(writer.write(&[]).unwrap(), 0);
    assert_eq!(writer.get_ref().puts, [0]);
    assert_eq!(writer.get_ref().position, 0);
}

#[test]
fn empty_writer_put_panics_propagate() {
    let mut buf = WriteBuf::new(0);
    buf.panic_put = true;
    let mut writer = buf.writer();
    assert!(catch_unwind(AssertUnwindSafe(|| writer.write(&[1]))).is_err());
    assert_eq!(writer.get_ref().puts, [0]);
}

#[test]
fn partial_reader_writes_only_the_available_prefix() {
    let mut reader = ReadBuf::new(3).reader();
    let mut dst = [0xa5; 5];
    assert_eq!(reader.read(&mut dst).unwrap(), 3);
    assert_eq!(reader.get_ref().copies, [3]);
    assert_eq!(reader.get_ref().position, 3);
    assert_eq!(dst, [1, 2, 3, 0xa5, 0xa5]);
}

#[test]
fn partial_writer_uses_only_the_available_capacity() {
    let mut writer = WriteBuf::new(3).writer();
    assert_eq!(writer.write(&[1, 2, 3, 4, 5]).unwrap(), 3);
    assert_eq!(writer.get_ref().puts, [3]);
    assert_eq!(writer.get_ref().position, 3);
    assert_eq!(writer.get_ref().bytes, [1, 2, 3, 0xa5]);
}
