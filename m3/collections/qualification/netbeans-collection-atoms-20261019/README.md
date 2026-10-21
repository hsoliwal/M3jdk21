<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 collection atoms: current-owner qualification

M3JDK21 owns these Java and JNI implementations. The canonical Synexia workspace owns the reusable transformations and their qualification. This pass refines the existing `m3/collections` owners and the existing full Java/JNI build gate. It introduces no replacement payload store, generic recipe engine, or public collection API.

## Source and contract boundary

- Receiving baseline: `hsoliwal/M3jdk21@677437be295b23f7ccf7409be287008944d0e8b0`.
- Initial canonical authoring baseline: `hsoliwal/com.synexia@f015c57d05504e3b4d3d30a956e15a27f31b567e`.
- Canonical engine revalidation baseline: `hsoliwal/com.synexia@4565392487bf83b55a3031a26ea1a61b1b526b3c`. The final qualification uses its unchanged engine sources and preserves earlier receipts.
- Recipe: `com.synexia.m3.NetBeansCollectionAtoms20261019`, under `synexia-openrewrite-recipes/recipe-crates/m3-netbeans-collection-atoms-20261019`.
- Evaluated donor: `apache/netbeans@f26ca8d8025ff820b8ffe751b6db6c1cc67baa97`, especially the editor utility `GapList` owner. The supplied Antora repository contains documentation (`pages`, `partials`, and `images`); implementation evaluation uses the NetBeans source repository.

The existing public Java signatures, JNI symbols, dense/segmented array owners, physical indexes, unsigned state masks, offsets, and callback contracts remain the receiving authority. API entries are fresh caller projections; a map must not retain an entry facade cache. Runtime ownership and the ordered String/array/collection receiving policy are preserved. This collection-module proof does not advance a global JDK receiving phase.

## What the pass qualifies

The merged `M3FilteredViews` preimage cannot compile: it declares four methods twice. The repair reconciles the two predecessor implementations, retaining the established predicate callback tape and read-only reverse-view behavior. The existing `M3SlotSpliterators.sorted` atom restores `SORTED` characteristics and comparator metadata for every split of a reversed sorted subrange. Removing duplicate declarations alone does not satisfy that regression.

The other two production repairs restore fresh caller-owned entry projections in `M3SlotMap` and failure progress in `M3KeyedParallelismPlan`. The keyed worker publishes the original mapper exception and drains cancelled work until the existing shutdown interrupts it, allowing the unchanged coordinator to reach that exception. It adds no field, queue, worker, timer, or normal-path operation. See `OWNING_GATE_DECISION.md` and `KEYED_ADMISSION_DECISION.md`.

Full owning-module verification additionally checks unsigned required/excluded mask selection and every current JNI library. The qualification records original compiler and test failures before applying their successor recipes. Test corrections must be supported by a contract or an independent oracle; a failing assertion is never simply deleted.

The NetBeans gap-copy pattern is evaluated against `M3ArrayList.removeIf` and the native long-address gather owner. Correctness is necessary but insufficient for promotion. The evaluated span-copy variants slowed fragmented workloads, so their production postimages are refused. The existing `M3ArrayList`, long-address gather, and native mask-compactor implementations remain authoritative. Two zero-mask native proposals also passed their semantic checks, but their shared-host measurements did not resolve the cost gate; `NATIVE_DECISION.md` records that refusal. The independent callback/array contract proof and native gather failure instrumentation are retained as reusable verification inputs.

Consult `../../algorithm-catalog/netbeans-collection-atoms-20261019.tsv` for category decisions, candidate identities, measured counterexamples, license boundaries, and reuse rationale. Exact files, source hashes, executed recipe receipts, and target test results accompany this packet. Historical failed and rejected runs are evidence, not receiving outputs.

## Repeat the owning target gate

With JDK 21, Maven, a C17 compiler, and the source-JUnit dependency closure available, run from the repository root:

```sh
python3 m3/collections/qualification/full-java-jni-20261018/verify.py
```

Set `JAVA_HOME`, `MVN`, and optionally `M2_REPO`; set `M3_MAVEN_OFFLINE=1` to require the resolved cache. `EXPECTED_HEAD` can bind the checkout commit. The gate checks source inventories, fresh Java compilation, generated JNI headers, all product native libraries, fatal undefined-behavior sanitization, Java-only and required-native Maven suites, explicit native contracts, unavailable-library refusal, packaged legal/provenance bytes, and the `java.base` runtime dependency boundary. Native-only tests may skip only in the Java-only run; the required-native run must have zero skips.

The existing `.github/workflows/m3-collections.yml` invokes this same gate. A local source-hash-bound pass and a hosted check at the published PR head are distinct receipts. A queued, unavailable, or failed hosted run is never reported as passing.

## Synexia M3 refinement invariant

Consult the accumulated category catalogue and current owner first. Review LeetCode, HackerRank, and GeeksforGeeks as separate discovery inputs, then inspect compatible licensed GitHub implementations at exact revisions. Record retained and rejected candidates as well as selected candidates. Challenge statements and public submissions do not grant copying rights.

For each file, seal its interfaces, callback order, exceptions, mutation timing, aliasing, ownership, and applicable native effects. Decompose only where a behavioral atom improves reuse, independent replacement, or verification. Keep arbitrary decomposition limits out of the process. Execute the existing canonical Maven/OpenRewrite recipe against the exact preimage, prove refusal and fixed point, and qualify the actual generated result before the next refinement.

Parallel candidate research and independent proof preparation are permitted. Receiving mutations are serial. The next pass reuses this catalogue, the successful recipes, and the retained counterexamples; it does not repeat donor discovery or promote a faster but incompatible implementation.

## Immutable donor inputs

The active combined catalogue is `../../algorithm-catalog/netbeans-collection-atoms-20261019.tsv`.
The 20-row same-named file in this qualification directory is an unmodified donor-input receipt;
its relative links preserve the original `m3/collections/algorithm-catalog` path base. It is not a
second active catalogue. `REVIEWED_CANDIDATES.tsv` and `DONOR_DECISION.md` describe that frozen
input, while the active catalogue adds current-owner contract and qualification decisions.
`DONOR_SHA256SUMS.txt` checks the relocated supporting payloads. The diagnostic benchmark is
stored as `.java.txt` to identify it as evidence outside the production/test source roots; its
Java source bytes match the original input. The original donor checksum receipt remains in
the canonical evidence archive.

## Current and historical verification scopes

`KEYED_ADMISSION_EVIDENCE.json` binds the original deterministic deadlock, corrected failure-progress oracle, exact API descriptors, unchanged coordinator, and independent review. `METADATA_DECISION.md` and `METADATA_EVIDENCE.json` explain the two test-only historical-receipt corrections. The current owning metadata command passes all 15 tests.

The first actual combined receiver run passed the Java suite (453 tests, zero failures/errors, two native-only skips). Its required-native suite ran all 453 tests with zero skips and found one stale test assumption: `M3ObjectRangeMapSelfTest` unconditionally expected a missing native library even in the loaded JNI lane. `INITIAL_OWNING_GATE.json` preserves that failed run. The successor fixture must execute missing-library refusal in the Java lane and exact native LIVE/high-bit/physical-slot selection in the JNI lane. The successful complete successor gate is recorded in `FINAL_OWNING_GATE.json`: Java 453 tests with zero failures/errors and two native-only skips; required JNI 453 tests with zero failures/errors/skips. It also passes all 19 explicit native contracts, both missing-library programs, all ten product JNI libraries, fatal UBSan, checked JNI, exact packaging of 507 classes and seven legal/provenance resources, and `jdeps` limited to `java.base`. The receipt binds the tested dirty worktree to baseline677 and every source hash; published commit identities and hosted checks are observed separately.

The historical installed-source job is a separate known limitation. Its unchanged sealed inventory composes 113 paths, including 107 Java files, against 503 Java files in baseline677. Eighteen declared source hashes differ and 396 Java paths are unlisted. `INSTALLED_SOURCE_LIMITATIONS.json` records the audit. The verifier and all source-admission checks remain strict; correcting receipt unit tests does not qualify that stale inventory. The complete current module gate and hosted checks must be reported by their actual results, without treating this finite pass as whole-JDK acceptance.
