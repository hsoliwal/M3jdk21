<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Build-tool reuse inside M3JDK21

M3JDK21 owns the admitted file, text, AST, compiler and reusable build mechanics.
The Maven and Gradle forks, IDE builders, Ant, Tycho/m2e and recipe consumers
reuse those capabilities through their existing owners. Preserve each tool's
configuration language, lifecycle, plugin contracts and dependency semantics.
The objective is more useful build functionality with less duplicate state,
reading, hashing, parsing, analysis and rebuilding.

## One admitted generation, multiple consumers

1. The original file/snapshot owner establishes a stable input generation.
2. Retain its content identity once. Watcher events nominate files to recheck;
   timestamps, sizes and an absence of events do not certify content identity.
3. The authoritative parser publishes syntax and source coordinates once for
   each source generation and compatible parser configuration. Other consumers
   project the existing result. Mutable compiler trees and symbols remain with
   their original task and compilation context.
4. The compiler publishes dependency and lookup facts, constant/annotation
   dependencies and source-to-emitted-output ownership during its actual work.
5. The existing build planner determines the affected workset and reuses valid
   outputs. Publish new dependency/output facts after successful work, preserving
   transactional rollback and stale-output deletion.

Unchanged dependency `.class` files do not require reading or parsing their
sources merely because another source changed. Existing decoded binary facts
remain reusable for the same admitted content and schema. Bindings and complete
outputs additionally depend on compiler, classpath, module and task context.

This does not make one mutable Java AST object shareable between JVMs. Cross-process
reuse requires an admitted immutable representation with schema, compiler/provider,
source and context identities. Do not serialize/reparse text or JSON to manufacture
a second syntax authority. File/package/module/project scopes select existing
work; they do not replace dependency analysis.

## Separate four identities

| Identity | Contents and reuse boundary |
| --- | --- |
| Raw content | Exact observed bytes and length, independently of timestamp; source and binary file inputs use the same boundary. |
| Syntax | Decoded source generation, encoding policy, language/dialect, parser/provider/schema and syntax options; source coordinates remain generation-specific. |
| Semantics | Syntax plus ordered class/module paths, module graph, release/toolchain/options, processors/plugins, symbol and lookup environment. |
| Build result | Complete task inputs, implementation/configuration/environment, dependency facts and owned output manifest. |

A `.class` content digest is not its compile interface. A source digest is not a
semantic context. A POM/build-script generation is not the complete evaluated build
model: profiles, settings, properties, plugin bytes and configuration inputs matter.
Maven model reuse and Gradle configuration-cache reuse must preserve their own
input validation. User plugin side effects are never silently skipped from a Java
source comparison.

## Delta decisions

| Change | Required decision |
| --- | --- |
| Stable admitted inputs and intact outputs unchanged | Reuse valid syntax, binary facts and task outputs; no compiler invocation for an already current compile task. |
| Timestamp changed, bytes unchanged | Do not infer a content change from timestamp alone. |
| Same size/time, bytes changed | Detect content change; metadata equality cannot authorize reuse. |
| Comment or whitespace change | Update source geometry; assess diagnostics, documentation, debug tables and source-sensitive consumers. Do not globally discard the edit. |
| Method body change, qualified compile interface unchanged | Compile the changed unit as needed; dependent compilation may remain valid. Runtime tests and packaging still see changed class bytes. |
| Signature, hierarchy, visibility, overload/type lookup, annotation or constant change | Rebind/recompile the affected dependency/lookup closure. Previously unsuccessful lookups and inlined constants require edges or conservative invalidation. |
| Processor/options/resource change | Respect isolating/aggregating/nonincremental processing contracts and generated-source/resource ownership. |
| Source deleted or renamed | Remove its old nested/anonymous/generated outputs using the source-to-output map and update affected dependents. |
| Binary bytes changed | Analyze the new binary generation once; separately classify compile-interface and runtime effects. |
| Same source/binary content, different compiler/context | Requalify semantic/output reuse; immutable content facts can survive. |
| Resource-only change | Run affected resource/package/runtime consumers; compile Java only if declared processor/generator dependencies require it. |
| Missing/tampered outputs, watcher overflow or incomplete history | Restore verified outputs or revalidate affected work; never turn uncertainty into unchanged. |

Prefer existing primitive file/class IDs, offset ranges, bit masks and indexed
dependency planes. Reuse admitted array/index owners rather than adding a retained
object for every edge or another process-wide graph/cache. Keep cold facts bounded
and preserve canonical identities if optional derived facts are evicted.

## Existing receiver owners

**Maven.** `M3AstCompilerConfiguration` in `impl/maven-core` already preserves the
original compiler goal and adds the existing M3AST argument. Model, reactor and
compiler-plugin work selection happen elsewhere. Feed admitted source/class and
context facts to those original planning owners; adding a plugin argument alone
does not implement compile avoidance. Retain goal-specific resource/test/package
inputs and the existing build-cache contract.

**Gradle.** Retain `M3ASTCompilerArguments`, `M3JavacASTTask`, `JdkJavaCompiler`,
the M3JDK21 `M3GradleASTProvider`, and Gradle's original incremental listeners.
Planning receivers belong in the existing `JavaRecompilationSpecProvider`,
`CurrentCompilationAccess`, class/classpath analysis and successful-result storage
paths. Gradle already caches class analysis and performs selective compilation;
share admitted M3 facts with those owners. Preserve constant dependencies,
annotation processors, source/output mapping, backups and configuration-cache
lifecycle. Existing worker API/bytecode compatibility remains a receiving gate.

**Other forks.** Apply the same ownership rule to Ant tasks, Tycho/m2e and IDE
builders. Inventory their actual planners and input contracts before adding a
receiver. This document does not claim runtime changes in those forks.

## This receiving change and acceptance

The original `com.m3.file.M3File` now offers `fingerprint(long)` and
`contentDelta(previous, current)` as the raw-content primitive. It retains five
long fields per immutable observation and hashes with bounded scratch space.
Comparison performs no reading, hashing or parsing. Null means independently
known absence; I/O failure propagates. Detected concurrent metadata changes are
refused, while filesystem atomicity still belongs to input admission. Hash
equality remains a content-addressing signal, not exact semantic proof.

This is a bounded capability in the existing M3JDK21-owned `com.m3.file` module.
It neither wires every build planner nor certifies a complete source-built JDK.
The independent contract runs against the source-built module on the host and
in a linked image; real javac fixtures preserve an unchanged dependency class
and expose the need to recompile users of changed inline constants. The Maven
and Gradle companion changes document precise receiver responsibilities.

Future planner acceptance must demonstrate actual no-op and one-change builds,
parse/read counters, dependency and lookup invalidation, stale-output cleanup,
processor resource changes, changed compiler context and valid cache restoration
through the original tools. Record cold/warm CPU and retained heap/native memory
separately; operation counts alone do not establish a speedup. Exact-head hosted
workflows, broad fork compatibility and full product gates remain explicit.

## Primary references and provenance

- [Gradle Java compilation and avoidance](https://docs.gradle.org/current/userguide/java_plugin.html).
- [Gradle configuration cache](https://docs.gradle.org/current/userguide/configuration_cache.html).
- [Maven compiler incremental policies](https://maven.apache.org/plugins/maven-compiler-plugin-4.x/compile-mojo.html).
- [Maven build-cache model](https://maven.apache.org/extensions/maven-build-cache-extension/concepts.html).
- [Existing product mapping](name-mapping.d/m3file-build-delta-20261020.json).
- Canonical source-sealed recipe dataset: `hsoliwal/com.synexia`,
  `synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017/datasets/file-content-delta-20261020`.

Existing JDK/public/provider APIs keep their names and contracts. Reusable recipe
authority stays in Synexia; runtime/product authority stays in M3JDK21. The
repository-wide receiving sequence and pending family gates are unchanged.
