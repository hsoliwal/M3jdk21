<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Exact packed queries, block scans, memo storage and stable lazy ordering

## Scope and provenance

This delivery extends existing M3 Java and JNI owners. It applies compact exact
representations, computation over compressed storage, conservative block bounds
with exact refinement, array-backed memo state and stable lazy selection.
All returned values remain exact. No lossy numeric mode, new collection
framework, native loader, or change to ordinary JDK public class names is added.

The receiving baseline is M3jdk21 commit
`0017450623ff8d1f31b328b1949fe2ee0c17dadc`. Canonical authoring is in
com.synexia, based on `fc48d05e149d66dbfa46c65229780ef1cab7743c`, under task
`m3-optimization-transfer-20261108`. The associated pull request links the exact
canonical source commit and source-custody evidence. This repository receives
product sources, tests and this passive receipt. Recipe implementation and its
execution machinery remain in their canonical repository.

## Public and internal behavior

| Existing owner | Change | Contract |
| --- | --- | --- |
| `jdk.m3.arrays.packed.PackedIntArray` | `countInRange(from,to,lower,upper)` and `firstInRange(from,to,lower,upper)` | Signed 40/48/56-bit values; inclusive numeric interval and half-open element window. First match is an absolute element index or -1. No decoded array. |
| `jdk.m3.arrays.M3ArrayNative` | `countPackedInRangeRequired(bytes,bits,from,to,lower,upper)` and `firstPackedInRangeRequired(...)` | Same exact query over little-endian packed bytes. Required JNI; aligned ingress chunks of at most 4096 bytes; 8192 bytes of fixed byte scratch; no retained array pointer or decoded result array. |
| `jdk.internal.m3.collections.M3BlockPackedLongArray` | Block-wise `copyTo`, `toArray`, `countEqual` and `countInRange` | Constant, packed-delta and plain blocks retain their existing layout. Bounds may accept/reject a block only when sound; other cases compare exact values. |
| `jdk.internal.m3.collections.M3MatMemoTable` | Primitive entry/index pages and exact argument pool/id lanes | Existing API, synchronized/reentrant computation, tuple identity, limits and statistics preserved. No retained key object, boxed result or handle array per entry. |
| `jdk.m3.collections.M3LazyTopKWindow` | Source-row tie order preserved in descending windows | Direction applies to ordering keys; final equal-key ties retain original row order in both directions. |

### Packed query example

```java
Int40Array values = new Int40Array(4);
values.setAll(0, new long[] {-7, 0, 12, 12}, 0, 4);
int matches = values.countInRange(0, 4, 10, 20); // 2
int first = values.firstInRange(1, 4, 10, 20);  // 2
```

For already packed caller-owned bytes, the required-native methods can process
payloads larger than the original codec materialization batch. The original six
packed/full64 encode/decode operations keep their 4096 bound and wire format.
The new query methods do not implicitly select native execution for portable
`PackedIntArray` callers.

Java/native query validation checks null, supported width, complete payload
geometry, element window, then numeric interval. Width is exactly 40, 48 or 56.
Numeric bounds can span the entire signed-long domain. Reversed bounds fail,
including for empty windows. Valid empty windows still require the matching
native library. Native ingress repeats validation independently. The caller
must not mutate bytes during the call; these APIs do not promise an atomic
snapshot of concurrently modified arrays.

## Donor and category treatment

The canonical review records category lookups in LeetCode, HackerRank and
GeeksforGeeks, plus commit/file/license pins for Apache Lucene, JavaFastPFOR,
RoaringBitmap and Apache Arrow. They informed algorithm and layout evaluation.
No challenge editorial, submitted solution or third-party implementation is
copied into this delivery. Existing source notices remain intact; added code
and tests are first-party work.

## Qualification and limits

The receiver's focused Java suites contain 39 JUnit tests: 13 packed/codec,
14 block/lazy, and 12 memo tests. Canonical counterparts add 41 tests. Tests use
independent source-value and tuple/list oracles, literal signed wire vectors,
seeded randomized ranges, all block bit widths, collisions, reentrant cache
callbacks, concurrency, invalid geometry and stable IEEE ordering.

Native qualification uses C17 with warnings as errors, generated JNI header
parity, actual checked-JNI execution, and separate documented JNI fault models.
The existing codec kernel's 205602 assertions and 172 modeled fault scenarios
remain covered. New query kernels cover 568 assertions per namespace; new
query JNI models cover 401 receiver and 392 canonical assertions. ASan and
UBSan runs pass. LeakSanitizer is unavailable under the qualification workspace's
ptrace environment; the new scan kernels do not allocate native heap storage.

The retained descending-tie counterexample is two equal keys: the old receiver
returned row IDs `[1,0]`; the corrected result is `[0,1]`. The old canonical test
oracle mirrored the implementation defect and was replaced with independent
stable-key sorting and explicit tie assertions.

`PackedQueryResourceProbe` records exact counts, Java thread allocations and
local elapsed times for expansion, direct Java scanning and bounded JNI
scanning. On the recorded 262144-element int40 workload, expansion allocated
2097168 Java bytes per call; direct Java and JNI count each measured zero.
Inputs were prepared before measurement. Timing is a local observation, not a
portable throughput guarantee or an end-to-end application benchmark.

This is component qualification on Java 21 with compiled native libraries. A
full Synexia reactor build, installed modified JDK image, complete cross-platform
native matrix and repository-wide release/performance admission remain separate
gates. This change does not promote the repository's family-receiving phase or
claim that every project under the account has been optimized.
