# M3 wrapper convergence contract — 2026-10-18

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.

## Baseline and authority

Target: hsoliwal/M3jdk21 at 0d73745d7203123533dce8f8fb264ba2746f6d2d. Canonical reusable recipe ownership: hsoliwal/com.synexia, synexia-openrewrite-recipes. This crate refines the existing hash-pinned snapshot recipe; it does not establish another rewrite engine. Target work remains inside the independently licensed m3/collections proving module. Public JDK runtime promotion and the global STRING phase remain outside this change.

## Frozen behavior

Existing callable public/protected signatures, payload owners, serialization promises, comparator order, stored representative identity, mutable/live versus detached/read-only distinctions, predicate order/short-circuit/exception behavior, validation precedence, half-open offsets, ordered duplicate row IDs, native loading and the two current mask-window JNI exports remain authoritative. Native array storage is borrowed only inside a call; no callback, critical pin or retained pointer is added. State flags are application selection metadata.

## Proposed leaf scope

1. Correct demonstrated bounded-map and bounded mutable-set navigation errors in existing wrapper owners. Queries outside a view are navigation inputs; range construction and inserts retain their own endpoint restrictions. Preserve the old traversal for inputs it already accepts; fallback must begin at an actual stored in-range representative.
2. Correct null-sentinel ambiguity for lawful comparators with null in the middle of their order, only after a reproducer and callback analysis.
3. Route strict read-only list iteration through its existing guarded backing iterator so read-only refusal, sequential traversal and fail-fast behavior follow the documented wrapper contract.
4. Reconcile the previously published stable equal-group reversal atom from M3JDK21 PR 1164 into the current M3LongIndexProjection. Preserve the newer M3LongAddressKernel and direct filtered index-window routing. Keep unknown encounter-order stable merge sorting.
5. Evaluate the same historical JNI decision-mask compaction atom against the current bounded scan owner; preserve end/cursor/limits/ABI and select it only with exact parity and resource evidence.

A source finding is not executed proof. All candidates remain pending until the following gates execute in order. Additional owners require an explicit scope record before candidate edits.

## Admission decisions after source and donor review

The proposed scope above records the initial questions, not an instruction to force every candidate through the contract gate. Before qualification, the bounded-navigation fallback and null-middle repair are refused: a blanket IllegalArgumentException catch can swallow a caller-owned comparator failure, and null-presence probes add observable callbacks. Their frozen witnesses remain searchable pending entries.

The read-only list candidate is narrowed to a guard around the original AbstractList iterator. Reads, predicate demand, failure identity and retry behavior remain on that path. Only mutation refusal is repaired to the owner's explicit existing rule that even empty/no-op writes must throw UnsupportedOperationException. The old IllegalStateException before next is recorded as the defect; this exception correction is disclosed separately from the read-effect equivalence claim. Linear iteration and backing fail-fast changes remain pending because they would alter callback demand.

The stable equal-group reversal candidate remains admitted for qualification. It must retain forward encounter order within every equal-value group, use the existing address kernel and preserve all logical/physical offsets. The historical JNI bitmap-compaction alternative is refused because it abandons current cross-chunk output batching. The narrower native candidate reads row IDs only from the first admitted position to the existing chunk end, preserving state reads, output JNI call counts, cursor packing, early limits and both exports.

New verification-only owners are admitted under m3/collections/src/test and the receiving qualification directory: frozen wrapper and index contract oracles, their JUnit entrypoints, the existing native region-count witness refinement, and a bounded allocation probe. Their candidate bytes are frozen before application; no new public production API is introduced. Catalogue and receiving evidence describe exactly which owners were inventoried, reviewed, transformed and executed.

## Verification order and evidence

Document and freeze source/preimages and regression oracles; patch the recipe/templates; lint; compile; tests; runtime. Apply target files serially through the qualified recipe, then repeat lint, compile, tests and runtime for each admitted pass. Retain raw stdout/stderr/exit receipts, exact source hashes, fixed point, input-order replay, missing/drift/duplicate refusal, API declarations and generated JNI-header/export comparison. Fail a stage before advancing. Baseline counterexamples and pre-existing failures stay separate from candidate results. Performance observations describe the exact measured workload only.

## Donor and disclosure disposition

Reuse already-public M3/Synexia wrapper, projection and JNI owners and recipe engines. GitHub donors and LeetCode/HackerRank/GeeksforGeeks problem categories are compared per concrete contract and license. Challenge source is reference-only. This is maintenance and reconciliation of public source, with no new disclosure of unpublished private invention material. Every copied first-party source retains Apache-2.0 attribution; upstream licenses remain per-file.
