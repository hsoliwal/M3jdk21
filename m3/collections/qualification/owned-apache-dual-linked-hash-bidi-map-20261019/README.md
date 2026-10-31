<!-- SPDX-License-Identifier: Apache-2.0 -->
# Owned Commons DualLinkedHashBidiMap, 2026-10-19

`com.m3.collections.apache.M3DualLinkedHashBidiMap` (module `m3/collections/implementations/apache-dual-linked-hash-bidi-map`)
fills the five-provider scaffold row for Commons `DualLinkedHashBidiMap`; `INVENTORY.tsv` now records `DONE #1468`. Two
LinkedHashMap-shaped slot tables (one per direction, with a births lane for node-exact entries and iterators) are the whole
state and every operation is side-symmetric. Evidence (`proof-summary.json`): 20 module tests (96,000 differential operations
on forward or inverse targets, 960,117 comparisons incl. exact orders on both sides and the provider quirks that
leave the directions out of step), JaCoCo 100% line/branch gated at 99%,
0-byte allocation probe, 2 receiving-recipe tests. Local Windows/JDK 21 only.
