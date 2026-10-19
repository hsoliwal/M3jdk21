<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3EnumHashBiMap (owned)

`com.m3.collections.guava.M3EnumHashBiMap` is an owned implementation of the public contract of Guava
33.4.8-jre `EnumHashBiMap`. It covers the `create(Class)` and `create(Map)` factories, `keyType()`, the
full `BiMap`/`Map` API (including `forcePut`, `replaceAll` and the Map defaults), the forward views, and a
live `inverse()` with its own views. It implements the Guava `BiMap` interface directly. No Guava
implementation class, helper, interface default or body is called or copied; Guava's `EnumHashBiMap`/
`EnumBiMap` classes are referenced only to read the key type of a provider map passed to `create(Map)`.

Storage is ordinal-indexed. The lanes are `Object[] vals` and a `long[] present` bit per key ordinal; null
values are legal, so presence is a bit and not a sentinel. A power-of-two `int[] index` with backward-shift
deletion over `int[] valueHashes` serves the inverse direction. Forward iteration is ordinal order and
weakly consistent, as EnumMap is. Inverse iteration is fail-fast on `valueMod`, which changes exactly when
the value set changes, as Guava's backing HashMap does. Entry objects exist only where a view iterator
returns them.

Deliberate differences:
- Inverse iteration follows ordinal order; Guava's HashMap order is unspecified.
- Where Guava would leave its two maps inconsistent, the owned class checks the key type, or validates
  every new key, before it mutates anything. This covers an inverse `put`/`forcePut`/entry `setValue` with a
  key of the wrong enum type, an inverse `replaceAll` producing null or wrong-type keys, and a forward
  iterator `remove` after the key was removed externally.
- Forward `replaceAll` computes every new value before it commits.
- A removed forward entry's `toString` is `Entry was removed`.
- The serialized form is the owned format (key type, size, then key and value pairs), with validation.

Validation (`mvn -f pom.xml test`, Java 21, `-Xlint:all -Werror`): 17 JUnit tests. The seeded differential
run applies 96,000 operations to the real Guava provider and this class and makes 1,248,156 comparisons;
forward `toString` is compared exactly. JaCoCo line and branch coverage is 100% (422/422 lines, 306/306
branches over 13 classes), gated at 99% per class. The steady-state allocation probe measures 0 bytes.
