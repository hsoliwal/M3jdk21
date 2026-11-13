<!-- SPDX-License-Identifier: Apache-2.0 -->
# cpu-tools: M3PackedSpatialIndex with its contract split from the wave9 FlowSpatial contract (census2 P2-131), 2026-11-06

**Source.** com.synexia synexia-openrewrite-recipes hash-pinned crate m3-cpu-structured-wave9-20261031, develop
165ceb5a0202 (wave9 blobs unchanged since 7a455139f0a; the same blobs as the census2 pin d5ad5090d87e). Donor rows,
blobs and sha256: [PROVENANCE.tsv](PROVENANCE.tsv).

**Why it was held.** #2608 (merge 6c13b7d8fa97) received waves 6-8, 10-12 and 29-32 through HASH_PINNED_CHAIN v2. It
held `M3PackedSpatialIndex`, because the class's only contract, `M3Wave9FlowSpatialContract`, also exercises wave9's
cpu-tools `M3MinCostFlow`. That solver is not ported: the one min-cost flow owner is `jdk.m3.algorithm.M3MinCostFlow`
(m3/algorithms, #2551).

## Recipe

The same recipe kind as #2608, then one split step:

1. `../cpu-structured-waves-20261106/crate_chain2.py` (HASH_PINNED_CHAIN v2, unchanged, not copied) over the wave9
   hash-pinned-java and hash-pinned-text dirs as one graph, on master. Its output is in
   [VERDICTS.tsv](VERDICTS.tsv), verbatim, with this leaf's verdict per row.
   - `M3PackedSpatialIndex.java` is NEW. It lands byte-identical to the chain output, i.e. `jdk_naming(final)`; the
     package line is the only change from the donor.
   - `M3Wave9FlowSpatialContract.java` is NEW. It is the input of step 2 and is not landed itself.
   - The other 7 rows stay excluded, as in #2608.
2. [split_contract.py](split_contract.py) (CONTRACT_SPLIT v1) applies the declared rows of [SPLIT.tsv](SPLIT.tsv) and
   writes `fixtures/M3PackedSpatialIndexContract.java`. Every row is pinned: the input sha256, each dropped span's
   sha256, each edit's text sha256 and occurrence count. A mismatch refuses with exit 2 and writes nothing.

| row | lines (input) | what | why |
|---|---|---|---|
| D1_FLOW_IMPORT | 4 | `import jdk.m3.tools.cpu.M3MinCostFlow;` | the wave9 solver is not ported (one owner, #2551) |
| D2_FLOW_ORACLE | 29-54 | `flowOracle(...)`, exhaustive integral-flow enumeration | used only by the flow cases |
| D3_FLOW_CASES | 55-94 | `flowCases()`: 3,100 seeded networks, input non-aliasing, empty network, 3 refusals | the flow half: 3 `check` and 3 `refuses` sites, 6,204 checks |
| D4_FLOW_CALL | 158 | `flowCases();` in `main` | |
| E1_TYPE | 2 occurrences | `M3Wave9FlowSpatialContract` to `M3PackedSpatialIndexContract` | named after the owner it checks |
| E2_DOC | 1 | drops "exhaustive small-flow and " from the class comment | the flow oracle is gone |
| E3_TOKEN | 1 | pass token `M3_PACKED_SPATIAL_INDEX_PASS` | the token named the flow half |
| A1-A3 | output | `MinCostFlow`, `flowOracle`, `flowCases` absent | nothing of the flow half is left |

Assertion-site census, printed by the tool: source 16 sites, 6 dropped (all in D3), 10 kept. The kept sites are the
6 `check` and 3 `refuses` sites of `spatialCases()` plus the `check` inside `refuses`. They are byte-identical to the
donor lines. The split contract prints `checks=132040`; the unsplit donor contract prints 138,244. The difference,
6,204 = 3,100 x 2 + 1 + 3, is exactly the flow half.

**The flow half is not ported here.** #2603 (merge 4cdaa6073c08) already covers it on the owner as
`m3/algorithms/src/test/java/jdk/m3/algorithm/M3MinCostFlowWave9Contract.java`: 8,070 checks, plus 4 owner mutants
killed. #2603 dropped the spatial half because it had no M3 owner (its RELOCATION_MAP OMIT row); this leaf supplies
that half.

**Owner check.** `jdk.m3.tools.cpu.M3SpatialQueries` (master) is a different contract: an inclusive point filter by
linear scan, returning a bitmask in row order. `M3PackedSpatialIndex` is an immutable half-open rectangle index with
result and visit budgets. They are different shapes, so this is not a second owner. Their equivalence is not decided
here (NEC). The gate uses `M3SpatialQueries` as an independent point reference: a unit-cell point `[x, x+1) x [y, y+1)`
meets the half-open query `[a, c) x [b, d)` exactly when the point lies in the inclusive range `[a, c-1] x [b, d-1]`.

**Not registered in build.py `FIXTURES`.** This follows #2608, which registered none of its 23 new fixtures. The gate
compiles and runs the new fixture separately.

## Gate

[spatial_gate.sh](spatial_gate.sh) runs CPU_TOOLS_PACK_GATE v2, mirroring `../cpu-structured-waves-20261106/waves_gate.sh`
and `cpu-tools/build.py`, plus the split and differential steps. Toolchain: stock JDK 21.0.9 and MinGW gcc 15.2.0.
Every JVM runs with `-Djava.io.tmpdir` on the scratch drive. First run on master 5bd70ef520bb; re-run on db148c0aa760 (the delta touches only m3/compression) with identical figures; rebased onto the push base 17aab27f4f1a, whose further delta touches only m3/collections (no gate input).

**Step 0, recipe.**
- `crate_chain2.py` reproduces the class bytes: verdict NEW, final c18469be6e1c, class file identical to the chain
  output.
- `split_contract.py --check` is a fixed point: UNCHANGED, out sha256 a9a7a0d1238d.
- A tampered D3 pin refuses with exit 2 and writes nothing.

**Step 1, modules and natives.**

| check | live | leaf |
|---|---|---|
| modules: javac `-Xlint:all,-module -Werror` | 0 errors, 0 warnings, 312 classes | 0 errors, 0 warnings, 314 classes |
| gcc `-Werror` m3textio, m3textops | 0 diagnostics | 0 diagnostics |
| build.py `FIXTURES`, run with `-Xcheck:jni` | 15/16 | 15/16, 0 regressions |

The leaf adds 2 classes: `M3PackedSpatialIndex` and `M3PackedSpatialIndex$Index`. The failing fixture on both sides
is M3CpuToolsTest ("CLI explicit search truncation"), the pre-existing Windows failure recorded by #2608.

**Step 2, new fixture.**
- `M3PackedSpatialIndexContract` compiles with javac `-Xlint:all -Werror` with 0 diagnostics.
- Run with `-Xcheck:jni`, JIT and `-Xint`: `M3_PACKED_SPATIAL_INDEX_PASS checks=132040` both times.

**Step 3, donor figure.** The unsplit donor contract with the donor class and the donor wave9 solver, compiled in
scratch only, prints `M3_WAVE9_FLOW_SPATIAL_PASS checks=138244` under JIT and under `-Xint`. This matches the crate
evidence and the #2603 donor figure.

**Step 4, differential** ([oracle/M3PackedSpatialIndexDifferential.java](oracle/M3PackedSpatialIndexDifferential.java)).
- **Inputs.** 136 seeded cases in 5 families:
  - RECT: mixed rectangles;
  - POINT0: zero-area points;
  - POINT1: unit-cell points with duplicates;
  - TIES: few distinct minX;
  - EXTREME: both ends of int.

  Each case holds up to 2,000 rectangles and runs 200 queries with an ample budget, 27,200 queries in all (2,254,561
  hits). B lines (21,495) cover the rest:
  - result and visit budgets;
  - query and build refusals, including the 200,000 cap;
  - caller-lane and output non-aliasing.
- **Donor vs relocated.** The dumps under JIT and `-Xint` are byte-identical: one sha256, 313ecd105de2, 13,079,812
  bytes.
- **Linear-scan reference** (exact half-open intersection). Agrees with the relocated class on all 27,336 C/A lines,
  with 0 mismatches.
- **M3SpatialQueries point reference.** Agrees with the relocated class on all 6,432 POINT1 lines, with 0 mismatches.

**Step 5, mutants** (one-line changes of the relocated class, scratch only).

| mutant | change | split contract | differential |
|---|---|---|---|
| M1 | touching edge counted (`>=`) | killed | differs |
| M2 | search bound `<=` | killed | differs |
| M3 | ids left unsorted | killed | differs |
| M4 | zero-width rectangles admitted | killed | differs |
| M5 | caller minX lane aliased | survives (it checks only `size()` after the caller mutation) | differs on 419 alias lines |

Only the differential kills M5.

**jdk_naming.** `jdk_naming.py apply --only` and then `plan --only`, on `make/m3/tooling/module-packs/cpu-tools/` and this
directory, report moves=0, rewrites=0, refusals=0: a fixed point.

## Run

```
python -I make/m3/qualification/cpu-structured-waves-20261106/crate_chain2.py <wave9 hash-pinned-java dir>,<hash-pinned-text dir> <M3jdk21 git dir> <master> <fe35ea98841^> make/m3/tooling/jdk-naming <out>
python -I make/m3/qualification/cpu-packed-spatial-index-20261106/split_contract.py <out>/m3/tooling/module-packs/cpu-tools/fixtures/M3Wave9FlowSpatialContract.java make/m3/tooling/module-packs/cpu-tools/fixtures/M3PackedSpatialIndexContract.java [--check]
bash make/m3/qualification/cpu-packed-spatial-index-20261106/spatial_gate.sh <master> <com.synexia checkout> <develop rev> <scratch>
```

The donor dir names are the CLASS and CONTRACT rows of PROVENANCE.tsv.
