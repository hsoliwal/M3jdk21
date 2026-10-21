<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 JNI batch upper-bound receiving candidate (2026-10-21)

M3JDK21 remains one Java/HotSpot/JNI SDK. This is an additive extension of
`M3LongRankIndex` and its original primitive `M3LongRankBatchNative` owner,
not a new wrapper/layer or payload store. Rank uses signed Java long ordering,
duplicates included; return positions are insertion coordinates in [0, n].
No java.lang.String, JNI array pointer or native memory is retained.
Java remains the default; JNI is explicitly opt-in and batches smaller than
64 queries run in Java. Old native libraries lacking the new ABI probe retain
the old lower-bound native behavior and use the Java upper-bound fallback.

Canonical recipe/source provenance:
- Synexia draft PR https://github.com/hsoliwal/com.synexia/pull/12744
- Recipe com.synexia.m3.RankBatchUpper20261021
- Manifest source hashes verified before receiver writing; native C has
  a separate hash-pinned source template.
- Donor category LeetCode #34 (taxonomy/counterexamples only), existing
  original M3LaneSearch and original rank JNI owner; no challenge code copied.

## Local kernel evidence and outstanding admission

An independently instantiated Java21/C11 candidate ran 23,398 scalar-oracle
cases per Java and current JNI lane, plus a symbol-absent JNI fallback lane,
all passing with -Xcheck:jni (the new C also compiled with strict flags).
Those are bounded prototype results, **not a claim of complete target build**.

Run this receiver script on the actual PR branch:
`bash m3/collections/qualification/rank-batch-upper-20261021/verify.sh`.

At PR creation **NOT RUN**: exact SHA-backed OpenRewrite SDK application,
Maven full collection reactor/JUnit, current PR hosted CI, installed custom
JDK/HotSpot, jtreg/JCK, Windows/Android, sanitizer-on-real-branch,
whole-family ABI audit, cold/warm JNI crossover CPU/retained-heap/native RSS,
String -> arrays -> collections global admission. These remain explicit
review blockers, not silently passing gates.
