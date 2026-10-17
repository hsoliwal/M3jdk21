<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 long-range family receiver: indexed RangeSet + RangeMap

**Product receiver:** this branch is stacked on [M3JDK21 #1184](https://github.com/hsoliwal/M3jdk21/pull/1184), which installs the distinct \`M3LongRangeSet\` first. **Canonical source/recipe authority:** [Synexia #12180](https://github.com/hsoliwal/com.synexia/pull/12180) at \`d491ec56d4dab78236302e29163cbc94a942aa72\`. The target source is exact generated AST postimage, never hand-patched product code.

### Data structure

\`M3LongRangeMap<V>\` owns exactly three dynamic parallel arrays: \`long[] starts\`, \`long[] ends\`, \`Object[] values\`. Each slot is a signed-long, half-open \`[startInclusive, endExclusive)\` interval. Sorted, nonoverlapping intervals support \`put\` overwrites with stable left/right fragments, \`remove\` splits and deleting ranges, binary membership lookup, nullable mapped values with \`containsPoint\`, primitive callback and detached snapshots. Adjacent intervals are allowed, even with equal stored values. No retained per-interval Entry or Node; array movement is index/span manipulation and value-array memmoves.

\`selectedByState(int[] flags,required,excluded,M3IndexPathMasks.Backend)\` maps one state word per **interval ordinal** to the **unchanged 32-bit precomputed mask path JNI**. No new native symbol, Java callback or heap pointer retention. \`Backend.NATIVE_REQUIRED\` must actually load \`libm3_collections_pathmask\`; Java default stays Java. The \`Long.MAX_VALUE\` coordinate cannot be included through a signed-long exclusive upper endpoint. This separate M3 API is **not** Guava RangeMap, JDK Map, Spring MultiValueMap or Apache/Eclipse provider contract substitutability.

### Recipe and evidence boundaries

Named \`com.synexia.m3.Jdk21LongRangeMap20261017\` invokes the incumbent **M3HashPinnedJavaSnapshotRecipe** in true Java AST mode. New source templates, 3-target SHA-256, JUnit, original category decisions, Maven fixture, licence and local receipts are mirrored **before** installing emitted Java. The target \`verify.py\` enforces bytes, SHA256 and unchanged Java/C selector and range-set owner Git SHA1 blobs.

Local actual JDK21/Maven/OpenRewrite 8.17.1: **4 recipe JUnit pass**, 8 partial states in 2 orders, fixed point, drift/duplicate refusals. **Real recipe-generated** source compiled strict Java21 and ran **1,689,303 checks / 49,152 seeded operations** per tiered and interpreted run. **Local M3IndexPathMasks was a test-only stub**; those results **are not real JNI evidence**. Real target module Java21/JUnit, checked C17 JNI/native-required selection, installed JDK, all provider API family tests, JMM/GC/serialization and benchmarks are required next.

Run the exact-head target CI workflow and review source/receipt before promotion. Preserve the STRING → arrays → collections → AST/compiler phase order, existing JDK/Guava/Eclipse/Spring/Apache type names, and all Apache-2.0/source rights. LeetCode/HackerRank/GeeksforGeeks are problem categories only, no copied code.
