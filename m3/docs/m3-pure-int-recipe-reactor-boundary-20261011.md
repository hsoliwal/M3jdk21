# Synexia pure-int recipe reactor boundary — 2026-10-11

## CI failure and invariant

The M3 migration-recipe Maven build fails before tests because its unconditional compile dependency is
`com.synexia:synexia-openrewrite-recipes:1.0.0-SNAPSHOT`, which is not published to Maven Central.
The POM's own comment says the full canonical Synexia recipe pack is opt-in, but the dependency is
currently declared outside the opt-in profiles. The source snapshot in `m3/vendor/synexia` is also
not a complete build of that full pack: the 14-file snapshot includes recipes that import additional
`atomdb` and `sealed` classes not present in that subset. It must not be misrepresented as the full
artifact.

## Bounded repair

Introduce a distinct reactor artifact, `com.synexia:synexia-openrewrite-pure-int-recipes:1.0.0-SNAPSHOT`,
whose compiler includes only the seven inventoried pure-int atom classes from the existing, pinned
Synexia vendor source snapshot. This is a separate, explicit recipe crate; it does not claim to package
the complete canonical Synexia recipe project.

The migration-recipes module depends on this bounded local artifact for the current pure-int consumer
test. The full `synexia-openrewrite-recipes` dependency remains confined to its existing opt-in plugin
profiles. No recipe Java source is copied, edited, or renamed by this change; the module compiles the
already-vendored source files in place.

## Exact included source paths

All seven paths are under
`m3/vendor/synexia/synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/atom/`:

- `M3AtomizePureIntReturnRecipe.java` — Git blob `1e27f9dd496c099f27caa253d1518a125a39fd53`
- `M3DocumentPureIntAtomRecipe.java` — Git blob `1d36b554449d2d5c0a0d8eab926c07fc968ed7a3`
- `M3FileAtomCandidateTable.java` — Git blob `79e1c509b21be188605e45b345d08d37e2747a59`
- `M3InventoryPureIntAtomCandidates.java` — Git blob `2b2744e36bcb588a739b8f45741f82a1ffc482fb`
- `M3PatternizePureIntAtomRecipe.java` — Git blob `dad92691710df9255912ba405a68f0e98082c426`
- `M3PureIntAtomEligibility.java` — Git blob `1facba993720c6d1c69cfe21b13321140ebee2e8`
- `M3PureIntConvergenceRecipe.java` — Git blob `b11aff3ace8e77684cfa8cc13fbbb445a05c8a09`

## Verification requirements

1. Maven reactor order builds the bounded Synexia pure-int artifact before migration-recipes.
2. Java 21 compilation and the canonical recipe consumer tests pass.
3. Existing source-snapshot SHA pins and unknown-drift refusal tests remain unchanged.
4. Full canonical Synexia profile activation still fails closed unless its actual upstream reactor/artifact is installed.
5. No target JDK source file or public API is changed by this dependency repair.
