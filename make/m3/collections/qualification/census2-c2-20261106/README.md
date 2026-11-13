<!-- SPDX-License-Identifier: Apache-2.0 -->
# census2-c2-20261106

Census pass-2 group C2 (filtered views) of the M3JDK convergence programme on `m3/collections`: leaves P2-043
(`M3FilteredLists`), P2-042 (`interop/M3LibraryCollections` + the crate's new mutation oracle), P2-047
(`M3FilteredViews`), P2-048 (`M3MutableFilteredViews`), P2-067/P2-068 (`M3FirstVisibleAtomContract`/`Test`).
Census pins: com.synexia develop `d5ad5090d87e`, M3jdk21 master `c4e268d495a0`; rows re-run on the push base
`3beb791e7418` (`RECENSUS.tsv`, owners byte-identical to `c6ce2ad332e6` and the gate base `0b7482c3263`).

| file | content |
|---|---|
| `apply.py` + `EDITS.tsv` | recipe CENSUS2_C2 v1 (from `../census-small-batch-20261106/apply.py`): the MAPPED P2-043 hunk, hash-pinned preimage blob `78c77749664c`, `--check` fixed point |
| `relocate.py` + `RELOCATION_MAP.tsv` + `PROVENANCE.tsv` | recipe RELOCATE_CENSUS2 v1 (from `../../recipe/entryless-lane-kernel-20261106/relocate.py` v3): `M3FilteredListMutationProof` and `M3FilteredListMutationTest`, body_match 2/2 |
| `LEAVES.tsv` | verdict and evidence per row |
| `LINEAGE.tsv` | every census-missing line: last master commit carrying it and the commit that replaced it, or never on master |
| `RECENSUS.tsv` | census compare on the push base (`tools/ROWS.tsv`, tool `../census-small-batch-20261106/tools/recensus.py`) |
| `PROTECTED.tsv` | HOLD_63 owners; no row of this group touches one |
| `oracle/` | `CensusFilteredListDifferential` (P2-043 before/after) and `classify_p2043.py` |
| `GATES.tsv` | gates and oracle figures |

Verdicts: P2-043 MAPPED (fail-fast cursor probe before the three backing writes, onto the owner's later state mask
and supported-write helpers); P2-042 SUPERSEDED (owner later than the fixture copy) with the crate's new oracle
relocated; P2-047, P2-048 NO_PREIMAGE_PARTIAL (the crate hunks live in indexed slot cursors master never carried;
predecessor = retained variant `pr1463` of master-landing-20261028); P2-067, P2-068 SUPERSEDED by `6311c7c8bb3`.
HOLD_63: none.

Replay: `python -I apply.py --check` and `python -I relocate.py --donor-git <com.synexia> --donor-rev
d5ad5090d87ef8477a56317e60ac0d29c65029f7 --check` (exit 0, changed=0). Oracle: compile
`oracle/jdk/m3/collections/CensusFilteredListDifferential.java` against the master classes, run
`CensusFilteredListDifferential 20000 50` on the master and on the edited classes, then
`python -I oracle/classify_p2043.py master.txt edited.txt`.

Open items: land the indexed slot cursors of `pr1463` together with the m3jdk21-20261019 readiness hunks (the
M3TreeMap/M3TreeSet hooks they need are on master since #2547/#2565); the readiness oracle is the gate.
