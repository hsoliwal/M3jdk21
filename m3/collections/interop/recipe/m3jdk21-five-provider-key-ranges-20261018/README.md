<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Five-provider indexed key-range recipe: 2026-10-18

This is the canonical Synexia recipe, not a second product implementation. The production target is
`hsoliwal/M3jdk21` (optional `m3/collections/interop`), and Synexia remains the source-sealed
convergence, donor evaluation, test and recipe authority. M3JDK21 may mirror this exact source for
proof; no Synexia runtime dependency may be introduced.

## Source and contracts

Source-execution owner: existing `com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe`.
Named OpenRewrite composition: `com.synexia.m3.FiveProviderKeyRanges20261018`.
Two Java 21 targets are ABSENT-before. Their SHA-256 is recorded in the sorted manifest and
verified after recipe generation. No pre-existing API method or JNI symbol is changed.

`M3FiveLibraryKeyRanges` is a non-owning metadata view over immutable indexed `M3FiveLibraryIndexViews`.
It sorts only the index directory once and supplies O(log n) comparator-based lower/upper-bound,
rank/duplicate occurrence, head, tail, inclusive/exclusive range, and filtered state-bit windows.
Slicing/filtering preserve sort order; Java predicates/comparators never cross native. Caller remains
responsible for a stable comparator and any null semantics. Five real provider output surfaces
continue to come from the unchanged existing descriptor factories and vendor libraries. A filtered
snapshot is not silently substituted for a live provider view.

## Review and qualification sequence

1. Inspect `CATALOGUE.tsv`; reuse the named first-party array index, byte-mask and JNI owners.
2. Verify manifest hashes, `ABSENT` preimages, license, and source equality.
3. Run real Maven/OpenRewrite JUnit source generation, missing/drift refusal, fixed point.
4. Verify Java 21 strict compilation, real JDK/Guava/Eclipse/Spring/Apache provider contracts.
5. Exercise existing JNI `NATIVE_REQUIRED` behavior under `-Xcheck:jni` and real native binary.
6. Examine CPU cost, retained heap, selected provider versions, installed-JDK/jtreg/JCK,
   multi-OS JNI and full-module policies before promotion.

Repeat the recipe, not hand modifications to generated product output. This slice does not certify
all upstream API methods; provider-specific semantics and release qualification remain separate.
