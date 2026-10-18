<!-- SPDX-License-Identifier: Apache-2.0 -->
# Owned Guava ArrayListMultimap, 2026-10-18

`com.m3.collections.guava.M3ArrayListMultimap` (module `m3/collections/implementations/guava-array-list-multimap`)
fills the five-provider scaffold row for Guava `ArrayListMultimap`; `INVENTORY.tsv` now records `DONE #1400`.

Before code: the scaffold (#1392) fixed the public surface; `FOSS_IMPLEMENTATION_DECISION.tsv` records the
provider contract/oracle, the first-party key-directory and mask-compaction atoms, the reused receiving
engine and the excluded Eclipse pattern. `ATOM_COMPOSITION.tsv` maps each operation atom to its source and
its validation receipt. Algorithm rows are appended to the shared
`../five-provider-inventory-20261018/ALGORITHM_CATALOGUE.tsv`.

Evidence (`proof-summary.json`): 22 module tests including a 96,000-operation differential run against the
real Guava jar (1,025,276 comparisons); JaCoCo 100% line and branch for all 14 classes, gated at 99%;
0-byte steady-state allocation probe; 2 receiving-recipe tests (8 fixed-point executions, 4 refusals).
Local Windows/JDK 21 only; no hosted CI, performance or JNI claim.
