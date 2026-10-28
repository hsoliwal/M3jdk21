<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 primitive LC239 sliding maximum — original Java/JNI owners

**Product identity:** M3JDK21 is the JDK product, not a compatibility layer. Synexia remains the sole convergence/recipe workspace. This product branch receives only exact executed source postimages, tests, CI and this data-only receipt. No Synexia runtime or OpenRewrite recipe is copied into the product.

## Source custody
- Canonical recipe: [Synexia #13224](https://github.com/hsoliwal/com.synexia/pull/13224), commit `e0e6697af20744fcbc9620001c7428e8c5b7f7dd`.
- Existing original Java owner `m3/collections/src/main/java/com/m3/collections/M3LaneAlgorithms.java` pre-blob `ca8c32bf77825453e071d9dd771d97fd8a4b268d`, exact canonical result `6691be3fcc1d3b3b276dd2feb7aa488dea5c00d2`.
- Existing original JNI C owner `m3/collections/src/main/native/collections/m3_long_address_kernel.c` pre-blob `123e40c56995574979e999e6233a218b3dc2e4cb`, post-blob `b4e698c6ede7297f5e3d6d2af6fdeb25bb7fb8e7`.
- New standalone Java21 differential contract `M3LaneSlidingMaximumContract.java`: `84cfbc24efa4884584ac7cd1b71b55d6cba6c834`; JUnit wrapper `M3LaneSlidingMaximumTest.java`: `987dde85f0260c1f43801df4d514e52a7e1ffd71`.
- New exact-head native/parity workflow `.github/workflows/m3-lc239-sliding-max-jni-java21.yml`: `cbe34640a4ca8d1a9f2a19bec9266cef7ddc8098`.

## Primitive algorithm contract
The source-owned implementation returns physical indexes of window maxima over arbitrary signed `long[]` lanes and half-open windows. The earliest physical index wins equal-value ties. Java uses an O(n) primitive index ring deque; JNI uses exactly the original `m3_collections_address` native library, bounded 256-value array regions and O(k) temporary native primitive candidates. No per-entry Java/native object, comparator callback, retained GC pointer, heap pin, or payload permutation. Existing classes and JNI exports retain prior behavior. All public input/output windows are checked before the first write. Native dispatch is optional and opt-in via `-Dm3.collections.lane.native=true`; native-required failure never silently falls back.

## Executed local focused qualification
- Java21 `javac --release 21 -Xlint:all -Werror` PASS.
- C17 `cc -Wall -Wextra -Werror -fsanitize=undefined` PASS.
- Java normal and interpreter `-Xint`: `PASS M3_LC239_SLIDING_INDEX checks=10215 native=false` each.
- Native-required normal and interpreter: `PASS M3_LC239_SLIDING_INDEX checks=10224 native=true` each.
- Required-native JNI `-Xcheck:jni` normal/interpreted PASS. Local fixture uses the exact appended original Java/C algorithm fragments and independent brute-force oracle, not a source-built JDK image.
- Test vectors include duplicates, stable physical tie breaks, signed Long.MIN_VALUE/MAX_VALUE, offsets, invalid input/out-of-bounds, native-required rejection, malformed direct JNI requests and source/untouched output lanes.

## Catalogue and attribution
LeetCode #239 monotonic deque category; HackerRank Queries with Fixed Length and GeeksForGeeks sliding maximum are comparative problem patterns. Independent first-party Java and C code is written under the existing Apache-2.0 module rights; no contest/editorial solution source is copied. Canonical Synexia `CATALOGUE.tsv` retains complexity comparisons, alternatives and licence decisions.

## Unpassed admission gates — HOLD
Current-head hosted Java/JNI workflow, real OpenRewrite SDK replay, full owning Maven/JUnit source corpus, JNI/native parity with every existing provider, source-built JDK/JMOD, jtreg/JCK and CPU/allocation benchmarks remain unverified. Local isolated JNI parity does not establish performance gain or installed JDK promotion. Preserve DRAFT, require exact source and ABI gates before merge.
