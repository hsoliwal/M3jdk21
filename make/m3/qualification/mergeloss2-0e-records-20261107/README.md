<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Merge-loss audit phase 2, lane com-synexia-0e: decision record (2026-11-07)

This directory records the decisions for 17 leaves of the phase-2 merge-loss audit (`CLEAN_0E.tsv`: no ancestry
risk, not rename products). The lost lines of each leaf were re-tested against master `2ec96ee32ff5`.

| decision | leaves | edit |
|---|---|---|
| RESTORE | 59, 97, 102, 103 | #2662, #2668, #2666, #2672 (one draft PR per module, each with a hash-pinned recipe) |
| RESTORED_SINCE | 45 | #2653 (merged, 322af721aa7) carries all 3 lost lines |
| HANDOFF | 30 | #2653's owner: `reuseUnborrowedProjection` already carries the semantics; the 34 exact lost lines are in `exact_lost_lines` |
| PRESENT_EVOLVED | 33, 44, 49, 54, 99, 100 | none |
| SUPERSEDED | 28, 32, 39, 47 | none (a later deliberate master commit chose another design) |
| NEC | 50 | none: blocked until java.base leaf 79 lands `m3_jni_util.h` (lane com-synexia-4d) |

`DECISION.tsv` gives each leaf's origin commit, dropping merge, audit atoms, evidence (cited commits) and action.
`PROBES.tsv` is the mechanical form of every no-edit decision. `verify.py` re-checks it:

```text
python -I make/m3/qualification/mergeloss2-0e-records-20261107/verify.py [--rev REV]
```

On `origin/master` (`2ec96ee32ff5`): probes=33, failures=0. On `baafdafdc817`, before #2653: the 3 leaf-45 probes fail. A
FAIL means master moved under a decision, and that leaf needs a new decision.
