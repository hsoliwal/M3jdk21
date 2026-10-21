<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->

# M3JDK21 five-source provider relocation — canonical recipe

**Task:** first mechanically rename isolated donor source packages; only then qualify
M3 dynamic arrays, logical offsets, physical indexes, state masks, cached filtering and
sorted wrappers file-by-file. This is a *source-first* recipe for the complete five-provider
programme, **not** 1:1 binary compatibility or proof that all upstream sources have been moved.

## Forks actually found, pinned before work
See `DONOR_PINS.tsv`. Guava, Eclipse Collections and Apache Commons Collections are writable
`hsoliwal/` fork workspaces; M3JDK21 itself is the JDK source fork. There is no confirmed
writable `hsoliwal/spring-framework` fork. For Spring, only the pinned upstream source and
existing `com.m3.collections.interop.M3SpringMultiValueMap` are admissible at this stage.

## Mechanical pass A: package only
The five named OpenRewrite recipes in `src/main/resources/META-INF/rewrite/` use the official
`org.openrewrite.java.ChangePackage`. Four provider recipes recurse through their provider
source trees; the isolated JDK recipe deliberately does NOT recurse into `java.util.concurrent`,
`java.util.function` and other standard library packages.

- Guava `com.google.common.**` -> `com.m3.sdk.guava.**`.
- Eclipse Collections `org.eclipse.collections.**` -> `com.m3.sdk.eclipse.collections.**`.
- Apache Commons Collections `org.apache.commons.collections4.**` -> `com.m3.sdk.apache.commons.collections4.**`.
- Spring collections helpers `org.springframework.util.**` -> `com.m3.sdk.spring.util.**`.
- **Isolated copied JDK sources only** `java.util` -> `com.m3.sdk.jdk.util`;
  do not touch `src/java.base` runtime source or change public JDK packages.

Run within a clean detached worktree of a pinned fork, not on a protected branch;
review any metadata (reflective class names, serialization forms, ServiceLoader, generated
sources, package-private access), update source-paths through the real recipe and compile
against all donor dependencies. The rule **does not** rewrite string literals or Maven
coordinates by accident; those require separately reviewed recipes and explicit lineage.
Keep original copyright, Apache, Eclipse EPL/EDL, or GPL/ClassPath notices in all sources.
Do not pretend that successful source renaming implies complete runtime compatibility.

## Qualification
`mvn -B -f pom.xml test` exercises five Java-LST package transformation fixtures and
fixed input/output examples. Full pinned fork builds, whole-source rewrite output, exact
postimage receipts, module packaging, JNI and installed-image tests are additional required
steps; no such full run is claimed here.

The existing product owner `m3/collections` remains authoritative: avoid copying
a second collection engine. Source changes must be produced by the recipe and reconciled
by semantic atom before moving into `hsoliwal/M3jdk21`. Synexia `develop` remains append-only.
