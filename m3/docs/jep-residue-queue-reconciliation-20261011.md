# Released-JEP residue queue reconciliation — 2026-10-11

Baseline: `260636298a320a5f11be4c6c8b6a74efc12a8cc6`
Branch: `aix/repair-jep-residue-queue-evidence-20261011`

## CI failures

The backport admission workflow's unit suite has three failures in
`m3/backports/test_jep_residue.py`:

1. `test_detects_packet_and_recipe_class_evidence` builds a temporary root with
   only JEP 458's packet and JEP 485's Java recipe class, but asserts entries for
   JEP 423, 484 and 523 that are not in the test catalogue. It also asserts both
   `PACKET_EVIDENCE` and `RECIPE_CLASS` for JEP 485, contradicting the
   `jep_residue.py` state precedence.
2. `test_live_repository_queue_matches_released_pending_denominator` expects
   JEP 467 receipt state `PACKET_READY`, but its current packet receipt says
   `PROOF_REPAIR_REQUESTED` and requires repaired 250-file atoms plus deletion
   evidence before build/fixed-point proof.
3. `test_live_repository_queue_file_matches_current_tree_evidence` detects
   that `JEP_RESIDUE_QUEUE.tsv` no longer matches the deterministic output of
   `jep_residue.py`. The current tree now includes `m3/backports/recipes/j423`
   in addition to `jep-423-region-pinning`, and other evidence/receipt fields
   have advanced.

## Repair contract

- Correct the temporary fixture's assertions to match the exact evidence it
  creates: JEP 458 is `MATERIALIZED_PACKET`; JEP 485 is `RECIPE_CLASS`.
- Keep packet detection, receipt precedence, and fail-closed duplicate checks
  unchanged.
- Change the JEP 467 expectation to the exact current receipt state.
- Regenerate `JEP_RESIDUE_QUEUE.tsv` from the checked-in catalogue, priority
  matrix, current packet receipts and recipe classes. Do not hand-edit queue
  rows or promote any candidate.
- Preserve the 42-item released/pending denominator and
  `NOT_AUTHORIZED` promotion boundaries.

## Verification

Run `python3 -m unittest -v m3/backports/test_inventory.py
m3/backports/test_jep_residue.py`, compare regenerated queue bytes with the
checked-in file, run `python3 m3/backports/verify.py --root .`, and then the
backport Maven recipe tests. The queue is evidence-only; no JEP is promoted by
this repair.
