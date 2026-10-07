<!-- SPDX-License-Identifier: Apache-2.0 -->
# MIndex family migration — resume first

Status: dependency-ready compatibility-port increment; NOT a completed family migration.

## Exact inspected baselines

- Source: hsoliwal/com.synexia (private), develop, 75fb1abaecb56969bea2914520bbe819f130632b.
- Destination: hsoliwal/M3jdk21 (public), master, 8bb6215372e07712f1fdf5a0cb912af495007b19.
- The first destination observation was d6390ea3bb348f0c22afdba0819ca4ec0e97970f. The branch moved during inspection.
- PR 7 merged documentation. PR 6 subsequently merged into its base on 2026-10-02 at 01:27:56 UTC, merge 305dc277139b0e7cff1f4e284e7019a381480153. Its tested historical head remains 3776d6e674d6c9b04539ca24aca2504aa88d4a57. A merge does not resolve its two enabled StringJoiner failures or establish compiled-mode conformance.

Read m3/docs/shared-atom-concatenation.md, m3/docs/synexia-reconciliation.md and the authoritative m3/docs/name-mapping.json before resuming. Extend that mapping rather than introducing a second mapping authority.

## Ownership decision for this increment

Retain the established com.synexia.indexstring FrozenChars/FrozenBytes and MIndexJoinedChars/MIndexJoinedBytes descriptor/stream/interner closure. Preserve original packages and source contracts in an isolated stock-JVM compatibility port. Do not put these application classes, Maven or OpenRewrite into java.base. Never put the original source artifact and this compatibility artifact on the same class/module path: they intentionally preserve the same class names.

The existing com.m3.text format prototype is not silently promoted to canonical ownership. Structural MIndexAst/MIndexDag/MIndexInteraction, runtime representations and the existing synexia-mindex-indexstring-bridge remain separate mapped obligations. Owner-local IDs are not interchangeable. A descriptor interner's exact-content-plus-segmentation behavior is not proof of namespace/generation identity.

## Scope and publication

Only source files explicitly required by this authorized migration may be ported, with their notices and exact source hashes. Private whole-repository inventory remains private. Do not publish unrelated material or use this port to replace private historical comparison fixtures after an HTTP 404. Preserve upstream OpenJDK licensing and existing failing gates.

The initial executable closure contains six source files. Whole-family discovery, symbol/dependency attribution and the complete coverage decision remain OPEN; a small closure must not be described as exhaustive.

## Required gates

Recipe-first changes require deterministic replay, drift/mixed-state refusal, idempotence and rollback. Record exact input/output hashes and actual execution. Separate stock-view tests, compiler-lowering proof and a complete matched JDK build. No String.class transplantation, JDK installation replacement, PR merging, security changes or paid-capacity changes are authorized.

Local Java 21 is available. CodexPro failed to connect and local Git could not resolve github.com. The authenticated GitHub connector supports pinned text reads and draft publication. Maven/OpenRewrite integration, full source reactor and full modified-JDK builds must remain unverified unless actually executed later in this change.

## Next dependency-ready work

Implement and test the retained-view extension as a source-owner proposal; port the exact closure through sealed recipes; extend name-mapping.json with source/target lineage, exclusions and explicit open gates. Run whole-family inventory in an authorized complete source checkout before claiming complete coverage. Review three-way changes against each mapping's last synchronized source and target, never overwrite divergent targets automatically.
