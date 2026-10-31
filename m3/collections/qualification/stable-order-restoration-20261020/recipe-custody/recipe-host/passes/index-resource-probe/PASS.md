<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Verification pass index-resource-probe

Admits only `m3/collections/src/test/java/jdk/m3/collections/M3IndexOrderWindowResourceProbe.java` under the parent contract and verification-packets/PLAN.md. This is a verification-only owner. Existing preimages remain exact; new owners explicitly admit ABSENT. Missing-source refusal is inapplicable to an additive target, while wrong representation, drift, duplicate, post-scan, fixed-point and input-order gates still apply. The standalone frozen modes preserve their recorded baseline expectations; the native instrumented table measures resource work separately from actual JVM/JNI correctness.

Preimage: `ABSENT`. Postimage: `a9dfb46285a65001c135fda31e3b69fe7a3882e00412029fe2abd64b1384fb9e`. Actual SDK replay and receiving gates are still required.
