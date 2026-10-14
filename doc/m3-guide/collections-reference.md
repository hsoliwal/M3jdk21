<!-- SPDX-License-Identifier: Apache-2.0 -->

# Linked-hash collection API and storage reference

[Guide](README.md) · [Package map](m3-packages.md#package-3) · [Coverage](coverage.md)

This is a bounded source reference for `com.m3.collections.M3LinkedHashMap`
and `M3LinkedHashSet`, including their shared slot, sequenced-view, capacity and
spliterator helpers. It deepens one existing package; it does not complete the
other collection owners, the array package, or the JDK collection replacement.

Source inspection pin: `e90339415b849248181e80d112933bf7cfe5de27`,
14 October 2026. The production bodies and test bodies identified below were
read. Examples and tests were **not compiled or executed**. No Maven, recipe,
receiver-policy verifier, native build, jtreg, benchmark or runtime qualification
was run. Complexity statements below are deductions from the inspected loops
and representations, not measured throughput or retained-heap results.

## Module, factories and ownership

The independent [module descriptor][module] exports `com.m3.collections` and
declares no additional module dependency beyond implicit `java.base`.
The [POM][pom] targets Java 21. Its test and authoring-tool configuration does
not make those dependencies part of this collection's runtime representation.
Use the [existing module guide][module-guide] for build and receiver routes;
its historical qualification receipts are not results from this documentation pass.

| Public entry | Concrete result | Order / storage choice |
| --- | --- | --- |
| `new M3LinkedHashMap<>()` | Mutable `M3LinkedHashMap<K,V>` | Insertion order |
| `new M3LinkedHashMap<>(accessOrder)` | Mutable `M3LinkedHashMap<K,V>` | `true` enables access-order relocation; `false` retains insertion order |
| `M3Collections.hashMap()` / `linkedHashMap()` | Same insertion-ordered M3 map class | The `hashMap` factory does not return `java.util.HashMap` |
| `M3Collections.linkedHashMap(boolean)` | M3 map with the selected order | Same owner as the constructor |
| `new M3LinkedHashSet<>()` | Mutable `M3LinkedHashSet<E>` | Insertion-ordered, key-only map storage |
| `M3Collections.hashSet()` / `linkedHashSet()` | Same M3 set class | Neither factory returns `java.util.HashSet` |
| `M3Collections.immutableMap(source)` / `immutableSet(source)` | Unmodifiable JDK wrapper around a new packed M3 owner | Copies mappings/elements by reference; rejects null keys/values/elements |

The [factory body][factory] deliberately also returns ordinary JDK implementations
for several other families, including `arrayList`, `arrayDeque`, `priorityQueue`,
`identityMap`, `enumMap`, `enumSet`, `bitSet` and copy-on-write collections. A factory
name is not evidence that every family has a new M3 backend. Those concrete owners
are outside this reference's method-level review.

The mutable map/set strongly retain caller-supplied keys and values in GC-traced
Java reference arrays. They do not clone, freeze, intern or replace payload objects
with primitive IDs. Mutating a value object remains visible through its reference;
keys must keep equality and hash behavior stable while stored. There is no
`close`, native handle, off-heap payload or resource-release protocol in this closure.
Removal nulls the corresponding payload references. Caller-held views and live
entry objects can keep the shared map/table reachable.

The immutable factories create a new container, not a deep immutable object graph.
Later structural changes to the source do not change the new container, but
mutable key/value objects are still shared. Their declared return types are `Map`
and `Set`; these wrappers do not promise the M3 sequenced API. Source iteration
and null checks can fail while building a private partial result; the factory
does not modify the source or return that partial result.

## Representation: parallel arrays, slots and masks

The [map's private `Table`][map] is the single mutable owner shared by every
reversed map facade. The [set][set] uses its `keyOnly` mode.

| Lane / field | Meaning | Important distinction |
| --- | --- | --- |
| `Object[] keys`, `Object[] values` | Payload references at logical slot `s` | GC-traced strong references keep payloads reachable while the table is reachable; values are not boxed or unboxed by the map |
| `int[] hashes` | Cached spread hash for each slot | `h ^ (h >>> 16)`, with null-key hash zero |
| `int[] before`, `after` | Doubly linked encounter order using slot numbers | No retained link-node object per mapping; removed slots reuse `after` as a free-list link |
| `int[] positions` | Slot-to-index-bucket back pointer | Makes erasing an already known slot independent of another key lookup |
| `int[] index` | Open-addressed buckets | `0` is never occupied, `-1` is a tombstone, positive `s + 1` identifies a live slot |
| `first`, `last`, `free`, `used`, `occupied`, `size` | Order endpoints, recycling and occupancy counters | Slot high-water mark, nonempty buckets and live mapping count are different quantities |
| `modifications` | Shared structural/order revision | A fail-fast check, not a synchronization or lifetime-generation mechanism |

Payload arrays start as shared zero-length arrays. First insertion allocates slot
capacity at least eight and an index of 16 buckets. [Capacity helpers][arrays]
grow slot arrays by approximately 1.5×, capped at `Integer.MAX_VALUE - 8`;
hash-index growth doubles a power-of-two capacity and refuses doubling at `1 << 30`.
These are source limits, not a guarantee that the VM can allocate those sizes.

Lookup begins at `spreadHash & (index.length - 1)` and advances by
`(bucket + 1) & (index.length - 1)`. The mask wraps a bucket position because
the index length is a power of two. It is **not** a bitmap membership test,
a 28-bit segmented address, an element-order index or an exposed stable handle.
Lookup skips tombstones and stops at a zero bucket, checking cached hash and
`Objects.equals(queryKey, storedKey)` for live candidates. Null payloads therefore
do not encode absence; bucket state does.

Before insertion, if `occupied + 1 >= B - B / 3` for bucket capacity `B`, the
table rehashes. It doubles when `size + 1 >= B - B / 3` as well;
otherwise it rebuilds the same-size index to discard tombstones. Rehashing
preserves payload slot numbers and encounter order. Growing payload arrays copies
their lanes and preserves slot numbers too, but the array objects themselves move.
These owners do not use the separately defined segmented lanes or immutable
`com.m3.arrays` views.

Removal unlinks the slot, tombstones its bucket, clears its key/value references,
and adds the slot to the free list. A subsequent insertion may reuse that number.
There is no slot-generation check and no public slot API in these map/set classes.
Neither removals nor `clear()` shrink backing capacity. Nonempty `clear()` clears
reference lanes and index buckets and resets topology; already-empty `clear()`
returns immediately, so it is not a compaction operation.

For slot capacity `C` and bucket capacity `B`, the map's retained primitive lane
payload is `16*C + 4*B` bytes, plus `2*C` reference slots; the set uses the same
primitive lanes but only `C` key-reference slots, leaving its values lane empty.
This is lane accounting only: it excludes headers, alignment, owner/facade objects,
the payload objects themselves, temporary copies and VM-specific
reference widths. It is not a heap measurement or a lower-memory guarantee for
every size/workload.

## Map reads, writes, equality and nulls

The following inherited public operations are implemented by the complete
[slot-map body][slot-map], using the concrete linked-hash table.

| Operation | Return / mutation contract | Null and absence |
| --- | --- | --- |
| `size`, `isEmpty`, `containsKey` | Count or membership; membership does not create an access-order access | Null key is supported |
| `get`, `getOrDefault` | Return current value; a found key triggers the access hook | Stored null is returned, rather than the supplied fallback; `get` alone cannot distinguish absence |
| `containsValue` | Encounter-order linear scan using value equality | Null values are supported; no access-order relocation |
| `put` | Insert a missing key at the underlying table's end; replace a found value and return its previous value | Null key/value accepted; return null can mean absence or a prior null value |
| `putAll` | Calls `source.forEach(this::put)`; exact self argument is a no-op | Null source fails; a different facade over the same storage is not the exact-self special case |
| `putIfAbsent` | Insert if absent, or replace a present null value; return prior value | A found key triggers access even when its old value is non-null |
| `remove(key)` / `remove(key,value)` | Remove a found mapping; conditional form requires value equality | Conditional form distinguishes a present null value and returns a boolean |
| `replace(key,value)` / `replace(key,old,value)` | Replace only a found mapping; conditional form also checks old value | Successful replacement triggers access; failed replacement does not |
| `clear` | Remove all live mappings; no capacity release | Shared reversed/views observe the change |

Lookup invokes user key `hashCode`/`equals`; comparisons and output may invoke
value `equals`, `hashCode` or `toString`. User-method exceptions propagate unless
the specific operation catches them. Side-effecting or inconsistent key methods
are not a supported substitute for stable keys.

Map equality is independent of encounter order: another `Map` must have equal
size and matching values for each key, with a `containsKey` check to distinguish
absent keys from null values. `ClassCastException` or `NullPointerException`
during that comparison yields false. Hash code is the int sum of
`Objects.hashCode(key) ^ Objects.hashCode(value)` over mappings, with null
contributing zero. This is equality-based storage, not identity-map semantics.
The comparison calls the other map's `get`; that other map may have access-order
side effects. Equality/hash are not concurrent snapshots.

`toString()` walks encounter order, renders `{key=value, ...}`, and handles the
map facade itself as a direct key/value with `(this Map)`. It allocates output;
this is not a general cycle detector for arbitrary mutually referring objects.

### Callback methods and partial effects

| Operation | When the callback runs | Result handling |
| --- | --- | --- |
| `computeIfAbsent` | Absent key or present null value | Non-null result is put; null leaves existing null mapping/absence unchanged. A present non-null value is returned with access and without a callback |
| `computeIfPresent` | Present non-null value only | Null removes; non-null writes and accesses |
| `compute` | Always, with old value or null | Null removes if present; non-null puts |
| `merge` | Only for a present non-null old value | Absent/null old value uses supplied value; callback null removes; otherwise puts |
| `forEach` | Once per encountered mapping if traversal completes | Passes key/value directly, without constructing entry projections |
| `replaceAll` | Once per encountered mapping if traversal completes | Writes each result directly, including null; does not itself reorder for access |

All these methods reject a null callback. `merge` additionally rejects a null
supplied value. Exceptions thrown by callbacks propagate. Computation methods
record the structural revision and check it after a returning callback, before
applying their computed result. `forEach` and `replaceAll` check after each
callback; the latter writes the result only after that check.

This is reentrant-change detection, not transactional rollback. Callback writes
already performed remain visible when `ConcurrentModificationException` is thrown;
earlier `replaceAll` updates remain too. If the callback throws, that exception
escapes before the post-callback revision check. Nonstructural value changes do
not increment the revision and need not be detected. Do not mutate structure or
access order from these callbacks, or depend on exact effects after interference.

## Encounter order, reverse views and access-order reads

`reversed()` allocates another map facade with the same `Table` and opposite
traversal direction. It is a writable live view, not a snapshot or payload copy.
Repeated reversal is not facade-identity caching: `map.reversed().reversed()`
has the original order and storage but is a new object, not necessarily `map` by `==`.
The set likewise creates new wrappers on reversal.

| Operation through either map facade | Underlying encounter effect |
| --- | --- |
| Ordinary `put` of a new key | Append to the original table's end, which is the reversed view's beginning |
| `putFirst` / `putLast` | Insert or relocate at the endpoint named in the receiving view's order; update value and return previous value |
| Found access when `accessOrder=true` | Move to the original table's end, hence the reversed view's beginning |
| Replacement in insertion-order mode | Preserve existing position unless an explicit positioning method was used |
| `containsKey`, `containsValue`, iteration, `LiveEntry.setValue`, `replaceAll` | Do not independently count as an access |

A relocation increments the shared structural revision only when the slot
actually changes position. A new insertion, removal or nonempty clear also
increments it. Merely replacing a value does not. Thus an access-order `get`
can invalidate an iterator even in one thread, while accessing the already-last
slot need not do so. Ordinary `get` is not universally read-only on this type.

The null-return path of `computeIfAbsent` on an existing null value does not call
the access hook. Use the method-specific rules above instead of assuming every
operation that mentions a key always reorders it. `putAll` order and failure
effects depend on the source traversal, and aliased reversed sources are not
snapshot-copied before mutation.

## Views, iterators and entry lifetimes

[Ordered-slot views][ordered-map] back `keySet`/`sequencedKeySet`,
`values`/`sequencedValues`, and `entrySet`/`sequencedEntrySet`. Each call creates
a fresh view object. The views share the owner and encounter order; their reverse
views share it with opposite traversal. Removals and clear write through. Key
views support removing null-valued mappings, and entry views match key and value.
`values().remove(value)` removes the first equal value in the view's encounter order.

Key and entry views implement set equality/hash independent of order. Values
views inherit object identity equality/hash rather than list-style equality;
two separately obtained values views need not compare equal even though they
refer to the same mappings. Compare their elements explicitly when that is the
intended question.

`add`, `addFirst` and `addLast` on these map views are unsupported and throw
`UnsupportedOperationException`. The inherited `addAll` attempts `add` for each
element, so an empty input can return false without throwing. Bulk removals and
retention operate through the view iterators; there is no independent copy of
the collection being edited.

| Entry-producing API | Entry type / connection | Empty or removed behavior |
| --- | --- | --- |
| `entrySet().iterator().next()` | Fresh `LiveEntry`, storing the key and its enclosing map facade | Iterator exhaustion throws `NoSuchElementException` |
| `sequencedEntrySet().getFirst/getLast` | Fresh live entry | Empty view throws `NoSuchElementException` |
| `firstEntry/lastEntry` | Fresh `SimpleImmutableEntry` snapshot of key/value references | Empty map returns null; `setValue` is unsupported |
| `pollFirstEntry/pollLastEntry` | Same immutable snapshot, followed by removal | Empty map returns null |
| `sequencedEntrySet().removeFirst/removeLast` | Detached `SimpleEntry`, followed by removal | Empty view throws; returned entry's `setValue` changes only that detached entry |

For non-iterator sequenced-view entry methods such as `getFirst/getLast`,
these concrete choices are permitted variations where the [SequencedMap
interface][sequenced-map] does not specify whether the entry is connected to
the map. Iterator-produced entries have the interface's connection guarantee
during iteration. Do not confuse an immutable entry
snapshot with deep immutability of its referenced key/value objects.

A live entry resolves its stored key again on every `getValue`/`setValue`.
It is not bound to an immutable slot generation. If the key is absent,
`getValue()` returns null and `setValue()` throws `IllegalStateException`.
If an equal key is removed and later reinserted, this implementation can resolve
the new mapping. Do not use retained entries as stable row handles or rely on
their behavior after external structural changes. A live entry's equality and
hash use its current resolved value and can consequently change over time.

Each iterator records the shared revision. `hasNext`, `next` (through
`hasNext`) and `remove` check it. `remove` is valid once after a successful
`next`, erases that slot and refreshes the iterator's expected revision;
otherwise it throws `IllegalStateException`. Key and value iteration return
payload references directly; entry iteration creates a new projection per
returned mapping. None is a weakly consistent or snapshot iterator.

Fail-fast checks are diagnostics, not a JMM or thread-safety guarantee. These
map/set owners use plain mutable fields and have no locks, volatile publication
protocol or atomic operations. Coordinate all shared access externally, including
access-order reads and operations through every view. Parallel stream support
does not authorize concurrent mutation.

### Spliterators, streams and arrays

[M3SlotSpliterators][spliterators] delegates these views/sets to the JDK
collection-based `Spliterators.spliterator`. In the [inspected JDK helper][jdk-spliterators],
iterator and size bind on first traversal, split or size query, rather than at
spliterator creation. Mutations before binding are visible; structural changes
after binding can trip the iterator's revision checks.

Characteristics are `ORDERED`, `SIZED` and `SUBSIZED`, plus `DISTINCT` for key,
entry and set traversal. Values are not declared distinct. These adapters do not
claim `SORTED`, `IMMUTABLE`, `CONCURRENT` or `NONNULL`. Splits batch iterator
results into new `Object[]` arrays; this is not direct zero-copy partitioning of
the table. Entry-stream splits also carry newly allocated live entry projections.
Array-backed child splits do not share the original iterator's revision checks;
no universal detection of later mutation is promised.

Inherited [`AbstractCollection.toArray`][abstract-collection] copies encounter-order
references into a new or suitable caller array; it does not expose the table's
lanes. Entry-view arrays contain live entry objects, not frozen values. A null
typed destination throws `NullPointerException`; incompatible runtime component
types can throw `ArrayStoreException`. A sufficiently large supplied array is
reused, with a null after the last element when space remains. Normal Java array
allocation/type rules still apply.

## Set-specific methods

`M3LinkedHashSet` exposes no access-order constructor. `add(e)` returns false
without moving an existing element; a new element is appended to underlying
insertion order, including when called through the reversed facade. `addFirst`
and `addLast` insert or reposition an existing element according to the receiving
view's order. `contains`, `remove`, clear and iterator removal operate on the
same key-only table. Null is a supported element.

`getFirst/getLast` return endpoint keys; `removeFirst/removeLast` also remove
them. Each throws `NoSuchElementException` when empty. They do not allocate
map-entry projections. The [`AbstractSet` inheritance][abstract-set] supplies
order-independent set equality and the sum of element hash codes, with null
contributing zero. The set's general array/bulk operations inherit the ordinary
collection algorithms; they are not specialized primitive-array kernels.

## Costs and the precise no-entry-object result

Let `n` be live mappings, `C` retained slot capacity and `B` bucket capacity.
Assume stable, bounded-cost key hash/equality and callback/value operations;
user code costs must otherwise be added.

| Operation | Source-derived work / allocation boundary |
| --- | --- |
| `size`, endpoint slot read, value write at known slot | Constant work; map entry-return APIs additionally allocate an entry |
| Key lookup / ordinary non-growing update | Expected constant probing with suitable hashes; worst case scans a cluster up to `B` buckets, including tombstones |
| Insertion | Lookup/probing plus constant link updates; growth copies `O(C)` lane slots, rehash allocates/zeros `O(B)` buckets and reinserts `n` hashes |
| Rehash | Expected `O(B+n)` with suitable hashes; pathological equal hashes can make reinsertion quadratic in `n` |
| Known-slot erase / order relocation | Constant topology/index work, with no backing-array shrink |
| Full direct map traversal, `containsValue`, map hash | `O(n)` visits plus invoked user operations; traversal follows links rather than scanning capacity |
| Map equality / set equality | Includes the other map's lookups or this set's membership probes; not an unconditional `O(n)` bound under collisions |
| Nonempty `clear` | `O(C+B)` array clearing; capacities remain retained |
| Key/value iterator | One iterator allocation; no per-element entry object from this traversal |
| Entry iterator | Iterator plus one fresh live entry per returned mapping |
| View / reverse creation | Constant-size wrapper allocation; does not copy payload lanes |
| `toArray`, splitting, output conversion | Explicit array, entry-projection where applicable, and/or output allocations |
| `verifyInvariants()` | Diagnostic `boolean[used]` allocation and index/link/free-list scans, plus per-key lookup probes; collision-sensitive, not a cheap constant-time guard |

The map/set therefore meet a specific **representation** property in the inspected
source: the container does not retain one structural `Entry`, `Node`, bucket
object or linked cell per mapping. Key-only set storage also avoids a populated
dummy-value lane. Internal map `forEach`, `replaceAll`, equality, hashing and
string traversal read slots directly rather than manufacturing entry objects.

This does **not** mean no objects, no allocations, no boxing by callers, no retained
payload references, no temporary copies, or that `Map.Entry` APIs were eliminated.
The owner/table, public views, iterators, snapshots, live entries, spliterators,
split batches and capacity growth all allocate as described above. A caller can
also supply an entry object as an ordinary payload; that is distinct from the
container using it as structural storage. No throughput, GC, total-heap or native
memory advantage was measured here.

## Compatibility and explicitly unsupported surfaces

These classes implement `SequencedMap`/`SequencedSet` behavior through M3 types;
they do not install a backend into `java.util`. They are final classes, with
different constructor surfaces from `LinkedHashMap`/`LinkedHashSet`. There is no
public capacity/load-factor/copy constructor, public clone, `Serializable`
implementation or `removeEldestEntry` subclass hook in this closure. Do not cast
them to the concrete JDK classes, assume a JDK serialized form, or use the boolean
access-order constructor as an automatically bounded LRU cache.

Map-view insertion is unsupported as described above. There is no exposed raw
lane, numeric position lookup, trim/compact method, primitive key/value API,
bitmap rank/select, mapped storage or optional native/provider path for these
two classes. The generic map stores references, including caller-created wrappers
for primitive values. Separate primitive, bit-lane, concurrent, tree, weak and
identity owners retain separate contracts and are not qualified by this page.

The documented public-interface behaviors have focused source/test support below.
That is narrower than every Java Collections Framework edge case passing on the
current target. Serialization, subclass and concrete-class identity compatibility
are not supplied here; reflection, hostile/reentrant user methods, allocation
failure and all aliasing/default-method combinations are not newly qualified.
Fail-fast behavior does not establish concurrent correctness.

The [porting invariant](../../m3/docs/m3jdk21-porting-invariant.md#collection-family)
and [naming map](../../m3/docs/name-mapping.json) retain the original JDK public
owners and ordered receiving gates. String qualification remains the active
phase. This separately built library and its historical receipts do not advance
arrays/collections or establish a replacement for every JDK collection.

## Usage example: live view versus entry snapshot

Source-checked illustration, **NOT_RUN**:

```java
import com.m3.collections.M3Collections;

var map = M3Collections.<String, Integer>linkedHashMap();
map.put("a", 1);
map.put("b", null);
var snapshot = map.firstEntry();                  // detached, unmodifiable entry
var live = map.sequencedEntrySet().getFirst();    // connected entry in this owner
live.setValue(2);                                // map now has a=2
// snapshot.getValue() is still 1; live.getValue() is now 2
map.reversed().putFirst("c", 3);                 // original order: a, b, c
boolean present = map.containsKey("b");           // true, even though get("b") is null
map.keySet().remove("b");                        // removes the null-valued mapping
map.verifyInvariants();                          // diagnostic, allocating check
```

For access-order owners, avoid calling `get` while iterating unless the iterator
is no longer needed: the access can move the key and invalidate that iterator.
Use direct `forEach((key,value) -> ...)` for entry-free traversal and keep the
callback free of structural/access-order changes; callback objects and invoked
work can still allocate.

## API → representation → test / fallback map

All linked tests below were read, **not run**. A finite differential workload or
reflection guard is useful evidence design, not proof of every reachable state.

| API / behavior | Representation / operation | Focused existing test evidence | Fallback / remaining boundary |
| --- | --- | --- | --- |
| Null-aware map reads/writes, compute/merge, equality/hash | Slot lookup and direct value lane | [M3MapContractTest][map-test]: 30,000-step seeded differential per configured owner; linked-hash insertion/access cases compare JDK maps | No alternate backend; callbacks, adversarial equality and every null/alias combination remain broader than these finite cases |
| Collision deletion/reuse | Tombstones, cached hash, free slots | [M3MapContractTest][map-test] `collisionChainsRecyclingAndNull`, 15,000 steps with constant-hash keys | Linear collision path; no treeification fallback |
| Reversed/order/endpoint behavior | Shared table plus direction; primitive links | [M3MapContractTest][map-test] `sequencedAndReversedHashViews`, 8,000 operations in each order mode, plus live/detached endpoint checks | Does not establish facade `==` identity or serialization parity |
| Null/access-order/default mutations | Slot-map callbacks and access hook | [M3JdkCollectionDifferentialTest][jdk-test] `sequencedNullAndAccessOrderOperationsMatchLinkedHashMap`, 1,600 operations per mode | Separate targeted assertions are not a whole-JDK acceptance result |
| Set order and reverse writes | Key-only map | [M3SequenceContractTest][sequence-test] `orderedSetSequenceOperations`, 15,000 operations, plus ordered parallel-stream case | No access-order set and no primitive-value specialization |
| Backed views, fail-fast, callback reentrancy | Shared revision and live entry projection | [M3MapContractTest][map-test] `backedViewsAndCallbacks` and null-valued view removals | Callback side effects are not rolled back; no concurrent mutation guarantee |
| Late-binding streams/spliterators | Collection-based JDK iterator batching | [M3JdkCollectionDifferentialTest][jdk-test] `lateBindingTraversalsFollowListSetAndMapViews` | Child arrays allocate; not zero-copy splitting or universal post-split fail-fast |
| No retained structural entries | Payload arrays and primitive topology; fresh boundary projections | [M3NoRetainedStructuralEntryTest][entry-test]: declared-field/nested-type inspection, reachable identity checks, 1,024-entry freshness bursts | Finite selected-owner guard; no measured total-heap or zero-allocation result |
| Factory and immutable wrappers | Concrete M3/JDK factory choices, defensive container copy | [M3FactoryContractTest][factory-test] factory smoke cases and mutation/null rejection | Factory smoke tests are not full method coverage for every returned owner |

The no-retained-entry test deliberately permits fresh `LiveEntry` API projections
and checks that selected packed owners do not retain those projections. Its type
name/field checks and selected runtime object-graph traversal are not an exhaustive
static verifier for all possible payloads or future fields.

Next bounded qualification work includes explicit removed/reinserted entry cases,
aliased `putAll`/bulk operations, access-order callback interference, split-child
mutation behavior, self-referential payloads, allocation-failure boundaries, and
measured cold/growing/tombstone-heavy/steady-state costs. The remaining types
in the package and all `com.m3.arrays` method contracts remain separate work.

## Exact source bodies used

These Git blob identities tie the reference to the inspected source; they are
not test receipts. Existing source copyright, Apache-2.0/OpenJDK per-file licensing,
receiver custody and recipe authority are unchanged. No private donor source,
private chat or uploaded design material is reproduced here.

| Complete M3 body | Git blob |
| --- | --- |
| [M3LinkedHashMap][map] | `ade59880b8c9b2bee1b32ae67ca9d29b7382d998` |
| [M3LinkedHashSet][set] | `e1ac806d1ebe8aac1b9efe7bcfbaa3f675e7d0d6` |
| [M3SlotMap][slot-map] | `a7d20300717661c26897f0b69e1e5a83400a62ad` |
| [M3OrderedSlotMap][ordered-map] | `e7be2297d19f22cf4dd4ef8b7d1424fe963063d9` |
| [M3SlotArrays][arrays] | `e337c69710c871f8caa77a1afe53149f1fd705cc` |
| [M3SlotSpliterators][spliterators] | `30cf137538353ac8e91514fff7c5db3903da8819` |
| [M3Collections][factory] | `3d2f5aa53bda55293b3254bed3d526b896cbd044` |
| [M3MapContractTest][map-test] | `d40c88ec184b90eb057dd859d840c915e4f93df8` |
| [M3SequenceContractTest][sequence-test] | `02ba3c683428a02855517093edbaece60ba7444e` |
| [M3JdkCollectionDifferentialTest][jdk-test] | `0ca5ef64c7a3fbbc8f869e96affca5dcd8672003` |
| [M3NoRetainedStructuralEntryTest][entry-test] | `9e64f4c57ab840b0da744bcff43b9f703fe033f8` |
| [M3FactoryContractTest][factory-test] | `e7e156e6d73d178b1bf6f2ee32deb1db5bc046c6` |

Bounded JDK-source reads covered `AbstractCollection` array/add/remove/bulk methods,
`AbstractSet` equality/hash/bulk removal, `Spliterators.IteratorSpliterator`,
`SequencedMap` entry/reversal contracts and `LinkedHashSet` reversed-view behavior.
They were dependency checks for the described surfaces, not a whole-JDK scan.

[module]: https://github.com/hsoliwal/M3jdk21/blob/e90339415b849248181e80d112933bf7cfe5de27/m3/collections/src/main/java/module-info.java
[pom]: https://github.com/hsoliwal/M3jdk21/blob/e90339415b849248181e80d112933bf7cfe5de27/m3/collections/pom.xml
[module-guide]: https://github.com/hsoliwal/M3jdk21/blob/e90339415b849248181e80d112933bf7cfe5de27/m3/collections/README.md
[map]: https://github.com/hsoliwal/M3jdk21/blob/e90339415b849248181e80d112933bf7cfe5de27/m3/collections/src/main/java/com/m3/collections/M3LinkedHashMap.java
[set]: https://github.com/hsoliwal/M3jdk21/blob/e90339415b849248181e80d112933bf7cfe5de27/m3/collections/src/main/java/com/m3/collections/M3LinkedHashSet.java
[slot-map]: https://github.com/hsoliwal/M3jdk21/blob/e90339415b849248181e80d112933bf7cfe5de27/m3/collections/src/main/java/com/m3/collections/M3SlotMap.java
[ordered-map]: https://github.com/hsoliwal/M3jdk21/blob/e90339415b849248181e80d112933bf7cfe5de27/m3/collections/src/main/java/com/m3/collections/M3OrderedSlotMap.java
[arrays]: https://github.com/hsoliwal/M3jdk21/blob/e90339415b849248181e80d112933bf7cfe5de27/m3/collections/src/main/java/com/m3/collections/M3SlotArrays.java
[spliterators]: https://github.com/hsoliwal/M3jdk21/blob/e90339415b849248181e80d112933bf7cfe5de27/m3/collections/src/main/java/com/m3/collections/M3SlotSpliterators.java
[factory]: https://github.com/hsoliwal/M3jdk21/blob/e90339415b849248181e80d112933bf7cfe5de27/m3/collections/src/main/java/com/m3/collections/M3Collections.java
[map-test]: https://github.com/hsoliwal/M3jdk21/blob/e90339415b849248181e80d112933bf7cfe5de27/m3/collections/src/test/java/com/m3/collections/M3MapContractTest.java
[sequence-test]: https://github.com/hsoliwal/M3jdk21/blob/e90339415b849248181e80d112933bf7cfe5de27/m3/collections/src/test/java/com/m3/collections/M3SequenceContractTest.java
[jdk-test]: https://github.com/hsoliwal/M3jdk21/blob/e90339415b849248181e80d112933bf7cfe5de27/m3/collections/src/test/java/com/m3/collections/M3JdkCollectionDifferentialTest.java
[entry-test]: https://github.com/hsoliwal/M3jdk21/blob/e90339415b849248181e80d112933bf7cfe5de27/m3/collections/src/test/java/com/m3/collections/M3NoRetainedStructuralEntryTest.java
[factory-test]: https://github.com/hsoliwal/M3jdk21/blob/e90339415b849248181e80d112933bf7cfe5de27/m3/collections/src/test/java/com/m3/collections/M3FactoryContractTest.java
[abstract-collection]: https://github.com/hsoliwal/M3jdk21/blob/e90339415b849248181e80d112933bf7cfe5de27/src/java.base/share/classes/java/util/AbstractCollection.java
[abstract-set]: https://github.com/hsoliwal/M3jdk21/blob/e90339415b849248181e80d112933bf7cfe5de27/src/java.base/share/classes/java/util/AbstractSet.java
[jdk-spliterators]: https://github.com/hsoliwal/M3jdk21/blob/e90339415b849248181e80d112933bf7cfe5de27/src/java.base/share/classes/java/util/Spliterators.java
[sequenced-map]: https://github.com/hsoliwal/M3jdk21/blob/e90339415b849248181e80d112933bf7cfe5de27/src/java.base/share/classes/java/util/SequencedMap.java
