<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->

# Retained regex reference: source and evidence coverage

[Reference](retained-regex-reference.md) · [Guide](README.md) · [Package ledger](packages/custody-01.md)

This is the body/member/evidence map for exactly seven root owners at public
M3JDK21 commit `363c02caa2e5af1df9460b927ecd78f6a9dbcefb`. Their complete bodies
were read, and current public byte identities were checked. Coverage means
source explanation, not compilation, test success, security certification,
provider availability, full package dependency closure or runtime admission.
No source, configuration, manifest, policy, workflow or license file is changed
by this reference. All repository code execution is **NOT_RUN** in this
documentation authoring/review pass. Hosted CI, if subsequently run, must be
reported separately for its exact commit and is not included in these NOT_RUN
statements.

## 1. Exact primary-owner inventory

All seven paths have prefix
`m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/`.
No `fs`, `precompute`, `shared`, AST or compatibility-port sibling is counted
as an additional primary owner. Hashes below are of the exact current file bytes,
not Java image roots, wire checksums, historical owner literals or artifact IDs.

| Primary source / full body | Lines / bytes | Git blob SHA-1 | SHA-256 |
| --- | --- | --- | --- |
| [MIndexCompactAsciiDfaImage.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexCompactAsciiDfaImage.java#L1-L199) | 199 / 7563 | `63094d85ec32de5dd99a8261c35936e2c5051fbe` | `31f4fd022eba03828d5ddc9d7dec0ab3dc978b3397d305f8b7655957e6a15269` |
| [MIndexRegexAcceleratedImage.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexAcceleratedImage.java#L1-L80) | 80 / 3102 | `f5d7c7bb37fbfb651e198fa1f9b692d8b3043402` | `c5d761d364c68ee588091c7258334afb8120125c1638c14c8151c37587360608` |
| [MIndexRegexPrecompute.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexPrecompute.java#L1-L111) | 111 / 5269 | `f59b1b1bd8623ac88eecf86bc272ad31a4aa9541` | `973210b927170cba5fc862a3a87336f5eb8e2e57ba7e8283a6339aa2a892ab13` |
| [MIndexRegexPrecomputeKey.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexPrecomputeKey.java#L1-L41) | 41 / 1943 | `9de8b8aa760adb4379c2d1067ffe8cefb0aee1af` | `6b911035ec43ae48ff4cabcabb38b33334138d09822249a3dec2f0ba3d1353a9` |
| [MIndexRegexProgram.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L1-L3760) | 3760 / 141730 | `f39709b64c82ddd1acf349446eeac8efe8f3ef27` | `4e217b9aad286aa2c3f1bc77d2900fe2e3ff1889550bfa399792fb999807732c` |
| [MIndexRegexProgramImageCodec.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgramImageCodec.java#L1-L692) | 692 / 30937 | `a9ba16b48c8c1756f40880f7c3092911258198ba` | `f7ad944462b279135d82c93e5490d3d6a88bf0e4dfb1de9bbb232990b9c6dc44` |
| [MIndexRegexTrigramQuery.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexTrigramQuery.java#L1-L411) | 411 / 15093 | `e7f1d4a181c76c0a2c4bc1464d3bc9e500cbbbea` | `428cb5cdd1afc83449ad35cbc228883dc36d3f1c36ceba8c208a6aa3aa0f400f` |

## 2. Program member-to-contract map

“Components” includes the generated record accessors; the explicit defensive
array accessors are listed separately. Generated record `equals`, `hashCode` and
`toString` follow record behavior, not a custom deep-array identity. Public
Program getters expose the stored values; private helpers below explain how
those values are constructed and consumed.

| Source range | Members | Contract route |
| --- | --- | --- |
| [17–102](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L17-L102) | `MIndexRegexProgram`, `Semantics.RE2/JDK`, opcode/table/planner limits, stored state | Reference §§1–3: boolean subset, optional optimizations and ownership |
| [110–215](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L110-L215) | `Image` components and compact constructor: expression, semantics, op/out1/out2/classId, asciiLow/asciiHigh, rangeOffset/rangeCount/rangeLow/rangeHigh, startState/matchState, minLength/maxLength/captureCount, requiredPresence64/requiredCharacterFlags/mayConsumeSurrogateUnit | §4: lane copies, per-opcode/coordinate checks and semantic-validation limits |
| [217–232](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L217-L232) | `Image.op/out1/out2/classId/asciiLow/asciiHigh/rangeOffset/rangeCount/rangeLow/rangeHigh`, `rootHash` | §4: cloned exports and portable root |
| [241–346](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L241-L346) | `ExecutionImage` components/constructor: program, asciiStateMasks, epsilonWordsPerRow/epsilonRowByState/epsilonTerminals, asciiSubsetBits/asciiSubsetSourceCount/asciiSubsetTransitions, bmpClassWordCount/bmpPageByHighByte/bmpClassMasks, anchoredAsciiDfa/searchAsciiDfa | §4: optional table geometry; no rederivation of table truth |
| [348–357](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L348-L357) | `ExecutionImage` six defensive array accessors; `rootHash` | §4: detached exports and execution identity |
| [465–496](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L465-L496) | `AsciiDfaImage` constructor/components, transitions/accepting accessors, stateCount, primitivePayloadBytes | §4: dense geometry, optional snapshots and primitive-only accounting |
| [648–685](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L648-L685) | Both `compile` overloads and both `tryCompile` overloads | §2: RE2 default, explicit semantics/budget, null refusal versus propagated exception |
| [800–802](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L800-L802) | `CompilationTimings` and all five generated component accessors | §2: CPU phases, unavailable -1 and completion flag |
| [810–842](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L810-L842) | `CompilationSession.Policy`, `Limits` constructor/components, `Statistics` components; reserved arena/index fields | §2: LRU/MRU, fixed capacities and retained metadata |
| [844–875](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L844-L875) | Both `CompilationSession` constructors; both session `tryCompile` overloads | §2: optional measurement, synchronized lifecycle, admit flag, exact hits |
| [876–897](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L876-L897) | `accountedBytes`, `statistics`, `lastTimings`, `clear`, `close` | §2: accounting exclusions, reset/closed distinction, busy refusal |
| [1015–1038](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L1015-L1038) | `image` | §4: portable snapshot with cloned lanes |
| [1040–1054](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L1040-L1054) | `PrecomputedImage` constructor/components; `precomputedImage` | §4: dispatch facts and bounded count, no custom root method |
| [1056–1085](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L1056-L1085) | `fromPrecomputedImage`, `executionImage`, public `fromExecutionImage` | §4: distinction between table restore and complete dispatch restore |
| [1150–1244](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L1150-L1244) | Both `fromImage` overloads | §4: no parser; cancellable derived-table rebuild |
| [1246–1264](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L1246-L1264) | `expression`, `semantics`, `stateCount`, `characterClassCount`, `captureCount` | §§2–3: immutable expression/mode and metadata, no capture results |
| [1266–1296](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L1266-L1296) | `trigramQuery` | §7: lazy synchronized publication, reparsing, unsupported fail-open ALL |
| [1298–1364](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L1298-L1364) | `requiredPresence64`, `requiredCharacterFlags`, `mayConsumeSurrogateUnit`, `firstAsciiLow`, `firstAsciiHigh`, `branchingStateCount`, `structurallyDeterministic`, `nullable`, `firstAsciiAllows`, `minLength`, `maxLength` | §3: necessary conditions, length units and conservative dispatch |
| [1366–1384](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L1366-L1384) | `primitivePayloadBytes` | §3: counts named primitive arrays, not retained heap/process memory |
| [1386–1422](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L1386-L1422) | `hasAsciiStateMasks`, `asciiStateMaskPayloadBytes`, `hasAsciiSubsetTransitions`, `asciiSubsetBits`, `asciiSubsetTransitionPayloadBytes`, `hasBmpClassMasks`, `bmpClassMaskPayloadBytes` | §§2–3: optional table availability and size diagnostics |
| [1424–1463](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L1424-L1463) | `hasPrecomputedEpsilonClosures`, `epsilonClosurePayloadBytes`, `hasAsciiDfa`, `asciiDfaStateCount`, `asciiDfaPayloadBytes`, `anchoredAsciiDfaImage`, `searchAsciiDfaImage` | §§3–4: optional projections and defensive snapshots |
| [1465–1496](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L1465-L1496) | `supportsInput`; support helper `supportsMetrics` | §3: surrogate-sensitive admission and trusted metadata sources |
| [1498–1617](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L1498-L1617) | `matches`, `lookingAt`, `find`; scratch overloads, fast and NFA paths | §3: whole/prefix/any-position boolean truth, refusal before fast paths |
| [1619–1628](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L1619-L1628) | `workspace` | §3: caller-owned reuse |
| [2525–2554](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L2525-L2554) | `Workspace.program`, `matches`, `lookingAt`, `find`, `primitivePayloadBytes`; private constructor | §3: one mutable scratch owner per worker, no input retention |

### Program internal support boundaries

All private/nested implementation regions were read; these are not additional
public APIs or separate canonical owners.

| Source range | Support members | Purpose |
| --- | --- | --- |
| [360–462](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L360-L462) | `imageRoot`, `executionImageRoot`, `updateDfaDigest`, `updateUtf8/Bytes/Shorts/Ints/Longs/Chars/Int/Long` | Domain-separated, ordered root construction |
| [498–646](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L498-L646) | Both Program constructors, `firstAsciiMask`, `countOpcode` | Owned lanes and derived-versus-precomputed dispatch |
| [688–798](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L688-L798) | `compileInternal` | Phase order, snapshots, refusal catch, timing/finally |
| [899–998](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L899-L998) | Session `expressionHash`, `parse`, `evict`, `internClass`, `internNode`, `restore`, `restoreNode` | Collision confirmation, append-only IDs, partial retention and exact remap |
| [999–1013](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L999-L1013) | `ParsedSyntax`, `SharingLimit`, `PhaseCpu` constructor/now/elapsed/parsed/lowered/tabled/finish | Cold transient syntax and timing/refusal support |
| [1087–1148](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L1087-L1148) | Private `fromExecutionImage` overload | Copy tables and optionally reuse dispatch facts |
| [1630–1707](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L1630-L1707) | `asciiKnowledge`, `requiredFactsPresent`, `characterFlags`, `requireSupportedInput` | Metadata trust, unknown/fail-open facts and explicit unsupported-input throw |
| [1709–1855](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L1709-L1855) | `step`, `addClosure`, `pushClosureState`, `isAsciiWordBoundary`, `isAsciiWord`, `stepAsciiSubsetTable`, `classMatches`, `set` | Bitset stepping, absolute assertions, ASCII boundary math and class lookup |
| [1857–1964](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L1857-L1964) | `buildAsciiStateMasks`, `buildBmpClassMasks`, `BmpClassTable/NONE` | Masks, page geometry, assignment/byte bounds and cancellation |
| [1966–2064](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L1966-L2064) | `buildAsciiSubsetTransitions`, `tryBuildAsciiSubsetTransitions`, `AsciiSubsetTable/NONE` | Eight/four-bit choice, exact geometry and absence |
| [2066–2416](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L2066-L2416) | `AsciiDfaTable`: constructor/fromImage/tryBuild; boundedStateLimit/fitsBuildBudget/hashCapacity; subset lookup/insertion/hash/mix/equality/contains/closure helpers; matches/lookingAt/find and known-ASCII variants; stateCount/payloadBytes/image/empty | Bounded determinization, restart semantics, no native execution |
| [2418–2523](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L2418-L2523) | `EpsilonClosureTable`: constructor/tryBuild/pushStatic, both orInto forms, payloadBytes | Assertion refusal, sparse closure rows and fallback |
| [2556–2593](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L2556-L2593) | `Scratch`: constructor/reset/beginClosure/contains/isEmpty | Mutable work lanes and stamp wraparound |
| [2595–2724](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L2595-L2724) | `trigramQuery`, `trigramConcatQuery`, `trigramExact`, `trigramConcatExact`, `trigramAlternateExact`, `trigramRepeatExact`, `trigramCross` | Bounded exact language and adjacent finite-run necessary conditions |
| [2726–2842](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L2726-L2842) | `Node`; Empty/Atom/Quoted/Start/End/WordBoundary/Concat/Alt/Repeat node records and metadata methods/constructors | Parser algebra and quoted-tail binding marker |
| [2844–2938](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L2844-L2938) | `mayConsumeSurrogateUnit`, `anyMayConsumeSurrogate`, concat/alt requiredPresence/Flags, sumMin/sumMax/altMin/altMax/multiplyMin/multiplyMax | Necessary metadata, surrogate flags and saturated length arithmetic |
| [2940–3271](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L2940-L3271) | `Parser`: constructor/parse/parseAlternation/parseConcatenation/parseRepeated/parseBounds/parseDecimal/parseAtom/atom/parseGroup/parseEscape/parseQuotedLiteral/parseHex/parseClass/tryParsePosixClass/parseClassPiece/peek | Accepted subset and syntax/feature/state refusals |
| [3273–3300](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L3273-L3300) | `ClassPiece` single/spec/apply; `ClassSpec` constructor/apply/key | Character-class intermediate representation |
| [3302–3593](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L3302-L3593) | `ClassTableBuilder`: literal/dot/digits/word/space/specDigits/specWord/specSpace/specPosix/fromAllowed/smallExactStrings/canConsumeSurrogateUnit/requiredPresence64/requiredFlags/allWithin/allWord/allSpace/rangeWithin/maybeNegate/add/freeze/compress | Exact class lanes, semantic mode differences and conservative facts |
| [3595–3760](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L3595-L3760) | `ClassTable`; `Compiler` constructor/compile/epsilon/atom/assertion/concat/alternate/repeat/concatenate/add/patch/ops/out1/out2/classIds; `State`, `Fragment`, `Patch`; unsupported/UnsupportedRegex | Thompson lowering, patch ownership, maxStates refusal and frozen primitive arrays |

## 3. Other six owners: complete member map

| Owner and range | Members | Contract route |
| --- | --- | --- |
| [MIndexCompactAsciiDfaImage.java 19–65](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexCompactAsciiDfaImage.java#L19-L65) | Record components/constructor: startState, asciiClassByValue, classCount, transitions, accepting | §4: copies and compact geometry |
| [MIndexCompactAsciiDfaImage.java 67–115](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexCompactAsciiDfaImage.java#L67-L115) | asciiClassByValue/transitions/accepting, stateCount, primitivePayloadBytes, nextState, rootHash | §4: defensive exports, checked lookup and compact root |
| [MIndexCompactAsciiDfaImage.java 117–199](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexCompactAsciiDfaImage.java#L117-L199) | from, requireEquivalent; private sameColumn | §4: hash-routed exact column grouping and dense equivalence |
| [MIndexRegexAcceleratedImage.java 18–47](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexAcceleratedImage.java#L18-L47) | Record constructor/components; from, restoreProgram | §4: optional paired projections and execution-image restoration |
| [MIndexRegexAcceleratedImage.java 49–80](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexAcceleratedImage.java#L49-L80) | rootHash; private validatePair/update | §4: pair validation and framed root identity |
| [MIndexRegexPrecompute.java 15–46](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexPrecompute.java#L15-L46) | Private constructor; Request constructor/components; Compiler.compile; Result constructor/components | §6: exact revisions, null unsupported and matching Optional presence |
| [MIndexRegexPrecompute.java 48–58](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexPrecompute.java#L48-L58) | key, default restoreOrCompile | §6: request identity and default compiler |
| [MIndexRegexPrecompute.java 60–111](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexPrecompute.java#L60-L111) | Custom restoreOrCompile; requireCompatible | §6: hit/miss ordering, error boundaries and publication |
| [MIndexRegexPrecomputeKey.java 11–41](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexPrecomputeKey.java#L11-L41) | Package-private class/constructor, PROGRAM_OWNER/CODEC_OWNER, key, revision | §6: historical literals, runtime/expression hash, ASCII revision validation |
| [MIndexRegexTrigramQuery.java 17–116](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexTrigramQuery.java#L17-L116) | Op; private constructor; package-private all/none/fromExact/and/or; private combine | §7: algebra, sorted direct terms and immutable children |
| [MIndexRegexTrigramQuery.java 118–170](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexTrigramQuery.java#L118-L170) | op, trigrams, subqueries, hasConstraints, termCount, test; package-private trigramsForReadOnlyKernel/subqueriesForReadOnlyKernel | §7: defensive public access, trusted kernel access and callback order |
| [MIndexRegexTrigramQuery.java 172–236](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexTrigramQuery.java#L172-L236) | Both precompute overloads; private scanFacts; testPrecomputed | §7: null/range/budget ordering, one-read scan and isolated range scope |
| [MIndexRegexTrigramQuery.java 238–321](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexTrigramQuery.java#L238-L321) | Facts constructor, utf16Length, keyCount, keys, test, concat; private first/seamKeys | §7: retained boundary metadata, merge/seam correctness and ownership |
| [MIndexRegexTrigramQuery.java 323–386](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexTrigramQuery.java#L323-L386) | requireFactBudget, compactFactKeys, mergeFactKeys; public trigram; package-private uniqueTrigrams | §7: bounded exact code-unit membership |
| [MIndexRegexTrigramQuery.java 388–411](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexTrigramQuery.java#L388-L411) | toString; private render/quote | §7: diagnostics only |

### Codec overloads and all support helpers

| Source range | Members | Contract route |
| --- | --- | --- |
| [30–48](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgramImageCodec.java#L30-L48) | Constants and private constructor | §5: seven versions, magic/header and hard codec bounds |
| [50–105](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgramImageCodec.java#L50-L105) | encode(Image) | §5: portable lanes and v1/v4 metadata |
| [111–140](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgramImageCodec.java#L111-L140) | encode(ExecutionImage) | §5: v2/v5 with nested base |
| [146–164](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgramImageCodec.java#L146-L164) | encode(AcceleratedImage) | §5: v3/v6 with dense/compact pairing |
| [167–183](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgramImageCodec.java#L167-L183) | encode(PrecomputedImage) | §5: v7 nested execution and dispatch facts |
| [185–206](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgramImageCodec.java#L185-L206) | decodePrecomputed(byte[]), decodePrecomputed(ByteBuffer) | §5: array clone versus private buffer views; v7 framing |
| [208–222](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgramImageCodec.java#L208-L222) | decode(byte[]), decode(ByteBuffer) | §5: portable v1/v4 |
| [224–246](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgramImageCodec.java#L224-L246) | decodeAccelerated(byte[]), decodeAccelerated(ByteBuffer) | §5: v3/v6, exact nested version and compact validation |
| [248–258](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgramImageCodec.java#L248-L258) | decodeExecution(byte[]), decodeExecution(ByteBuffer) | §5: v2/v5 |
| [260–273](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgramImageCodec.java#L260-L273) | write(Path, Image/AcceleratedImage/ExecutionImage) | §5: encode-before-create-only I/O; no v7 overload |
| [275–299](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgramImageCodec.java#L275-L299) | read, readMapped | §5: portable file size/transport and lifetime |
| [301–321](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgramImageCodec.java#L301-L321) | readAccelerated, readAcceleratedMapped | §5: accelerated file transport |
| [323–342](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgramImageCodec.java#L323-L342) | readExecution, readExecutionMapped | §5: execution file transport |
| [344–429](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgramImageCodec.java#L344-L429) | decodeExecutionPayload, decodePayload | §5: nested bounded slices, counted lanes and trailing-data refusal |
| [431–459](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgramImageCodec.java#L431-L459) | requiresUtf16, readExpression | §5: unpaired-surrogate version choice and exact text decoding |
| [461–510](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgramImageCodec.java#L461-L510) | imageVersion, requireNestedVersion, envelope, payload | §5: validation order, framing and checksum |
| [512–537](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgramImageCodec.java#L512-L537) | writeCountedInts/Longs, writeCompactDfa | §5: deterministic counted lanes/presence |
| [539–570](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgramImageCodec.java#L539-L570) | readCompactDfa | §5: presence/count/remaining guards before constructor validation |
| [572–605](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgramImageCodec.java#L572-L605) | writeDfa, writeInts/Longs/Chars, readCountedInts/Longs | §5: primitive lane widths |
| [607–649](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgramImageCodec.java#L607-L649) | readDfa, readInts/Longs/Chars | §5: dense validation, byte arithmetic and lane allocation |
| [651–692](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgramImageCodec.java#L651-L692) | boundedCount, requireCount, getInt, getLong, requireRemaining, both sha256 overloads | §5: bounded scalar reads, malformed input and digest handling |

## 4. Bounded dependency/caller review, separate from primary scope

The following public bodies/ranges were read to trace calls made by the seven
owners. This is **not a complete FS package review**. Generic typed precompute,
The [generic typed precompute reference](retained-precompute-reference.md) covers
the eight typed-plan owners. FS bundle/catalog pipelines, native-content implementations and unrelated
siblings retain separate review/qualification work. No replacement dependency
or runtime wiring is introduced.

| Dependency range | Reason read |
| --- | --- |
| [fs/M3ContentId.java 1–71](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3ContentId.java#L1-L71) | Expression-hash input parsing, SHA-256 content identity and defensive byte exports |
| [fs/M3ObjectRef.java 1–13](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3ObjectRef.java#L1-L13) | Immutable id/size/kind tuple; nonnegative size |
| [fs/M3ObjectKind.java 1–15](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3ObjectKind.java#L1-L15) | PRECOMPUTE_OUTPUT and PRECOMPUTE_DESCRIPTOR role distinction |
| [fs/M3PrecomputeKey.java 19–168](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeKey.java#L19-L168) | Canonical request encoding, ordered inputs, sorted configuration, defensive bytes; decode validation |
| [fs/M3PrecomputeResult.java 12–102](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeResult.java#L12-L102) | VALUE/EMPTY invariant, descriptor bytes and decode guards |
| [fs/M3PrecomputeCache.java 16–46](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeCache.java#L16-L46) | Constructor directory effect and lookup/key-descriptor verification |
| [fs/M3PrecomputeCache.java 220–280](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeCache.java#L220-L280) | Descriptor put, lock/pointer publication, conflicting-result refusal and temporary cleanup |
| [fs/M3DirectoryContentStore.java 485–590](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3DirectoryContentStore.java#L485-L590) | Buffered output put, size/digest read checks and openMapped handoff |
| [fs/M3DirectoryContentStore.java 727–779](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3DirectoryContentStore.java#L727-L779) | Create-only content publication, exact-existing comparison and cleanup boundaries |
| [fs/M3MappedObject.java 19–93](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3MappedObject.java#L19-L93) | Per-mapping admission, integrity, read-only view and nondeterministic unmapping lifetime |
| [precompute/MIndexPrecomputePasses.java 49–59](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePasses.java#L49-L59) | Length-prefixed exact UTF-16 hashing used by the regex request key |
| [precompute/MIndexPrecomputeScope.java 70–72](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeScope.java#L70-L72) | Big-endian long length framing used by sha256Utf16 |

Exact current dependency file identities (a hash identifies bytes, not full-body coverage):

| Dependency | Bytes | Git blob SHA-1 | SHA-256 |
| --- | --- | --- | --- |
| [M3ContentId.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3ContentId.java) | 2243 | `b8dffdfaf6809e130b7c95ae38c79cb2beb937c6` | `c1412090abf49c04fa5cc9b5d457d3809ad4bbac5bb41c45085b6ccaf075d74e` |
| [M3ObjectRef.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3ObjectRef.java) | 452 | `41cfec7999b4e07689069b584ce825d76a0b9e9d` | `e69debebeae92ecae49c7047f293ae256045243caec5e9e8b17fe81560499c77` |
| [M3ObjectKind.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3ObjectKind.java) | 379 | `661a00395debb0d216d36cb028bcd72bb8651aa0` | `4ab6ba55814edd9501803e26e03c0dee8c73b033c105f1b3756ce7bf5c15c4ea` |
| [M3PrecomputeKey.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeKey.java) | 6630 | `25d34816903b02a5af53a131191d72f71d09dc09` | `720cff4115f6d30a90d228ae4a98a3fef76763c1ab0b08d909218c221f7d4cde` |
| [M3PrecomputeResult.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeResult.java) | 3747 | `727c38c2bd9c91cc643feff2fbb1c4290ccfda5d` | `e50355b571f0ebb61319ddf338003481c26e177f8346e1920f6702a457269fe4` |
| [M3PrecomputeCache.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeCache.java) | 11298 | `3603832eefe138bea46f22a7c1bf548153d2d391` | `14ecc79d5d298539085aa922090eb70665c05e913ef99acc74faeb040f00ea87` |
| [M3DirectoryContentStore.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3DirectoryContentStore.java) | 27555 | `0fd9aec324ca0e1f608b0a72c22945ded63bd486` | `882a34ba5e4956c38c3c6ebd37840c76bc2ea7e480d2d86a3ef2cee5c7ea3f39` |
| [M3MappedObject.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3MappedObject.java) | 5840 | `103921e3c8efcf102c09c684faf46e2ac2c81838` | `4238d100d53cea1a7bbe3b4fd4105bf3f0b3a3acb30b0aee67e4fbe3797708a4` |
| [MIndexPrecomputePasses.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePasses.java) | 3341 | `8dfe2ab9cc99da76a4f3eb5b5d41b6b0b2c7189f` | `8973ba20e7c524b0cef2e10250a84f69297b340012d734e260b0459cc8cd90e0` |
| [MIndexPrecomputeScope.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeScope.java) | 2684 | `712e219585561967dc3152e1323368c2d87dffb8` | `32e0508e5502ba50b9ea833cd7193f821122a935f72a7ae634c3a12733a0e3a9` |

Within the primary slice, AcceleratedImage calls Program execution snapshots and
Compact equivalence; Codec constructs those immutable records; Precompute calls
Program/v7 codec and the FS cache; PrecomputeKey calls raw-UTF-16 hashing; Program
builds TrigramQuery. There is no retained code here that installs a Pattern/Matcher
replacement, exports a JDK module or launches a native provider.

External original-owner references observed in Program include `MIndexString`,
`IndexLexiconWordView`, `IndexUtf16SearchImage.RowView`, `IndexTextMetrics`,
`IndexCharacterFlags`, `IndexCharacterFlagsResolver`, `MIndexStringBitOps`,
`IProgressMonitor` and `PackedLongLongOrderedMap`. The seven bodies and the
retained subtree listing do not establish their full original module closure.
Their referenced metadata/lifetime preconditions are described in the reference,
without substituting frozen-text fixture or `ports/indexstring` owners.

## 5. Historical public oracle bodies read, NOT_RUN

Both files below are public retained `.java.txt` evidence. They are not executable
root-regex tests under this retained module's `src/test`. Their dependencies include
original text/resolver/hybrid/search owners and RE2/J outside this seven-file slice.
Reading a method's assertions is not running that test or qualifying its complete
historical environment. The optional JNI method can return when a provider is
unavailable unless its required property is set; its presence is not a native pass.

| Historical oracle | Bytes | Git blob SHA-1 | SHA-256 |
| --- | --- | --- | --- |
| [MIndexRegexProgramTest.java.txt](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt) | 39322 | `13ec38e57bd6d4f8317566821f8d6e2dad153961` | `5c185acb76ef6de2a79eebec30a6b156fa982c7283cb0f4d0ef96840ce1a9dd6` |
| [MIndexRegexTrigramQueryTest.java.txt](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexTrigramQueryTest.java.txt) | 11007 | `601a424ef6f477e675c64cdaa0643b0247c5476a` | `bae0950fd49e10eaada3d8e9e61a9b99dd251357f3952bbd867e134f77ef2225` |

### MIndexRegexProgramTest.java.txt

| Method / source range | Evidence scope |
| --- | --- |
| [compilationHonorsProgressCancellation](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L23-L31) | Historical assertions read; NOT_RUN |
| [imageRestoreHonorsProgressCancellation](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L34-L44) | Historical assertions read; NOT_RUN |
| [primitiveProgramMatchesRegularSubsetWithPrimitiveState](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L47-L58) | Historical assertions read; NOT_RUN |
| [precomputedBitKernelsPreserveBooleanSemantics](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L61-L83) | Historical assertions read; NOT_RUN |
| [boundedAsciiDfaPreservesAllBooleanOperationsAndFallsBackForBmp](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L86-L109) | Historical assertions read; NOT_RUN |
| [regexImageRoundTripRebuildsDfaSubsetAndBmpAccelerators](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L112-L129) | Historical assertions read; NOT_RUN |
| [frozenAndMappedAsciiFactsDriveCompactDfaWithoutChangingTruth](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L132-L174) | Historical assertions read; NOT_RUN |
| [bitKernelPrecomputationIsSparseBoundedAndAssertionsUseDynamicClosure](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L177-L210) | Historical assertions read; NOT_RUN |
| [jdkSubsetIsDifferentiallyEquivalentForBooleanOperations](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L213-L254) | Historical assertions read; NOT_RUN |
| [acceptedSubsetIsDifferentiallyEquivalentAcrossGeneratedCorpus](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L257-L305) | Historical assertions read; NOT_RUN |
| [astPresenceProofIsConservativeAcrossRegexAlgebra](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L308-L339) | Historical assertions read; NOT_RUN |
| [requiredCharacterClassFactsAreConservativeAcrossRegexAlgebra](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L342-L368) | Historical assertions read; NOT_RUN |
| [exactResolverClassFactsRejectBeforeCharacterReads](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L371-L414) | Historical assertions read; NOT_RUN |
| [resolverFamiliesExposeConsistentPrecomputedRegexFacts](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L417-L459) | Historical assertions read; NOT_RUN |
| [reusableWorkspaceDoesNotLeakStateAcrossInputs](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L462-L476) | Historical assertions read; NOT_RUN |
| [bmpCharacterClassesUsePagedDirectAddressMasks](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L479-L517) | Historical assertions read; NOT_RUN |
| [asciiSubsetTransitionsUseDirectAddressRamTableWithoutChangingTruth](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L520-L581) | Historical assertions read; NOT_RUN |
| [astPresenceAlgebraCoversGroupedMandatoryText](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L584-L607) | Historical assertions read; NOT_RUN |
| [plannerProvenLiteralsProduceSafePrecomputedPresenceGate](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L610-L628) | Historical assertions read; NOT_RUN |
| [lexiconPresenceGateRejectsBeforeCharacterReads](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L631-L679) | Historical assertions read; NOT_RUN |
| [re2PosixClassesLowerToPrimitiveAsciiRanges](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L682-L736) | Historical assertions read; NOT_RUN |
| [whitespaceClassesPreserveRe2AndJdkVerticalTabDifference](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L739-L781) | Historical assertions read; NOT_RUN |
| [re2WordBoundariesUseNativeAsciiBoundaryMath](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L784-L833) | Historical assertions read; NOT_RUN |
| [lazyQuantifiersAndQuotedLiteralsStayNativeForBooleanTruth](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L836-L914) | Historical assertions read; NOT_RUN |
| [unsupportedOrSemanticallyDifferentSyntaxDelegatesInsteadOfApproximating](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L917-L933) | Historical assertions read; NOT_RUN |
| [supplementaryInputDelegatesOnlyWhenAnAtomCanConsumeSurrogateUnits](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L936-L990) | Historical assertions read; NOT_RUN |
| [hybridUsesNativeBackendForNoCaptureSubsetAndRe2ForCaptures](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L993-L1005) | Historical assertions read; NOT_RUN |
| [mindexStringMatchesPreservesJdkSemanticsWithoutRequiringStringInputCopy](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L1008-L1023) | Historical assertions read; NOT_RUN |

### MIndexRegexTrigramQueryTest.java.txt

| Method / source range | Evidence scope |
| --- | --- |
| [extractsConservativeBooleanTrigramStructureFromNativeRegexAst](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexTrigramQueryTest.java.txt#L17-L27) | Historical assertions read; NOT_RUN |
| [exactAlternativesAndSmallClassesProduceUsefulQueries](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexTrigramQueryTest.java.txt#L30-L48) | Historical assertions read; NOT_RUN |
| [queryNeverRejectsARealFindForSupportedCorpus](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexTrigramQueryTest.java.txt#L51-L103) | Historical assertions read; NOT_RUN |
| [finiteDomainPlanPrunesDistinctSpellingsBeforeAuthoritativeVerification](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexTrigramQueryTest.java.txt#L106-L131) | Historical assertions read; NOT_RUN |
| [forcedNativeSparseAndPreservesHybridRegexResults](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexTrigramQueryTest.java.txt#L134-L169) | Historical assertions read; NOT_RUN |
| [adjacentFiniteRunsRetainTheQuotedCandidateStructure](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexTrigramQueryTest.java.txt#L172-L196) | Historical assertions read; NOT_RUN |
| [finiteRunBudgetAndEmptyBranchesNeverRejectAnAuthoritativeFind](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexTrigramQueryTest.java.txt#L199-L245) | Historical assertions read; NOT_RUN |
| [finiteRunRecoveryKeepsExistingModeAndSurrogateRefusals](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexTrigramQueryTest.java.txt#L248-L263) | Historical assertions read; NOT_RUN |

Program oracle support includes imports/TempDir (lines 1–21) and generated-corpus/
printable helpers (1024–1039); Query oracle support includes imports (1–15) and
shape/membership helpers (265–279). The complete files, not just named assertions,
were read. Program assertions cover progress cancellation, finite RE2/JDK boolean
comparisons, optional masks/DFA/BMP/subset behavior, image reconstruction,
workspace reuse, required facts, quoting/classes/boundaries, surrogate refusal
and historical hybrid/string callers. Query assertions cover AND/OR/NONE,
finite alternatives, candidate-only false-positive examples, finite-domain plans,
optional JNI behavior, adjacent runs, bounded expansion and refusal preservation.

These snapshots do not contain direct test bodies for CompilationSession,
PrecomputedImage/v7 persistence, current Codec transport/failure paths,
Compact/Accelerated image geometry, or Facts whole/range/concat APIs. The reference
explains those inspected implementations but does not mark those execution gates
passed. The original intake qualification and later codec records are distinct
historical receipts; no missing current test is inferred to have run from them.

## 6. Provenance map and intentional successor evolution

The chain preserves old evidence instead of rewriting old manifests to describe
new bodies. Public source-custody records identify donor revisions; their claims
are attributed historical records, not a new verification of another repository.

| Stage | Exact recorded identity/change | Public authority |
| --- | --- | --- |
| Original v7 | Donor 80d288951148548ef6aa4c95df9cb31a438aac16; six root owners among 22 nonlicense payloads, plus LICENSE/NOTICE. Manifest root d1889319f72f5e3ab9fb4804e27e8ec58d2bfc75e112539f5322f5f95e3d658b | [export.tsv](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/export.tsv) |
| Original custody | Program 9999e8afe1ec44de58edd644c0ca278415fb7a379f6cdacfe9dbf7011ed77131; codec 09f4ef3d52a432b5a46bd784a91728352d121241dca9ab010df7a06f38d01dc7; closure/runtime flags false | [SOURCE_CUSTODY.json](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/SOURCE_CUSTODY.json) |
| Receipt binding | PROVENANCE binds export raw SHA-256 ae72faedec7913ee25e74f4592b06bc9b6e31e80800587df1629619f91b20c9e and SOURCE_CUSTODY SHA-256 97df0a295c9bb3c32ff71a3df6380c5beceb21e4528c218baa12cd4c39df0fb4 | [PROVENANCE.json](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/PROVENANCE.json) |
| Program successor | 9999e8afe1ec44de58edd644c0ca278415fb7a379f6cdacfe9dbf7011ed77131 → 4e217b9aad286aa2c3f1bc77d2900fe2e3ff1889550bfa399792fb999807732c | [manifest.tsv](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/recipe/templates/manifest.tsv) |
| Query addition | ABSENT → 428cb5cdd1afc83449ad35cbc228883dc36d3f1c36ceba8c208a6aa3aa0f400f | [manifest.tsv](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/recipe/templates/manifest.tsv) |
| Successor execution attribution | Actual authority distinguishes later receiver execution from the frozen metadata review; separate donor production/test qualification is reported as 40 selected methods, not current-head execution here | [CURRENT_ACTUAL_AUTHORITY.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/CURRENT_ACTUAL_AUTHORITY.md) |
| Codec copy recovery | 09f4ef3d52a432b5a46bd784a91728352d121241dca9ab010df7a06f38d01dc7 → c7f17a7b60b8e90c5bc8730c6a5c222dd3598474ea126377ecdf89cb0bcc6d8e; recipe commit 970b2a40b4d6df391027f1bcd59bb6362d62276b; NOT_ADMITTED | [regex-copy-recovery-20261011.json](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/qualification/regex-copy-recovery-20261011.json) |
| Codec nested-slice successor | c7f17a7b60b8e90c5bc8730c6a5c222dd3598474ea126377ecdf89cb0bcc6d8e → f7ad944462b279135d82c93e5490d3d6a88bf0e4dfb1de9bbb232990b9c6dc44; recipe commit c41b6adb6b64f6f0432a0599e09a6c6dae99c93e; NOT_ADMITTED | [regex-nested-slice-20261011.json](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/qualification/regex-nested-slice-20261011.json) |
| Current request-key contract | PROGRAM_OWNER/CODEC_OWNER still embed original hashes; Request requires independently accurate compilerRevision/codecRevision covering behavior-bearing dependencies | [MIndexRegexPrecomputeKey.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexPrecomputeKey.java#L13-L39); [MIndexRegexPrecompute.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexPrecompute.java#L18-L27) |

The historical `PROVENANCE.json` field named `README.md` records the original
packet input; do not equate that recorded historical README hash to later edited
intake README bytes. The current documentation cites the export/custody bindings
and the separately identified current authority/qualification records.

Exact public evidence-file bytes inspected for this reference:

| Evidence at documentation pin | Bytes | Git blob SHA-1 | SHA-256 |
| --- | --- | --- | --- |
| [export.tsv](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/export.tsv) | 8547 | `7bc78e7c6f65ccd1f56cf6f06907512a5a50fa36` | `ae72faedec7913ee25e74f4592b06bc9b6e31e80800587df1629619f91b20c9e` |
| [PROVENANCE.json](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/PROVENANCE.json) | 1518 | `1af65edd0f61261d6bcce7200ee1b3860c623fe9` | `dfa3e4023a370da8a0b6f406a2391b2d1c24f86915e785f9287cab10d111e014` |
| [SOURCE_CUSTODY.json](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/SOURCE_CUSTODY.json) | 29478 | `dc3b25ff91f9f32c66e9a9989992acf0d6bc9721` | `97df0a295c9bb3c32ff71a3df6380c5beceb21e4528c218baa12cd4c39df0fb4` |
| [README.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/README.md) | 1929 | `703e4bad34a6a474e4fbfa399c0590db656546ff` | `b31fe2f51acb0c591860fe9ebd41da95c9696a21751b6e9a2fb38ba2a0dd3f58` |
| [QUALIFICATION.json](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/evidence/QUALIFICATION.json) | 2048 | `4d54c1449f4189264d84f1eda27a9bacfd591370` | `597e119d7fd3375a0c6fd8ddf88c1f9a6c32932c01ed76f32e00ea14023a1dab` |
| [manifest.tsv](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/recipe/templates/manifest.tsv) | 620 | `3ab0de89d0388b0da8c1895a481e4ec6edb38239` | `c460929619e876fb0c96a90fbe62383157ccc872d9ef5d9947e5cd74e5937b9c` |
| [CURRENT_ACTUAL_AUTHORITY.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/CURRENT_ACTUAL_AUTHORITY.md) | 3888 | `8c67c45d6432034d06700a5dc3e7200eaa8986d8` | `6927b0cc5b33eae7d62bfd9c33fbd9262b103111993c50e861554e9d990737ac` |
| [regex-copy-recovery-20261011.json](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/qualification/regex-copy-recovery-20261011.json) | 4084 | `297e816bf313729e628e53727d1b6a04904a83fb` | `0f66d81f02325c3684fb41264b9ea215d56616429ee6c37d0b8a00287de3d5ed` |
| [regex-nested-slice-20261011.json](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/qualification/regex-nested-slice-20261011.json) | 12278 | `6c1047feb93d9b4074c8303597c7e41841a96b9c` | `21db91365d98c8fd4e833622e2b32d5f119e24ec553d07c7f5e6be562d8bd513` |

The original QUALIFICATION record reports 21 importer tests and bounded host/AST
and replay evidence, with the exact scope named in that record. It explicitly
excludes a source-complete receiving build, consumer wiring, Maven/coverage gate,
JNI/native, matched JDK/jtreg, performance and full-family completion. The two
codec receipts report bounded source/SDK/runtime/JNI and allocation experiments,
while separately withholding complete receiving Program/JDK qualification and
promotion. Their diagnostic allocation findings are not current retained-heap,
throughput or provider admission results.

## 7. Policy, build and existing-guide reconciliation

Read-only authorities reviewed include vendor custody; delivery, borrowing and
Apache reuse policy; the canonical porting invariant and relevant name-map rows;
the public full-family pin/catalogue and current-full-borrow README, String phase,
pin, phase order, receiver status and String gates. Exact current instruction/map
identities are:

| Authority | Bytes | Git blob SHA-1 | SHA-256 |
| --- | --- | --- | --- |
| [AGENTS.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/AGENTS.md) | 4554 | `2253be71930e69f34420a72df8e8ba278ae61fb5` | `7de3689f6941871be9af41171851efa87cea662b29d831ecf87eeea4a5a2919b` |
| [name-mapping.json](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/name-mapping.json) | 399051 | `288f20575d3f9d106571990f21944e7f46b0cf71` | `f99b80ededfb898804547ad3baf87cdc86aaccda4331c076591e2459d4f09ecb` |
| [synexia-full-family-pin.tsv](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/compatibility/synexia-full-family-pin.tsv) | 1171 | `3d24fc38677435453787ca67f3d5458dbfaf5b5f` | `5d2a89e133164bfc276ed834be47ad3d1bd2583e4bed08a7f94a7e1eb2b50bcc` |
| [synexia-full-family-catalogue.json](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/synexia-full-family-catalogue.json) | 10564 | `8145c667c941a9358e9a5149489e455d950afc34` | `2e6fa85aeaaec5a6c37aa429b66d102ee9497735fdd5476ca4085d34667b5eef` |
| [README.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/current-full-borrow/README.md) | 4013 | `92347a83839d85d5e9b07b829a545f5c8a4c431a` | `9b6960f76ed3dfaf2cb97ad497b59d0c76294502d6d0047bed70a1dd7d5e26c6` |
| [STRING_PHASE.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/current-full-borrow/STRING_PHASE.md) | 2931 | `1d6c3d266e3c34c336fb75c6c3c2a99d1fd47e35` | `1f02e854d90fb7cd2f29ca04dbfa29bd3be7a86248fd4ee12978cac28fbae676` |
| [pin.tsv](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/current-full-borrow/pin.tsv) | 1328 | `7b3c0b7d0a6b8daab40ef0062a9e9293d9b7abd2` | `d84e185891f08d76da765e43d1e138e33fbef99f50de9cc83a99c5284ab46ab5` |
| [promotion-phases.tsv](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/current-full-borrow/promotion-phases.tsv) | 1070 | `c4e8582f99caba32d718b2c1e86dae0854cdce3d` | `e4730068f321d551913a4cf7d40832f43b315c3973312c73b744ca5081758eb5` |
| [receiver-status.tsv](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/current-full-borrow/receiver-status.tsv) | 3721 | `be8a15064e9b50f2c5c2a581159ee9e6b3e60cbc` | `7c1326d024e111e953c139e30be1cfe3026bab0458d648963efb0247684a0715` |
| [string-phase-gates.tsv](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/current-full-borrow/string-phase-gates.tsv) | 1869 | `8e28b7278d35b869d08e39bad37675b195809628` | `a984bca40b74a7da856780a4ed92b665591285394413631470746beaefa2ffdf` |

The root instruction and naming-map Git blobs were independently read at the
specified public commit. Relevant naming rows keep exact Java regex semantics
authoritative and precompute internal; the full-borrow inventory does not advance
String → arrays → collections → AST/compiler → remaining-family promotion.
Repository policy verifiers were not run in this documentation-only pass.

The existing routes in `packages/custody-01.md`, `indexed-string-reference.md`,
`runtime-owners.md` and `projects.md` were read to retain their bounded ownership
claims. This reference adds detail for these seven retained regex owners; it does
not replace the separate compatibility bridge or bootstrap/runtime guides.
`m3/pom.xml`, `m3/synexia-import/pom.xml`, `m3/build.sh` and vendor custody establish
separate build responsibilities, not a standalone retained regex build. The
retained subtree has no root regex test sources or module/POM descriptor.

## 8. Verification disposition

| Check | Disposition |
| --- | --- |
| Seven current primary bodies and all nested/member/support regions | Read; exact file identity checked; complete primary scope only |
| Public oracle `.java.txt` bodies | Read in full; historical evidence; NOT_RUN |
| FS dependency boundary | Named call paths read; broader package/member proof remains separate |
| Compilation and complete dependency closure | NOT_RUN / not established by this retained slice |
| Unit tests, policy verifiers, Maven/JaCoCo, JDK build/jtreg | NOT_RUN |
| Native/JNI/GPU/library loading and platform qualification | NOT_RUN |
| CPU, retained heap, native/process memory or throughput benchmarks | NOT_RUN |
| Matcher captures/regions/state, full-family parity or runtime promotion | Not established |
| Documentation-only examples and inferred call sequences | Inspected source explanations; not executed examples |

A future qualification must bind the exact producer/consumer revisions, complete
dependency closure, source/target and serialized identities, input owner/range,
UTF-16 behavior, failure/effect ordering, budgets and cold/warm behavior. Source
hashes and finite historical oracles alone cannot close those gates.
