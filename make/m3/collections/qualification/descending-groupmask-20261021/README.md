<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# M3JDK21 primitive filtered/sorted index group-mask receiving

## Direct product ownership
M3JDK21 **is** the integrated Java/JNI/HotSpot JDK, not an external compatibility layer. This bounded receiving slice changes the existing `com.m3.collections.M3IndexedListSnapshot` implementation inside the owning `m3/collections` module. The canonical recipe, original preimage, source-sealed postimage, donor catalogue, permutation/contract tests and append-only correction memory are maintained in the Synexia convergence workspace.

## Behavioral atom
Ascending physical index sequences previously allocated an extra `int[n]` temporary for stable descending reordering across comparator-equivalence groups. The target retains the exact historical forward and reverse comparator calls **before any array writes**, records the group beginnings as `long[]` bits, then reverses the existing **private `int[]` index directory** in two deterministic passes to preserve encounter order within each equality group. Returned snapshots still share the original `Object[]` payload. The unsorted stable mergesort path, public signatures and native selectors are unchanged.

This is a private-scratch reduction from `4*n` bytes to about `n/8` bytes for the qualifying ascending path, not a retained-heap or CPU speedup claim. Small arrays also have object headers. Native-required masked selection still uses the existing source-built JNI path-mask library and refuses unavailable JNI; no new C exports or Java object pointers cross native code.

## Source identity and acceptance
`SOURCE_MANIFEST.tsv` pins exact original/postimage SHA-256 and Git blob hashes for three Java files. The source postimages are copied exactly from the canonical Synexia recipe `m3-indexed-descending-groupmask-20261021`, not independently hand-edited product code.

Required actual gates:
1. Canonical OpenRewrite SDK first application, deterministic fixed point and preimage-drift refusal.
2. Strict Java21 original collection owner build and JUnit; independent comparator-tape and exception-injection differential.
3. JNI_REQUIRED path mask selection + sort under `-Xcheck:jni` against existing C11 library, without Java fallback.
4. Full source-built M3JDK21 image, jtreg/JCK, public ABI, five-provider compatibility, GC/CDS and platform qualification.
5. Comparative JMH/CPU/allocated-heap/retained-heap/native-memory measurements. A temporary workspace reduction alone is not a measured throughput improvement.

Isolated predecessor-prototype comparisons passed 260,000 checks. Product-side acceptance is not yet established by recipe publication. Keep DRAFT while any required gate remains NOT_RUN, FAILED or HOLD.
