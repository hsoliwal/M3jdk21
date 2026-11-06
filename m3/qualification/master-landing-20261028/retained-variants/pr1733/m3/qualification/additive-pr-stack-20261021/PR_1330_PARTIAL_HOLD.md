<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original #1330 additive array-collection wave — candidate only

Original source PR: https://github.com/hsoliwal/M3jdk21/pull/1330
Pinned original head: `8357d7d4bac0847b6b5bc5f74d4eff4ec9b32fc7`.
Pinned receiving master source: `51259ccbdc9b17dc7a76a68730acd92f8b278525`.

This candidate materializes **eight previously absent first-party source/test/receipt paths as their exact donor Git blobs**, with original `100644` modes confirmed by Git Data tree inspection. It preserves every other current source file from `51259ccbdc9b17dc7a76a68730acd92f8b278525`. There is no duplicated M3 runtime facade: M3JDK21 remains the integrated Java21/JNI SDK and these original collection owners have parent-owned dynamic arrays, offsets and state masks.

**Two original Java owner files are intentionally HELD rather than replaced**:

- `M3ArrayDeque.java`: incoming `ce144dd8a77c9634874203f0aac61e299733de92`, receiving `25d61c5e3e0bdf28a75e66de03da7256761e8fbb`.
- `M3PriorityQueue.java`: incoming `94f6556b8486d299cd3dcbb5216d8745defcf05e`, receiving `795e6036e0e6ef4cf4e3f04f6fbe61b544583a2a`.

Both need the original Synexia Maven/OpenRewrite per-file atom/hunk resolver and exact public contract/Java/JNI differential tests. Do not select ours or theirs. The missing and held paths are recorded in `PR_1330_FILE_RECEIPTS.tsv`.

**This commit is intentionally ONE-PARENT and does not claim PR #1330 merged.** An original donor second parent may be included only after all ten source paths are provably materialized without code loss. This branch is for review and test qualification, not final runtime promotion. Full Java21 Maven/JUnit/ABI/HotSpot/JNI/CI and multi-platform gates: **HOLD / NOT_RUN**. No original PR was closed or rewritten; no force/squash/rebase performed.

The algorithm catalogue and receiver contracts are preserved as original donor blobs, not copied LeetCode/HackerRank/GeeksforGeeks challenge code. Follow the existing category review and reusable recipe-first invariant.
