<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Verification pass index-junit

Admits only `m3/collections/src/test/java/jdk/m3/collections/M3IndexOrderWindowTest.java` under the parent contract and verification-packets/PLAN.md. This is a verification-only owner. Existing preimages remain exact; new owners explicitly admit ABSENT. Missing-source refusal is inapplicable to an additive target, while wrong representation, drift, duplicate, post-scan, fixed-point and input-order gates still apply. The standalone frozen modes preserve their recorded baseline expectations; the native instrumented table measures resource work separately from actual JVM/JNI correctness.

Preimage: `ABSENT`. Postimage: `2d0b1bb9a515eefd48c3de29b32dced613c4e9f05c0547d677d8c8f67c004397`. Actual SDK replay and receiving gates are still required.
