<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# Five-provider M3 Collections SDK names and indexed multivalue substrate

Canonical convergence and Maven recipe owner: `hsoliwal/com.synexia`, append-only `develop@892e3f6c51d78bc0948e8a8cf736055b4c70eafa`. The public receiver is the **existing** `hsoliwal/M3jdk21/m3/collections` independent Apache-2.0 module. These are original Synexia-authored first-party files and independently implemented array/offset/mask mechanics; no third-party source copied.

## Named APIs are not interchangeable

The source-sealed resource `M3_SDK_FAMILY_CATALOG.tsv` describes **104 distinct fully qualified upstream collection names** (JDK 41, Guava 23, Eclipse Collections 17, Apache Commons 17, Spring 6), their provider, existing M3 storage owner if present, candidate M3 name, concrete semantic contract category and one of `JDK_RETAINED`, `M3_BACKEND_ONLY`, `M3_VIEW_ONLY`, `PENDING`.

These dispositions cannot be interpreted as API/ABI compatibility. `org.springframework.util.MultiValueMap` (a Map-of-Lists contract), `com.google.common.collect.Multimap`, `org.eclipse.collections.api.multimap.list.MutableListMultimap`, and `org.apache.commons.collections4.MultiValuedMap` remain distinct source-level interfaces and licenses. Vendor types are not silently renamed, shadowed, or copied into M3's runtime package. No exact compatibility adapter is claimed until each original provider's contract tests, null/equality/callback/ownership/JMM and ABI gates pass.

## Runtime increment

`com.m3.collections.M3CollectionSdkNames` loads and validates the sorted resource at SDK runtime and gives fail-closed lookups by upstream FQCN and provider, with an explicit `vendorContractProven() == false` barrier.

`com.m3.collections.M3IndexedMultiValueMap<K,V>` is one **independent M3-owned** dynamic-array backend. It reuses the incumbent zero-node `M3LinkedHashMap<K,Integer>` for distinct group indexing and owns `Object[]` values plus `int[]` next offsets and `byte[]` per-occurrence state flags. Its group heads/tails/counts are primitive parallel arrays; removals clear references and recycle group/value offsets. It permits duplicate and null values plus null keys; counted `size()` is flattened occurrences, not distinct keys. It intentionally is NOT a `java.util.Map`, Spring `MultiValueMap`, Guava `Multimap`, Apache `MultiValuedMap` or Eclipse `MutableListMultimap`.

Detached `ValuesSnapshot` copies references once, then filters once into integer ordinal offsets, sorts stably by an indirect primitive merge sort and creates O(1) half-open index slices. `selected(...,required,forbidden,backend)` snapshots the existing state flags and delegates **only** primitive selection to the existing `M3IndexMaskCompactor` Java/optional JNI engine; no new native ABI, no Java predicate or reference crosses JNI, no retained JVM pointer. JNI required means no silent fallback.

## Recipe and proof boundaries

The Maven-bound additive recipe `com.synexia.m3.M3SdkNamesRecipe` owns **four** exact SHA-256 postimage templates, tests itself on missing/drift refusal, two applies and fixed point, preflights all paths and symlinks, then installs **only absent** receiver files. No existing target java.util / HotSpot / library source is hand-rewritten; future modifications of existing Java ASTs must use a qualified OpenRewrite visitor. Recipe authoring source and generated target files have separate license/provenance receipts.

Selected-source local Java21 `javac --release 21 -proc:none -Xlint:all -Werror`: PASS **42,897** deterministic assertions. Same oracle with the prior pinned existing C17 index-mask kernel, UBSan and `-Xcheck:jni`: PASS **42,897**. The **local Java key index was a behaviorally compatible JDK-backed stand-in**, not the actual `M3LinkedHashMap` source; the local mask source was a prior pinned original kernel, not the latest M3JDK21 `master` revision. Consequently real target class/JNI parity, Maven verification (not installed locally), full owning module, jtreg/HotSpot, memory and throughput are **PENDING** and must run on GitHub exact target head. Counts are assertion totals, not benchmark speedup.

The full five-library SDK superset and JDK substitution are **NOT PROVEN**. Preserve existing String -> arrays -> collections -> AST/compiler admission order; no merging without green required gates.
