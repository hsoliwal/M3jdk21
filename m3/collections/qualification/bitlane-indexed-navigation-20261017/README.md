<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Indexed M3BitLane28 successor/predecessor — source-first JDK receiver

**Canonical recipe owner:** [Synexia #12104](https://github.com/hsoliwal/com.synexia/pull/12104), branch m3/bitlane-indexed-navigation-20261017, last pre-receiving source commit 92a2483937e683694900bb21a9fce11773a3a365.

**Product base:** current M3JDK21 master original M3BitLane28 Git blob 1877cf9eea2990ed74b4d10467bf3612e3c470d4. Do not rebase, merge, force push, modify master or overwrite another collections PR. Synexia is the recipe/catalogue convergence workspace and M3JDK21 is the owning product source tree.

## Exact three output candidates

Source manifest RECEIVING.tsv seals each original precondition, resulting Java21/C17 SHA-256 and canonical donor Git blob. One product Java owner has an exact preimage. Two proof files must not exist in the parent branch.

The source recipe class com.synexia.rewrite.M3Jdk21BitLaneIndexedNavigation20261017 composes existing M3HashPinnedJavaSnapshotRecipe and M3HashPinnedTextSnapshotRecipe with real JavaParser LST. The canonical Maven/JUnit fixed-point/refusal suite is M3Jdk21BitLaneIndexedNavigation20261017Test. This product PR *does not recreate* its implementation or mutate its original manifest.

Read-only receipt and verifier are committed **before** installation of the three exact canonical postimages. The verifier makes no repairs or writes. It must refuse drift, symlinks, missing/duplicated proof inputs, wrong Git blobs or imported sources not byte-for-byte identical to templates. If this passes, source was received; no behavioral equivalence is implied by hashes alone.

## File-local algorithm

Existing M3BitLane28.nextSetBit/previousSetBit scans within the current 4096-bit block using up to 64 long words. If the current block has no qualifying bit, jump via the owner-managed two-level Fenwick rank/select index. Next uses select(rank(exclusive nextBlockStart)); previous uses the last ordinal strictly below the current block.

This maintains exactly one authoritative bitmap array state, one already-existing index, unchanged setters/getters/bit counts, lazily allocated pages, zero per-entry objects, stable 28-bit region/block/offset coordinates, and a single-threaded contract. It preserves same-block fast paths. It does **not** create a second mask array or a comparator/predicate wrapper.

No changes to public method descriptors, java.base, GC, HotSpot or production JNI ABI; no heap pointer is stored in native memory.

## Execute

From a checked-out M3JDK21 candidate with Java 21, Python3, GCC C17 and JNI headers:
- python3 m3/collections/qualification/bitlane-indexed-navigation-20261017/verify.py
- python3 m3/collections/qualification/bitlane-indexed-navigation-20261017/verify.py --jni
- mvn -B -ntp -f m3/collections/pom.xml test

The verifier compiles actual Java21 product sources, standalone differential oracle, compares public javap descriptors with original source snapshot, compiles strict C17 native proof, runs -Xcheck:jni under tiered and -Xint. It checks exact source hashes, not partial snippets. The native C code is a test-only checked consumer of Java bitmap methods, not a product optimization kernel.

## Donors, results, release boundary

Canonical catalogue in [Synexia #12104](https://github.com/hsoliwal/com.synexia/pull/12104) compares existing indexed two-level Fenwick, stock JDK BitSet, RoaringBitmap, Eclipse/Guava and LeetCode/HackerRank/GfG categories with provenance and licensing. No donor challenge implementation is copied.

Isolated reconstructed-kernel Java21 proof: 68,012 assertions. Isolated Java/C17 JNI proof: 68,034 checks in each of tiered and interpreter modes, strict warning flags and UBSan. Those were *not* tests against exact installed product sources. The hosted receipt gate compiles actual delivered files, and its results must be read separately.

HOLD: full Synexia Maven/OpenRewrite SDK qualification on pinned source, full M3 collections Maven/JUnit and native platform matrix, source-built JDK/jtreg/JCK, code review, runtime benchmarking and confirmed safe integration with dependent collection PRs. No speedup or complete-programme claims. The M3 String -> arrays -> collections receiving order remains intact.
