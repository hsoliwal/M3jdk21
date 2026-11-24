<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->

# JUnit provider module class-loader repair — 2026-11-24

This correction is **test infrastructure only**. It does not change any
original provider Java source, JUnit `@Test` method, module descriptor,
M3 array JNI implementation, source identity manifest, or Maven POM.

## Observed, exact CI failure

The genuinely source-built combined image in [M3JDK21 run 45134282192]
(https://github.com/hsoliwal/M3jdk21/actions/runs/45134282192)
completed the 12 original CTest cases, both GINQ and countByMany
full/linked JVM tests, installed-image identity, and all five selected
jtreg tests. The provider Java21 `javac --patch-module` invocation then
compiled, but JUnit Platform Console 1.12.2 printed **0 tests found**
and exited **2**. The original Maven bridge step was not reached.

## Reproduced loader visibility mechanism

The test classes are patched into `jdk.m3.collections`, a JDK module
defined to the platform class loader, whereas JUnit Console Standalone
was present only on the application class path. The module readability
edge `--add-reads jdk.m3.collections=ALL-UNNAMED` alone does not grant
the platform loader visibility into classes of a child loader.

A local Java 21 synthetic reproduction patched 15 package-private
Jupiter test classes into the standard `java.xml` module: with the
original runtime arguments JUnit reported **0 tests** and exited 2.
Adding the pinned JUnit jar to the bootstrap append search path
(`-Xbootclasspath/a:/path/to/junit.jar`) made the same class files
discoverable: **15/15 tests passed**. A separate two-test reproduction
also passed. This local reproduction used the repository-bundled
JUnit Platform Console 1.14.2 artifact; hosted production uses the
already SHA-256-pinned standalone 1.12.2 jar and must qualify separately.

## Repair and fail-closed verification

`make/m3/collections/test-provider-junit.sh` still compiles the
existing original test and helper sources into the JDK module using
`--patch-module`; it does **not** move the tests back to the Maven
share-only bridge. For execution only, the same hashed JUnit Console
jar is on both the bootstrap append path and the launcher classpath.
The selector list is retained as text and all **15** original JUnit
classes remain explicitly selected.

The runner enables the **Jupiter** engine and requests its XML report.
New `ProviderJUnitReportsCheck.java` validates the generated report:
exactly 15 distinct expected classes selected, a positive executed
testcase for every selected class (including nested Jupiter names),
consistent suite totals, and **no errors, failures or skipped tests**.
Unknown or missing test classes, no tests, partial discovery, malformed
XML, external DOCTYPE/entity declarations, or missing reports are
fatal. Existing `--fail-if-no-tests` is also retained. This requires
real execution, not merely 15 class selectors or an all-green empty
JUnit discovery summary.

`ProviderJUnitReportsCheckSelfTest.java` compiles under JDK21 strict
`-Xlint:all -Werror` and rejects 12 deliberately invalid XML/class
selection fixtures; the source-identity CI job executes these before
starting the expensive JDK image build. The native CI stage keeps the
original provider JUnit gate and broad `m3/collections/pom.xml`
JUnit stage **fatal**. The 15 original tests, 57 source files, CMake,
jtreg, GINQ and countByMany checks remain present and unwaived.

**Admission status:** The synthetic reproduction and standalone report
checker have been locally executed. The exact merged repository's
source-built `jdk.m3.collections` provider suite, original Maven
bridge and full-platform runtime have **not yet produced a passing
post-correction hosted receipt**. Never label this a full native
integration success until that exact CI run completes.

## Exact pinned-JUnit 1.12.2 preflight

The companion `make/m3/collections/test-provider-junit-loader-repro.sh` is an
executable, self-contained 15-class source reproduction. It compiles
package-private Jupiter tests into an existing standard JDK21 module
(`java.xml`, package `javax.xml.parsers`) with original `--patch-module`
compilation. In the same run it demands the original classpath-only launcher
report **0 tests found / exit 2**, then demands the bootstrap-visible
launcher report **15 successful tests / exit 0**, and checks the generated
Jupiter XML for all 15 exact classnames. A positive receipt cannot come
from the original empty discovery state or a partial selection.

This script executed successfully against the locally retained 1.14.2
JUnit Console Standalone JAR. The existing source-identity workflow now
downloads the **actual production 1.12.2** Console Standalone JAR with
its reviewed SHA-256
`329bd10288875a74d04c9ca6b7c9889c265ddf87b21a9ea42a7f7392f391472a`,
runs the same differential on Java21 **before any full JDK compilation**,
and retains its original/candidate transcripts and report. Exact 1.12.2
execution remains pending until that hosted job reports a result; no
cross-version compatibility is inferred from the 1.14.2 local proof.
