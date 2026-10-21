<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3MultiValueMapAdapter (owned)

`com.m3.collections.spring.M3MultiValueMapAdapter` is an owned implementation of the public contract of
Spring Framework core 6.2.0 `org.springframework.util.MultiValueMapAdapter`. It covers:
- the `(Map<K, List<V>>)` constructor;
- the full `MultiValueMap` API (`getFirst`, `add`, `addAll`, `addIfAbsent`, `set`, `setAll`,
  `toSingleValueMap`, `asSingleValueMap`);
- every `Map` method;
- serialization.

It implements the Spring `MultiValueMap` interface directly. No Spring implementation class, helper,
interface default or body is called or copied, including Spring's package-private first-value adapter.

## Storage

The provider contract is write-through over the caller's target map. Every change lands in that map and
every read observes it. The adapter therefore keeps one reference and no lanes of its own. The scaffold's
planned key and pool lanes do not apply here, because a second copy would break write-through.

Lists the adapter creates (`add`, `addAll`, `set`) are `ArrayList`s, as Spring's are. Caller-supplied
lists stay aliased. In M3jdk21 `ArrayList` is itself backed in place by M3 array lanes, so per-key values
sit on array lanes without a second list type. This also keeps list classes identical to Spring's, which
the differential checks.

The adapter's own hot paths allocate nothing:
- `add` passes a non-capturing list factory to `computeIfAbsent`;
- `getFirst`, `get`, `containsKey`, `size` and `forEach` delegate to the target.

## Spring-specific semantics kept

- `add` goes through `computeIfAbsent`, so a key mapped to null gets a fresh list.
- `addAll(key, null)` fails without adding a mapping.
- `addIfAbsent` tests `containsKey`.
- `toSingleValueMap()` returns a `LinkedHashMap` that skips null and empty lists.
- `asSingleValueMap()` returns a new live view per call. In that view:
  - `get`, `put` and `remove` adapt null or empty lists to null;
  - `put` stores `Collections.singletonList(value)`, or null;
  - `values`, `entrySet`, `forEach` and `containsValue` read index zero, so they fail on null or empty
    lists exactly as Spring's do;
  - `equals` answers false on a null list but propagates the empty-list failure;
  - `hashCode` and `toString` use the list-valued target;
  - its iterators do not support removal.
- `equals`, `hashCode` and `toString` delegate to the target, and the `Map` defaults dispatch through the
  adapter.

## Deliberate differences

- `serialVersionUID` is the owned value 0. Spring's is computed. The serialized form, the adapter and its
  target map, has the same shape.
- The class is final.

## Validation

Run `mvn -f pom.xml test` (Java 21, `-Xlint:all -Werror`). There are 12 JUnit tests.

The seeded differential run applies 96,000 operations to the real Spring adapter and to this class, each
over its own `LinkedHashMap` target. It reports 576,063 comparisons. After every operation it compares the
exact target text, the class of every value list, hash and equality. Operations include the first-value
view and its failure modes.

JaCoCo line and branch coverage is 100%: 102/102 lines and 48/48 branches over 6 classes, gated at 99% per
class. The steady-state allocation probe measures 0 bytes. It covers `add`, `get`, `getFirst`, the
single-value view's `get`, `containsKey`, `size` and `forEach`.

Relation to `implementations/spring` `M3MultiValueMap`, the owner of the LinkedMultiValueMap row:
- That class owns its storage.
- Its `M3SingleValueMapView` takes an `M3MultiValueMap`, so it cannot view an adapter.
- This class carries a private first-value view of the same protocol over any `MultiValueMap`.
- Widening that view's constructor is a proposal only.
