# Migration recipe test-contract repairs — 2026-10-10

Baseline: `e617717424bd0af23881fb383777ae14dc7c8bb4`
Branch: `aix/repair-migration-test-contracts-20261010`

## Inventory findings

CI run 38041788663 reports two independently reproducible test-contract mismatches with narrow fixes:

1. `M3SynexiaFullDeliveryA3RecipeTest` reparses a generated Markdown `PlainText`
   postimage through `JavaParser` during the fixed-point pass. The text snapshot
   recipe correctly requires OpenRewrite `PlainText`; the test helper must choose
   a parser by source-path type so the fixed-point assertion exercises the same
   contract as production.
2. `M3SynexiaCanonicalRecipeConsumerTest` assumes the activated YAML recipe's
   child is directly the Java class `M3PureIntConvergenceRecipe`. OpenRewrite
   wraps YAML-defined recipes in `DeclarativeRecipe`. The test must inspect the
   nested recipe graph for the canonical implementation while retaining the
   activation count and tag assertions.

## Scope and invariant

Only these test helpers/assertions are changed. Recipe implementations, hash pins,
production behavior, APIs, target policy, and promotion gates remain unchanged.
The fixes must not suppress exceptions, relax drift checks, skip fixed-point
replay, or accept a missing canonical recipe.

## Verification

Run the two focused JUnit classes first, then the migration-recipes Maven module.
The complete module is expected to continue exposing unrelated known failures:
source-bound donor hash drift, invalid text manifest rows, exception-type
assumptions, collection corpus pins, and the ownership/policy inventory.
Those remain separate leaves and are not claimed fixed by this change.
