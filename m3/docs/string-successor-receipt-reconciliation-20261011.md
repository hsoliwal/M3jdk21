# M3String successor receipt reconciliation — 2026-10-11

Baseline: `6087735d58fe7b9790e1a700b1d3b32ac1ddb912`
Branch: `aix/repair-string-successor-receipt-20261011`

## Failure and exact history

The `M3JDK String owner-coordinate runtime` workflow fails in
`test_actual_owner_sources_match_exact_receipts` because
`m3/synexia-import/current-full-borrow/string-target-successors.tsv`
expects Git blob `d99a5080f153c24d3efaf37c30c26f9528563a38` for
`src/java.base/share/classes/java/lang/M3String.java`, while the checked-out
source is `70d1d03c109126fdda36c54da21b484bbe368fd9`.

The old receipt matches the exact blob at commit
`088c333478f99f56b99360eb8367ea8032d6d553`. The current source is the exact
blob at `3c3d66694aaa1b345bbee1c3fe905f1a07ad3906`, which is part of
[M3JDK PR #531](https://github.com/hsoliwal/M3jdk21/pull/531). Its documented
change preserves the flat String fallback when bounded M3 pool admission
refuses a piece of a binary concat. The immediately preceding source history
also includes #531's re-applied A9 equality gates and A6 bounded-pool work.

## Semantic check

The existing successor validator checks the current M3String source for the
owner/value layout, designated join, direct canonical concat, and conservative
literal-regex responsibility. Those markers remain in the current source. The
update changes the exact successor receipt and records #531 in the M3JDK PR
lineage; it does not alter source code, historical target pins, admission policy,
or the approved successor path set.

## Repair rule

- Preserve `historical_git_blob=b646eedfad856c640f727aa99c5142de0f03e8ae`.
- Replace only the stale `successor_git_blob` with the observed exact current
  Git blob `70d1d03c109126fdda36c54da21b484bbe368fd9`.
- Append PR `531` to the existing `m3jdk_prs` lineage; retain Synexia donor
  PR lineage and all other receipt fields.
- Extend the semantic evidence with the bounded-pool refusal / flat-fallback
  invariant. Keep the existing marker validator and fail-closed refusal tests.

## Verification

Run the complete `test_string_phase_successors.py` and
`check_string_phase.py` controls, then the owner-coordinate runtime workflow.
The stacked recipe PR must also be rerun because this receipt is a separate
precondition. No runtime qualification or release pass is inferred from the
metadata correction alone.
