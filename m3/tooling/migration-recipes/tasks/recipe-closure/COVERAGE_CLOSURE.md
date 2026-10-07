# Scope/atom coverage closure

Status: measured contract-coverage repair for the existing migration-recipe gate.

## Measured starting point

The existing JaCoCo gate requires:

- LINE >= 0.99
- BRANCH >= 0.99

The focused current-owner suite was green but left defensive branches uncovered in
`M3ScopeInference` and `M3PureIntAtomEligibility`.

## Scope cleanup

Inside a branch already guarded by `path.startsWith("src/")`, split-with-trailing-empty semantics
guarantee a second path component. Likewise `test/` guarantees index 1 and `make/modules/`
guarantees index 2. Redundant length conditions therefore create impossible coverage branches
without adding safety.

The repair removes only those impossible length tests. Existing blank-component/refusal behavior is
retained. A new `src/demo/` case exercises the real empty-relative refusal in the generic src
module path.

## Atom fail-closed proof

The eligibility oracle intentionally accepts `J.MethodDeclaration` rather than assuming every LST
is pristine. The new test starts from parser-created Java and immutably corrupts one structural/type
field at a time:

- null method return type;
- non-variable parameter node;
- null parameter type;
- multi-variable parameter declaration;
- duplicate parameter name;
- null atom declaration type;
- int-typed literal carrying a non-Integer value;
- parentheses carrying a non-expression child.

Every case must refuse without mutation or exception.

## Executed proof

Using the exact PR129 proof-kit toolchain (Java 21, Maven 3.9.16, OpenRewrite 8.17.1) and the unchanged
module JaCoCo configuration:

- 40 focused JUnit tests passed;
- 0 failures, 0 errors, 0 skips;
- line coverage: 369/369 = 100%;
- branch coverage: 260/261 = 99.6168%;
- JaCoCo: `All coverage checks have been met.`

No exclusion, threshold, POM, dependency, API, JNI ABI, or OpenJDK product-source change is part of
this closure.
