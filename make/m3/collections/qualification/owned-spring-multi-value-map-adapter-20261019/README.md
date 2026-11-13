<!-- SPDX-License-Identifier: Apache-2.0 -->
# Owned Spring MultiValueMapAdapter, 2026-10-19

`com.m3.collections.spring.M3MultiValueMapAdapter` (module `m3/collections/implementations/spring-multi-value-map-adapter`)
fills the five-provider scaffold row for Spring `MultiValueMapAdapter`; `INVENTORY.tsv` now records `DONE #1469`. The
contract is write-through over the caller's map, so the owner holds one reference; created value lists are JDK `ArrayList`s
(M3-backed in place in M3jdk21) and `asSingleValueMap()` is an owned live first-value view. Evidence (`proof-summary.json`):
12 module tests (96,000 differential operations, 576,063 comparisons incl. list classes and view failure modes), JaCoCo 100%
line/branch gated at 99%, 0-byte allocation probe, 2 receiving-recipe tests. Local Windows/JDK 21 only.
