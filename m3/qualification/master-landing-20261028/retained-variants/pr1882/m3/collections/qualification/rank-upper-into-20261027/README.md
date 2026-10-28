<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 LeetCode #34 JNI reuse: upperBoundsInto

Original production Java owners only; no M3-over-JDK layer and no new native ABI. This result is source-sealed under canonical [Synexia #13224](https://github.com/hsoliwal/com.synexia/pull/13224) commit `0c400f9a80d042d9db39f3fe6156b390e86ac980`, recipe `com.synexia.m3.Jdk21RankUpperInto20261027` and problem-category catalogue.

The original M3LongRankBatchNative.upperBounds still returns a detached allocated result array. Added `upperBoundsInto` delegates to the **existing** `boundBatchInto(...,MODE_UPPER)` Java/JNI dispatcher and permits caller-owned primitive output offsets. M3LongRankIndex exposes the same operation. The original 2,500-seed-loop rank self-test adds padded-output, scalar oracle, and invalid-window cases. The m3rankbatch native C source and exports are untouched; no per-query objects or data ownership changes.

Git blobs: rank batch 86a901ee34deae468db9c453f6f8cb636b49238d; rank index 0f3256ad47e5f14e841adeda7fd9898e353964a2; original self-test c5a640a4acd61233eccfac97ff52c39f3b95c44b.

**HOLD**: actual Java21 module build, normal/JNI parity tests with native required, Maven/JUnit, real OpenRewrite SDK execution, source-built JDK/jtreg/JCK and benchmarks not established on this head. Do not merge based on static source custody.
