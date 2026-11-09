<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->

# M3 PCollections superset

This Java 21 module supplies persistent M3 collections and immutable index views
alongside the complete, unchanged PCollections production surface. It reuses the
existing M3 collection wrappers and optional JNI mask selector. Canonical source
templates, per-file OpenRewrite transformations and algorithm evaluation records
live in `hsoliwal/com.synexia` at
`synexia-openrewrite-recipes/recipes/m3jdk21-pcollections-superset-20261018`.

The three runtime modules are `com.m3.pcollections`, `org.pcollections` and
`com.m3.collections`. None requires a Synexia runtime jar. This is an independent
receiving module; it does not promote collection backends into `java.base`, alter
the active String receiving phase, or prove a complete M3JDK/HotSpot image.

## Persistent owners

| M3 class | Public abstraction | Storage and behavior |
| --- | --- | --- |
| `M3PVector` | `PVector`, Java List | Balanced immutable 32-slot pages, primitive cumulative offsets, path copying, shared slices |
| `M3PStack` | `PStack`, Java List | Same paged owner; top at index zero; push-all reverses encounter order |
| `M3PQueue` | `PQueue`, Java Queue | Same paged owner; append and persistent head deletion; no accumulating queue prefix |
| `M3PMap` | `PMap`, Java Map | Bitmap-ranked hash-trie arrays, packed key/value lanes, full-hash collision comparison |
| `M3PSet` | `PSet`, Java Set | Persistent map membership, original representative on duplicate insertion |
| `M3PBag` | `PBag` | Persistent map of checked counts, total occurrence size |
| `M3PSortedSet` | `PSortedSet`, NavigableSet | Vector key lane, binary search, shared range offsets, descending metadata |
| `M3PSortedMap` | `PSortedMap`, NavigableMap | Aligned key/value vector lanes and immutable on-demand entry projections |
| `M3POrderedMap` | `PMap`, Java Map | Packed vector slots, HAMT key-to-ordinal index, 32-bit liveness masks |
| `M3POrderedSet` | `PSet`, Java Set | First insertion order through the ordered map owner |
| `M3PListView` | Immutable Java List | Source-version identity, primitive index selection, slice offset and reverse flag |
| `M3PCollections` | Static factories | All owners and explicit snapshot/live wrapper entry points |

All owners expose `empty`, `singleton` and `from` factories appropriate to their
interface. Persistent producers are covariant; destructive mutators refuse with
`UnsupportedOperationException`, including no-op inputs and Java 21 end operations.
Mutable payload objects remain ordinary shared Java references: persistence fixes
collection membership/order, not the internal state of a user object.

```java
import com.m3.pcollections.M3PCollections;
import com.m3.pcollections.M3PVector;
import com.m3.collections.M3IndexMaskCompactor.Backend;
import java.util.Comparator;
import java.util.List;

var original = M3PCollections.vector(List.of(7, 2, 5, 2));
var next = original.with(0, 9).plus(11);   // original still contains 7, 2, 5, 2
var selected = M3PCollections.view(next)
        .filter(value -> (value & 1) == 1, Backend.JAVA)
        .sorted(Comparator.naturalOrder())
        .reversed();
M3PVector<Integer> compactSnapshot = selected.toVector();
var sorted = M3PCollections.<Integer, String>sortedMap()
        .plus(3, "three").plus(1, "one");
var range = sorted.headMap(3, false);     // membership snapshot, no lasting fence
var extended = range.plus(5, "five");
```

## Exact contracts and compatibility

The pinned donor is
[`hrldcpr/pcollections@1f2212e7bf935451ea421ca3d499ef3c96c45e5c`](https://github.com/hrldcpr/pcollections/tree/1f2212e7bf935451ea421ca3d499ef3c96c45e5c).
All 34 production Java files (including its module descriptor), all 15 original
Java test files, upstream README/changelog/Gradle build and exact MIT license bytes
are tracked with source Git object IDs and SHA-256. Its ten interfaces, six
unmodifiable bases, eleven concrete types, four factories and two internal tree
classes remain usable without changes. The donor Maven coordinate
`5.0.0-m3-source-1` identifies this receiving package, not an upstream release.

The additive M3 family follows the frozen interface-level contract. A few donor
concrete quirks remain available only on the unchanged donor classes:

| Operation | Additive M3 contract | Preserved donor observation |
| --- | --- | --- |
| Sequence `minusAll` | Removes every matching occurrence, as PCollection specifies | TreePVector and its queue consumer remove one per supplied occurrence |
| Empty sorted-map `firstEntry` / `lastEntry` | Returns null, as NavigableMap specifies | TreePMap throws NoSuchElementException |
| Conditional destructive map methods | Always refuse, including absent-key/no-op calls | Some donor inherited defaults return without throwing on no-op inputs |
| Bag null hash | Null occurrences contribute zero; checked frequency arithmetic | MapPBag allows null insertion but its hashCode path throws for null |

Null values/elements are supported by ordinary hash/sequence/ordered owners.
Natural sorted keys reject null; custom comparators may support it. Comparator
equivalence controls sorted membership and retains the first stored key. Hash-map
replacement retains incoming equal key/value references. Ordered-map replacement
retains the first key representative and updates the supplied value in place in
encounter order; removal followed by insertion appends. Sorted range results are
persistent clipped snapshots whose later additions may lie outside old endpoints.

List slices preserve `[from,to)` semantics. Bad endpoints throw
IndexOutOfBoundsException; `from > to` throws IllegalArgumentException. Visible-only
serialization avoids retaining or serializing hidden vector/view payload. Serial
forms of new M3 classes are independent of the unchanged donor serial forms.

## Offsets, masks and complexity

A vector's `(root,offset,length)` captures one immutable page tree. `get`, `with`,
single insertion and removal traverse logarithmically in the retained backing size.
`subList` shares the tree in constant time, so a short slice can retain a large
source. `M3PVector.empty().plusAll(slice)` and `M3PListView.toVector()` explicitly
materialize only visible values. Unchanged pages remain shared across updates.

The hash trie consumes five hash bits per level, uses bit population counts for
compact lane rank, and always checks Java equality after a hash match. Exact
full-hash collisions use packed collision arrays and linear comparison within that
group. No Map.Entry is retained per mapping. Ordered maps reuse that owner for
boxed ordinal values and keep keys/values in alternating vector slots; liveness is
one integer mask per 32 physical entries. A new deletion clears its payload slots.
When physical entries exceed `max(64, 2 * liveSize)`, deterministic compaction
rebuilds encounter order. Its O(n log n) rebuild cost is reported explicitly.

Sorted lookup performs binary search over vector-backed keys, costing
O(log visible-size * log retained-size). Descending views change metadata; clipped
key/value slices share the same physical range. Stable view sorting merges primitive
indexes with two arrays of scratch/output and retains equal-key occurrence order.
There is no per-row wrapper/boxed-index layer in the filtered/sorted view.

## Java and JNI selection

`M3PListView.filter` calls a predicate once per visible occurrence in encounter
order. It stops at the original thrown exception, retains no callback, and selects
primitive positions through the existing `M3IndexMaskCompactor`. `select` accepts a
state byte per current logical position and an unsigned-byte required bit mask;
it copies the state input and preserves order, duplicates and payload identity.

`Backend.JAVA` is deterministic default behavior. `Backend.NATIVE_REQUIRED` requires
the actual `m3_collections_mask` library, checked before any predicate callback.
There is no silent Java fallback for a required native call. Existing native ABI
capability handling stays in the existing selector. Generic objects, predicates,
comparators, hash/equality calls, and persistence logic remain in Java.

`M3PCollections.liveFiltered` delegates the existing Collection, Set, SortedSet and
NavigableSet wrappers and retains their runtime capabilities, backing mutation,
range and iterator behavior. Use `view` or `copyOf` when an immutable captured
version is needed; use `liveFiltered` when subsequent owner changes must be visible.

## Verification and reproduction

Run the bounded verifier with Java 21, a C compiler, Python 3, and the two exact
proof dependency jars identified in its manifest:

```sh
python3 m3/collections/pcollections/tools/build.py \
  --jdk-home /path/to/jdk-21 \
  --junit-console /path/to/junit-platform-console-standalone-1.12.2.jar \
  --assertj /path/to/assertj-core-3.27.7.jar \
  --out /path/to/pcollections-proof --stage all
```

The script enforces `lint -> compile -> test -> runtime` against source, toolchain,
dependency and output fingerprints. Changed inputs invalidate previous gates.
It compiles every donor and M3 source, generates JNI headers, builds the actual C
library, compares native exports, runs the untouched donor tests and independent
M3 fixtures, checks native absence and Java/native parity with `-Xcheck:jni`, then
uses the existing generic JMOD packager to build and exercise a linked image.
All test/probe compilation precedes execution. Failed generations and logs remain
available; resource measurements are descriptive observations, not benchmark claims.

The bounded Maven reactor is
`m3/collections/pcollections/reactor/pom.xml`. It contains the donor adapter,
existing M3 collection owner and this module. The normal root reactor and existing
POMs are unchanged. Its JUnit bridge invokes the same four M3 contract fixtures.
The preserved donor tests run with explicitly recorded compatible proof dependencies
(JUnit Platform 1.12.2 and AssertJ 3.27.7); the original Gradle configuration remains
available verbatim and is not claimed to have been executed by this adapter.

The canonical recipe crate records algorithm discovery by problem category,
candidate/license decisions, file mappings, unchanged before/after contracts,
exact source hashes, recipe refusal cases and fixed-point replay. Receiving receipts
record actual gate results. Full root-reactor, jtreg, HotSpot and cross-platform
receiving gates must be reported separately; standalone module/image success does
not imply those gates passed.
