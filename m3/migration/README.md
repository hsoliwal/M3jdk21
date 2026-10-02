# MIndex → M3 migration control plane

This directory is the durable source-to-target mapping for migrating MIndex/MatIndex/SubMIndex capabilities from the private `hsoliwal/com.synexia` source repository into M3 without blind renames or loss of lineage.

## First-read order

1. `manifest.json` — machine-readable mappings, exact pins, ownership, conflicts, recipes and evidence.
2. `COVERAGE.md` — generated status view; gaps stay visible.
3. `RESUME.md` — exact execution state and next dependency-ready work.
4. `../docs/shared-atom-concatenation.md` — shared-storage/String contract.
5. Route-specific recipe/evidence files named by the relevant mapping.

Do not infer implementation from a discussion name. A newly discovered MIndex/MatIndex/SubMIndex owner must be inventoried before migration. The source repository is private and the destination is public: publish only the minimum source metadata, contracts and provenance required for traceability unless a reviewed migration explicitly authorizes source transfer.

## Validation

```sh
python3 m3/migration/validate_manifest.py
python3 m3/migration/test_validate_manifest.py
python3 m3/migration/generate_coverage.py
```

The validator is dependency-free and fail-closed. JSON Schema is included for editors/external CI; the Python validator is the repository gate.

## Three-way enhancement porting

For each later source enhancement:

1. Read each affected mapping's last synchronized source and target revisions.
2. Diff last-synchronized source → current source and compute dependency closure.
3. Diff last-synchronized target → current target to retain target-only adaptations.
4. Classify API, semantic, format and performance changes. Hashes/fingerprints assist review; they never prove equivalence.
5. Replay a recipe only if exact preconditions still hold. Drift or semantic conflict is a refusal, not permission to overwrite.
6. Run exact-candidate differential/acceptance gates.
7. Update pins, mappings, evidence, conflicts and generated coverage in the same change.

Reverse-porting target changes into Synexia is proposal-only unless separately reviewed. Bidirectional traceability does not authorize automatic two-way mutation.
