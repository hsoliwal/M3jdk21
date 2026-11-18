<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# MOP SDK core invariant: top-down atoms, bottom-up patterns

**Status:** normative design and coding-agent rule; qualification is required per actual source change. This document does not claim every source file has been transformed or verified.

M3JDK21 is the integrated Java/HotSpot/JNI **product receiver** of Synexia Machine-Oriented Programming (MOP). The canonical recipe implementation, reusable transformation logic, correction memory, source seals, and qualification evidence remain in **hsoliwal/com.synexia**. This product contains only executed and independently tested postimages, original JDK product source, applicable product documentation and product-native tests; no recipe mirrors.

**MOP = Atomize -> Patternize -> Annotate -> Document, with evidence.** Every file must be understood as an inventory of semantically meaningful atoms, higher-level compositions, external contracts, visible effects, known patterns and documented proof obligations. A source file with no safely extractable private atom remains valid and requires a documented NO_SAFE_EXTRACTION result, not a forced rewrite.

**Top-down inventory:** repository -> project -> module -> package -> file -> class/interface -> fields, constructors, methods and initializers -> semantic atoms.

**Bottom-up qualification:** semantic atom -> file -> package/interface contract -> module -> project/reactor -> repository/system/API. Interfaces are cross-cutting contracts rather than a strict containment level; follow resolved callers, effects and native/module/reflection boundaries. Widen the affected set only where verified dependencies demand it.

Independent file-local inventory and candidate evaluation may run **in parallel** once shared contracts are sealed. Each mutable file has a single writer; each successive atom replacement is **serial** and verified. Cross-file, cross-package and cross-module promotion is dependency-closed and proof-gated. Do not silently combine competing branches, remove prior functionality or assume an isolated test proves the whole JDK.

Where appropriate, internal implementation atoms should be **private, static or instance-local** helpers with clear contracts and descriptive Javadoc, and higher methods should compose these atoms. Preserve the actual existing JDK class and method owners, public/protected API/ABI, identity, ordering, exceptions, null semantics, side effects, synchronization, native lifecycle, reflection and serialization. Never force extraction that changes these properties or adds parallel M3 public types.

A coding agent must first retrieve the governing contracts and source/recipe history, identify exact file and atom owners, compare the best *proven* compatible alternatives, and submit its transformation to canonical Synexia Maven/OpenRewrite recipes. The agent must use actual compiler, differential, jtreg/JNI/native and compatibility evidence, when relevant, before any receiving claim. It must fix the recipe and rerun it rather than silently hand-patch a generated postimage. Exact SHA-256, structure/logic hashes and SimHash are discovery and custody signals, not proof of semantic equality.

MOP guides agents toward **the best qualified implementation under declared constraints**, not a universal guarantee of optimal code. A pattern description or generated documentation alone is not proof. Unsupported or unrun gates remain PENDING/NOT_RUN. Reapply a versioned transformation until its bounded deterministic fixed point is demonstrated.

Preserve upstream licenses, notices, donor lineage, full-history best-of-best additive superset rules and the never-rebase-develop directive. Eligible newly authored first-party code may use Apache-2.0; original OpenJDK license obligations remain intact.

**Canonical detailed policy and execution owner:** [SYNEXIA-MOP-SDK-BIDIRECTIONAL-LATTICE-1](https://github.com/hsoliwal/com.synexia/blob/develop/docs/M3-SCALE/invariants/SYNEXIA-MOP-SDK-BIDIRECTIONAL-LATTICE-1.md) (link resolves after accepted Synexia merge). Existing M3JDK21 AGENTS.md and Synexia source-sealed admission rules remain authoritative if a particular receiving gate is stricter. No new product-side recipe execution or correction-memory store is authorized by this documentation.
