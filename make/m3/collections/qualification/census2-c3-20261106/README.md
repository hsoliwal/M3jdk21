<!-- SPDX-License-Identifier: Apache-2.0 -->
# census2-c3-20261106

Census pass-2 group C3 of the M3JDK convergence programme on `m3/collections`: leaves P2-020
(`M3IndexedListSnapshot`, crate m3-atom-pattern-multipass, dataset m3-stable-run-reversal-20261021, + its new tests)
and P2-075 (`M3LaneAlgorithms`, crate m3jdk21-lane-rotation-compact-20261029, + its new test). Census pins:
com.synexia develop `d5ad5090d87e`, M3jdk21 master `c4e268d495a0`; rows re-run on the push base (`RECENSUS.tsv`).

| file | content |
|---|---|
| `apply.py` + `EDITS.tsv` | recipe CENSUS2_C3 v1 (from `../census-small-batch-20261106/apply.py`): the five P2-075 hunks, preimage blob `08a2e274ed7f`, `--check` fixed point |
| `relocate.py` + `RELOCATION_MAP.tsv` + `PROVENANCE.tsv` | recipe RELOCATE_CENSUS2 v1 (as `../census2-c2-20261106/relocate.py`): `M3StableGroupReversalSelfTest`, `M3StableGroupReversalTest`, `M3LaneCycleOverflowAndCompactTest`, body_match 3/3 |
| `LEAVES.tsv` | verdict and evidence per row |
| `LINEAGE.tsv` | census-missing lines through the owners' master history |
| `RECENSUS.tsv` | census compare on the push base (`tools/ROWS.tsv`) |
| `PROTECTED.tsv` | HOLD_63 owners; no row of this group touches one |
| `oracle/` | `CensusLaneDifferential` (P2-075 before/after, with the reversal rotation as reference) |
| `GATES.tsv` | gates and oracle figures |

Verdicts: P2-075 APPLICABLE (applied); P2-020 SUPERSEDED (the owner's group-mask algorithm, re-received on purpose by
`0ac0c66e4ac`, passes the crate's contract oracle; the oracle and its JUnit adapter, retained since the 2026-10-28
landing as `retained-variants/pr1768`, are relocated as live tests). HOLD_63: none.

Replay: `python -I apply.py --check`; `python -I relocate.py --donor-git <com.synexia> --donor-rev
d5ad5090d87ef8477a56317e60ac0d29c65029f7 --check` (exit 0, changed=0). Oracle: compile
`oracle/jdk/m3/collections/CensusLaneDifferential.java` against the master classes and run
`CensusLaneDifferential 200000` on the master and the edited classes (identical digests).
