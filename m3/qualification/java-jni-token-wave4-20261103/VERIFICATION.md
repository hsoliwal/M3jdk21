<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 token proof wave 4 — exact executed source integration receipts

2026-11-03. Source/history merge **EXECUTED** on a new integration branch, `master` **NOT MUTATED**. Real Java21/JNI compiler-runtime/ABI gates on this merged head **NOT EXECUTED**, still DRAFT/HOLD.

## Exact source and branch ancestry

Receiving head [#2344](https://github.com/hsoliwal/M3jdk21/pull/2344): `86a9463933670782a0ecbe6abbe47d17b461db61`. Original frozen `master@ce005d25b29fb6246e013bffb462afd5ed070362`. Its original token cursor donor [#2341](https://github.com/hsoliwal/M3jdk21/pull/2341) is an ancestor at `1f07831c0146facf16dfc051d38c23023f8f14b2`. The review-only receiving source contract was committed **before** either merge at `410de45a2e8981d3985db45088d1ffaf7b7bb753`.

- [#2345](https://github.com/hsoliwal/M3jdk21/pull/2345) head `57efa8d104ca273272ad32a36107446874268085` (borrowed token-presence fact correctness): actual GitHub merge-method transport [#2348](https://github.com/hsoliwal/M3jdk21/pull/2348) produced **two-parent merge `25d0faf91101bca8f881b8f7e8d06eee5cc3a04e`** with parents [`410de45a2e8981d3985db45088d1ffaf7b7bb753`, exact donor]. Git tree `b720aafafae7db3727d083f1eecb2f2c1026a23f`. Full `compare(receiving,merge)` filename/status rows equal the independently checked `compare(common #2341, donor)`: **five entries, two added, three modified Java sources, 0 removed/missing/extra**.
- [#2347](https://github.com/hsoliwal/M3jdk21/pull/2347) head `f3791ffa0d1a56bad43741ea8004586293f1f3eb` (includes #2346 original compile-before-native-test ordering fix and restores missing original `M3CompleteTokenProjectionProof` plus JUnit bridge): transport [#2349](https://github.com/hsoliwal/M3jdk21/pull/2349) produced **two-parent merge `9b192342c0431df7611c9a4f4f73c3826a7980c3`**, parents [`25d0faf91101bca8f881b8f7e8d06eee5cc3a04e`, exact donor], Git tree `531b56caa4ef640c1b354f47a9d2ceb52eaee383`. Full diff set matches `compare(common #2341, donor)`: **six entries, five added, one existing script modified, 0 removed/missing/extra**.
- Both GitHub mergeability signals were checked before triggering the exact head-pinned `merge` API. Neither operation modified default `master`, squashed commits, force pushed or changed source names. The complete donor changed-path sets are disjoint (5+6 = 11). Source modes and parent object identities retained.

## Full source Git blob readback witnesses (seven, PASS)

| Source | Git blob identical on original parent and integrated result |
|---|---|
| `M3TokenCursor.java` borrowed-token safety owner | `2e4df89ac667973e979e3568f497adbb6fe5a069` |
| `M3CursorMatcher.java` | `eced91ed00c48b50ce59d3325698e8b75194b0ea` |
| `M3GrammarTokenProjection.java` | `1945dad38b2285af75c3b394357097748e5b5032` |
| Newly restored `M3CompleteTokenProjectionProof.java` | `7995260401c07a8d6e5ba5fd26b8782fef9fbaeb` |
| New `M3CompleteTokenProjectionTest.java` Jupiter bridge | `a4bcdfd7bd89ab5a9e0552d24d70c9fb131cb398` |
| Existing `verify-token-cursor-seek-locality-20261102.sh` ordered runner | `9c2f90eb5fb327136538a76601779ba5e6a7d5b0` |
| Existing packed JNI/native C codec, inherited unmodified | `1813a024d4c2ce514cebe4928ab4379b7950ff94` |

All seven were independently fetched from their pinned original donor/receiver and merged tree, with exact Git blob SHA **and entire source text** equal. Before admission, original #2341 cursor and #2345 corrected cursor had the same ten ordered public/protected declaration lines. **This is a source API skeleton check only**, not JVM/JMOD binary/observable runtime proof.

## Gating receipts and explicit holds

The original [#2347](https://github.com/hsoliwal/M3jdk21/pull/2347) proof references a genuine Java21 Maven/Jupiter success in a prior separate workflow, and a JNI job that failed at strict Java compilation because the complete projection proof file was missing. This source restoration fixes that compile-input omission, but **the combined integration head has not yet demonstrated a passing JNI run**.

The actual required order is: source/recipe inventory + prehash → merge/diff integrity → lint → `javac --release 21 -Xlint:all -Werror` with all original sources/proofs → strict C17/JNI compile **before** native/Java runtime tests → source-sealed Maven/OpenRewrite `Recipe.run` fixed point/refusal → genuine JUnit/JNI suite with non-skipped XML → full JDK21 reactor/JMOD/native and platform compatibility. No inversion or silent replay.

LeetCode/HackerRank/GeeksforGeeks supply algorithm categories (token search, binary lookup, first/count/all offsets), not copying authority over external submissions/editorials. Only first-party and license-admitted GitHub donors are eligible for source reuse. No standard JDK21 public API or original native library export was modified or qualified for release here. **Keep the final review PR DRAFT; no default `master` merge**.
