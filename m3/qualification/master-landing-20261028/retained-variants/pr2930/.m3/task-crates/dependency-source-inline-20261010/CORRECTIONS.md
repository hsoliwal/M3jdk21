# Corrections after the equivalence gate (2026-10-11)

The first push switched tooling pins on a compile-only gate. An API-surface and resource equivalence gate
(`API_SURFACE_RUNNER.tsv`: ASM comparison of each source-built jar with the published binary jar; classes,
members, signatures, constants, annotations, and resources by SHA-256) found that the slf4j-api 1.7.36 build
packages the three `org.slf4j.impl` stub binders (StaticLoggerBinder, StaticMDCBinder, StaticMarkerBinder) that
upstream deletes before packaging. With those stubs on the classpath slf4j 1.7 binds to a placeholder that throws.

This commit reverts the six slf4j-api 1.7.36 pins (five jcc verification poms and
`m3/synexia-import/pure-int-recipe-custody`) to the published 1.7.36. The other switched pins keep their
vendored coordinates: jetbrains annotations 24.1.0, slf4j-api 2.0.9 and slf4j-simple 2.0.13 are IDENTICAL to
the published jars.

Next commit on this branch: the upstream packaging rule for slf4j 1.7 (jar excludes `org/slf4j/impl/**`), the
complete OpenRewrite 8.17.1 runtime closure (delomboked), resource completion, and the gate re-run on the
Maven-built module jars.

# Closure, equivalence gate on Maven-built jars, and the shaded tooling jar (2026-10-11)

- The vendored tree now holds 55 Maven-built source modules: the complete OpenRewrite 8.17.1 runtime closure
  (`OPENREWRITE_8_17_1_RUNTIME_CLOSURE.txt`) plus the tooling's direct pins. `m3/vendor/third_party-images`
  is removed; every former image is now a built module.
- Gate (`API_SURFACE.tsv`, ASM comparison of each Maven-built jar with the published jar): 33 IDENTICAL,
  21 EQUIVALENT_MODULO_COMPILER (anonymous-class constructor captures and flags, empty package-info,
  Signature attributes newer javac emits), 1 IDENTICAL_EXCEPT_SHADED. 0 missing classes, 0 missing resources,
  0 differing resources.
- Upstream build steps reproduced: slf4j 1.7 drops `org/slf4j/impl/**` (the six 1.7.36 pins are switched
  again, verdict IDENTICAL); rewrite-core shades jgit to `org.openrewrite.shaded.jgit` and junit-jupiter-params
  shades univocity (maven-shade-plugin); byte-buddy runs the five upstream byte-buddy-maven-plugin
  transformations; junit-jupiter-api/params compile their Kotlin facades; the OpenRewrite modules are delomboked
  with Lombok 1.18.30 and the upstream `lombok.config` (269 files; each carries a NOTICE line, the unchanged
  upstream file is kept under `upstream-lombok/`).
- `m3/vendor/third_party/m3-tooling-shaded`: maven-shade-plugin aggregate of the vendored OpenRewrite stack,
  `com.m3.vendor:m3-tooling-third-party:8.17.1-m3-shaded-1`; `SHADE_CLOSURE.tsv` shows 45 merged artifacts, all
  vendored source modules, 0 downstream binaries.
- `DOWNSTREAM_DEPENDENCY_LEDGER.tsv`: 52 pins VENDORED_SOURCE_BUILD+SWITCHED, 0 NOT_VENDORED; 55 rows are Maven
  build plugins (build tools, not JDK runtime dependencies).

Known residue (not yet fixed): 78 resources in 24 built jars that the published jars do not contain
(`package.html`/`overview.html`/doc-files, Jackson `PackageVersion.java.in` templates, ANTLR `.tokens`/`.interp`
files, the classgraph `README`, jackson-core's copies of LICENSE/NOTICE under `META-INF/jackson-core-*`). No class
differs. Next commit: the upstream packaging excludes for those paths and a gate re-run.

# Residue resolved: every Maven-built jar holds exactly the published resource set (2026-10-11)

- Source-only resources: 79 non-Java files of the sources jars that the published jars do not contain (doc
  `package.html`/`overview.html`/doc-files, Jackson `PackageVersion.java.in`, ANTLR `.tokens`/`.interp`,
  the classgraph `README`, jackson-core's `META-INF/jackson-core-*` copies) move to `<module>/upstream-source-only/`.
  They are kept byte-for-byte and are not packaged, as upstream does.
- `META-INF/jpms.args` (written by maven-compiler-plugin when javac receives `--add-exports`, rewrite-java-21) is
  excluded from every jar; no published jar carries it.
- Gate re-run on the rebuilt jars: 33 IDENTICAL, 21 EQUIVALENT_MODULO_COMPILER, 1 IDENTICAL_EXCEPT_SHADED; resources
  0 missing, 0 differing, 0 built-only across all 55 modules. Shade closure: 45 artifacts, 0 downstream binaries.

# Consumer coverage extended to every m3 pom on master (2026-10-12)

A whole-repository scan of master (`M3_POM_SCAN.tsv`, every pom.xml read from git) found 33 tooling poms added since
the first inventory. 71 of their pins (rewrite-core/-java/-java-21/-test 8.17.1, slf4j-api 1.7.36, jetbrains
annotations 24.1.0) are vendored modules and are now switched: 123 pins in 51 poms (`CONSUMER_SWITCH.tsv`). Recipe
test fixtures under `src/test/resources/` are input data and stay unchanged.

Still downstream after this commit:
- `org.junit.jupiter:junit-jupiter` 5.10.2 / 5.11.4 (test scope, 51 poms). The vendored junit-jupiter-api/-params do
  not include the engine and launcher that surefire loads.
- `com.synexia:synexia-jdk-a3-recipes` and `com.synexia.m3:cpu-gpu-rv32im` (1.0.0-SNAPSHOT): first-party Synexia
  artifacts.
- Compile-only (`provided`) inputs of the vendored modules: micrometer-core's optional binder APIs (hazelcast,
  kafka, hibernate, jetty, mongodb and others), plus byte-buddy and assertj optional APIs. They are never on a
  runtime classpath.
- Maven build plugins (129 rows): build tools, resolved from the plugin repository.
- `src/utils/IdealGraphVisualizer` and `src/utils/LogCompilation`: upstream OpenJDK developer utilities, not part
  of the JDK build.

# Fidelity audit fixed: descriptors, multi-release overlays, shading, class versions; JUnit 5 runtime (2026-10-12)

The gate had two blind spots. It skipped `module-info.class`, and for multi-release jars it scored the better of the
base and JDK 21 views. Both are closed. Descriptors are compared on name, open, requires and modifiers,
exports/opens and targets, uses, provides, main class, class version and placement. Every view must match.

Build changes, each reproducing what upstream does:
- **Descriptors.** Where the sources jar ships `module-info.java`, javac compiles it at the published class version
  (maven-compiler-plugin execution, `--patch-module`, published `moduleVersion`). Otherwise moditect 1.2.2.Final
  (upstream's tool) assembles the declaration, generated from the published Module attribute
  (`src/main/descriptor/<root|N>/`).
- **Multi-release overlays.** `META-INF/versions/N` sources come from the sources jar (xz), or from the upstream
  release tag with the commit pinned (`OVERLAY_SOURCES.json`): junit-platform-commons 1.10.2/1.11.4, snakeyaml 2.2,
  FastDoubleParser 1.0.0. They are compiled at release N into `META-INF/versions/N`, with `Multi-Release: true`.
  FastDoubleParser's published base classes come from `fastdoubleparser-java8` at v1.0.0; the sources jar ships the
  JDK 11+ variants.
- **jackson-core** shades FastDoubleParser with the published pom's shade plugin block, verbatim (relocations of
  classes, multi-release entries and legal files, and `minimizeJar`).
- **Class versions.** The main compile targets the published base class version (minus 44, floor 8); manual
  overrides had raised jackson to 21 and rewrite to 17.
- **JUnit 5 test runtime** is vendored as 11 more modules: Jupiter/Platform 5.10.2/1.10.2 and 5.11.4/1.11.4, aligned
  per release train, with the `junit-jupiter` descriptor-only aggregates. The 5.11.4 Kotlin facades use Kotlin
  1.9.23 with API 1.6, as upstream.

Gate on the rebuilt jars, 66 modules: 36 IDENTICAL, 30 EQUIVALENT_MODULO_COMPILER. Resources: 0 missing, 0 differing,
0 built-only. Descriptors: 0 missing; classgraph differs only in module version (moditect cannot omit it; it is
informational and not used in resolution).

Not reproduced on the JDK 21 toolchain: FastDoubleParser `META-INF/versions/22` (2 classes and the descriptor),
which a JDK 21 runtime never reads. Class versions below 52 (asm, jsr305 and others published for Java 5/6/7)
compile at 52, the JDK 21 javac floor.

Consumer pins: 172 switched, the JUnit 5 test pins included. Still downstream: `com.synexia:synexia-jdk-a3-recipes`
(first-party snapshot), and two `junit-jupiter` pins whose version is inherited from a parent's dependencyManagement
(the ledger cannot resolve it). Maven build plugins (129 rows) are build tools.

User decision (2026-10-12): the inlined libraries use M3String transparently. On M3jdk21 every `java.lang.String`
they create is M3String-backed under `-XX:+UseM3StringStorage`, and `java.lang.M3String` is package-private.
Sources stay unchanged. A runtime proof lane (flag on/off) follows the first master JDK build.

# M3jdk21 as the SDK superset: the full third-party set; inline -> atomize -> patternize -> reuse (2026-10-12)

User direction: M3jdk21 is an SDK, a superset of JDK 21, that "is going to have it all". The vendored tree now
carries the full set Synexia carries: 131 donors, 118 Maven-built source modules and 13 images. Gate on the rebuilt
jars: 63 IDENTICAL, 55 EQUIVALENT_MODULO_COMPILER. 120/120 built.

Images:
- caffeine 3.1.8 and lz4-java 1.8.0: build-generated classes. Their sources have now been reproduced by running
  upstream's own generators at the release tags (caffeine: JavaPoet generators, 528 files, clock pinned to the
  release instant for the copyright line; lz4-java: MVEL templates, 16 files). They land as modules in the next
  commit.
- ecj 3.46 and zstd-jni: FAILED.
- Nine OpenRewrite language modules and jdt.core: SOURCE_ONLY.

Ledger corrections:
- `com.synexia:synexia-jdk-a3-recipes` is built in-reactor from `m3/vendor/synexia` (IN_REPO_MODULE).
- Two `junit-jupiter` pins inherit their version from a parent's dependencyManagement and are vendored (MANAGED).
- Still external: `com.synexia.m3:cpu-gpu-rv32im` (hsoliwal/synexia-tornadovm). Its `tornado-api` dependency will not
  be vendored: tornado-api 6.1.0 contains a GPL-2.0-only file. Per the user, TornadoVM becomes a donor for a new M3
  parallel API and a JNI vCPU layer (CPU / Intel / CUDA / AMD backends). The design document comes first.

Atomize and patternize over all 131 donors (CPU projection, `reuse/ATOMIZE_PROOF.json`): 21,717 Java files, 360,050
leaves, 144,155 exact classes, 26,367 cross-donor exact classes. No representative outside the donor sources.

Reuse (`reuse/REUSE_*`, proposal-only): 1,478 exact patterns shared across different artifacts and 15,077 across
versions of one artifact. Top pairs:
- asm and byte-buddy (byte-buddy embeds a relocated ASM; 363 patterns, 114k tokens);
- the OpenRewrite language modules (rewrite-hcl/json/yaml/protobuf);
- commons-lang3 and commons-text;
- jgit and rewrite-core (shaded jgit);
- snakeyaml and snakeyaml-engine.

Refactor tracks:
- R1, shade instead of copy: build byte-buddy from the vendored asm plus shading, as upstream does.
- R2, converge to one version per library, proven by building the consumers.
- R3, a recipe-driven refactored layer, proven against upstream test suites.

# caffeine and lz4-java built from upstream generator output (2026-10-12)

The last two images that were missing build-generated classes are now Maven-built source modules:
- **caffeine 3.1.8.** The 528 generated sources come from upstream's JavaPoet generators
  (`LocalCacheFactoryGenerator`, `NodeFactoryGenerator`) run at tag v3.1.8, commit b0723da5, with the generator
  dependencies pinned to the tag's libs.versions.toml. The generators stamp the current year into each file's
  copyright line, so the clock is pinned to the release instant (2023-08-09).
- **lz4-java 1.8.0.** 16 generated sources come from upstream's MVEL templates (`src/build/gen_sources.mvel`) run at
  tag 1.8.0, commit 78eae489, with mvel2 2.3.2.Final. The generator needs `java.lang.Compiler`, so it runs on JDK 17;
  a JDK 21 run with a one-class stand-in produced byte-identical output.

Both modules keep `GENERATED_SOURCES.json` and `regenerate.sh` (which checks every output hash) at the module root.
Provenance kind: upstream-generated-source-at-tag.

Gate, all 120 modules: 63 IDENTICAL and 57 EQUIVALENT_MODULO_COMPILER, with caffeine and lz4-java both
EQUIVALENT_MODULO_COMPILER (0 missing classes). Images left: ecj and zstd-jni (FAILED), and the nine OpenRewrite
language modules plus jdt.core (SOURCE_ONLY). 172 pins switched; shade closure 0 downstream binaries; crate
131 entries, drift 0.
