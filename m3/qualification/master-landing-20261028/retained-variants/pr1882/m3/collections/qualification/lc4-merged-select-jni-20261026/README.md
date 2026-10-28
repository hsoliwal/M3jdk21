<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original LC #4 merged rank select — native receiving receipt

This is an original M3JDK21 collection product source/JNI pass, NOT a compatibility overlay or a second native owner. All modifications are to the actual M3JDK21-owned `M3LongRankBatchNative`, `M3LongRankIndex`, `m3_long_rank_batch_jni.c`, their new contract/JUnit tests and exact-head CI. The canonical algorithm catalogue, OpenRewrite Java+Text source-sealed recipes and their fixpoint/refusal tests are kept solely in [Synexia #13224](https://github.com/hsoliwal/com.synexia/pull/13224), commit `3c49097ea158c4c75764b50ad1ef2669e9be362f`.

## Original source SHA custody
- M3JDK21 initial product head: `2b78514275f0aaf79b3709b1806de38808f34c03`.
- Java rank batch: `f546cd6d947b1a9c5f22de9bd7633cf966715f12` -> `a82c7b93a0919cdfe92de32e636399b429f8cca1`.
- Java rank index: `461f927506227501e26926e08e5aa3f6e360243f` -> `b3d88e4b28aab8cbda451c87184c4540549352db`.
- Original m3rankbatch C17 JNI: `9ca26b26412068d7fc58b5e431d9d091c8e062e8` -> `09551a86c7f9a4ce3b0ed1a26cbbd2b89b7f6b5a`.
- New standalone product contract: `115fd28026985612746701105ccdbc331e059637`; JUnit public facade: `bb59937e9e290bed20373470c2b720825f9f395c`.
- Dedicated exact-head compiler/JNI workflow: `db595b195325519ec80eec56f5324a8ea28d74f1`.
All six source/CI product Git blobs match the source-sealed Synexia Java/Text recipe templates **byte-for-byte**.

## LeetCode problem-category design and JNI
Adapt independently the *problem classification*, not third-party challenge implementation code: LC #4 kth sorted selection without materializing the union; LC #88 sorted merge and LC #215 quickselect were compared and rejected for immutable owner/array-copy reasons. Original `M3LongRankIndex` is an immutable signed-long array snapshot. Added public `selectMerged(other,long rank)`, `selectMergedBatch(other,long[] ranks)` and `selectMergedInto(other,...)` methods and package-private primitive JNI dispatch. All previously available JDK and JNI signatures remain unchanged. Selection uses elimination in O(log(rank+1)) per query and O(1) scalar scratch, preserving duplicate positions and Long.MIN/MAX semantics.

The unchanged JNI library `libm3rankbatch.so` gains `selectMergedInto0` with *no* per-element object or Java comparator callbacks, fixed 128-query pages, short array critical sections, exception-safe paired releases, all-rank prevalidation before output writes, explicit alias rejection and no JNI calls while source arrays are pinned. Small batches and missing optional JNI support use Java; explicitly required native execution refuses unavailable symbols instead of silently falling back. The original C source prefix is untouched, and the appended code matches the locally compiled C fragment Git blob `f6513ee18ad6a52d3029265beedf8bf784c80417`.

## What actually passed, and what is still blocked
**PASS on local original-compatible subset:** Java 21 strict compile, GCC C17 `-Wall -Wextra -Werror -fsanitize=undefined`, JVM Java fallback 172,254 assertions, required-native 237,179 assertions in both normal and `-Xint` with `-Xcheck:jni`. Cases include 400 seeded inputs, duplicates, signed extremes, same original source owner, invalid second 128-rank page, checked partial windows and alias refusal.

**NOT YET QUALIFIED:** whole existing 60 KB C JNI owner compilation and generated exact JNI header; full original M3JDK21 collection reactor/JUnit/native branch, canonical Synexia OpenRewrite SDK replay, installed source-built JDK/jtreg/JCK, Java 25 portability, cross-platform JNI and measured performance. The dedicated source-sealed GitHub Action is required to execute these focused original-owner JNI checks on the live PR HEAD; no queued job or local subset test substitutes for them. Preserve all historical source receipts and branch protections. Both PRs remain **DRAFT / HOLD**.
