<!-- SPDX-License-Identifier: Apache-2.0 -->
# census3-gaps-20261107 (m3/collections)

Census pass 3 member-gap leaves of lane com-synexia-0e on `m3/collections`: **P3-006** (`M3MonotoneLongDeque` vs
develop `synexia-mindex/foundation` `MIndexMonotoneLongDeque`) and **P3-009** (`M3PersistentLongSumTree` vs develop
`synexia-mindex/data-structure` `MIndexPersistentLongSumTree`). Census pins: com.synexia develop `78cf2ad4c7e5`,
M3jdk21 master `2274e18709b8` (`S:/m3-scratch/eb-conv/census3`); donor re-read at develop-live head `78cf2ad4c7e5`
(fetched 2026-11-07, unchanged), owner at master `9540290ac55e`.

Both owners were absorbed on 2026-11-07 from a different Synexia lineage (`m3-primitives` `MonotoneLongDeque` /
`PersistentLongSumTree`: 2d94d78634e wave15, 1bd4dfd879f wave12c), so the donor members are compared by contract,
not by text.

| file | content |
|---|---|
| `DECISION.tsv` | one decision per unexplained MEMBER_GAPS row (24): RESTORE 14, PRESENT_EVOLVED 6, NEC 3, HOLD_DEP 1 |
| `apply.py` | recipe CENSUS3_GAPS v1 (from `census-small-batch-20261106/apply.py`): hash-pinned anchored edits, `--leaf P3-006/P3-009`, `--check` fixed point, refuses drift (exit 2) |
| `EDITS.tsv` | file, before/after sha256, leaf, donor blob, rule (written by apply.py) |
| `oracle/census3/` | seeded differential: `Side` + `OwnerSide` (restored owner) + driver |
| `oracle/SIDE_MAP.tsv`, `mkdonor.py` | the donor side: `OwnerSide.java` mapped to the develop types (donor package names stay in the lineage TSV, exact under jdk_naming) |
| `differential.sh` | compiles both sides, runs default JIT and `-Xint`, requires byte-identical output |
| `mutants.py` | 12 mutants of the restored members, replayed through the differential |
| `GATES.tsv` | gates with numbers |

Decisions: RESTORE `lastIndex()`, `lastValue()`, `requireNonEmpty()` (the tail is the latest offer under both
equal-value policies; index widened to `long` like the owner's `firstIndex()`), and `forkAdd`, `rootNode`,
`length()`, the presizing constructor `(int, int)`, `pathNodeCount`, the scratch lanes and `PackedCapacity`
(`MAX`). NEC: `offer`/`evictBefore`/`firstIndex` (int vs long indexes, oldest vs newest equal value). HOLD_DEP:
`windowMaximum` (`MIndexExecutionContext`, com-synexia-4d). PRESENT_EVOLVED: lanes, `payloadBytes`, `physical`,
`size`, owner growth policy.

Replay: `python -I apply.py --check` (exit 0, changed=0). Differential: build the donor classes from develop
`78cf2ad4c7e5` (`javac -sourcepath <stub dir>;synexia-mindex/foundation/src/main/java;synexia-mindex/data-structure/src/main/java`,
one stub class for the held `MIndexExecutionContext`), `mvn package` this module, then
`bash differential.sh <donor-classes> m3/collections/target/classes <out> 20000` and
`python -I mutants.py <repo> m3/collections/target/classes <out> <work> 2000`.
