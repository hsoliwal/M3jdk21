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
| `M3ArrayDeque.java` | `55be5f7d9747f88c751987e69f9c2587559d27f55ece13c0e8aef84506f04108` |
| `M3ColumnarLanes.java` | `fb44d6095f7107a6f8407d290b5fa5a2e91f68e96921e7d259167bbbbe0cc49f` |
| `M3ArrayDequeGrowthContract.java` | `576840364559d3c6e3a467b1e9f22b18cf2bb8c81e459039ac0ed7cb5663a312` |
| `M3ColumnarGrowthContract.java` | `ff82a42cd66551c00ff12fb7bbcdc0583f4914d686b571024cbf10c3f74fb74a` |
| `M3ArrayExpansionTest.java` | `a127595d7eb71525ddd0d73ebe8c783cda60316bedecbdf9297e0cf9653f036c` |

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
