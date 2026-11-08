<!-- SPDX-FileCopyrightText: 2026 Synexia contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK JDK21 opt-in profile-guided CDS training proof

Contract `M3JDK21-CDS-TRAIN-REPLAY/1`; profile: opt-in, standalone JDK tooling.

This is a concrete JDK21 implementation of a **training -> archived state -> deployment** pattern
related to Leyden/Excelsior-style preparation. It **reuses** standard HotSpot 21 AppCDS machinery
(`-XX:ArchiveClassesAtExit` and `-XX:SharedArchiveFile`), not vendor runtime source or proprietary
code. Do not treat it as a port of Excelsior JET, Leyden AOT caches, or compiled-code AOT.

## Frozen boundary

- No edits to `java.base`, standard Java/JNI APIs, class files, HotSpot, or existing tools.
- The command is explicitly opt-in. An unchanged normal JDK21 launch remains unchanged.
- Input: one exact executable JDK21 `java`, one ordinary regular JAR file, one explicit main
  class, an output directory **which must not exist**, and finite argument strings.
- Training runs do not inherit `JAVA_TOOL_OPTIONS`, `_JAVA_OPTIONS` or `JDK_JAVA_OPTIONS`.
- Native OS argument encoding is sealed as an input; characters not representable by that
  charset refuse admission. The Linux fixture explicitly executes under `LC_ALL=C.UTF-8`;
  production launches inherit their configured locale and do not silently change it.
- `ProcessBuilder` constructs each invocation without a shell. Stdin is closed; interactive
  or nondeterministic applications are not admitted by this bounded proof.
- The tool takes exact SHA-256 identities of the JDK launcher, `release`, `lib/modules`, VM shared
  library where present, the input JAR, its exact size and modification timestamp, and
  canonical argument vector. JDK21 CDS also validates the JAR timestamp, so SHA-256 equality
  by itself does not establish replay eligibility. The tool verifies all inputs before
  replay, and refuses modifications.
- The tool executes baseline `-Xshare:off`, training `-XX:ArchiveClassesAtExit`, then deployment
  `-Xshare:on -XX:SharedArchiveFile` in that order, with bounded process timeouts.
- Each stage must exit zero. Byte-exact stdout AND stderr parity are required. A class-load
  log must demonstrate that the application's named main class came from `shared objects file
  (top)`; `-Xshare:on` alone is insufficient evidence of using the dynamic application archive.
- The produced archive, receipt and original stage logs are retained. Repeated `verify` may
  run the deployment again but never changes the immutable training receipt or source JAR.
- Archive bytes need **not** be reproducible between independent trainings. A deterministic
  input plan and immutable per-training archive SHA-256 are the checkable identities.
- A positive fixture proves only that the selected JDK21 fixture replays under AppCDS.
  It does **not** prove target M3JDK21, JNI ABI, class loader state, hidden exceptions,
  callback ordering, side effects, performance improvement, or all possible application behavior.

## CLI

```text
java -cp <tool-classes> jdk.m3.tooling.cds.CdsTrainReplay train <jdk-java> <application.jar> <MainClass> <new-output-dir> [app-args...]
java -cp <tool-classes> jdk.m3.tooling.cds.CdsTrainReplay verify <existing-output-dir>
```

A **PASS** receipt binds an exact source/toolchain/archive and *bounded fixture output parity*.
Admission to a source-built M3JDK target additionally requires: target JDK21 image, jlink/CDS
checks, JIT/interpreter/C1/C2, jtreg, cross-platform and JVM/GC/native acceptance. All such
product gates remain **NOT_RUN** for this candidate until real execution receipts exist.

## Donor provenance

- OpenJDK 21 AppCDS options: `openjdk/jdk21u`; GPLv2 with Classpath exception, **API
  invocation only**, no source copied.
- Project Leyden training-to-deployment design: conceptual reference only, not copied.
- Excelsior JET incremental AOT/JIT preparation: architecture reference only, proprietary
  implementation not accessed or copied.
- OpenJ9, BiSheng, Corretto and Graal: future runtime/optimization review candidates only.

## Ordered proof

`inventory -> documentation -> SHA sealing -> lint -> javac -> executable negative/positive
fixture -> javadoc -> fixed-point receipt check -> Maven/OpenRewrite SDK -> target JDK21`.

If any stage is missing, mark it `NOT_RUN`, not `PASS`.
