# Five library M3 SDK: typed actual-owner factory convergence (18 Oct 2026)

**Product** `hsoliwal/M3jdk21` (five-family `m3/collections` optional interop). **Canonical recipe owner** `hsoliwal/com.synexia`. Canonical and target ancestors are stacked on existing five-provider source-census PRs, preserving historical develop ancestry.

This is **actual Java source**: add five strongly typed factories to the existing `M3LibraryCollections` class, returning the correct *upstream* interfaces implemented by five incumbent M3 array/index/count owners:
- `java.util.List` -> `M3ArrayList` (dynamic Object[]).
- `com.google.common.collect.Multiset` -> `M3GuavaMultiset` (M3CountedBag).
- Eclipse Collections `MutableListMultimap` and `MutableSetMultimap` -> the two existing M3Eclipse owner classes.
- `org.apache.commons.collections4.MultiSet` -> existing M3ApacheMultiSet.
- The fifth provider, Spring `org.springframework.util.MultiValueMap`, was already exposed through the DIRECT `M3SpringArrayMultiValueMap` factory; its behavior is unchanged.

The work does NOT implement every method/class in the five external libraries. It does not claim aliases of all provider models are equal: Spring counts distinct keys; Guava/Apache list multimap counts occurrences; bags count repeated keys; Eclipse set multimap suppresses duplicates. Test `M3FiveLibraryInteropContractTest` is extended, not replaced, to compare actual named provider semantics and provenance.

**Existing C17 JNI** (`M3IndexPathMasks`, `M3IndexMaskCompactor`) remains the shared primitive state-mask boundary. No new native ABI, Java object pointers, side-effecting callback crossing or implicit Java/native backend switch. The JDK-facing factory adds no `java.util` class replacement.

**Source-sealed recipe:** Existing Maven-based OpenRewrite `M3HashPinnedJavaSnapshotRecipe` and `M3HashPinnedTextSnapshotRecipe`. All previous Java methods and test bodies remain unchanged, new methods appended after the old source terminator. Two exact Java source SHA-256 preimages; the original 34-file interop source census remains enumerated, with exact SHA-256 and Git blob updates for ONLY the two refined files. The existing fail-closed verifier is modified solely to pin the new census checksum. The original five historical source receipts remain immutable.

**CI blocker correction:** Hosted run [39046293766](https://github.com/hsoliwal/M3jdk21/actions/runs/39046293766) failed before SDK tests because `org.intellij.lang.annotations.Language` was absent on the compiler path under `-Werror`. The existing scoped Maven recipe `pom.xml` gains the donor's `org.jetbrains:annotations:24.1.0` **provided** dependency; warning suppression and blanket test exclusion are forbidden. Subsequent exact-head Maven and provider/JNI tests remain acceptance requirements.

Reproduction:
```sh
mvn -B -ntp -f synexia-openrewrite-recipes/pom.xml -Dtest=M3FiveRealSdkFactories20261018RecipeTest test
python3 m3/collections/interop/qualification/current-census-20261017/verify.py
mvn -B -ntp -f m3/collections/qualification/m3-five-provider-current-census-20261017/recipe/pom.xml test
mvn -B -ntp -f m3/collections/pom.xml install
mvn -B -ntp -f m3/collections/interop/pom.xml -Dtest=M3FiveLibraryInteropContractTest test
```

**Qualification boundary:** canonical Maven/OpenRewrite SDK run, target real-provider JUnit, checked JNI, original collection census and installed-M3JDK/jtreg/GC/JMM/CPU/heap acceptance must be observed, not inferred from source SHA-256. None was executed on the local authoring container (Maven unavailable); head CI must prove them. No universal throughput or memory-superiority claim.

SPDX-License-Identifier: Apache-2.0. Copyright 2026 Hitesh Soliwal and contributors. Third-party names remain in their original packages. No challenge/tutorial implementation copied; use pinned first-party M3 owners.
