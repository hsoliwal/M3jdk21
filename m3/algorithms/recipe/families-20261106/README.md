<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# m3/algorithms: canonical algorithm families (leaf families-20261106)

Module `org.m3jdk.algorithms` (package `jdk.m3.algorithm`), Maven coordinates `com.m3:m3-algorithms:0.1.0-SNAPSHOT`,
Java 21; `com.m3:m3-collections` (owner of the progress contract) is the only dependency outside `java.base`. First leaf of the m3scale convergence plan
(`C:/m3-superset/conv/plans/m3scale.tsv`, rank 1 `m3-algorithms-families`, decision ABSORB).

## What it is

Eleven static families, every loop observing the `M3ProgressMonitor` cancellation contract, plus the catalog that
names them:

| Type | Shapes |
|---|---|
| `M3ArrayAlgorithms` | twoSum, maxSubarray, productExceptSelf, longestConsecutive, sortColors, threeSum, trapRainWater, longestDistinctSubstring, maxSlidingWindow, subarraySumEqualsK, lowerBound, upperBound, searchRotated, topKFrequent, sortedTwoSum, minSubarrayLengthAtLeast, prefixSums, rangeSum, pivotIndex, firstTrue |
| `M3IntervalGreedyAlgorithms` | record `Interval`; merge, insert, eraseOverlapCount, canReachEnd, minimumJumps, gasStationStart |
| `M3DynamicProgrammingAlgorithms` | coinChange, longestIncreasingSubsequence, longestCommonSubsequence, editDistance, houseRobber, canPartition, wordBreak, uniquePaths, knapsack01 |
| `M3StringAlgorithms` | prefixFunction, kmpSearch, zFunction, longestPalindrome, wildcardMatch |
| `M3StackAlgorithms` | validBrackets, nextGreater, dailyTemperatures, largestRectangleArea, evaluateRpn |
| `M3HeapAlgorithms` | kthLargest, mergeSorted |
| `M3ListAlgorithms` | class `Node<T>`; reverse, middle, cycleStart, mergeSorted, removeNthFromEnd |
| `M3TreeAlgorithms` | class `Node<T>`; inorder, levelOrder, maxDepth, isBalanced, isValidBst, kthSmallest, lowestCommonAncestor |
| `M3MatrixAlgorithms` | spiralOrder, rotateClockwise, countIslands, setZeroes, searchSorted |
| `M3BacktrackingAlgorithms` | subsets, permutations, combinationSumUnique, nQueens, generateParentheses |
| `M3BitAlgorithms` | hammingWeight, reverseBits, singleNumberTriples, missingNumber (`singleNumber` dropped: `java.base` `jdk.internal.m3.M3IntAlgorithms.singleNumberOfPairs` owns it) |
| `M3AlgorithmShapeCatalog` | 51 `Shape` rows over 18 `Family` values (the GRAPH, UNION_FIND and TRIE rows name leaf-2 types by string) |
| `M3ProgressMonitor` | the donor contract: `int UNKNOWN`, `beginTask(String,int)`, `worked(int)`, `checkCanceled()`, every method default, `none()`. Since 2026-11-06 an adapter over `jdk.m3.collections.M3ProgressMonitor` (`m3/qualification/progress-monitor-unify-20261106`, int units widen to long); its map row is `SUPERSEDED` |

The pre-existing `M3PrefixZ` (`src/jdk/m3/algorithm`, a `java.base` `M3StringPiece` input, qualified by its own
workflow) is untouched and outside this Maven build.

## Donor and relocation (recipe, never by hand)

| Item | Value |
|---|---|
| donor | `hsoliwal/synexia-m3scale-apache2` develop `3385d79a2968c770aedf1d3398b2a62f05ebc092` (2026-11-06); freshest leaves fd5f4479 twoSum, c11cf8f0 longestConsecutive, 6f2f987f + cddc48a0 jump range (2026-11-05) |
| donor files | `m3-algorithms/src/main/java/com/synexia/m3scale/algorithm/*` (12 of 16 classes), `src/main/java/com/synexia/m3scale/progress/M3ProgressMonitor.java`, 4 of 8 tests; blob shas in `PROVENANCE.tsv` |
| recipe | `relocate.py` v1 (`RELOCATE_ALGORITHM_FAMILIES`): `python -I recipe/families-20261106/relocate.py --donor-git <clone> --donor-rev <rev> [--check]` |
| map | `RELOCATION_MAP.tsv`: 1 `REPO`, 17 `FILE`, 1 `SUPERSEDED` (the progress monitor, since 2026-11-06), 2 `PACKAGE` (`com.synexia.m3scale.{algorithm,progress} -> jdk.m3.algorithm`), 18 `TYPE` (M3 prefix, `M3ProgressMonitor` identity), 1 `EXTERNAL`, 2 `DROP_METHOD`, 2 `DROP_LINE`, 1 `DROP_DUMP`, 8 `SKIP`, 2 `NEC` |
| provenance | `PROVENANCE.tsv`: donor commit, blob, sha256, URL, target sha256, counts of renamed / mapped / dropped lines, `body_match` per file |
| naming | `python -I m3/tooling/jdk-naming/jdk_naming.py apply --root . --only m3/algorithms/`: 0 moves, 0 rewrites, 0 refusals |

The recipe rewrites only the license header (M3JDK style), the `package` line, mapped `import` lines, the declared
type names (whole words, on every line, the catalog's entry-point strings included because they name these types),
applies the declared `DROP` rows, and inserts one `//FLLDCIM: RELOCATE` anchor (donor blob and commit; the repository
and path are in `PROVENANCE.tsv`, since no naming keep-span covers this repository) before each top-level type.
`body_match=TRUE` for all 17 files: read back from disk, every target line is the donor line at its position, that
line with the declared renames, the mapped package or import line, or an explained insertion, and every missing
donor line is a declared drop. Idempotent (`--check` after the run and after `jdk_naming.py`: `changed=0`); refuses
an unmapped package or import instead of guessing the closure.

Drops, each with its owner or reason in the map: `BitAlgorithms.singleNumber` (14 lines, java.base owner) and the
catalog row `xor-single` that pointed at it; `AlgorithmFamiliesTest.graphDsuAndTrieShapes` (47 lines, needs the
leaf-2 types `GraphAlgorithms`, `IntUnionFind`, `StringTrie`) and its one `singleNumber` assertion.

## NEC rows (a human decides equivalence, nothing overwritten)

| Donor | M3JDK owner | Note |
|---|---|---|
| `StringAlgorithms.zFunction` (blob `a947a2c59855`) | `m3/algorithms/src/jdk/m3/algorithm/M3PrefixZ.java` (blob `317108234d76`) | both compute the Z array; donor over `CharSequence` into `int[]`, owner over a sealed `M3StringPiece` |
| `progress.M3ProgressMonitor` (blob `6d0b659af853`) -> `jdk.m3.algorithm.M3ProgressMonitor` | `jdk.m3.collections.M3ProgressMonitor` (blob `117b4eadd484`), `jdk.m3.file.M3ProgressMonitor` (blob `223cad76aa79`) | int / default / `none()` vs long / abstract / `state()`. `m3/collections` builds and installs from a sparse tree (probe, exit 0) but the donor bodies and oracles do not compile against it (30 javac errors), so the donor contract is carried byte for byte and one monitor owner is a decision, not a recipe step |

## Build

| Step | Command |
|---|---|
| compile, test, jar | `JAVA_HOME=<JDK 21> mvn -q -B -ntp -f m3/algorithms/pom.xml package` (`-Xlint:all -Werror`, `proc none`; surefire 3.5.4 runs the four oracles) |
| from the control reactor | `mvn -f m3/pom.xml -pl algorithms package` (`algorithms` is a default module of `m3/pom.xml`) |
| differential | `bash recipe/families-20261106/differential.sh <donor clone> <rev> <scratch> [cases]` after `package`: compiles the donor families from the donor tree, replays the same seeded inputs through both sides with `M3AlgorithmFamiliesDump`, normalizes the donor dump by the map (type names inside messages and catalog strings, the dropped catalog element) and compares byte for byte |
| all gates | `bash recipe/families-20261106/verify.sh <donor clone> <rev> <scratch> [cases]` |

Consumers depend on the module with

```xml
<dependency>
  <groupId>com.m3</groupId>
  <artifactId>m3-algorithms</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

and `requires org.m3jdk.algorithms;` in their module descriptor.

## Gates (2026-11-06, see GATES.tsv)

| Gate | Result |
|---|---|
| 1 `mvn package`, JDK 21.0.9, `-Xlint:all -Werror` | BUILD SUCCESS |
| 2 four donor oracles in the test phase | 15 tests, 0 failures; asserted figures 216,472 two-sum, 97,449 longest-consecutive, 127,656 jump-range cases; families test 5 of 6 methods + catalog coverage |
| 3 differential donor vs relocated | 1,100,019 cases (100,000 per family x 11 + 102 catalog elements) over 92 methods, 158,503 identical exceptions, 0 timeouts; normalized donor dump sha256 `f85280ac94b2fc25bf7f10ac17193bf7b788dc1aea731a6a2b4c3da04fc392f9` = relocated |
| 3b `javap -p` parity | 19 of 20 class files identical; `M3BitAlgorithms` differs only by the dropped `singleNumber` |
| 4 `jar --describe-module` | `org.m3jdk.algorithms` exports `jdk.m3.algorithm`, requires `java.base` |
| 5 `jdk_naming.py apply --only m3/algorithms/` | 0 moves, 0 rewrites, 0 refusals |
| 6 `CONVERGE_COLLECTIONS.tsv` | 14 owner rows added after the StringTrie row (which stays GAP for leaf 2) |
| 7 `m3/pom.xml` default module | `algorithms` after `compiler/lifetime-slots` |

Hard-limit notes for a later pass, recorded not fixed: donor bodies use loops throughout and several methods
exceed 15 lines (`M3ArrayAlgorithms` 526 lines); `HeapAlgorithms.mergeSorted` and the generic `Node<T>` families are
object-heavy (HP-LAW review later). Lessons of the run are in `RECIPE_MEMORY.tsv`.

## Files

| File | Purpose |
|---|---|
| `../../pom.xml`, `../../src/main/java/module-info.java` | the Maven module and its descriptor |
| `../../src/main/java/jdk/m3/algorithm/M3*.java` | the 12 relocated families and the catalog, `M3AlgorithmSupport`, `M3ProgressMonitor` |
| `../../src/test/java/jdk/m3/algorithm/M3*Test.java` | the four relocated oracles |
| `../../src/test/java/jdk/m3/algorithm/M3AlgorithmFamiliesDump.java` | reflective dump driver of the differential gate (not a test) |
| `relocate.py`, `RELOCATION_MAP.tsv`, `PROVENANCE.tsv` | the relocation recipe, its map and its pins |
| `differential.sh`, `verify.sh`, `GATES.tsv`, `RECIPE_MEMORY.tsv` | gates and their record |
