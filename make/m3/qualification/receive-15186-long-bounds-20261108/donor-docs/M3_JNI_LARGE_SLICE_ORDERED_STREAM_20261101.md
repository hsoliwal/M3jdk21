<!-- SPDX-FileCopyrightText: 2026 Synexia contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Large-slice JNI ordered stream: bounded M3 replaceable atom

Baseline `hsoliwal/com.synexia@d772c28eee430d7f3c40e2350098702c214c602f`.
Existing Maven/OpenRewrite crate: `m3jdk-sorted-long-batch-bounds-20261101`;
existing receiver paths only, no added JDK API, no Java signature changes.

## Category and alternatives

- Algorithm category: sorted batch searching / two-pointer merge / monotone cursor.
- Prior owner: signed-long C17/JNI 4 x 256-entry region cache plus small-slice prefetch.
- Candidate: for a large source slice and a dense nondecreasing query prefix,
  maintain forward-only lower/upper source cursors with `GetLongArrayRegion`-backed
  `at()`; otherwise retain the unchanged per-key binary search. Based on first-party
  small-slice merge, not copied competitive-programming submissions.
- LeetCode 34/35/704, HackerRank binary-search category, and GeeksforGeeks
  first/last occurrence are *problem/counterexample references only*; no copied code.
- No third-party source, compiler, dependency, or architecture-specific JNI API.

## Locked contracts

`M3NativeSortedLongBounds.bounds` still validates both arrays, slice, and output
size in that order before loading the library. `nativeBounds` ABI, Java source,
values, duplicate first/last positions, side effects, exception behavior, array
ownership, concurrency, absolute index semantics and Unicode are untouched.
No stale Java-heap pointers, retained JNIEnv, dynamic native allocations or new
native export. Native selection remains explicit opt-in.

For a nondecreasing signed-long source `[from,to)` and `key >= previousKey`,
`lower` and `upper` source offsets may only move forward.  The first source
index `>= key` is `lower`; first `> key` is `upper`. Every candidate query is
checked against its immediate predecessor; a descending key irreversibly
returns to original binary search for the remaining invocation, even when the
new query batch starts at a 256-key JNI region boundary.

**Cost gate:** try large-source streaming only when `1024 < to-from <= 8192`,
`keys.length >= 256`, and `(long)(to-from) <= 4L * keys.length`.
This bounds scan comparisons by 4q; any source beyond that budget or first
chunk with fewer than eight strict increases retains the old binary path.
The first 256 query keys must be nondecreasing before activating streaming;
the first-chunk classifier uses the *already copied* JNI query buffer. No
native full-source prefetch: existing four 256-value windows remain source
storage. No source sortedness scan is added; caller's original sortedness
precondition remains locked.

## Required qualification

- Input identity: exact base Git blob SHA-1, source SHA-256 and receiver
  absent-before manifest. Atomize only C17 source, Java self-test and
  benchmark leaves, preserve other sources.
- Bounded correctness: test length 1024/1025/2048/4096/8192/8193,
  query count 127/128/255/256/257/1024/2048/4096, signed extrema,
  equal runs, late descent, slice offsets, 256-key query/source boundaries,
  parallel read-only use, all ownership and error invariants.
- Verification order: diff/lint -> `javac --release 21 -Xlint:all -Werror -h`
  -> GCC C17 `-Wall -Wextra -Werror -Wpedantic` + `nm` -> actual JVM
  interpreter/C1/tiered independent-oracle tests -> paired benchmark ->
  Javadoc -> recipe hash resources/refusals/fixed point -> real OpenRewrite SDK
  -> source-built M3JDK/jtreg/product JNI ABI/benchmarks.
- Benchmark workload with `N=4096,Q=4096` independently against the original
  exact C17 artifact using identical Java class bytes. Measure ordered,
  ordered-then-late-drop and unordered separately. Do not claim a universal
  speedup, and retain rejected candidates and regressions.
- Local compile/test evidence is host JDK only. Unless actual SDK and target
  build run, receiver change is HOLD and M3JDK product is unchanged.
