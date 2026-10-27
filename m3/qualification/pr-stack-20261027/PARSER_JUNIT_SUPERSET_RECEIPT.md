<!-- SPDX-License-Identifier: Apache-2.0 -->
# 2026-10-27 native M3JDK21 parser/JUnit source-superset wave

**Source custody, not product acceptance.** M3JDK21 remains one integrated Java21/JNI/HotSpot SDK. Canonical recipes/atom extraction and exhaustive JUnit generation live in `hsoliwal/com.synexia`; do not create a duplicate recipe runtime in this repository.

Original protected `master`: `05e947be6fe9a25d3ca0b4803777beebfb52876a` (tree `a21b33d80553ddff8c7c75608092ee458c70441a`). Original four *draft* PRs received in UTC creation and parent-before-child order as genuine two-parent Git merge commits:

| Draft PR | Original donor HEAD | Actual two-parent merge commit | Paths |
|---|---|---|---:|
| [#1995](https://github.com/hsoliwal/M3jdk21/pull/1995) original JavacTask AST capture and JUnit | `d8b7627e2bb80aaa9f7707ae976642d60c73d607` | `e8924efa2372794924d1dffa981a6fbfeba7715f` | 3 |
| [#1999](https://github.com/hsoliwal/M3jdk21/pull/1999) M3File Unicode codepoint coordinates + existing parser build | `b926eefc360e155f6ee486b39cf3b1642cb4c47a` | `ca80b57a880058fd10a169e549d70d7cca360f22` | 5 |
| [#2004](https://github.com/hsoliwal/M3jdk21/pull/2004) owning array-mask Java21/JNI acceptance workflow | `e71bd70968edf88b030101ce9a84abc10125e675` | `3345ff153a6b55405de5a9c8c4569cef7b98a3b5` | 1 |
| [#2005](https://github.com/hsoliwal/M3jdk21/pull/2005) real generated ANTLR4 parser Java21 test, stacked on #1999 | `5235038e1152c19e7b4f51906fdea58b73c0e72f` | `80e813ee2f84e472c697d2c2b5f6d5c84ef67f83` | 5 |

All **four** receiving commits were independently read back at GitHub and have the correct first-parent chronological integration tip and original donor HEAD as second parent. GitHub compared the active tree with the pinned baseline: **14 source paths, 13 added, 1 modified, 0 deleted**, ahead 10/behind 0 at initial proof. The **14/14 final blob SHAs were independently fetched** and match their exact original GitHub donor file SHA IDs.

All new paths were individually verified absent prior to receiving. The one replaced existing file, `m3/tooling/module-packs/parser/build.py`, was checked on live master, original merge base and receiving parent: identical blob `ad30df42695aeed675380b3e2ca3e435046d78f3`. Incoming exact donor postimage `366fbe10339a97d1f65d5c4e63e3a9097a359cf7` preserves unchanged source semantics except that source PR's reviewed addition; no wholesale conflict preference.

**Canonical proof continuation:** Reuse Synexia `M3EnsureAtomJUnitRecipe`, `M3GenerateMissingAtomJUnitRecipe`, `M3AtomJUnitCoverageLedger`, `M3AtomJUnitFleetMergeGate` and `m3-pr-stack-preservation`. Generate one RED test obligation per meaningful behavioral leaf that lacks directly attributed, executed JUnit; do not accept test file existence as proof. **NOT RUN by this integration:** real Maven/OpenRewrite Recipe.run and fixed point, parser/JUnit combined module suite, ANTLR4 generated lexer/parsers and whole JDK/jlink, native/JNI/HotSpot/jtreg, complete API/ABI/runtime differential and hosted CI. These must pass before protected master promotion.

This is a bounded, reviewable **source superset** rather than the completion of all outstanding draft PRs; hold stale/conflicting patches for serial recipe repair and actual no-loss validation. Do not squash or rebase away original Git parent identities.
