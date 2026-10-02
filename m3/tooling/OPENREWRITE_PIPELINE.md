# M3JDK21 OpenRewrite convergence pipeline

## Single entry point

`com.m3.rewrite.M3Java21ConvergenceRecipe` is the canonical Java 21 FILE-local convergence DAG.

The declarative OpenRewrite alias is:

`com.m3.java21.Convergence`

The execution order is:

```text
inventory
  -> atomization
  -> patternization / IOP
  -> documentation
  -> OpenRewrite cycle
  -> fixed point
  -> Maven/JUnit/JaCoCo proof
  -> serial promotion
```

## Admission law

The pipeline is not populated by scanning the classpath.

A recipe enters the DAG only when all of the following are true:

1. the semantic family has a bounded Java 21 fixture domain;
2. its behavior and complete observable contract are stated;
3. the recipe has a deterministic scope classification;
4. FILE recipes remain independently applicable to one file;
5. JUnit proves positive, negative, malformed, already-applied and fixed-point behavior;
6. relevant transformed fixtures compile with `--release 21`;
7. behavior-sensitive fixtures execute before and after when practical;
8. JaCoCo satisfies the configured 99% gate for admitted atom/pattern infrastructure;
9. the recipe is registered in `M3Java21RecipePipeline`;
10. the pipeline manifest and declarative OpenRewrite resource agree with code.

No unproved recipe is added as a no-op placeholder merely to make the DAG look complete.

## Scope escalation

Recipe authority follows:

```text
FILE -> VISIBILITY -> PACKAGE -> MODULE -> MULTI_MODULE -> LIBRARY_API
```

Atomization, patternization/IOP and documentation begin at FILE scope whenever the external contract
remains sealed. Scope is promoted only when the transform actually crosses a boundary.

A repository with one million independent FILE candidates still has one million FILE transforms;
repository size does not itself justify PACKAGE, MODULE or LIBRARY authority.

## Recipe-family expansion

Each new semantic family should contribute the smallest useful set of recipes:

```text
M3Inventory<Family>Candidates
M3Atomize<Family>Recipe
M3Patternize<Family>Recipe
M3Document<Family>Recipe
M3<Family>ConvergenceRecipe        # optional family composite
```

The top-level pipeline then admits those recipes in deterministic stage order.

Examples of future families are pure primitive expressions, null-safe local guards, local constant
normalization, known collection idioms, known String/M3Index bridges, compiler quick fixes and
contract-preserving JDK backport adaptations. Each family needs its own proof; this document does
not pre-admit them.

## Build truth

The trusted recipe build is:

```bash
mvn -B -ntp -f m3/tooling/migration-recipes/pom.xml clean verify
```

A successful build proves only the currently registered recipe set. It does not prove unregistered
future recipes, and it does not by itself authorize canonical source mutation.

## Backport integration

The JDK 22-27 donor inventory is upstream of this pipeline.

```text
complete upstream inventory
  -> Java21 compatibility proof
  -> exact donor patch OR reusable OpenRewrite adaptation recipe
  -> bounded JUnit proof
  -> recipe registration when mechanically reusable
  -> convergence DAG
  -> CI proof
  -> serial promotion
```

Repeated adaptation logic must move into reusable OpenRewrite recipes rather than being hand-edited
across many files.
