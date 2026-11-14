<!-- SPDX-License-Identifier: Apache-2.0 -->
# m3/intern -> java.base `jdk.internal.m3.intern` (M3_TO_JDK_LAYOUT P1, leaf 1)

Approved layout 2026-11-08: java.base `jdk.internal.m3.*` holds intern, lexicon, native, runtime, core and compression.
Tool: `make/m3/tooling/jdk-naming/move_area.py` v1.1 with `--keep-pom`. Ledger: `make/m3/integrate/P1_intern.tsv` (82 rows).

| What | From | To | Files |
| --- | --- | --- | ---: |
| product sources | `m3/intern/src/main/java/jdk/m3/intern` | `src/java.base/share/classes/jdk/internal/m3/intern` | 8 |
| tests | `m3/intern/src/test/java/jdk/m3/intern` | `test/jdk/jdk/internal/m3/intern` | 41 |
| JNI oracles of the tests | `m3/intern/src/test/native` | `test/jdk/jdk/internal/m3/intern/native` | 2 |
| Maven bridge (`pom.xml`, `module-info.java`) | `m3/intern` | `make/m3/intern` | 2 |
| recipe records | `m3/intern/recipe/<name>` | `make/m3/intern/recipe/<name>` | 29 |

Text rules on the moved Java and C files: package `jdk.m3.intern` -> `jdk.internal.m3.intern` (dotted, slash and JNI
forms). The package is not exported by java.base. Nothing is removed: the bridge keeps the artifact `m3-intern`
(module `org.m3jdk.intern`, now exporting the renamed package) building on a stock JDK 21 for its consumer
`m3/ports/indexstring`, whose one import follows. The bridge retires when that consumer moves into java.base.

## Gates (WSL Ubuntu, OpenJDK 21.0.13, gcc 13.3; files extracted from the commit tree)

| Gate | Before (Maven layout) | After |
| --- | --- | --- |
| product compile, `-Xlint:all -Werror` | 22 classes | 22 classes, as a java.base patch (`--patch-module`) |
| JUnit, class path (what the bridge runs) | 31 found, 31 passed | 31 found, 31 passed |
| JUnit as a java.base patch (`make/m3/intern/test-java-base.sh`) | n/a | 29 found, 29 passed; 2 classes skipped by name |
| JNI oracles, `gcc -Wall -Wextra -Werror` | n/a | both build; `ClassSpaceArchiveSelfTest` passes with its library (10,265 assertions) |
| consumer `jdk.internal.m3.indexstring.type` compiles against the renamed package | yes | yes (12 classes) |

The two skipped classes are `ClassSpaceJUnitTest` and `ClassFileAtomsJUnitTest` (one test each). Their oracle compiles
fixtures with `javax.tools`; `java.compiler` belongs to the platform class loader, so a class patched into java.base
cannot link to it. They run in the bridge on the class path, where all 31 tests pass.

Not run: `make java.base-java` and jtreg on a built M3JDK image (no local JDK build environment for this lane). The
new step of `.github/workflows/m3-java-base-internals.yml` runs the java.base patch harness in CI.
