<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3JDK21 collection family evidence ledger, 2026-10-17

Exact pinned **M3JDK21 master** source revision: `e913ab89056657eca4009678cfe9a657c020e79b`.
`COLLECTION_SOURCE_INVENTORY.tsv` indexes 114 Java implementation/source entries across 13 proposed problem categories. 34 entries have tests matching their class name by a narrow filename heuristic; other tests may exist, so **NOT_DISCOVERED** does not mean untested. Category and mechanism are search candidates, not verified source-level facts. Every row is SOURCE_INVENTORIED, **not** a claim of feature parity, fastest algorithm, installed JDK delivery or API compatibility.

`ALGORITHM_DECISIONS.tsv` documents reuse/rejection by category. Product APIs retain JDK and provider names/contracts, and primitive analogues remain distinct. M3 favors parent-owned dynamic arrays, primitive masks, offsets, indirect indexes and indexed views without retained Entry/Node payload graphs when contracts allow. Existing JNI selectors never evaluate a Java comparator or Predicate.

The immediate sealed FILE improvement is `M3FilteredViews.java`: preserve read-only filtered NavigableSet semantics under null-admitting comparators and out-of-range queries against bounded/range views. The existing `M3FilteredNavigableSet` compatibility facade delegates to this implementation; no second payload owner is added.

This census is a **bounded exact-source inventory**, not evidence that all collections (or all SDK families) are fully implemented. String → arrays → collections → AST/compiler promotion remains proof-gated. The canonical reusable Maven/OpenRewrite recipe belongs in Synexia; target repository retains generated sources and exact donor receipts. The JDK remains GPLv2/CPE, Synexia/M3 source Apache-2.0 where covered, Eclipse Collections EPL/EDL, Guava/Spring/Apache Commons Apache-2.0. Upstream material was consulted as contract evidence only, not copied.
