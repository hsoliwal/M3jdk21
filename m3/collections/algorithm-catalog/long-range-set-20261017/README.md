<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Recipe-first M3 primitive long half-open range set

Canonical Synexia source/recipe owner: `com.synexia.m3.Jdk21LongRangeArrays20261017`.
Target: separately built Apache-2.0 `m3/collections` in M3JDK21.
New ABSENT-before source `M3LongRangeSet.java` Git blob
`8f3ed8efe75e7eb4744b35537192cf1a8b169423` and SHA256
`11a1001d54bbd39d25102dab36b9ee7a9125713f8a1496598655f41f9c47652e`.
Differential oracle source blob `e9d18b07711ab54f8411697827921910b7c64b86`,
SHA256 `420496cf602f3f08f8aef10a9ff73d1e104d7582a19be18d697f1898025595cb`.
JUnit blob `7864c4d62e9cb113a22aad31cef643e28461d96c`,
SHA256 `b57500ea01b78f1db7c5deb809758116ec27c99f23d108b04431c3fbdadf6b4d`.

New primitive interval collection using sorted nonadjacent long[] start/end
lanes with dynamic growth, merge/split, binary membership, exact
BigInteger cardinality, immutable detached snapshots and primitive callback.
Indexed full 32-bit state-mask selection reuses current
`M3IndexPathMasks.select` Java/C17 JNI owner; no new JNI symbol or
native Java-predicate/comparator callback. No per-range object graph.

Half-open [fromInclusive,toExclusive), signed long; cannot represent
inclusive Long.MAX_VALUE through this exclusive bound. This is not a
Guava RangeSet/RangeMap, Eclipse, Spring, Apache or java.util compatible
type without provider-specific proof. Input rights and donor decisions
are listed in CATEGORY_REVIEW.tsv; only first-party code was authored.

**Executed local preflight**: OpenJDK 21 strict compilation
`javac --release 21 -proc:none -Xlint:all -Werror`, 20,000 seeded
operations and 411,913 assertions in `-Xmx64m` PASS.
The existing M3IndexPathMasks kernel was replaced by a
**test-only source stand-in** in this finite local experiment.
These numbers are NOT evidence of real M3 JNI parity.

Pending: Maven/OpenRewrite actual recipe fixed point/refusal, target
full-module Maven/JUnit, real existing M3IndexPathMasks Java and native
C17 `-Xcheck:jni`, installed JDK jtreg/JCK, serial replay/negative
controls, native/process heap and CPU comparison. Keep both PRs
draft/HOLD until head proof; no blanket public JDK replacement and
no String -> arrays -> collections promotion claim.
