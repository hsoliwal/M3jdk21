<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Verification pass wrapper-contract-v2

This new packet admits only the absent verification owner `m3/collections/src/test/java/com/m3/collections/M3WrapperConvergenceContract.java` against integration revision `f29b0afe7ad253cc0562bc8dfdd48fc1acec710e`. The exact postimage is `2913aeae2f20b80c669398e5727ad9bc000eccf3bb225d85bdd672c17d4fb847`. The original `wrapper-contract` packet remains unchanged and unqualified for this integration scope.

The parent contract already admits new frozen wrapper oracles under `src/test`; this separately versioned packet selects the successor documented in `../../../candidates-v2/CANDIDATE_NOTES.md` and `../../../catalogue-review/RECONCILIATION.md`. Version 2 adds only the `readonly-policy` mode and normalizes the copyright line. All original methods, modes, comparator-failure assertions, and the default `readonly` entrypoint survive. The inherited upstream comparator regression remains a failing standalone witness; a policy pass cannot establish full wrapper correctness.

The source passed the parent's strict Java 21 compilation against the frozen f29b0afe baseline. Its new policy mode reproduces the original unconditional-removal defect; the read-effect mode passes eight checks, and the comparator mode retains its failure. These are baseline observations, not a candidate policy pass. Their exact receipts are in `work-m3-wrappers-20261018/integration-f29/`.

The existing canonical Java snapshot engine and five unchanged OpenRewrite SDK host tests remain authoritative. The parser classpath consists only of the real frozen integration baseline classes. The packet uses crate identity `m3-wrapper-wrapper-contract-v2` and descriptor path `META-INF/rewrite/m3-wrapper-wrapper-contract-v2.yml`.

Required order: reviewed packet -> lint -> strict host compile -> five SDK tests -> parent target reception and target gates. The SDK must produce this exact postimage, reach a fixed point, preserve unrelated decoys, and refuse drift, duplicate Java sources, wrong source representation, and changes after scanning. Missing-source refusal is inapplicable because this is an explicitly absent target. Reversing the initial one-element decoy set is a bounded replay path, not a distinct multi-input permutation. No target file is written by host qualification, and no production API or global receiving phase is changed.
