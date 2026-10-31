<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original JNI LC239 aligned primitive sidecar receipt — 2026-10-27

**Product authority:** M3JDK21 directly owns `M3LaneAlgorithms` and `m3_long_address_kernel.c`. This is not a layer around the JDK. Synexia is the canonical convergence/recipe and donor-catalogue workspace: [Synexia PR #13224](https://github.com/hsoliwal/com.synexia/pull/13224), commit `5d72d1d342089f74789b0f038f7e90601755aace`, named recipe `com.synexia.m3.Jdk21Lc239NativeSoa20261027`. Only product result code, its workflow, and this data-only receipt are retained here.

## Exact source custody

| Original product file | Before Git blob | Received Git blob |
| --- | --- | --- |
| `m3/collections/src/main/native/collections/m3_long_address_kernel.c` | `b4e698c6ede7297f5e3d6d2af6fdeb25bb7fb8e7` | `76f68619a78b52ab528f4f16dcf26b510288c7c9` |
| `.github/workflows/m3-lc239-sliding-max-jni-java21.yml` | `cbe34640a4ca8d1a9f2a19bec9266cef7ddc8098` | `f0634addf43ce205871bafb212bec68006b28538` |

The Java production `M3LaneAlgorithms.java` original Git blob `6691be3fcc1d3b3b276dd2feb7aa488dea5c00d2` and `M3LaneSlidingMaximumContract.java` blob `84cfbc24efa4884584ac7cd1b71b55d6cba6c834` are unchanged. JNI export `Java_com_m3_collections_M3LaneAlgorithms_slidingMaximumIndices0` is unchanged. No new Java runtime owner, new `m3rankbatch` kernel, public API, Java callback, or alternative JNI library was received.

## Algorithm and JNI storage

The existing LeetCode #239 monotone-window physical-index algorithm previously stored one padded C struct per live candidate (`jint physical; jlong value;`). It now stores two **parallel primitive lanes** (offset indexes and candidate longs) in one aligned, checked native allocation. The JNI library retains bounded 256-value `GetLongArrayRegion/SetIntArrayRegion` pages, Java heap pointer isolation and C17/UBSan safety. The same strictly-less tail eviction retains the **earliest physical index** when maxima tie, and the source array, output offsets, native error classes and write ordering are unchanged.

Correctness: O(n) time and O(k) scratch. On a typical LP64 C ABI, old AoS is 16 bytes per candidate while the new SoA payload is approximately 12 bytes per candidate plus alignment padding (not counting allocation metadata). This is a layout calculation, **not a measured CPU or memory benchmark**.

Donor/category: [LeetCode #239](https://leetcode.com/problems/sliding-window-maximum/); [HackerRank Queries with Fixed Length](https://www.hackerrank.com/challenges/queries-with-fixed-length/problem); [GeeksforGeeks sliding-window maximum](https://www.geeksforgeeks.org/dsa/sliding-window-maximum-maximum-of-all-subarrays-of-size-k/). All are problem-reference/donor patterns only, no solution code copied. The catalogue and review rationale stay in Synexia rather than product.

## Qualification evidence and restrictions
Locally: an independent Java21/C17 SoA JNI oracle compiled with `javac --release 21 -Xlint:all -Werror`, `cc -std=c17 -Wall -Wextra -Werror -fsanitize=undefined`, and verified **2,417 Java and 2,418 native-required** checks in both normal and interpreted JVM execution (JNI checked), including duplicate physical ties, signed extreme longs, page boundaries, source immutability and output padding. These were isolated fixtures.

Current-head full-source GitHub workflow `.github/workflows/m3-lc239-sliding-max-jni-java21.yml` retains the original 10,215 Java/10,224 JNI contract checks, normal/interpreted parity, and new generated JNI header inclusion. **Not yet passed on this branch**. The Synexia real OpenRewrite SDK recipe replay, owning Maven/JUnit/JNI reactor, full JDK image/jtreg/JCK and cross-platform/performance checks are also unverified. Keep this PR in **DRAFT/HOLD**, with no promotion until exact-head CI succeeds.
