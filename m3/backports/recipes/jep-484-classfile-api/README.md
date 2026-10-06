# JEP 484 Class-File API cumulative compatibility inventory

Status: **read-only inventory / opt-in planning only**. No JDK product source is changed by this packet.

## Why the final JEP commit is not a standalone backport

JDK 21 already contains a substantial internal class-file engine under
`jdk.internal.classfile`. The first preview (JEP 457) publicized and renamed that machinery into
`java.lang.classfile`; the second preview (JEP 466) continued API evolution; JEP 484 finalized the
public API in JDK 24.

Therefore the final JEP 484 commit cannot be evaluated in isolation.

Pinned milestones:

- Java21 internal baseline: M3JDK21 / OpenJDK21 commit
  `890adb6410dab4606a4f26a942aed02fb2f55387`;
- JEP 457 transition to preview:
  `2b00ac0d02a110326846c75ea7ea535dccbb1924`;
- JEP 466 transition to second preview:
  `19a99d023e32fa9f4d26b76bd36993719e1dfe21`;
- JEP 484 finalization:
  `84ffb64cd73f8af11cf3670c6f19d282c2ac6961`;
- composition donor state: `jdk-24+36`.

## Mechanical denominator

Java source-tree census:

- JDK21 internal engine: **241** Java files;
- JDK21 public `java.lang.classfile`: **0**;
- JDK24 final public API: **161** Java files;
- JDK24 retained internal engine: **84** Java files;
- JDK24 public + internal: **245** Java files.

After normalizing the historical `Classfile*` spelling to final `ClassFile*`, **238 / 241**
JDK21 internal paths have direct JDK24 descendants. Three direct paths disappear and seven are new.

The public classfile subtree has **59 commits** between the first-preview transition and finalization.
JEP 466's transition commit changes only preview-stage metadata, so cumulative history is mandatory
evidence.

## Java21 identity policy

JEP 484 is a large final Java SE public API addition. It is **not** silently enabled in default
M3JDK21.

Canonical policy:

- default Java21: **NO**;
- opt-in extension: **YES**;
- scope: **LIBRARY_API**;
- contract: **EXPLICIT_CONTRACT_CHANGE**;
- promotion authority from this inventory: **false**.

Any later opt-in materialization must keep Java21 runtime/class-file defaults truthful. In
particular, copied JDK24 API/runtime code must not silently change the default emitted class-file
version away from Java21 (major version 65), nor imply that the default runtime is Java24.

## Next pass

Before source-changing work:

1. inventory all 59 public-subtree commits plus retained internal/downstream-tool migrations;
2. build an exact JDK21-internal -> JDK24-public/internal path map;
3. classify JDK24 APIs/options/constants that encode post-21 class-file versions;
4. define the Java21 default-version adaptation;
5. generate bounded source-sealed recipe crates;
6. materialize only in disposable CI;
7. build OpenJDK and run Class-File API/javap/jlink/JFR regressions.

This packet grants no mutation, compatibility, semantic-equivalence, or promotion authority.
