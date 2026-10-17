<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 generic filtered Collection/Map owners intake from qualified first-party donor

Canonical source and Maven/OpenRewrite recipe: [Synexia #11946](https://github.com/hsoliwal/com.synexia/pull/11946),
exact source `fc95bae20658979bd79edd2451213e089a067774`. Donor [M3JDK21 #985](https://github.com/hsoliwal/M3jdk21/pull/985)
at `e81a42da2158801a9358b5371e22563c24f4853f`. Current
target parent `04fb615b658604271d63a713402d7ce72b878844`
(PR #989 primitive sorted-set algebra).

Two source owners, copied **byte-for-byte** from the original qualified
first-party Git commit, are new to this target's current family stack.
They expose JDK Collection/Set/SortedSet/NavigableSet and
Map/SortedMap/NavigableMap backed filtered facades while retaining the
actual M3 tree/linked/reference-slot payload owner. Erased static types
retain runtime NavigableSet/Map capability. Sorted and navigable
comparators/representatives and hidden-entry effects remain backed.
Source copying is intentional donor reuse, not a duplicate independent
algorithm implementation.

Generic JDK APIs may return one `Map.Entry` boundary adapter per
visible entry. Such public contract objects do not live in a cached
second map, internal node/entry table or primitive ID plane. The
primitive String/array/mask/JNI owners remain unchanged. No generic
filter calls an arbitrary Java predicate from JNI.

The existing finite first-party M3 filtered-map JDK oracle is received
unaltered in the test tree, plus a new Java21 test of actual M3TreeSet,
M3LinkedHashSet, M3TreeMap, JDK TreeSet, source-order views,
stored comparator-equivalent keys, single-pass stable sortedSnapshot,
guarded additions/removal, and no resident per-element array.
This is not complete Guava/Eclipse/Commons/Spring signature parity.

The original Java source and test blobs are source-sealed in a
five-file SHA256 manifest, enforced here by
`verify_generic_filtered_wrappers.py`. CI compiles the whole target
M3 Java21 core, runs new and inherited primitive java/sorted/filtered
regression tests and actual C17 JNI `-Xcheck:jni`. The separate
Synexia Maven/OpenRewrite SDK is also required for recipe promotion.

Do not conflate filtered views with Apache PredicatedCollection
(validator), Guava mutable filtered sets, Spring MultiValueMap or
Multiset count semantics. Extra provider-specific adapter contracts,
concurrency/reference/serialization, installed JDK jtreg/ABI and
JMH/memory pressure are still independently unqualified.
