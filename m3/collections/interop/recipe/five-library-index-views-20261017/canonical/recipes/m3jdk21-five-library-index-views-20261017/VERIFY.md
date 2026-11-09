<!-- SPDX-License-Identifier: Apache-2.0 -->
# Verification ladder

1. Verify original source and template output SHA256 from `manifest.tsv`.
2. Maven/OpenRewrite JUnit test for generation, occupation refusal, partial resumption and fixed point.
3. Strict Java 21 compile against **real** five library binaries (interop POM).
4. Run original module's JUnit tests and `M3FiveLibraryIndexViewsTest`.
5. Run mask JNI ABI parity with `-Xcheck:jni`, explicitly reporting absence and failure.
6. Preserve exact-head hosted CI status and benchmark CPU, retained Java heap, native memory.

The isolated local stub test is only a Java syntax/state-algebra smoke test;
it is not proof of actual third-party provider API resolution.
