# E4 actual focused execution review — V3, T01

**Disposition: the admitted focused receiving execution passed, with completion correctly remaining blocked.** No concrete execution defect was found in this bounded review. This disposition covers the existing ten-stage runner against the previously admitted resources. It does not admit the separate explicit reference audit, publication manifest, remote publication, source export, original receiver behavior, whole-module/JDK capability, or source gates. The reviewer did not rerun any helper, generator, build, test, installer, validator or remote operation.

The actual execution receipt is `verification-v3/receipt.json`, SHA256 `1f4ac9825d975520a04b7dc5ac1b282d96be32601c7abe2b0cfb676b0ff2df09`. Its source is `0c53b4f1dfe66c7eff05c8047b8fc681d26fecb8`, root `0361e4a07b77a01df170523033fef01c4052ddc5`. The exact plan seal remains `0aa1f4a3e080c36d233066f0a6e1dc8e1047e6f0592dba60829b8cca3177df64`, with E3 destination preimage `0994ecd65e86600f417f4d8702838d8c2663af61`.

## Actual invocation and stage observations

The invocation capture binds runner SHA256 `496e1877914e15f4342111c37020a2d58790038516b62872680427e585b21f50`, final input `df6713ad19ce80daad0383fbd5c3716412e2dca133e1db7e0150b88e141313b5`, and the prior resource-admission report `df198e3ccda995d5d40fe9e17aca02507b3cb6b5077314c05b3c06b3eeb2cd28`. It records an ordinary `python -B` invocation, exit zero, empty invocation stderr, and all ten expected child exit outcomes. The input freeze precedes all child commands; the actual stage times are ordered within the invocation interval on 2026-10-05, 17:40:48–17:40:56 UTC.

The exact per-stage JSON records, stdout and stderr identities were checked against the aggregate receipt. Their commands and working directory match the admitted runner and receiving overlay. No owner or resource edit was needed to pass this epoch.

| Actual stage | Observed result |
| --- | --- |
| E3 preimage check | `before`, four files, zero writes, exit 0 |
| Focused Maven `clean verify` | Twelve Java tests passed; no failures, errors or skips; exit 0 |
| Python packet contract | All six named methods report `ok`; `Ran 6 tests` / `OK`; exit 0 |
| Apply | `after`, four files, four writes, exit 0 |
| Apply fixed point | `after`, four files, zero writes, exit 0 |
| Rollback | `before`, four files, four writes, exit 0 |
| Replay | `after`, four files, four writes, exit 0 |
| Final check | `after`, four files, zero writes, exit 0 |
| Whole-map validation | `MIGRATION_MANIFEST_VALID completion=INCOMPLETE`, exit 0 |
| Completion refusal | Same VALID/INCOMPLETE result, expected exit 2, with unresolved/blocked reasons |

Every installer response reports the same admitted plan seal. The separate completion command's nonzero exit is the required negative outcome, not an execution failure disguised as a pass. Its stderr explicitly retains incomplete source inventory/ownership closure, both blocked JCC capabilities, incomplete destination gates and baseline reconciliation obligations.

## Actual Java and Python evidence

The copied Surefire XML is SHA256 `09d606a9675a7aa17a64945e499c8f8062bd75a42cce2ed53851309aa3efd001`; it equals the actual generated Surefire report byte-for-byte. Its twelve distinct case names equal the twelve methods in the admitted `JccSourceRecoveryHandoffTest.java`. The XML has exactly twelve testcase nodes, zero failure/error/skipped nodes, and twelve/zero/zero/zero suite counts. It reports Java 21.0.2 at the configured JDK path and UTF-8 encoding. Maven stdout independently reports the same test counts and `BUILD SUCCESS`.

The Maven command uses offline mode, the exact existing local repository, empty explicit user settings and the admitted verification POM. The log shows compilation of one retained recipe owner and one test source with javac release 21, and twelve copied resources. The POM retains its strict warnings/fail-on-warning configuration. Maven stderr contains only the retained SLF4J missing-binding/NOP-logger diagnostic; it contains no compilation or test failure. This is the focused verification project, not an entire owning-module or JDK build.

Python stderr lists exactly the six admitted test method names, all `ok`, then `Ran 6 tests` and `OK`; stdout is empty. The **132 refusal checks** are a finite count derived from the exact executed test loops: eight missing/drifted preimages, fourteen missing/drifted guards, fourteen mixed before/after states, four foreign postimages and four missing postimages, each exercised across check/apply/rollback. They are 44 conditions across three modes, not 132 test methods or 132 separately persisted event traces. The executed six-method success and unchanged test bytes support this loop-derived count.

These results exercise the actual canonical text recipe, named entrypoint/serialization, exact four resources, retained sealed installer, fixed point, rollback/replay and finite refusal controls already described by the resource admission. They do not rerun the original four-test receiver fixture or any Java/JNI source lane.

## Frozen inputs and materialized outputs

`verification-v3/inputs.json`, SHA256 `6367c7d343ece1f1547e4db7873180f4dfae6dda83212edadf0564e0a93bc341`, records **391 fixed receiving inputs plus 617 external inputs**, totaling **1,008 unique paths**. The reviewer independently re-read and rehashed every one, including byte count, SHA256, Git blob identity and permission mode; all still exactly match the pre-run freeze. No frozen file is missing.

This explicitly includes all 527 declared current be92 bodies, the ten receiving current-context receipt copies, the two exact malformed-UTF8 fixture bodies added by the corrected V3 freeze, all nineteen selected current source bodies and all fifty-five proof bodies. Declared current-context identities and modes also match their original authority. The prior source custody object in the run receipt equals the admitted input, including the two raw-body/SHA256-readback false flags and recorded failed reads.

The four captured operational preimages match the admitted E3 resources. All four current operational bodies now equal the admitted afterimages byte-for-byte and preserve their original modes. The exact plan/manifest, bound Java/Python proof owners, YAML/POM and all seven guards remain unchanged. All 89 nonoperational E3 publication payloads remain exact; the old operational preimages remain available in the unchanged historical resources. The 134 source-recovery copies and ten context receipts were part of the receiving freeze, so their previously reviewed data and provenance remain exact.

At review time, the owner had subsequently staged 107 additive task-evidence paths beyond the run's frozen receiving set. Those paths are listed separately in `POSTRUN_ADDITIONAL_PATHS.json`. They do not alter any frozen path or the four checked operational bodies. They are not backdated into the execution freeze and are **not admitted by this execution review**; their reference-audit/manifest/publication review belongs to the separate final review. The initial independent inspector's overly broad current-directory-equality assumption and its boundary correction are preserved under `inspector-revisions/`.

## Scope and remaining boundaries

The actual runner concludes `PASS_WITH_COMPLETION_BLOCKED`. Source qualification remains BLOCKED, and source export, source-qualified destination capability, receiver behavior rerun, owning-module coverage, JNI/platform/JDK acceptance and remote-write flags remain false or explicitly unexecuted. The command environment and offline/settings observations do not establish historical JVM-loaded-byte attestation, a hermetic full-module build, or dependency qualification beyond the admitted focused run.

This review did not inspect or execute the new explicit reference checker, and it makes no claim about its separately reviewed outcome. Nor does it preapprove later evidence copies, the final publication manifest or GitHub publication. Actual immutable input/resource and custody boundaries remain those established by the earlier bound-input and resource reviews.

`EXECUTION_CHECKS.json` records the independent findings, method names, stage/log identities and preservation counts. `inputs/` snapshots the exact receipt, input freeze, invocation, all ten stage records/logs, settings and actual Java XML. `inspect_execution.py` uses only standard-library reads/hashes/JSON/XML/AST inspection and writes only this review directory. No candidate or gate was rerun to produce these observations.
