<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JEP-CORE-011: Compatible interface/default/abstract extraction

| Field | Value |
| --- | --- |
| Proposal | `M3JEP-CORE-011` (Synexia internal; not assigned by OpenJDK) |
| State | **Draft / implementation HOLD** |
| Author | Hitesh Soliwal and contributors to the Synexia Project |
| Owning component | `java.base API owners` |
| First receiving stage | `REMAINING` |
| Nature | Cross-language prerequisite / compiler and runtime compatibility proposal |

## Summary

Extract extensible behavior into compatible interfaces and abstract helper classes, including APIs declared final.

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

Retain final public class shape and identity; only internal implementation may delegate. Add defaults and overloads after erasure, overload and diamond checks; do not create fictitious default methods in classes.

The first-stage architecture is: The implementation requires a separately reviewed runtime/JDK API owner, verifier, linker or compatibility plan after earlier receiving stages are qualified. If exact compatibility cannot be proved, leave the capability explicit and uninstalled.

## Compatibility

Existing descriptors, overriding, binary linkage, reflection, constructors and serialization are preserved. Java 21 source, ABI, JNI symbols, class loaders, reflection, serialization, GC, exception/effect order and platform invariants remain independently testable.

## Risks

The primary risk oracle is **source/binary API matrix, inherited defaults, SAM, reflection, final class callers**. Other risks include accidental representation duplication, unresolved alias/ownership facts, compiler order changes and premature product promotion.

## Alternatives

Use current Java 21 libraries and explicit opt-in helpers; postpone grammar or VM changes until ABI and source-compatibility proofs justify them. Evaluate exact donor licenses and alternatives first.

## Testing

1. Source-seal exact preimages; prove recipe fixed point, determinism, and stale/duplicate refusal.
2. Compile boundary, adversarial and false-positive fixtures against Java 21; compare old binaries and observable effects.
3. Execute the named **source/binary API matrix, inherited defaults, SAM, reflection, final class callers** differential suite and relevant JNI/GC/native tests (not merely a source-level simulation).
4. Require the target's source-built JDK, jtreg, installed-image and CI gates before promotion. Record missing gates as `NOT_RUN`.

## Dependencies and delivery

- Entry prerequisite: **individual API source-sealed recipes**.
- Product receiving order: String → arrays → collections → AST/compiler → remaining.
- This is an internal **Draft**. Canonical recipe/target qualification and official standards review are **not complete**.
