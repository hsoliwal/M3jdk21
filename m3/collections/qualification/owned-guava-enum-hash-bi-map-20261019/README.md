<!-- SPDX-License-Identifier: Apache-2.0 -->
# Owned Guava EnumHashBiMap, 2026-10-19

`com.m3.collections.guava.M3EnumHashBiMap` (module `m3/collections/implementations/guava-enum-hash-bi-map`) fills the
five-provider scaffold row for Guava `EnumHashBiMap`; `INVENTORY.tsv` now records `DONE #1460`. The contract was read
from the Guava 33.4.8-jre sources jar (Apache-2.0) and fixed by differential tests against the real provider; no source
text was copied. `FOSS_IMPLEMENTATION_DECISION.tsv`, `ATOM_COMPOSITION.tsv` and `proof-summary.json` record donors,
atoms and evidence: 17 module tests (96,000 differential operations, 1,248,156 comparisons), JaCoCo 100% line/branch
gated at 99%, 0-byte allocation probe, 2 receiving-recipe tests. Local Windows/JDK 21 only.
