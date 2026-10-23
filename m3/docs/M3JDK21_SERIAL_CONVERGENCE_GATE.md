<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 integrated source-convergence receiving gate

Status: review-only product acceptance record; NO Java, HotSpot or JNI product postimage in this PR.

M3JDK21 is one integrated JDK, not M3 plus JDK. Public String, ArrayList, HashMap, TreeMap, Pattern, Matcher and Java SE collection contracts retain standard names, descriptors and behavior. Internal M3 dynamic Object[] and primitive array lanes, physical slot/offset/index directories, masks, generation and bit-state owners remain implementation atoms inside the original JDK owners. They are not an exported replacement API, parallel wrapper engine or Synexia runtime dependency.

## Canonical producer and source boundary

The reusable Synexia recipe and its memory remain exclusively in hsoliwal/com.synexia/synexia-openrewrite-recipes.
Canonical source review: https://github.com/hsoliwal/com.synexia/pull/13209 (draft; SDK proof pending). The additive read-only recipe com.synexia.rewrite.M3Jdk21MigrationStageRecipe invokes M3Jdk21MigrationStageGate on a source-sealed stage ledger.

This recipient's only authoritative capability naming registry is m3/docs/name-mapping.json migration.records[].id. Reuse the existing Synexia name-adaptation, owner-naming, hash-pinned snapshot, M3JdkHandoff and category-catalogued behavioral atoms owners. Do not copy recipe source/YAML, task crates, templates, algorithm catalogues or improvement memory into M3JDK21.

A source producer must prove, for each stable capability ID, the strict order:

CONTRACT -> PACKAGE_IMPORT -> MODULE -> CLASS -> API -> DOCUMENTATION -> VERIFY -> PROMOTE

CONTRACT documents the current API/ABI, ownership and full caller/reflection/serialization/JNI closure. PACKAGE_IMPORT uses attributed changes and requires closed import/caller updates. MODULE reconciles module-info, native symbols, services and build metadata while preserving standard JDK module identities. CLASS changes only approved internal names. API preserves source, binary and protected contracts without a separate unlock. DOCUMENTATION converges Javadoc, package/module docs and durable semantic lineage. VERIFY authenticates diff, lint, compiler, tests, actual source-built M3JDK21 image, jtreg/JNI/GC/JMM/HotSpot runtime, fixed-point replay and benchmark evidence. PROMOTE is a separately reviewed product-receiving event and contains no source mutation.

The stage ledger requires before/after SHA-256, unchanged public API fingerprints, stable target module and a receipt for every claimed PASS. A predecessor that remains HOLD, NOT_RUN or FAIL cannot precede a PASS. Mandatory CONTRACT, VERIFY and PROMOTE cannot be NOT_APPLICABLE. Structural gate success does not authenticate external evidence and does not authorize a runtime switch.

## Filtered and sorted collection contracts

Current M3JDK21 implementation work already exists under its original java.base owners and on draft PRs including #1802 and #1804. Reconcile exact heads before applying another change; do not duplicate an indexed-array/JNI implementation or invent an external compatibility layer.

Preserve the distinction between backed/live filtered wrappers and detached sorted snapshots. Preserve stored representatives under comparator equivalence, nulls and duplicates, Java predicate callback order/exceptions, comparator tie stability, descending and bounded range navigation, mutation visibility, iteration/spliteration, fail-fast and serialization contracts.

Prefer one authoritative GC-traced Object[] owner, stable logical/physical int[] slot directories, generation and state-mask lanes and indexed stable sorting. API-required Entry/iterator objects may be transient projections, never the retained storage substrate. JNI handles only validated primitive mask/state/index values; Java retains all references, callbacks and applicable memory semantics. Reuse the currently qualified JNI owners.

## Current verification boundary

- Existing source/naming/catalogue authorities: INSPECTED.
- New isolated Java 21 gate, javac --release 21 -Xlint:all -Werror: PASS.
- New deterministic Java 21 gate oracle: PASS (15 assertions).
- OpenRewrite scanner compilation: only local structural-stub compilation, not SDK_EXECUTION.
- Actual Maven/OpenRewrite source recipe execution against current M3JDK21: NOT_RUN.
- Current PR stack source and branch reconciliation: NOT_RUN.
- M3JDK21 source-built java.base, HotSpot, jtreg/JCK, JNI/GC/runtime and platform tests: NOT_RUN.
- Before/after CPU, retained-heap, allocation and native/process-memory comparisons: NOT_RUN.
- JDK implementation promotion/merge: HOLD.

This receiving record neither widens public APIs nor changes String -> arrays -> collections -> AST/compiler admission order. Product runtime promotion requires a later proof-complete postimage PR.
