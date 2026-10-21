<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Verification pass index-contract

Admits only `m3/collections/src/test/java/com/m3/collections/M3IndexOrderWindowContract.java` under the parent contract and verification-packets/PLAN.md. This is a verification-only owner. Existing preimages remain exact; new owners explicitly admit ABSENT. Missing-source refusal is inapplicable to an additive target, while wrong representation, drift, duplicate, post-scan, fixed-point and input-order gates still apply. The standalone frozen modes preserve their recorded baseline expectations; the native instrumented table measures resource work separately from actual JVM/JNI correctness.

Preimage: `ABSENT`. Postimage: `7c1c055ec9a58a9c807fc05be3ec4a83b0a411729f41c259e3180d1e45262b94`. Actual SDK replay and receiving gates are still required.
