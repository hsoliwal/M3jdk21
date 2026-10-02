# Whole-JDK M3 dependency work packets

Status: documentation-only planning artifact. Target pin: hsoliwal/M3jdk21@45f546ff5bcb06a1b2604f14baf998785d98d9a1. Synexia pin: hsoliwal/com.synexia@3db24805d640c72ab1bd637d83561696d99561a0.

This file turns the whole-JDK scope into bounded implementation packets for the migration execution layer. The canonical ten-pass programme, packet schema and evidence model live in [../../docs/whole-jdk-work-packets.md](../../docs/whole-jdk-work-packets.md); this file is an execution decomposition that must remain consistent with that owner. It is not a status registry. Operational capability IDs and synchronization state remain in ../../docs/name-mapping.json and its validated tooling.

## Packet state vocabulary

Use these planning dispositions consistently:

- replace-backend: preserve the public/VM contract while changing internal representation;
- adapt: keep an existing implementation or owner and add a compatibility bridge;
- reuse: existing JDK/Synexia representation is already appropriate;
- retain-pending-evidence: no replacement is justified yet;
- platform-specific: semantics depend on OS/CPU/native implementation;
- blocked: dependency or semantic proof is missing;
- deferred: intentionally later, with reason and re-entry trigger.

Implemented, retained, tested, proposed, blocked and deferred are independent facts. A mapped symbol is not automatically retained or tested.

## WP0 — inventory, pins and mapping closure

Scope:
- all 70 java.* / jdk.* directories at the target pin plus hotspot, demo, utils;
- make/configure, generated sources and generators, native libraries, resources, tests and distribution/image tooling;
- existing Synexia MIndex*, MatIndex* and non-prefix dependencies selected as candidate owners.

Outputs:
- module/package/symbol/native/resource/test census;
- dependency and reverse-consumer graph;
- bootstrap phase for every candidate dependency;
- source/target pin and provenance receipt;
- mapping dispositions using the existing authority.

Gate:
No subsystem may claim complete coverage until its denominator, omissions and exclusions are explicit.

## WP1 — M3 primitive identity, ownership and lifetime foundation

Candidate owners:
- existing Synexia primitive/Mat/MIndex foundations after dependency and license review;
- target M3 storage owners where already retained.

Specify:
- namespace + generation + record/atom/handle identity;
- checked lengths/offsets/overflow;
- immutable versus mutable owner rules;
- heap, mapped, native and persistent lifetime;
- generation publication and rollback;
- cache admission/eviction budgets;
- no address-as-identity;
- GC visibility and native-handle validation.

Depends on: WP0.
Blocks: every storage-changing packet.

## WP2 — String, Unicode, charset and regex

Primary existing specs:
- ../../docs/shared-atom-concatenation.md
- ../../docs/mindex-migration-handoff.md
- ../../../doc/mindex-string-backing.md

Required scope:
- exact UTF-16 code-unit behavior;
- shared lexicon + VM-local interner;
- reference-only joins/slices with bounded metadata;
- materialization boundaries;
- String hash/equality/comparison and distinct identity domains;
- StringLatin1/StringUTF16/concat factories;
- Charset encode/decode seam state;
- java.util.regex compatibility plus accelerator filters;
- StringTable/intern, dedup, CDS, JNI/JVMTI and serviceability for Route C.

Routes:
A explicit M3 view; B attributed lowering; C matched custom JDK.

Gate:
Differential Java String/Unicode/regex tests, malformed input, exact-image runtime, GC/JIT/JNI/CDS/serviceability and performance/memory gates. Existing historical evidence remains scoped and cannot be borrowed to a changed candidate.

## WP3 — collections and maps

Canonical detailed spec: [../../docs/whole-jdk-collections-replacement.md](../../docs/whole-jdk-collections-replacement.md). Execution overlay: [COLLECTIONS.md](COLLECTIONS.md).

Scope:
List, Set, Map, Queue, Deque, sorted/navigable/sequenced families, wrappers, views, iterators, spliterators, utilities, immutable factories and legacy collections.

Candidate owners:
synexia-mat-collections primitives and planners where semantics fit; otherwise new minimal bootstrap-safe internals or retained JDK implementations.

Gate:
Per-implementation differential/serialization/subclass/view/order/null/equality tests, boxing-boundary evidence, memory/CPU measurements and exact mapping updates.

## WP4 — concurrency and atomics

Scope:
java.util.concurrent collections; atomics; VarHandle-backed state; locks, synchronizers, executors/futures, fork-join, cancellation/interruption and thread/scheduler consumers.

Rules:
- preserve JMM happens-before and safe publication;
- identify linearization points where promised;
- preserve documented progress/fairness;
- prevent ABA/stale generation;
- integrate with GC/barriers for any handle/off-heap representation;
- do not infer snapshot semantics for weakly consistent operations.

Depends on:
WP1, relevant WP3 storage, HotSpot memory/barrier knowledge from WP10.

Gate:
JCStress-style or equivalent concurrency stress, contention, resize, interruption, timeout, cancellation, GC and exact-runtime tests.

## WP5 — streams, iterators, spliterators and bulk algorithms

Scope:
java.util.stream, java.util.function consumers, Arrays/Collections helpers, sorting/searching/prefix operations and parallel bulk paths.

Candidate M3 value:
reuse precomputed facts and specialized primitive loops without changing encounter order, callbacks, exceptions or side effects.

Rules:
- algorithm selection is data/contract dependent;
- precomputation validity and amortization are explicit;
- cancellation/progress monitors belong to Synexia tooling/APIs where appropriate, not silently added to JDK public signatures;
- parallelism must not change non-thread-safe collection semantics.

Depends on:
WP3 and WP4.

Gate:
Differential sequential/parallel results, side-effect/evaluation-order corpus, adversarial data, warm/cold benchmarks and allocation measurements.

## WP6 — I/O, NIO, buffers, files, networking and serialization

Scope:
java.io, java.nio, channels, filesystems, buffers/mapped memory, charset boundaries, sockets/http integration and Object serialization.

Candidate M3 value:
segmented/pooled buffers, shared immutable bytes, direct/mapped representations and indexed path/text storage where contracts allow.

Rules:
- ordinary Java arrays remain contiguous objects;
- direct buffers require contiguous address ranges and explicit ownership;
- scatter/gather is a separate ABI;
- public mutable-array results are independent writable storage;
- serialized form and stream protocol remain exact;
- file/mapping lifetime and truncation/replacement behavior are platform-tested.

Depends on:
WP1, WP2, selected WP3 structures.

Gate:
malformed/truncated inputs, close/cancel/error paths, mapped-file replacement, direct/native lifetime, serialization compatibility and platform-specific tests.

## WP7 — numeric, math, time and value-oriented APIs

Scope:
primitive wrappers, BigInteger/BigDecimal, math helpers, random APIs, java.time and other immutable value classes.

Disposition rule:
prefer reuse when current representation is already compact and semantics are rich; specialize only where an M3 primitive/index representation removes measurable duplication without changing identity/caching/serialization.

Risks:
wrapper caches and reference identity, NaN/signed zero, exact arithmetic/rounding, locale/time-zone data versioning.

Gate:
differential numerical vectors, serialization, edge/overflow/rounding/time-zone cases and memory/CPU evidence.

## WP8 — reflection, method handles, class loading, modules and service loading

Scope:
Class metadata consumers, reflection objects, MethodHandle/VarHandle, invokedynamic, class loaders, module graph, ServiceLoader and dynamic proxies.

Candidate M3 value:
indexed metadata and compact immutable lookup tables only where VM/JDK ownership permits.

Rules:
- descriptor/binary names and loader identity are semantic;
- do not canonicalize across class-loader identity domains;
- preserve access checks and initialization timing;
- bootstrap cannot depend recursively on higher tooling.

Depends on:
WP1, WP2 and WP10.

Gate:
class-loader isolation, modules, hidden/dynamic classes, reflection, MH/VH descriptor and service-provider tests.

## WP9 — compiler, javac and transformation recipes

Scope:
javac/JDK compiler internals, compiler-lowering Route B, bytecode metadata and reusable Maven/OpenRewrite recipes for source-level transformations.

Rules:
- original attributed program is semantic oracle;
- transform only proven operations;
- preserve evaluation order, overload resolution, exceptions, identity, synchronization and ABI;
- fail closed on unresolved/unsafe cases;
- transformations are source-pinned, hash-bound, idempotent and rollback-aware;
- Maven/application tooling never enters java.base bootstrap.

Depends on:
semantic owners from WP2–WP8.

Gate:
before/after corpus, compile both sides, differential execution, refusal tests, recipe drift/idempotence/partial-state/rollback and provenance receipts.

## WP10 — HotSpot allocation, object layout, GC, references and runtime metadata

Scope:
oop layouts, barriers, allocation, references, String/collection VM special cases, monitors, safepoints/handshakes, deoptimization and runtime metadata.

Rules:
- GC must discover all live object references;
- native/off-heap handles need roots/lifetime and generation validation;
- layout changes require matched VM readers/writers;
- do not smuggle arbitrary pointers into fields interpreted as oops/arrays.

Depends on:
WP1 and exact library candidates requiring VM support.

Gate:
all selected GCs/build modes, stress, verification options, deoptimization, reference processing, OOME paths and serviceability.

## WP11 — interpreter, C1/C2, intrinsics and JVMCI

Scope:
bytecode execution, compiled code, intrinsics, CPU stubs, JVMCI/Graal integration present in the tree.

Rules:
- library semantic fallback remains authoritative;
- intrinsic and compiled paths must agree with interpreter;
- architecture-specific stubs are separate evidence domains;
- deoptimization reconstructs valid observable state.

Depends on:
WP2/WP3 candidates and WP10 layouts.

Gate:
-Xint, tiered/C1/C2 modes, intrinsic on/off, deopt/stress, supported CPU ports and exact-image differential tests.

## WP12 — JNI, foreign/native interfaces, platform integration and serviceability

Scope:
JNI String/array/object entry points, native libraries, JVM TI, JFR, SA/debugging, management, attach, platform ports and any FFM surfaces present in the selected JDK baseline.

Rules:
- JNI modified UTF-8 is not normal UTF-8;
- acquired array pointers obey release rules;
- native descriptors validate owner/generation;
- serviceability tools understand any changed layout before enablement;
- OS/CPU behavior and file mapping semantics are tested separately.

Depends on:
WP1, WP10 and affected library packets.

Gate:
native boundary tests, attach/JFR/JVMTI/SA where applicable, platform matrix and crash/error cleanup.

## WP13 — security, cryptography and providers

Scope:
java.security, crypto providers, TLS/authentication, certificates/keystores and native crypto integration.

Default disposition:
retain-pending-evidence. Security-sensitive code is not a target for representational churn without a precise benefit and side-channel/security review.

Candidate use:
immutable indexed metadata or buffer reuse only where it does not change provider contracts, constant-time requirements or key-material lifetime.

Gate:
provider compatibility, vectors, permissions/access, serialization/formats, native provider tests and security review.

## WP14 — higher JDK modules, desktop, XML, SQL, management and tooling

Scope:
remaining java.* / jdk.* modules from the WP0 census including desktop/graphics/audio/fonts/accessibility, XML, SQL/JDBC, naming/RMI, management, instrumentation, logging, prefs, javadoc/jar/jdeps/jlink/jpackage/jshell and related tools.

Method:
process in dependency order. Each module receives:
- symbol/API/native/resource inventory;
- data-owner and candidate reuse review;
- disposition for every surface;
- routes A/B/C eligibility;
- compatibility and performance gates.

Default:
do not force module-specific complex state into one M3 backend. Many surfaces may remain JDK-native and only consume lower M3 primitives.

## WP15 — distribution, build, packaging and rollout

Scope:
configure/make, generated code, images, launchers, modules, packaging, cross compilation and release artifacts.

Rules:
- preserve OpenJDK build conventions;
- no Maven runtime dependency in the JDK build;
- feature flags/fallbacks are explicit;
- stock behavior is available for rollback until acceptance;
- exact source/image identity is recorded;
- unsupported platform/build modes remain named and disabled rather than silently accepted.

Gate:
clean builds, image creation, packaging, smoke/conformance suites, rollback/flag-off and release provenance.

## Cross-packet acceptance evidence

Each implementation candidate records:

1. source and target commits;
2. stable mapping/capability ID;
3. fully qualified symbols and formats/ABIs touched;
4. owner/lifetime/identity contract;
5. dependencies and reverse consumers;
6. route(s) claimed;
7. transformation/recipe version and hashes;
8. differential/concurrency/runtime tests;
9. exact build/image/platform;
10. performance/memory methodology and results;
11. provenance/license obligations;
12. unresolved risks and explicit blockers.

Evidence is attached to the exact candidate only. A historical pass, matching filename or merged PR does not transfer automatically to a changed tree.

## Ten-pass program alignment

The user-requested dependency-aware passes map to these packets:

1. pin and inventory -> WP0;
2. dependency/bootstrap map -> WP0/WP1;
3. canonical mapping dispositions -> WP0;
4. minimal storage/ownership/identity -> WP1;
5. deep String and collections -> WP2/WP3;
6. remaining subsystems -> WP4–WP14;
7. route-specific packets -> each packet's route section plus WP9–WP12;
8. recipes/tests/runtime/rollback -> WP9–WP12/WP15;
9. allocation/memory/throughput/latency/contention -> every performance-bearing packet;
10. omission/contradiction/stale-mapping audit -> WP0 + final WP15 promotion review.

The program remains incomplete until every selected denominator entry has a disposition and every claimed replacement has closed its applicable gates.
