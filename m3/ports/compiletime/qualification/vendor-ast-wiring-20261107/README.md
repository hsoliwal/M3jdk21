<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# compiletime: compile the vendored mindex AST, repair the residency descriptors (2026-11-07)

`m3/ports/compiletime` did not compile on master.

1. **Missing AST package.** The ten `jdk.m3.compiletime.ast` owners import `jdk.internal.m3.mindex.compiler.ast`
   (`MIndexJavaAST`, `MIndexASTDocument`, `MIndexASTOccurrence`, ...). This has been true since 46ee4ad33e90
   (2026-10-18; before the JDK-naming rename the package was `com.synexia.mindex.compiler.ast`). The package
   never resolved from inside the repository. Its sources are carried at `m3/vendor/m3/m3-mindex/compiler`, with
   the runtime closure at `m3/vendor/m3/m3-mindex/runtime` and `m3/vendor/m3/m3-native-interop`, but no pom
   builds them.

   Fix: the `default-compile` execution adds those three directories as source roots. This is the same shape as
   `m3/tooling/m3jdk-a3-recipes`, which compiles `m3/vendor/m3/m3-openrewrite-recipes`. Tests keep the default
   test root. All 138 main files compile with `-Xlint:all -Werror` and 0 warnings.

2. **Stale annotation descriptors.** With the module compiling, 8 of its 30 tests failed.
   `M3ItemReferenceScan` matched the residency annotations by JVM descriptor
   `"Lcom/m3/residency/M3Transient;"` (and likewise `M3Retains` and `M3BehaviourOnly`). The rename fe35ea988411
   moved those types to `jdk.m3.residency` but left the strings unchanged. Naming rule `PKG_PATH_COM_M3` refuses a
   letter before `com/`, so an `L`-prefixed descriptor is never rewritten.

   Fix: the three descriptors now read `Ljdk/m3/residency/...`. Result: 30/30.

Open: the `L`-descriptor gap in `jdk_naming.py` may affect other `"Lcom/m3/` or `"Lcom/synexia/` strings in the
tree. That rule change is a separate jdk-naming leaf.
