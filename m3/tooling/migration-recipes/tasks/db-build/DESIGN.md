# DbBuild: typed metadata-copy repair

## Scope and source authority

This bounded pass continues PR88 at271f76ece03243183cd921133e909f24c8f80d3e.
The exact-head recipe job111383897788/run37184634302 failed compilation:
M3Jdk21HashPinnedSnapshotRecipe.java:191 calls withSourcePath on inferred Tree.
The source preimage is Git blob828724689638268574c2b599b1bfcef7921a2f90.

The existing sibling M3HashPinnedJavaSnapshotRecipe already uses sequential
SourceFile assignments for the same copy operations. Reuse that form. Preserve
call order, input values, checksum invalidation, guards, synchronization, path
admission and public signatures. No new class or generic recipe engine is needed.

## Atom and pattern role

The metadata-copy sequence is one cohesive adapter operation. Retain it locally
rather than proliferate helpers. Each typed assignment preserves the interface
needed by the next operation. It copies identity, path, markers, attributes,
charset and BOM flag before invalidating the stale checksum.

## Reusable recipe and tests

DbBuild uses the existing M3HashPinnedJavaSnapshotRecipe, with exact original
and corrected Java LST templates for both production and test sources. Execution
root is m3/tooling/migration-recipes.
The existing DbRecipeTest gains repair/replay and direct JDK-recipe behavior
tests using a small test-only pinned fixture. All six prior tests remain.

This is a compile repair; an uncompilable original is not a runtime oracle.
Actual metadata behavior must be exercised on the compiled recipe. No fake
OpenRewrite stubs or dependency substitutes count as proof.

## Admission boundary

The unchanged DB implementation has an executed51-test pass at271f76e.
Its measured coverage is663/681 lines and235/241 branches, both below0.99.
That remains a separate rejection, not a reason to alter/exclude tests or guards.

No DB production source, POM, workflow, canonical branch or quality threshold
changes in this pass. No full JDK build, JNI parity or whole-programme completion
is inferred. Apply only to the existing draft branch; no merge/rebase/force.
