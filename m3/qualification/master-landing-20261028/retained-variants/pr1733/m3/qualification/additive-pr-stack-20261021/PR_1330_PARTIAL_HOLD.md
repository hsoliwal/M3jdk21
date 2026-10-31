<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original #1330 additive array-collection wave — candidate only

Original source PR: https://github.com/hsoliwal/M3jdk21/pull/1330
Pinned original head: `8357d7d4bac0847b6b5bc5f74d4eff4ec9b32fc7`.
Pinned receiving master source: `51259ccbdc9b17dc7a76a68730acd92f8b278525`.

This candidate materializes **eight previously absent first-party source/test/receipt paths as their exact donor Git blobs**, with original `100644` modes confirmed by Git Data tree inspection. It preserves every other current source file from `51259ccbdc9b17dc7a76a68730acd92f8b278525`. There is no duplicated M3 runtime facade: M3JDK21 remains the integrated Java21/JNI SDK and these original collection owners have parent-owned dynamic arrays, offsets and state masks.

**Two original Java owner files are intentionally HELD rather than replaced**:

- `M3ArrayDeque.java`: incoming `f30b0694bd3a9d2bdb40fa889e85b601194ff7d8`, receiving `9df441b51b1912fcaece88197b0e69e6fe89986a`.
- `M3PriorityQueue.java`: incoming `7f6a6d4836ed1ae6329502373475467a2a0780b4`, receiving `bd462291a8a33862d943e0afc5dd839693548e8b`.

Both need the original Synexia Maven/OpenRewrite per-file atom/hunk resolver and exact public contract/Java/JNI differential tests. Do not select ours or theirs. The missing and held paths are recorded in `PR_1330_FILE_RECEIPTS.tsv`.

**This commit is intentionally ONE-PARENT and does not claim PR #1330 merged.** An original donor second parent may be included only after all ten source paths are provably materialized without code loss. This branch is for review and test qualification, not final runtime promotion. Full Java21 Maven/JUnit/ABI/HotSpot/JNI/CI and multi-platform gates: **HOLD / NOT_RUN**. No original PR was closed or rewritten; no force/squash/rebase performed.

The algorithm catalogue and receiver contracts are preserved as original donor blobs, not copied LeetCode/HackerRank/GeeksforGeeks challenge code. Follow the existing category review and reusable recipe-first invariant.
