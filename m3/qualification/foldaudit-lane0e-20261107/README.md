<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Fold-close audit, lane 0e, leaves outside m3/core and m3/collections: decision record

The fold-close audit (`S:/m3-scratch/eb-conv/foldaudit/`, inputs `LEAVES.tsv`, `HUNK_VERDICTS.tsv`, `CLOSED_PRS.tsv`,
`CITED_MERGES.tsv`) lists closed PRs whose close comment cites a landing that does not carry their hunks. This
directory records the lane-0e decisions for the leaves of PRs #1080, #2234, #2182, #4 and the `test_m3_export.py`
leaf of #513/#546/#564/#568/#626/#627/#650. Each leaf was checked against the PR head (fetched by `refs/pull/N/head`,
verified equal to the recorded `headRefOid`) and against master (`74402d981fa0` for the greps and ls-trees,
re-checked by `verify.py` at the revision recorded in `GATES.tsv`).

No leaf qualifies for RESTORE, so this PR changes no source.

| verdict | leaves | why |
|---|---|---|
| HOLD_DEP (lane 4d) | L0034, L0040, L0041, L0209, L0071, L0075, L0078, L0085 | #1080 probes, test-support and before/after harnesses whose subject is the java.base prepared-batch API (L0006/L0007) or the affix refinement (L0052); none of those members exist on master (0 grep hits), and the three crates they live in have 0 paths on master |
| HOLD_DEP (lane 0e, owner-first) | L0083 | six tests of `test_m3_export.py` never entered master (every binding merge is ancestry-only, tree == parent 1); they exercise `export-m3-lexicon.py` / `verify-m3-lexicon.py` members that are themselves lost (L0403, L0326) |
| HOLD_CHROME | L0017 | `M3StructuredFormatter` was lost when receiver #2390 was folded (`fc917d80ea3c`, empty first-parent delta on `src/jdk.m3.logging`), but PR #2234 carries a recorded HOLD under the Chrome rule that only the user can override |
| PRESENT_EVOLVED | L0045, L0043 | master's `M3HashPinnedJavaSnapshotRecipe` copy is a superset of the hunk (one more admitted path); `M3Cleanable` was deliberately moved to `java.lang.ref.Cleaner` by `c500a57e2340` |
| RETIRED_DESIGN | L0039 | the `java_lang_MIndexString` HotSpot accessors of #4 were replaced by `java_lang_M3String` (12 references in master's `javaClasses.inline.hpp`); `java.lang.MIndexString` does not exist on master |

- `DECISION.tsv`: one row per leaf with the cited commits, blobs and paths, the evidence and the action.
- `HANDOFF_4D.tsv`: the java.base halves (L0006, L0007, L0052 of #1080; L0046, L0047, L0058, L0059 of #2182) with their lane-0e dependents.
- `GATES.tsv`: what was run.
- `verify.py`: re-derives every cited fact from the owner revision with read-only git commands and refuses on drift
  (`python -I verify.py --root <clone> --rev origin/master`).

## Confirmed: the cited landing of #1080 carries none of its content

`0890555a47c2` ("merge(superset): preserve conflicting and stacked M3 Java/JNI PR ancestry") has six parents; its tree
`8b9fd78cca7a` equals its first parent's tree. The #1080 head `6adadadf64ae` is parent 5. The crate
`m3/runtime-integration/regex-rope-affix-effects` (278 paths on the head) and its sibling crates
`regex-mandatory-affixes` (3) and `regex-plan-amalgamation` (5) have 0 paths on master.

## User decision needed: #2234 (L0017)

The 9-file postimage of #2234 (`f12245e0b7a8`: `module-info.java`, `package-info.java`, `M3StructuredFormatter.java`,
`make/modules/jdk.m3.logging/Java.gmk`, the `PLATFORM_MODULES` line, four tests under `test/jdk/jdk/m3/logging`) is
reachable from master and recoverable byte-exact, and no successor owns structured JUL formatting
(`git grep -i structured -- src/jdk.m3.logging` is empty because the module directory does not exist). The PR's
comment of 2026-10-31T15:07:14Z holds the class under the Chrome rule pending a Chrome per-file work order or an
explicit override. This record does not override it.

## Owner-first restore for L0083

Heads and the defs each carries that master lacks:

| head | PR | defs |
|---|---|---|
| `82280f083ec3` | #564 | `test_directed_related_lexeme_families_round_trip_and_require_input`, `test_python_export_opens_through_java_catalog_for_both_relation_families` |
| `454a95173f9b` | #568 | the same two |
| `f402f7423bae` | #626 | `test_composite_owner_shapes_and_overloaded_field_types_are_admitted` |
| `c13a6de31193` | #627 | that one plus `test_typed_family_sidecars_round_trip_and_reject_partial`, `test_family_mapping_catalog_matches_receiver_schema`, helper `write_family_bundle` |
| `19d1d4a91f87` | #650 | `test_directed_related_lexeme_families_round_trip_and_require_input` |

The owner on master (`export-m3-lexicon.py`, 673 lines) has no `load_family_sidecars`; the #627 owner (680 lines) has
it. Restore the owners (L0403, L0326) first, then these defs, in one `m3/runtime-integration` PR with its own
hash-pinned recipe; the test file alone would fail on master.
