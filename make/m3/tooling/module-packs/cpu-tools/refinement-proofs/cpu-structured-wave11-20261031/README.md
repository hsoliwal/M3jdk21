<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# CPU Wave11 receiving-source proposal (not an installed M3JDK 21 build)

Replace exactly one previously verified native C file: `native/m3_sorted_int_set.c`.
The matching Java class public contracts and JNI symbol are unchanged. Add a Java
fixture under `fixtures/` and a standalone C-only fixture under `native/tests/`;
never ship the recipe, test-runner or authoring preimage templates in the public
product. Source-only and independently compiled JNI proofs are not JMOD/jtreg,
HotSpot/GC, broader reactor, hosted CI or measured throughput qualification.
