<!-- SPDX-License-Identifier: Apache-2.0 -->
# viewport-retire-nodelete-20261107

Split of #2673 without deletions. #2673 retires two tracked files (the stale viewport region test and #2547's held
`M3MaskViewportNative.java`) and is held for the user's explicit approval of those deletions. This directory carries
everything else, and the deletions only behind an explicit flag:

* **R1** `m3/collections/src/test/native/m3_mask_viewport_regions.c` is **RETIRED_PENDING_DELETE**, citing
  `7fb4de24b1e8`. It was decided by running it: retargeted to `M3_VIEWPORT_CHUNK` it compiles with `gcc -Werror` but its
  allocation/read/write counts describe `d0cec9717801`'s replaced `project()`. `verify_mask_viewport.py`, its only
  caller, drops the region steps now (it failed on master, it passes here). The test file stays tracked.
* **R2** #2547's held `M3MaskViewportNative.java` is **RETIRED_PENDING_DELETE**, citing #2656: a
  `RETIRED_PENDING_DELETE` row is appended to `m3/qualification/retained-twins-core-lanes-20261106/HELD.tsv`. The held
  copy stays tracked.

`retire.py` (VIEWPORT_REGIONS_RETIRE v2) applies the edits; its RETIRE rules only report `PENDING_DELETE` unless
`--delete` is given (default off). Once the user approves, `python -I retire.py --delete` performs the two deletions
(then `--check` stays `changed=0`).

| file | content |
|---|---|
| `DECISION.tsv` | per leaf: decision, status, citations, evidence, action now, action pending the user |
| `retire.py` | recipe VIEWPORT_REGIONS_RETIRE v2: hash-pinned EDIT and RETIRE rules, `--leaf`, `--check`, `--delete` (default off), refuses drift |
| `EDITS.tsv` | file, before/after sha256 (`-` = removed once `--delete` runs), leaf, decision, citations, rule |
| `tools/stage.sh`, `tools/probe.sh`, `tools/make_probe.py` | the R1 decision run (unmodified, retargeted with -Werror, non-fatal probe) against master |
| `GATES.tsv` | gates with numbers |
