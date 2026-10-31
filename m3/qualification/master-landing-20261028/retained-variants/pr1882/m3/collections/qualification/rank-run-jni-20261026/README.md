<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original primitive long run facts, JNI-first candidate (2026-10-26)

Canonical recipe owner: [hsoliwal/com.synexia PR #13224](https://github.com/hsoliwal/com.synexia/pull/13224) commit `463e4b95e28111b1bd604dbd38c0ca75adcc3ed3`. Product owner: [hsoliwal/M3jdk21 PR #1882](https://github.com/hsoliwal/M3jdk21/pull/1882). This is a product-source implementation in the existing `m3/collections` Java/JNI owners, **not an M3-on-JDK overlay**; no recipe or Synexia runtime jar is included.

## Exact source/postimage custody
| Original product target | Required preimage Git blob | Received postimage Git blob |
| --- | --- | --- |
| `M3LongRankBatchNative.java` | `5aa4d2c8cc8683a08ab20625c8763cac6c1fd9c6` | `3617962b12e314cc60670171c923b4bbca78da48` |
| `M3LongRankIndex.java` | `716189db70586d221f63dc647d692951d77094cf` | `6a8cf91eb78b6e77fb53b73d55cea0a214a33dd7` |
| `m3_long_rank_batch_jni.c` | `25a228e37629490a18fe395f4c51df4f76296701` | `fab1b17dc529b81a401dd1d3d77fcb8035dc33c7` |
| `M3LongRunStatisticsContract.java` | ABSENT | `c2b46e696d6a55b03632651d4d43d4f05e398bb2` |
| `M3LongRunStatisticsTest.java` | ABSENT | `7d1083709ae5a7107003752157c002295908b53a` |
| `.github/workflows/m3-rank-run-jni-java21.yml` | ABSENT | `8208954fe7eb9addba01ba5dfcd570eb26bf9011` |

Synexia owns named `Jdk21LongRunStatisticsJava20261026` (4 Java targets) and `Jdk21LongRunStatisticsNative20261026` (1 original JNI C17 owner plus source-sealed workflow). Their manifests pin original+post UTF-8 SHA-256, prevent missing/stale owners and enforce second-pass fixed-point; original Java sources are parsed through OpenRewrite Java and native C/workflow through OpenRewrite Text.

## Primitive algorithm, LeetCode categories
LC #26 (sorted duplicates) and LC #128 (longest consecutive sequence) are problem categories only; no LeetCode editorial/submission code copied. The existing M3 rank owner captures sorted primitive long[] once and never exposes it. A fused O(n) scan returns `(distinctCount << 32) | longestConsecutiveRun`, where duplicate values do not extend a run and signed Long.MIN_VALUE / MAX_VALUE arithmetic is overflow-safe. The only new index-level public methods are additive: `distinctValueCount()` and `longestConsecutiveRun()`. A single lazily populated volatile long scalar caches the result on immutable snapshot owners, with benign concurrent first-read recomputation.

JNI implementation is appended to the **existing** `m3rankbatch` C17 source as `runStatistics0`. No new native library, JNI handle, JDK public ABI, Object.Entry, callback crossing or per-row object. The native scan uses fixed 256-long GetLongArrayRegion stack pages and no retained moving-GC addresses. JNI is opted into with `-Dm3.collections.rank.native=true` and selected on prepared arrays of length >=256. Small owners use Java; an older m3rankbatch binary missing the new symbol falls back to identical Java unless the call explicitly requires native, in which case it fails closed.

## Reproducible verification and HOLD
Independent local Java 21/C17 UBSan fixture, from the same JNI scalar algorithm: 2,480 Java differential checks PASS; 3,720 actual JNI checks PASS; 3,720 actual JNI `-Xcheck:jni -Xint` checks PASS. This fixture is **not** proof of installed product JDK/JNI integration. The source-sealed workflow compiles six real Java files, the **whole existing** native C source under C17 UBSan/Werror, then executes the full M3LongRankRunStatisticsContract on actual library; missing native is a hard failure. Its expected full product checks are 9,792 Java and 11,031 required-native checks. Current-head GitHub Actions run, full module Maven/JUnit/native parity, source-built java.base HotSpot, jtreg/JCK, and microbenchmarks are **pending**.

No performance gain over HotSpot is claimed without a workload benchmark, and no default Java behavior is replaced in this pass. Historical source-receipt divergence and other CI failures remain independently gated. Maintain DRAFT/HOLD; no merge, branch protection override, or release promotion.
