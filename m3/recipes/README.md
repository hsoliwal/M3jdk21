# Source-bound enhancement replay

`apply.py` retains the original additive installation mode. Every managed source
file must match its SHA-256 pin, and every target runtime/native fence must still
match `unchanged_runtime_files`. Existing destination edits cause refusal.

For an enhancement, provide the reviewed last-synchronized source tree and its
original `m3/recipes/manifest.json`. The current recipe tree supplies the desired
source and manifest. Inspect first, then replay:

```powershell
python m3/recipes/apply.py --target C:\candidate --baseline C:\last-synchronized --check
python m3/recipes/apply.py --target C:\candidate --baseline C:\last-synchronized
```

The recipe compares baseline, current source and target bytes for the union of
their managed `m3` paths and the manifest itself. Each target file must be exactly
the baseline or exactly the desired version. It accepts those exact partial
states, preserves unrelated target files, and refuses target-specific edits
without attempting a semantic merge. A reviewed path removed from the current
manifest is an explicit deletion; preserve its lineage in the migration mapping.
Missing files are accepted only when absence is a valid baseline/desired state.

Rollback requires that same explicit baseline and refuses intervening edits:

```powershell
python m3/recipes/apply.py --target C:\candidate --baseline C:\last-synchronized --reverse --check
python m3/recipes/apply.py --target C:\candidate --baseline C:\last-synchronized --reverse
```

Both manifests must use schema 1 and identical runtime/native fences. This recipe
cannot authorize a String/HotSpot layout change. It rejects unsafe paths and
symbolic links/reparse points on managed paths. It verifies all preconditions
before effects, generates a deterministic binary patch with the installed Git
CLI, runs `git apply --check`, applies, and checks exact postimages. Git history
is not needed in the destination. `--check` leaves its tree unchanged.

This is file replay tooling, not compiler lowering or semantic equivalence.
Publish only authorized sources; a valid manifest does not authorize publishing
private implementation files. Callers must serialize concurrent writers. No
power-loss atomicity or adversarial concurrent filesystem mutation is claimed;
after an interrupted write, rerun only if every managed file matches an exact
baseline or desired state. A torn file causes refusal and needs reviewed recovery.

Run `python m3/recipes/test_recipe.py`. The synthetic three-way tests exercise
real binary Git patches, replay, deletion, rollback, idempotence, exact partial
states, target-only preservation, schema/path checks, and source/baseline/target/
runtime refusal with unchanged target snapshots. The original source-bound tests
also require all pinned runtime fence files in the checkout and current source
hash pins. Refresh pins only after reviewing the complete candidate; refreshing
is not an apply step.
