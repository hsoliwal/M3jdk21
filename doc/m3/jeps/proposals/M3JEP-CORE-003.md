<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JEP-CORE-003: Decimal and fixed-point primitive types

| Field | Value |
| --- | --- |
| Proposal | `M3JEP-CORE-003` (Synexia internal; not assigned by OpenJDK) |
| State | **Draft / exact-quantum decimal128 const-fact candidate; IEEE interchange/runtime HOLD** |
| Author | Hitesh Soliwal and contributors to the Synexia Project |
| Owning component | `javac_m3/HotSpot` |
| First receiving stage | `AST_COMPILER` |
| Nature | Cross-language prerequisite / compiler and runtime compatibility proposal |

## Summary

Introduce opt-in decimal64, decimal128 and scale-qualified fixed-point numeric types.

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

Distinguish exact source coefficient/exponent from IEEE 754 packed decimal interchange; specify rounding, quantum, traps, context, overflow and serialization. The held decimal128 fact is not a packed decimal ABI.

### Received bounded evaluator candidate (2026-11-18)

The unexported `jdk.internal.m3.paradigm.numeric.M3Const128` evaluator
accepts `const decimal128 amount = 125.50;` as an *isolated source string*
and emits `D128:+12550E-2`, retaining the exact source coefficient,
quantum, and sign, including explicit negative zero. It rejects exact
coefficients wider than 34 significant decimal digits, quantum outside
[-6176, 6111], and adjusted exponent above 6144. It does **not** round
an inadmissible literal into a different value or quietly interpret its
spelling through Java `double`. It does not conflate exact decimal
coefficient/exponent facts with IEEE 754 decimal128 densely packed decimal
(DPD) or binary integer decimal (BID) interchange bits.

A Java 21 target self-test passes 949 checks across both 128-bit literal
types, including **500 seeded quantum/coefficient comparisons** against
independent `BigDecimal` construction, signed zero, source spelling,
invalid exponents, and 34-digit precision boundaries. The related
Synexia recipe carries exact SHA-256 source seals and replay tests.

Full decimal rounding/traps, overflow policies, IEEE decimal128 packed
encoding and conversion, arithmetic/JNI layout, a language primitive and
`const` compiler lowering remain **HOLD**. The named fact evaluator is
not a public Java API and is not an installed Java 21 syntax extension.

The first-stage architecture is: The implementation begins as opt-in, typed, source-attributed compiler facts; syntax is accepted only after explicit grammar, overload and lowering oracles, not lexical substitutions. If exact compatibility cannot be proved, leave the capability explicit and uninstalled.

## Compatibility

Neither Java double nor BigDecimal behavior may be silently changed. Java 21 source, ABI, JNI symbols, class loaders, reflection, serialization, GC, exception/effect order and platform invariants remain independently testable.

## Risks

The primary risk oracle is **decimal literal quantum, scale, negative zero, ties, overflow and IEEE encoding**. Other risks include accidental representation duplication, unresolved alias/ownership facts, compiler order changes and premature product promotion.

## Alternatives

Use current Java 21 libraries and explicit opt-in helpers; postpone grammar or VM changes until ABI and source-compatibility proofs justify them. Evaluate exact donor licenses and alternatives first.

## Testing

1. Source-seal exact preimages; prove recipe fixed point, determinism, and stale/duplicate refusal.
2. Compile boundary, adversarial and false-positive fixtures against Java 21; compare old binaries and observable effects.
3. Execute the named **decimal literal quantum, scale, negative zero, ties, overflow and IEEE encoding** differential suite and relevant JNI/GC/native tests (not merely a source-level simulation).
4. Require the target's source-built JDK, jtreg, installed-image and CI gates before promotion. Record missing gates as `NOT_RUN`.

## Dependencies and delivery

- Entry prerequisite: **CORE-001 and FACT-015/016 candidate receipts**.
- Product receiving order: String → arrays → collections → AST/compiler → remaining.
- This is an internal **Draft**. Canonical recipe/target qualification and official standards review are **not complete**.
