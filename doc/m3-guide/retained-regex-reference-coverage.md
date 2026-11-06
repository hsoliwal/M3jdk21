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
| [MIndexCompactAsciiDfaImage.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexCompactAsciiDfaImage.java#L1-L199) | 199 / 7563 | `f1bb86763a0d57fc546ce5995952ef095e9865ef` | `62ee73e6c7458d38e54570a2196e12b4fe468004f379f69c1be72dcd46ab780d` |
| [MIndexRegexAcceleratedImage.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexAcceleratedImage.java#L1-L80) | 80 / 3102 | `7371c9e3d668f05de4d1af7397a88435967b7105` | `ef7a5428ec39215ccaa39c9728079998230a319d6f50eca5845fa7e7a4e4a3fb` |
| [MIndexRegexPrecompute.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexPrecompute.java#L1-L111) | 111 / 5269 | `83684890535d4307a50131253507d76a2555e30e` | `7992224b9d14e1c0d87bdad53428f23564fa76f8de0677c5d2ffbc164417c76c` |
| [MIndexRegexPrecomputeKey.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexPrecomputeKey.java#L1-L41) | 41 / 1943 | `176161952504d95176099e2d23766912ec32ff56` | `58f723ca0c40563a54a0393b8e5d153b3b7a7df1a8353a9f0251425496753e9a` |
| [MIndexRegexProgram.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L1-L3760) | 3760 / 141730 | `98d9be66015639a8087c744d17f31c76d53c8ae5` | `9e3243cd150c965eeb8dc04baabb0f686d985c57b59bfa339cfa994d7eb608d7` |
| [MIndexRegexProgramImageCodec.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgramImageCodec.java#L1-L692) | 692 / 30937 | `9e9b33e4fec76458966ea26b5b190bc51d653161` | `3500c508b398f344938767325133dbd17a636580c144ae7fbd5933c17458b85a` |
| [MIndexRegexTrigramQuery.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexTrigramQuery.java#L1-L411) | 411 / 15093 | `4e1d24af8d3054a27ae3b6f2d8eebd6486225118` | `9c645f73e3c302d03371d2ccd6659de22b0fee27ab328ecfd1f1c7702a151b2e` |

## 2. Program member-to-contract map

“Components” includes the generated record accessors; the explicit defensive
array accessors are listed separately. Generated record `equals`, `hashCode` and
`toString` follow record behavior, not a custom deep-array identity. Public
Program getters expose the stored values; private helpers below explain how
those values are constructed and consumed.

| Source range | Members | Contract route |
| --- | --- | --- |
| [17–102](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L17-L102) | `MIndexRegexProgram`, `Semantics.RE2/JDK`, opcode/table/planner limits, stored state | Reference §§1–3: boolean subset, optional optimizations and ownership |
| [110–215](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L110-L215) | `Image` components and compact constructor: expression, semantics, op/out1/out2/classId, asciiLow/asciiHigh, rangeOffset/rangeCount/rangeLow/rangeHigh, startState/matchState, minLength/maxLength/captureCount, requiredPresence64/requiredCharacterFlags/mayConsumeSurrogateUnit | §4: lane copies, per-opcode/coordinate checks and semantic-validation limits |
| [217–232](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L217-L232) | `Image.op/out1/out2/classId/asciiLow/asciiHigh/rangeOffset/rangeCount/rangeLow/rangeHigh`, `rootHash` | §4: cloned exports and portable root |
| [241–346](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L241-L346) | `ExecutionImage` components/constructor: program, asciiStateMasks, epsilonWordsPerRow/epsilonRowByState/epsilonTerminals, asciiSubsetBits/asciiSubsetSourceCount/asciiSubsetTransitions, bmpClassWordCount/bmpPageByHighByte/bmpClassMasks, anchoredAsciiDfa/searchAsciiDfa | §4: optional table geometry; no rederivation of table truth |
| [348–357](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L348-L357) | `ExecutionImage` six defensive array accessors; `rootHash` | §4: detached exports and execution identity |
| [465–496](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L465-L496) | `AsciiDfaImage` constructor/components, transitions/accepting accessors, stateCount, primitivePayloadBytes | §4: dense geometry, optional snapshots and primitive-only accounting |
| [648–685](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L648-L685) | Both `compile` overloads and both `tryCompile` overloads | §2: RE2 default, explicit semantics/budget, null refusal versus propagated exception |
| [800–802](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L800-L802) | `CompilationTimings` and all five generated component accessors | §2: CPU phases, unavailable -1 and completion flag |
| [810–842](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L810-L842) | `CompilationSession.Policy`, `Limits` constructor/components, `Statistics` components; reserved arena/index fields | §2: LRU/MRU, fixed capacities and retained metadata |
| [844–875](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L844-L875) | Both `CompilationSession` constructors; both session `tryCompile` overloads | §2: optional measurement, synchronized lifecycle, admit flag, exact hits |
| [876–897](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L876-L897) | `accountedBytes`, `statistics`, `lastTimings`, `clear`, `close` | §2: accounting exclusions, reset/closed distinction, busy refusal |
| [1015–1038](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L1015-L1038) | `image` | §4: portable snapshot with cloned lanes |
| [1040–1054](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L1040-L1054) | `PrecomputedImage` constructor/components; `precomputedImage` | §4: dispatch facts and bounded count, no custom root method |
| [1056–1085](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L1056-L1085) | `fromPrecomputedImage`, `executionImage`, public `fromExecutionImage` | §4: distinction between table restore and complete dispatch restore |
| [1150–1244](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L1150-L1244) | Both `fromImage` overloads | §4: no parser; cancellable derived-table rebuild |
| [1246–1264](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L1246-L1264) | `expression`, `semantics`, `stateCount`, `characterClassCount`, `captureCount` | §§2–3: immutable expression/mode and metadata, no capture results |
| [1266–1296](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L1266-L1296) | `trigramQuery` | §7: lazy synchronized publication, reparsing, unsupported fail-open ALL |
| [1298–1364](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L1298-L1364) | `requiredPresence64`, `requiredCharacterFlags`, `mayConsumeSurrogateUnit`, `firstAsciiLow`, `firstAsciiHigh`, `branchingStateCount`, `structurallyDeterministic`, `nullable`, `firstAsciiAllows`, `minLength`, `maxLength` | §3: necessary conditions, length units and conservative dispatch |
| [1366–1384](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L1366-L1384) | `primitivePayloadBytes` | §3: counts named primitive arrays, not retained heap/process memory |
| [1386–1422](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L1386-L1422) | `hasAsciiStateMasks`, `asciiStateMaskPayloadBytes`, `hasAsciiSubsetTransitions`, `asciiSubsetBits`, `asciiSubsetTransitionPayloadBytes`, `hasBmpClassMasks`, `bmpClassMaskPayloadBytes` | §§2–3: optional table availability and size diagnostics |
| [1424–1463](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L1424-L1463) | `hasPrecomputedEpsilonClosures`, `epsilonClosurePayloadBytes`, `hasAsciiDfa`, `asciiDfaStateCount`, `asciiDfaPayloadBytes`, `anchoredAsciiDfaImage`, `searchAsciiDfaImage` | §§3–4: optional projections and defensive snapshots |
| [1465–1496](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L1465-L1496) | `supportsInput`; support helper `supportsMetrics` | §3: surrogate-sensitive admission and trusted metadata sources |
| [1498–1617](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L1498-L1617) | `matches`, `lookingAt`, `find`; scratch overloads, fast and NFA paths | §3: whole/prefix/any-position boolean truth, refusal before fast paths |
| [1619–1628](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L1619-L1628) | `workspace` | §3: caller-owned reuse |
| [2525–2554](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L2525-L2554) | `Workspace.program`, `matches`, `lookingAt`, `find`, `primitivePayloadBytes`; private constructor | §3: one mutable scratch owner per worker, no input retention |

### Program internal support boundaries

All private/nested implementation regions were read; these are not additional
public APIs or separate canonical owners.

| Source range | Support members | Purpose |
| --- | --- | --- |
| [360–462](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L360-L462) | `imageRoot`, `executionImageRoot`, `updateDfaDigest`, `updateUtf8/Bytes/Shorts/Ints/Longs/Chars/Int/Long` | Domain-separated, ordered root construction |
| [498–646](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L498-L646) | Both Program constructors, `firstAsciiMask`, `countOpcode` | Owned lanes and derived-versus-precomputed dispatch |
| [688–798](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L688-L798) | `compileInternal` | Phase order, snapshots, refusal catch, timing/finally |
| [899–998](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L899-L998) | Session `expressionHash`, `parse`, `evict`, `internClass`, `internNode`, `restore`, `restoreNode` | Collision confirmation, append-only IDs, partial retention and exact remap |
| [999–1013](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L999-L1013) | `ParsedSyntax`, `SharingLimit`, `PhaseCpu` constructor/now/elapsed/parsed/lowered/tabled/finish | Cold transient syntax and timing/refusal support |
| [1087–1148](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L1087-L1148) | Private `fromExecutionImage` overload | Copy tables and optionally reuse dispatch facts |
| [1630–1707](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L1630-L1707) | `asciiKnowledge`, `requiredFactsPresent`, `characterFlags`, `requireSupportedInput` | Metadata trust, unknown/fail-open facts and explicit unsupported-input throw |
| [1709–1855](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L1709-L1855) | `step`, `addClosure`, `pushClosureState`, `isAsciiWordBoundary`, `isAsciiWord`, `stepAsciiSubsetTable`, `classMatches`, `set` | Bitset stepping, absolute assertions, ASCII boundary math and class lookup |
| [1857–1964](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L1857-L1964) | `buildAsciiStateMasks`, `buildBmpClassMasks`, `BmpClassTable/NONE` | Masks, page geometry, assignment/byte bounds and cancellation |
| [1966–2064](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L1966-L2064) | `buildAsciiSubsetTransitions`, `tryBuildAsciiSubsetTransitions`, `AsciiSubsetTable/NONE` | Eight/four-bit choice, exact geometry and absence |
| [2066–2416](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L2066-L2416) | `AsciiDfaTable`: constructor/fromImage/tryBuild; boundedStateLimit/fitsBuildBudget/hashCapacity; subset lookup/insertion/hash/mix/equality/contains/closure helpers; matches/lookingAt/find and known-ASCII variants; stateCount/payloadBytes/image/empty | Bounded determinization, restart semantics, no native execution |
| [2418–2523](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L2418-L2523) | `EpsilonClosureTable`: constructor/tryBuild/pushStatic, both orInto forms, payloadBytes | Assertion refusal, sparse closure rows and fallback |
| [2556–2593](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L2556-L2593) | `Scratch`: constructor/reset/beginClosure/contains/isEmpty | Mutable work lanes and stamp wraparound |
| [2595–2724](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L2595-L2724) | `trigramQuery`, `trigramConcatQuery`, `trigramExact`, `trigramConcatExact`, `trigramAlternateExact`, `trigramRepeatExact`, `trigramCross` | Bounded exact language and adjacent finite-run necessary conditions |
| [2726–2842](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L2726-L2842) | `Node`; Empty/Atom/Quoted/Start/End/WordBoundary/Concat/Alt/Repeat node records and metadata methods/constructors | Parser algebra and quoted-tail binding marker |
| [2844–2938](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L2844-L2938) | `mayConsumeSurrogateUnit`, `anyMayConsumeSurrogate`, concat/alt requiredPresence/Flags, sumMin/sumMax/altMin/altMax/multiplyMin/multiplyMax | Necessary metadata, surrogate flags and saturated length arithmetic |
| [2940–3271](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L2940-L3271) | `Parser`: constructor/parse/parseAlternation/parseConcatenation/parseRepeated/parseBounds/parseDecimal/parseAtom/atom/parseGroup/parseEscape/parseQuotedLiteral/parseHex/parseClass/tryParsePosixClass/parseClassPiece/peek | Accepted subset and syntax/feature/state refusals |
| [3273–3300](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L3273-L3300) | `ClassPiece` single/spec/apply; `ClassSpec` constructor/apply/key | Character-class intermediate representation |
| [3302–3593](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L3302-L3593) | `ClassTableBuilder`: literal/dot/digits/word/space/specDigits/specWord/specSpace/specPosix/fromAllowed/smallExactStrings/canConsumeSurrogateUnit/requiredPresence64/requiredFlags/allWithin/allWord/allSpace/rangeWithin/maybeNegate/add/freeze/compress | Exact class lanes, semantic mode differences and conservative facts |
| [3595–3760](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgram.java#L3595-L3760) | `ClassTable`; `Compiler` constructor/compile/epsilon/atom/assertion/concat/alternate/repeat/concatenate/add/patch/ops/out1/out2/classIds; `State`, `Fragment`, `Patch`; unsupported/UnsupportedRegex | Thompson lowering, patch ownership, maxStates refusal and frozen primitive arrays |

## 3. Other six owners: complete member map

| Owner and range | Members | Contract route |
| --- | --- | --- |
| [MIndexCompactAsciiDfaImage.java 19–65](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexCompactAsciiDfaImage.java#L19-L65) | Record components/constructor: startState, asciiClassByValue, classCount, transitions, accepting | §4: copies and compact geometry |
| [MIndexCompactAsciiDfaImage.java 67–115](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexCompactAsciiDfaImage.java#L67-L115) | asciiClassByValue/transitions/accepting, stateCount, primitivePayloadBytes, nextState, rootHash | §4: defensive exports, checked lookup and compact root |
| [MIndexCompactAsciiDfaImage.java 117–199](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexCompactAsciiDfaImage.java#L117-L199) | from, requireEquivalent; private sameColumn | §4: hash-routed exact column grouping and dense equivalence |
| [MIndexRegexAcceleratedImage.java 18–47](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexAcceleratedImage.java#L18-L47) | Record constructor/components; from, restoreProgram | §4: optional paired projections and execution-image restoration |
| [MIndexRegexAcceleratedImage.java 49–80](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexAcceleratedImage.java#L49-L80) | rootHash; private validatePair/update | §4: pair validation and framed root identity |
| [MIndexRegexPrecompute.java 15–46](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexPrecompute.java#L15-L46) | Private constructor; Request constructor/components; Compiler.compile; Result constructor/components | §6: exact revisions, null unsupported and matching Optional presence |
| [MIndexRegexPrecompute.java 48–58](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexPrecompute.java#L48-L58) | key, default restoreOrCompile | §6: request identity and default compiler |
| [MIndexRegexPrecompute.java 60–111](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexPrecompute.java#L60-L111) | Custom restoreOrCompile; requireCompatible | §6: hit/miss ordering, error boundaries and publication |
| [MIndexRegexPrecomputeKey.java 11–41](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexPrecomputeKey.java#L11-L41) | Package-private class/constructor, PROGRAM_OWNER/CODEC_OWNER, key, revision | §6: historical literals, runtime/expression hash, ASCII revision validation |
| [MIndexRegexTrigramQuery.java 17–116](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexTrigramQuery.java#L17-L116) | Op; private constructor; package-private all/none/fromExact/and/or; private combine | §7: algebra, sorted direct terms and immutable children |
| [MIndexRegexTrigramQuery.java 118–170](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexTrigramQuery.java#L118-L170) | op, trigrams, subqueries, hasConstraints, termCount, test; package-private trigramsForReadOnlyKernel/subqueriesForReadOnlyKernel | §7: defensive public access, trusted kernel access and callback order |
| [MIndexRegexTrigramQuery.java 172–236](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexTrigramQuery.java#L172-L236) | Both precompute overloads; private scanFacts; testPrecomputed | §7: null/range/budget ordering, one-read scan and isolated range scope |
| [MIndexRegexTrigramQuery.java 238–321](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexTrigramQuery.java#L238-L321) | Facts constructor, utf16Length, keyCount, keys, test, concat; private first/seamKeys | §7: retained boundary metadata, merge/seam correctness and ownership |
| [MIndexRegexTrigramQuery.java 323–386](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexTrigramQuery.java#L323-L386) | requireFactBudget, compactFactKeys, mergeFactKeys; public trigram; package-private uniqueTrigrams | §7: bounded exact code-unit membership |
| [MIndexRegexTrigramQuery.java 388–411](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexTrigramQuery.java#L388-L411) | toString; private render/quote | §7: diagnostics only |

### Codec overloads and all support helpers

| Source range | Members | Contract route |
| --- | --- | --- |
| [30–48](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgramImageCodec.java#L30-L48) | Constants and private constructor | §5: seven versions, magic/header and hard codec bounds |
| [50–105](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgramImageCodec.java#L50-L105) | encode(Image) | §5: portable lanes and v1/v4 metadata |
| [111–140](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgramImageCodec.java#L111-L140) | encode(ExecutionImage) | §5: v2/v5 with nested base |
| [146–164](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgramImageCodec.java#L146-L164) | encode(AcceleratedImage) | §5: v3/v6 with dense/compact pairing |
| [167–183](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgramImageCodec.java#L167-L183) | encode(PrecomputedImage) | §5: v7 nested execution and dispatch facts |
| [185–206](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgramImageCodec.java#L185-L206) | decodePrecomputed(byte[]), decodePrecomputed(ByteBuffer) | §5: array clone versus private buffer views; v7 framing |
| [208–222](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgramImageCodec.java#L208-L222) | decode(byte[]), decode(ByteBuffer) | §5: portable v1/v4 |
| [224–246](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgramImageCodec.java#L224-L246) | decodeAccelerated(byte[]), decodeAccelerated(ByteBuffer) | §5: v3/v6, exact nested version and compact validation |
| [248–258](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgramImageCodec.java#L248-L258) | decodeExecution(byte[]), decodeExecution(ByteBuffer) | §5: v2/v5 |
| [260–273](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgramImageCodec.java#L260-L273) | write(Path, Image/AcceleratedImage/ExecutionImage) | §5: encode-before-create-only I/O; no v7 overload |
| [275–299](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgramImageCodec.java#L275-L299) | read, readMapped | §5: portable file size/transport and lifetime |
| [301–321](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgramImageCodec.java#L301-L321) | readAccelerated, readAcceleratedMapped | §5: accelerated file transport |
| [323–342](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgramImageCodec.java#L323-L342) | readExecution, readExecutionMapped | §5: execution file transport |
| [344–429](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgramImageCodec.java#L344-L429) | decodeExecutionPayload, decodePayload | §5: nested bounded slices, counted lanes and trailing-data refusal |
| [431–459](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgramImageCodec.java#L431-L459) | requiresUtf16, readExpression | §5: unpaired-surrogate version choice and exact text decoding |
| [461–510](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgramImageCodec.java#L461-L510) | imageVersion, requireNestedVersion, envelope, payload | §5: validation order, framing and checksum |
| [512–537](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgramImageCodec.java#L512-L537) | writeCountedInts/Longs, writeCompactDfa | §5: deterministic counted lanes/presence |
| [539–570](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgramImageCodec.java#L539-L570) | readCompactDfa | §5: presence/count/remaining guards before constructor validation |
| [572–605](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgramImageCodec.java#L572-L605) | writeDfa, writeInts/Longs/Chars, readCountedInts/Longs | §5: primitive lane widths |
| [607–649](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgramImageCodec.java#L607-L649) | readDfa, readInts/Longs/Chars | §5: dense validation, byte arithmetic and lane allocation |
| [651–692](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexProgramImageCodec.java#L651-L692) | boundedCount, requireCount, getInt, getLong, requireRemaining, both sha256 overloads | §5: bounded scalar reads, malformed input and digest handling |

## 4. Bounded dependency/caller review, separate from primary scope

The following public bodies/ranges were read to trace calls made by the seven
owners. This is **not a complete FS package review**. Generic typed precompute,
The [generic typed precompute reference](retained-precompute-reference.md) covers
the eight typed-plan owners. FS bundle/catalog pipelines, native-content implementations and unrelated
siblings retain separate review/qualification work. No replacement dependency
or runtime wiring is introduced.

| Dependency range | Reason read |
| --- | --- |
| [fs/M3ContentId.java 1–71](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/fs/M3ContentId.java#L1-L71) | Expression-hash input parsing, SHA-256 content identity and defensive byte exports |
| [fs/M3ObjectRef.java 1–13](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/fs/M3ObjectRef.java#L1-L13) | Immutable id/size/kind tuple; nonnegative size |
| [fs/M3ObjectKind.java 1–15](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/fs/M3ObjectKind.java#L1-L15) | PRECOMPUTE_OUTPUT and PRECOMPUTE_DESCRIPTOR role distinction |
| [fs/M3PrecomputeKey.java 19–168](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/fs/M3PrecomputeKey.java#L19-L168) | Canonical request encoding, ordered inputs, sorted configuration, defensive bytes; decode validation |
| [fs/M3PrecomputeResult.java 12–102](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/fs/M3PrecomputeResult.java#L12-L102) | VALUE/EMPTY invariant, descriptor bytes and decode guards |
| [fs/M3PrecomputeCache.java 16–46](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/fs/M3PrecomputeCache.java#L16-L46) | Constructor directory effect and lookup/key-descriptor verification |
| [fs/M3PrecomputeCache.java 220–280](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/fs/M3PrecomputeCache.java#L220-L280) | Descriptor put, lock/pointer publication, conflicting-result refusal and temporary cleanup |
| [fs/M3DirectoryContentStore.java 485–590](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/fs/M3DirectoryContentStore.java#L485-L590) | Buffered output put, size/digest read checks and openMapped handoff |
| [fs/M3DirectoryContentStore.java 727–779](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/fs/M3DirectoryContentStore.java#L727-L779) | Create-only content publication, exact-existing comparison and cleanup boundaries |
| [fs/M3MappedObject.java 19–93](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/fs/M3MappedObject.java#L19-L93) | Per-mapping admission, integrity, read-only view and nondeterministic unmapping lifetime |
| [precompute/MIndexPrecomputePasses.java 49–59](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/precompute/MIndexPrecomputePasses.java#L49-L59) | Length-prefixed exact UTF-16 hashing used by the regex request key |
| [precompute/MIndexPrecomputeScope.java 70–72](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/precompute/MIndexPrecomputeScope.java#L70-L72) | Big-endian long length framing used by sha256Utf16 |

Exact current dependency file identities (a hash identifies bytes, not full-body coverage):

| Dependency | Bytes | Git blob SHA-1 | SHA-256 |
| --- | --- | --- | --- |
| [M3ContentId.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/fs/M3ContentId.java) | 2243 | `275ed0c9b6aecbd002477c5817a11f7e7b60d3d8` | `7ae2451f6f2e94426ef55e12178dcde5954fba5ea66478003a12bca6e8a5631a` |
| [M3ObjectRef.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/fs/M3ObjectRef.java) | 452 | `cc153fcef6ef243e843845fa2c4d1583856eee97` | `15653345ffb0c2c2ad8e7dd61d4d1ffe27bbc676533761ce24d2940bb85ec009` |
| [M3ObjectKind.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/fs/M3ObjectKind.java) | 379 | `8403936d5acb0455105fe999b470228742296f86` | `e87f532f00cd053cc04ade758d9d2a899aabfbc49b8f324bb8c18cf14be0c061` |
| [M3PrecomputeKey.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/fs/M3PrecomputeKey.java) | 6630 | `13ced6c0bee4ad15cd90bdff062029b29ca73dc0` | `64109aa32ece8ac187458d4a4123746d19df88e202f5e7fbceaef932e1edfe9c` |
| [M3PrecomputeResult.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/fs/M3PrecomputeResult.java) | 3747 | `1edee18ccf36a35c10246ca43d2db7b984509fd9` | `f7319e23c764beb464ce2486df3fb91a21b8613dae893ce7f236d0a73d888fd1` |
| [M3PrecomputeCache.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/fs/M3PrecomputeCache.java) | 11298 | `a1ec2bb81b52b68835ef2d9b7191641174e00a08` | `e370a35fc135c60ca6c12867a2d2175a1985beeda97262fc007a690cefe047a2` |
| [M3DirectoryContentStore.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/fs/M3DirectoryContentStore.java) | 27555 | `562143cbeec01b10000e48f3a6b17ec8740270eb` | `e89deb7382d47f02bf08ccb509bc5160b93f9fbdecd76650495069ba5c3e621b` |
| [M3MappedObject.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/fs/M3MappedObject.java) | 5840 | `a2885985831963634e252a4f405bc3b5fcacd2f7` | `33faca17d593a7548c64dea9065969d325f33be366d16f563176e40f2a2ff8dc` |
| [MIndexPrecomputePasses.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/precompute/MIndexPrecomputePasses.java) | 3341 | `0d7c0e9cd26d2628491794aad4dda99d8c6a1071` | `155cfaba7a28284c7aa9f638beccd924d120b556ac45e6ea26d032065dfcaca8` |
| [MIndexPrecomputeScope.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/precompute/MIndexPrecomputeScope.java) | 2684 | `5edd2c8e1187d8c7ab3be16c9ece8ff465e54973` | `3ac799d0754f82e239489ac76cf623de3a2d82163a27c4d652e61a371600aefa` |

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
| [MIndexRegexProgramTest.java.txt](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt) | 39322 | `13ec38e57bd6d4f8317566821f8d6e2dad153961` | `5c185acb76ef6de2a79eebec30a6b156fa982c7283cb0f4d0ef96840ce1a9dd6` |
| [MIndexRegexTrigramQueryTest.java.txt](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexTrigramQueryTest.java.txt) | 11007 | `601a424ef6f477e675c64cdaa0643b0247c5476a` | `bae0950fd49e10eaada3d8e9e61a9b99dd251357f3952bbd867e134f77ef2225` |

### MIndexRegexProgramTest.java.txt

| Method / source range | Evidence scope |
| --- | --- |
| [compilationHonorsProgressCancellation](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L23-L31) | Historical assertions read; NOT_RUN |
| [imageRestoreHonorsProgressCancellation](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L34-L44) | Historical assertions read; NOT_RUN |
| [primitiveProgramMatchesRegularSubsetWithPrimitiveState](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L47-L58) | Historical assertions read; NOT_RUN |
| [precomputedBitKernelsPreserveBooleanSemantics](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L61-L83) | Historical assertions read; NOT_RUN |
| [boundedAsciiDfaPreservesAllBooleanOperationsAndFallsBackForBmp](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L86-L109) | Historical assertions read; NOT_RUN |
| [regexImageRoundTripRebuildsDfaSubsetAndBmpAccelerators](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L112-L129) | Historical assertions read; NOT_RUN |
| [frozenAndMappedAsciiFactsDriveCompactDfaWithoutChangingTruth](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L132-L174) | Historical assertions read; NOT_RUN |
| [bitKernelPrecomputationIsSparseBoundedAndAssertionsUseDynamicClosure](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L177-L210) | Historical assertions read; NOT_RUN |
| [jdkSubsetIsDifferentiallyEquivalentForBooleanOperations](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L213-L254) | Historical assertions read; NOT_RUN |
| [acceptedSubsetIsDifferentiallyEquivalentAcrossGeneratedCorpus](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L257-L305) | Historical assertions read; NOT_RUN |
| [astPresenceProofIsConservativeAcrossRegexAlgebra](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L308-L339) | Historical assertions read; NOT_RUN |
| [requiredCharacterClassFactsAreConservativeAcrossRegexAlgebra](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L342-L368) | Historical assertions read; NOT_RUN |
| [exactResolverClassFactsRejectBeforeCharacterReads](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L371-L414) | Historical assertions read; NOT_RUN |
| [resolverFamiliesExposeConsistentPrecomputedRegexFacts](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L417-L459) | Historical assertions read; NOT_RUN |
| [reusableWorkspaceDoesNotLeakStateAcrossInputs](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L462-L476) | Historical assertions read; NOT_RUN |
| [bmpCharacterClassesUsePagedDirectAddressMasks](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L479-L517) | Historical assertions read; NOT_RUN |
| [asciiSubsetTransitionsUseDirectAddressRamTableWithoutChangingTruth](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L520-L581) | Historical assertions read; NOT_RUN |
| [astPresenceAlgebraCoversGroupedMandatoryText](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L584-L607) | Historical assertions read; NOT_RUN |
| [plannerProvenLiteralsProduceSafePrecomputedPresenceGate](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L610-L628) | Historical assertions read; NOT_RUN |
| [lexiconPresenceGateRejectsBeforeCharacterReads](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L631-L679) | Historical assertions read; NOT_RUN |
| [re2PosixClassesLowerToPrimitiveAsciiRanges](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L682-L736) | Historical assertions read; NOT_RUN |
| [whitespaceClassesPreserveRe2AndJdkVerticalTabDifference](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L739-L781) | Historical assertions read; NOT_RUN |
| [re2WordBoundariesUseNativeAsciiBoundaryMath](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L784-L833) | Historical assertions read; NOT_RUN |
| [lazyQuantifiersAndQuotedLiteralsStayNativeForBooleanTruth](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L836-L914) | Historical assertions read; NOT_RUN |
| [unsupportedOrSemanticallyDifferentSyntaxDelegatesInsteadOfApproximating](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L917-L933) | Historical assertions read; NOT_RUN |
| [supplementaryInputDelegatesOnlyWhenAnAtomCanConsumeSurrogateUnits](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L936-L990) | Historical assertions read; NOT_RUN |
| [hybridUsesNativeBackendForNoCaptureSubsetAndRe2ForCaptures](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L993-L1005) | Historical assertions read; NOT_RUN |
| [mindexStringMatchesPreservesJdkSemanticsWithoutRequiringStringInputCopy](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt#L1008-L1023) | Historical assertions read; NOT_RUN |

### MIndexRegexTrigramQueryTest.java.txt

| Method / source range | Evidence scope |
| --- | --- |
| [extractsConservativeBooleanTrigramStructureFromNativeRegexAst](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexTrigramQueryTest.java.txt#L17-L27) | Historical assertions read; NOT_RUN |
| [exactAlternativesAndSmallClassesProduceUsefulQueries](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexTrigramQueryTest.java.txt#L30-L48) | Historical assertions read; NOT_RUN |
| [queryNeverRejectsARealFindForSupportedCorpus](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexTrigramQueryTest.java.txt#L51-L103) | Historical assertions read; NOT_RUN |
| [finiteDomainPlanPrunesDistinctSpellingsBeforeAuthoritativeVerification](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexTrigramQueryTest.java.txt#L106-L131) | Historical assertions read; NOT_RUN |
| [forcedNativeSparseAndPreservesHybridRegexResults](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexTrigramQueryTest.java.txt#L134-L169) | Historical assertions read; NOT_RUN |
| [adjacentFiniteRunsRetainTheQuotedCandidateStructure](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexTrigramQueryTest.java.txt#L172-L196) | Historical assertions read; NOT_RUN |
| [finiteRunBudgetAndEmptyBranchesNeverRejectAnAuthoritativeFind](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexTrigramQueryTest.java.txt#L199-L245) | Historical assertions read; NOT_RUN |
| [finiteRunRecoveryKeepsExistingModeAndSurrogateRefusals](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexTrigramQueryTest.java.txt#L248-L263) | Historical assertions read; NOT_RUN |

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
| Original v7 | Donor 80d288951148548ef6aa4c95df9cb31a438aac16; six root owners among 22 nonlicense payloads, plus LICENSE/NOTICE. Manifest root d1889319f72f5e3ab9fb4804e27e8ec58d2bfc75e112539f5322f5f95e3d658b | [export.tsv](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/export.tsv) |
| Original custody | Program 9999e8afe1ec44de58edd644c0ca278415fb7a379f6cdacfe9dbf7011ed77131; codec 09f4ef3d52a432b5a46bd784a91728352d121241dca9ab010df7a06f38d01dc7; closure/runtime flags false | [SOURCE_CUSTODY.json](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/SOURCE_CUSTODY.json) |
| Receipt binding | PROVENANCE binds export raw SHA-256 002c9dd9dd192bb1b23a31e0546a5bcf0165a109afb8c2c20c5155e8180d18c3 and SOURCE_CUSTODY SHA-256 d77b8036dd8cbbae3a3a852071fd0050c8c623b710f45d5e01188c92ae9bf918 | [PROVENANCE.json](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/PROVENANCE.json) |
| Program successor | 9999e8afe1ec44de58edd644c0ca278415fb7a379f6cdacfe9dbf7011ed77131 → 9e3243cd150c965eeb8dc04baabb0f686d985c57b59bfa339cfa994d7eb608d7 | [manifest.tsv](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/recipe/templates/manifest.tsv) |
| Query addition | ABSENT → 9c645f73e3c302d03371d2ccd6659de22b0fee27ab328ecfd1f1c7702a151b2e | [manifest.tsv](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/recipe/templates/manifest.tsv) |
| Successor execution attribution | Actual authority distinguishes later receiver execution from the frozen metadata review; separate donor production/test qualification is reported as 40 selected methods, not current-head execution here | [CURRENT_ACTUAL_AUTHORITY.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/CURRENT_ACTUAL_AUTHORITY.md) |
| Codec copy recovery | 09f4ef3d52a432b5a46bd784a91728352d121241dca9ab010df7a06f38d01dc7 → c7f17a7b60b8e90c5bc8730c6a5c222dd3598474ea126377ecdf89cb0bcc6d8e; recipe commit 970b2a40b4d6df391027f1bcd59bb6362d62276b; NOT_ADMITTED | [regex-copy-recovery-20261011.json](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/qualification/regex-copy-recovery-20261011.json) |
| Codec nested-slice successor | c7f17a7b60b8e90c5bc8730c6a5c222dd3598474ea126377ecdf89cb0bcc6d8e → 3500c508b398f344938767325133dbd17a636580c144ae7fbd5933c17458b85a; recipe commit c41b6adb6b64f6f0432a0599e09a6c6dae99c93e; NOT_ADMITTED | [regex-nested-slice-20261011.json](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/qualification/regex-nested-slice-20261011.json) |
| Current request-key contract | PROGRAM_OWNER/CODEC_OWNER still embed original hashes; Request requires independently accurate compilerRevision/codecRevision covering behavior-bearing dependencies | [MIndexRegexPrecomputeKey.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexPrecomputeKey.java#L13-L39); [MIndexRegexPrecompute.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/m3/m3-indexstring/src/main/java/jdk/internal/m3/indexstring/MIndexRegexPrecompute.java#L18-L27) |

The historical `PROVENANCE.json` field named `README.md` records the original
packet input; do not equate that recorded historical README hash to later edited
intake README bytes. The current documentation cites the export/custody bindings
and the separately identified current authority/qualification records.

Exact public evidence-file bytes inspected for this reference:

| Evidence at documentation pin | Bytes | Git blob SHA-1 | SHA-256 |
| --- | --- | --- | --- |
| [export.tsv](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/export.tsv) | 8547 | `0e1d2ba77403ebf7ba7a851b84493dff8b1a7cac` | `002c9dd9dd192bb1b23a31e0546a5bcf0165a109afb8c2c20c5155e8180d18c3` |
| [PROVENANCE.json](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/PROVENANCE.json) | 1518 | `15a03b8141055d87856d54b5154974945667f9cb` | `cdf7b0cb64afdded26994e999a03da8eb6bf6b8f3b9193ae930a15ceefe79ee6` |
| [SOURCE_CUSTODY.json](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/SOURCE_CUSTODY.json) | 29478 | `5f5e88dabb017ec2b0307a5e40e8c907f3f51311` | `d77b8036dd8cbbae3a3a852071fd0050c8c623b710f45d5e01188c92ae9bf918` |
| [README.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/README.md) | 1929 | `703e4bad34a6a474e4fbfa399c0590db656546ff` | `b31fe2f51acb0c591860fe9ebd41da95c9696a21751b6e9a2fb38ba2a0dd3f58` |
| [QUALIFICATION.json](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/evidence/QUALIFICATION.json) | 2048 | `9b6337b9d925bd085898f13a26f9b3dca47733ba` | `2ffe18cb3a68aa9a9a0e24fbd86e1ee42cdbf053d0c3277111bd0d423d07cb08` |
| [manifest.tsv](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/recipe/templates/manifest.tsv) | 620 | `1d22a7f21c48e299b1d2f613c3e0299494189887` | `d1b8e074f294b04376b27a41a1a9ab2b4e5a94ebc280668cb526248ef9780025` |
| [CURRENT_ACTUAL_AUTHORITY.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/CURRENT_ACTUAL_AUTHORITY.md) | 3888 | `ce1d14b8d6e3690fff3d15273b70cd11559d59c7` | `c8137ea24e11862d5f1ad9e7f440e76550f846cfbd58a62b058c3e8d376e0fb6` |
| [regex-copy-recovery-20261011.json](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/qualification/regex-copy-recovery-20261011.json) | 4084 | `511eaa0fdb4aa159202c4a808963178390fe3b1b` | `c3911bba5a01a807b1eff132d5987ed043c34fb3f168547502be850b07a1ca4e` |
| [regex-nested-slice-20261011.json](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/qualification/regex-nested-slice-20261011.json) | 12278 | `6229911a6793c087580522f0a97fb9ef68318d80` | `2684b4b2ff17522e7be43c032fcbf85d3944b1bb31f43c336a314bd8d328f2ad` |

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
| [AGENTS.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/AGENTS.md) | 4554 | `f78685ca6efb5191e4e11b0bcfd137ddac7430b9` | `d9cc483da6c7330b30a454f6ce65868978167e302bcdb0805bf6d8a5da018621` |
| [name-mapping.json](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/name-mapping.json) | 399051 | `288f20575d3f9d106571990f21944e7f46b0cf71` | `f99b80ededfb898804547ad3baf87cdc86aaccda4331c076591e2459d4f09ecb` |
| [synexia-full-family-pin.tsv](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/compatibility/m3-full-family-pin.tsv) | 1171 | `3d24fc38677435453787ca67f3d5458dbfaf5b5f` | `5d2a89e133164bfc276ed834be47ad3d1bd2583e4bed08a7f94a7e1eb2b50bcc` |
| [synexia-full-family-catalogue.json](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/m3-full-family-catalogue.json) | 10564 | `8145c667c941a9358e9a5149489e455d950afc34` | `2e6fa85aeaaec5a6c37aa429b66d102ee9497735fdd5476ca4085d34667b5eef` |
| [README.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/current-full-borrow/README.md) | 4013 | `11fbf03808fe5d1999b38766eebd7d1944ace049` | `8a010b39ee657098078ddc8e9502a545b6628c9b23c0b3f63526423ba36a4b55` |
| [STRING_PHASE.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/current-full-borrow/STRING_PHASE.md) | 2931 | `1d6c3d266e3c34c336fb75c6c3c2a99d1fd47e35` | `1f02e854d90fb7cd2f29ca04dbfa29bd3be7a86248fd4ee12978cac28fbae676` |
| [pin.tsv](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/current-full-borrow/pin.tsv) | 1328 | `8bdaaaf8bacbaf190e537e82955915a720592c92` | `8cdf72c68a0075ad8b3bb0968369619d1c4c34bc675d785d19aec39717f495b4` |
| [promotion-phases.tsv](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/current-full-borrow/promotion-phases.tsv) | 1070 | `c4e8582f99caba32d718b2c1e86dae0854cdce3d` | `e4730068f321d551913a4cf7d40832f43b315c3973312c73b744ca5081758eb5` |
| [receiver-status.tsv](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/current-full-borrow/receiver-status.tsv) | 3721 | `895d91811075652f0328a9c1cf28489c68c09158` | `390785bf33ebe9e9b3d7a60f5a2e597fb110f72ab68b7d3e45bead245f33a5e1` |
| [string-phase-gates.tsv](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/m3-import/current-full-borrow/string-phase-gates.tsv) | 1869 | `8e28b7278d35b869d08e39bad37675b195809628` | `a984bca40b74a7da856780a4ed92b665591285394413631470746beaefa2ffdf` |

The root instruction and naming-map Git blobs were independently read at the
specified public commit. Relevant naming rows keep exact Java regex semantics
authoritative and precompute internal; the full-borrow inventory does not advance
String → arrays → collections → AST/compiler → remaining-family promotion.
Repository policy verifiers were not run in this documentation-only pass.

The existing routes in `packages/custody-01.md`, `indexed-string-reference.md`,
`runtime-owners.md` and `projects.md` were read to retain their bounded ownership
claims. This reference adds detail for these seven retained regex owners; it does
not replace the separate compatibility bridge or bootstrap/runtime guides.
`m3/pom.xml`, `m3/m3-import/pom.xml`, `m3/build.sh` and vendor custody establish
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
