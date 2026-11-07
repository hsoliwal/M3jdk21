<!-- SPDX-License-Identifier: Apache-2.0 -->
# Recipe planner-cycle-fix-20261107: the greedy planner closes dependency cycles

Correctness fix on top of `planner-family-20261107` (PR #2679). Above `EXACT_LIMIT` (20) candidates,
`M3IndexPrecomputePlanner` plans greedily, and its `collect()` recursed into a capability's dependencies before
marking the capability. A dependency cycle therefore never ended and threw `StackOverflowError`. Develop
(`78cf2ad4c7e5`) carries the same donor blob `18b16bda6e3b`, so there is no newer donor fix to port.

## Contract

The brute-force reference (up to 12 candidates) and the exact search (up to 20) admit a capability only together with
all of its dependencies, so a cycle is one dependency-closed group. On cyclic catalogues the brute force and the exact
planner agree: 238 cyclic brute-force plans in the dump. The greedy pass is made to agree by **visiting each node
once**: the closure of a cycle member is the whole group. It does not refuse, because a refusal would reject catalogues
the exact search plans. The class javadoc states this contract.

## Recipe

`apply.py` (PLANNER_CYCLE_FIX v1) applies the hash-pinned rows of `EDITS.tsv`. The payloads are in `edits/`:

- E1: `import java.util.ArrayDeque`.
- E2: the cycle contract in the class javadoc.
- E3: `collect()` uses an explicit stack and visits each capability once. There is no recursion; the visited set is
  the result set.

The recipe refuses a drifted preimage and is a fixed point after one run. The relocation recipe
`planner-family-20261107` now runs as relocate.py v5. Its new PATCH row accepts this recipe's pinned after hash, so
both recipes stay at one fixed point. Its donor differential leaves out the CYCLE_PROBE, CYCLIC and TOTAL lines, the
only lines where the fixed class departs from the donor.

## Tests and gates

- `M3IndexPrecomputePlannerCycleTest`: 5 tests (two-cycle, group that does not fit, mandatory member, mandatory
  group over budget, strict cyclic corpus). All 5 fail with StackOverflowError on the base classes and pass on head.
- The dump `M3IndexPrecomputePlannerDump` gains CYCLIC lines from a separate seed, checked against the recomputed greedy
  pass, and a strict mode. Its PLAN, TUPLE and HASH lines stay byte-identical with the base dump on the JIT and under
  `-Xint` (`differential.sh`).
- `mutants.py`: four one-edit mutants of the patched `collect()` must be killed.
- `class_identity.sh`: only the declared planner and dump classes change.
- `DECISION.tsv`: the contract, the fix, and the TWIN_PENDING note for the donor (file, lines, repro).

```
python -I m3/ports/precompute/recipe/planner-cycle-fix-20261107/apply.py [--check]
bash m3/ports/precompute/recipe/planner-cycle-fix-20261107/verify.sh <com.synexia clone> 0616288bfa7d91eb0abb754c7a29707f10318752 <scratch> <built base m3/ports/precompute> <junit-platform-console-standalone.jar>
```
