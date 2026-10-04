# M3JDK21 bounded multi-pass convergence

Status: canonical tool-plane execution model.

## Governing invariant

A source file may be atomized and patternized repeatedly while its externally observable contract
and behavior remain unchanged.

Repository size does not force refactoring scope to grow.

Scope grows only when the required relationship crosses the current boundary:

    FILE
      -> VISIBILITY
      -> PACKAGE
      -> MODULE
      -> MULTI_MODULE
      -> LIBRARY_API

Independent FILE work may fan out massively. Canonical truth advances only through serial,
content-addressed receipts.

## Canonical pass sequence

| Pass | ID | Maximum scope | Mutation | Main evidence |
| ---: | --- | --- | --- | --- |
| 0 | inventory | FILE | no | atom candidates + semantic index |
| 1 | file-fixed-point | FILE | yes | tested FILE recipes until no change |
| 2 | visibility | VISIBILITY | no by default | declaration visibility inventory |
| 3 | package | PACKAGE | no by default | deterministic package boundary roots |
| 4 | module | MODULE | no by default | attributed source type relationships |
| 5 | multi-module-fan-in | MULTI_MODULE | no | canonical M3IndexDB graph + recipe DAG evidence |
| 6 | library-api-admission | LIBRARY_API | no by default | deterministic exported API roots |
| 7 | proof | LIBRARY_API | no | diff -> lint -> compile -> tests -> runtime contract |

A later pass does not retroactively widen an earlier recipe's authority.

## FILE fixed point

A FILE transformation is supplied exactly one source file and one FILE-admitted recipe.

The FILE executor repeats the recipe until:

    T(file) = file

or fails closed because the recipe:

- creates/deletes/renames the source;
- attempts broader authority;
- does not converge inside the bounded pass count;
- violates the declared behavior/contract mode.

A repository may execute many independent FILE fixed points concurrently.

## Atomization and patternization

The FILE semantic inventory produces nested behavioral atoms for admitted Java constructs such as:

- declaration;
- assignment / assignment operation;
- binary / unary operation;
- invocation;
- construction / array construction;
- return / throw;
- if / loop / switch / ternary;
- cast / instanceof;
- synchronization / try;
- literals.

Atom identity is normalized-logic based inside its semantic parent. Ordered ATOM edges preserve
execution order.

Every semantic node also carries a first-class M3/IOP pattern role. Patternization is therefore
queryable state, not prose decoration.

## M3IndexDB ownership

M3IndexDB is the sole canonical semantic-index persistence owner.

The semantic graph contains:

    atoms
      -> fields / methods
      -> interface / implementation
      -> file + documentation
      -> package
      -> module
      -> library
      -> project
      -> repository

Each node carries exact, structural, normalized-logic and SimHash signals.

SimHash and logic/structure hashes are candidate signals. They never automatically declare semantic
equivalence.

M3IndexDB recomputes parent fingerprints child-first during fan-in. Producer parent hashes are not
canonical authority.

## Visibility pass

The visibility pass records TYPE/METHOD/FIELD visibility as:

    PRIVATE
    PACKAGE
    PROTECTED
    PUBLIC

Java implicit public semantics for interface and annotation members are normalized explicitly.

The pass records whether a declaration escapes FILE and whether it participates in the exported
library surface.

## Package pass

The package pass groups package/protected declarations from all files in a package.

It produces a deterministic package-boundary root over sorted semantic declarations.

Public/private members do not contaminate the package/protected boundary root.

## Module pass

The module pass resolves source type EXTENDS/IMPLEMENTS relationships only when the target type is
present in the scanned module/source set.

It emits evidence edges; it does not invent unresolved external relationships.

## Multi-module fan-in

The fan-in bridge runs FILE semantic inventory plus module type-relation scanning and persists one
canonical M3IndexDB semantic artifact.

Input file order must not affect the canonical artifact content hash.

Ordered behavioral edges remain ordered; unordered membership is canonically sorted.

## LIBRARY_API pass

The API pass hashes public/protected type/member surfaces by module and then library.

A nested public type/member is exported only when every owning type in its nesting chain is also
exported.

Package-private/private owners therefore cannot leak public-looking members into the API root.

## Proof pass

The proof-plan recipe emits requirements only. It never claims execution.

Mandatory order:

    diff
      -> lint
      -> compile
      -> tests
      -> runtime when required

Compiler/test/runtime results remain external proof authority.

## Serial pass receipts

Each pass iteration has an immutable receipt containing:

- pass ordinal and ID;
- iteration;
- input canonical state root;
- output canonical state root;
- proof root;
- exact implementation commit;
- PASSED / FAILED / BLOCKED;
- changed flag;
- stop-condition flag;
- receipt root.

The append-only pass ledger refuses:

- pass skips;
- iteration skips;
- stale input roots;
- wrong pass IDs;
- false unchanged claims with a changed state root;
- stop-condition success on FAILED/BLOCKED receipts.

Resume starts from the last ledger output root and the next unresolved pass/iteration.

## Recipe retention law

A recipe may be authored and saved as a candidate before proof.

It becomes retained/verified only when its exact commit passes the configured recipe unit tests,
coverage gate and required downstream verification.

Current minimum tooling gates are:

    JUnit behavior proof
    JaCoCo line >= 99%
    JaCoCo branch >= 99%

Do not weaken coverage gates to promote a recipe. Improve the atomization/tests/recipe instead.

## Backport reuse

The same convergence fabric applies to JDK 22–27 assimilation:

    inventory
      -> classify
      -> dependency closure
      -> materialize patch/recipe
      -> bounded scoped apply
      -> proof
      -> serial promotion

Language/source-semantics changes remain outside the Java 21 compatibility baseline unless
explicitly unlocked.
