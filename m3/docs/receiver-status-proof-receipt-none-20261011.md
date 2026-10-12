# Receiver status proof-receipt correction — 2026-10-11

## Observed invariant violation

After the status-state correction, the merged `receiver-status.tsv` row for
`MINDEX_STRING_RUNTIME` remains:

- target state: `SOURCE_PIN_ONLY`
- proof receipt: `m3/synexia-import/current-full-borrow/string-source-target-map.tsv`

The receiver checker rejects this because a source-only family must have
`proof_receipt=NONE`. The source-to-target map is a mapping/provenance artifact,
not a target implementation proof receipt. Reusing its path in the proof column
silently changes the artifact's authority.

## Minimal correction

Change only the `MINDEX_STRING_RUNTIME` proof-receipt field to `NONE`. Keep
the family, source identity, target state and target surface unchanged. Do not
weaken `check_synexia_full_borrow_receiver.py` or create a fabricated receipt.

## Verification

Run `python3 m3/compatibility/check_synexia_full_borrow_receiver.py` and the
full-borrow receiver workflow. All 32 families must remain present exactly
once and source-only; this does not authorize implementation or runtime
promotion.
