# Developer module pack and password-backport execution

Status: Linux candidate. Local runtime results are separate from full M3JDK21 admission.

The existing module inspector, dependency graph and diagnostics assembler remain the owners.
`build_pack.py --profile developer` adds selected standard JDK development modules; the default
`diagnostics` selection and reviewed JavaFX-lock path remain available. Extra receipt fields
`profile` and `developer` describe the new selection. No Java SE or native ABI changes are made.

## Deliverables and responsibility

- `image/`: selected developer/diagnostics runtime, built by the supplied Java21 jlink.
- `link-inputs/`: matching JDK JMODs copied as a separate link kit with before/copy/after checksums.
  A linked image alone is not assumed to carry another complete set of link inputs.
- `developer-proof/`: documented fixture source, jar/jmod, second linked application, and actual
  jpackage native app-image launcher. Fixture material is not a Spring/Synexia application pack.
- Tool probes execute java, javac, jar, javap, jdeps, javadoc, jmod, jlink, jshell, keytool and
  jpackage from the selected image. Javadoc uses doclint and warnings as errors.
- The disposable keytool PKCS12 fixture is not a production key. Do not publish that private-key
  file or upstream runtime binaries as original M3 artifacts.

Inputs remain read-only; occupied output or output inside JAVA_HOME is refused. Developer native
launcher proof is explicitly Linux-only. No downloads occur during assembly. License/update
obligations for copied upstream components remain with those components.

## Exact backport proof

`verify_backport.py` reuses the checked-in JDK-8368692 `Password.java`, not a rewritten password
algorithm. It verifies its source checksum, stages only that file and the independent test helper,
then compiles a temporary java.base patch with the explicitly supplied Java21 system modules.
The supplied JDK is never overwritten. Changed source requires an explicitly reviewed new proof pin.

Fourteen property scenarios run in fresh normal-JIT and interpreter VMs, with `-Xcheck:jni`:
default-allow, system/security properties, case handling, precedence, malformed values, cached
initialization, caller ownership, echo/unrelated-stream behavior, nulls and buffer boundaries.
Console/TTY behavior, jtreg and a rebuilt modified JDK are not implied by this patch experiment.
Ambient JAVA_TOOL_OPTIONS, JDK_JAVA_OPTIONS and _JAVA_OPTIONS are refused.

## Reusable recipe and Maven

`com.m3.rewrite.packs.JdkExecutionFollowthrough` composes the existing hash-pinned Java and text
OpenRewrite recipes. Java test materialization uses the Java parser; Python/XML/Markdown replay
uses the text engine. Exact preimages, absent-before additions, out-of-target preservation,
source drift and a second-run fixed point are tested by `M3JdkExecutionFollowthroughRecipeTest`.
Source-seal checks do not establish general behavior equivalence or measured coverage.

```
python3 -S m3/tooling/module-packs/build_pack.py --jdk /path/to/jdk21 \
  --profile developer --out /fresh/developer
python3 -S m3/tooling/module-packs/verify_backport.py --jdk /path/to/jdk21 \
  --out /fresh/password
mvn -o -B -ntp -f m3/tooling/module-packs/verification/jdk-execution/pom.xml clean verify
```

The Maven aggregator includes the existing migration-recipe and module-pack reactors and their
JUnit/JaCoCo gates without changing thresholds or exclusions. It orchestrates the same standalone
commands; they are not a replacement for those framework gates. Linux, Python3, JDK21 and an
approved populated offline Maven dependency repository are prerequisites.

No repository-wide completion, 99% coverage, cross-platform runtime certification, native speedup,
JavaFX admission or full compatible-backport denominator closure follows merely from this pack.
