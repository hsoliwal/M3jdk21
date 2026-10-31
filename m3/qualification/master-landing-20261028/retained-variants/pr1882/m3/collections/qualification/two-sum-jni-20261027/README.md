<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original JNI sorted two-sum batches — source receipt

This is native **M3JDK21** product code in its original collection/index owners, not a library layered over Java. Canonical source convergence, donor catalogue and execution recipe: [Synexia #13224](https://github.com/hsoliwal/com.synexia/pull/13224), commit `1d13ca9bb6e5efad0d70dede8415ae4352208aec`, `com.synexia.m3.Jdk21TwoSumBatchJNI20261027`. Only Java/C postimages and this data-only receipt/CI are received into the product; no OpenRewrite runtime or recipe engine is imported.

## Byte-exact custody
Original product commit before this pass: `f2aa93d393ce4fd323716625b047319ad236842d`.

| Original product file | Preimage Git blob | Canonical postimage Git blob |
| --- | --- | --- |
| `src/main/java/com/m3/collections/M3LongRankBatchNative.java` | `86a901ee34deae468db9c453f6f8cb636b49238d` | `8ba40d4b547f53bcf47ec0fb52eda6085a27efc9` |
| `src/main/java/com/m3/collections/M3LongRankIndex.java` | `0f3256ad47e5f14e841adeda7fd9898e353964a2` | `77c6f8b98e39d9e22513a9c1cf5108cf82a0e866` |
| `src/main/native/collections/m3_long_rank_batch_jni.c` | `107d5f86c901042c7fe23c926495aadb568798ca` | `2afe176456419c5d6f4a29d2bfa2ce8f24eed1c4` |
| `src/test/java/com/m3/collections/M3SortedTwoSumBatchContract.java` | ABSENT | `7e6cd5cdef950fda1067451181279510d02ba219` |
| `src/test/java/com/m3/collections/M3SortedTwoSumBatchTest.java` | ABSENT | `e0a03fadb838eb6bc82f1175134fee445150eabe` |
| `.github/workflows/m3-two-sum-rank-jni-java21.yml` | ABSENT | `521a2e1fcb0dfd4d015e325dea2200f28fc380e6` |

Product Java files are under `m3/collections/`; all source Git blobs above were independently compared to the current Synexia manifest/templates **before** this receiving commit.

## Algorithm / JNI invariant

[LeetCode #167](https://leetcode.com/problems/two-sum-ii-input-array-is-sorted/) is categorized as sorted opposing two pointers (time O(n), auxiliary O(1) per query). This product adapts it to signed-long rank snapshots and batches: zero-based primitive pairs flattened in an `int[]` of length `2*q`; `[-1,-1]` means no pair. Duplicates are independent physical occurrences; no same-element reuse. The Java comparator and native kernel both check overflow/underflow across the entire Long.MIN..Long.MAX domain before calculating a sum.

The original `M3LongRankIndex` public methods and their behavior remain unchanged; `pairSumIndices` and `pairSumIndicesInto` are additive. Small batches remain in Java. For prepared large batches and explicit opt-in, the existing `libm3rankbatch.so` JNI owner performs one native transition and reuses two bounded 256-long pages, plus 128 query / 256 output stack slots. No Java callback crosses JNI; no per-element objects, full-payload copy, pinned heap regions or retained native pointer. Older installed JNI ABIs fall back only for automatic mode; explicit native-required tests must fail rather than conceal an unavailable export.

Conceptual donors: LeetCode 167, HackerRank Ice Cream Parlor (unsorted contrast), GeeksforGeeks sorted pair sum. The Java and native code are original adaptations. No donor challenge implementation is copied. Canonical catalogue and license decisions live in Synexia.

## Verification truth

**Executed locally on isolated prototype (not the entire current repository):**
- Java 21 `javac --release 21 -Xlint:all -Werror`;
- C17 `gcc -Wall -Wextra -Werror -pedantic -fsanitize=undefined`;
- `java -Xcheck:jni` / `java -Xint -Xcheck:jni`: 92,227 assertions each with the native export loaded;
- 46,124 fallback assertions with JNI absent.
Oracle: BigInteger mathematical sums, Java vs JNI coordinates, duplicate values, missing solutions, target and owner offsets, caller padding, MAX/MIN long boundaries, and prevalidation-before-write.

**Pending current-head gates:** Full 78KB original JNI owner strict C17 compilation, actual source-generated Java contract on original 65KB Java owner, Maven/JUnit/full Java/JNI parity, real Synexia Java/Text OpenRewrite SDK recipe replay, native-required ABI and `-Xcheck:jni` on hosted CI; installed-JDK, jtreg/JCK and measured CPU/allocation comparisons. All three source owners, JDK phase authority, original release gates and provenance remain intact.

**DRAFT / HOLD:** No merge, automatic promotion or performance claim until original Maven/native/JDK source/runtime gate results pass.
