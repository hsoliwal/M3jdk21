# M3 collection SDK: five correctly identified contract families

This is an **additive SDK surface**, not a renamed donor class library. Preserve JDK,
Guava, Eclipse Collections, Spring Framework, and Apache Commons Collections package/type
identities. Pair each exposed contract with the existing correctly named M3 storage owner.
A Guava `ListMultimap`, Apache `ListValuedMap`, and Spring `MultiValueMap` are not synonyms.
In particular Spring is a map of lists with key-count size; the two multimap APIs count
mappings and have different absent/empty and view behavior. Keep their contracts separate.

The historical `name-mapping.json` remains the source-migration authority. The accompanying
`sdk-family-bindings.tsv` maps **this factory surface** to exact API names and physical
owners; it does not replace that migration history or the accumulated algorithm catalogue.
There are 23 mapped factories, not 22 complete donor libraries or an exhaustive API census.
All other APIs remain outside this packet's qualification boundary.

## Names and storage

| Family | Factory | Exact public contract | Accompanying M3 owner(s) |
|---|---|---|---|
| JDK | `jdkMap` | `java.util.Map` | `com.m3.collections.M3LinkedHashMap` |
| JDK | `jdkSet` | `java.util.Set` | `com.m3.collections.M3LinkedHashSet` |
| JDK | `jdkList` | `java.util.List` | `com.m3.collections.M3LinkedList` |
| JDK | `jdkDeque` | `java.util.Deque` | `com.m3.collections.M3LinkedList` |
| JDK | `jdkSortedMap` | `java.util.NavigableMap` | `com.m3.collections.M3TreeMap` |
| JDK | `jdkSortedSet` | `java.util.NavigableSet` | `com.m3.collections.M3TreeSet` |
| GUAVA | `guavaListMultimap` | `com.google.common.collect.ListMultimap` | `com.m3.collections.M3LinkedHashMap`, `com.m3.collections.M3LinkedList` |
| GUAVA | `guavaSetMultimap` | `com.google.common.collect.SetMultimap` | `com.m3.collections.M3LinkedHashMap`, `com.m3.collections.M3LinkedHashSet` |
| GUAVA | `guavaSortedSetMultimap` | `com.google.common.collect.SortedSetMultimap` | `com.m3.collections.M3LinkedHashMap`, `com.m3.collections.M3TreeSet` |
| GUAVA | `guavaTable` | `com.google.common.collect.Table` | `com.m3.collections.M3LinkedHashMap` |
| ECLIPSE_COLLECTIONS | `eclipseMap` | `org.eclipse.collections.api.map.MutableMap` | `com.m3.collections.M3LinkedHashMap` |
| ECLIPSE_COLLECTIONS | `eclipseList` | `org.eclipse.collections.api.list.MutableList` | `com.m3.collections.M3LinkedList` |
| ECLIPSE_COLLECTIONS | `eclipseSet` | `org.eclipse.collections.api.set.MutableSet` | `com.m3.collections.M3LinkedHashSet` |
| ECLIPSE_COLLECTIONS | `eclipseSortedMap` | `org.eclipse.collections.api.map.sorted.MutableSortedMap` | `com.m3.collections.M3TreeMap` |
| ECLIPSE_COLLECTIONS | `eclipseSortedSet` | `org.eclipse.collections.api.set.sorted.MutableSortedSet` | `com.m3.collections.M3TreeSet` |
| SPRING_FRAMEWORK | `springMultiValueMap` | `org.springframework.util.MultiValueMap` | `com.m3.collections.M3LinkedHashMap`, `com.m3.collections.M3LinkedList` |
| SPRING_FRAMEWORK | `springSingleValueMap` | `org.springframework.util.MultiValueMap` | `com.m3.collections.M3LinkedHashMap` |
| SPRING_FRAMEWORK | `springAutoPopulatingList` | `java.util.List` | `com.m3.collections.M3LinkedList` |
| SPRING_FRAMEWORK | `springCaseInsensitiveMap` | `java.util.Map` | `com.m3.collections.M3LinkedHashMap` (original payload owner plus folded-key index) |
| APACHE_COMMONS_COLLECTIONS | `apacheListValuedMap` | `org.apache.commons.collections4.ListValuedMap` | `com.m3.collections.M3LinkedHashMap`, `com.m3.collections.M3LinkedList` |
| APACHE_COMMONS_COLLECTIONS | `apacheSetValuedMap` | `org.apache.commons.collections4.SetValuedMap` | `com.m3.collections.M3LinkedHashMap`, `com.m3.collections.M3LinkedHashSet` |
| GUAVA | `guavaFilteredCollection` | `java.util.Collection` | `com.m3.collections.M3LinkedList` |
| GUAVA | `guavaFilteredNavigableSet` | `java.util.NavigableSet` | `com.m3.collections.M3TreeSet` |

`M3LibraryBindings.all()`, `forFamily(...)`, and `search(text, monitor::isCanceled)` expose
immutable metadata without loading the donor runtimes. `verifyRuntime(loader, monitor)`
checks every exact public factory parameter/return type, adapter and M3-owner class name,
then refuses unmapped public factories. It loads types without initialization and does not
invoke factories. It proves naming/availability only, not every inherited behavior or the
internal allocation choices of arbitrary future donor implementations. A missing dependency
is a hard failure, never a silently omitted family.

## Added executable paths

`M3ApacheListValuedMap` and `M3ApacheSetValuedMap` reuse Apache's abstract multimap classes
rather than reimplementing views, mutation, iterator protocols or pair-count semantics.
Their key directory is the existing `M3LinkedHashMap`; their value collections are existing
`M3LinkedList` and `M3LinkedHashSet`. Constructors and the value-collection factory are the
only new behavioral atoms. This does not claim Apache concrete-class serialized identity,
thread safety, random access, or M3 ownership of every donor-allocating return operation.

`M3LibraryCollections.guavaFilteredCollection(M3LinkedList, Predicate)` and
`guavaFilteredNavigableSet(M3TreeSet, Predicate)` borrow existing M3 owners and invoke the
real Guava live-view APIs. Predicates remain live: no cached bitmap silently freezes them.
The sorted wrapper retains the original comparator, ranges, descending view and mutation
policy. Guava filtered-Collection iterator removal remains unsupported; a predicated
insertion decorator or copied filtered list is not substituted for that contract.
The 18 older factory bodies/signatures remain byte-for-byte identical at the parsed-method
level. New methods are additive; there is no new competing collection hierarchy.

```java
var map = M3LibraryCollections.<String, Integer>apacheListValuedMap();
map.get("group").add(1); // donor live view, M3-backed storage
map.get("group").add(1); // duplicates preserved; size() == 2
var family = M3LibraryBindings.forFamily(
    M3LibraryBinding.Family.APACHE_COMMONS_COLLECTIONS);
```

## Arrays, offsets, indexes, masks, and JNI

M3 owners are reused, not relabeled as donor ArrayList/HashMap implementations. Indexed
links and tree topology still have their actual access costs; `M3LinkedList` is not advertised
as an O(1) random-access array list. Reference payloads remain GC-traced Java references.
Prepared mask/index pipelines reuse `M3LongIndexProjection`, `M3IndexMaskCompactor`, and the
existing `m3_index_mask_compactor.c` unchanged. This packet reexecutes actual Java/JNI tests
for sorted slices, stable duplicate occurrences, unsigned state bytes and missing-native
refusal. No Java predicate crosses JNI. A prepared mask is not a live collection predicate.
No new native ABI or duplicated compaction engine is needed for these named adapters.

## Recipe and verification

Canonical task: `m3/collections/recipes/sdk-family-names-20261017`.
Named recipe: `com.synexia.m3.SdkFamilyNames20261017`, extending the existing unchanged
Synexia source-seal kernel and OpenRewrite wrapper. Changes belong in reviewed templates;
qualify the recipe, emit per-file outputs, then verify exact bytes. Never patch generated
outputs. Recipe/tests/evidence and product outputs are reviewed in that order.

```sh
python3 m3/collections/recipes/sdk-family-names-20261017/verify.py
mvn -B -f m3/collections/recipes/sdk-family-names-20261017/pom.xml clean verify
# Requires the existing M3 core artifact and the real pinned donor dependencies:
mvn -B -f m3/collections/interop/pom.xml clean verify
```

The focused local verifier compiles and tests binding/query code, parses all new Java source,
checks old method preservation, executes the unchanged real C/JNI mask core, and exercises
source-seal refusal/fixed-point gates. **Syntax parsing is not donor-resolved compilation.**
The seven new real-donor JUnit tests and original interop tests must run with actual Guava,
Eclipse Collections, Spring and Apache binaries. New dependency coordinate:
`org.apache.commons:commons-collections4:4.5.0`; existing pins are unchanged. They are
compatibility inputs, not latest-version or security endorsements.

See the task evidence for exactly which gates ran. Full SDK API closure, all primitive and
concurrent families, full inherited/default/protected compatibility, installed OpenJDK image,
jtreg/GC/platform, benchmarks and earlier SWT/Nebula/RWT/GWT conversion remain open. A type
mapping or available factory alone never turns those into completed capabilities. Historical
interop delivery receipts remain historical; this is a source-pinned successor, not a replay
of the older absent-only installer against modified outputs.
