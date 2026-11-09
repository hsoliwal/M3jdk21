<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3Scale five-provider per-file collection census — 2026-10-18

## Scope and truth
Exactly **3,214 Java Git blobs** were enumerated from **12 non-truncated, immutable Git subtrees** of Guava, Eclipse Collections, Apache Commons Collections, upstream Spring, and the M3JDK21 `java.util` tree. This is a **reproducible file inventory with an unproved semantic classification**, **not 3,214 implemented M3 refactorings**. It must never be used to claim all five providers have been ported or that any donor class can be replaced by an M3 analog.

| Provider | Java entries | Root and legal boundary |
|---|---:|---|
| Guava | 450 | original Apache-2.0 source + 234 donor tests |
| Eclipse Collections | 1516 | API / impl / tests / code generator; EPL-1.0 + EDL-1.0 file-specific source review |
| Apache Commons Collections | 678 | source + tests; Apache-2.0 / ASF NOTICE |
| Spring | 192 | pinned upstream Spring Core util + tests, **READ-ONLY**, no writable hsoliwal spring-framework fork discovered |
| JDK | 378 | pinned `java.base/java/util` subtree, including non-collection files classified as typed exclusions; **NEVER rewrite java.base package declarations** |

## Reuse, transformation and consolidation law
1. Read entire file, Javadoc and interfaces and freeze complete externally observable ContractEnvelope (types, mutation, equality, order, iterators, serialization, concurrency, reflection/resources, JNI).
2. Compare against the existing `com.m3.collections` M3 dynamic-array/index/offset/mask backend. Choose the incumbent rather than creating an alternate collection hierarchy.
3. Evaluate candidate algorithms by problem category (LeetCode/HackerRank/GeeksforGeeks reference only) and preserve original donor and license provenance.
4. Use existing canonical Maven/OpenRewrite recipes `RelocateGuavaFork20261018`, `RelocateEclipseFork20261018`, `RelocateApacheCommonsFork20261018` on **scratch copies only**. Spring has no authorized writable fork, and JDK may only use isolated copied `java.util` sources, never actual `src/java.base`.
5. For each replaceable semantic leaf, create a **separately qualified source-hash-sealed OpenRewrite M3 atom transformation**, first compile and compare original behavior, then apply in its fork and rerun. Fix recipe, not postimage.
6. Prove diff → lint → compile → donor tests → runtime/JNI where applicable. After valid proof, serialize M3JDK21 product handoff and mark exact file status; no parallel promotion.

## Known boundaries
- Git blob SHA-1 is source identity at immutable tree root, **not** a byte-level semantic equivalence proof or SHA-256 transformation receipt.
- Path/name classifier categories and `m3_backend_candidate` are heuristic candidate suggestions, not contracts or verified backend relationships.
- For Spring and JDK, source locations contain noncollection files: these get `TYPED_EXCLUSION_NON_COLLECTION` rows rather than silently omitted.
- Rooted inventory is an **explicit finite subset** of broader five-library source trees. Other variants (GWT, Android, other Spring modules, generated files and additional JDK modules) need additional pinned root inventories.
- Existing relocated specimens and the canonical relocation recipe must be reused, not duplicated or called complete based on PR merges.

## Exact evidence
Immutable Git-tree APIs returned **truncated=false** for all 12 subtrees, and each root's Java file count was recorded. Product Java/native files were **not changed by this census**; no Maven (unavailable locally), original-provider reactor, OpenJDK image, JNI parity or benchmark gate was executed for these 3214 files.
Canon source SHA-256: `5ec31d51207bda02d2f1f9a7fcd940f2ae5e1bfc9f0ef9358139fe62b47f393b`. Expanded witness SHA-256: `3d227d1872bbf80802f6bb00e233b9e912447efcd3fe899e0668cd68f74bc341`. Coding-agent witness SHA-256: `c73d6f321717813ea5d95a970ab338dc3c6731020c8725fa87182de187814fdf`.

All five inventory TSVs are SHA-pinned as immutable Git objects in the canonical Synexia branch. The accompanying standalone Java 21 verifier validates the file-count, path, role, SHA and unique-owner admission rules, while hosted Maven and source materialization remain separately required.