<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# Shared AST and custom build requirements: separate source status

This document keeps the attachment's broader requirements visible alongside the
finite wrapper pass. The observations below were collected on 2026-10-18. They
come from fresh GitHub metadata, the exact README/source revisions linked here,
and the source comparison explicitly described for Maven. **No compiler/build
runtime test was rerun by the catalogue author.** Recorded PR proof counts are
historical results for those pinned tested artifacts, not current wrapper receipts.
`AST_BUILD_STATUS.tsv` contains searchable per-requirement statuses and remaining gates.

## Existing receiving work

| Requirement | Existing evidence | Current disposition |
| --- | --- | --- |
| Actual javac task/tree shared by consumers and primitive-image compiler | [M3JDK21 #1086](https://github.com/hsoliwal/M3jdk21/pull/1086), head `1489e102d5edcb4d66bfa5c99615d7d0157f59d7`, merged `d47a4b4fdb8e3794ec56768b06aa9e23730c6b8e` | Existing pinned implementation record; reconcile current compiler sources and reproduce their applicable gates. |
| Lazy isolated detached M3 index, context/source identities, LRU leases/SQLite JNI, L0-L5 hierarchy | [M3JDK21 #1192](https://github.com/hsoliwal/M3jdk21/pull/1192), head `215f6b9b4bb0ffbd9539106e6e8e394105933138`, merged `f4fbe3333946366964e1ea05697eaf48421ddb02` | Existing receiving record; current whole-tree/runtime acceptance is separate. |
| Native Gradle opt-in and selected provider | [Gradle #1](https://github.com/hsoliwal/gradle/pull/1) and [#3](https://github.com/hsoliwal/gradle/pull/3), latter head `7108faa007c42341c5ce103ed42d96d942b831bc`, merged/master `419a77cf61af3924f60678510ed2440230047671` | #3 records source-built selected compiler tests and normal compiler fixtures. Phased attribution/OpenRewrite has separate runtime evidence requirements. |
| Maven plugin session-service selection for shared provider | [Maven #2](https://github.com/hsoliwal/maven/pull/2), tested head `c1afa5f5d749603af41470ca75bc7797b1ec1a92` | Its service-binding implementation is absent from inspected master despite merged status. Recover through a reconciled recipe before crediting current-master functionality. |
| Independently selected complete Maven/Gradle/Ant assets and JMOD closure | [Tool-pack owner](https://github.com/hsoliwal/M3jdk21/blob/1489e102d5edcb4d66bfa5c99615d7d0157f59d7/m3/tooling/module-packs/build-tools/README.md) | Existing packaging source record; each real tool/distribution/source-fork/consumer runtime gate is separate. |
| Complete OpenRewrite runtime pack/shared recipe CLI | #1086 body and tool-pack owner | The inspected packet calls this follow-up work. Later current source/CLI completion has not been established by this catalogue pass. |

## Confirmed Maven ancestry-only loss

The [inspected master commit](https://github.com/hsoliwal/maven/commit/bd11c44cd98aef16a3d049db0ebd8477608ccf4d)
retains the previous master tree while keeping PR #2 as an additional parent. This
is source loss relative to the reviewed service-binding fix, not merely an uncertain
PR label. The exact product file is:

`impl/maven-core/src/main/java/org/apache/maven/plugin/internal/DefaultMavenPluginManager.java`.

| Source | Git blob | Observed content |
| --- | --- | --- |
| #2 head `c1afa5f5d749603af41470ca75bc7797b1ec1a92` | `61b405090554188af5ed271460c324fa56604b5b` | `InjectorImpl` plus `bindSessionServices(...)` inspects binding metadata, keeping suppliers lazy and avoiding the specific default-priority tie. |
| [Master `bd11c44cd98aef16a3d049db0ebd8477608ccf4d`](https://github.com/hsoliwal/maven/blob/bd11c44cd98aef16a3d049db0ebd8477608ccf4d/impl/maven-core/src/main/java/org/apache/maven/plugin/internal/DefaultMavenPluginManager.java) | `3d3aff471624af4daf4bff3ca95ed285e6a05dda` | `Injector.create()` and unconditional `services.forEach(...bindSupplier...)` remain. Master independently retains `M3AstCompilerConfiguration.configure(...)`. |

A correction must preserve the current compiler-configuration atom while restoring
and requalifying the lazy binding fix. The #2 body reports ten dedicated binding
contracts and a source-built shared-compiler fixture for its tested head. Those
receipts do not establish the behavior of the different current-master source.
This wrapper packet changes neither Maven source nor its publication state.

## Preserve the precise AST boundary

The [builtin frontend owner](https://github.com/hsoliwal/M3jdk21/blob/1489e102d5edcb4d66bfa5c99615d7d0157f59d7/m3/tooling/shared-frontend/README.md)
keeps live javac units callback-scoped. Projection **key instance identity** controls
memoization; equal display names do not authorize sharing. The
[detached hierarchy receiver](https://github.com/hsoliwal/M3jdk21/blob/215f6b9b4bb0ffbd9539106e6e8e394105933138/m3/compiler/ast-sharing-20261017/README.md)
adds a separate `M3ASTIndex` with context, provider, phase, schema and source seals.
MIndex/M3 atom facades, public compiler views and detached source/cache formats are
not automatically interchangeable. The source map must reflect those real owners.

The reviewed OpenRewrite 8.17.1 routes both reuse its actual Java 21 visitor, but
their admitted phase policies differ. The builtin frontend route documents `byfile`
or explicit parse/analyze/generate phases; the detached optional module documents
`simple`, no string folding, completed FLOW state and reentry refusal. Preserve
these exact owner-specific preconditions. Expression structure such as `"a" + "b"`,
type bindings, comments/printing, source generations and processor context need
actual differential proof. A print-equal folded tree is not recipe equivalence.

Only established detached syntax/primitive images cross process boundaries in
these reviewed packets. They do not establish a general attributed semantic-cache
format. Complete compiler/options/dependencies/processors/generated-source context
is needed for any wider reuse. Weighted LRU and active-value leases bound admitted
cache residency; they do not bound javac's live heap or a client's retained objects.

Maven plugin realms, Gradle workers/toolchains, annotation processors, incremental
collectors and diagnostic identities remain product contracts. JMOD selection and
retained launchers make tools available; packaging alone never proves that their
consumers reused one parse or attribution. Count source reads/parses/conversions
and measure full build cost against matched workloads before making speed claims.

## Relationship to this delivery

The wrapper recipe is a separate finite review unit. Its Java/JNI source, tests,
source seals and current correction evidence must be qualified without implying
completion of all compiler/build requirements above. Broader pending gates remain
explicit, including current-tree source reconciliation, portable semantic reuse,
phased Gradle/OpenRewrite runtime proof, Ant custom source-build scope, final
Rewrite CLI/pack closure, complete JDK tests and matched performance measurements.
