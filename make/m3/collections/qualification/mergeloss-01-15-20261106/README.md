<!-- SPDX-License-Identifier: Apache-2.0 -->
# Merge-loss restore R4: audit cases 01 + 15 (m3/collections/interop)

Source: merge-loss audit S:/m3-scratch/eb-conv/mergeloss (LEAVES.tsv rows 1 and 15, cases/01.md, cases/15.md).

| case | dropping merge | origin | decision |
|---|---|---|---|
| 01 | f4fbe3333946 (ancestry, 2026-10-17) | 58a72d0b2b8 (#1174) | RESTORE: 5 factories, 9 collectors (+2 helpers), 5 inventory rows, the origin's own test; owner overloads NEC; M3GuavaBiMap NEC; multiset adapters SUPERSEDED (c92266bb605) |
| 15 | 0efc1719ca57 (ancestry #1214 #1218 #1221) | 50779ade2a3 (#1218) | NEC: guavaMultiset/apacheMultiSet come back with case 01; jdkArrayList, eclipseListMultimap, eclipseSetMultimap had no SDK inventory rows in the origin |

- `DECISION.tsv`: decisions and the evidence (positive evidence for case 01: the master crate
  `m3/collections/interop/qualification/five-provider-current-selection-20261017/README.md`, stacked on #1174, states
  33 factories and 20 collectors are retained; master had 25 and 11).
- `HUNKS.tsv`: every lost member with its disposition (RESTORED, PRESENT, NEC, SUPERSEDED, SKIPPED) and citation.
- `apply.py` + `EDITS.tsv`: hash-pinned recipe (preimage blobs on master b28b7c938e2, origin and merge cited per rule,
  refuses drift, idempotent). `//FLLDCIM: MRL` anchors cite origin and merge in each restored block.
- `oracle/`: owner differential (`MergelossInteropDifferential`) and reference model (`MergelossInteropReference`,
  real Guava / Commons Collections / Eclipse Collections); `tools/mutants.py`, `MUTANTS.tsv`; `tools/failset.py`.
- `GATES.tsv`: gate results.

Open item outside this leaf: `m3/collections/interop/sdk-family-bindings.tsv` still names `com.m3.collections` owners
(JDK naming missed the TSV), so `M3LibraryBindings` refuses to load on master (M3SdkFamiliesTest and
M3SpringCaseInsensitiveSdkFactoryTest error before and after this change). With the TSV JDK-named the inventory
verifies 30/30 here (25/25 on master).
