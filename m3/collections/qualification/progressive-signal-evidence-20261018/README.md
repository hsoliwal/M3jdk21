<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# M3 progressive comparison evidence — bounded receiver 2026-10-18

## Exact ownership and source qualification

This independently built Apache-2.0 `m3/collections` capability receives the
Synexia-owned Maven/OpenRewrite recipe
`com.synexia.m3.M3Jdk21ProgressiveSignalEvidence` from
[hsoliwal/com.synexia#12236](https://github.com/hsoliwal/com.synexia/pull/12236).

Source authority and reviewed historical donor: Synexia PR #11857, commit
`79d63fe2ab3656d7387617c0c3bf73900dacd81d`, with source blob
`44c84c84d5d788b15f8e3d5d43f02eebd92573ca` and test blob
`e3e979e5b649dd74637b01189da40848a147dd78`.
The target source and test are exact copies of the two recipe-sealed
postimages after an explicit target owner/package naming adaptation.

**Scope:** `com.m3.collections.M3ProgressiveSignalEvidence` is an
additional candidate API in the existing product proving ground. It does
not replace `java.util`, enter HotSpot, or advance the active STRING
receiving stage. The required order is STRING -> arrays -> collections
-> AST/compiler. There is no Synexia runtime dependency.

## How it fits the existing collection architecture

Existing `M3SignalOrder` owns stable primitive signed-double ordering,
optional required-native radix sort, source row-index preservation and
byte-mask filtering. The new evidence owner is **not a sorter**.
It consumes precomputed signal dimensions through a caller-provided
immutable snapshot `Reader` and operates on one pair at a time.

Physical retained comparison state:
- `long[]` evaluated masks (multiword for more than 64 dimensions)
- `byte[]` per-dimension comparison signs
- `double[]` numeric separation facts
- O(dimensions) shared descriptors and one caller-owned workspace

No additional per-collection-item Entry, no global N-by-N pair table,
no retained source object pointers, and no second source payload.
The caller supplies the snapshot sourceVersion, invalidating older
workspaces after any relevant underlying source mutation.

## Distinguish order and distance

`Double.compare(a,b)` defines a total order including signed zero and
canonical NaNs. Its sign is **not** a distance. Distance observations
are available only when finite operands produce finite separation, and
they apply to explicitly declared weighted-L1 coordinate dimensions,
not automatically to whole Java objects. A finite maximumGap is a
caller-provided certificate checked on refinement. Bounds are
rounded conservatively; nonfinite/overflow cases remain unknown.

`refineUntilRankKnown()` evaluates the first decisive ordered dimension,
`refineMetric()` evaluates only metric dimensions and reuses prior facts.
Only a complete exact schema or a canonical verifier can certify exact
domain order. Rank equality does not imply object equality.

Signal families include identity, lexical, structural, fields, methods,
dependencies, graph, semantic, distance, statistical, temporal, memory,
runtime, collection, compiler, behavioral, reliability and domain.
`SOURCE_TIME` provenance is metadata about a compiled symbol; instance
field values remain separately sampled/precomputed.

## Native boundary

The existing opt-in `M3SignalOrderNative` continues to operate on
`long[]` sortable keys and `int[]` row ordinals. This receiver does
not add or claim a JNI comparison ABI: Java domain callbacks,
exception behavior and GC-traced object identity stay in Java.
Future native batch-distance work requires a separately sealed recipe,
Java oracle, JNI parity, native lifetime and CPU/heap proof.

## Exact current qualification command (target checkout)

```sh
d="$(mktemp -d)"
javac --release 21 -proc:none -Xlint:all -Werror -d "$d" \
  m3/collections/src/main/java/com/m3/collections/M3SignalOrder.java \
  m3/collections/src/main/java/com/m3/collections/M3SignalOrderNative.java \
  m3/collections/src/main/java/com/m3/collections/M3ProgressiveSignalEvidence.java \
  m3/collections/src/test/java/com/m3/collections/M3SignalOrderContract.java \
  m3/collections/src/test/java/com/m3/collections/M3ProgressiveSignalEvidenceContract.java
java -cp "$d" com.m3.collections.M3SignalOrderContract
java -cp "$d" com.m3.collections.M3ProgressiveSignalEvidenceContract
rm -rf "$d"
```

The donor recorded focused Java 21 verification and 18,549 assertions.
The target Actions gate is configured in
`.github/workflows/m3-progressive-signal-evidence-20261018.yml`.
Only actual target runs qualify target Java evidence. Full Maven reactor,
JDK-image, jtreg, JNI platform, measurement and binary distribution gates
remain independently PENDING until recorded.

## Recipe and catalogue reuse

The named OpenRewrite recipe, exact resource SHA-256, JUnit fixed-point and
stale-input rejection, and category decision table are canonical in Synexia
PR #12236. LeetCode 215 (top-K/selection), HackerRank Java Comparator/Sort
(tie chains), GeeksforGeeks Radix/Counting Sort (bounded digit passes),
TheAlgorithms/Java (MIT), and OpenJDK 21 Comparator are reviewed as
problem/category/donor references. No challenge solution or third-party
implementation body is copied into M3JDK.

See `TARGET_MAPPING.tsv` for the exact target disposition. Updating a
historical full-donation identity manifest without a new qualified
family-wide source review is intentionally deferred; that manifest must
not be rewritten to imply collections phase completion.
