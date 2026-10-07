# DbMark: inherited metadata and scheduler provenance

The exact head 565bc02094731fbe0dcf3a2fff75706be8d0a60b passed all 61 database
JUnit tests and both unchanged 0.99 coverage gates. The separate focused recipe
job 111389766948 (run 37186615402) compiled the real recipe and executed ten
tests, nine passing and one failing on whole-marker equality.

The scheduler adds RecipesThatMadeChanges. OpenRewrite v8.17.1's published
Markers, BuildMetadata and RecipesThatMadeChanges implementations confirm this
is an additional provenance record, not loss of the input metadata. The test
must not require its absence or change production to remove it.

## Contract and allowed delta

Change only DbRecipeTest and existing-engine recipe resources/documentation.
Seed a nonempty BuildMetadata marker. Require its identifier and payload to
survive, require the original ordered marker collection after filtering only
the scheduler's provenance type, and require exactly one provenance marker
with the expected recipe stack. Keep source identifier/path, attributes,
charset/BOM, checksum invalidation, exact Java text and fixed-point assertions.
All ten tests remain; add one exact-current-preimage repair/replay test.
No database, production recipe, POM, workflow, dependency or threshold changes.

DbMark is a named crate using the unchanged M3HashPinnedJavaSnapshotRecipe.
It owns the exact current test preimage and reviewed output. DbBuild's current
output is synchronized while its original preimage remains immutable.
Execution root is m3/tooling/migration-recipes. Actual engine execution, not
static snapshot identity alone, is the acceptance criterion.

## Broader obligations

The separate all-recipe run 37186615413 executed 77 tests with 12 failures.
One is this marker expectation; eleven concern JEP458 attribution, verbatim
exception wrapping, a stale scope count, strict-crate introspection and joined
chars fixture drift. They remain explicit bounded work items, not infrastructure
failures or permission to weaken their checks. This pass does not certify the
whole recipe module, Maven reactor, modified JDK, JNI or all backports.
