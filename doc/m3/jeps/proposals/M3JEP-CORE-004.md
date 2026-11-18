<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JEP-CORE-004: const primitive facts and compile-time evaluation

| Field | Value |
| --- | --- |
| Proposal | `M3JEP-CORE-004` (Synexia internal; not assigned by OpenJDK) |
| State | **Draft / isolated source-bound const128 fact candidate; compiler admission HOLD** |
| Author | Hitesh Soliwal and contributors to the Synexia Project |
| Owning component | `M3CompilationEvidence` |
| First receiving stage | `AST_COMPILER` |
| Nature | Cross-language prerequisite / compiler and runtime compatibility proposal |

## Summary

Define const for compiler-proven primitive values, including decimal128 and float128.

## Goals

- Provide a deterministic, versioned M3JDK21-owned Java/JNI/compiler implementation of this capability.
- Preserve canonical shared storage, typed source attribution and exact behavioral contracts.
- Admit additive functionality with executable evidence and a source-sealed Synexia recipe.

## Non-goals

- Reinterpret already-valid Java 21 syntax or change the observable JDK public contract.
- Change Java SE specifications, claim official OpenJDK JEP status, or import donor runtime source without rights review.
- Treat a scaffold, a focused oracle, or a skipped source-built JDK gate as production delivery.

## Motivation

This shared building block supports the 70 opt-in cross-language M3 features. It allows useful donor ideas to converge through one JDK-native owner without incompatible sidecar APIs or per-entry wrapper proliferation.

## Description and proposed semantics

A const expression is typed, bounded, deterministic and free of observable effects. Capture source/provenance hashes; reuse values and precomputation without running arbitrary Java code during compilation.

### Received isolated fact candidate (2026-11-18)

An unexported `jdk.m3.paradigm` implementation,
`jdk.internal.m3.paradigm.numeric.M3Const128`, evaluates exactly one
bounded source declaration of the forms
`const decimal128 id = decimal-literal;` or
`const float128 id = decimal-literal;` into an immutable
`Fact(Type, name, sourceLiteral, encodedValue, sourceSha256)`. The source
SHA-256 includes exact spelling and whitespace; the fact also has a
versioned tab-separated representation. Literal-to-fact evaluation is
deterministic, does not run user code, and refuses general expressions,
multiple declarations, unsupported types, comments and fake literals
in strings. The parser is deliberately a small fail-closed diagnostic
cursor, **not** an alternative Java lexer, AST or compiler.

The Java 21 product proof passes 949 bounded source/encoding checks.
The independent GCC `libquadmath` binary128 reference proof covers
367 decimal values. The Synexia-owned recipes pin both the exact source
postimages and the three existing CORE-002/003/004 proposal preimages.
Full compiler AST attribution, constant propagation, side-effect proofs,
cycles, public primitives, JNI/GC/JIT and installed-image compatibility
remain **HOLD**. In particular, Java 21 `final` is not reinterpreted as
`const`, and the M3 source spelling does not compile on ordinary javac.

The first-stage architecture is: The implementation begins as opt-in, typed, source-attributed compiler facts; syntax is accepted only after explicit grammar, overload and lowering oracles, not lexical substitutions. If exact compatibility cannot be proved, leave the capability explicit and uninstalled.

## Compatibility

Java final continues to mean a non-reassignable binding; const is opt-in and not an alias for final. Java 21 source, ABI, JNI symbols, class loaders, reflection, serialization, GC, exception/effect order and platform invariants remain independently testable.

## Risks

The primary risk oracle is **exact constants, invalid expressions, cycles, side effects, class initialization, fixed point**. Other risks include accidental representation duplication, unresolved alias/ownership facts, compiler order changes and premature product promotion.

## Alternatives

Use current Java 21 libraries and explicit opt-in helpers; postpone grammar or VM changes until ABI and source-compatibility proofs justify them. Evaluate exact donor licenses and alternatives first.

## Testing

1. Source-seal exact preimages; prove recipe fixed point, determinism, and stale/duplicate refusal.
2. Compile boundary, adversarial and false-positive fixtures against Java 21; compare old binaries and observable effects.
3. Execute the named **exact constants, invalid expressions, cycles, side effects, class initialization, fixed point** differential suite and relevant JNI/GC/native tests (not merely a source-level simulation).
4. Require the target's source-built JDK, jtreg, installed-image and CI gates before promotion. Record missing gates as `NOT_RUN`.

## Dependencies and delivery

- Entry prerequisite: **CORE-001..003 and existing M3Const128 sidecar**.
- Product receiving order: String → arrays → collections → AST/compiler → remaining.
- This is an internal **Draft**. Canonical recipe/target qualification and official standards review are **not complete**.
