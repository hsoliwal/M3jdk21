# JCC CI repair — 2026-10-06

This crate repairs three exact current-master inputs at M3jdk21 commit
b71ee5bf88398fb80961c13db03e8c675fb445ce:

- One missing backslash in each of the two existing A3 regex-memory recipe tests.
- Foundation CI's selection of its already pinned Temurin JDK 21+35 for Maven.

The two Java changes each insert one byte. Every test method, case and assertion
is retained. M3ScopeInference is already repaired on this baseline and is a guard.
The GCC10 package version remains unchanged; its failed package retrieval is a
separate unresolved toolchain obligation.

## Existing owners

The unchanged M3Jdk21HashPinnedTextSnapshotRecipe produces the three afterimages
from their complete hash-pinned preimages. PlainText is required for the two
initially unparsable Java files. Real javac parsing verifies the original failures
and repaired syntax; parsing is not type checking.

The unchanged m3/migration/recipe.py consumes plan.json through its
m3.sealed-install/1 schema. It checks all targets and five dependency guards before
writing and provides check, apply, rollback and recovery. No new installer or
runtime owner is introduced.

The recipe name is com.m3.rewrite.backport.JccCiRepair and its crate is
jcc-ci-repair-20261006. The full three-file diff is CANDIDATE_DIFF.patch.

## Verification scope

JccCiRepairTest declares eight JUnit methods: exact outputs and metadata, fresh
fixed point, named/serialized entry points, missing/drift/duplicate/post-scan
refusals, the original malformed Java literals, and exact manifest coverage.

test_installer.py declares six Python methods covering the actual installer
lifecycle, atomic refusals and foundation environment propagation. The environment
control explicitly mocks archive acquisition, Java and Maven. It establishes
selection and preserved command arguments; it is not a compiler installation or
Maven execution result.

verification/pom.xml is a focused bootstrap with the repository's unchanged
OpenRewrite 8.17.1 and JUnit 5.10.2 dependencies. It compiles two retained recipe
owners and the new JUnit class, avoiding the invalid target tests until the
OpenRewrite transformation has produced their repaired files. The root module
POM and its coverage gates remain intact.

The optional m3.ci.materialized system property writes actual OpenRewrite outputs
from the positive JUnit run to a fresh directory. Compare those files with the
installer output before selecting them for publication. Compile the two repaired
test classes against the actual focused Maven test classpath as a distinct type
checking step. Whole-module verification, original A3 behavioral tests and JDK
runtime acceptance require their own complete input closure and receipts.

## Invocation

Run the focused Maven verification using an admitted JDK 21 and artifact cache:

    mvn -o -B -ntp -f m3/tooling/migration-recipes/tasks/jcc-ci-repair-20261006/verification/pom.xml \
      -Dm3.ci.materialized=/absolute/fresh/proof-directory clean verify

Run the six installer tests against a complete before-image workspace:

    python3 m3/tooling/migration-recipes/tasks/jcc-ci-repair-20261006/test_installer.py \
      --repo /absolute/before-workspace \
      --plan m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/jcc-ci-repair-20261006/plan.json \
      --out /absolute/fresh/installer-proof

Use the retained installer for the actual reviewed transition:

    python3 m3/migration/recipe.py check --root . --plan PATH_TO_PLAN
    python3 m3/migration/recipe.py apply --root . --plan PATH_TO_PLAN

INPUTS.json describes authored inputs and keeps that status historically accurate.
Execution receipts, when present, identify what actually ran. Prior CI observations
and their exact head versus generated-merge scope are retained under prior-ci.

