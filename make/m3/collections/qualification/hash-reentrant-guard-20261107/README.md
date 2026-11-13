<!-- SPDX-License-Identifier: Apache-2.0 -->
# Re-entrant key/value callbacks on the hash slot-map owners (S1/S2 of #2676)

#2676 guarded M3TreeMap against comparators that change the map during a lookup and left two items open:
- S1: key `equals`/`hashCode` running inside the hash owners' `findSlot`.
- S2: value `equals` inside `remove(k,v)` and `replace(k,old,new)` on every slot-map owner.

This leaf measures both first, against the java.util twin of every owner, then guards them by recipe `apply.py`
(HASH_REENTRANT_GUARD v1, 4 hash-pinned rules: M3SlotMap, M3HashMap, M3LinkedHashMap, M3WeakHashMap).

## Measurement (REFERENCE.tsv)
`M3HashReentrantLookupSelfTest` covers six M3 owners, each paired with its java.util twin:
M3HashMap, M3LinkedHashMap in insertion and access order, M3WeakHashMap, M3HashSet and M3LinkedHashSet.

- Every case runs 33 operation shapes (28 map, 5 set) over keys 1..7, with spread hashes and with fully colliding hashes.
- For every user-callback call N, a one-shot action fires at call N. It is either clear + re-insert of 50/60, or,
  for operations on a new key 9, an insert of key 9 itself.
- Each outcome is classified:
  - linearizable: BEFORE (the callback's change first) or AFTER (the operation first);
  - FAILFAST: ConcurrentModificationException, with exactly the callback's content;
  - WRONG: none of the above;
  - DAMAGED: invariants or map consistency broken.

Base (#2676 head): 330 M3 cases fail, of three kinds:
- **Foreign reads and writes:** `get(2)` returns `v60`, and `remove(2)` erases key 60.
- **Released-slot damage:** `remove(6)`, and `get(6)` in access order, erase or move a released slot.
- **Lost or duplicated racing inserts:** absent paths looked the key up twice; M3WeakHashMap.put hashed the key twice.

The java.util twins answer reads with the pre-change value, and are themselves damaged in many mutating shapes
(HashMap: 57 DAMAGED).

## Guard (not stricter than the JDK)
- A lookup walk whose key `equals` changed the structure walks again from the start, so the answer reflects the
  post-change state. It throws CME only after `M3SlotMap.REWALKS` (8) disturbed walks.
- `remove(k,v)` and `replace(k,old,new)` look the key up again when the value's `equals` changed the structure.
- `putIfAbsent`, `computeIfAbsent`, `compute` and `merge` do a single walk. `locate` returns the slot plus the key's
  hash. A present key is written through its slot. An absent key is inserted by its hash (`insertAbsent`) without
  calling key code again. This is the callback sequence of java.util.HashMap's compute.
- `M3WeakHashMap.put` hashes the key once, as WeakHashMap does.

Head: 0 of 1018 M3 cases fail. M3 fails fast only in `values().remove`, the iterator's CME, where every twin fails
fast too. The test asserts the brief's rules:
- never DAMAGED;
- WRONG only on a read where the twin is WRONG too;
- FAILFAST only where the twin is not cleanly linearizable.

The shared M3SlotMap edits keep #2676's tree contract: 796 cases, 0 failures; P2-098 checks=975.

## Other evidence
- `DECISION.tsv` records the decision per owner.
- `MUTANTS.tsv` (`mutants.py`) holds 16 mutants: 13 killed, and 3 backstops (insertAbsent preconditions) that survive by design.
- `BENCH.tsv` (`bench/bench.sh`) compares M3HashMap get/put on base and head: paired rounds in one JVM, with an A/A
  run as the noise band.
- `GATES.tsv` has the remaining gates; `RECIPE_MEMORY.tsv` the lessons.

The linked owners need the m3_arrays JNI library. Build it from `m3/arrays/src/main/native` with CMake, then pass
`-Dm3.arrays.native.path=...`, or use the `m3-collections-native` profile. Without it the test skips those owners and
names them.

## Re-run
After `mvn package` of m3/arrays and m3/collections with the committed poms:
1. `python -I apply.py --check`
2. `JAVA_HOME=<JDK 21> NATIVE_LIB=<libm3_arrays> bash differential.sh <base classes> <head classes> <m3-arrays jar> <out>`
3. `python -I mutants.py --java-home <JDK 21> --classes <head classes> --test-classes <head test-classes> --arrays-jar <jar> --native-lib <libm3_arrays> --work <dir>`
4. `JAVA_HOME=<JDK 21> bash bench/bench.sh <base classes> <head classes> <out>`
