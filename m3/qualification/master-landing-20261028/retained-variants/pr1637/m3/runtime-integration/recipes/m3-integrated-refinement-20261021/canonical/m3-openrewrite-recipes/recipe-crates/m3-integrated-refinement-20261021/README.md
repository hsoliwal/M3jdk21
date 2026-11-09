<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3 integrated refinement — fresh recovery candidate, 2026-10-21

This crate owns a new finite Maven/OpenRewrite qualification and handoff for
`hsoliwal/M3jdk21@2779aba3f98f31c324be43f731be7914e1a05935`.
The named recipe is `com.synexia.m3.IntegratedRefinement20261021`.
It composes the unchanged canonical Java-LST and text snapshot owners at
`hsoliwal/com.synexia@e02e7cb0f2e8e46db4c5b476d59aa26658fa4d07`.

The October 20 integrated scratch packet and its review archive were unavailable
when this task resumed. This is a new candidate with fresh inputs and results.
It does not reconstruct or inherit the missing 53-path proof, seven correction
events, test counts, benchmarks, native packaging receipts, or publication claims.
The current source and retained executable evidence determine its scope.

## Existing ownership and source lineage

The four existing canonical owners are `M3HashPinnedJavaSnapshotRecipe`,
`M3HashPinnedTextSnapshotRecipe`, `M3EditScope`, and `M3ScopeInference`. Their exact
source hashes remain unchanged. The task adds data and qualification around those
owners; it does not add another transformation engine.

The scheduler test and proof-checked output copier are adapted from
[PR12605 at b6c35b9a](https://github.com/hsoliwal/com.synexia/pull/12605).
Its original files are preserved under `provenance/donors/pr12605/` and exact
readbacks are in `provenance/RECOVERY_SOURCE_READBACKS.json`. That donor's own
provenance explicitly describes a nonidentical recovered harness and fresh
12-path results. Those results are not this task's results.

The new probe binds actual compiled owners from this task's classpath. The donor's
String/compiler-specific bootstrap checks do not apply to this packet; this task
does not claim to rebuild or qualify those unrelated owners. Product compilation,
behavior, reflection/binary compatibility, native parity, full JDK builds, and
platform tests are independent gates.

## Preparation, execution, and receiving

`prepare_packet.py` accepts the reviewed handoff arrays, checks the exact target
revision and all before/after bytes, then creates finite phase manifests,
preimages, postimage resources, a named descriptor, and `SOURCE_SEALS.json`.
Every target has one serial file phase. New files require `ABSENT` preimages.
This preparation step does not write to M3JDK21 and is not recipe execution.

The current descriptor selects the separately recorded phase049 workflow
successor. **From pristine2779, first execute the exact initial crate and resources
retained under `proof/sdk-002-offline/inputs/` and apply that stage. Then execute
and apply the current crate.** The original phase002 resources remain immutable;
phase049 accepts only its exact emitted workflow. This avoids broadening any
preimage guard. The final receiving PR still has2779 as its Git base.

Run the focused Maven crate on Java 21 with the actual compiled owning dependencies:

```sh
mvn -B -ntp -o -f pom.xml \
  -Dmaven.repo.local=/absolute/approved/cache \
  -Dparser.classpath=/absolute/actual/classes:/absolute/actual/dependency.jar test
```

The SDK test checks exact source hashes and lossless Java-LST parsing, actual
qualified owner attribution, individual and named serial phases, fixed points,
mixed application states, missing/drift/duplicate/wrong-kind refusals, unrelated
code-looking payloads, order permutations, serial repeat, and independent
two-worker replay. `target/recipe-proof.tsv`, phase receipts and `target/receiver/`
are created only after the declared proof completes. The emitted source replay
is not a target runtime equivalence claim.

After retaining that result, the copier checks the proof and emitted bytes before
planning or applying them to a receiving tree:

```sh
python3 apply_receiver.py /absolute/M3jdk21
python3 apply_receiver.py /absolute/M3jdk21 --apply
python3 apply_receiver.py /absolute/M3jdk21 --apply
python3 verify_sources.py /absolute/M3jdk21
```

The second application must report zero writes. Drift and symbolic destination
paths refuse. The copier transforms nothing: it installs exact SDK-emitted bytes.
Its receipts distinguish a check from an applied result. The final source verifier
checks every declared postimage and the unchanged canonical engines.

The target mirror preserves the canonical relative layout under
`m3/runtime-integration/recipes/m3-integrated-refinement-20261021/canonical/`.
The executable POM and source seals remain in
`synexia-openrewrite-recipes/recipe-crates/m3-integrated-refinement-20261021/`.
This is authoring/evidence custody and introduces no target runtime dependency.

## Correction memory and qualification boundaries

Start with `memory/README.md` and the current `memory/LESSONS-002.md`. The closed MOP-RCM/1.0 profile remains
`RECORDED` / `PENDING`; its conformance and byte-integrity checks cannot grant
product admission, publishing permission, or merge authority. Real new failed
attempts and corrections retain their exact logs and artifact hashes. Synthetic
conformance events remain isolated test fixtures.

Fresh tooling bootstrap uses pinned Maven artifacts and an explicit local cache.
The final focused repeat must run offline. This does not satisfy the separate
repository-wide source-vendored/offline reactor policy. No queued or zero-step CI,
selected-source compile, or source replay is called a full SDK or JDK build.

Current result summaries live in the retained proof receipts. A missing gate stays
NOT_RUN or BLOCKED. Full-family receiving remains at its existing phase; this
finite task does not advance the String → arrays → collections → AST/compiler →
remaining-family admission sequence.

## Rights and publication

Eligible first-party recipe tooling retains Synexia's Apache-2.0 notices. The
postimages, original tests, and donor fixtures retain their actual file licenses,
including OpenJDK and Eclipse obligations where applicable. A recipe does not
relicense its input or output. Applicable root texts are retained in `licenses/`.

Canonical develop is append-only. New files and registry/provenance additions
must preserve current source and earlier history. Prepared patches, local commits,
remote publication, hosted CI and merge are separate states and require their
own readback or executed evidence.
