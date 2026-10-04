# A3 OpenRewrite recipe catalogue

Status: provenance and activation evidence for A3. It is not JDK product mutation authority.

## Official model

OpenRewrite recipes are composable search/refactoring units over Lossless Semantic Trees. Recipes may
be imperative Java classes or declarative YAML compositions. The Maven plugin executes only recipes
that are explicitly activated.

A3 follows that model but adds the M3 contract/scope/provenance gates before activation.

Official references:

- https://docs.openrewrite.org/concepts-and-explanations/recipes
- https://docs.openrewrite.org/recipes
- https://docs.openrewrite.org/reference/rewrite-maven-plugin
- https://docs.openrewrite.org/reference/yaml-format-reference
- https://docs.openrewrite.org/running-recipes/getting-started

The current official catalogue exposes Core, Java, Maven, Search, static-analysis and many other
families. A3 does not equate catalogue presence with JDK suitability.

## Pinned GitHub donor evidence

| Repository | Revision | Observed license | A3 use |
| --- | --- | --- | --- |
| openrewrite/rewrite | fcd2a0896e25d87866ae0d65fe94fa670a3283f4 | Apache-2.0 | core/Java recipe mechanics and API provenance |
| openrewrite/rewrite-docs | 0cf02a34d3ff50c6ce12eb1578ade0010d497bbe | Apache-2.0 | documentation/catalogue provenance |
| openrewrite/rewrite-rewrite | b6677ef2a3480f202673e141950fefdcd72ca42d | Moderne Source Available | REFERENCE_ONLY |
| openrewrite/rewrite-static-analysis | eca2b4c1f8cef1c503b1259e95dc280e2942d08b | Moderne Source Available | REFERENCE_ONLY |
| openrewrite/rewrite-migrate-java | 2881c45316e6d5034edfa24d119d7bce2d301ca2 | Moderne Source Available | REFERENCE_ONLY |
| openrewrite/rewrite-maven-plugin | 647cb5e792fc62e95e0abf54664a77382b4d4062 | separately review before source reuse | orchestration evidence only |

No body from a non-Apache source is copied into M3JDK21 by this catalogue.

## Executable authority

The current executable FILE-local A3 authority remains:

    M3Java21ConvergenceRecipe
      -> M3InventoryPureIntAtomCandidates
      -> M3AtomizePureIntReturnRecipe
      -> M3PatternizePureIntAtomRecipe
      -> M3DocumentPureIntAtomRecipe

Official Apache-licensed recipes such as RemoveUnusedImports, OrderImports, AutoFormat and
FindSourceFiles are catalogued as OPT_IN. They require a separate JDK-specific contract/style proof
before activation. ChangeType is REFERENCE_ONLY because it can cross type/API boundaries.

The Maven AddPlugin recipe is CONTROL_ONLY: Maven is the M3 authoring/proof plane, not the OpenJDK
product build.

## Distribution note

The current OpenRewrite documentation states that recipe releases are moving from Maven Central to
the Code Genome Project. A3 therefore records recipe identities/provenance independently of artifact
resolution and never treats successful dependency download as recipe admission.

## Competitive-programming donors

LeetCode, HackerRank and GeeksforGeeks stay in the separate algorithm-review lane described by A3.
They are used for taxonomy, complexity classes, edge cases and benchmark ideas. Problem/editorial
bodies are reference evidence, not code-copy authority. Any selected algorithm is independently
implemented as a JDK-owned atom and passes Java/JNI/native/JIT/GC contract gates as applicable.
