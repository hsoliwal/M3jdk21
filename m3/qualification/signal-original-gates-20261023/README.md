<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# Original M3JDK21 javac/JSR source signals and JUnit source vendor admission

**M3JDK21 remains the integrated JDK; this is a product build-gate repair, not an external runtime.** No Java object API, user code, compiler plugin provider type, native C/JNI signature or stored signal relationship data changed.

## Two source-file-local fixes
1. `m3/compiler/source-signals/pom.xml`: before compilation the standard Maven resources phase used to expose `META-INF/services/com.sun.source.util.Plugin` in `target/classes`, causing the compiler to ServiceLoader-discover the **not-yet-compiled** provider and abort. The source descriptor is excluded from **only the premature main resource copy** and placed at the exact same package/service path after compilation in `process-classes`, with the standard Maven Resources `copy-resources` goal. It remains present for JAR packaging and normal javac plugin use.
2. `.github/workflows/m3-source-signal-image-20261022.yml`: the existing original source-vendored JUnit build selected `junit-jupiter-5.10.2 -am` but omitted independent `junit-platform-launcher-1.10.2`, causing Surefire to fail resolution from Maven Central. The workflow builds **both original source donor modules and their reactor dependency closure**, with exact PROVENANCE/NOTICE verifiers for both binaries. This does not change the original `m3/collections/pom.xml` or pretend ordinary Central JUnit is equivalent.

These defects were observed in real failed GitHub Actions:
- [Original javac plugin job](https://github.com/hsoliwal/M3jdk21/actions/runs/39757037695): `ServiceConfigurationError` before JUnit ran.
- [Original Top-K JUnit job](https://github.com/hsoliwal/M3jdk21/actions/runs/39757043726): missing `org.junit.platform:junit-platform-launcher:1.10.2-m3-source-1` at Surefire execution.
- Focused Java21 primitive CSR and Top-K source jobs **passed**, and are not mislabeled JUnit passes.

## Recipe and receipts
Canonical Maven/OpenRewrite recipe and first-apply/fixed-point/drift-refusal test: `hsoliwal/com.synexia`, crate `m3-signal-junit-compiler-bootstrap-20261023`. Both inputs were captured from current M3JDK21 master and rehashed before product writes. `SOURCE_MANIFEST.tsv` pins source before/after SHA-256 plus exact after Git blob. `verify.sh` refuses mismatches; it is only a custody gate.

## Acceptance
Strict actual Maven/JUnit and source-vendored launcher compilation are required. A success of these two **does not** qualify HotSpot/JNI/full JDK image, JCK/jtreg, cross-platform behavior, performance or the remaining 4,770-path receiving inventory. Existing owned source/JNI contracts and the locked String → arrays → collections → AST/compiler order remain unchanged.

Statuses at authoring: SOURCE_RECEIVER_IDENTITY=PASS; original Maven compiler=NOT_RUN on corrected POM; original Jupiter+Launcher=NOT_RUN on corrected workflow; whole JDK/JNI=NOT_RUN. No automatic merge or ancestry rewrite.

## Completed original-gate progression and Java21 fixture correction

Focused run [39886641532](https://github.com/hsoliwal/M3jdk21/actions/runs/39886641532)
**PASSED** the original `M3SourceSignalPlugin` Maven/JUnit SPI job. In
[39888402849](https://github.com/hsoliwal/M3jdk21/actions/runs/39888402849)
the original source-vendored Jupiter and Launcher plus current `m3-arrays`
prerequisite progressed through build, but M3 collections `testCompile`
failed at `M3LongTreeSetMaskCompactionContract.java:12:38` with
`integer number too large`. That pre-existing invalid Java21 test source
declared a `long` seed with unsuffixed `0x4D33534F52544C`.

The canonical **named, double SHA-256 guarded** Synexia recipe
`m3-long-tree-mask-seed-literal-20261023` adds only the Java `L` suffix.
Host Java21 `javac --release 21` independently rejects the exact old
literal and compiles its long-typed successor with `-Xlint:all -Werror`.
The original full 1,280-case test fixture is retained unchanged except for
the suffix; it is not considered a passing product test until the owning
Maven/JUnit workflow runs on the corrected checkout.

There are now four exact source-receiving SHA-256/Git-blob rows in the product
manifest (two original POM/workflow inputs, one additive focused gate and one
original invalid Java21 test fixture). This is a reviewable draft, not global
String/arrays/collections/compiler admission.
