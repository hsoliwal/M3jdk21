<!-- SPDX-License-Identifier: Apache-2.0 -->
# Merge-loss leaf 78: ASCII-run frontiers for the M3 Boolean full-match plan

Origin 38a246c3869 (2026-10-26) taught `M3PatternQuery.BooleanPlan` to consume runs of 8+ ASCII units through the
plan's precomputed ASCII masks, carrying the proved frontier forward instead of re-rolling every unit. A retain merge
kept the first parent, so master still rolls every unit. The plan serves `Pattern.asMatchPredicate()` for straight-line
atom/slice/greedy-star patterns; the engine (`Matcher.matches`) stays authority and the plan only answers when it is
exact.

Restored by a clean 3-way merge. Gates in GATES.tsv:
- the predicate equals the matcher on 480,000 random cases per side, with identical digests master vs leaf;
- 3.5x to 7x faster on long mixed inputs.

Not run: make images or jtreg (stock VM, patched java.util.regex only).
