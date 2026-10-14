# JDK-8357439 — deterministic jcmd Bash-completion proof

This is a target-side verification packet for the already materialized OpenJDK jcmd
completion backport (`openjdk/jdk@8549d1896054dd230ba3038c83bce23b10dcda22`).
It does **not** change the upstream Bash script, its Git blob, JDK public APIs, HotSpot,
or JNI/JVMTI behavior.

The Bash smoke harness mocks the jcmd CLI. It proves PID/main-class completion,
self-process exclusion, deduplication, generic options, JVM-specific command
completion, `jcmd <pid> help` filtering, `-f` filename completion, unsupported
positions, and empty JVM/command listings. The test is independent of running
Java processes, attach permissions, or network access.

Run from the M3JDK21 repository root:

```sh
bash -n src/jdk.jcmd/share/conf/bash-completion/jcmd
bash -n m3/backports/recipes/jdk-8357439/test_jcmd_completion.sh
bash m3/backports/recipes/jdk-8357439/test_jcmd_completion.sh
```

Canonical source-changing recipe: `com.synexia.rewrite.M3Jdk8357439CompletionProofRecipe`
in `hsoliwal/com.synexia`. Three target additions are SHA-256-sealed and must
reach an exact second-pass fixed point. This packet only supplies deterministic
behavior/packaging evidence; a full M3JDK21 image build, image-copy proof,
recipe JUnit, and applicable jtreg remain separately required for promotion.

The original `m3/backports/recipes/jdk-8357439/manifest.tsv` continues to pin
the exact upstream implementation blobs. Do not rewrite that historical receipt
merely because additional verification has been added.
