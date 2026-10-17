<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Canonical Synexia Maven/OpenRewrite recipe: Eclipse Collections multimaps

**Exact convergence baseline:** append-only `hsoliwal/com.synexia develop@0a5d36185342e6f59d5cd8e4321ea0ab0d9aeecd`. **Product receiving parent:** `hsoliwal/M3jdk21 m3/five-provider-array-interop-20261017@58a72d0b2b8ffab220e3dc6e53ff7cc5d136bfb5`, PR #1174. These branches are not rebased or force-updated. SDK product runtime remains independent of private Synexia.

## Implemented Java21 surface
Three actual Eclipse Collections 11.1.0 APIs are implemented by independent, additive classes in the existing `com.m3.collections.interop` package:

- `M3EclipseListMultimap<K,V>` extends Eclipse `AbstractMutableListMultimap<K,V>` and implements `org.eclipse.collections.api.multimap.list.MutableListMultimap<K,V>`.
- `M3EclipseSetMultimap<K,V>` extends Eclipse `AbstractMutableSetMultimap<K,V>` and implements `org.eclipse.collections.api.multimap.set.MutableSetMultimap<K,V>`.
- `M3EclipseBagMultimap<K,V>` extends Eclipse `AbstractMutableBagMultimap<K,V>` and implements `org.eclipse.collections.api.multimap.bag.MutableBagMultimap<K,V>`.

All three use the pre-existing `M3LinkedHashMap<K,MutableList/Set/Bag<V>>` group directory, `M3LinkedList` index-link list, `M3LinkedHashSet` key/index set, and `M3EclipseBag` (the existing `M3ObjectCounts` primitive multiplicity and application-mask columns). There is no new object-per-pair Entry/Node storage layer. Eclipse's actual superclass governs its `get()`, `removeAll`, count, absent/empty, and view contracts; it is **not** Guava's writable `get()`, Spring's map-of-lists, or Apache's multimap. Distinct mutable `newEmpty()` and `toMutable()` copies reuse M3; `flip()` returns M3-backed bag/set peer.

`M3EclipseMultimaps` creates the three correctly typed Eclipse interfaces. No fake org.eclipse package or duplicate donor implementations. Existing five-provider interop and JNI C17 machinery remain unchanged; these Java callback-bearing owner operations do not cross JNI. Existing M3 flag/mask selection can be exercised independently by the current PR #1174 native oracle.

## Mandatory source-sealed Maven/OpenRewrite generator
This crate **reuses exact first-party blobs** `M3GpuToolJoinRecipe.java` and `M3GpuToolJoinManifest.java` from the already received M3JDK21 source-sealed SDK generator. The new named class `com.synexia.rewrite.M3EclipseMultimapRecipe` is only a resource-path selector. Its `manifest.tsv` seals five ABSENT Java postimages by SHA-256; generator preflights drift/occupied/duplicate source, supports fixed point, and emits exact `PlainText` SourceFile candidates through OpenRewrite's actual `ScanningRecipe`. No production file is hand-edited.

**Local environment limitation:** JDK 21 and gcc exist, but Maven and dependency binaries were not installed and Maven Central DNS was unavailable. Real Eclipse library/JUnit and host recipe execution, plus actual whole-module/JNI tests, are **NOT RUN locally**. The new JUnit suite is authored against the *real* Eclipse classes, not test-only shadows, and will run in the branch CI when dependencies resolve. No speedup, full five-library SDK completion, installed JDK/jtreg, or branch-protection success is claimed.

## Licenses and donor review
New wrapper, recipe, and tests: Copyright 2026 Hitesh Soliwal and contributors, Apache-2.0. Eclipse Collections 11.1.0 remains an external EPL-1.0 / EDL-1.0 dependency; we reference superclass hooks, not copy Eclipse source. The existing M3EclipseBag adaptation preserves its upstream notice. Exact Eclipse donor Git blob IDs, LeetCode/HackerRank/GFG classification and rejection rationale are retained in `CATEGORY_REVIEW.tsv` and `PROVENANCE.tsv`.
