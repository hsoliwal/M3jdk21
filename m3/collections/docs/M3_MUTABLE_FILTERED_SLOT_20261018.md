<!-- SPDX-License-Identifier: Apache-2.0 -->
# Direct physical-slot mutable filtering — M3JDK21 (2026-10-18)

**Owner:** the existing M3JDK21 `m3/collections` module. This is an
in-place refinement of `M3MutableFilteredViews.MapView`; it is not a
new M3/JDK adapter layer and does not modify the public `NavigableMap`
surface or its behavior. It depends on the existing `M3TreeMap`,
`M3SlotMap`, and primitive AVL topology arrays. There is no new
stored map-entry or tree-node object.

## Reusable behavioral atoms

- When the source is an `M3TreeMap`, find the retained representative
  using `findSlot`, then operate through `keyAt`/`valueAt` on the
  original physical slot. `size` and `isEmpty` use
  `firstSlot`/`nextSlot` directly. Filter callback order remains
  owner encounter order. Null values and comparator-equivalent probes
  evaluate the **stored** representative.
- `put`, `remove`, and `clear` write through to the same
  authoritative M3 AVL owner without building intermediate API entries.
  Pre-computing `nextSlot` before deletion keeps traversals anchored
  to the tree's unchanged node coordinates during AVL relinking.
- `entrySet().iterator()` requests a Java `Map.Entry` object
  explicitly, so its `next()` constructs a `LiveEntry`. The
  underlying filter/skip/advance path uses only physical integer slots
  and retains no entries. Structural drift is detected through the
  owner's modification epoch; the iterator's own deletion refreshes it.
- Key-only neighbor operations (`lowerKey`, `floorKey`,
  `ceilingKey`, `higherKey`, `firstKey`, `lastKey`) use the
  M3TreeMap AVL neighbor slots and never create `Map.Entry` objects.
  Entry-returning neighbors and polls materialize exactly one detached
  `SimpleImmutableEntry` at the explicit API boundary; filtering
  hidden candidate slots allocates no intermediate entry.
- `values()`, `forEach`, `equals`, `hashCode`, and `toString`, plus `containsValue` and `replaceAll`,
  run on canonical M3 slot coordinates without manufacturing intermediate
  `Map.Entry` instances. A requested `values()` view has its own
  fail-fast slot iterator and forwards iterator deletion to the M3 owner.
- `putAll` continues full input validation before any write, using
  two call-local dynamic reference arrays. Comparator representative
  tracking uses an existing array-backed `M3TreeMap<K,K>`, not a
  temporary JDK `TreeMap.Node` structure. For the M3-owned backing,
  admitted arrays write directly to the owner, avoiding intermediate
  `SimpleImmutableEntry` allocations.
- For a supplied non-M3 `NavigableMap`, the existing generic
  implementation remains in place. Its storage and external API entry
  allocations are not under M3's physical-representation control.

## Contracts and runtime admission

The new `M3MutableFilteredSlotSelfTest` compares native indexed
M3TreeMap filtered views with JDK TreeMap filtered views under the same
operations, including ascending/descending/bounded views, direct owner
writes, filter-admitted writes, clear, remove, iterator removal,
bulk admission, visible/hidden null values, and comparator navigation.
It checks `M3TreeMap.verifyInvariants()` after each operation, and
verifies fail-fast native iterator semantics for structural drift.
A JUnit adapter invokes the same standalone test.

Reuse the existing full `M3MutableFilteredViewsContract`, range,
stored-representative, ownership and Java/JNI gates before admission.
Local compilation, Maven/JUnit, OpenRewrite fixed-point/refusal,
installed-image and allocation evidence are **not yet claimed**.

Recipe/crate in the Synexia convergence workspace:
`com.synexia.m3.Jdk21MutableFilteredSlot20261018` using
`M3HashPinnedJavaSnapshotRecipe`, with SHA-256 pre/post manifests for
the changed source, the M3TreeMap neighbor slot owner and the two added test sources and the existing M3TreeMap neighbor slot atoms. The catalogue is
`synexia-openrewrite-recipes/catalogs/m3jdk21-mutable-filtered-slot-20261018.tsv`.
Competitive-programming sites are problem-category references only;
no third-party code has been copied.

Focused Java 21 gate:

```sh
mvn -f m3/collections/pom.xml -Dtest=M3MutableFilteredSlotTest,M3MutableFilteredViewsTest test
```

This change remains separate from wholesale replacement of
`src/java.base/share/classes/java/util`. Such a replacement must
separately prove the JDK contracts, serialization and installed-image
behavior without introducing a Synexia runtime dependency.
