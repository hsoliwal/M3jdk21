# jdk.m3.ast and jdk.internal.m3.ast: product prefix dropped (2026-11-09)

`rename_types.py` v1.7 renamed 39 types in m3/ast (`RENAME.tsv`), run with `--string-words --comment-words --doc
m3/ast/`. What follows:
- the three workflows that compile the syntax sources by path and run their mains;
- the module docs;
- the consumer m3/eclipse/optional-jmod (M3EclipseCore, M3EclipseCoreProvider and the smoke test import ASTHierarchy).
The 18 //FLLDCIM anchors are unchanged. jdk.internal.m3.indexstring.ast and the split jdk.internal.m3.rewrite of
this module are not renamed here.

These are records and keep their bytes (`--exclude`):
- m3/fs/verification/ (a dated verification record);
- the receiver custody of 2026-10-18: SOURCE_BORROW.tsv, SOURCE_SUCCESSOR.json and verify_receiver.py.
verify_receiver.py asserts that the live M3ASTHierarchy.java byte-equals an immutable recorded SDK result at its old
path, so no rename can carry it. It already fails on master: since the JDK naming rename, its expected source_recipe
is jdk.internal.m3.rewrite.M3ASTL0L5Target20261017 while the historical TSV keeps com.synexia.rewrite.*. No workflow
runs it.

Inventory: pom_dependents gives 0 dependents for m3-ast-levels-candidate, and no open PR touches the module.
optional-jmod/build.py builds jmods from these sources and checks only that two builds agree; it pins no digest.

## Gate (WSL OpenJDK 21), before vs after

| | Before | After |
|---|---|---|
| m3/ast javac main/test, with the pom's flags (source/target 21, add-exports java.base/jdk.internal.org.objectweb.asm) | rc=0 / rc=0 | same |
| m3/ast JUnit | 19/19 | same, per-test status identical |
| optional-jmod main sources against this m3/ast and the current core, arrays, collections and file (`consumer_compile.sh`) | 4 errors, all `jdk.m3.arrays.M3Utf16ArrayView` | the same 4; every jdk.m3.ast reference resolves |

The 4 errors already fail on master: the arrays rename made the type Utf16ArrayView and left optional-jmod behind.
That is reported to its owner.
