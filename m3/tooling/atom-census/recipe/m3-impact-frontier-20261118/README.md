<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 semantic impact frontier receiving crate

**Status:** additive candidate / source custody passed / hosted Maven + OpenRewrite not yet observed.

Canonical generating recipe: `com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe`,
instantiated with crate `m3jdk-impact-frontier-20261118` in `hsoliwal/com.synexia`.
The three Java target paths in `manifest.tsv` have exact `ABSENT` preimages and
the committed reviewable Java sources are byte-identical to the three templates.

This is **data-only receiving evidence** in M3JDK21. The public JDK does not import
OpenRewrite runtime classes or Synexia recipe-correction memory. The recipe owner
must run its own JUnit fixed-point/drift/refusal tests against the same source pins.
If a generated target is wrong, change the Synexia recipe/template and regenerate
the JDK postimage. Never amend the generated target without a new recipe revision.

The first target builds a read-only impact frontier using the existing atom coverage
ledger, array/offset-backed reverse-graph traversal and a fixed work queue. The tests
prove deterministic file-level downstream closure, JNI source relation propagation,
cycle handling, unknown reference refusal and blocker preservation. They do not prove
an assembled OpenJDK product or native ABI qualification.

Verify target Java with:
```sh
bash m3/tooling/atom-census/verify-contract.sh
mvn -B -ntp -f m3/tooling/atom-census/pom.xml -Dtest=M3ImpactFrontierContractTest test
```

Verification priority: diff -> lint -> Java 21 compile -> corpus/JUnit -> full JDK,
JNI/ABI when changed -> fixed point and performance only under measured workloads.
