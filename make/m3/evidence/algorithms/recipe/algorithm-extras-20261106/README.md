<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# m3/algorithms: the m3scale src algorithms extras (leaf algorithm-extras-20261106)

Third algorithms leaf of the m3scale convergence plan (`C:/m3-superset/conv/plans/m3scale.tsv`, rank 5, decision
ABSORB) into the module `org.m3jdk.algorithms` (package `jdk.m3.algorithm`). The donor is the older, broader catalogue
`src/main/java/com/synexia/m3scale/algorithms` of `hsoliwal/synexia-m3scale-apache2` develop
`3385d79a2968c770aedf1d3398b2a62f05ebc092` (21 files, 4,894 LOC, last substantive change 9af4ac4e 2026-09-25,
restored 99f80871 2026-10-27; built upstream only through the root monolith pom); gates run on master a33b19ba618 and again on c7f779bc370 after the rebase over #2551 (min-cost flow). Only the members that the landed
families (`recipe/families-20261106`, #2509) and graph-trie (`recipe/graph-trie-20261106`, #2535) leaves and the other
M3JDK owners do not already answer are relocated; per-method NEC, the landed body wins on overlap.

## Census (`METHOD_CENSUS.tsv`, checked by `census.py`)

Rule P1 (problem identity): OVERLAP_LANDED when a landed `jdk.m3.algorithm` method answers the same catalogue problem
(the answer may differ in container or representation and in the trailing monitor); OVERLAP_OTHER_OWNER when another
built owner does (`jdk.m3.collections`, java.base `jdk.internal.m3`, the cpu-tools module pack); ABSORB otherwise. A
derivable answer to a different question (count vs enumerate, exists vs locate, first vs every occurrence) is a
different problem; named-algorithm entries compare by algorithm. A non-ABSORB member stays only as closure (a kept
member calls it, or it is an operation of a structure relocated as a unit).

| Rows | ABSORB | OVERLAP_LANDED | OVERLAP_OTHER_OWNER |
|---|---|---|---|
| 156 public static methods (javap -public) | 79 ported | 72 (70 dropped, 2 kept as closure: `StringAlgorithms.prefixFunction`, `LinkedListAlgorithms.cycleStartIndex`) | 5 (3 dropped: `dinicMaxFlow`, `hopcroftKarp`, `IntAlgorithms.singleNumber`; 2 kept as closure: `suffixArray`, `lcpArray`) |
| 25 structure members and nested types | 17 ported | 6 (3 PrimitiveTrie operations kept as a structure unit; `SccResult`, catalog types dropped/not ported) | 2 dropped (`FenwickTree` -> `jdk.m3.collections.M3LongFenwickTree`, `MatchingResult`) |

The four catalog statics (`AlgorithmCatalog.descriptors/require`, `AlgorithmShape.values/valueOf`) are among the 70:
the catalog files are not ported (one catalog per module: the landed `M3AlgorithmShapeCatalog`; SKIP + NEC rows).

## Classes added (ABSORB members; closure in brackets)

| Class (donor) | Members |
|---|---|
| `M3PrimitiveArrayAlgorithms` (IntAlgorithms; not a second M3IntAlgorithms) | binarySearch x2, nextGreaterCircular, maxProductSubarray, maxProfitOneTransaction, majorityElement, countBits, findMinRotated, findDuplicateFloyd, containerMostWater, moveZeroes, rotateRight |
| `M3SortingAlgorithms` | mergeSort, heapSort, radixSortSigned, countingSort (int[] only) |
| `M3RangeQueryStructures` | SegmentTreeSum, SparseTableMin, RangeAdder |
| `M3StreamingStructures` | IntMinStack, MedianTracker, MovingAverage, KthLargestStream |
| `M3NumericAlgorithms` | gcd, lcm, modPow, modInverse, isPrime, sievePrimes, smallestPrimeFactors, primeFactorization, binomial, fibonacci, catalan |
| `M3AdvancedGraphAlgorithms` | zeroOneBfs |
| `M3AdvancedStringAlgorithms` | manacherOdd, manacherEven, longestRepeatedSubstring [suffixArray, lcpArray] |
| `M3LinkedListAlgorithms` | record IntLinkedList (+linear), hasCycle, toArray, reordered [cycleStartIndex] |
| `M3PrimitiveTrie` | constructor, nodeCount, edgeCount, longestStoredPrefixLength [add, contains, hasPrefix] |
| `M3PrimitiveGraphAlgorithms` (GraphAlgorithms) | records CsrGraph, WeightedCsrGraph, EdgeList, MstResult (+4 factories); dfsPreorder, connectedComponents, isBipartite, bellmanFord, unionFindComponents, hasUndirectedCycle, kruskalMst |
| `M3PrimitiveTreeAlgorithms` (TreeAlgorithms) | record BinaryTree; preorder, postorder, diameterEdges, lowestCommonAncestorBstByValue, maxPathSum, inverted |
| `M3PrimitiveDynamicProgrammingAlgorithms` | climbStairs, minCostClimbingStairs, coinChangeWays, minPathSum, targetSumWays, decodeWays |
| `M3PrimitiveBacktrackingAlgorithms` | combinations, combinationSum, nQueensCount, solveSudoku |
| `M3PrimitiveMatrixAlgorithms` | floodFill, shortestPathBinaryMatrix, maximalSquareArea, deepCopy |
| `M3PrimitiveStringAlgorithms` | minimumWindow x2, kmpIndexOf x2, rabinKarpIndexOf, isAnagram, isPalindromeIgnoringNonAlphanumeric, longestCommonPrefix, reverseWords [prefixFunction] |
| `M3PrimitiveBitAlgorithms` | isPowerOfTwo, addWithoutPlus, rangeBitwiseAnd, twoSingleNumbers |
| `M3PrimitiveAlgorithmSupport` (package-private) | LongIntMinHeap, DisjointSet (closure of the kept graph members) |

The `M3Primitive*` names: the landed names (`M3GraphAlgorithms`, `M3TreeAlgorithms`, ...) are taken and the donor
catalogue is the primitive-lane variant of those families (int[]/CSR/array-backed records, no boxing). Oracles added
(M3JDK-authored; the donor has no tests beyond a plain-main self-test): `M3SortingRangeStreamingReferenceTest`,
`M3NumericReferenceTest`, `M3GraphStringReferenceTest`, `M3ArrayTreeDpReferenceTest`; differential driver (test scope,
not a test) `M3AlgorithmExtrasDump`.

## Recipe (never by hand)

| Item | Value |
|---|---|
| recipe | `relocate.py` v1 (`RELOCATE_ALGORITHM_EXTRAS`, lineage `graph-trie-20261106/relocate.py` <- `families-20261106/relocate.py`): `python -I recipe/algorithm-extras-20261106/relocate.py --donor-git <clone> --donor-rev <rev> [--check]` |
| differences from the lineage | DROP_METHOD may select one overload (`Type.method(type,...)`); a dropped declaration takes its leading Javadoc; new row kind DROP_TYPE (nested type); the declaration matcher refuses a call statement (type must start with a word character and not be `return`/`new`/...) |
| map | `RELOCATION_MAP.tsv`: 1 REPO, 17 FILE, 1 OWNED, 2 PACKAGE, 18 TYPE, 1 EXTERNAL, 81 DROP_METHOD, 7 DROP_TYPE, 9 DROP_LINE (imports only dropped members used), 2 INSERT_AFTER (explicit constructors of IntMinStack and MedianTracker, missing-explicit-ctor), 2 SKIP, 5 NEC |
| provenance | `PROVENANCE.tsv`: body_match=TRUE 17/17 (every kept line a donor line or its renamed form) |
| ledger | `converge_rows.py`: 21 rows in `make/m3/qualification/converge-collections-20261030/CONVERGE_COLLECTIONS.tsv` (PRESENT_IN_M3JDK 75 -> 96); re-run after a rebase |
| naming | `jdk_naming.py apply --only m3/algorithms/`: 0 moves, 0 rewrites, 0 refusals; fixed point 0/0 on the committed tree (the recipe scripts read donor package and repository names from `RELOCATION_MAP.tsv`; `OverlapAgreement.java` is a `DONOR_PACKAGE` template) |

## NEC observations (`OVERLAP.tsv`, landed or owner body wins)

| Donor member vs owner | Disagree / cases | Cause |
|---|---|---|
| IntAlgorithms.twoSumIndices vs M3ArrayAlgorithms.twoSum | 14 / 2,000 | donor accepts an int-overflow-wrapped complement; landed refuses it (fd5f4479) |
| IntAlgorithms.canJump / minJumps vs canReachEnd / minimumJumps | 7 / 2,000, 5 / 2,000 | donor `i + jumps[i]` wraps at Integer.MAX_VALUE; landed long frontier (6f2f987f, cddc48a0) |
| IntAlgorithms.trapRainWater | 50 / 2,000 | heights at Integer.MIN_VALUE/MAX_VALUE (outside LC 42's domain) |
| IntAlgorithms.singleNumber vs java.base singleNumberOfPairs | 362 / 2,000 | owner refuses an even length, donor returns the XOR |
| GraphAlgorithms.dijkstra vs M3GraphAlgorithms.dijkstra | 36 / 2,000 | donor long[] keeps the Long.MAX_VALUE sentinel: a vertex reachable at exactly MAX is reported unreachable (53dbec7a + ac965fb8) |
| AdvancedStringAlgorithms.zFunction | 1,902 / 2,000 | convention: donor z[0] = n, landed z[0] = 0 |
| StringAlgorithms.longestSubstringWithoutRepeating | 83 / 2,000 | UTF-16 units vs code points |
| StringAlgorithms.validParentheses vs validBrackets | 92 / 2,000 | donor refuses other characters (IllegalArgumentException), landed answers false |
| BacktrackingAlgorithms.permutations | 437 / 2,000 | donor skips repeated values (LC 47 unique permutations), landed lists position permutations (LC 46) |
| PrimitiveTrie vs M3StringTrie (kept) | 85 / 2,000 | UTF-16 units vs code points (a probe ending inside a surrogate pair) |
| FenwickTree vs M3LongFenwickTree | 130 / 2,000 | the owner's add is failure-atomic on overflow, the donor's is not |
| knapsack01 vs cpu-tools M3KnapsackSelection | 138 / 2,000 | the tool refuses negative weights or values (the landed knapsack01 agrees 2,000 / 2,000) |
| catalog | 1 / 1 | donor 87 descriptors over 36 shapes; landed 51 shapes in 18 families |

Equivalent (both valid): topKFrequent 675 tie cases, topologicalSort 790 order differences. Every other overlap
row agrees on all cases (59 rows, 114,013 cases: 109,206 agree, 1,465 equivalent, 3,342 disagree in 14 rows).

## Findings on the relocated bodies (recorded, not fixed: bodies are never edited by the port)

dfsPreorder marks on push (order differs from recursive DFS preorder in 3,495 / 20,000 graphs; reachable set holds);
decodeWays wraps int without refusal and admits non-ASCII digits; nextGreaterCircular's -1 sentinel is also a value;
combinationSum([], t > 0) throws ArrayIndexOutOfBoundsException; SegmentTreeSum refuses on an internal-node overflow
although the queried sum fits; MovingAverage.add is not failure-atomic on overflow. Hard limits (15-line methods, the
loop ban) are recorded, not fixed: crude scan of the 17 relocated files: 2,767 lines, 54 declarations over 15 lines,
181 loop statements.
