<!-- SPDX-License-Identifier: Apache-2.0 -->
# Fold-audit lane 4d: first correctness group (2026-11-07)

This is the first pass over the correctness-priority leaves in 0e's fold-audit (PRs closed as folded with hunks
missing from master). DECISION.tsv records each verdict.

- **Number parsing** (L0046, L0047, L0058, L0059, all from #2182): PRESENT_EVOLVED. Master atomized and renamed the
  same logic, so the audit's lexical check reports it missing.
- **L0062** (`M3PatternQuery`, #1985): master already goes beyond the retained copy everywhere except one routing hunk.
  When the 64-unit lookahead finds no 8-unit ASCII run, the input tail is checked before falling back to the rolling
  kernel. This hunk is restored. Answers are identical, and the targeted shape is about 3x faster (GATES.tsv).
