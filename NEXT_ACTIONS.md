# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 16/19 (84.2%)
- **Function parity:** 354/445 matched (target 433) — 79.6%
- **Class/type parity:** 15/25 matched (target 26) — 60.0%
- **Combined symbol parity:** 369/470 matched (target 459) — 78.5%
- **Average inline-code cosine:** 0.12 (function body across 16 matched files)
- **Average documentation cosine:** 0.52 (doc text across 16 matched files)
- **Cheat-zeroed Files:** 1
- **Critical Issues:** 16 files with <0.60 function similarity
- **Needs Review:** 0 files with 0.60-0.84 function similarity
- **Excellent:** 0 files with >=0.85 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. buf.buf_mut

- **Target:** `buf.BufMut`
- **Similarity:** 0.14
- **Dependents:** 4
- **Priority Score:** 4126308.8
- **Functions:** 50/62 matched (target 56)
- **Missing functions:** `remaining_mut`, `chunk_mut`, `advance_mut`, `put_slice`, `put_bytes`, `remaining_mut`, `advance_mut`, `chunk_mut`, `put`, `put_slice`, `put_bytes`, `_assert_trait_object`
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_

### 2. bytes

- **Target:** `Bytes`
- **Similarity:** 0.10
- **Dependents:** 2
- **Priority Score:** 2331409.0
- **Functions:** 78/107 matched (target 93)
- **Missing functions:** `new`, `into_iter`, `eq`, `partial_cmp`, `eq`, `partial_cmp`, `eq`, `partial_cmp`, `eq`, `partial_cmp`, `eq`, `partial_cmp`, `eq`, `partial_cmp`, `eq`, `partial_cmp`, `eq`, `partial_cmp`, `eq`, `partial_cmp`, `from`, `from`, `from`, `from`, `drop`, `ptr_map`, `_split_to_must_use`, `_split_off_must_use`, `bytes_cloning_vec`
- **Types:** 4/7 matched (target 5)
- **Missing types:** `Target`, `Item`, `IntoIter`
- **Tests:** 0/1 matched
- **Lint issues:** 10

### 3. buf.uninit_slice

- **Target:** `buf.UninitSlice`
- **Similarity:** 0.09
- **Dependents:** 2
- **Priority Score:** 2011309.1
- **Functions:** 11/12 matched (target 14)
- **Missing functions:** `from`
- **Types:** 1/1 matched
- **Missing types:** _none_

### 4. bytes_mut

- **Target:** `BytesMut`
- **Similarity:** 0.08
- **Dependents:** 1
- **Priority Score:** 1441409.2
- **Functions:** 70/109 matched (target 84)
- **Missing functions:** `eq`, `into_iter`, `extend`, `extend`, `from_iter`, `is_unique`, `test_original_capacity_to_repr`, `test_original_capacity_from_repr`, `eq`, `partial_cmp`, `eq`, `partial_cmp`, `eq`, `partial_cmp`, `eq`, `partial_cmp`, `eq`, `partial_cmp`, `eq`, `partial_cmp`, `eq`, `partial_cmp`, `eq`, `partial_cmp`, `eq`, `partial_cmp`, `eq`, `partial_cmp`, `eq`, `partial_cmp`, `eq`, `eq`, `from`, `vptr`, `invalid_ptr`, `_split_to_must_use`, `_split_off_must_use`, `_split_must_use`, `bytes_mut_cloning_frozen`
- **Types:** 1/5 matched (target 1)
- **Missing types:** `Shared`, `Target`, `Item`, `IntoIter`
- **Tests:** 0/3 matched
- **Lint issues:** 2

### 5. buf.chain

- **Target:** `buf.Chain`
- **Similarity:** 0.17
- **Dependents:** 1
- **Priority Score:** 1041808.4
- **Functions:** 13/15 matched (target 17)
- **Missing functions:** `chunks_vectored`, `into_iter`
- **Types:** 1/3 matched (target 1)
- **Missing types:** `Item`, `IntoIter`

### 6. buf.take

- **Target:** `buf.Take`
- **Similarity:** 0.18
- **Dependents:** 1
- **Priority Score:** 1011208.2
- **Functions:** 10/11 matched (target 10)
- **Missing functions:** `chunks_vectored`
- **Types:** 1/1 matched
- **Missing types:** _none_

### 7. buf.limit

- **Target:** `buf.Limit`
- **Similarity:** 0.20
- **Dependents:** 1
- **Priority Score:** 1001008.1
- **Functions:** 9/9 matched
- **Missing functions:** _none_
- **Types:** 1/1 matched
- **Missing types:** _none_

### 8. buf.buf_impl

- **Target:** `buf.Buf`
- **Similarity:** 0.09
- **Dependents:** 0
- **Priority Score:** 59409.1
- **Functions:** 88/93 matched (target 100)
- **Missing functions:** `chunks_vectored`, `remaining`, `chunk`, `advance`, `_assert_trait_object`
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_

### 9. buf.iter

- **Target:** `buf.Iter`
- **Similarity:** 0.30
- **Dependents:** 0
- **Priority Score:** 10807.0
- **Functions:** 6/6 matched (target 7)
- **Missing functions:** _none_
- **Types:** 1/2 matched (target 1)
- **Missing types:** `Item`

### 10. buf.vec_deque

- **Target:** `buf.VecDeque`
- **Similarity:** 0.02
- **Dependents:** 0
- **Priority Score:** 10409.8
- **Functions:** 3/4 matched (target 3)
- **Missing functions:** `chunks_vectored`
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_

### 11. fmt.hex

- **Target:** `fmt.Hex`
- **Similarity:** 0.01
- **Dependents:** 0
- **Priority Score:** 10209.9
- **Functions:** 1/2 matched (target 3)
- **Missing functions:** `fmt`
- **Types:** 0/0 matched
- **Missing types:** _none_

### 12. buf.reader

- **Target:** `buf.Reader`
- **Similarity:** 0.23
- **Dependents:** 0
- **Priority Score:** 807.7
- **Functions:** 7/7 matched
- **Missing functions:** _none_
- **Types:** 1/1 matched
- **Missing types:** _none_

### 13. buf.writer

- **Target:** `buf.Writer`
- **Similarity:** 0.17
- **Dependents:** 0
- **Priority Score:** 708.3
- **Functions:** 6/6 matched
- **Missing functions:** _none_
- **Types:** 1/1 matched
- **Missing types:** _none_

### 14. loom

- **Target:** `Loom`
- **Similarity:** 0.15
- **Dependents:** 0
- **Priority Score:** 208.5
- **Functions:** 1/1 matched (target 6)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 6)
- **Missing types:** _none_

### 15. fmt.debug

- **Target:** `fmt.Debug`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 110.0
- **Functions:** 1/1 matched (target 2)
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

### 16. serde

- **Target:** `Serde [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched (target 16)
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 2)
- **Missing types:** _none_

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Missing

| Source | Expected target | Deps | Source path | Expected path |
|--------|-----------------|------|-------------|---------------|
| `buf.mod` | `buf.Mod` | 0 | `buf/mod.rs` | `buf/Mod.kt` |
| `fmt.mod` | `fmt.Mod` | 0 | `fmt/mod.rs` | `fmt/Mod.kt` |
| `lib` | `Lib` | 0 | `lib.rs` | `Lib.kt` |

