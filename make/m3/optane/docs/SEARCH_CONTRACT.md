<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/36827ee8a798ef6ce07f6fc55a2ac371b1382c06/m3-pmem-sdk/docs/SEARCH_CONTRACT.md blob ae47ff5bf921; recipe make/m3/optane/recipe/pmem-region-core-20261106/relocate.py v2 -->
# M3 mapped-byte search: v0.4 contract

## Public API and ownership

`PmemSearch.indexOf(PmemRegion, long start, long count, byte[] needle)` returns
an offset **relative to the mapped region**, not a file offset. The result is
the first exact byte match in `[start, start + count)`, or `-1`. Empty patterns
match at `start`, including when `start == region.length()` and `count == 0`.

This is an additive API. No `PmemRegion` method or pre-existing JNI export is
removed or changed. A full read lease is acquired before JNI searches memory
and is released after return; `close()` must wait for the search. The native
address is never returned to Java, and the pattern bytes are copied into
bounded native scratch memory before scanning. External writers and truncation
are outside this intra-process lease protocol; model mappings must remain
immutable for the duration of inference or search.

## Kernel selection and complexity

- Empty pattern: returns `start` without reading mapped bytes.
- One byte: native libc `memchr`, first occurrence only.
- Two or more bytes: KMP with native prefix table, linear worst-case time
  `O(count + needle.length)` and `O(needle.length)` extra space.
- Needle sizes over 1 MiB are rejected. No model-sized staging buffers or
  per-element wrapper objects are allocated.
- Offset/bounds checks use subtraction on Java and native sides; offsets beyond
  2 GiB are supported by 64-bit mapping coordinates.

## Independent test gates

`PmemSearchSmokeTest` compares against a deliberately simple, independent
naive byte-array reference, includes 250 seeded trials, adversarial prefix
repetition, 4 KiB page boundaries, binary bytes, sparse files above 2 GiB,
close concurrency, invalid windows and null or over-limit patterns.

`PmemAtomCases` adds `indexOf` as the 17th public method atom with an independent
correctness case and a checksummed, non-threshold performance workload.
`PmemAtomJUnitRecipe` emits a JUnit 5 correctness test and an opt-in perf test
for this atom; the Maven `verify` goal provisions the JUnit launcher and runs
all tests on CI. The host-local JMOD and UBSan checks are separate.

## Donor decisions (no donor code imported)

| Reference | Category | Selection |
| --- | --- | --- |
| LeetCode 28 | first-occurrence search | First-match / `-1` result semantics, adapted to binary memory and `long` offsets |
| HackerRank Find a String | overlapping occurrences | Problem taxonomy only; counts are not an acceptable API substitute |
| GeeksforGeeks KMP | prefix-function algorithm | Selected linear worst-case algorithm and independently authored C implementation |
| libc `memchr` | native single-byte search | Use the platform's optimized implementation |

This implementation does not claim a trained-LLM speedup, PMDK persistence
proof, FSDAX/DEV DAX verification, or physical Optane/dual-Xeon performance.

## Extension: sorted signed 64-bit key lookup

The `PmemSearch.lowerBoundLongLE`/`upperBoundLongLE` methods are **separate**
from KMP byte searching. The input is a pre-sorted array of `long` values
stored as little-endian bytes; the methods return an element insertion point.
See [SORTED_LONG_SEARCH.md](SORTED_LONG_SEARCH.md).
