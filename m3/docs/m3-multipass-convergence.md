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
| 0 | inventory | FILE | no | fingerprints + atom candidates + semantic index |
| 1 | file-atomization | FILE | yes | independently tested atomization recipes |
| 2 | file-patternization | FILE | yes | admitted pattern/IOP role recipes |
| 3 | file-documentation | FILE | yes | derivable semantic/Javadoc recipes |
| 4 | file-fixed-point | FILE | no | complete convergence DAG dry-run yields zero changes |
| 5 | visibility | VISIBILITY | no by default | declaration visibility inventory |
| 6 | package | PACKAGE | no by default | deterministic package boundary roots |
| 7 | module | MODULE | no by default | attributed source type relationships |
| 8 | multi-module-fan-in | MULTI_MODULE | no | canonical M3IndexDB graph + recipe DAG evidence |
| 9 | library-api-admission | LIBRARY_API | no by default | deterministic exported API roots |
| 10 | proof | LIBRARY_API | no | diff -> lint -> compile -> tests -> runtime contract |

A later pass does not retroactively widen an earlier recipe's authority.

## FILE fixed point

The FILE frontier is explicit and ordered:

    inventory
      -> atomization
      -> patternization / IOP
      -> documentation
      -> fixed-point dry-run

Each mutating leaf is independently JUnit-proven. The final dry-run executes the trusted composite
recipe from the staged postimage and must produce no source change:

    T(file) = file

or fails closed because the recipe:

- creates/deletes/renames the source;
- attempts broader authority;
- does not converge inside the bounded pass count;
- violates the declared behavior/contract mode.

A repository may execute many independent FILE fixed points concurrently.

## Mavenized whole-JDK bootstrap

OpenJDK's configure/make build remains the product build. Maven is the M3 control shell.

The opt-in profile:

    mvn -B -ntp -f m3/pom.xml \
      -pl tooling/migration-recipes -am \
      -Pm3-jdk-source-convergence verify

walks the original `src/**/*.java` tree and processes every file independently. It never writes
`src/`. Changed postimages and receipts are emitted under the migration-recipes Maven
`target/m3-jdk-source-convergence` directory.

The receipt records pre/post SHA-256, whether atomization/patternization/documentation changed the
file, fixed-point status, HOLD reason and candidate path. Rows are sorted before a semantic root is
computed, so parallel scheduling cannot change the receipt identity.

Backport `apply` may consume a Java target only when the target's current preimage matches a
non-HOLD SOURCE_CONVERGENCE row with `fixedPoint=true`. The product delta is still separately
reviewed and verified; baseline convergence is a prerequisite, not semantic equivalence proof.

The convergence receipt may then be materialized into a detached worktree with:

    python3 m3/backports/materialize_source_convergence.py \
      . \
      m3/tooling/migration-recipes/target/m3-jdk-source-convergence/SOURCE_CONVERGENCE.tsv \
      /tmp/m3-normalized-jdk \
      --copy

Only fixed-point rows are admitted. Changed Java candidates replace their exact preimages in the
detached worktree; unchanged files retain the canonical bytes. The tool writes
`m3-normalized-baseline.tsv` plus a semantic root. Canonical source remains untouched.

For absorption, the same Java-21 convergence executor can be run against a newer-JDK donor checkout.
A donor Java file that parses and converges under the Java-21 recipe set is eligible for normalized
comparison. A file that requires post-21 syntax/type-system semantics becomes HOLD and remains in the
language/incompatible review lane. Thus normalization may simplify compatible donor/target diffs
without pretending incompatible language features are Java-21 compatible.

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

    baseline FILE convergence
      -> inventory
      -> classify
      -> dependency closure
      -> materialize patch/recipe
      -> bounded scoped apply
      -> proof
      -> serial promotion

Language/source-semantics changes remain outside the Java 21 compatibility baseline unless
explicitly unlocked.
