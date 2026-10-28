<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 LC315: primitive count-smaller-to-right Java/JNI

This is an original M3JDK21 product-source refinement, **not an M3+JDK overlay**. Canonical Synexia convergence recipe and algorithm catalogue: [hsoliwal/com.synexia#13224](https://github.com/hsoliwal/com.synexia/pull/13224), commit `ae6894beca2d2d398db352e664f73d4ceb3ee3d3`. Public M3JDK21 owns only the original Java/JNI implementation and executable test/CI results, not Synexia recipe engines.

## Algorithm category, rights and complexity
LeetCode #315 ("Count of Smaller Numbers After Self") is a *problem-category reference*, not copied editorial or challenge code. For original long[] array positions i, result[i] counts all j>i with input[j]<input[i]. Strict comparison means duplicates do not count. Negative values and Long.MIN/MAX use signed comparisons without subtraction or overflow. A stable primitive-index merge computes counts in O(n log n) time, O(n) primitive scratch; no Long boxing, Map.Entry allocations, retained Java pointers or payload mutation. The Java fallback is always available.

JNI is an **appended ABI in the existing `m3rankbatch` library** (`m3_long_rank_batch_jni.c`). An eligible n>=64 uses one native transition when `-Dm3.collections.rank.native=true`. Native code copies input in 256-element JNI regions into private buffers before the merge, avoiding JVM critical-array regions across O(n log n) work. Four O(n) native buffers can OOME; allocation failure leaves Java output unpublished, with explicit OutOfMemoryError. No Java callback enters native, no source-owned array address persists.

An older optional library with no new symbol falls back to the Java kernel. The existing exact native-required proof seam always throws UnsatisfiedLinkError if the ABI is unavailable; it never silently downgrades. The additive `M3LongRankIndex.countSmallerAfterSelf(long[])` entry point accepts original encounter-order arrays.

## Exact source custody at starting M3JDK21 PR head
- Java rank batch owner `M3LongRankBatchNative.java`: `1013859f83470bdd7682ae732ea0293634ea2687` -> `7871ab51e2ec8c3e4fb990817f5fbdb7ec9845cb`.
- Java rank-index owner `M3LongRankIndex.java`: `b3d88e4b28aab8cbda451c87184c4540549352db` -> `ce7f53f1a4c69c9f5a990f26944519b1194dfe6e`.
- Original native C owner `m3_long_rank_batch_jni.c`: `3a56a28ae6b48867260f9f3bad79d8adb8070a86` -> `442630c291ce9ba4c9c4490dca4076e7f09558c3`.
- Add `M3LongCountSmallerRightContract.java` (`90995dfabb36d7d3dc639867270f91723de2b8da`) and original-module JUnit wrapper `M3LongCountSmallerRightTest.java` (`ce53d077c93db272d9fdaa510bb61e8e22f9e5b9`).
- Dedicated exact-current-PR-head Java21/C17/JNI workflow `.github/workflows/m3-lc315-count-smaller-jni-java21.yml` (`6e7cccfea584c30a50f2ed8689b87fe5214e9bed`).

## Evidence actually executed
A standalone Java21 class running this primitive algorithm and a standalone C17 translation unit containing the kernel were compiled under `--release 21 -Xlint:all -Werror` and `-std=c17 -Wall -Wextra -Werror -fsanitize=undefined`. The deterministic quadratic reference checked 2,864 Java-mode assertions, 4,296 native-required under `-Xcheck:jni` (same 4,296 with `-Xint`), 2,865 legacy-library fallback assertions, and 2,865 missing-library refusal/fallback assertions. An additional 100,000-element descending and all-equal source passed Java/native parity.

**NOT EXECUTED** for this new head: full original 66 KB C source compilation, integrated 52 KB Java original-owner compilation, actual Synexia OpenRewrite SDK `Recipe.run`, owning JUnit/Maven reactor, source JDK image, jtreg/JCK, native platforms besides Linux, or repeatable allocation/performance benchmarks. The source-pinned dedicated workflow must run on the exact PR head. A narrow standalone kernel test is not full M3JDK21 qualification; this branch remains DRAFT/HOLD until all independent gates succeed.

## Admission controls
The Synexia Java source template uses existing `M3HashPinnedJavaSnapshotRecipe`; the native C and CI template use `M3HashPinnedTextSnapshotRecipe`. Both require original source SHA-256, exact output SHA-256 and fixed-point/missing-source/refusal tests. Their recipes, donor evaluation and candidate catalogue remain exclusively in Synexia. This product contains only admitted candidate Java/C outcomes, focused contract tests and data-only provenance. Do not merge or bypass pending previous collection/JNI gates.
