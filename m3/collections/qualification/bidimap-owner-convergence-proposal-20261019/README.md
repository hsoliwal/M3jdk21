<!-- SPDX-License-Identifier: Apache-2.0 -->
# Proposal: converge the two owned Commons BidiMap bodies, 2026-10-19

This is a proposal with evidence. It changes no owned source. The owners decide.

## The two bodies

| | `M3BidiMap` (master) | `M3DualLinkedHashBidiMap` (PR #1468) |
|---|---|---|
| Inventory row | DualHashBidiMap (`DONE master`) | DualLinkedHashBidiMap (`DONE #1468`) |
| Storage | two core `M3LinkedHashMap` owners | two slot tables with a births lane |
| Iteration order | insertion order | insertion order |
| Qualified against | Commons `DualLinkedHashBidiMap` | Commons `DualLinkedHashBidiMap` |
| `Serializable` | no | yes (owned format) |

Both bodies implement the same Commons contract, `DualLinkedHashBidiMap`. Master records `M3BidiMap` as the
owner of the DualHashBidiMap row. Commons `DualHashBidiMap` iterates in HashMap order, which neither body
reproduces. The `commons_hash` column below shows how often its outcome text differs from insertion order.

## Evidence

`BidiOwnerConvergenceDiscriminator.java` runs 8 seeds x 4,000 seeded operations (16 operation kinds, keys and
values in 0..7 plus null). Each operation runs on Commons `DualLinkedHashBidiMap` (the oracle), on both bodies
and on Commons `DualHashBidiMap`. Two things are compared per operation: the outcome (value or exception)
and the text of both directions afterwards.

The two provider quirks can leave the directions out of step: raw entry-set arrays and stale values-iterator
removal. After such a step every map restarts from the oracle's forward mappings, so a corrupted state is
never compared.

`convergence-results.tsv` counts the operations whose outcome equals the oracle's:

- On 14 of 16 kinds, both bodies equal the oracle on every operation. That includes both provider quirks,
  `values().containsAll` and the map-iterator and entry setters.
- `serializationRoundTrip`: `M3BidiMap` 0 of 1929, because it is not `Serializable`. `M3DualLinkedHashBidiMap`
  matches on 1929 of 1929.
- `entryAfterRemoval`: `M3BidiMap` matches 961 of 2007 and `M3DualLinkedHashBidiMap` 968. The step is: take an
  iterator entry, change its value through `put`, remove the key, then read the entry.
  - Commons reports the node's final value.
  - `M3BidiMap` reads the live map, so it reports null.
  - `M3DualLinkedHashBidiMap` reports the last value it observed.
  - Both READMEs document this as a boundary; the `Map.Entry` contract leaves it undefined.

`footprint-results.tsv` (local JDK 21.0.9, one run; not a benchmark):
- Allocation is ThreadMXBean bytes per steady-state `put` + `get` + `getKey`.
- Retained heap is per mapping, over 40 copies of 10,000 mappings with shared pre-boxed keys.

| owner | B allocated per op | B retained per mapping |
|---|---|---|
| Commons DualLinkedHashBidiMap | 40.0 | 93.1 |
| `M3BidiMap` | 0.0 | 81.4 |
| `M3DualLinkedHashBidiMap` | 0.0 | 91.8 |

## Proposal

Keep one body for the DualLinkedHashBidiMap contract. The evidence favours `M3BidiMap` as the survivor:
- It equals `M3DualLinkedHashBidiMap` on every contract operation measured here.
- It retains about 11% less heap per mapping.
- It already has master custody (recipe lineage V1 to V3).

Steps, each its own PR with a successor recipe:
1. Add the owned serialized form from PR #1468 to `M3BidiMap`, as a V4 transition recipe. That form is size,
   then validated key/value pairs, and the inverse resolves back to its forward map's inverse.
   - The V3 structural check (`ownedStorageBoundary`: two `M3LinkedHashMap` fields, no other map storage)
     still holds.
2. Point the DualLinkedHashBidiMap inventory row at `M3BidiMap`, and make `M3DualLinkedHashBidiMap` a
   documented alias or retire it.
   - The #1468 differential suite (960,117 checks) and its quirk tests move to `M3BidiMap` as oracle tests.
3. Re-label the DualHashBidiMap row: insertion order refines HashMap's unspecified order, so the row is
   API-compatible but not order-identical.

What #1468 has that `M3BidiMap` lacks today:
- Serialization.
- A 100% JaCoCo-gated module with a Commons differential suite.
- A frozen-last-value entry after external removal (versus null).

If the owners prefer the slot-table body instead, the same steps run in the other direction. The cost is
about 10 B more per mapping.

## Reproduce

```
javac --release 21 -Xlint:all -Werror -proc:none -cp "<core m3-collections classes>;commons-collections4-4.5.0.jar" -d out \
  ../../implementations/apache/src/main/java/com/m3/collections/apache/M3BidiMap.java \
  ../../implementations/apache-dual-linked-hash-bidi-map/src/main/java/com/m3/collections/apache/M3DualLinkedHashBidiMap.java \
  BidiOwnerConvergenceDiscriminator.java
java -Xmx768m -cp "out;<core classes>;commons-collections4-4.5.0.jar" BidiOwnerConvergenceDiscriminator
```

The run log has SHA-256 `106d52a8...`. Output goes through `java.util.logging`. This was run locally on
Windows with JDK 21 only; hosted CI was not run.
