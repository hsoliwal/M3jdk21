<!-- SPDX-License-Identifier: Apache-2.0 -->
# census-small-batch-20261106

Twelve small census leaves of the M3JDK convergence programme on `m3/collections` (L066, L071-L074, L076-L078,
L080, L083, L085, L086; L075 excluded by the owner decision). Census pins: com.synexia develop `ca1aafc4d94f`,
M3jdk21 master `80743e25b9e6`; every row re-run on master `e65f0f664fa3` and again on the push base `feae0743e6c2`
(`tools/recensus.py`, rows `tools/ROWS.tsv`, output `RECENSUS.tsv`; only L076 moved: #2547 landed its hunk),
verdicts in `LEAVES.tsv`.

| file | content |
|---|---|
| `PROTECTED.tsv` | owners this batch must not edit: the brief's 8 + `m3/ports/collections` (4 colliding, 11 scaffolds) + the jdk.m3.collections main sources added or modified by #2491, #2494, #2496, #2538, #2541, #2543 (`gh pr view --json files`), plus #2552 and #2555 (63, merged after the brief) |
| `apply.py` | recipe CENSUS_SMALL_BATCH v1 (from `make/m3/qualification/progress-monitor-unify-20261106/unify.py`): hash-pinned anchored edits for L066 and L077; `--leaf` per commit, `--check` fixed point |
| `EDITS.tsv` | file, before/after sha256, leaf, crate, donor blobs, rule (written by apply.py) |
| `oracle/` | `CensusResidencyDifferential` (L066), `CensusTreeDifferential` + `CensusTreeHooksCheck` (L076/L077), `classify_l066.py` |
| `tools/` | `recensus.py` (census.py compare on a newer master; optional same-package type list), `failset.py` (surefire failing set) |
| `GATES.tsv` | gates per leaf and overall |

Verdicts: MAPPED L066; APPLICABLE L077; APPLIED L071-L074 (census PARTIAL was a same-package type-name
artifact), L076 (MAPPED on `e65f0f664fa3`; #2547 landed the same hunk before the push, commit dropped), L083, L085 (crate intent already on the owner in a later shape); NO_PREIMAGE_PARTIAL L078, L080, L086
(owner moved on, undecidable; missing lines in `LEAVES.tsv`). No leaf needed a protected owner (HOLD_63: none).

Replay: `python -I apply.py --check` (exit 0, changed=0). Oracle (JDK 21, after `mvn package`; CP = module
classes + m3-arrays jar): compile `oracle/jdk/m3/collections/*.java` against the master classes (the hook check
against the edited classes), run each side, `cmp` the outputs, `python -I oracle/classify_l066.py` for L066.
