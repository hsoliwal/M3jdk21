# Optional complete OpenRewrite framework bundle

The `openrewrite` tool pack adds the complete published production framework
from **OpenRewrite 8.17.1** to a selected M3JDK21 image. It includes its language
modules, Java parser adapters, framework recipe discovery, the complete resolved
runtime dependency graph, the existing M3 shared-compiler frontend, and the
`m3-rewrite` Java recipe command. Ordinary JDK images and compiler invocations
do not acquire this bundle implicitly.

The standalone `openrewrite-tool/pom.xml` and its README describe the smaller
Java-focused runner dependency graph. **This selectable bundle expands that
graph to all 15 production framework artifacts listed below**, using the
separate `openrewrite-runtime/pom.xml`. The bundle's scope is pinned to the
upstream repository and release; it does not claim to include every third-party
recipe repository, every programming language, or proprietary recipe catalogs.
Additional compatible recipe/plugin libraries remain explicit extensions.

## Upstream membership and runtime layout

Production module membership comes from
[the upstream settings file at commit 3bd8271be963c025ce0856072d0f09360e3184cd](https://github.com/openrewrite/rewrite/blob/3bd8271be963c025ce0856072d0f09360e3184cd/settings.gradle.kts),
the immutable commit selected by tag `v8.17.1`. The Git blob for that settings
file is `5ae46344dd79723f0965b756dcac141e30f124ee`.

| Framework area | Original artifacts retained |
| --- | --- |
| Core recipe/runtime APIs | `rewrite-core` |
| Java and parser adapters | `rewrite-java`, `rewrite-java-8`, `rewrite-java-11`, `rewrite-java-17`, `rewrite-java-21` |
| Build files and Groovy | `rewrite-gradle`, `rewrite-groovy`, `rewrite-maven` |
| Configuration and data languages | `rewrite-hcl`, `rewrite-json`, `rewrite-properties`, `rewrite-protobuf`, `rewrite-xml`, `rewrite-yaml` |

The upstream `rewrite-bom` is retained as POM metadata. Benchmark, test, test
compatibility, and build-only modules are excluded from the runtime. This
pinned release's production settings do not list Kotlin; no Kotlin runtime is
invented or silently substituted. The qualified aggregate resolves **57
original runtime dependency JARs**, including the M3 frontend, plus the
separately built M3 runner JAR. Transitive dependencies and upstream embedded
parser resources remain intact.

Every JAR is copied without modification into
`lib/m3-tools/openrewrite/lib/<Maven repository path>`. Original dependency and
parent POMs, the framework BOM, source license files and supplied source/build
receipts are retained alongside them. `RUNTIME_INVENTORY.json` records original
coordinates, file SHA-256 values, embedded legal-entry hashes, exact classpath,
source membership pins, and the required compiler exports.

The pack adds a small **manifest-only** `launch.jar` containing no replacement
framework classes. Its `Class-Path` names the original JARs with relative URLs;
its `Main-Class` is the real `org.m3jdk.tooling.rewrite.M3RewriteTool`. This keeps
both shell launchers short, preserves the caller's working directory, and
allows the image to move. The POSIX launcher has been executed with paths
containing spaces. A `.cmd` launcher is also generated; Windows execution is a
separate platform gate.

The framework remains on the classpath. Some upstream adapters have numeric
Java-version suffixes that cannot be automatic module names, and upstream
Gradle parser resources include classes in the unnamed package. These original
JARs are not relocated, filtered, or misrepresented as JMODs. Real JDK modules
are selected through the existing module-pack graph and lock API.

## Explicit selection and independent build tools

Create an image with the OpenRewrite bundle using the existing selector:

```sh
python3 m3/tooling/module-packs/build_pack.py \
  --jdk /path/to/source-built-m3jdk21 \
  --out /path/to/new-selected-image-output \
  --profile diagnostics \
  --tool-pack-lock /path/to/openrewrite-lock.json \
  --tool-pack openrewrite
```

The image is written beneath the new output directory's `image/` child.
Supplying no tool selection keeps optional bundles absent. A lock alone does
not enable its bundles. Maven, Gradle, OpenRewrite and additional plugin packs
remain independent selections; a combined lock can contain all reviewed pack
entries and the caller can repeat `--tool-pack` for the desired names. Selecting
OpenRewrite's Maven/Gradle **framework APIs** does not enable the separate Maven
or Gradle **build-tool distributions**.

The OpenRewrite lock requests `java.se`, `jdk.compiler`, `jdk.management`,
`jdk.unsupported`, `jdk.crypto.ec` and `jdk.zipfs`. `java.se` deliberately retains
the standard Java platform for the full framework. The other roots cover the
compiler, runtime management/introspection, compatibility support and explicit
crypto/ZIP filesystem providers. Static `jdeps` analysis and actual selected
image execution qualify the supplied framework. A custom recipe that requires
additional JDK modules, native libraries or external services must supply those
requirements explicitly.

## Java command and non-Java framework APIs

The image's `bin/m3-rewrite` accepts qualified recipe names, YAML configuration
and caller-supplied recipe JARs. See
[the Java runner usage](../../shared-frontend/openrewrite-tool/README.md) for
complete command syntax. For example:

```sh
image/bin/m3-rewrite \
  --source-root /path/to/project/src/main/java \
  --source example/Api.java --source example/Use.java \
  --recipe example.RenameApi --recipe-config /path/to/rewrite.yml \
  --patch /path/to/new-review.patch \
  -- --release 21 -proc:none
```

The Java command remains narrowly defined: one lossless, by-file M3 javac
compilation, one direct OpenRewrite conversion per explicit input unit, recipes
loaded/run only after successful compilation and complete LST coverage, and a
review patch with unchanged original source files. It never falls back to
reparsing or re-attributing the input compilation units. Arbitrary recipes can
use their ordinary synthetic-template parsers. The retained Java 8/11/17
adapters are upstream runtime artifacts; the shared compiler conversion uses
the Java 21 adapter appropriate for M3JDK21.

Non-Java parser and recipe APIs are present and executable through the original
OpenRewrite framework. The runtime tests exercise lossless Gradle, Groovy,
Maven, HCL, JSON, properties, protobuf, XML and YAML parsing, cross-language
recipe discovery, and a real properties recipe. **The `m3-rewrite` command's
input options still accept explicit Java sources.** It does not automatically
discover or transform a project's POM, Gradle, YAML or other non-Java files.
Applications or compatible recipe plugins can use the bundled framework APIs
for those inputs; project model/marker requirements remain those of upstream
OpenRewrite.

## Build the source-owned runtime and create its lock

Use the source-built M3JDK21 image as `JAVA_HOME`. Build/install the existing
frontend first, then build the Java runner and the complete runtime aggregator:

```sh
mvn -f m3/tooling/shared-frontend/openrewrite/pom.xml install
mvn -f m3/tooling/shared-frontend/openrewrite-tool/pom.xml package
mvn -f m3/tooling/shared-frontend/openrewrite-runtime/pom.xml package
```

The last command is a POM-only runtime aggregator with real, explicitly bound
Java test compilation and JUnit execution. Its test JVM places framework caches
under the proof output directory. It emits Maven's `runtime-classpath.txt` and
`runtime-dependencies.txt`; these contain runtime dependencies, excluding the
JUnit test graph. `-Dm3.proof.directory=/absolute/new-output` redirects all
outputs for recipe qualification without adding generated files to source
candidates.

Run the pack assembler against those exact outputs:

```sh
python3 m3/tooling/module-packs/build-tools/openrewrite_pack.py \
  --project-pom m3/tooling/shared-frontend/openrewrite-tool/pom.xml \
  --frontend-pom m3/tooling/shared-frontend/openrewrite/pom.xml \
  --runtime-pom m3/tooling/shared-frontend/openrewrite-runtime/pom.xml \
  --runner-jar m3/tooling/shared-frontend/openrewrite-tool/target/m3-openrewrite-tool-1.0-SNAPSHOT.jar \
  --runtime-classpath m3/tooling/shared-frontend/openrewrite-runtime/target/runtime-classpath.txt \
  --runtime-dependencies m3/tooling/shared-frontend/openrewrite-runtime/target/runtime-dependencies.txt \
  --maven-repository /path/to/the/resolved/maven-repository \
  --distribution /path/to/new-openrewrite-distribution \
  --out /path/to/new-openrewrite-lock.json \
  --source 'Reviewed M3 source/recipe receipts; OpenRewrite 3bd8271be963c025ce0856072d0f09360e3184cd' \
  --source-license LICENSE \
  --source-license ADDITIONAL_LICENSE_INFO \
  --source-license ASSEMBLY_EXCEPTION
```

Add repeated `--source-proof PATH` options to retain canonical recipe and build
receipt bytes. Input files must be explicit regular files. Distribution and
lock paths must be new and outside the Maven input repository. A failed
assembly may leave an incomplete work directory, but it does not publish a
usable lock. Use a fresh output directory for the next attempt.

The assembler checks complete upstream framework membership, agreement between
actual Maven classpath and coordinate lists, artifact/POM identity, fixed
framework versions and compiler export agreement. It seals source inputs,
JARs and parent POMs across admission and copying, refuses unsafe coordinate
components, and validates the resulting lock with the existing `tool_packs`
owner. Original embedded licenses and notices remain inside their original
JARs, with hashes in the inventory. Packaging itself records no claim of recipe
execution or shared AST consumption; those claims require the separate runtime
proofs.

## Reproduce the actual pack contracts

After source recipe emission and both Maven builds, set these task-scoped test
inputs and run the Python contract suite:

```sh
export M3_TEST_REPOSITORY=/path/to/M3jdk21
export M3_TEST_JDK=/path/to/source-built-m3jdk21
export M3_TEST_MAVEN_REPOSITORY=/path/to/the/resolved/maven-repository
export M3_REWRITE_TOOL_PROOF=/path/to/qualified-runner-output
export M3_REWRITE_RUNTIME_PROOF=/path/to/qualified-full-runtime-output
export M3_REWRITE_TOOL_POM=/path/to/emitted/openrewrite-tool/pom.xml
export M3_REWRITE_RUNTIME_POM=/path/to/emitted/openrewrite-runtime/pom.xml
python3 m3/tooling/module-packs/build-tools/test_openrewrite_pack.py
```

The suite uses the real resolved framework and source-built runner, including
the runner's qualification receipt. It verifies every retained JAR and legal
entry, actual manifest recipe execution over typed Java files, exact asset
installation, explicit selection and the refusal of missing framework modules,
coordinate/export drift, metadata traversal, overwrite and source changes
during assembly. The selected-image gate then runs the actual launcher and
framework tests on the image produced by `build_pack.py`.
