# Jackson ObjectMapper in an M3 JMOD pack

The pack builds the existing Jackson 2.19.2 source modules and the small
`jdk.m3.jackson` integration module, then runs the same executable contracts on
the selected JDK and a linked image. Upstream `com.fasterxml.jackson.*` packages,
ObjectMapper behavior, service declarations and module names remain available.

## Source and module ownership

The runtime inputs are the existing in-repository source inclusions of Jackson
annotations/core/databind 2.19.2 and FastDoubleParser 1.0.90. `SOURCE_LOCK.json`
pins their complete source/build/legal trees. Their exact Maven POMs perform the
source builds, FastDoubleParser shading and multi-release compilation in fresh
staging directories. No upstream source or POM is patched by this build.

The existing `m3/tooling/jmod-recipes/tools/jmod_pack.py` owns JMOD assembly and
release-21 flattening. Its existing module graph checks the selected closure and
split packages. The linked image explicitly includes `java.se`, retaining the
JDK modules that Jackson's descriptor marks as optional. Four JMODs are produced:

- `com.fasterxml.jackson.annotation.jmod`
- `com.fasterxml.jackson.core.jmod`
- `com.fasterxml.jackson.databind.jmod`
- `jdk.m3.jackson.jmod`

FastDoubleParser is built from source and shaded by Jackson core's original
build configuration; it is not installed as a competing runtime module.

## Build and execute

Use a complete JDK 21, Maven 3.9 or later, Python 3.10 or later, and the repository
checkout with all four source directories materialized. A partial clone must
materialize the needed donor trees before an offline build.

First populate the Maven build-plugin cache if necessary:

```sh
python3 m3/tooling/module-packs/jackson/build.py \
  --jdk-home /path/to/jdk21 --maven /path/to/maven/bin/mvn \
  --maven-repo /path/to/maven-cache --resolve-build-tools \
  --out /new/jackson-proof
```

The default is offline. Repeat with a new output directory and the same Maven
cache, omitting `--resolve-build-tools`, to prove that dependency resolution no
longer needs the network. Runtime libraries are source-built in either mode;
network access in the first mode is for Maven build plugins and their tooling
dependencies. `--maven-settings` and `--maven-global-settings` can select explicit
task settings without changing global Maven configuration.

The standalone Maven entry point is:

```sh
mvn -B -ntp -f m3/tooling/module-packs/jackson/pom.xml verify \
  -Dm3.jackson.out=/new/jackson-proof \
  -Dm3.jackson.maven.repo=/path/to/maven-cache
```

The output directory must be absent. Existing proof directories are retained
and refused; no host JDK installation or user file is overwritten. The pack is
an optional separately licensed module and does not advance the JDK-wide
String → arrays → collections → AST/compiler receiving sequence.

## Executed contracts and remaining runtime gates

`JacksonContract` and `M3FileContract` run first from named source-built modules,
then from the linked image. Test classes are patched into `jdk.m3.jackson` only
for execution. Their private fixture POJOs use an explicit test-only qualified
opening to `com.fasterxml.jackson.databind`. An application's own named model
module must similarly export public members or open its model package for
Jackson reflection; this pack does not bypass JPMS access rules.

`RESULT.json`, `SOURCE_RECEIPT.json`, `SOURCE_BUILD.json`, the existing assembler
receipts and command logs identify the exact inputs and executed lanes. A stock
JDK run records the M3String storage flag as unavailable. A selected M3 runtime
that accepts `-XX:+UseM3StringStorage` additionally runs host/image contracts with
the flag enabled and disabled. `--require-m3-runtime` refuses completion without
that separate lane. Passing this bounded corpus does not claim the full Jackson
upstream suite, full JDK/jtreg coverage, all platforms, or a performance gain.

## Optional M3AST compiler module

`jdk.m3.jackson.compiler` supplies the integration with the existing public
`com.sun.source.util.M3AST` compiler API. Its dependency on `jdk.compiler` stays
in that separate optional module. The four-module runtime pack above can be used
without the compiler bridge.

After the core build succeeds, the compiler qualification entry point is:

```sh
python3 m3/tooling/module-packs/jackson/compiler-build.py \
  --jdk-home /path/to/jdk21 --repo-root /path/to/M3jdk21 \
  --jars /new/jackson-proof/jars --out /new/jackson-compiler-proof
```

The harness compiles the actual four-source M3AST/compiler owner closure from
the selected repository and records every source hash. It compiles the bridge
against that API, packages a fifth JMOD with the existing assembler, and exercises
real compiler callbacks on the host and the five-module linked image. The
compiler patch is supplied explicitly through `--patch-module jdk.compiler`
during qualification and remains outside the bridge JMOD. Its original source
headers and GPLv2-with-Classpath-Exception ownership remain intact.

This bounded compiler harness is distinct from a full M3JDK VM build. Use a
qualified M3JDK compiler for production; the harness records its limited scope
in `COMPILER_RECEIPT.json` and does not establish the M3String VM storage lane.
The fifth-module pack reuses the complete legal bundle from the core build.
