<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Existing OpenRewrite Java trees in the M3 index

`M3OpenRewriteIngress.openrewriteJ(M3File, J.CompilationUnit, M3ASTIngress.Limits)`
projects an existing OpenRewrite Java tree into the existing `M3ASTIndex` owner.
The caller supplies the tree produced by its own frontend. This companion does
not invoke a parser, print the tree, run attribution, or create a payload store.
It is optional: core `jdk.m3.parser` has no OpenRewrite runtime dependency.
`M3ASTIngress.openrewriteJ(M3File, Object, Limits)` dispatches to this typed
companion when its real dependency closure is available.

```java
M3ASTIndex rows = M3OpenRewriteIngress.openrewriteJ(
        source, existingCompilationUnit, M3ASTIngress.Limits.defaults());
```

## Source and identity contract

An external tree can have been rewritten independently of the supplied source.
Every returned row therefore retains `SYNTAX_ONLY | SOURCE_UNVERIFIED`, and its
start/end offsets and start/end lines are `-1`. UUIDs, source paths, printed text,
and source checksums do not establish a binding. A supplied source still has its
own immutable identity; that identity does not certify the external tree.

Rows represent tree occurrences in encounter order. Parents and enclosing type
or executable indices refer to strict represented ancestors. A lambda is an
executable ancestor. These coordinates are not global word IDs or semantic
binding identities. No `J`, `JavaType`, cursor, marker, or live compiler handle is
retained in the result. Selected names and literal values are detached scalar
strings. Common Java declaration, identifier, literal and statement kinds share
the core kind names; other model-specific kinds retain an `OPENREWRITE_` prefix.

Literal values preserve their typed value: floating-point metadata contains the
unsigned decimal encoding of the raw IEEE bits, a character is one UTF-16 unit,
and an empty string remains a string. A contradictory literal type/value is
rejected. Interface and annotation declarations carry the `INTERFACE` capability;
their distinct declaration kinds remain available to policy consumers.

The iterative projection bounds source UTF-16 units, node occurrences and depth.
It visits direct child edges using the existing OpenRewrite visitor. Runtime and
index storage grow with represented occurrences and selected scalar metadata.
No claim of zero allocation, global interning, semantic equivalence or faster
parsing follows from this projection.

## Executed qualification and build boundary

The task's `verification/openrewrite/run_contract.py` compiles the selected core,
this companion, and its contract under Java 21 with `-Xlint:all -Werror`. It uses
the actual cached OpenRewrite 8.17.1 artifacts and records all source/dependency
SHA-256 values and commands. The final candidate receipt is
`verification/openrewrite/candidate3/RESULTS.json`: nine groups and 110 assertions
passed. They exercise modified existing trees, scalar values and ownership,
unknown ranges, source mismatch, limits, interface/annotation capabilities, the
core optional bridge, contradictory literals, and a real marker that throws if
the tree is printed. Fixture parsing happens only in the test.

The earlier infrastructure failure, feature RED, and corrected test-fixture
failure remain separate receipts. The feature RED failed because the typed
provider and core bridge were absent. The no-print test initially expected an
unwrapped assertion; the actual OpenRewrite printer wraps it in
`RecipeRunException`. Its correction verifies that real cause before testing
that the provider never prints.

The donor `rewrite.core` and `rewrite.java` 8.17.1 artifacts are automatic modules.
This classpath proof does not make them directly linkable with `jlink`. An
explicit companion descriptor and qualified dependency descriptors/JMODs belong
to the existing module-pack build, with their legal notices preserved. The
companion introduces no guessed Maven artifact for `jdk.m3.parser`. Full Maven,
linked-image and source-built JDK gates require their own executed receipts.

Implementation and fixtures are original Apache-2.0 M3/Synexia integration work
by Hitesh Soliwal and contributors. OpenRewrite retains its own source, artifact,
license and notice identities; no donor source was copied into this companion.
