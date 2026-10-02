# Whole-JDK execution packets, mapping, evidence and rollout

Status: **implementation handoff; proposed work and gates are not executed results**.
This document completes the [architecture](whole-jdk-architecture.md), [source-root inventory](whole-jdk-inventory.md), [collections](whole-jdk-collections.md), and [text operations](whole-jdk-text-operations.md) specifications. The existing [mapping lifecycle](migration-mapping-lifecycle.md), [worked port decisions](migration-worked-port-decisions.md), [acceptance matrix](migration-acceptance-matrix.md), and [resume guide](../migration/docs/RESUME.md) remain authoritative within their stated scopes.

## 1. Dependency-aware passes and present status

| Pass | Required result | Status of this documentation tranche |
|---|---|---|
| 1. Pin and inventory | Exact target/source/producer pins; all JDK and relevant Synexia source, dependency and owner obligations | Target source-root census and selected source/document inspections completed; recursive file/symbol/native closure OPEN. |
| 2. Bootstrap/dependency map | Subsystem, generated-input, initialization, module, native and runtime-consumer graph | Framework and critical boundaries specified; full graph not produced or verified. |
| 3. Reconcile mapping authority | Preserve retained capability IDs; join private source obligations and branch-scoped manifests | Existing retained target authority identified; competing/lagging records remain review obligations. No operational schema or mapping changed. |
| 4. Minimal foundations | Ownership, IDs/generations, references, ranges, indexes, immutable/mutable lifetime and facts | Specified for implementation through existing owners; no runtime implementation in this tranche. |
| 5. Text and collections | Deep operation/implementation specifications, views, semantic counterexamples | Delivered as documentation; actual declaration-level coverage and implementations remain gated. |
| 6. Remaining JDK | All source roots and subsystem-specific reuse/retention decisions | Every source-root directory receives a preliminary disposition; each still needs capability-level refinement. |
| 7. Independent routes | A/B/C packets, dependency closure, refusal and fallback | Specified below; no route inherits another route's proof. |
| 8. Recipes and tests | Reusable transformation recipes, differential/runtime tests and rollback | Requirements specified; no recipe or test code changed or executed. |
| 9. Performance and memory | Representative measured baseline/candidate results, regressions and amortization | Measurement plan only; no new performance result. |
| 10. Consolidated audit | No missing roots, duplicate authority, stale unsupported claims or hidden blockers | Documentation/root-accounting review; complete semantic inventory and runtime acceptance remain OPEN. |

Source leaves can be inspected and candidate transformations produced in parallel under exclusive leaf ownership. Ratification remains deterministic and serial at the relevant file/package/module/full-build/API boundary. Cross-file recipes acquire the required ownership scope before publication; an isolated passing leaf does not authorize a partially updated module. Reuse existing progress-monitor contracts for tooling, with its supported no-op/default behavior, without changing public JDK method signatures.

## 2. Work-packet contract

Every implementation packet must name an accountable owner, exact source and target pins, existing semantic owners, prerequisites, affected capability IDs, actual symbols/files, route, public/native/format contracts, recipe and provenance, tests, measurement cases, rollback and unresolved decisions. New named reviewers are not assigned by this document; assignment remains open until recorded by the project. Packet labels below are documentary, not new registry IDs.

A packet can investigate alternatives without enabling them. Promotion requires the combined dependency closure, including views and runtime consumers. Keep candidate generation separate from accepted canonical changes. Preserve the original implementation where no proposed candidate passes the gates.

| Packet | Dependencies and scope | Deliverable and decisive acceptance |
|---|---|---|
| **WP-00 Inventory and closure** | First; every root in the inventory and relevant source owners | Extend the existing producer for OpenJDK topology; acquire raw-byte/source/symbol/native/generator obligations and unresolved edges. Gate on complete denominators, pinned producers, diagnostics and no silently skipped items. |
| **WP-01 Ownership and minimal foundations** | WP-00 owner selection and mapping review | Reuse minimal lanes, references, IDs, generation/lifetime and fact owners. Prove bounds, overflow, null/absent distinction, GC reachability, reclamation and bootstrap closure. No global arbitrary-object interner. |
| **WP-02 String/text** | WP-01; relevant charset/regex/consumer inventory | Implement operation bridges through canonical atoms/ranges, not a parallel spelling store. Pass exact UTF-16, identity, seams, regex, admission/export and retention tests. Route C additionally requires WP-11. |
| **WP-03 Sequential mutable collections** | WP-01; promotion includes WP-04 | Per-owner recipes for compatible growth/probe/sort/ring/heap/slot primitives and candidate backends. Pass API, callbacks, null/type, overflow, collisions, order, subclass and serialization gates. Preserve complexity profiles and record losing candidates. |
| **WP-04 Immutable forms and all views** | WP-01 plus the owner under adaptation | Define snapshot, shallow immutable, unmodifiable/live, range/reversed/entry ownership. Prove mutation isolation or write-through as specified, supported view lifetime, iterator/spliterator and array-export behavior. Required before affected WP-03/05 promotion. |
| **WP-05 Concurrency, atomics and scheduling** | WP-01; relevant admitted collection primitives/views | Per-method linearization/visibility and progress obligations, plus interruption, fairness, timeout, cancellation and reclamation. Retain existing protocols unless replacement is proved. Cover locks/synchronizers/executors and virtual-thread consumers, not only concurrent maps. |
| **WP-06 Streams and bulk algorithms** | Relevant WP-02/03/04/05 owners | Reuse range cursors and primitive loops where valid. Preserve laziness, encounter order, short-circuiting, characteristics, parallel effects and callback exceptions. Generic collectors must not silently become primitive or immutable. |
| **WP-07 IO, NIO, files and networking** | WP-01; text/charset where used; WP-16 for native | Buffer/segment/path/metadata candidates with position/limit/mark, aliasing, read-only, partial transfer, close, cancellation and backpressure contracts. Test network/filesystem/provider behavior and explicit contiguous boundaries. |
| **WP-08 Numeric, math, time and values** | WP-00/01 and exact semantic owners | Reuse immutable numeric tables/lane algorithms only for compatible semantics. Preserve overflow, floating NaN/signed zero, rounding, BigDecimal scale/equality, chronology, timezone/locale and random-generator state. No reassociation justified only by speed. |
| **WP-09 Reflection, handles and loading** | WP-01; WP-11 for changed VM readers | Loader-scoped metadata and linkage facts with invalidation/unloading. Preserve Class/object identity, lookup/access checks, fields/descriptors, redefinition, initialization and services. Reject unsafe reflection-dependent lowering. |
| **WP-10 Compiler and source infrastructure** | WP-00/01 and WP-09 as needed | Reuse indexed source/AST facts and existing OpenRewrite recipes without replacing javac attribution by a name-based parser. Preserve ordered children, scopes, symbols, diagnostics, source positions, annotation processing and emitted behavior. Lower only proven operations. |
| **WP-11 VM, GC, interpreter, JIT and layout** | All affected owner/native/metadata closures; not just a Java facade | Build a matched complete image. Verify object/reference scanning, barriers, allocation, deoptimization, interpreter/compiled/intrinsic paths, CDS/images, JNI/JVMTI, agents and diagnostics for supported configurations. Unsupported combinations stay disabled or blocked. |
| **WP-12 Security and cryptography** | WP-00 provenance/threat boundary; WP-16 where relevant | Retain algorithms/providers absent a separate reviewed case. Test provider lookup, keys/certificates, secure parsing, errors, secret lifetime, cache isolation and timing-sensitive behavior. Performance does not waive cryptographic or confidentiality gates. |
| **WP-13 Desktop, media and accessibility** | WP-00; WP-16; relevant text/value owners | Resource/pixel/font/audio/metadata candidates must retain native-peer lifetime, UI/event-thread behavior, printing and accessibility interfaces. Separate platform/headless/device gates; no generic text substitution for mutable media buffers. |
| **WP-14 XML, SQL, naming, RMI and services** | Relevant text/collection/loader/security owners | Preserve graph/order/namespace semantics, provider discovery, external object identity, JDBC state/transactions, remote protocols and serialization. Cache scope and invalidation follow actual owners, not a global dictionary. |
| **WP-15 Tools, diagnostics, build and images** | WP-00; all changed producer/consumer contracts | Keep OpenJDK build and quality policy. Reconcile jlink/jpackage/jar/jdeps/javadoc/JShell, monitoring, JFR, debugger/agent schemas and resource images. Prove generated inputs, reproducible identity and matched reader/producer compatibility. |
| **WP-16 JNI, foreign and platform interfaces** | WP-01 plus each affected owner | Inventory entry points, registration, generated headers, calling conventions, alignment/endian/word-size and lifetime/close/critical-region rules. Test each supported OS/architecture/provider. Respect Java 21 API/preview status; a newer foreign API is not a drop-in baseline. |
| **WP-17 Unicode, locale and resource tables** | WP-00/01; relevant WP-02/08 consumers | Versioned immutable resource reuse without eager bootstrap recursion or unintended normalization. Preserve provider/fallback order, charset state, Unicode/locale/timezone outputs, provenance and per-generation invalidation. |

### Route-specific enablement

Route A uses stock Java 21 and explicit modules/APIs; it must pass its own API and lifetime tests and disclose stock conversion/boxing boundaries. Route B additionally proves admission and refusal on the exact compiler/recipe and records original/transformed effect traces. Route C requires the full affected image and configuration matrix, not just A/B test success. A packet can be accepted for A while remaining proposed or blocked for B/C.

Do not invent an enabled runtime flag in documentation. A future per-subsystem opt-in flag/configuration needs its own implementation, supported-combination checks, default/fallback semantics and tests. An invalid combination must fail safely rather than silently falling into an unverified execution mode.

## 3. Mapping authority and enhancement continuity

### Reconcile, do not replace

The retained `m3/docs/name-mapping.json` contains `migration.records`, including actual retained migration IDs. Its operational schema and existing validators remain the target authority. Preserve the early top-level mappings as historical directions rather than overwriting stable records. The existing explanatory `migration-mapping.schema.json` and example are proposals, not the deployed registry.

The separate [#12 manifest at d543255294ae85e4c8015812a3c8a97aaf498354](https://github.com/hsoliwal/M3jdk21/blob/d543255294ae85e4c8015812a3c8a97aaf498354/m3/migration/manifest.json) belongs to its feature-stack lineage. Its absence at the inspected master path does not mean other `m3/migration` tooling is absent. The private [source inventory #7696](https://github.com/hsoliwal/com.synexia/pull/7696) has its own source-obligation schema and producer/input pins; those rows do not admit target implementations or authorize publication.

Reconcile these through an explicit reviewed crosswalk into the retained target authority. Preserve source-obligation identities, capability identities and legacy IDs as distinct roles. A source path hash can identify an initial inventory item without being the final stable semantic capability identity. A rename/split/consolidation needs lineage, not a freshly disconnected registry.

The existing mapping-lifecycle addendum records an important concrete gap: source can be retained in `m3/ports/indexstring` while its `synexia.frozen-chars` record still has pending targets. That requires reconciliation, not a false claim that the file is absent or that its behavior is accepted. Likewise, `com.m3.text.compat.M3Text` and historical `com.m3.indexstring.M3Text` receipts must not be conflated because their short names match.

### Required field semantics

Extend the existing schema through a separate tested implementation change only where necessary. This documentation does not add these as operative JSON fields. Records must be able to express stable capability identity; source and target repository/commit/blob/raw-content identities; fully qualified declarations; ownership and namespace; dependencies and consumers; API/ABI/format/serialization contracts; route and adaptation; recipe/version/preconditions; provenance/license review; acceptance requirements and exact evidence; unresolved conflicts; and last fully reviewed/synchronized source revision.

Keep at least these independent evidence questions: was source inspected; was a port produced; is that code retained in the selected target tree; was behavior verified for this owner/route/configuration; was performance measured; and was promotion accepted? A merged PR, matching filename or equal structural signal does not answer them all. Historical receipts remain pinned and cannot move automatically to a different owner, renamed facade, combined tree or changed dependency.

### Enhancement replay procedure

1. Freeze `S0`, the mapping's last reviewed source, and `S1`, the chosen new source revision. Freeze `T0`, the recorded target adaptation, and `T1`, the actual candidate target baseline. Pin the recipe, tools, options and dependency versions independently.
2. Diff source and target separately. Detect added/changed/deleted/renamed paths, declarations, resources, generated inputs, native entries, formats, tests and dependencies. Resolve all affected items to source obligations and capability IDs; unclassified items become visible work.
3. Distinguish compatible enhancement, source bug fix, performance-only change, API/format evolution, target-only improvement, conflict, deferred work and non-applicability with a reason. Do not treat a rename as proof of unchanged semantics or a disappearing discovery row as proof of deleted source.
4. Compute the affected dependency/runtime-consumer closure. Compare the target's current hashes and semantic adaptation with `T0`; preserve target-only fixes. A whole-file donor postimage must refuse unexpected target drift.
5. Reuse/improve the appropriate recipe, tests and provenance decision. Perform exact confirmation after structural/logic hashes or similarity signals suggest a match. Parallel candidate generation does not authorize parallel conflicting publication.
6. Test the exact combined candidate: deterministic replay, idempotence, drift/refusal, partial/mixed state, contract tests, relevant module/build/runtime gates and rollback. No borrowed receipt from another branch or donor head.
7. Update mappings and evidence with the implementation change. Advance the synchronized revision only when every relevant source delta has a disposition. Deferred/blocked obligations remain visible. Preserve retired IDs, aliases and tombstones with reasons.
8. Review reverse/backport work independently. A forward adaptation is not automatically valid in the reverse direction, and a historical rollback does not delete unrelated later changes.

### Worked rename/split/conflict example

This example uses illustrative owners, not implemented classes or registry rows. Suppose one admitted source search owner is renamed and split into a bound-search atom and capacity helper. Preserve the existing capability's lineage and add the reviewed one-to-many relationship. The target has already added an overflow guard absent from the new donor source. Replay must retain that target-only guard while adapting the search change; copying the donor file wholesale is a conflict, not a completed port. Both resulting dependencies and their tests must map back to the reviewed source range. The old path receives a move/split record, not an unexplained deletion.

If the source also changes comparator or exception behavior, refuse promotion until that contract is reconciled. An apparently equivalent sorted primitive implementation cannot replace a generic comparator-based owner solely because the new method name is similar.

## 4. Recipe and verification gates

Prefer the existing Maven/OpenRewrite recipe machinery for appropriate Java transformations. A narrow exact-source Java/Maven replay can be used within the existing project policy for pinned changes, with explicit preimage/postimage and refusal tests. Neither mechanism is a new rewrite engine. Native/VM/build changes require source-pinned mechanisms suited to their languages and generated consumers. Preserve the OpenJDK build rather than trying to build the JDK as a Maven reactor.

| Gate | Evidence required before the corresponding promotion |
|---|---|
| G0 Scope and provenance | Complete affected inventory, owner, dependencies, raw source pin, applicable notices/license review and authorized publication boundary. |
| G1 Mapping and baseline | Stable IDs, source/target adaptations and exact candidate/tree identified; conflicts and unresolved consumers visible. |
| G2 Deterministic transformation | Recipe/tool/version/options, guarded preimages, absent-before policy for additions, exact outputs, idempotence, drift refusal and owned rollback. |
| G3 Build/static quality | Applicable formatting, source compile, attribution, module/native build, static checks and existing project checks on the candidate. Stubs/extraction are separately labelled. |
| G4 Public behavior | Differential API and state-machine tests, null/type/bounds/overflow, identity, views, callback effects, Unicode/order/comparator/collision and serialization/subclass/reflection support. |
| G5 Concurrency/lifetime | Stress and method-specific histories, JMM publication, weak/strong reachability, slot generations, native acquire/release, close/GC, mappings and failure/cancellation. |
| G6 Runtime integration | For C, matched complete images and affected interpreted/compiled/intrinsic/collector/agent/image combinations, plus negative configuration tests. For A/B, exact stock-runtime and compiler/tooling scope instead. |
| G7 Compatibility | Applicable API/binary/serialized/native/format compatibility and authorized conformance suites. Regression suites alone do not certify universal Java compatibility. |
| G8 Cost and regression | Reproducible performance/allocation/retention/native memory evidence, cold admission and budget behavior, representative losses as well as wins. |
| G9 Review and rollout | Accountable review, explicit enablement scope, fallback, upgrade/format/reclamation and exact rollback artifacts. |

`NOT_RUN`, `SKIPPED`, `BLOCKED`, `FAILED` and `PASSED` remain different states. Only a reviewed not-applicable decision can remove a gate from a particular scope. A passing inventory validator can honestly report incomplete migration. A benchmark cannot waive a behavior failure. Do not change expected exceptions, suppress diagnostics, reduce coverage or relax CI to manufacture success.

### Build/test entry points

Use the pinned [OpenJDK building guide](https://github.com/hsoliwal/M3jdk21/blob/45f546ff5bcb06a1b2604f14baf998785d98d9a1/doc/building.md) and [testing guide](https://github.com/hsoliwal/M3jdk21/blob/45f546ff5bcb06a1b2604f14baf998785d98d9a1/doc/testing.md). The inspected testing guide describes `make test`, tier selections, jtreg/native tests and microbenchmarks; `run-test` is a compatibility alias. Configure the actual authorized toolchain, jtreg and JMH dependencies as required by that guide. This documentation does not install them.

Illustrative sequence after an implementation team has configured the exact candidate:

```sh
make images
make test-tier1
make test TEST=jdk_lang
```

These commands were **not run here** and are not a complete whole-JDK acceptance set. Select owning collection/concurrency/compiler/GC/native/platform tests and higher tiers from the actual test tree, record them explicitly, and execute exact candidate images. Do not present a focused `jdk_lang` run as collection, compiler, GC or whole-runtime acceptance. Record unavailable conformance infrastructure rather than claiming its tests passed.

### Failed gate example

The existing shared-atom document records historical enabled StringJoiner OOME expectation failures on its exact runtime candidate. A plausible allocation explanation is not a passing result. A later candidate must rerun the relevant tests and obtain an explicit reviewed disposition; neither merging history nor passing a different focused suite clears those failures. Similarly, a combined tree that was merely merge-checked must be built/tested independently before receiving any parent branch's runtime acceptance.

## 5. Performance and memory measurement plan

### Measure the complete cost, not just payload

Break costs into object headers/alignment, reference width, wrapper objects, structural nodes, reference/primitive arrays, spare capacity, resize copies, temporary iterators/entries/streams, descriptor directories, retained backing/snapshots, interner/fact metadata, class-loader retention, native/mapped memory, code size and synchronization/coordination. Record GC configuration, heap/compressed-reference configuration and actual layout rather than assuming a universal header or reference size.

For a compact map, compare removed node/header/link costs against added control/hash/index arrays, reference lanes, entry adapters, alignment and spare capacity. Key/value object payload usually remains in the generic case. For text, include dictionary admission, descriptors, balancing, seam facts, local overlay, page faults and exports. For native acceleration, include crossing, marshalling/copies, setup, synchronization and fallback rather than timing only a kernel.

Shared mapping claims require two independent processes and evidence of common backing plus resident/shared-page accounting. Summed RSS can double-count shared pages; report the metric and platform clearly. Logical mapped bytes, resident bytes, privately charged bytes and Java heap allocation are different measurements. A small heap can hide excessive native or mapped retention.

### Workload grid

Include empty/tiny and large collections; sparse/full capacity; high/low reuse and duplication; ordered/random/adversarial keys; collision clusters; near-sorted runs and random order; small/large batches; growth/shrink and clear/reuse; mutation-heavy and read-heavy patterns; sequential/parallel streams; short/long-lived views and snapshots; many class loaders/owners; mixed readers/writers; cancellation/timeout pressure; and long retention with eviction/GC.

For text include flat and highly segmented geometry, shared/local/mixed atoms, cold/warm lexicons, partial-range queries, malformed/surrogate seams, locale/regex complexity and repeated export. For files/IO include cold/warm pages, partial transfers and backpressure. For desktop/native/provider work, include the appropriate real platform workload; a microbenchmark cannot stand in for device/protocol correctness.

Use identical datasets, generated seeds, JVM/toolchain versions, CPU limits, memory budgets, flags and warmup/fork methodology for baseline and candidate. Separate construction/admission from steady state, and also report end-to-end totals. Retain raw results, variance/distribution, throughput, latency including tails where meaningful, allocation and post-GC/long-lived retention. Avoid dead-code elimination and accidentally benchmarking already-computed answers when the real workload pays admission.

### Precomputation economics

For each cached fact, record validity, producer, build cost, byte cost, hit/reuse distribution, invalidation, admission, eviction, concurrency and behavior on budget refusal. If setup cost is `B`, uncached per-operation cost `C0`, and cached cost `C1`, an idealized break-even reuse count exists only when `C0 > C1`; it is approximately `B / (C0 - C1)`. This is a planning equation, not a measurement, and must include expected invalidation/eviction and additional memory cost. If the owner dies or changes before that reuse, the cache may lose.

Set acceptance/regression thresholds for the intended workloads before tuning, and keep workload-specific fallbacks where justified. Do not convert a single favorable allocation micro-result into a universal speedup or declare every cache worthwhile. Record rejected strategies and their exact inputs so later contributors do not repeat them blindly.

### Receipt content

A performance or correctness receipt binds source, recipe, candidate commit/tree and build-image hashes; dependency/tool versions; OS/CPU/architecture; compiler/JVM/GC/configuration; commands/test selection; dataset/seed and provenance; raw outputs; elapsed/timeout/exit state; baseline; and limitations. A local extracted-source test, full module test, complete image test and hosted synthetic-merge run are different scopes. Preserve the full proof lineage, including failed attempts, without implying a documentation-only head was runtime-tested.

## 6. Publication, provenance and rollout

Keep OpenJDK root and per-file obligations, assembly/classpath exceptions where actually applicable, and third-party notices intact. A donor's Apache-2.0 notice or a renamed package does not relicense OpenJDK code. Review each source/component/dataset and distribution use; this documentation makes no blanket license-compatibility determination. Code provenance, algorithm inspiration and dataset redistribution are separate decisions.

Keep private source, detailed private catalogues, datasets, paths not approved for publication, credentials and machine details out of public changes. Authorization to inspect an access-controlled donor is not automatic authority to copy its implementation into this repository. Public documentation can explain original designs and link to authorized review surfaces without importing their bodies. Source-side owner ledgers stay in the private companion.

Roll out per subsystem and route. First establish a known baseline and safe fallback; then admit an opt-in candidate only for the supported configuration and workload. Archive the prior complete image and format/owner commitments. A runtime flag-off test is necessary where supported but is not by itself a downgrade/rollback proof.

For C, distribute only matched complete images. Never replace an installed JDK or transplant a class/library into an unrelated image during this programme's documentation work. A later deployment needs explicit authorization, migration/reclamation procedures and platform rollback tests. Immutable shared generations coexist while live readers need them; rollback must not truncate a mapped file or free native memory still referenced by a supported view. Serialized/native/image format changes need versioned compatibility or explicit refusal, not reinterpretation.

Recipe rollback acts only on receipt-owned, matching changes. Preserve unrelated later source and target improvements. A force push, destructive whole-file overwrite or ancestry-only merge is not a substitute for contract-preserving consolidation.

## 7. Durable contributor/resume checklist and open decisions

Start with the exact pins and historical scopes in the existing RESUME and mapping documents. Then inspect the current target/source refs without rewriting historical receipts. Read the actual delivered tree for each relevant PR, not only its merge state. Reconcile current #23/#24 implementation candidates and source-side owner drafts through the existing map; do not borrow tests across their different heads or combined trees.

The next dependency-ready work is WP-00/01: acquire complete file/symbol/native/build closure; join source obligations to retained target IDs; resolve competing owners and retained-but-unmapped code; define minimal bootstrap/lifetime foundations; and assign accountable packet owners. Do not begin a broad backend rewrite while those prerequisites are unresolved.

Open decisions include: exact OpenJDK topology/native inventory producer extensions; canonical crosswalk for branch/source manifests; ownership among overlapping donor variants; generic identity-preserving compact-map layout; descriptor/segment/cache budgets; weak-key and concurrent-slot reclamation; protected/private reflection support; per-subsystem enablement and supported VM/GC/platform combinations; format upgrade/downgrade; measurement thresholds; and provenance/publication permissions. Each must acquire an owner, disposition and evidence in the existing records.

Completion requires every discovered obligation to be classified, every affected contract/consumer to have evidence or a visible blocked/deferred decision, and no unsupported whole-JDK claim. Retained, implemented, tested, proposed, blocked and deferred states remain distinct. This documentation lets the next contributor find unfinished work systematically; it does not certify that the unfinished work has already run.
