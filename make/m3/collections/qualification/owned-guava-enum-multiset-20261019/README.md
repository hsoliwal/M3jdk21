<!-- SPDX-License-Identifier: Apache-2.0 -->
# Owned Guava EnumMultiset, 2026-10-19

`com.m3.collections.guava.M3EnumMultiset` (module `m3/collections/implementations/guava-enum-multiset`) fills the
five-provider scaffold row for Guava `EnumMultiset`; `INVENTORY.tsv` now records `DONE #1499`. It is the first of
the approved next wave (EnumMultiset, LinkedHashMultiset, LinkedHashMultimap, TreeBag, TreeMultimap).

Before code: the scaffold (#1392) fixed the public surface; `FOSS_IMPLEMENTATION_DECISION.tsv` records the
provider contract/oracle, the first-party ordinal-lane and presence-mask pattern (`M3EnumLongMap`), the counting
and view patterns from the owned `M3HashMultiset` (#1418), the reused receiving engine and the excluded Eclipse
pattern. `ATOM_COMPOSITION.tsv` maps the atoms to their validation receipts. One algorithm row is appended to
`../five-provider-inventory-20261018/ALGORITHM_CATALOGUE.tsv`.

Evidence (`proof-summary.json`): 15 module tests including a 96,000-operation differential run against the
real Guava jar over a 70-constant enum (960,225 comparisons, exact ordinal order, traversals interleaved with
external modification); JaCoCo 100% line and branch for all 13 classes, gated at 99%; 0-byte steady-state
allocation probe; 2 receiving-recipe tests. Local Windows/JDK 21 only.

In-place landing (M3jdk21-is-the-JDK rule): the inventory row names the `java.util.HashMap` store that Guava's
`AbstractMapBasedMultiset` family lands in; `EnumMultiset` itself keeps no map, so this owner's lanes are the
store and the API owner stays outside `java.base`.
