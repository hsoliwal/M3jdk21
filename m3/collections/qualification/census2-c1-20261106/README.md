<!-- SPDX-License-Identifier: Apache-2.0 -->
# census2-c1-20261106

Census pass-2 group C1 (linked-slot family) of the M3JDK convergence programme on `m3/collections`: leaves P2-057
(`M3LongIndexPathMasks`, crate m3-long-pathmask-bridge-20261018) and P2-058..P2-062 (`M3LinkedHashMap`,
`M3OrderedSlotMap`, `M3SlotArrays`, `M3SlotMap`, `M3SlotSpliterators`, crate m3-linked-clear-switch-20261019).
Census pins: com.synexia develop `d5ad5090d87e`, M3jdk21 master `c4e268d495a0`; rows re-run on master `8481e882d633`.

No product source changes: every row is either superseded by a later owner commit or blocked on an absent dependency.

| file | content |
|---|---|
| `LEAVES.tsv` | verdict and evidence per row |
| `RECENSUS.tsv` | census compare re-run on `8481e882d633` (`tools/ROWS.tsv`, tool `../census-small-batch-20261106/tools/recensus.py`) |
| `LINEAGE.tsv` | every census-missing line of P2-058..P2-062: last master commit carrying it and the commit that replaced it |
| `PROTECTED.tsv` | HOLD_63 owners (brief + `m3/ports/collections` + files of 63's absorb PRs); no row of this group touches one |
| `GATES.tsv` | gates and oracle figures |
| `tools/lineage.py` | owner line lineage over the pre- and post-rename paths (stdlib, read-only git) |

Verdicts: P2-057 NEC (the bridge needs `M3IndexPathMasks.selectLongJava` of the unlanded wrapper r2 candidate;
master's owner equals the crate baseline); P2-058 SUPERSEDED (crate received by `4f33ce9281c`, tombstone/capacity
shapes replaced by `4c01121a9d9`, `6a2bdd65bd7`, `fe4c53178f6`); P2-059..P2-062 SUPERSEDED (the census postimages are
the crate's `inputs/` snapshots of those owners, replaced by `06e3c61cc16`, `c23e14692d9`, `09d1407a307`,
`f30b5e10888`, `dcaf125c210`, `0c24884dcf5`). HOLD_63: none.

Proof: owner tests with the real m3_arrays JNI library (54/0), the crate contracts on master (live mask 166,720
checks = crate receipt; clear contract 1,306,068 checks = crate receipt once the later fail-fast split of
`4650910eff1` is accepted at its one stale check), and a supersede mutant of P2-060 killed by the owner's own test.

Open items: `M3LinkedClearContract` (on master since `4f33ce9281c`) is stale at its bound-spliterator check and has
no JUnit adapter; P2-057 lands only with (or after) the wrapper r2 shared long kernel and its JNI.
