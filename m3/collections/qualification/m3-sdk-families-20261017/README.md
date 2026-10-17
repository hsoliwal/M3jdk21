<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# M3 Collections SDK: provider names and correctly scoped M3 backends

## Authority and exact scope

Canonical Maven-based generator: `hsoliwal/com.synexia`, `synexia-m3-recipe/recipes/m3-collection-sdk-names-20261017/`. Public receiver: `hsoliwal/M3jdk21`, separately built `m3/collections` Apache-2.0 module. JDK public APIs continue to be named `java.util.*` / `java.util.concurrent.*`; Guava `com.google.common.collect.*`, Eclipse `org.eclipse.collections.api.*`, Spring `org.springframework.util.*` and Apache `org.apache.commons.collections4.*` never get incorrectly renamed into public `M3*` types. Provider licenses remain upstream; only first-party original catalogue, source, recipe and tests are under Apache-2.0.

`FAMILY_CATALOG.tsv` is the primary, sorted, source-sealed definition of **107 family API names**. The audited existing optional library adapter module `m3/collections/interop` is represented explicitly: `M3LibraryCollections` / `M3SpringMultiValueMap` are existing *optional* types, not silently marked absent or implicitly installed in the core SDK. The generated runtime `M3CollectionSdkCatalog` stores these in compact parallel arrays and supports O(log n) exact fully-qualified lookup and vendor queries. `existing_m3_owner` identifies a present proving-ground M3 type or an already-existing **optional interop adapter**, never unqualified broad provider compatibility; `proposed_m3_role_NOT_IMPLEMENTED` is an unimplemented capability name, never an installed Java type. `JDK_RETAINED` means the original JDK owns the contract; `OPTIONAL_ADAPTER` identifies specific existing `m3/collections/interop` factories with separate third-party dependencies and finite contract tests; and `PENDING` means no admitted owner in this mapped target scope. An optional adapter is not full concrete-provider/serialization compatibility. No blanket promise of compatibility, universal speed, or entire SDK family delivery.

The additional `M3CollectionSdk` factory **reuses** existing `M3FilteredLists`, `M3FilteredViews`, `M3LongIndexProjection`, and `M3LongBitmapFilteredSnapshot` without any new payload hierarchy. Generic List/Collection/Set/NavigableSet/NavigableMap wrappers retain their existing owner, comparator, range, mutation, and live/snapshot semantics. Long snapshot predicates evaluate once; cached bitmap/index structure is reused on future reads, stable sorting, and slices. Optional JNI dispatch is delegated to existing `M3IndexMaskCompactor` only; no new C code, no retained Java pointers, no native callbacks. No Synexia runtime dependency.

## Distinct semantics: absolutely no name collisions

- Guava `Multimap<K,V>` tracks mappings/pairs. `ListMultimap` allows duplicate pairs; `SetMultimap` does not.
- Spring `MultiValueMap<K,V>` **extends `Map<K,List<V>>`** and has `getFirst`, `add`, `set` and `toSingleValueMap`; not the Guava contract.
- Apache Commons `MultiValuedMap<K,V>` and `MultiSet<E>` are independently named/contracted; `Bag`/`MultiMap` are legacy deprecated family names in newer Commons Collections.
- Eclipse Collections `MutableList`, `MutableBag`, `MutableLongList`, and `MutableMultimap` retain their rich provider interfaces, not a JDK-only substitute.
- The JDK Java 21 `SequencedCollection`, `SequencedSet`, and `SequencedMap` remain public JDK 21 APIs.

## Serial recipe sequence and proof

1. Maintain catalog/creator canonical data and SHA-256 inputs in this Synexia crate. `M3CollectionSdkNamesRecipe` validates exact schema, sorted FQCNs, vendor/status correctness, no upstream name alias collisions, fixed 104-entry corpus, no unsafe template insertion, all source input hashes and exact output hashes.
2. Generate only four previously absent Java product/test files into a clean M3JDK21 receiver. All-target preflight rejects path traversal, unexpected existing source, drift, symlinks and missing templates before any writes. Correct mistakes in the **generator/template and rerun**, not generated Java.
3. Verify from the target via Maven or the identical Java generator; run selected Java21 `javac -Xlint:all -Werror` and real existing mask JNI under `-Xcheck:jni`. Use separate full module build, JDK/jtreg, native-platform, A/B benchmark and memory gates before promotion.
4. Open one reviewable draft canonical recipe PR and one target receiver PR (recipe first, generated source second); keep base `develop` append-only and never rebase it.

**Local actually performed:** Java21 generator compilation, `--selftest` missing/drift/refusal/fixed-point, `--apply` + `--check` against a disposable repository, exact SHA-256 emitted bytes for all 4 files, and strict Java21 standalone catalogue test **521 checks, 107 exact names**. Local Maven is unavailable. Full target SDK facade integration, Maven, selected real JNI, full JDK, class compatibility and benchmarks require hosted CI; they are not claimed locally.

## Source and donor evidence

Sources for API identities: JDK 21 Oracle `Sequenced*` docs; Guava official `com.google.common.collect` Javadocs; Eclipse Collections official features/API docs; Spring `MultiValueMap` Javadocs; Apache Commons Collections 4.6.0 API docs. No third-party implementation, LeetCode/HackerRank/GeeksforGeeks submission, or copyrighted donor expression was copied. Problem references and alternatives are documented separately in `CATEGORY_REVIEW.tsv`.

This is a **bounded SDK naming and existing-wrapper receiver**, not full Guava/Eclipse/Spring/Apache/JDK substitutability and not a promotion of the String → arrays → collections → AST/compiler target acceptance order. PENDING rows remain PENDING until exact appropriate interfaces/dependencies/builds/contracts are proven.

## v2 source-sealed reconciliation of the current optional SDK

`m3/collections/interop/src/main/java/com/m3/collections/interop/M3LibraryCollections.java` Git blob `35d419cc78f4fe583ea97cab3026d1dbf037f795` already contains selected Guava ListMultimap, SetMultimap, SortedSetMultimap and Table adapters, Eclipse MutableList/Map/Set/Sorted variants, and Spring AutoPopulatingList. `M3SpringMultiValueMap.java` blob `baf64557424e17a2650d44f697f6dd6253edb55a` is the existing map-of-lists owner. This catalogue records 12 named optional interfaces/classes as `OPTIONAL_ADAPTER`; it does **not** imply full provider coverage, no third-party dependencies, or installed-JDK status.

The first catalog generated 104 names. An exact-source **versioned recipe migration** admits precisely the earlier catalog and catalog-oracle SHA-256 postimages, adds the two `before/` template seals, then regenerates the 107-name metadata and corrected oracle without editing output source manually. The upgrade is tested against actual prior template bytes (not an invented surrogate). Everything else retains identical Git blob identity. The prior PR commits stay in history.
