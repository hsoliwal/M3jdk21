<!-- SPDX-License-Identifier: Apache-2.0 -->
# Integrated M3JDK21 Java/JNI algorithm convergence — inventory and gate ledger
**Date:** 2026-10-21. **Product:** hsoliwal/M3jdk21, *not* an M3 layer over stock OpenJDK.

## Invariant and execution model

Every recurring coding/LLM change must create or improve a reusable **Maven/OpenRewrite recipe** and qualify its contract before source receiving. Source inventory and a searchable problem-category/donor catalogue come first. Apply typed file-local atoms **serially per file**, with identical interface/ABI/observable behavior. Independent files may fan out horizontally; escalate FILE -> PACKAGE -> MODULE -> REACTOR -> PUBLIC API only when needed, and prove exact target contracts separately. Retain exact SHA-256 pre/postimages, donor commit pins, license decision, source/test receipts and a fixed-point replay; on source drift refuse, do not guess or hand-patch output. Synexia is the canonical recipe/donor convergence workspace, while Java/HotSpot/JNI implementation ownership stays **inside M3JDK21**.

## Canonical owner and review map

| Program category | Existing M3JDK21 owner and pending review | Contract boundary / next verified step |
| --- | --- | --- |
| String/UTF-8/UTF-16 and composite native ownership | `java.lang.String`, `M3String`, `M3StringPool`, HotSpot/JNI; [PR #1536](https://github.com/hsoliwal/M3jdk21/pull/1536) | Preserve ordinary String UTF-16, JNI modified UTF-8, intern and GC lifetime; source-built matched Java/HotSpot/native image and jtreg required |
| Byte/char immutable atoms and shared lexicon | Existing M3String native/lexicon owners in product | Zero-copy only when lifetime and aliasing are proven; no raw oop/nonnative byte array smuggling |
| Primitive array/offset/index/mask collections | `M3LongIndexProjection`, `M3IndexPathMasks`, `M3IndexMaskCompactor`, `M3LongAddressKernel`; this [PR #1667](https://github.com/hsoliwal/M3jdk21/pull/1667) and [PR #1654](https://github.com/hsoliwal/M3jdk21/pull/1654) | Stable encounter order, duplicate physical indices, signed strides and detached output; JAVA/NATIVE_REQUIRED parity |
| Backed filtered / sorted / navigable / read-only wrappers | `M3FilteredViews`, `M3MutableFilteredViews`, `M3FilteredLists`, `M3CollectionViews`, `M3LongSortedViews`; [PR #1619](https://github.com/hsoliwal/M3jdk21/pull/1619) | Live backing, predicate/comparator order, stored representative identity, range enforcement, iterator mutations and sequenced endpoints; no object entry storage in index internals |
| JVM object size, stable ID, reference diagnostics | `java.base/jdk.internal.misc.M3ObjectMemory` plus `libjava` JVMTI; [PR #1648](https://github.com/hsoliwal/M3jdk21/pull/1648) | Shallow bytes != retained graph/native footprint; GC-stable opaque tag != raw address; JVMTI `FollowReferences` snapshot != continuous exact reference count |
| Cooperative native leases and accounted native storage | Synexia [#12660](https://github.com/hsoliwal/com.synexia/pull/12660) remains a convergence candidate | Integrate into existing JDK native ownership only after proved allocator ownership; counted leases are not Java field/local/JNI-root aliases |
| Common JNI and Android platform kernels | Existing product platform/JNI work [#1572](https://github.com/hsoliwal/M3jdk21/pull/1572), [#1658](https://github.com/hsoliwal/M3jdk21/pull/1658) | Batch primitive spans; JNI call/array copy vs JIT measurements; Linux/Windows/macOS/Android ABI and NDK acceptance separately |
| M3File / incremental compiler / Maven reuse | Existing M3File owners and [#1661](https://github.com/hsoliwal/M3jdk21/pull/1661) | Hash exact source/class delta, avoid repeated parse/build; require invalidation on type/classpath change |
| AST and recipe/agent DAGs | Original M3AST/Atomizer/Patternizer/OpenRewrite recipe owners | Bounded regex signals after string/comment masking, AST/LST authority, original public surface preservation; compiler is oracle |
| Regex/search/precompute | Native M3String regex/token/search and product `java.util.regex` integration | Composite logical text and JDK Unicode behavior; prepared fast filters may reject only when safe, exact matcher must verify candidates |

**This table is a review/receiving map, not a claim these separate PRs are merged into master.** A passing isolated JVM patch test or recorded Git blob alone does not establish a full M3JDK21 native image.

## Catalogue-driven decisions

- **Algorithm taxonomy:** GitHub source/commit inventories are checked against license and contracts; LeetCode, HackerRank and GeeksforGeeks are used to discover problem categories and adversarial tests. Do not copy challenge editorial/submission code blindly or conflate a practice problem with a validated JDK algorithm.
- **Storage invariant:** dynamic arrays, primitive indexes/offsets, compact state masks, reusable owner-range descriptors; API-required `Map.Entry`/facade allocations are on demand, never retained per internal row.
- **Execution choice:** primitive bulk masks, array compaction, UTF kernels and native hashing may be JNI/native batch candidates. Java callbacks, arbitrary comparators, backed iterators and HotSpot object metadata should not cross JNI per element. HotSpot GC/JIT/barrier and object-address facilities belong to the integrated VM, not a userland adapter.
- **Comparison policy:** document time, transient allocations, memory pressure, effect ordering and fallback. A structural fast path is not a performance claim without before/after benchmark and contract-specific test.
- **Provenance:** source owners retain original notices, Apache-2.0 for Synexia-sourced non-JDK work and GPLv2+Classpath provisions for Java base, as applicable after file-level license review. Preserve correct donor links in each recipe/task ledger.

## Current pass

Canonical recipe [Synexia #12700](https://github.com/hsoliwal/com.synexia/pull/12700) generates exactly one private `M3LongIndexProjection` source refinement and two owned test files, verified by matching Git blob content. Scope is one file-local immutable-snapshot atom. Do not transform `java.util.Collection`, `java.util.SortedMap`, `java.lang.String` or JNI symbols as a side effect.

Gates: SHA-256 source seals and received Git blobs PASS; real Maven/OpenRewrite/replay, product focused compilation and full owning-module integration **pending CI receipts**; full HotSpot/JNI/Android/native release qualification **NOT_RUN for this pass**. Keep draft pending proof.
