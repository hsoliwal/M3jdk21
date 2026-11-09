<!-- SPDX-License-Identifier: Apache-2.0 -->
# Name-contract two-parent superset repair for M3JDK21 PR #1723

Source parent: master b7ef4cacece061414e56e231f98c76f6932140ca. Donor second parent: 0ac24aaeb340f719aa51453e5b733d8d61aabb73 (original PR #1723).

All five original newly-added source/workflow/test files are donor-exact Git blob SHA values; none existed in the root tree.

## Exact AGENTS.md conflict resolution

- Original master AGENTS.md blob: 4b21951c9f5589267ad72b0e0f934a728eca09ce; it already carries the distinct #1726 naming/formatting rules.
- Donor #1723 has a single AGENTS.md unified hunk at original lines 100..102: three old-context lines and seventeen postimage lines. Exact old lines matched uniquely at effective current-master line 111 (zero-based 110) after #1726 shifted earlier content.
- The canonical strict old/new line counts were checked; no absent/forged EOF marker and no CRLF-normalization were allowed. Applied only that unique additive hunk; no whole-file ours/theirs selection.
- Replayed AGENTS.md Git blob SHA: aef1e6bde9fbd39981c9bb8931888f4b0ace1669. The original #1726 section and the new #1723 section are both present and source file ends with LF. This is a finite exact-context replay receipt, NOT an executed Maven/OpenRewrite Recipe.run receipt.

## Recipe authority / remaining checks

Use original Synexia canonical m3-pr-stack-preservation ExactFileHunkReplay and independent hunk union rather than a new merge engine. The follow-up Maven/OpenRewrite named recipe fixed-point, check.py/test_check.py, whole source-built Java21/HotSpot, ABI/JNI, jtreg, CI and performance gates remain to be observed. Keep existing source rights, Java public API and JNI invariants.

Source PR still open at construction; do not close unrelated drafts merely because their names overlap.
