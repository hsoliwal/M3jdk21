# Whole-JDK M3 migration: scope, architecture and acceptance

Status: documentation-only planning contract, 2026-10-02. Inspected default branch: `master` at `45f546ff5bcb06a1b2604f14baf998785d98d9a1`. No implementation or operational mapping is added or promoted here.

## Documentation topology and authority

This file is the whole-program overview and inventory denominator. It is not a second operational mapping registry.

- [../migration/docs/ARCHITECTURE.md](../migration/docs/ARCHITECTURE.md) contains the shared architecture and semantic laws.
- [../migration/docs/COVERAGE.md](../migration/docs/COVERAGE.md) is the human-readable coverage framework; operational capability state remains in [name-mapping.json](name-mapping.json).
- [../migration/docs/COLLECTIONS.md](../migration/docs/COLLECTIONS.md) is the deep collections replacement specification.
- [../migration/docs/WORK_PACKETS.md](../migration/docs/WORK_PACKETS.md) decomposes the whole JDK into dependency-aware implementation packets.
- [../migration/docs/ACCEPTANCE.md](../migration/docs/ACCEPTANCE.md) defines universal and subsystem promotion gates.
- [../migration/docs/RESUME.md](../migration/docs/RESUME.md) is the durable contributor continuation guide.
- [mindex-migration-handoff.md](mindex-migration-handoff.md), [shared-atom-concatenation.md](shared-atom-concatenation.md) and [../../doc/mindex-string-backing.md](../../doc/mindex-string-backing.md) remain the deep String/MIndex slice documents.

Historical receipts, branch-specific manifests and dated evidence remain pinned historical records. They are not silently upgraded by this planning document.

## Scope decision

The migration program covers **all JDK parts**, including the collections framework, concurrent collections and JVM/runtime integration. String, shared text atoms and MIndex-family ports are one vertical slice of that program, not its outer boundary. The goal is to replace redundant representations and operations with reviewed M3 owners/backends wherever compatibility and measured benefit justify the design.

“All parts” is the inventory and review obligation. It does not establish that every API can use one representation, that every replacement is already feasible, or that every implementation should be rewritten. Every surface needs a disposition: replacement, adapter, specialization, retained dependency, deferred/blocked work or justified exclusion. A retained dependency remains visible for future review; it cannot silently disappear from the denominator.

The [consolidated copy-ready assignment](whole-jdk-consolidated-prompt.md) requests documentation and explanation only. Other contributors implement through separately authorized, source-pinned changes.

This scope supersedes String-only framing for future planning. Preserve [shared-atom-concatenation.md](shared-atom-concatenation.md), [stages.md](stages.md), the [MIndex handoff](mindex-migration-handoff.md) and historical receipts as dated slice-specific evidence. This document does not rewrite their pinned history or turn historical passes into current acceptance.

## 1. Coverage denominator and taxonomy

The [source root at the inspected commit](https://github.com/hsoliwal/M3jdk21/tree/45f546ff5bcb06a1b2604f14baf998785d98d9a1/src) contains the following **70 java.* / jdk.* source directories**. This is a directory census, not proof that all are built, shipped, supported on every platform, or contain a completed semantic inventory:

- `java.base`
- `java.compiler`
- `java.datatransfer`
- `java.desktop`
- `java.instrument`
- `java.logging`
- `java.management.rmi`
- `java.management`
- `java.naming`
- `java.net.http`
- `java.prefs`
- `java.rmi`
- `java.scripting`
- `java.se`
- `java.security.jgss`
- `java.security.sasl`
- `java.smartcardio`
- `java.sql.rowset`
- `java.sql`
- `java.transaction.xa`
- `java.xml.crypto`
- `java.xml`
- `jdk.accessibility`
- `jdk.attach`
- `jdk.charsets`
- `jdk.compiler`
- `jdk.crypto.cryptoki`
- `jdk.crypto.ec`
- `jdk.crypto.mscapi`
- `jdk.dynalink`
- `jdk.editpad`
- `jdk.hotspot.agent`
- `jdk.httpserver`
- `jdk.incubator.vector`
- `jdk.internal.ed`
- `jdk.internal.jvmstat`
- `jdk.internal.le`
- `jdk.internal.opt`
- `jdk.internal.vm.ci`
- `jdk.internal.vm.compiler.management`
- `jdk.internal.vm.compiler`
- `jdk.jartool`
- `jdk.javadoc`
- `jdk.jcmd`
- `jdk.jconsole`
- `jdk.jdeps`
- `jdk.jdi`
- `jdk.jdwp.agent`
- `jdk.jfr`
- `jdk.jlink`
- `jdk.jpackage`
- `jdk.jshell`
- `jdk.jsobject`
- `jdk.jstatd`
- `jdk.localedata`
- `jdk.management.agent`
- `jdk.management.jfr`
- `jdk.management`
- `jdk.naming.dns`
- `jdk.naming.rmi`
- `jdk.net`
- `jdk.nio.mapmode`
- `jdk.random`
- `jdk.sctp`
- `jdk.security.auth`
- `jdk.security.jgss`
- `jdk.unsupported.desktop`
- `jdk.unsupported`
- `jdk.xml.dom`
- `jdk.zipfs`

Also inventoried at this root: `hotspot`, `demo` and `utils`. Include `make/`, configure/build support, `test/`, resources, generated sources and their generators, launchers, native libraries, packaging/image tooling and bundled dependencies/licenses. A future source update must reconcile additions/removals against this denominator. Do not use a prefix search or a list of prominent public classes as a whole-JDK census.

For every directory/module, enumerate packages, public/protected and relevant internal symbols, overloads/constructors/default methods, fields, nested classes, module descriptors, services/providers, native entry points, formats/resources, tests and platform variants. Attribute dependency edges and reverse consumers. Enumerate generated outputs through their inputs and producer. Record unavailable sources and unresolved symbols as gaps.

Use these workstreams to organize the inventory, without creating another mapping registry:

| Workstream | Required coverage and key boundaries |
| --- | --- |
| Core language and value substrate | Object identity, String/text, primitive wrappers, arrays, numbers/math, exceptions, references, class metadata, reflection, method handles, invokedynamic, class loading and modules |
| Collections and algorithms | List/Map/Set/Queue/Deque, sorted/navigable/sequenced families, primitive specializations, iterators, spliterators, Arrays/Collections helpers, comparators, streams and collectors |
| Concurrency | Concurrent collections, atomics/VarHandles, locks/synchronizers, executors/futures, fork-join, virtual/platform threads, cancellation/interruption and scheduler boundaries |
| Data and IO | IO/NIO/channels/filesystems, direct/mapped memory, charsets, compression/archive formats, serialization, time/date/locales, regex, random APIs and resources |
| Networking and security | Sockets/HTTP/DNS/RMI, TLS, authentication, cryptographic providers, certificates/keystores, permissions and native/platform integration |
| Higher-level platform | SQL/JDBC, naming, XML, scripting, desktop/graphics/fonts/audio/accessibility, preferences, logging, instrumentation and management |
| Toolchain and distribution | javac/javadoc/jar/jdeps/jlink/jpackage/jshell, debug agents, launchers, image layout, cross-compilation, module resolution, service binding and native packaging |
| HotSpot and platform runtime | Interpreter, C1/C2/compiler interface/JVMCI, intrinsics and CPU stubs, allocation/object layout, GC/barriers/references, class linking/bootstrap, monitors, safepoints/handshakes, deoptimization, CDS, JNI/JVMTI, JFR, SA/debugging and OS/CPU ports |

The HotSpot inventory must cover `src/hotspot/share` **and** `cpu`, `os`, `os_cpu` variants; directory presence is only a starting point. Every workstream needs a named semantic/storage owner, route eligibility, phase, dependency closure, compatibility risks, verification plan and source-bound evidence status. No whole-JDK completion percentage is meaningful until the denominator and exclusions are reviewed.

## 2. Architecture: common ownership, distinct semantics

Use canonical M3 backends with public compatibility surfaces. Canonical means one reviewed responsibility/ownership model per semantic domain; it does **not** mean one universal object layout, one global pool or one process-global identity namespace.

Separate:
- Immutable value atoms, reusable facts and composition metadata
- Mutable collection storage and independently observable collection objects
- Application object identity, equality/hash semantics and comparator-defined equivalence
- Heap references, arena-local handles and persistent image offsets/generations
- Synchronization/publication protocols and read-only cross-process images
- Compatibility adapters that expose the original API and explicit materialization boundaries

A wrapper over a backend still costs memory and may allocate. Use wrappers where their behavior is provable, specialize where justified, and document unavoidable duplication/copies. Do not declare a layout change compatible solely because public method names match.

Immutable shared atoms can back text or immutable collection data, subject to owner/lifetime and equality contracts. Two equal mutable lists/maps must not be interned into the same mutable object. Mutating one must not unexpectedly mutate another. Persistent/copy-on-write storage is a candidate technique with explicit aliasing and cost rules, not automatic JDK compatibility. Immutable elements do not make a mutable container immutable.

`IdentityHashMap` uses reference equality for keys and values. Replacing distinct equal keys by canonical value atoms changes its meaning; preserve the references/identity domain or reject that optimization. See its [Java 21 contract](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/IdentityHashMap.html). Likewise preserve weak-reference reachability, monitor identity, identity hash behavior, subclass-visible state and externally observable ownership.

Do not move all M3 classes into `java.lang`. Place bootstrap-safe internals according to actual dependency and module rules. Keep tooling, Maven/OpenRewrite and application dependency graphs outside early `java.base` bootstrap. Higher modules consume reviewed lower-level contracts without reversing the module graph.

## 3. Three routes for every eligible family

| Route | Generalized whole-JDK contract | Collections example and proof boundary |
| --- | --- | --- |
| A: explicit M3 API | Opt-in APIs over reviewed M3 owners on a stock JVM; adapt to standard APIs when needed | Explicit primitive/compact storage with List/Map adapters where lawful. Boxing, copying, identity and unsupported operations are documented. Does not replace ordinary JDK objects |
| B: compiler lowering | Transform only attributed operations whose behavior is proven to fit the same backend; otherwise preserve or refuse | Selected construction/operations lowered after overload, generic type, alias/escape, null, callback, exception, ordering and identity analysis. Unresolved reflection/subclass/external ABI use blocks unsafe rewrites |
| C: matched custom JDK | Preserve required public platform contracts while changing internals in a coherent complete image | Standard collection implementations may use M3 internals after library/bootstrap tests; VM-sensitive changes additionally require matched interpreter/JIT/GC/native/serviceability integration |

B must preserve evaluation order, number/timing of callbacks, method dispatch, exceptions, synchronization and observable aliases. Do not substitute constructors or standard classes using textual renames. Reflection, serialization, native consumers and separately compiled code are explicit boundaries. Recipe before/after source maps, semantic preconditions and refusal cases must be retained.

C is not an instruction to transplant class files into an installed JDK. Library-only tests do not prove runtime integration. Full image builds, platform tests and exact image identities are separate gates. Route A performance or correctness does not certify B or C.

## 4. Collections replacement contract

The [Java 21 collections overview](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/package-summary.html) includes both modern and legacy collections. Inventory interfaces, abstract bases, concrete implementations, factories, wrappers and helper algorithms rather than treating `Collection` as the entire scope; `Map` is a separate hierarchy.

### Required family coverage

- Lists: ArrayList, LinkedList, Vector/Stack, immutable/fixed-size/unmodifiable/synchronized/checked wrappers, sublists and ListIterator
- Maps/sets: HashMap/HashSet, linked-order variants, TreeMap/TreeSet, EnumMap/EnumSet, IdentityHashMap, WeakHashMap, Hashtable/Properties; entry/key/value views and mutable entries
- Queues/deques: ArrayDeque, PriorityQueue and all blocking, bounded, transfer, delay, concurrent and priority variants
- Sorted/navigable APIs: comparator identity/behavior, range inclusivity, descending views, boundary entries and mutation through views
- Java 21 sequenced APIs: encounter order, first/last operations and reversed live views where specified; inspect each implementation's restrictions
- Concurrent maps/sets/queues and copy-on-write collections; atomic variables/arrays/references, locks and synchronizers as connected but distinct owners
- Arrays/Collections algorithms, primitive arrays/adapters, comparators, iterators/enumerations, spliterators, streams/parallel streams and collectors

This is a required family checklist; the symbol census must find the complete set, including implementation-specific and platform-generated surfaces.

### Compatibility matrix for each operation

Document inputs/results, mutability and aliases, optional operations, structural modification, size/capacity limits and errors. Cover:
1. Null acceptance/rejection for elements, keys, values, comparators and callbacks; no universal null policy
2. Duplicates, List/Set/Map/Entry equals/hash contracts, ordering, comparator consistency and behavior with mutable keys/elements
3. Insertion/access/encounter order, tie behavior where specified and deliberately unspecified order; never promise incidental implementation order as a spec guarantee
4. Live views versus snapshots; subList, reversed, descending, keySet, values, entrySet, iterator remove/set/add, Map.Entry.setValue, and range-view aliasing
5. Fail-fast best-effort behavior versus snapshot/weakly consistent iteration; do not treat ConcurrentModificationException as a synchronization guarantee
6. Spliterator characteristics, size estimates, splitting, encounter order, stream laziness, short-circuiting, collector characteristics and parallel safety
7. Bulk/default operations and callback reentrancy, side effects and exceptions; compute/merge/replaceAll and partial updates where specified
8. Boxing/unboxing, primitive specialization and round trips: null, wrapper reference identity where observable, numeric equality/hash, NaN and signed-zero rules
9. Public/protected constructors, subclass hooks and inherited behavior; reflection, runtime class identity, binary linkage, generic signatures, bridges, serialization form/proxies/UIDs and old serialized data
10. Capacity growth, resize/rehash, overflow and allocation-failure paths, without weakening existing tests just because storage changes

### Concurrency and lifetime gates

The [concurrency package contract](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/package-summary.html) and [JLS memory model](https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html) govern visibility and ordering. Define happens-before edges, safe publication, volatile/final/atomic access and per-method atomicity. Specify a linearization argument for each operation whose contract promises atomic behavior; do not claim an entire bulk traversal or size observation is a single atomic snapshot.

Test mixed readers/writers, resize, contention, callback reentrancy, cancellation, interruption, timeout, capacity pressure and weakly consistent observations. Document progress/fairness guarantees only where promised; a lock-free primitive does not make the entire collection lock-free.

GC must see all live object references. Off-heap IDs cannot replace references without reviewed reachability/barrier/root integration. Owners must survive views, iterators, asynchronous consumers and native calls. Define reclamation, ABA/generation reuse, close/eviction, weak references, cleaners and cross-process reader lifetime. Mutable cross-process collections require a separately specified synchronization, recovery and ownership protocol; a shared immutable lexicon is not that protocol.

## 5. Measured bloat reduction, not assumed speed

Pinned source illustrates different baseline costs: [ArrayList](https://github.com/hsoliwal/M3jdk21/blob/45f546ff5bcb06a1b2604f14baf998785d98d9a1/src/java.base/share/classes/java/util/ArrayList.java) has an Object-array backing and size; [HashMap](https://github.com/hsoliwal/M3jdk21/blob/45f546ff5bcb06a1b2604f14baf998785d98d9a1/src/java.base/share/classes/java/util/HashMap.java) has entry nodes with hash/key/value/next fields. These observations identify hypotheses, not universal byte counts.

Measure separately: object headers/alignment, reference width, boxing objects, node/link overhead, backing arrays and slack capacity, resize/rehash transient peaks, adapters/views/iterator allocation, synchronization metadata, cache/index/fact storage, retained heap, native/mapped memory, GC work and class/code footprint. Report shared pages without double-counting per-process RSS as uniquely owned memory. A tiny live view can retain a large owner.

Compare stock JDK and candidate under the same workload, image/flags, GC, hardware and environment. Include empty/small/large collections, primitive/reference payloads, duplicates, sparse/dense occupancy, hit/miss patterns, collision/adversarial keys, ordering, mutation/iteration mixes, read/write contention, cold startup and steady state. Record warmup, forks, distributions, latency tails, throughput, CPU, allocated bytes, retained memory and peak memory. Charge conversion, admission, precomputation and cleanup separately and in end-to-end totals; test escape-analysis/dead-code-elimination pitfalls.

Primitive lanes, compact metadata, chunked storage and structural sharing are candidates to compare, not promised wins. Report regressions and break-even points. Lower allocation does not establish lower CPU or latency. Any numeric speed/memory target remains a target until source-bound measurements support it.

## 6. Dependency-led phases and stop gates

These whole-program phases supplement the historical text-specific P0–P3 stages; they do not renumber or overwrite them.

| Phase | Required outcome before promotion |
| --- | --- |
| W0: inventory and contracts | Complete selected-module/surface/dependency census, owner decisions, provenance/publication review, dispositions and explicit unknowns |
| W1: independent substrate | Validate immutable/mutable owner separation, identity, lifetimes and Route A compatibility outside bootstrap; compare primitive/collection designs without claiming platform replacement |
| W2: java.base closure | Review bootstrap dependency cycles and minimal initialization, collection/text/IO/concurrency interactions, allocation failure and reentrancy; retain stock fallback where required |
| W3: collection and lowering vertical slices | Differential operation tests, alias/subclass/serialization checks, concurrency/JMM stress where relevant, attributed compiler refusal/replay gates and workload measurements |
| W4: matched runtime integration | Complete exact-candidate image; interpreter/C1/C2/intrinsics, GCs, JNI/JVMTI, CDS/JFR/serviceability and OS/CPU gates for affected surfaces |
| W5: remaining modules and distribution | Migrate in dependency order, verify service/native/external ABI consumers, module images, packaging, platform matrix and applicable conformance suites |
| W6: sustained upstream porting | Review every changed symbol/format/dependency, replay source-pinned recipes, preserve target adaptations, revalidate affected evidence and retain unresolved dispositions |

Dependency edges may force an earlier investigation of a later workstream. Do not enable a lower layer that recursively needs itself via logging, exceptions, class loading, atomics, maps, allocation or service discovery. Build/test tools are not runtime dependencies. Deferred modules and unsupported modes stay named and disabled where appropriate; no full-JDK acceptance while mandatory gates remain open.

## 7. Mapping authority and future enhancement ports

At the inspected base, [name-mapping.json](https://github.com/hsoliwal/M3jdk21/blob/45f546ff5bcb06a1b2604f14baf998785d98d9a1/m3/docs/name-mapping.json) retains legacy directions **and 25 migration.records**, governed by [name-mapping.schema.json](https://github.com/hsoliwal/M3jdk21/blob/45f546ff5bcb06a1b2604f14baf998785d98d9a1/m3/docs/name-mapping.schema.json). [migration.py](https://github.com/hsoliwal/M3jdk21/blob/45f546ff5bcb06a1b2604f14baf998785d98d9a1/m3/migration/migration.py) consumes this schema, and [RESUME.md](https://github.com/hsoliwal/M3jdk21/blob/45f546ff5bcb06a1b2604f14baf998785d98d9a1/m3/migration/RESUME.md) directs contributors to extend this authority. Existing records explicitly leave whole-source/dependency coverage incomplete.

Do not mistake directory presence for registry identity:
- `m3/migration/` is present on this base; `m3/migration/manifest.json` is absent in the inspected directory listing
- `m3/recipes/manifest.json` is an existing recipe/hash authority, not the semantic migration registry
- `m3/docs/migration-mapping.schema.json` and its example are illustrative proposals, not a replacement operational registry
- [Draft #12's separately pinned manifest](https://github.com/hsoliwal/M3jdk21/blob/d543255294ae85e4c8015812a3c8a97aaf498354/m3/migration/manifest.json) belongs to that feature stack; reconcile deliberately if its work is selected
- [Draft #22](https://github.com/hsoliwal/M3jdk21/pull/22) proposes correction of older mapping prose. Read its actual diff/state rather than assuming it is merged

For new collection/module/runtime ports, extend the selected existing authority after schema/validator review. Preserve stable IDs and source/target history. Map every relevant symbol, format version, native ABI, resource, generator, VM layout/offset/intrinsic, module/service surface and test obligation. Relationships can be many-to-many: adapter, split, consolidation, specialization or retained dependency. Never merge owners merely because their names share a prefix.

Reuse current record fields for source/target pins, contract, identity, format/ABI/bootstrap, dependencies, materialization, recipe, tests, provenance, sync and lineage. Whole-JDK applicability may require **proposed** additions for module/platform/build-mode coverage, concurrency guarantees, bootstrap phase, binary/serialization/native surface IDs and evidence applicability. Evaluate those against the existing schema, which restricts additional properties; describe gaps first, then evolve it in a separately reviewed implementation. Do not silently add fields, redefine status values or create a parallel “complete” manifest in this docs change.

Future-port procedure: pin source and target baselines; compare against last reviewed pins; enumerate changes including deletions/renames; resolve to stable mapping IDs and reverse dependents; classify applicability and route impact; reconcile target-specific changes; prepare deterministic before/after recipes; test drift refusal, idempotence, partial failure and rollback; attach exact candidate/image evidence; advance synchronized pins only when every relevant change has a disposition. Unmapped changed surfaces block a completeness claim.

Repository/PR ancestry, source presence, mapped target presence, executed tests and accepted integration are separate facts. Re-fetch selected tips before implementation, reconcile concurrent owner proposals, and never borrow another branch's receipts. Preserve license notices and private-source publication boundaries.

## 8. Concurrent public runtime candidates

M3 [draft #23](https://github.com/hsoliwal/M3jdk21/pull/23) contains a separately tested runtime restoration; [draft #24](https://github.com/hsoliwal/M3jdk21/pull/24) evolves text-owner consolidation. The [reconciliation document inspected at commit prefix 7a0d549e](https://github.com/hsoliwal/M3jdk21/blob/7a0d549e/m3/docs/concurrent-owner-reconciliation.md) binds seven-owner consolidation to code `ef33a8cceff4848551f32ec302f8940ec6609591` and runtime receipts to candidate `69667e3f1ce673aaded2ad93e4ae2ecf6c59668b`. It records a combined-source warning-as-error failure and a subsequent narrow fix whose matched rebuild/acceptance is pending in that snapshot. Do not infer that no runtime candidate exists from the earlier master snapshot, or that the combined candidate is accepted from separate passes.

Re-fetch selected candidates before work, inspect actual retained bytes and keep each receipt bound to its exact source/image. This snapshot does not promote any mapping or prescribe blind cherry-picks.

## 9. Acceptance and this document's verification limit

A replacement is accepted only for its named APIs, routes, build modes and platforms after required compatibility, dependency, provenance, replay, build/conformance and performance gates are met. Failed/skipped/unrun gates remain explicit; do not change an oracle or remove a test to manufacture acceptance.

For this documentation change: inspected pinned repository directories, mapping/schema/tooling and selected collection source; checked official Java 21 contracts. No new code, operational manifest, pinned README, workflow or recipe pin is changed. No build, benchmark, concurrency test or JDK conformance suite was executed. The 70-directory census is complete for that root listing; the whole-JDK symbol/ABI/dependency inventory remains required future work.
