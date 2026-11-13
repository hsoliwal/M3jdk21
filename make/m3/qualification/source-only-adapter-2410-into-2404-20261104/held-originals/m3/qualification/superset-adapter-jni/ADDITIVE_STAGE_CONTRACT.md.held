<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: GPL-2.0-only -->

# Additive source-history merge: Object adapter + concurrent JNI qualification

## Immutable parents and scope (2026-11-04)

Receiving chain:
[M3JDK21 #2394](https://github.com/hsoliwal/M3jdk21/pull/2394) then
[#2395](https://github.com/hsoliwal/M3jdk21/pull/2395).
Frozen receiving head:
`8cacca9a03392540e46c1a0dc01b5d106c6d43af`.
Source diff from common base:
16 paths, 15 added and one original `java/lang/Object.java` modified.

Donating chain:
[#2393](https://github.com/hsoliwal/M3jdk21/pull/2393) →
[#2405](https://github.com/hsoliwal/M3jdk21/pull/2405) →
[#2406](https://github.com/hsoliwal/M3jdk21/pull/2406).
Frozen donor head:
`6cfc59b98f7d9609feee847a720bcb851f36ef04`.
Donor diff from common base:
11 added files, zero modified, deleted or renamed.

Common ancestor:
`172e39d5e69a8f7775c25ad1ce2bfaf2bf5d1246`.
GitHub compare returned 16 receiving changed paths and 11 donor changed
paths with **zero intersections**. All 11 donor paths must be absent
from the frozen receiving head.

## Exact proposed operation

Perform one GitHub-native `merge` (two-parent history-preserving,
neither `squash` nor `rebase`) through a short-lived **transport PR**
from the exact donor branch into this isolated staging branch.
Supply the donor's exact expected-head SHA to reject drift. Do not
write `master` or any protected/default branch; do not modify the
original PR heads or replace existing receiving source paths.

Required post-merge proof:
1. Merge commit must have **exactly two parents**: first the
   documentation-preflight staging head, second the frozen donor SHA.
2. First-parent diff must add precisely the 11 donor source paths
   with no modifications/deletions/renames, aside from a separate
   already committed contract/evidence receipt.
3. Every resulting donor source blob SHA-1 and Git tree mode must
   exactly match the original donor source; no inference of file
   equivalence without hash/readback.
4. The unchanged `Object.java` Git blob must remain
   `c703c72bea6e792a378bb6d3babc96f22dbf6cb4`, from the
   receiving self-cast chain.
5. Preserve original public `Object.toAdapter(Class<T>)` signature,
   method erasure `(Ljava/lang/Class;)Ljava/lang/Object;`, Java/JNI
   virtual dispatch, object identity, null handling and exception
   contracts. No `toAdpater` alias, HotSpot extra vtable slot,
   unqualified JNI native library or root file writes.

## Admission boundary

This is **source-history preservation only**. The recipient's
historical progress check still reports eight unadmitted source
transitions. The JNI donor's model tests do not prove the actual
source-built M3JDK21 `java.base`, native `CallObjectMethod*`,
weak-GC and HotSpot behavior. CI, JDK21 build, JNI jtreg/JCK,
Maven/OpenRewrite source recipe SDK and independent API/ABI
qualification must run against the **final merge head** before
promoting source into any protected default branch.

LeetCode 706/146, HackerRank Frequency Queries, GeeksforGeeks hash
probing are category/test-shape references; no challenge solution
implementation is copied by the merge. Both full original PR
histories and source rights remain intact.

Google Drive continuity checkpoint (not merge authority):
https://docs.google.com/document/d/14POJyi5FgXiD_GpE_snZQI5pyraL3CdgTCZFPUr1GXQ/edit
