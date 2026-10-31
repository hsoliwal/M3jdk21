# Direct-copy review ledger reconciliation — 2026-10-11

## Observed mismatch

The intake verifier derives its review set from `name-mapping.json` records
whose provenance says `copied_code=true`, with a Synexia source and at least
one `src/**` target. Current master has 23 such records; the direct-copy review
ledger has 22. The only missing record is
`synexia.counterpart.MIndexRegexTrigramQuery.sparseLookup`.

Its mapped target is `src/java.base/share/classes/jdk/internal/mindex/M3TQ.java`.
The record remains subject to explicit license/reimplementation review; this
ledger entry does not authorize copying or promotion.

## Minimal correction

Add exactly the missing mapping ID and target path to
`m3/docs/synexia-direct-copy-review.tsv`, preserving `REVIEW_REQUIRED` and
both resolution alternatives: rightsholder-authorized compatible relicensing
or target-owned reimplementation without incompatible-license commingling.

## Verification

Run `m3/runtime-integration/verify-synexia-intake.py` and the intake-policy
workflow. Expected ledger cardinality is 23, matching the copied-code
inventory exactly.
