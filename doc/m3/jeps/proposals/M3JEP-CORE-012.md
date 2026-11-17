<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JEP-CORE-012: Packed primitives and typed ID, offset and slice lanes

| Field | Value |
| --- | --- |
| Proposal | `M3JEP-CORE-012` (Synexia internal; not assigned by OpenJDK) |
| State | **Draft / implementation HOLD** |
| Author | Hitesh Soliwal and contributors to the Synexia Project |
| Owning component | `M3ArrayViews` |
| First receiving stage | `ARRAYS` |
| Nature | Cross-language prerequisite / compiler and runtime compatibility proposal |

## Summary

Add compact mask/bitfield primitives and explicit typed offset, index, range, ID and slice capabilities.

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

Use owner/generation-aware representations and checked conversions; do not expose unsafe raw addresses through ordinary values. Define little/big-endian transport and bounds explicitly.

The first-stage architecture is: The implementation reuses the existing M3 dynamic primitive-array owner, offset/index/generation and mask lanes, with checked alias lifetimes and JNI rules. If exact compatibility cannot be proved, leave the capability explicit and uninstalled.

## Compatibility

Java integer arithmetic and array index exceptions remain unchanged unless an M3 extension is opted into. Java 21 source, ABI, JNI symbols, class loaders, reflection, serialization, GC, exception/effect order and platform invariants remain independently testable.

## Risks

The primary risk oracle is **bit widths, alignment, zero-length views, owner mismatch, overflow and JNI**. Other risks include accidental representation duplication, unresolved alias/ownership facts, compiler order changes and premature product promotion.

## Alternatives

Use current Java 21 libraries and explicit opt-in helpers; postpone grammar or VM changes until ABI and source-compatibility proofs justify them. Evaluate exact donor licenses and alternatives first.

## Testing

1. Source-seal exact preimages; prove recipe fixed point, determinism, and stale/duplicate refusal.
2. Compile boundary, adversarial and false-positive fixtures against Java 21; compare old binaries and observable effects.
3. Execute the named **bit widths, alignment, zero-length views, owner mismatch, overflow and JNI** differential suite and relevant JNI/GC/native tests (not merely a source-level simulation).
4. Require the target's source-built JDK, jtreg, installed-image and CI gates before promotion. Record missing gates as `NOT_RUN`.

## Dependencies and delivery

- Entry prerequisite: **CORE-001/005/007**.
- Product receiving order: String → arrays → collections → AST/compiler → remaining.
- This is an internal **Draft**. Canonical recipe/target qualification and official standards review are **not complete**.
