<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Superset branch m3/collections-superset-20261025 (M3jdk21)

Built 2026-10-25 from master `b35759a36ec` by merging the nine open collections draft PRs of 2026-10-24 server side
(`POST /repos/{r}/merges`, the 2026-10-23 method; no rebase, no force, no PR merged or closed) in PR order, the
stacked #1911 right after its base #1909. All nine merged without a conflict; `SUPERSET_COLLECTIONS_20261025.tsv`
is the ledger.

Why a new branch and not `m3/superset-20261023`: that branch is 39 commits behind master and master's drift
conflicts with it in 10 core files (`M3FilteredViews`, `M3LinkedHashMap`, `M3SlotMap`, ... and
`m3/tooling/module-packs/parser`), so every collections PR, all branched from current master, conflicts against it
for the same files. The collections PRs touch no core source; they add implementations, interop, qualification,
receiving, recipe and catalogue files plus four workflows, each PR in its own files.

Proof of the merges (local, blobless clone, `git diff` from each PR's merge base): for every PR the files it changed
are byte-identical between the PR head and this superset head `6a3b18ad252e`, except the five files of #1909 that
#1911 (stacked on it) changes, which equal #1911's bytes. Each PR carried its own gate (see its body); the superset
adds no new source interaction, because no two PRs share a file.

Merge is the owner's call: merging this branch merges the nine PRs at once; merging them one by one is equivalent.
Tools: `S:/m3-scratch/consolidate/coll-20261025` (`order.tsv`, `superset_merge.sh`, `merges.M3jdk21.tsv`).
