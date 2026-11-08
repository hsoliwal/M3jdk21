<!-- SPDX-License-Identifier: Apache-2.0 -->
# census2-a5-20261106: wave9 flow contract as an extra oracle of the landed min-cost flow owner

Census pass 2, leaf P2-070 (crate `m3-cpu-structured-wave9-20261031`, com.synexia develop
`d5ad5090d87e`). The census matched the crate's `M3MinCostFlow` template by name to the owner landed by #2551
(`jdk.m3.algorithm.MinCostFlow`, recipe [`../mincost-flow-20261106`](../mincost-flow-20261106/README.md)), whose
map already holds that template as the SKIP row "template variant". Decision: **tests only**. The crate's flow
contract becomes an extra oracle of the landed owner. The wave9 solver stays a NEC row: one fast implementation per
JDK, never a second owner. The mapped contract passes on the owner, so there is no owner edit.
Per-row verdicts: [LEAVES.tsv](LEAVES.tsv).

## What the recipe writes

`relocate.py` (RELOCATE_WAVE9_FLOW_CONTRACT v1, derived from `m3/arrays/recipe/bitmatrix-20261106/relocate.py` v3)
reads [RELOCATION_MAP.tsv](RELOCATION_MAP.tsv) and writes two test files:

| file | source |
|---|---|
| `src/test/java/jdk/m3/algorithm/M3MinCostFlowWave9Contract.java` | FILE row: the donor contract `M3Wave9FlowSpatialContract.java.txt` (blob `66fab6b48f17`) after 10 pinned EDIT rows, the TYPE row and the M3JDK header |
| `src/test/java/jdk/m3/algorithm/M3MinCostFlowWave9ContractTest.java` | TEMPLATE row: surefire entry asserting the printed figure (the pattern of `M3MinCostFlowOracleTest`) |

The EDIT rows map the wave9 call shape (`long[] {flow, cost}`, with a work budget) onto the owner's
`solve(...) -> Result`. Each row pins the sha256 of the donor bytes it replaces, and the recipe refuses on a
mismatch.

- **Adapter.** An adapter `solve(...)` (E8) replaces the spatial half (`naive`, `spatialCases`). That half needs
  `M3PackedSpatialIndex`, which has no M3 owner.
- **Self-loops.** The owner refuses self-loop edges. A network that has one is first checked as that refusal, then
  solved without its self-loops. Costs are nonnegative, so a self-loop never changes the exhaustive optimum.
- **Work budget.** The zero-budget refusal is dropped (E5) because the owner has no work budget.
- **Census2 addition (E10).** One costed reverse-arc reroute, a 4-vertex diamond. Mutant M1 survives both the donor's
  random half and the module's donor oracle. This case is the only one that kills it.

[PROVENANCE.tsv](PROVENANCE.tsv) pins donor commit, blob, both sha256 and the edit count. `body_match` and
`body_match_renamed` are FALSE: the body differs from the donor's. `body_match_edited` is TRUE: only the declared
EDIT and TYPE bytes differ.

## Figures

| run | figure |
|---|---|
| donor contract + donor wave9 solver and spatial index (compiled from the donor tree, scratch only), JIT and `-Xint` | `M3_WAVE9_FLOW_SPATIAL_PASS checks=138244` (= crate evidence `jit-flow-spatial.log` / `interpreted-flow-spatial.log`): flow half 6,204 = 3,100 x 2 + 1 + 3, spatial half 132,040 |
| mapped contract on the landed owner, JIT and `-Xint`, and surefire | `M3_MINCOST_FLOW_WAVE9_PASS checks=8070` = 3,100 x 2 + 1,866 self-loop refusals + 1 reroute + 1 empty network + 2 refusals |

`verify.sh` also runs four one-line mutants of the owner:

| mutant | change | wave9 contract | module donor oracle |
|---|---|---|---|
| M1 | reverse arcs priced +cost | killed (E10) | survives (6,671) |
| M2 | one Bellman-Ford pass fewer | killed | killed |
| M3 | demand unbounded | killed | killed |
| M4 | self-loops admitted | killed | killed |

## Run

```
python -I m3/algorithms/recipe/census2-a5-20261106/relocate.py --donor-git <com.synexia clone> --donor-rev d5ad5090d87ef8477a56317e60ac0d29c65029f7 [--check]
mvn -B -ntp -f m3/algorithms/pom.xml package
bash m3/algorithms/recipe/census2-a5-20261106/verify.sh <com.synexia clone> d5ad5090d87ef8477a56317e60ac0d29c65029f7 <scratch dir>
```

Gates: [GATES.tsv](GATES.tsv). Lessons: [RECIPE_MEMORY.tsv](RECIPE_MEMORY.tsv).
