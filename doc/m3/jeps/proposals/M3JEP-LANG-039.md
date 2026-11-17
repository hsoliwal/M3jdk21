<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JEP-LANG-039: Borrowed Views

| Field | Value |
| --- | --- |
| Proposal | `M3JEP-LANG-039` (Synexia internal, **not** an OpenJDK JEP number) |
| State | **Draft / implementation HOLD** |
| Author | Hitesh Soliwal and contributors to the Synexia Project |
| Kind | M3JDK21 opt-in feature candidate |
| Inspiration | Rust (ideas only; no third-party source copied) |
| Owning component | `M3ArrayViews` |
| First receiving stage | `ARRAYS` |
| Canonical catalogue ID | `LANG-039` |

## Summary

Introduce an opt-in M3JDK21 **borrowed views** capability: `borrow_aware_slices_with_owner_lifetime`. This is a design proposal, not installed Java syntax, a completed compiler feature, or acceptance of an upstream runtime.

## Goals

- Specify the typed contract for borrow aware slices with owner lifetime and its precise Java 21 boundary.
- Reuse `M3ArrayViews` and the existing Synexia source-sealed compiler/recipe owners.
- Admit lowering only after deterministic compiler facts and executable differential oracles.

## Non-goals

- Change meaning, overload resolution, class files, or observable behavior of any valid Java 21 program.
- Copy the Rust implementation, runtime or licensed source wholesale.
- Claim an OpenJDK JEP assignment or a public M3JDK21 target release.

## Motivation

The Rust ecosystem motivates the **borrowed views** problem category. M3JDK21 seeks the useful capability through its own Java/JNI and compiler contracts rather than equating different source-language semantics.

## Description and proposed semantics

1. Admit `borrow_aware_slices_with_owner_lifetime` only in an explicitly selected M3 source mode. Reserve no new meaning in ordinary Java 21 source.
2. Resolve types and symbols using the existing compiler evidence owner, including source range, classpath/module graph and overload identity. Reject unknown or ambiguous resolution; no token/regex rewrite grants meaning.
3. The implementation reuses the existing M3 dynamic primitive-array owner, offset/index/generation and mask lanes, with checked alias lifetimes and JNI rules.
4. Preserve the baseline safeguard: **Java array aliasing and GC reachability preserved**. Specify null, error paths, side effects, evaluation order, generics/bridges and binary descriptors before lowering.
5. Record source hashes, inferred atom/logic/structural fingerprints and a qualified versioned owner. Hash or SimHash similarity is evidence only, never equivalence.

## Compatibility

The baseline is Java 21, including source and binary compatibility, verifier/linkage/reflection, JNI and GC, serialization, exception order and public API identity. The proposal is an *additive superset*, not a source-level reinterpretation.

## Risks and failure cases

- Mandatory differential oracle: **escape mutation alias and native pinning**.
- Separate obligations: **alias owner lifetime bounds abi**.
- Unsupported mapping must fail closed with an attributed diagnostic, not silently select another operator, method or representation.

## Alternatives

Keep the present Java 21 API/library expression unchanged; provide explicit helper methods where possible; or defer M3 syntax until deterministic contracts exist. Do not adopt a foreign-language parser or evaluator without a pinned rights and behavior review.

## Testing

1. Differential compile/execute existing Java 21 examples and old binaries, including ambiguous overloads, fake code in strings/comments, generic erasure and exception ordering.
2. Add simple, boundary, hostile and pathological tests for **escape mutation alias and native pinning**; retain exact source/output hashes and minimal failure counterexamples.
3. Execute source-sealed Maven/OpenRewrite recipe, exact emitted output comparison, stale/duplicate refusal, fixed point and repeated deterministic schedules.
4. At product admission: run JDK `javac --release 21` baseline, relevant jtreg, JNI/GC/native/runtime and installed-image gates; record actual resource measurements where applicable.

## Dependencies and delivery

- Prerequisite: `STRING_PRODUCT_GATE_AND_M3_ARRAY_VIEW_OWNER`; receiving order: String → arrays → collections → AST/compiler → remaining.
- Native/legal donor review: **NOT_RUN**. Canonical Maven/OpenRewrite qualification: **PENDING**. M3JDK21 product promotion: **HOLD**.
- Source catalogue: [`LANG-039`](../PROPOSALS.tsv); do not mutate this historical fact row to announce implementation.
