<!-- SPDX-License-Identifier: Apache-2.0 -->
# core-contract-matrix re-seal (2026-11-07)

`CURRENT_INPUTS.json` pinned m3/core as of 306b54ad6c70. `qualify.py` hard-codes the cohort size (59 files, 16
programs, 64 executions). Since then 12 pinned files changed and #2702 and #2707 added two test programs. The
workflow therefore refused every m3/core change at its identity step.

`reseal_ccm.py` carries the identities to the current master:
- the old order is kept for files that remain, and the 5 m3/lexicon inputs stay pinned;
- new tests are appended;
- programs follow build.sh order;
- each new marker is read from its test source;
- the three size constants in `qualify.py` follow.

`check_admission.py` runs `qualify.py`'s own admission without the build. It fails on the old pair and passes on the
new one.

The build replay still stops at `M3NumberSpaceContractTest`, a pre-existing failure (GATES.tsv row 4). The test
literal was renamed to `m3-dictlang/...`, while the manifest TSV keeps the donor path `synexia-dictlang/...`. That fix
belongs to the lexicon owner; this change makes the workflow fail at that real test, not at stale pins.
