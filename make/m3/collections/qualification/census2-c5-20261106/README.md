<!-- SPDX-License-Identifier: Apache-2.0 -->
# census2-c5-20261106

Census pass-2 group C5 of the M3JDK convergence programme on `m3/collections`: leaf P2-041
(`M3WrapperConvergenceTest`, crate m3-wrapper-convergence-20261018). Census pins: com.synexia develop
`d5ad5090d87e`, M3jdk21 master `c4e268d495a0`; row re-run on the push base (`RECENSUS.tsv`).

No product or test source changes. Verdict: SUPERSEDED: master's JUnit runs the admitted `readonly-policy` mode
by design (`5fde7416ab1`, `1666268f57d`); the crate line (`run("readonly")`) adds the comparator-failure leaf, which
fails on master (`GATES.tsv`). HOLD_63: none (`PROTECTED.tsv`).

| file | content |
|---|---|
| `LEAVES.tsv` | verdict and evidence |
| `RECENSUS.tsv` | census compare on the push base (`tools/ROWS.tsv`, tool `../census-small-batch-20261106/tools/recensus.py`) |
| `LINEAGE.tsv` | the census-missing line: never on master |
| `PROTECTED.tsv` | HOLD_63 owners |
| `GATES.tsv` | gates and contract-mode figures |
| `tools/lineage.py` | owner line lineage (as in `../census2-c1-20261106/tools/`) |

Open item: the comparator-failure, readonly-linear and readonly-failfast leaves of `M3WrapperConvergenceContract`
remain pending on master (owner: filtered views, see census2-c2-20261106 P2-047/P2-048).
