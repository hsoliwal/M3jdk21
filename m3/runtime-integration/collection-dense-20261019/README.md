<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Integrated collection dense selection

This is an incremental continuation of PR 1435 at
2717d156ad2aba73dfdfcfc6a58b59a82371798e. Runtime changes are inside the existing
java.base collection owners and libjava. The public Collections APIs and JNI symbols
are unchanged from that parent. No Synexia runtime dependency is introduced.

Fully accepted filtered snapshots retain the already-detached reference array.
Fully selected mask ranges produce consecutive indices after a second count check;
partial selections preserve the original sparse extraction loops. Java remains default.

The canonical recipe is com.synexia.m3.CollectionDense20261019. The exact source revision,
hashes and target authoring mirror are recorded in SYNEXIA_HANDOFF.json beside this file.
The public mirror under m3/tooling/collection-dense-20261019/synexia-openrewrite-recipes
contains the named recipe, tests, fixtures, category catalogue, licenses and evidence.
It excludes private Synexia-wide ledgers and custody fixtures.

Read the mirrored CONTRACT.md and BENCHMARKS.md before review. The executed pass compares
the baseline and three cumulative production-file refinements, including public API,
JNI headers, view tests, dense offsets, native buffer residues and >2^31-bit capacity.
The original candidate, mixed/slower timing samples and proof-runner failures are retained.

Use the mirrored proof/qualify.py recipe then product phases in a fresh scratch directory,
followed by measure.py and focused.py. The source recipe performs real Maven/OpenRewrite
execution and refuses source drift; generated output must never be hand-patched.

Qualification is Java 21 patch-module/Linux JNI. Full M3JDK21 image, jtreg/tier/platform,
reactor and release acceptance are pending. PR 1435 remains the parent dependency; this
does not advance the global String-first receiving phase or complete the entire SDK.
