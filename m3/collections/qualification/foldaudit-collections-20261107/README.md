# foldaudit-collections-20261107 — fold-close audit restores of m3/collections (lane 0e)

Recipe `FOLDAUDIT_COLLECTIONS v1` (`apply.py`, hash-pinned, idempotent, drift-refusing). Inputs: the fold-close audit
under `S:/m3-scratch/eb-conv/foldaudit/` (`LEAVES.tsv`, `HUNK_VERDICTS.tsv`, `CLOSED_PRS.tsv`, `CITED_MERGES.tsv`).

## Why

PRs #1084 and #1081 were closed as "integrated additively into master via ancestry-preservation commit". The cited
commits `b42ab7605b86` and `0890555a47c2` are ancestry-only merges: `git rev-parse <sha>^{tree}` equals the first
parent's tree, so none of the PR content reached master although every PR head is an ancestor of master. PR #1791 landed
through #2070 (`2028b1fb5f4`), whose retain merge kept master's pre-identity `M3IndexPathRank`; PR #2231/#2488's
`M3ArrayList` hunks were superseded by the 31l landing (`b65fef768d34`, BRANCH_CHANGED).

## Decisions (DECISION.tsv)

| leaf | owner | decision |
|------|-------|----------|
| L0014 L0050 | interop `M3GuavaLongMultiset`, `M3LibraryBags` | RESTORE (origin test `M3BagInteropTest`) |
| L0025 L0028 L0029 L0032 | core `M3CountTotals`, `M3CountLanes`, `m3_count_totals.c`, `m3_count_lanes.c` | RESTORE (origin tests `M3CountTotalsTest`, `M3CountLanesTest`, probe) |
| L0054 | `M3CountedCatalogue` + `counts/catalogue.tsv` | RESTORE MAPPED into the core module (no counts pom on master) + new test |
| L0308 L0311 L0341 L0346 L0373 L0377 L0493 | hash-pinned receiver texts (passed from lane 22) | RESTORE at owner paths |
| L0048 | `M3IndexPathRank` identity path | RESTORE by 3-way merge (origin tests L0129 L0180); caller `M3LongStateArray` HANDOFF |
| L0044 | deep-filter-list engine copy `javaPath` | PRESENT_EVOLVED (m3/arrays empty-segments engine copy) |
| L0066 | `M3FilteredLists` orderedAnd | PRESENT_EVOLVED (`17370668c70` FilterChain) |
| L0067 | `M3FilteredSortedMultisets` | NEC (file and its owners never landed) |
| L0053 | `M3ArrayList` required-JNI backing | RETIRED_DESIGN (the PR's own admission hold; `accc6e8ac47`) |

The PR-head `M3LongBag` spelled `size()/distinctSize()/add(long)`; master's `M3LongBag` (`e6722c63e9b`, lane 22) spells
`totalCount()/distinctCount()/add(long, long)`. The restored callers bind to master's owner with those calls renamed
(anchor `API` in each file, `HANDOFF.tsv` API_PIN). `M3PackedLongBag` carries the PR-head API under another name
(NEC_NOTE, lane 22's convergence decision).

## Layout

- `apply.py` — RULES (ADD from donor blobs with exact substitutions, MODIFY through `rules/M3IndexPathRank.hunks.json`,
  NEW from `templates/`); `EDITS.tsv` is its fixed-point output.
- `native-gate.sh` — WSL gate: javac -h prototype match, gcc -Werror, nm exports, native == Java digests
  (`oracle/.../FoldauditCountsDigest.java`), origin tests native-required (default and -Xint) and the refusal without a
  library, C and Java mutants.
- `tools/interop_mutants.sh` — interop owner mutants judged by `M3BagInteropTest`.
- `DECISION.tsv`, `HANDOFF.tsv`, `GATES.tsv`, `FOSS_REUSE_DECISION.tsv`, `RECIPE_MEMORY.tsv`.

## Open items

- `M3LongStateArray.Snapshot.prepare` still allocates the identity `int[]` (HANDOFF.tsv; owner not in this leaf).
- The hash-pinned receiver `manifest.tsv` (L1058) and the rest of PR #1084's counts/ recipe tree (poms, verify.py,
  provenance, README) are lane-0e leaves outside this assignment and were not restored.
- The core pom's default surefire run has no count library: the restored tests run their Java path; the native path runs
  in `native-gate.sh` (or with `-Dm3.counted.native=<lib> -Dm3.totals.native=<lib> -Dm3.test.native.required=true`).
