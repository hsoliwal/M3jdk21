<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3EnumMultiset (owned)

`com.m3.collections.guava.M3EnumMultiset` is an owned implementation of the public contract of
Guava 33.4.8-jre `EnumMultiset`: the factories `create(Class)`, `create(Iterable)` and
`create(Iterable, Class)`, the full `Multiset` API, and the `elementSet`/`entrySet` views. It implements
the Guava `Multiset` API interface directly. No Guava implementation class, helper, interface default or
body is called or copied.

Storage is two lanes indexed by ordinal: `int[] counts` and a `long[] present` mask with one bit per
constant whose count is positive. The enum universe is read once per enum class through a `ClassValue`
and shared, so an instance retains only its lanes, a `long` total (saturated by `size()`) and the distinct
count. Cursors walk the presence mask with `numberOfTrailingZeros`, so a sparse multiset over a large enum
skips empty words. `Multiset.Entry` objects exist only as `entrySet()` iteration results; they hold an
ordinal and read their count live.

Guava-specific semantics kept:
- Iteration is ordinal order and never fail-fast: a cursor sees counts added or zeroed while it runs,
  `hasNext()` re-scans from its position, and `remove()` zeroes the last returned ordinal even after
  external changes.
- The occurrence iterator removes through the ordinal cursor when the count read at `next()` was 1, and
  through `remove(element, 1)` otherwise, with Guava's bookkeeping of the count seen at `next()`.
- `setCount(E, int, int)` is declared with an erased `Object` parameter in a package-private base class,
  as Guava's `AbstractMultiset` erases it: a raw call with a foreign element compares counts and returns
  false instead of failing a bridge-method cast.
- Foreign or non-enum arguments: `count`/`contains`/`remove` return 0 or false; `add`/`setCount` throw
  `ClassCastException` with Guava's message; null `add`/`setCount` throw `NullPointerException`.
- View bulk rules (`removeAll` iterates this view when the argument is a larger set; `retainAll` and
  `removeAll` reject null), `entrySet().remove` through compare-and-set, and the empty-`Iterable`
  factory message.
- The spliterator reports `SIZED` only and does not split; Guava's splits at implementation-defined
  points, so the differential run compares the concatenated walk.

Deliberate differences:
- The serialized form is the owned format (the enum class, the element count, then constant and count
  pairs), validated on read: a non-enum type, an out-of-range count, a foreign constant or a repeated
  constant is refused with `InvalidObjectException`.
- `verifyInvariants()` (package-private) checks the shared universe, mask, distinct count and total.

Validation (`mvn -f pom.xml test`, Java 21, `-Xlint:all -Werror`): 15 JUnit tests. The seeded differential
run applies 96,000 operations to the real Guava provider and this class over a 70-constant enum (two mask
words) and makes 960,225 comparisons, including exact iteration order, traversals interleaved with
external modification and live entries. JaCoCo line and branch coverage is 100% (254/254 lines, 200/200
branches over 13 classes), gated at 99% per class. The steady-state allocation probe measures 0 bytes for
add/count/contains/remove/setCount/forEachEntry/size/hashCode.
