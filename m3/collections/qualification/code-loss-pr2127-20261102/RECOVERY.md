<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# PR #2127 long-map merge-content recovery — 2026-11-02

State: **DRAFT / current Java 21 Maven and target gates pending**.

This is a verified missing behavioral atom from a *merged* historical pull request, not a
request to revert current master or to replay its historical branch as a wholesale merge.

## Exact history and provenance

- Historical receiving PR: https://github.com/hsoliwal/M3jdk21/pull/2127
- Historical source authority: https://github.com/hsoliwal/com.synexia/pull/14040
- Current target baseline: `ce005d25b29fb6246e013bffb462afd5ed070362`.
- Current map preimage blob: `134508fdd9857110ca21d0042353805586db3ea0`.
- Historical qualified test blob: `e97ed53b5dc1318a5fe865fe032ee8608690d621`.
- Exact private SDK-generated composed map postimage blob: `37e5575193fb010b03e786ad1f717fecce111fec`.
- Canonical source-sealed recipe:
  `synexia-openrewrite-recipes/recipe-crates/m3-collections-long-receiver-20261029`,
  leaf `m3-long-receiver-staged-rehash-20261029`, using
  `M3HashPinnedJavaSnapshotRecipe`.
- Canonical manifest SHA-256:
  `d8a89c3826ccb035a37652bd0289221c383c0e806999015a935b8ce0253f9890`.
- Composition evidence:
  `composition/evidence/QUALIFICATION.json` at `hsoliwal/com.synexia:develop`.

The current master map was independently read by exact Git blob SHA. Its rehash method
and **entire suffix** are byte-identical to the canonical composed postimage. The
source change preserves all existing members and their contracts, extracts the
existing absent-key insertion statement sequence for reuse, and adds
`long addTo(long key, long delta)`.

The operation updates present keys with one lookup (no structural epoch change),
inserts absent keys even for zero delta, and preserves Java long overflow. The
historical six-group JUnit source is recovered *byte-for-byte*.

## Coverage and gates

Historical source receipt (not a newly run qualification): private Java 21
OpenRewrite recipe/fixed-point/refusal tests, six AddTo test groups, seven
additional staged-lane tests, strict compile, and 520,041 checks in the staged
composition. Do not transfer these results to a newer whole-JDK acceptance claim.

This recovery branch:
- does not rebase or rewrite master;
- does not change JNI, a Java SE API, module exports, or dependency owners;
- retains the current failure-safe staged rehash and primitive lane ownership;
- adds one exact source file postimage, one historical test file, and this record.

**Pending:** actual current Java 21 Maven module reactor, the original six JUnit
cases against the recovered branch, target naming/ABI admission, dependency
closure, installed-JDK/JNI/jtreg verification, full code-loss guard and CI.
A local Java compiler/Maven distribution was unavailable in this execution
environment; no fresh Java compilation or test pass is claimed.

**Held separately:** the historical naming-map migration record and its
`name-plan.json` authority pin. Current naming metadata has advanced to 71
migration records, whereas the historical recipe input had 67. The metadata
must be source-sealed against this newer current preimage, unioned without
discarding those later records, and emitted by a new canonical recipe; do not
copy the older 540 KiB mapping postimage onto current master.

Maven gate from the exact target checkout:

```bash
./mvnw -B -ntp -pl m3/collections -am -Dtest=M3LongLongHashMapAddToTest -Dsurefire.failIfNoSpecifiedTests=false test
```
