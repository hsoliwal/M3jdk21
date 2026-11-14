<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# NAMING_VAR_FIX v1: m3/m3-import tests compile again (2026-11-07)

## Defect

On master `aa4c65c3701f5bb3c36f1f6e3bb5292d2f566c83`, `mvn -f m3/m3-import/pom.xml test-compile` failed with
24 errors, all `cannot find symbol: variable m3` ([ERRORS_BEFORE.tsv](ERRORS_BEFORE.tsv)): 1 in
`M3ImportCoverageTest`, 4 in `M3ImportPlanTest`, 19 in `M3ImporterTest`. The tests declare the donor-root
variable `Path synexia` (also a record component, read as `fixture.synexia()`) and call methods on `m3`.

## Root cause

Commit `fe35ea98841157594d9aeac71c734b2476d07da4` (JDK naming: rename every Synexia name, recipe
JDK_NAMING_RENAME) moved `m3/synexia-import` to `m3/m3-import` and applied the text rule `SEGMENT_DOT` of
`m3/tooling/jdk-naming/NAMING_RULES.tsv` to Java code. The rule is meant for a dotted name segment
(`com.m3.<word>.importer`, a schema id) and matches the bare word when a dot and a letter follow it. In Java
that is exactly a method call on a variable of that name: `<word>.resolve(..)` and `<word>.toString()` became
`m3.resolve(..)` and `m3.toString()`, while the declaration `Path <word> = ..` and every use as an argument
(`verify(<word>, ..)`, `git(<word>, ..)`) kept the old spelling. `git log --follow` shows no other commit on
these files since the rename; the pre-rename module (`06495fea415`, `m3/synexia-import`) compiles and its
tests run.

`jdk_naming.py plan --only m3/m3-import/` reports `moves=0 rewrites=0` (fixed point): the recipe has no rule
for a bare identifier (the bare word names the donor in prose and is kept on purpose), so it cannot produce the
consistent state. This recipe does it the M3 way.

## Recipe

`apply.py` renames every Java identifier token of that name to `m3` in the files of
[EDITS.tsv](EDITS.tsv): declarations, uses, record components and their accessors (63 tokens in 3 files).
String literals, text blocks, character literals and comments are data and keep their bytes, so fixture
directory names do not change. Hash-pinned (preimage blob + sha256, postimage sha256, token count), refuses
drift and a file that already declares `m3`, idempotent; `--check` recomputes each postimage from the pinned
blob. A token-level diff of the result removes 63 times the old word and adds 63 times `m3`, nothing else.

```text
python -I m3/m3-import/qualification/naming-var-fix-20261107/apply.py           # CHANGED x3, then changed=0
python -I m3/m3-import/qualification/naming-var-fix-20261107/apply.py --check   # CHECK files=3 failed=0
```

## Census of the same pattern

`census.py` (HALF_RENAMED_IDENTIFIER v1) scans every Java blob under `m3/` that names the bare word or uses a
simple-name receiver `m3.` and reports methods that declare one spelling and use the other, plus files that use
`m3.` with no `m3` declared. On master it found 9 METHOD rows in 4 files ([HITS.tsv](HITS.tsv)); their
24 use lines are exactly the 24 compiler errors. One row is outside this module and stays open:

| file | method | uses |
|---|---|---|
| `m3/tooling/migration-recipes/src/main/resources/jdk/internal/m3/rewrite/hash-pinned-java/m3-full-delivery-a3/M3ImportPlanTest.java` | `fixture` (line 417) | 444-447 |

It is a hash-pinned resource (a recipe's expected Java), not compiled by any module; a fix must reseal its pin.

## Test results (gates in [GATES.tsv](GATES.tsv))

The tests had not compiled since the rename, so no master result exists. After the fix: 50 run, 43 pass,
4 failures, 3 errors. The pre-rename module at `06495fea415` ran 50, 49 pass, 1 error (its seed snapshot test
reads vendored files that the sparse checkout did not hold). All 7 are further output of the same rename, which
changed Java strings but not the data they are checked against; they are open, not part of this fix:

| test | cause |
|---|---|
| `M3ImportPlanTest.plansFullDeltaAndStagesOnlyChangedCandidates` (5 expected, 4) | `M3ImportPlan.appendStale` still walks the literal vendor root ending in the old word (rule `SEGMENT` needs a following `/`), the fixtures write under `m3/vendor/m3/`, so the stale row is missed |
| `M3ImporterTest.producerAndReceiverShareKnownDeliveryRootVector` | the pinned delivery-root sha256 was computed over the old vendor path; the path was renamed, the vector was not resealed |
| `M3CanonicalFamilyPolicyTest.checkedInReceiverTableMatchesJavaClassifier` | `m3-canonical-family-receiver.tsv` header keeps `canonical_synexia_owner` (TSV is lineage, outside the rename) |
| `M3RecipeOwnershipPolicyTest.checkedInIntakeIsBoundToExactM3RevisionAndBlobs` | intake marker keeps `M3_SYNEXIA_RECIPE_OWNERSHIP_INTAKE_V1` |
| `M3RecipeOwnershipPolicyTest.checkedInM3LedgerAuditsEveryCurrentRecipeSource` | borrowing ledger header kept, parser expects the renamed header |
| `M3CanonicalRecipeHandoffTest.checkedInHandoffPinsCurrentCanonicalM3RecipeAssets` | handoff TSV schema kept, parser expects the renamed schema |
| `M3SeedSnapshotTest.checkedInSeedManifestMatchesEveryVendoredM3File` | `m3-seed-export.tsv` keeps the old source paths, the mirror check expects renamed targets |
