# M3 recipe-atom evidence and orchestration DAG

Status: executable architecture contract for M3JDK21 backport/refactoring work.

## Primary invariant

A source file may be atomized and patternized repeatedly, without a practical iteration limit, while
the externally observable contract and behavior remain unchanged.

That makes the first refactoring pass mechanically parallel at FILE scope.

Authority expands only when the transformation actually crosses a boundary:

```text
FILE
  -> VISIBILITY
  -> PACKAGE
  -> MODULE
  -> MULTI_MODULE
  -> LIBRARY_API
```

A larger repository does not imply a larger edit scope.

## Recipe-first law

For recurring work, the reusable recipe is the work product.

```text
requirement
  -> documented contract
  -> semantic atom
  -> pattern / IOP role
  -> OpenRewrite recipe
  -> recipe JUnit
  -> fixed point
  -> mechanical application
  -> compile / jtreg / runtime proof
```

A recipe atom is not admission-complete merely because its source mutation is small. Its companion
evidence row must identify:

- the atom id;
- the contract reference;
- the documentation reference;
- the pattern;
- the IOP role;
- the JUnit proof owner;
- `fixed_point_required=true`.

## Packet split and fan-out

One backport can be decomposed into independent recipe atoms.

Example:

```text
file-delta
   |-------------------|
   v                   v
java-leaf           text-leaf       <- FILE, parallel
   |                   |
   +--------+----------+
            v
       package-join                <- PACKAGE, explicit promotion
            |
            v
        recipe-junit
            |
            v
 diff -> lint -> compile -> jtreg -> runtime
            |
            v
       serial promote
```

The package join is not allowed to inherit PACKAGE authority silently. The packet row must declare
`scope_promotion_approved=true`.

## Framework-neutral authority

The canonical semantic authority is the Java `M3RecipeDag` plus its TSV input.

Camel, Airflow and Drools are projections:

- Camel can execute topological layers and parallelize independent FILE atoms.
- Airflow can represent exact packet dependencies and schedule independent atoms concurrently.
- Drools can block admission/promotion based on evidence and verification facts.

They are not allowed to rewrite the dependency graph or grant edit scope.

Runnable adapter resources carry an `M3-DAG-ROOT` receipt. JUnit compares that receipt to the
current canonical DAG semantic root. Changing the DAG without regenerating/reviewing the framework
projection therefore fails the build.

## Evidence-bound projection

`M3OrchestrationProjectionMain` accepts:

```text
<packet.tsv> <output-dir> [atom-evidence.tsv]
```

When the evidence manifest is supplied it must cover exactly the packet atom set. The output gains an
`atom-evidence.tsv` receipt containing the evidence semantic root.

This lets a CI/workflow engine prove that it scheduled the exact documented/patternized/tested atom
set, not merely atoms that happen to share names.

## JUnit and 99% coverage

The DAG kernel and atom-evidence kernel are both included in the module's 99% line and branch JaCoCo
gate.

Coverage is expected to come from the structure:

- constructor/manifest contract tests;
- missing/duplicate/drifted input tests;
- scope-promotion tests;
- parallel fan-out/fan-in tests;
- semantic-root tests;
- framework projection tests;
- fixed-point recipe tests.

Line execution alone is not the admission criterion.

## JDK build boundary

Maven/OpenRewrite orchestrates M3 tooling, recipes, proof and replay.

It does not replace OpenJDK's native configure/make/jtreg build. The canonical verification tail
remains:

```text
recipe JUnit
 -> diff
 -> lint
 -> OpenJDK compile
 -> jtreg
 -> runtime proof
 -> serial promotion
```

This keeps the JDK build truthful to upstream while making recurring source transformations
deterministic and reusable.
