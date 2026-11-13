<!-- SPDX-License-Identifier: Apache-2.0 -->
# viewport-regions-retire-20261107

Follow-up of merge-loss cases 10 and 14 (`mergeloss-s3-20261106`, #2656, decision SUPERSEDED). Two retirements, each
with a status row, none silent:

* **R1** `m3/collections/src/test/native/m3_mask_viewport_regions.c` (landed by `2f9a1a2a9d0e`, #1497): an
  instrumented-JNI test of `d0cec9717801`'s `project()`. Decided by running it: retargeted from `REGION_SIZE` to
  master's `M3_VIEWPORT_CHUNK` it compiles with `gcc -Werror` but fails, and the probe shows the failing assertions are
  the allocation/read/write counts of the replaced streaming design (4097-row disjoint case: asserted 0/20/5, master
  1/99/1; overlap case: asserted 0 allocations, master 1). `7fb4de24b1e8` deliberately publishes once from
  O(selected) scratch, so the test is RETIRED citing it. Its other assertions are covered by the live
  `M3MaskViewportWindowContract`. `verify_mask_viewport.py`, its only caller (not wired to CI), drops the region steps
  and passes on master (it failed before). No CTest exists to wire into.
* **R2** #2547's held `M3MaskViewportNative.java` (pr1766): a second `projectFramePage` with master's exact descriptor.
  RETIRED citing #2656; a `RETIRED` row (hold.py's five columns) is appended to
  `m3/qualification/retained-twins-core-lanes-20261106/HELD.tsv` and the held copy is removed.

| file | content |
|---|---|
| `DECISION.tsv` | per leaf: decision, cited commits, evidence, action |
| `retire.py` | recipe VIEWPORT_REGIONS_RETIRE v1: hash-pinned EDIT and RETIRE rules, `--leaf`, `--check`, refuses drift |
| `EDITS.tsv` | file, before/after sha256 (`-` = removed), leaf, decision, cited commits, rule (written by retire.py) |
| `tools/stage.sh`, `tools/probe.sh`, `tools/make_probe.py` | the R1 decision run: unmodified, retargeted (-Werror) and non-fatal probe of the retired test against master |
| `GATES.tsv` | gates with numbers |

Replay: `python -I retire.py --check` (exit 0, changed=0); `bash tools/stage.sh <repo> <master> <dir> &&
TMPDIR=<tmp> JAVA_HOME=<jdk21> bash tools/probe.sh <dir>`.
