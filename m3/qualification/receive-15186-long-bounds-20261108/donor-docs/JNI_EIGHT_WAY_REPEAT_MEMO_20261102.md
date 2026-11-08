<!-- SPDX-FileCopyrightText: 2026 Synexia contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# JNI exact-key repeat memo: bounded set-associative atom (eight ways)

**Owner**: `m3_native_sorted_long_bounds.c`, inside the existing
`m3jdk-sorted-long-batch-bounds-v1` hash-pinned Maven/OpenRewrite recipe.
**Immutable Git preimage**: `hsoliwal/com.synexia@bacf1e8e394c8765cbe6b40a250d51f7421aac8a`.
This is a candidate-only native internal change; seven M3JDK result postimages remain
additive to the pinned product source, never copied to the receiver before real SDK/product gates.

## Existing contract is locked

The Java21 `bounds(long[],int,int,long[])` and JNI `nativeBounds(...)` method
signatures, exception order, input immutability, freshly-owned absolute bound pairs,
nondecreasing-source precondition, native-library opt-in and native JNI export
remain unchanged. The existing sorted/streamed path and four-window source-region
cache are not changed. All memory remains stack-local within an invocation; no JNI
array elements pointer is retained and no per-entry allocation is permitted.

## Targeted bottleneck

The current 64-entry primitive key-to-(lower,upper) memo is direct mapped; exact
non-adjacent repeated keys with equal memo hash indexes repeatedly evict one another.
This can make every lookup a binary search even though the query working set is
small. Category: duplicate-heavy batched search + fixed-size set-associative cache.
Donors: first-party memo already implemented by the preceding Synexia commit;
LeetCode 34/35/704, HackerRank binary-search category and GeeksforGeeks first/last
occurrence supply *problem taxonomy only*, not source or licence authority.

## Proposed atom and invariants

The initial unconditional four-way experiment improved adversarial collisions but
slowed existing seven-key rings. A guarded four-way attempt also thrashed on five
keys sharing a former slot; it is **rejected**. Instead activate the associative
memo **only when the first copied 64 query keys contain two unequal keys with the
same previous 64-slot index AND each proposed set contains at most eight distinct
sampled keys**. Otherwise execute the *original direct-mapped loop unchanged*.
The bounded classifier examines at most 64 keys and eight candidates per set.
Keep exactly **64 `M3BoundMemo` entries** in eight fixed sets of eight ways. Select set by
existing unsigned bit-mix; compare full signed 64-bit key in each occupied way.
A miss probes at most eight ways. Prefer a vacant way; if the set is full, use
an invocation-local deterministic round-robin victim index for that set.
Collisions are always cache misses, never false hits; no probing outside the
selected set. The prior conditional activation threshold and fallback are
unchanged. The number of cached entries, heap allocations and JNI reads for a
hit remain bounded; total space is O(64), query work O(q log n) in the worst case.

## Ordered execution and evidence

1. Hash/compare exact baseline sources and run baseline strict javac/C17, exported
   native JNI symbol, three JVM modes and independent linear-reference oracle.
2. Implement only the private C memo-selection atom and new deterministic fixtures:
   four, five and eight distinct signed-long keys deliberately sharing one prior
   direct slot; nine-key forced eviction/refusal, 255/256/257 query boundaries, source slices, signed
   extrema, independent oracle and unchanged array ownership/ABI.
3. Benchmark old and new native libraries with **identical Java class bytes** and
   fixed ordered, distinct, ordinary seven-key repeating and collision-heavy batches.
   Record regressions and refuse promotion if correctness/API/JNI or performance fails.
4. Update existing Maven/OpenRewrite Java/text postimage templates and seven-file
   SHA-256 manifest (no second recipe engine), category review, rights seal,
   correction logs, full crate manifest, refusal tests and fixed-point preflight.
5. Complete exact Git preimage/postimage matching and fast-forward only on a
   reviewed draft branch. Actual Maven/OpenRewrite SDK Result.getAfter, JDK runtime
   build, jtreg and target JNI/ABI remain **NOT_RUN/HOLD** until executed.
