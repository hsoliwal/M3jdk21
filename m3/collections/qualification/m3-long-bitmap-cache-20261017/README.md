<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# M3 Collections: cached bitmap-filtered primitive snapshots (2026-10-17)

## Ownership and boundaries

* Canonical donor-convergence recipe: `hsoliwal/com.synexia`, `synexia-m3-recipe/recipes/m3-long-bitmap-cache-20261017/` (new branch/PR).
* Product receiver: `hsoliwal/M3jdk21`, separately compiled Apache-2.0 `m3/collections` module.
* No new `java.util`, JDK `String`, `java.base`, JNI ABI, C, native array pinning, object payload owner, or additional collection hierarchy.
* The old `M3LongFilteredView` is **live** and intentionally unchanged. Caching a dynamic predicate without an invalidation/version contract would be semantically incorrect.
* Apache-2.0 first-party source/recipe; retain named contributor copyright and target NOTICE. OpenJDK/LeetCode/HackerRank/GeeksforGeeks and Apache Commons source is not copied.

## One bounded new collection atom

`M3LongBitmapFilteredSnapshot` is an opt-in `M3LongCollection` adapter: retain one immutable `M3LongIndexProjection` owning a privately copied `long[]` and `int[]` index permutation, plus one `BitSet` of **selected physical slots**. The predicate runs **once per input occurrence**, creating an aligned `byte[]` for the current `M3IndexMaskCompactor` Java/optional JNI selector. The returned compact projection provides O(1) ordinal reads. Values and duplicate occurrences are retained; only index IDs are compacted. No `new BitSet(Integer.MAX_VALUE)`: the BitSet grows as selected physical IDs are seen, and the backing is limited to actual source length.

`sortedAscending` and `sortedDescending` are index-only stable sorts of admitted slots, not of payload values. They reuse the same proven projection and retained state bitmap; the predicate is not called. `slice` shares the immutable indexed value backing, rebuilding a view-accurate bitmap from selected IDs, not by rerunning the predicate. `filtered` composes a second predicate **once on previously selected occurrences**. There is no automatic re-filter on read, iterator, size, contains, stream, sort, or slice.

`VersionedView` opt-in adapter caches the immutable snapshot by caller-provided revision (long). Repeated reads of the same revision only read the revision supplier, with no filter calls. A new revision triggers one source `toArray()` and one predicate pass, with before/after revision checks. If the revision changes while capturing, the change refuses with `ConcurrentModificationException` rather than publishing a partial cache. An explicit `invalidate()` forces regeneration if a trustworthy epoch is unavailable. The revision **must cover the source data and every predicate dependency**. Iterators reject later revision changes. Snapshot data and live Java collection semantics are deliberately not conflated. No claim of lock-free concurrent source capture; external synchronization is required for concurrent mutable sources.

## Cost and semantic qualifications

| Operation | Cost | State | Contract |
| --- | --- | --- | --- |
| Snapshot + first filter | O(n) | immutable `long[]` + index `int[]`, compacted index `int[]`, `BitSet` up to highest accepted ID, temporary `byte[n]` | Long predicate once per source occurrence; throwing predicate publishes nothing |
| Repeated `get` / `size` / physical membership | O(1) | none new | Cached only; never re-run predicate |
| Iteration / `toArray` | O(k) | output `long[k]` only for `toArray` | Duplicates, encounter order, immutable snapshot |
| Stable ascending/descending sort | O(k log k) | O(k) primitive index scratch | Sort IDs, not payload; stable ties |
| Slice | O(k_window) | result `BitSet`, shared payload/index | No predicate calls, offsets retained |
| Versioned unchanged read | O(1) epoch check + query cost | one cached snapshot | Safe only with fully covering revision |
| Versioned refresh | O(n) | new detached snapshot | On source/predicate revision change |
| Optional JNI | O(n) primitive index selection | bounded region copies | Only existing `M3IndexMaskCompactor`, no new native owner |

`BitSet` dense enough for 1 bit per physical ID. For sparse 28-bit M3 slot namespaces, prefer the **existing** `M3BitLane28`, not maximum-capacity eager allocation. This PR does not duplicate it.

## Recipe-first serial receiving plan

1. `M3LongBitmapFilteredSnapshot.java`: source absent on exact receiving base. Use the pinned Synexia Maven class-based installation recipe; generate a new owner. Existing `M3LongIndexProjection` and `M3IndexMaskCompactor` blobs are retained byte-for-byte.
2. `M3LongBitmapFilteredSnapshotSelfTest.java`: new Oracle generated through that same recipe. Fixed random seed, signed extremes, duplicate physical IDs, JDK stable sorting, masks/slices, source/mask isolation, revised predicate behavior and refusal/JNI parity.
3. Repository CI `mvn ... recipe/pom.xml verify`, exact fixed-point source check, targeted Java 21 `-Xlint:all -Werror`, standalone test without native, checked JNI+UBSan, full owning Maven module test. Promotion beyond a draft requires verified head-run results and resource-sensitive benchmarks.
4. Any incorrect production output must be repaired in the **Synexia recipe template** and regenerated; do not manually modify generated sources. Recipe refuses occupied/drift/symlink targets, checks all before writing, executes idempotently and reaches fixed point.

## Local qualification completed

* `javac --release 21 -proc:none -Xlint:all -Werror` selected Java baseline/interface stand-ins and new source: **PASS**.
* Java-only standalone oracle: **PASS 31,923 checks**.
* Existing real JNI C (`m3_index_mask_compactor.c`, Git blob `f1a9488905807e6417dd267e125bfc44dbcc9297`) built with `-std=c17 -O2 -Wall -Wextra -Werror -fsanitize=undefined -fPIC` and exercised with `java -Xcheck:jni`: **PASS 32,425 checks**. Counts are overlapping; do not add them.
* Canonical deterministic Java recipe compiled, --selftest PASS (missing/drift, occupied refusal, idempotence, fixed point); --apply then --check against a bounded target proof fixture PASS. Local compiler uses a matching API signature copy of current M3LongCollection/Iterator and exact Git-blob-matched incumbent M3LongIndexProjection/M3IndexMaskCompactor (including the newer all-selected fast path on receiving base `b42ab7605b862a5157997f1787c5032db2831f46`).
* Maven command and actual full receiving-project CI were **NOT_RUN** locally: Maven unavailable. Full installed JDK/HotSpot/jtreg and profiler/benchmark gates remain **PENDING**. No speedup claim.

## Receiver references

* Existing `M3LongIndexProjection.java` Git blob: `416291e4d93e583f07fab280c577b7cdddc6d5ce`.
* Existing `M3IndexMaskCompactor.java` Git blob: `958d1bc57f6c90f78d4e5f408cc758d1adc8fba5`.
* Existing `M3LongCollection.java` Git blob: `d0b389681ade738e97426d950b6291c746912cf0`.
* Exact new template byte SHAs are embedded in the recipe `Output` manifest and can be independently verified.
