# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 18/19 (94.7%)
- **Function parity:** 344/370 matched (target 428) — 93.0%
- **Class/type parity:** 21/28 matched (target 31) — 75.0%
- **Combined symbol parity:** 365/398 matched (target 459) — 91.7%
- **Average inline-code cosine:** 0.42 (function body across 16 matched files)
- **Average documentation cosine:** 0.58 (doc text across 16 matched files)
- **Cheat-zeroed Files:** 5
- **Critical Issues:** 12 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. buf.buf_mut

- **Target:** `buf.BufMut`
- **Similarity:** 0.35
- **Dependents:** 4
- **Priority Score:** 4015006.5
- **Functions:** 48/49 matched (target 57)
- **Missing functions:** `_assert_trait_object`
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_

### 2. bytes

- **Target:** `bytes.Bytes [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 2
- **Priority Score:** 2008110.0
- **Functions:** 74/74 matched (target 92)
- **Missing functions:** _none_
- **Types:** 7/7 matched (target 9)
- **Missing types:** _none_
- **Tests:** 1/1 matched
- **Lint issues:** 16

### 3. buf.uninit_slice

- **Target:** `buf.UninitSlice`
- **Similarity:** 0.49
- **Dependents:** 2
- **Priority Score:** 2001205.1
- **Functions:** 11/11 matched (target 14)
- **Missing functions:** _none_
- **Types:** 1/1 matched
- **Missing types:** _none_

### 4. bytes_mut

- **Target:** `bytes.BytesMut [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 1
- **Priority Score:** 1238310.0
- **Functions:** 59/78 matched (target 87)
- **Missing functions:** `from_vec`, `advance_unchecked`, `promote_to_shared`, `shallow_clone`, `get_vec_pos`, `set_vec_pos`, `eq`, `increment_shared`, `release_shared`, `is_unique`, `test_original_capacity_to_repr`, `test_original_capacity_from_repr`, `vptr`, `invalid_ptr`, `rebuild_vec`, `_split_to_must_use`, `_split_off_must_use`, `_split_must_use`, `bytes_mut_cloning_frozen`
- **Types:** 1/5 matched (target 1)
- **Missing types:** `Shared`, `Target`, `Item`, `IntoIter`
- **Tests:** 0/3 matched
- **Lint issues:** 4

### 5. buf.chain

- **Target:** `buf.Chain`
- **Similarity:** 0.60
- **Dependents:** 1
- **Priority Score:** 1041804.0
- **Functions:** 13/15 matched (target 17)
- **Missing functions:** `chunks_vectored`, `into_iter`
- **Types:** 1/3 matched (target 1)
- **Missing types:** `Item`, `IntoIter`

### 6. buf.take

- **Target:** `buf.Take`
- **Similarity:** 0.72
- **Dependents:** 1
- **Priority Score:** 1011202.9
- **Functions:** 10/11 matched (target 10)
- **Missing functions:** `chunks_vectored`
- **Types:** 1/1 matched
- **Missing types:** _none_

### 7. buf.limit

- **Target:** `buf.Limit`
- **Similarity:** 0.79
- **Dependents:** 1
- **Priority Score:** 1001002.1
- **Functions:** 9/9 matched
- **Missing functions:** _none_
- **Types:** 1/1 matched
- **Missing types:** _none_

### 8. buf.buf_impl

- **Target:** `buf.Buf`
- **Similarity:** 0.43
- **Dependents:** 0
- **Priority Score:** 29005.7
- **Functions:** 87/89 matched (target 100)
- **Missing functions:** `chunks_vectored`, `_assert_trait_object`
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_

### 9. buf.iter

- **Target:** `buf.Iter`
- **Similarity:** 0.94
- **Dependents:** 0
- **Priority Score:** 10800.6
- **Functions:** 6/6 matched (target 7)
- **Missing functions:** _none_
- **Types:** 1/2 matched (target 1)
- **Missing types:** `Item`

### 10. buf.vec_deque

- **Target:** `buf.VecDeque`
- **Similarity:** 0.29
- **Dependents:** 0
- **Priority Score:** 10407.1
- **Functions:** 3/4 matched (target 3)
- **Missing functions:** `chunks_vectored`
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_

### 11. lib

- **Target:** `bytes.Lib [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 1010.0
- **Functions:** 8/8 matched
- **Missing functions:** _none_
- **Types:** 2/2 matched
- **Missing types:** _none_

### 12. buf.reader

- **Target:** `buf.Reader`
- **Similarity:** 0.91
- **Dependents:** 0
- **Priority Score:** 800.9
- **Functions:** 7/7 matched
- **Missing functions:** _none_
- **Types:** 1/1 matched
- **Missing types:** _none_

### 13. buf.writer

- **Target:** `buf.Writer`
- **Similarity:** 0.81
- **Dependents:** 0
- **Priority Score:** 701.9
- **Functions:** 6/6 matched
- **Missing functions:** _none_
- **Types:** 1/1 matched
- **Missing types:** _none_

### 14. loom

- **Target:** `bytes.Loom`
- **Similarity:** 0.20
- **Dependents:** 0
- **Priority Score:** 208.0
- **Functions:** 1/1 matched (target 6)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 6)
- **Missing types:** _none_

### 15. fmt.mod

- **Target:** `fmt.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 110.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 1/1 matched
- **Missing types:** _none_

### 16. fmt.hex

- **Target:** `fmt.Hex`
- **Similarity:** 0.10
- **Dependents:** 0
- **Priority Score:** 109.0
- **Functions:** 1/1 matched (target 3)
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

### 17. fmt.debug

- **Target:** `fmt.Debug`
- **Similarity:** 0.13
- **Dependents:** 0
- **Priority Score:** 108.7
- **Functions:** 1/1 matched (target 2)
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

### 18. buf.mod

- **Target:** `buf.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

## Next Commands

```bash
# Initialize task queue for systematic porting
cd tools/ast_distance
./ast_distance --init-tasks ../../tmp/bytes/src rust ../../src/commonMain/kotlin/io/github/kotlinmania/bytes kotlin tasks.json ../../AGENTS.md

# Get next high-priority task
./ast_distance --assign tasks.json <agent-id>
```
