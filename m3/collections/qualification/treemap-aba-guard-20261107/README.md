<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3TreeMap re-entrant lookup guard (follow-up of #2641)

#2641 made `M3TreeMap.eraseAt` refuse a slot that a re-entrant comparator had already released. The owner-lane
review found the residual ABA: a comparator inside `remove(2)` runs `clear(); put(50); put(60)`, key 2's slot is
live again holding 60, the height check passes, and `remove(2)` erases 60 and returns `"v60"`. The same window
exists wherever a slot is found through user comparator calls and then read, written, erased or returned.
`SITES.tsv` lists 85 such sites (`sites.py`) across `M3SlotMap`, `M3OrderedSlotMap`, `M3TreeMap`, `M3TreeSet` and
the filtered views over tree owners.

Recipe `apply.py` (TREEMAP_ABA_GUARD v1, three hash-pinned rules) applies one rule where the slots come from, not
at each use: **after a comparator call that changed the structure, M3TreeMap neither reads another slot nor
returns one; it throws `ConcurrentModificationException`.**

| where | guard |
|---|---|
| key walks: `Tree.find` (findSlot, entrySlot), `Tree.near` (neighbors, bounded endpoints), put/defer descents | revision check after every comparator call: a walk never hands the comparator a released slot's key and never returns a reused slot |
| bounded endpoints and steps (`firstSlot`, `lastSlot`, `nextSlot`, `previousIndexedSlot`) and `near`'s bound probes | `located(expected, slot)`: the slot is returned only if the revision read before the lookup still holds |
| `put` admission | revision read before the admission probes: put is the write half of compute/merge/computeIfAbsent/putIfAbsent |
| cursor creation (`M3SlotMap.slots`, the #2641 mutable slot cursor) | never fails (P2-098 "capture epoch before firstSlot"): the cursor reads its epoch first and starts through `cursorStart`, which turns a structural change into -1; the first call throws |

`eraseAt`'s height check stays as a backstop. Comparator calls made before any slot is read (findSlot's and near's
bound/type probes, defer's admission) need no check: the walk that follows sees the changed structure
consistently. The hash owners are out of scope (`DECISION.tsv` S1, S2).

Evidence:
- `M3TreeMapReentrantLookupSelfTest` / `M3TreeMapReentrantLookupTest` (JUnit) run 53 operation shapes on maps, sets
  and lazy maps, each over root, bounded and descending-bounded views, plus 2 cursor-creation cases. For every comparator call N, a one-shot
  `clear + put(50) + put(60)` at call N must end in one of two outcomes: fail-fast (CME, with the owner holding
  exactly the action's content), or exactly the outcome of running the action first and the operation second.
  Either way the AVL invariants must hold. Base fails, head passes (`ABA_SHAPES.tsv`).
- `differential.sh` runs the seeded filtered-views differential, the B4 repro and the shapes on #2641's head (base)
  and this leaf (head). `classify.py` explains every differing line as the guard's (`DIFFERENTIAL_SUMMARY.tsv`).
- `mutants.py`: every guard of this recipe and every check of #2641 is mutated, then compiled and run against the
  four oracles (`MUTANTS.tsv`).
- `GATES.tsv`: real-pom builds, module tests, recipe fixed points, class identity, naming, protected owners.

The filtered-views differential oracle (`../filtered-views-owner-20261106/oracle`) now records a failed cursor
(re)open instead of crashing. Its outputs on #2641's classes are byte-identical to the recorded ones.

Re-run, after `mvn package` of m3/arrays and m3/collections with the committed poms:
`python -I apply.py --check`; `python -I sites.py --check`;
`JAVA_HOME=<JDK 21> bash differential.sh <base target/classes> <head target/classes> <m3-arrays jar> <out>`;
`python -I mutants.py --java-home <JDK 21> --classes <head classes> --test-classes <head test-classes> --arrays-jar <jar> --oracle-classes <out>/head --head-diff <out>/head.out --head-repro <out>/repro-head.out --work <dir>`.
