<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# M3JDK21 original owning Maven/JUnit repair
Original source `m3/collections/pom.xml` blob `1a02585f0cca2649f5a44073a104ea23dc4e9737` had test-scope `org.junit.jupiter:junit-jupiter:5.10.2-m3-source-1`. GitHub Actions [job 119611649959](https://github.com/hsoliwal/M3jdk21/actions/runs/39713990682) failed Maven Central resolution before any JUnit could run. Strict Java21 source compilation passed in job 119611649642.

This focused receiving changes exactly one POM value to published `5.10.2`, with no Java/JNI production source or public ABI changes. Its canonical source-sealed Maven/OpenRewrite recipe and original source/preimage/afterimage live in Synexia crate `m3-collections-junit-resolution-20261022`. Product postimage Git blob: `e9719c65de2eda0d5de89551fdabdc1589884b4d`. Original and resulting SHA-256: `f209b4b2e0f71796fdfe4cb49929839f24ffd56b98b9181f2d3163cd80e01c15` → `4032efb8f89dd4766d50675699c4c36684a300f9499ab3cf356d5a5cd5ae99c0`.

This is a source-identical test dependency fix **inside the M3JDK21 product**, not a new runtime layer. Actual collections Maven/JUnit, signal tests, source-built JDK, JNI, and full release acceptance must still run. If any private custom JUnit fork is required, its modified contract and provenance must be reviewed independently before acceptance.
