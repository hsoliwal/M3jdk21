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
