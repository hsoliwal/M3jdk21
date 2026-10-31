<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Backport contract filesystem differential

This is an admitted, source-only qualification input. No Maven, owning Python test,
filesystem integration or broader reactor success is claimed by publishing these files.
The parent admission is ab8633cc645487bc69bcd6919f328a0e1b5e40a9.

The driver adapts the existing exact-custody, inherited-process-group and receipt pattern
from state-relation-current-c11-20261019/runtime/qualify.py
(edf9f20eaf672f661d1f469a2d036f0c28da7805, Apache-2.0).
It introduces no production policy, scheduler, parser or validator. The receiver's
eight original owners total 59,781 bytes. Five production/data inputs remain unchanged;
three expected postimages must be the actual fresh Maven SDK outputs.

Run the existing SDK qualification first. Its three generated files must exist at
sdk/target/emitted/<original m3/backports path>, with the exact INPUTS.json postimage pins.
The runtime accepts the exact reviewed before tuple, not an unreviewed current file.

The existing external supervisor must create Q/artifacts, Q/tmp, Q/home and Q/cache,
set M3_SOURCE_ROOT to the canonical actual checkout, and set M3_PROOF_ROOT and
M3_QUALIFICATION_ROOT to canonical Q. Set a fresh M3_QUALIFICATION_ID,
EXPECTED_SOURCE_SHA to the actual checked-out PR head, TMPDIR/TEMP/TMP to Q/tmp,
HOME to Q/home and XDG_CACHE_HOME to Q/cache. Q and checkout must be disjoint.
The driver reads git rev-parse HEAD and compares EXPECTED_SOURCE_SHA; GITHUB_SHA is
recorded only as ciReportedSha because a pull-request event can report a merge commit.

Invoke with provisioned Python 3.11 or later:

    python3 -I -S -B runtime/qualify.py --output-root "$Q" --inputs-only
    python3 -I -S -B runtime/qualify.py --output-root "$Q" --run

Both invocations belong inside the existing monitored process group. Subprocesses
inherit that group; the driver kills/reaps its direct child on internal timeout.
It does not claim to enforce aggregate bytes, reap all descendants or supervise
an unwrapped standalone execution. The outer supervisor must enforce the unchanged
512 MiB aggregate cap, 8 GiB free reserve, bounded timeout and final complete census.

The runtime retains three small exact eight-file receivers at
Q/backport-contract-python/{baseline,candidate,child-reversion}. The candidate is
assembled from the real three Maven postimages plus the original five KEEP files.
The child-reversion changes only the unique literal
{"a.compile": "review/child-only"} back to {} in an isolated copy.
No source or emitted output is edited. All fixture directories remain for final custody.

Four fresh Python -I -S -B processes execute, without mock filesystem or CSV transport:

* backport-baseline: the exact six policy and fifteen wave methods; exactly KeyError
  401 in the real-ledger assertion and the known DEPENDENCY_HOLD/SCOPE_APPROVAL_HOLD
  assertion failure are required, with no additional failures or skips.
* backport-candidate: all same 21 owning methods must pass with no skips, errors,
  expected failures or unexpected successes. This includes the authentic TSV/JSON
  and approval-ledger file I/O methods.
* backport-child-approval-refusal: the same full 21 methods; only the original
  broader-scope method must fail after its child-approval fixture is reverted.
* backport-synthetic-validator: three independent unittest controls call the actual
  candidate helper and unchanged production validator using real temporary TSV files.
  Synthetic JEP 999 with NO is accepted; OPT_IN and CANDIDATE must each raise the
  existing specific ValueError. These controls grant no released-JEP membership.

The total is 66 method executions across four processes, including expected negative
runs. Every process inserts only its declared exact fixture directory into sys.path
and pins tempfile.tempdir to Q/tmp before any tempfile request. Python imports remain
real; no module, validator, unittest method, CSV API or file API is stubbed.

Existing owning tests remove their real tempfile.TemporaryDirectory directories.
The outer monitor therefore needs only the already-reviewed bounded full-rescan
exception for exact Q/tmp/tmp[a-z0-9_]{8} during runtime-proof, with at most three
complete rescans and a mandatory final complete census. No source, SDK target or M2
directory error is waived. Runtime-created receiver directories are not removed.

Four raw logs, source-custody JSON and backport-contract-execution.json go to
Q/artifacts. Each child emits one actual unittest result marker containing complete
loaded/executed IDs, problems, module paths, isolation flags and temp route. The driver
checks the full inventory and precise negative signatures, rather than a printed PASS.
It rehashes source owners, all three receivers, actual Maven outputs and every raw log.

The host may import inputs(receiver) -> (metadata, actual_sources, input_manifest_hash)
and validate_execution(execution, Q, receiver) -> None. The latter reparses all four
hash-bound raw logs, verifies real final fixtures and custody, and rejects scope drift.
Runtime receipt schema is M3_BACKPORT_CONTRACT_CURRENT_OWNER_EXECUTION_V1 and scope is
EXACT_EIGHT_OWNER_BACKPORT_CONTRACT_DIFFERENTIAL. SDK qualification remains a separate
prerequisite; the runtime cannot manufacture its receipt.

All broader flags stay false: original private source-fork model, full control reactor,
matched/full JDK, Java/JNI/native, performance, production application and phase promotion.
The existing broader failed reactor gates remain unresolved by this focused test proof.
