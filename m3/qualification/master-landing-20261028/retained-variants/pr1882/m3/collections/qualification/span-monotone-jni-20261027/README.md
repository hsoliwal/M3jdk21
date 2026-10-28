<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original span JNI monotone-query-page receiving receipt

This is a direct enhancement of M3JDK21's **existing native collection owner**, not an M3-on-JDK overlay. Synexia remains the canonical convergence workspace containing the OpenRewrite recipes, algorithm comparison, and donor references. This product branch carries only executed source-result files, a focused CI result workflow and this data-only receipt.

## Exact source custody
- Source-head before admission: `m3/index-sort-adaptive-20261023@79079d99443353b5be30ef19800090415461111f`.
- Canonical recipe: `com.synexia.m3.SpanMonotonePageJni20261027`, Synexia PR #13224, commit `91e8a00b37ec04ee1330b4e2516c89502bb06f96`.
- Original C owner `src/main/native/collections/m3_span_index_jni.c`: Git blob `05d13d490d6bc039a3549420fc92b5d12fd38520` -> exact canonical source postimage `3dccc1694fb8dd7c3148bc7f59b1cc4391d37077`.
- Original differential Java oracle `src/test/java/com/m3/collections/M3SpanBatchIntoContract.java`: Git blob `3797d53282f8127b11f824845773607728abcfa3` -> exact postimage `88e2ccf3c979fb0f25c42948639c5be17b71c5f0`.
- New focused source-sealed CI workflow `.github/workflows/m3-span-jni-monotone-20261027.yml`: previously ABSENT -> exact postimage `dc5b437453e3d7f1dfde5ce071e614727ce8be99`.
- The original `M3SpanIndex.java` Java product owner remains unchanged at Git blob `f8229851a1a36b72f807736b6df33ef5e8f4ea01`.

## Bounded algorithm and ABI
Original `batchNative` (detached array) and `batchIntoNative` (caller-owned output window) symbols are **both retained** with their original signatures. The native implementation still consumes the prepared `int[]` starts and `long[]` prefix-maxima; no Java object, callback or entry crosses JNI. The original binary-search first-qualifying-prefix proof is retained for unsorted queries and whenever scanning the owner would be costlier. For monotone sorted pages of 256 queries, and only when `owner_length <= page_length * bit_length(owner_length)`, a linear advancing cursor scans prefix boundaries once. The cost comparison uses 64-bit intermediates; signed edge values, exclusive span starts, arbitrary duplicate rows and query-source immutability retain existing meaning. The original JNI array-region and critical pinning lifecycle is unchanged.

The LeetCode #34 boundary-search and #88 ordered-merge categories were compared with the existing kernel in Synexia's `CATALOGUE.tsv`. Challenge source code was **not** copied. The general LC #56 interval-merge approach is rejected here because it could lose original earliest-start semantics. This is a CPU-oriented JNI algorithm selection over arrays and offsets, with constant 256-element stack scratch, not a new generic layer.

## Executed proof and pending gates
Executed locally on Java 21/GCC: isolated C17 O2/Werror/UBSan binary-vs-sweep differential **1,137,635 assertions**, including 2,753 sweep and 6,247 binary pages; independent surrogate JNI normal JVM `-Xcheck:jni` smoke **762,786 query checks**. These do not replace tests of the actual original product owner.

The focused product Actions job compiles the exact original `M3SlotArrays`, `M3SpanIndex`, and updated original `M3SpanBatchIntoContract` under Java 21; generates JNI headers, builds the original C with fatal UBSan and warnings-as-errors, and executes both old/new exports under `-Xcheck:jni`, plus an independent missing-native Java fallback. Only successful **current-head** receipts prove actual source JNI parity. Larger Java/Maven 665-source receiving contracts, full JNI libraries, source-built JDK image, Windows/macOS, jtreg/JCK and measured performance remain separate. Do not infer speedup without CPU/allocation measurements.

No public JDK surface, runtime Java class, existing JNI signature, source provenance or protected master has changed in this pass. Keep this product PR DRAFT/HOLD pending real gate results.
