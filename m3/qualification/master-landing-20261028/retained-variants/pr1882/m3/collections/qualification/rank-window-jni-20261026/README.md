<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original-owner JNI equal-range window (2026-10-26)

This is a **native result received by M3JDK21**, not a runtime Synexia layer. Canonical source-sealed Java and text transformation recipes and the donor catalogue remain exclusively in `hsoliwal/com.synexia`. Product implementation stays in the existing `M3LongRankBatchNative.java`, `M3LongRankIndex.java` and `m3_long_rank_batch_jni.c` owners.

## Algorithm and storage
LeetCode #34 and #35 supply the binary insertion-boundary *category*; #88 supplies the monotone two-stream scan category. These are compared concepts; **no third-party challenge solution code is copied**. Given sorted, immutable signed `long[]` source and any query window, the new `equalRangesInto` writes two primitive `int` offsets per query into a caller-owned output window: `[lower,upper)`. An optional, same-library JNI function processes 256-query pages using bounded native stack scratch and one pinned, read-only source page at a time. No boxed tuple, `Map.Entry`, JDK `java.util` overlay, new native library, Java comparator callback, native Java-object address or permanent JNI pin is introduced. It never modifies input long arrays.

The existing `equalRanges(long[])` remains source/binary compatible and delegates to the same result semantics; the original `equalRanges0` JNI entrypoint is retained. New JNI entrypoint `equalRangesWindow0` has a separate ABI: older `m3rankbatch` libraries correctly fall back to the Java oracle for optional calls, but explicit native-required calls **fail closed**. The public snapshot adds `M3LongRankIndex.equalRangesInto(...)`, a strictly additive method with identical signed-long and duplicate semantics.

Input and output slices (including overflow-safe `2 * length`) are checked before mutation; on validation errors the caller output is unchanged. Empty sorted snapshots write zeros; empty query windows leave all output unchanged. Native code also validates offsets and never invokes unrelated JNI APIs inside `GetPrimitiveArrayCritical`.

## Local executed proof
On Linux Java/OpenJDK 21 + GCC, the independent contract `M3LongRankWindowContract` ran 1,800 seeded randomized cases plus boundary sizes (0, 1, 31, 63/64/65, 255/256/257, 512/513 and 1024), signed Long.MIN_VALUE/MAX_VALUE, duplicate values, reversed and ordered query windows, nonzero output offsets, caller padding, invalid windows, missing native symbols and explicit native-required failure. The native kernel was compiled with `-std=c17 -Wall -Wextra -Werror -fsanitize=undefined` and exercised with `-Xcheck:jni`.

- Java-only: **3,686** checks passed.
- New JNI native-required: **3,686** checks passed.
- New JNI under `-Xint`: **3,686** checks passed.
- Deliberately old loaded ABI without the new symbol: **3,689** checks passed (optional Java fallback + required failure).

The local harness uses the same newly authored Java algorithm and JNI native function; that proof does **not** yet establish full original-repository module compilation or installed-JDK parity. The GitHub Actions workflow binds the original six Java/JNI/test blob hashes to the exact PR head, compiles the **original full Java owner and full native C source**, generates JNI headers, and executes all four modes. Its completion is an independent required gate.

## Catalogue and promotion
`CATALOGUE.tsv` is grouped by `binary-search/interval-boundaries`, `monotone-merge`, `JNI/batch`, and `ABI/evolution`. Donors evaluated: OpenJDK/LeetCode #34, #35 and #88 algorithm categories, current M3 JNI owners. Decline any donor needing JNI Java callbacks, generic comparators, heap per-entry allocations or license mixing. Java runtime ownership remains M3JDK21; Synexia recipes are not shipped.

**HOLD / NOT RUN:** Actual Java/OpenRewrite SDK convergence and fixed-point on this new crate, original full rank JNI source C compilation, full current M3 collections reactor, native sanitizer cross-platform parity, installed JDK21 build, jtreg/JCK and CPU/allocation benchmarks. Both PRs remain draft and must not be merged based on isolated kernel proof.
