<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 native counted-generic bag rank index: product receiver

Canonical recipe owner: [Synexia #12247](https://github.com/hsoliwal/com.synexia/pull/12247),
exact SHA `7f953f7c405449a293b58fd29cb500be8ea36277`.
Product M3JDK21 branch is an opt-in Java21 native collection increment, not a
wrapper over provider libraries. This is NOT a primitive-only value collection:
`M3BagRankIndex<E>` reuses the existing generic `M3CountedBag<E>` snapshots
with GC-traced object references, and retains only primitive indexing metadata.

A canonical immutable `CountSnapshot<E>` owns the key references/counts once.
New retained `int[]` carries stable order of distinct groups, and `long[]`
holds exact occurrence-prefix counts. Source-sealed Java21 source and test
blobs are identical to the Synexia Maven/OpenRewrite templates in
`recipe-mirror` and compared by `verify.py` before selected compilation.
No per-occurrence Entry/Node objects, copied Java key payload, comparator in
native code, or new JNI symbols.

The public `AbstractCollection<E>` surface is immutable and rejects
writes including removeIf(false), addAll(empty), iterator.remove and clear.
It provides comparator-aware lower/upper bound, distinct counts, long-size
occurrence rank/select, O(1) metadata slices, descending views, and
once-per-group Java predicate filtering. Nulls require comparator support.
A selected `byte[]` of full unsigned state bits is projected through
`M3IndexMaskCompactor`, whose existing C17 JNI is explicitly opt-in;
only `int[]` ordinals and state bytes cross JNI.

## Honest execution receipts

Isolated Java21 strict compiler and standalone oracle PASS 213,230
checks each (normal and interpreted JVM). These local tests used API-shaped
test-only stand-ins for existing CountedBag and JNI owners. This is **not
a real M3 source closure or native qualification**. GitHub checked that all
3 original generated source Git blobs match the canonical Synexia output.

Hosted workflow below runs real Java classes with full existing source
closure, both modes including checked C17/UBSan native, the existing module
Maven/JUnit, as well as the exact SHA-256 manifest. Synexia separately
runs real Maven/OpenRewrite Recipe.run, drift and fixed-point tests. Do not
count CI workflow creation or a queued job as passed.

This one generic index family is not the full implementation of all
JDK/Guava/Eclipse/Spring/Apache collection classes. Existing owner
classes, JDK String→arrays→collections→AST phase ordering, donor rights
and first-party Apache-2.0 attribution are preserved.
