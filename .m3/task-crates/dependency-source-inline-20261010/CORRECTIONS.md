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
