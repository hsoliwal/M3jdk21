<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3LinkedHashMultiset (owned)

`com.m3.collections.guava.M3LinkedHashMultiset` is an owned implementation of the public contract of
Guava 33.4.8-jre `LinkedHashMultiset`: the factories `create()`, `create(int)` and `create(Iterable)`, the
full `Multiset` API, and the `elementSet`/`entrySet` views. It implements the Guava `Multiset` API interface
directly. No Guava implementation class, helper, interface default or body is called or copied.

Storage is the dense-lane layout of the owned `M3HashMultiset` (PR #1418) plus an insertion-order chain:
`Object[] elements`, `int[] counts`, `int[] hashes`, `int[] gens`, `int[] before` and `int[] after`. A
power-of-two `int[] index` (dense offset+1, linear probe masked by `length - 1`) uses backward-shift
deletion, so it leaves no tombstones. A removal unlinks the element, then moves the last dense element
into the freed offset and re-points its index entry and both chain neighbours. Iteration follows the
chain from `first` to `last`, so it is insertion order: an element removed and re-added joins the end,
and count changes keep its position, as in Guava's `LinkedHashMap`. The total is a `long`, saturated by
`size()`. `modCount` changes only when an element appears or disappears. `Multiset.Entry` objects exist only
as `entrySet()` iteration results, and their counts are read live.

Deliberate differences:
- The serialized form is the owned format (element count, then element and count pairs in insertion
  order), with validation.
- A stale `elementSet`/`entrySet` iterator `remove()` throws CME before mutating, where Guava first zeroes
  the count.
- `forEachEntry` throws CME at the first structural change, where Guava's `LinkedHashMap.forEach` keeps
  visiting (including appended entries) and then throws.
- The spliterator reports `SIZED` and does not split; Guava's splits at implementation-defined points.

Validation (`mvn -f pom.xml test`, Java 21, `-Xlint:all -Werror`): 17 JUnit tests. The seeded differential
run applies 96,000 operations to the real Guava provider and this class and makes 960,208 comparisons, with
exact iteration order, text and iterator-driven removals. JaCoCo line and branch coverage is 100% (353/353
lines, 256/256 branches over 10 classes), gated at 99% per class. The steady-state allocation probe measures
0 bytes for add/count/contains/remove/setCount/forEachEntry/size.
