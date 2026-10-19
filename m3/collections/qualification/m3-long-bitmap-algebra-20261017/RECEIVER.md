<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# M3JDK21 target receiver — immutable bitmap filter composition

Canonical Synexia recipe PR: https://github.com/hsoliwal/com.synexia/pull/12092
Canonical recipe commit: `2c29486cdb115152fbc0aa5ac640489f260d1b97`
Target receiver baseline: `master@4e29dc9d4141261e8763fbe9ffd6a20e017b1aff`.
Exact canonical Maven recipe, Java templates, README, catalogue and provenance are mirrored here. No Synexia *runtime* dependency.

Two serial receiver commits: (1) the source-sealed recipe and proof workflow, (2) its exact emitted Java source and selected differential oracle. Existing M3 collection owner files, OpenJDK `src/java.base`, JNI C, HotSpot, and default branch remain untouched. This is not a general JDK collection replacement or a change to live filtered view behavior.

Selected-source Java 21 strict compile: PASS. Differential tests: **637172 checks**, Java only and checked real-JNI run with unchanged mask selector. Local direct generator fixed-point/missing/drift assertions: PASS. Local Maven: NOT_RUN (unavailable). Hosted Maven/OpenRewrite/JUnit selected module, JNI platform, jtreg/JCK/JVM-image and heap/performance: PENDING. Preserve String -> arrays -> collections acceptance ordering and keep this PR DRAFT until owning verification.

Generated source SHA-256: `bd7969cfc42e26171b9904c65eb12f3321b2fbffe77a34f6d76f4db15c11c1d3`.
Generated test SHA-256: `fa87bbf51b5a58b6b1562b909c3c16d0b670168165db57e6bb4430158de11fd7`.
The `source` projection identity is the algebra domain. Only cached immutable snapshots are composable; arbitrary live predicates remain unsupported without a trusted revision.
