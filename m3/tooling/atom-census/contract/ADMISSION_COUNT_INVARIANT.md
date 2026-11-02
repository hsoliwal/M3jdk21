<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->

# Admission count consistency (M3-ATOM-ADMISSION-COUNT/1)

An atom is admitted only when `admissionFindings()` reports no blocker for
that atom. Every recorded `FAIL` is a blocker, even when that gate is not
in the atom kind's required proof profile. A non-required `NOT_RUN` is
not by itself an admission blocker.

This correction aligns the `admittedAtomCount()` and `pending` summary
with the verifier's existing fail-closed `admit` behavior. It does not
relax mandatory proof, permit contract changes, synthesize evidence, change
a JDK API, or modify Java/JNI/HotSpot product code.

## Mechanical regression

- A fully verified declaration and a fully verified method: admitted=2.
- Add optional `CONTRACT_CHANGE_AUTHORIZED=FAIL` for the locked declaration:
  findings include `RECORDED_FAIL`, admitted=1, pending=1, admission refuses.
- Replace the optional failure with `NOT_RUN`: admitted=2 and no blocker.
- Unknown patterns, required incomplete gates and unauthorized unlocks
  retain their existing refusal semantics.

Canonical source-sealed recipe: Synexia
`recipe-crates/m3jdk-atom-admission-count-20261101`. This file is
an additive postimage, not a proof of product-wide atom coverage.
