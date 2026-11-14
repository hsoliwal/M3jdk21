<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3StaticBucketMap: parent-owned array/index implementation candidate

This module retains the original public `jdk.m3.collections.apache.M3StaticBucketMap`
Java21 type, constructor signatures, Map/Commons interfaces and Maven artifact
coordinate. It replaces **29 always-throwing scaffold method bodies** with an
independently implemented, bounded and reusable structure-of-arrays hash map.
The historical generated source is an exact, sealed preimage: it is not reused
at runtime and must not be regenerated over this candidate.

Storage: `Object[] keys/values` and `int[] hashes` own all payloads; `int[] index`
uses 0 empty / -1 deleted / physical-slot+1 live; `long[] liveMask` enumerates
occupied slots with `Long.numberOfTrailingZeros`; `int[] nextFree` reuses deleted
coordinates; split directory rebuilds/slot growth avoid per-mapping Node objects.
Views are lazy and backed; Entry projections materialize only on entry iteration.
This is not a foreign-code import, JNI rewrite or a HotSpot implementation.

## External algorithms (categories only, no copied solutions)

- LeetCode 706 — hash-table put/get/remove; adversarial collision and replacement categories
- HackerRank, Hash Tables: Ransom Note — multiplicity/counting oracle, not imported source
- GeeksforGeeks, Linear Probing — probe sequences, tombstone correctness and load factor
- GitHub `hsoliwal/commons-collections` PR #49 — array-backed StaticBucketMap *architecture reference*, Apache headers reviewed separately; no body copied

## Engineering limits

- Public signatures are retained; `Map` views support iteration/removal and
  `MapIterator` operates on primitive slot offsets.
- Null keys, null values, collisions, overwrites, tombstones, clear/reuse,
  bit-mask scans and mutation fail-fast behavior are exercised by an independent
  `HashMap` model.  Reentrant equality that *structurally mutates* this map
  fails closed to avoid corrupting the directory.
- `atomic(Runnable)` serializes through the owner monitor. Unlike the historical
  per-bucket donor, this is **coarse-grained** and its multi-thread throughput
  has **not** been admitted. No concurrency equivalence claim is made.
- `@Test` wraps the standalone Java21 proof for actual Maven/Surefire, which is
  a mandatory pending CI gate; tests have also been executed with test-only
  copies of the Commons interface shapes under strict `javac` on stock JDK21.
- No automatic JNI, native library, ancestor rewrite, binary release, or
  performance win is claimed. Product JTReg/JNI, build-image, actual Commons
  4.5.0 linkage, serialization/ABI, and full Maven remain HOLD.

The original generated catalogue row remains `SCAFFOLD` until full product
admission; this implementation is a reviewed `CANDIDATE/HOLD`, not a release.
