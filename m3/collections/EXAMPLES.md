<!-- SPDX-License-Identifier: Apache-2.0 -->
# Owned collection examples

Each owned five-provider module carries one runnable example under `implementations/<module>/examples/`. An
example calls the provider's public API identically on the provider class and on the owned class, logs both
results through `java.util.logging`, and then measures the two implementations the same way:

- **allocation**: `ThreadMXBean` bytes allocated per steady-state operation (200,000 warm-up operations, then
  200,000 measured), where one operation adds and removes the same element or re-puts the same mapping so the
  size does not change;
- **footprint**: heap retained after GC per stored element, over 40 copies filled from the same pre-boxed keys
  and values (the boxes are shared and so not counted).

The examples are generated: `qualification/five-provider-inventory-20261018/example-template/gen-example.sh`
fills `Example.java.tpl` from each module's `examples/example.spec` (the class names, the constructor calls,
and the demo, fill and steady-state bodies). Regenerate rather than editing a generated file.

Run one from its module directory, after `mvn package` there, with the provider jars on the class path:

```
java -cp "target/classes;<guava | commons-collections4 | spring-core jar>" examples/<Class>.java
```

The numbers below are one local run (Windows 11, JDK 21.0.9, `-XX:+UseSerialGC`, 512 MB heap). They are not
a benchmark: they say what each implementation allocates on its hot path and retains per element on this JDK.
Enum-keyed maps hold at most one mapping per constant, so their footprint row counts 64 stored mappings.

| module | example | provider B/op | m3 B/op | provider B/element | m3 B/element | elements |
|---|---|---|---|---|---|---|
| `guava-array-list-multimap` | [`ArrayListMultimapExample`](implementations/guava-array-list-multimap/examples/ArrayListMultimapExample.java) | 32.6 | 40.0 | 13.6 | 12.7 | 10000 |
| `guava-hash-multiset` | [`HashMultisetExample`](implementations/guava-hash-multiset/examples/HashMultisetExample.java) | 0.0 | 0.0 | 45.9 | 26.0 | 10000 |
| `apache-hash-bag` | [`HashBagExample`](implementations/apache-hash-bag/examples/HashBagExample.java) | 0.0 | 0.0 | 45.9 | 21.1 | 10000 |
| `guava-enum-hash-bi-map` | [`EnumHashBiMapExample`](implementations/guava-enum-hash-bi-map/examples/EnumHashBiMapExample.java) | 0.0 | 0.0 | 47.1 | 22.7 | 64 |
| `guava-enum-bi-map` | [`EnumBiMapExample`](implementations/guava-enum-bi-map/examples/EnumBiMapExample.java) | 0.0 | 0.0 | 11.0 | 19.0 | 64 |
| `apache-dual-linked-hash-bidi-map` | [`DualLinkedHashBidiMapExample`](implementations/apache-dual-linked-hash-bidi-map/examples/DualLinkedHashBidiMapExample.java) | 40.0 | 0.0 | 93.1 | 91.7 | 10000 |
| `spring-multi-value-map-adapter` | [`MultiValueMapAdapterExample`](implementations/spring-multi-value-map-adapter/examples/MultiValueMapAdapterExample.java) | 0.0 | 0.0 | 14.4 | 14.4 | 10000 |
| `guava-enum-multiset` | [`EnumMultisetExample`](implementations/guava-enum-multiset/examples/EnumMultisetExample.java) | 0.0 | 0.0 | 0.1 | 0.0 | 10000 |

Reading the table:
- `ArrayListMultimap`: both allocate on the steady-state path because `get(k)` returns a list view object in
  both implementations; the owned view is 8 bytes larger. Footprint per mapping is within 1 byte.
- `HashMultiset` and `HashBag`: the owned dense lanes retain about half of the provider's per-element heap
  (no `Count` or node objects); neither allocates on the steady-state path.
- `EnumHashBiMap`: the owned ordinal lanes retain about half. `EnumBiMap`: the owned class retains more
  (19 versus 11 bytes per mapping) because its fixed lanes are sized by the enum, not by the mappings
  present; the provider's `EnumMap` pair is already compact for a full 64-constant domain. The provider's
  inverse `EnumHashBiMap` view prints in `HashMap` order; the owned one prints in insertion order.
- `DualLinkedHashBidiMap`: the provider allocates 40 bytes per `put` of an existing mapping (a new node
  value wrapper); the owned slot tables allocate nothing. Footprint per mapping is within 2 bytes.
- `MultiValueMapAdapter`: both adapt the same `LinkedHashMap<K, List<V>>`, so the numbers are equal; the
  owned class differs only in its first-value view (see the module README).
- `EnumMultiset`: both keep one `int` per constant, so the per-occurrence footprint rounds to zero.
