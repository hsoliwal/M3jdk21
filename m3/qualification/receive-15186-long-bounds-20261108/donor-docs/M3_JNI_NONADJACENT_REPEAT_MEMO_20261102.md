<!-- SPDX-FileCopyrightText: 2026 Synexia contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Native non-adjacent repeated-key memo — bounded M3 search atom

Canonical Synexia recipe: `m3jdk-sorted-long-batch-bounds-v1`. Baseline:
`hsoliwal/com.synexia@e8288da6a9043c3b7b681c558f34991e45cf3288`.
The Java 21 code and public API, C17 JNI symbol/descriptor, `int[]` owner and
M3JDK21 product are unchanged. Category: repeated-key binary search / first and
last occurrence; compare first-party adjacent-key reuse, fixed-slot primitive
cache, and original four-window lookup. LeetCode 34/35/704, HackerRank search
and GeeksforGeeks first/last-position are category/counterexample references only;
no third-party source or challenge implementations are copied.

## Contract and admissibility

For a single invocation, a result pair is a deterministic function of one signed
`jlong` key and the same immutable sorted `long[]` slice `[from,to)`; therefore,
a previously computed pair may be reused for an **exactly equal** key even when
queries are nonadjacent. A fixed-size direct-mapped primitive table stores the
key and its absolute lower/upper pair. A conflicting hash slot may evict an entry
but MUST NEVER return another key's bounds. Entries, validity and classifier
state are local to one native call (no JNIEnv/pointer/global state retention).

The existing immediate previous-key reuse, ordered two-cursor scan, complete-slice
prefetch and four-window source cache remain authoritative. Activate the memo
only for sufficiently large unordered batches over slices larger than 1,024,
when the first already-copied 64 query keys contain a threshold of *nonadjacent*
exact repeated keys. Otherwise execute the original binary loop byte-for-byte.
A dropped ordered prefix never inherits a stale memo. A memo collision is a
miss, not equality evidence. No array sortedness check, native heap allocation,
external API or automatic JNI dispatch is admitted.

Source/keys are caller-owned immutable for the call; output is a new independent
Java array. Preserve null/range/length exception order and all JNI exception
checking. Query count, source-offset and signed-extrema arithmetic must not
introduce overflow or invalid indexing. Constant storage is bounded; the source
page cache remains four 256-value windows; at most 64 memo entries are added.

## Required proof

- Preimage Git blob/SHA-256 and exact postimage identity, source-document-first.
- Dedicated nonadjacent keys: rings of 3/7/17/63/64/65 keys; 255/256/257 and
  511/512/513 query lengths, signed extrema, missed/inserted keys, slices with
  nonzero offsets, hash-slot collisions and deterministic worker concurrency.
- Java21 `javac -h` + C17 strict compiler + exact JNI export/header, interpreter,
  C1-limited and tiered independent-oracle differential tests.
- Paired same-Java-class bytecode/native-libraries host comparisons for repeated
  nonadjacent, random-distinct, ordered, clustered-adjacent and large streams.
  A repeat workload gain MUST NOT be described as a universal speedup.
- Source hash and MIME/rights refusal, source-seal fixed point, real SDK/JUnit,
  M3JDK21 product JVM/JIT/jtreg/JNI acceptance separately. Missing actual SDK or
  target runtime evidence is HOLD, not PASS.
