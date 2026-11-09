<!-- SPDX-FileCopyrightText: 2026 Synexia contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 sorted signed-long bounds (opt-in tooling)

`jdk.m3.search.SortedLongBounds` performs absolute lower/upper bound lookups on a nondecreasing signed `long[]` slice. It returns an interleaved fresh `int[]` of `[first >= key, first > key]` positions for each query. The source array and query array are never changed, and Java-only use needs no native library. A Java batch of 128+ sorted query values uses a forward-only merge; other batches use binary search. It is not a replacement of `java.util.Arrays.binarySearch`, nor a mutation to standard JDK 21 behavior.

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

## Adaptive whole-slice JNI prefetch (host-only candidate)

The native owner is still opt-in. For query batches >=128 with a source slice
of at most 1,024 values, the first already copied query region is sampled for
at least eight adjacent changes among the first 32 keys. Only then does one
`GetLongArrayRegion` copy the whole source slice into the existing 8 KiB
stack cache, and pure native bounds use that snapshot. Low-diversity batches,
small batches, empty slices and larger sources keep the proven four-window
demand-cache path. No heap pin, JNI reference, public signature, exception
order or JNI export changes. LeetCode/HackerRank/GeeksforGeeks supply only
problem-category references. The paired host tests show workload-dependent
gains and a small clustered regression; there is **no automatic dispatch**
and no source-built M3JDK product admission. The Synexia-owned specification
is `docs/M3_JNI_SMALL_SLICE_PREFETCH_20261101.md`.

## Ordered native merge over a fully prefetched source slice (2026-11-01)

The existing C17/JNI native owner now recognizes a nondecreasing first query
chunk with eight or more strict increases, while retaining the short-sample
variation gate. For an eligible nonempty source slice of at most 1,024 signed
longs and at least 128 query keys, one JNI region read fills existing stack
storage; two forward-only cursors then find lower and upper bounds in O(n+q)
for fully nondecreasing keys. This selection is provisional: **every** later
query is compared with the preceding value and the first descending key
irrevocably switches the rest of the invocation to the original native binary
search against that same cache. Unqualified inputs keep the prior four-window
JNI fallback or cached binary path. No Java API, exception ordering, JNI
export, allocation ownership or automatic native dispatch changes. The
canonical specification is `docs/M3_JNI_ORDERED_MERGE_20261101.md` in Synexia.
Source-built M3JDK and actual OpenRewrite SDK acceptance remain **NOT_RUN**.

## Large-source ordered query streaming (opt-in native)

The C17/JNI implementation may use a forward-only two-cursor search for
nondecreasing query batches over 1,025..8,192 sorted signed-long values when
there are at least 256 keys and the source span is at most four times the
query count. Qualification examines the already-copied first 256 keys; every
later descending key irrevocably falls back to the original binary route.
The four fixed 256-long native windows are reused; no full-source copy,
retained Java reference, new JNI symbol, new Java API, or automatic native
dispatch is introduced. The Java owner and its exception/ownership behavior
are unchanged. Product acceptance requires source-built JDK tests.

## Native nonadjacent repeated-key memo (2026-11-02)

For sufficiently large **unordered** long-query batches, the optional JNI
path samples the already copied first query chunk and enables a bounded
invocation-local, 64-slot primitive cache only when at least ten keys recur
nonadjacently. A memo hit requires an **exact signed-long key equality**;
hash collisions cause a miss and replacement, never incorrect bounds.
Source pages and the 4x256 native window cache are unchanged. Adjacent-key
reuse, monotone streaming, original binary fallback, output-array ownership,
all Java and JNI signatures and validation/exception ordering are preserved.
This cache is neither shared among JNI invocations nor automatically selected
from the Java-only API. See the Synexia recipe's
`docs/M3_JNI_NONADJACENT_REPEAT_MEMO_20261102.md`. Source-built M3JDK/JNI
ABI, jtreg, actual OpenRewrite SDK and product benchmarks remain NOT_RUN/HOLD.

## Eight-way, collision-qualified JNI memo (2026-11-02)

The prior direct-mapped 64-entry native memo remains the default for unordered
repetition. A bounded classifier now enables an eight-way set-associative
layout **without increasing the 64-entry primitive cache** only when its first
64 query keys prove a former slot collision and each proposed set has <=8
sampled distinct keys; otherwise the exact old direct-map loop remains.
A nine-key sampled overcapacity refuses the associative path. Cache entries
retain exact signed-long key checks, independent absolute lower/upper pairs,
invocation-only stack state and deterministic round-robin replacement.
See `docs/JNI_EIGHT_WAY_REPEAT_MEMO_20261102.md`,
`validation/JNI_EIGHT_WAY_PAIRED_HOST.tsv`, the recorded rejected four-way
trial, the source seals and regression oracle. LeetCode, HackerRank and
GeeksforGeeks supply problem *categories only*; no third-party solutions were
copied. Host JVM/JNI tests are not an executed Maven/OpenRewrite SDK or
source-built M3JDK product qualification; both remain **NOT_RUN/HOLD**.
