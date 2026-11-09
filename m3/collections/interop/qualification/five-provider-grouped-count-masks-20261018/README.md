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
`a7c4dd4eb8de5d12b08d80e81fcb0c0e94830fd1b242dc6f7adedc50d1fd059a`,
Git blob `54f2345be366083dac32fc9ca379183fa17fd6f5`.
Postimage:
SHA256 `0e6314a8f73d66738956278d7b70022f4990b7e49fb4b469dea2fa003ca7e1e0`,
Git blob `749e8c105cc5d28ec658500af510be1dfcfbf332`.
New JUnit:
ABSENT-before, SHA256 `8dc2a4a0defb76ac027f83c22a9e8caccf39cee826aad72783656dcbfa1530ea`,
Git blob `169910504c84fb1c8d615f6e1f1d586be04bd9b6`.

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
