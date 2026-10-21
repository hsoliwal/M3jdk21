<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Physical-slot 32-bit state projection: catalogue and recipe receipt

**Canonical owner:** Synexia, source-sealed OpenRewrite recipe `com.synexia.rewrite.M3Jdk21ObjectPhysicalSlotProjection`.

**Target:** `hsoliwal/M3jdk21@2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49` (candidate source preimage).
Recipe edits only the existing M3 object snapshot and adds two focused tests. It never replaces java.util or advances the STRING → arrays → collections gate.

Per-file pass:
1. Inventory `M3ObjectIndexSnapshot` and its existing mask/sort/JNI owners;
2. Inline existing `M3IndexPathMasks.select` delegation conceptually; do not copy native code;
3. Atomize ordinal-to-dense-row-to-physical-slot geometry;
4. Patternize required/excluded full-int mask selection with two existing backends;
5. Comment the lifecycle/immutability/refusal boundaries.

The exact source preimage is SHA-256 `ed19c7b34e23f61ea5bfa808550a1189ccc07900b7022f3572d78c23c5929f0c`.
Postimage SHA-256 `db9e91e59fab8ef1166ee42d2936db0e23a6b59b34165273bfcac5b73382fa7a`.
New self-test and JUnit adapter are manifest-bound; no alternate payload owner or new JNI ABI.

Correctness: physical offsets are validated before selection; required bits match all and forbidden bits none; returns detached int[] row permutation with original object references and stable encounter/sorted order. The caller-owned state plane is not retained. All existing Java21 mutation methods explicitly refuse, even no-op calls.

**Verification:** checked-in SDK recipe test is intended to enforce pre/post SHA, actual OpenRewrite candidate generation, fixed point, and source-drift refusal. Target `M3ObjectPhysicalMaskSelfTest` is intended to enforce Java/JNI parity and invalid geometry. Neither a queued workflow nor a source-hash match is a claim of successful runtime/CI execution. Source-build JDK, jtreg/JCK, architecture/ABI and measured performance remain pending.
