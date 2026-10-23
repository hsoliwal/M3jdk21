<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Full owning collection Java and JNI qualification

This successor closes the owning Maven dependency/build gap in the preceding
[filtered-split and bounded-mask pass](../filtered-splits-mask-spans-20261018/README.md).
It reuses the current collections, dynamic arrays, offsets, state masks, filtered/sorted
wrappers and native backends; production APIs and implementations remain unchanged here.

The checked-in upstream source closure supplies the intentionally custom JUnit coordinates.
Source verification now resolves the repository root and real provenance fields. API
Guardian packages its exact existing upstream license instead of referring to an absent
root copy. Source hashes, source-set equality and packaged legal checks stay mandatory.
The signal-dimension oracle verifies unavailable and loaded native modes separately.

```sh
JAVA_HOME=/path/to/jdk21 MVN=mvn \
  python3 m3/collections/qualification/full-java-jni-20261018/verify.py
```

Linux, C17 and Maven dependencies/plugins are required. M2_REPO and
M3_MAVEN_OFFLINE=1 support an already populated offline cache; CC selects the C compiler.
The runner builds all nine source dependency modules, compiles all current collection
sources, and builds all nine collection JNI libraries from ten C sources with generated
headers, strict warnings and fatal UBSan. It runs the complete owning Maven verify lifecycle
in isolated Java and checked-JNI modes; native mode requires every backend and zero skipped
tests. Seventeen existing explicit native contracts and missing-library refusal are also
executed. The collection JAR must match the fresh class inventory, contain exact notices
and provenance, and depend only on java.base. A failed rerun cannot retain an old PASS file.

`target/full-java-jni/PROOF.json` records actual counts, hashes, JAR and dependency identities,
local worktree state, and checked-out head. Both owning-suite XML runs are retained. The
existing PR workflow verifies the exact head and covers the nine dependency source paths.
This job remains pending until GitHub actually starts and completes it.

Canonical recipe: `com.synexia.m3.FullJavaJni20261018` in Synexia PR #12311.
See RECEIVER.json for the immutable recipe commit and all exact input/output seals,
EXECUTION.json for the recorded run, and CATALOGUE.tsv for source/candidate decisions.
The previous recipe and its historical receipts remain intact.

This proves only the current independent collection module on the recorded Linux/JDK 21
configuration. It does not promote the global String-first migration, external provider
artifacts or the unexecuted 19,396-template/JAR claim from Synexia PR #8896.
=======
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
>>>>>>> 23acf33fac72fc29ecbf9e252c8e233b893d04f1
