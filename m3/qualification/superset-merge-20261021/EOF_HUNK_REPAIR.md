<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Repaired held source file from M3JDK21 #1621

This is a child of the existing draft M3JDK21 #1647 integration stack, not a change to protected master. Its first parent is exact stage head af82961b64b2501610d674ede66580bdb344a138, second parent is original donor PR #1621 exact head d736a12256ebe565a883680ac9c9a9708ad7ca76. No donor head is rewritten, no squash/rebase/force occurs and no previous receipt is deleted.

## One exact file-local hunk repair

Previously held path: m3/docs/synexia-recipe-ownership-classification.tsv. Stage preimage Git blob f9374e36999960c203ab0077eebb8061219bc819 contains no physical LF at the end. Original GitHub PR #1621 patch has a single @@ -39,4 +39,6 @@ hunk and an exact missing-final-LF marker on the old last line. The existing Synexia OpenRewrite/Maven source recipe was enhanced in [com.synexia PR #12677](https://github.com/hsoliwal/com.synexia/pull/12677), stacked on the original per-file replay recipe #12662.

The real GitHub patch was independently checked against its exact source. Resulting content equals original donor Git blob 9a9fa4164d19b02db17203bac1e3a872ecbc11e5, including final LF, not a guessed/fuzzy/whole-file preference. This commit restores exactly that original donor blob; no generated Java source was hand-edited.

In original #1647 tree, the other **12 of 13** source paths of PR #1621 were compared one by one against this exact donor commit and their Git blobs matched. The 13th now equals donor. See REPAIR_RECEIPTS_1621.tsv for per-file identity and the retained prior HOLD record in HELD_HUNKS.tsv. The original 12 source file modes and contents are not changed by this child candidate.

**Correct interpretation:** source-content materialization of all 13 PR #1621 paths and real two-parent Git ancestry is now proposed for the review branch; neither proves merged master acceptance, Maven/OpenRewrite SDK compliance, behavior, API/JNI or full-runtime correctness.

## Executed gates and honest holds

The corrected Synexia production recipe class was compiled locally with strict Java21, and a direct harness passed 24 assertions; seven Jupiter-compatible source test methods were compiled with API stubs and executed locally (actual Maven JUnit5 was NOT RUN). The exact #1621 previously held hunk was independently replayed against GitHub's source/donor content, and the computed candidate Git blob was byte-identical to the donor.

**NOT RUN / HOLD:** complete original Maven/OpenRewrite recipe lifecycle/fixed point, full M3JDK21 Java/HotSpot/JNI builds and test suites, jtreg/JCK and ABI/contracts, combined module tests/CI and cross-platform benchmarking. No merge to master; all other source PRs stay open/unchanged.
