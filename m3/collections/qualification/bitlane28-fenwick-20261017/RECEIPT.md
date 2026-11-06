# M3BitLane28 indexed rank/select — exact Synexia receiver

**Canonical recipe:** [Synexia PR #12053](https://github.com/hsoliwal/com.synexia/pull/12053),
`com.synexia.m3.M3Jdk21BitLane28Fenwick20261017`, reusing the existing
`M3HashPinnedJavaSnapshotRecipe`.

**Product scope:** one existing owner + two new tests. No JDK `java.base`,
HotSpot, JNI ABI, native library, sorted/filtered wrapper or additional collection
hierarchy changed.

## Exact source custody

| Receiver file | Source preimage SHA-256 | Candidate SHA-256 |
| --- | --- | --- |
| `M3BitLane28.java` | `90fe18474a60bc398252fdb701a45451bb70747436c9747e136335c00f77f8ad` | `45eb57846a3607ef68855c667e258c5c24b90e61e071becc19c9819cbd06dfcc` |
| `M3BitLane28FenwickSelfTest.java` | ABSENT | `8ce3930aa786cb5f07990b97241504436d6daaba2203f9ab1bccd4c28e064827` |
| `M3BitLane28FenwickTest.java` | ABSENT | `7b1a41d7ed8fc94525ff6ebd8cf34eb6a8ace8e837aa4c9b55ee3e56125d285a` |

Receiver files are copied **without edits** from the canonical Synexia
resource images. This is candidate installation, not proof that the SDK emitted
them during an actual Maven `rewrite:run`; that gate is separate.

## Behavior and cost

The sparse 28-bit bitmap remains the sole payload owner, partitioned
region/block/offset = 8/8/12. Lazy primitive Fenwick counters retain region
and per-region block population counts. Rank, range cardinality and
select operate on small primitive prefixes plus one bitmap block.

Original `payloadBytes()` still counts **only** occupied bitmap words;
`indexPayloadBytes()` is additive metadata and does not include array headers.
Empty bitmap index uses zero metadata bytes. Toggle costs increase; no throughput
claim without matched benchmark data. Existing `cardinalityNative` signature
and its underlying native ABI remain unchanged.

## Focused qualification

From a checked-out product repository with JDK 21:

```sh
mkdir -p /tmp/m3-bitlane28-proof
javac --release 21 -proc:none -Xlint:all -Werror \
  -d /tmp/m3-bitlane28-proof \
  m3/collections/src/main/java/jdk/m3/collections/M3Address28.java \
  m3/collections/src/main/java/jdk/m3/collections/M3BitLane28.java \
  m3/collections/src/test/java/jdk/m3/collections/M3BitLane28FenwickSelfTest.java
java -cp /tmp/m3-bitlane28-proof com.m3.collections.M3BitLane28FenwickSelfTest
```

Expected finite oracle receipt:
`PASS M3BitLane28FenwickSelfTest checks=21298 mutations=15000`.

Also run actual Maven/OpenRewrite SDK fixed-point/refusal test from the
Synexia PR and the target Maven/JUnit suite, existing C17 JNI `-Xcheck:jni`
parity, all applicable JDK integration gates and warmed/cold benchmarks
before promotion. Those broader gates have **not** been certified by this
receiving PR. Keep DRAFT.

## Provenance

First-party Apache-2.0 source in M3JDK21; first-party Synexia
`PackedLongFenwickTree` (Git blob
`05f97d7dd5d1e80216178f6a8b3805934b1a7e0a`) is the adaptation
reference. JDK/LeetCode/HackerRank/GeeksforGeeks are
comparison/problem-shape references only. No external solution code copied.
