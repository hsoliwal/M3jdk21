# Whole-JDK M3 migration coverage

**INCOMPLETE**

Whole-JDK directory scope is established, but the complete symbol/ABI/dependency inventory and implementation migration are not complete.

## Whole-JDK coverage denominator

The inspected target baseline is hsoliwal/M3jdk21@45f546ff5bcb06a1b2604f14baf998785d98d9a1. Its src root contains 70 java.* / jdk.* source directories plus hotspot, demo and utils. The denominator also includes make/configure, native/platform sources, resources, generated sources and generators, tests, launchers, image/package tooling and bundled licensing surfaces. The complete directory list and taxonomy are in ../../docs/whole-jdk-migration-scope.md.

This is a framework plus verified directory census, not a completed symbol/ABI/dependency inventory. Missing semantic inspection is recorded as open work rather than inferred absence.

| Subsystem | Current planning disposition | Primary dependency / evidence need |
| --- | --- | --- |
| String, Unicode, charset, regex | replace/adapt candidates; incomplete | Deep text specs, exact UTF-16 differential tests, materialization, regex/encoding seams, VM/JNI/CDS/JIT gates |
| Collections, maps, sets, queues/deques | mixed replace/adapt/reuse; incomplete | [whole-JDK collection contract](../../docs/whole-jdk-collections-replacement.md): per-family views/serialization/subclass/boxing evidence |
| Concurrent collections and atomics | retain-pending-evidence / research candidates | JMM, linearization, progress, GC/reclamation, contention/resize tests |
| Streams, iterators, spliterators, bulk algorithms | adapt/specialize candidates | Encounter order, callback/evaluation semantics, parallel behavior, precompute amortization |
| I/O, NIO, buffers, files, networking, serialization | mixed adapt/reuse/platform-specific | Contiguous-array/native ownership, close/error behavior, mapped-file/platform and serialized-form tests |
| Numeric, math, time and value APIs | mostly reuse pending measured specialization | Exact arithmetic/rounding/value contracts, caches/identity, serialization and benchmark evidence |
| Reflection, method handles, class loading, modules/services | retain/adapt candidates | Loader identity, descriptors, access/init order, bootstrap dependency closure |
| javac/compiler transformation | adapt + Route B tooling | Attributed semantic oracle, refusal corpus, deterministic recipes and replay |
| HotSpot allocation/object layout/GC/references | blocked until exact candidate needs VM change | Oop layout, roots/barriers, all selected GCs, OOME/reference processing |
| Interpreter/C1/C2/intrinsics/JVMCI | blocked behind library/layout candidates | -Xint/tiered/intrinsic/deopt and CPU-specific evidence |
| JNI/native/platform/serviceability | platform-specific, cross-cutting | JNI contracts, native lifetime, JVMTI/JFR/SA/attach and OS/CPU matrix |
| Security/crypto/providers | retain-pending-evidence by default | Provider/vector/side-channel/key-lifetime/security review |
| Higher java.* / jdk.* modules | per-module review required | Package/symbol/resource/native inventory and dependency-ordered dispositions |
| Build/image/package/distribution | retain/adapt | OpenJDK build compatibility, exact image, feature rollback and release provenance |

Per-subsystem implementation packets and dependency order are in [../../docs/whole-jdk-work-packets.md](../../docs/whole-jdk-work-packets.md). No row above is an operational capability status change.

## Required inventory row shape

For every module/package/public-or-relevant-internal symbol, native ABI, format, resource/generator and test obligation record:

- stable capability/mapping ID or explicit pending-ID decision;
- source and target commit plus fully qualified symbol/path;
- semantic/storage owner and lifetime/identity domain;
- dependencies and reverse consumers;
- Route A/B/C eligibility and bootstrap phase;
- current representation and proposed/retained disposition;
- API/binary/serialization/native/format contracts;
- recipe/transformation applicability;
- required differential/concurrency/runtime/performance evidence;
- provenance/license obligations;
- status and unresolved conflicts.

One-to-many, many-to-one, rename, split, consolidation, deletion and target-only adaptations are first-class. Source inspected, source ported, target retained and behavior verified are separate facts.

## Branch-scoped Synexia source-census evidence

A newer private Synexia census exists on draft [hsoliwal/com.synexia#7696](https://github.com/hsoliwal/com.synexia/pull/7696), head `1702b78729dc84c06aaebd9fff3ebb596479bfd6`, based on `develop@3db24805d640c72ab1bd637d83561696d99561a0`. Its recorded input source is `6df9df8d8f42111239013ee941723ec37f97ba6e`; the inventory artifact code/records are separately frozen at `edc04489dc46d4ff810517e74bf9162f83a2b15c`.

That branch reports **29,056 source obligations**, **16,343 supplied Java files**, **16,250 parser records**, **31,706 declared symbols** and **153,692 public/protected contract references**, with **93 real source syntax failures** kept visible. It is materially broader than the older filename-only MIndex slice and should be reconciled before anyone creates or reruns another source scanner.

This evidence does **not** close the operational `source-inventory` gate here:

- it is branch-scoped private evidence, not the target's canonical `name-mapping.json` state;
- its private schema-2 mapping rows still require reviewed reconciliation with the target schema/validator;
- source syntax admission is failed for the 93 recorded diagnostics;
- dependency/semantic ownership, target applicability and exact JDK reverse consumers remain open;
- raw private inventory/source artifacts must stay private.

WP0 should therefore consume/reconcile that census first, refresh it only where the selected source pin or required scope differs, and advance canonical coverage only through a separate reviewed mapping/validator update.

## MIndex/MatIndex slice coverage

This is the current observed MIndex/MatIndex mapping coverage, not an exhaustive source inventory.

Source baseline: `75fb1abaecb56969bea2914520bbe819f130632b`.
Target baseline: `d6390ea3bb348f0c22afdba0819ca4ec0e97970f`.

| Stable ID | Mapping | State | Canonical responsibility | Reason / limit |
|---|---|---|---|---|
| bridge.MIndexCanonicalBridge | pending | pending | Existing bridge responsibility; precise semantic ownership inspection pending | Discovered in pinned tree; README inspected, this body and signatures not yet inspected. |
| bridge.MIndexDagDeferredSwap | pending | pending | Existing bridge responsibility; precise semantic ownership inspection pending | Discovered in pinned tree; README inspected, this body and signatures not yet inspected. |
| bridge.MIndexDagPage | pending | pending | Existing bridge responsibility; precise semantic ownership inspection pending | Discovered in pinned tree; README inspected, this body and signatures not yet inspected. |
| bridge.MIndexDagPageCodec | pending | pending | Existing bridge responsibility; precise semantic ownership inspection pending | Discovered in pinned tree; README inspected, this body and signatures not yet inspected. |
| bridge.MIndexKnowledgeArgumentCompiler | pending | pending | Existing bridge responsibility; precise semantic ownership inspection pending | Discovered in pinned tree; README inspected, this body and signatures not yet inspected. |
| bridge.MIndexKnowledgeArguments | pending | pending | Existing bridge responsibility; precise semantic ownership inspection pending | Discovered in pinned tree; README inspected, this body and signatures not yet inspected. |
| bridge.MIndexLexiconCoordinateCompilerCli | pending | pending | Existing bridge responsibility; precise semantic ownership inspection pending | Discovered in pinned tree; README inspected, this body and signatures not yet inspected. |
| bridge.MIndexLexiconCoordinateManifest | pending | pending | Existing bridge responsibility; precise semantic ownership inspection pending | Discovered in pinned tree; README inspected, this body and signatures not yet inspected. |
| bridge.MIndexLexiconCoordinatePublisher | pending | pending | Existing bridge responsibility; precise semantic ownership inspection pending | Discovered in pinned tree; README inspected, this body and signatures not yet inspected. |
| bridge.MIndexLexiconCoordinateSpace | pending | pending | Existing bridge responsibility; precise semantic ownership inspection pending | Discovered in pinned tree; README inspected, this body and signatures not yet inspected. |
| bridge.MIndexRuntimeStructuralAdapterProvider | pending | pending | Existing bridge responsibility; precise semantic ownership inspection pending | Discovered in pinned tree; README inspected, this body and signatures not yet inspected. |
| bridge.MIndexStringConversionBridge | pending | pending | Existing bridge responsibility; precise semantic ownership inspection pending | Discovered in pinned tree; README inspected, this body and signatures not yet inspected. |
| family.application-structures | pending | pending | Existing AST/DAG/Object/interaction/Path/Algorithm/DataStructure/Tool/Library/Framework owners | Bodies and dependency closure not exhausted. Keep application facilities outside java.base. |
| family.entire-source-closure | pending | pending | All actual MIndex, MatIndex, SubMIndex and non-prefix dependencies | Full tree/symbol/resource/test inventory remains incomplete; do not infer absence from this tranche. |
| family.structural-owners | pending | pending | Existing MIndexAtomStore, MIndexAst, MIndexDag, MIndexInteraction | Established ownership from task and bridge documentation; source bodies, exact signatures and ports remain to inspect. |
| family.substring-conflict | pending | pending | Separate existing indexstring and runtime SubMIndexString contracts | Task identifies arbitrary UTF-16 ranges versus surrogate-splitting refusal/materialization. No renaming consolidation performed. |
| family.word-facts | pending | pending | Existing dictionary/atom/composition/pattern fact owners | Prefix facts ported only; primitive conversion, search, filtering, relations, regex accelerators and word facets need behavior-based inventory. |
| m3.p0-view-dependencies | dependency-reuse | pending | Existing com.m3.text P0 classes | Reused unchanged in scoped tests; not promoted to canonical Synexia storage owners. |
| m3.prefix-z | specialization | implemented-tested | com.m3.algorithm.M3PrefixZ facts; com.m3.text.M3StringPiece remains text owner | Tested algorithm specialization over existing P0 views, not a complete canonical text-family port. |
| route-a.canonical-storage | pending | pending | Canonical immutable atom/range and shared/local owner | Current P0 allocator is not the required interner. Publication, mapped generation lifetime, cache pressure, cross-process and closure gates remain open. |
| route-b.compiler-lowering | pending | pending | Proven compiler transform and same canonical owner | No compiler lowering implemented here. Order, exceptions, nulls, overloads, identity, invokedynamic and mixed callers require acceptance. |
| route-c.complete-jdk | pending | pending | Matched complete JDK String/HotSpot implementation | PR6 experiment history is not master runtime acceptance. No installed JDK replaced and no runtime enablement performed. |
| synexia.frozen-chars | pending | pending | com.synexia.indexstring.FrozenChars | Body inspected: exact UTF-16BE mapped/heap units, mutable admission copies, arbitrary ranges; FrozenBytes dependency and port pending. |
| synexia.inventory-cli | pending | pending | com.synexia.m3.inventory.M3InventoryMain | Existing scanner entry point inspected. Do not create a competing inventory scanner. |
| synexia.inventory-producer | pending | pending | com.synexia.m3.inventory.InventoryWriter | Existing TSV producer reused as format authority; full source scan not executed, and lexical findings do not prove semantic ownership. |

## Mandatory gates

| Gate | State | Evidence or blocker |
|---|---|---|
| source-inventory | blocked | Connector inspection is partial; local clone DNS failed, and the full existing InventoryWriter scan has not run. |
| ownership-naming | open | Legacy naming history preserved; canonical owners and substring contract conflict remain unresolved. |
| scoped-prefix-and-tool-tests | passed | Local Java21 normal/interpreter prefix and interop, 45 recipe checks and 26 validator tests passed; see local-20261002-final receipts. |
| source-owner-differential | open | Independent stock String oracle tested. Full original MIndex owner module tests not executed. |
| maven-recipe-lifecycle | blocked | Maven unavailable locally; dependency-free Java recipe compiled/run directly. POM is unexecuted. |
| route-a-canonical-storage | open | P0 views reused; canonical interning/sharing/lifetime not accepted. |
| route-b-lowering | open | Not implemented/tested in this tranche. |
| route-c-complete-image | open | No exact-candidate complete JDK image built. |
| runtime-flag-off | open | Historical 86 tests belong to PR6 head3776, not this candidate. |
| runtime-enabled | failed | Historical PR6 enabled16 pass and2 StringJoiner OOME failures remain unresolved; no fresh runtime result. |
| jit-intrinsics | open | PR6 enabled execution is interpreter-only; stock-JVM algorithm tests do not close this gate. |
| gc-intern-jni-jvmti | open | Owner lifetime, VM StringTable, JNI modified UTF-8/release paths and JVMTI require exact-head runtime tests. |
| cds-serialization-serviceability | open | No candidate acceptance. |
| string-unicode-regex | open | Focused stock view regex/codepoint tests passed; exhaustive Java21 String behavior coverage remains open. |
| shared-generation-cross-process | open | Atomic immutable generations, malformed/truncated mappings, close/eviction and platform replacement not accepted. |
| windows-native | open | No Windows execution; no WSL used. |
| performance-memory | open | No benchmarks or retained heap/native/mapped memory measurements; no speed claim. |
| independent-audit | open | Self-review and negative tests performed; no independent reviewer or complete omission audit. |
| future-port-full-closure | open | Three-way TSV review implemented; full pinned scan, symbol resolution and complete CI wiring remain unexecuted. |
| target-baseline-drift | blocked | PR #16 creation observed master aceb243be751f327c8d59f6d2ebcbeb126db531b; pinned candidate is based on d639. Reconcile current master before merge; no blind overwrite. |

## Completion blockers

- full source tree inventory is incomplete
- semantic dependency/ownership closure is incomplete
- pinned exhaustive inventory receipt is absent
- uninspected domains remain
- m3.p0-view-dependencies: pending
- m3.p0-view-dependencies: synchronization gaps remain
- synexia.frozen-chars: pending
- synexia.frozen-chars: synchronization gaps remain
- synexia.inventory-producer: pending
- synexia.inventory-producer: synchronization gaps remain
- synexia.inventory-cli: pending
- synexia.inventory-cli: synchronization gaps remain
- bridge.MIndexCanonicalBridge: pending
- bridge.MIndexCanonicalBridge: synchronization gaps remain
- bridge.MIndexDagDeferredSwap: pending
- bridge.MIndexDagDeferredSwap: synchronization gaps remain
- bridge.MIndexDagPage: pending
- bridge.MIndexDagPage: synchronization gaps remain
- bridge.MIndexDagPageCodec: pending
- bridge.MIndexDagPageCodec: synchronization gaps remain
- bridge.MIndexKnowledgeArgumentCompiler: pending
- bridge.MIndexKnowledgeArgumentCompiler: synchronization gaps remain
- bridge.MIndexKnowledgeArguments: pending
- bridge.MIndexKnowledgeArguments: synchronization gaps remain
- bridge.MIndexLexiconCoordinateCompilerCli: pending
- bridge.MIndexLexiconCoordinateCompilerCli: synchronization gaps remain
- bridge.MIndexLexiconCoordinateManifest: pending
- bridge.MIndexLexiconCoordinateManifest: synchronization gaps remain
- bridge.MIndexLexiconCoordinatePublisher: pending
- bridge.MIndexLexiconCoordinatePublisher: synchronization gaps remain
- bridge.MIndexLexiconCoordinateSpace: pending
- bridge.MIndexLexiconCoordinateSpace: synchronization gaps remain
- bridge.MIndexRuntimeStructuralAdapterProvider: pending
- bridge.MIndexRuntimeStructuralAdapterProvider: synchronization gaps remain
- bridge.MIndexStringConversionBridge: pending
- bridge.MIndexStringConversionBridge: synchronization gaps remain
- family.entire-source-closure: pending
- family.entire-source-closure: synchronization gaps remain
- family.structural-owners: pending
- family.structural-owners: synchronization gaps remain
- family.substring-conflict: pending
- family.substring-conflict: synchronization gaps remain
- family.word-facts: pending
- family.word-facts: synchronization gaps remain
- family.application-structures: pending
- family.application-structures: synchronization gaps remain
- route-a.canonical-storage: pending
- route-a.canonical-storage: synchronization gaps remain
- route-b.compiler-lowering: pending
- route-b.compiler-lowering: synchronization gaps remain
- route-c.complete-jdk: pending
- route-c.complete-jdk: synchronization gaps remain
- source-inventory: blocked
- ownership-naming: open
- source-owner-differential: open
- maven-recipe-lifecycle: blocked
- route-a-canonical-storage: open
- route-b-lowering: open
- route-c-complete-image: open
- runtime-flag-off: open
- runtime-enabled: failed
- jit-intrinsics: open
- gc-intern-jni-jvmti: open
- cds-serialization-serviceability: open
- string-unicode-regex: open
- shared-generation-cross-process: open
- windows-native: open
- performance-memory: open
- independent-audit: open
- future-port-full-closure: open
- target-baseline-drift: blocked
- source default-branch tip changed since baseline; reconcile rather than silently repin

Source-only changes require recipe review. Target-only adaptations are retained. Both-side changes are conflicts.
A missing row in an incomplete or different-branch inventory is unobserved, not a deletion.
No automatic reverse-port, merge, or runtime acceptance is authorized by this report.
