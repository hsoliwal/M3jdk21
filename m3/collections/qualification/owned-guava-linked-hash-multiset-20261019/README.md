<!-- SPDX-License-Identifier: Apache-2.0 -->
# Owned Guava LinkedHashMultiset, 2026-10-19

`com.m3.collections.guava.M3LinkedHashMultiset` (module `m3/collections/implementations/guava-linked-hash-multiset`)
fills the five-provider scaffold row for Guava `LinkedHashMultiset`; `INVENTORY.tsv` now records `DONE #1523`. It is
the second of the approved next wave (EnumMultiset, LinkedHashMultiset, LinkedHashMultimap, TreeBag, TreeMultimap).

Before code: the scaffold (#1392) fixed the public surface; `FOSS_IMPLEMENTATION_DECISION.tsv` records the
provider contract/oracle, the dense-lane and backward-shift patterns of the owned `M3HashMultiset` (#1418), the
first-party `M3LinkedHashMap` order-chain pattern, the reused receiving engine and the excluded Eclipse pattern.
`ATOM_COMPOSITION.tsv` maps the atoms to their validation receipts. One algorithm row is appended to
`../five-provider-inventory-20261018/ALGORITHM_CATALOGUE.tsv`.

Evidence (`proof-summary.json`): 17 module tests including a 96,000-operation differential run against the
real Guava jar with exact iteration order (960,208 comparisons); JaCoCo 100% line and branch for all 10 classes,
gated at 99%; 0-byte steady-state allocation probe; 2 receiving-recipe tests. Local Windows/JDK 21 only.
