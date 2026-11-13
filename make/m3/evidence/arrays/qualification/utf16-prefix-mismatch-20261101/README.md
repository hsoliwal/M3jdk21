# M3JDK21 result-only receipt: Java21/C17 UTF-16 first-mismatch atom

**Source authority:** [hsoliwal/com.synexia#15109](https://github.com/hsoliwal/com.synexia/pull/15109),
canonical `M3ArrayPrefixMismatch20261101` Maven/OpenRewrite recipe crate;
exact source parent `23a4e5e2308366ed80377f154c2b3551ded70512`.
**Receiving parent:** staged #2260 `57bd34d40448ab0623b86fe989e51c65f9247ae2`;
neither JDK `master` nor earlier donor PR refs are mutated.

The three original Java/C/test preimages were confirmed against the exact receiver
tree: old Java blob `8ab73b01...`, old C blob `1d026267...`,
and JUnit target **absent**. The postimage blobs are byte-identical to the
Synexia recipe template sources, including all original licence headers.
`SOURCE_SEALS.tsv` pins the full SHA-256 and Git blobs.

The Java fallback uses `java.util.Arrays.mismatch` to find the first
different UTF-16 code unit, then preserves the original numeric `char-char`
delta. The existing JNI branch and same-window identity fast path remain.
The JNI C17 owner uses `memcmp == 0` solely for complete equal 1,024-unit
chunks and otherwise retains the existing `m3_compare_utf16`. The sign of
`memcmp` is **never** returned as UTF-16 order.

The algorithm family is LeetCode LC14 prefix/first-mismatch; HackerRank
longest-common-subsequence is rejected as not equivalent, and GeeksforGeeks
C `memcmp` is restricted to equality. No third-party solution copied.
The public Java signatures, JNI entrypoint names, range checks, null
exceptions, ownership and GC model are unchanged.

## Test gates

The *existing* `.github/workflows/m3-array-substrate.yml` triggers on
`m3/arrays/**` and runs both Java21 JUnit and actual JNI
`mvn -Pnative-linux -DargLine=-Xcheck:jni test`. Do not add another
workflow or bypass the canonical Synexia recipe.

```sh
mvn -B -ntp -f m3/arrays/pom.xml test
mvn -B -ntp -f m3/arrays/pom.xml -Pnative-linux -DargLine=-Xcheck:jni test
```

Local *isolated* algorithm probes passed 231,500 Java21 and 1,024,500
C17 reference cases. **HOLD** until actual receiver-head CI, JDK image,
JIT/C1/C2 and JNI/ABI/GC/performance tests and independent source-loss
checks pass. No performance speedup claimed. Do not merge to master yet.
