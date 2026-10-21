<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 indexed full-int mask input-bounded scan qualification

**Product authority:** M3JDK21 integrated Java21/HotSpot/JNI SDK. Java public module owner `m3/collections/src/main/java/com/m3/collections/M3IndexPathMasks.java`. It remains the one authoritative primitive path/offset/state-mask selection owner. No M3-on-JDK layer, no Synexia runtime jar, no additional retained per-entry storage.

**Canonical recipe:** `hsoliwal/com.synexia@0e58045ec79ceb7d64284c5abb3bd5437961ffb4`, `com.synexia.m3.M3IndexPathBudgetedJni20261019` invoking existing class-backed `M3HashPinnedJavaSnapshotRecipe` and exact before/after source fixtures. No hand-edited generated target.

| Source seal | Exact value |
| --- | --- |
| Source Git blob | `1e77b2027aa008aa8aebfeaf4b00a5bb89e5b7bf` |
| Source SHA-256 | `33b894d2b4c9f89a0244023737d49645d54cbcb1b388a0956e475567fdd4a577` |
| Generated Git blob | `76205637d13cc185f5a46677358fefcf0279245d` |
| Generated SHA-256 | `b6c37fb98884558d396bd841aa700014b58b305b17ced29f69e3ec5d40a74c8c` |
| Existing C17/JNI donor | `m3/collections/src/main/native/collections/m3_index_path_masks.c`, blob `086b49293257fd17cc7fbd1ebbb98460aa677e35` |
| Java algorithm donor | Existing `M3IndexMaskWindow.scanBounded` for budget shape, existing `M3IndexPathMasks.scanWindow` for exact semantics |
| Third-party donor code | None copied; challenge sites taxonomy only; tools4j related candidates remain separately catalogued |

**New additive API**: `scanWindowBounded(..., int next, int maxExamined, int[] output, int outputOffset, int limit, Backend backend)`. No change to original `scanWindow` byte contents or JNI exported signatures. Checks full logical geometry, clamps ordinal count with `next + min(maxExamined, count-next)` (nonoverflowing after bounds check), and delegates exactly to existing validator, preflight, order-preserving Java selector and C17 selector. An empty result may still advance; `nextOrdinal` is in the original whole-path ordinal coordinates.

**Java21 execution (reduced contract model): PASS.** Strict `javac --release 21 -Xlint:all -Werror` plus 23,031 independent Java scalar assertions covering 5-stage bit planes, signed/repeated IDs, positive/negative stride, max 4,096 output, untouched cells/aliases, invalid future offsets, zero/huge budget, whole legacy parity, and 1,000,000 row sparse traversal in 512 input-ordinal windows.

**Added but NOT_EXECUTED as repo:** real target `M3IndexPathMaskBudgetSelfTest` main and `M3IndexPathMaskBudgetTest` JUnit gateway; canonical OpenRewrite replay/drift/fixed point tests.

**NOT_EXECUTED:** full Maven Java21 module/reactor, real JNI `--native` and `-Xcheck:jni`, native C17 compile and parity, full source-built M3JDK21 and installed-image launch, vt/jtreg/HotSpot, all five JDK/Guava/Eclipse Collections/Commons Collections/Spring Framework contracts, JMH/CPU/heap/native memory comparisons. This draft does not advance String -> arrays -> collections admission.

**Work selection catalogue:** `m3/collections/algorithm-catalog/index-path-budgeted-jni-20261019.tsv`. Serial next file must re-pin its preimage and consult accumulated problem-category decisions. Neither full filtered/sorted wrapper contract completion nor native speedup is claimed.
