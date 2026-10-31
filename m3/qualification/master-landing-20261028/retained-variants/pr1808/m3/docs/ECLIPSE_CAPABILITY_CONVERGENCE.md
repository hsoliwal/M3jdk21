<!-- SPDX-License-Identifier: Apache-2.0 -->
# Eclipse capability convergence into M3JDK21

Status: design and qualification backlog; no implementation or performance claim is made by this document.

## Authority and design invariants

M3JDK21 is the JDK-derived product, **not** an M3 sidecar attached to stock Java. Existing JDK API names and Eclipse extension contracts stay intact unless explicitly versioned. Synexia is the recipe/donor convergence workspace, not a runtime dependency. Reuse existing M3 owners and `m3/docs/name-mapping.json`; any new public or internal name requires a reviewed mapping record.

All production transformations must be produced by tested Maven/OpenRewrite recipes, then verified against the unmodified contracts. Do not manually patch transformed production outputs. Preserve donor attribution and license records. No performance or compatibility claim without reproducible evidence. Do not rebase the append-only Synexia develop evidence branch.

## Source-derived capability inventory

| Eclipse donor | Underlying capability | Candidate canonical M3 owner | Contract oracle / must-not-break |
| --- | --- | --- | --- |
| EFS / Resources | URI/file providers, resource lifecycle, visitors, markers, tree deltas | M3FS / M3File | java.nio.file SPI, Eclipse IFile/IResource, path and provider semantics |
| Core Jobs | priorities, scheduling rules, cancellation, progress, worker pools | JDK executor/scheduler internals with M3 job owner | ExecutorService, Future, interrupt and Eclipse Job scheduling semantics |
| Runtime | progress and nested work / cancellation | M3 progress implementation with compatibility facade | IProgressMonitor/SubMonitor; no fabricated completion |
| SWT / JFace | widget lifetime, virtual viewers, event dispatch, UI thread affinity | M3 indexed UI model and viewport primitive | SWT dispatch, disposed widgets, identity, ordering, listeners |
| EMF / Databinding | containment, notifications, observable lists and maps | M3 indexed observable collections | EMF Notification, EList, inverse relationships, null and ordering |
| JDT | parser AST, binding, indexes, refactoring, incremental rebuild | M3AST / M3Index / compiler internals | javac, JDT AST/bindings, incremental compile correctness |
| Compare / Team | snapshots, history, three-way diffs | M3File version/delta and semantic atom DAG | merge-base, rename/move, content and history preservation |
| Search / Text | rope/slices, matching, highlighting and editing | M3String / M3Index / M3Regex | UTF-16 indices, regex semantics, mutation and offsets |
| Equinox / PDE | modules, services, plugins and lifetimes | JDK module/service machinery where compatible | OSGi resolution and service lifecycle; do not conflate JPMS with OSGi |
| m2e / Buildship | dependency analysis, incremental execution, source mapping | M3File + M3AST + build DAG | Maven/Gradle dependency, compiler and cache invalidation rules |

These are **capability hypotheses**, not assertions that every Eclipse subsystem is absent from Java or interchangeable with JDK built-ins.

## Implementation order: additive, chronology-aware

1. **Inventory and mapping recipe**: read pinned Eclipse donor revisions and target current-tree owners; produce donor-to-target registry, gaps, existing implementations, license/attribution and a no-op proof. Refuse unreviewed conflicts with existing names.
2. **M3File delta recipe**: one streaming file-tree scan yielding stable identity, content hash, path/provider metadata and precise changed/added/deleted/moved candidates. Represent snapshots as compact indexed lanes with bounded collision verification. Distinguish unchanged content from metadata changes and resource model events.
3. **Jobs/progress recipe**: adapt callable/runnable tasks to JDK executors and scheduling rules. Preserve mutual exclusion, cancellation, priority constraints and nested progress. Never silently map cancellation to success.
4. **Observable collections recipe**: reuse existing dynamic arrays, indexes, offsets and masks; add compact mutation event journaling and ordered projection. Preserve EMF containment, inverse, notification and event-order guarantees.
5. **AST/build recipe**: derive AST invalidation from M3File deltas, create bounded reusable immutable atoms and exact dependency edges; never skip recompilation solely because an approximate hash matches.
6. **UI/viewer recipe**: use compact parent-owned row indices, lazy facade creation, viewport overscan and scoped disposal on UI thread. Maintain stable selection, focus, event ordering and accessibility.
7. **Eclipse adaptation recipes**: transform targeted platform modules one by one using the proven owner and compatibility adapters. Repeat: file → package → module → reactor/API promotion, with serial proof-gated admission.

## Required Maven/OpenRewrite recipe families

- `DiscoverEclipseCapabilityAtoms`: classify usages and emit a deterministic JSON mapping report; read-only first.
- `ReplaceResourceTreeRescanWithM3Delta`: rewrite only cases with verified resource/provider and refresh semantics.
- `ReplaceJobInfrastructureWithM3Scheduling`: operate on typed AST, preserving scheduling rules and interrupt semantics.
- `ReplaceObservableObjectGraphWithM3Lanes`: generated compact owners and event projections, with containment and notification oracle.
- `ReplaceRepeatedJdtParseWithM3ASTFacts`: qualified immutable parse reuse keyed by all semantic context.
- `ReplaceEagerViewerRowsWithM3Viewport`: introduce lazy facade only where exact widget lifecycle and event obligations can be met.

Each recipe needs before/after fixtures, idempotence check, type attribution, rollback trace, donor receipt and qualification record. Run recipes against pinned inputs. Generated output should be reviewed but changes must be made to recipes, not patched output.

## Mandatory verification

- Compile target JDK modules and selected donor Eclipse modules; no transitive Synexia runtime dependencies.
- Differential behavior: JDK and Eclipse public contracts, URI/provider behavior, deletion/rename events, cancellation and nested scheduling, EMF notification order, AST bindings and UI-thread disposal.
- Adversarial cases: Unicode/surrogates, symlinks, collisions, concurrency, ABA/stale handles, resource moves, churn, out-of-memory and cache eviction.
- Performance: allocation/op, retained heap, CPU time, GC pressure, cold/hot incremental rebuild latency, viewport frame budget and JNI crossing cost; compare matched workloads with confidence intervals.
- Gate promotion on exact-head CI, provenance, license review and contract proof. Track `discovered` → `implemented` → `tested` → `qualified` separately.

## First proof slice

Prefer **M3File tree-delta → precise AST invalidation** as the first end-to-end cross-Eclipse proof: it can serve EFS/Resources, JDT, Maven and Gradle without conflating Eclipse APIs. Demonstrate a two-pass scan, deterministic file identity ledger, equality/collision checking, incremental AST invalidation, and compile/test results before promoting further rewrites.

This roadmap does not claim the recipes or transformations above are already implemented.
