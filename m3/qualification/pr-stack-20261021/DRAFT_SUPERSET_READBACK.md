<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 draft source-superset wave — 2026-10-21

M3JDK21 is one integrated Java 21/JNI/HotSpot SDK. This branch is a **source-content** integration candidate, not a metadata-only merge. It starts at original protected `master` commit `6ac3d5834844d444979ab1f7b8c85e68982759b7` (tree `547d5abade0366baef31625436aea7bbfb4b2923`).

Five independent source PRs, including **drafts**, were carried in chronological PR-creation order as **real two-parent merge commits**. Every production, test, recipe/qualification and workflow blob came from the exact donor PR head. There was **no force push or donor head rewrite**.

| PR | exact donor head | our two-parent merge commit | resulting tree | source paths |
|---|---|---|---|---:|
| [#1649](https://github.com/hsoliwal/M3jdk21/pull/1649) | `9a28114c752c60eddc75bf8b5b2ce4029037bfe1` | `6344cefb141f3406ef973c410fa105114f7f58a6` | `88762efdd7daa4da548ed26bd3d2401ac9f1e985` | 3 |
| [#1655](https://github.com/hsoliwal/M3jdk21/pull/1655) | `cf89841d63c73706fb67a16ef83c132d86c8fcd5` | `59948d72164c8dec8aabb9c857dcb54d6b3dba69` | `11283f2d61b49b744eff4e94bb9491f9e529d864` | 3 |
| [#1662](https://github.com/hsoliwal/M3jdk21/pull/1662) | `ef4d19ef58bccf7927feea7c0091cdea7e669f28` | `178122127ea1f5c6a580f8971f6280075135efce` | `2f3b860a53c75fa9a63944b56f08ec6ca2546484` | 5 |
| [#1669](https://github.com/hsoliwal/M3jdk21/pull/1669) | `e5a0849ac4cf13dcf62c486025be21227fdc3b9a` | `0227b00499717be38b32f487896c970a49d378e1` | `f3fc7fd3619d976c0ab3bdcf86eff9009c9c7422` | 6 |
| [#1671](https://github.com/hsoliwal/M3jdk21/pull/1671) | `f5be7ab58b1b201d6d34e6cbf3649bf61682600f` | `e3efc82f92e9449f55fe4e1e55d0cd188655bf1c` | `7667b6e97959b73aea7f73f1c9d5dea2985f660e` | 5 |

## Exact Git tree readback

- GitHub compared the final source tree with original `master`: **22 changed paths, 16 added, 6 modified, 0 deleted**, branch 29 commits ahead and 0 behind at first readback.
- Each of the five commits was independently read back as a **two-parent commit** with first parent the previous integration tip and second parent its exact source PR head; all five resulting tree IDs match the tree creation receipts.
- **22 of 22** final active-tree Git blob IDs were independently read back and matched their original donor PR changed-file blob IDs, including all **six existing modified source files**.
- For each replaced existing file, the active preimage Git blob ID equaled the source PR's original base file Git blob. Every added path was confirmed absent on original `master`.
- No overlapping source paths existed among these five admitted PR file lists. This does **not** imply semantic independence or that the suite passes.

## Recipe and promotion boundary

This wave preserves the donor recipe/test/qualification files and avoids stale, wholesale conflict preferences. Canonical cross-repository OpenRewrite/Maven recipe owner: `hsoliwal/com.synexia` `synexia-openrewrite-recipes/crates/m3-pr-stack-preservation`, continued by [#12662](https://github.com/hsoliwal/com.synexia/pull/12662) and [#12677](https://github.com/hsoliwal/com.synexia/pull/12677).

**HOLD for `master`**: full named OpenRewrite `Recipe.run` / fixed-point, combined Maven/Java21 owner/reactor tests, native C/JNI/HotSpot build, jtreg/ABI/behavior and Linux/Windows/Android platform tests, CI and integrated performance evidence are not proved by this Git-tree receipt. Re-run the canonical recipes, fix recipes rather than hand-edit generated sources, and merge this branch to protected `master` only after acceptance.

Other open PRs (including older B01/B02 holds and conflicting drafts) are **not** declared merged or code-retained. Continue chronological, parent-first, contract-preserving reconciliation with those exact file/hunk patches.
