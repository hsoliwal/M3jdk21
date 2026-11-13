<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Candidate receiving receipt: primitive priority heapify

Source authority: private Synexia draft [#14086](https://github.com/hsoliwal/com.synexia/pull/14086),
exact canonical commit `f86a6668152dfa5218d0e1086b75e01349a5511e`.
Receiving source paths use the three exact files from its hash-pinned Java snapshot,
not a second independently invented implementation. Original M3JDK21 preimage
Git blobs: `7816135483c11e4c898e9ecd79443ac96085fa99`,
`7ea8b9cd3ebe1d582026a98649d8598c72789cc3`. The helper path was absent.

Acceptance boundary: additive bulk constructors and cold-path policy extraction only.
No changes to existing offer, removal, iterator, JNI or external dependency rules.

Local independent Java 21 algorithm proof: 1,000 heap sizes (0..999), 499,500
removals per primitive width compared to JDK PriorityQueue; 100,000 capacity-policy
comparisons to original reference formulas. This proof used a standalone harness;
it did not compile the exact repo sources or run full JUnit.

**Open:** actual recipe execution and fixed-point/refusal proof, strict source-built
Maven M3JDK21 collection suite, native/platform/jtreg gates and representative
performance qualification. This is a draft candidate, not a merge or release gate.
