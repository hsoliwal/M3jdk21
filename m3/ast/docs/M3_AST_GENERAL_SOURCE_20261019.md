# M3JDK21 — parser-neutral source facts inside the original AST owner
SPDX-License-Identifier: Apache-2.0

Direct source: `m3/ast/src/main/java/com/m3/ast/M3ASTHierarchy.java`.
Base: `677437be295b23f7ccf7409be287008944d0e8b0`. M3JDK21 is the sole product; Synexia is donor/recipe custody, not a runtime layer.

An unnamed Java module/package is the exact empty coordinate. A file with no collection semantics has a zero family mask. This extends the accepted domain of original L0–L5 AST file facts without changing existing hashes, signatures, arrays, mask layouts, atom ordering, or stored object count. The existing `int[]` atom-handle lane and `byte[]` state-mask lane remain authoritative. This is NOT a completed Eclipse JDT binding/working-copy integration or universal parser implementation.

## Real OpenRewrite SDK recipe (Java 21 / Maven 3.9+)
Reuses `M3HashPinnedJavaSnapshotRecipe` and the original `M3TaskRecipeTest`, without hand editing generated postimages. New names point to a two-file hash-pinned dataset. Run from repository root:

```sh
cd m3/ast/recipe-custody/hierarchy-encoding-20261018
mvn -B -ntp -Dm3.dataset=ast-general-source-20261019 \
  -Dm3.crate.name=m3-ast-general-source-20261019 \
  -Dm3.recipe.name=com.synexia.m3jdk.AstGeneralSourceFacts20261019 \
  -Drecipe.owner.root="$PWD/sdk-owner" test
```

The existing five test methods cover actual Java LST/recipe replay, fixed point, permutation, partial pre/postimages, source drift, duplicate/missing targets and post-scan mutation refusal. Use the incumbent `receive.py` only after actual SDK results are qualified. All source postimages are identical to the manifest templates.

## Java product contract
```sh
mkdir -p /tmp/m3-ast-general/classes
javac --release 21 -Xlint:all -Werror -d /tmp/m3-ast-general/classes \
  m3/ast/src/main/java/com/m3/ast/M3ASTHierarchy.java \
  m3/ast/src/test/java/com/m3/ast/M3ASTHierarchyContract.java \
  m3/ast/src/test/java/com/m3/ast/M3ASTHierarchyEncodingTest.java
java -cp /tmp/m3-ast-general/classes com.m3.ast.M3ASTHierarchyContract 2000
java -cp /tmp/m3-ast-general/classes com.m3.ast.M3ASTHierarchyEncodingTest
```

**Draft/HOLD** until actual Java 21 and Maven/OpenRewrite CI are verified; no host JDK image/jtreg/JDT PDE/JNI parity or speedup is claimed. See donor category decisions in `M3_AST_GENERAL_SOURCE_20261019.tsv`.
