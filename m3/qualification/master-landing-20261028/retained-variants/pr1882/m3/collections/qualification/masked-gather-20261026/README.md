<!-- SPDX-License-Identifier: Apache-2.0 -->
# Original M3JDK21 collection owner: fused masked address gather (2026-10-26)

This is a direct **M3JDK21-owned Java/C JNI source change** to the existing `com.m3.collections.M3LongAddressKernel` and `m3_long_address_kernel.c`; NOT an M3-on-JDK adapter or a Synexia runtime library. Canonical OpenRewrite transformation recipes and donor/problem-category catalogue stay solely in [Synexia PR #13224](https://github.com/hsoliwal/com.synexia/pull/13224), commit `d3a6cb6cfc9ce2ac2ce8b80e1876e77b2228daa3`. M3JDK21 receives only exact original-source outputs, test sources, scoped CI and this data-only receipt.

## Source custody and unchanged contracts
Original product PR #1882 head before pass: `1ff65d43bd3f4eb35de8e5174d46e7ac743f95fa`.

| Original product path | Original Git blob | Exact received Git blob |
| --- | --- | --- |
| `src/main/java/com/m3/collections/M3LongAddressKernel.java` | `fe30e94f5e66e2f4d43365bddb58dfdbb4f25207` | `9dea1df7254fc8f99449be544d410dbf973a137b` |
| `src/main/native/collections/m3_long_address_kernel.c` | `9f68bba46827db2cb53bdb597042829358663f91` | `123e40c56995574979e999e6233a218b3dc2e4cb` |
| `src/test/java/com/m3/collections/M3MaskedGatherContract.java` | ABSENT | `7c62c52d5fc97d933f76db143fb0ac54000ccfff` |
| `src/test/java/com/m3/collections/M3MaskedGatherTest.java` | ABSENT | `80714c8cd974dcdf6886bc80ca7d7b26c78def9f` |
| `.github/workflows/m3-masked-gather-java21-jni.yml` | ABSENT | `0a77c515f7944622918b4aff086243ceef08f641` |

The first four relative paths above are under `m3/collections/` (the workflow is repository-root). The original `gatherNative` JNI symbol is retained, all existing Java method bodies retain their original source bytes and signatures, no C source was deleted and no user callback crosses JNI.

## New API and behavior
`public static int gatherSelectedInto(long[] payload, int[] addresses, byte[] rowStates, int addressFrom, int length, long[] output, int outputFrom, int requiredBits, int forbiddenBits, M3IndexMaskCompactor.Backend backend)`.

Logical `rowStates` bytes align with positions in `addresses`. Required/forbidden flags are unsigned 8-bit masks, disjoint. Output must be a caller-owned non-aliasing long[] with at least `length` capacity from `outputFrom`; the method returns the number of packed values actually written. All selected physical addresses are checked before any output writes; an unselected invalid address is not dereferenced. Original source order and duplicate physical positions are retained, output padding stays unchanged, and no array metadata or native pointer persists beyond the operation. Caller must keep input arrays stable for the call.

`Backend.JAVA` is pure-Java two-pass preflight/gather; `Backend.NATIVE_REQUIRED` requires `m3_collections_address` and the additive `gatherSelectedNative` ABI, otherwise throws. JNI uses a fixed 256-slot stack index/mask/value buffer and bounds each `GetPrimitiveArrayCritical` hold to 256 positions; no JNI calls occur while pinned. Native preflight is complete before any `SetLongArrayRegion` write.

The algorithm catalogue relates this to LeetCode #27 (stable write-cursor compaction), #283 (order retention), #1920 (physical index lookup) and the existing M3 mask compactor. No LeetCode, HackerRank or GeeksforGeeks source code was imported. A fused primitive selection/gather avoids creating intermediate boxed entries or a detached selected index array; **CPU speedup is not claimed absent benchmarks**.

## Actually executed focused evidence
Local Java 21 compile `--release 21 -Xlint:all -Werror` PASS; GCC 14 C17 `-Wall -Wextra -Werror -fsanitize=undefined` PASS. Native symbol generated from javac `-h` and checked with `nm`. Standalone normal JVM Java: `PASS M3_MASKED_GATHER checks=976328 native=false`; JNI normal and `-Xint` with `-Xcheck:jni`: `PASS M3_MASKED_GATHER checks=1950158 native=true`. The local test compiled the new method against a minimal test-only `Backend` enum, not the whole product module. The new **real original-source GitHub job** uses both existing named modules, generates real JNI headers, builds the entire original native C owner and runs the same oracle; it is the authoritative product integration gate.

**NOT YET PASSED:** current-head Actions, full Maven/JUnit reactor, original all-JNI contract suite, actual Synexia OpenRewrite SDK fixed point, built/installable java.base JDK/jmods, jtreg/JCK, cross-platform and resource performance. Keep PR #1882 **DRAFT/HOLD**; no protected-branch writes, merges or gate bypasses.
