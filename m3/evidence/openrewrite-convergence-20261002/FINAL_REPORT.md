# M3 OpenRewrite convergence proof report

## Status

**CANDIDATE / CI infrastructure blocked.**

The Java 21 FILE-local convergence DAG is materialized as:

`M3AtomizePureIntReturnRecipe -> M3PatternizePureIntAtomRecipe -> M3DocumentPureIntAtomRecipe`

with `M3PureIntConvergenceRecipe` as the ordered composite.

## Exact delta

This pass changes only M3 tooling, tests, documentation and CI. It does not apply the recipe to
OpenJDK product source.

The scope registry was also corrected so the retained six-file MIndex joined-chars recipe requires
MODULE authority, and stale `PACKAGE_VISIBILITY` test references now use canonical `PACKAGE`.

## Verification executed

- Git compare against `master`: executed.
- OpenRewrite 8.17.1 source API inspected for composite recipes, comments and Java 21 parser surface.
- GitHub Actions PR workflows triggered.
- Minimal zero-dependency CI runner probe triggered.

## CI blocker

The following runs completed with `failure` and **zero instantiated jobs**:

- M3 CI probe: 36984186301
- M3 OpenRewrite recipe proof: 36984186332
- M3 foundation contracts: 36984186288

Because the probe contains only one Ubuntu job and shell commands, this is a pre-job GitHub Actions
execution/configuration blocker, not JUnit/OpenRewrite failure evidence.

Local Maven proof was not executed: the current execution image has no `mvn`, and its shell cannot
resolve external GitHub/Maven hosts.

## Stop condition

Do **not** apply the convergence DAG repository-wide until an execution environment runs:

```bash
mvn -B -ntp -f m3/tooling/migration-recipes/pom.xml clean verify
```

successfully under Java 21, including JUnit and the 99% JaCoCo gates. Canonical promotion remains
serial and evidence-gated.
