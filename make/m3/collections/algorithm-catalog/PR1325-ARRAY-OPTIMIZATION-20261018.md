<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# PR #1325: array-only optimization catalogue and hash-probe candidate

**Disposition:** ISOLATED_M3_COLLECTION_CANDIDATE; NOT an OpenJDK `java.util` replacement, NOT an admitted generated Synexia recipe, NOT a finished collection-family migration. The receiving order remains **String → arrays → collections → AST/compiler**; collection promotion is pending.

## Complete imported-source classification

`pr1325-leetcode-hackerrank-array-kernels.tsv` covers exactly the **59 Java source files** in the PR #1325 namespace-only import, grouped into 13 distinct algorithm/contract families. Every row remains `IMPORT_CUSTODY_ONLY_NO_RUNTIME_ADMISSION`, regardless of source filename. A category is a search/filter instruction, not evidence that its candidate solves the full JDK contract. Abstract/interface sources require contract verification rather than storage conversion.

LeetCode and HackerRank links identify **problem categories only**: hash lookup, array compaction and shifting, bitsets, indexed queues, heaps, navigable/range lookup, and logical-order manipulation. No editorial, submitted solution or proprietary implementation was copied. Inspect each candidate's license separately before source reuse; otherwise implement the public algorithm independently and compare against OpenJDK 21 JDK behaviors. Prioritize contracts and existing M3 owners over a new wrapper hierarchy.

## Concrete single-file optimization candidate

`com.m3.collections.M3LinkedHashMap.Table` already stores reference payloads only in `Object[] keys`/`Object[] values`; `int[] hashes`, `positions`, `before`, `after` and `index` describe physical positions, encounter order and free slots. It never stores a per-key `Entry` or `Node` object. The new delete operation is a circular, backward-shift **hash-index** compaction: it scans from the removed bucket until a truly empty bucket, moving only `index[bucket]` and `positions[slot]` when the gap lies on that key's home-to-current probe path. The bitmasked circular distance condition handles wraparound:

`((scan - home) & (capacity - 1)) >= ((scan - hole) & (capacity - 1))`

The existing stable key/value *slot numbers*, payload arrays, insertion/access encounter links, generation/lifecycle owners and no-entry API boundary are preserved. Deletion may scan a full collision run and so remains **O(n) worst case**; lookups and insertions remain **O(1) expected**, **O(n) worst case**. Removal frees only the old physical slot; reinsertions reuse its slot via the preexisting integer free list. No tombstone remains, and the live-size load threshold alone controls index doubling. The change is specifically advantageous for workloads where absent lookups otherwise probe historical deletion tombstones. It is not a universal speedup claim (high-collision delete-heavy workloads require separate measurement).

The optimization belongs in the existing `M3LinkedHashMap`, which can support the JDK-shaped `LinkedHashMap` and `LinkedHashSet` families **after** separate receiver/contract qualification. Do not write an unrelated `HashMap` copy or merge the GPLv2/Classpath OpenJDK source payload into an Apache-2.0 file. The original 59 imported GPL/Classpath source files and raw inverse/header seals remain untouched.

### Behavior and reuse policy

- Storage is only dynamic Java arrays for reference payloads and primitive index/mask/order metadata. No structural `Map.Entry`, node or per-element object. Java GC continues to trace `Object[]` references; native code must not retain unstable Java heap addresses.
- `entrySet().iterator().next()`, `firstEntry()`, `pollFirstEntry()`, etc. may construct API-specified entries **only on explicit calls**. An `M3LFUCache` may reuse transient private work objects only after exclusive ownership/release has been proven; it **must not recycle** public `Map.Entry` objects while callers can still retain them. Existing `M3NoRetainedStructuralEntryTest` asserts fresh user-visible entry identities and blocks implicit pooling.
- Null keys/values, reversed/ordered views, access order, `Map.Entry.setValue`, fail-fast iterators, mutating callbacks, equality/hashCode, serialization and concurrency semantics remain owner-specific contracts. Weak keys require GC-visible reference objects; concurrent families require proper JMM/progress proof rather than mechanical substitution.
- **Recipe-first graduation:** inventory → dated additive convergence → algorithm taxonomy → Inline → Atomize → Patternize → Comment → source-sealed Maven/OpenRewrite recipe fixed point/refusal → Java contract tests, cost evidence, JDK/HotSpot/JNI gates → PR. This candidate patch is an isolated target experiment; Synexia canonical recipe generation/pin is **PENDING** and must be completed before promoting it to a qualified receiver. Do not manually patch generated source after a qualified recipe exists.

### Proof status

- Included JUnit 5 test: `M3LinkedHashBackshiftTest` performs 48,000 seeded, colliding mixed-map operations against Java 21 `LinkedHashMap` in insertion and access modes, including null key/value, `putFirst`/`putLast`, `reversed`, `compute`, iterator removal, invariants, and fail-fast checks. Added test **not executed** against full M3 module in this change environment.
- Locally executed independent Java 21 *algorithm kernel* (not the built M3 target class): `javac -Xlint:all -Werror` and 450,000 random add/remove/lookup differential operations with repeated invariant checks **passed**. This supports the circular-index move rule, not target integration or production speed.
- Preliminary kernel-only, uncalibrated microtiming showed back-shift can beat accumulating-tombstone probing in synthetic churn. A JMH-style test with warm-up, controlled table occupancy, collision distribution, separate get/put/remove results and retained-heap/allocation reporting is still required. No multiplier is accepted as a product performance claim.
- Maven full module compilation, existing JDK-differential suite, Synexia recipe fixed-point/refusal, native/jtreg gates, CI, and benchmark crossover **PENDING**. No JNI addition is required for this Java-only candidate; a JNI route would need independent evidence.

### Related problem taxonomy

- [LeetCode 706 — Design HashMap](https://leetcode.com/problems/design-hashmap/): hash-index probes, update/delete semantics.
- [LeetCode 146 — LRU Cache](https://leetcode.com/problems/lru-cache/) and [460 — LFU Cache](https://leetcode.com/problems/lfu-cache/): integer slot-based recency/frequency indexes without persistent entry carriers; LFU object reuse is a separate future contract.
- [LeetCode 189 — Rotate Array](https://leetcode.com/problems/rotate-array/): index permutation while leaving payload identity unchanged.
- [HackerRank Dynamic Array](https://www.hackerrank.com/challenges/dynamic-array/problem): checked capacity and index addressing.
- [HackerRank Array Manipulation](https://www.hackerrank.com/challenges/array-manipulation/problem): batch/difference-array updates, subject to exact range semantics.

For every subsequent source family choose an independently derived algorithm only after profiling that family, and leave the imported custody source untouched until precise promotion proofs are available.
