# Exact current canonical mapping validation — C0 / V03

The unchanged canonical `m3/migration/migration.py` and schema pass against the exact receiving commit `c0a14387009aefc7d62bd3268055d526e04f9074`, root `491bbd80a51483d0710fc12c1bad3a037b385ba0`. The result is `MIGRATION_MANIFEST_VALID completion=INCOMPLETE`, exit 0. All acceptance remains blocked.

The fixture contains 50 files: exactly 44 required artifacts, the unchanged canonical validator/schema, and the four frozen current mapping images. The artifact closure is 30 unique implemented target/recipe paths, one tested receipt, and 13 additional selected-run input/stdout paths. No historical command or test was rerun.

## Immutable acquisition and reuse

`prepare_fixture.py` reconstructs both captured root trees and every old/current recursive m3 tree object. It verifies exact commit-to-root and root-to-m3 identities. The src subtree is identical in both root trees, proving that old exact d1 commit/path captures still resolve to the same Git blobs at C0. Each reused m3 body has the same path, mode, size and Git blob in the fully reconstructed C0 tree.

43 artifact bodies reuse immutable prior captures. The sole changed required body is the TQ recipe manifest, freshly read at exact C0: Git `777c79742b81d3fce8ad1f40e7a52e7d09474fde`, SHA-256 `3b195d75e2fdc32ac31b665c5541bd07d3ffdb11698c499bccfd7bc4c1b4e338`. Every body is decoded from a complete connector envelope and its Git blob hash recomputed. The map and six local inputs are checked against the exact V03 epoch and C0 tree. All declared target, recipe and selected receipt hashes agree; no declared metadata was repaired.

`ACQUISITION.json` records all 44 provenance rows, all hash comparisons and no mismatches. `FIXTURE_MANIFEST.json` pins every fixture byte. `REQUIREMENTS.json` is derived from the current map, and the exact selected receipt closure is reconstructed from the proven current receipt.

All prior d1 evidence remains unchanged in the separate `current-map-validation` packet. `prior-evidence/` additionally retains its packet/acquisition receipts, original environment failure, subsequent two TQ-drift diagnostics, and the old TQ manifest capture. Together with `calls/reused/`, all 44 prior captures remain available in this packet. The current pass does not relabel the prior failure.

## Canonical execution

`execution/baseline-v02/receipt.json` records exit 0, unchanged fixture, no diagnostics, the exact command and input hashes. `runtime.json` records the existing interpreter and jsonschema dependency closure; no package installation occurred. The validator and schema are byte-identical to the d1 fixture.

Run the same validator against future four actual recipe results with:

```sh
python3 -B run_validator.py \
  --python /absolute/path/to/existing/jsonschema-python \
  --materialized /absolute/path/to/actual-four-result-directory \
  --mapping-resources /absolute/path/to/actual-v03-binder-output \
  --output /absolute/path/to/new-execution-evidence
```

The result directory must contain exactly the four target paths. The script requires an actual-mode binder receipt and actual source readback packet, matching source/receiving identities and all four before/after hashes. It creates a fresh fixture copy, substitutes only the four actual Result bytes, and runs the unchanged validator with `--previous` pointing to the pinned C0 map. Raw nonzero results remain failures. Evidence is written outside every input directory; both input and execution trees are checked for mutation.

The source publication is still pending. No source-bound afterimage or final acceptance proof is present in this prepared packet.

## Epoch metadata revision 2

The current epoch SHA-256 is `027e47d20c7fb854e028487ceef341c929877a6421dbb75f6433b5cd007fe5ee`. It corrects only an inherited provenance assertion: all 47 original IDs remain ordered, but 46 complete original records are unchanged because the TQ record changed. All 51 other records are unchanged from d1. The 11 frozen operational/guard bodies, and therefore all 50 validator fixture bytes, are identical.

`previous-input-epoch-01/` preserves the complete first V03 sealed packet (`f145e87ddf885a5ceb81b1e0cf81f6040e07cebc399b2e069f65c102bcb54fd8`) and every file it sealed. The fresh `execution/baseline-v02/` run passes under the corrected preparation pin; the earlier receipt and its environment have not been relabelled.
