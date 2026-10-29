<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original long-rank JNI fused-bounds result — 2026-10-29

**M3JDK21 product source, not a layer on top of another JDK.** This product PR receives the exact Java/C/test/CI postimages from canonical [Synexia #13224](https://github.com/hsoliwal/com.synexia/pull/13224), source-sealed convergence commit `2cf9a60042ac913723010b5efed41b188af8911a`. No Synexia recipe or runtime engine enters product code.

Base master for this branch: `7f7accdd54e298d1febbdb70083e4272f25e9a83`. Every existing owner matched the canonical preimage before receipt. Existing lowerBounds0 (JNI + Java) remains unchanged; existing m3rankbatch C library gets one additional fused bounds symbol.

## Exact original-owner custody

| Product Java/C/CI result | Exact Git blob |
| --- | --- |
| `M3LongRankBatchNative.java` | `d100fe7c269ec40ed8cdcac1d9f7985422f0ca54` |
| `M3LongRankIndex.java` | `acb4ab5ee856de814ec5dab5c134b9bf27b15f37` |
| `m3_long_rank_batch_jni.c` | `52b2d518351ceabd87499c840d06d23d79e72439` |
| `M3LongRankBoundsContract.java` | `bcd12bca84aa58879536be2e3dd92e332af17b41` |
| `M3LongRankBoundsTest.java` | `d52d3fc06ad65ab9861233c5142a139e3298caad` |
| `.github/workflows/m3-long-rank-bounds-jni-java21.yml` | `09323ef84608473be09cddf5f387b557bb052279` |

Original M3LongRankIndex.java dual-license header (Apache-2.0 OR EPL-2.0) is retained. Native and helper owners remain Apache-2.0. Original donor code from LeetCode/HackerRank/GFG was not copied; only algorithm categories/specifications were evaluated. Synexia's catalogue records LeetCode #34 (duplicate lower/upper range), #35 (insertion position), #704 (membership), GFG lower/upper bound and rejected HackerRank Pairs donor.

## Primitive storage and contract

The immutable rank snapshot exposes `lowerUpperBounds(long[])` producing exactly `[lo0,hi0,lo1,hi1,...]` and `fillLowerUpperBounds(long[],int[])` for a caller-provided exact-sized pair lane. `count(q)=hi-lo`; signed Long.MIN_VALUE/Long.MAX_VALUE and absent values do not overflow. Java fallback retains the inherited 64-query JNI threshold and works when the opt-in library is absent; large opted-in batches use one JNI transition for both bounds. All long[] inputs are read-only; JNI input copies or pins are released with JNI_ABORT. No per-query entries/objects, callbacks, Java GC addresses, or hidden native memory owners.

## Actually executed local evidence

Strict OpenJDK 21.0.12 source compilation with `javac --release 21 -Xlint:all -Werror`: PASS. GCC C17 `-Wall -Wextra -Werror -fsanitize=undefined -fno-sanitize-recover=undefined` and generated native JNI header: PASS. Both original and added JNI export symbols observed via `nm -D`. Deterministic 56,973-check differential: PASS in Java default, opt-in with missing native library, actual native with `-Xcheck:jni`, and actual native with `-Xint -Xcheck:jni`. The test explicitly requires JNI in real-native modes so fallback cannot be mistaken for native success.

## Qualification and limits

The focused exact-head CI workflow compiles original Java owners and C17 native code, checks SHA-1 Git source blobs, runs actual JNI and Java fallback in four modes, and separately attempts original `com.m3.arrays` and `com.m3.collections` named-module source compilation. These current-head jobs have **not yet passed** until their GitHub Actions results are observed. The new JUnit wrapper also needs the full original collection Maven/JUnit suite. Canonical actual Java/Text OpenRewrite SDK source-replay/fixed-point tests are pending in Synexia.

No benchmark was executed: one JNI transition per eligible batch is structural, not proof of faster throughput or lower RAM use. JNI GetArrayElements may copy. Keep the original small-batch Java path until measured break-even.

**DRAFT / HOLD:** Full Maven/JUnit, original native suite, installed built-JDK runtime, jtreg/JCK, Windows/macOS, release and performance qualification remain pending. No protected-branch write, no merge, no bypass.
