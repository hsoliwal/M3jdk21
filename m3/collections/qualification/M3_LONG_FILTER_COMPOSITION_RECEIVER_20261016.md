# M3JDK21 primitive-long filtered view receiving slice

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.

## Canonical ownership and source pin
- Synexia recipe/source owner: [PR #11903](https://github.com/hsoliwal/com.synexia/pull/11903).
- Existing target runtime owner: `m3/collections/src/main/java/com/m3/collections/M3LongFilteredView.java` (not `java.util`).
- Existing original Git blob `f198006c86e7c8c3b7f6fceb8b82b114e3cdce39`; no duplicated collection hierarchy.
- Generated postimage SHA-256 `cf9f15e9712b169768be2611a9eae98d317d47b07b7494b168bf81988e678341`, test SHA-256 `3cefdce84f9f78cbd77a8ac42626b6b54a15930dcc4becb0802c87491f725044`.
- Canonical class-backed recipe: `com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe`, named `com.synexia.m3.Jdk21LongFilterCompose20261016`.

## Contract and ownership
The incumbent read-only `M3LongFilteredView` implements its incumbent `M3LongCollection` contract (all previous methods/descriptors remain). It refuses `clear()`; nested filters preserve left-to-right evaluation while retaining the original backing cursor. The primitive `longSpliterator()` is late-binding, unsized and unsplittable; it does not falsely advertise SORTED or DISTINCT for a generic collection. `toArray` grows primitive storage on actual matches, not by first scanning `source.size()`.

Keep `M3LongSortedViews` for sorted/range mutations and `M3LongCompositeViews` for sorted k-way union. Arbitrary Java predicates remain on Java's side of JNI. No native ABI, Java/HotSpot or OS intern pool changes and no per-value entry objects.

## Current evidence and HOLD
Actual original preimage hash reproduced; baseline eager-`size()` failure witnessed. Java 21 `javac --release 21 -Xlint:all -Werror` and standalone execution of the exact target Java test source passed **2,029 assertions**, including 500 randomized rounds. The standalone compilation used temporary stubs for unchanged primitive interfaces and the JUnit annotation because Maven was unavailable, so this is not a target-module acceptance. OpenRewrite source seal/fixed-point/drift refusal is authored in the paired Synexia JUnit suite; its real Maven execution is **NOT_RUN**.

**Pending:** actual Synexia Maven/OpenRewrite SDK execution; target `mvn -B -ntp -f m3/collections/pom.xml -Dtest=M3LongFilteredViewContractTest test`; module/reactor CI; matched JDK image jtreg and HotSpot/GC/JNI checks where applicable; memory/cold/warm benchmarks and Windows/macOS provider compatibility. No full Eclipse/Guava/Apache/Spring/JDK superset or phase promotion claimed.

Donor catalogue: paired Synexia recipe crate `ALGORITHM_CATALOG.tsv`. License: independently scoped M3 collection source Apache-2.0, not a GPL/OpenJDK source-copy import. Maintain original target rights, no default-branch mutation.
