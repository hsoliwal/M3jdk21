<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- TEMPLATE of make/m3/collections/qualification/flat3map-single-owner-20261106/apply.py (census leaf L075); edit through that recipe. -->
# M3Flat3Map: Commons-compatible facade

`jdk.m3.collections.apache.M3Flat3Map` keeps the public surface of `org.apache.commons.collections4.map.Flat3Map`
(Apache Commons Collections 4.5.0, Apache-2.0) and delegates every operation to the single implementation
`jdk.m3.collections.M3Flat3Map` of `com.m3:m3-collections` (owner decision of 2026-11-06, census leaf L075). The
facade keeps no keys, values or slot states of its own; no Commons implementation code is copied.

| Surface | Delegation |
|---|---|
| `get`, `put`, `remove`, `containsKey`, `containsValue`, `size`, `isEmpty`, `clear`, `putAll` | the core's map operations |
| `entrySet`, `keySet`, `values`, `mapIterator` | write-through views and iterators over the core's slot cursor (`nextSlot`, `slotKey`, `slotValue`, `setSlotValue`, `removeSlot`); an iterator's `remove` keeps its position |
| `equals`, `hashCode`, `toString` | the `java.util.Map` contracts computed over the slot cursor |
| `clone` | `core.copy()`: same mappings, regime and iteration order |
| `getOrDefault`, `putIfAbsent`, `replace`, `remove(key, value)`, `compute*`, `merge`, `forEach`, `replaceAll` | the JDK `java.util.Map` defaults, as Commons uses them |
| serialization | class name, `serialVersionUID` 0 and no serializable fields as in the scaffold; data in the Commons layout (entry count, then each key and value) |

Iteration order is the core's slot order: insertion order while at most three entries are inline (a removal shifts
the later entries down), table order once hashed. Commons does not specify an order (flat mode keeps slots 1..3 and
moves the last entry into a removed slot; delegate mode follows `HashedMap` buckets), so the differential compares
iteration results as sorted entry sets and counts raw order differences separately.

Proof: `M3Flat3MapCommonsDifferentialTest` runs the seeded lockstep differential `M3Flat3MapDifferential`
(1,000,000 ops: Commons `Flat3Map` against the facade over the whole API, and against the core over the shared
subset). Recipe, hunk verdicts, design decisions and gates:
`make/m3/collections/qualification/flat3map-single-owner-20261106/`.

Build: `mvn -f m3/collections/pom.xml install` first (the facade depends on `com.m3:m3-collections:0.1.0-SNAPSHOT`),
then `mvn -f m3/collections/implementations/apache-flat3-map/pom.xml package`.
