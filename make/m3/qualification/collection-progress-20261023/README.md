<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 native java.base collection progress defaults receiver

**Canonical authoring/recipes remain exclusively in Synexia** [draft PR #13199](https://github.com/hsoliwal/com.synexia/pull/13199), pinned to `6e9d9940053a9fd425ab9d03f0f1414066441a0f`. This directory is a product receiving receipt, NOT another Maven/OpenRewrite recipe. No M3+JDK adapter or independent collections implementation.

The original product `Iterable`, `Iterator`, `PrimitiveIterator`, `Spliterator`, `Collection`, `List`, and `Map` are modified additively: **17 default methods**. Existing method signatures, virtual override dispatch, data-structure owners and native symbols remain unchanged. The original source preimages were captured from M3JDK21 `85ac51d562af4e3efff73d46a724134fe189a842`; the receiving parent `b032048eece51c897d8623fd7a69f7f16eac5a57` is later and differs only in compiler workflow/tooling files, not these owners (verified commit comparison).

## Contract and failure semantics
Each new method takes a borrowed non-null `ProgressMonitor` final argument; no method owns `begin`, `cancel`, or `done`. Checks are cooperative at element and delegate boundaries. Progress counts completed callbacks/evaluations/writes; callbacks throwing retain their exception identity with no false credit. Iterator and spliterator monitored names are deliberately distinct to avoid an additional dual-interface default diamond. Primitive traversal is direct and avoids boxing in its typed counterparts. `Collection.removeIfMonitored` delegates to original override and credits completed predicate evaluations, not committed removals. `addAllMonitored` and `putAllMonitored` are incremental and may leave a successful prefix if cancelled or failed. No infinite-memory or forced interruption claim is made.

## Source SHA-256 pre/post seal
```tsv
src/java.base/share/classes/java/lang/Iterable.java	745ac29fdcfb8887a461e5dd70b673024adb8a8ea54644724cbc4a3a09e23492	a4730f9f6db2e483a735ac22ce35bc8612601328aece59e58b7aa7efed6dd6d6	Iterable.java.txt
src/java.base/share/classes/java/util/Iterator.java	28f0ab0de8727ee357bd00792b6e490103219d177331e262f257ff0098994a7c	768f1b2b77a30e45a2f3bac44ccf3579289ab236837569b965e289eeb4197db9	Iterator.java.txt
src/java.base/share/classes/java/util/PrimitiveIterator.java	02ba4be245ced47049a679e6cf50229abce50fada7fd8b56874acc2fe29a72ff	5e6a843599faeb8474d5912664744499cbc7677633fafcf715bd8a90455e6621	PrimitiveIterator.java.txt
src/java.base/share/classes/java/util/Spliterator.java	70d75491a8ca51fe30a1a6935467763b5bdc3753b8624bec055edd4967383596	6b839fc39db8374b4243f3ab01d0eefc53bac224360db09f9d3c2a4eff3981b4	Spliterator.java.txt
src/java.base/share/classes/java/util/Collection.java	de833f449dd331f125692ae3dd0ff06d7ab62d9be47d7edb56907ce2dd7988e4	14e8012288203d6c1cdea12f20fa89f6e6dba989a08c85f235099d85509a7dad	Collection.java.txt
src/java.base/share/classes/java/util/List.java	bc5d882564a59adcadbe39615ba8213f4c33ad88222b8a3c4e7cdf50c62316b6	98c4f9dfbaa796a565cd29e5e37e4b467d54a17ac6f5a5cc82680de1da6f3974	List.java.txt
src/java.base/share/classes/java/util/Map.java	c2cb6eebb59d81a35a82827d7ad09b633317e5aa9c640b47c3fd5ebd9204c0a6	35646888b10d8cd78a2b664f602ded9d258eb371070e71a83d51038f033e068d	Map.java.txt
```

## Qualification gates
- **PASS (repository custody only):** seven sealed product source preimages with full before/after resources, no source deletion, static diff pinned to a known M3JDK21 source commit, receiver based on a newer parent where none of the seven source paths changed.
- **NEW HOST GATE:** `verify-host-patch.sh` and workflow compile seven actual target java.base owner files with Java21 synthetic ProgressMonitor and run the target jtreg contract in normal and `-Xint` modes. Do not claim a pass before observed.
- **HOLD:** canonical class-backed Maven/OpenRewrite fixed-point/drift refusal, source-built M3JDK21 JDK/HotSpot/jmods, actual jtreg and every JDK+Guava+Eclipse/Commons/PCollections contract matrix, native memory/performance metrics and platform gates remain to be completed before merging. No direct master write or forced rebase.

OpenJDK GPLv2-with-Classpath-exception owner headers are unchanged. First-party receipt is Apache-2.0.
