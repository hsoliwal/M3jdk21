# M3JDK21 backport programme status — 2026-10-07

This is a current-tree evidence update, not a completion or promotion claim.

Baseline:

- repository: `hsoliwal/M3jdk21`
- branch: `master`
- baseline commit for this repair: `bd0f6b8e13a4af6f118d523d87a2711b75ea11da`
- released donor interval: JDK 22 GA through JDK 27 GA
- released JEP denominator: 85 rows
- pending compatibility/proof rows: 42

## Correct packet-state semantics

A recipe/evidence directory is not product materialization.

`jep_residue.py` now reports:

- `MATERIALIZED_PACKET` only when a matching packet directory contains a
  `CURRENT_TREE_RECEIPT.tsv`;
- `PACKET_EVIDENCE` when packet/inventory/recipe evidence exists without a current-tree receipt;
- `RECIPE_CLASS` when only a recipe implementation is present;
- `NO_RECIPE_EVIDENCE` when no repository-owned packet/recipe evidence exists.

This repairs four false current-master materialization claims:

| JEP | Correct state | Reason |
| --- | --- | --- |
| 423 | PACKET_EVIDENCE | inventory/cumulative-materialization control packet, no current-tree receipt |
| 484 | PACKET_EVIDENCE | custody/path-map packet, no current-tree receipt |
| 485 | PACKET_EVIDENCE | gatherer packet/recovery evidence, no current-tree receipt in current tree |
| 523 | PACKET_EVIDENCE | G1-default packet/adaptation evidence, no current-tree receipt |

No compatibility decision changes.

## Hosted proof state

### JEP 458

The reviewed 26-target postimage remains present and receipt-backed.

PR #277 refreshed the proof plane, but GitHub Actions run `37569443699` created zero jobs.
Therefore there is no compiler, jtreg, or runtime failure evidence. Promotion remains
`NOT_AUTHORIZED`.

### JEP 493

The 47-path file-atom generator/materializer packet remains ready.

PR #308 records that the three requested hosted runs created zero jobs:

- generated-crate materialization: `37578205192`
- backport admission: `37578205260`
- file-atom inventory: `37578205216`

No JEP493 product/jtreg/runtime failure is inferred. Product materialization remains unproven and
promotion remains `NOT_AUTHORIZED`.

## Nebula/Synexia recipe transfer

The reusable FILE-local atomization/patternization recipe authority remains Synexia.
M3JDK21's `m3/tooling/a3` receiver pins:

- Synexia commit `a062661e91e769a00c2d027e1f04a7a0c8f9dc28`
- export manifest Git blob `8a3e3d6e802e95dbcc7b0bf83a347887b02d6717`
- convergence source Git blob `b11aff3ace8e77684cfa8cc13fbbb445a05c8a09`
- portable mastery verifier Git blob `cd54b351f444a497c759a009681cc97d3e79c5ba`

Those three blob identities were re-read from the pinned Synexia commit on 2026-10-07 and match the
receiver contract exactly.

Nebula remains the proving/product laboratory; M3JDK21 remains the Java21 JEP/tooling backport
receiver. Nebula/Synexia proof does not waive JDK-specific configure/build/jtreg/runtime/ABI gates.

## Next mechanical order

1. keep JEP 458 in proof-only state until a hosted build/jtreg/runtime job actually executes;
2. keep JEP 493 in FILE-atom materialization/proof state until its 47-path job actually executes;
3. process remaining receipt-backed priority-20 packets by their recorded next action;
4. materialize packet-only priority-20 candidates only after their own source-pinned recipe/JUnit
   gates are complete;
5. continue candidate-high-risk dependency closure after the priority-20 proof frontier;
6. continue the non-JEP released-commit denominator independently;
7. rerun the residue generator after every receipt/materialization change.

The queue, not conversation order, remains the scheduling authority.
