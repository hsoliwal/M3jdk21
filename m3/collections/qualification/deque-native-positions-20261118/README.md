<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->
# Direct M3JDK21 generic deque JNI ordinal projection

**Product:** the original `jdk.m3.collections.M3ArrayDeque` inside the integrated M3JDK21; no runtime overlay, no duplicate JNI library, no second collection owner.

## Stacked review and immutable source
- Parent product PR [#3372](https://github.com/hsoliwal/M3jdk21/pull/3372), exact commit `a753e1aa04a26fd6056fa07af59760f85ca0cb63`. Review/merge it first; this follow-up PR is based on its branch, not `master`.
- Canonical source-sealed recipe: [Synexia PR #18810](https://github.com/hsoliwal/com.synexia/pull/18810), commit `5c70d9da2a8e9dfdfb3ea13757e025a5286fc481`, class `com.synexia.m3.DequeNativePositionSelect20261118`.
- Original `src/jdk.m3.collections/share/classes/jdk/m3/collections/M3ArrayDeque.java` blob `e0d2d5a558cda8e00ce39e3aac0176377f0f7710`, SHA256 `250e797ecf6d8f677cc7aa1c45d636abaec57b657f74735a95b3e9c9c8d8ca08`.
- Generated source SHA256 `c70380825ab7c443404189350566a22e057c9680991284e0b1851cf14d834300`.
- New JUnit SHA256 `24c3c5222927a1f3cfaf4ec10e2b2e60f025643a29c4671910d45faf633d391b` (ABSENT before).

## Code change and contract
- Keep the original Object[] payload and byte[] per-slot masks, identical public Deque methods, arbitrary ring capacities and original JAVA backend.
- Replace NATIVE_REQUIRED's eager `int[size]` physical-row array by the existing `IndexMaskCompactor.selectPositions(byte[], LIVE, backend)` primitive logical-position owner. One `byte[size]` admission array remains; JNI returns only selected ordinals, mapped to physical offsets by the original deque's `position()` coordinate method.
- Modern native selector avoids the caller's full 4n-byte identity index; legacy compatible native libraries retain the existing `IndexMaskCompactor` fallback. Never pass Java object references or user callback predicates into JNI. No new native C symbols or library dependencies.
- Package-local helper is independently exercised with the JAVA ordinal-selector oracle, because native is not available in the local proof; product JUnit with `-Dm3.test.native=true` REQUIRES actual JNI, and will fail rather than silently skip when unavailable.
- Original overlapping required/excluded mask combinations still throw before selecting. The JUnit excludes those invalid combinations from the parity loop and retains refusal checks.

## Proof executed / holds
- Independently compiled Java21 old physical-array vs new ordinal projection: **7,321,600 JIT** and **130,560 -Xint** physical-output cases passed. This validates only Java ring/mask projection arithmetic.
- **PENDING**: full canonical Maven/OpenRewrite JUnit, target built-M3JDK21 module JUnit and JNI parity with actual native library, JCK/jtreg, Eclipse SWT/Equinox/Tycho platforms, installed-image packaging and performance measurements. Do not merge automatically based only on mathematical oracle; review source and native ABI.
