<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 filtered List / signal sorting / signed JNI receiver

Candidate product receiver for canonical Synexia recipe
[hsoliwal/com.synexia#11990](https://github.com/hsoliwal/com.synexia/pull/11990),
exact recipe head `9b8f0f0efb9a198a1c0d513669760c68f4080f43`.

## Exact receiving scope

This branch stacks on M3JDK21 [#992](https://github.com/hsoliwal/M3jdk21/pull/992)
commit `11dde1eac33cea2a8b90baba8c155eabec341144`.
It does not replace existing `M3CollectionViews`, `M3FilteredMaps`,
primitive lane owners, `java.base`, HotSpot or public JDK contracts.
The 9 Java and 1 C17 postimages are **ABSENT-before**, SHA-256 pinned,
and installed exactly as the canonical Synexia source templates.

New opt-in components:
- `M3FilteredLists.filtered/readOnlyFiltered`: live backed List with
  selected logical positions, safe bidirectional iterator access,
  explicit mutation/strict read-only contracts and physical subList.
  Reuses prior Synexia #11977; no retained per-element payload store.
- `M3SignalSortedViews.sortedBySignalSnapshot`: immutable reference
  snapshot, IEEE-754 Double.compare order, stable ties, primitive
  row coordinates, one signal evaluation per source member.
  Reuses MatIntOrder's ascending-sortable IEEE bits and primitive LSD radix.
- `M3IntFilterKernel.indicesGreaterOrEqual`: stable signed-int original
  row ordinal selection. Default Java; JNI optional via
  `-Dm3.collections.intfilter.jni=true` plus `libm3_int_filter`.
  Ported from Synexia #11982/#11985. Dense native selection does not
  allocate a redundant second full-length result. No native callback
  to arbitrary Java predicates or object-address retention.

## Mandatory recipe-first provenance

Existing `m3/collections/wrappers/recipe/pom.xml` consumes the retained
OpenRewrite LST snapshot provider; it remains the **only target recipe engine**.
Named source recipe: `com.synexia.m3.M3Jdk21FilteredMatReceiver20261017`.
Two SHA-256 manifests and the corresponding source templates live under
`../recipe/src/main/resources/com/synexia/rewrite/`; JAVA and C17 are separate
qualified atoms. Do not hand patch generated source; repair the canonical
Synexia recipe/templates and rerun then receive bytes to the target.

## Verification

`sh m3/collections/wrappers/filtered-mat-receiver-20261017/verify.sh`
performs manifest/template/postimage byte checks, Java21 strict compilation,
filtered-list contract and work oracles, 320 randomized IEEE differential
sort rounds, JNI compilation, native parity and dense allocation under
Serial/Parallel/G1/ZGC/Shenandoah and G1 interpreter mode.

Real OpenRewrite LST `Recipe.run`/fixed point/refusal test:

`mvn -B -ntp -f m3/collections/wrappers/recipe/pom.xml -Dtest=M3FilteredMatReceiverRecipeTest test`

Passing focused tests does **not** claim whole reactor, source-built modified
JDK, jtreg/JCK, JNI/MSVC portability, or superior total runtime/RSS.
Any missing Maven dependencies or CI failures keep this PR in DRAFT.

## Deferred dependencies (not silently imported)

- Synexia `M3WritableFilteredMaps` needs its stored-key representative
  algorithm reconciled with M3JDK21's existing `M3FilteredMaps` owner.
- Synexia indexed sorted multiset requires M3IndexSortedMultiset /
  M3IndexCountKernel ownership not present on this receiving stack.

No unsupported algorithm or unlicensed challenge-site solution is copied;
LeetCode, HackerRank and GeeksforGeeks are category/test taxonomy only.
