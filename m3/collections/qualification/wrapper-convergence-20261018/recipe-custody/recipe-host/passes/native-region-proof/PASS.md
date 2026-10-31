<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Verification pass native-region-proof

Admits only `m3/collections/src/test/native/m3_mask_window_regions.c` under the parent contract and verification-packets/PLAN.md. This is a verification-only owner. Existing preimages remain exact; new owners explicitly admit ABSENT. Missing-source refusal is inapplicable to an additive target, while wrong representation, drift, duplicate, post-scan, fixed-point and input-order gates still apply. The standalone frozen modes preserve their recorded baseline expectations; the native instrumented table measures resource work separately from actual JVM/JNI correctness.

Preimage: `0b56e30eeda0022644fd6dd9448ccc3689aa7f05e515d5a4087dc0ed02fb4ec2`. Postimage: `3b1de8c0e8d046841425289aa8c0b244b1497ef8033508c0d5fe59ad64b86141`. Actual SDK replay and receiving gates are still required.
