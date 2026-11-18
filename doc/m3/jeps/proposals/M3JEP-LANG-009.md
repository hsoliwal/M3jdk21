<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JEP-LANG-009: Extensions

| Field | Value |
| --- | --- |
| Proposal | `M3JEP-LANG-009` (Synexia internal, **not** an OpenJDK JEP number) |
| State | **Draft / implementation HOLD** |
| Author | Hitesh Soliwal and contributors to the Synexia Project |
| Kind | M3JDK21 opt-in feature candidate |
| Inspiration | Kotlin, C#, Swift (ideas only; no third-party source copied) |
| Owning component | `javac_m3_frontend` |
| First receiving stage | `AST_COMPILER` |
| Canonical catalogue ID | `LANG-009` |

## Summary

Introduce an opt-in M3JDK21 **extensions** capability: `lexical_extension_method_imports`. This is a design proposal, not installed Java syntax, a completed compiler feature, or acceptance of an upstream runtime.

## Detailed technical annex and related proposal ownership

[Extension Members — detailed design annex](../../../m3-guide/m3jep-extension-members.md) elaborates the LANG-009 lookup, compiler integration and verification contracts. It has **no separate proposal ID**, does not change the existing `LANG-009` catalogue row, and is not proof that any native M3JDK language feature has shipped.

Related independent proposal authorities:

| JEP identity | Ownership boundary |
| --- | --- |
| [LANG-001](M3JEP-LANG-001.md) | Operators and any symbolic lowering |
| [LANG-008](M3JEP-LANG-008.md) | Computed property access and lvalue semantics |
| [LANG-010](M3JEP-LANG-010.md), [LANG-019](M3JEP-LANG-019.md) | Nullability and generic applicability |
| [LANG-025](M3JEP-LANG-025.md), [LANG-026](M3JEP-LANG-026.md) | Delegated state and DSL provider scopes |
| [LANG-047](M3JEP-LANG-047.md) | Protocol/default/interface extension interactions |
| [CORE-008](M3JEP-CORE-008.md), [CORE-009](M3JEP-CORE-009.md), [CORE-010](M3JEP-CORE-010.md) | Compiler AST, canonical metadata, and frontend integration |

The indexed-provider and opt-in `javac` plugin work in Synexia PRs #18291, #18301, #18309 and #18336 is **candidate executable work only**. The annotated provider syntax demo is not native `javac` symbol-resolution integration. All product JDK compilation, jtreg, Maven/OpenRewrite execution, ABI, installed-image and receiving-phase gates remain `HOLD` or `NOT_RUN`, as applicable.

## Goals

- Specify the typed contract for lexical extension method imports and its precise Java 21 boundary.
- Reuse `javac_m3_frontend` and the existing Synexia source-sealed compiler/recipe owners.
- Admit lowering only after deterministic compiler facts and executable differential oracles.

## Non-goals

- Change meaning, overload resolution, class files, or observable behavior of any valid Java 21 program.
- Copy the Kotlin, C#, Swift implementation, runtime or licensed source wholesale.
- Claim an OpenJDK JEP assignment or a public M3JDK21 target release.

## Motivation

The Kotlin, C#, Swift ecosystem motivates the **extensions** problem category. M3JDK21 seeks the useful capability through its own Java/JNI and compiler contracts rather than equating different source-language semantics.

## Description and proposed semantics

1. Admit `lexical_extension_method_imports` only in an explicitly selected M3 source mode. Reserve no new meaning in ordinary Java 21 source.
2. Resolve types and symbols using the existing compiler evidence owner, including source range, classpath/module graph and overload identity. Reject unknown or ambiguous resolution; no token/regex rewrite grants meaning.
3. The implementation begins as opt-in, typed, source-attributed compiler facts; syntax is accepted only after explicit grammar, overload and lowering oracles, not lexical substitutions.
4. Preserve the baseline safeguard: **Java lookup and erasure unchanged**. Specify null, error paths, side effects, evaluation order, generics/bridges and binary descriptors before lowering.
5. Record source hashes, inferred atom/logic/structural fingerprints and a qualified versioned owner. Hash or SimHash similarity is evidence only, never equivalence.

## Compatibility

The baseline is Java 21, including source and binary compatibility, verifier/linkage/reflection, JNI and GC, serialization, exception order and public API identity. The proposal is an *additive superset*, not a source-level reinterpretation.

## Risks and failure cases

- Mandatory differential oracle: **ambiguity inheritance and binary name**.
- Separate obligations: **JDK21 source, binary and observable effect compatibility**.
- Unsupported mapping must fail closed with an attributed diagnostic, not silently select another operator, method or representation.

## Alternatives

Keep the present Java 21 API/library expression unchanged; provide explicit helper methods where possible; or defer M3 syntax until deterministic contracts exist. Do not adopt a foreign-language parser or evaluator without a pinned rights and behavior review.

## Testing

1. Differential compile/execute existing Java 21 examples and old binaries, including ambiguous overloads, fake code in strings/comments, generic erasure and exception ordering.
2. Add simple, boundary, hostile and pathological tests for **ambiguity inheritance and binary name**; retain exact source/output hashes and minimal failure counterexamples.
3. Execute source-sealed Maven/OpenRewrite recipe, exact emitted output comparison, stale/duplicate refusal, fixed point and repeated deterministic schedules.
4. At product admission: run JDK `javac --release 21` baseline, relevant jtreg, JNI/GC/native/runtime and installed-image gates; record actual resource measurements where applicable.

## Dependencies and delivery

- Prerequisite: `STRING_ARRAY_COLLECTION_STAGE_ORDER`; receiving order: String → arrays → collections → AST/compiler → remaining.
- Native/legal donor review: **NOT_RUN**. Canonical Maven/OpenRewrite qualification: **PENDING**. M3JDK21 product promotion: **HOLD**.
- Source catalogue: [`LANG-009`](../PROPOSALS.tsv); do not mutate this historical fact row to announce implementation.
