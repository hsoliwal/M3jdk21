<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# M3JDK21 source-time weighted structural-distance receiver

This change is **inside the M3JDK21 product repository**, not a Synexia runtime layer.
The existing product source-time `@Sortable` producer already manufactures package,
class, field, method, dependency and compiler-attributed call tokens. This tranche
refines only their existing six-dimensional weighted distance consumer, without
introducing a new payload owner, runtime reflection, per-element wrappers or JNI symbols.

## Ownership and exact source

Canonical recipe: [Synexia PR #12464](https://github.com/hsoliwal/com.synexia/pull/12464),
`com.synexia.M3Jdk21SortableDistanceNormalization20261019`.
Target file before blob: `532f3078f9125e526882a3ae0367bbb32b784589`.
Target file after blob: `e3fe7865fd7a05459ae842b1af85ccaacb84d065`.
Added standalone Java21 contract blob: `8742c9cde89bf432773806b5194bf91aec41c692`.
The target implementation/test were received **byte-identically** from the canonical
recipe templates; their target SHA-256 values are checked by the workflow.

## Contract and capability

The source-time feature fingerprints remain sorted unsigned unique 64-bit hints.
Their weighted Jaccard distance is approximate relative to original source structure.
It must not replace exact JDK String, Comparator, reflection, identity or
collections semantics, nor infer runtime reference counts from static call edges.

The previous direct double-weight summation could overflow when weights are each
finite. The corrected path divides by the largest positive weight first, preventing
spurious overflow while preserving the scalar [0,1] range and rejecting nonfinite,
negative and all-zero weights. Zero-weight lane validation is retained.

No additional retained per-instance fields or cache allocation is introduced.
The existing M3 signal-order Java/JNI path remains the downstream primitive
order consumer. There is no new native ABI: six short structural dimensions do
not justify a JNI crossing without measured evidence.

## Executed versus pending

Locally executed: OpenJDK 21 `javac --release 21 -Xlint:all -Werror`;
standalone differential contract **7,616 checks PASS**. Exact Git blob identity
was verified for the original target and locally tested postimage.
The canonical Maven/OpenRewrite recipe and hosted product workflow are authored;
their hosted execution must be observed separately before claiming PASS.

Not qualified: full Maven/reactor, installed M3JDK21 build, jtreg/JCK, JNI/native
platform parity/resource/cost testing, full five-library collection contracts,
and String -> arrays -> collections -> compiler phase promotion.
The broader filtered/sorted `java.util` PRs retain independent ownership and
must not be overwritten or merged speculatively.

See `m3/collections/algorithm-catalog/source-signal-distance-20261019.tsv`
for problem-category donor decisions and restrictions.
