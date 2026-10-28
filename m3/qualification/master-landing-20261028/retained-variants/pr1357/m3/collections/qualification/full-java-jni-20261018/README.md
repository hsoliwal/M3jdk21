<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Owning collection Java and JNI qualification

This verification driver is reused from M3JDK21 PR #1303 at
`e70058e73e4f42036815a24c2c22dac9dd948074`. Its canonical origin is Synexia
PR #12311. Current source identities, donor decisions, execution logs and limits
are recorded in [the convergence packet](../convergence-20261018/README.md).
The convergence pass adds the source-built platform launcher required by Surefire.
Historical results from the donor are not treated as current results.

Run with Java 21, Maven, Python 3, Linux and a C17 compiler:

```sh
JAVA_HOME=/path/to/jdk21 MVN=mvn python3 m3/collections/qualification/full-java-jni-20261018/verify.py
```

`M2_REPO` selects a dependency cache and `M3_MAVEN_OFFLINE=1` requests offline
Maven execution. The runner verifies and builds ten source dependency modules,
compiles all current collection Java sources, generates JNI headers, and builds
all nine existing native libraries from ten C owners with strict warnings and
fatal UBSan. It executes the owning Maven tests in Java-only and required-native
modes, additional native parity and missing-library contracts, and checks the
packaged class inventory, notices and java.base-only runtime dependency.

A successful run writes `m3/collections/target/full-java-jni/PROOF.json` and retains
both JUnit XML report sets. No whole-JDK or non-Linux acceptance is inferred.
The local Apache license and notice are retained as LICENSE.txt and NOTICE.txt.
