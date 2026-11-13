<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# M3JDK21: direct filtered/sorted iterator state-bit receiving

## Product, not a layer

**M3JDK21 is one modified Java/HotSpot/JNI JDK product.** The direct JDK `java.util.CollectionViews` owner and its existing M3 collection implementation owners are refined, not wrapped in a new Synexia runtime. External vendor APIs retain their distinct Java contracts. There is no new public Java surface or native symbol.

The eight target Java files in `SOURCE_MANIFEST.tsv` match the source-sealed canonical Synexia recipe dataset `m3-filtered-cursor-mask-20261021`. The original four master owner files are independently SHA-256-pinned. This M3JDK21 module receives their generated results as first-party code, with source-file license headers unchanged. JDK source packaging/license promotion is a separate required gate.

## Behavioral atoms refined

1. **Editable filtered List**: `M3FilteredLists.View.Cursor` keeps one `int` flags word for NEXT_PREPARED, PREVIOUS_PREPARED, FAILED, LAST_WAS_NEXT. All source-position deltas, reversible lookahead, predicate evaluations, exception poisoning, remove/set/add targeting and identity retain original operation order.
2. **Editable filtered Deque**: `M3FilteredDeques.View.Cursor` keeps READY, ENDED, POISONED in one flags word. Direct removal and the prefetched-element rebinding path are unchanged; no retained per-element object is introduced.
3. **Read-only Collection/SortedSet/NavigableSet/Map views**: `M3FilteredViews.filtered` uses READY/EXHAUSTED flags. Its nested predicate directory is materialized as one exact-size array of existing predicate references rather than a temporary growable ArrayList; evaluations still occur innermost-to-outermost and short-circuit identically. Sorted comparator, representative lookup and range errors remain untouched.
4. **Direct `java.base` factory**: `java.util.CollectionViews.selected` packs PREPARED/FOUND. PREPARED remains set if a user predicate throws; this preserves the original exception-and-retry behavior exactly rather than inventing a new iterator state machine.

All payload values remain in the backing JDK/M3 `Object[]`/primitive-array owners with offsets and indices; the cursor state is a small bit mask. No entry/node/cell objects are retained per element. This change **does not** introduce a new JNI function, move a Java callback into C, replace a backed view with a snapshot, or change thread-safety.

## Canonical recipe and donor decisions

The reusable Maven/OpenRewrite recipe and test suite live in hsoliwal/com.synexia. It includes four current-product source preimages, eight postimages, a direct java.base jtreg regression, three original M3 owner probes, JNI-required parity proof, actual OpenRewrite first-run/fixed-point/drift-refusal JUnit, problem-category `CATALOGUE.tsv`, and append-only `RECIPE_MEMORY.tsv`.

LeetCode #78, HackerRank Java Iterator/Java Dequeue and GeeksforGeeks bitmasking are catalogue problem references only. Original Guava, Eclipse Collections, Apache Commons Collections and Spring provider semantics were compared and not copied. Their mutability/iterator differences prohibit blind API replacement.

## Verification boundary

Source/postimage SHA-256 and Git blob readback verify byte identity but **not** JVM behavioral correctness. The owning M3 module JUnit and native-required mask selector with `-Xcheck:jni`, direct java.base patch-module compilation and jtreg, JDK installed-image ABI, full Maven/OpenRewrite SDK run, source-built M3JDK21, HotSpot/GC/CDS, other platforms and comparative heap/CPU/native-memory gates are independent and must be recorded as actually run, failed, blocked or not run.

The packet is reviewable but cannot certify full five-provider collection superset adoption or the Synexia 4,770-path inventory. The locked target receiving order String → arrays → collections → AST/compiler remains unchanged.

## Additive stable-sorted source reconciliation (second independent atom)

This review branch also contains the seven exact source/test/manifest/workflow files from previously open M3JDK21 [#1757](https://github.com/hsoliwal/M3jdk21/pull/1757), canonical recipe [Synexia #12854](https://github.com/hsoliwal/com.synexia/pull/12854). The original sorted owner `M3IndexedListSnapshot` has an otherwise disjoint current master preimage and retains the same public APIs, comparator callback order and existing JNI selector. Its ascending-only temporary `int[n]` reversal is replaced by `long[(n+63)/64]` group-boundary bits and a word-level predecessor walk, with no new retained backing store. Unsorted merge sorting is unchanged. The product source matches the prior canonical recipe's Git blob and preserves the prior separate source manifest and CI gate.

This is an **additive source union**, not a Git two-parent ancestry merge, a public API expansion, or proof that either independently opened draft passed its compilation/JNI/installed-JDK gates. The earlier PR and all its history remain available for review; do not close it until combined-source acceptance and owner sign-off. The `M3IndexPathRank.copySelectedRowIds` proposal from #1738 adds a callable API and is deliberately **not** copied into this contract-preserving branch without review.

## Second serial pass on the same filtered owner

The original `M3FilteredViews.CollectionView.contains` nested membership path
used a temporary growable `ArrayList<CollectionView>` to collect immutable
wrapper links and then replay their representative checks from inside to out.
The recipe now counts the same parent links without invoking predicates,
materializes one exact-size `CollectionView<?>[]` metadata directory,
and executes the **same** `visible` / `visibleMember` callbacks in their
previous order. No result/payload is cached, no public API is added and no
JNI ABI changes.

A source-owned Java21 regression compares a four-level nested membership
callback tape (`0,0,1,0,1,2,0,1,2,3`), absence short-circuit and a deliberately
throwing inner predicate. The canonical Synexia recipe and this product's
SHA-256/Git-blob manifest were re-sealed together. Source equality is
verified independently from compiler, JUnit, JNI and installed-JDK admission.
