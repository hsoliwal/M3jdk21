<!-- SPDX-License-Identifier: Apache-2.0 -->
# Original M3JDK21 collection owner: fused masked address gather (2026-10-26)

This is a direct **M3JDK21-owned Java/C JNI source change** to the existing `com.m3.collections.M3LongAddressKernel` and `m3_long_address_kernel.c`; NOT an M3-on-JDK adapter or a Synexia runtime library. Canonical OpenRewrite transformation recipes and donor/problem-category catalogue stay solely in [Synexia PR #13224](https://github.com/hsoliwal/com.synexia/pull/13224), commit `d3a6cb6cfc9ce2ac2ce8b80e1876e77b2228daa3`. M3JDK21 receives only exact original-source outputs, test sources, scoped CI and this data-only receipt.

## Source custody and unchanged contracts
Original product PR #1882 head before pass: `1ff65d43bd3f4eb35de8e5174d46e7ac743f95fa`.

| Original product path | Original Git blob | Exact received Git blob |
| --- | --- | --- |
| `src/main/java/com/m3/collections/M3LongAddressKernel.java` | `9ac6c5a1e121affd28e6503d922927f6c91c3a66` | `a106baf182c47088dfb6dbfc6c249592f2a0c71b` |
| `src/main/native/collections/m3_long_address_kernel.c` | `821876dfb56d836a0ae23e4bc16a391fd7f97ef2` | `123e40c56995574979e999e6233a218b3dc2e4cb` |
| `src/test/java/com/m3/collections/M3MaskedGatherContract.java` | ABSENT | `d4271622eabfab82f7329d8817b0b35d0307391f` |
| `src/test/java/com/m3/collections/M3MaskedGatherTest.java` | ABSENT | `88ce86d76bfd740dfdab47761e9f38e9d0af1e5e` |
| `.github/workflows/m3-masked-gather-java21-jni.yml` | ABSENT | `acfce5498e0f6e346bb22c7e6a474cf4beaba94f` |

The first four relative paths above are under `m3/collections/` (the workflow is repository-root). The original `gatherNative` JNI symbol is retained, all existing Java method bodies retain their original source bytes and signatures, no C source was deleted and no user callback crosses JNI.

## New API and behavior
`public static int gatherSelectedInto(long[] payload, int[] addresses, byte[] rowStates, int addressFrom, int length, long[] output, int outputFrom, int requiredBits, int forbiddenBits, M3IndexMaskCompactor.Backend backend)`.

Logical `rowStates` bytes align with positions in `addresses`. Required/forbidden flags are unsigned 8-bit masks, disjoint. Output must be a caller-owned non-aliasing long[] with at least `length` capacity from `outputFrom`; the method returns the number of packed values actually written. All selected physical addresses are checked before any output writes; an unselected invalid address is not dereferenced. Original source order and duplicate physical positions are retained, output padding stays unchanged, and no array metadata or native pointer persists beyond the operation. Caller must keep input arrays stable for the call.

`Backend.JAVA` is pure-Java two-pass preflight/gather; `Backend.NATIVE_REQUIRED` requires `m3_collections_address` and the additive `gatherSelectedNative` ABI, otherwise throws. JNI uses a fixed 256-slot stack index/mask/value buffer and bounds each `GetPrimitiveArrayCritical` hold to 256 positions; no JNI calls occur while pinned. Native preflight is complete before any `SetLongArrayRegion` write.

The algorithm catalogue relates this to LeetCode #27 (stable write-cursor compaction), #283 (order retention), #1920 (physical index lookup) and the existing M3 mask compactor. No LeetCode, HackerRank or GeeksforGeeks source code was imported. A fused primitive selection/gather avoids creating intermediate boxed entries or a detached selected index array; **CPU speedup is not claimed absent benchmarks**.

## Actually executed focused evidence
Local Java 21 compile `--release 21 -Xlint:all -Werror` PASS; GCC 14 C17 `-Wall -Wextra -Werror -fsanitize=undefined` PASS. Native symbol generated from javac `-h` and checked with `nm`. Standalone normal JVM Java: `PASS M3_MASKED_GATHER checks=976328 native=false`; JNI normal and `-Xint` with `-Xcheck:jni`: `PASS M3_MASKED_GATHER checks=1950158 native=true`. The local test compiled the new method against a minimal test-only `Backend` enum, not the whole product module. The new **real original-source GitHub job** uses both existing named modules, generates real JNI headers, builds the entire original native C owner and runs the same oracle; it is the authoritative product integration gate.

**NOT YET PASSED:** current-head Actions, full Maven/JUnit reactor, original all-JNI contract suite, actual Synexia OpenRewrite SDK fixed point, built/installable java.base JDK/jmods, jtreg/JCK, cross-platform and resource performance. Keep PR #1882 **DRAFT/HOLD**; no protected-branch writes, merges or gate bypasses.
