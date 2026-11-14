<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# File workflow dependency and contract gates

The existing `org.m3jdk.file` module requires `jdk.m3.collections`, and that
module requires `jdk.m3.arrays`. Compile both complete dependency modules before
running the file contracts. A source subset that happens to satisfy a few
imports does not establish a complete exported module for the host or `jlink`.

From the repository root, with a Java 21 JDK and a new output path:

```sh
bash -n m3/file/compile-dependencies.sh
python3 -m py_compile m3/file/verify.py m3/file/test_workflow_wiring.py
python3 -m unittest discover -s m3/file -p test_workflow_wiring.py -v
bash m3/file/compile-dependencies.sh "$JAVA_HOME" /tmp/m3-file-dependencies
python3 m3/file/verify.py --java-home "$JAVA_HOME" \
  --module-path /tmp/m3-file-dependencies --output /tmp/m3-file-contracts
```

The dependency builder enumerates complete main source trees in deterministic
order, retains their module descriptors and compiles with `--release 21`, strict
lint and `-Werror`. It refuses an existing output path to avoid stale class files.
It writes no source files. The move, original-shadow and layered-shadow workflows
use these same modules and run when either dependency source tree changes.
Each workflow first lints and compiles the Python wiring gate and then executes
its tests. The separate Java gate compiles all module contracts before their
runtime executions.

The file verifier compiles the independent and module contracts before starting
the host contracts. It preserves the existing source-role classification:

| Role | Current source count | Execution |
| --- | ---: | --- |
| Shared standalone contracts | 18 | Host and linked image |
| Host compiler contract | 1 | Host with `javac` available |
| Bounded workspace probe | 1 | Two arguments on host and linked image, `-Xmx48m` |
| Maven-owned tests and wrapped contracts | 13 | Separate Maven/JUnit gate |
| Donor fixtures without an entry point | 12 | Compiled by the Maven test gate |

The exact 45-source role inventory remains fail closed. The original-shadow
receipt consumer also checks both bounded-probe invocations, the current
change-map proof, duplicate JSON keys, success markers and the separate Maven
gate. The layered-shadow workflow keeps its normal and interpreted host and
linked-image executions of the existing `M3ShadowStreamsContract`.

`test_workflow_wiring.py` checks these consumers against the actual descriptors,
verifier declarations and source paths. Its positive and negative receipt
fixtures qualify the receipt mechanism; they do not replace execution of the
real Java contracts. Java/JNI source, public signatures, module exports and
historical qualification receipts are unchanged by this build repair.

Reusable transformation recipes, source seals and fixed-point qualification
belong in `hsoliwal/com.synexia`. This target receives executed build/test/doc
postimages and passive provenance. A successful bounded file contract run does
not qualify a full M3JDK bootstrap, a native JNI platform, all Maven tests or a
performance claim, and does not advance the ordered String receiving phase.
