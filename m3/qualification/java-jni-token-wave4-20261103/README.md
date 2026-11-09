<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 additive source wave 4 — token-cursor fact safety and JNI proof-chain repair

2026-11-03. Documentation FIRST. New integration branch `m3/jdk21-token-proof-wave4-20261103`, review only. **No `master` mutation, no Java platform API or JNI exported ABI unlock.**

## Exact parent and donor source custody

Current receiving [#2344](https://github.com/hsoliwal/M3jdk21/pull/2344) head `86a9463933670782a0ecbe6abbe47d17b461db61`, already includes token-cursor #2341 at `1f07831c0146facf16dfc051d38c23023f8f14b2` as an exact ancestor. Its original `master` is `ce005d25b29fb6246e013bffb462afd5ed070362`.

1. [#2345](https://github.com/hsoliwal/M3jdk21/pull/2345), exact source `57efa8d104ca273272ad32a36107446874268085`, stacked directly on #2341 common ancestor. Against common ancestor **five paths**: 2 new (contract/JUnit) and three modified existing Java owners (`M3CursorMatcher.java`, `M3GrammarTokenProjection.java`, `M3TokenCursor.java`); no removals. Preserves all ten ordered public/protected declarations in `M3TokenCursor`, but replaces potentially stale borrowed token-presence facts with rescanned/owned evidence. Original cursor Git blob `5d03f64b310c2cc9ed232cfb91ce8eea0e0e509a`; candidate `2e4df89ac667973e979e3568f497adbb6fe5a069`. This modifies `M3TokenCursor.java` only from its original #2341 source, not from unrelated bytecode or JDK public API.
2. [#2347](https://github.com/hsoliwal/M3jdk21/pull/2347), exact `f3791ffa0d1a56bad43741ea8004586293f1f3eb`, includes #2346 source verification-order predecessor at `4809234ad312e1a84620060cc1483409e0d4ea4a`. Relative to the same common ancestor its complete source delta is **six paths (five additions, one test-runner modification)**, zero removed: ordered Java21/C17/JNI compile-before-test runner, one GitHub workflow, documentation/guard and both actual missing complete-token proof and JUnit bridge. This is **test/verification only**, not a native implementation change.

The two donors' six- and five-path changed-file sets are **mutually disjoint** against the exact shared #2341 parent. The #2344 receiver's `M3TokenCursor.java` is **byte-identical** to the common #2341 original, and its Java API line skeleton matches the candidate; there is no need to discard or replace any other receiver source. The existing packed `VInt.java` and `m3_packed_codec.c` Git blobs remain pinned to #2325, which is already an ancestor.

## Deterministic additive procedure

- Freeze the exact receiver and donor head SHAs, and this README before writing another source file.
- Merge #2345 first and #2347 second using temporary transport PRs **only targeting the new integration branch**. Require GitHub `mergeable=true` and exact source head checks. Use real `merge` to preserve both donor and receiving histories, never squash/rebase/force-push.
- After each, verify that `compare(previous receiver, merge)` has the **exact (filename,status) entries** of `compare(common #2341, donor)`, with no additional, missing, renamed or deleted entries; verify actual two merge parents and postmerge branch ref. If the verification differs, STOP before accepting another donor.
- Compare full Java/JNI owner Git blobs to their pinned source parent, especially `M3TokenCursor.java`, `M3CursorMatcher.java`, `M3GrammarTokenProjection.java`, missing projection proof/JUnit, existing packed Java/C codec, and no native implementation ABI modifications.
- Record exact proof in `VERIFICATION.md` and publish one **draft** successor review PR to `master`; default branch stays untouched until real Java21/JNI acceptance.

## Hard release gates

The native JDK/JNI gate must run Java21 module compilation **before** JUnit, C17 compilation before native tests, source-sealed Maven/OpenRewrite SDK replay and fixed-point/refusal, all borrowed-array and owned token-cursor differential and regex matcher tests, full JDK/JMOD build, cross-platform linked library checks and performance/exception paths. A GitHub mergeability flag and Git object SHA are source custody, **not** runtime proof.

LeetCode, HackerRank and GeeksforGeeks provide category/problem test shapes (first, overlapping count, all offsets, binary seek and token window), **not permission to copy challenge source or editorial solutions**. Use the existing first-party Synexia/M3JDK21 source-sealed recipes and eligible licensed GitHub donor algorithms. No public `Object.toAdapter` signature change, JavaSE/JNI API declaration, native export or binary ABI change is authorized.
