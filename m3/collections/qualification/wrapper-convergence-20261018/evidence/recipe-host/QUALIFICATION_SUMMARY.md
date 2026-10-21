# Recipe host qualification — 2026-10-18

All eight authorized stages passed. The two machinery fixture lanes and the one-file read-only iterator product pass each passed five real OpenRewrite SDK tests (15 total, zero failures, errors or skips). No host or canonical engine repairs were required during execution.

| Stage | Result | SDK tests |
|---|---|---:|
| LINT | PASS, exit 0 | — |
| FIXTURE_JAVA_COMPILE | PASS, exit 0 | — |
| FIXTURE_JAVA_SDK_TESTS | PASS, exit 0 | 5 |
| FIXTURE_TEXT_COMPILE | PASS, exit 0 | — |
| FIXTURE_TEXT_SDK_TESTS | PASS, exit 0 | 5 |
| READONLY_ITERATOR_LINT | PASS, exit 0 | — |
| READONLY_ITERATOR_COMPILE | PASS, exit 0 | — |
| READONLY_ITERATOR_SDK_TESTS | PASS, exit 0 | 5 |

The immutable recipes remain pinned to Synexia `98368e107f4a205d852ef958cd48c1ee633219b1`: the existing Java/text snapshot engines and two owner-closure files. The actual Java 21 compiler uses `-Xlint:all -Werror`; dependencies are OpenRewrite 8.17.1 and JUnit Jupiter 5.10.2. The final four Maven stages executed offline.

Each SDK lane demonstrates exact replay, zero-change fixed point, equal results and patches after input reversal, missing/drift/duplicate refusal, post-scan refusal, unchanged unrelated source and guarded output retention. Java lanes additionally refuse a target represented as plain text. Raw command/stdout/stderr/exit receipts and emitted SDK metadata are identified in `QUALIFICATION_SUMMARY.json`.

The read-only iterator product output has SHA-256 `032bffd456e67c7568b028489ceec85f24510469b3a0e00571ab36cd17ae3b75`. Its 341 frozen inputs include the parent contract and oracle files, exact receiver preimage, and 301 actual baseline parser-classpath files (295 compiled classes). None changed during its gates. The output remains under the host candidate directory for parent inspection and serial receiving.

This evidence proves recipe execution. Receiving Java behavior/API/allocation checks and C/JNI compilation/runtime/ABI checks remain separate parent tasks. The C-text fixture alone makes no C compilation or runtime claim. Existing historical host-ready documents remain unmodified; they record the earlier pre-execution state.

The initial report assembly encountered the older lint receipt schema before producing output; `SUMMARY_ASSEMBLY_ATTEMPT_1.txt` preserves the diagnostic. Summary assembly was corrected without changing any source, test or gate receipt.
