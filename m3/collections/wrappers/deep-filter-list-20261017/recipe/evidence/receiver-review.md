<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->
# Independent wrapper receiver review

## Outcome

All seven requested receiving cases passed against the actual ten-target SDK Results from the mapped canonical wrapper recipe. The review found no blocking integrity defect in those cases under the receiver's serial, single-writer use. The receiving adapter and collection wrapper sources were not changed.

The tested receiver SHA-256 is `24ce6011c0bc7d89fc863f8f809740b24d943cb40c5aaf9aba03adf0f222dadf`. The SDK Results ledger SHA-256 is `dfbf6113acdd3e3ed7020a01b6fe5805e5c2293f55609b6e323dcd4edbe260db`. The final review driver SHA-256 is `f461e520c893a7d6a45bd237c2e7f2ef9ac27392969908477b3d6c9b6f98d4b5`.

`final-qualification/evidence/RECEIVER_RECEIPT.json` retains the result, input bindings, exact ten output hashes, and per-case status. Adjacent stdout/stderr files retain each subprocess result. Intentional refusal cases return nonzero and retain the expected diagnostic; this is a successful refusal proof, not a failed contract.

## Cases

| Case | Exercised condition | Observed result |
|---|---|---|
| Check | Ten sealed preimages/absences, with unrelated sentinel | Reports ten pending changes; all paths, bytes, permissions, and mtimes unchanged |
| Apply | Same admitted overlay | All ten files equal actual SDK outputs; existing permissions preserved; new files use mode 0644; unrelated sentinel unchanged |
| Second apply | All ten sealed postimages | Reports zero changes; file and directory state, including mtimes, unchanged |
| Late target drift | Altered final sorted target, m3/docs/name-mapping.json | Refuses before earlier targets are written; entire overlay unchanged from the deliberate drift state |
| Stale SDK inputs | Named YAML altered after qualification | Refuses the mismatched source binding; all target state unchanged |
| Altered SDK output | Final candidate output bytes altered | Refuses the output hash mismatch; all target state unchanged |
| Symlink parent | m3 parent points to another directory inside the target root | Explicitly refuses the symlink; linked backing graph unchanged |

The symlink case deliberately remains within the root. Its refusal therefore exercises the explicit parent-symlink check rather than relying only on path-escape detection. Refusal and no-op snapshots include directory mtimes and symlink metadata without traversing symlinks.

## Named descriptor and binding review

The current M3TaskRecipeTest activates the named YAML inside its first JUnit test, validates the recipe, executes the combined Java/text source set, and compares the complete result with the expected output including the unrelated decoy. Per-lane tests additionally cover exact replay, fixed point, input order, mixed pre/post states, drift/missing/duplicate/wrong-representation refusal, and post-scan drift. The mapped Maven run reported exactly five tests with no failures, errors, or skips.

The SDK binding records the POM, receive.py, all regular source and contract files, and the four source SDK owners. The receiver checks the exact input set and file hashes before considering target writes. It also reconciles actual SDK Results against all manifested target paths and postimage hashes. The stale-binding and altered-output controls independently exercise these two custody checks.

This is a source and output integrity mechanism for a trusted local Maven execution. It is not a cryptographic attestation of an execution environment or protection against someone who rewrites every source, receipt, and report together.

## Boundaries that remain explicit

Each target replacement uses an atomic file replacement, but the group of ten replacements is serial rather than one cross-file transaction. A filesystem error after an earlier successful replacement can therefore leave a partially applied set. Exact PRE/POST admission and the tested zero-change replay permit a later retry when the underlying error is resolved; rollback on arbitrary I/O failure is not claimed.

The receiver checks the target graph before the first write and assumes no concurrent target mutation during the subsequent replacements. It does not hold directory file descriptors or a checkout-wide lock to prevent a different process from changing a path after admission. The tests cover preexisting drift and symlinks, not hostile concurrent path replacement.

The adapter checks SDK qualification. The full runtime/ABI/native contract remains a separate required Maven verification gate before the root applies the outputs. The receiver review does not relabel SDK qualification alone as completion of all runtime gates.

## Portable integration

Copy verify_receiver.py into the canonical recipe's contract directory before its final Maven SDK run, so the existing source-binding test includes the driver. Then invoke it with the actual crate:

```sh
python3 contract/verify_receiver.py \
  --crate /absolute/path/to/the/recipe-crate \
  --output /absolute/path/to/a-fresh-receiver-proof
```

The recipe-owner location defaults to the actual Surefire property and can be supplied with --recipe-owner. All mutations occur in temporary test fixtures. Candidate bytes are copied from actual SDK output files, never produced from templates by this driver. The script verifies the original qualified files and mtimes again after the tests, removes the temporary overlays, and retains the evidence.

The preparatory receipt records driver_in_sdk_source_binding=false because this new test had not yet been copied into the canonical crate. Root's final SDK run after copying it will bind its hash, and the receiver proof receipt will then record true. This explicit distinction preserves the private convergence lifecycle and the custody of the final qualification.

