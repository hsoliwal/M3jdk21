<!-- SPDX-License-Identifier: Apache-2.0 -->
# Owned Commons PassiveExpiringMap, 2026-10-24

`com.m3.collections.apache.M3PassiveExpiringMap` (module `m3/collections/implementations/apache-passive-expiring-map`)
fills the five-provider scaffold row for Commons Collections `PassiveExpiringMap`; `INVENTORY.tsv` now records
`DONE #1913`. Leaf C20 of the 2026-10-24 algorithm plan, taken after owner decision D3 (keep #1261 as the
HashMultimap owner).

Before code: `FOSS_REUSE_DECISION.tsv` records the provider contract and differential oracle (no source text
copied), the pattern-only Guava write queue and Caffeine timer wheel (NO_FIT), the first-party heap, probe index
and hash mix patterns, and the test-clock redirection decision. The LeetCode ids of the catalogue rows in
`../../algorithm-catalogue/challenge-patterns-20261024/EXPIRY_ORDER_INDEX.tsv` are idea references only. One algorithm
row is appended to `../five-provider-inventory-20261018/ALGORITHM_CATALOGUE.tsv`.

Evidence (`proof-summary.json`): 51 module tests including a 100,000-operation differential run against the
unmodified commons-collections4 4.5.0 classes with `System` redirected to a test clock (1,100,220 checks, 2,201
serialization round trips); JaCoCo 100% line and branch for all 11 classes, gated at 99%. Local Windows/JDK 21 only.
