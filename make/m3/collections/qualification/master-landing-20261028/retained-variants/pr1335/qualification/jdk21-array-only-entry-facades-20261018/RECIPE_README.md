<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# Canonical Synexia JDK21 array-only Entry-facade recipe

**Source authority:** Synexia. **Runtime receiver:** hsoliwal/M3jdk21 PR #1325 successor.

M3JDK21 PR #1325 at source-receiving head 81172a891fa55bfd6d8c1fe04c89eefe92f5136b imports 59 JDK21 source files under private M3 package names with their original JDK GPL+Classpath headers. Those files still retain JDK Node/Entry allocation and 78 full-compile errors; they are SOURCE_ONLY. This canonical recipe **does not** falsely mark them runtime-compatible or relicense them.

The generated runtime M3LFUCache<T> contains only long[] handles, int[] hash buckets/indexes/frequencies/free lists, byte[] state masks, long[] recency and one Object[] for **optional API-created facade references**. The LFU cache is bounded, capacity-adaptive and evictable; it is never the authoritative mapping store. Expected O(1) cached lookup; O(capacity) deterministic LFU/LRU-tie eviction; clear/invalidate/compact wipe stale references.

M3ApiEntryFacades<K,V> is opt-in: immutable detached Map.Entry is created only when explicitly requested. Matching handle+key/value identity can reuse it; a held immutable facade is never repointed, and recycled physical slots need generation-bearing handles. Mutable Map.Entry.setValue, live iterator entries, fail-fast and concurrent contracts remain incumbent-owner responsibilities, not silently replaced by cached immutable facades. No new scalar JNI boundary: existing M3IndexMaskCompactor Java/C17 handles bulk primitive indexes/masks, not Java objects.

This Maven/OpenRewrite recipe **reuses** the existing first-party Synexia M3GpuToolJoinRecipe and M3GpuToolJoinManifest byte-identically; three ABSENT outputs are SHA-256 sealed. Four SDK JUnit methods cover emitted bytes, fixed point, drift refusal and named-recipe discovery. Generated source bytes must be corrected in canonical templates, never handwritten product edits.

Local strict javac --release 21 -proc:none -Xlint:all -Werror PASS; original real-source Java oracle PASS **2,050,202 checks** (8x4500 randomized differential operations plus reflection and API facade contracts). Maven/OpenRewrite SDK was unavailable locally and remains HOSTED_PENDING. No benchmark, 59-JDK replacement, JDK ABI, JNI speedup or source-stage promotion claim.

License: new first-party source Apache-2.0, retained original upstream JDK and third-party copyrights untouched. LeetCode/HackerRank/GFG donor material is category-only; no challenge source copied.
