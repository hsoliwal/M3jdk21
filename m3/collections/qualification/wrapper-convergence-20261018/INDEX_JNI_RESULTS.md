<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Index and JNI receiving result

This companion change adds the two qualified production atoms and their four verification owners to the wrapper foundation. The complete series at tested base `f29b0afe7ad253cc0562bc8dfdd48fc1acec710e` passed 233 JUnit tests across 79 suites in ordinary Java mode and again with the real candidate JNI libraries plus `-Xcheck:jni`, with zero failures, errors or skips.

[ACCEPTANCE.json](ACCEPTANCE.json) identifies all source postimages. [RESOURCE_COMPARISON.json](evidence/integration-f29/RESOURCE_COMPARISON.json) retains the exact CPU/allocation/region observations. [The Java receipt](evidence/integration-f29/invariant-policy-product-test.json) and [native receipt](evidence/integration-f29/invariant-policy-native-test.json) preserve command, exit status, test counts and source-state hash. [The invariant and recipe overview](README.md) records review limits and both evidence-label corrections.

The independent finite oracle checks 2,990,400 Java observations and 3,267,362 native observations, including all 65,536 byte-mask pairs and 67,914 window cases. Both routes retain observation digest `e15c399b7be3d8c0fbd38bc6e2cd4c63b93bb0b95d1272bce9acdcc7bec5445b`. These are finite domains and selected compositions, not an exhaustive proof over all long values or all callers.

The sampled 100-sort workload allocated about 26.2 MB before and 13.1 MB after the equal-group reversal. The one-tail-hit-per-chunk JNI fixture transfers 256 row IDs instead of 32,768, retaining 256 row calls, 256 state calls and two output writes. No universal throughput, retained-heap or native-RSS reduction is asserted. Current JNI exports and generated headers match the baseline.
