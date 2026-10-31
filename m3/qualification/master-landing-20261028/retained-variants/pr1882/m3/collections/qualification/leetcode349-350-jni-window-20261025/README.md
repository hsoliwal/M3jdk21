<!-- SPDX-License-Identifier: Apache-2.0 -->
# Original M3JDK21 rank JNI: signed-long offset intersection

## Canonical source custody (2026-10-25)
Synexia is the convergence/recipe owner: [com.synexia #13224](https://github.com/hsoliwal/com.synexia/pull/13224), commit `a7fb69b543d8874ce8c75cb11c2e15d17fd1e2e3`. Product M3JDK21 owns original Java/C implementation. Only exact Java/C/test/CI results are received here; this module **does not depend on Synexia runtime or recipes**. The existing rank/index owner and JNI library remain authoritative, not a new m3+JDK compatibility wrapper.

| Original product file | Old Git blob | Exact received Git blob |
|---|---|---|
| `src/main/java/com/m3/collections/M3LongRankBatchNative.java` | `fe4bd1751126d1a5c21c359bf9f028013d380a4f` | `99872c10902c15cbbf143dd158a6bcf6736465e1` |
| `src/main/native/collections/m3_long_rank_batch_jni.c` | `496e04bc57c00ba66895379229f3dc4e4dc0aa2c` | `e7ed7e9a3a9eb71e1970d8b4f023147afb8e7543` |
| `src/test/java/com/m3/collections/M3LongRankWindowIntersectionContract.java` | ABSENT | `d5e611897131123061c5e40886be1cb672c58f0a` |
| `src/test/java/com/m3/collections/M3LongRankWindowIntersectionTest.java` | ABSENT | `c147f3f3c2376407bd95fe138c5eb7f2d07b1458` |
| `.github/workflows/m3-leetcode349-350-jni-window-java21.yml` | ABSENT | `d2a988be9e802e5f9fd729dd21968076752955c1` |

The source files above are under `m3/collections/` unless their relative path starts with `.github/`. The canonical Synexia crates use both the existing `M3HashPinnedJavaSnapshotRecipe` and `M3HashPinnedTextSnapshotRecipe`. They preserve the original source digests and fail on missing/drifted Java/C owners. Original C source is preserved byte-for-byte as a prefix, with only one additive JNI function.

## LeetCode-driven primitive design
- **#349 (unique intersection):** skip repeated matched values in already-sorted signed-long windows.
- **#350 (multiset intersection):** count each common value up to the minimum occurrence count.
- **#26/#88:** dedup state and ordered two-cursor merge are problem *patterns*, not copied code.
- **#34/#35:** retain the existing rank/binary-search APIs, which may outperform scanning on unbalanced inputs.

All endpoints are validated before native dispatch, preventing `offset+length` overflow. The new package-local APIs accept two original `long[]` lanes with independent offsets/lengths; inputs are immutable during operation. There is no full-slice allocation, Map.Entry, Java comparator JNI callback, retained native pointer, per-value object, or sorting of the source payload. One optional `m3rankbatch` JNI call scans two fixed 256-long pages (4 KiB stack) and returns a `long` count. Old `m3rankbatch` binaries lacking this additive export retain the Java oracle in *automatic* mode; explicit native-required calls fail closed.

No LeetCode editorial, HackerRank answer, GfG solution, or OpenJDK implementation is copied. All new Java/C and source-compiled tests are original first-party Apache-2.0 code; algorithm choices, licenses and rejected approaches are recorded in Synexia's `CATALOGUE.tsv`.

## Verified, not verified
**Executed in local Java21/C17 isolation**: `javac --release 21 -Xlint:all -Werror`, `gcc -std=c17 -Wall -Wextra -Werror -fPIC -shared`; 6,037 Java-mode assertions, 10,059 actual native-mode assertions with `-Xcheck:jni` and 10,059 under `-Xcheck:jni -Xint`. Each tests 1,000 deterministic sorted-window trials, duplicate frequencies, `Long.MIN_VALUE`/`MAX_VALUE`, JNI/direct ABI errors, integer-overflow ranges and source-array immutability. The fixture compiles the new ABI function and matching Java algorithm in isolation; it **does not prove the full original C shared library together**.

**Current-head mandatory gates**: source-sealed exact-head original Java/C compile, full C JNI library build, JUnit plus independent native-required oracle, actual Synexia Java/Text OpenRewrite SDK and fixed point, full M3JDK21 collections Java/JNI source module, jtreg/JCK, installed JDK image, Windows/macOS and CPU/alloc benchmarks. These are pending. Do not assume speedup merely because a JNI path exists.

**DRAFT/HOLD:** This code stays in the original M3JDK21 product. It is not release-certified integration into `java.base`. No original JNI symbol, `M3IndexSort.stable` observable behavior, existing source-epoch receipt or branch protection may be bypassed.
