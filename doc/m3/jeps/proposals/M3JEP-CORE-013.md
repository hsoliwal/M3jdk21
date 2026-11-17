<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JEP-CORE-013: Static regex and known-schema precomputation

| Field | Value |
| --- | --- |
| Proposal | `M3JEP-CORE-013` (Synexia internal; not assigned by OpenJDK) |
| State | **Draft / implementation HOLD** |
| Author | Hitesh Soliwal and contributors to the Synexia Project |
| Owning component | `M3String/M3Compute` |
| First receiving stage | `STRING` |
| Nature | Cross-language prerequisite / compiler and runtime compatibility proposal |

## Summary

Precompute immutable regex, schema and grammar facts for fixed language specifications.

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

Use parsed grammar/automaton and masked-source structural evidence. Preserve full Java regex fallback and locale/Unicode behavior; no LeetCode-10 fragment is a replacement for java.util.regex.

The first-stage architecture is: The implementation first joins the canonical M3String byte/UTF-16 atom owner; every public String return follows Java 21 copy and encoding semantics. If exact compatibility cannot be proved, leave the capability explicit and uninstalled.

## Compatibility

Existing Pattern and Matcher flags, backtracking, capture and exceptions remain exact. Java 21 source, ABI, JNI symbols, class loaders, reflection, serialization, GC, exception/effect order and platform invariants remain independently testable.

## Risks

The primary risk oracle is **Java regex differential, anchors, backrefs, Unicode, catastrophic cases, false positives**. Other risks include accidental representation duplication, unresolved alias/ownership facts, compiler order changes and premature product promotion.

## Alternatives

Use current Java 21 libraries and explicit opt-in helpers; postpone grammar or VM changes until ABI and source-compatibility proofs justify them. Evaluate exact donor licenses and alternatives first.

## Testing

1. Source-seal exact preimages; prove recipe fixed point, determinism, and stale/duplicate refusal.
2. Compile boundary, adversarial and false-positive fixtures against Java 21; compare old binaries and observable effects.
3. Execute the named **Java regex differential, anchors, backrefs, Unicode, catastrophic cases, false positives** differential suite and relevant JNI/GC/native tests (not merely a source-level simulation).
4. Require the target's source-built JDK, jtreg, installed-image and CI gates before promotion. Record missing gates as `NOT_RUN`.

## Dependencies and delivery

- Entry prerequisite: **CORE-006 and existing M3 regex catalogue**.
- Product receiving order: String → arrays → collections → AST/compiler → remaining.
- This is an internal **Draft**. Canonical recipe/target qualification and official standards review are **not complete**.
