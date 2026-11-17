<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JEP-LANG-068: Value Layout

| Field | Value |
| --- | --- |
| Proposal | `M3JEP-LANG-068` (Synexia internal, **not** an OpenJDK JEP number) |
| State | **Draft / implementation HOLD** |
| Author | Hitesh Soliwal and contributors to the Synexia Project |
| Kind | M3JDK21 opt-in feature candidate |
| Inspiration | NewerJDKs (ideas only; no third-party source copied) |
| Owning component | `HotSpot_and_javac_m3` |
| First receiving stage | `REMAINING` |
| Canonical catalogue ID | `LANG-068` |

## Summary

Introduce an opt-in M3JDK21 **value layout** capability: `inline_value_layout_candidate`. This is a design proposal, not installed Java syntax, a completed compiler feature, or acceptance of an upstream runtime.

## Goals

- Specify the typed contract for inline value layout candidate and its precise Java 21 boundary.
- Reuse `HotSpot_and_javac_m3` and the existing Synexia source-sealed compiler/recipe owners.
- Admit lowering only after deterministic compiler facts and executable differential oracles.

## Non-goals

- Change meaning, overload resolution, class files, or observable behavior of any valid Java 21 program.
- Copy the NewerJDKs implementation, runtime or licensed source wholesale.
- Claim an OpenJDK JEP assignment or a public M3JDK21 target release.

## Motivation

The NewerJDKs ecosystem motivates the **value layout** problem category. M3JDK21 seeks the useful capability through its own Java/JNI and compiler contracts rather than equating different source-language semantics.

## Description and proposed semantics

1. Admit `inline_value_layout_candidate` only in an explicitly selected M3 source mode. Reserve no new meaning in ordinary Java 21 source.
2. Resolve types and symbols using the existing compiler evidence owner, including source range, classpath/module graph and overload identity. Reject unknown or ambiguous resolution; no token/regex rewrite grants meaning.
3. The implementation requires a separately reviewed runtime/JDK API owner, verifier, linker or compatibility plan after earlier receiving stages are qualified.
4. Preserve the baseline safeguard: **Java21 identity monitor JNI preserved**. Specify null, error paths, side effects, evaluation order, generics/bridges and binary descriptors before lowering.
5. Record source hashes, inferred atom/logic/structural fingerprints and a qualified versioned owner. Hash or SimHash similarity is evidence only, never equivalence.

## Compatibility

The baseline is Java 21, including source and binary compatibility, verifier/linkage/reflection, JNI and GC, serialization, exception order and public API identity. The proposal is an *additive superset*, not a source-level reinterpretation.

## Risks and failure cases

- Mandatory differential oracle: **classfile verifier reflection and array abi**.
- Separate obligations: **runtime identity linkage platform and native**.
- Unsupported mapping must fail closed with an attributed diagnostic, not silently select another operator, method or representation.

## Alternatives

Keep the present Java 21 API/library expression unchanged; provide explicit helper methods where possible; or defer M3 syntax until deterministic contracts exist. Do not adopt a foreign-language parser or evaluator without a pinned rights and behavior review.

## Testing

1. Differential compile/execute existing Java 21 examples and old binaries, including ambiguous overloads, fake code in strings/comments, generic erasure and exception ordering.
2. Add simple, boundary, hostile and pathological tests for **classfile verifier reflection and array abi**; retain exact source/output hashes and minimal failure counterexamples.
3. Execute source-sealed Maven/OpenRewrite recipe, exact emitted output comparison, stale/duplicate refusal, fixed point and repeated deterministic schedules.
4. At product admission: run JDK `javac --release 21` baseline, relevant jtreg, JNI/GC/native/runtime and installed-image gates; record actual resource measurements where applicable.

## Dependencies and delivery

- Prerequisite: `STRING_ARRAY_COLLECTION_AST_COMPILER_PRODUCT_GATES`; receiving order: String → arrays → collections → AST/compiler → remaining.
- Native/legal donor review: **NOT_RUN**. Canonical Maven/OpenRewrite qualification: **PENDING**. M3JDK21 product promotion: **HOLD**.
- Source catalogue: [`LANG-068`](../PROPOSALS.tsv); do not mutate this historical fact row to announce implementation.
