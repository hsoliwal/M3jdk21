# Collection allocation candidate: 2026-10-18

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0

This receiver contains actual SDK outputs from canonical Synexia commit
`40843d9ecc4404bdb48ae6d3f7c107ed94b22bfc`,
[recipe/evidence PR 12380](https://github.com/hsoliwal/com.synexia/pull/12380).
The canonical crate is
`synexia-openrewrite-recipes/crates/m3-collection-allocation-20261018`.
The source-sealed manifest lives under
`src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-collection-allocation-20261018`
in that recipe module. Target preimage: `2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`.

## Received behavior

* Distinct-owner `M3ArrayList.addAll` copies the logical reference lane directly;
  self-addition and foreign/aliased views retain their snapshot fallback.
* `M3ObjectIndexSnapshot.filtered` scans an accepted prefix without allocating bits,
  then records accepted suffix positions in primitive words and enumerates set bits.
  Predicate order/evaluation count, all-pass identity, slice and slot/state mappings
  stay unchanged.

The constructor, counted-bag filter and list removeIf experiments were rejected for
timing regressions. Their baseline implementations are preserved. No public/protected
runtime API, serialized form, retained layout, capacity policy or concurrency contract
changes. This is the existing Apache-2.0 proving ground; java.util and the String-first
receiving phase are unchanged.

## Evidence and limits

All 133 module main Java sources compiled on Temurin 21.0.12.1+1 with warnings as errors.
Four canonical SDK tests verify exact outputs, unchanged replay and drift/missing/
duplicate refusals. Existing standalone list/bag/object-slot oracles and the new
`M3CollectionAllocationSelfTest` passed 1,528,419 checks per variant. Inspected
public/protected API surfaces match. No native library was loaded.

In 42 serial JVM forks at 1,024 elements, distinct-owner bulk addition measured
728.2 -> 303.3 ns/op and 4,112 -> 0 temporarily allocated bytes/op. Snapshot all-pass
filtering measured 3,206.5 -> 889.6 ns/op and 168 -> 0 bytes/op. These are local probe
medians, not universal performance guarantees. The all-rejected snapshot measured
1,128.4 -> 1,231.5 ns/op; the unchanged bulk-copy control was also noisy. That edge
case and platform performance remain acceptance concerns. See the canonical raw
benchmark archive, including rejected experiments, for every measured workload.

Temporary allocation is not retained heap. Retained heap/JMH/platform gates, the
approved source-owned offline dependency mirror, full reactor/JUnit/native/jtreg
suites and repository-wide acceptance remain pending. The existing POM and CI are
unchanged. Keep this candidate draft until those gates are resolved. The global
scope ledger covers 163 repositories and 6,185 filename candidates with partial
coverage and unqualified rows explicitly recorded; it is not a completion claim.
