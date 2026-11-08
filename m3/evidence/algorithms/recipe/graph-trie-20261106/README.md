<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# m3/algorithms: graph and trie families (leaf graph-trie-20261106)

Second leaf of the m3scale convergence plan (`C:/m3-superset/conv/plans/m3scale.tsv`, rank 2
`m3-algorithms-graph-trie`, decision ABSORB) into the existing module `org.m3jdk.algorithms` (package
`jdk.m3.algorithm`, Maven `com.m3:m3-algorithms:0.1.0-SNAPSHOT`). Recipe, map and gates follow
`recipe/families-20261106` (leaf 1) byte for byte where they can; differences are listed below.

## What it adds

| Type | Contract |
|---|---|
| `M3GraphAlgorithms` | nested record `WeightedEdge<V>(V to, long weight)` (refuses a negative weight); `breadthFirst`, `shortestPaths` (unweighted, `Map<V,Integer>`), `topologicalOrder` (Kahn with a comparator; refuses a directed cycle with `IllegalArgumentException`), `dijkstra` (`Map<V,Long>` of reachable vertices: a path of exactly `Long.MAX_VALUE` is a distance, not the absent sentinel; MAX followed by a positive edge is skipped; a representable predecessor plus an edge that overflows still throws `ArithmeticException` via `Math.addExact`), `stronglyConnectedComponents` (Kosaraju, iterative, components sorted by the comparator). Every loop observes `M3ProgressMonitor` |
| `M3StringTrie` | code-point trie over `Map<Integer,Node>`: `insert` (atomic: the new branch is attached only after the last code point was ticked and the terminal set, so a cancelled or failing monitor publishes no ghost prefix), `contains`, `startsWith`, `remove`, `wordCount`; an explicit `public M3StringTrie()` (the donor's implicit constructor, see INSERT_AFTER) |

Oracles relocated as tests: `M3GraphDijkstraLongMaxDistanceTest` (4 of 5 donor methods),
`M3DijkstraLongMaxReachabilityTest` (4), `M3TrieAtomicInsertTest` (5). Differential driver (test scope, M3JDK-authored,
not a test): `src/test/java/jdk/m3/algorithm/M3GraphTrieDump.java`.

## Owner checks (before porting)

| Family | Search on master f9430bc190c | Verdict |
|---|---|---|
| union-find (`IntUnionFind`, blob `a254fd158850`) | `git ls-files \| grep -i 'DisjointSet\|DisjointIndex\|UnionFind'`: `jdk.m3.collections.M3IntDisjointSet` (blob `301e06fa76e7`, programme leaf 1), `M3DisjointIndex` (blob `c8296043b571`), cpu-tools `M3RollbackUnionFind` | **ALREADY** (not ported; SKIP + NEC row). `unionfind_differential.sh`: 1,000,000 seeded ops (union 50 %, connected 20 %, find 15 %, componentSize 10 %, componentCount 5 %, 1 % out-of-range probes) over 101 sequences of sizes 0..98,473; per-op dumps of every partition observation plus a canonical-labelling sha256 per sequence, byte-identical: sha256 `3974870afe23f6ac18b5813ba80f999c498bad762a06740d910a3a5c234b765e` both sides; `first_partition_difference=none`. Representatives were compared in process only: 150,123 finds, 0 mismatches (both are union by size with the same tie-break; full compression vs path halving does not change roots). NEC row text: "IntUnionFind -> M3IntDisjointSet (partition-equivalent, representatives differ: no)" |
| trie | `git grep -n -i trie -- m3 src/java.base/share/classes/jdk/internal/m3 ':!**/target/**'`: `jdk.m3.collections.M3TreePathTrie` (blob `043c2bb63dc1`, int segment paths to int ids with flag lanes), `M3PatriciaTrie` (blob `8f89c1dbb739`, scaffold, every method throws), `M3LongPatriciaTrie` (blob `fc76bf4a4700`, crit-bit over long keys); nothing in java.base `jdk/internal/m3` | **ABSORB + NEC**: no code-point String trie with insert/contains/startsWith/remove/wordCount; `StringTrie` relocated as `M3StringTrie`, NEC row names the three owners with blob ids |
| graph | `git grep -n -i -E 'dijkstra\|topological\|StronglyConnected\|Tarjan\|Kosaraju'`: only `m3/tooling/module-packs/cpu-tools` (`TopologicalOrder` blob `86119c6acae8`, `ShortestPaths` blob `429772cec77d`, `M3ShortestPaths` blob `a20e8ff49798` with `UNREACHABLE = Long.MAX_VALUE`), guava copies (doc mentions), catalogue TSVs | **ABSORB + NEC**: int-vertex tool shapes returning arrays, no generic `V`, no monitor, no BFS/SCC, and the MAX sentinel the donor's `dijkstra` removes; `GraphAlgorithms` relocated as `M3GraphAlgorithms`, NEC row names the four tool owners |

## Donor and relocation (recipe, never by hand)

| Item | Value |
|---|---|
| donor | `hsoliwal/synexia-m3scale-apache2` develop `3385d79a2968c770aedf1d3398b2a62f05ebc092` (2026-11-06; `git fetch`: develop unchanged); graph fix 53dbec7a + ac965fb8 (2026-11-05), trie fix 9016b968 + 8181b2b3 (2026-11-05) |
| donor files | `GraphAlgorithms.java` blob `c5ac13fda361`, `StringTrie.java` blob `efb4d9e2cdbc`, tests `GraphDijkstraLongMaxDistanceTest` `96b87962b6db`, `DijkstraLongMaxReachabilityTest` `727aab26183e`, `TrieAtomicInsertTest` `0d6166040baa`; full shas, sha256 and URLs in `PROVENANCE.tsv` |
| recipe | `relocate.py` v1 (`RELOCATE_ALGORITHM_GRAPH_TRIE`, lineage: `families-20261106/relocate.py` v1): `python -I recipe/graph-trie-20261106/relocate.py --donor-git <clone> --donor-rev <rev> [--check]` |
| map | `RELOCATION_MAP.tsv`: 1 `REPO`, 5 `FILE`, 2 `OWNED`, 2 `PACKAGE`, 7 `TYPE`, 1 `EXTERNAL`, 1 `DROP_METHOD`, 1 `INSERT_AFTER`, 3 `SKIP`, 3 `NEC` |
| provenance | `body_match=TRUE` 5/5; renamed lines 11/8/11/16/7, 1 mapped import each, 9 dropped lines (one oracle method), 4 inserted lines (the constructor block) |
| naming | `python -I m3/tooling/jdk-naming/jdk_naming.py apply --root . --only m3/algorithms/`: 0 moves, 0 rewrites, 0 refusals; fixed point 0/0 |

Differences from the families recipe (each recorded in `RECIPE_MEMORY.tsv`):

* `OWNED` row kind: `AlgorithmSupport` and the progress monitor are owned by earlier recipes (families-20261106,
  progress-monitor-unify-20261106); the recipe never writes them, `differential.sh` compiles their donor sources on the
  donor side only.
* `INSERT_AFTER` row kind: `StringTrie` has no explicit constructor; `-Xlint:all -Werror` refuses that for a public
  class of the exported package (`missing-explicit-ctor`, first build). The row inserts the 4-line block (anchor comment
  plus `public M3StringTrie() {}`) after the one line `private int words;`; `body_match` strips declared blocks before
  comparing; javap parity shows the same `public M3StringTrie();` the donor's implicit constructor has.
* `DROP_METHOD GraphDijkstraLongMaxDistanceTest.positiveWeightExtensionKeepsExistingExactArithmeticOverflowRefusal`:
  the donor suite is red at the pinned commit. The method (53dbec7a) expects `ArithmeticException` for MAX followed by a
  positive edge; the body (ac965fb8) and the sibling oracle
  `DijkstraLongMaxReachabilityTest.newlyReachedMaximumMustNotIntroduceAnOverflowException` require `{0=0, 1=MAX}` with
  no exception. The body is kept; the stale method is dropped by the recipe (second build: 29 tests, 1 failure, exactly
  this method). Donor follow-up: update or delete it upstream.

## Gates (2026-11-06, see GATES.tsv)

| Gate | Result |
|---|---|
| baseline (master f9430bc190c) | `mvn package` BUILD SUCCESS, 15 tests (6+3+3+3), 24 class files |
| 1 `mvn package`, JDK 21.0.9, `-Xlint:all -Werror` | BUILD SUCCESS, 0 WARNING lines; 28 tests, 0 failures (15 baseline + 13 relocated: 4 + 4 + 5) |
| 2 differential donor vs relocated | 218,560 cases: 100,000 graph cases (20,000 per method; 99,008 graphs with 1..12 vertices, 888 with 13..200, 104 with 201..2,000; max 1,915 vertices / 14,886 edges; 1,893,988 edges in all) + 100,000 trie ops + 18,560 prefix probes after failed inserts; 25,593 exceptions, 0 timeouts; dumps identical, sha256 `59954d0f9f92dfa47363c8e3abeab10ede067de0350440c1b8303a668c25c503`; both runs in 23 s |
| 3 `javap -p` parity | 6 of 6 class files identical modulo names (`M3GraphAlgorithms`, `$1Frame`, `$1QueueEntry`, `$WeightedEdge`, `M3StringTrie`, `$Node`) |
| 4 families differential (leaf 1) | PASS, 1,100,019 cases, sha256 `f85280ac94b2fc25bf7f10ac17193bf7b788dc1aea731a6a2b4c3da04fc392f9` unchanged |
| 5 untouched class files | 24 of 24 master class files byte-identical (module-info included); 6 new |
| 6 `jdk_naming.py apply --only m3/algorithms/` | 0/0/0, fixed point 0/0 |
| 7 scope | `m3/algorithms/**` plus the three ledger rows; `m3/pom.xml` untouched |
| owner check union-find | PASS (partition-equivalent, representatives differ: no), see above |

## Hard-limit notes (recorded, not fixed)

Donor bodies use loops (15 `for`/`while` lines in `M3GraphAlgorithms`, 4 in `M3StringTrie`) and exceed 15 lines per
method (`stronglyConnectedComponents` 73, `topologicalOrder` and `dijkstra` 37, `insert` 34, `remove` 31). The
generic `Map<V, Collection<V>>` API allocates (boxed keys, `HashMap`, `PriorityQueue` entry records, one `HashMap`
per trie node): HP-LAW / DOOP review in a later pass. The M3JDK-authored driver and the union-find program are test
tooling with loops as well.

## Open items

* The catalog `M3AlgorithmShapeCatalog` (owned by families-20261106) still names the entry points
  `GraphAlgorithms.*`, `StringTrie.insert` and `IntUnionFind.union` as strings. Renaming them is a TYPE row in the
  families map; it changes `M3AlgorithmShapeCatalog.class` and the families digest, so it is a separate leaf.
* `M3AlgorithmFamiliesTest.graphDsuAndTrieShapes` stays dropped (families DROP_METHOD): it also exercises
  `IntUnionFind`, which is not ported.
* Donor follow-up: the stale oracle method above (donor suite red at `3385d79a2968`).
* NEC rows: one union-find owner with or without a monitor facade; tool-shape graph owners vs `M3GraphAlgorithms`;
  lane-based trie owners vs `M3StringTrie`.

## Files

| File | Purpose |
|---|---|
| `../../src/main/java/jdk/m3/algorithm/M3GraphAlgorithms.java`, `M3StringTrie.java` | the relocated families |
| `../../src/test/java/jdk/m3/algorithm/M3{GraphDijkstraLongMaxDistance,DijkstraLongMaxReachability,TrieAtomicInsert}Test.java` | the relocated oracles |
| `../../src/test/java/jdk/m3/algorithm/M3GraphTrieDump.java` | reflective dump driver of gate 2 |
| `relocate.py`, `RELOCATION_MAP.tsv`, `PROVENANCE.tsv` | recipe, map, pins |
| `differential.sh`, `javap_parity.sh`, `unionfind_differential.sh` + `UnionFindPartitionDump.java`, `verify.sh` | gates; `verify.sh <donor clone> <rev> <scratch>` runs them all, the families differential included |
| `GATES.tsv`, `RECIPE_MEMORY.tsv` | gate record and lessons |
