<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Reuse existing JEP source before donor intake

M3JDK21 already inherits substantial OpenJDK implementation. Existing JEP code should enter
the current owner’s atomization/patternization review. The existing donor generator now accepts
an optional exact target comparison so it does not create replacement crates for those files.

This is file custody and review routing. A matching JEP title, class name, existing path or
single matching file does not establish that a whole feature is implemented. Git blob equality
with equal object type and mode establishes exact source identity for that file; it does not
establish dependency closure, compiled inclusion, Java 21 compatibility or runtime behavior.

## Three-way routing

The generator resolves the baseline, selected donor, per-file donor overrides and current target
refs to immutable Git commits before reading trees. It reuses `file_delta_inventory.py` and
the existing candidate generators; no second parser, inventory engine or A3 executor is added.

| Exact selected-file evidence | Route | Next work |
| --- | --- | --- |
| Current and donor are present with equal blob/type/mode | `ALREADY_PRESENT_ATOMIZE_PATTERNIZE` | Preserve current source; review the existing owner and its contract |
| Current equals the locked baseline and donor adds/changes the file | `MECHANICAL_FILE_REPLAY` | Generate the existing source-sealed candidate, then prove compatibility |
| Current differs from baseline and donor | `HOLD_CURRENT_TREE_DRIFT` | Preserve evolved M3 code and reconcile the owning recipe |
| Donor lacks a file which current M3 contains | `HOLD_DONOR_PATH_ABSENT` | Preserve target code; review missing donor lineage |
| Neither donor nor target has the selected file | `NO_DONOR_IMPLEMENTATION` | Record the missing source; absence is never implementation proof |
| A nonregular Git object/mode is selected | `HOLD_UNSUPPORTED_FILE_MODE` | Review source kind and mode without copying it |
| Source kind was not requested | `SOURCE_KIND_NOT_SELECTED` | Record the path and choose the explicit source lane if needed |

Baseline-SAME source is included in existing-source review when it is present and equal in
the target. An absent baseline/target pair is an addition candidate only when the donor has a
real file. Donor removals never silently remove current JDK or M3 source.

## Command

Use an upstream checkout containing the required baseline/donor objects and the actual current
M3JDK21 Git repository. A ref may be supplied for convenience; the output always records its
resolved immutable commit.

```bash
python3 m3/backports/generate_recipe_crates.py \
  --repo /path/to/upstream-jdk \
  --release 22 \
  --donor-ref jdk-22+36 \
  --target-git /path/to/M3jdk21 \
  --target-ref HEAD \
  --paths-file /path/to/reviewed-paths.txt \
  --out /path/to/new-candidate-bundle
```

`--target-git` defaults to `--repo` and requires `--target-ref`. `--all-candidates` can replace
the explicit path list. `--include-native` includes native paths; the existing broader
`--include-text` option retains its meaning. Without `--target-ref`, historical generator
behavior and file formats are preserved. Target-aware output must use a new or empty directory
so an old replacement crate cannot survive a later already-present decision.

## Review artifacts and source identity

`TARGET_REUSE.tsv` records every selected path, source kind, three immutable commits,
presence, modes, Git blob hashes and its routing decision. Each row explicitly keeps
`source_mutation_authority`, `compatibility_authority` and `feature_acceptance` false.

The ledger does not download already-present source merely to hash it a second time.
SHA-256 fields are `NOT_MATERIALIZED` until the existing candidate generator reads the bytes;
the exact Git blob hash still binds those unmaterialized bytes. Generated candidates record
their actual pre/post SHA-256 values and validate their donor blob identity against the plan.
This makes current-source discovery practical in blob-filtered Git checkouts. Worktree edits
are outside the committed target snapshot; source-sealed application must still recheck the
actual target preimage before any mutation.

`A3_EXISTING_SOURCES.tsv` is a review input containing only exact existing Java files below
`src/` or `test/`. It is not a runnable or admitted A3 packet. The existing A3 owner still
requires an authentic mastery receipt, its root and a compatible transformation/contract.
Its current narrow Java transform support is not a claim of arbitrary JEP optimization.
Native and resource paths remain in the full ledger with native/resource owner review actions;
they never enter the Java-only A3 input list.

If every selected source is already present, the command succeeds with these ledgers and an
empty `CRATES.tsv`. It creates no replacement source resources or empty runnable YAML recipe.
Unselected, unsupported-mode, encoding and compatibility decisions remain explicit. Eligible
replay remains candidate-only and retains all existing generator exclusions and source seals.

## Canonical recipe and verification

The reusable change is owned by `hsoliwal/com.synexia`:
`com.synexia.rewrite.M3Jdk21ExistingJepReuseRecipe`, crate
`synexia-openrewrite-recipes/recipes/m3jdk21-existing-jep-reuse-20261017`.
It reuses `M3HashPinnedTextSnapshotRecipe`, with exact target pre/postimages, tests and receipts.
The reviewed target baseline is `970a34350ebdf6a3f1c5ff5acc8445d0f3515f55`.

The behavioral regression command is:

```bash
cd m3/backports
python3 -m unittest -v test_file_delta_inventory.py test_generate_recipe_crates.py
```

Tests retain the legacy generator contract, exercise mixed inherited/added/evolved/missing
sources, mode differences, per-file donor overrides, ref movement, native routing, empty
imports and deterministic repeated output. Recipe proof separately checks actual OpenRewrite
execution, exact output, fixed point and refusal of stale/missing/duplicate inputs.

No native implementation or OpenJDK runtime source is changed by this routing improvement.
Whole-JEP completion, A3 transformation qualification, target build/jtreg/native behavior and
performance remain separate evidence. The existing target CI workflow is extended to compare
its exact target commit while generating released-donor candidate bundles.

Existing OpenJDK and donor notices remain authoritative per file. The Python tooling and
new tests retain their Apache-2.0 declarations with Synexia attribution; the retained workflow
payload follows its existing repository license scope. Packaging a source snapshot in the
recipe does not change that source's license. Reference-only category evidence does not
authorize copying challenge submissions or editorials.
