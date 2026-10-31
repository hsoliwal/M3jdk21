<!-- SPDX-License-Identifier: Apache-2.0 -->
# LC719 pair-distance primitive: original M3JDK21 rank JNI receiving receipt

**M3JDK21 is the product JDK, not an M3-on-JDK overlay.** This change refines the existing M3-owned `m3/collections` Java/JNI rank substrate in place. Synexia [PR #13224](https://github.com/hsoliwal/com.synexia/pull/13224), commit `696a9b029931a374ff05decf63b3f00b505bb7ce`, retains the canonical catalogue and hash-pinned Java AST/C native Text transformation recipes. Neither recipe, a Synexia runtime jar, nor an external wrapper is imported here.

## Exact original-owner custody
Original product PR #1882 preimage `66ddfd93f75f4830e2e3c8fea139f95e49dec65e`:
- `src/main/java/com/m3/collections/M3LongRankBatchNative.java`: before `3915c02d735d128b74f5857edc81864fa05048bc`, after `8106997f17e17fa584f1b3437360f31d85609188`
- `src/main/java/com/m3/collections/M3LongRankIndex.java`: before `b1992bbe965c2f42500b02b5bb687dbb7153638b`, after `0f9763abfa67046490f344abfe620ae086d833c2`
- `src/main/native/collections/m3_long_rank_batch_jni.c`: before `32753502cfa1e9f7e5e4a4833b22e495fe679f79`, after `294447e6fe84c37a7b7bdbc3492a9bf62a78a731`
- New original-module standalone Java `M3LongPairDistanceContract.java`: `a28b679781123efa0b4c3ae4721d1e03de69a435`
- Its original-module JUnit wrapper `M3LongPairDistanceTest.java`: `7dbc1c1bc8f78766492cd04252ba75fb4c2603e9`
- Focused exact-head original C17 `javac -h` generated JNI ABI / `-Xcheck:jni` CI: `2045017c674d7a886a21b69b639dc58abb32a0c6`

All six Git blobs were compared byte-for-byte against canonical Synexia source templates. This receiver changes exactly the original owners/results plus a separate qualification workflow; no copied donor LeetCode/HackerRank/GFG solution implementation.

## Algorithm, Java compatibility, and JNI
The LeetCode #719 category supplies the sorted-array **count pairs with distance <= threshold** predicate. Existing M3 #532 computes *distinct exact K-difference pairs* and is preserved. The new `M3LongRankIndex.pairCountWithinDistance(long)` and `pairCountsWithinDistances(long[])` expose a primitive-only scalar/batched result with long counts. Every occurrence pair i < j contributes independently, including duplicate values. Java uses `Long.compareUnsigned(high-low, threshold)`, C uses unsigned `uint64_t` subtraction, avoiding signed overflow across `Long.MIN_VALUE` and `Long.MAX_VALUE`. Negative thresholds reject before output. The Java caller retains private sorted immutable `long[]` ownership.

`M3LongRankBatchNative` provides the original m3rankbatch JNI dispatch and optional legacy-ABI Java fallback; explicit required-native parity never falls back. The existing C owner gains `pairDistanceCounts0(long[],long[],long[])`. It accesses JVM arrays using bounded 256-long `GetLongArrayRegion`/ `SetLongArrayRegion` pages, two source cursors, no retained Java pointer, no JNI Java callback, no full source native copy, and no per-value entry object. Every threshold is checked before the first result write. Running time per batch is O(n * q); no universal speedup is asserted without benchmarks.

## Executed and outstanding gates
A standalone original-package Java21 fixture containing the exact new Java methods and JNI C atom passed:
- `javac 21.0.12.1 --release 21 -Xlint:all -Werror`
- `gcc 14.2.0 -std=c17 -Wall -Wextra -Werror -pedantic`
- Java fallback: **10,893** checks passed
- JNI `-Xcheck:jni`: **11,047** passed
- JNI `-Xint -Xcheck:jni`: **11,047** passed

Edge cases include duplicate occurrence counts, random arrays, Long.MIN/MAX differences, empty arrays, invalid negative thresholds, invalid direct JNI output lengths, unmodified Java inputs, required-native parity, and 65,537 equal elements yielding >2^31 pair counts. The source UTF-8 Git blobs are pinned and checked, not merely described.

**NOT YET VERIFIED**: whole original C source compiled with newly generated complete JNI header, whole actual `M3LongRankBatchNative` owner / Maven/JUnit source-module reactor, native backend parity on the full original library, original arrays dependency and historical source-manifest successor, installed M3JDK21 image / java.base JMOD, jtreg/JCK, Windows/macOS and measured CPU/allocations. Dedicated new Actions workflow must PASS on this exact PR head before considering promotion.

Remain **DRAFT/HOLD**, do not merge, skip or waive failed source/ABI/contract gates.
