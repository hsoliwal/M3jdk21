<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 collection interoperability: five-provider array/count/flag extension

This optional Java 21 artifact exposes selected JDK, Guava, Eclipse Collections,
Spring and Apache Commons Collections contracts over M3 collection owners.
The additive extension retains all 22 factory signatures and 11 collectors from
the frozen base. It adds seven provider families, strengthens the two existing
Apache multivalue families, and adds shared count-owner overloads and nine collectors.
M3LibraryCollections now has 33 public factory signatures; M3LibraryCollectors has 20.
The exact supported interfaces and limits are listed in `five-provider-coverage.tsv`.

## Shared storage and flags

`M3ObjectCounts<E>` is the new core owner. An existing key-only
`M3LinkedHashMap` holds the element references, hash directory and primitive slot
links. Primitive `int[]` count and flag columns use the same stable slots. An
exact `long` total avoids iterating occurrences to compute cardinality. Deleting
an element clears its count, flags and key reference before its slot is reused.
No count box, bucket node or entry object is retained for each distinct element.
API entry projections and iterators can still allocate.

The same owner can back Guava Multiset, Apache MultiSet, legacy Apache Bag and
Eclipse MutableBag facades. A mutation through any facade is immediately visible
through the others. Provider interfaces keep their own method returns, equality,
bulk-operation and overflow rules. Sharing storage does not make those interfaces
interchangeable, concurrent or serializable.

```java
import com.m3.collections.M3IndexPathMasks;
import com.m3.collections.M3ObjectCounts;
import com.m3.collections.interop.M3LibraryCollections;

var owner = new M3ObjectCounts<String>();
var guava = M3LibraryCollections.guavaMultiset(owner);
var apache = M3LibraryCollections.apacheMultiSet(owner);

guava.add("ready", 3);                 // previous count: 0
owner.setFlags("ready", 0b0101);       // metadata on the existing element
apache.add("ready", 2);               // previous count: 3; both facades now see 5
var selected = owner.flaggedElements(
        0b0001, 0b0010, M3IndexPathMasks.Backend.JAVA);
// selected is an unmodifiable, detached list containing "ready"
```

All 32 flag bits are usable. A flag update requires an existing counted element;
flags are not encoded in payload references. The window overload supports bounded
positive or negative strides and progress/cancellation callbacks. It first
captures encounter-order physical slots, then dispatches to the existing
`M3IndexPathMasks` Java or JNI implementation. A forced NATIVE request fails if
the native library is unavailable. Native calls receive primitive arrays and
never retain Java-heap addresses. Callback-driven mutations are detected.
Selection costs O(distinct elements) snapshot metadata plus O(selected elements)
result references; it does not promise constant-time arbitrary filtering.

## Provider contracts

The existing SDK bindings, Apache predicated factories and Guava filtered
collection/navigable-set factories from the frozen base are also retained.
This task upgrades the optional Apache dependency from 4.5.0 to the reviewed
4.6.0 release and runs the complete retained adapter suite.

| Provider | Added contracts | Observable rules |
|---|---|---|
| Guava 33.4.8-jre | Multiset, BiMap | Multiset returns previous counts and rejects per-key overflow. BiMap rejects duplicate values; forcePut displaces the previous key. |
| Apache Commons Collections 4.6.0 | ListValuedMap, SetValuedMap, BidiMap, MultiSet, Bag | Multivalue maps preserve Apache views. BidiMap put displaces duplicate values. MultiSet saturates per-key counts. Legacy Bag duplicate-add returns false and remove(element) removes all copies. |
| Eclipse Collections 11.1.0 | MutableBag, MutableOrderedMap | Rich bag APIs use the public abstract hierarchy over M3 counts. OrderedMapAdapter uses an M3 insertion-ordered map. |
| JDK | Existing six contracts retained | Map, Set, List, Deque, NavigableMap and NavigableSet keep the existing M3 owners. M3LinkedList positional access is O(n). |
| Spring 6.2.0 | Existing three contracts retained | MultiValueMap, the single-value bridge and AutoPopulatingList preserve the previously tested behavior. |

Guava and Apache collection `size()` saturate at Integer.MAX_VALUE while the
owner retains the exact long total. Eclipse's new bag facade also uses a
nonnegative, bounded per-key count domain: it rejects count overflow instead of
reproducing the pinned HashBag implementation's integer wraparound. This is an
explicit new-facade limit, not concrete HashBag identity or overflow parity.
Its iterator removes one occurrence correctly; the pinned HashBag counterexample
that loses the distinct entry on the last repeated iterator position is retained
as a regression fixture, rather than reproduced as storage corruption.

The Guava bimap's new `replaceAll` implementation validates an M3 candidate before
committing the changed mappings. Duplicate results or callback failure preserve
the old mappings unless the callback itself mutates the map. Successful results
follow the provider interface. This strengthens an unspecified failure state;
it is not a claim that every donor failure leaves identical partial state.

## Stream result ownership

```java
import com.m3.collections.interop.M3LibraryCollectors;
import java.util.function.Function;
import java.util.stream.Stream;

var counts = Stream.of("red", "blue", "red").parallel()
        .collect(M3LibraryCollectors.toGuavaMultiset());
var byInitial = Stream.of("red", "rose", "blue").parallel().collect(
        M3LibraryCollectors.toApacheListValuedMap(
                word -> word.substring(0, 1), Function.identity()));
```

All 20 collectors create independent M3-backed accumulation owners and merge
them in encounter order. They declare neither CONCURRENT nor UNORDERED.
Map collectors reject null incoming values; counted and multivalue collectors
retain supported null payloads. Both bimap collectors reject duplicate keys or
values during collection, including conflicts between parallel partitions. This
keeps the collector's composition deterministic for valid input. Subsequent
Apache BidiMap put calls keep the ordinary Apache displacement behavior.

Provider methods that produce new immutable, primitive, sorted or grouped
collections can use donor storage. The explicit M3 collectors close only the
listed result families. Concrete donor classes and serialized formats are not
replaced.

## Recipe custody and verification

The canonical task is
`com.synexia.m3.FiveProviderArrayInterop20261017` in
`hsoliwal/com.synexia`, under
`synexia-openrewrite-recipes/recipes/m3-five-provider-array-interop-20261017`.
The target carries an exact, attributed proof mirror in
`recipe/five-provider-array-interop-20261017`; `CANONICAL_SOURCE.json` pins its
canonical commit and engine identities. Changes are authored in the canonical
templates and applied through the existing Java/text OpenRewrite snapshot
engines. Java targets require actual Java compilation units, exact preimages,
fixed-point replay and drift/missing/duplicate refusal. The receiver preflights
the complete result set and validates the actual SDK reports before writes.

From the repository root, with Java 21 and Maven:

```sh
TASK="$PWD/m3/collections/interop/recipe/five-provider-array-interop-20261017"
mvn -B -ntp -f "$TASK/pom.xml" \
  -Drecipe.owner.root="$TASK/sdk-owner" test
python3 "$TASK/receive.py" --crate "$TASK" --target "$PWD" --check
mvn -B -ntp -f m3/collections/pom.xml install
mvn -B -ntp -f m3/collections/interop/pom.xml verify
```

The core's repository-specific JUnit coordinate requires the existing approved
artifact mirror. The workflow also supports the complete core/native donation
gate using the SHA-256-pinned JUnit console. Exact commands, compiler/dependency
identities, all test reports and any retained failed trials are recorded in
`qualification/five-provider-array-interop-20261017`.

The historical core source manifests remain immutable. A separately sealed
master reconciliation accounts for source already present at target base
`f3c615693c41bac1bf6ff49ba53e83f85425bca0`; a six-file successor then admits
the new count owner/test, two corrected test fixtures, and explicit public
constructors for M3ObjectSlotStore28 and M3SpanIndex. Those constructor additions
retain the implicit constructors’ existing signatures and fix strict compilation. The constructor repair
from PR #1107 is already present on the frozen base; its later state-slot APIs
are retained without replacing that owner.
The four baseline failing assertions were checked against JDK behavior and
corrected without altering the working collection algorithms.

## Retained history and limits

The original four-provider delivery remains documented in `donor-origin.json`,
`recipe-receipt.tsv` and the earlier qualification records. Its six recipe tests
and 42 runtime tests describe that earlier source revision. Those receipts are
historical evidence; the new successor proof binds this expanded source set.

Spring's `springSingleValueMap()` retains the Spring 6.2.0 null-equality anomaly:
get(key) returns null for a null scalar while entrySet exposes a singleton-null
list. Its separate map-of-lists factory has a different contract. Caller-supplied
value lists retain their identity and aliasing.

This is a bounded collection-family extension. Complete Eclipse primitive APIs,
ranges, concurrency/reference/serialization families, other Spring and Apache
utilities, full intrinsic result-owner closure, an installed M3JDK image and
repository-wide offline dependency closure remain unqualified. Pinned versions
are compatibility inputs. Recorded cost probes are diagnostic measurements, not
a universal speedup guarantee. Original OpenJDK, M3 and donor licenses remain
unchanged; exact provider sources and notices are bound in `five-provider-origin.json`.
