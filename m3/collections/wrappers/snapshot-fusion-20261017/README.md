<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 JDK21-superset: fused filtered/sorted snapshots

Status: source-sealed additive receiving candidate. Stacked on the exact
M3JDK21 #988 filtered/sorted owner branch, not on master `java.base`.
Canonical Synexia transformation/donor is draft #11938. The **M3JDK21
superset invariant** requires complete preservation of the original JDK21
APIs/contracts/observable behavior. M3 features remain opt-in extensions.

## Two new public M3 methods
- `M3CollectionViews.filteredSnapshot(Collection, Predicate)`: single-pass,
  detached, unmodifiable encounter-order List. Null and duplicate references
  preserved. One predicate call for each encountered source element.
- `M3CollectionViews.filteredSortedSnapshot(Collection, Predicate, Comparator)`:
  identical admission with stable `ArrayList.sort`, original comparator tie
  order, null comparator using natural Comparable sort and nulls as allowed by
  comparator. Source data remains caller-owned and unmodified.

Both reuse one private `selectedCopy` behavioral atom and never call
`source.size()` or run a second predicate pass. O(n) visit, O(k) allocated
result references, optional O(k log k) sort. This is an explicitly **detached
snapshot**, not a live filtered Set/Map, a primitive mask, an O(1) array, a
zero-allocation algorithm or a synchronized snapshot.

No existing Collection/Set/SortedSet/NavigableSet/Map factories, signatures,
iterators, comparators, backed views, `toArray(T[])`, JDK public APIs,
serialization or JNI ABI are changed. Because `Collection.toArray` can
evaluate predicates and perform typed-array termination differently, the
single-pass algorithm is not silently installed behind that API.

## Recipe and donor provenance
Reuse the unchanged `com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe` in
the existing `m3/collections/wrappers/recipe` Maven module with the
target-specific YAML binding
`com.synexia.m3jdk.FilteredSnapshotFusionReceiver20261017`.
SHA-256-sealed two modified file preimages and two ABSENT test targets,
reviewed postimages, exact source test, Maven/JUnit hostile cases, unchanged
JNI C source and receiver category ledger are authored **before** installing
the four Java postimages. Fix recipe, rerun and reseal rather than patch
generated code.

Test-only native composition reuses the prior #988
`M3MapCursorNativeProof` JNI peer `m3_map_cursor_jni.c`: transfer detached
primitive snapshot indices in bounded `GetIntArrayRegion` chunks, no Java
comparator/predicate callbacks or cross-call retained heap addresses.
No new production JNI symbol or duplicated hashing kernel.

`CATEGORY_REVIEW.tsv` records JDK/Guava/Eclipse/Commons/Spring and
LeetCode/HackerRank/GeeksforGeeks problem-category decisions. No third-party
code is copied; OpenJDK source licenses and the isolated Apache-2.0 M3
module remain separate.

## Gates and constraints
Actual Maven/OpenRewrite SDK four-target LST generation and exact postimages,
fixed-point replay and missing/drift/occupied refusals; Java21 strict compile,
public JDK array null-terminator and original view regression, randomized
duplicate/null, stable sorted snapshot, one-pass callback evidence; existing
C17 JNI test with -Xcheck:jni/fatal UBSan. Full source-built JDK/jtreg/JCK,
GC/HotSpot, all providers, threading, serialization, whole reactor,
JMH and global MIndex/M3 family closure are **not implied** by this slice.
