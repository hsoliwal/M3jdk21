<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3EnumBiMap (owned)

`com.m3.collections.guava.M3EnumBiMap` is an owned implementation of the public contract of Guava 33.4.8-jre
`EnumBiMap`. It covers the `create(Class, Class)` and `create(Map)` factories with Guava's type inference,
`keyType()`/`valueType()`, the full `BiMap`/`Map` API, all views, and a live `inverse()`. It implements the
Guava `BiMap` interface directly. No Guava implementation class, helper, interface default or body is called
or copied; Guava's `EnumBiMap`/`EnumHashBiMap` classes are referenced only to read the enum types of a provider
map passed to `create(Map)`.

Storage is two `int[]` ordinal lanes and nothing else. `lanes[0][k]` holds the value ordinal + 1 of key `k`,
and `lanes[1][v]` holds the key ordinal + 1 of value `v`; 0 marks absence, so each lane is its own presence
mask. Every operation is written once and runs on either side: side 0 is this map and side 1 is the inverse
flyweight. Both directions iterate in ordinal order and are weakly consistent, as Guava's two backing
EnumMaps are. Entry objects exist only where a view iterator returns them.

Deliberate differences:
- Where Guava would leave its two EnumMaps inconsistent after throwing, the owned class validates before it
  mutates. This covers `replaceAll` functions that return null or a wrong-type value, and raw puts with a
  wrong-type value.
- A removed entry's `toString` is `Entry was removed`.
- The serialized form is the owned format (key type, value type, size, pairs), with validation.

Validation (`mvn -f pom.xml test`, Java 21, `-Xlint:all -Werror`): 15 JUnit tests. The seeded differential run
applies 96,000 operations to the real Guava provider and this class and makes 960,066 comparisons; both
`toString` forms and the inverse are compared exactly. JaCoCo line and branch coverage is 100% (266/266
lines, 188/188 branches over 8 classes), gated at 99% per class. The steady-state allocation probe measures
0 bytes.
