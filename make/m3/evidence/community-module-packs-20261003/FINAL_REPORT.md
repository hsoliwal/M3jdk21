# Community module-pack execution report — 2026-10-03

## Files changed and exact delta
This packet adds the external `m3/tooling/module-packs` reactor (kernel + OpenRewrite
adapters), actual artifact inventory/graph validation, the diagnostics/JavaFX image
assembler, test fixtures, license notices and documentation. The sole existing-file
edit is one `tooling/module-packs` module entry in `m3/pom.xml`, with exact XML
pre/postimages owned by `M3RegisterModulePacksRecipe`.

`SOURCE_SHA256.tsv` enumerates the 30 source/build/doc files. Evidence files are
separate. Existing JDK sources, standard APIs, master history and installed JAVA_HOME
were not changed. New exported tooling has LIBRARY_API scope; the root reactor
edit has MULTI_MODULE scope. No canonical merge is performed by this packet.

## Exact verification executed
The final source diff and XML/JSON/Python syntax checks preceded the final strict
Java-21 compile (`--release 21 -Xlint:all -Werror`). The standard-Java shared contract
suite passed 41 checks, and the Python guard suite passed 11 unittest tests.

Actual JAR/JMOD fixtures test explicit descriptors, class-version/MR behavior,
signature refusal, checksums, graph dependency closure, static requires, module-name
collisions, split packages, cycles and deterministic output. The offline case runner
is not JUnit; the JUnit entry point calls the same cases but was not executed here.

Two clean diagnostics-pack builds emitted byte-identical original-tool and
diagnostic JMODs (ARTIFACT_SHA256.tsv). jlink produced a 13-module image. The image
executed with -Xcheck:jni, wrote/read a JFR event, and successfully launched the
linked native jcmd against its own VM. This is upstream native/tool integration,
not a custom C/JNI-kernel parity result.

The tested image uses installed Debian OpenJDK 21.0.11, not a build of modified
M3JDK21. MODULES.tsv records the actual JMOD byte fingerprints. Logs and independent
JSON receipts preserve this distinction. Original-source/JMOD notices are included.

## Exact blockers and unfinished obligations
Maven is not installed (`mvn` exit127); Maven Central is unreachable from this
sandbox (`curl` DNS exit6). No Maven/JUnit/OpenRewrite/JaCoCo success is claimed.
99% line/branch coverage remains a configured hard admission gate, unmeasured.

The JavaFX assembler and real toolkit/render fixture exist, but actual JavaFX21
artifacts and reviewed checksums are absent. The example lock deliberately fails.
No JavaFX compilation, native launch or distribution acceptance is claimed.

The broader compatible-backport programme, modified-JDK build/jtreg verification,
Spring/Synexia application packs and cross-platform release proof remain open.
ACTION_QUEUE.tsv lists required exits. This is implementation progress, not total
task or production-release completion.

## Reproduction
From the repository with Java21:
```
python3 m3/tooling/module-packs/verify.py --jdk /path/to/jdk21 --out /fresh/proof
python3 m3/tooling/module-packs/build_pack.py --jdk /path/to/jdk21 --out /fresh/image
mvn -B -ntp -f m3/tooling/module-packs/pom.xml clean verify
```
The first two commands were executed here against the recorded JDK. The Maven
command is the unexecuted admission gate, not a claimed result. JavaFX requires
a reviewed real lock and display/Xvfb. Outputs must not already exist or be inside
the JDK. Rerun module inventory at the same inputs to verify the fixed point.

## Artifact paths
This directory contains all required M3 output-contract files plus source and
runtime manifests and raw logs. No upstream runtime binary is committed.
