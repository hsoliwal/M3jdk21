<!-- SPDX-License-Identifier: Apache-2.0 -->
# Original M3JDK21 rank JNI batch receiver — 2026-10-25

**Source convergence:** [Synexia #13224](https://github.com/hsoliwal/com.synexia/pull/13224), canonical recipe commit `f6f9cd0f9a37b56f89a1cba307e1084c33b3c06d`. **Product receiver:** [M3JDK21 #1882](https://github.com/hsoliwal/M3jdk21/pull/1882), original product branch `m3/index-sort-adaptive-20261023`; preimage HEAD `47762a83fccdc036b2c1be36d7d8cac583d3e1f3`. This is M3JDK21's own collection Java/JNI owner, not an M3-on-JDK adapter. No Synexia recipe or runtime dependency is imported into the JDK repository.

## Exactly received source results
```text
M3LongRankBatchNative.java     fa87b9bf25dff5cb858caeee962dfb1bd12a607a
M3LongRankIndex.java           45f608496420cd2f555c0f69b26f572667be6690
M3LongRankIndexSelfTest.java   b2de41ebb3ee281e18c7062c5580bc25bcbad6f0
M3RankBatchIntoContract.java   e4e15d78dfa68cff935c3e26ccee7f954800be24
M3RankBatchIntoTest.java       e430a06d3a056971c6aee6701326ce3cabbc8c20
m3_long_rank_batch_jni.c      714d89c8fcb3e63e9f721af2acd5664f40bb8c30
```
All six product Git blobs are byte-identical to the canonical Synexia Java/Text recipe postimages. The validation workflow is an exact copy of canonical source template, blob `8e662920a3fb1aa39feceb1374896247796a36bd`. No original source path is deleted and the old JNI exports `lowerBounds0`/`equalRanges0` remain unchanged.

## Algorithm/ABI selection
The LeetCode **#34** range of equal values and **#35** insertion-coordinate category provide algorithm-discovery taxonomy; **#704** membership derives from nonzero count. The implementation is independent first-party code, not copied LeetCode solutions. Existing `M3LongRankBatchNative.lowerBounds` and `equalRanges` (including monotone sweep cost gate) remain intact.

Java API adds `M3LongRankIndex.upperBounds(long[])`, `counts(long[])`, and `countInto(long[],int,int,int[],int)`. These operate on the original immutable, privately-owned sorted `long[]` index without boxing, per-element Entries, payload moves or new wrapper classes. The `countInto` output is a caller-owned `int[]` window; all ranges are checked before writing. `Long.MIN_VALUE`/`Long.MAX_VALUE`, duplicate values, empty input and absent values are supported without `target+1` overflow. Native acceleration is opt-in under the existing `m3.collections.rank.native` setting and only selected for batches of at least 64. Otherwise a deterministic Java implementation is authoritative.

The new **`boundBatchInto0`** JNI export is appended to the *existing* `m3rankbatch` C17 library. Per JNI transition, it processes a batch with fixed stack scratch of 256 query longs plus 256 result ints, reading the sorted payload only through a short-lived `GetPrimitiveArrayCritical` region with no callbacks/JNI allocation/blocking while held. Reads are never published as raw addresses, inputs remain unchanged, native errors preserve output on validation refusal, and library absence falls back to Java. Existing native library/old public JNI names are unmodified.

## Executed bounded evidence
On local OpenJDK 21.0.12.1 with strict `javac --release 21 -Xlint:all -Werror` and GCC 14 C17 `-Wall -Wextra -Werror -fsanitize=undefined`, the *exact added Java methods and C extension* passed dependency-free Java fallback **79,775 assertions** and JNI enabled **79,781 assertions** in both default and `-Xint` runs, with `-Xcheck:jni`. Test cases include duplicate batches, signed extreme values, empty and alias-free inputs, native validation exceptions, output offset/canary preservation, 63/64/65 and 255/256/513 thresholds, sorted/unsorted query arrays and random differential reference comparisons. These results prove the extracted extension; the full original C translation unit is qualified independently in the new Actions workflow and Synexia source-template CI.

## Honest HOLD gates
No full product module Maven/JUnit/native qualification, complete original C/JNI symbol compilation, installed M3JDK21 java.base/HotSpot image, jtreg/JCK or measured CPU/heap gain is claimed yet. Product `M3LongRankIndexSelfTest` now includes the new public API's Java/native parity and offset canaries; the dedicated `M3RankBatchIntoTest` JUnit adapts the 79k-check standalone oracle. The new exact-head Actions workflow compiles the original Java owner and full C translation unit with generated JNI headers, C17/UBSan and Java `-Xcheck:jni`, running both normal and interpreted VMs. **Neither PR is merge-ready until the relevant live-head runs pass.**

**Architecture invariant:** All reusable algorithms, donor comparisons, recipes and source seals remain in Synexia. Only tested recipe *results* belong in M3JDK21. This PR is an additive reviewable increment, not a claim that the entire JDK-wide M3 program has been implemented.
