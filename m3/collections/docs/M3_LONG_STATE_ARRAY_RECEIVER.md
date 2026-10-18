<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3 long-value/state-mask dynamic array — isolated receiver

## Representation

`M3LongStateArray` retains a single `M3LongArrayList` as the mutable
primitive-value owner and a parallel full-width `int[]` of caller-defined
state bits. Capacity is dynamic. Positional insert/remove shifts both lanes
in lockstep, while `set`, `setState` and `updateState` target one slot.
Values include all longs; masks include all 32 state bits. No per-element
Entry allocation, unstable native pointer, GC-root claim, or object-header
mutation is introduced.

`snapshot()` produces a detached value/state generation. A prepared selection
reuses the existing `M3IndexPathRank` bitmap/prefix rank/select owner. It
supports source-ordinal windows with `[from,to)` offsets, preserving repeated
occurrences. `indexed()` and ascending/descending sorting explicitly
materialize a second detached value copy through the established
`M3LongIndexProjection`; this is **not** zero-copy value sorting or a live
sorted wrapper. Caller arrays are not retained.

Explicit `M3IndexPathMasks.Backend.NATIVE_REQUIRED` invokes the existing
C17/JNI path-mask primitive bulk selector and checks its row indices against
the independently prepared Java rank index. An unavailable JNI library fails
closed. Java callbacks and object references remain outside JNI.

The existing live `M3FilteredLists`, `M3FilteredViews`,
`M3FilteredNavigableSet`, sorted primitive views, concurrent/GC-sensitive
owners, and all `java.util` APIs remain unchanged. This new owner is
single-threaded, and neither index positions nor generation values are stable
object handles. The current M3 migration phase is still STRING; this is an
additive collection receiving candidate, not general JDK promotion.

## Reproducible source recipe and proof

Source recipe authority: `hsoliwal/com.synexia` descriptor
`com.synexia.m3.M3Jdk21LongStateArrayReceiver` with exact after-images for
the Java owner, JUnit tests, algorithm catalogue, and this receipt.
Target: `hsoliwal/M3jdk21/m3/collections`. All source changes are
reviewable alongside recipe tests and provenance; no Synexia runtime
dependency or LeetCode/HackerRank/GeeksForGeeks solution code is copied.

Focused Java 21 gate:

```sh
mvn -B -ntp -f m3/collections/pom.xml \
  -Dtest=M3LongStateArrayTest,M3LongArrayListTest,M3IndexPathRankTest,M3IndexPathMasksTest test
```

Optional native parity depends on the already installed
`M3IndexPathMasks` JNI provider; no new JNI ABI is added.
The test covers randomized array operations against `ArrayList`, masks
including `0x80000000`, indexed rank/select, stable sorted ties,
detached generations, bounds, fail-closed native requests, and reflection
checks prohibiting retained per-entry storage.

Standalone Maven, full JDK image/jtreg, JNI parity, CPU, peak allocation,
retained heap and cross-platform gates must be reported separately. No
throughput win or java.base adoption is claimed without that evidence.
