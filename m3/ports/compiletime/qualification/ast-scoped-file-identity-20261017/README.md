<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3AST scoped-file identity receiver — 2026-10-17

**Product:** hsoliwal/M3JDK21. **Canonical source and recipe:**
[hsoliwal/com.synexia#12081](https://github.com/hsoliwal/com.synexia/pull/12081),
exact Maven/OpenRewrite receipt commit
`cc9f7956c91eb6a4b95696bfaafe3e1a74ed68ef`.
This is an additive two-Java-file, one behavioral-atom correction to the
previously admitted compiletime evidence index. It does not instantiate
new public java.util wrappers, an AST parser, a JNI backend, HotSpot or
java.base changes. Synexia is never loaded by the product at runtime.

## Bug / invariant

Original `build` sorted input by project, module, package and normalized
relative path but deduplicated only normalized `path`. Independent projects
and modules commonly share paths such as
`src/main/java/example/Main.java`. Rejecting that pair incorrectly
breaks repository aggregation.

The recipe replaces the predicate with a framed tuple consisting of
**(project, module, normalized relative path)**. Package is not a distinct
source owner; duplicate same-project/same-module paths remain invalid
even if a different package is supplied. Fixed-length source SHA-256 remains
bound into existing file rollups. Public methods, L0 facts/atoms, L1 file,
L2 package, L3 module, L4 project, L5 repository, physical `int[]`
atom offsets, parent/child index edges and `long[]` state words are
unchanged. Stable sort, duplicate atom occurrences and full 64-bit
required/forbidden mask tests remain intact.

**No new JNI code is appropriate:** this defect is a compiler metadata
source-owner identity check, not a primitive array kernel. Existing
M3IndexPathMasks Java/JNI and M3ObjectStateSlots28 generations are
retained as the only relevant already-owned primitive-native components.
Filtered/sorted wrapper JDK contracts remain unchanged.

## Recipe and exact source identity

The reviewed candidate output is supplied by
`com.synexia.rewrite.M3ASTScopedFileIdentityJdk2120261017`, which
delegates to existing `M3HashPinnedJavaSnapshotRecipe` using
`m3-ast-scoped-file-identity-jdk21-20261017` template/manifest.
The exact before/after SHA-256 values are recorded in
`SOURCE_RECEIPT.tsv`, with a preserved original preimage and an
explicit admission/refusal JUnit. Only these two product files are
candidate postimages; this receiver does not bring private donor modules
into the product.

## Local and pending proof

- **RED** with unchanged old owner and new test: duplicate relative path
  across modules throws the old `IllegalArgumentException`.
- **PASS** on actual selected code: Java 21 strict
  `javac --release 21 -proc:none -Xlint:all -Werror`;
  `M3ASTLeafIndexSelfTest` **88,828** checks; Synexia equivalent
  **88,828** checks.
- **PASS**: four generated source hashes and Git blob bytes agree with
  original Synexia template derivation, no hand-patched receiver output.
- **PENDING/HOLD**: actual hosted Maven/OpenRewrite SDK fixed-point +
  drift-refusal run, complete Maven/JDK build, jtreg/JCK, JNI cross-platform
  package validation and CPU/retained-heap measurements. Do not advance
  the required STRING -> arrays -> collections -> AST receiving phase.

Focused repeat:
```sh
python3 m3/ports/compiletime/qualification/ast-scoped-file-identity-20261017/verify.py
mkdir -p /tmp/m3-ast-owner
javac --release 21 -proc:none -Xlint:all -Werror -d /tmp/m3-ast-owner \
  m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ASTLeafIndex.java \
  m3/ports/compiletime/src/test/java/com/m3/compiletime/M3ASTLeafIndexSelfTest.java
java -cp /tmp/m3-ast-owner com.m3.compiletime.M3ASTLeafIndexSelfTest
```
Canonical SDK:
`mvn -B -ntp -f synexia-openrewrite-recipes/pom.xml -Dtest=M3ASTScopedFileIdentity20261017RecipeTest test`

First-party expression: Copyright 2026 Hitesh Soliwal and contributors,
Apache-2.0. No external challenge implementation copied; LeetCode,
HackerRank and GeeksforGeeks serve as algorithm category references only.
The target runtime source and upstream/JDK license owners are unaffected.
