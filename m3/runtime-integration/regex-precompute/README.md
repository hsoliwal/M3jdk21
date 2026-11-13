<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3String regex precompute product verification

Canonical value mapping remains **Synexia MIndexString -> java.lang.M3String**.
Public String and its Pattern fallback retain their existing contracts. This
module verifies the current target-owned regex implementation using the existing
`test/jdk/java/lang/String/M3StringRegexPrecomputeTest.java`. It does not transform
sources, load private recipe fixtures, or require a Synexia checkout.

## Build and verify

After building an M3JDK21 image from the source under review, run:

```sh
mvn -B -ntp -f m3/runtime-integration/regex-precompute/pom.xml \
  -Dm3.test.jdk=/absolute/path/to/source-built-M3JDK21 verify
```

`m3.test.jdk` defaults to the JDK running Maven. That default works only when Maven
itself runs on an M3JDK21 image with the required implementation. A stock Java 21
image must fail on the required M3 VM flag; the verifier never ignores an
unrecognized flag or converts missing native linkage into a passing result.
`m3.python` defaults to `python3` and may be set to a Python 3.11+ executable on
platforms that use another command name. Maven retains the existing pinned
`exec-maven-plugin:3.3.0`; the usual plugin artifacts must be available locally
for offline Maven execution.

The same product proof can run directly, without Maven artifact resolution:

```sh
python3 -B m3/runtime-integration/regex-precompute/verify.py \
  --target . --java-home /absolute/path/to/source-built-M3JDK21 \
  --output m3/runtime-integration/regex-precompute/target/product-verification
```

The standard Maven `test` phase runs `test_verify.py`, which tests the verifier's
failure handling with explicitly synthetic process fixtures. Those unit tests
do not qualify a runtime. The `verify` phase then compiles the unchanged product
test with the selected image's Java 21 compiler and executes all three existing
product modes:

| Mode | Required behavior |
| --- | --- |
| Storage enabled | Pattern differential, invalid/null regex, Unicode seam, real M3 storage, required native route and owner-field contract |
| Storage enabled, compact Strings disabled | The same complete product contract with the original no-compact mode |
| Storage disabled | Existing public String/Pattern behavior with M3 storage explicitly disabled |

Every runtime command enables assertions and `-Xcheck:jni`. Reflection access is
limited to the test's existing `java.base/java.lang` requirement. No product class
is patched into the image. Java launcher environment variables that could inject
options or replacement classes are removed from the child process environment.

## Receipts and acceptance

`target/product-verification/receipt.json` records source-file hashes, compiler
and runtime commands, each exit code, per-mode results, log hashes, test class
hashes and fingerprints of the selected image's release file, launchers, modules,
libjava and VM. Each run gets a fresh directory. An earlier PASS is replaced with
STARTED before source/image inspection, and a missing tool, failed command,
timeout, absent success marker or input/image/class drift records FAILED.

A PASS covers the existing product test in its three direct execution modes.
It is not a jtreg harness receipt or proof that the image was built from these
exact sources. An independent matching build receipt must establish that source
correspondence. Retain the image/build receipt together with these fingerprints.
Full JDK build, jtreg, GC/lifetime, platform/ABI, full-family receiving, performance
and release gates remain separate. This module never advances the String -> arrays
-> collections -> compiler receiving phases or grants release admission.

The target-authoritative jtreg entry remains:

```sh
make test TEST=jtreg:java/lang/String/M3StringRegexPrecomputeTest.java
```

## Preserved recipe and historical evidence

Recipe authoring/execution stays in `hsoliwal/com.synexia`. The original receiver
was `synexia-openrewrite-recipes/recipes/m3jdk21-regex-precompute-20261014`, class
`com.synexia.rewrite.M3Jdk21RegexPrecomputeReceiverRecipe`, in merged Synexia
PR #11408 at head `e8be86e762c9f1187638c3a90eec17f2b47923ee`; its original target
preimage was `d5abed865bd47c4dd5ad78e95704348fe2095ed2`. The private recipe and
raw evidence remain private. Its historical six-file postimage seal is not
silently reused as acceptance for today's evolved runtime.

The original proof used four **TEST_ONLY** storage classes with exact candidate
Java/C, stock Java 21 Pattern and the frozen earlier DP. It recorded 2,205,708
Java and 2,280,860 JNI assertions, including 75,136 required native calls under
UBSan and `-Xcheck:jni`. These overlapping historical counts are not current
product runs or unique-case totals. The interpreter repeat timed out without a
completion receipt; local Maven/SDK did not run in that historical proof.

Historical cold timings regressed for small inputs while warm observed-frontier
reuse improved. The 256-unit native threshold remains a candidate crossover,
not an admitted universal performance result. Unsupported syntax still belongs
to Pattern; no general regex-safety claim follows from this bounded receiver.

Fix the canonical Synexia recipe and rerun it for every correction. The target
receives its executed verifier/build/test/documentation results and passive
provenance only. Preserve OpenJDK per-file licenses; Apache recipe licensing
does not relicense generated target runtime code. Keep product promotion on
HOLD until the independent acceptance gates have real matching receipts.
