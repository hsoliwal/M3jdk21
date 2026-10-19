<!-- SPDX-License-Identifier: Apache-2.0 -->
# Owned Guava EnumBiMap, 2026-10-19

`com.m3.collections.guava.M3EnumBiMap` (module `m3/collections/implementations/guava-enum-bi-map`) fills the five-provider
scaffold row for Guava `EnumBiMap`; `INVENTORY.tsv` now records `DONE #1462`. Two mirrored `int[]` ordinal lanes are the
whole state and every operation is side-symmetric. Evidence (`proof-summary.json`): 15 module tests (96,000 differential
operations, 960,066 comparisons incl. both toString forms), JaCoCo 100% line/branch gated at 99%, 0-byte allocation probe,
2 receiving-recipe tests. Local Windows/JDK 21 only.
