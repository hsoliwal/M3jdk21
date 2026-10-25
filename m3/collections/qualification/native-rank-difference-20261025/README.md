<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original primitive JNI rank difference (2026-10-25)

**Product:** M3JDK21, not an M3-on-JDK compatibility layer. Original Java and JNI owners in the existing m3/collections module were refined. There is no runtime or build-time Synexia recipe dependency in this product result. The canonical transformation recipes, before/after byte seals, algorithm catalogue and OpenRewrite SDK tests live only in [Synexia PR #13224](https://github.com/hsoliwal/com.synexia/pull/13224), source commit 7e3d7108b61c18d372b17e60842d2dc9328bb315.

## Source custody: original owner preimage -> product result
- M3LongRankBatchNative.java: 201c51afbf525b442d9b80b8a03ad4208b52c782 -> 5aa4d2c8cc8683a08ab20625c8763cac6c1fd9c6
- M3LongRankIndex.java: 00aa416af3cf08ebae2db8ef19cac66fe5f8c29a -> 716189db70586d221f63dc647d692951d77094cf
- m3_long_rank_batch_jni.c: 7ea1afda505894b9cc1294bfc1c0a74a58ea0587 -> 25a228e37629490a18fe395f4c51df4f76296701
- M3LongDifferenceContract.java (new): 22ebb243b0fbad16daa1972dadc1d2cbce5e41dc
- M3LongDifferenceTest.java (new): 74591bf6eb38b640b6603fb2c33b6e0167dc5d6b
- Original focused GitHub Actions Java/JNI gate (new): 5f99a1912aefc0e6f912d785c6059337f32e5c58

## Problem category and algorithm choice
LeetCode #2215 (distinct difference of two arrays), #350 (duplicate-aware occurrence frequencies), and the existing #349/#88 intersection/merge donor categories. The implementation is first-party Apache-2.0 Java and C, **not copied LeetCode challenge submissions**. Sorted run-group cursors over original immutable primitive arrays implement:
- difference distinct: only values in left and absent from right;
- difference multiset: each value max(leftCount-rightCount, 0) times;
- symmetric distinct: keys appearing in exactly one snapshot;
- symmetric multiset: abs(leftCount-rightCount) occurrences.

Existing lower/upper/range-count/intersection/union APIs and JNI symbols are unchanged. Added original M3LongRankIndex APIs delegate to the original M3LongRankBatchNative, and an additive JNI method lives in the existing m3rankbatch shared library.

## JNI and lifetime
A single JNI call handles large prepared batches with two count/emit scans and three fixed 256-long stack pages (two input pages and one output page). Input/output are transferred via GetLongArrayRegion/SetLongArrayRegion; there is no retained GC address, per-entry object, Java comparator callback or permanent native allocation. JNI is opt-in for large batches and explicit native-required calls; an older library without the additive symbol falls back to Java unless native was explicitly required. Signed Long.MIN_VALUE/MAX_VALUE and duplicate runs crossing page boundaries are covered.

## Evidence, NOT a release claim
Isolated strict Java 21 compilation and C17/JNI/UBSan compilation passed. A standalone public-API/JNI fixture ran 4,200 randomized rounds plus fixed edge cases and verified **55,060 assertions** in native normal JVM, **55,060** in interpreted native JVM, and **36,707** in Java-only fallback. The exact canonical Git postimage identities were verified byte-for-byte during product receipt.

Pending: actual focused GitHub Actions on this exact product head, full original m3/collections Maven/JUnit with all native providers, full JNI/jtreg/JCK, source-built/installed JDK modules, Windows/macOS, licensing/source-manifest successors, stress under concurrent GC and throughput/allocation benchmarks. JNI speedups are **not** claimed without measured results. Do not merge, bypass native/source/contract gates or promote this draft on isolated fixture evidence alone.
