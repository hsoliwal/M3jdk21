<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Native M3 counted-bag order-statistics index (2026-10-18)

**Not an adapter.** Product owner is `hsoliwal/M3JDK21`, `m3/collections`.
This is a new, source-sealed M3 collection capability reusing the existing
first-party `M3CountedBag.CountSnapshot` and `M3IndexMaskCompactor`;
it does not wrap Guava, Eclipse, Spring or Apache types or load their binaries.

## Existing-owner inventory and implementation

`M3CountedBag` already maintains equality-keyed occurrence counts using the
M3 parallel-array hash/map owner, and `sortedCounts(comparator)` already
produces an immutable snapshot of distinct references/counts in stable order.
Reimplementing that mutable storage would violate source-owner convergence.

The newly generated `M3BagRankIndex<E>` shares that immutable snapshot and
adds only a primitive `int[]` permutation of distinct physical ordinals and a
`long[]` exclusive prefix count index. Viewports are half-open offsets
`[from,to)`; `slice` is O(1) shared metadata. Stable comparator ties
retain separate equality groups. `occurrenceAt(long)`,
`occurrencesBefore`, `occurrencesThrough`, lower/upper bounds and exact
comparator group counts use O(log distinct) binary search. Source snapshot
is detached, so modifying the original M3 bag cannot invalidate the view.
Reverse permutes ordinals only, never the GC-traced key references.
No per-occurrence Entry/Node allocation, duplicate payload store or
unsafe retained Java-object address is introduced.

The `filtered(Predicate)` method invokes arbitrary Java code at most once
per visible distinct group. `selected(byte[] states, required, forbidden,
Backend)` handles full unsigned-byte flags and disjoint required/forbidden
mask constraints; the explicit `NATIVE_REQUIRED` backend calls the already
existing unchanged `M3IndexMaskCompactor.select(int[],byte[],int,Backend)`
C17/JNI kernel with detached precomputed primitive index/flag arrays only.
No new JNI ABI, no Java callback/Comparator in native code, and no
silent fallback. Every JDK Collection mutator, including no-op bulk
methods and iterator removal, refuses.

## Mandatory recipe-first custody

Canonical source-producing recipe:
`com.synexia.rewrite.M3NativeBagRankIndex20261018`,
extending the existing Maven/OpenRewrite
`M3HashPinnedJavaSnapshotRecipe` with three ABSENT-before source
templates (production, standalone test and JUnit adapter) and SHA-256
postimage seals. The generator's real `Recipe.run` SDK JUnit tests
assert byte-exact emitted output, 16 partial source permutations,
second-pass fixed point, missing/drifted file and duplicate ownership
refusals. Fix the recipes and re-run; never patch generated files.

Run after obtaining the actual Maven/Java 21 environment:
```sh
mvn -B -ntp -f synexia-openrewrite-recipes/pom.xml \
  -Dtest=M3NativeBagRankIndex20261018RecipeTest test
```

## Executed local proof and HOLD

OpenJDK 21 strict selected-source compilation
`javac --release 21 -proc:none -Xlint:all,-unchecked -Werror`
and `M3BagRankIndexSelfTest` were run in both normal and interpreted
(`-Xint`) JVMs: **213,230 assertions PASS each**, across 1,200 randomized
scenarios, comparator ties, nulls, high-bit masks, rank/select, stable
slices, saturation and exact long cardinality, strict immutable writes.
**Test-only API-shaped stand-ins** were used for `M3CountedBag` and
`M3IndexMaskCompactor` in that local run. This is NOT actual-target
owner, JNI, Maven SDK, or installed JDK qualification.

The separately reviewable M3JDK21 receiver must run actual owners,
Maven/JUnit, checked real C17 JNI with UBSan and `--native` parity
at the exact GitHub head. Current recipe and receiver tests/CI are
authored only until the completed runs can be inspected. Full provider
superset completion, JVM image promotion, GC, concurrency,
serializability, memory pressure, retained-heap, CPU and Windows/ARM
native/platform qualification remain separate gates.

All algorithms were authored against existing first-party M3
owners. LeetCode/HackerRank/GeeksforGeeks are taxonomy references,
not copied solutions or implementation bodies; Apache-2.0
copyright/attribution of new first-party expressions is retained.
