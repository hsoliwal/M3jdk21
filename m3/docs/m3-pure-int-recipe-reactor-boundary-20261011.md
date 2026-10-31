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

- `M3AtomizePureIntReturnRecipe.java` — Git blob `63adecfa799fcd52a73df02f6020103f066f109c`
- `M3DocumentPureIntAtomRecipe.java` — Git blob `ed5c1afbaa791806f31c99d7eb9aa0700869690f`
- `M3FileAtomCandidateTable.java` — Git blob `27cc32e135dae61eae42b2fa7cfa27b3bd93b4aa`
- `M3InventoryPureIntAtomCandidates.java` — Git blob `b13c56d6a57cd110261eb4f5425afd4396fab2cc`
- `M3PatternizePureIntAtomRecipe.java` — Git blob `c408b37be59691131f41a9c2f4566e2dcb912929`
- `M3PureIntAtomEligibility.java` — Git blob `69fff237eb8b943c33a6ec154f9a1b6edba63481`
- `M3PureIntConvergenceRecipe.java` — Git blob `7dc6cea0a969a1bd094e324c47a1bb724b5ff1dc`

## Verification requirements

1. Maven reactor order builds the bounded Synexia pure-int artifact before migration-recipes.
2. Java 21 compilation and the canonical recipe consumer tests pass.
3. Existing source-snapshot SHA pins and unknown-drift refusal tests remain unchanged.
4. Full canonical Synexia profile activation still fails closed unless its actual upstream reactor/artifact is installed.
5. No target JDK source file or public API is changed by this dependency repair.
