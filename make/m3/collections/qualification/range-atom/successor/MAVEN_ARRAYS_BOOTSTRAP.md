<!-- SPDX-License-Identifier: Apache-2.0 -->
# Range/JNI test bootstrap successor: exact hosted failure and repair

The original 2026-10-17 recipe gate and the 2026-11-02 source-custody successor are unchanged and still run before compiled tests. On exact Git HEAD `013cd1860f7883f8484805bd05642225aa549224`, original historic/2026-11-02 admission passed, original OpenRewrite `M3LongRangeSetReceiverRecipeTest` **4/4 green**, and the existing Java `M3LongRangeSetSelfTest` **411913 checks** passed in ordinary Java and under `-Xcheck:jni` with the actual C17 JNI backend.

Three hosted failures after those successes:
- [Native workflow #41449910708](https://github.com/hsoliwal/M3jdk21/actions/runs/41449910708) fails at the strict module compile with `module not found: com.m3.arrays`. It compiled every collections module source without `--module-path` and without building the arrays module.
- [JNI parity #41449910083](https://github.com/hsoliwal/M3jdk21/actions/runs/41449910083) and [range owner #41449909988](https://github.com/hsoliwal/M3jdk21/actions/runs/41449909988) fail at Maven dependency resolution: `com.m3:m3-arrays:jar:0.1.0-SNAPSHOT` was not installed.
- No observed Java behavior or native mask parity failure is inferred from these missing-dependency errors.

Fix: run existing `m3/arrays/pom.xml` with `mvn -B -ntp -f m3/arrays/pom.xml -Dmaven.test.skip=true install` **only as a dependency/bootstrap phase** (m3-arrays has its own full dedicated tests elsewhere). Then run the unchanged M3 collections Maven owner/JUnit tests. The native whole-module `javac` invocation also uses `--module-path m3/arrays/target/m3-arrays-0.1.0-SNAPSHOT.jar`, and checks that the real built JAR declares `com.m3.arrays`. JNI still uses the identical previously qualified C17 owner and `-Xcheck:jni`, not a Java fallback.

No protected runtime Java files, JDK APIs, C sources, source-pins or original recipe templates are modified by this successor. **HOLD:** test outcomes on the successor commit and remaining unrelated custody failures need inspection before merge.
