<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original Java/JNI primitive multiset union candidate (2026-10-25)

**Source ownership:** Synexia convergence and hash-pinned OpenRewrite recipes: [com.synexia PR #13224](https://github.com/hsoliwal/com.synexia/pull/13224), commit `eef26eb4a86592c3e54c336c696310307abc0033`. **Product owner:** M3JDK21, original `com.m3.collections` owners. M3JDK21 is the native JDK product, **not an M3-on-JDK overlay**; this bounded original module change does *not yet establish* upstream `java.base` replacement in the installed JDK image.

## Actual Java/JNI owners, no new adapter layer
The original M3LongRankBatchNative.java and M3LongRankIndex.java gain only an additive primitive union-values method. The existing original `m3_long_rank_batch_jni.c` gains one new `unionValues0` export while retaining every other JNI symbol and C kernel. Java rank/index APIs and previous semantics are unchanged. No retained Map.Entry, Long boxes, Java comparator crossing JNI, heap object pointer or new runtime dependency.

The algorithm uses LeetCode #88 sorted two-cursor merge and #26 adjacent distinct-run categories (**original implementation, no copied LeetCode/editorial code**). Multiset union retains max occurrence frequencies; distinct union emits one sorted copy of each value. The returned long[] is detached; original snapshots remain private. JNI reads/writes bounded 256-element array regions, no GC heap pointer retained; fallback Java runs for small batches or older m3rankbatch binaries missing the additive export. Explicit native-required request fails instead of silently falling back.

## Exact receiver Git blobs
- Original Java batch kernel before: `19dbdde0a25da8484b225cd88b9cec586e0431e2`, after: `201c51afbf5706a146fa0cef9317e08fc4e9a58a`
- Original Java rank index before: `7003a9bf043b118f25f2d21428ddb8ec86267e32`, after: `00aa416af3cf08ebae2db8ef19cac66fe5f8c29a`
- Original JNI C before: `04d9410518bfae1894be2fd7324d5c952699d1ce`, after: `7ea1afda505894b9cc1294bfc1c0a74a58ea0587`
- Standalone differential contract: `2aa7692d5480f557a976374509e4bc47915ffb0a`
- JUnit5 entrypoint: `308e29580d9f22c7648b9af0bf01d0755f4ae836`
- Legacy ABI compatibility contract: `75b2ae741d4991de4d44055c753264ac442b474d`
- Original exact-head Java/C JNI workflow: `70c58641533145a77294a68430067049a75b835b`

## Observed evidence and hold gates
**PASS local standalone prototype:** JDK21 `javac --release 21 -Xlint:all -Werror`, C17 `gcc -std=c17 -Wall -Wextra -Werror -fsanitize=undefined`, 7,200 Java assertions, 10,797 JNI assertions each in normal and interpreter `-Xint` with `-Xcheck:jni`; old export-free JNI stub fallbacks and required-native refusal passed. These are **prototype tests**, not claims that full original repository C sources or all JDK modules compiled.

**PENDING:** new exact original source JNI [workflow](https://github.com/hsoliwal/M3jdk21/actions/workflows/m3-rank-union-jni-java21.yml); normal/JNI on the product head; Synexia real Java/Text OpenRewrite SDK, original full Maven/JUnit/JNI sources, source-built JDK image, jtreg/JCK, Windows/macOS, native SIMD performance, GC stress, and java.base ownership transition.

No recipe source ships with this product. Historical source manifests and all other receiving receipts remain unchanged. Do not merge, remove draft or treat a queued job as qualified until all relevant real gates pass.
