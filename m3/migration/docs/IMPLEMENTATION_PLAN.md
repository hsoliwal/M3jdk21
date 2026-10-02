# MIndex-to-M3 first reproducible port implementation plan

> **Scope note:** this is the retained first reproducible MIndex/prefix implementation tranche, not the whole-JDK implementation plan. The canonical dependency-aware programme is [../../docs/whole-jdk-work-packets.md](../../docs/whole-jdk-work-packets.md), with detailed collection design in [../../docs/whole-jdk-collections-replacement.md](../../docs/whole-jdk-collections-replacement.md) and continuation guidance in [../../docs/whole-jdk-resume.md](../../docs/whole-jdk-resume.md). The checkboxes below describe only this historical bounded tranche and must not be read as whole-JDK completion.

**Goal:** Establish repeatable pinned porting and one independently testable prefix-facts specialization, without changing the existing text owners or the installed JDK.
**Architecture:** Retain the canonical naming authority at m3/docs/name-mapping.json; extend its history with versioned capability records. A dependency-free Java source-bound recipe verifies input/output closure and fails closed on drift. A separate algorithm module consumes the existing sealed M3StringPiece view. Python with tooling-only jsonschema 4.26.0 validation follows the target's existing recipe tooling convention and does not enter java.base.
**Tech stack:** Java 21; Maven tooling descriptor; Python 3 and jsonschema 4.26.0 for manifest validation; standard-library packaging. Native OpenJDK build remains unchanged.
**Spec:** User's 2026-10-02 MIndex family migration specification; authoritative design m3/docs/shared-atom-concatenation.md in target PR7. This plan is a dependency-ready tranche, not a substitute for its twelve passes.

## Global constraints
Inventory before changes; no blind type-prefix substitution; no new canonical text storage; no public API removals; no merges; no private source fixtures in public CI; no unsupported test/benchmark claims. Preserve OpenJDK licensing. Keep all three routes and full inventory explicitly open until their gates pass.

## Tasks
- [x] Pin default branches and requested PR heads; separate PR6's experiment-branch merge from master.
- [x] Read existing naming and recipe manifests, owner reconciliation, source bridge and prefix implementation.
- [x] Write failing algorithm tests using actual pinned target owner classes, not stubs.
- [x] Port prefix facts to com.m3.algorithm.M3PrefixZ, retaining UTF-16 semantics and adding inner-loop cancellation checks.
- [x] Implement/test a Maven-executable, dependency-free Java exact-file recipe with preflight, replay, drift refusal, bounded reads, symlink/path refusal and rollback ownership receipts.
- [x] Extend the existing naming map; implement/test schema/coverage validation, three-way change classification and truthful partial-state handling.
- [x] Generate the stock Java 21 String API surface and acceptance matrices; persist omissions and resume instructions.
- [x] Run actual Java 21 tests in normal and interpreter modes; record exact blob and candidate closure hashes.
- [ ] Publish a draft PR with the correct base and verify remote content against locally tested content.

## Review focus
Cancellation during a long equal-prefix scan; all UTF-16 code units and seam segmentations; integer-overflow and zero budgets; recipe drift in late files causing partial writes; stale/tombstoned mappings and incomplete-source scans incorrectly presented as complete. Test each explicitly.
