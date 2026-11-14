<!-- SPDX-License-Identifier: Apache-2.0 -->
# census2-c4-20261106

Census pass-2 group C4 (rank batch native) of the M3JDK convergence programme on `m3/collections`: leaves P2-073
(`M3LongRankBatchNative` + LC 315 / LC 719 contracts and JUnit adapters + the JNI kernels in
`m3_long_rank_batch_jni.c`) and P2-074 (`M3LongRankIndex` facades). Census pins: com.synexia develop `d5ad5090d87e`,
M3jdk21 master `c4e268d495a0`; rows re-run on the push base and the head (`RECENSUS.tsv`, `RECENSUS_HEAD.tsv`).

| file | content |
|---|---|
| `apply.py` + `EDITS.tsv` | recipe CENSUS2_C4 v1 (from `../census-small-batch-20261106/apply.py`): 3 hash-pinned rules (Java owner, index, C source), 7 anchored edits; `--check` fixed point |
| `tools/gen_apply.py`, `tools/hunks.py`, `tools/DONOR.tsv` | generator of the RULES table from the six donor pre/post pairs (pure-addition blocks slid to whole declarations; declared JNI prefix rename, SPDX line drop, include set) |
| `relocate.py` + `RELOCATION_MAP.tsv` + `PROVENANCE.tsv` | recipe RELOCATE_CENSUS2 v1: the 6 contract/JUnit files, body_match 6/6 |
| `tools/native-gate.sh` | javac -h + prototype-checked gcc of the edited C, the legacy library, the contracts in every JNI mode |
| `LEAVES.tsv`, `LINEAGE.tsv`, `RECENSUS*.tsv`, `PROTECTED.tsv`, `GATES.tsv` | verdicts, lineage, census re-run, HOLD_63 list, gates |
| `oracle/` | `CensusRankDifferential` (pre-existing rank surface, before/after) |

Verdicts: P2-073 and P2-074 MAPPED (the crates' preimages are the unlanded pr1882 rank stack; their deltas are pure
additions, self-contained against master, placed at master anchors); P2-073h (the crate's history variant)
SUPERSEDED by the crate's own opt-in correction. HOLD_63: none.

Replay: `python -I apply.py --check`; `python -I relocate.py --donor-git <com.synexia> --donor-rev
d5ad5090d87ef8477a56317e60ac0d29c65029f7 --check`; regenerate the rules with `python -I tools/gen_apply.py
<com.synexia> <template> <out>` (given the committed apply.py as template it rewrites the same bytes). Native: `tools/native-gate.sh <repo> <classes:test-classes>
<out> <base>` on Linux.

Open items: master's `m3_long_rank_batch_jni.c` (LC 347 `topKFrequent0`) used `SIZE_MAX` without `<stdint.h>` and did
not compile under gcc/glibc; the include set added for the mapped kernels covers it. The rest of the pr1882 rank stack
(retained-variants/pr1882: 20+ LeetCode kernels) is outside these rows.
