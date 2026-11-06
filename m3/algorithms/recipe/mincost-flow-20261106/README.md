<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# m3/algorithms: bounded min-cost flow (leaf mincost-flow-20261106)

`jdk.m3.algorithm.M3MinCostFlow` in module `org.m3jdk.algorithms` (Maven `com.m3:m3-algorithms:0.1.0-SNAPSHOT`),
relocated by recipe from the com.synexia leaf `M3CpuMinCostFlow`. Plan
`C:/m3-superset/conv/plans/native-compiler-openjdk.tsv` rank 4 `m3-cpu-mincost-flow`, decision ABSORB. One solver
only: the JDK owns one fast implementation, the other donor solver is oracle input of the differential and is not
ported.

## Contract (the donor CONTRACT.md, restated)

`M3MinCostFlow.solve(int vertices, int[] from, int[] to, int[] capacity, int[] unitCost, int source, int sink, int demand)`
returns `Result` with `flow()`, `cost()` (long, exact arithmetic) and `edgeFlows()` (a fresh caller-owned copy).

| Clause | Value |
|---|---|
| input | 2..64 vertices, 0..256 directed edges in four same-length columns, distinct source and sink, demand 0..1000, capacities 0..1000, unit costs 0..1,000,000 |
| accepted | parallel edges, directed cycles, zero-cost cycles, zero capacities, zero demand (flow 0) |
| refused (`IllegalArgumentException`) | self-loop, vertex out of range, negative or too large capacity or cost, demand out of range, source = sink, vertex count out of range, more than 256 edges, column length mismatch; a negative cost is refused, so a negative-cost cycle cannot be stated; `NullPointerException` names a null column |
| infeasible demand | not a refusal: the maximum feasible flow below the demand is returned (`flow() < demand`), at minimum cost |
| output | the maximum flow not exceeding the demand, with the minimum original-edge cost among flows of that value; conservation at every intermediate vertex; 0 <= edge flow <= capacity; inputs neither mutated nor retained |
| algorithm and storage | successive shortest augmenting paths, Bellman-Ford over the residual graph (reverse arcs carry negated costs and permit rerouting); CSR incidence and primitive residual lanes, no per-edge objects; O(F V E) time, O(V + E) space per solve |

## Solver decision (DECISION.tsv)

| Criterion | A `M3CpuMinCostFlow` (crate m3-cpu-mincost-flow-20261031) | B `LongMinCostFlow` (synexia-primitives) | Verdict |
|---|---|---|---|
| 1a donor oracle contract | `M3_MINCOST_FLOW_PASS checks=6671` JIT and -Xint | flow and cost equal A on 602/602 oracle instances, but no per-edge flow surface and a repeat call is incremental (165/602 repeats differ) | A |
| 1b the other donor's tests | none needed | none exist: the only `LongMinCostFlowTest` on develop tests the synexia-algo lineage (SKIP) | - |
| 2 contract width | 64 vertices, 256 edges, caps 1000, costs 0..1e6 | unbounded vertices, long caps and costs, negative costs without reachable negative cycles | B |
| 3 allocation per solve (10,000 solves) | 1,360 bytes, 12 objects | 1,754 bytes JIT (1,777 -Xint), 15 objects | A |
| agreement on the common contract | 100,602 / 100,602 standalone; 575,620 / 575,620 in gate 4 | | no NEC row |

Decision: port A as `M3MinCostFlow` (the donor name minus the `Cpu` token; the module names no execution unit) with
its oracle as `M3MinCostFlowOracle`; B is the SKIP row `differential oracle`, compiled from the donor tree into the
scratch directory of `differential.sh` and never committed. Criterion 2 is an open item (below).

## Donors (com.synexia develop `fd9a233680b43197eb8c5745a729c342ca56c4e2`, 2026-11-06)

| File | Blob | Last commit | Role |
|---|---|---|---|
| `synexia-openrewrite-recipes/crates/m3-cpu-mincost-flow-20261031/src/main/java/com/synexia/m3/cpu/M3CpuMinCostFlow.java` | `afa706282c5f` (sha256 `7d3ee7cdcc0c` = SOURCE_SEAL post) | be18ce20be8 2026-10-31 | FILE, chosen |
| `.../src/test/java/com/synexia/m3/cpu/M3CpuMinCostFlowOracle.java` | `1f0dcde31649` (sha256 `cfa98429d5df` = SOURCE_SEAL post) | be18ce20be8 2026-10-31 | FILE, oracle |
| `synexia-primitives/src/main/java/com/synexia/primitives/LongMinCostFlow.java` | `98d6b67790e7` | b1b66529904 2026-10-07 (header only; body fb33f92b07d9 of 6e99398f15a 2026-10-01) | SKIP, differential oracle |
| `synexia-primitives/.../LongPushRelabelMaxFlow.java` | `87b86a3b3025` | | SKIP, later leaf (max flow) |
| `synexia-algo/.../shapes/packed/LongMinCostFlow.java` + `LongMinCostFlowTest.java` | `d4913201462c`, `5245692b4d6d` | 2026-09-28 | SKIP, third lineage |
| `synexia-openrewrite-recipes/src/main/resources/.../m3-cpu-structured-wave9-20261031/M3MinCostFlow.java.txt` | `614f46eb2090` | 7a455139f0a 2026-10-31 | SKIP, template variant |

The retained variants `m3/qualification/master-landing-20261028/retained-variants/pr14891` and `pr14898` carry the
same blobs `afa706282c5f` / `1f0dcde31649`: one lineage. Full rows with notes are in `RELOCATION_MAP.tsv`.

## Recipe (never by hand)

| Item | Value |
|---|---|
| recipe | `relocate.py` v1 (`RELOCATE_MINCOST_FLOW`), derived by a declared byte edit from `../families-20261106/relocate.py` v1 (name, lineage note, `RECIPE_DIR_REL`): `python -I recipe/mincost-flow-20261106/relocate.py --donor-git <clone> --donor-rev <rev> [--check]` |
| map | `RELOCATION_MAP.tsv`: 1 `REPO`, 2 `FILE`, 1 `PACKAGE` (`com.synexia.m3.cpu -> jdk.m3.algorithm`), 2 `TYPE`, 7 `SKIP`, 0 `NEC` |
| provenance | `PROVENANCE.tsv`: donor commit, blob, sha256, URL, target sha256, 2 and 8 renamed lines, 0 imports, 0 drops, `body_match=TRUE` for both files |
| anchors | the SPDX header and one `//FLLDCIM: RELOCATE` line before each top-level type, as families; the M3JDK-authored test files carry `//FLLDCIM: ORACLE` and `//FLLDCIM: DIFFERENTIAL` |
| progress monitor | the donor takes none, so none is added |
| naming | `jdk_naming.py apply --only m3/algorithms/` with the files staged: 0 moves, 0 rewrites, 0 refusals; fixed point |

## Tests and differential

| File | Author | Purpose |
|---|---|---|
| `src/test/java/jdk/m3/algorithm/M3MinCostFlowOracle.java` | donor, relocated | the oracle main, body byte for byte |
| `src/test/java/jdk/m3/algorithm/M3MinCostFlowOracleTest.java` | M3JDK | surefire entry: runs the oracle main once, asserts the printed `M3_MINCOST_FLOW_PASS checks=6671` |
| `src/test/java/jdk/m3/algorithm/M3MinCostFlowDump.java` | M3JDK | reflective differential driver (not a test): seeded networks in six sections (exhaustive tiny, random up to the contract maximum, zero capacity and zero-cost cycles, parallel edges, infeasible demand, refusals); dump mode prints flow, cost and every edge flow; cross mode compares flow value and cost against the unchosen solver |

## Build and gates

| Step | Command |
|---|---|
| compile, test, jar | `JAVA_HOME=<JDK 21> mvn -B -ntp -f m3/algorithms/pom.xml package` |
| differential (gates 2, 3, 3b, 4) | `bash recipe/mincost-flow-20261106/differential.sh <com.synexia clone> <rev> <scratch> [random=100000] [xint-random=2000]` |
| javap parity (gate 5) | `bash recipe/mincost-flow-20261106/javap_parity.sh <scratch of differential.sh>` |
| all gates | `FAMILIES_DONOR_GIT=<m3scale clone> FAMILIES_DONOR_REV=3385d79a2968 BASELINE_CLASSES=<master target copy> MAVEN_REPO_LOCAL=<repo> bash recipe/mincost-flow-20261106/verify.sh <com.synexia clone> <rev> <scratch>` |

| Gate | Result (2026-11-06, GATES.tsv) |
|---|---|
| 1 `mvn package`, `-Xlint:all -Werror` | BUILD SUCCESS, 0 warnings (master 0); 29 tests (master 28) on master 3a429ee33a7e |
| 2 donor oracle figure | `checks=6671` donor and relocated, JIT and -Xint |
| 3 differential | 576,643 networks, donor = relocated sha256 `6ecf3330707dc9bdb13f73bb8b3e4f231f87ec6966e1f0b3bd355804c9645467` |
| 3b -Xint | 478,644 lines (2,000 random), donor -Xint = relocated -Xint = relocated JIT, sha256 `87265cb74e21...` |
| 4 cross-solver | 575,620 compared, 575,620 agree, 0 differ; oracle solved 719 of the 1,023 refused networks |
| 5 `javap -p` / `-p -c` | 3 of 3 classes identical modulo names |
| 6 families differential | PASS, `f85280ac94b2fc25bf7f10ac17193bf7b788dc1aea731a6a2b4c3da04fc392f9` |
| 7 master class files | 51 of 51 byte-identical |
| 8 naming | 0/0/0, fixed point |

Hard-limit notes for a later pass, recorded not fixed: `solve` is 110 lines with 12 loops (MethodLength 15, loops
banned); the oracle methods `example`, `exhaustive` and `main` exceed 15 lines; `Result` clones the edge-flow array
on construction and on every `edgeFlows()` call; the M3JDK-authored dump driver uses loops like the families driver.

## Open items

- Contract width (criterion 2): the unchosen solver accepts negative costs, more vertices and long capacities; a
  widening leaf may lift the bounds of `M3MinCostFlow` (or adopt potentials plus Dijkstra) with B, the wave9
  template and the synexia-algo lineage as differential oracles.
- `LongPushRelabelMaxFlow` (max flow, blob `87b86a3b3025`): a later leaf of the plan.
- The shape catalog (`M3AlgorithmShapeCatalog`) has no min-cost-flow row; adding one touches a shared file and is
  left to a catalog pass.
