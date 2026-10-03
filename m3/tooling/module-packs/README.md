# M3 community module packs — executable admission

## Contract and scope

This is an external Java 21 tooling module, not a java.base dependency. It implements
COMMUNITY-1 artifact inventory and supplies COMMUNITY-2/4 image assembly proof fixtures.
No new JVM feature, backport compatibility, external artifact authenticity, performance result,
or cross-platform acceptance follows merely from descriptor inspection.

The read-only inspector consumes a reviewed JAR/JMOD and records its SHA-256, explicit module
descriptor, effective Java 21 class-file ceiling and native entry names. Automatic modules,
preview/newer bytecode, missing runtime dependencies, module collisions and split packages
fail closed. Multi-release JARs are inspected through Java 21's effective view.

The closure validator is a pure graph atom. Standard modules come from the running Java 21
JDK. The image assembler uses that same JDK's jmods/jlink; it does not modify JAVA_HOME.
Input acquisition and license approval remain explicit build-time operations. No network
access or class initialization occurs in the inspector or generated runtime bootstrap.

Atom/IOP roles: M3ModuleArtifact = immutable inventory value; M3ModuleInspector = archive Adapter;
M3ModuleClosure = dependency-closure Validator; M3PackTool = build Facade; the OpenRewrite
inventory recipe = read-only Observation adapter. Native and third-party image launch tests
are separate from Java coverage. Cohesive semantic operations are not arbitrarily fragmented.

## Admission and execution

Maven/JUnit/JaCoCo gates precede distribution acceptance. Tests must cover real JAR/JMOD
fixtures, bytecode boundaries, multi-release selection, graph refusal cases and read-only
OpenRewrite behavior. New Java code has a 99% line and branch gate without coverage exclusions.
The offline JDK harness is additional evidence, never labelled JUnit or a coverage measurement.

`mvn -B -ntp -f m3/tooling/module-packs/pom.xml clean verify`

Artifact acceptance does not prove all reflective dependencies, native ABI compatibility or
application correctness. Actual jlink and application/native launches are mandatory. The first
platform proof is Linux x86_64; other operating systems stay explicitly unverified.

## Existing authorities

See ../../docs/community-capability-admission.md, ../../backports/COMPATIBILITY_ADMISSION.md
and ../../docs/atom-pattern-admission-and-tooling-boundary.md. The complete upstream backport
queue remains active; this work does not reclassify pending JEPs as implemented.
