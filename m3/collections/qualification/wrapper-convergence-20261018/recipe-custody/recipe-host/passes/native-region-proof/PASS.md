<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Verification pass native-region-proof

Admits only `m3/collections/src/test/native/m3_mask_window_regions.c` under the parent contract and verification-packets/PLAN.md. This is a verification-only owner. Existing preimages remain exact; new owners explicitly admit ABSENT. Missing-source refusal is inapplicable to an additive target, while wrong representation, drift, duplicate, post-scan, fixed-point and input-order gates still apply. The standalone frozen modes preserve their recorded baseline expectations; the native instrumented table measures resource work separately from actual JVM/JNI correctness.

Preimage: `ac09456844268b592475d9da1e6eee6f154e741c893167cf21d675010580deab`. Postimage: `64f0b8605f8dfbe18ff9dd9905b925c4feac3571715e99bc20b28db2a23db41c`. Actual SDK replay and receiving gates are still required.
