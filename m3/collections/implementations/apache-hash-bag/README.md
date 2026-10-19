<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3HashBag (owned)

`com.m3.collections.apache.M3HashBag` is an owned implementation of the public contract of Apache
Commons Collections 4.5.0 `org.apache.commons.collections4.bag.HashBag`: the `()`, `(Iterable)` and
`(Collection)` constructors, the full `Bag` API, the unmodifiable `uniqueSet()` view and the Commons
`toString`/`toArray(T[])` forms. It implements the Commons `Bag` interface directly. No Commons
implementation class, helper or body is called or copied.

Storage reuses the dense-lane design of the owned `M3HashMultiset` (#1418). The lanes are `Object[] elements`,
`int[] counts` and `int[] hashes`, plus a power-of-two `int[] index` with backward-shift deletion. A removal
moves the last dense element into the freed offset. Two counters mirror the provider's fail-fast rules:
`modCount` (every bag mutation except iterator removal, as in Commons) and `structMod` (element appearance
or disappearance, as in Commons' backing HashMap).

Commons-specific semantics kept:
- `add` returns true only for a new element, and every `add` call counts as a modification.
- `removeAll` removes one copy per argument occurrence, and a null argument changes nothing.
- `containsAll` and `retainAll` compare counts, using Commons' retain rule.
- `toArray(T[])` null-fills the whole tail.

Deliberate differences:
- Iteration follows dense order; Commons' HashMap order is unspecified.
- The serialized form is the owned format (element count, then element and count pairs), with validation.
- The class is final; Commons' HashBag can be subclassed.

Validation (`mvn -f pom.xml test`, Java 21, `-Xlint:all -Werror`): 16 JUnit tests. The seeded differential
run applies 96,000 operations to the real Commons provider and this class and makes 768,082 comparisons.
JaCoCo line and branch coverage is 100% (251/251 lines, 166/166 branches over 4 classes), gated at 99% per
class. The steady-state allocation probe measures 0 bytes.
