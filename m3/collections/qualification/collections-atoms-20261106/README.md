<!-- SPDX-License-Identifier: Apache-2.0 -->
# m3/collections atoms from com.synexia (2026-11-06)

Plan rows 6 and 7 of the collections/arrays convergence plan (C:/m3-superset/conv/plans/collections-arrays.tsv):
two first-party atoms received from hsoliwal/com.synexia `develop` pinned at
`ca1aafc4d94f9dd1ee288226dcca070cc60991c9` as recipe-applied postimages. Decision per atom: **ABSORB** (neither was
already on master: the master bodies were the 3-probe conditional remove and the 1.5x growth jump).

| atom | donor crate (tree id) | donor blobs | owner (master blob) |
|---|---|---|---|
| INTINT_CONDITIONAL_REMOVE | `synexia-openrewrite-recipes/crates/m3-intint-conditional-remove-20261030` (`ca5c636be146`) | preimage `d876c246ea06` (com.m3 package) -> postimage `3d17af324de9` (sha256 `22a0ff07d173...`) | `m3/collections/src/main/java/jdk/m3/collections/M3IntIntHashMap.java` (`d50114519d36`) |
| EVICTING_QUEUE_GEOMETRIC_GROWTH | `synexia-openrewrite-recipes/crates/m3jdk21-evicting-queue-geometric-growth-20261024` (`cee4e21d4a77`; README `bc2d2c4b23a8`, GROWTH_ALLOCATION_COMPARISON.tsv `e313eaf38cc2`) | source sha256 `488d21e7...` -> candidate `e4d4f9fa...` (com.m3 package) | `m3/collections/implementations/evicting-queue/src/main/java/jdk/m3/collections/queue/M3EvictingQueue.java` (`e6f8b47da01c`) |

## Recipe

`apply.py` (COLLECTIONS_ATOMS_APPLY v1) reads `EDITS.tsv` (file, before_sha256, after_sha256, rule, donor) and
`rules/<rule>.before|.after`: the snippet must occur exactly once in a preimage whose sha256 is pinned; a file
already at the pinned postimage is ALREADY (fixed point); any other digest is REFUSED. The donor hunks were derived
by `git diff d876c246ea06 3d17af324de9` (atom 1) and the crate README's single rule `oldLength / 2L` ->
`(long) oldLength` (atom 2), and re-pinned against the owners' current blobs because the JDK-naming rename changed
the preimage bytes. Each edited method carries a `//FLLDCIM: MRL` anchor.

```
python -I m3/collections/qualification/collections-atoms-20261106/apply.py --root .   # changed=3, then changed=0
```

Hunks (anchors omitted):

```diff
     public boolean remove(int key, int expectedValue) {
-        if (!containsKey(key) || getOrDefault(key, 0) != expectedValue) {
+        if (key == 0) {
+            return hasZero && zeroValue == expectedValue && remove(0);
+        }
+        int pos = find(key);
+        if (pos < 0 || values[pos] != expectedValue) {
             return false;
         }
         return remove(key);
```

```diff
-        long jump = oldLength < 64 ? (long) oldLength + 2 : oldLength / 2L;
+        long jump = oldLength < 64 ? (long) oldLength + 2 : (long) oldLength;
```

The third rule moves the module's existing oracle with the atom, as the donor README states ("the source-bound
Java21 target oracle is refined to assert exactly the new deterministic count"):
`M3EvictingQueueCanonicalContractProbe` asserted `expansions > 700_000L` (master emits 739,429); it now asserts
`expansions == 453_002L`, the donor README's count, which the live class reproduces exactly.
`.github/workflows/m3-evicting-queue-canonical-owner.yml` pins the new postimage sha256 and normalizes the one admitted
growth line (checked locally by running the workflow's Python step) and runs the new growth probe.

## Proofs

* `src/test/java/jdk/m3/collections/M3IntIntHashMapConditionalRemoveDifferentialTest` (JUnit, in `mvn test`):
  seed `0x4D33494E54494E54`, 1,000,000 ops over put, remove(key), remove(key, expected), getOrDefault, containsKey,
  size; identical sha256 dumps for the live class, `java.util.HashMap<Integer,Integer>` and the pre-change class
  compiled at test time from the hash-pinned fixture `src/test/resources/.../M3IntIntHashMap.preimage.java.txt`
  (master blob d50114519d36) into package `jdk.m3.collections.atoms.preimage` in its own class loader. Probe count:
  the class is final and `find` private, so no subclass hook exists; the javap -c listing of `remove(int,int)` must
  call `find:(I)I` once, `containsKey`/`getOrDefault` never, `remove:(I)Z` twice (zero-key path and delegation).
* `implementations/evicting-queue/src/test/java/jdk/m3/collections/queue/M3EvictingQueueGeometricGrowthProbe`
  (main probe, the module's existing pattern; run by the workflow): growth sequence of the private
  `expandedLength` from 1 past `1 << 26` printed and checked against the rule (2n+2 below 64, 2n from 64): 25 steps
  (pre-change: 39); GROWTH_ALLOCATION_COMPARISON.tsv physical slots after create+fill at max 1,000,000:
  1024 -> 1120 (pre-change 1188), 65536 -> 71680 (68485), 1000000 -> 1000001 (1000001), both columns matched;
  seed `0x4D33455649435451`, 1,000,000 ops (add, poll, peek, remove(Object), size/remainingCapacity, iterate,
  toArray, iterator.remove, addAll) over limits {0, 1, 2, 15, 16, 63, 64, 65, 1000, 65536}: identical sha256 dumps
  for the live class and the pre-change class compiled from the hash-pinned fixture (master blob e6f8b47da01c).

Gate results: GATES.tsv. Logs on the executing host: S:/m3-scratch/eb-conv/collatoms/.

## Follow-ups (not in this change)

* com.synexia side: retire the four evidence-only crates `m3jdk21-evicting-queue-canonical-owner`,
  `-canonical-model-fuzz`, `-growth-arithmetic-proof`, `-physical-growth-ceiling` (memory/events only), and mark
  the two received crates ABSORBED.
* `m3/tooling/name-contract/receipts/evicting-queue-canonical-owner.json` still pins canonical sha256 `77ba9ea6...`,
  already stale on master (55f35c29...); left for the name-contract owner.
* Pre-existing on master, untouched here: `jdk.m3.collections.guava.M3EvictingQueueContractProbe` fails with
  "observable or serialized queue contract drift: d6f1b707..." (the guava-evicting-queue module is byte-identical
  to master).
* Master 9763168700 (and the push base c7f779bc370, not re-run) does not compile m3/collections under its `-Werror` (M3PrimitiveRoaringIntSet
  missing explicit constructor, #2561); the module gates were run on 0b7482c3263 and, with `-Werror` stripped in
  scratch only, on 9763168700 (GATES.tsv push_base_*).
* No CPU speed claim: the donor's own timings are mixed (README); JMH not run.
