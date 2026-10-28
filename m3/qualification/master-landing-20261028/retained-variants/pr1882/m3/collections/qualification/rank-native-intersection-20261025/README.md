<!-- SPDX-License-Identifier: Apache-2.0 -->
# Original M3JDK21 JNI intersection value materialization — October 25, 2026

This is **M3JDK21 product-source refinement**, not an `M3 + JDK` adapter or new runtime abstraction. It changes the existing Java rank-index/batch owners and the original native `m3rankbatch` C file, plus exact Java contract/JUnit sources and an Actions gate. It is not yet a replacement inside the installed `java.base` JMOD, so this PR must remain DRAFT until those further integrations qualify.

## Exact custody
- Product predecessor HEAD: `3be5e14402f2eee530004f65328de82b1652e0e0`
- Canonical source-sealed Synexia donor/recipe: `hsoliwal/com.synexia` PR #13224, commit `74758563a86fcc99240b139416ca43303a64fca0`
- Java OpenRewrite recipe: `com.synexia.m3.Jdk21RankIntersectionValues20261025`
- Native C/Text OpenRewrite recipe: `com.synexia.m3.Jdk21RankNativeIntersectionValues20261025`
- Source ownership remains original `m3/collections/src/main/java/com/m3/collections` and `m3/collections/src/main/native/collections`; neither generated recipe engine nor Synexia runtime is included

| Original product path | Previous Git blob | Current canonical Git blob |
|---|---|---|
| `M3LongRankBatchNative.java` | `99872c10902c15cbbf143dd158a6bcf6736465e1` | `19dbdde0a25da8484b225cd88b9cec586e0431e2` |
| `M3LongRankIndex.java` | `f9de7b74124cd1e2f6f32c4e9a34af4178c1d787` | `7003a9bf043b118f25f2d21428ddb8ec86267e32` |
| `m3_long_rank_batch_jni.c` | `e7ed7e9a3a9eb71e1970d8b4f023147afb8e7543` | `04d9410518bfae1894be2fd7324d5c952699d1ce` |
| standalone Java differential contract | ABSENT | `45b9281a36e224499e7a643af020ed515628f36b` |
| JUnit integration | ABSENT | `5620d728f668c477837e2d3605589fbba15b486d` |
| native exact-head Actions gate | ABSENT | `c99ddd81ca8a2a73ab2a519317ea3b68d820a146` |

## LeetCode problem-category and native contract
- **#349**: unique sorted intersection, one copy per shared signed-long value
- **#350**: occurrence-preserving multiset intersection, retaining the minimum duplicate count from each owner
- **#26**: skip equal emitted runs without allocating a boxed set
- **#88**: merge-style sorted two-pointer traversal, with no in-place mutation of the original sorted owners
- Earlier **#34/#35** bound kernels and #349/#350 window cardinalities remain untouched

Only LeetCode problem patterns are considered; **no solution code was copied**. Exact rationale, rejected hashing/bitset approaches and source/license decisions remain in Synexia's `CATALOGUE.tsv`.

The API adds `M3LongRankIndex.intersectionValues(other, boolean distinctValues)`, returning a detached, sorted `long[]`. Original Java methods and JNI exported symbols keep their previous contracts. The optional native method `intersectionValues0(left,right,distinct)` performs two linear scans within **one JNI transition**: count exact output length, then emit into `NewLongArray(k)`. Both input arrays remain Java/GC owned and are read through bounded `GetLongArrayRegion` pages; the native algorithm has three 256-long stack pages (~6 KiB), no retained pointer, Java callback or per-entry object. Java fallback uses two-pointers. For `nativeRequired=true`, missing JNI fails closed; an old library lacking this additive symbol safely falls back for automatic calls.

## Evidence and release boundary
**Executed locally:** Java 21 `javac --release 21 -Xlint:all -Werror`; C17 GCC `-Wall -Wextra -Werror`, UBSan and `-Xcheck:jni`; 3106 Java fallback assertions, 6207 present JNI assertions in normal VM and `-Xint`, plus 3106 checks against a deliberately older/ABI-missing native library. Includes empty arrays, long extrema, duplicate frequencies, 256-page output flush, randomized 1500-round oracle, null/required-native refusal, and detached array ownership. Native atom/standalone contract Git blobs match locally compiled files exactly. These are isolated atom proofs, **not** an already executed full 26KB original native library or installed JDK test.

**Required but not yet qualified:** exact full source-rebuilt C owner and Java 21 JNI Actions, original Maven/JUnit reactor (including first-party `com.m3.arrays` and checked native requirements), Synexia Java/Text OpenRewrite SDK replay and fixed point, full built M3JDK21 modules, jtreg/JCK, all supported OS/JNI variants, measured CPU and heap performance. The existing full collection/JNI source-closure and donor-vendor failures remain independent admission blockers. Keep this PR DRAFT/HOLD; never treat a source hash or queued job as passing.
