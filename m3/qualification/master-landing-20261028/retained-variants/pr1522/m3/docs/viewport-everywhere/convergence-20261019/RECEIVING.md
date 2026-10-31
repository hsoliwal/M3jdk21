<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# M3 collections receiving report

This report is recovered from the published canonical packet in
[Synexia PR #12528](https://github.com/hsoliwal/com.synexia/pull/12528), commit
`74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd`. Its qualified M3JDK21 source epoch
remains `9a164c31b38c7013c179a3ff38b0bb60c199c7fd`. Publication and recovery do
not qualify a later M3JDK21 revision. No tests were run during this recovery.

The existing `M3CurrentMasterConvergence20261019` recipe has a
[60-target manifest][manifest], SHA-256
`5bb24c679cea5b4d6daddd870323401466419f90ad105a845efe94352e708819`.
All 21 changed outputs were emitted and their unchanged replays passed.
The [source intake][inputs] includes all 268 main Java files, 254 baseline test
Java files and seven original POM resources; the [composed candidate][emission]
has 657 admitted files and 264 test Java files. This covers the admitted
default-POM inputs, not the whole repository or auxiliary profiles.

## Existing owners and preserved behavior

The selection updates seven Java owners and one C unit. Filtered terminal scans,
dense linked clearing, primitive counts and Java snapshot selection reuse the
existing collection structures. Keyed admission checks an already published
worker failure after a successful immediate queue offer. Projection direction
conversion preserves equal-key occurrence order. Empty JNI selections skip
unneeded row reads while retaining the second state-validation pass. The selected proof/test bodies and
catalogue accompany those changes. [The exact before/after files are sealed by
the manifest][manifest].

Current physical-state mask APIs, the primitive-mask keyed result ring,
ordinal-window and global-budget rules, failed-lane drain, and fresh caller-owned
map entries remain present. Current contract tests are retained; the additional
keyed regression uses `M3KeyedParallelismConvergenceTest` without replacing the
current admission contract.

**RowTree is a current-source guard reversal.** The qualified baseline already
contains the endpoint guard. The selected body reverses it to SHA-256
`96067f630bcf3288c69410151759fa738fd5968daccf68cd1aed2aeade38fc7b`.
The retained [resource experiment][resource] found endpoint gains but material
regressions in control workloads across three independent alternating JVM pairs.
The [current combined receipt][combined] therefore records
`CURRENT_SOURCE_REVERT_OF_RESOURCE_REJECTED_GUARD`. A fresh combined resource
benchmark was not run.

## Published execution results

| Gate | Recorded result | Published evidence |
| --- | --- | --- |
| Actual OpenRewrite SDK | 3 passing OpenRewrite 8.17.1 PlainText tests; JavaLST and whole reactor NOT_RUN. | [SDK receipt][sdk] |
| Existing recipe kernel | 7,734 passing bounded checks; 60 targets / 21 changes; `exhaustiveSubsets=0`. Separate from SDK execution. | [Kernel log][kernel] |
| Focused current owners | 14 passing tests and six matching paired owner observations. Nine selected public/protected owner/type outputs and all 12 JNI headers are byte-equal to the current baseline. | [Focused receipt][focused] |
| Original complete module | Original offline Maven and independent console each discover 477 tests, with no errors/failures and two native assumptions. All 19 frozen probes pass. | [Full receipt][full] |
| Separate complete native gate | All 477 tests pass with zero errors, failures, skips or aborts. Eleven C units, ten fatal-UBSan libraries and 21 JNI exports pass; checked JNI passes. | [Native receipt][native] |
| Seven combined proofs | Projection Java/absent/native, window Java/native-library environment, and tree Java/native-required pass on complete module classes. Four selected owner/type outputs and all 12 headers match the baseline. The window owner itself is Java-only. | [Combined receipt][combined] |

## Receiving requirements

[PR #1480](https://github.com/hsoliwal/M3JDK21/pull/1480) was rechecked as open on
2026-10-20. Include the exact original `verify.py` and `probe-plan.json` support
files in the receiving PR or its explicit stack. They are outside the current
60-target manifest. [REPRODUCE.md](REPRODUCE.md) maps both receiving targets to
their pinned canonical templates and documents portable commands for the
unchanged verifier and canonical native helper.

[EVIDENCE.json](EVIDENCE.json) records the published commit, exact receipt/source
hashes, support-file identities, tested Maven JARs and source-epoch linkage.
The full and native receipts retain their original execution bytes and dates.
This recovery does not recreate the former unexecuted launcher.

These results do not establish an installed M3JDK21/HotSpot image, jtreg/JCK,
SWT/Skija host behavior, a `M3MaskViewportNative` host, or Android/iOS runtime,
packaging, simulator or device acceptance. Swing has separate source admission
and execution receipts.

[manifest]: https://github.com/hsoliwal/com.synexia/blob/74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd/synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/m3-current-master-convergence-20261019/manifest.tsv
[inputs]: https://github.com/hsoliwal/com.synexia/blob/74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd/synexia-openrewrite-recipes/recipes/m3-collection-refinement-20261019/viewport-convergence/evidence/current-master-reconciliation/SOURCE_INVENTORY.json
[emission]: https://github.com/hsoliwal/com.synexia/blob/74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd/synexia-openrewrite-recipes/recipes/m3-collection-refinement-20261019/viewport-convergence/evidence/current-master-reconciliation/EMISSION_RECEIPT.json
[sdk]: https://github.com/hsoliwal/com.synexia/blob/74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd/synexia-openrewrite-recipes/recipes/m3-collection-refinement-20261019/viewport-convergence/evidence/current-master-sdk/receipt.json
[kernel]: https://github.com/hsoliwal/com.synexia/blob/74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd/synexia-openrewrite-recipes/recipes/m3-collection-refinement-20261019/viewport-convergence/evidence/current-master-reconciliation/kernel-qualify.log
[focused]: https://github.com/hsoliwal/com.synexia/blob/74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd/synexia-openrewrite-recipes/recipes/m3-collection-refinement-20261019/viewport-convergence/evidence/current-foundation-validation/attempt02/RECEIPT.json
[full]: https://github.com/hsoliwal/com.synexia/blob/74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd/synexia-openrewrite-recipes/recipes/m3-collection-refinement-20261019/viewport-convergence/evidence/current-master-reconciliation/verification/receipt.json
[native]: https://github.com/hsoliwal/com.synexia/blob/74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd/synexia-openrewrite-recipes/recipes/m3-collection-refinement-20261019/viewport-convergence/evidence/current-native-convergence-attempt01/execution/canonical-output/receipt.json
[combined]: https://github.com/hsoliwal/com.synexia/blob/74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd/synexia-openrewrite-recipes/recipes/m3-collection-refinement-20261019/viewport-convergence/evidence/current-master-reconciliation/combined-proofs/receipt.json
[resource]: https://github.com/hsoliwal/com.synexia/blob/74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd/synexia-openrewrite-recipes/recipes/m3-collection-refinement-20261019/viewport-convergence/evidence/rowtree-resources/attempt02/RECEIPT.json
