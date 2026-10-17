<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 AST sharing: existing compiler ingress

Copyright 2026 Hitesh Soliwal and contributors.

This task implements the attached 17 October 2026 AST-sharing conversation from its latest delivery requirements backwards. Synexia owns the reusable compiler adapters and transformation recipes. M3JDK21 receives exact source custody and the independently built compiler tooling; ordinary JDK interfaces and the global receiving phase remain unchanged.

## Frozen contract and owner inventory

The source baseline is Synexia `e244d68fdee88d4a406be48e4ff1d31b60a17988`; the initial receiver baseline is M3JDK21 `e336c083c90bd882030e9a370949136e191386f2`. The final compiler templates compose with the newer receiving owner at `4e29dc9d4141261e8763fbe9ffd6a20e017b1aff`. `MIndexJavaAST` already converts an existing javac unit through its private `capture` method. Its scoped `MIndexASTPool` is the owner used here; the unrelated process-global `com.synexia.mindex.MIndexAST` and indexstring AST family are not interchangeable.

The current receiver supplies `com.sun.source.util.M3AST`, the `M3ASTPC` plugin, exact parser-input retention and the primitive-image consumer. All of those interfaces and methods are preserved. The recipe adds independently closeable immutable-index observers and the primitive command's before-parse callback. The external plugin is named `M3ASTIndex`, preserving the existing built-in `M3AST` name and options. `UPSTREAM_COMPILER_COMPOSITION.json` binds the exact upstream sources; primary and recovery manifests qualify both current upstream and the previously generated local candidates without patching product outputs by hand.

The public parse overloads retain their source, diagnostics, release, pool and exception behavior. The additional existing-unit ingress never calls `parse`, `analyze` or creates another compiler task. Consumers receive the actual task's tree only during the corresponding callback. Immutable snapshots retain source spelling and tree getter structure. They do not claim full OpenRewrite LST equivalence or that structural hashes establish semantic equivalence.

`M3File` freezes compiler input before parsing. `M3CompilationContext` binds the ordered options and dependency/processor/generated-source inputs by SHA-256. A random session identity prevents accidental semantic reuse where the complete external context has not been supplied. A new source generation requires a new context. Syntax is frozen at PARSE; attribution is offered per ANALYZE type, with incomplete and failed compilation states explicit. Live compiler trees, symbol tables and classloaders are never persisted to SQLite.

Pluggable projections use the existing `TieredSwapCache` through `M3ASTWorkspaceCache`: leases pin active values, keys bind source/context/provider/schema/phase, and spill/reload retains exact immutable content identity. Callback-scoped tree access is invalid after its callback and the session releases task references when closed. There is no hidden global compilation cache.

## Validation authority

Run the class-backed Maven/OpenRewrite snapshot recipe; prove input drift refusal, unchanged replay, exact source output, and the Java compiler/JUnit probes. Compile generated receiving sources, not stand-ins. Tests cover parse-only and analyze-only lifecycle, multiple top-level types, generated sources, source mutation, Unicode and string-folding structure, duplicate consumers, lazy projection, context isolation, codec corruption and actual SQLite JNI reopen.

Packaging and fork smoke tests are separately recorded. Resource measurements are finite workload evidence. A complete JDK build, all plugin compatibility, cross-platform behavior, full lossless OpenRewrite projection and whole-JDK phase promotion cannot be inferred from these tests.

## Explicit hierarchy contract

| Level | Meaning |
|---|---|
| L0 | Per-file facts and atoms |
| L1 | File summary |
| L2 | Package |
| L3 | Module |
| L4 | Project |
| L5 | Repository |

The `hierarchy/` recipe applies this mapping in the canonical MIndex and receiving M3 counterparts. It preserves the existing partial-AST parser scopes as a separate concept. Its topology uses child offsets and child indices, and its immutable identities bind the admitted leaf and dependency identities before parent results are reused. The hierarchy qualification describes the exact lazy, snapshot, budget and invalidation boundaries.

## Optional conversion and catalogue

The separate `openrewrite/` and `module-wiring/` crates reuse the stock OpenRewrite 8.17.1 Java 21 visitor against the original javac unit. Their finite proof includes full-FLOW admission, exact printing, shared types, unchanged original class output, source completion reentry refusal and operation with no Rewrite dependency in the core module. It establishes this specific compatible adapter, not equivalence for every parser, visitor or plugin.

`CATALOGUE.tsv` records category-level donor decisions; `FILE_REVIEW.tsv` links each reviewed operation to its contract and evidence. The nested crates add their own operation catalogues. These tab-separated records can be searched directly by category, source filename or operation and retain rejected approaches alongside selected owners.

## Source and license custody

New compiler adapters and reusable Synexia owners retain their Apache-2.0 headers. The existing JDK compiler sources and the target-owned compiler seam retain their GPL-2.0 with Classpath exception notices. Exact JDK license and exception texts, with Git blob pins, are under `licenses/`; recording those sources in recipe templates does not change their licenses. The independent receiving source mirror retains the existing Synexia license and notice files.

The public recipe packet includes its original `source-pins.json`, exact generated dependency candidates and the cache's ABI-only declaration custody. `build_candidates.py` builds only actual SDK candidates, rejects conflicting source identities, and does not depend on an unpublished private checkout for compiler input.
