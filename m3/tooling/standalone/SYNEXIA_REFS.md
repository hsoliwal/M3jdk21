<!--
SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
-->
# Standalone: cross-repository references (SYNEXIA_REFS v1)

User rule (2026-10-31): M3JDK builds, tests, gates and releases without any other repository. This lane
classifies every dependency finding of `check_standalone.py` (#2192) as ABSORB, MOVE, RETIRE, LINEAGE or
HOLD, implements the mechanical cases and records the rest for the next leaves.

## Scope

The first list (21 rows: D_CHECKOUT 2, D_SECRET 1, D_REPO_REF 18) came from a sparse run (1,509 scoped files
scanned, 3,844 not checked out). A full-scope run over all 5,353 scoped files at master `52fb81c4` finds
D_CHECKOUT 4, D_SECRET 4, D_REPO_REF 361 and D_MAVEN 474. Every one of those rows is in
[SYNEXIA_REFS.tsv](SYNEXIA_REFS.tsv) with its class, decision, action, evidence and state
(RESOLVED or REMAINS; NEW for a finding that only the after scan holds), followed by COUNT rows (full scope
and leaf scope, each counted in the before scan and in the after scan).

## Decisions taken here (leaf scope: the 21 listed rows and the 8 rows in m3/release)

| Owner | Decision | What changed |
|---|---|---|
| `m3-a3.yml`, `m3-a3-backport-completion.yml`, `m3-a3-portable-mastery-gate-java21.yml` | ABSORB | The slim A3 recipe export was already absorbed: its 8 sources and manifest sit byte-identically under `m3/vendor/synexia/synexia-openrewrite-recipes/` (9/9 sealed sha256 equal), and the reactor module `m3/tooling/synexia-jdk-a3-recipes` builds the same coordinate. The workflows stop checking out or downloading the donor (and stop reading `SYNEXIA_READ_TOKEN`); they verify the absorbed closure and bootstrap the source-built closure with `install.sh`. The export's 7 unit tests are absorbed too (byte-identical, 16/16 sealed with the closure; provenance in `m3/synexia-import/intakes/jdk-a3-public-export-tests-20261031/`) and run in the module's test phase: 28 methods pass, 1 is a HOLD (`A3_EXPORT_TAG_ASSERTION`: it expects a `synexia` tag that the sealed recipe never carried; the donor's own export build fails the same way). |
| `m3-jep484-classfile-path-map.yml`, `m3-jep484-custody-plan.yml` | ABSORB | Repointed to the absorbed validator `m3/tooling/validators/WorkflowStructureCheck.java`, pinned by Git blob `f61be81f` (the donor bytes), as `m3-jep523-g1-default.yml` already does. |
| `m3-jdk21-second-pass-review-dag.yml` | MOVE | The `upstream-dag` job cloned the donor and ran the donor's own tests with `secrets.M3_SYNEXIA_TOKEN`; those proofs belong to the donor's CI. The local DAG job stays. |
| `m3/release/*`, `m3-first-release-admission.yml` | ABSORB | The `M3_MAVEN_CONTROL_REACTOR` release gate is owned by `hsoliwal/M3jdk21` (`PRODUCT_REPOSITORY`), and its `canonical_recipe.repository` must name this repository. The contract may hand no gate to another repository and every gate receipt must name this one. The policy paragraphs of `m3/release/README.md` and the control-reactor labels of the admission workflow say the same. |
| `m3-hf-intake-20261017.yml` | RETIRE | The synthetic export names its real producer (`$GITHUB_SERVER_URL/$GITHUB_REPOSITORY` at `$GITHUB_SHA`). |
| `m3-merged-atom-preservation-20261022.yml` | RETIRE | The guard on recipe roots stays; its message points to `m3/tooling/<lane>/tools/` instead of the donor. |
| `m3/tooling/migration-recipes/pom.xml` | RETIRE | The opt-in `m3-synexia-recipe-ownership-audit` profile is gone: no workflow activated it, and its recipe class exists only in the donor. |
| `m3-collections.yml`, `m3-jep485-*.yml`, `m3-jep493-*.yml`, `m3/ports/collections/verify_collection_port_wave1.py` | LINEAGE | LINEAGE_LEDGER_REF v1: the donor identity lives once in [LINEAGE_SOURCES.tsv](LINEAGE_SOURCES.tsv) and the checks read it from there. |
| `m3-backport-contract-current-owner.yml`, `m3-original-mapping-provider-qualification.yml` | HOLD | The bytes of both workflows are pinned by binding JSON, and the matched line asserts lineage without fetching anything. A change has to come from the recipe that owns them. |
| retained-variant pom under `m3/qualification/` | HOLD | History record, kept verbatim. |

## Remaining (next leaves)

The REMAINS rows hold no build, test, CI or release fetch of the donor. The live-workflow fetch guard in
`tools/test_refs_ledger.py` finds 0 hits after this change (8 before). By class:

- HISTORY (`m3/qualification/`, kept verbatim), TASK_CRATE (historical task crates under legacy recipe roots),
  RECIPE_FIXTURE (hash-pinned recipe before/after images) and SEALED_BINDING: HOLD.
- LIVE_LINEAGE and OWNER_POLICY: lineage literals and donor-ownership policy strings in live verifiers and
  tests. LINEAGE_LEDGER_REF v1 is the next mechanical recipe.
- OWNER_POLICY in documentation (NEXT, not in the TSV): `check_standalone.py` skips `.md` files
  (`DOC_SUFFIXES`), so donor-ownership wording in docs is neither counted nor listed by the ledger.
  `m3/release/README.md` is rewritten here. Known next: `m3/vendor/synexia/README.md` ("Ownership and
  use": Synexia as the canonical recipe owner). The next leaf runs a docs scan with the same patterns over
  every tracked `.md` file and classifies its rows with these rules.
- DONOR_CHECKOUT_INPUT: `m3/tooling/shared-frontend/eclipse-jdt/prepare_sources.py` reads a local donor
  checkout (ABSORB its inputs or MOVE the tool).
- D_MAVEN 474: all are the `m3-recipe-boot` profile (#2213). #2213 lists 470 poms under `m3/`. Four poms outside
  `m3/` (`.m3/openrewrite-recipes`, `make/m3/recipes`, `src/utils/IdealGraphVisualizer`,
  `src/utils/LogCompilation`) need RETIRE_PROFILE v1 run again over the full tree.

## Tools

- `tools/refs_ledger.py` (REFS_LEDGER v2) builds the ledger from two checker scans and [SYNEXIA_REFS_RULES.tsv](SYNEXIA_REFS_RULES.tsv)
  (first match wins). Both scans are classified, so the after counts come from the after scan and after-only findings
  (regressions, moved files) are listed as NEW. The output is deterministic, and `--check` confirms the ledger is a
  fixed point.
- `tools/apply_patch_recipe.py` with [SYNEXIA_REFS.patch](SYNEXIA_REFS.patch) holds the in-file edits of this
  lane as one structured patch. Each run reports FIXED_POINT, APPLIED or DRIFT.
- `tools/test_refs_ledger.py` covers the rule engine, the fixed point, the ledger consumers (with drift
  discriminators) and the live-workflow fetch guard. CI runs it as `.github/workflows/m3-standalone-refs.yml`.

To re-run, check out every scoped file (sparse patterns `*.yml *.yaml *.sh *.py *.mjs *.js *.properties *.gradle
*.toml *.cfg *.ini pom.xml pom-*.xml /.github/ /m3/release/`), run
`python -I check_standalone.py --root . --tsv after.tsv` against the pre-change tree and the post-change tree,
then run `python -I m3/tooling/standalone/tools/refs_ledger.py --before before.tsv --after after.tsv --rules
m3/tooling/standalone/SYNEXIA_REFS_RULES.tsv --out m3/tooling/standalone/SYNEXIA_REFS.tsv`.
Gates are in [GATES.tsv](GATES.tsv) and corrections in [RECIPE_MEMORY.tsv](RECIPE_MEMORY.tsv).
