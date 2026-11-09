<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Reusable lessons from the interop correction passes

## I01 — java/local-type-inference/strict-compilation

Fix the earliest owning compile error and rerun the same strict gate; a parse fix can expose independent downstream defects.

**Observed result:** FAIL_COMPILE_OTHER_DEFECTS. Split-only candidate removed the parse error but exposed deprecated Predicate under Werror, a missing Map import, and a nonexistent factory class.

**Guard:** Refuse a changed preimage, unknown owner contract, mismatched source/class evidence, or unsupported backend claim; retain HOLD/failure instead of promoting it.

**Immutable record:** [event](events/3c1b0b057c2266d6ee0fffcccb070acebe13238477219b5ea8bd867710e6e672.json).

## I02 — java/api-resolution/strict-compilation

Resolve against the compiled current public owner before proposing a missing runtime implementation.

**Observed result:** COMPILE_PASS_RUNTIME_FAIL. The unchanged owning gate compiled32 main and44 test sources, then ran158 tests with6 failures and1 error; all are retained.

**Guard:** Refuse a changed preimage, unknown owner contract, mismatched source/class evidence, or unsupported backend claim; retain HOLD/failure instead of promoting it.

**Immutable record:** [event](events/ae0edb78925263b8788ef38dd21d54aca0ac63210cb52c5deb360dd0e5a74cbe.json).

## I03 — test-oracle/stability-backend-and-owner-metadata

Classify a discrepancy against the exact owner contract and environment before changing production behavior.

**Observed result:** PARTIAL_NOT_QUALIFIED. Attempt03 still failed3 tests; two exercised stale classes and one exposed a real donor/contract mismatch. Later fresh12 and fresh13 receipts validate these exact final fixtures.

**Guard:** Refuse a changed preimage, unknown owner contract, mismatched source/class evidence, or unsupported backend claim; retain HOLD/failure instead of promoting it.

**Immutable record:** [event](events/7ee3fbcd70be1435c1d2784bf46dc90a7314260b1be2a62b3a163acb2f07ebf5.json).

## I04 — differential-testing/exception-and-borrowed-list-contract

Retain failed and superseded fixture designs even when a narrower observation helper is selected.

**Observed result:** PARTIAL_PASS_SUPERSEDED. Spring four-test group passed in attempt03 and independent4500-operation probe observed209 matching UOEs; the full attempt03 suite did not qualify.

**Guard:** Refuse a changed preimage, unknown owner contract, mismatched source/class evidence, or unsupported backend claim; retain HOLD/failure instead of promoting it.

**Immutable record:** [event](events/869ff821d01fc74f06c7615620a61185b133fc376025b6a21240c13ecd2c6076.json).

## I05 — differential-testing/minimal-exception-observer

Minimize the changed behavioral atom after the contract is understood; exception parity adds coverage without mutating borrowed inputs.

**Observed result:** LOCAL_OWNER_VALIDATED. Both final fresh Java/native159-test suites pass; the independent probe retains4500 original operations and209 matching UOEs.

**Guard:** Refuse a changed preimage, unknown owner contract, mismatched source/class evidence, or unsupported backend claim; retain HOLD/failure instead of promoting it.

**Immutable record:** [event](events/f5be5a978d542fa908c3af6648b84b46757649ffa2e513b26021f5767c305aca.json).

## I06 — multiset/retention-cardinality/independent-oracle

A donor is a candidate whose compatibility must be checked; preserve the established M3 contract instead of copying an implementation quirk.

**Observed result:** LOCAL_OWNER_VALIDATED. Independent original6000 operations:51079 checks;294 exhaustive retention cases with90 explicit donor divergences. Final159 tests pass in both modes; the added fixture accounts for the158-to159 increase.

**Guard:** Refuse a changed preimage, unknown owner contract, mismatched source/class evidence, or unsupported backend claim; retain HOLD/failure instead of promoting it.

**Immutable record:** [event](events/37fde9c09a879de805574f56096f9c3f93412470dbe2a92dabfbe0cd95eb5452.json).

## I07 — verification/source-class-custody

A clean-build banner is insufficient when artifact evidence conflicts; source and class custody are separate facts.

**Observed result:** REJECTED_STALE_ARTIFACTS. Attempt03 remains158 tests/3 failures. Original compiled binary bodies remain in scratch; durable packet preserves their SHA256/mtime metadata, relevant source bodies and javap output.

**Guard:** Refuse a changed preimage, unknown owner contract, mismatched source/class evidence, or unsupported backend claim; retain HOLD/failure instead of promoting it.

**Immutable record:** [event](events/fb7aae912864759767fa699a3cc2522109dda9aa371dfbd4ad5c674956d4219f.json).

## I08 — verification/fresh-compiler-output

State the scope of each successful gate precisely; compile-only success cannot erase a prior runtime failure.

**Observed result:** PASS_COMPILE_ONLY. Updated Arrayification/Guava bytecode is observed and source hashes remain stable; no full-suite pass is attributed to attempt04.

**Guard:** Refuse a changed preimage, unknown owner contract, mismatched source/class evidence, or unsupported backend claim; retain HOLD/failure instead of promoting it.

**Immutable record:** [event](events/1a71bca3b347a12e2204ba45c8de35d3e5f926471a08931716dc30d258b7a21d.json).

## I09 — verification/capacity-refusal-and-isolated-output

A pre-execution capacity refusal is not a Java test failure or a successful mutation. Never infer a cache repair from unexplained file reappearance.

**Observed result:** BLOCKED_THEN_RETRIED_SEPARATELY. Retained exact diagnostic fields identify the two unexecuted commands and later cleanup observation. Fresh-path qualifications are separate later attempts.

**Guard:** Refuse a changed preimage, unknown owner contract, mismatched source/class evidence, or unsupported backend claim; retain HOLD/failure instead of promoting it.

**Immutable record:** [event](events/9a7f972a7e6e27939e2b5767d17804f0b9d31e81ed8fe8975d300676752d5eec.json).

## I10 — verification/fresh-java-jni-corpus

Freeze exact source inputs and verify actual backend availability before qualifying a reused implementation.

**Observed result:** PASS_LOCAL_12_TARGETS. Each mode159 tests/0 failures/errors/skips; standalone bindings152 checks across5 families/25 bindings; observed both native availability flags true in native and false in Java, with existing Guava26421 checks/6400 steps in each.

**Guard:** Refuse a changed preimage, unknown owner contract, mismatched source/class evidence, or unsupported backend claim; retain HOLD/failure instead of promoting it.

**Immutable record:** [event](events/5fb40a533f04f7316e9661bb307c7ed1dc6f1c51e377e01d1422536a66e8af52.json).

## I11 — metadata/factory-owner-source-consistency

A passing test set does not make an incorrect owner description true; record the discrepancy and explicit successor scope.

**Observed result:** HOLD_METADATA_SCOPE_AT_CAPTURE. Exact12 candidate source review is retained, including the historical metadata finding; final13 successor handles it separately.

**Guard:** Refuse a changed preimage, unknown owner contract, mismatched source/class evidence, or unsupported backend claim; retain HOLD/failure instead of promoting it.

**Immutable record:** [event](events/8b44344534e75c43097affa31b267eb87b5db7ee54f4f00417bad7e852376d70.json).

## I12 — metadata/factory-owner-source-consistency

Correct factual source metadata mechanically; never add a new owner or change runtime behavior to fit outdated prose.

**Observed result:** SOURCE_REVIEW_PASS_RUNTIME_PENDING_AT_CAPTURE. The independent13 metadata review verifies all11 Java postimages unchanged from12, one TSV cell plus one overview owner label changed, and exact source/factory/resource loading witnesses. Later final13 receipts contain runtime requalification.

**Guard:** Refuse a changed preimage, unknown owner contract, mismatched source/class evidence, or unsupported backend claim; retain HOLD/failure instead of promoting it.

**Immutable record:** [event](events/b96fc57ef307acdbd610d3fdadbe6d3f7543885f0504c366f3be9d0a8f0e2fde.json).

## I13 — verification/exact-expanded-closure

A metadata successor must carry its own exact closure and receipts, even when all Java class bytes remain identical.

**Observed result:** PASS_LOCAL_13_TARGETS. Each mode159 tests/0 failures/errors/skips; each binding run152 checks/5 families/25 bindings; each Guava corpus26421 checks/6400 steps with asserted backend availability. All Java class hashes match the previous12 build; packaged TSV SHA95075c5f... matches the final resource.

**Guard:** Refuse a changed preimage, unknown owner contract, mismatched source/class evidence, or unsupported backend claim; retain HOLD/failure instead of promoting it.

**Immutable record:** [event](events/4f25af9c5aa586862e7a419c0063db8c33af9923905d911abd6aa315636e52c0.json).

## Shared claim limits

The Apache donor implementation is not a universal oracle for the explicit M3 retention contract. The Spring borrowed-list exceptions are legitimate outcomes to compare. No test corpus was pruned, no strict flags were removed, and no native implementation or production collection owner was changed. The unexplained stale-file mechanism remains open; fresh-path evidence qualifies the observed runs only. No timing benchmark or general speed claim follows from these correctness results. Later canonical gates are independent observations.
