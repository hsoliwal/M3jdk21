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
| synexia.collections.PackedBitAtoms | adapter | implemented-unverified | jdk.internal.mindex.M3Bits | Complete bounded collection kernel in java.base; full-image and automatic JDK consumer admission remain open. |
| synexia.collections.SegmentedAddress28 | adapter | implemented-unverified | jdk.internal.mindex.M3Address28 | Complete bounded collection kernel in java.base; full-image and automatic JDK consumer admission remain open. |
| synexia.collections.SegmentedBitLane28 | adapter | implemented-unverified | jdk.internal.mindex.M3BitLane28 | Complete bounded collection kernel in java.base; full-image and automatic JDK consumer admission remain open. |
| synexia.collections.SegmentedIntLane28 | adapter | implemented-unverified | jdk.internal.mindex.M3IntLane28 | Complete bounded collection kernel in java.base; full-image and automatic JDK consumer admission remain open. |
| synexia.collections.SegmentedLongLane28 | adapter | implemented-unverified | jdk.internal.mindex.M3LongLane28 | Complete bounded collection kernel in java.base; full-image and automatic JDK consumer admission remain open. |
| synexia.counterpart.MIndexAST | pending | pending | jdk.internal.mindex.M3AST | Concise counterpart name reserved; dependency closure and runtime integration remain unimplemented. |
| synexia.counterpart.MIndexASTPrecompute | pending | pending | jdk.internal.mindex.M3ASTPC | Concise counterpart name reserved; dependency closure and runtime integration remain unimplemented. |
| synexia.counterpart.MIndexClass | adapter | implemented-unverified | jdk.internal.mindex.M3Class | Inlined reflection counterpart and caller-owned runtime bridge; bounded tests pass; global JVM admission remains open. |
| synexia.counterpart.MIndexClassBoundary | adapter | implemented-unverified | jdk.internal.mindex.M3Boundary | Inlined reflection counterpart and caller-owned runtime bridge; bounded tests pass; global JVM admission remains open. |
| synexia.counterpart.MIndexClassFlags | adapter | implemented-unverified | jdk.internal.mindex.M3ClassFlags | Inlined reflection counterpart and caller-owned runtime bridge; bounded tests pass; global JVM admission remains open. |
| synexia.counterpart.MIndexClassIndex | adapter | implemented-unverified | jdk.internal.mindex.M3CI | Inlined reflection counterpart and caller-owned runtime bridge; bounded tests pass; global JVM admission remains open. |
| synexia.counterpart.MIndexCompilerASTPrecompute | pending | pending | com.sun.tools.javac.m3.M3ASTPC | Concise counterpart name reserved; dependency closure and runtime integration remain unimplemented. |
| synexia.counterpart.MIndexConstructor | adapter | implemented-unverified | jdk.internal.mindex.M3Constructor | Inlined reflection counterpart and caller-owned runtime bridge; bounded tests pass; global JVM admission remains open. |
| synexia.counterpart.MIndexField | adapter | implemented-unverified | jdk.internal.mindex.M3Field | Inlined reflection counterpart and caller-owned runtime bridge; bounded tests pass; global JVM admission remains open. |
| synexia.counterpart.MIndexJvmDescriptor | adapter | implemented-unverified | jdk.internal.mindex.M3Descriptor | Observed retained target-only hidden-class descriptor correction; exact target and recipe identities reconciled. Prior scoped receipts remain historical; current full module/image and source-target contract acceptance is not established by this ledger update. |
| synexia.counterpart.MIndexLibraryRelease | adapter | implemented-unverified | jdk.internal.mindex.M3Release | Inlined dormant JDK-owned kernel; local recipe/module/runtime proof passed; complete JDK integration unverified. |
| synexia.counterpart.MIndexMember | adapter | implemented-unverified | jdk.internal.mindex.M3Member | Inlined reflection counterpart and caller-owned runtime bridge; bounded tests pass; global JVM admission remains open. |
| synexia.counterpart.MIndexMemberFlags | adapter | implemented-unverified | jdk.internal.mindex.M3MemberFlags | Inlined reflection counterpart and caller-owned runtime bridge; bounded tests pass; global JVM admission remains open. |
| synexia.counterpart.MIndexMemberKind | adapter | implemented-unverified | jdk.internal.mindex.M3MemberKind | Inlined reflection counterpart and caller-owned runtime bridge; bounded tests pass; global JVM admission remains open. |
| synexia.counterpart.MIndexMethod | adapter | implemented-unverified | jdk.internal.mindex.M3Method | Inlined reflection counterpart and caller-owned runtime bridge; bounded tests pass; global JVM admission remains open. |
| synexia.counterpart.MIndexRecordComponent | adapter | implemented-unverified | jdk.internal.mindex.M3RecordComponent | Inlined reflection counterpart and caller-owned runtime bridge; bounded tests pass; global JVM admission remains open. |
| synexia.counterpart.MIndexRegexTrigramQuery | adapter | implemented-unverified | jdk.internal.mindex.M3TQ derived facts; existing M3StringBacking retains payload authority | JDK-owned TQ kernel and explicit canonical-backing range bridge. Finite module/JNI qualification is separate from complete-image and automatic regex dispatch admission. |
| synexia.counterpart.MIndexRegexTrigramQuery.sparseLookup | adapter | implemented-unverified | jdk.internal.mindex.M3TQ derived facts; existing M3StringBacking retains payload authority | Forward refinement of the surviving prepared TQ owner: adaptive sparse set containment. Finite host-kernel/SDK proof; complete custom JVM receiving admission remains pending. |
| synexia.counterpart.MIndexVersionTable | adapter | implemented-unverified | jdk.internal.mindex.M3VI | Inlined dormant JDK-owned kernel; local recipe/module/runtime proof passed; complete JDK integration unverified. |
| synexia.counterpart.MIndexVmPrecomputeKey | adapter | implemented-unverified | jdk.internal.mindex.M3PC | Inlined reflection counterpart and caller-owned runtime bridge; bounded tests pass; global JVM admission remains open. |
| synexia.counterpart.MIndexVmSession | adapter | implemented-unverified | jdk.internal.mindex.M3CB | Inlined reflection counterpart and caller-owned runtime bridge; bounded tests pass; global JVM admission remains open. |
| synexia.frozen-chars | pending | pending | com.synexia.indexstring.FrozenChars | Body inspected: exact UTF-16BE mapped/heap units, mutable admission copies, arbitrary ranges; FrozenBytes dependency and port pending. |
| synexia.inventory-cli | pending | pending | com.synexia.m3.inventory.M3InventoryMain | Existing scanner entry point inspected. Do not create a competing inventory scanner. |
| synexia.inventory-producer | pending | pending | com.synexia.m3.inventory.InventoryWriter | Existing TSV producer reused as format authority; full source scan not executed, and lexical findings do not prove semantic ownership. |
| synexia.jcc-java-jni-regression | pending | blocked | Existing M3JDK21 migration-recipes proof owners; Synexia source-canon remains the source admission authority | Current 20-owner source publication is bound after exact readback; historical source and receiving work is retained. Source export, whole owning-module coverage and applicable M3JDK21 runtime/JNI/platform gates remain unadmitted. |
| synexia.jcc-recipe-laboratory | dependency-reuse | blocked | Existing M3JDK21 migration-recipes proof owners; Synexia source-canon remains the source admission authority | Current 20-owner source publication is bound after exact readback; historical source and receiving work is retained. Source export, whole owning-module coverage and applicable M3JDK21 runtime/JNI/platform gates remain unadmitted. |
| synexia.regex.hierarchical-route-acceptance | pending | pending | java.lang.M3StringRegexDp derived metadata; M3StringBacking and M3StringOwner retain canonical payload authority | Committed additive jtreg successor preserves the original hierarchical corpus and adds deterministic route/helper coverage, but the actual receiving jtreg and DP remain their pinned older versions. |
| synexia.regex.metadata-pressure | pending | pending | java.lang.M3StringRegexDp derived metadata; M3StringBacking and M3StringOwner retain canonical payload authority | Existing Synexia pressure-governor policy and its committed current-owner fixture are not received by the pinned strong-table DP owner. |
| synexia.regex.mr | adapter | implemented-unverified | java.util.regex.Matcher derived-fact lifecycle; M3 String retains payload authority | MR adapts completed-work reuse to the existing JDK Matcher lifecycle; exact immutable input and region facts survive same-input reset. Scoped tests pass; complete-image admission remains open. |
| synexia.regex.ordered-relations.adapter | adapter | implemented-unverified | jdk.internal.mindex.M3DotStarRelations ordered algebra; M3StateRelation primitive representation; existing text owners retain payload authority | Observed source-to-target package/type/JNI adaptation exists at the pinned receiving revision. This is a partial ordered-relation numeric adapter, not the complete MIndexStateRelation API or production dispatch qualification. |
| synexia.regex.ordered-relations.dispatch | pending | pending | java.lang.M3StringRegexDp derived metadata; M3StringBacking and M3StringOwner retain canonical payload authority | Committed source recipe output proposes connecting the existing ordered-relation helper to the current DP owner. The pinned receiving DP remains 164c and has no RELATION_ACCESS field or helper call. |
| synexia.regex.rxa | pending | pending | java.util.regex.Pattern/Matcher for compatible atoms; M3 String remains canonical text owner | RXA preserves a restricted RE2/J-oriented boolean provider, including recorded disagreements with JDK dot, anchors and whitespace. A table or provider rename cannot grant JDK Pattern equivalence. |
| synexia.regex.rxm | pending | pending | java.util.regex.Pattern/Matcher for compatible atoms; M3 String remains canonical text owner | RXM freezes exact full-input JDK matches, lookingAt and find capture offsets. Matcher has mutable region, bounds, reset, pattern and append state; direct Result substitution is not admitted. |

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
- synexia.counterpart.MIndexVersionTable: implemented-unverified
- synexia.counterpart.MIndexVersionTable: synchronization gaps remain
- synexia.counterpart.MIndexLibraryRelease: implemented-unverified
- synexia.counterpart.MIndexLibraryRelease: synchronization gaps remain
- synexia.counterpart.MIndexVmSession: implemented-unverified
- synexia.counterpart.MIndexVmSession: synchronization gaps remain
- synexia.counterpart.MIndexVmPrecomputeKey: implemented-unverified
- synexia.counterpart.MIndexVmPrecomputeKey: synchronization gaps remain
- synexia.counterpart.MIndexClass: implemented-unverified
- synexia.counterpart.MIndexClass: synchronization gaps remain
- synexia.counterpart.MIndexClassIndex: implemented-unverified
- synexia.counterpart.MIndexClassIndex: synchronization gaps remain
- synexia.counterpart.MIndexAST: pending
- synexia.counterpart.MIndexAST: synchronization gaps remain
- synexia.counterpart.MIndexAST: unresolved dependency
- synexia.counterpart.MIndexASTPrecompute: pending
- synexia.counterpart.MIndexASTPrecompute: synchronization gaps remain
- synexia.counterpart.MIndexASTPrecompute: unresolved dependency
- synexia.counterpart.MIndexCompilerASTPrecompute: pending
- synexia.counterpart.MIndexCompilerASTPrecompute: synchronization gaps remain
- synexia.counterpart.MIndexCompilerASTPrecompute: unresolved dependency
- synexia.counterpart.MIndexClassBoundary: implemented-unverified
- synexia.counterpart.MIndexClassBoundary: synchronization gaps remain
- synexia.counterpart.MIndexJvmDescriptor: implemented-unverified
- synexia.counterpart.MIndexJvmDescriptor: synchronization gaps remain
- synexia.counterpart.MIndexMember: implemented-unverified
- synexia.counterpart.MIndexMember: synchronization gaps remain
- synexia.counterpart.MIndexMemberKind: implemented-unverified
- synexia.counterpart.MIndexMemberKind: synchronization gaps remain
- synexia.counterpart.MIndexMemberFlags: implemented-unverified
- synexia.counterpart.MIndexMemberFlags: synchronization gaps remain
- synexia.counterpart.MIndexClassFlags: implemented-unverified
- synexia.counterpart.MIndexClassFlags: synchronization gaps remain
- synexia.counterpart.MIndexField: implemented-unverified
- synexia.counterpart.MIndexField: synchronization gaps remain
- synexia.counterpart.MIndexMethod: implemented-unverified
- synexia.counterpart.MIndexMethod: synchronization gaps remain
- synexia.counterpart.MIndexConstructor: implemented-unverified
- synexia.counterpart.MIndexConstructor: synchronization gaps remain
- synexia.counterpart.MIndexRecordComponent: implemented-unverified
- synexia.counterpart.MIndexRecordComponent: synchronization gaps remain
- synexia.jcc-recipe-laboratory: blocked
- synexia.jcc-recipe-laboratory: synchronization gaps remain
- synexia.jcc-recipe-laboratory: unresolved dependency
- synexia.jcc-java-jni-regression: blocked
- synexia.jcc-java-jni-regression: synchronization gaps remain
- synexia.jcc-java-jni-regression: unresolved dependency
- synexia.jcc-java-jni-regression: unresolved dependency
- synexia.jcc-java-jni-regression: unresolved dependency
- synexia.counterpart.MIndexRegexTrigramQuery: implemented-unverified
- synexia.counterpart.MIndexRegexTrigramQuery: synchronization gaps remain
- synexia.counterpart.MIndexRegexTrigramQuery: unresolved dependency
- synexia.collections.SegmentedAddress28: implemented-unverified
- synexia.collections.SegmentedAddress28: synchronization gaps remain
- synexia.collections.SegmentedAddress28: unresolved dependency
- synexia.collections.SegmentedIntLane28: implemented-unverified
- synexia.collections.SegmentedIntLane28: synchronization gaps remain
- synexia.collections.SegmentedIntLane28: unresolved dependency
- synexia.collections.SegmentedLongLane28: implemented-unverified
- synexia.collections.SegmentedLongLane28: synchronization gaps remain
- synexia.collections.SegmentedLongLane28: unresolved dependency
- synexia.collections.SegmentedBitLane28: implemented-unverified
- synexia.collections.SegmentedBitLane28: synchronization gaps remain
- synexia.collections.SegmentedBitLane28: unresolved dependency
- synexia.collections.PackedBitAtoms: implemented-unverified
- synexia.collections.PackedBitAtoms: synchronization gaps remain
- synexia.collections.PackedBitAtoms: unresolved dependency
- synexia.regex.mr: implemented-unverified
- synexia.regex.mr: synchronization gaps remain
- synexia.regex.rxm: pending
- synexia.regex.rxm: synchronization gaps remain
- synexia.regex.rxa: pending
- synexia.regex.rxa: synchronization gaps remain
- synexia.counterpart.MIndexRegexTrigramQuery.sparseLookup: implemented-unverified
- synexia.counterpart.MIndexRegexTrigramQuery.sparseLookup: synchronization gaps remain
- synexia.regex.ordered-relations.adapter: implemented-unverified
- synexia.regex.ordered-relations.adapter: synchronization gaps remain
- synexia.regex.ordered-relations.dispatch: pending
- synexia.regex.ordered-relations.dispatch: synchronization gaps remain
- synexia.regex.metadata-pressure: pending
- synexia.regex.metadata-pressure: synchronization gaps remain
- synexia.regex.metadata-pressure: unresolved dependency
- synexia.regex.hierarchical-route-acceptance: pending
- synexia.regex.hierarchical-route-acceptance: synchronization gaps remain
- synexia.regex.hierarchical-route-acceptance: unresolved dependency
- synexia.regex.hierarchical-route-acceptance: unresolved dependency
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
