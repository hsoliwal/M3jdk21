<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# E1 source-built core evidence

The original integrated jtreg run is **failed: 23 tests passed, 4 failed, and 1
timed out**, out of 28 tests. It executed 63 main actions: 58 passed, 4 failed,
and 1 timed out. The full exported action record is `JTREG_E1.json`; its
separate stdout and stderr fields preserve every recorded stream. The test
summary and harness arguments are also included. A later E2 checkpoint must
report rebuilt-image results separately; these original results stay intact.

## Source epoch

E1 is the source tree published in target PR [1657](https://github.com/hsoliwal/M3jdk21/pull/1657),
commit `f35342202149b98465bb4e9e6be807eb0cf3f762`, with 49 received file changes
over `a115db5d8d9a53c20bf6f6880286a967a80a8dda`. Its canonical source and
qualification checkpoint is PR [12680](https://github.com/hsoliwal/com.synexia/pull/12680),
commit `0ee3e62241cb1252f2d018a33969f64e66c2b2fe`. These identify different
repositories. `SOURCE_HASHES.tsv` uses frozen E1 postimages or exact parent
blobs, never later working-tree source bytes.

## Failures and bounded receiving evidence

| Original E1 result | Diagnosis and separate evidence | Remaining boundary |
| --- | --- | --- |
| Char-array composition: missing private method | Restored historical fixture depends on `admitArrays` and `admitArrayRanges`, absent in both parent and E1 owner. Exact test blob and restoration lineage are in `DIAGNOSTICS.json`. | Fixture and failure preserved; historical multi-array API reconciliation is deferred. |
| Concat reference DAG: null conversion assertion | Existing canonical Factory correction and donor test are checked through 18 commands, all matching expected outcomes. Candidate DAG runs pass 23 checks in both compact modes. Four separate source-gate cases retain unrelated protections. | Isolated Factory patch, not an E1 image change. Six baseline failures are expected witnesses, not authoring failures. |
| Concat overflow: 480-second timeout | Original stack remains inside repeated leaf equality traversal. An identical-child-coordinate shortcut passes the unchanged overflow fixture's 111 checks. Five semantic proof commands guard equal coordinates and forced route collisions; both baseline and candidate pass 59 checks in both compact modes. | The 111-check run uses one isolated Pool patch and default compact strings. No candidate timing or numerical speedup is claimed. |
| Pool metadata bounds: short input admitted | Test-only receiving fixture selects 32-byte payloads beyond both immediate profiles. Default, immediate-disabled, and noncompact runs each pass 6660 checks. | The original assertion is preserved; the fixture now reaches the intended native-budget refusal lane. |
| Pool bounds: global accounting assertion | Original M3-disabled action passes 204 checks. Diagnostic M3-enabled run observes +65 bytes. A later temporary fixture experiment fails another assertion with a -8-byte discrepancy and is excluded. | Original fixture and runtime remain unchanged; this inherited global-counter assumption remains unresolved. |

`RECEIVING_PROOFS.json` preserves exact commands, candidate source seals,
expected and actual outcomes, and complete captured combined logs for these
separate probes. Combined streams were not retrospectively split. The
nullable-concat probe source is included for review. No patch-only proof
changes the failed E1 suite status.

The accepted receiving source gate's PASS is retained separately from the four
guard-proof outcomes. The latter include an expected baseline rejection and
two mutation rejections. This source-gate PASS does not advance the failed E1
aggregate or the repository's broader String phase gate.
`DIAGNOSTICS.json` retains the original baseline phase checker's failure,
`ValueError: String successor chain length drift`; no phase advancement is
claimed from these bounded implementation proofs.

## Build and capture scope

The successful full build and final E1 rebuild logs are included under `build/`.
`SOURCE_BUILD.json` records the exact make command, configure arguments,
toolchain versions, and compact earlier failed-build summaries. Warnings as
errors remained enabled. The final configure disabled the javac server after
socket/transport failures. The earlier full build predates the final E1 source
stage; only the final rebuild is bound to E1.

The packet contains source/log evidence, no binaries or credentials. JTR
environment, configuration, and rerun dumps are omitted. Paths, builder names,
and timeout process addresses are normalized as described in
`SANITIZATION.json`. `PACKET_MANIFEST.json` records both original input hashes
and public file hashes. The collections packet provides its separate broader
module proofs; the 28-test integrated suite is retained here in full.
