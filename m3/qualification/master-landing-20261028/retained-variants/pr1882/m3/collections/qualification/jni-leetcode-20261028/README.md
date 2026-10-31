<!-- SPDX-License-Identifier: Apache-2.0 -->
# Native rank LeetCode source qualification (draft)

Canonical Synexia convergence source: hsoliwal/com.synexia PR #13224 at fe6a8e4cea690d77445013d7bdc53bd23d174753.

This pass receives only an original-product Java21 source test and GitHub Actions JNI qualification workflow. The original M3LongRankBatchNative.java and m3_long_rank_batch_jni.c remain unmodified. The source test is Git blob f9779493ec50e8d396522b88026c9832e19f302a; the workflow is f5bb60ba7c47e2762276e35695fc26ae8091bf79.

Algorithm catalogue: LC #34/#35 signed lower and upper insertion bounds, LC #349/#350 sorted intersection cardinality with duplicate counts, LC #88 merge-style ordered queries. This uses categories only, with no copied challenge code. Tests span sizes 0, 1, 2, 63, 64, 65, 255, 256, 257, 512, 80 deterministic rounds each, 258 query slots plus padded output, signed extremes and duplicate occurrences. Explicit JNI required mode fails if the original m3rankbatch library cannot load. The workflow compiles the actual product owner with javac -h, builds the original C17 JNI source with UB sanitizer, and runs Java and native-required parity under -Xcheck:jni.

Strict standalone Java21 source syntax checked with a matching method-signature fixture. Real source compile, C17 native build and runtime parity are **pending** until the current exact-head GitHub Actions workflow executes. This does not certify installed JDK/jtreg/JCK or throughput. Keep PR draft/HOLD; no native callback or new JNI export was added.
