# Resume procedure for future MIndex -> M3 work

Read these files first, in order:

1. `m3/migration/baseline.json`
2. `m3/migration/migration-mappings-v1.json`
3. `m3/migration/COVERAGE.md`
4. `m3/migration/ACCEPTANCE.md`
5. the affected `m3/migration/inventory/*.tsv` shards
6. the recipe record for the capability being changed

Do not restart from names alone and do not assume an older PR receipt proves a newer candidate.

## Enhancement port workflow

For every later Synexia enhancement:

1. Resolve the current source and target heads and preserve the previous synchronized revisions.
2. Diff source from the mapping's `last_source_revision`.
3. Identify affected mapping IDs, canonical owners and dependency closure.
4. Classify each source change as semantic, API, format/ABI, performance-only, documentation-only or test/evidence.
5. Compare the current target against the mapping's `last_target_revision`.
6. If both source and target changed the same contract, stop automatic replay and record an explicit conflict. Use three-way review; never blindly overwrite target adaptations.
7. Update or author the deterministic recipe with exact preconditions, preimage/postimage hashes, refusal behavior, idempotence and rollback.
8. Apply only to admitted source states.
9. Run the route-specific exact-head tests and all affected shared-format/native gates.
10. Update the mapping, inventory disposition, evidence and synchronization pins in the same change.
11. Generate/review coverage and ensure no new discovered capability is silently unmapped.
12. Open a reviewable draft PR. Do not merge automatically.

## Current dependency-ready work

1. Materialize the MatIndex and dictlang row-level inventory shards from their pinned trees. They are currently visible but partial because connector publication was refused.
2. Port/replay Route B compiler lowering into M3 tooling, beginning with the current `MIndexStringCompilerRuntime` and the separately inventoried compiler/Maven-plugin modules. Reconcile open Synexia PR #7568 before freezing postimages.
3. Reconcile current shared-lexicon/native composition work, especially open source PRs #7552, #7559 and #7570, against Route A and PR #6 rather than copying them blindly.
4. Replace Route A's copied mapped-record boundary with a validated retained shared-mapping owner while preserving local fallback and lifetime/corruption guarantees.
5. Complete the Java 21 String API/Unicode/regex matrix for Route A.
6. Rebase or restack Route C only after its source-owner pins are refreshed. Rebuild a complete candidate image and resolve the two enabled StringJoiner failures before any completion claim.
7. Refresh the older `m3/docs/name-mapping.json` to point at the migration manifest when the GitHub write guard permits that in-place update.
8. Publish/enable the executable recipe runner from the sealed `route-a-v1/recipe.json` and rollback preimage when the write guard permits it.

## Evidence discipline

Use one of these states in reports:
- implemented and tested
- implemented but unverified
- partial
- proposed
- blocked
- intentionally excluded

For every build/test receipt record the exact commit/tree, command, environment and skipped gates. Historical receipts stay attached to the revision that produced them.

The current migration is **partial**, not complete.
