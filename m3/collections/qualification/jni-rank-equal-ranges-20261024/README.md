<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original Java/JNI rank batch: LeetCode 34 equal-range kernel

**Product ownership:** The existing M3JDK21 `M3LongRankBatchNative.java`, `M3LongRankIndex.java` and `m3_long_rank_batch_jni.c` are directly refined in their original `m3/collections` module. This is a Java/JNI implementation result, **not an M3-on-JDK overlay**, an added native library, a mapper layer or a Synexia runtime dependency. The original optional native `m3rankbatch` lowerBounds ABI/body remains unchanged.

**Canonical source recipes:** [hsoliwal/com.synexia PR #13224](https://github.com/hsoliwal/com.synexia/pull/13224) at commit `506c7985c89168334793b91b35e0ea5a27af7dcf`, named `com.synexia.m3.Jdk21NativeRankEqualRangeJava20261024` and `com.synexia.m3.Jdk21NativeRankEqualRangeC20261024`. These existing Java and Text OpenRewrite recipe owners have hash-pinned exact source preimages, accepted postimages, generated tests and standalone CI. Reuse [catalogue](https://github.com/hsoliwal/com.synexia/blob/506c7985c89168334793b91b35e0ea5a27af7dcf/synexia-openrewrite-recipes/recipe-crates/m3jdk21-jni-equal-ranges-20261024/CATALOGUE.tsv). The LeetCode sources supply problem **categories**, no copied solution code.

## Algorithm and JNI strategy
LeetCode [#34](https://leetcode.com/problems/find-first-and-last-position-of-element-in-sorted-array/) supplies the two binary-boundary-search category. The new M3JDK21 API returns flat primitive `int[]` pairs `[lower,upper)`, not inclusive LeetCode endpoints; absence means `[p,p)` (the insertion position), and frequency is `upper-lower`. Signed `long` extrema are safe: upper is first element strictly greater, **never target+1**. Complexity is O(Q log N) over prepared sorted `long[]`, output exactly 2Q primitive int indexes. No Map.Entry or per-query object allocations.

The previous `-Dm3.collections.rank.native=true` opt-in policy and small-batch threshold 64 are retained. Prepared query batches of at least 64 enter the **existing** `m3rankbatch` JNI library once, performing two bisections per query in C17. Short batches and unavailable native libraries use Java; the original `lowerBounds0` remains untouched. JNI uses GetLongArrayElements and JNI_ABORT for input and publishes an output primitive int[]; JVM copying is implementation-dependent and **zero-copy is not asserted**. Java comparators and callbacks never cross JNI. The public product `M3LongRankIndex.equalRanges(long[])` delegates to this primitive owner without retaining per-entry objects.

## Exact original product output
| Original product path | Result Git blob |
|---|---|
| `m3/collections/src/main/java/com/m3/collections/M3LongRankBatchNative.java` | `bcf60297cd2a3b04668b10b4a0baae6e80d23bbb` |
| `m3/collections/src/main/java/com/m3/collections/M3LongRankIndex.java` | `b6cf17d87db4d0d5890e33febda5332ca6c7ee7a` |
| `m3/collections/src/main/native/collections/m3_long_rank_batch_jni.c` | `5248b615f66f72b1e390d1aaa7b4797d66691c5b` |
| `m3/collections/src/test/java/com/m3/collections/M3LongRankEqualRangesContract.java` | `7ca8f2fff2b1d662b53064596a1b29715fe60052` |
| `m3/collections/src/test/java/com/m3/collections/M3LongRankEqualRangesTest.java` | `13512e6278aec62f4ca9f5f1839e72e809df758f` |
| `.github/workflows/m3-jni-rank-equal-ranges-java21.yml` | `088625718ddc6cb8354f31ac8bd57cf086fb280c` |

## Proof and HOLD conditions
Local OpenJDK21 strictly compiled the actual Java native owner and executable contract with `--release 21 -Xlint:all -Werror`, generated JNI headers via `javac -h`, and compiled the original C17 file into `libm3rankbatch.so` with `-Wall -Wextra -Werror -fsanitize=undefined`. **198,919 deterministic assertions passed per execution** for Java fallback, JNI with `-Xcheck:jni`, and JNI with `-Xint -Xcheck:jni`; tests include thresholds 63/64/65, signed extremes, duplicates, empty sequences, source immutability and original lowerBound parity. Public API shape was type-checked on a local harness, but the whole original M3JDK21 module/JUnit reactor was not run locally.

The new dedicated GitHub Actions workflow executes exact-head Git-blob checks, Java/JNI compilation, C17 UBSan, fallback normal/interpreted and native normal/interpreted tests, and missing-native fallback. It does **not** waive the independent previously failing full Maven/OpenRewrite, existing JNI, actual source-built JDK/java.base, jtreg/JCK, installed-image or measured performance gates. The existing source-census remains `INVENTORY_ONLY_NOT_QUALIFIED`. Keep PR #1882 DRAFT/HOLD until these gates produce real green evidence.
