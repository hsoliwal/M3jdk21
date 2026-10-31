<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
# Array expansion execution receipt

Date: 29 October 2026. Passive product provenance; this directory owns no recipe or generator.

Canonical owner: `hsoliwal/com.synexia`, task
`synexia-openrewrite-recipes/crates/m3-array-expansion-20261029`.
Recipe: `com.synexia.m3.ArrayExpansion20261029`, composed from the existing
`M3HashPinnedJavaSnapshotRecipe` and `M3HashPinnedTextSnapshotRecipe` engines.
Product input: `3569c790d5cc3f9bc871ab0ecd025dab0fd5cafc`.

The executed Java stages preserve deque capacity selection and public signatures,
copy the ring in at most two spans per lane, reject overflowing row ranges, and
publish columnar replacement lanes only after all allocations and copies succeed.
The three test files were generated through the same canonical Java recipe.

| Executed output | SHA-256 |
|---|---|
| `M3ArrayDeque.java` | `c67e58ade928f25f5add7b374701fd33417890a59b324bbc2df220de5c23312b` |
| `M3ColumnarLanes.java` | `f20ebe2b3f6877a830f4cb8670be646dd38ea496d7e1d3223340e0ec1e6fc266` |
| `M3ArrayDequeGrowthContract.java` | `ceb6e333dfdf640a12560b3c00e3bdc200a5d89a52b8d6594b39edd137d5e56e` |
| `M3ColumnarGrowthContract.java` | `ebf942a8bacd4426e00973fc0a7c4912db858bec480c230e5f8c0de91d2d194a` |
| `M3ArrayExpansionTest.java` | `76d8968b8e53b8d00f82dc2d205f1f304e220b7c4c83b08cabedb1b25dcac1e1` |

Qualification: Java 21 strict compilation; both serial owner orders; all four
before/after owner states; exact public API surface; zero changes at fixed point;
eight incompatible-input refusals; protected decoy; three owning JUnit tests;
JIT and interpreter behavior checks; unchanged C17/UBSan JNI mask selection;
and all twelve exercised allocation-failure positions with successful retry.
Canonical receipts retain the actual commands, counts and baseline counterexamples.

The 72-process deque timing comparison is diagnostic evidence on one shared host.
It reports unchanged capacities, checksums and allocated bytes, with variable
elapsed times. Columnar publication adds shallow directories on materialized growth;
no columnar speedup or universal performance optimum is claimed.

Broader module failures and release limitations are recorded in the canonical task.
This receipt does not certify a full integrated JDK build, jtreg, other platforms,
or phase promotion. The canonical task was executed from a sealed local source
closure; its remote commit was unavailable because GitHub returned an API rate
limit error. No published Synexia revision or merged product release is claimed.
