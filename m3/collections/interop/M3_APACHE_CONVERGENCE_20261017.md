<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# Five-family collection naming and M3 array/offset/mask backing — 2026-10-17

## Authority and scope

This is an **additive draft receiving candidate** under the existing `m3/collections/interop` independent optional module. Synexia is the canonical private convergence and Maven/OpenRewrite recipe authority; M3JDK21 is the public product receiver. The public JDK `java.util.*`/`java.util.concurrent.*` contracts and the nominal Guava, Eclipse Collections, Spring and Apache Commons Collections interfaces retain their upstream qualified names and behavior. An M3 array-backed implementation is a *delegate/adapter*, not permission to rename upstream packages or claim concrete class, serialization or performance equivalence.

`m3/collections` is not currently an installed `java.base` backend, and target receiving order is still **String -> arrays -> collections -> AST/compiler**. Neither this document nor source presence advances that phase. No vendor library source was copied; Apache Commons Collections 4.5.0 is an **optional runtime dependency** of the isolated interop artifact. `java.base` remains free of third-party collection dependencies.

## Existing owners reused

The existing `M3LinkedHashMap`, `M3LinkedHashSet`, `M3LinkedList`, `M3TreeMap`, `M3TreeSet` own reference payloads and their primitive slot, index and order lanes. They are already used by `M3LibraryCollections` for JDK/Guava/Eclipse/Spring contracts. The Apache Commons addition creates one decorator per requested facade and shares *no* hidden global payload owner.

`M3IndexPathMasks` owns explicit `int[]` encounter row IDs, logical-to-physical offsets, 32-bit required/forbidden mask planes, deterministic signed-stride and multi-stage selection. `M3StateMaskArrays` owns byte state updating, aligned slot-mask compaction and rotation; `M3ObjectStateSlots28` owns generation-checked slot masks. Object-level predicates/comparators never transit the optional existing `M3IndexMaskCompactor`/JNI primitive array bridge. The operation result may materialize a detached array where the caller's contract requires it; such copies are not falsely described as zero-cost.

## Semantics: do not conflate filtering with predication

Apache Commons `PredicatedList`, `PredicatedSet`, `PredicatedSortedSet`, `PredicatedMap` and `PredicatedSortedMap` are **write-validating** decorators. They reject nonconforming admission/mutation but do not automatically hide already stored members. Guava and M3 live `filter` wrappers are **visibility-filtering** views, with distinct mutation and predicate semantics. Eclipse adapters expose their own nominal rich mutable APIs. Spring `MultiValueMap` keeps list cardinality and empty-vs-absent policy. The Java collection names remain standard Java names.

The five new Apache nominal factories are independent and additive to `M3LibraryCollections`; they do not replace or modify its eighteen existing factories or eleven collectors. The selected source tests include comparator/range, null-predicate admission, direct and iterator writes, map-entry validation and independent backing. They are *candidate test fixtures*, not proof for all Commons APIs. Other Commons APIs such as `Bag`, `BidiMap`, `MultiValuedMap`, fixed-sized, synchronized, transformed and unmodifiable decorators require separate owner/contract proofs; no universal coverage is claimed.

## Recipe-first packet and proof gates

One Synexia-owned named Maven/OpenRewrite composition, `com.synexia.m3.Jdk21ApachePredicatedInterop20261017`, supplies two Java LST templates plus exact target POM/third-party-notice/text receipts. Each target is pinned to `ABSENT` or its exact preimage SHA-256; source changes not in the manifest must refuse. The receiver PR contains only these target postimages, a linkage to the Synexia recipe PR, a per-library mapping, catalogue and proof status. The existing Maven reactor and `m3/collections/interop/recipe` are not replaced.

Verified locally **only**: Java 21 `javac --release 21 -Xlint:all -Werror` and six executable tests using isolated **test-only stubs** of Apache and M3 APIs; the first fixture compile's missing serial IDs was repaired and rerun. These results do NOT qualify Apache Commons 4.5.0 binary/API integration, the real M3 backends, Maven, OpenRewrite SDK execution, jtreg/JDK-image delivery, native runtime/JNI or performance. No new native symbol or native source is added. The existing C17 JNI mask owner remains separate.

Required pending: Maven/OpenRewrite fixed-point and drift rejection, real Apache 4.5.0 + Guava/Eclipse/Spring coexistence against actual interop module, full Java21 JUnit, `m3/collections/interop/recipe` tests, serialization/range/iterator edge cases, JDK contract/API diff, optional native parity only for actual native changes, artifact packaging, installed-image proof, benchmarks and CI. Preserve draft / HOLD until these execute.

## Catalogue and mapping

`VENDOR_CONTRACT_MAPPING_20261017.tsv` is the per-family name/contract/backing/status receiver ledger. `APACHE_DONOR_CATALOGUE_20261017.tsv` records algorithm categories, complexity and donor selection/rejection. LeetCode, HackerRank and GeeksforGeeks inform adversarial workload shapes only, not permission to copy solutions. No measured speedup, universal superiority or semantic equivalence is asserted.
