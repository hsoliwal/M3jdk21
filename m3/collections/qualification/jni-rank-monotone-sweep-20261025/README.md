<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original C17/JNI batched monotone search, October 25 2026

**Product implementation, not overlay:** Existing original `m3/collections/src/main/native/collections/m3_long_rank_batch_jni.c` in M3JDK21's `m3rankbatch` library. Unchanged product Java owner `M3LongRankBatchNative.java` (`bcf60297cd2a3b04668b10b4a0baae6e80d23bbb`). No extra Java JNI declaration, native export, runtime Synexia dependency, `java.util` wrapper, per-query object, or Java callback to native. This original module is not yet proven to replace `java.base` in an installed M3JDK21 image; native runtime admission remains separate.

## Catalogue and exact custody

Canonical convergence workspace: [Synexia PR #13224](https://github.com/hsoliwal/com.synexia/pull/13224), source-sealed commit `c29cf2050b8c37ffe605e16e1c6b474a36b18db5`. Named recipes:
- `com.synexia.m3.Jdk21RankMonotoneJniText20261025` — original JNI C owner plus existing exact-head Actions workflow
- `com.synexia.m3.Jdk21RankMonotoneJniJava20261025` — additive Java21 standalone/JUnit tests

| Original M3JDK21 product file | Original Git blob / predecessor | Exact received Git blob |
|---|---|---|
| `m3_long_rank_batch_jni.c` | `5248b615f66f72b1e390d1aaa7b4797d66691c5b` | `3290b819f9f0cfbc9b61832c4fbc9789450c3afa` |
| `.github/workflows/m3-jni-rank-equal-ranges-java21.yml` | `088625718ddc6cb8354f31ac8bd57cf086fb280c` | `664ca5072763e85cef2f63d77eaa216a38ce1389` |
| `M3LongRankMonotoneSweepContract.java` | ABSENT | `80b2153a19f1e6f34a2c01f2ffd18f33e2c31bb5` |
| `M3LongRankMonotoneSweepTest.java` | ABSENT | `63b8064ef2da0104ba57312732d802283e3a6912` |

All Java and native file postimages are byte-identical to the canonical Synexia recipe, which catalogs candidate algorithms and reviews donor licenses. The old [LeetCode #34](https://leetcode.com/problems/find-first-and-last-position-of-element-in-sorted-array/) independent binary lower/upper bound strategy stays as fallback. [#35](https://leetcode.com/problems/search-insert-position/) grounds insertion-coordinate semantics, and [#88](https://leetcode.com/problems/merge-sorted-array/) supplies the sorted two-pointer category; **no challenge implementation was copied**.

## JNI algorithm contract

Prepared `long[] sorted` and `long[] queries` are read-only. If queries are nondecreasing, native code estimates the sweep against independent binary probes and selects monotone source cursors for suitable batch geometry. Boundaries remain signed `long` insertion positions `[lower,upper)`, preserving duplicates, query encounter order and signed extrema without computing `needle+1`. Unknown/reverse/zigzag queries take the original binary search. Complexity: best admissible monotone case O(n+q), otherwise O(q log n) plus O(q) orderedness probe; additional C algorithm workspace O(1). The existing Java q>=64 threshold, explicit JNI opt-in, `JNI_ABORT` input policy and original two JNI exports are unchanged.

## Local actual JNI evidence

The original Java class and original C file were first restored and checked by **exact Git blob identities**, then the reviewed C postimage was compiled locally using OpenJDK 21 `javac --release 21 -Xlint:all -Werror -h`, actual generated JNI headers, and GCC C17 `-Wall -Wextra -Werror -O2 -fsanitize=undefined -fno-sanitize-recover=undefined -shared -fPIC`. The executable differential oracle produced **3,813,533 successful assertions per run** for Java fallback normal, Java `-Xint`, checked JNI normal, and checked JNI `-Xint` (15,254,132 assertions across those four runs). Explicit native opt-in with missing library also exercised correct Java fallback. Signed `Long.MIN_VALUE/MAX_VALUE`, duplicates, empty inputs, n=0..8192, q=0,1,63,64,65,128,512,2048 and monotone/constant/descending/zigzag/random query orders are covered.

The original rank JNI workflow is augmented, **not bypassed**, to re-run historical 198,919-assertion equal-range Java/JNI proofs along with the new sweep tests, exact source pins, ABI and UBSan gates. The new `M3LongRankMonotoneSweepTest` integrates with the owning Maven/JUnit reactor. Native processing may copy/pin Java arrays; **zero-copy, universal throughput gains, or memory pressure wins are not asserted** without separate measurements.

## Required HOLD / next original JDK gates

Keep PR #1882 DRAFT/HOLD. Required independent evidence: current-head rank JNI workflow PASS; canonical Synexia Text and Java OpenRewrite SDK fixed-point/source-drift PASS; full M3JDK21 module Maven/JUnit and JNI parity; actual source-built `java.base`/HotSpot JMOD or installed JDK tests; jtreg/JCK, cross-platform and CPU/heap benchmarks. The collection source census remains `INVENTORY_ONLY_NOT_QUALIFIED` and never grants source promotion. Do not bypass unrelated failing gates or change protected branches.
