<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 additive token-cursor search / JNI verification superset

2026-11-02. **DRAFT / HOLD**; no direct `master` modification or Java platform API change. This specification precedes all merge operations on a new branch `m3/jdk21-search-superset-wave3-20261102`.

## Frozen source inventory

Receiving current-M3JDK21 source-superset [#2337](https://github.com/hsoliwal/M3jdk21/pull/2337) at `9b139f3720d12242c9ac7f160ab5da443f4474c5`, on exact `master@ce005d25b29fb6246e013bffb462afd5ed070362`. Compared to that master, 64 paths (36 added, 28 modified), zero deleted. It already retains the seven independently admitted source PR heads and relevant packaged JDK21/JNI recipes.

Donor [#2341](https://github.com/hsoliwal/M3jdk21/pull/2341) exact head `1f07831c0146facf16dfc051d38c23023f8f14b2`, including its parent token-cursor path (one-lane #2328 and multi-lane #2332), compared to the same master: **38 paths, 37 added + 1 modified, zero deletions or renames**. The modified owner is only `m3/ports/regex-cursor/src/main/java/jdk/m3/regexcursor/M3TokenCursor.java`; its original source must remain the same public descriptor and the existing pure Java/JNI external behavior unless proved. The two changed-path sets (64 and 38) have **zero filename intersection** on complete (<300) GitHub comparisons.

Independent codec [#2325](https://github.com/hsoliwal/M3jdk21/pull/2325) is **already an ancestor** of #2337 by GitHub Git comparison (receiver 46 commits ahead, zero behind). Its three files have **exact matching Git blobs/bytes** on donor and receiver: `VInt.java` `346c05694f6921b8211d15fd5fc1da0cd01bae83`, native `m3_packed_codec.c` `1813a024d4c2ce514cebe4928ab4379b7950ff94`, and passive qualification receipt `35acdae9b6c7982e7c65a27df936c4d2605904ae`. No duplicate codec-copy or unsafe rewrite is needed.

## Additive historical merge protocol

1. Freeze current receiving and donor Git SHAs. Publish this source/release qualification before a transport merge. Original branch histories, file names, modes and recipes remain authoritative.
2. Open one temporary **non-draft transport PR** with #2341 as head and this dedicated integration branch as base. The transport PR is not a release PR, and `master` must remain unchanged. Require GitHub three-way `mergeable=true` and exact expected donor head before calling the real `merge` method (no squash/rebase).
3. Verify actual two-parent merge commit: first parent receiving documentation head; second parent exact #2341 donor head. Compare the complete receiving→merge diff to master→donor for **38 exact (filename,status) rows**, expecting 37 additions, one `M3TokenCursor.java` modification, zero missing/extra/deleted. Preserve all 64 original receiving paths and exact source Git blob witnesses for codec/native and core JDK source.
4. Record actual Git tree and differential identities; open final **draft** review PR to `master`. No default merge without real JDK21 module compilation and source-JNI parity.
5. Explicitly defer any further PR with shared Java source/API changes until hash-pinned three-way semantic reconciliation; no hidden API changes, random sharding or forced merges.

## Category/donor rights and Java/JNI contracts

LeetCode/HackerRank/GeeksforGeeks supply **search problem taxonomy and independent test cases**, not unrestricted algorithm source. Reuse the existing first-party Synexia category-to-primitive/KMP/Horspool recipe owners and pinned, license-admitted GitHub donors for actual code. This donor's `M3TokenCursor.seek` retains lower-bound/offset semantics and public Java signatures by contract; native JDK/JNI library/exports cannot change merely because a proof file was added.

**Release HOLD:** run the original `m3/ports/regex-cursor` standalone Java 21 JUnit/proofs (single lane, multi lane, seek locality, invalid input, signed IDs, grammar/trie projection), actual javac/JNI `-Xcheck:jni` proof, module/POM/full JDK21 reactor, compatibility of classfile/JMOD/native C, baseline runtime tests, disabled-by-default M3 enhancements and performance measurements. Git tree and source-blob proofs are not substitutes for compiler/runtime evidence. User-requested all-repository convergence remains broader than this bounded source integration.
