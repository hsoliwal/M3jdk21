<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# M3File deltas for M3JDK21 build services

M3JDK21 owns the shared file, source and compiler facts used by its Maven, Gradle
and Rewrite forks. The objective is to retain valid work across builds, compute
the affected dependency closure, and avoid repeated reads, parsing and analysis.
Build-tool models, lifecycle ordering, task contracts and classloaders remain
authoritative. Synexia remains the convergence and reusable-recipe owner.

## Scope and evidence

[FORKS.tsv](FORKS.tsv) records four inspected forks and immutable revisions.
The [existing-owner naming fragment](../../../../docs/name-mapping.d/m3-file-build-delta-20261020.json)
records the bounded additions in the established M3JDK21 mapping directory.
Existing owners were inspected at M3JDK21
[`d6d76b563050fc13c4fa79bc494edf29f1614b37`](https://github.com/hsoliwal/M3jdk21/tree/d6d76b563050fc13c4fa79bc494edf29f1614b37).

| State | Capability |
| --- | --- |
| Existing source | Maven compiler-plugin activation; Gradle native compiler option and original-task provider; M3AST compiler-session sharing; file/source facts, workspace cache and snapshot store. Source inspection is distinct from runtime qualification. |
| This candidate | Source-range comparison, streaming raw-file fingerprint, source/context change masks, and one shared source-only compiler projection on existing owners. Execution receipts establish which tests actually ran. |
| Pending | ABI extraction, persistent source-to-class ownership, task-planner consumption, cross-build/cross-process reuse, Maven/Gradle lifecycle qualification and measured resource improvements. |

The candidate APIs are:

- `jdk.m3.parser.M3File.changesFrom(previous)` returns
  `SourceDelta(sameUri, start, previousEnd, currentEnd, byteChange)`. The range is
  an exact UTF-16 replacement window, not an AST edit or semantic classification.
  `ByteChange` distinguishes `UNKNOWN`, `MATCHING_SEALS` and `DIFFERENT_SEALS`.
- `com.m3.file.M3File.fingerprint(long maxBytes)` returns
  `Fingerprint(long size, String sha256)` from a bounded streaming read through
  the original NIO provider with no-follow file access. It also applies to
  `.class` bytes; it neither validates the class format nor extracts an ABI.
- `com.m3.compiletime.ast.M3FileSourceFacts.changesFrom(previous)` returns
  independent source-URI, source-content, context, provider, schema and phase
  bits. Equal source text does not erase a changed compiler context.
- `jdk.m3.parser.M3JavacParser.sourceProjection()` exposes one existing-event
  operation key shared by source and tree consumers.
  `M3JavacParser.changesFrom(unit, previous)` obtains source delta evidence
  without constructing a tree index. The original source seal and callback
  lifetime checks remain in force.

SHA-256 seals are content evidence, not mathematical proof of collision absence.
Source-range equality uses actual UTF-16 units. Metadata and watcher events are
change hints; the existing file readers do not promise an atomic snapshot of an
externally changing filesystem. Callers need a stable input generation when
publishing build results.

## Existing owners and reuse points

| Owner | Responsibility |
| --- | --- |
| `com.m3.file.M3File` | Original provider-owned `Path`, refreshed metadata, bounded reads and raw byte fingerprints; no directory object graph. |
| `jdk.m3.parser.M3File` | One immutable String source payload, exact UTF-16 coordinates, separate optional file-byte seal and source deltas. |
| `M3FileSourceFacts` | Detached source identity plus compilation context, provider, schema and phase. Its domain-separated digest format remains distinct from the parser's source digest. |
| `com.sun.source.util.M3AST` | Facts from the actual compiler task. Live compiler views are event-scoped; detached immutable projections can outlive the task under complete source/context identity. |
| `jdk.m3.parser.M3JavacParser` | Source-only and tree consumers share one immutable M3File per event through M3AST's existing memoizer. A static operation key retains no source or task. |
| `M3ASTWorkspaceCache` | Existing context-bound deferred projections and tiered storage. Retain its ownership, leases and eviction policy. |
| `com.m3.fs.M3SnapshotStore` | Existing namespace, format, dictionary, SHA-256 and generation-qualified immutable blobs and published roots. Reuse its publication contract for future detached build facts. |

Relevant source roots are `m3/file`, `m3/tooling/module-packs/parser`,
`m3/ports/compiletime`, `src/jdk.compiler`, and `m3/fs`. This work introduces no
second cache framework, filesystem or mandatory Synexia runtime dependency.

Maven's existing `M3AstCompilerConfiguration` activates the compiler-owned plugin.
Gradle's existing `M3ASTCompilerArguments`, `M3JavacASTProvider` and
`M3GradleASTProvider` retain the original `JavacTask`, processor path, incremental
collectors and cleanup. Provider assets already participate in Gradle's input
fingerprint. Compiler callbacks run only when compilation runs: an up-to-date or
cache-restored task creates no new callback facts.

An existing `M3AST.Consumer.parsed(Unit)` can request the source-only projection
when its explicit assets include the optional parser module:

```java
M3File current = unit.project(M3JavacParser.sourceProjection());
M3File.SourceDelta delta = M3JavacParser.changesFrom(unit, previous);
```

`previous` is a real caller-owned immutable snapshot. Repeated source requests
and tree requests share the same per-event source object; no tree index is
constructed for source-only use. The existing Maven plugin and Gradle provider
can deliver this callback without a new SPI. This does not retain live compiler
objects or admit reuse across tasks automatically.

For later Gradle planner work, reuse `DefaultInputFingerprinter`,
`WatchingVirtualFileSystem`, `CachingClassDependenciesAnalyzer`,
`CachingClassSetAnalyzer`, `CompilerApiData`, `PreviousCompilationData`, and
`AbstractRecompilationSpecProvider`. Rewrite consumers must retain language,
source-set, classpath and recipe semantics when admitting a shared projection.
Java compiler facts alone do not implement Maven model building or Gradle's
Groovy/Kotlin configuration model.

## Required task decisions

This matrix specifies the future planner contract; the narrow APIs do not
perform these decisions automatically.

| Observed change | Required response |
| --- | --- |
| Source, context and required outputs unchanged | Reuse qualified facts/results under the tool's existing input contract. |
| Source characters changed | Reparse the affected source where necessary; retain exact diagnostics, encoding and source positions. A range alone cannot justify semantic reuse. |
| Comment/whitespace edit | Refresh source-sensitive documentation, processors and diagnostics. Skip compilation only when a qualified policy proves all relevant outputs unaffected. |
| Method body changed; compiler-visible surface unchanged | Compile the owning source; downstream compilation may be avoided. Runtime tests and packaging still observe changed class bytes. |
| Accessible signature, annotation, module or type hierarchy changed | Invalidate the applicable reverse compilation-dependency closure. Package and protected visibility matter too. |
| Compile-time constant changed | Use compiler-recorded constant dependents, including inlined uses; conservatively widen when evidence is missing. |
| Source/class removed or renamed | Remove all owned stale outputs, including nested classes and generated resources; invalidate affected references and unresolved lookups. |
| Output missing, corrupt or replaced | Restore a validated result or rerun its producer even when source is unchanged. |
| Compiler/options/classpath/processor/model context changed | Invalidate context-dependent facts and tasks. Source equality alone cannot retain semantic analysis. |
| Compilation failed or watcher history lost | Do not publish a successful generation; preserve the tool's rollback/revalidation and conservative fallback. |

## Build phases and compact state

1. **Observe:** retain verified source/file generations, detect added/removed
   inputs and revalidate uncertain watcher state.
2. **Resolve:** keep Maven's effective model and Gradle's configuration/task
   models. Record compiler versions, ordered options, release/encoding,
   classpath/module-path contents, processor assets, source sets and model inputs.
3. **Compile and compare:** share the actual compiler facts; associate generated
   classes/resources with their producers; compare raw class bytes and separately
   qualified ABI/constant facts. Propagate invalidation until stable.
4. **Publish:** publish detached facts and output ownership only after successful
   completion, with context/schema identity and a consistent generation. Restore
   facts together with cached outputs. Preserve declared side effects.

Future dependency storage uses the existing dynamic primitive-array owners:
integer file/class/task IDs, offset ranges, contiguous adjacency arrays and
dirty/known-state masks. No retained object per edge is required. Unknown evidence
remains explicit. Independent work may run in parallel with deterministic fan-in;
per-file recipe passes remain serial. Pressure may evict derived facts while
preserving live owners and safely regenerating or reloading them.

## Donors and qualification

Use the inspected Gradle snapshot, incremental compiler, constant-mapping and
configuration-cache implementations as donor categories. Maven model/lifecycle
and build-cache mechanisms remain distinct. Official references:
[incremental builds](https://docs.gradle.org/current/userguide/incremental_build.html),
[Java compilation](https://docs.gradle.org/current/userguide/java_plugin.html),
[file watching](https://docs.gradle.org/current/userguide/file_system_watching.html),
and [configuration cache](https://docs.gradle.org/current/userguide/configuration_cache.html).
These document existing Gradle behavior; no claim is made that every build
currently reparses or recompiles everything.

Reuse canonical `M3HashPinnedJavaSnapshotRecipe` and the existing hash-pinned text
recipe for this task crate, retaining exact preimages/postimages, provenance,
refusal and fixed-point evidence. No new transformation engine is needed.

The focused fixture plan uses actual javac to compare source and generated class
changes: unchanged content, same-size edits, method bodies, constants, multiple
classes, deletion and failed compilation. It separately checks UTF-16 seams,
unknown byte seals, read limits and context-mask changes. Actual Maven reactor,
Gradle daemon/worker, configuration/build-cache, generated-source and full
source-built distribution gates remain pending until executed. Benchmark cold
and warm CPU, reads/parses, retained heap and process/native memory before any
claim that the resulting tools are lighter or faster. Existing ordered receiving
gates remain in force; this candidate advances no whole-family acceptance phase.

## Reproduce the bounded API qualification

From the M3JDK21 checkout, with an actual Java 21 JDK:

```sh
python3 m3/tooling/module-packs/build-tools/delta/verify.py \
  --repo-root . --jdk-home /path/to/jdk21 \
  --baseline-ref d6d76b563050fc13c4fa79bc494edf29f1614b37 \
  --out /path/to/new/delta-proof
```

The runner freezes and compiles the actual three owners and four contract programs,
checks preservation of their original public declarations, and records exact source
hashes and test outputs. It refuses nonempty output directories and source drift
during qualification. Declaration preservation is not a whole-program binary or
behavioral compatibility proof. The parser's
existing `build.py` also executes `M3FileDeltaContract` and the real-compiler
`M3SharedSourceDeltaContract` on its host and linked module image. The latter
checks shared object identity and operation counts, source-read counts, class
bytes, diagnostics, separate tasks and expired/non-PARSED view refusal.
The existing parser workflow includes an explicit job for the standalone delta
verifier as well. These are bounded tests; running them does not execute Maven
or Gradle's scheduling/cache lifecycle.

The legacy compiletime snapshot-custody gate is already inconsistent at baseline
`d6d76b563050fc13c4fa79bc494edf29f1614b37`: its manifest lists 29 Java paths while
the source tree has 45. The unchanged gate refuses this baseline. This task's
canonical dataset seals its exact changed owners/tests; it does not waive or
repair the separate legacy snapshot/reconciliation gate.
