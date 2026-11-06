<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# m3/native: the DOOP scoped-lease table family (jdk.m3.jni)

Module `org.m3jdk.jni` (package `jdk.m3.jni`), Maven coordinates `com.m3:m3-native:0.1.0-SNAPSHOT`, Java 21,
one dependency inside M3JDK (`jdk.m3.util.M3RuntimeHeap`, m3/ports/collections), optional JNI library
`m3_scoped_leases` (C17, `src/main/native`). The area is `native`; the Java package is `jdk.m3.jni` because
`native` is a Java keyword (recorded exception).

## What it is

| Owner | Role |
|---|---|
| `M3ScopedLeaseTable` | explicit leases for monotonically assigned logical handles over primitive lanes (`ids`, `leases`, `pins`, `jniArrayBorrows`, `jniArrayKinds`, `poolOwners`, `states`): lease/pin lanes, the pool pin lane (`retainPoolReference`, `borrowPoolReference`, `returnPoolReference`, `discardPoolBorrow`, `poolReuseEligible`), the per-ID JNI borrow lane (`registerJniArray`, `beginJniArrayAccess`, `endJniArrayAccess`), dirty state, `tryRetire`, full and windowed candidate scans; grows with `M3RuntimeHeap.chooseAmortizedCapacity` |
| `M3NativeLeaseScan` | Java/JNI parity kernel of the scans (`collect`, `collectWindow`); loads the optional library, validates every JNI result, falls back to the exact Java path |
| `M3LeaseScopes` | allocation-free lexical `withLease` / `withPin` |
| `M3LeaseWindowScan` | bounded resumable eligible-ID scan with a packed cursor |
| `M3JniArrayReferences` | JNI array ownership bridges (`withArray`, `withConcat`, `withCritical`, `withArrayBatch`, `retainSources`/`releaseSources`, `pinGlobal`/`unpinGlobal`) with failure-preserving cleanup |
| `M3JniByteConcat` | materialized `byte[]` concatenation through the managed JNI owner, optional native path with the exact Java fallback; `-Djdk.m3.jni.requireNative=true` forbids a silent fallback |
| `m3_scoped_leases.c` | the three exports `Java_jdk_m3_jni_M3NativeLeaseScan_collectNative`, `..._collectNativeWindow`, `Java_jdk_m3_jni_M3JniByteConcat_concatNative`: bounded `Get/SetArrayRegion` chunks, no retained pointers, globals or off-heap state |

Zero counts are candidates only: the owner must still prove escape, alias, identity and dirty state before
reclaiming anything (the DOOPS residency owner `jdk.m3.collections.M3ObjectResidency` keeps the veto).
Fail-closed native paths stay as in the donor: a native-required run refuses to pass on the Java fallback.

## Donor and relocation (recipe, never by hand)

| Item | Value |
|---|---|
| donor | `hsoliwal/com.synexia` develop `64652588f0d4c23b1f6e0411090b2a796325ae0e`; the family was introduced by `4a2e6452a7a` (2026-10-31, "Merge primitive JNI allocation with complete atom correctness/performance proof"); the parity probe fixture by `f5e453ac791` |
| donor files | `synexia-primitives/src/main/java/com/synexia/primitives/{M3ScopedLeaseTable,M3NativeLeaseScan,M3LeaseScopes,M3LeaseWindowScan,M3JniArrayReferences,M3JniByteConcat}.java`, `synexia-primitives/src/main/native/m3_scoped_leases.c` and `m3-scoped-leases/CMakeLists.txt`, the 13 tests of `synexia-primitives/src/test/java/com/synexia/primitives/`, and the qualifier fixture `M3NativeConcatParityProbe.java` of crate `m3-doop-pool-native-c17-20261031` |
| recipe | `relocate.py` v1 (`RELOCATE_SCOPED_LEASES`): `python -I m3/native/recipe/scoped-leases-20261106/relocate.py --donor-git <clone> --donor-rev <rev> [--check]` |
| map | `RELOCATION_MAP.tsv`: 22 `FILE` rows, `PACKAGE com.synexia.primitives -> jdk.m3.jni`, 20 `TYPE` rows (18 identity, `NativeLease*Oracle -> M3NativeLease*Oracle`), 3 `DEPENDENCY` rows (`M3RuntimeHeap -> jdk.m3.util.M3RuntimeHeap`; `M3CompositeBytes`/`M3ByteSlices -> jdk.internal.m3`, Javadoc mentions only), 1 `EXTERNAL` row (`org.junit.jupiter.api`), 2 `JNI` rows (symbol and header prefixes), 4 `TEXT` rows (property name, prose product name, CMake paths) |
| provenance | `PROVENANCE.tsv`: donor commit, blob sha, donor sha256, target sha256, `body_match`, `body_match_renamed` per file (15 of 22 `body_match=TRUE`, all 22 `body_match_renamed=TRUE`) |
| naming | `python -I m3/tooling/jdk-naming/jdk_naming.py apply --root . --only m3/native/`: 0 moves, 0 rewrites, 0 refusals |

The recipe rewrites only the license header (in the file's comment style), the `package` line, mapped `import`
lines (static imports through the package of their class), the declared type names, the donor package inside
string literals and text blocks, the JNI symbol and header prefixes, the declared literal texts, and adds one
import per `DEPENDENCY` row that a file's code uses; it inserts one `//FLLDCIM: RELOCATE` anchor (`#FLLDCIM:` in
CMake) naming the donor blob URL at the donor commit. Everything else is kept byte for byte. It is idempotent
(second run `changed=0`) and refuses an unmapped package or import instead of guessing the closure.

The dependency on `jdk.internal.m3` is prose only, so the module needs neither `--add-exports
java.base/jdk.internal.m3=org.m3jdk.jni` nor `--patch-module`. `jdk.m3.util.M3RuntimeHeap` is the one M3JDK owner of
amortized capacity choice; its artifact `m3-collection-port-wave1` has no module descriptor yet, so `module-info.java`
requires the automatic module `m3.collection.port.wave1` and the pom turns the `requires-automatic` lint off (and
`missing-explicit-ctor`, because `M3ScopedLeaseTable` keeps the donor's implicit public constructor; the explicit one
belongs to the donor's next revision).

## Build

| Step | Command |
|---|---|
| dependency owner | `mvn -q -B -ntp -f m3/ports/collections/pom.xml -Dmaven.test.skip=true install` (its vendored JUnit family is installed by the `m3-collection-port-wave1` workflow) |
| compile, test, jar | `JAVA_HOME=<JDK 21> mvn -B -ntp -f m3/native/pom.xml package` (`-Xlint:all,-requires-automatic,-missing-explicit-ctor -Werror`, `proc none`; surefire runs the donor's JUnit wrappers and contracts plus `M3ScopedLeaseJavaParityTest` on the classpath; native-only cases are skipped by assumption without the library) |
| from the control reactor | `mvn -f m3/pom.xml -pl native package` (`native` is a default module of `m3/pom.xml`; the reactor does not hold `ports/collections`, install it first) |
| self-tests directly | `java -cp target/classes;target/test-classes;<m3-collection-port-wave1 jar> jdk.m3.jni.M3ScopedLeaseTableSelfTest` (also `M3ScopedLeaseWindowSelfTest`, `M3LeaseScopesSelfTest`, `M3JniArrayBatchSelfTest`; `:` instead of `;` outside Windows) |
| differential | `bash m3/native/recipe/scoped-leases-20261106/differential.sh <donor clone> <donor rev> <scratch dir> [operations]` after `package` |
| native matrix (Linux) | `bash m3/native/recipe/scoped-leases-20261106/native-matrix.sh jdk.m3.jni jdk.m3.jni.requireNative m3/native/src/main/native/m3_scoped_leases.c <scratch> <the six owners> m3/ports/collections/src/main/java/jdk/m3/util/M3RuntimeHeap.java <the four self-tests, the two oracles, the probe>` |
| CMake library | `cmake -S m3/native/src/main/native/scoped-leases -B <build> && cmake --build <build>` (JDK 21 `javac` and JNI headers; or `cmake -S m3/native -B <build> -DM3_SCOPED_LEASES_JNI=ON`) |
| all Java gates | `bash m3/native/recipe/scoped-leases-20261106/verify.sh <donor clone> <donor rev> <scratch dir>` |

Consumers depend on the module with

```xml
<dependency>
  <groupId>com.m3</groupId>
  <artifactId>m3-native</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

and `requires org.m3jdk.jni;` in their module descriptor. The library is optional: `System.loadLibrary("m3_scoped_leases")`
at class initialization, `java.library.path` names its directory.

## Gates (2026-11-06, see GATES.tsv)

| Gate | Result |
|---|---|
| 1 `mvn package`, JDK 21.0.9, `-Xlint:all -Werror` (two lints off, see above) | BUILD SUCCESS |
| 2 tests in the test phase, native=false | 25 tests, 0 failures, 3 native-only skips; `M3_JNI_CONCAT_JAVA_PARITY_PASS cases=1025 native=false`, `M3ScopedLeaseTableSelfTest PASS native=false`, `M3ScopedLeaseWindowSelfTest PASS native=false`, `M3_JNI_ARRAY_BATCH_PASS checks=257302 nativeAvailable=false`, 12 of 12 `M3JniArrayReferencesTest` |
| 3 differential donor vs relocated | identical dumps: 1,025 concat cases + deterministic scan + 7 window caps over 65,536 slots + 100,000 seeded random operations; sha256 `6b7753577f67effc2e6b3c5e024c12ca51df0ea31586bc60a2014d838e19b611` on both sides (7,344,659 bytes, 102,164 lines) |
| 4 native matrix (WSL Ubuntu 24.04, gcc 13.3.0, Temurin 21.0.13) | both families `NATIVE_MATRIX PASS`: `-std=c17 -O2 -Wall -Wextra -Wpedantic -Werror`, 3 symbols, `-Xcheck:jni` default/`-Xint`/C1 identical; relocated digests equal the donor crates' recorded figures (fallback `5b7859c1...`, native `0b0878d3...`, `M3_NATIVE_CONCAT_PARITY_PASS cases=1025 native=true` `29673af8...`); preflight `checks=4050`, code carrier `permutations=6` |
| 4b relocated CMake subproject | configure + build on cmake 3.28.3: `libm3_scoped_leases.so` with the 3 symbols |
| 5 scope | only `m3/native/**` and one `m3/pom.xml` module line |
| recipe fixed point, naming fixed point, module description | `changed=0`; `moves=0 rewrites=0 refusals=0`; `org.m3jdk.jni exports jdk.m3.jni` |

Lessons of the run are in `RECIPE_MEMORY.tsv`.

## Files

| File | Purpose |
|---|---|
| `m3/native/pom.xml`, `m3/native/src/main/java/module-info.java` | the Maven module and the module descriptor (new) |
| `m3/native/src/main/java/jdk/m3/jni/*.java` | the six relocated owners |
| `m3/native/src/main/native/m3_scoped_leases.c`, `m3/native/src/main/native/scoped-leases/CMakeLists.txt` | the relocated JNI source and its CMake project; `m3/native/CMakeLists.txt` gains the option `M3_SCOPED_LEASES_JNI` (default OFF) |
| `m3/native/src/test/java/jdk/m3/jni/M3*{SelfTest,Test,Oracle,Probe}.java` | the 13 relocated tests and the relocated parity probe |
| `m3/native/src/test/java/jdk/m3/jni/M3ScopedLeaseDump.java`, `M3ScopedLeaseJavaParityTest.java` | the reflective differential driver (not a test) and the gate 2 wrapper (new) |
| `recipe/scoped-leases-20261106/relocate.py`, `RELOCATION_MAP.tsv`, `PROVENANCE.tsv` | the relocation recipe, its mapping table and its pins |
| `recipe/scoped-leases-20261106/differential.sh`, `native-matrix.sh`, `verify.sh`, `GATES.tsv`, `RECIPE_MEMORY.tsv` | gates and their record |
