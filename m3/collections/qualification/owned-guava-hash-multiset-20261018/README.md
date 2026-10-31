<!-- SPDX-License-Identifier: Apache-2.0 -->
# Owned Guava HashMultiset, 2026-10-19

`com.m3.collections.guava.M3HashMultiset` (module `m3/collections/implementations/guava-hash-multiset`) fills the
five-provider scaffold row for Guava `HashMultiset`; `INVENTORY.tsv` now records `DONE #1418`.

Before code: the scaffold (#1392) fixed the public surface; `FOSS_IMPLEMENTATION_DECISION.tsv` records the
provider contract/oracle, the first-party backward-shift and counting-lane patterns, the reused receiving
engine and the excluded Eclipse pattern. `ATOM_COMPOSITION.tsv` maps the atoms to their validation receipts.
Two algorithm rows are appended to `../five-provider-inventory-20261018/ALGORITHM_CATALOGUE.tsv`.

Evidence (`proof-summary.json`): 16 module tests including a 96,000-operation differential run against the
real Guava jar (867,116 comparisons); JaCoCo 100% line and branch for all 9 classes, gated at 99%; 0-byte
steady-state allocation probe; 2 receiving-recipe tests. Local Windows/JDK 21 only.
