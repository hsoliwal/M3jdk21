# Whole-JDK M3 work packets, measurement plan and durable resume guide

Status: documentation-only execution handoff. This file organizes implementation work. It does not mark an implementation accepted, run tests, alter the operational mapping registry, or replace an installed JDK.

## 1. Mandatory read order

Before implementing a packet, read:

1. `m3/docs/whole-jdk-m3-architecture.md`
2. `m3/docs/jdk-subsystem-migration-matrix.md`
3. `m3/docs/collections-replacement-spec.md` when collections are in scope
4. `m3/docs/string-text-replacement-spec.md` when text is in scope
5. `m3/docs/name-mapping.json`
6. `m3/docs/migration-mapping-lifecycle.md`
7. `m3/docs/migration-worked-port-decisions.md`
8. the exact branch/commit-specific evidence for the packet

Do not treat a branch-only manifest or receipt as current master authority merely because it is newer or more detailed.

## 2. State vocabulary

Every work packet uses these independent dimensions.

### Source state

- discovered
- inspected
- contract-reviewed
- source-blocked

### Target state

- absent
- proposed
- implemented
- retained
- drifted
- removed

### Evidence state

- unexecuted
- compile-verified
- differential-verified
- runtime-verified
- performance-measured
- accepted

### Programme disposition

- replace-backend
- adapt
- reuse
- retain-pending-evidence
- platform-specific
- blocked
- deferred
- exclude-custody

Never compress these dimensions into one optimistic “done” flag.

## 3. Standard packet template

Every implementation packet must contain:

- packet ID;
- subsystem;
- exact source/target baseline commits;
- inventory rows and mapping IDs;
- current semantic owner;
- candidate owner/backend;
- route A/B/C applicability;
- bootstrap constraints;
- public/API/ABI/format contracts;
- native/VM/serviceability consumers;
- source transformations;
- recipe ID/version;
- before hashes;
- expected/derived postimage;
- refusal conditions;
- rollback;
- tests;
- benchmarks;
- evidence directory;
- unresolved decisions;
- promotion gate.

A packet that cannot name its exact owner and contract stays at inventory/contract review.

## 4. WP-00 — whole-source inventory and dependency graph

### Goal

Produce a pinned, repeatable inventory of the entire JDK fork and relevant Synexia MIndex/MatIndex families.

### Inputs

- exact M3jdk21 commit/tree;
- exact Synexia source commit/tree;
- current public mapping authority;
- build metadata.

### Outputs

- module/package/type/native/test inventory;
- public/protected contract references;
- VM/native/build/generated edges;
- bootstrap dependency graph;
- unclassified-row report.

### Acceptance

- repeated scan produces identical logical records;
- every tracked relevant item is classified or visibly excluded;
- omissions caused by platform overlays/generated source are explicit;
- private source bodies are not leaked into the public repository.

### Blocks

Every later “complete subsystem” claim.

## 5. WP-01 — mapping authority reconciliation

### Goal

Make one operational mapping authority capable of representing whole-JDK capability migration.

### Rule

Extend/reconcile `m3/docs/name-mapping.json` and its existing validator/toolchain in an implementation PR. Do not create a third registry from these architecture documents.

### Required record semantics

- stable capability ID;
- one-to-many and many-to-one sources/targets;
- rename/split/consolidation/deletion;
- owner;
- namespace/generation identity;
- API/ABI/format contracts;
- dependencies/downstream consumers;
- target adaptations;
- recipe;
- exact evidence;
- lineage;
- unresolved conflict.

### Acceptance

- legacy records preserved;
- validator rejects unsupported promotion;
- forward and reverse lookup;
- no evidence transferred to a changed owner without rerun.

## 6. WP-02 — bootstrap-safe M3 primitive foundation

### Goal

Define the minimal reusable primitives permitted in low JDK layers.

### Candidate atoms

- capacity arithmetic;
- primitive lanes;
- packed state bits;
- owner/generation handles;
- immutable range;
- bounded join descriptor;
- exact hash helpers;
- collision-confirmed lookup;
- generation-aware fact cache.

### Constraints

- no Maven/runtime framework dependency;
- no application DI;
- no optional accelerator required for correctness;
- no hidden thread;
- no unbounded cache.

### Acceptance

- strict compile;
- overflow/bounds;
- deterministic serialization/format if any;
- allocation/lifetime tests;
- source-bound recipe.

## 7. WP-10 — explicit M3 text Route A

### Goal

Provide/retain an explicit indexed immutable text API on a stock JVM as a semantic oracle and design proving ground.

### Dependencies

WP-00, WP-01, relevant WP-02 atoms.

### Required evidence

- exact UTF-16;
- value equality/hash;
- slices/joins;
- explicit materialization;
- regex and encoding boundaries;
- owner/interner lifetime;
- canonical factory identity only where specified.

### Non-goal

No claim that ordinary `java.lang.String` is replaced.

## 8. WP-11 — text compiler lowering Route B

### Goal

Lower only proven M3-typed or otherwise contract-safe expressions.

### Required refusal conditions

- unresolved type;
- identity-sensitive observation;
- unknown escape;
- reflection/native boundary;
- synchronization/monitor use;
- unsupported concat/bootstrap form;
- target drift.

### Acceptance

- evaluation order;
- side effects;
- nulls;
- exceptions;
- source maps/diagnostics;
- fixed point;
- rollback;
- baseline fallback.

## 9. WP-12 — custom String Route C

### Goal

Replace/adapt the internal backing of ordinary String in a matched JDK.

### Dependencies

WP-10/11 evidence is informative but not sufficient. WP-00, WP-01, WP-02 and full VM consumer inventory are mandatory.

### Work

- decide transitional/final String layout;
- M3 body/descriptor ownership;
- StringTable/intern;
- builders/joiner;
- regex/charset;
- JNI/JVMTI;
- interpreter/JIT;
- GC;
- CDS;
- serviceability.

### Promotion

Only an exact image whose mandatory gates pass.

## 10. WP-20 — immutable collection shapes

### Goal

Optimize JDK immutable factory products and internal immutable snapshots where contracts permit shared backing.

### Candidates

- shared empty/singleton shapes;
- flat reference lanes;
- compact immutable maps/sets;
- structural sharing when measured.

### Acceptance

- null/duplicate rejection;
- encounter order;
- equals/hashCode;
- serialization;
- independent copy versus view semantics.

## 11. WP-21 — ArrayList/reference-array family

### Goal

Reduce avoidable reference-lane overhead without changing List behavior.

### Candidate

A compact reference lane plus exact logical size/modification state.

### Required tests

- all constructors;
- bounds;
- add/remove/set;
- ensureCapacity/trim;
- iterator/listIterator;
- nested subList;
- self/aliased bulk mutation;
- clone;
- serialization;
- fail-fast best effort.

## 12. WP-22 — hash collection family

### Scope

HashMap, HashSet, LinkedHashMap/Set, IdentityHashMap, WeakHashMap, Hashtable and related views, split into separate implementation packets where semantics diverge.

### Candidate

Compact key/value/hash/state/index lanes with optional explicit encounter-order lanes.

### Special blockers

- IdentityHashMap: reference identity only;
- WeakHashMap: reference processing/lifetime;
- Hashtable: synchronization/legacy contract;
- LinkedHashMap: access/insertion order and subclass hook.

### Acceptance

Collision/adversarial, callback/reentrancy, views, serialization, iterator behavior and memory.

## 13. WP-23 — ordered/navigation collection family

### Scope

TreeMap/Set and navigable views.

### Candidate

Packed tree index lanes or paged sorted lanes.

### Acceptance

Comparator identity, navigation, nested views, descending views, entry mutation, iterator behavior, serialization, complexity tradeoff.

## 14. WP-24 — deque/queue/heap family

### Scope

ArrayDeque, PriorityQueue and nonblocking single-threaded internal queues.

### Candidate

Ring/reference lane and heap/reference lane using shared scalar arithmetic.

### Acceptance

Null rules, ordering, occurrence removal, iterator behavior, growth, serialization.

## 15. WP-25 — copy-on-write family

### Scope

CopyOnWriteArrayList/Set.

### Candidate

Immutable generation with optional chunk sharing.

### Acceptance

Snapshot iterators, mutation lock/atomicity, memory visibility, old-generation retention, independent arrays.

## 16. WP-26 — concurrent map/queue/deque family

### Goal

Reduce node/metadata overhead without weakening JMM or progress.

### Rule

Each owner is a concurrency algorithm packet, not a storage-only rewrite.

### Required design evidence

- linearization points;
- happens-before edges;
- atomics/fences;
- resize/help protocol;
- lock/condition protocol where applicable;
- weak/snapshot traversal;
- progress guarantee;
- ABA/generation considerations;
- stress testing.

### Promotion

JCStress-style or equivalent memory-model stress plus functional/oracle tests. A single-threaded benchmark is irrelevant to concurrency acceptance.

## 17. WP-30 — streams, iterators and spliterators

### Goal

Use accepted M3 backends without boxing/internal nodes when specialization is possible.

### Preserve

- laziness;
- encounter order;
- side effects;
- short-circuit;
- close handlers;
- parallel split;
- exact spliterator characteristics.

### Acceptance

Differential pipelines including stateful/stateless, sequential/parallel and exceptional callbacks.

## 18. WP-40 — path, I/O and NIO

### Goal

Evaluate indexed path representation and reduced staging copies.

### Scope

Path providers, Files, channels, buffers, mapped buffers, zipfs and native I/O.

### Acceptance

- provider-specific equality;
- symlinks;
- normalization/resolution;
- partial reads/writes;
- direct/mapped lifetime;
- close/interrupt/asynchronous behavior;
- platform path encodings.

A logical MIndexPath is not automatically a replacement for every provider's Path object.

## 19. WP-41 — networking

### Goal

Optimize immutable parsing/cache metadata only where security/platform semantics remain exact.

### Preserve

DNS/cache policies, addresses, sockets, selectors, errors, native ABI and security manager-era compatibility surfaces retained by JDK 21 APIs.

## 20. WP-50 — compiler AST/symbol/index integration

### Goal

Use MIndexAST/MIndexDag/indexed symbols as immutable projections or approved owners without losing javac semantics.

### Preserve

- ordered AST children;
- source spans;
- attribution;
- symbols/types;
- diagnostics;
- annotation processing;
- plugins;
- file manager;
- incremental invalidation.

### Partial-AST rule

LRU/secondary residency is an optional memory policy. Eviction may discard derived/resident state, never semantic source ownership or stale-generation validation.

## 21. WP-51 — reflection/invoke/class loading/modules

### Goal

Reduce duplicate immutable metadata only after linkage/lifetime proof.

### High-risk areas

- weak/hidden class lifetime;
- MethodHandle species;
- call sites;
- LambdaMetafactory;
- constant pools;
- loader namespaces;
- module readability/exports;
- reflection object caching.

Default disposition is retain/adapt, not replace wholesale.

## 22. WP-60 — VM object layout/GC

### Goal

Support accepted Route C owners.

### Work

- object field/layout contract;
- tracing;
- barriers;
- reference processing;
- compressed oops/class pointers;
- heap iteration;
- dump/SA metadata;
- collector-specific paths.

### Promotion

Every supported collector/configuration in scope must pass exact candidate tests or remain explicitly unsupported/blocked.

## 23. WP-61 — interpreter/JIT/intrinsics

### Goal

Make accepted representations executable in interpreted and compiled modes.

### Work

- interpreter templates;
- C1/C2;
- intrinsics;
- escape analysis assumptions;
- deoptimization;
- JVMCI if exposed;
- mixed transitional representations.

### Acceptance

Interpreter + compiled + tier transitions + deopt + uncommon cases.

## 24. WP-62 — native/JNI/JVMTI/FFM

### Goal

Preserve all native boundaries.

### Rule

Copy/materialize when required for correctness; optimize the copy only after the boundary is proven.

### Acceptance

ABI, pin/release, modified UTF-8, direct-buffer alignment/capacity, agent/service events, platform builds.

## 25. WP-63 — CDS/classfile/serviceability/JFR

### Goal

Ensure a changed runtime remains buildable, archiveable, observable and diagnosable.

### Acceptance

- CDS creation/use;
- class loading/constant resolution;
- heap/serviceability inspection;
- JFR events;
- jcmd/jmap/jstack where applicable;
- SA;
- management counters.

A runtime representation invisible to tools is not production-ready.

## 26. WP-70 — security/crypto/provider review

### Default

Retain implementation unless a bounded optimization has security-specific evidence.

### Required review

- constant-time concerns;
- secret lifetime/zeroization;
- provider order;
- native key handles;
- serialization/keystore formats;
- FIPS/provider constraints where applicable.

Memory saving alone is not sufficient.

## 27. WP-80 — remaining module sweep

Run the inventory against every module not covered above.

For each module, create bounded packets only when a concrete owner/overhead/opportunity exists. “Remaining modules” may not be marked complete as one catch-all row.

## 28. Route comparison

| Property | Route A explicit API | Route B compiler lowering | Route C custom JDK |
| --- | --- | --- | --- |
| Stock JVM | yes | yes, for emitted compatible code | no |
| Ordinary Java API unchanged | no for explicit type use | source may remain ordinary only at proven sites | yes by goal |
| VM layout change | no | no | possible |
| Boxing removal | explicit primitive APIs | possible at proven local sites | possible internally; generic boundaries remain |
| Fallback | explicit conversion | mandatory refusal/fallback | baseline representation/flag during migration where feasible |
| Evidence unit | explicit type | recipe/site class | exact JDK image/subsystem |
| Risk | bounded | semantic lowering | whole runtime |

## 29. Performance and memory measurement plan

### 29.1 Memory categories

Record separately:

- object count;
- headers;
- reference bytes;
- primitive payload;
- metadata lanes;
- boxed wrappers;
- unused capacity;
- peak resize overlap;
- temporary allocations;
- retained backing outside views;
- cache/fact metadata;
- native allocations;
- mapped bytes;
- generated code/code cache;
- synchronization objects/atomic metadata.

### 29.2 Workloads

Every relevant subsystem includes:

- empty/tiny;
- small;
- medium/cache-resident;
- large;
- sparse;
- dense;
- cold admission;
- warm repeated operation;
- mutation-heavy;
- read-heavy;
- adversarial collisions/input;
- long-lived retention;
- concurrency mixes;
- platform variants.

### 29.3 Metrics

- allocation bytes/op;
- retained heap after stabilization;
- mapped/native retained bytes;
- throughput;
- p50/p95/p99 latency when meaningful;
- CPU cycles/time;
- GC count/time;
- contention;
- cache/fact hit/miss/admission/eviction;
- code size where relevant.

### 29.4 Measurement rule

Compare the exact baseline and candidate with identical workload/input/environment.

No result from an earlier candidate transfers to a changed tree.

## 30. Precomputation admission policy

Every precomputed fact has:

- key;
- exact source owner/generation;
- derivation;
- byte cost;
- admission cost;
- expected reuse;
- invalidation;
- concurrency;
- eviction;
- fallback.

Suggested states:

- ALWAYS_INLINE: tiny fact stored in owner;
- LAZY_OWNER: computed once per immutable owner;
- BOUNDED_CACHE: optional reusable fact with eviction;
- SIDECAR: versioned derived image;
- EPHEMERAL: operation-local.

Do not create an unbounded global cache to save local CPU.

## 31. Reusable transformation protocol

For Java-source implementation work, prefer a reusable Maven/OpenRewrite recipe plus tests.

A recipe must prove:

- exact source applicability;
- target precondition;
- deterministic postimage;
- fixed point;
- source/target drift refusal;
- partial-state refusal/recovery;
- rollback;
- test association.

Native/VM/build source may use a different transformation mechanism, but must meet the same source-pinned deterministic rules.

The recipe is the reusable implementation of the change; manually editing hundreds of targets without a reusable transformation is the exception requiring justification.

## 32. Future upstream enhancement procedure

Given a mapping with source synchronized at S0 and target synchronized at T0:

1. pin new source S1;
2. diff S0..S1;
3. inventory every changed/deleted/renamed symbol and dependent format/test;
4. map each item to existing capability IDs;
5. classify bug/feature/performance/contract/format/test/dependency change;
6. diff target T0..current T1;
7. detect target adaptations/divergence;
8. choose adopt/adapt/defer/not-applicable with reason;
9. update recipe;
10. replay against exact preimage;
11. run required gates;
12. record candidate T2 and receipts;
13. update mapping sync only after all source changes are dispositioned.

Blind overwrite is prohibited.

## 33. Rename example

Source `A.OldOwner` becomes `B.NewOwner` with unchanged behavior.

Do:

- preserve stable capability ID;
- update source qualified symbol/path/hash;
- inspect reflective/serialization/native names;
- preserve target-specific package adaptation;
- run behavior tests.

Do not create a new “completed” capability solely because the filename changed.

## 34. Split example

One source owner splits into storage + admission policy.

Do:

- preserve lineage to the original capability;
- enumerate both new source owners;
- choose whether target also splits;
- bind lifetime and cache policy;
- preserve existing target adaptation.

Do not silently duplicate canonical ownership.

## 35. Consolidation example

Several source helpers become one M3 atom.

Do:

- prove their contracts compatible;
- retain source relationships in mapping;
- list all downstream consumers;
- test every original surface;
- preserve provenance.

Same suffix or duplicate code is not enough evidence for consolidation.

## 36. Conflict example

Upstream changes substring exception semantics while target has a stricter explicit view.

Resolution:

- keep explicit view contract separate;
- port String-compatible behavior to the owning/String route;
- update compiler lowering to select the correct semantic operation;
- test exception class/timing and all UTF-16 boundaries.

Do not text-merge two incompatible operations into one.

## 37. Failed gate example

Illustration:

- candidate compact ConcurrentHashMap uses 35% less retained heap;
- functional single-threaded tests pass;
- concurrency stress finds a retrieval can miss a completed same-key update because publication uses insufficient ordering.

Disposition:

- performance result retained as diagnostic;
- candidate status = blocked;
- no promotion;
- mapping evidence state does not advance to accepted;
- fix requires a new candidate and rerun;
- old memory measurement cannot certify the repaired implementation.

Correctness gate dominates memory win.

## 38. Rollback

Every packet defines rollback before promotion.

Rollback types:

- source recipe reverse;
- feature flag to baseline backend;
- persistent-format reader compatibility + generation switch;
- branch revert after verifying no later dependency;
- cache disable for optional precomputation.

Rollback must not:

- delete unrelated later changes;
- reuse stale IDs/generations;
- relabel old receipts;
- leave mixed format without a reader.

## 39. Licensing/provenance packet

For every imported/adapted source body record:

- donor/source repository;
- exact commit;
- path/symbol;
- license;
- original notice;
- copied/adapted/independently implemented;
- target path;
- transformation recipe;
- notice update.

Problem sites such as LeetCode, HackerRank and GeeksforGeeks may be used as problem/taxonomy references. Do not copy submissions/editorials without a compatible source/license basis.

## 40. Resume checklist

When resuming after any interruption:

1. read the mandatory documents in section 1;
2. fetch current master and mapping authority;
3. identify open whole-JDK documentation/implementation PRs;
4. compare current target with the last synchronized target pin;
5. fetch the exact private source pin through authorized access;
6. rerun/inspect the inventory before assuming source-family coverage;
7. select the smallest dependency-ready packet;
8. docs/spec changes first when a contract is missing;
9. implement by reusable recipe where applicable;
10. execute only the gates whose results are claimed;
11. bind receipts to the exact candidate;
12. update mapping/resume docs in the same reviewed change.

Do not require the user to rediscover omitted subsystems manually. The inventory/unclassified report is the omission detector.

## 41. Open decisions that implementation must resolve

These remain decisions, not implied conclusions:

- final canonical M3 naming/compatibility policy in the JDK tree;
- exact String transitional/final object layout;
- StringTable relationship to M3 canonical atoms;
- descriptor structure and compaction thresholds;
- mapped shared-image publication/lifetime protocol;
- which collection owners benefit from compact lane replacement;
- whether HashMap uses open addressing, compact chaining or hybrid;
- concurrent collection algorithms versus storage-only reuse;
- exact primitive-specialization strategy for compiler lowering;
- bootstrap subset ownership/location;
- accepted precomputation budgets;
- persistent image formats/versioning;
- supported platform/collector matrix for initial Route C release.

Resolve them with measurements and contract evidence, not architectural preference.

## 42. Handoff completion criterion

This documentation handoff is useful when an implementation contributor can:

- find the authoritative registry;
- identify the next packet;
- see dependencies;
- locate required contracts;
- know what must be tested;
- know what does not yet count as evidence;
- add a future source enhancement without losing target adaptations;
- resume after interruption without asking for the user to list obvious omissions.

Implementation completion remains a separate, evidence-gated programme.


## 43. Migration progress: primitive collections seed (2026-10-02)

The first dependency-ready collection implementation packet is now represented by these operational
mapping IDs:

- `m3.collections.primitive-foundation`
- `m3.collections.long-deque-route-a`
- `m3.collections.long-long-hash-map-route-a`

Exact tested packet candidate: `baea39f7629abda59b5f584602589feca42bac58`.

Implemented scope:

- separate `com.m3.collections` Route-A module outside `java.base`;
- primitive long collection/deque contracts and a packed circular `long[]` deque;
- primitive long-to-long contract and open-addressed `long[]/long[]/byte[]` map;
- shared capacity, ring and hash-probe atoms;
- exact-postimage source-bound installer with unchanged `ArrayDeque.java` and `HashMap.java` guards;
- Maven/OpenRewrite installer authored with exact postimage resources and refusal/fixed-point tests.

Executed evidence:

- strict Java 21 compile;
- 1,006,642 differential/focused checks in normal mode;
- 1,006,642 checks under `-Xint`;
- two source-bound recipe unit tests after the final documentation/manifest rebind;
- deterministic module jar hash with a pinned archive timestamp.

Open gates remain explicit:

- Maven/OpenRewrite execution is unexecuted because Maven was unavailable;
- no `java.util` implementation has been replaced;
- no generic boxing/view/serialization/subclass compatibility is claimed;
- no concurrent collection/JMM claim exists;
- no throughput or whole-heap benchmark has been accepted.

The next JDK-facing collection packet should start from `ArrayDeque`: inventory every public,
iterator/spliterator, clone/serialization, null, growth and overflow obligation, then adapt the
accepted ring/storage atoms behind a candidate without weakening the ordinary Java API. `HashMap`
follows only after its larger equality/view/callback/collision/serialization contract is fully
mapped.
