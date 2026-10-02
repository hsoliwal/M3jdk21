# Whole-JDK M3 work packets, ten-pass migration and evidence plan

Status: **documentation-only planning annex**. This file defines the ten-pass programme, subsystem packet schema and evidence model; it does not implement a JDK change or advance migration status. The implementation-packet execution owner is [../migration/docs/WORK_PACKETS.md](../migration/docs/WORK_PACKETS.md); this annex supplies the broader pass/evidence structure and must not become a parallel operational registry.

Target planning pin: `hsoliwal/M3jdk21@45f546ff5bcb06a1b2604f14baf998785d98d9a1`.
Synexia owner-inspection pin: `hsoliwal/com.synexia@3db24805d640c72ab1bd637d83561696d99561a0`.

See [whole-jdk-subsystem-matrix.md](whole-jdk-subsystem-matrix.md), [whole-jdk-collections-replacement.md](whole-jdk-collections-replacement.md), and the existing [MIndex migration handoff](mindex-migration-handoff.md).

## 1. Layered architecture

Keep ownership and dependencies explicit. The programme has seven logical layers; a class may participate in more than one, but a runtime dependency may not flow arbitrarily upward.

| Layer | Responsibility | Bootstrap rule |
| --- | --- | --- |
| L0 Canonical data and lifetime | immutable payloads, owner namespaces/generations, handles, exact identity, ranges, tuples | minimal/no application dependencies; lifetime must be valid before higher-level caches |
| L1 Storage and indexing | primitive/reference lanes, slot arenas, packed tables, trees, CSR, dictionaries, mapped images | must not require Maven/OpenRewrite/UI/network/service registries during early bootstrap |
| L2 Algorithms and precomputation | search, hash/index structures, derived facts, ordering, graph algorithms, encoders | derived state binds exact owner/version and has bounded admission/eviction/invalidation |
| L3 Public Java compatibility | String/collections/buffers/files/etc. facades, views, iterators, serialization | preserves API/ABI/behavior; materialization boundaries are explicit |
| L4 Compiler transformations | attributed lowering, source/bytecode recipes, generated-source adaptation | tooling only; refusal on unresolved semantics; output revalidated |
| L5 VM/native integration | HotSpot layouts, intrinsics, GC barriers, JNI/JVMTI/FFM/platform code | exact matched JDK image and OS/CPU-specific evidence |
| L6 Diagnostics/evidence/migration | inventories, mappings, contract snapshots, receipts, benchmarks, rollback | never becomes semantic runtime authority |

A dependency from an early `java.base` path to L4/L6 tooling is prohibited. A derived index cannot become canonical identity merely because it is faster to query.

## 2. Reusable M3 foundation vocabulary

Before subsystem-specific backends are proposed, resolve whether an existing Synexia owner already provides the required capability:

- primitive lanes and bounded slot arenas;
- immutable atoms and exact-content dictionaries;
- namespace/generation handles;
- ranges/slices/segment directories;
- tuples/composite identities;
- CSR graphs and postings;
- pools for repeated immutable values;
- adaptive dense/sparse/bitmap/hash/tree choices;
- precomputed facts bound to an exact owner;
- deterministic contract/inventory/change receipts.

When two components have similar names but incompatible lifetime/equality/order semantics, keep them separate and write an adapter or explicit consolidation decision. No “M3 universal store” is created by default.

## 3. Ten dependency-aware passes

These are the programme passes for whole-JDK migration. They supplement existing text-specific P0/P1/etc. history; they do not rewrite or renumber historical receipts.

### Pass 1 — pin and inventory

- Pin target JDK tree and every source-owner tree used for planning.
- Inventory the entire JDK denominator: modules, packages, symbols, nested/public/protected/internal surfaces, native boundaries, formats/resources, services, generators, build surfaces and tests.
- Inventory MIndex/MatIndex/Synexia candidate owners and competing implementations.
- Record missing/unavailable/private surfaces as explicit gaps.
- Output: source census, owner census, reverse-consumer edges, immutable commit/blob identities.
- Stop gate: no “complete” claim while any denominator bucket is unenumerated.

### Pass 2 — subsystem/dependency/bootstrap map

- Build module/package/runtime dependency graph and reverse consumers.
- Mark bootstrap phases and cycles through allocation, exceptions, class loading, logging, maps, atomics, services, charsets and native libraries.
- Classify public API, internal ABI, serialized formats, VM layouts and platform-specific boundaries.
- Output: dependency graph plus bootstrap-safe closure candidates.
- Stop gate: no `java.base` owner imports higher-level Maven/application/runtime dependencies.

### Pass 3 — canonical mapping registry and dispositions

- Reconcile the established mapping authority and branch-scoped historical manifests.
- Extend stable capability IDs rather than introducing a parallel registry.
- Assign each inventoried surface one disposition: replace backend, adapt, reuse, retain pending evidence, platform-specific, blocked/deferred.
- Record many-to-many split/consolidation/adapters explicitly.
- Stop gate: no target mapping promoted without source/target pins and contract owner.

### Pass 4 — minimal storage/ownership/identity foundations

Specify, before integrating APIs:
- owner namespace/generation rules;
- primitive/reference lane ownership;
- immutable/mutable separation;
- GC reachability and off-heap/native lifetime;
- slot reuse/stale-handle rejection;
- exact equality confirmation after hash/signal filtering;
- mapped-image publication/versioning;
- bounded precompute admission/invalidation/eviction;
- materialization policy.

Stop gate: numeric IDs from independent domains are never assumed interchangeable.

### Pass 5 — String and collections in depth

String:
- retain shared lexicon + VM-local misses;
- reference-only joins/slices where legal;
- exact UTF-16, seam-aware encoding and regex behavior;
- explicit materialization and `intern`/object-identity distinctions.

Collections:
- use [whole-jdk-collections-replacement.md](whole-jdk-collections-replacement.md);
- specify every concrete family and view;
- preserve null/order/equality/identity/serialization/JMM contracts;
- identify primitive/generic boxing boundaries.

Stop gate: no family-wide “replacement” status based on one facade or primitive prototype.

### Pass 6 — extend to all remaining subsystems

For each subsystem packet in section 5:
- select compatible M3 foundations;
- reject unsuitable common representations;
- enumerate external/native/serviceability consumers;
- produce route A/B/C eligibility and gates.
- Stop gate: no forcing security, GC, compiler, IO, networking or UI objects into a text/collection identity model.

### Pass 7 — route-specific work packets

Produce independent packets for:
- Route A explicit opt-in APIs/adapters;
- Route B compiler lowering with attribution/escape/refusal;
- Route C matched JDK internal replacement.

Each packet binds exact source/target symbols, recipes or native patches, tests and rollback. Evidence is not transferable between routes without explicit applicability analysis.

### Pass 8 — transformations, differential tests and rollback

Prefer reusable Maven/OpenRewrite recipes for Java/source-tree transformations where appropriate. For HotSpot/native/build files, use source-pinned deterministic transformations suitable to those languages/files.

Required recipe/patch behavior:
- exact preimage hash or semantic precondition;
- deterministic output;
- drift and mixed-state refusal;
- idempotent fixed point;
- partial-failure handling;
- explicit rollback only when postimage matches expected hash;
- preserved external contract unless an intentional migration is declared.

Verification sequence for implementation packets: **diff → lint/static validation → compile → tests → runtime → replay/idempotence → integrity/acceptance**. A later pass may not retroactively excuse an earlier failed gate.

### Pass 9 — performance and memory evaluation

Measure all cost categories in section 6. Include precomputation/admission/cleanup and retained-owner effects. Compare the exact stock baseline and exact candidate under the same environment.

Stop gate: lower allocation, smaller primitive payload or a microbenchmark win never waives contract failures.

### Pass 10 — complete-inventory audit and consolidated handoff

Audit:
- denominator omissions;
- duplicate/competing owners;
- stale source/target pins;
- missing reverse consumers;
- unresolved mapping conflicts;
- tests/receipts attached to the wrong candidate;
- unsupported completion/performance claims;
- licensing/provenance gaps;
- rollout/rollback gaps.

Publish one consolidated resume state pointing to authoritative mappings and evidence. Historical receipts remain pinned and are not rewritten to match a newer owner.

## 4. Common work-packet schema

Every subsystem implementation packet should carry:

```text
packet_id
status
target_repo + target_commit
source_owner_repo + source_commit
mapping_ids
target_modules/packages/symbols
native/resource/build/test surfaces
current_semantic_owner
current_physical_representation
proposed_owner/layout
dependencies + reverse_consumers
api_abi_serialization_format_contracts
identity_mutability_lifetime_bootstrap_contracts
route_A_scope
route_B_scope_and_refusals
route_C_scope_and_vm_consumers
recipe_or_patch_identity
pre_hashes
post_hashes
differential_tests
compile/build/runtime_gates
performance_matrix
provenance_license_review
rollout_default
rollback_procedure
open_conflicts
evidence_links
last_synchronized_source
last_verified_target
```

No field may be inferred from a PR title. “Source inspected”, “source ported”, “target retained”, “candidate tested”, “accepted”, and “merged” are distinct facts.

## 5. Subsystem packets and acceptance criteria

### WP-TEXT — String, chars, Unicode, charset, regex

Inputs:
- existing String/MIndex docs and retained text owners;
- `java.lang.String`, compact-string helpers, concat/invokedynamic, regex and charset consumers;
- HotSpot String helpers, CDS/StringTable/dedup/intrinsics/JNI.

Acceptance:
- exact UTF-16 code units, split-surrogate ranges and malformed-surrogate behavior;
- content equality/hash/order and Java object identity remain distinct;
- lexicon hit/local miss/join/slice lifetime safe;
- charset/regex seam behavior differential;
- every contiguous array/native projection named;
- exact matched-JDK interpreter/JIT/GC/CDS/JNI/serviceability gates for Route C.

### WP-COLLECTIONS — sequential and immutable collections

Inputs: [whole-jdk-collections-replacement.md](whole-jdk-collections-replacement.md) and actual `java.util` inventory.

Acceptance:
- operation-by-operation contract matrix;
- public/protected/serialization compatibility;
- live views/iterators/spliterators/streams;
- primitive/generic boxing boundaries;
- memory/performance break-even and regressions;
- no mutable value interning.

### WP-CONCURRENT — concurrent collections, atomics, synchronizers

Inputs: `java.util.concurrent`, VarHandle/Unsafe/atomic primitives, VM fences and thread/runtime dependencies.

Acceptance:
- JMM happens-before and safe-publication proof;
- linearization point per promised atomic operation;
- progress/fairness/cancellation/timeout behavior;
- stale-handle/ABA/reclamation solution for any slot-based design;
- stress under resize/contention/GC;
- exact interpreter/JIT/fence/platform tests when low-level memory operations change.

### WP-STREAM — iterators, spliterators, functions, streams and bulk operations

Candidate optimization:
- primitive/internal pipeline lanes, fused loops and precomputed partition metadata where semantics allow.

Acceptance:
- encounter order, laziness, short circuit, side effects, exceptions and close handlers;
- spliterator characteristics and size/split behavior;
- collector associativity/identity/concurrency rules;
- parallel execution correctness and cancellation;
- no lowering that changes callback count/order.

### WP-IO — IO/NIO, buffers, channels, filesystems, networking and serialization

Candidate optimization:
- segmented/direct/mapped buffers, reusable indexes and zero/low-copy transfer only at APIs that permit it.

Acceptance:
- byte order, position/limit/mark, aliasing and mutable-view semantics;
- channel blocking/nonblocking/partial IO and interruption;
- file identity/path/provider semantics;
- native resource lifetime and cleaner/close behavior;
- serialization compatibility and object graph semantics;
- network protocol/TLS boundaries unaffected.

### WP-VALUE — numeric, math, time, locale, random and value-oriented APIs

Default:
- reuse existing primitive/value representations unless profiling shows a redundant representation.

Acceptance:
- exact overflow/rounding/NaN/signed-zero and algorithm stream contracts;
- temporal/calendar/zone/locale resource versioning;
- deterministic seeded random behavior for named algorithms;
- no canonicalization that changes object/monitor identity when observable.

### WP-META — reflection, method handles, class loading, modules and services

Candidate optimization:
- compact metadata/indexes and precomputed lookup tables bound to class/module generation.

Acceptance:
- linkage/access checks, hidden classes, class identity/unloading;
- method-handle/invokedynamic type and exception semantics;
- module readability/exports/opens/services;
- class-loader namespace isolation;
- cache invalidation on unloading/redefinition.

### WP-COMPILER — javac, bytecode/classfile, interpreter and JIT interfaces

Route B:
- attributed transformations only;
- preserve source evaluation/overload/boxing/exception behavior;
- fail closed on unresolved types/aliases/escapes/identity boundaries.

Route C:
- preserve classfile/JVM specs;
- audit interpreter/C1/C2/JVMCI/intrinsic consumers;
- differential compiled/uncompiled execution;
- deoptimization/debug metadata and tooling visibility.

### WP-GC — allocation, object layout, GC, references and runtime metadata

Default:
- **blocked/deferred from representation changes until exact reverse consumers are mapped**.

Acceptance for any change:
- all collectors/barriers/root scanners/object iterators;
- weak/soft/phantom references and ReferenceQueue;
- compressed oops/class pointers/layout helpers;
- allocation/TLAB/OOME behavior;
- heap dumps, SA, JVMTI/JFR/CDS and diagnostics;
- stress across every supported collector/build mode in scope.

### WP-NATIVE — JNI, FFM/native interfaces, OS/CPU integration and serviceability

Rule:
- native acceleration is cross-cutting and optional; it is not a fourth architecture route.

Acceptance:
- JNI reference/pinning/critical/modified-UTF behavior;
- FFM/native ABI, alignment, endian, lifetime and downcall/upcall boundaries;
- platform-specific source selection;
- sanitizer/static-analysis where applicable;
- `-Xcheck:jni` and exact native tests for changed JNI paths;
- no native address used as persistent identity.

### WP-SECURITY — security, crypto and providers

Default:
- retain algorithm/provider ownership unless a separately reviewed optimization is security-neutral.

Acceptance:
- provider selection and API compatibility;
- algorithm/format vectors;
- constant-time/side-channel requirements where applicable;
- key/certificate/keystore and native-token formats;
- failure behavior and policy constraints;
- provenance/licensing review of any donor.

### WP-TOOLS-DIST — tools, images, build and packaging

Acceptance:
- OpenJDK build graph preserved;
- generated source/resource reproducibility;
- jmod/modules-image/jlink/jpackage/launcher compatibility;
- debug/symbol/serviceability images;
- supported cross-build/platform matrix;
- tools remain outside runtime bootstrap unless already required.

## 6. Evidence and “bloat” measurement model

Measure categories separately:

| Category | Required observation |
| --- | --- |
| object headers/alignment | object counts and measured layout, not assumed constants |
| references | width/configuration and per-occurrence count |
| boxing | allocation/count and boundary causing it |
| nodes/links | per-entry object/slot/link metadata |
| capacity/slack | logical size vs backing capacity |
| resize/copy peak | transient old+new storage and copied work |
| views/iterators/wrappers | allocation and retained-owner effect |
| cache/precompute | canonical data vs derived metadata, admission and eviction |
| native/mapped | committed/resident/mapped accounting and ownership |
| GC | allocation rate, pause/concurrent work and remembered/barrier effects |
| code/class metadata | generated machine code, class metadata and extra methods/tables |
| synchronization | locks/CAS/fences/contention/retry and parking costs |

Workloads must include small/large, sparse/dense, cold/warm, adversarial keys, mutation-heavy, iteration-heavy, mixed readers/writers and long-lived retention. Report regressions and confidence/variance, not only wins.

Precomputation documents:
- validity domain and input generation;
- cost to build;
- memory retained;
- admission threshold;
- invalidation/eviction;
- concurrency/publication;
- amortization/break-even.

## 7. Verification suites

Per affected subsystem select and pin:
- API differential tests against stock JDK;
- malformed-input and overflow/error tests;
- serialization old/new fixtures where applicable;
- GC/lifetime/reference tests;
- concurrency stress and memory-model litmus tests;
- JNI/native validation and sanitizers where applicable;
- exact-build runtime acceptance;
- jtreg/JCK or other applicable platform suites;
- workload benchmarks after correctness.

A benchmark PASS cannot override a semantic FAIL. A historical PASS belongs to its exact source/target/environment hashes only.

## 8. Provenance and licensing

For every imported or adapted donor:
- pin repository, commit/tag and exact files;
- record license and notice obligations;
- distinguish algorithmic/reference inspiration from copied/adapted source;
- retain required notices and source-offer obligations;
- do not publish private Synexia material merely because the target JDK repository is public;
- do not copy challenge-site solutions as production source without explicit compatible licensing/provenance review.

OpenJDK files retain their upstream license headers and applicable Assembly Exception. Separate Apache-2.0 Synexia code cannot be dropped into GPLv2+Classpath/OpenJDK files without a reviewed licensing decision.

## 9. Rollout and rollback

Every Route C replacement starts default-off unless compatibility policy explicitly says otherwise and evidence justifies default enablement.

Rollout stages:
1. build-only/read-only diagnostics;
2. explicit experimental flag;
3. selected subsystem enablement with stock fallback;
4. broader test/benchmark matrix;
5. default-on proposal only after required gates;
6. removal of fallback only as a separately reviewed compatibility decision.

Rollback:
- recipes/patches must identify exact expected postimage;
- refuse rollback over user/divergent edits;
- retained persistent formats must have version/fallback policy;
- a failed runtime gate blocks promotion and returns the subsystem to the last verified stage.

## 10. Completion semantics

The programme is not complete when:
- a facade compiles;
- a PR merges;
- one microbenchmark wins;
- a module directory has a disposition;
- one route passes;
- historical receipts exist.

A subsystem reaches accepted state only when its mapped denominator and required routes/platforms/build modes have exact candidate evidence and no mandatory blocker remains. Whole-JDK acceptance requires the same condition across the entire reviewed denominator.
