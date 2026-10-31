<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original rank JNI K-difference candidate — 2026-10-26

M3JDK21 is the actual JDK product workspace, NOT a compatibility layer on an ordinary JDK. This candidate modifies the original M3JDK21-owned `m3/collections` Java/JNI sources, retaining all original public contracts and pre-existing JNI exports. This bounded pass does not yet claim any `src/java.base` / HotSpot replacement, installed JDK image or JCK compliance.

## Canonical source custody
Convergence algorithms, donor comparisons and source-changing recipes live ONLY in [Synexia PR #13224](https://github.com/hsoliwal/com.synexia/pull/13224), commit `7ada264acf222521257757650fda50186a33aa1f`. Named source-sealed OpenRewrite recipes:
- `com.synexia.m3.Jdk21LongRankKDiffJava20261026` (typed Java original owner)
- `com.synexia.m3.Jdk21LongRankKDiffNative20261026` (native C / CI original owner)

Exact original source preimages at M3JDK21 commit `73c1e821af71e7c6444e30f786e3fec97c458a7d`, verified before receipt:
- `M3LongRankBatchNative.java`: `cd0a9b800a05e3cdd6478b408b31c71784a9ebb9` -> `3915c02d735d128b74f5857edc81864fa05048bc`
- `M3LongRankIndex.java`: `1965edd9843d29070b01cc9d3107c8226a176e37` -> `b1992bbe965c2f42500b02b5bb687dbb7153638b`
- `m3_long_rank_batch_jni.c`: `d578908b39e626485638b1ca9915484912b4d1c9` -> `32753502cfa1e9f7e5e4a4833b22e495fe679f79`
- `M3LongRankKDiffContract.java`: ABSENT -> `d8e0ea51792e73ce6d83294cb67e66c3715a48a2`
- `M3LongRankKDiffTest.java`: ABSENT -> `7fbf910912740e6ecfe7e4e403a9db4b118475f7`
- `.github/workflows/m3-long-rank-kdiff-jni-java21.yml`: ABSENT -> `c4e26753a1c7d81bcf07583dd94981ba169305cc`

## Mechanism and LeetCode problem catalogue
LeetCode #532 K-diff unique value-pair category. Distinct K-difference pairs over a *prepared, immutable, signed-sorted long[]* snapshot: at K=0 values count once only after a duplicate is observed; K>0 uses two primitive monotonically advancing cursors and unsigned gap comparison, avoiding overflow for `Long.MIN_VALUE` and `Long.MAX_VALUE`. No per-value Entry or boxing, no owner mutation, no Java comparator crossing JNI. Exact native storage is two local 256-long pages accessed via `GetLongArrayRegion`; no retained Java pointers, no heap pin or native full-array clone. The existing optional `m3rankbatch` provider owns the additive export; Java fallback remains authoritative for small batches and absent optional ABI. Explicit native-required calls throw if unavailable.

The catalogue in Synexia compares a boxed hash-set (rejected memory), per-key binary search (rejected extra passes), and primitive two-pointer Java/JNI (selected). LC #167 and #350 are analogous patterns, not code donors; **no LeetCode/HackerRank/GFG or other donor source has been copied**.

## Qualification boundaries
**Executed isolated Java21/JNI fixture**: 900 randomized differential inputs each in Java, native checked JNI, and interpreted checked JNI; verified against an independent BigInteger O(n^2) oracle. Java 1,819 assertions PASS; JNI 2,727 assertions PASS; interpreted JNI 2,727 assertions PASS. C source compiled using C17, `-Wall -Wextra -Werror`, fatal UBSan, and JDK 21 JNI headers. This fixture does **not** prove the entire existing 46 KB JNI owner, full original Maven module or installed JDK image.

**Exact-head CI gate**, in original `.github/workflows/m3-long-rank-kdiff-jni-java21.yml`, must still compile the full original Java rank owner and full original JNI C file, verify exact Git source hashes, enforce native symbol presence, execute Java/checked JNI/interpreted parity and retain receipts. Both source-changing OpenRewrite recipes must pass real SDK LST/text replay, missing-owner/diff refusal and fixed point. The existing full M3 collection JNI suite, HotSpot/JDK build, jtreg/JCK, Windows/macOS and resource/throughput benchmarks are independently mandatory.

**DRAFT/HOLD: no product merge, deployment, protected branch mutation, or source-history rewrite.**
