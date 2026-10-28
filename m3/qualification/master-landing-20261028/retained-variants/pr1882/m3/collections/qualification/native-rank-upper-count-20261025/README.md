<!-- SPDX-License-Identifier: Apache-2.0 -->
# Original M3JDK21 JNI upper/count batch rank — 2026-10-25

## Product/native owner
This change is in the actual existing `m3/collections/src/main/native/collections/m3_long_rank_batch_jni.c` JNI owner, not an M3-on-JDK overlay or a new JNI provider. Existing `M3LongRankBatchNative.java` and its public/package-private method signatures, System.loadLibrary opt-in, missing-native fallback and Java small-batch behavior are **unchanged**. No Java Comparator, object graph, per-entry object, JVM address or native pointer is retained across a call.

## Provenance and exact generated source
Canonical private convergence recipe: [Synexia PR #13224](https://github.com/hsoliwal/com.synexia/pull/13224), commit `e9ba8bc52b58d70a60de78500293abe376cf99c1`.
- Existing native C before Git blob `1f84f2d0221d735f2209a6da1673479382e67776`; received postimage `ac7f123549b2b7e0a3788fe9176bb56dee8efe85`.
- Original-package standalone Java21 native/fallback oracle `M3LongRankUpperCountSweepContract.java`: `41883b5b1bee07503ab506aa9bd9e9068727a882`.
- Owning module JUnit wrapper `M3LongRankUpperCountSweepTest.java`: `7a24fb7c7a5ce623021d7d367c3186bb178c31e8`.
- Focused **actual original C source** native/Java 21 workflow `.github/workflows/m3-rank-upper-count-jni-java21.yml`: `d6f72de18cf4a6cb143b1c9721e03e1a2d172839`.
- `CATALOGUE.tsv` and implementation/test transformation recipes remain **only** in Synexia. No Synexia runtime dependency enters M3JDK21.

## Problem-category adaptation (no copied challenge solutions)
LeetCode #34 (first/last sorted occurrence), #35/#704 (insertion/binary bounds), and #88 (merging ordered streams) inform a bounded C17 merge-style monotone-query sweep. Reuse `m3_use_monotone_sweep` for upper and count batches of at most 256 queries, with two signed-long cursor offsets and the existing original binary-search kernel as cost-gated/unsorted fallback. Preserve exactly the original `boundBatchInto0` JNI export and exception validation, caller output offsets, primitive stack buffers, critical-section ownership/release, `JNI_ABORT` read-only arrays and signed long semantics. Small query batches stay in Java.

## Test truth
**Local independent C17/UBSan:** 935,928 upper/count comparison checks PASS. **Java21 local test class:** 849,784 oracle/canary checks per run PASS in Java fallback, normal JNI and `-Xint -Xcheck:jni` native runs. For these local JNI modes an independently compiled JNI bridge with the same existing native signature and algorithm was used, **not** the complete original M3JDK21 native C file. These are algorithm/ABI harness results, **not** full-product native success.

The new focused GitHub workflow must generate JNI headers from the **actual unchanged Java native declaration**, compile the **complete received existing C file** with `-Wall -Wextra -Werror -fsanitize=undefined`, load the resulting `libm3rankbatch.so`, require the native flag in a fresh JVM and compare its exact 849,784-check native/interpreter receipts. Failure or skipped jobs are not qualification. Performance improvement is a candidate cost model, **not a measured throughput claim**.

**HOLD:** Full M3JDK21 array/collection module, source manifest successors, original JNI/JUnit suite, installed JDK/java.util/java.base, jtreg/JCK and performance gates remain separately mandatory. No automatic merge, no force-push, no protected base edit.
