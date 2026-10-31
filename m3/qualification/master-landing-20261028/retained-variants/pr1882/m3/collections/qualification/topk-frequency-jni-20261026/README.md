<!-- SPDX-License-Identifier: Apache-2.0 -->
# Original M3JDK21 LC347 primitive Top-K frequency — Java and JNI

**Product owner:** original `hsoliwal/M3jdk21` collection Java/native sources, not an M3-on-JDK overlay. **Recipe owner:** [Synexia PR #13224](https://github.com/hsoliwal/com.synexia/pull/13224), canonical commit `839d4ac8dc22449b40038bf6735781d63e41571d`. The product contains only admitted source postimages, tests, workflow and this data receipt; no Synexia recipes/runtime dependency.

## Exact source custody
Baseline product head: `35662be63cafc35153f18c0b3d7bbe2f689960de`.

| Original product file | Input Git blob | Received Git blob |
| --- | --- | --- |
| `M3LongRankBatchNative.java` | `3617962b12e314cc60670171c923b4bbca78da48` | `cd0a9b800a05e3cdd6478b408b31c71784a9ebb9` |
| `M3LongRankIndex.java` | `6a8cf91eb78b6e77fb53b73d55cea0a214a33dd7` | `1965edd9843d29070b01cc9d3107c8226a176e37` |
| `m3_long_rank_batch_jni.c` | `fab1b17dc529b81a401dd1d3d77fcb8035dc33c7` | `d578908b39e626485638b1ca9915484912b4d1c9` |
| `M3LongTopKFrequencyContract.java` | ABSENT | `d717fd8134fc6f35f8da95caf6eef38a24cf748f` |
| `M3LongTopKFrequencyTest.java` | ABSENT | `2e2262f5fec76a8cb26ab0a5c0d488c04d8ed51e` |
| `.github/workflows/m3-topk-frequency-jni-java21.yml` | ABSENT | `bb74adee1e631f851e83e70458195d4440dbc166` |

All six exact canonical Synexia source outputs match the received Git blobs, including the existing JNI translation unit. No old native entrypoint was removed, no new native library was introduced, and no original `module-info.java`, `java.util` public API or unrelated source file changed.

## Primitive problem-category implementation
LeetCode #347 Top K Frequent Elements, adapted to privately sorted M3 signed-long rank snapshots. `M3LongRankIndex.topKFrequent(k)` returns `long[]` of up to k distinct keys in frequency descending / signed key ascending order. No boxing, Map.Entry, per-element heap objects or Java comparator callbacks occur in product. Existing `M3SignalTopK` worst-first heap pattern is reused with two aligned primitive arrays. Native `m3rankbatch` scans 256-long JNI pages through `GetLongArrayRegion` with no retained moving-GC pointers. Native heap is O(min(k,n)), failure returns OOME, source owner remains immutable; missing *optional* new ABI falls back to the Java oracle while *required-native* mode always fails closed. Public `k < 0` throws IAE; if k exceeds number of unique values, return all available keys.

The exact source implementation, candidate comparisons (LeetCode, HackerRank, GeeksforGeeks), donor provenance and licences are recorded in the Synexia catalogue. Complexity: O(n + d log k + k log k) work, where d is distinct-run count. No universal speedup or installed JDK claim.

## Evidence and HOLD gates
Executed **in a separate isolated Java21/JNI kernel fixture**: strict `javac --release 21 -Xlint:all -Werror`, C17 `-Wall -Wextra -Werror` UBSan JNI compilation, 13,883 Java fallback assertions and 18,508 required-native assertions each in normal and `-Xint -Xcheck:jni` modes. This is not yet a full source-built M3JDK21 runtime validation.

The product current-head workflow compiles original full rank Java sources and *all existing* m3rankbatch C source, verifies six blob SHA-1 seals, requires 18,561 exact Java+rank contract assertions, and 23,198 required JNI assertions in each execution mode, with a C17 UBSan binary and `-Xcheck:jni`. Only a **completed green current-head** run proves that full original-owner test. Actual Synexia OpenRewrite SDK Java/Text fixed-point tests, whole Maven/Collections-JNI, native provider parity, installed JDK, jtreg/JCK, cross-platform and resource/performance gates remain independent and incomplete.

**DRAFT / HOLD:** Do not merge, deploy, change protected branches, waive failing checks or describe this as an already integrated installed JDK solely on the isolated result.
