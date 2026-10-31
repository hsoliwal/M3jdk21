<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Real M3 collection implementations across five provider libraries

Canonical reusable source, recipes, fixtures and provenance:
[Synexia #12223](https://github.com/hsoliwal/com.synexia/pull/12223),
recipe `com.synexia.m3.FiveProviderProjectionClosure20261018`,
exact source `master@0efc1719ca5766aead09f4f7c49f5067975bd946`
and hash-pinned Java-LST postimage. The public product is M3JDK21.
Source change is strictly one existing Java implementation owner,
`M3FiveLibraryIndexViews.java`; the other source is one new JUnit.

## Array/index/mask storage invariant

The original descriptor owns one immutable captured set of
`Object[] keys, Object[] values, byte[] state, int[] occurrences` and
two ordinal offsets. It retains exact identity/multiplicity/encounter
order through slices, stable index-only sort and 32-bit multi-stage
`M3IndexPathMasks` Java/C17 JNI selection. No new storage hierarchy,
extra JNI ABI, native callback or per-mapping resident Entry/Node.

Thirteen additive real-provider methods reuse the **existing**
M3 collection backends:

| Provider | Additive typed outputs | Original M3 backend |
| --- | --- | --- |
| JDK 21 | Deque, Set, read-only filtered NavigableSet | M3LinkedList, M3LinkedHashSet, M3TreeSet + M3FilteredViews |
| Guava | Multiset, BiMap, Table, mutable filtered Collection | M3CountedBag, M3GuavaBiMap, nested M3LinkedHashMap, M3LinkedList |
| Eclipse Collections | MutableBag, MutableBagMultimap, MutableSortedMap | M3EclipseBag, M3EclipseBagMultimap, M3TreeMap adapter |
| Spring Framework | last-wins single-value MultiValueMap | existing M3LinkedHashMap adapter |
| Apache Commons Collections | MultiSet, BidiMap | M3ApacheMultiSet/M3ApacheBidiMap, primitive M3 links |

Those contracts are intentionally **different**. Guava rejects conflicting
BiMap values, while Apache BidiMap can move the value to a new key;
Spring counts distinct keys, bags count occurrences, and the two
filtered views have different mutation/admission contracts.
Provider materializations are independently mutable and do not write
back into the immutable captured descriptor. Guava Table row/column
extraction is a Java callback, never JNI.

## Byte-exact receiver

| Product target | Before SHA-256 | After SHA-256 |
| --- | --- | --- |
| `src/main/java/com/m3/collections/interop/M3FiveLibraryIndexViews.java` | `fb8125ef79ffc4788b00571ab447a1ed08eca65a56668725db5b2d799a67e85b` | `33fe97faa1c93fb963be080529fb08112d852033fd82efa37b5eb5f5b0b44d2a` |
| `src/test/java/com/m3/collections/interop/M3FiveLibraryExpandedContractsTest.java` | `ABSENT` | `a4006d9c2d9995ba22687ef40db45ddd62993f11804c65c55bbff7d9c95f7de4` |

The original public methods remain unchanged. The named class-backed
Synexia recipe validates exact before-source identity, Java syntax,
fixed point and drift/occupied-source refusal. Its full source
templates/Git blobs have been transferred identically, never
manually patched in product source. All additional behaviors are
tested against real pinned vendor jars, **not mocks**.

## Acceptance gates and limits

The focused Actions workflow runs: original source SHA checks; strict
Java21 whole-real-M3-core javac and java.base dependency proof; standalone
M3 core behavior oracles; actual pinned official Guava, Eclipse Collections,
Spring and Apache Commons Maven/JUnit real-provider tests; original C17
JNI shared libraries and Java `-Xcheck:jni` parity. Failure logs and XML
reports are kept. The separate canonical Synexia Maven/OpenRewrite JUnit
must also execute green at the exact reviewed source commit.

The original core `m3/collections/pom.xml` currently requests
`junit-jupiter:5.10.2-m3-source-1` unavailable from Maven Central.
A separate source-only compiled **real core JAR** can support provider
qualification, but it does **not** turn the authoritative core Maven/JUnit
reactor into a pass. This limitation remains explicit until its source
dependency is properly provisioned/qualified.

Draft and HOLD for all unexecuted or failing gates. This extends **all
five real provider surfaces** but is not completion of every vendor
class/method, installed java.base/JCK/jtreg, reflection/serialization,
concurrency/JMM, memory/CPU superiority, or the whole M3 stage promotion.
Upstream Guava/Spring/Apache Apache-2.0, Eclipse EPL/EDL and OpenJDK
GPL2 with Classpath Exception remain distinct. Synexia remains
the recipe/donor convergence authority; never rebase develop.
