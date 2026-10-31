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

## 2026-10-27 Guava public Multimap API source closure

Now ships modified Apache-2.0 Guava 33.4.8-jre `Multimap`,
`ListMultimap` and `Multiset` with nested `Multiset.Entry`
as source under exact original Guava names. The independent already-owned
`M3ArrayListMultimap` implements all storage and protocol details through
arrays/primitive keys/counts/index masks; no Guava implementation bodies
and no added Entry/Node store. Real Guava is retained in TEST scope for
the existing 96,000 seeded operation differential test suite.

Canonical Synexia source/Maven recipe:
`com.synexia.m3.InlineGuavaListMultimapApi20261027`, and byte-exact
`Multiset` API source reused from the Synexia Guava HashMultiset recipe.
Target receives generated Java/test sources and data-only receipts only.
Actual external runtime dependency on `com.google.guava:guava` is removed
from this isolated product module; `jdeps` and Maven runtime-tree gates
must pass at exact head before promotion. First-party M3 Java class
JaCoCo coverage still requires 99%; original vendored API interfaces
are excluded from that first-party implementation denominator only.

**Cross-jar constraint:** Another standalone M3 Guava HashMultiset module
contains the same `com.google.common.collect.Multiset` name. Until a
single source-built module owns and packages the complete Guava API
graph, do NOT distribute these two jars together on one classloader or
combine them with stock Guava. JSpecify/GWT annotation and default
spliterator metadata are not byte-for-byte equivalent; those gates HOLD.
