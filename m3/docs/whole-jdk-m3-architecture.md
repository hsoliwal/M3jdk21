# Whole-JDK M3 architecture and migration specification

Status: **documentation-only architecture and implementation handoff**. This document does not modify production Java, HotSpot, native code, an installed JDK, generated mapping records, or acceptance evidence. It deliberately distinguishes observed repository state, candidate designs, implementation work and verified behavior.

## 1. Programme scope

The M3 programme covers the complete JDK and runtime implementation. String is one subsystem. Collections, primitive/value storage, concurrency, I/O, compiler/runtime infrastructure, VM metadata and native boundaries are first-class parts of the same programme.

The optimization goal is to reduce avoidable:

- object headers and per-element nodes;
- boxing and wrapper churn;
- unused capacity and resizing copies;
- duplicated immutable payloads;
- temporary materialization;
- repeated hashing/search/shape computation;
- duplicate indexes and sidecars;
- native/JVM staging copies;
- synchronization and indirection where the public contract permits a cheaper representation.

No optimization is accepted by name alone. A replacement must preserve the observable Java contract for the surface it replaces.

## 2. Documentation and mapping authority

Do not create another mapping database.

On the current master line, the operational public mapping authority is:

- `m3/docs/name-mapping.json`, including its `migration.records` extension;
- the lifecycle rules in `m3/docs/migration-mapping-lifecycle.md`;
- the worked conflict/rename/split guidance in `m3/docs/migration-worked-port-decisions.md`.

Branch-scoped manifests, generated coverage and receipts remain evidence or candidate tooling tied to their exact branches. They may be reconciled into the authority through a reviewed change, but their existence on another branch does not silently replace the master registry.

Private Synexia inventories may remain the detailed source-side census. Public JDK documentation may record authorized pins, hashes, mapping IDs and dispositions without copying unrelated private source bodies or datasets.

Every future implementation change must keep separate:

1. source item inspected;
2. source behavior selected for the target;
3. target code physically retained;
4. recipe replayed successfully;
5. target behavior verified;
6. subsystem accepted for promotion.

A merged PR, matching filename, ancestry edge, benchmark win or successful compilation proves only its own bounded fact.

## 3. Architecture: seven layers

The M3 JDK architecture is split into seven layers so that a storage optimization cannot silently become a public semantic change.

### 3.1 Canonical data and lifetime ownership

Owners define:

- identity namespace;
- generation/version;
- mutability;
- lifetime;
- retention and eviction;
- collision confirmation;
- stale-handle behavior;
- canonical versus derived data.

An integer is never a globally meaningful identity without its namespace and generation. A cache slot is never a durable handle.

### 3.2 Storage backends and indexing mechanisms

Candidate backends include:

- primitive flat arrays;
- segmented primitive arrays;
- open-addressed tables;
- sorted primitive lanes;
- packed offsets and CSR-style adjacency;
- immutable ranges and joined descriptors;
- versioned mapped images;
- VM-local interners;
- explicit off-heap/native buffers where justified.

A backend is selected by compatible semantics, not by a similar class name.

### 3.3 Algorithms and reusable precomputation

Reusable facts may include:

- content hash;
- length/code-point counts;
- prefix/suffix and search tables;
- sorted-run boundaries;
- bucket geometry;
- structural hashes;
- AST/DAG derived indexes;
- regex candidate-rejection facts;
- encoding facts;
- pre-sized growth/capacity plans.

Precomputation is not free. Every fact has an owner, validity domain, admission cost, invalidation rule, concurrency model and memory budget.

### 3.4 Public Java compatibility surfaces

The public type remains authoritative for:

- method signatures;
- specified exception class and timing;
- identity-sensitive behavior;
- equality/hashCode;
- iteration and encounter order;
- backed views;
- mutability and unsupported operations;
- serialization;
- reflection and subclass hooks;
- Java Memory Model promises.

Internal primitive storage does not automatically remove boxing at a generic `Collection<E>` or `Map<K,V>` boundary.

### 3.5 Compiler transformations

Compiler lowering may replace only operations whose:

- resolved types are known;
- evaluation order is preserved;
- side effects are preserved;
- exception behavior is preserved;
- identity/escape behavior is compatible;
- synchronization/monitor behavior is compatible;
- external ABI boundary is known.

Unsupported or ambiguous sites must retain ordinary Java.

### 3.6 VM and native integration

Custom-JDK work coordinates all affected consumers, including:

- interpreter;
- C1/C2;
- intrinsics;
- GC barriers and scanning;
- object layout;
- StringTable/symbol tables where applicable;
- class loading and CDS;
- JNI/JVMTI;
- serviceability agent;
- JFR;
- native libraries;
- serialization and reflection internals.

A Java-only implementation is not evidence that VM consumers understand a changed object layout.

### 3.7 Diagnostics, evidence and migration tooling

Every accepted capability requires exact:

- source/target commits;
- file/symbol hashes;
- mapping IDs;
- recipe version;
- before/after conditions;
- executed commands;
- environment;
- test/benchmark receipts;
- unresolved gates;
- rollback path.

## 4. Bootstrap rule

The JDK must not import the whole Synexia application framework into `java.base`.

Bootstrap-safe M3 leaves must be dependency-minimal and source-controlled inside the JDK build. They may use ordinary JDK primitives available at that bootstrap layer. They must not require:

- Maven at runtime;
- application dependency injection;
- external databases;
- service graphs that are initialized after boot;
- optional GPU/JNI engines for correctness;
- higher JDK modules from lower layers.

Higher-level M3 tooling, OpenRewrite recipes, source inventories and optional accelerators remain outside the boot image unless a separate dependency review proves otherwise.

## 5. Reusable M3 foundations

Before replacing any JDK owner, select or define the minimal owners below.

| Foundation | Required contract |
| --- | --- |
| Primitive storage lanes | exact primitive width, bounds, growth, overflow, publication and ownership |
| Handle/ID | namespace + generation + row/index; stale and cross-owner rejection |
| Immutable atom | exact content equality with collision confirmation; owner-bound lifetime |
| Range | start/length over a retained owner; exact bounds and coordinate system |
| Joined/segmented view | ordered ranges, overflow-safe length, seam-aware iteration |
| Dictionary/index | immutable or versioned lookup, no accidental identity collapse |
| Tuple | ordered element identity; tuple identity distinct from element identity |
| Graph/DAG | node/edge semantics, parallel-edge policy, cycle policy, deterministic traversal where promised |
| AST | ordered children, source/attribution preservation, immutable node/version model |
| Pool/interner | canonicalization domain, weak/strong retention, concurrency and eviction |
| Precomputed fact store | fact key, validity, memory budget, invalidation, exact fallback |

Existing Synexia owners must be inspected before proposing another owner. Reuse only when contracts match.

## 6. Three implementation routes

### Route A — explicit M3 APIs on a stock JVM

Purpose: prove storage/algorithm designs without changing ordinary JDK classes.

Allowed:

- explicit `MIndexString`/M3 values;
- primitive collection APIs;
- immutable snapshots;
- adapters at Java API boundaries;
- named materialization.

Limitations:

- generic APIs still expose boxed/reference values;
- ordinary `String` and JDK collection identity remain ordinary JVM objects;
- JVM internals and native code do not automatically see M3 storage.

Acceptance is per explicit API.

### Route B — compiler lowering

Purpose: lower mechanically safe code to M3 representations while retaining stock-JVM fallback.

The compiler must refuse lowering when it cannot prove the semantic preconditions. The generated program must preserve source-visible behavior, including evaluation order and exception timing.

Typical candidates:

- locally non-escaping primitive collections;
- literal/constant String construction into an explicit indexed value;
- bounded immutable tuple/range composition;
- generated loops over primitive stores.

General object identity, synchronization, serialization, reflection and mixed unknown callers are fallback boundaries unless proven.

### Route C — matched custom JDK

Purpose: change internal JDK implementations behind ordinary Java APIs.

This route may replace internal representation while retaining the public type. It requires a matched source tree, exact build and per-subsystem VM/native acceptance.

Route C is not “Route A copied into `java.lang`”. Bootstrap, VM, GC, JIT, JNI, CDS and serviceability consumers must all be reconciled.

## 7. Whole-JDK subsystem programme

The detailed matrix lives in `jdk-subsystem-migration-matrix.md`. Initial dispositions are intentionally conservative.

| Subsystem | Initial disposition | Typical M3 opportunity | Primary risk |
| --- | --- | --- | --- |
| String/Unicode/encoding/regex | replace backend incrementally | immutable atoms, segmented ranges, precomputed facts | exact UTF-16, identity, JNI/JIT |
| Collections | replace backend per concrete class | flat lanes, compact entries, structural sharing | views, order, equality, serialization |
| Concurrent collections/atomics | retain semantics; adapt only with proof | compact lanes, fewer nodes | JMM, linearizability, progress |
| Streams/iterators/spliterators | adapt | specialized loops and primitive traversal | encounter order, laziness, side effects |
| I/O/NIO/files/networking | reuse/adapt | indexed paths, fewer staging copies | native ABI, resource lifetime, partial I/O |
| Numeric/math/time | retain pending evidence | precomputed immutable tables, primitive facts | precision and compatibility |
| Reflection/method handles/class loading/modules | retain/adapt narrowly | indexed metadata projections | identity, linkage, hidden classes |
| Compiler/javadoc/jshell | adapt | MIndexAST/DAG/indexed symbols | attribution, order, diagnostics |
| HotSpot interpreter/JIT | custom-JDK only | direct M3-aware intrinsics and layouts | deopt, barriers, code cache |
| GC/object layout/reference processing | custom-JDK only | lower retained heap graph | reachability and collector invariants |
| JNI/FFM/platform libraries | adapt | direct buffers/segmented projections | pinning, encoding, ABI |
| Security/crypto/providers | retain unless isolated proof | immutable tables/indexes | constant-time/security semantics |
| Serviceability/JFR/management | adapt after runtime layout | compact metadata | observability compatibility |
| Remaining JDK modules | inventory first | case-specific | accidental scope shrink |

No subsystem is declared “replaced” because one helper or facade exists.

## 8. Identity model

The following identities must remain distinct:

- Java object identity;
- content/value equality;
- M3 atom identity;
- tuple/composition identity;
- storage owner identity;
- generation/version identity;
- source symbol identity;
- mapping capability identity.

Examples:

- `IdentityHashMap` must continue comparing keys by reference identity.
- Equal mutable keys must not be globally interned.
- Two `String` objects with equal content may remain different Java objects even if they refer to one immutable content atom internally.
- Two AST nodes with equal spelling can differ by ordered children or attribution.
- Two graph edges with the same endpoints can differ by kind, direction, payload or multiplicity.

## 9. Mutability and structural sharing

Immutable data may share immutable backing when lifetime is explicit.

Mutable public collections require one of:

- exclusive mutable owner;
- copy-on-write generation;
- persistent structure with a mutable facade;
- versioned snapshot plus explicit mutation publication.

A mutable collection cannot be treated as one globally shared immutable atom. Backed views must observe the mutation relationship promised by the JDK surface.

## 10. Compatibility invariants

Every replacement specification must record:

- null policy;
- duplicate policy;
- equality/hashCode;
- comparator semantics;
- insertion/access/sorted/unspecified order;
- key mutation hazards;
- iterator removal;
- fail-fast versus weakly consistent versus snapshot iteration;
- spliterator flags and encounter order;
- subviews and backed views;
- clone/copy semantics;
- serialization stream form;
- subclass/protected hooks;
- capacity and overflow;
- allocation-failure behavior where observable;
- concurrent happens-before/atomicity/linearization;
- native/reflective names and descriptors.

## 11. Migration passes

### Pass 1 — pin and inventory

Pin:

- M3jdk21 source tree;
- private Synexia source tree;
- branch-scoped candidate receipts;
- donor references.

Generate a whole-source inventory before choosing replacement owners.

### Pass 2 — dependency/bootstrap map

For each owner, record:

- module;
- package;
- exported/internal status;
- native consumers;
- VM consumers;
- generated inputs/outputs;
- bootstrap phase;
- service/reflection/serialization dependencies.

### Pass 3 — mapping authority

Reconcile candidate records into the existing public mapping authority. Preserve stable IDs and historical receipts.

### Pass 4 — minimal foundations

Approve the smallest storage/identity/lifetime primitives required by the next subsystem. Do not import a mega-framework into `java.base`.

### Pass 5 — String and collections deep specification

Complete exact public/internal contract tables and differential oracles before backend replacement.

### Pass 6 — remaining subsystems

Extend the same discipline to I/O, compiler, metadata, serviceability, security and all remaining modules. Do not force unsuitable owners into one backend.

### Pass 7 — split routes

Create independent Route A, B and C work packets with explicit dependencies and fallback.

### Pass 8 — reusable transformations and rollback

Prefer source-pinned Maven/OpenRewrite recipes for Java source transformations. VM/native transformations need equally deterministic source-pinned mechanisms. Require idempotence, drift refusal and rollback.

### Pass 9 — measurement

Measure allocation, retained memory, throughput, latency, contention and precomputation cost on representative workloads.

### Pass 10 — omission audit

Reconcile every inventory item to:

- replacement;
- adapter;
- reuse;
- retain pending evidence;
- platform-specific;
- blocked;
- deferred;
- excluded custody/archive copy.

No silent unclassified row is allowed at programme acceptance.

## 12. Evidence model

“Bloat” is decomposed into measurable categories:

- object headers;
- reference width;
- wrapper/boxing count;
- node count;
- unused capacity;
- resize copies;
- temporary allocations;
- retained backing outside views;
- index/cache metadata;
- native/mapped bytes;
- generated code size;
- synchronization/atomic metadata.

For every candidate compare baseline and replacement under:

- tiny, medium and large sizes;
- sparse and dense data;
- cold and warm paths;
- mutation-heavy and read-heavy paths;
- adversarial collisions;
- long-lived retention;
- mixed readers/writers where applicable;
- platform-specific modes.

Record regressions as first-class results.

## 13. Promotion gates

A subsystem cannot move to accepted until applicable gates pass:

1. inventory complete for the subsystem;
2. contract matrix complete;
3. source-to-target mapping reconciled;
4. deterministic recipe and rollback;
5. differential API tests;
6. malformed/overflow/bounds tests;
7. serialization/ABI tests;
8. GC/lifetime tests;
9. concurrency/JMM stress where applicable;
10. exact custom-JDK image build for Route C;
11. VM/JIT/native/serviceability acceptance where layout changes;
12. performance and retained-memory measurement;
13. licensing/provenance review;
14. independent omission audit.

A benchmark improvement cannot waive a contract failure.

## 14. Rollout and rollback

Every Route C change must be independently gateable during migration.

Prefer:

- subsystem flags rather than one global “M3 on” switch;
- baseline fallback where technically possible;
- exact image/source receipts;
- reversible source recipe;
- no migration of persistent format without versioned readers/writers;
- no transfer of old evidence to a new combined tree.

Rollback must restore the reviewed prior representation without deleting unrelated later work.

## 15. Licensing and provenance

OpenJDK files retain their existing GPLv2 + Classpath Exception or other applicable OpenJDK notices. M3/Synexia source retains its own license where imported lawfully. Reference-only donor study does not authorize copying code.

Each source port must record:

- source repo and commit;
- file/symbol;
- license;
- whether code was copied, adapted or independently reimplemented;
- target file;
- recipe;
- notice changes.

Do not publish unrelated private Synexia source, user data, dictionaries or harvested corpora into the public JDK.

## 16. Definition of programme completion

Whole-JDK completion is not a percentage inferred from a handful of ports.

Completion requires:

- a pinned whole-tree inventory;
- every inventory row dispositioned;
- all accepted replacements mapped to exact source/target records;
- all required route-specific gates passed;
- no unresolved duplicate owner for an accepted capability;
- no stale mapping that claims behavior no longer present;
- retained evidence bound to the exact accepted code;
- a resume guide that identifies every remaining blocked/deferred item.

Until then, report the programme as partial and name the accepted bounded capabilities only.
