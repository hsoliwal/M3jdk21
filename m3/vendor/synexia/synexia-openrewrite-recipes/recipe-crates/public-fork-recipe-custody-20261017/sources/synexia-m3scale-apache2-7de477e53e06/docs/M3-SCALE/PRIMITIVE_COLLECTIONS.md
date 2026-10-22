<!-- SPDX-License-Identifier: Apache-2.0 -->
# Flat and primitive collections

M3's hot path now has a node-free collection family for the common Java collection shapes.
The goal is not to replace the JDK everywhere. The goal is to prevent repository-scale
indexes, contexts, hashes, graph edges and counters from paying for one helper object
per element when a flat array representation is enough.

## Core invariant

**Objects are boundary/API values. Hot storage is primitive arrays, Object[] reference
arrays, bitsets and integer indexes.**

No collection in this package allocates a Node, Entry, Pair, Tuple or wrapper object
for each mapping/element. Generic collections necessarily retain references to the
user's key/value objects; they do not add an additional entry object.

## Collection matrix

| Shape | Primitive/flat implementation | Entry representation |
| --- | --- | --- |
| hash map | IntIntHashMap, IntLongHashMap, LongIntHashMap, LongLongHashMap | parallel primitive arrays + OCCUPIED/DELETED bitsets |
| quantized map | IntByteHashMap, LongByteHashMap | primitive key array + byte[] value array |
| mixed map | IntObjectHashMap, LongObjectHashMap, ObjectIntHashMap, ObjectLongHashMap | one Object[] side only; primitive other side |
| generic hash map | FlatHashMap | Object[] keys + Object[] values + bitsets |
| hash set | IntHashSet, LongHashSet, FlatHashSet | value array + bitsets |
| insertion ordered | FlatOrderedHashMap, FlatOrderedHashSet | flat table + int[] previous/next slot links |
| array list | IntArrayList, LongArrayList, ByteArrayList, FlatArrayList | one backing array |
| vector | IntVector, LongVector, ByteVector | one backing array |
| queue / deque / stack | IntDeque, LongDeque, FlatDeque | circular array; push/pop/enqueue/dequeue aliases |
| priority queue | IntPriorityQueue, LongPriorityQueue, FlatPriorityQueue | binary heap array |
| sorted map | IntIntSortedMap, LongLongSortedMap, FlatSortedMap | parallel sorted arrays |
| sorted set | IntSortedSet, LongSortedSet | one sorted primitive array |
| bit set | IntBitSet, PackedBits | long[] bit words |
| packed 0..255 | PackedUnsigned8 | eight unsigned values per long |
| packed context | PackedContextStore | struct-of-arrays, no Context object per record |
| concurrent map/set | StripedIntIntHashMap, StripedFlatHashMap, StripedFlatHashSet | flat tables with one lock per stripe, never per entry |

The existing LongIntHashMap, LongLongHashMap and LongHashSet remain canonical long-key
owners. This change adds the missing int-key, object-reference, ordered, queue/deque,
priority, sorted and striped shapes additively.

## Integer-first layout rules

* A Java long contributes all 64 bits to masks. Signedness does not reduce flag capacity.
* 0..255 values use byte storage or PackedUnsigned8; no float/double is needed for quantized strength.
* Power-of-two table capacity turns modulo into `index & mask`.
* Open addressing keeps keys/values contiguous and removes the per-entry linked node.
* Hash-table state is carried by packed bitsets so every key value, including zero and
  negative primitive keys and null generic keys, can be represented without sentinel loss.
* Ordered hash collections use int slot indexes for previous/next links instead of Java objects.
* Sorted-array maps deliberately trade O(n) mutation for compactness and O(log n) lookup;
  use them for read-heavy indexes rather than pretending they have TreeMap mutation costs.
* Striped variants trade a bounded number of lock objects for concurrent access; locks are
  proportional to stripes, not entries.

## Iteration without iterator garbage

Hot loops use slot/index traversal. Examples:

```java
for (int slot = 0; slot < map.capacity(); slot++) {
    if (!map.occupiedAtSlot(slot)) continue;
    long key = map.keyAtSlot(slot);
    int value = map.valueAtSlot(slot);
}
```

Insertion-ordered generic maps expose `firstSlot()` / `nextSlot(slot)`, again without a
new iterator or Map.Entry object per element.

## Verification

`tools/primitive/VerifyPrimitiveStorage.java` validates packed contexts and the original
primitive map/set layer.

`tools/primitive/VerifyAllFlatCollections.java` performs deterministic differential
checks against JDK HashMap, HashSet, LinkedHashMap, ArrayList, ArrayDeque, PriorityQueue,
TreeMap, TreeSet and BitSet. The JDK collections are test oracles only; the M3 runtime
implementations remain flat.

The dedicated Java 21 workflow compiles the entire primitive package with
`-Xlint:all -Werror` and runs both verifiers.
