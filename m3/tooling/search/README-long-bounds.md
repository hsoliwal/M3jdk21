<!-- SPDX-FileCopyrightText: 2026 Synexia contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 sorted signed-long bounds (opt-in tooling)

`jdk.m3.tooling.search.M3SortedLongBounds` performs absolute lower/upper bound lookups on a nondecreasing signed `long[]` slice. It returns an interleaved fresh `int[]` of `[first >= key, first > key]` positions for each query. The source array and query array are never changed, and Java-only use needs no native library. A Java batch of 128+ sorted query values uses a forward-only merge; other batches use binary search. It is not a replacement of `java.util.Arrays.binarySearch`, nor a mutation to standard JDK 21 behavior.

`M3NativeSortedLongBounds` loads a native library only on explicit invocation using the `m3.native.sorted.long.search.library` property. Its C17 JNI implementation accesses both `long[]` inputs through fixed 256-element regions, writes at most 256 result pairs at once, stores no Java heap pointer, and exports one generated-header-pinned JNI symbol. The native route is for behavior qualification, not automatic speed selection. Supply the property as a full path and run `verify-long-bounds.sh` for host JDK21 differential evidence.

Contract: arrays non-null; source slice `[from,to)` valid; selected source slice sorted in signed nondecreasing order and not concurrently modified; output `int[]` length is twice `keys.length`. Null checks precede range checks; malformed sizes refuse before work. `Long.MIN_VALUE`/`Long.MAX_VALUE`, empty slices, duplicate keys, and concurrent read-only invocations are accepted.

The native optimization remains on HOLD until the actual source-built M3JDK target, ABI, JVM and benchmark gates execute. This is a prepared product postimage only; its Synexia recipe is retained separately.

Native loading is lazy and occurs only after Java argument validation; invalid arguments retain Java exceptions without requiring any JNI library or native side effects. Valid JNI calls load the configured library explicitly once per classloader.

## Adjacent-long query reuse (recipe successor)

In the independent binary-search path, consecutive identical signed-long query values share an identical absolute lower/upper pair. Java reuses the previous pair rather than searching twice. The native C17 batch retains one prior key/pair on the stack across JNI regions of 256 keys and reuses it on consecutive equality. The nondecreasing sorted-query merge path and every Java/JNI interface, validation rule, allocation and ordering contract are unchanged. An independent linear oracle, real JNI and three JVM modes must verify duplicate runs straddling JNI regions before admission; see the Synexia-owned atom specification.

## Bounded duplicate-run upper-bound fusion (recipe successor)

For unordered signed-long keys, the Java and explicitly selected C17/JNI paths now compute the lower bound once. Starting at that index, they inspect at most eight equal source elements. A mismatch or the slice end gives the exact upper bound immediately; otherwise a binary upper search starts at the already verified prefix end. This maintains O(q log n) asymptotic cost, signed comparison semantics, result ownership, validation/exception order, and the existing JNI export. The Java-only path still never loads the native library. Query caches and native buffers are invocation-local, and native dispatch is never automatic. Retained qualification covers duplicate runs of length 0, 1, 7, 8, 9, 17, 255, 256 and 257, empty slices, extremes and cross-region query keys. See the corresponding Synexia `BOUNDED_DUPLICATE_RUN_ATOM.md` evidence. Benchmarks are workload-specific; an arbitrary unordered batch may not accelerate.

## Four-slot JNI source-region cache (proposed successor)

The native C17 reader now retains four independently tagged 256-element source regions
on the stack for one invocation (8 KiB payload). A page is selected by a fixed bit-mask;
conflicting pages are simply reloaded with `GetLongArrayRegion`. Source and query arrays
remain read-only and no managed-heap pointer, JNI reference or state is retained between
invocations. No Java method, exception, native symbol or JNI descriptor changes.
This is an explicit opt-in native path; it is not the default Java search and is not a
source-built M3JDK performance claim. The canonical design and host/JNI checks remain
under Synexia `docs/M3_JNI_FOUR_WINDOW_CACHE_20261101.md`.
