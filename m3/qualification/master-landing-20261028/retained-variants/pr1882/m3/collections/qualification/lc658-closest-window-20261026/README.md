<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 — Original rank-index K-closest JNI, LeetCode #658 (2026-10-26)

## Ownership and unchanged contracts
Synexia is the **convergence recipe** workspace; M3JDK21 is the original product/JDK owner, **not** M3 + JDK. This pass mutates only existing original `M3LongRankBatchNative.java`, `M3LongRankIndex.java`, and existing `m3_long_rank_batch_jni.c`, and adds two Java tests. There is no new wrapper library, native export from a second library, retained Map.Entry, heap-owned per-element node, or Synexia runtime dependency.

The previous product head already added LeetCode #315 count-smaller-right. This candidate was **rebased onto that exact successor**, preserving all earlier public and package-local Java methods and JNI exports. Source preimages are Git blobs `7871ab51e2ec8c3e4fb990817f5fbdb7ec9845cb`, `ce7f53f1a4c69c9f5a990f26944519b1194dfe6e`, and `442630c291ce9ba4c9c4490dca4076e7f09558c3`; templates/manifest pin all pre/post UTF-8 SHA-256.

## Implemented algorithm atom
On a sorted signed-long owner, each query yields the start index of the K closest elements in an otherwise untouched owner. The window is contiguous; distance ties favor the smaller signed value, and equivalent all-equal windows choose the **earliest** physical start. K may be zero through owner length, including empty owners. Invalid K, query/output ranges and nulls are refused **before output writes**. The immutable index exposes two additive original-owner methods. Java fallback remains default for short batches; opt-in native `m3rankbatch` serves batches of at least 64, and a required-native internal path never silently falls back. Older native libraries without the additive symbol fall back only in optional mode.

Inside the binary window decision, cases target <= left and >= right are separated; when target lies strictly between endpoints, `Long.compareUnsigned(target - left, right - target)` (C `uint64_t` subtraction) evaluates distances mathematically up to 2^64-1. `Math.abs(Long.MIN_VALUE)` and signed `target-left` comparison would be incorrect. After binary search, identical windows are canonicalized using a lower bound on that equal value, never by an O(n) backscan.

JNI receives only original primitive arrays and validated ranges. It copies **at most 256 query longs and 256 output ints per page** into 3,072 bytes of C stack scratch. The owner uses a bounded `GetPrimitiveArrayCritical` region for pure C bisection, releases before `SetIntArrayRegion`, and never invokes JNI while pinned or retains addresses across calls. The VM may choose to copy/pin internally; no zero-copy guarantee or throughput speedup is asserted. Signed order, duplicate occurrence identity, offsets, and native exception classes are contract-bound.

## Donors, evidence and release status
Category discovery: LeetCode #658 closest K window, LeetCode #34 lower/upper bound, LeetCode #88 merge; independent Apache-2.0 implementation; **no competitive-programming solution code copied**. CATALOGUE.tsv records alternatives, complexity, licensing and decisions.

**Executed**: 1,116,830 finite exhaustive cases and 50,000 randomized 64-bit edge cases against an independent value-ranking oracle; strict Java21 `javac --release 21 -Xlint:all -Werror` and GCC `-std=c17 -Wall -Wextra -Werror -fsanitize=undefined`. Standalone JNI with `-Xcheck:jni` and `-Xint` passed 50,976 assertions each; Java-only passed 50,977. This is a real tested JNI kernel integrated into the pre-existing ABI symbol family, but those tests compiled a focused original-contract fixture, not the entire current product source closure.

**HOLD**: actual full 55k Java owner and 70k C library compilation on PR HEAD, generated JNI header parity, JUnit/Maven reactor, canonical OpenRewrite Java+Text SDK replay, installed JDK image, jtreg/JCK/JCK-equivalent, Windows/macOS native testing, and CPU/GC memory benchmarks. Product `m3/collections` currently carries a separate module and this pass alone does not prove replacement of `java.base` or installed-JDK integration. Do not merge on partial tests or queued CI.
