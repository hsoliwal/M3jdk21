# Full-borrow receiver status self-promotion refusal — 2026-10-11

## CI evidence

The `M3 Synexia full-borrow receiver` workflow fails in
`check_synexia_full_borrow_receiver.py:load_status`:

`initial full-borrow receiver cannot self-promote MINDEX_STRING_RUNTIME: TARGET_MAPPING_PINNED`.

The status ledger row carries the correct source revision, target surface, and
`proof_receipt=NONE`, but its `target_state` was advanced from
`SOURCE_PIN_ONLY` to `TARGET_MAPPING_PINNED`. The receiver checker explicitly
requires every initial receiver row to remain `SOURCE_PIN_ONLY`; the pinned
source mapping is not a target implementation proof and cannot authorize
promotion.

## Minimal correction

Restore only the `MINDEX_STRING_RUNTIME` receiver-status state to
`SOURCE_PIN_ONLY`. Keep the source tree/blob, target surface, and proof receipt
unchanged. Keep any mapping analysis in its own mapping ledger; do not represent
it as implementation status.

The checker remains strict. Do not weaken it to accept `TARGET_MAPPING_PINNED`
or add a proof receipt that does not exist.

## Verification

Run the receiver inventory/status checker and the full-borrow receiver
workflow. Then verify that the source estate still contains all 32 families and
that the receiver has no missing or duplicate family rows. This repair does not
promote any family beyond source-only intake.
