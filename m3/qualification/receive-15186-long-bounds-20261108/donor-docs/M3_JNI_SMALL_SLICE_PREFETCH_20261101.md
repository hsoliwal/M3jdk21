<!-- SPDX-FileCopyrightText: 2026 Synexia contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 signed-long JNI small-slice preload: contract and experiment

## Scope and stable interfaces

- Source owner: `m3_native_sorted_long_bounds.c` only; keep `Java_jdk_m3_tooling_search_M3NativeSortedLongBounds_nativeBounds` unchanged.
- Java signatures, argument validation order, signed comparison, absolute `[from,to)` bound semantics, previous-key reuse, eight-element duplicate probe, 256-key JNI query chunks, and independent Java-only implementation are locked.
- Source arrays are caller-owned, read-only during the call; no JNI heap pointer or `JNIEnv` escapes. The result is always a fresh Java `int[]`; native dispatch remains explicit opt-in.
- Earlier four-slot, 256-element source cache stays the fallback for large slices and small query batches. Existing source-array and cache memory cannot be increased beyond four 256-element `jlong` windows.

## Source algorithm

For query batches of at least 128 elements and a nonempty source slice of at most 1024 `long`s, first inspect the first min(32, chunk length) already-copied query keys. Prefetch only when at least eight adjacent key transitions are present in that sample. This explicitly rejects clustered and long repeated-key prefixes, retaining the four-window demand cache for those inputs without a second JNI query-region copy. On admission, use one bounded `GetLongArrayRegion` call to copy the complete `[from,to)` slice into the pre-existing 8 KiB contiguous, stack-owned cache storage. If JNI raises an exception, return without exposing a partial result. Subsequent private `at()` lookups use exact `index - from` offsets directly into that native array. For any other input, continue four independently tagged, demand-filled 256-element windows. Neither path stores a Java-heap pointer or exposes the native data after the call.

A one-dimensional `jlong values[4*256]` is required so that the JNI region write never crosses the C pointer arithmetic boundary of an inner two-dimensional array. Cached-region tags and the page modulo-four mapping remain exactly as before for fallback.

## Problem category and donor decision

Categories: first/last occurrence, binary lower/upper bound, batched read-only primitive array, spatial locality (LeetCode 34/35/704, HackerRank binary search, GeeksforGeeks first/last); these are taxonomy references, not donor implementation sources. This optimization adapts the existing first-party Synexia JNI 4-window cache. No third-party source or proprietary JDK implementation is copied.

## Proof and selection gate

Execute the existing independent Java linear oracle against actual C17/JNI in interpreter, tier-1-limited, and tiered Java 21; include lengths 0, 1, 255, 256, 257, 1023, 1024 and 1025, invalid/empty source slices, source offsets, 127/128/129 query counts, signed extrema, duplicates, native key-region boundaries and parallel read-only invocations. Compare generated `javac -h` header hash and public `javap` signatures against preimage. Run strict Java/C17 compilation, Javadoc, recipe manifest regeneration and five drift/rights refusals, pre/post SHA-256, fixed-point preflight and real Maven/OpenRewrite SDK when available. Record separate host benchmark data for small, large, ordered, unordered, and clustered input; if the new path regresses workloads, reject promotion or revise the selector.

This documented candidate is NOT target-runtime admission. Source-built M3JDK, jtreg, product JNI/ABI, and cross-platform testing remain NOT_RUN until executed.

## Hot-loop specialization after paired proof

The initial `at()` implementation checked `whole_slice_cached` on every value
read; paired host results showed that branch regressed the fallback on several
workloads. The corrected recipe keeps the old `at()` demand-window kernel and
selects a separate, pure-C contiguous-slice bounds kernel at the **per query**
boundary only when the already-copied slice is resident. The latter performs
exact lower-bound and bounded-eight/equal-run upper-bound semantics with
overflow-safe integer midpoints. No extra heap/native storage is allocated.
A prefetch rejection therefore cannot introduce a new branch inside `at()`.
This refinement requires the expanded independent linear oracle, JNI-header
identity and paired five-workload timing before acceptance.
