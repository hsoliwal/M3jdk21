<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->

# Provider-dependent collection JUnit: source-only bridge and complete JDK lane

The original `m3/collections/pom.xml` compiles only
`src/jdk.m3.collections/share/classes`. Its compiler excludes the
`test/jdk/jdk/m3/collections/providers/**` tests, which depend on the
separate `src/jdk.m3.collections/providers/classes` owner. Merged
M3JDK21 PR #3212 established this source/module boundary.

## Source-preserving receiving correction

The 54 original JUnit Java sources and helpers in the Apache, Eclipse,
Guava and Vavr provider test folders were relocated under the existing
`providers/` test root, **without changing a single source byte**, license,
package declaration, API or assertion. The canonical
`provider-tests-20261123.tsv` inventory records their old and new paths
and unchanged Git blob SHA-1 values. The old paths must be absent.

The new `make/m3/collections/test-provider-junit.sh` runs those same
tests as a *required* source-built complete-module lane, rather than
silently excluding them. It rejects stock Java, verifies three installed
provider classes, checks all 54 source identities, uses the source-built
`jdk.m3.collections` module and compiles with `javac --patch-module`.
It invokes 14 original JUnit test classes using a SHA-256-pinned JUnit
Platform Console launcher and publishes success only after execution.

The existing native-proof GitHub Actions workflow runs the provider test
lane and keeps the normal default Maven/JUnit bridge as a **separate,
fatal** stage. The original POM, production Java owners, JDK module
descriptor and native libraries remain unchanged.

## HOLD and acceptance

The test-only move by itself is not proof of a passing build. Require
exact-commit CI evidence of the full JDK, original JUnit provider
assertions, retained default Maven suite, source hashes, and native
libraries. Original Groovy String collisions, Groovy GINQ semantic
differences, combined collection integration, other platforms and
history gates remain separate. Do not merge before actual runtime
results are reviewed.
