<!-- SPDX-License-Identifier: Apache-2.0 -->
# JNI lower-bound primitive-page refinement, 2026-10-27

Product owner: original M3JDK21 `m3/collections/src/main/native/collections/m3_long_rank_batch_jni.c`. Canonical Synexia source custody: [Synexia #13224](https://github.com/hsoliwal/com.synexia/pull/13224) commit `2874adf5e5fd9647e478a28f967d9a2ea47dd17c`.

Native original blob `d52ddd76ccc2cb53c7175a3a62155d7e34eb7234`, received exact postimage `1db4c8dbaf28b1aea4315e9c03a2d5ce0c4c7543`. Only the existing `lowerBounds0` C function is refined: 256-long query stack pages through `GetLongArrayRegion`; short-lived source `GetPrimitiveArrayCritical` released before `SetIntArrayRegion`. Preserves signed-long comparisons, monotone sorted-page merge O(n+q), binary-search fallback O(q log n), empty arrays, duplicates, Long.MIN/MAX, original symbol and Java fallback, JNI pointer ownership and error contracts. No new JNI ABI/public API/Java class/runtime recipe or per-entry object.

Catalogue: LeetCode #35 insertion boundary, #88 merge sorted arrays, #34 duplicate limits; HackerRank/GFG binary-search categories are pattern references only. No challenge source copied.

Isolated actual Java21 + C17 `-Wall -Wextra -Werror` shared JNI compiled; `java -Xcheck:jni` validated 252,281 randomly generated and adversarial lower-bound assertions and input immutability; null/mismatched length exceptions. The 74KB full original owner, source-built JDK, required-native suite, sanitizer and multi-platform benches are **NOT yet qualified**. Keep PR draft/HOLD; never claim speedup without CPU/allocation measurements.
