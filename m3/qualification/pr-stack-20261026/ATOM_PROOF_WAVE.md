<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 atomic source/test/JNI superset wave — 2026-10-26

This is a **source-content, reviewable Git wave** built from original protected master
`b513047feaefd282c27178588581b93840ba8658` (tree
`9a9fed6b28cdbbb9d7d41ea1312aea35241d04d4`).
M3JDK21 remains one integrated JDK 21/HotSpot/JNI product.

All five donor PR heads, **including drafts**, were inspected by actual GitHub
changed-file URLs and source/base Git blobs. Modified file preimages were
byte-identical to current master before admission. All added file paths were
absent. No donor source branch was rebased, squashed, force-pushed or closed.

| UTC order | source PR | exact donor head | two-parent merge commit | materialized unique paths |
|---|---|---|---|---:|
| 1 | #1939 original five-provider JNI-before-JUnit | `f3de6abf69c2515b239e0a37eecca4aea267dfba` | `1c16bc20a05f62cac87f6c06787d63e77a614dc2` | 2 |
| 2 | #1940 parser-neutral source/UTF-16 AST | `1243e70546de7a39003c9749b95044c255e1488e` | `01cfb39e2303cb50e5182d8ad224758036ae2aaf` | 7 |
| 3 | #1949 tree preorder/state protection | `0075ff0cb1b79c5b834130c7958704a19d8e5dee` | `1dd0f87eb13a12a8b6df528ccdcd84d7de35001f` | 2 |
| 4 | #1950 current-head window/compiler closure | `3769eb91a4cc5582268c52164b9ecde4d70a17a4` | `ccf23d8ca55e76b828d280b49cc9123099ab40c8` | 5 |
| 5 | #1951 tree-window final receiving receipt | `8d4cfa828f8d7018cbd0ef7a4ef3c709c89b9b3a` | `676ab81323469cf1886f39efe3b512be97b1415a` | 1 |

## Superset and no-loss evidence

- **17** active-tree file paths changed compared with the exact base: **10 added, 7 modified, 0 deleted**.
- GitHub fetched all 17 postimage blob IDs from the final branch and they matched
  the exact original donor changed-file IDs (**17/17**).
- GitHub independently fetched all five merge commits; every commit has exactly
  two correctly ordered parents (previous integration tip; original source PR head).
- #1950's head moved while intake was running. The old head was refused;
  the full five-file changed-file list, new SHA and unchanged preimages were
  inspected before materialization.
- #1951's two code/test paths were already byte-identical to #1950's accepted
  postimages; they were preserved rather than overwritten. Its distinct
  qualification path was added. The tree actually changed.
- Production classes, JUnit tests, workflow and source evidence are all retained.
  This proof covers these five PRs **only**, not unrelated open/held PRs.

## Canonical recipe/JUnit ownership and acceptance

The reusable Maven/OpenRewrite atom/JUnit owner is in `hsoliwal/com.synexia`,
with the existing `M3EnsureAtomJUnitRecipe`, original JUnit completeness
and exact-source/atom census, the `m3-pr-stack-preservation` DAG,
and the focused source-sealed candidate
[Synexia #13422](https://github.com/hsoliwal/com.synexia/pull/13422).
This document is **not another merge engine or new atomizer**.

**Required before calling product tests qualified:** named actual OpenRewrite
`Recipe.run` and fixed point, JUnit 5/Surefire full source-atom census, Java 21
module/reactor compiler, C/JNI checked native integration, HotSpot/JDK image,
jtreg, exception/API/ABI/serialization/concurrency and cross-platform
differential probes. Results must be matched to the final combined tree and
original preserved source blobs, not just donor claims.

**Execution status:** source Git-tree and ancestry checks PASS; actual combined
Maven/OpenRewrite/JUnit/JDK/HotSpot/JNI gates NOT_RUN by this integration work.
Do not infer green CI from queued jobs or receipts. Other PRs remain HOLD where
preimages conflict or source/contract proof is insufficient.
