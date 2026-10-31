<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3HashMultiset (owned)

`com.m3.collections.guava.M3HashMultiset` is an owned implementation of the public contract of
Guava 33.4.8-jre `HashMultiset`: the factories `create()`, `create(int)` and `create(Iterable)`, the
full `Multiset` API, and the `elementSet`/`entrySet` views. It implements the Guava `Multiset` API
interface directly. No Guava implementation class, helper, interface default or body is called or copied.

Storage is a set of dense parallel lanes: `Object[] elements`, `int[] counts`, `int[] hashes` and
`int[] gens`. A power-of-two `int[] index` (dense offset+1, linear probe masked by `length - 1`) uses
backward-shift deletion, so it leaves no tombstones. A removal moves the last dense element into the
freed offset. The total is a `long`, saturated by `size()`. `modCount` changes only when an element
appears or disappears, which matches Guava's backing-map fail-fast rules. `Multiset.Entry` objects exist
only as `entrySet()` iteration results, and their counts are read live.

Deliberate differences:
- Iteration follows dense order; Guava's HashMap order is unspecified.
- The serialized form is the owned format (element count, then element and count pairs), with validation.
- A stale `elementSet`/`entrySet` iterator `remove()` throws CME before mutating, where Guava first zeroes
  the count.
- `forEachEntry` throws CME at the first structural change, where Guava's `HashMap.forEach` keeps visiting
  and then throws.

Validation (`mvn -f pom.xml test`, Java 21, `-Xlint:all -Werror`): 16 JUnit tests. The seeded differential
run applies 96,000 operations to the real Guava provider and this class and makes 867,116 comparisons.
JaCoCo line and branch coverage is 100% (310/310 lines, 230/230 branches over 9 classes), gated at 99%
per class. The steady-state allocation probe measures 0 bytes for
add/count/contains/remove/setCount/forEachEntry/size.

## Source-closed Guava Multiset interface (2026-10-27)

Now ships the Apache-2.0 `com.google.common.collect.Multiset` interface
under the **original Guava public name**, with its nested `Entry` API.
Inlined source is version pinned to Guava v33.4.8 and owned refactoring
replaces Guava-only default-method helpers with JDK Java21 equivalents.
The existing `M3HashMultiset` stays an independent array/index/count owner:
`Object[]` element pool, dense `int[]` count/hash/generation lanes,
primitive open-addressed hash slots, backward-shift deletion. No new
per-entry/payload object graph and no Guava implementation body was copied.

Maven Guava dependency is **test scope only**, enabling original seeded
96,000-step Guava HashMultiset oracles without any runtime vendor JAR in
the published module. First-party 99%-coverage gate remains; inlined
`com/google/common/collect/**` API source alone is excluded from that
ownership-specific coverage denominator, not compilation/tests.

Canonical Maven/OpenRewrite recipe:
`com.synexia.m3.InlineGuavaMultisetApi20261027` in
`hsoliwal/com.synexia`. No recipe executable is installed here.
Default spliterator's characteristics and JSpecify/GWT/ErrorProne
annotation metadata are **not claimed binary-metadata-equivalent**;
erased public method names/descriptors are retained. Standalone-only
JPMS packaging must avoid co-loading duplicate Guava classes.
The full Guava SDK/ABI and other four-provider modules remain open.
