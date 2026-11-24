<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->

# JUnit provider tests in a platform-loaded JDK module: test loader correction

## Hosted failure observed, not inferred

The full-image [combined proof 45134282192](https://github.com/hsoliwal/M3jdk21/actions/runs/45134282192)
and the merged GINQ branch's
[proof 45148359472](https://github.com/hsoliwal/M3jdk21/actions/runs/45148359472)
passed the original 12 native CTests, full/linked **5,020-check**
`countByMany`, full/linked **8,041-check** GINQ, source-bound
installed-image checks, and 5/5 selected jtreg tests.

Both source-built images compiled the provider JUnit sources; the
subsequent JUnit Platform Console 1.12.2 invocation discovered **zero
tests** (three containers successful, zero tests found, exit code 2).
This was not an assertion failure. The original default Maven bridge
remains a separate fatal gate and was not reached.

## Java 21 class-loader cause and correction

The original `jdk.m3.collections` is loaded with the JDK platform
class loader. Its provider test classes are compiled using
`javac --patch-module jdk.m3.collections=...`. Those test classes
cannot resolve JUnit Jupiter annotations from an application-only
`-cp junit-platform-console-standalone.jar`, even with an
`--add-reads ...=ALL-UNNAMED` read edge. Reflection then sees no
runtime `@Test` metadata.

This mechanism was reproduced on **Java 21.0.12.1**, independently
of JUnit internals, with a synthetic `java.sql` module patch,
a runtime-retained `org.junit.jupiter.api.Test` annotation, and
`M3ProviderJUnitDiscoveryProbe`. The negative application-only
annotation case refused; adding the same test jar to the bootstrap
append path found one original annotation. The fast
`make/m3/collections/test-provider-junit-loader.sh` test runs both
cases, fails on a false success, and emits
`M3_PROVIDER_JUNIT_LOADER_SELFTEST_PASS negative=refused positive=1`
only after real Java compilation and both JVM launches.

The production-provider runner now appends the SHA-256-pinned original
JUnit standalone 1.12.2 jar to the **test JVM only**, with
`-Xbootclasspath/a:`, while retaining the original Java module,
`--patch-module`, package opens, test source hash manifest, JNI
property, 15 original JUnit test classes and 57 source files. An
independent application-loaded discoverability probe rejects any
class that is outside the named module or lacks a readable
runtime `org.junit.jupiter.api.Test` annotation. It must produce
`M3_PROVIDER_JUNIT_DISCOVERY_PASS classes=15 methods=N` with N
strictly positive before the real JUnit launcher is permitted to run.
The genuine launcher still uses 15 explicit `--select-class` entries
and `--fail-if-no-tests`, and can publish
`M3_PROVIDER_JUNIT_PASS classes=15 sourceFiles=57` only on success.

The JUnit jar is not added to any product module, JMOD or runtime
distribution. The original 57 Java test sources and all assertions
remain unchanged, and the broad collections Maven tests stay fatal.

## Real JUnit Platform reproduction (stronger than annotation inspection)

Using the actual GitHub Actions artifact from the combined run (the
repository-owned jtreg 8.2.1+1 bundle), the pinned standalone
`junit-platform-console-standalone-1.14.2.jar` (SHA-256
`5566ffe2aa48263867bca745925f73bf7b01591b30d9a60f191c0b16fa0955e9`)
was extracted locally and used with OpenJDK **21.0.12.1** to run a
genuine Jupiter `@Test` assertion patched into Java's platform-loaded
`java.sql` module.

The **uncorrected** JUnit Platform Console discovered **zero tests**
and exited with code **2**, reproducing the exact hosted failure mode.
With the same jar on the test-only bootstrap append path, it discovered
**one** test, executed the real `Assertions.assertEquals` call, and
exited with code **0**. This is an executed, synthetic Java module and
real Jupiter engine regression; it is not a claim that the original
15-class provider cohort has passed.

The fast source-identity job now downloads the exact pinned **1.12.2**
standalone jar (SHA-256
`329bd10288875a74d04c9ca6b7c9889c265ddf87b21a9ea42a7f7392f391472a`),
executes both the annotation-only negative/positive class-loader probe
and the real JUnit Jupiter negative/positive module fixture, and
requires `M3_PROVIDER_REAL_JUNIT_LOADER_SELFTEST_PASS tests=1 negative=refused`
before building the full image. The real 1.12.2 source-job execution
is pending hosted evidence and cannot be inferred from the 1.14.2
reproducer.

## Qualification boundary / HOLD

The synthetic Java 21 platform-loader negative/positive fixture
**passed locally**. The exact 15-class provider JUnit execution with
the source-built M3JDK21 image has not yet been demonstrated passing
with this correction; neither have the downstream original Maven
bridge, Groovy 4/5/6 compatibility, cross-platform JDK/JNI, nor
full collection release qualification. Do not merge this fix based
only on the synthetic self-test; review the hosted exact-head CI
and all original tests.
