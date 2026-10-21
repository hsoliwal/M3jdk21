<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
# M3 collection JNI array storage contract

Policy ID: M3-COLLECTION-JNI-ARRAY-STORAGE-1. Recorded 2026-10-21.

This records the user's existing design and supplied-code direction. It is a continuation requirement for fleet agents, not a new design proposal. Read it before implementing or optimizing any collection. Reuse the already shared custom JNI array code and existing M3 owners; do not ask the user to provide them again or silently replace their design.

## Required representation

- Authoritative collection storage is dynamic or segmented arrays: payload/reference lanes and primitive lanes for offsets, physical slots, indexes, links/order, counts, generations and state/capability masks.
- Ordering and other behavior are represented in primitive arrays and flags. A linked, tree-shaped, ordered or concurrent API does not authorize a retained Node, Entry, Bucket, Cell or link-wrapper object per logical record.
- Create collection backing arrays through the already shared custom JNI array-creation owners for their types, and reuse the existing native operations. Preserve their type, offset/range, mask, ABI, exception, ownership and GC rules. Connect gaps through the existing recipe/owner; do not invent a competing allocator, JNI framework or storage hierarchy. A Java-only backing allocation or a JNI selector alone is not completion of this requirement; keep each unconnected backing-array path explicitly pending.
- Grow, segment, compact, page and swap through the established M3 array/index and MIndex residency/swap owners. Retain one authoritative payload store. Pressure may evict optional derived state or page canonical contents through its established durable owner; it must not lose data or change behavior.
- Reference payload arrays may contain caller values. The prohibition concerns collection-owned structural objects per record; it does not prohibit caller values, the collection owner, or necessary API views.

## Entry objects exist only at the API boundary

Create an Entry projection only when an actual API requires it. Reuse/manage API projections through the existing M3LFUCache mechanism, with bounded retention and the original API's observable ownership, mutability, identity and liveness rules.

An API projection is never the authoritative store or a substitute for array lanes. Internal traversal, filtering, sorting and bookkeeping operate on arrays/offsets/masks directly. Cached objects must not be retargeted while an escaped caller still observes them. Equal hashes do not establish equal reference identity or compatible API roles. Live asMap/count entries must continue to follow the donor's remove/clear/reinsert behavior. If a path has not proved these rules, record the gap; do not call it fully implemented.

This explicit user requirement supersedes the older blanket instruction to always allocate fresh caller-visible Entry objects and never cache them. Preserve the array-only structural invariant and prove donor-compatible caching rather than reinstating that blanket rule.

## Existing code to reuse

These are source references, not a claim that every collection or array kind is already integrated.

| Existing owner | Source and role |
| --- | --- |
| M3ArrayNative | [Java bridge](https://github.com/hsoliwal/M3jdk21/blob/2779aba3f98f31c324be43f731be7914e1a05935/m3/arrays/src/main/java/com/m3/arrays/M3ArrayNative.java), [custom JNI code](https://github.com/hsoliwal/M3jdk21/blob/2779aba3f98f31c324be43f731be7914e1a05935/m3/arrays/src/main/native/m3_arrays_jni.c). nativeConcatenateUtf16 creates a Java char array with NewCharArray and fills it through bounded region copies; nativeCompareUtf16 works over checked array regions. |
| M3IndexMaskCompactor | [Java owner](https://github.com/hsoliwal/M3jdk21/blob/2779aba3f98f31c324be43f731be7914e1a05935/m3/collections/src/main/java/com/m3/collections/M3IndexMaskCompactor.java), [JNI owner](https://github.com/hsoliwal/M3jdk21/blob/2779aba3f98f31c324be43f731be7914e1a05935/m3/collections/src/main/native/collections/m3_index_mask_compactor.c). Existing select/range/positions/window paths create detached int[] results with NewIntArray; long-window selection uses NewLongArray. Preserve checked offsets, state masks and explicit native-required behavior. |
| Canonical MIndexNativeArrayFlatten | [Java owner](https://github.com/hsoliwal/com.synexia/blob/66684406873953ee7ffa8ba9cd5058338fce10d7/synexia-indexstring/src/main/java/com/synexia/indexstring/nativebridge/MIndexNativeArrayFlatten.java), [JNI code](https://github.com/hsoliwal/com.synexia/blob/66684406873953ee7ffa8ba9cd5058338fce10d7/synexia-indexstring/native/src/mindex_array_flatten_jni.c). Existing canonical array flatten/mask owner; preserve its ABI and pending-exception rules. |
| Existing LFU owner | [M3LFUCache](https://github.com/hsoliwal/M3jdk21/blob/6ae20943e2295e6120c9666a04dc1f2be29f4873/m3/collections/src/main/java/com/m3/collections/M3LFUCache.java). Reuse the actual primitive-array cache mechanism for contract-qualified API projections. |

JNI-created Java arrays remain Java-managed objects. Preserve reference/GC barriers and local/global-reference lifetimes. Never retain a movable Java-heap address beyond its valid JNI access scope. A direct-buffer plan is a separate existing owner and is not evidence that Java heap-array allocation, Object[] covariance or GC behavior is qualified. The references above establish concrete char/int/long paths; each additional array kind and collection integration still needs its exact owner and execution evidence.

The earlier [PR #1325](https://github.com/hsoliwal/M3jdk21/pull/1325) is an upstream package/source intake reference. Copied JDK Node/Entry implementations are donors to transform; they are not the final M3 representation.

## Serial implementation and evidence

Continue the full superset of JDK 21, Eclipse Collections, Spring collection utilities, Apache Commons Collections/Lang, Guava and fastutil. Preserve each provider's public API and distinct semantics. Follow [FAMILY_CATALOG.tsv](https://github.com/hsoliwal/M3jdk21/blob/2779aba3f98f31c324be43f731be7914e1a05935/m3/collections/qualification/m3-sdk-families-20261017/FAMILY_CATALOG.tsv) one production owner/file at a time.

Inventory existing code and licensed donors before generating a gap. Bind the reuse decision, source revision/path/hash and license. Reuse and qualify the canonical Synexia Maven/OpenRewrite recipe; fix the recipe and regenerate its output. Keep transformations, contracts, failures, source seals and target proof together. Preserve String -> arrays -> collections receiving order and product admission gates; preparation may continue without claiming phase advancement.

Prove unchanged public/protected descriptors and donor behavior, array storage, JNI allocation/overflow/failure paths, mutation and escaped views, serialization, applicable concurrency and actual target runtime packaging. Report Java and JNI proof separately. Finite tests, a source copy, a draft PR or a catalogue row do not prove the whole superset.

Use LeetCode/HackerRank problem categories and licensed GitHub donor algorithms to propose improvements. Measure on frozen workloads, retain rejected candidates and report CPU, allocation and native/process-memory tradeoffs. Do not claim absolute peak performance or online ranking without evidence.

## Fleet handoff

Every delegated task must carry this policy ID, the current owner/file and catalogue row, exact reused Java/JNI code references, the recipe and source seals, current proof/gaps and the next permitted action. Re-read the current owners rather than relying on chat recollection.

Keep storage protection active and preserve all sources, proofs, attempts and unrelated work. This document changes no production code and grants no storage exception, release/merge approval or unproved coverage.
