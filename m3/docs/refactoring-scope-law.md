# M3 contract-preserving atomization, patternization and recipe scope law

Status: execution law for M3JDK21 refactoring recipes.

## Invariant

A source file may be atomized and patternized repeatedly, including repeated decomposition into
smaller semantic leaves and repeated assignment of explicit pattern/IOP roles, while its declared
observable contract and behavior remain unchanged.

The first safe refactoring frontier is therefore the individual file. Contract-preserving FILE
changes are mechanically independent and may be discovered, tested and applied in parallel. A
recipe gains broader authority only when the transformation actually crosses a boundary.

## Scope lattice

The required edit authority is strictly ordered:

1. FILE — private/file-local implementation only.
2. PACKAGE — visibility or package participant relationships cross file boundaries.
3. MODULE — module descriptor, exported package, service or module-owned contract changes.
4. MULTI_MODULE — reactor/dependency edges or coordinated changes span modules.
5. LIBRARY_API — public/protected API or library-level observable contract is intentionally in scope.

Do not promote a FILE transformation merely because a broader scope is convenient. Do not execute a
broader transformation under a narrower scope.

## Recipe-first execution

Every recurring refactor is implemented as a reusable OpenRewrite recipe or a deterministic recipe
primitive used by OpenRewrite. The normal flow is:

    inventory
      -> classify required scope
      -> author/improve recipe
      -> JUnit recipe contract tests
      -> OpenRewrite dry run
      -> candidate diff
      -> lint
      -> compile
      -> tests
      -> runtime/benchmark when required
      -> serial promotion

Independent FILE candidates may fan out in parallel. Canonical promotion remains serial.

## JUnit is the recipe proof gate

A mutating recipe is not accepted because its implementation looks correct. Its unit tests must
prove the recipe behavior on bounded fixtures before repository-wide execution.

At minimum, recipe tests cover:

- positive transformation;
- negative/non-matching input;
- already-applied fixed point;
- idempotent second cycle;
- exact edit-scope fence;
- no mutation outside the declared scope;
- expected public/type surface before and after;
- relevant exception/order/state behavior for the transformed atom;
- malformed or ambiguous inputs fail closed.

For atomization and patternization recipes, the target is at least 99% meaningful JUnit/JaCoCo
coverage of the recipe/atom/pattern implementation, with 100% coverage of declared contract and IOP
role invariants. Coverage is evidence, not a substitute for contract tests.

## Scope promotion examples

A private pure-expression extraction that changes only one file is FILE.

Moving a helper from private to package-private so another class can call it is at least PACKAGE.

Changing module-info.java, exported packages, service providers or module-owned SPI wiring is at
least MODULE.

Changing Maven/reactor dependencies across two modules is MULTI_MODULE.

Changing a public/protected Java API, serialization contract or externally consumed library contract
is LIBRARY_API.

## Enforcement substrate

com.m3.rewrite.scope.M3EditScope, M3ScopeFence and M3ScopedRecipe are the executable scope
vocabulary for OpenRewrite recipe code. Each mutating recipe is progressively migrated to declare
its scope. Existing exact-source recipes remain bounded by their existing source fences until their
scope declaration is added and tested.
