<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Retained AST navigation source and assertion coverage

[Reference](retained-ast-navigation-reference.md) · [Guide](README.md) · [AST ownership](retained-ast-ownership-reference.md)

## Scope and evidence rules

The ten primary bodies below were read in full at public M3JDK21 source pin
`363c02caa2e5af1df9460b927ecd78f6a9dbcefb`, including nested types,
package-private restore/native-projection helpers and private validation/search
support. Their blobs match public documentation head
`fb1e2f78ae4000f34adbed4a7fb91a69dd5ef8e0`; the comparison between those commits
changes only eight Markdown files. The hash table is file-identity evidence;
source reading is not executable branch coverage, semantic verification or
complete module/package/repository qualification.

All test bodies listed here were read, **NOT_RUN**. No Java source, test, build,
repository script, verifier, native library or benchmark was executed for this
reference. The one usage sketch is inspected, not compiled or executed.
Hosted CI remains separate evidence for its exact commit; no hosted result is
asserted as an authoring or review run here.

Primary ownership is limited to navigation/search/diff/reuse sources. Pool,
document, parser, occurrence interface, edit and cache lifetimes are supporting
boundaries owned by [AST ownership](retained-ast-ownership-reference.md).
Language/spec/production tables and generic spec-tree navigation remain separate
owners. The retained subtree is not source-complete and has no independent POM
or module descriptor.

## 1. Exact primary-source inventory

All primary paths are below
`m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/`.
Each link points to the complete pinned body. SHA-256 values are over exact file
bytes, not normalized line endings. Git blobs include Git's blob header.

| Complete source | Lines / bytes | Git blob SHA-1 | SHA-256 |
| --- | --- | --- | --- |
| [MIndexASTDiff.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTDiff.java#L1-L57) | 57 / 1649 | `8c4c0064113b1d433e5f1f041cc30bf5f0c04008` | `53cebb46529470640eb6b4fd1bd812306cc87fda8079a7f99323b1fd4e14fc8e` |
| [MIndexASTReuseKey.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTReuseKey.java#L1-L151) | 151 / 5691 | `096e3ade5df2eb85a46dc0dfc6ff1072486e8a73` | `b4fbcd9c7734152f4e564337fe4ec5ff15a68e6dacde8d3eb12e5af88e527a7b` |
| [MIndexASTQuery.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTQuery.java#L1-L286) | 286 / 9016 | `39595feb11d7177c510b1d2b93145abab41a8202` | `94a96d7accee6d889b7cb48e9b4b1768bba6fe5459437478897e0c5b054e7172` |
| [MIndexASTPoolPrecompute.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPoolPrecompute.java#L1-L763) | 763 / 27965 | `6179457eebaaa180d9706382f5cf69abb7bf4ac2` | `7ea7bd71c9e180571656144ff0904530a07d17c3293b8110bf734c0b36fcea7b` |
| [MIndexASTPrecompute.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPrecompute.java#L1-L767) | 767 / 27124 | `422d465eed951328273937ee9713c8822f415cd3` | `7baae702ff9c58180329df4b2c9947d3f2d6247fca37757d9656b8ec776460b7` |
| [MIndexASTPrecomputeCodec.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPrecomputeCodec.java#L1-L255) | 255 / 10065 | `f18a4cbd3ffb70e3396885a88311c1f13bcf2202` | `686f9d6e4fbf27af15cc0a5ab64cea8055a19aa8c4e92a7a66f88bd80bddb958` |
| [MIndexASTLcaSparseTable.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTLcaSparseTable.java#L1-L361) | 361 / 12376 | `6d42d4d76b582481a419ea4d231d60b21b9372e1` | `feb546609e2fa96f530e19ec9975fc5b17d51db07dd5719e1b573546c8caae6c` |
| [MIndexASTSourceIndex.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSourceIndex.java#L1-L280) | 280 / 9345 | `0eeb995745f38cca7ef3d43a1de982a142d2b5a6` | `d182de784b0c307ea1c81ed802d7bef0f351928d0a6ecdcb676aa582ca8f414a` |
| [MIndexASTKindPattern.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTKindPattern.java#L1-L90) | 90 / 3247 | `42634344220f60e3a8c3f6f59747a4a50afcd47c` | `2d7bb59f1fc7a7aa58b6ecc156cd577a8950b47a1d4577e6921708c2fd9f2d26` |
| [MIndexASTKindAutomaton.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTKindAutomaton.java#L1-L230) | 230 / 7459 | `bc98d8bb72a51919c4f2cdda5b6420211d0e27c6` | `d30301a6309651dfa18ab0a67690932d247ef38cc4b43a23849c4e6685e0c984` |

The [AST export manifest][export] records each of these ten bodies with donor
revision `7d2133f1412a9e7295296c3f86baae577bb3251c`, category `indexstring-ast`,
mode `APACHE_SOURCE`, original path and matching SHA-256. The public receiving
record and actual retained bytes are the evidence used here. The manifest's
license column does not supersede per-file, OpenJDK or third-party rights.

## 2. Title-to-owner and entry-point map

Ranges are inclusive source lines at the pin. Public record components include
their generated accessors and standard record construction/equality/hash/string
surface; generated methods are not independently authored bodies or AST semantic
comparators. Enum constants include their ordinary Java enum surface. Listed
private support is explanatory, not additional public API.

### Diff and candidate reuse

| Owner / source range | All declared members or support in range | Contract route |
| --- | --- | --- |
| [Diff 7–27][diff-types] | Private utility constructor; `Classification` (`IDENTICAL`, `LABEL_ONLY`, `NORMALIZED_LOGIC_CHANGED`, `STRUCTURE_CHANGED`); `Result` components `classification`, left/right exact/structural/logic hashes, `simHashDistance`, left/right expanded nodes | Reference §7: records may be directly constructed without validation |
| [Diff 29–56][diff-compare] | `compare(left,right)` | Exact → structural → normalized-logic checks; result capture; null rejection |
| [ReuseKey 13–41][reuse-types] | Seven record components; compact constructor; `Candidate` five constants and `Side.LEFT/RIGHT` | Primitive values; negative/unknown bounds; no implicit source/pool identity |
| [ReuseKey 43–75][reuse-from] | `from(ast)`, `from(result,side)` | Full feature extraction versus zero SimHash / `-1` shape placeholders |
| [ReuseKey 78–108][reuse-candidates] | `exactCandidate`, `normalizedLogicCandidate`, `structuralCandidate`, `approximateCandidate`, `hammingDistance` | Hash/shape filters; approximate has no shape requirement; null/distance validation |
| [ReuseKey 110–150][reuse-classify] | `classify`; package-private `classifyWithHammingDistance`; private `sameShape` | Candidate precedence, conditional versus upfront distance checks, unknown-shape wildcard semantics |

### Query and builder

| Owner / source range | All declared members or support in range | Contract route |
| --- | --- | --- |
| [Query 14–63][query-state] | Final predicate fields; private copying constructor | Reference §6: immutable compiled settings retain one pool |
| [Query 65–111][query-match] | `builder`, `pool`, `matches`; package-private `matchesHandle` | Same-pool guard; ordered final predicates; subtree signal then exact kind traversal |
| [Query 113–147][query-index] | Package-private `rootKindOrNull`, `labelSpecifiedForIndex`, `labelHandleForIndex`, `exactHashSpecifiedForIndex`, `exactHashForIndex`, `structuralHashSpecifiedForIndex`, `structuralHashForIndex`, `logicHashSpecifiedForIndex`, `logicHashForIndex` | Pool posting planner metadata only |
| [Builder 149–201][query-builder-labels] | Mutable fields/defaults; private constructor; `rootKind`; both `label` overloads; `noLabel` | Nulls, same-resolver requirement through pool, interning and empty-label overload distinction |
| [Builder 203–245][query-builder-filters] | `requireFlags`, `forbidFlags`, `requireCategory`, `exactHash`, `structuralHash`, `logicHash`, `simHashNear` | OR versus replacement semantics; Hamming range |
| [Builder 247–285][query-builder-ranges] | `expandedNodeCount`, `depth`, `expandedLeafCount`, `containsKind`, `build` | Inclusive positive ranges; one subtree-kind predicate; conflicting flag failure |

### Document occurrence navigation

| Owner / source range | All declared members or support in range | Contract route |
| --- | --- | --- |
| [Precompute 16–78][nav-state] | Path constants, retained lanes/pool/document, private constructor | Reference §§1–2: frozen occurrence geometry |
| [Snapshot 86–140][nav-snapshot-type] | Package-private nested type and constructor for count, preorder/inverse, sizes/counts/ordinals, levels/ancestors, powers/path hashes, kinds/postings, atom limit/postings | Internal raw-array support container; not a public wire API |
| [Snapshot 142–209][nav-snapshot-accessors] | `occurrenceCount`, `preorderToOccurrence`, `occurrenceToPreorder`, `subtreeSizes`, `childCounts`, `childOrdinals`, `levels`, `ancestors`, `powers`, `kindPathHashes`, `exactPathHashes`, `preorderKinds`, `kindOffsets`, `occurrencesByKind`, `atomLimit`, `atomOffsets`, `occurrencesByAtom` | Package-private accessors expose snapshot-owned lanes |
| [Precompute 211–256][nav-copy-restore] | Package-private `snapshot`, `restore` | Detached export, validation and defensive restore copies |
| [Precompute 258–399][nav-validate] | Private `validateSnapshot` | Dimensions, preorder, child/subtree, ancestors, powers, kind/atom/hash and posting checks |
| [Precompute 401–426][nav-posting-check] | Private `IntKey.key`, `validateOffsetsAndPosting` | Key/cardinality checks; no explicit posting uniqueness/order check |
| [Precompute 429–559][nav-build] | `build(document)` | Iterative traversal; nonempty/traversal-count checks; captured pool atom limit; postings |
| [Precompute 561–599][nav-access] | `document`, `astSpec`, `constantTimeLca`, `sourceIndex`, `occurrenceCount`, `preorderOccurrence`, `preorderIndex`, `subtreeSize`, `subtreeEndPreorderExclusive` | Reference §§2–4: direct accessors, optional new projections and coordinate checks |
| [Precompute 601–639][nav-tree] | `subtreeOccurrences`, `childCount`, `childOrdinal`, `isAncestor`, `kthAncestor` | Detached subtree, root ordinal, inclusive ancestry, distance-zero/above-root semantics |
| [Precompute 641–679][nav-path] | `lowestCommonAncestor`, `distance`, `pathOccurrences` | LCA, edge count, arbitrary endpoint path |
| [Precompute 681–717][nav-search] | Both `occurrences` overloads, `preorderKindOrdinal`, `preorderKindOrdinals`, `kindPathHash`, `exactPathHash` | Pool identity, frozen atom limit, copy/order and candidate path hashes |
| [Precompute 719–767][nav-tail] | `primitivePayloadBytes`; private `pathHash`, `checkOccurrence`, `prefixOffsets`, `avalanche` | Payload accounting, nonancestor rejection, checked indexes/sums and 64-bit hash arithmetic |

### Optional Euler/RMQ LCA

| Owner / source range | All declared members or support in range | Contract route |
| --- | --- | --- |
| [LcaSparseTable 13–92][lca-state] | Final lanes; package-private `Snapshot` constructor and accessors `firstEuler`, `eulerOccurrences`, `eulerDepths`, `log2`, `levels`, `sparseMinIndex`; private table constructor | Reference §3: same document, support-image array boundary |
| [LcaSparseTable 94–163][lca-build] | `build(document)` | Iterative Euler construction, checked size arithmetic, empty/tour-mismatch refusals |
| [LcaSparseTable 165–194][lca-restore] | Package-private `snapshot`, `restore` | Clone export; validate; clone publication |
| [LcaSparseTable 196–235][lca-api] | `document`, `lowestCommonAncestor`, `eulerLength`, `levels`, `primitivePayloadBytes` | O(1) LCA lookup, coordinate validation and payload count |
| [LcaSparseTable 237–347][lca-validate] | Private `validateSnapshot` | Expected geometry, Euler adjacency/multiplicity/depth, first visit, logs, sparse recurrence and padding |
| [LcaSparseTable 349–360][lca-visit] | Private `visit` | Euler row/depth writes and first-visit initialization |

### Source intervals

| Owner / source range | All declared members or support in range | Contract route |
| --- | --- | --- |
| [SourceIndex 14–75][source-build] | Final lanes/private constructor; `build(document)` | Reference §4: known-range selection, sort, power-of-two max tree |
| [SourceIndex 77–134][source-api] | `document`, `indexedOccurrenceCount`, `deepestContaining`, `containingPath`, `firstStartingAtOrAfter`, `primitivePayloadBytes` | Half-open ranges, missing sentinel, ancestry path, lower-bound tie rule and payload count |
| [SourceIndex 136–165][source-restore] | Package-private static `snapshot`, `restore`; `Payload(starts,ends,occurrences,treeBase,maxEndTree)` | Clones; restore itself has no geometry/identity validator |
| [SourceIndex 167–199][source-bounds] | Private `rightmostWithEndAtLeast`, `lowerBound`, `upperBound` | Pruning is candidate-only; strict end checked by public lookup |
| [SourceIndex 201–279][source-sort] | Private `sortRanges`, `compareRange` | Iterative merge sort by start/depth/occurrence; unchecked int block arithmetic precedes tree-size guard; equal-depth containing tie derives from reverse traversal |

### Pool postings, planning and restoration

| Owner / source range | All declared members or support in range | Contract route |
| --- | --- | --- |
| [PoolPrecompute 19–93][pool-state] | Six source-kind constants, frozen lanes; package-private `Snapshot` record; private constructor | Reference §5: pool-local atom/label limits |
| [Snapshot 44–60][pool-snapshot-type] | Components `limit`, `labelLimit`, `kindOffsets`, `handlesByKind`, `labelOffsets`, `handlesByLabel`, `parentOffsets`, `parentsByChild`, `exactKeys`, `exactHandles`, `structuralKeys`, `structuralHandles`, `logicKeys`, `logicHandles`, `labelTexts`, `atomIdentities` | Internal wire support values; no independent public codec |
| [PoolPrecompute 95–185][pool-build] | Public static `snapshot(pool)` | Counts/fills kind/label/edge postings and unsigned sorted hash pairs |
| [PoolPrecompute 187–218][pool-snapshot] | Package-private `snapshot()` | Materializes label texts, creates 32-byte identities and copies arrays |
| [PoolPrecompute 227–375][pool-restore] | Package-private `restore(pool,payload)` | Exact live-count match, bounded shape, identity remap, posting/edge/hash verification |
| [PoolPrecompute 377–417][pool-remap-identity] | Private `HashKind` enum; `remapAtoms`, `remapLabels` | Digest/text maps; duplicate live/stale/null identity refusal |
| [PoolPrecompute 418–482][pool-verify] | Private `remapKindPosting`, `remapPairs`, `mapHandle`, `validatePairs`, `validateOffsets` | Membership/uniqueness, limits, hash/ordering and offset checks |
| [PoolPrecompute 484–494][pool-identity-key] | Private `IdentityKey` record, overridden `equals` and `hashCode` | Byte-array content key for internal maps |
| [PoolPrecompute 496–541][pool-access] | `pool`, `atomCount`, `labelCount`, `handles`, `handlesByLabelHandle`, `parents`, `incomingParentCount`, `handlesByExactHash`, `handlesByStructuralHash`, `handlesByLogicHash` | Copy-return lookup, empty label miss, checked atom domain and duplicate edge counts |
| [PoolPrecompute 543–588][pool-groups-find] | `structuralHashGroups`, `exactHashGroups`, `logicHashGroups`, `find` | Candidate grouping, pool guard and predicate rechecking |
| [PoolPrecompute 590–649][pool-planning] | `primitivePayloadBytes`; private `bestSource`, `narrower`, `checkHandle` | Minimum single posting, stable tie precedence, captured-domain failures |
| [PoolPrecompute 651–699][pool-search-support] | Private `lookup`, `groups`, `offsets`, `lowerBound`, `upperBound` | Minimum group count; checked offsets; unsigned key bounds |
| [PoolPrecompute 700–762][pool-sort] | Private `sortPairs`, `compare`; `Source(kind,from,to)` record and `length` | Stable merge geometry and unsigned-key/handle ordering |

### Kind patterns and automaton

| Owner / source range | All declared members or support in range | Contract route |
| --- | --- | --- |
| [KindPattern 9–40][pattern-create] | Immutable lanes/private constructor; `of`, `length`, `kindAt` | Reference §8: nonempty/non-null kinds, copied ordinals and KMP preprocessing |
| [KindPattern 42–77][pattern-search] | `findAll`, `matchesPath` | Overlap-aware preorder starts versus complete arbitrary endpoint path |
| [KindPattern 79–90][pattern-tail] | `failureTable`, `primitivePayloadBytes`; package-private `ordinalAt` | Clone, byte/int payload, raw compiler support accessor |
| [KindAutomaton 21–120][automaton-compile] | Frozen lanes/private constructor; `compile` | 1–64 patterns; trie, dense transitions, failure/output construction and checked capacity |
| [KindAutomaton 122–144][automaton-access] | `patternCount`, `stateCount`; package-private `alphabetSizeForNative`, `transitionsForNative`, `outputMasksForNative`, `patternLengthsForNative` | Scalar counts and cloned native-image support; no JNI invocation |
| [KindAutomaton 146–173][automaton-scan] | `scanPreorder`, `primitivePayloadBytes` | End-position/pattern-index emission, unmodifiable result list, payload count |
| [KindAutomaton 175–223][automaton-identity] | `imageRootSha256`; private `updateInts`, `updateLongs`, `updateInt` | Versioned compiled-image identity, exact big-endian hashing, SHA failure |
| [KindAutomaton 225–230][automaton-match] | Public `Match(patternIndex,startPreorder,endPreorderExclusive,startOccurrence)` | Derived scan coordinates; direct record construction is unvalidated |

### Navigation image codec

| Owner / source range | All declared members or support in range | Contract route |
| --- | --- | --- |
| [PrecomputeCodec 25–34][codec-constants] | Magic/version/projection/text/occurrence/level/atom constants; private constructor | Reference §9: one navigation format, not general AST/LCA/source/pool serialization |
| [PrecomputeCodec 36–84][codec-write] | `write(key,languageId,index,output)` | Key/document validation, snapshot/restore validation, streaming checksum and caller-owned output |
| [PrecomputeCodec 86–171][codec-read] | `read(key,languageId,document,input)` | Identity/header/length checks, digest/trailing refusal, restore publication and caller-owned input |
| [PrecomputeCodec 173–204][codec-validation] | Private `validateIdentity`, `validateHeader` | Materialized path/UTF-16 digest/spec fingerprint; read-side caps and exact shape |
| [PrecomputeCodec 206–244][codec-io] | Private `writeInts`, `writeLongs`, `readInts`, `readLongs`, `writeText`, `readText` | Big-endian length-prefixed arrays; 1 MiB UTF-8 field bound; no strict UTF-8 decoder |
| [PrecomputeCodec 247–253][codec-sha] | Private `sha256` | SHA-256 provider absence throws `IllegalStateException` |

## 3. Test assertions, all read and NOT_RUN

### Four historical navigation methods

The public [MIndexASTPrecomputeTest][test-precompute] has four test methods. The
2026-10-07 [qualification receipt][qualification] attributes a four-method pass
to this original oracle set over its bounded host. That dated result is not a run
at either documentation commit and does not apply to every retained test source.

| Test and assertion lines | Assertion content actually present | Relevant primary owners / limit |
| --- | --- | --- |
| `occurrencePrecomputeBuildsIntervalsBinaryLiftingPathHashesAndPostings`, 16–34 | Document/index count equal; root subtree covers count; root preorder inverse; positive primitive payload | Precompute construction/access, not exact heap size |
| Same, 36–48 | Every preorder inverse; subtree array length; each nonroot parent is ancestor, one-step ancestor matches, child ordinal nonnegative | Precompute traversal/ancestor support; no negative bounds/root ordinal assertion |
| Same, 50–66 | Two PLUS occurrences; canonical repeated atom has same occurrence postings after sorting; enclosing methods are ancestors/LCA; positive edge distance | Occurrence versus atom coordinates; no hash-collision adversary |
| Same, 68–76 | Sparse LCA equals binary-lifting LCA for two pairs; Euler size `2*n-1`; positive payload | LCA build/query only; no restored image |
| Same, 78–88 | Equal kind path hashes and unequal exact path hashes for selected methods; path starts/ends at expected occurrences | Candidate features in one fixture, not equivalence proof |
| `sourceIndexFindsDeepestOccurrenceAtCursorOffset`, 91–115 | Cursor at `'+'` is valid and finds a PLUS; path length/root/deepest; positive indexed count/payload; start lookup at zero succeeds | SourceIndex happy path; no unknown/zero-width/tied/negative/end-boundary assertions |
| `poolPrecomputeIndexesKindsLabelsHashesParentsAndQueryCandidates`, 118–146 | Pool atom count; one canonical PLUS; exact/structural hash postings; at least one incoming edge/parent; positive payload | PoolPrecompute; despite method title, there is no direct label-posting assertion |
| Same, 148–159 | PLUS query sets root, three hashes, exact depth and leaf ranges; find yields its one handle | Query/PoolPrecompute happy path; no flags/category/label/SimHash/bad-range/refusal assertion |
| Same, 161–174 | Two METHOD handles share structural hash and appear in an equal-hash group of minimum two | Candidate grouping only; no `structurallyEquals` assertion |
| `kmpAndAhoCorasickScanThePrecomputedKindStream`, 178–213 | Two methods, at least two matches for three-kind prefix, successful method-to-PLUS whole path pattern | KindPattern, including one genuine ancestor path; no arbitrary-endpoint test |
| Same, 215–225 | Automaton compiled from method prefix and PLUS/child prefix has a match for each pattern index; positive payload | KindAutomaton; no exact match-list/order, 64-pattern, image-root or invalid-input assertions |

Helpers at [228–271][test-precompute-helpers] decode kind ordinals, walk parents,
search an int array, sort a clone and construct a dictionary/runtime/pool/parser
fixture. Those fixtures depend on `IndexDictionary`, `IndexRuntime`, `IndexSpace`
and indexed-string infrastructure outside these ten owners. The test's parsed
Java sample does not show standalone buildability of this retained directory.

### LCA codec test names an absent owner

[MIndexASTLcaCodecTest][test-lca] contains one test:
`authenticatedRoundTripPreservesEveryLcaAndRejectsTampering` (18–56).
It builds a `MIndexASTLcaSparseTable`, invokes absent retained owner
`MIndexASTLcaCodec.write/read` (36–39), compares every occurrence pair's LCA
(41–48), flips the byte just before a presumed 32-byte checksum and expects
`IOException` (50–55). Fixture/helper construction is at 58–70.

The present table body supports documenting LCA computation and its own internal
restore checks. The absent codec body prevents claiming its envelope, I/O failure
contract, byte budgets or test execution. This is not a test of
`MIndexASTPrecomputeCodec`, and its method name is not cryptographic
producer-authentication evidence.

### General AST codec proof names another absent owner

[MIndexASTCodecBoundedReadProof][test-bounded] is an executable main, not a JUnit
test. Its class/SOURCE/private constructor are at 16–20. Every codec call is to
absent retained owner `MIndexASTCodec`.

| Lines / member | Assertions and helper behavior |
| --- | --- |
| 22–66 `main` | Creates an image; tries oversized atom/child/occurrence/diagnostic counts and path text; valid bounded read must preserve AST `contentEquals` and exact rewritten bytes; short budget must fail; prints a PASS marker only after those checks |
| 68–79 `expectRejected` | Overwrites a selected count with `Integer.MAX_VALUE`; requires `IOException` mentioning that field |
| 81–89 `expectBudgetRejected` | Byte limit is `max(12,image.length-1)`; accepts any `IOException`, including truncation |
| 91–102 `expectTextRejected` | Overwrites path length with `Integer.MAX_VALUE`; requires `IOException` containing `UTF-8 field length` |
| 104–129 `atomCountOffset`, `pathTextLengthOffset`, `childCountOffset` | Parse assumed general-AST wire offsets; first atom must exist |
| 132–166 `occurrenceCountOffset`, `diagnosticCountOffset`, `header`, `skipText` | Skip assumed atom rows and 36-byte occurrence rows; header offset 12; text length must fit fixture buffer |
| 168–187 `parse`, `newPool`, `encode` | Parser/dictionary/runtime fixture and absent codec write |

Its asserted budget/count protections cannot be transferred to the navigation
codec, which has different magic, header, arrays and no caller-total-budget
parameter. The printed marker is source text, not a result of this documentation
pass. No general codec body or current runnable dependency closure is established.

### Explicit untested contracts

There is no test body in this three-file set directly exercising Diff,
ReuseKey, navigation codec write/read, pool image restore, navigation image
restore, SourceIndex internal restore or the many validation/failure edges listed
above. In particular: empty/malformed geometry, same-key duplicate restored
postings, root ordinal tampering, overflow/resource peaks, concurrency under pool
admission, independent streams, missing digest provider, nulls, bad ranges,
threshold-validation ordering, placeholder SimHashes, checksum recomputation,
strict UTF-8 requirements and cross-process/JDK-ordinal portability are not
qualified by the four happy-path AST methods.

## 4. Supporting callers and dependency limits

These are bounded caller/support reads, not extra primary owners.

| Supporting source and ranges | Observed linkage / retained limit |
| --- | --- |
| [MIndexASTDocument 31–111, 118–179, 182–265][document-support] | Defensive occurrence geometry/digest construction; postorder-like parent constraints; source digest and pool identity; projection factories; coordinate accessors; linear `findOccurrences(query)` returns occurrence IDs |
| [MIndexASTPool 23–60, 106–134, 170–225][pool-support] | Volatile arena/size publication, synchronized atom admission, projection factory, label interning and same-resolver check; snapshot factory is not globally synchronized with label growth |
| [MIndexASTPool 289–405, 627–637, 695–723][pool-equality] | Kind signal then traversal; exact/structural/logic comparison bodies; label relevance is primitive type or `*_LITERAL`; Hamming/kind signal helpers |
| [MIndexAST 207–248][atom-support] | `reuseKey` factory and delegates for exact/structural/normalized-logic comparisons |
| [MIndexASTParser 66–108, 132–151, 167–187][parser-source] | javac parse-only construction; stable character input; UTF-16 code-unit digest; source range extraction; no new parser/runtime owner implied |
| [MIndexASTPartialCache 210–385][resident-upgrade] | Resident optional projection slots; upgrade builds only requested missing projection; FULL combines document, navigation, source, LCA and pool projection |
| [MIndexASTPartialCache 387–420][resident-weight] | Estimated retained-byte accounting is separate from each projection's primitive payload and samples the retained pool/labels |

`MIndexASTQuery` imports `MIndexString`; pool/document/parser callers also depend
on `MatIndexString`, `MatIndexStringPool`, resolver/runtime and grammar families.
Those full owners are not present as a source-complete dependency set in this
retained subtree. Standard `Tree.Kind`/javac APIs belong to the compiler module;
JNI-named support accessors in the automaton do not establish native execution.
The corpus does not include `MIndexASTCodec` or `MIndexASTLcaCodec` bodies, and
SourceIndex's restore comment cannot stand in for an absent validator. The
separately built bridge with some similarly named types is not an automatic
replacement dependency or loader-compatible module.

Kind patterns and preorder streams use byte-sized Java `Tree.Kind` ordinals.
The pool guards an alphabet of at most 256 kinds, but patterns/images do not gain
a cross-JDK stable ordinal ABI from that guard. The codec version/spec checks and
ordinary source pin remain distinct from an unperformed portability proof.

## 5. Source, test, provenance and build support identities

The following exact-byte identities bind the supporting excerpts and retained
public records. Hash identity does not mean a whole supporting file received the
same every-member review as the ten primary bodies. Whole tests and the AST
intake README/export/qualification records were read; larger caller/build/policy
bodies were read only for the stated boundary.

| Supporting source or public record | Lines / bytes | Git blob SHA-1 | SHA-256 |
| --- | --- | --- | --- |
| [m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexAST.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexAST.java) | 278 / 8133 | `4a58f248301dc2b74f343fd1280e0e5deb87a1df` | `4867c08790906ad0acdaaf878ede37d9d33a5bf7e24e4faff737d584b960b003` |
| [m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTDocument.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTDocument.java) | 267 / 9220 | `3742876cabaab02119539b08327d75e51c32fce5` | `32859a872cc1cb600177c140926aff63df7d8698efd928d9eab635c001fcf4a8` |
| [m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPool.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPool.java) | 724 / 24458 | `1e3dcb323345407d8dbdb21fb22d9af7e6a9ca8b` | `1ca4f5a6428257e6346cdb4bd4da78f76755889d236fd6d5196895efdea79eff` |
| [m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTParser.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTParser.java) | 370 / 13575 | `b9874be52019114aed75342b767bb4eeb514ce40` | `d250b33d84b597284a84255c3d52b1ecfb8f2338eea27377643bb651acba7b90` |
| [m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPartialCache.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPartialCache.java) | 1038 / 36255 | `69d8846b6d6c22302edd18ce5430511e3c556653` | `68a21dea8a4b30a7fcc37f0277e1841f1dc4fba229a88192342542eff8f23f43` |
| [m3/vendor/synexia/synexia-indexstring/src/test/java/com/synexia/indexstring/ast/MIndexASTPrecomputeTest.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/test/java/com/synexia/indexstring/ast/MIndexASTPrecomputeTest.java) | 272 / 10081 | `13bb16a6e6731d6604b676e4e3328dd60ff27f1b` | `d399496e53b0a758ed5a06abca4aaac4ca0a9b8d239cc5571b65d4d8b983e4fa` |
| [m3/vendor/synexia/synexia-indexstring/src/test/java/com/synexia/indexstring/ast/MIndexASTLcaCodecTest.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/test/java/com/synexia/indexstring/ast/MIndexASTLcaCodecTest.java) | 71 / 2559 | `bc961ddbd08380fe6bb1bb8955378b71a1542903` | `60c7a9617c5291abf727940da0a6896724520a7a3ae11ebc600310fdfb6b7822` |
| [m3/vendor/synexia/synexia-indexstring/src/test/java/com/synexia/indexstring/ast/MIndexASTCodecBoundedReadProof.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/test/java/com/synexia/indexstring/ast/MIndexASTCodecBoundedReadProof.java) | 188 / 7010 | `4c5d1996d43ff68db4ba4bc8576fc82108cb8f7c` | `feb671ea19431f4c8b146e521a5029c1382815cd562be1813470a902f135d7ea` |
| [m3/synexia-import/intakes/ast-navigation-20261007/README.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/ast-navigation-20261007/README.md) | 12 / 1859 | `89083d28fa631253a452224ddbe0d5aa81c63ea7` | `d1ffddc299cc975de0ccaa6bd503bc49ffab03cd78ef77a53173e36960f42a29` |
| [m3/synexia-import/intakes/ast-navigation-20261007/export.tsv](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/ast-navigation-20261007/export.tsv) | 46 / 15240 | `3940385926917448c9777ffe94f57be4ce45f5ac` | `860eade0c05a175373e3f96a19150de03f4865efdfcf077af5d5a52d15a471b7` |
| [m3/synexia-import/intakes/ast-navigation-20261007/evidence/QUALIFICATION.json](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/ast-navigation-20261007/evidence/QUALIFICATION.json) | 56 / 2048 | `4d54c1449f4189264d84f1eda27a9bacfd591370` | `597e119d7fd3375a0c6fd8ddf88c1f9a6c32932c01ed76f32e00ea14023a1dab` |
| [m3/vendor/synexia/README.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/README.md) | 79 / 3492 | `e9a7f0159de1e2c069662455202065a1c93fb522` | `95317a8e1f02ce87776d4b042781ffbbd3a8f38321ff7ed30e1891a51a315ca7` |
| [m3/vendor/synexia/NOTICE](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/NOTICE) | 303 / 14991 | `cf6694a1316785ba2aa45394314bbae2e856c7fe` | `31cec4f2c4936b0e7a71a77847a3c20b0959e119a02ef5cf537f4f707fff9427` |
| [m3/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/pom.xml) | 229 / 11214 | `693156006961717eccfc649585e724d67682b3df` | `15f0d5a0e942749392480372ff4b414949becb11370d1dbe25dbdda7dfb50209` |
| [m3/synexia-import/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/pom.xml) | 258 / 12210 | `6b3fa2948f055227790041553700f6abdef79eb5` | `d85d3d4ed6c2ed2b2c637d64805c5f9462e2157ba66019be15075ca1bafbddb0` |
| [m3/build.sh](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/build.sh) | 55 / 5412 | `5ac0b944e34bf5f226d77541adbcb28e35edcfe5` | `9119dec83045301a2166d0a1de3b1008dfd431eaf3b5317daa14dc7e41325807` |
| [m3/ports/indexstring/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/ports/indexstring/pom.xml) | 184 / 10841 | `d3fb14b11cf2cf76185c02ea1ffac82167356ba6` | `d07cf94116968269d1367e6e4ad41c6bc847bb9410ade7ff542255a74d2f346b` |
| [m3/docs/m3jdk21-porting-invariant.md](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/m3jdk21-porting-invariant.md) | 499 / 30964 | `f65cc98e9088377755c51a447ce974356b28f15c` | `fa84270f09a42fe4dbeb245812e7b710297825e2bfd461bce99bb64056e1399d` |

The [preserved NOTICE][notice] records architectural donor acknowledgments for
AST precompute; its original descriptions and license distinctions remain
unchanged. No new legal grant, relicensing, third-party endorsement or claim of
verified unpublished donor bodies is made by this source map.

## 6. Historical receipt and build-route disposition

The public qualification dated `2026-10-07T07:00:18.452661+00:00` declares
`BOUNDED_HOST_AST_ORACLES_AND_EXACT_ISOLATED_BYTE_INTAKE_ONLY`. It reports four AST
methods, 21 importer unit tests and eight byte-intake cases over 44 payloads
(37 production, five test/oracle, two license/notice). The intake README states
41 previously strict-compiled classes plus a sealed inherited host. These are
historical claims of that receipt, not a source-complete build recipe for the
current retained directory.

The receipt explicitly marks portable source-complete receiver build, actual
target consumer wiring, valid-checksum/wrong-geometry refusal, cache/edit/resident
lifecycle, full seed tests, Maven verify/JaCoCo gates, native providers, matched
M3JDK/jtreg/platforms, performance and full-family completion as unqualified.
The four AST methods do not cover the two absent-codec tests just because all
five test/oracle files were included in byte custody.

- The M3 reactor's module list points to the separate import verifier and other
  control modules; it does not declare this vendor source directory a module
- `m3/synexia-import/pom.xml` defines an import-verifier artifact with its own
  compiler, test and coverage setup; those settings are not evidence the retained
  AST tree was compiled or measured
- `m3/build.sh` compiles `core/src` and named foundation tests. Reading the script
  does not run it or select this retained AST source family
- `m3/ports/indexstring/pom.xml` defines the separate Java-21 retained-view bridge
  and its two main-class tests; it is not the imported AST module
- The public ownership/porting policy keeps Synexia source custody separate from
  JDK runtime integration and preserves the String → arrays → collections →
  AST/compiler receiving sequence. No phase was advanced by these Markdown pages

## 7. Completion boundary

Complete here: source-body and declared-member documentation for the ten listed
owners, exact hashes, supporting caller boundaries, three complete test-source
reads, dated evidence interpretation and explicit source/runtime limitations.

Open: independent executable behavioral coverage, compilable external dependency
closure, every sibling package/module body, full hostile-image validation,
parser/compiler semantic equivalence, editing behavior, resource/portability and
concurrent-use proof, native and matched-runtime qualification. All larger-scope
completion claims must retain those open gates.

[atom-support]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexAST.java#L207-L248
[automaton-access]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTKindAutomaton.java#L122-L144
[automaton-compile]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTKindAutomaton.java#L21-L120
[automaton-identity]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTKindAutomaton.java#L175-L223
[automaton-match]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTKindAutomaton.java#L225-L230
[automaton-scan]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTKindAutomaton.java#L146-L173
[codec-constants]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPrecomputeCodec.java#L25-L34
[codec-io]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPrecomputeCodec.java#L206-L244
[codec-read]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPrecomputeCodec.java#L86-L171
[codec-sha]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPrecomputeCodec.java#L247-L253
[codec-validation]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPrecomputeCodec.java#L173-L204
[codec-write]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPrecomputeCodec.java#L36-L84
[diff-compare]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTDiff.java#L29-L56
[diff-types]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTDiff.java#L7-L27
[document-support]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTDocument.java#L31-L265
[export]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/ast-navigation-20261007/export.tsv
[lca-api]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTLcaSparseTable.java#L196-L235
[lca-build]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTLcaSparseTable.java#L94-L163
[lca-restore]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTLcaSparseTable.java#L165-L194
[lca-state]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTLcaSparseTable.java#L13-L92
[lca-validate]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTLcaSparseTable.java#L237-L347
[lca-visit]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTLcaSparseTable.java#L349-L360
[nav-access]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPrecompute.java#L561-L599
[nav-build]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPrecompute.java#L429-L559
[nav-copy-restore]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPrecompute.java#L211-L256
[nav-path]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPrecompute.java#L641-L679
[nav-posting-check]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPrecompute.java#L401-L426
[nav-search]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPrecompute.java#L681-L717
[nav-snapshot-accessors]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPrecompute.java#L142-L209
[nav-snapshot-type]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPrecompute.java#L86-L140
[nav-state]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPrecompute.java#L16-L78
[nav-tail]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPrecompute.java#L719-L767
[nav-tree]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPrecompute.java#L601-L639
[nav-validate]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPrecompute.java#L258-L399
[notice]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/NOTICE
[parser-source]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTParser.java#L66-L187
[pattern-create]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTKindPattern.java#L9-L40
[pattern-search]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTKindPattern.java#L42-L77
[pattern-tail]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTKindPattern.java#L79-L90
[pool-access]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPoolPrecompute.java#L496-L541
[pool-build]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPoolPrecompute.java#L95-L185
[pool-equality]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPool.java#L289-L723
[pool-groups-find]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPoolPrecompute.java#L543-L588
[pool-identity-key]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPoolPrecompute.java#L484-L494
[pool-planning]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPoolPrecompute.java#L590-L649
[pool-remap-identity]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPoolPrecompute.java#L377-L417
[pool-restore]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPoolPrecompute.java#L227-L375
[pool-search-support]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPoolPrecompute.java#L651-L699
[pool-snapshot]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPoolPrecompute.java#L187-L218
[pool-snapshot-type]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPoolPrecompute.java#L44-L60
[pool-sort]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPoolPrecompute.java#L700-L762
[pool-state]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPoolPrecompute.java#L19-L93
[pool-support]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPool.java#L23-L225
[pool-verify]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPoolPrecompute.java#L418-L482
[qualification]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/ast-navigation-20261007/evidence/QUALIFICATION.json
[query-builder-filters]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTQuery.java#L203-L245
[query-builder-labels]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTQuery.java#L149-L201
[query-builder-ranges]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTQuery.java#L247-L285
[query-index]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTQuery.java#L113-L147
[query-match]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTQuery.java#L65-L111
[query-state]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTQuery.java#L14-L63
[resident-upgrade]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPartialCache.java#L210-L385
[resident-weight]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPartialCache.java#L387-L420
[reuse-candidates]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTReuseKey.java#L78-L108
[reuse-classify]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTReuseKey.java#L110-L150
[reuse-from]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTReuseKey.java#L43-L75
[reuse-types]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTReuseKey.java#L13-L41
[source-api]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSourceIndex.java#L77-L134
[source-bounds]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSourceIndex.java#L167-L199
[source-build]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSourceIndex.java#L14-L75
[source-restore]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSourceIndex.java#L136-L165
[source-sort]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSourceIndex.java#L201-L279
[test-bounded]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/test/java/com/synexia/indexstring/ast/MIndexASTCodecBoundedReadProof.java#L1-L188
[test-lca]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/test/java/com/synexia/indexstring/ast/MIndexASTLcaCodecTest.java#L1-L71
[test-precompute]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/test/java/com/synexia/indexstring/ast/MIndexASTPrecomputeTest.java#L1-L272
[test-precompute-helpers]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/test/java/com/synexia/indexstring/ast/MIndexASTPrecomputeTest.java#L228-L271
