<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 product receiver: original LC15 signed-long triplet Java/JNI

**This is M3JDK21 integrated product code, not an M3-on-JDK overlay.**

## Original owners and exact source custody
Canonical convergence: [Synexia PR #13224](https://github.com/hsoliwal/com.synexia/pull/13224), source recipe commit `2ed08a7595880f8f3abbac61f8f367d79d943300`. The reusable recipes and their donor catalogue are retained **only in Synexia**. This product includes only the executed Java/C source postimages, original package test sources and a focused original-workflow CI proof.

Source baseline: product PR #1882 previous head `e2e1ea70b3c804d5fe7f6c974384d25fdf31bc50`.

| Original product owner | Baseline Git blob / ABSENT | Exact received postimage Git blob |
|---|---|---|
| `M3LongRankBatchNative.java` | `8ba40d4b547f53bcf47ec0fb52eda6085a27efc9` | `e7f9a656ae90a7e90478a80cabbe8e19419f6d5d` |
| `M3LongRankIndex.java` | `77c6f8b98e39d9e22513a9c1cf5108cf82a0e866` | `1d6930704b737f38aaaca1de37570efa3cdd4de5` |
| `m3_long_rank_batch_jni.c` | `226e7dec4413166d9bda2a2d8e5dd73751184bb8` | `f03722dd7836b668247ea595d953769b4ffa7e60` |
| `M3LongRankTripletSumContract.java` | ABSENT | `011265b99581d3bd86be09c3112fea335439756d` |
| `M3LongRankTripletSumTest.java` | ABSENT | `6f835bcaef2caf20e633754af2bc5ed3b6da2229` |
| `.github/workflows/m3-long-rank-triplet-jni-java21.yml` | ABSENT | `4b3f5739b06ee0ae94f1b33441674c8a4b360acf` |

## Contract
Public additive method `M3LongRankIndex.distinctTripletSumCount(long target)` uses the existing immutable signed-long sorted `values` owner. Returns the count of **distinct value triples**, not the number of physical `i<j<k` occurrence combinations, satisfying the uniqueness pattern of [LeetCode #15](https://leetcode.com/problems/3sum/). Existing M3 rank/index/JNI methods remain unchanged.

The default for small arrays is a first-party O(n^2) two-pointer Java oracle, O(1) Java scratch. Exact 128-bit signed comparison of `a+b+c` prevents silent `long` overflow. For >=192 elements the existing `m3rankbatch` library may be loaded through the pre-existing opt-in `m3.collections.rank.native`; the new C17 function copies the immutable original `long[]` once to transient native scratch, **without holding a GC-critical pointer** over O(n^2) work, and never mutates the owner. No Java callbacks, per-triplet entries or retained native handles. A pre-existing older native library with no new symbol gets Java fallback; the explicit native-required path fails closed.

## Executed and remaining qualification
Local test fixture uses Java 21 `javac --release 21 -Xlint:all -Werror`, C17 `-Wall -Wextra -Werror` with fatal UBSan and an actual compiled JNI `libm3rankbatch` implementing this new ABI; it runs the independent `BigInteger` oracle over 1,800 randomized small snapshots, signed extremes, duplicate cases and 191/192/193/256/512 threshold cases.

- Java-only: **5,502** checks PASS.
- JNI-opt-in and explicit-required: **7,348** checks PASS.
- Loaded older mock JNI library lacking this export: **5,502** checks PASS with normal fallback and explicit-required refusal.

These numbers are from an **isolated, source-equivalent local Java/C harness**, not the complete original 68KB Java/81KB C product files. Original-head CI now compiles the full original classes, generated JNI headers and full C owner, then runs all three modes. The canonical Synexia source-qualification workflow similarly compiles full postimages and executes real OpenRewrite SDK replay. Those live-head gates remain **PENDING** until runners complete. No unmeasured CPU/heap speedup or universal JNI superiority is claimed.

**DRAFT / HOLD.** Full Maven/JUnit/JNI module, installed JDK21, jtreg/JCK, Windows/macOS, provenance and performance tests remain required. Do not merge, bypass guards, or change protected bases.
