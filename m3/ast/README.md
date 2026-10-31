<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3AST L0-L5 — independently built Java 21 receiving candidate

The M3ASTHierarchy Java facade implements the canonical MIndexAST hierarchy
level mapping L0 per-file facts/atoms, L1 file, L2 package, L3 module, L4
project, L5 repository. It stores flat primitive atom handles and byte masks
plus scope-offset arrays. The 13-family classification covers all named M3
collection *kinds*, but is not an installation of all implementations.

This is target-received Apache-2.0 implementation work in a separate module.
It is not a public OpenJDK `java.util` or `jdk.compiler` backend, does not add
JNI symbols, and has no Synexia Maven/runtime dependency. The canonical
algorithm and source-sealed recipe remain in hsoliwal/com.synexia.

Run `mvn -B -ntp -f m3/ast/pom.xml test` on a configured target Maven/JDK 21 host.
The supplied Java-only verifier can run without Maven; it does not replace the
OpenRewrite SDK, jtreg, native, whole-build, CI, or independent-review gates.
