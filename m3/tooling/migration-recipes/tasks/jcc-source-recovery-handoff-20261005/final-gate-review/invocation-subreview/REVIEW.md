# Actual V3 reference audit: invocation and custody subreview

No issue found in the recorded completed invocation or its bounded PASS claim. The output `reference-audit-v3/REFERENCE_AUDIT.json` is 769,381 bytes, SHA-256 `5867bffd36e65f2fd755921d0dbf9981a006bc0e38134b2d0e4e6830030900ea`. It is the actual output bound by the recorded stdout, not a proposed or reconstructed execution result.

The complete supplied invocation directory contains `EXECUTION.json`, `stdout.log` and `stderr.log`. The recorded command is:

```
/opt/codex/runtimes/codex-primary-runtime/dependencies/python/bin/python -B /workspace/scratch/1c68df1bae79/javac-convergence-20261005/work/m3jdk21-current/successor-e04/check_references_v3.py
```

The cwd is that `successor-e04` directory. The checker bytes match the exact admitted source: 40,725 bytes, SHA-256 `521c18944616fe46a3e347860c25a7283e1b74674c594e40996c787e01e96b81`. The invocation runs the script directly, uses no `-O` option, and the admitted checker itself refuses optimized execution. This subreview does not independently recapture the Python runtime or environment; that is the separate runtime review's scope.

The recorded invocation began at `2026-10-05T17:42:54.173327+00:00` and finished at `2026-10-05T17:42:55.864392+00:00`, with exit code 0. The audit's `observed_at_utc` is `2026-10-05T17:42:55.842021+00:00`, within that interval. The stdout is exactly one 244-byte JSON summary; its output hash matches the actual audit bytes. The stderr is empty, with the standard empty SHA-256. Both log lengths and hashes agree with `EXECUTION.json`.

The stdout's counts equal the corresponding actual output arrays: 19 source-reference occurrences, 55 proof-reference occurrences, 182 destination-reference occurrences, 1,008 frozen-input records, 89 preserved E3 payloads and 56 preserved E2 payloads. The audit distinguishes the 182 occurrences from 170 distinct destination paths and records four materialized afterimages. These are invocation/output consistency checks, not a duplicate independent rehash of the 1,008 inputs or a replacement for the parent's receipt/copy-accountability review.

The invocation binds the actual receiving receipt at 31,015 bytes, SHA-256 `1f4ac9825d975520a04b7dc5ac1b282d96be32601c7abe2b0cfb676b0ff2df09`. The audit uses the same source commit/root, destination preimage and plan identity, and its complete `publication_custody` object equals the receipt's `source_publication_custody`. The receipt's 12 Java and six Python tests remain prior receiving-verification evidence; this read-only reference checker did not run them. The checker audit also does not rerun the four historical E2 receiving behavior tests.

There is one completed checker attempt in the supplied `reference-audit-invocation-v3` directory. The separate readiness document records only draft AST parsing and explicitly says the checker was not executed at that earlier stage. No failed checker invocation is present in the supplied invocation observations. This is a statement about the inspected evidence, not a claim about all possible unrecorded process history. Earlier source transport failures remain preserved within the carried custody record rather than being converted to successful raw readbacks.

The actual output retains 21 observed direct source bodies, including six mandatory direct bodies, and exactly two Git-identity-only malformed-UTF8 fixtures: `59b4ec83f687499a54f15695b9b246599f9579d7` (four bytes) and `24cdc62e634934527d551324c2449bcf951b5589` (22 bytes). Each overall entry and its probe/final failure entries retain `raw_body_readback=false` and `remote_sha256_observed=false`, with `FAILED_UTF8_REENCODING`. The aggregate custody fields retain false `malformed_utf8_body_readback` and `all_required_blob_bodies_read_back`. This successful local audit neither recovers those remote byte arrays nor replaces their content-addressed custody basis.

PASS is expressly limited to the read-only exact-body, reconstructed-tree-path, receiving-preimage, lineage and frozen-input audit. The result retains false receiver-behavior-rerun, source-export and full-module/JDK acceptance, with empty capability tests and unchanged gates. Its limits explicitly preserve current independent native 19 alongside upstream 66 of 67 with one error, historical source/receiver evidence, and absent repository-wide semantic closure. Publication custody is not promoted to source qualification or receiving acceptance.

The five newly planned donor documents and any future companion source PR are outside this execution and receive no retroactive coverage from the audit. This subreview does not inspect or admit those pending additions. No source publication verification, checker execution/import, project gate, remote operation or owner edit was performed by this reviewer. All recorded input files were rehashed unchanged for the attached input seal.
