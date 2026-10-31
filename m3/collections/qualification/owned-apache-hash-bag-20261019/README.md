<!-- SPDX-License-Identifier: Apache-2.0 -->
# Owned Commons HashBag, 2026-10-19

`com.m3.collections.apache.M3HashBag` (module `m3/collections/implementations/apache-hash-bag`) fills the five-provider
scaffold row for Commons `HashBag`; `INVENTORY.tsv` now records `DONE #1421`. The Commons contract was read from the
4.5.0 sources jar (Apache-2.0) and fixed by differential tests against the real provider; no source text was copied.
`FOSS_IMPLEMENTATION_DECISION.tsv`, `ATOM_COMPOSITION.tsv` and `proof-summary.json` record donors, atoms and evidence:
16 module tests (96,000 differential operations, 768,082 comparisons), JaCoCo 100% line/branch gated at 99%,
0-byte allocation probe, 2 receiving-recipe tests. Local Windows/JDK 21 only.
