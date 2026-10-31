<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# One M3 indexed group directory for five real collection APIs

Canonical source owner: Synexia [PR #12263](https://github.com/hsoliwal/com.synexia/pull/12263)
at source recipe commit `29e6d23fff81964cd86d7eb179225680cb6dea52`.
Named Maven/OpenRewrite recipe
`com.synexia.m3.FiveProviderGroupSetBagIndexMasks20261018`,
reusing the installed `M3HashPinnedJavaSnapshotRecipe` Java LST engine.

Product target: M3JDK21 `master@2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`;
no direct edits to product master and no rebase of Synexia develop.

## Source seam and unchanged public behavior
Preimage:
`M3FiveProviderKeyGroups.java` SHA256
`484a79decdfd5507dbdb4276c6a08df9048d98ddf7756a630f5493cd85e48a4c`,
Git blob `f546905499583ae209c1bd92f0b3f9e6f7c15699`.
Postimage:
SHA256 `49767629cccbc847c4a66252f86ce18ce57be8aa49a685350624e570a126597f`,
Git blob `13dd02b3212cb14e7929b964c025f7e5047e5012`.
New JUnit:
ABSENT-before, SHA256 `97debdc8af2e48e695191daa9a5ad872b9d16131556b9372441ad75c3185a30a`,
Git blob `fd29c1cf71f8adc3ce718fc38e03913ac4293ecb`.

Every existing public method on the M3 group owner retains its name,
signature and behavior; new methods are additive. Old list-valued
JDK/Guava/Eclipse/Spring/Apache interfaces remain distinct.

## Shared M3 array/index/flag mechanics

One capture of `M3FiveLibraryIndexViews` remains authoritative for
Object[] key/value payloads. This grouped descriptor reuses:
- Existing `M3LinkedHashMap<K,Integer>` group hash directory;
- `int[] starts` (prefix boundaries), `int[] ordinals` (occurrence
  physical rows), `int[] groupOrder` (visible/sorted group IDs);
- An `Object[]` of distinct group representatives, at most one per key.

Group state selection calls unchanged `M3IndexPathMasks.select` with
the primitive group-ID lane as both row IDs and physical offsets,
against caller-borrowed exact-length `int[][]` 32-bit state words.
Required masks, forbidden masks, negative high bit (bit31), multiple
stages and checked C17 JNI preserve original contract. No
Java object/predicate/comparator enters JNI; no native ABI changes.

New count-filter and stable rank operations rearrange only primitive
group IDs. They do not copy captured payloads, construct retained
per-occurrence Node/Entry objects, or mutate the immutable source.
Output collections remain independent, mutable M3-backed provider
objects with expected allocation at their API boundaries.

| Library | Additional real public API projections |
| --- | --- |
| JDK 21 | `Set<K>`, `Map<K,Set<V>>`, `Map<K,Integer>` |
| Guava | `SetMultimap<K,V>`, `SortedSetMultimap<K,V>`, key `Multiset<K>` |
| Eclipse Collections | `MutableSetMultimap<K,V>`, `MutableBagMultimap<K,V>`, key `MutableBag<K>` |
| Spring Framework | last-wins scalar `MultiValueMap<K,V>` |
| Apache Commons Collections | `SetValuedMap<K,V>`, key `MultiSet<K>` |

JDK/Spring Map.size counts distinct keys; Guava/Eclipse/Apache
multimap/bag APIs count pairs or repeated occurrences; set-valued
maps suppress duplicate pairs. Nulls and identity are governed by each
actual provider contract. No API proxy that silently impersonates a
vendor class or stores a second parallel collection hierarchy.

## Qualification / exact-head HOLD

Real-provider JUnit tests compare 850 seeded group-state projections
against independent JDK oracle maps/lists; test per-group sorting
stability, count filtering, bit31/multiple stages, null keys/values,
repeated index paths, SetMultimap/BagMultimap/multiset cardinality,
Spring scalar last-wins and detached provider mutations. Source
hashes and Git blobs must remain identical to Synexia recipe postimages.

Target CI builds ALL actual M3 core owners with `javac --release 21
-Xlint:all,-module -Werror`; packages real java.base-only core source
classes as a JAR; runs actual Maven Guava/Eclipse/Spring/Apache/JUnit;
compiles existing C17 mask JNI libraries and reruns under
`-Xcheck:jni`. Original standalone M3 core behavior checks run too.
Source-only core JAR is a **separate proof**, not an accepted replacement
for the authoritative `m3/collections/pom.xml` Maven/JUnit reactor
because that POM requests custom source-only JUnit coordinate
`5.10.2-m3-source-1` absent from Maven Central.

At authoring, hosted JUnit/OpenRewrite/JNI results remain **PENDING**.
No claims of full 5-vendor API/class coverage, installed JDK/jtreg/JCK,
serialization, JMM/concurrency, CPU/retained-heap improvement or
global String->arrays->collections phase qualification are made.
Preserve all upstream donor licenses: M3 original Apache-2.0;
Guava/Spring/Apache Commons Apache-2.0; Eclipse EPL-1.0/EDL-1.0;
OpenJDK GPL-2.0 with Classpath Exception. Challenge algorithms are
taxonomies only, no copied solutions.
