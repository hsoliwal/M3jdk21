<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 legacy signed-long equal-range JNI paging, 2026-10-27

**Product implementation:** Original `m3/collections/src/main/native/collections/m3_long_rank_batch_jni.c`, not an M3 runtime overlay. `M3JDK21` owns the original Java/JNI implementation. Synexia solely owns the reusable exact-source transformation recipe.

Canonical source-sealed recipe: [Synexia #13224](https://github.com/hsoliwal/com.synexia/pull/13224), commit `1bbc94b7e1ee2ef717cd671f2b4b6bbbd38480e0`, named `com.synexia.m3.Jdk21LegacyEqualRangesBoundedJni20261027`. Exact source preimage C Git blob `2afe176456419c5d6f4a29d2bfa2ce8f24eed1c4` (SHA-256 `0cdce8b6b71d7207b774eb91d9c11049dfc1d75ec1cd1bf53a57e69ccb93c827`), generated postimage Git blob `226e7dec4413166d9bda2a2d8e5dd73751184bb8` (SHA-256 `5dc1d91f29738e3a6a3ec0013bcad6e4e5d55dbd83829dc6b2f727b7f55d1255`). Byte identity was checked by independent source/product Git blob reads.

**Algorithm category:** LeetCode 34/35 (first/last occurrence as [lower,upper) indices, no max+1 overflow), LeetCode 88 (monotone two-cursor merge), implemented as original M3 native code, **no challenge code copied**. Only the legacy `equalRanges0` C body changes: 256-query stack pages, 512-int page results, JNI region copies for query/output, critical pinning only for read-only sorted values during each bounded page's search. Monotone sorted-query decision and binary fallback, null validation, exact JNI name/signature, numeric ordering, duplicate semantics and existing Java API/JNI fallback remain unchanged. No Java owner or JNI export was added.

**Executed local evidence:** an isolated Java 21 JNI test of a matching bounded algorithm ran **586,579** input-query pair comparisons in normal and `-Xint` JVM modes, both with `-Xcheck:jni`, PASS, compiled with C17 `-Wall -Wextra -Werror`. This is not a full-file compile or full integration run.

**Required gates:** actual full C/JNI product compilation, Java and native parity, missing-library fallback, actual Synexia OpenRewrite SDK replay/fixed-point, original full Maven module, source-built JDK, jtreg/JCK and performance measurements. These have NOT been established on the resulting HEAD. No claim of speed gain or full release readiness; keep PR DRAFT/HOLD and do not merge until gates pass.
