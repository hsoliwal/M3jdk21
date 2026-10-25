<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original primitive interval batch Java/JNI receiving receipt

**Product:** M3JDK21 original Java/JNI `m3/collections` owners, NOT an M3+JDK adapter. **Canonical convergence recipe:** Synexia PR #13224 commit `01a94d4c2cff13f11e0ddadc2b29f6b0708115cb`. Exact original product predecessor head `6e86f2b03c3661203dfa87cb9c25d1715e06cad0`; this repository receives only reviewed canonical source-output files and a data-only receipt, never the Synexia recipe engine/runtime.

| Original owner | Preimage Git blob | Exact received postimage |
|---|---|---|
| `M3LongRankBatchNative.java` | `fa87b9bf25dff5cb858caeee962dfb1bd12a607a` | `20446b00731fdd0b9524ac35be3860fbc0bb062b` |
| `M3LongRankIndex.java` | `45f608496420cd2f555c0f69b26f572667be6690` | `fe87db69813f63bf60beeff702eab10cfe306f99` |
| `m3_long_rank_batch_jni.c` | `714d89c8fcb3e63e9f721af2acd5664f40bb8c30` | `1f84f2d0221d735f2209a6da1673479382e67776` |
| `M3RankIntervalBatchContract.java` | ABSENT | `278e77bd3264b8bd113114f44af996615297ec0d` |
| `M3RankIntervalBatchTest.java` | ABSENT | `bdf022d93b9b2f9e71c79e64112a8005ab29a049` |
| `.github/workflows/m3-jni-rank-interval-batch-java21.yml` | ABSENT | `ddd3961c43b255822f1de14311fd019fdb273bb8` |

These are the exact Git blobs fetched and reproduced from the canonical Synexia Java/Text source templates. No original file is deleted; the C source retains its original three JNI exports (lowerBounds0, equalRanges0 and boundBatchInto0) and appends one `rangeCountInto0` operation.

## Original-owner ABI and array invariants
`M3LongRankIndex.rangeCounts(long[] startInclusive, long[] endExclusive)` and `rangeCountInto(...)` operate on the original private sorted `long[]` value index. The new JNI facade and `m3rankbatch` C17 native symbol process batches of signed-long `[from,to)` count queries. Bounds are found by two `lower_bound` searches; counts include duplicates, exclude the upper endpoint, and never compute `to+1`, so `Long.MIN_VALUE` and `Long.MAX_VALUE` remain safe. All user-supplied windows and every interval are validated BEFORE any output writes, including query at offset 512 (after two whole native chunks); invalid input raises Java contract exceptions and preserves caller-owned output. Inverted intervals are never silently clamped. The native loop uses 256 two-endpoint input pairs and one int-output buffer on the C stack; a short `GetPrimitiveArrayCritical` source region is released using `JNI_ABORT` between chunks, with no callback or JNI call inside. No borrowed pointer outlives its critical scope or crosses to user Java code. Existing JNI opt-in and 64-query eligibility threshold remain unchanged; missing native library uses Java fallback, with no mandatory JNI for JDK startup.

**LeetCode catalogue:** #34 first/last boundaries, #35 insertion position, #986 interval intersection categories selected as algorithm discovery/testing references. #56/#57 interval merging rejected as semantically wrong for independent per-query counts; #88 monotone sweep already covered by the existing rank JNI atoms. All new code is independent first-party implementation, not copied challenge submissions or donor code. Source licenses remain Apache-2.0.

## Bounded executed evidence
The isolated new algorithm/JNI fragment was built with OpenJDK 21 `javac --release 21 -Xlint:all -Werror`, native generated header and GCC 14 C17 `-Wall -Wextra -Werror -O2 -fsanitize=undefined`. The narrow deterministic original-API-shaped oracle passed **21,819 assertions** in Java fallback and **21,822 assertions** with JNI enabled, each in normal and `-Xint` mode with checked JNI. It verifies output canaries, chunk thresholds, signed long extrema, duplicate counts, inverse interval refusal and JNI-native exception conversion. The actual complete original Java/JNI translation units and the 9,720-byte repository oracle have **NOT** been executed successfully yet.

## HOLD
The dedicated original-source Java21/C17 JNI GitHub workflow must verify all five exact source blobs, compile all original rank dependencies and complete C translation unit against `javac -h` generated JNI headers, then execute the larger actual `M3RankIntervalBatchContract` Java fallback/JNI/`-Xint` tests. Synexia's actual OpenRewrite SDK replay, full collection Maven/JUnit and JNI, source-built JDK `java.base`/HotSpot, jtreg/JCK, Windows/macOS and measured CPU/heap regressions are separate compulsory gates. No universal JNI speed or zero-copy guarantee is made. Keep this PR **DRAFT and unmerged** until all required current-head gates pass.
