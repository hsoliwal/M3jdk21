<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3ArrayListMultimap (owned)

`com.m3.collections.guava.M3ArrayListMultimap` is an owned implementation of the public contract of
Guava 33.4.8-jre `ArrayListMultimap` (factories `create()`, `create(int, int)`, `create(Multimap)`, the
`ListMultimap` API, every view and `trimToSize`). It implements the Guava `ListMultimap`, `Multimap` and
`Multiset` API interfaces directly; no Guava implementation class, helper, interface default or body is
called or copied. Production code links Guava API types and the JDK only.

Storage is arrays, offsets and masks with no per-entry objects: an open-addressed key directory
(`Object[] keys`, `int[] hashes`, power-of-two `int[] index` with tombstones, `int[] before/after` order
links, `long[] live` slot bits) and one dynamic `Object[]` value segment per key with `int[] counts`.
`int[] stamps`/`int[] births` and `keyModCount` give Guava-shaped fail-fast behaviour. Entry objects exist
only where an API returns them (`entries()`, `asMap().entrySet()`, `keys().entrySet()` elements).

Deliberate differences: key iteration follows insertion order (Guava: unspecified HashMap order); the
serialized form is the owned format (key count, then key, value count, values) with count validation;
`MappingCursor.remove` refuses a stale key-directory removal before mutating, where Guava would leave an
empty value list behind after throwing.

Validation (`mvn -f pom.xml test`, Java 21, `-Xlint:all -Werror`): 22 JUnit tests; the seeded
differential run applies 96,000 operations to the real Guava provider and this class and makes 1,025,276
comparisons; JaCoCo line and branch coverage are 100% for every class, gated at 99%. The steady-state
allocation probe measures 0 bytes for put/containsEntry/containsKey/keys().count/remove/forEach.
See `m3/collections/qualification/owned-guava-array-list-multimap-20261018/`.
