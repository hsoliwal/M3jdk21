<!-- SPDX-License-Identifier: Apache-2.0 -->
# Recipe residency-pressure-alloc-20261107 (RESIDENCY_PRESSURE_ALLOC v1)

This recipe closes the allocation debt that #2653 (residency fold leaf PS) left open in its `GATES.tsv` (`allocation_jit`).

After #2653, `M3ObjectResidency.targetBudget()` built `HeapSnapshot.current()` inside the `PRESSURE_AWARE` test and
read one field of it. Whether that 48-byte record disappeared depended on C2 scalar replacement:

* the resident `use()` hit path allocated 48 B/call in 2 of 8 JIT runs;
* it allocated 48 B/call under `-Xint` every time;
* in one `-Xbatch` JVM it went from 0 (fixed budget) to 48 (`PRESSURE_AWARE`).

## The edit

`apply.py` makes one anchored rule (leaf `PA`). The preimage is pinned to #2653's postimage, blob `b8793d63a3cd`, and the
postimage to its sha256. `targetBudget()` now calls the new private static `sampledHeadroomBytes()`, which:

* reads `Runtime.totalMemory()`, `freeMemory()` and `maxMemory()` in `HeapSnapshot.current()`'s order;
* applies the same `used` clamp and the same headroom clamp;
* returns a `long`.

So no path builds an object. There is no cache and no epoch: every boundary still samples the running heap at the same
points, so budgets and victims are unchanged by construction. `HeapSnapshot` (public API) is untouched. The options
that were considered and rejected are in [DECISION.tsv](DECISION.tsv): reusing the snapshot, or caching it with a
sampled-at epoch.

```
python -I m3/collections/qualification/residency-pressure-alloc-20261107/apply.py [--check]
```

## Proof

* `M3ObjectResidencyPressureAllocationTest` (new) checks four `PRESSURE_AWARE` boundaries: resident `use()`,
  `signalMemoryPressure()`, a budget-refused reuse, and an admission net of its `Decision`. Each must stay below one
  byte per call over 20,000 calls.
  * On master classes under `-Xint` it fails 4 of 4.
  * On the head it passes 4 of 4, both JIT and `-Xint`.
* `oracle/alloc.sh` runs #2653's `PressureSamplingAllocationProbe` unchanged on master and head classes. Each side gets
  8 JIT runs, 8 `-Xbatch` runs (fixed budget, then `PRESSURE_AWARE`, in one JVM) and one `-Xint` run.
* `tools/mutants.py` with [MUTANTS.tsv](MUTANTS.tsv): the master gate, the head gates and 4 mutants, all killed.
* The neighbouring oracles were replayed with `../doop-facade-fixes-20261107/tools/regression_oracles.sh` and are
  byte-identical on master and head classes:
  * #2565 L066;
  * #2610 victim progress;
  * #2614 donor semantics;
  * #2653's 560,000-op seeded trace, JIT and `-Xint`;
  * #2566's donor differential and facade dump.

Gate results are in [GATES.tsv](GATES.tsv).
