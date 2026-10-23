# Superset branch m3/superset-20261023 (M3jdk21)

Built 2026-10-23 from master by merging every open PR head server side (POST /repos/{r}/merges, no rebase, no
force, no PR merged or closed) in the order of `build_order.py`: default-branch PRs first, GitHub-mergeable before
dirty, oldest first, a PR stacked on another PR right after its base. `SUPERSET_20261023.tsv` is the ledger: one
row per PR with MERGED (merge commit on this branch) or CONFLICT (left out; its row names the track and files).

The conflicts are the fill: 84 PRs were already dirty against master, the rest conflict with PRs merged before
them in this branch (shared owners: m3/collections M3FilteredViews, M3IndexedListSnapshot, M3LongIndexProjection;
m3/docs/name-mapping.json). They are resolved one PR at a time by agents (merge the superset into the PR branch,
resolve as a superset, gate with the module's Maven build, push a side branch, merge that into the superset) and
never by hand-editing shared ledgers (feedback-com-synexia-push-api-fallback).

Merge is the owner's call. Tools: S:/m3-scratch/consolidate (dump_prs.py, build_order.py, superset_merge.sh).

<!-- resolutions -->
## Resolutions (snapshot 2026-10-23 06:29 UTC)

`SUPERSET_20261023_RESOLUTIONS.tsv` is the additive successor of the conflict rows: 52 resolved, 50 refused; JNI implementations diverge: 1; no gate for native or workflow conflicts: 49.
RESOLVED = merged into this branch through a side branch (hand resolution with the module's Maven gate, or the
compiler-arbitrated batch: superset side first, PR side if the gate fails, refused if both fail). REFUSED rows name the
reason; the C/JNI and workflow conflicts no gate here covers have their reading packets under
`reading-20261023/` (conflict-marked files, SUMMARY.tsv, HUNKS.tsv with a class per hunk, NATIVE_SYMBOLS.tsv with the
native entry points of both sides). Tools and the day's corrections: Synexia recipe crate
`recipe-crates/m3-superset-merge-20261023` (RECIPE_MEMORY.tsv). Merge is the owner's call.
<!-- /resolutions -->
