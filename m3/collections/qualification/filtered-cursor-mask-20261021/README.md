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
