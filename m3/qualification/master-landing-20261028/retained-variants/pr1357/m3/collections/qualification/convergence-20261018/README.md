<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 collection convergence

M3JDK21 is the target JDK product. This pass combines existing Synexia-derived
Java/JNI work in its existing collection owners: filtered and sorted wrappers,
indexed snapshots, deque spans, array offsets and object-state masks.

Read [CONTRACT.md](CONTRACT.md), [INPUTS.tsv](INPUTS.tsv), [REVIEW.tsv](REVIEW.tsv)
and [VERIFICATION.md](VERIFICATION.md). Search [CATALOGUE.tsv](CATALOGUE.tsv) by
problem category, operation or donor, for example `rg 'navigation|mask-' CATALOGUE.tsv`.
The canonical catalogue remains in Synexia's existing collection recipe crate;
this packet is the receiving record for the same finite pass.

The invariant is to maintain reusable validated transformation recipes, refine
files serially under their original public contracts, and accumulate algorithm
comparisons, donor decisions, refusals and verification results for later passes.
The unchanged `com.synexia.m3jdk.CollectionsContractRefinement20261017` recipe
and four engines are pinned in `recipe/ENGINE_SOURCES.tsv`.

| Final dataset | Java targets | Text targets | Purpose |
| --- | ---: | ---: | --- |
| `converged-owners-v2-20261018` | 10 | 7 | Existing runtime owners, JNI, driver and notices |
| `converged-oracles-20261018` | 19 | 1 | Regression contracts and native traffic fixture |
| `converged-build-20261018` | 0 | 12 | Source dependency verifiers and legal packaging |

These 49 targets are sealed against master `2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`.
The receiver admits actual SDK results together, then applies each file serially.
Newer master bag-rank owners are preserved; overlapping donor atoms are selected
once while their independent regression tests are retained.

The full-module driver builds ten source dependency modules, all 134 collection
Java sources, and nine JNI libraries from ten C sources. The packaged runtime
requires only `java.base`. Source-pinned donor notices are preserved.

This is a draft collection qualification pass. The standalone comparator,
linear-iterator and fail-fast probes remain unresolved on both baseline and
candidate. The owning module suite passes; that does not make those probes pass.
No global receiving phase, whole-JDK build, jtreg, hosted CI, non-Linux run or
full Synexia reactor acceptance is claimed.
