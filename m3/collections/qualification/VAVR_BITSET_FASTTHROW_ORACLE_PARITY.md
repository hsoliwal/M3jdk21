<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->

# Vavr BitSet: reconcile original fast-throw oracle with the installed JDK

## Observed evidence, not a conjectured fix

The original source-built M3JDK21 [run 45323189042](https://github.com/hsoliwal/M3jdk21/actions/runs/45323189042)
successfully compiled and discovered 119 provider JUnit invocations but failed
three Vavr BitSet oracle comparisons. Under default HotSpot tiering, the
reference's implicit exception can lose its message at a warmed invocation
site (fast throw) while the optimized implementation can still expose a
different message. The original provider runner was subsequently hardened to
run *only its provider-test subprocess* with
`-XX:-OmitStackTraceInFastThrow` to keep comparable VM diagnostics, without
rewriting or suppressing any original tests.

**The resulting actual source-built run**
[45328222291](https://github.com/hsoliwal/M3jdk21/actions/runs/45328222291)
ran the original BitSet implementation and the corrected provider runner and
completed **119 found, 119 started, 119 passed; 0 failed and 0 skipped**.
The independent final Maven stream suite then failed its loader/creation
stage; this is not a full JDK release PASS.

Later, [PR 3428](https://github.com/hsoliwal/M3jdk21/pull/3428) inserted three
explicit `new NullPointerException()` calls into private BitSet paths to match
the older, message-less fast-throw outcome. Those calls were merged *after*
the provider-runner FastThrow correction. With both active, actual
[run 45369096818](https://github.com/hsoliwal/M3jdk21/actions/runs/45369096818)
again executed all **119** tests but failed the same three original
`BitSetWordOpsOracleTest` methods: the reference now reported the
**helpful** implicit-JDK21 NPE message and the patched implementation
reported `null` for the explicit NPE. The actual return values, receiver
bits, and logged arguments matched.

## Intrinsic receiving correction

Restore the original `BitSet.java` **byte-for-byte** to Git blob
`471fde3cd42b8c51d1623609964ff08dc127e23f`, SHA-256
`3dc641daa50867f83cbd4157811616093f02339274e6f07804c34d46a1e0d3eb`.
This source is the prior exact work-order postimage of the immutable Synexia
`m3-bitset-prefix-allocation-20261123` recipe. The current preimage from
the now-historical null-parity work order was blob
`0f63b5e63ba98ecb8c133d11e06728146dc8003e`, SHA-256
`1e9fa447590f14a83592ee455f0198d155e00ad087370c9477738a688dfd5d27`.
Only those three private null-handling changes are undone; no adjacent
implementations, public methods, native sources, module descriptors, original
tests, or Maven POM are modified. The original Apache/Vavr attribution and
all optimized non-null word paths are retained.

The provider runner already contains
`-XX:-OmitStackTraceInFastThrow`; it is not changed. The existing native
workflow's **source guard now requires this option** as well as the exact
restored BitSet SHA-256, original `BitSetWordOpsOracleTest`, reference
`RefBitSetModule`, prefix-allocation regression, and the three current
Maven `ObjectStream` suite hashes. It preserves source-built JNI/C11,
full/linked GINQ and countByMany, selected jtreg, **119 original provider
JUnit invocations**, and the 22-case Maven/Surefire stream test proof from
merged [PR 3430](https://github.com/hsoliwal/M3jdk21/pull/3430).

## Admission / compatibility

The 119/119 JUnit PASS described above belongs to **an earlier exact commit**,
not this new receiving head. A fresh source-built image must independently
pass the original provider cohort and the previously blocked **22-case
Maven/Surefire stream suite**, with zero skipped/failed assertions, and must
retain native and jtreg results. Groovy 4/5/6 equality, GINQ DSL/AST,
String dispatch collision, multi-platform JNI and repository-wide history
remain separate unwaived gates. The source-based restoration is not
performance or general release admission.

Do not retroactively mutate the merged Synexia null-parity recipe: keep it
as an immutable historical work order and publish a **new exact reverse
source-sealed recipe** for this regression correction.
