<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# Retained AST specification source and evidence coverage

Companion to [language specifications, production plans and dispatch](retained-ast-specification-reference.md).

This is complete body/declaration routing for the eighteen assigned production
files at public M3JDK21 snapshot `363c02caa2e5af1df9460b927ecd78f6a9dbcefb`.
Every body, nested declaration, explicit public entry point, validation branch
and non-public support region was read. Scope is these physical files, not
whole-package dependency closure or semantic correctness. The
[37-file routing map](retained-ast-routing-map.md) keeps the nine ownership and
ten query owners separate. Their members/tests are not counted again here.

A read-only directory comparison on 2026-10-15 verified that all 37 AST production
Git blobs are unchanged at documentation base
`fb1e2f78ae4000f34adbed4a7fb91a69dd5ef8e0`. The fifteen shared policy/build/intake
bodies listed below were also byte-equal across the source and documentation pins.
No repository build, code, test, script, verifier, JNI, library load, benchmark or
example was executed. Runtime qualification is **NOT_RUN**, not PASS. Hosted CI is separate exact-commit
evidence and does not supply missing source-contract qualification.

## 1. Exact primary body identities

All paths below have the prefix
`m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/`.
Links address complete source bodies at the source pin. Git blob SHA-1 identifies
the Git object; raw SHA-256 hashes the exact retained file bytes. Neither proves
behavior or license compatibility with another destination.

| File | Lines / bytes | Git blob SHA-1 | Raw SHA-256 |
| --- | --- | --- | --- |
| [MIndexASTChildRole.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTChildRole.java#L1-L10) | 10 / 253 | `944f49e36060de38e466fd74a338bfe39f491af2` | `73f1e33242917c1c1e6f7b2aa9112f643bcebb9e203bd437ad89343644477cd8` |
| [MIndexASTDispatchNative.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTDispatchNative.java#L1-L478) | 478 / 15349 | `eb36fc138bb582dc9d6ccd9aac29bb71099a808b` | `fdcb748b1d5882c499a386be7e1e0e4aa4bd7e28e14876eeff0aa170bf7e0be1` |
| [MIndexASTProductionDispatch.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionDispatch.java#L1-L589) | 589 / 19376 | `d8942d23d396f882ef697b9f5bda1535a3ad0528` | `1f9308fe51809e91f8ee1426dc5ff309a7b8960933a1bcbc7e73a1688e540bfe` |
| [MIndexASTProductionPlans.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionPlans.java#L1-L769) | 769 / 27613 | `a9b4c112cdf58f6c8937519ad4536f5b004310f4` | `ced8f9f5d4bdfabb226f82dad65cc386566f4b347f27f1ea59921a2e7a743c91` |
| [MIndexASTSpecImage.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecImage.java#L1-L798) | 798 / 31206 | `3837f39dd9789cf8cac0d671af1240cc5b798cc9` | `6fafd946cc5e724aa16accaa4edc7d7b97134a58e75754da103c2b26f50757e1` |
| [MIndexASTSpecImageView.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecImageView.java#L1-L197) | 197 / 7612 | `e91ae5467e0408e93de903e063ebe13cf30e12bd` | `f8d0b00fa63a3de45568e5194d577d1ee58887c1ba50e072a5cc55baf1f4c978` |
| [MIndexASTSpecPrecompute.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecPrecompute.java#L1-L586) | 586 / 21477 | `9afa3503ed5b406ba78e50ec64649a28d55d2dab` | `9a0d72ea590fa65bd221232bbe075e89e069ffa4469813cbea4dc9eb8c1a5589` |
| [MIndexASTSpecTreePrecompute.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecTreePrecompute.java#L1-L1024) | 1024 / 37941 | `44f78ed27e0bc2260ef0643ac073370102715083` | `20de972d1b22f1fb71a06d49a8fafd012008d5fdaa7b4aa93762f68806316562` |
| [MIndexASTSpecs.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecs.java#L1-L94) | 94 / 3772 | `d5a73ba53831512d6a4520880da3392099a1fadd` | `0b2c9fac3e7a9743efa5dd0deb5e90d54c2e3a2d8ebcb29b14384013bd21a0d0` |
| [MIndexAssociativity.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexAssociativity.java#L1-L9) | 9 / 209 | `9ae2a782b6576056735979861ee684b7178ba316` | `2198ef1962011cc4552579d1793a300040f62ab4d258a782077f384d2be3797a` |
| [MIndexFlagPolicy.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexFlagPolicy.java#L1-L10) | 10 / 243 | `d6a7c77126357108adc1845901d5306292ea8ef9` | `4ec5fc80f340cefb3e53201d725c9d9e2414bf76a56aa42408f8116746e5de28` |
| [MIndexJava21LanguageSpec.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexJava21LanguageSpec.java#L1-L527) | 527 / 16949 | `496b94fef7116f1124a16fca6ce8a72349201a07` | `0ca21831b3c312a2ceb0342442bd696d0b968c5fac6d72ac405006c3cc9df6d9` |
| [MIndexLabelPolicy.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexLabelPolicy.java#L1-L22) | 22 / 483 | `a03e165c591ef78988152a0c54c3a39ca17d7dea` | `581d346c63d37fb86a9e523c6a92846d46b9cd32cd0685dab9a81aa93c68ef50` |
| [MIndexLanguageRule.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexLanguageRule.java#L1-L42) | 42 / 1343 | `1b3914e46da4e1ad691b16275fda94294b6c7e17` | `e3cf4890992375d2d1cfa3405deff06ccbe57cf64d63c9bed64d286b1c7f6cb0` |
| [MIndexLanguageSpec.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexLanguageSpec.java#L1-L87) | 87 / 2833 | `1bbf42dee0a4986b822876b6072de499eea1e67c` | `14ab6a310cbc1671bdf523bdd8119fab6ea5854526b48aa67021ea5ce5839e35` |
| [MIndexLanguageSpecs.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexLanguageSpecs.java#L1-L16) | 16 / 589 | `804ed263f053e4bcf84aff554ce7ae93395d126f` | `fa721d76f0862277edac7f371a849fd253075ab6fbd19c3833d62cfd9cdb38fa` |
| [MIndexOperatorRule.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexOperatorRule.java#L1-L36) | 36 / 1154 | `1a53089c22da8ea3c70f0389afd22b307d740987` | `26b57626bc5e1d18228aaa004d7d652208c2a82b2165d5654a615510373c5a64` |
| [MIndexSyntaxCategory.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexSyntaxCategory.java#L1-L25) | 25 / 418 | `40954e8ff77c0c31ad6c7715b66f92b5cb59cb7e` | `9cd7885997097ff10d97d092178eee62ffda4f3eea99e52e896957fb06f1d87c` |

## 2. Title-to-owner and declared-member routing

Ranges include full method bodies, not just signatures. Overloads are explicit;
record component accessors/equality/hash/toString and enum values/valueOf inherited
or generated by Java are distinguished from explicitly declared methods. Public
modifiers on a member of a package-private class do not export that class.
No protected entry point is declared in these eighteen bodies.

### MIndexASTChildRole

Reference: 5. Production construction plans.

| Source range | Declarations / body responsibilities |
| --- | --- |
| [L5–L10](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTChildRole.java#L5-L10) | Enum and all constants: NON_TERMINAL, LITERAL, TOKEN_CLASS, OPERATOR_LITERAL |

### MIndexASTDispatchNative

Reference: 7. Batch outputs and the optional native boundary.

| Source range | Declarations / body responsibilities |
| --- | --- |
| [L14–L23](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTDispatchNative.java#L14-L23) | Package-private owner; ABI_VERSION, NATIVE_BATCH_THRESHOLD, volatile nativeBatchAvailable; private constructor |
| [L25–L97](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTDispatchNative.java#L25-L97) | batchHeap, batchHeapEof |
| [L99–L212](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTDispatchNative.java#L99-L212) | batchMapped, batchMappedEof |
| [L214–L233](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTDispatchNative.java#L214-L233) | nativeBatchAvailable() lazy cache/probe |
| [L235–L313](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTDispatchNative.java#L235-L313) | javaHeapDispatch, javaHeapEof, javaViewDispatch, javaViewEof |
| [L315–L378](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTDispatchNative.java#L315-L378) | writeResult, validateSymbols, requireBatchLengths, requireEofBatchLengths |
| [L380–L427](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTDispatchNative.java#L380-L427) | requireMappedDispatchLayout, requireMappedEofLayout, checkRange |
| [L429–L478](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTDispatchNative.java#L429-L478) | Native declarations: nativeAbiVersion, nativeHeapDispatch, nativeMappedDispatch, nativeHeapEofDispatch, nativeMappedEofDispatch |

### MIndexASTProductionDispatch

Reference: 6. Predictive dispatch, order and ambiguity.

| Source range | Declarations / body responsibilities |
| --- | --- |
| [L15–L59](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionDispatch.java#L15-L59) | MIN_HASH_CAPACITY; all immutable geometry arrays/counts/fingerprint; private constructor |
| [L61–L85](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionDispatch.java#L61-L85) | build(precompute), build(image); both anonymous Source adapters and all six overrides |
| [L87–L153](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionDispatch.java#L87-L153) | build(Source): kind validation, pair gathering, sorting, duplicate refusal, candidate and row offsets |
| [L155–L222](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionDispatch.java#L155-L222) | build(Source): ambiguity totals, hash insertion, EOF postings, fingerprint and constructor transfer |
| [L224–L262](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionDispatch.java#L224-L262) | fingerprint, entryCount, candidateCount(), uniqueEntryCount, ambiguousEntryCount, eofCandidateCount, dispatchEntryCount, predictedTerminals |
| [L264–L307](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionDispatch.java#L264-L307) | mayPredict, candidateCount(int,int), candidateProductions, uniqueProductionOrMinusOne, ambiguous, eofCandidateProductions, uniqueEofProductionOrMinusOne, eofAmbiguous |
| [L309–L338](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionDispatch.java#L309-L338) | batchDispatch, batchEofDispatch, primitivePayloadBytes |
| [L340–L383](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionDispatch.java#L340-L383) | Package access: keyAt, entryNonTerminal, entryTerminal, entryCandidateStart, entryCandidateEnd, candidateAtAbsolute, hashSlotCount, hashSlotKey, hashSlotEntry, eofOffset, eofCandidateAtAbsolute |
| [L385–L415](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionDispatch.java#L385-L415) | Package access: symbolCountForNative, slotKeysForNative, slotEntriesForNative, offsetsForNative, candidatesForNative, eofOffsetsForNative, eofCandidatesForNative, entryOffset |
| [L417–L468](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionDispatch.java#L417-L468) | entryStart, entryEnd, findEntry, checkSymbol, requireNonTerminal, requireTerminal, pack, encodeKey |
| [L470–L503](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionDispatch.java#L470-L503) | hashCapacityForEntries, prefixOffsets, lowerBound (declared helper, no caller in this body) |
| [L504–L579](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionDispatch.java#L504-L579) | sortPairs, compare, mixToInt, mix, avalanche |
| [L581–L588](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionDispatch.java#L581-L588) | Private Source interface: symbolCount, productionCount, symbolKind, productionLhs, predictionSymbols, productionCanEndAtEof |

### MIndexASTProductionPlans

Reference: 5. Production construction plans.

| Source range | Declarations / body responsibilities |
| --- | --- |
| [L16–L124](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionPlans.java#L16-L124) | RHS_* flags, TERMINAL_MASK, VALUE_MASK; all retained spec/grammar references, counts, RHS lanes, postings, bit matrices, fingerprints; private constructor |
| [L126–L225](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionPlans.java#L126-L225) | build: flattened RHS roles/flags/operator indexes, before/after minima and shape/plan hashes |
| [L227–L333](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionPlans.java#L227-L333) | build: operator/LHS/prediction postings, terminal ordinals, closure matrices, fingerprint |
| [L335–L364](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionPlans.java#L335-L364) | build: constructor transfer of fresh arrays |
| [L366–L396](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionPlans.java#L366-L396) | spec, grammar, fingerprint, productionCount, productionRhsLength, rhsSymbol, childRole |
| [L398–L444](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionPlans.java#L398-L444) | childNullable, nullablePrefixBefore, nullableSuffixAfter, minimumTokensBefore, minimumTokensAfter, directOperator, operatorIndex, terminalChildCount, nonTerminalChildCount, directOperatorCount, directOperatorOrdinals |
| [L446–L475](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionPlans.java#L446-L475) | shapeHash64, planHash64, productionsForLhs, productionsPredicting |
| [L477–L541](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionPlans.java#L477-L541) | derivesTerminal, derivableTerminals, productionDerivesTerminal, productionDerivableTerminals, derivesOperator, derivableOperators, productionDerivesOperator, productionDerivableOperators |
| [L543–L573](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionPlans.java#L543-L573) | syntaxKindCodeForProduction, syntaxKindCodeForTerminal, terminalSyntaxKindCode, terminalSymbolFromSyntaxKindCode, productionFromSyntaxKindCode |
| [L575–L622](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionPlans.java#L575-L622) | primitivePayloadBytes, flag, absolute, checkProduction, checkSymbol |
| [L624–L647](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionPlans.java#L624-L647) | role, offsets, setBit, bit |
| [L649–L727](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionPlans.java#L649-L727) | propagateDerivationClosure, rowHasAnyBit, orRow, orRows |
| [L729–L768](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionPlans.java#L729-L768) | symbolsFromBits, ordinalsFromBits, mix, avalanche |

### MIndexASTSpecImage

Reference: 9. Compiled spec image and serialization effects.

| Source range | Declarations / body responsibilities |
| --- | --- |
| [L23–L42](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecImage.java#L23-L42) | MAGIC, LEGACY_FORMAT, FORMAT, HEADER_BYTES, SHA256_BYTES, six SYMBOL_FLAG_* and four RHS_FLAG_* constants; private constructor |
| [L44–L127](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecImage.java#L44-L127) | compile, write; legacy refusal, rebuild/count/fingerprint checks, filesystem effects, digest and size checks |
| [L129–L201](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecImage.java#L129-L201) | contentFingerprintOf(view) and (view,boolean) |
| [L203–L252](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecImage.java#L203-L252) | counts, layout |
| [L254–L362](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecImage.java#L254-L362) | writeHeader, writeSymbolScalarLanes, writeSymbolPostingLanes, writeProductionScalarLanes, writeProductionPostingLanes |
| [L364–L431](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecImage.java#L364-L431) | writePlanLanes |
| [L433–L515](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecImage.java#L433-L515) | writeTerminalMatrix, writeOperatorMatrix, writeDispatchLanes |
| [L517–L562](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecImage.java#L517-L562) | terminalOrdinal, symbolFlags, rhsFlags, writeOffsets, writeValues |
| [L564–L595](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecImage.java#L564-L595) | mixArray, mixText, mix, avalanche, sha256 |
| [L597–L616](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecImage.java#L597-L616) | Private IntArrayLookup.values; package-private Counts record and fourteen scalar components |
| [L618–L681](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecImage.java#L618-L681) | Package-private Layout and every absolute section-base field plus arraysStart/counts/format/cursor |
| [L683–L762](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecImage.java#L683-L762) | Layout constructor: sequential byte/int/long allocation, conditional v2 dispatch planes |
| [L764–L797](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecImage.java#L764-L797) | Layout.payloadEnd, symbols, productions, sourceRules, takeBytes, takeInts, takeLongs |

### MIndexASTSpecImageView

Reference: 7 and 9. Batch contract and image view.

| Source range | Declarations / body responsibilities |
| --- | --- |
| [L15–L47](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecImageView.java#L15-L47) | DISPATCH_PRESENT, DISPATCH_AMBIGUOUS; identity, language, version; languageFingerprint, grammarFingerprint, astSpecFingerprint, productionPlanFingerprint, contentFingerprint; symbolCount, productionCount, operatorCount, sourceRuleCount; symbolKind, tokenClass, literalCase, nullable, productive, reachable, leftRecursive, canEndAfter, operatorTerminal, minimumTokens, symbolHash64, firstSymbols, followSymbols, productionsUsingSymbol, operatorIndexForSymbol |
| [L49–L72](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecImageView.java#L49-L72) | productionLhs, productionRhsLength, productionRhsSymbol, productionMinimumTokens, productionCanEndAtEof, productionHash64, predictionSymbols; childRole, childNullable, nullablePrefixBefore, nullableSuffixAfter, minimumTokensBefore, minimumTokensAfter, directOperator, operatorIndex, terminalChildCount, nonTerminalChildCount, directOperatorCount, directOperatorOrdinals, shapeHash64, planHash64, productionsForLhs, productionsPredicting |
| [L74–L84](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecImageView.java#L74-L84) | derivesTerminal, derivableTerminals, productionDerivesTerminal, productionDerivableTerminals, derivesOperator, derivableOperators, productionDerivesOperator, productionDerivableOperators, sourceRuleOperatorIndex |
| [L86–L99](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecImageView.java#L86-L99) | productionDispatchFingerprint, productionDispatchEntryCount() and (int), productionDispatchUniqueEntryCount, productionDispatchAmbiguousEntryCount, predictedTerminals, mayPredict, dispatchCandidateCount, candidateProductions, uniqueProductionOrMinusOne, dispatchAmbiguous, eofCandidateProductions, uniqueEofProductionOrMinusOne, eofAmbiguous |
| [L101–L151](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecImageView.java#L101-L151) | Default batchDispatch, batchEofDispatch |
| [L153–L188](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecImageView.java#L153-L188) | Private requireBatchLengths, requireEofBatchLengths |
| [L190–L197](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecImageView.java#L190-L197) | syntaxKindCodeForProduction, syntaxKindCodeForTerminal, terminalSyntaxKindCode, terminalSymbolFromSyntaxKindCode, productionFromSyntaxKindCode, primitivePayloadBytes |

### MIndexASTSpecPrecompute

Reference: 4. Formal spec precompute.

| Source range | Declarations / body responsibilities |
| --- | --- |
| [L24–L64](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecPrecompute.java#L24-L64) | Six FLAG_* constants; all final language/grammar/source references, symbol/production lanes, offsets/postings, operator indexes, plans/dispatch/fingerprint |
| [L66–L138](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecPrecompute.java#L66-L138) | Constructor: symbol facts and first/follow copying; flattened RHS allocation |
| [L139–L243](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecPrecompute.java#L139-L243) | Constructor: production minima/hashes, distinct reverse uses, sorted prediction sets and EOF |
| [L245–L272](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecPrecompute.java#L245-L272) | Constructor: operator/source-rule mapping; plans, dispatch, fingerprint |
| [L274–L309](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecPrecompute.java#L274-L309) | lang, identity, language, version, grammar, decisionIndex, fingerprint, symbolCount, productionCount |
| [L311–L366](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecPrecompute.java#L311-L366) | symbolKind, tokenClass, literalCase, nullable, productive, reachable, leftRecursive, canEndAfter, minimumTokens, symbolHash64, firstSymbols, followSymbols |
| [L368–L420](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecPrecompute.java#L368-L420) | productionLhs, productionRhsLength, productionRhsSymbol, productionMinimumTokens, productionCanEndAtEof, productionHash64, predictionSymbols, predictsTerminal, productionsUsingSymbol |
| [L422–L459](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecPrecompute.java#L422-L459) | operatorTerminal, operatorIndexForSymbol, operatorForSymbol, hasSourceRules, sourceRuleCount, sourceRule, sourceRuleOperatorIndex, sourceRuleExpressionOperator |
| [L461–L510](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecPrecompute.java#L461-L510) | productionPlans, productionDispatch, precompute, primitivePayloadBytes |
| [L512–L529](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecPrecompute.java#L512-L529) | flag, findExpressionOperator, checkSymbol, checkProduction |
| [L531–L585](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecPrecompute.java#L531-L585) | computeFingerprint, stableSymbolHash, stableProductionHash, offsets, mix, avalanche |

### MIndexASTSpecTreePrecompute

Reference: 8. Per-formal-tree geometry and snapshot transfer.

| Source range | Declarations / body responsibilities |
| --- | --- |
| [L20–L102](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecTreePrecompute.java#L20-L102) | VALUE_MASK, hash seeds, three MAX_SNAPSHOT_* constants; all final references/scalars/geometry lanes; private constructor |
| [L104–L180](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecTreePrecompute.java#L104-L180) | Public nested Snapshot; private scalar/lane fields; package-private constructor stores arrays directly |
| [L182–L204](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecTreePrecompute.java#L182-L204) | Snapshot package accessors: nodeCount, root, parents, childOrdinals, preorderToNode, nodeToPreorder, subtreeSizes, depths, leafCounts, levels, ancestors, kindCodes, exactHashes, structuralHashes, logicHashes, symbolSignals, symbolOffsets, nodesBySymbol, preordersBySymbol, productionOffsets, nodesByProduction, preordersByProduction |
| [L206–L271](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecTreePrecompute.java#L206-L271) | snapshot, restore |
| [L273–L331](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecTreePrecompute.java#L273-L331) | validateSnapshot: dimensions, scalar/lane shape |
| [L333–L380](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecTreePrecompute.java#L333-L380) | validateSnapshot: parentage, supplied preorder permutation/inverse/root and depths |
| [L384–L449](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecTreePrecompute.java#L384-L449) | validateSnapshot: kind, subtree/leaf/hash/signal derivations |
| [L451–L503](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecTreePrecompute.java#L451-L503) | validateSnapshot: ancestors and exact posting arrays/offsets |
| [L505–L554](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecTreePrecompute.java#L505-L554) | build: empty/parent/root/connectivity checks and iterative DFS |
| [L556–L627](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecTreePrecompute.java#L556-L627) | build: node kinds, hashes, symbol signals and counts |
| [L629–L696](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecTreePrecompute.java#L629-L696) | build: ancestor table, node/preorder postings, constructor transfer |
| [L698–L745](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecTreePrecompute.java#L698-L745) | spec, tree, root, nodeCount, parent, childOrdinal, depth, subtreeSize, expandedLeafCount, preorderIndex, preorderNode, subtreeEndPreorderExclusive |
| [L747–L782](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecTreePrecompute.java#L747-L782) | isAncestor, kthAncestor, lowestCommonAncestor |
| [L784–L833](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecTreePrecompute.java#L784-L833) | syntaxKindCode, terminalKind, exactHash64, structuralHash64, logicHash64, mayContainSymbol, nodesForSymbol, nodesForProduction |
| [L835–L880](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecTreePrecompute.java#L835-L880) | subtreeSymbolCount, subtreeProductionCount, containsSymbol |
| [L882–L910](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecTreePrecompute.java#L882-L910) | childRole, childGrammarSymbol, productionShapeHash64 |
| [L912–L954](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecTreePrecompute.java#L912-L954) | primitivePayloadBytes, isTerminalKindCode, terminalSymbolFromKindCode, productionFromKindCode |
| [L956–L1023](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecTreePrecompute.java#L956-L1023) | checkNode, terminalCount, logicTokenContentRelevant, stableTokenHash, symbolBit, lowerBound, offsets, mix, avalanche |

### MIndexASTSpecs

Reference: 3. Canonical spec registry and cache lifetime.

| Source range | Declarations / body responsibilities |
| --- | --- |
| [L12–L18](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecs.java#L12-L18) | SPECS, HEAP_IMAGES; private constructor |
| [L20–L41](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecs.java#L20-L41) | forLang, byIdentity |
| [L43–L60](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecs.java#L43-L60) | heapImage(MIndexLang), heapImage(String) |
| [L62–L75](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecs.java#L62-L75) | writeImage, openMappedImage |
| [L77–L93](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecs.java#L77-L93) | precomputeBuiltIns, cachedSpecCount, cachedHeapImageCount |

### MIndexAssociativity

Reference: 2. Java 21 metadata and value rows.

| Source range | Declarations / body responsibilities |
| --- | --- |
| [L5–L9](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexAssociativity.java#L5-L9) | Enum and all constants: NONE, LEFT, RIGHT |

### MIndexFlagPolicy

Reference: 2. Java 21 metadata and value rows.

| Source range | Declarations / body responsibilities |
| --- | --- |
| [L5–L10](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexFlagPolicy.java#L5-L10) | Enum and all constants: NONE, MODIFIER_BITS, BOOLEAN, ENUM_ORDINAL_PLUS_ONE |

### MIndexJava21LanguageSpec

Reference: 2. Java 21 metadata and value rows.

| Source range | Declarations / body responsibilities |
| --- | --- |
| [L21–L128](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexJava21LanguageSpec.java#L21-L128) | Package-private singleton; INSTANCE, KEYWORDS, CONTEXTUAL_KEYWORDS, LITERAL_TOKENS, PRIMITIVE_TYPES, MODIFIER_TOKENS, MODIFIER_MASK, rules; constructor |
| [L130–L163](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexJava21LanguageSpec.java#L130-L163) | identity, language, release, sourceVersion, ruleCount, rule(Tree.Kind), rule(int) |
| [L165–L243](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexJava21LanguageSpec.java#L165-L243) | keywordCount/keywordAt/isKeyword; contextualKeywordCount/contextualKeywordAt/isContextualKeyword; literalTokenCount/literalTokenAt/isLiteralToken; primitiveTypeCount/primitiveTypeAt/isPrimitiveType; modifierCount/modifierAt/isModifierKeyword/modifierMask |
| [L245–L285](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexJava21LanguageSpec.java#L245-L285) | buildRule, categories |
| [L287–L372](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexJava21LanguageSpec.java#L287-L372) | isCompilation, isDeclaration, isType, isLiteral, isControlFlow, isName, isAnnotation, isModule |
| [L374–L427](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexJava21LanguageSpec.java#L374-L427) | labelPolicy, flagPolicy, minimumRelease, isPreview |
| [L429–L475](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexJava21LanguageSpec.java#L429-L475) | operator and every kind-name operator row |
| [L477–L527](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexJava21LanguageSpec.java#L477-L527) | buildModifierMask, op, sorted, contains, compare |

### MIndexLabelPolicy

Reference: 2. Java 21 metadata and value rows.

| Source range | Declarations / body responsibilities |
| --- | --- |
| [L5–L21](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexLabelPolicy.java#L5-L21) | All six constants, labelled field, constructor, isLabelled |

### MIndexLanguageRule

Reference: 2. Java 21 metadata and value rows.

| Source range | Declarations / body responsibilities |
| --- | --- |
| [L13–L32](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexLanguageRule.java#L13-L32) | Record components ruleIndex, kind, categoryMask, labelPolicy, flagPolicy, operator, minimumRelease, preview; compact constructor |
| [L34–L41](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexLanguageRule.java#L34-L41) | is, isOperator |

### MIndexLanguageSpec

Reference: 2. Java 21 metadata and value rows.

| Source range | Declarations / body responsibilities |
| --- | --- |
| [L18–L27](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexLanguageSpec.java#L18-L27) | identity, language, release, sourceVersion, ruleCount, rule(Tree.Kind), rule(int) |
| [L28–L47](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexLanguageSpec.java#L28-L47) | keywordCount, keywordAt, isKeyword; contextualKeywordCount, contextualKeywordAt, isContextualKeyword; literalTokenCount, literalTokenAt, isLiteralToken; primitiveTypeCount, primitiveTypeAt, isPrimitiveType; modifierCount, modifierAt, isModifierKeyword, modifierMask |
| [L49–L71](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexLanguageSpec.java#L49-L71) | Default astSpec, astProductionPlans, astProductionDispatch, formalExpressionSpec |
| [L73–L86](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexLanguageSpec.java#L73-L86) | Default isReservedToken, modifierBit, hasModifier |

### MIndexLanguageSpecs

Reference: 2. Java 21 metadata and value rows.

| Source range | Declarations / body responsibilities |
| --- | --- |
| [L5–L16](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexLanguageSpecs.java#L5-L16) | Private constructor; java21; forLanguage |

### MIndexOperatorRule

Reference: 2. Java 21 metadata and value rows.

| Source range | Declarations / body responsibilities |
| --- | --- |
| [L12–L30](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexOperatorRule.java#L12-L30) | Record components token, precedence, associativity, arity, shortCircuit, assignment; NONE; compact constructor |
| [L33–L35](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexOperatorRule.java#L33-L35) | isOperator |

### MIndexSyntaxCategory

Reference: 2. Java 21 metadata and value rows.

| Source range | Declarations / body responsibilities |
| --- | --- |
| [L5–L25](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexSyntaxCategory.java#L5-L25) | All fifteen constants, bit |

## 3. Public callers and tests read

The following are supporting boundaries, not additional primary owners. The complete
71-line retained LCA-codec test was read; other entries identify the precise caller
bodies inspected. No directly named spec, production-plan, dispatch, spec-image,
spec-tree or native-dispatch test body is present in retained `src/test`.

| Public retained source | Actual use and limit |
| --- | --- |
| [MIndexAST.java L39–L74](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexAST.java#L39-L74) | spec/rule/astSpec/astProductionPlans/astProductionDispatch/formalExpressionSpec/is delegate through the pool; no per-atom spec state |
| [MIndexASTPool.java L88–L104](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPool.java#L88-L104) | spec always returns Java21; languageId is separate label-domain metadata |
| [MIndexASTPrecompute.java L561–L578](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPrecompute.java#L561-L578) | astSpec delegates to document root; source/LCA accelerators concern Java document occurrences |
| [MIndexASTPrecomputeCodec.java L173–L181](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPrecomputeCodec.java#L173-L181) | Navigation identity checks path, source UTF-16 digest and AST-spec fingerprint; does not serialize this formal spec image |
| [MIndexASTPartialCache.java L54–L77](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPartialCache.java#L54-L77) | Key factory uses Java21 AST-spec fingerprint; does not prove grammar or dispatch behavior |
| [MIndexASTPartialCache.java L957–L969](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPartialCache.java#L957-L969) | Loaded-document identity validation compares document AST-spec fingerprint |
| [MIndexASTEditSession.java L179–L200](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTEditSession.java#L179-L200) | Loaded edit document identity includes key/spec fingerprint |
| [MIndexASTEditSession.java L253–L258](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTEditSession.java#L253-L258) | Edit key accepts the Java21 AST-spec fingerprint |
| [MIndexASTLcaCodecTest L1–L71](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/test/java/com/synexia/indexstring/ast/MIndexASTLcaCodecTest.java#L1-L71) | `authenticatedRoundTripPreservesEveryLcaAndRejectsTampering` uses the spec fingerprint in a cache key; compares every pairwise LCA in one sample before/after an LCA-codec round trip and expects IOException after flipping a payload byte. It does not test spec metadata, dispatch, image-v2 writing, aliasing, native availability or formal-tree restore. `MIndexASTLcaCodec` itself is absent from the retained package. |

Test Git blob: `bc961ddbd08380fe6bb1bb8955378b71a1542903`. Raw SHA-256: `60c7a9617c5291abf727940da0a6896724520a7a3ae11ebc600310fdfb6b7822`. Test execution: **NOT_RUN**.

The five physical retained test files are MIndexASTPrecomputeTest,
MIndexASTLcaCodecTest, MIndexASTCodecBoundedReadProof, MIndexASTPartialCacheTest and
MIndexASTPartialCacheErrorTest. Their complete assertion maps belong to the
[ownership coverage](retained-ast-ownership-reference-coverage.md) and
[navigation coverage](retained-ast-navigation-reference-coverage.md). Merely having
these files, or an indirect fingerprint call, does not close the specification/dispatch direct test gaps.

## 4. Provenance and historical qualification

Every primary row appears in the public AST intake export with donor revision
`7d2133f1412a9e7295296c3f86baae577bb3251c`, target `m3jdk21`, category
`indexstring-ast`, license declaration `Apache-2.0`, mode `APACHE_SOURCE`, and the
original `synexia-indexstring/src/main/java/com/synexia/indexstring/ast/` path
mirrored below `m3/vendor/synexia/`. The raw hashes in section 1 equal those export
rows. These are exact public custody declarations, not independent donor-history
or legal-rights verification.

| Primary file | Exact export row |
| --- | --- |
| MIndexASTChildRole.java | [export.tsv L5](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/ast-navigation-20261007/export.tsv#L5) |
| MIndexASTDispatchNative.java | [export.tsv L8](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/ast-navigation-20261007/export.tsv#L8) |
| MIndexASTProductionDispatch.java | [export.tsv L21](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/ast-navigation-20261007/export.tsv#L21) |
| MIndexASTProductionPlans.java | [export.tsv L22](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/ast-navigation-20261007/export.tsv#L22) |
| MIndexASTSpecImage.java | [export.tsv L27](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/ast-navigation-20261007/export.tsv#L27) |
| MIndexASTSpecImageView.java | [export.tsv L28](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/ast-navigation-20261007/export.tsv#L28) |
| MIndexASTSpecPrecompute.java | [export.tsv L29](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/ast-navigation-20261007/export.tsv#L29) |
| MIndexASTSpecTreePrecompute.java | [export.tsv L30](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/ast-navigation-20261007/export.tsv#L30) |
| MIndexASTSpecs.java | [export.tsv L31](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/ast-navigation-20261007/export.tsv#L31) |
| MIndexAssociativity.java | [export.tsv L32](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/ast-navigation-20261007/export.tsv#L32) |
| MIndexFlagPolicy.java | [export.tsv L33](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/ast-navigation-20261007/export.tsv#L33) |
| MIndexJava21LanguageSpec.java | [export.tsv L34](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/ast-navigation-20261007/export.tsv#L34) |
| MIndexLabelPolicy.java | [export.tsv L35](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/ast-navigation-20261007/export.tsv#L35) |
| MIndexLanguageRule.java | [export.tsv L36](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/ast-navigation-20261007/export.tsv#L36) |
| MIndexLanguageSpec.java | [export.tsv L37](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/ast-navigation-20261007/export.tsv#L37) |
| MIndexLanguageSpecs.java | [export.tsv L38](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/ast-navigation-20261007/export.tsv#L38) |
| MIndexOperatorRule.java | [export.tsv L39](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/ast-navigation-20261007/export.tsv#L39) |
| MIndexSyntaxCategory.java | [export.tsv L40](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/ast-navigation-20261007/export.tsv#L40) |

The intake README describes 42 nonlicense payloads plus two preserved LICENSE/NOTICE
entries. QUALIFICATION.json labels its result
`BOUNDED_HOST_AST_ORACLES_AND_EXACT_ISOLATED_BYTE_INTAKE_ONLY`: four original AST
precompute tests, 21 importer unit tests and eight isolated intake cases for 44
payloads (37 production sources, five tests, two license/notice files), with
hash/size/mtime second-apply and refusal evidence. These are attributed historical
results, not tests run during authoring or proof for the present spec/dispatch APIs.
It explicitly excludes portable source-complete build, target consumers,
valid-checksum wrong-geometry cold-image refusal, cache/edit/resident lifecycle,
full seed/Maven/JaCoCo, JNI providers, matched JDK/jtreg/platforms, performance and
full-family completion. No missing implementation is filled from private source.

## 5. Policy and build identities

The following support bodies were read for their relevant contract and compared
byte-for-byte at both pins. They remain unchanged. Hashes below are raw SHA-256;
these are evidence identities, not executable verification receipts.

| Public path | SHA-256 | Scope |
| --- | --- | --- |
| [m3/vendor/synexia/README.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/README.md) | `95317a8e1f02ce87776d4b042781ffbbd3a8f38321ff7ed30e1891a51a315ca7` | Custody, licensing, ownership or admission policy |
| [m3/docs/synexia-delivery.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/synexia-delivery.md) | `197e73090a07272677b7b344470e47557e302ab63975f14e3325235b046f757d` | Custody, licensing, ownership or admission policy |
| [m3/docs/synexia-apache-reuse.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/synexia-apache-reuse.md) | `6957c8f8796a7546a8676cdead62fff1440a32a742f1fcc377ae54cb2f31ee2c` | Custody, licensing, ownership or admission policy |
| [m3/docs/synexia-apache2-borrowing-contract.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/synexia-apache2-borrowing-contract.md) | `20d6460e3746913964d07712e7455b25c9f08ccba70cce49c15cd689bb54e9da` | Custody, licensing, ownership or admission policy |
| [m3/docs/M3JDK21_PORTING_INVARIANT.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/M3JDK21_PORTING_INVARIANT.md) | `a79062d8f88e59b6b34f57fbf22c525a3d8fe67dc00fa0d0e8d3176c62f9e90e` | Custody, licensing, ownership or admission policy |
| [m3/docs/m3jdk21-porting-invariant.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/m3jdk21-porting-invariant.md) | `fa84270f09a42fe4dbeb245812e7b710297825e2bfd461bce99bb64056e1399d` | Custody, licensing, ownership or admission policy |
| [m3/docs/synexia-intake-license-field-semantics-20261011.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/synexia-intake-license-field-semantics-20261011.md) | `d840f7d7be25bb736fd4d372a7c9c11e8c794dfc54dd13b04acb144608c65bdf` | Custody, licensing, ownership or admission policy |
| [m3/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/pom.xml) | `15f0d5a0e942749392480372ff4b414949becb11370d1dbe25dbdda7dfb50209` | Declared build routing only; not executed |
| [m3/build.sh](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/build.sh) | `9119dec83045301a2166d0a1de3b1008dfd431eaf3b5317daa14dc7e41325807` | Declared build routing only; not executed |
| [m3/synexia-import/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/pom.xml) | `d85d3d4ed6c2ed2b2c637d64805c5f9462e2157ba66019be15075ca1bafbddb0` | Declared build routing only; not executed |
| [m3/ports/indexstring/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/ports/indexstring/pom.xml) | `d07cf94116968269d1367e6e4ad41c6bc847bb9410ade7ff542255a74d2f346b` | Declared build routing only; not executed |
| [m3/compatibility/synexia-reference/PROVENANCE.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/compatibility/synexia-reference/PROVENANCE.md) | `249a8f82e2b3cd32767d5ae6eb31569d59b1f4cd3cb264720b1b9473b3d325ce` | Separate test-only frozen-text fixture; not retained AST dependency closure |
| [m3/synexia-import/intakes/ast-navigation-20261007/README.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/ast-navigation-20261007/README.md) | `d1ffddc299cc975de0ccaa6bd503bc49ffab03cd78ef77a53173e36960f42a29` | Historical AST intake identity/evidence, bounded as above |
| [m3/synexia-import/intakes/ast-navigation-20261007/evidence/QUALIFICATION.json](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/ast-navigation-20261007/evidence/QUALIFICATION.json) | `597e119d7fd3375a0c6fd8ddf88c1f9a6c32932c01ed76f32e00ea14023a1dab` | Historical AST intake identity/evidence, bounded as above |
| [m3/synexia-import/intakes/ast-navigation-20261007/export.tsv](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/ast-navigation-20261007/export.tsv) | `860eade0c05a175373e3f96a19150de03f4865efdfcf077af5d5a52d15a471b7` | Historical AST intake identity/evidence, bounded as above |

The main `m3/pom.xml` lists control modules, not this vendor subtree. `m3/build.sh`
selects `core/src` and named core tests. `m3/synexia-import/pom.xml` defines the
separate Java-21 import verifier with JUnit `5.10.2-m3-source-1` and declared
JaCoCo 0.99 line/branch thresholds; declaration is not successful execution.
`m3/ports/indexstring/pom.xml` defines the independent
`com.m3.compat:indexstring-retained-view:0.1.0-SNAPSHOT` bridge and its own tests.
None makes the retained AST sources an included Maven/JPMS module.

Current root AGENTS, name-mapping, historical full-family pin/catalogue and current
full-borrow README/pin/promotion/receiver-status records were also read at
`fb1e2f78ae4000f34adbed4a7fb91a69dd5ef8e0`. They preserve historical inventory,
String-first ordered qualification and source-pin-only AST/grammar family states.
No verifier command was run and no policy record was edited.

## 6. Explicit unresolved dependency and behavior boundaries

- Formal language/grammar: MIndexLang, MIndexLangs, MIndexGrammar,
  MIndexGrammarDecisionIndex, MIndexGrammarParseTree, MIndexGrammarSymbolKind,
  MIndexLiteralCase, MIndexTokenClass, MIndexTokenStream,
  MIndexExpressionOperator, MIndexFormalLanguageSpec and MIndexFormalLanguageSpecs
  are referenced but not present as source owners in this retained subtree
- Same-package image owners HeapMIndexASTSpecImage and MappedMIndexASTSpecImage are
  absent. Reader validation, v1 migration, mapping/close/unmapping, immutable-view
  implementation and their overrides remain unsupported here
- IndexStringNative and implementations of the five native dispatch declarations
  are absent. The retained native shared-array sources/CMake target do not establish
  an AST dispatch provider, ABI parity, address lifetime or runtime fallback proof
- javac/compiler tree and model APIs supply Java rule kinds and modifiers; ordinal
  use is host-enum-dependent. No lexer implementation or whole Java semantic
  validator is present among these eighteen owners
- Public batch defaults and concrete dispatch differ in prevalidation and write
  order; input/output aliasing is not refused or copied. Native/provider alias and
  failure atomicity are unqualified
- Formal-tree restore does not explicitly compare grammar fingerprints or rebuild
  canonical DFS/preorder contiguity. Hashes and shape checks are not substituted
  for a complete hostile-input proof
- Derivation bit propagation is not productivity-filtered; token-first operator
  mapping is not overload resolution; identity hashes are not semantic equality
- File writes are direct, non-atomic output effects. No retained mapped-reader or
  cryptographic authenticity claim follows from writer checks or the SHA-256 trailer
- Registry lifetime is process-static without eviction; caller output arrays and
  package-private snapshot/native aliases remain outside defensive immutable APIs
- Payload-byte counters are primitive-storage estimates only. No measured CPU,
  allocation, retained heap, native memory or concurrency benchmark is available

Remaining qualification requires source/dependency closure, direct metadata and
ambiguity/error tests, identity-collision and cache behavior, snapshot corruption
and alias/race cases, writer/reader version/geometry/refusal tests, native ABI and
fallback/lifetime proof, actual consumer/module integration and target compiler/
JDK/runtime/resource evidence. Complete member routing here promotes none of them.
