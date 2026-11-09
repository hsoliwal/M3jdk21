<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 chronological source-superset wave, 2026-10-21

**Eight PRs have real two-parent source-merge commits on this integration branch. This is source materialization, not all-PR closure or product acceptance.** M3JDK21 remains the integrated Java21/HotSpot/JNI SDK, not an external wrapper.

## Receipts and chronology

- Wave started from protected master SHA 5eeb841168ed420085223806fefcfca8d1b6b3ca.
- Donor order is #1719, #1720, #1725, #1727, #1728, #1729, #1730, #1731. Each first parent is the previous integration tip; each second parent is the pinned source PR head. Original donors were not rewritten or closed.
- Original-root comparison before concurrent-master reconciliation: 51 changed paths; 30 additions, 21 modifications and zero deletions. Each source received the actual donor blob SHA rather than creating a replacement implementation.
- PR #1730 has 21 source/contract/test files and its original base SHA is exactly the wave root. GitHub reported zero path overlaps with the earlier 24 changed paths.
- A concurrent master update was incorporated by real two-parent merge b1f167b1e48d65dd242e873661cd4ea8b27754ca. Second parent 51259ccbdc9b17dc7a76a68730acd92f8b278525; master naming-contract files are retained. The #1727 capability-map donor blob is identical in both branches.
- Effective comparison against that master is 50 paths: 29 additions, 21 modifications, zero deletions. See WAVE_RECEIVING.tsv for exact source heads, merge commits and tree receipts.

## Existing canonical recipe, not a new merge engine

Preserve and refine Synexia's synexia-openrewrite-recipes/crates/m3-pr-stack-preservation, including PrStackRecipeDag, GitHubPatchMboxGate, GitHubPatchHunkChronology, ExactFileHunkReplay, IndependentHunkUnion, StackConflictResolver, MergeCodeLossGuard and MergeReadback. Relevant Synexia pull requests: https://github.com/hsoliwal/com.synexia/pull/12662 , https://github.com/hsoliwal/com.synexia/pull/12677 , https://github.com/hsoliwal/com.synexia/pull/12692 and https://github.com/hsoliwal/com.synexia/pull/12740 .

Every LLM-assisted production mutation requires a reusable Maven/OpenRewrite recipe (or qualified native companion), serial per-file behavioral atoms, exact source preimages, differential tests and second-pass fixed point. Fix the recipe rather than hand-editing generated files. Preserve existing Java/JDK interfaces, native ABI, observable behavior, source rights and donor provenance.

## Explicit gates

NOT RUN by this wave: canonical real Maven/OpenRewrite SDK Recipe.run and JUnit fixed-point suite; full original JDK21/HotSpot/JNI compilation; M3File/M3FS owning reactors; jtreg/API/ABI, cross-platform and full differential oracles; end-to-end benchmarks. Git blob equality and Git ancestry are *not* evidence that those tests passed. CI must check the exact combined branch before promotion.

## Unresolved PRs remain in custody

- PR #1722 overlaps #1719 in language-resource TSV end-of-file append and Java oracle expectations. Original #1719 material is in this tree. Reconcile all fourteen #1719 language additions with the nine separate #1722 additions using the canonical exact-hunk replay/independent union recipe; do not replace a whole file and lose records.
- PR #1723 is a separate naming contract/tools change. New master already includes #1726 naming rules; reconcile #1723 additions and its independent AGENTS.md hunk separately, avoiding duplication.
- PR #1724 has an open parent dependency; retain and qualify the parent source before materializing it.
- Remaining older conflicted ready/draft PRs require systematic chronological hunk/atom replay. Their source heads are not falsely declared merged by this limited wave.

## Promotion

Refresh master and donor SHAs, run canonical source/recipe and build gates, correct any failing recipe, then use GitHub's normal merge action with expected head SHA. Never force push or bypass branch protection. Verify the active merged tree, not Git ancestry alone.
