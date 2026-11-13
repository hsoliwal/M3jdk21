# MIndex-to-M3 migration coverage

**INCOMPLETE**

This is the current observed mapping coverage, not an exhaustive source inventory.

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
