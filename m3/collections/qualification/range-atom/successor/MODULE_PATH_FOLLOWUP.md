<!-- SPDX-License-Identifier: Apache-2.0 -->
# Range/JNI actual module test compilation continuation

On M3 draft head `6b8ccc7fd5a4879f71437c8046bfa10e3150c1f7`, the exact historical receipt passes, 4/4 original OpenRewrite tests pass, and `m3-arrays` builds/installs as a real explicit Java21 `com.m3.arrays` module. The whole `m3-collections` Java module successfully compiles with this module path.

[Hosted JNI run #41452368972](https://github.com/hsoliwal/M3jdk21/actions/runs/41452368972) then fails while compiling two test helper sources; the second `javac -cp proof/classes` invocation cannot resolve the `requires com.m3.arrays;` declaration without the same `--module-path`. The later `javac -h` JNI header invocation had the identical missing module path.

This next additive candidate passes the **same already-built arrays module JAR** to both dependent `javac` invocations. All three compile sites use the same module identity and no source file/JNI C body, original recipe, history or dependency pins are altered. It is a test dependency-closure fix, not a new algorithm. The canonical first workflow bootstrap recipe remains exact for its earlier stage; this successor is an independent additive pass.

Tests remaining: actual Maven `M3LongRangeSetTest,M3LongRangeSetAtomTest`, C17 UBSan + `-Xcheck:jni` native proof, and all wider CI/source ownership. Do not merge until the new hosted head is qualified.
