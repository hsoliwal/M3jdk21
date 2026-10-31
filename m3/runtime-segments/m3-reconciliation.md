# Latest develop and the three routes

Read-only source review: hsoliwal/com.synexia develop
`3713f70c1f97f1801da43e933296b48af55a7871`, fetched into a separate bare repository.
Exact reviewed file hashes are in evidence/synexia-develop-pin.json. No Synexia
branch was modified and no implementation was imported into OpenJDK by this
review. Its tests were located, not independently rerun here.

| Route | Existing useful implementation | Missing connection / reuse decision |
|---|---|---|
| Explicit wrapper API | FrozenChars/Bytes heap and mapped immutable backing; MIndexJoinedChars/Bytes canonical weak bodies and ranges; MIndexString token-ID concat; native joined descriptors and handles | Keep these owners. Whole String conversion is an explicit materialization boundary. Weak body retention is not a bounded metadata budget. Native lifetime/budget/builders are a separate substantive audit/integration lane. |
| Compiler lowering | MatIndexStringSafeLowerer attributed rewriting and ABI/boundary classification; MatIndexStringLoweringPlan; execution verifier comparing original/lowered UTF16 output, exception classes and public ABI | renderConcatString currently emits concatPart(left)+concatPart(right) then indexes; concatPart uses String.valueOf. This still materializes. Reuse the pipeline and introduce proven-safe token/segment operations with null, primitive, evaluation-order and identity checks. Do not call current lowering zero-copy concat. |
| Custom String/HotSpot | Immutable ownership, exact content equality, ranges, malformed UTF16 and encoding conformance are useful contracts | Those APIs do not supply VM fields/offsets, no-safepoint readers, native StringTable/JNI, bootstrap or GC integration. This branch implements an isolated ordinary-String vertical slice with Java heap leaves; native canonical handles are not connected yet. |

Develop has changed all four previously present Frozen/Joined/interner files.
LocalM3StringPiece is absent at its previous path. Its current FrozenByteInterner
is a 63-line synchronized HashMap with entry/payload admission bounds, not the
primitive LRU/MRU local-piece lane reviewed in PR7388/7392. Preserve that separate
work and resolve owner differences explicitly; do not overwrite it based on a
false assumption that latest develop contains the same implementation.

Native source/interface locations:
- synexia-indexstring/src/main/java/com/synexia/indexstring/nativebridge/MIndexNativeIntern.java
- synexia-indexstring/src/main/native/mindex_native_intern_jni.c
- MIndexNativeJoinedChars / MIndexNativeJoinedBytes in the indexstring package.

Compiler sources are under synexia-indexstring-compiler/src/main/java/com/synexia/indexstring/compiler/.
Existing relevant tests include MIndexJoinedStorageInternTest,
MIndexJoinedSegmentsTest, MIndexNativeJoinedStorageTest,
MIndexJoinedJdkRe2jConformanceTest, MatIndexHybridBoundaryLoweringTest, and
MatIndexStringLoweringExecutionVerifierTest. Their existence is not a pass claim.

## Central indexed composition and in-flight sources

MIndexString remains the central indexed abstraction: resolver-scoped interned
words/atoms feed IndexStringTuple and MIndexStringIntern.TupleBody; tuple identity,
indexes and precomputed facts serve operations before the exact asString boundary.
Joined character/byte storage is a supporting bridge, not a replacement for these
capabilities. The runtime experiment accepts ordinary Strings at that boundary;
it does not carry resolver namespace, lexical token IDs or derived tuple facts
inside its segment directory. No end-to-end MIndexString-native-VM integration is
claimed tested by this branch.

Connected PR metadata was checked during this experiment:
- #7425, head f9e4a68377101522fb16ed8a477d1adf3e8fe730: open draft restoring native
  tuple descriptors beneath the canonical TupleBody abstraction, preserving
  resolver IDs and facts. Its description explicitly lacks local reactor proof.
- #7429, head f129fbf000523e21734831d2b81e63ccd292c706: open draft restoring live
  shared arrays/precomputation and reporting its scoped proof separately.
- #7435, head 24dca1808e2bb4abc35c9a7894e81fd4adbd5b6e: the connector reported
  closed and unmerged at this check; its retained source head proposes ABI v3,
  heap/native-preferred/native-required runtime policy and direct compiler append.
  Those claims are not evidence that develop contains this implementation.

These are reuse candidates for the separately owned Synexia integration work,
not sources silently imported or declared complete here. The actual develop tree,
not parent ancestry or a merged flag, controls what is present. Process-life,
closeable, mapped and shared-arena identity/lifetime domains must remain distinct.
