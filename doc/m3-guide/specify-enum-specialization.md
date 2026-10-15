<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Enum primitive maps and bit-word sets

[Guide](README.md) · [Int-key maps](specify-int-key-map-specializations.md) · [Factories and delivery](reconcile-factories-and-delivery-scope.md) · [Coverage](coverage.md)

This reference covers every declared member and the complete bodies of
`M3EnumLongMap`, `M3AdaptiveEnumLongMap` and `M3EnumSet` in
`com.m3.collections`, including the dense map's two nested consumer interfaces.
Source pin: `33cd1758a4a4613d3868ecde31d6d83400f66aed`, inspected 15 October
2026. These bodies and the named tests are unchanged from
`1011050da4205a901ad079d7711b6b8efd8a9af7`. **All examples, tests, builds,
recipes, verifiers, benchmarks and runtime gates are NOT_RUN in this pass.**
The descriptions are source-derived contracts and limitations.

## Select the actual owner

All three owners are public final classes in the independently built, exported
[`com.m3.collections` module][module]. None implements `java.util.Map`,
`java.util.Set`, `Iterable`, `Serializable` or `Cloneable`. They expose a bounded
specialized API; they do not replace `java.util.EnumMap` or `java.util.EnumSet`.
In particular, [`M3Collections.enumMap(type)` and `enumSet(type)`][factory]
return JDK owners, not any of these classes. See the
[factory reference](reconcile-factories-and-delivery-scope.md) for the exact
delegate and build/receiver boundaries. The module [POM][pom] targets Java 21;
its presence is not installed-JDK or successful-test evidence.

| Owner | Creation | Representation and scope |
| --- | --- | --- |
| [`M3EnumLongMap<E>`][dense] | `new M3EnumLongMap<>(EnumType.class)` | One long value per domain ordinal plus presence words; retains the enum class and a universe array |
| [`M3AdaptiveEnumLongMap<E>`][adaptive] | Class constructor or `(Class<E>, int expectedSize)` | Uses one active `M3IntLongHashMap` or dense M3EnumLongMap, chosen by primitive-array payload calculation |
| [`M3EnumSet<E>`][set] | `noneOf`, `allOf`, `of`, `copyOf`, `complementOf`, `range`; constructor private | Presence words plus enum class and universe array; no value lane |

The dense map comment still says the adaptive variant is deferred. At this pin
`M3AdaptiveEnumLongMap` and its int/long backing physically exist, with the
concrete source paths above. The stale comment is not a missing-owner result.
This reference does not edit sealed source or change the canonical
[porting/receiving policy][policy]. Separate specializations and donor adaptation
comments are not evidence of a generation process or JDK substitution.

## Enum type and null rules

The class token is required. Null class tokens throw `NullPointerException`;
passing a non-enum class through a raw/unchecked construction route is rejected
when `getEnumConstants()` returns null, with `IllegalArgumentException`.
The full enum domain is fixed at construction. Validation uses
`getDeclaringClass() == keyType/elementType`, so a constant with a specialized
constant-specific class body is still in its declaring enum's domain.

The adaptive expected-size constructor accepts only `0..keyCapacity()` and
throws `IllegalArgumentException` outside that interval. Sizing does not insert
entries: every constructor starts empty. Enum classes with no constants are
accepted by the bodies: the dense map has no value elements and one zero
presence word; the set has one zero word; the adaptive map starts direct, has
switch size zero, and reports eight primitive payload bytes. These empty-domain
observations were inferred from source, not exercised by the inspected tests.

The two maps have the same key-validation split:

| Method family | Null enum key | Enum from another declaring class |
| --- | --- | --- |
| Both `put` overloads, `putIfAbsent`, `getAsLong` | `NullPointerException` | `ClassCastException` |
| `containsKey`, `removeKey` | false | false |
| `getOrDefault(key,fallback)`, `remove(key,absentValue)` | Caller fallback/absentValue | Caller fallback/absentValue |

The map parameters are `E extends Enum<E>`, not JDK Map's arbitrary `Object`
lookup parameters. A foreign enum can reach them through a raw type; a
non-enum object is not a supported lookup argument. Set `contains(Object)` and
`remove(Object)`, in contrast, explicitly return false for null, non-enum objects
and foreign-enum constants. Set `add(E)` throws for null or foreign enums.

## Map values and complete operation surface

All `long` values, including zero, `Long.MIN_VALUE` and `Long.MAX_VALUE`, are
legal. Presence is independent of value bits. Caller-selected fallback and
absence-return values are not reserved; a returned value equal to one of them
does not alone prove the key was absent. There is no null-valued mapping.

| Method declared on both maps | Result and effect |
| --- | --- |
| `keyType()` | Supplied enum class |
| `keyCapacity()` | Number of constants in the fixed domain; not current size or sparse-table capacity |
| `size()`, `isEmpty()` | Number of present mappings / whether none are present |
| `containsKey(E)`, `containsValue(long)` | Presence and primitive-value tests; absent zero-filled cells do not count as a stored zero |
| `getOrDefault(E,long)` | Stored value or caller fallback, without insertion |
| `getAsLong(E)` | Stored value; an absent valid key throws `NoSuchElementException` using its name |
| `put(E,long,long absentValue)` | Store new value; return previous value if present, otherwise caller absentValue |
| `put(E,long)` | Store new value; return true only for an absent key, false for replacement |
| `putIfAbsent(E,long)` | Insert and return true only when absent; a present zero value is already present and is not replaced |
| `remove(E,long absentValue)` | Delete and return old value if present, otherwise caller absentValue |
| `removeKey(E)` | Delete and return true only when present |
| `clear()` | Remove all mappings; retain storage and, for adaptive, the current mode |
| `forEachOrdinal(OrdinalLongConsumer)` | Visit ordinal and primitive value in ascending ordinal order when not mutated during traversal |
| `forEach(EntryConsumer<E>)` | Visit enum constant and primitive value in the same order |
| `values()` | New `long[]` containing present values in ordinal order |
| `keys()` | New array with runtime component type equal to the supplied enum class, containing present keys in ordinal order |
| `payloadBytes()` | Primitive element bytes of the currently active value/presence or sparse-key/value arrays |

The nested functional interfaces are
`M3EnumLongMap.OrdinalLongConsumer.accept(int ordinal,long value)` and
`M3EnumLongMap.EntryConsumer<E>.accept(E key,long value)`. Each has that single
abstract method and no composition/default behavior. The adaptive map reuses
these types rather than declaring another pair. Both callback methods reject
null even on an empty map, and callback exceptions propagate immediately.

There are no live JDK views, entry objects, `iterator`, stream/spliterator
methods, concurrent operations, conditional expected-value removal, `replace`,
`addTo`, `compute`, `merge` or content equality overrides in these map APIs.
The three-argument `put` is a previous-value operation; it is not a conditional
replace. The maps inherit identity equality from `Object`. Returned arrays are
detached, caller-editable results, but creating them is not a synchronized
snapshot under racing changes. Exporting with `map.forEach(jdkMap::put)` is an
explicit copy into a separate boxed destination, not a live adapter; a callback
failure can leave that destination partially populated.

## Dense representation and traversal lifetime

The dense map stores presence at word `ordinal >>> 6`, bit
`1L << (ordinal & 63)`, with `max(1,(domainSize + 63) >>> 6)` words. It stores
the value directly at `values[ordinal]`. New presence increments size;
replacement leaves it unchanged. Removal clears both the bit and value.
Nonempty clear fills both arrays with zero; empty clear returns immediately.
Storage never resizes because the enum domain is fixed.

`forEachOrdinal` visits words in increasing index and uses trailing-zero bit
selection inside each word. Its current word is a local bit copy, while values
are read from the live value array. `forEach` translates ordinals through the
stored universe array. This is neither a whole-map snapshot nor fail-fast:
there is no modification count or reentrancy guard.

If a callback removes a later member of the current word, the saved bit can
still cause its ordinal to be visited, with the now-cleared value zero.
Inserting a new bit into that already-loaded word need not visit it, whereas
changes to a later word can be seen when that word is loaded. Replacement can
change a later callback's value. Callback effects are not rolled back on an
exception. These source-derived details rule out treating structural callback
mutation as a stable traversal protocol; they are not executed regression
results or a blanket JDK weak-consistency guarantee.

## Adaptive representation: exact sizing and transitions

The adaptive owner adds `isDirect()`, `directPayloadBytes()`,
`directSwitchSize()` and `compact()`. Its fields are only `keyType`, `domainSize`,
`sparse` and `direct`; the currently inactive backing is null after a successful
transition. It does not itself retain a universe array; its active dense owner
does, and sparse key-resolving operations obtain an enum-constant array for the
call.

For domain size `D`, the direct calculation is
`D * 8L + max(1,(D + 63) >>> 6) * 8L`. A requested mapping count `n` chooses
direct mode exactly when that value is less than or equal to
`M3IntLongHashMap.payloadBytesFor(max(1,n))`. Equality selects direct. The sparse
helper uses the actual minimum-eight, power-of-two, `0.65f` constructor rule;
see [int-key map sizing](specify-int-key-map-specializations.md#capacity-deletion-and-failure-effects).

`directSwitchSize` binary-searches `0..D` for the first count satisfying the
rule. Construction applies it to expected size, not live size. The inspected
70-constant test domain has direct payload 576 bytes and switch size 21;
the three-constant domain starts direct at size zero (32 bytes versus the
minimum sparse 96). These are finite source/test calculations, not benchmarks
or a claim that direct is faster or uses less total heap in every workload.

| Action | Mode transition |
| --- | --- |
| Overwrite existing sparse key | Replace value in sparse map; do not reassess or migrate |
| Insert absent sparse key through either put or putIfAbsent | Compare the predicted new count before insertion; if direct is selected, migrate existing mappings then insert the new mapping into direct |
| Remove / removeKey | Remove from active backing; never automatically change mode or shrink the sparse arrays |
| Clear | Empty active backing; retain its mode/capacity |
| Compact while direct | Migrate to newly sized sparse backing only if the current size now favors sparse; otherwise return |
| Compact while sparse | Migrate direct if the current-size rule selects direct; otherwise rebuild sparse only if its current payload is greater than the fresh sizing calculation |

`migrateToDirect` allocates a new dense map, resolves sparse ordinals through
the enum universe, copies mappings, then assigns `direct` and clears `sparse`.
`migrateToSparse` builds a new int/long map sized to current count, copies with
`forEachOrdinal(next::put)`, then assigns `sparse` and clears `direct`.
`copySparse` likewise builds and populates a replacement before assigning it.
There is no asynchronous migration, user-visible release operation or
synchronization around those assignments. Exceptions and allocation failure
do not establish a general transactional API guarantee; old and new backing
arrays can coexist during a transition.

In direct mode traversal delegates to the dense owner. In sparse mode,
`forEachOrdinal` scans domain ordinals in ascending order, probes the live sparse
map for each candidate, and stops after its initially captured entry count has
been visited or the domain is exhausted. This differs from the sparse map's
physical-slot traversal. Sparse `forEach` obtains a universe array once and
translates these ordinals; `keys` does similarly. There is no snapshot cursor,
fail-fast check or safe concurrent-migration contract.

Structural reentrancy has a particularly important limit: sparse traversal
accesses the wrapper's `sparse` field again on later iterations. If a callback
inserts enough keys to switch to direct and the loop continues, migration has
set that field to null, so a subsequent probe can throw `NullPointerException`.
Conversely, a traversal delegated to a dense owner keeps that owner's method
active even if a callback compacts the wrapper into sparse mode. Do not change
membership or mode from a callback when you need stable visit semantics. The
normal return and exception paths do not undo the callback's changes. These
are body-based inferences; the named tests do not exercise reentrant migration.

Payload methods count only active primitive array elements. They exclude the
class/universe references, scalar fields, object and array headers/alignment,
temporary universe copies and old/new arrays during migration. No retained-heap,
allocation-rate, throughput or latency qualification is implied.

## Enum set creation and operations

The private constructor allocates the fixed presence words and obtains the enum
universe. The static factories return independent containers:

| Factory | Contract |
| --- | --- |
| `noneOf(Class<E>)` | Empty set over the supplied domain |
| `allOf(Class<E>)` | Fill valid ordinal bits, masking unused tail bits; empty enum remains empty |
| `of(E first,E... rest)` | Derive the domain from nonnull first, add each supplied element, collapse duplicates; null rest array/elements fail during traversal, and a foreign-enum element fails validation |
| `copyOf(M3EnumSet<E>)` | Copy words and size into a new owner with the same domain; null source fails; this is not a Collection-taking overload |
| `complementOf(M3EnumSet<E>)` | Copy, then flip membership over the domain; source remains unchanged |
| `range(E from,E to)` | Inclusive ordinal interval; null endpoints fail; different declaring classes or descending endpoints throw IllegalArgumentException |

`elementType`, `size` and `isEmpty` report the domain and current member count.
`payloadBytes` is `8L * words.length`, excluding the owner, universe, headers and
references. `add` and `remove` return true only when membership changes.
`containsAll`, `addAll`, `removeAll` and `retainAll` accept only another
`M3EnumSet<E>`. Every one validates nonnull and identical enum class before any
word operation. Foreign domains throw `ClassCastException`, even if the other
set is empty. This stricter bounded bulk API is not a claim of every JDK Set
bulk-operation behavior.

The bulk mutators apply word union, subtraction or intersection respectively,
return whether any word changed and recount bits only after a change.
`containsAll` checks for any other-set bit absent here. `clear` zeros words and
size. `complement` flips words, masks the unused tail and sets size to
`domainSize - oldSize`. For a domain divisible by 64, complete last words need
no partial mask; for an empty domain the sole retained word is forced to zero.
The set has no growth, tombstone, trim or compaction mode.

`forEach(Consumer<? super E>)` rejects null and visits members in ordinal order
using a locally copied word and the stored universe. Like dense-map traversal,
it is not fail-fast or a whole-set snapshot: removing a later current-word
member can still leave a callback for that constant, and updates to later words
can affect later visits. There is no mutation rollback or reentrancy guard.

`toArray()` returns a new array of the supplied enum runtime component type in
ordinal order. `toLongArray()` clones the complete word array, including zero
words for unused portions of the fixed domain; it is not a trimmed BitSet
encoding. Editing either result does not edit the set. Creating either is not
a concurrent atomic snapshot contract. There is no public import of arbitrary
bit words, iterator, stream or spliterator.

Set equality is deliberately owner-specific: the other object must be an
`M3EnumSet`, have the same enum class, and have equal word arrays. Equal empty
sets from different domains are unequal, and a JDK EnumSet is not an equal
M3EnumSet. Hash code is `31 * elementType.hashCode() + Arrays.hashCode(words)`;
it is not JDK Set's element-hash sum. `toString()` uses
`Arrays.toString(toArray())`, so enum constants' string representations are
used in ordinal order. No general Collection/Set interoperability, clone or
serialized-form compatibility is provided.

## Complete member and source evidence

| Complete source | Git blob | Remaining declared implementation members |
| --- | --- | --- |
| [M3EnumLongMap][dense] | `43623113339ffff197d682f2c497b0f07abd4b25` | Fields keyType/universe/values/present/size (37–41); constructor (43–52); both nested accept interfaces (27–35); API (54–226); private containsOrdinal/isValidKey/ordinal (228–243) |
| [M3AdaptiveEnumLongMap][adaptive] | `67a1bf136c21ab2b347f0a33ec4784e17c0eb245` | Four fields (32–35); both constructors (37–58); API (60–287); private shouldUseDirect, static directPayloadBytes(int), migrateToDirect, migrateToSparse, copySparse, isValidKey, ordinal (289–330) |
| [M3EnumSet][set] | `3b9cd8179549116524b7f9252a29ba185c22fc98` | Four fields (24–27); private constructor (29–37); factories/API/Object overrides (40–226); private combine/fillAll/maskTail/recount/checkUniverse/ordinal (228–284) |

No additional protected/package-private methods or named nested implementation
types are declared in these three files. The complete int/long dependency is
covered in the [int-key map reference](specify-int-key-map-specializations.md),
not counted again here. Concrete package-local calls are the adaptive owner's
delegation to dense/int-long owners and consumer interfaces. The set's mention
of dense ordinal handling is a comment, not an executable map call; the factory
does not instantiate these specializations. Broader repository callers were not
proven absent.

## Test assertions inspected, not executed

All three complete test bodies and their helper methods were read.

| Test source / exact methods | Bounded assertions present in source |
| --- | --- |
| [M3EnumLongMapTest][dense-tests]: `wideEnumMatchesJdkAcrossMutationsAndOrdinalOrder` (46–99) | 20,000-step seeded EnumMap differential sequence over 70 constants: both puts/removals, putIfAbsent, presence/default/value/getAsLong, size/empty, key/value arrays and callback ordinal order |
| Same: `narrowEnumAndEdgeContracts` (102–135), `foreignEnumKeysAreRejectedLikeJdk` (139–154), `retainsOnlyPrimitiveLanesAndTheSharedUniverse` (157–174) | Three-constant payload, null/foreign key distinctions, absent getAsLong through differential path, present zero/extrema, clear, wrong class token, field-type shape, 70-key export into EnumMap |
| [M3AdaptiveEnumLongMapTest][adaptive-tests]: `wideEnumMatchesJdkAcrossMutationsAndModes` (58–128), `modeTransitionsAndOrdinalOrderInSparseMode` (131–213) | 20,000-step finite differential sequence, computed mode rule, switch at 21 for 70 constants, preserved mappings/ordinal order, overwrite stays sparse, constructor sizing, removal/clear retain mode, explicit compact changes mode/shrinks |
| Same: `narrowEnumIsDenseFromTheStartAndEdgeContracts` (216–255), `foreignEnumKeysAreRejectedLikeJdkInBothModes` (259–276), `retainsOnlyTheActiveBackingAndNoKeyReferences` (279–297) | Narrow direct mode, null/foreign/absent distinctions, expected-size rejection, field-type list and full-domain boxed export; the reflection list alone does not inspect transient allocation or prove active-field nullness |
| [M3EnumSetTest][set-tests]: `wideEnumMatchesJdkAcrossMutationsAndBulkOperations` (55–110) | 20,000-step seeded EnumSet comparison over 70 constants for single/bulk mutation, range/copy/complement, membership/ordinal array/callback order; full-domain tail mask and payload |
| Same: `narrowEnumEdgeContractsMatchJdk` (113–154), `foreignEnumConstantsAreRejectedLikeJdk` (158–168), `retainsOnlyPresenceWordsAndTheSharedUniverse` (171–178) | Null/foreign/non-enum queries, add/remove no-ops, range error, string output, M3-only equality checks, complements, bulk domain refusal, field-type list |

None of these read assertions proves empty enum domains, constant-specific class
bodies, structural callback mutation/migration, total heap use, allocation
failure recovery, concurrent access or full JDK compatibility. Their titles do
not broaden their concrete assertions. The source headers expressly describe
finite proving-ground comparisons. Preserve that limit, historical receiver
scope, Apache-2.0 attribution and the ordered **String → arrays → collections
→ AST/compiler → remaining families** policy; this reference advances no
runtime receiving phase.

[dense]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3EnumLongMap.java
[adaptive]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3AdaptiveEnumLongMap.java
[set]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3EnumSet.java
[factory]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3Collections.java
[module]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/module-info.java
[pom]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/pom.xml
[policy]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/docs/m3jdk21-porting-invariant.md
[dense-tests]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/test/java/com/m3/collections/M3EnumLongMapTest.java
[adaptive-tests]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/test/java/com/m3/collections/M3AdaptiveEnumLongMapTest.java
[set-tests]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/test/java/com/m3/collections/M3EnumSetTest.java
