<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Int-key primitive map specializations

[Guide](README.md) · [Primitive long maps](primitive-map-reference.md) · [Enum specializations](specify-enum-specialization.md) · [Coverage](coverage.md)

This reference covers the complete `M3IntIntHashMap`, `M3IntLongHashMap`,
`M3IntIntConsumer` and `M3IntLongConsumer` sources in `com.m3.collections`.
Every declared member and both anonymous key cursors were inspected. The
`M3IntIterator` default traversal was also read as a dependency; its primary
reference is [primitive set variants](specify-primitive-set-variants.md).

Source pin: `33cd1758a4a4613d3868ecde31d6d83400f66aed`, inspected 15 October
2026. The complete 77-file physical package and these test files have the same
Git blobs as `1011050da4205a901ad079d7711b6b8efd8a9af7`. **Tests, examples,
builds, recipes, receiver verifiers, benchmarks and runtime checks were NOT_RUN
in this documentation authoring/review pass.**
The behavior below is a reading of those source bodies, not measured performance
or a Java SE compatibility certification.

## Construction and API boundary

Both owners are public final classes exported by the separate
[`com.m3.collections` module][module]. They do not implement `Map`, `Iterable`,
`Serializable`, `Cloneable` or a JDK primitive-collection interface. Their keys
are primitive `int`; their values are respectively primitive `int` and `long`.
There are no boxed overloads, entry/key/value views, stream or spliterator
methods, iterator removal, map equality override, `compute`, `merge`, `putAll`,
clone or serialization protocol. Inherited `Object` equality is identity.
Passing a wrapper to a primitive parameter uses Java unboxing and cannot store
null. A caller can deliberately export through `forEach` to a boxed map; that
creates a separate destination whose boxing and null contracts are its own.

Use `new M3IntIntHashMap()` / `new M3IntLongHashMap()` or their single-int
`expectedSize` constructors. The empty constructors delegate to expected size
zero. There is no corresponding int-map factory in the inspected
[`M3Collections`][factory]; its long-long factory selects a different owner.
[Factory and delivery routing](reconcile-factories-and-delivery-scope.md)
explains this distinction and the independent module's build/provenance limits.
The [POM][pom] declares release 21 and test-scoped JUnit; that declaration does
not establish a completed build or installation into a JDK image.

The related [long-map reference](primitive-map-reference.md) must not be applied
mechanically: these int-key owners use a special zero-key cell and backward-shift
deletion, rather than the long-long owner's occupancy bytes/tombstones. Their
`forEach` methods also have a different mutation check, described below.

## Keys, values and return conventions

Every int key, including zero and both extrema, is representable. A zero in the
key array marks an unused slot, but a real zero key is represented separately by
`hasZero` and `zeroValue`. Presence does not depend on value bits. Every primitive
value, including zero and either value type's extrema, is usable.

`getOrDefault(k, fallback)` returns the stored value for a present key, even if
it equals the fallback; otherwise it returns the supplied fallback. Comparing
the result with the fallback cannot determine presence. Use `containsKey` when
that distinction is required, with external coordination if other code mutates
the owner. No operation here provides concurrent atomicity.

In this table `V` is `int` for `M3IntIntHashMap` and `long` for `M3IntLongHashMap`.
The complete bodies are [`M3IntIntHashMap`][int-int] and
[`M3IntLongHashMap`][int-long].

| Declared operation | Result and effect |
| --- | --- |
| `put(int,V)` | Returns true only for a previously absent key. An existing key is overwritten and returns false, even when its value changes |
| `getOrDefault(int,V)` | Read present value or caller fallback; never insert |
| `containsKey(int)` | Test separate zero-key presence or probe the nonzero key lane |
| `containsValue(V)` | Compare the zero-key value when present, then values at occupied nonzero-key slots; unused zero-filled value cells do not count |
| `replace(int,V expected,V replacement)` | If present and equal under primitive `==`, write replacement and return true; true also when replacement equals the old value; false for absence/mismatch |
| `addTo(int,V delta)` | Treat absence as zero, compute primitive addition, put the result and return it. Delta zero still inserts an absent key; ordinary Java int/long overflow wraps, with no checked-arithmetic guard |
| `remove(int)` | Remove a present key and return true; return false for absence |
| `remove(int,V expected)` | Check both presence and value before removal; a missing key does not match expected zero |
| `size()`, `isEmpty()` | Live mapping count, including a present zero key |
| `capacity()` | Length of the nonzero-key slot array; not a byte count, logical maximum or count of live mappings |
| `payloadBytes()` | Key/value array element bytes: `8 * capacity` for int/int and `12 * capacity` for int/long |
| `clear()` | When nonempty, zero both arrays and reset size/zero-key state; retain capacity. Already empty is a no-op |
| `forEach(consumer)` | Visit zero first if present, then nonzero keys in current ascending slot index |
| `keyIterator()` | Primitive key cursor, zero first then current ascending slot index; tied to structural modification count |
| `keysToArray()` | New caller-owned `int[size]`, zero first if present, then nonzero slot order |
| `M3IntLongHashMap.payloadBytesFor(int expectedSize)` | Static constructor-sizing calculation, returning `12 * capacityFor(expectedSize)` without allocating a table; no counterpart in the int/int owner |

Array results are detached from later owner changes and may be edited by the
caller. Their construction is not a synchronized snapshot under concurrent
mutation. Slot order is neither signed-key order nor insertion order and can
change after deletion or growth. No ordering comparator is accepted.

## Capacity, deletion and failure effects

Both constructors reject negative expected size with `IllegalArgumentException`.
They use a minimum of eight slots and the exact source constant `0.65f`:

1. Compute `max(8, ceil(expectedSize / (double) 0.65f))`
2. Reject a required size greater than `1L << 30` with `IllegalArgumentException`
3. Round upward to a power of two
4. Set the insertion threshold to `min(capacity - 1, max(1, (int)(capacity * 0.65f)))`

The float constant and the float threshold multiplication are part of this
calculation; an independently rounded decimal approximation is not the exact
sizing rule. Expected size chooses initial storage, not a permanent entry limit.
Allocation can still fail for an arithmetically accepted size.

Nonzero keys use `mix(key) & mask` and linear probing with wraparound. An empty
key cell terminates an absent lookup. Removal back-shifts the following probe
run using each key's home slot, moving its associated value with it, and zeros
the final hole. There are **no tombstones**, occupancy bytes or tombstone
compaction counters in either class. Removing zero only clears its separate
presence/value state. Removal and clear do not shrink storage; no public trim or
compact method is declared.

New insertion first writes the key/value, increments size and modification
count, then doubles/rehashes if `size > threshold`. The zero key counts toward
this threshold even though it is outside the arrays. Rehash reinserts nonzero
keys and keeps zero-key state. An existing-key overwrite does not trigger growth.
At an existing capacity of `1 << 30`, a requested rehash throws
`IllegalStateException`. Because the insertion precedes that guard, such an
exception is not a promise that the new mapping was rejected without effect.
Rehash also assigns new arrays separately. No strong rollback or recovery
guarantee for allocation failure is provided by these bodies.

Payload reporting counts array element bytes only. It excludes object and array
headers/alignment, scalar fields (including the special zero-key value), the map
object, cursor/callback objects and temporary replacement arrays. It is not a
retained-heap measurement or a promise of allocation-free operations. The
int/long sizing helper is used by
[`M3AdaptiveEnumLongMap`][adaptive] to compare sparse storage with direct enum
storage; its migration rules belong to the [enum reference](specify-enum-specialization.md).

## Structural changes and key cursors

Successful new-key insertion, successful removal and nonempty `clear` each
increment `modCount`. Existing-key `put`, successful `replace`, and `addTo` on an
existing key do not. Absent removals, mismatched conditional operations and
empty clear also do not. Rehash does not increment the counter separately.

Each `keyIterator` captures that count. Its `hasNext` and `nextInt` check the
count before their ordinary work and throw `ConcurrentModificationException`
if it changed. With no intervening structural change, exhausted `nextInt`
throws `NoSuchElementException`. Value replacement alone does not invalidate
the key cursor. The cursor exposes neither boxed `next()` nor `remove()`.

[`M3IntIterator.forEachRemaining`][iterator] rejects a null `IntConsumer` even
when exhausted, then repeats `hasNext`, `nextInt` and the callback. The key is
consumed before the callback. A callback exception propagates with that key
already consumed. A structural mutation followed by normal callback return is
noticed by the next iterator check, including the next `hasNext` after the last
key. These checks are not synchronization or a guarantee of detecting data races.

## Callback traversal is not fail-fast

[`M3IntIntConsumer.accept(int key,int value)`][int-consumer] and
[`M3IntLongConsumer.accept(int key,long value)`][long-consumer] are the sole
methods of their respective functional interfaces. Arguments are key first,
value second. Neither interface adds composition, exception handling, a default
method or an adapter to JDK `BiConsumer`. A method reference into a boxed map can
be used by caller code, but it does not change these primitive signatures.

Both map `forEach` methods require a nonnull consumer before checking content.
They visit the special zero key first, then compute the remaining nonzero count
and read live array fields as they walk slots. **They do not capture or check
`modCount` before or after callbacks.** The post-callback structural check in
`M3LongLongHashMap.forEach` is not present here.

There is no reentrancy guard or stable traversal guarantee if the callback
changes the map. A value replacement can affect a later visit. Structural
mutation can change the live arrays, move keys behind/ahead of the cursor or
make the saved remaining count inconsistent with current contents. Insertion,
deletion, growth and clear during callbacks should therefore be kept out of
code that needs a complete, once-only traversal. No mandatory
`ConcurrentModificationException` is promised for `forEach`.

Callback exceptions stop and escape immediately. Earlier callback effects and
map mutations remain; traversal has no transaction/rollback boundary. A caller
needing to mutate during a stable key walk can first obtain `keysToArray` in a
quiescent context and explicitly decide how to handle keys no longer present.
That does not create a concurrent transaction over the map.

## Complete member and source evidence

In addition to the public API above, both complete bodies include private
`find`, `shiftKeys`, `rehash`, `capacityFor`, `thresholdFor`, `mix`, and
`checkForComodification`; these implement the probe, sizing and cursor rules
described here. Both declare `MIN_CAPACITY`, `LOAD_FACTOR`, `keys`, `values`,
`mask`, `threshold`, `size`, `hasZero`, `zeroValue`, and `modCount`; int/long also
declares `BYTES_PER_SLOT`. Each anonymous cursor declares `cursor`, `emitted`,
`hasNext` and `nextInt`, and captures the local expected modification count.
There are no other declared protected/package-private methods or named nested
implementation classes in these four primary files.

| Complete source | Git blob | Member locations |
| --- | --- | --- |
| [M3IntIntHashMap][int-int] | `f944165b0ddd1b4c45b656a3e522e0038676d17e` | Fields 22–32; constructors 34–47; public methods 50–249; private helpers 251–338; cursor 205–235 |
| [M3IntLongHashMap][int-long] | `00e9190be49f7071504a477f0aad4eabd46f22d1` | Fields 25–36; constructors 38–51; sizing helper 54–59; public methods 62–261; private helpers 263–350; cursor 217–247 |
| [M3IntIntConsumer][int-consumer] | `f1329b55c234a65faad61be2a1a557b2a9b2dcd8` | Interface/accept 8–11 |
| [M3IntLongConsumer][long-consumer] | `49404c6e9b450da64228ddb4af958770f9060382` | Interface/accept 8–11 |

The package-local incoming use is concrete: adaptive-enum construction, reads,
writes, sizing comparison, migration and copy all call the int/long owner;
both callback interfaces are parameters of their map's `forEach`. Their lexical
mentions in comments do not establish additional runtime callers. No claim of
an exhaustive repository-wide caller inventory is made.

## Test assertions inspected, not executed

| Complete test file / exact methods | Actual bounded assertions |
| --- | --- |
| [M3IntLanesTest][int-tests]: `intIntMapMatchesJdkAcrossChurnIncludingZeroKey` (101–141) | Seeded 30,000-step comparison with boxed HashMap for put/removal/default/presence/value/conditional operations, addTo, clear, key arrays, callback content, iterator content and payload arithmetic |
| Same: `intIntMapEdgeContractsAndFailFast` (144–167), `intLanesRetainOnlyPrimitiveFields` (217–225) | Negative sizing, zero overwrite/conditional operations, insertion through addTo, cursor invalidation after removal, null callback, clear capacity; reflection checks permitted instance-field type names |
| [M3IntLongHashMapTest][long-tests]: `matchesJdkAcrossChurnIncludingZeroKeyAndRehash` (47–96) | Seeded 30,000-step boxed comparison, zero-key presence, callback/key-cursor content and payload calculation |
| Same: `edgeContractsFailFastAndSizing` (99–148) | Negative sizing/helper arguments, seven finite sizing examples, zero key with long extrema, exhaustion/invalidation, null callback, arithmetic/conditional removal, 10,000-key growth and deletion pattern |
| [M3AdaptiveEnumLongMapTest][adaptive-tests]: `modeTransitionsAndOrdinalOrderInSparseMode` (131–213), `wideEnumMatchesJdkAcrossMutationsAndModes` (58–128) | Sparse/direct payload rule, threshold, retention and compact, and ordinal traversal through the int/long dependency |

Those three test files were read in full, including helpers and the unrelated
set/heap tests in `M3IntLanesTest`; those other tests are not counted as int-map
assertions. The consumer interfaces are exercised through callbacks rather than
standalone named tests. No inspected assertion establishes structural reentrant
`forEach`, maximum-capacity failure recovery, race behavior or full allocation
costs. Reflection shape assertions are not heap profiles. All execution and
broader receiver/JDK-compatibility gates remain open.

[int-int]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3IntIntHashMap.java
[int-long]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3IntLongHashMap.java
[int-consumer]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3IntIntConsumer.java
[long-consumer]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3IntLongConsumer.java
[iterator]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3IntIterator.java
[adaptive]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3AdaptiveEnumLongMap.java
[factory]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3Collections.java
[module]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/module-info.java
[pom]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/pom.xml
[int-tests]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/test/java/com/m3/collections/M3IntLanesTest.java
[long-tests]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/test/java/com/m3/collections/M3IntLongHashMapTest.java
[adaptive-tests]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/test/java/com/m3/collections/M3AdaptiveEnumLongMapTest.java
