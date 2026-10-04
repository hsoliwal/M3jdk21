# TypeFix: publish the saved compiler repair

Pinned repository base: 81f1aeb9b88408a763601982806f9c46f917ba82.
Existing owner blob: 828724689638268574c2b599b1bfcef7921a2f90.

This restores the already prepared two type-witness correction to the real upstream ancestry.
It does not merge the selected-source export history and does not create a second recipe engine.
The complete original and candidate were read and their Git/SHA-256 identities checked.

## Contract and semantic atoms

The existing metadata-copy atom must keep a SourceFile receiver after generic Tree.withId and
Tree.withMarkers. Both calls receive explicit SourceFile type arguments; existing invocation
order, arguments, declarations, source admission and metadata/checksum behavior remain unchanged.
Pattern: immutable-copy adapter. IOP role: typed source replay. Production edit authority is FILE;
recipe fixtures/tests and this task documentation form the module-owned proof composition.

The named recipe is com.m3.rewrite.backport.TypeFix. Its paths are relative to
m3/tooling/migration-recipes, not the OpenJDK root. It reuses M3Jdk21HashPinnedSnapshotRecipe.
A broken compiler cannot execute its own repair: the two-witness source bootstrap is explicit;
real OpenRewrite/JUnit replay must subsequently prove the source-sealed transformation.

## Verification

Fresh local syntax/shape probe: PASS, 3 sources, 22 retained declarations, 140 retained invocation
shapes, exactly two added type witnesses. This is NOT framework type-linking or JUnit execution.
Maven is absent and external dependency acquisition fails DNS in this local environment.
No test stubs, warnings suppression, exclusions, dependency changes or lowered coverage gates.

The new focused hosted workflow must compile actual dependencies, run the five TypeTest cases,
and run the complete existing migration-recipes verify lifecycle with its unchanged coverage
configuration. Missing/skipped reports fail. Keep the PR draft until executed results exist.
The full modified-JDK/JNI/build/jtreg/atom-pattern/JEP and module-pack programme remains separate.

## History

Documentation -> sealed recipe/tests -> exact source materialization. No root POM edit, default
branch update, force-push, squash, rebase or automatic merge.
