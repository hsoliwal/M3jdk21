<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3 filtered/sorted current-owner source handoff

Canonical Maven/OpenRewrite recipe: hsoliwal/com.synexia PR #12151,
`com.synexia.m3.Jdk21FilteredBoundedOwner20261017`,
exact source commit `a9670e22d6b19a3fcf62441f3bc9922738539646`.
M3JDK21 baseline `master@e913ab89056657eca4009678cfe9a657c020e79b`.
All four Java blobs match canonical Synexia postimage templates;
two pre-existing files are replaced in serial commits, two tests
have source-manifest ABSENT-before admission.

M3FilteredNavigableSet is a read-only facade and was not edited.
M3FilteredViews owns read-only filtered Collection/Set/SortedSet/
NavigableSet/SortedMap/NavigableMap; M3MutableFilteredViews owns mutable
NavigableSet/Map filter behavior. Those exact owners were refined;
public/protected APIs and source names remain unchanged.

**Atom:** constructing a new headSet/tailSet/headMap/tailMap range
from a previously bounded view may reject valid lower/floor/ceiling/
higher lookup pivots. The original one-iterator scan is unchanged for
in-range queries. Out-of-range range-construction refusal alone falls
back to the same live owner's comparator and iterator, preserving
null-support, stored representatives, order and predicates. Exceptional
fallback O(n), not a performance claim. Source code retains original
parallel array/index/offset/bitmask owners, no new retained per-entry
objects, no Java callbacks/objects through JNI, no new native ABI.

**Catalogue:** ../algorithm-catalog/filtered-bounded-navigation-20261017.tsv
contains problem categories, original donor APIs and commit pins,
decision/rationale, complexity and future proof gaps. Guava, Eclipse,
Apache, Spring and OpenJDK contracts stay distinct and correctly licensed;
competitive sites supply category ideas, no copied editorials.

**Executable gates in .github/workflows/m3-filtered-bounded-owner-20261017.yml:**
SHA-256 exact target check; strict Java 21 original/target `javap -public`
parity; prior JDK filtered view selftests; additional seeded/null/descending
subview oracle; actual Maven/JUnit; existing C17 M3IndexPathMasks JNI parity.
Canonical OpenRewrite fixed-point/refusal is in Synexia PR #12151.

**Draft / HOLD:** As authored, hosted gates were queued, not passed.
Do not claim a complete five-provider SDK, benchmarked speedup,
installed JDK/jtreg/JCK/serialization/concurrency qualification, or
String->arrays->collections promotion. Preserve master and Synexia
develop from direct writes, rebases and force pushes.
