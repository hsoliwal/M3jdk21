<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3JDK21 primitive half-open long RangeSet receiver

Canonical Maven/OpenRewrite recipe [Synexia #12159](https://github.com/hsoliwal/com.synexia/pull/12159),
exact source commit `b353a2d74552334e3ff323853af9d1b41be0827c`.
Recipe identity: `com.synexia.m3.Jdk21LongRangeArrays20261017`.
Target product baseline: `e913ab89056657eca4009678cfe9a657c020e79b`.
Three Java target paths were ABSENT before. Their receiver Git blobs
are **exactly** the reviewed canonical template blobs; no other
Java, `java.util`, HotSpot, JNI or native source changed.

- `M3LongRangeSet.java`: SHA256
  `11a1001d54bbd39d25102dab36b9ee7a9125713f8a1496598655f41f9c47652e`,
  Git blob `8f3ed8efe75e7eb4744b35537192cf1a8b169423`.
- `M3LongRangeSetSelfTest.java`: SHA256
  `420496cf602f3f08f8aef10a9ff73d1e104d7582a19be18d697f1898025595cb`,
  Git blob `e9d18b07711ab54f8411697827921910b7c64b86`.
- `M3LongRangeSetTest.java`: SHA256
  `b57500ea01b78f1db7c5deb809758116ec27c99f23d108b04431c3fbdadf6b4d`,
  Git blob `7864c4d62e9cb113a22aad31cef643e28461d96c`.
- Canonical donor/category ledger copied exactly into product
  `algorithm-catalog/long-range-set-20261017/`.

Dynamic array `starts[]` and `ends[]` represent sorted merged
half-open signed-long ranges, with explicit split/merge, binary
search, exact `BigInteger` count and primitive callback.
Caller-provided full 32-bit interval state flags flow through
the existing M3JDK21 `M3IndexPathMasks.select` Java/C17 JNI ABI;
only primitive row IDs, physical offsets and int mask words are sent.
No stored Range/Entry/Node objects, Java predicate callbacks through
JNI, native heap pointers or new JNI symbols. The RangeSet is an
M3 primitive analogue only, **not** Guava's generic `RangeSet`
or an implementation of any Eclipse/Spring/Apache/JDK collection
interface. Intervals do not encode the inclusive Long.MAX_VALUE
endpoint. Underlying source state is not thread-safe.

**Local finite qualification**: `javac --release 21 -Xlint:all
-Werror`, `-Xmx64m`, 20000 seeded operations and 411913 checks PASS
against a **test-only mask-selector stand-in**. The new target
workflow compiles the actual current M3IndexPathMasks Java owner,
exercises the same selftest, and runs owning module Maven/JUnit.
Actual native deployment and `-Xcheck:jni` parity, full
Synexia OpenRewrite execution, provider/JDK ABI qualification,
installed JDK/jtreg/JCK and CPU/retained heap measurements are
PENDING until exact-head evidence exists. DRAFT/HOLD; do not
claim superset completion or advance String→arrays→collections.
