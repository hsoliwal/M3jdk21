<!-- SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0 -->
# M3 substrate for Eclipse base-framework convergence

Status: **implementation contract / inventory gate**. This document defines the first Eclipse-facing integration slice; it does not claim that the Eclipse Platform, SWT, Nebula, or GEF have already been replaced by M3. Production changes must follow as small, source-mapped, recipe-driven PRs.

## Goal

Use M3JDK21's canonical data and runtime owners to reduce duplicated framework machinery while preserving the observable contracts of the Eclipse projects. Eclipse remains the compatibility surface. M3 is the implementation substrate; it is not a reason to rename public Eclipse types, bypass extension points, or replace framework semantics with storage details.

Target consumers, in dependency order:

1. `hsoliwal/eclipse.platform.swt` — widget/resource lifecycle, event-loop and JNI-facing workloads.
2. `hsoliwal/eclipse.platform` — resources, jobs, scheduling, registry/service metadata and workspace state.
3. `hsoliwal/eclipse.platform.ui` — workbench selection, UI state and presentation models.
4. `hsoliwal/nebula` and `hsoliwal/gef-classic` — large collections, tables, trees, diagrams and graph/editor workloads.

The sequence is a target map, not a claim that any target has passed its build or integration gates.

## Substrate ownership and boundaries

| Capability | Canonical M3 owner to inspect/reuse | Eclipse integration rule |
| --- | --- | --- |
| Indexed coordinates, offsets, stable slots, masks and primitive sidecars | Existing M3Index/M3 collection and array owners; `m3/collections/` and the operational mapping in `m3/docs/name-mapping.json` | Use owner+generation-aware coordinates. Never treat a raw integer slot as a durable/global identity. Preserve sparse-zero versus absent semantics, order, mutation visibility and stale-handle behavior. |
| Immutable text, ranges, concatenation and reusable text facts | Existing `java.lang.M3String` runtime candidate, `M3StringOwner`, `M3StringTuple`, `M3StringFacts`, and the independent `com.m3.text` foundation | Public Eclipse/JDK String APIs remain authoritative. Keep UTF-16 code-unit, equality, hash, interning, serialization, reflection and JNI materialization semantics exact. No duplicate spelling store or Synexia runtime dependency. |
| File and mapped-resource access | **Inventory required**: locate the canonical M3File implementation and its Synexia recipe/source mapping before adding or porting a type | Do not invent a second file owner. Preserve Path/File/URI conversion, symlink and provider behavior, permissions, exceptions, close/lifetime rules, mapping bounds, external mutation and resource refresh semantics. Read-only mapping alone is not immutability. |
| Filtered/sorted/navigable views | Existing M3 filtered-view owners and JDK differential tests | Preserve backed-view behavior, comparator equivalence, null-friendly comparators, descending order, bounded navigation, bulk mutation and unmodifiable wrappers. Reuse the existing owner instead of adding wrapper stacks. |
| JNI/native fast paths | Existing native selector and String JNI boundary implementations | Native code may accelerate primitive, bounded operations only. Arbitrary Java predicates/comparators must remain Java-side. Preserve old ABI behavior or explicitly version the ABI with a tested fallback. |

**Important:** this table identifies the owners to inspect, not proof that every capability is qualified for every Eclipse workload. In particular, the public repository must locate and hash-pin the actual M3File owner before a file-layer implementation can be accepted.

## First implementation slices

Keep each slice independently reviewable and reversible.

### E0 — source census and compatibility baselines

- Pin exact commits for M3JDK21, Synexia, SWT, Eclipse Platform, Platform UI, Nebula and GEF Classic.
- Generate an inventory of relevant source, tests, service registrations, extension points, generated code, resources, build metadata and JNI/native declarations.
- Record each candidate as `DISCOVERED`, `CONTRACT_REVIEWED`, `IMPLEMENTED`, `VERIFIED`, `ACCEPTED`, `BLOCKED` or `DEFERRED`; a file name match is not evidence of semantic equivalence.
- Establish upstream behavior with existing project tests and focused differential tests before changing storage.

### E1 — indexed collection-backed models

- Select one concrete high-volume model at a time (for example, a table/tree/selection backing store) only after tracing its factories, identity, mutation, ordering and disposal paths.
- Keep public Eclipse interfaces and plugin extension contracts unchanged.
- Compare existing M3Index/M3 collection owners against the exact donor algorithm; do not introduce parallel indexes or per-row wrapper objects when an existing owner can represent the state.
- Preserve UI-thread rules and concurrency guarantees; primitive atomic lanes do not automatically provide atomic multi-index updates.

### E2 — text-heavy registries and metadata

- Identify text hot paths through source inspection and profiling rather than replacing every String use mechanically.
- Reuse M3String only behind supported JDK/runtime boundaries and exact materialization semantics.
- Include Unicode, locale, normalization assumptions, identity-sensitive code, serialization and JNI cases in the differential suite.

### E3 — file/resource metadata

- Do not start until E0 resolves M3File's real API, source owner, license, recipe, lifetime and mutation contract.
- Prototype only an explicitly bounded metadata/index sidecar. The original file provider remains authoritative for reads, writes, permissions, watching, locking, symlinks and failure behavior.
- Test external file mutation, atomic replacement, truncation, mapped-region bounds, descriptor cleanup, refresh and recovery after invalidation.

### E4 — SWT/native boundary and end-to-end workloads

- Keep native resource handles, OS widgets, Display ownership and disposal protocols intact.
- Test Linux, Windows and macOS where the implementation is platform-specific.
- Measure UI-thread latency and tail latency, allocation, retained heap/native memory, startup, steady-state throughput and regression rates on representative IDE workflows.

## Required M3 recipe invariant

Every production refinement must have a reusable Maven/OpenRewrite transformation recipe where suitable, its focused recipe tests, exact source pins, donor license/provenance, generated postimage hashes, and the product change in the same reviewable PR.

1. Inventory the target file and public/implicit contracts.
2. Select the problem category from the living algorithm catalogue.
3. Compare first-party existing owners and eligible donors; record complexity, correctness, compatibility and license rationale.
4. Apply the recipe **serially, per file**. Do not hand-edit generated postimages to make a recipe pass.
5. Verify exact preimage/postimage seals and recipe fixed-point/drift refusal.
6. Run focused JUnit and differential contract tests; compile with strict Java 21 warnings.
7. For JNI changes, run Java/native parity, `-Xcheck:jni`, native warnings-as-errors and sanitizers, plus old-ABI fallback tests when relevant.
8. Run the owning module and relevant Eclipse target builds; record unexecuted gates as pending, never as passing.
9. Benchmark against the upstream baseline with environment and retained-memory costs included.
10. Keep the PR draft until the evidence for its actual scope is green.

Atomize a source file only where a behavior is independently replaceable and testable. No arbitrary atom-count limit; no fragmentation without a reuse or verification benefit.

## Acceptance checklist for each Eclipse PR

- [ ] Exact upstream and M3/Synexia pins and source-to-target mapping recorded.
- [ ] Existing canonical M3 owners reused; duplicate storage and new public APIs justified or avoided.
- [ ] Java observable behavior and Eclipse extension/plugin contracts preserved.
- [ ] Tests cover boundaries, nulls, exceptions, iteration/order, mutation visibility, disposal/lifetime, concurrency and platform-specific behavior as applicable.
- [ ] Recipe self-tests, deterministic replay and drift refusal pass.
- [ ] Java/JNI parity and old ABI behavior verified when applicable.
- [ ] Owning module and targeted Eclipse builds pass on the exact PR head.
- [ ] Performance and retained-memory results are reproducible and disclose regressions.
- [ ] Remaining gates and unsupported targets are explicitly listed.

## Current limitation

This contract PR deliberately changes no production Java/JNI/Eclipse source. It establishes the boundary for the first implementation slice. It does not assert that the full Eclipse base framework, M3File, or any of the listed Eclipse forks has been fully implemented or qualified by M3JDK21.
