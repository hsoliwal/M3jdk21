<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# M3JDK21 receiver for fork relocation recipes

Canonical Synexia recipe PR: https://github.com/hsoliwal/com.synexia/pull/12293
Exact Synexia source-first commit: `0f96532236f9d2b113b635f8c14884f64a75f6fc`.
M3JDK21 product receiver baseline: `master@2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`.

All six recipe/fixture/ledger source files are exact mirrors from Synexia. This
receiving commit does not overwrite `java.base` or alter any existing M3 collection
runtime class. Package relocation must run only inside a staged copy of each
provider fork; preserve upstream FQCNs in existing public optional interop JARs
until their own real compatibility tests pass.

This first pass includes the source-level OpenRewrite algorithms and five
isolated target namespaces. **The five full donor source trees have NOT yet
been recipe-materialized into this receiver.** They require per-fork generation,
dependency correction, license preservation, full compile/tests and CI receipts
before actual M3 SDK backend admission. The OpenJDK GPL+Classpath source and
Eclipse EPL/EDL source must keep their upstream terms; they cannot simply be
rebadged as Apache-2.0.

The complete end-goal remains the superset of JDK, Guava, Eclipse Collections,
Apache Commons Collections and Spring, with M3 array/offset/index/mask
backends, live vs snapshot contracts, and JNI proof gating. This recipe is
the first mechanical source-migration pass, not proof of all family coverage.
