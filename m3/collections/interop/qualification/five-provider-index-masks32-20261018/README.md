<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Five real provider interfaces over M3 indexed arrays: full 32-bit masks

Target repository: hsoliwal/M3jdk21, pinned master baseline
`4598bfc398d3024e7ea67e42034e857a53a1b378`.

Canonical Maven/OpenRewrite recipe: hsoliwal/com.synexia
[PR #12208](https://github.com/hsoliwal/com.synexia/pull/12208),
commit `7c1619d80f9596c98648d43c5835ac40065a6f08`.
Named recipe `com.synexia.m3.FiveLibraryIndexMasks32` composes the
existing `M3HashPinnedJavaSnapshotRecipe`. This branch received exactly
one existing Java file source-sealed replacement plus one absent-before
real dependency JUnit oracle, each in a separate ordered commit.

**Source seal**:
- M3FiveLibraryIndexViews.java PRE:
  605dbbf0193dbe9b298499d399f9c3607a95ce74b26884d8b3ac9c7aad50f193
- M3FiveLibraryIndexViews.java POST:
  fb8125ef79ffc4788b00571ab447a1ed08eca65a56668725db5b2d799a67e85b
- M3FiveLibraryState32Test.java PRE: ABSENT
- M3FiveLibraryState32Test.java POST:
  26548a60299d8647807401c467d013da60e2f9d248cb82450dcc6d0fe70a3d15

## Shared actual storage

`M3FiveLibraryIndexViews` continues to own only Object[] keys/values,
byte[] captured states, int[] occurrence path, half-open [from,to) views.
Full-width `int[][]` external state planes are used as borrowed
read-only metadata for each selection. Each plane must match captured
physical payload size. `int[]` row IDs are also physical offsets,
including duplicate occurrences. Use the existing
`M3IndexPathMasks.select(..., Backend.JAVA/NATIVE_REQUIRED)` C17 JNI
mechanism for all required/excluded 32-bit stages; bit31 supported.
`NATIVE_REQUIRED` never downgrades or masks a missing library. Java
comparators/objects/predicates are never passed to native. No new
JNI ABI, collection payload store or retained Map.Entry/Node.

The new first-match method returns zero-based occurrence ordinal
relative to this view (or -1) without allocating an output selection.
Output materializations remain independent mutable provider-native
interfaces using original M3 maps/lists/sets/trees; native selection
only returns detached primitive indexes.

## Original contracts remain separate

| Provider | Real contract/example | Existing M3 payload owner |
|---|---|---|
| JDK 21 | NavigableMap last-occurrence-wins, sorted-set filtering | M3TreeMap/M3TreeSet |
| Guava 33.4.8-jre | SortedSetMultimap; sorted comparator dedup per key | M3LinkedHashMap/M3TreeSet |
| Eclipse Collections 11.1.0 | MutableMap/Set/ListMultimap/SetMultimap | M3LinkedHashMap/Set, M3Eclipse*Multimap |
| Spring 6.2.0 | MultiValueMap: size counts KEYS, lists remain distinct | M3SpringArrayMultiValueMap |
| Apache Commons 4.5.0 | ListValuedMap/SetValuedMap: size counts PAIRS | M3ApacheList/SetValuedMap |

This does **not** replace every concrete library class, algorithm,
serializer, stream transformer, weak/reference collection, concurrent
collection or API. No library's public package is impersonated.
LeetCode/HackerRank/GFG contribute algorithm categories and test
shapes, not copied challenge solutions. Upstream Guava/Spring/Apache
Apache-2.0; Eclipse EPL/EDL; OpenJDK GPLv2 Classpath Exception
remain separately attributable.

## Admission gates / honest status

The new JUnit requires **actual** Guava, Eclipse, Spring and Apache
JARs, does not compile against provider stubs, and exercises five
real APIs, full-width bit31, negative masks, slice/duplicate rows,
stable index sorting, nulls, seeded 1,024 randomized trials, detached
materializations and Java/C17 JNI parity if loaded.

The exact-head workflow additionally compiles/installs the actual
M3 collections module, runs current vendor/JUnit including existing
SDK regressions, builds both existing JNI libraries, and reruns
against `-Xcheck:jni`. Separate canonical recipe fixed-point/refusal
tests are in Synexia #12208.

**DRAFT/HOLD** until this exact-head Maven/provider/JNI job actually
passes; baseline and source hash receipts alone are not runtime proof.
Installed-JDK jtreg/JCK, all-provider exhaustive source/API coverage,
serialization, JMM/concurrency, CPU and retained-heap comparisons also
remain pending. Do not advance the String->arrays->collections phase
solely from this receiver. Do not rebase Synexia develop or edit
M3JDK21 master directly.
