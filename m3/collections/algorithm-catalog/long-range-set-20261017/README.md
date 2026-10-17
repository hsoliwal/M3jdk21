<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Recipe-first M3 primitive long half-open range set

Canonical Synexia source/recipe owner: `com.synexia.m3.Jdk21LongRangeArrays20261017`.
Target: separately built Apache-2.0 `m3/collections` in M3JDK21.
New ABSENT-before source `M3LongRangeSet.java` Git blob
`5dbc2ec9317c79d801bb4c312f2c0b7ffad6ffbd` and SHA256
`f865b006888f1d4554b51cf69e7f4d6bac67458982f957f9151af0b4f09eaec8`.
Differential oracle source blob `f18a4d01b263a7e8471ecba2720e027b2a119abe`,
SHA256 `3b4ba4e2542ebbc607710f183f231d7c1bbe3e94ef4aa4fe6de49f0f9242ac29`.
JUnit blob `592004d8776f5122914a214566e33a84ad5cdfdf`,
SHA256 `1768a8be759b46ae24678ce089cdf328ef6c83f335bd3f5582cda813819261a5`.

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
