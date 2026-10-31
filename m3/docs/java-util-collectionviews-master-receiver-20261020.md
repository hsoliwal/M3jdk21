<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 native java.util filtered/sorted receiver — 20 October 2026

## Product and source ownership
The product is **M3JDK21**, an integrated OpenJDK/HotSpot/JNI source tree, **not M3 plus a JDK**.
The supported receiver in this bounded pass is `src/java.base/share/classes/java/util/CollectionViews.java`.
It lives directly in `java.base` and loads with the bootstrap class loader. The standard
`java.util.Collection`, `Set`, `SortedSet`, `NavigableSet`, `Arrays`, and
`java.lang.Class` contracts remain authoritative; this new `CollectionViews`
class is an M3JDK21 extension, not a Java SE API claim.

Synexia is the canonical recipe and convergence owner. This direct-blob replay
uses previously reviewed **Synexia PR #12015** and **M3JDK21 PR #1038**
(`m3/java-util-views-file-pass-20261017`) without new handwritten
wrapper algorithms. It **does not** replace or delete the newer
`m3/collections` convergence owner on master. That owner now has additional
algorithm refinements; reconcile them contract-by-contract before reducing
`com.m3.collections.M3CollectionViews` to the previously qualified facade.
This is an additive receiving candidate, not an assertion of canonical
single-owner completion or supersession of newer work.

## Exact files received
| Destination | Prior qualified Git blob |
| --- | --- |
| `src/java.base/share/classes/java/util/CollectionViews.java` | `60ee7b46b457167cecb242d8ec14a4e9dd7653cd` |
| `test/jdk/java/util/CollectionViews/BootstrapViewsTest.java` | `381dce4756f42cea485bbca7afa856536145f9b4` |
| `test/jdk/java/util/CollectionViews/CollectionViewsContract.java` | `29fe9aac7ee438e63f5409a50d1ce25929d2790b` |

These are additive **ABSENT -> EXACT DONOR BLOB** admissions on the current
`master` preimage. No public existing class, field, signature, factory
descriptor or native JNI symbol is overwritten by this transfer.

## Contracts and algorithm atoms
1. Live filtered Collection/Set/SortedSet/NavigableSet views retain backing
   owner and range/comparator metadata; no retained Map.Entry/Node object per element.
2. Deep nested predicates are persistent immutable terms, flattened lazily;
   execution is iterative, ordered and short-circuiting rather than recursive.
3. Filtered/sorted snapshots use one dynamic ArrayList, one filter traversal
   and stable existing JDK sorting. Equal-comparator references remain distinct.
4. State masks, dynamic index coordinates and primitive JNI kernels remain the
   existing dedicated M3 Java/JNI owners; no arbitrary Java predicate enters JNI.
5. Fail-fast and exception/visibility behavior must be compared against the
   exact original owner. Constructor, iterator, null and comparator effects
   must not be changed merely for speed.

## Per-file receipt and status
- `CollectionViews.java`: copied earlier generated, reviewed implementation
  with source SHA/Git blob pin; current JDK image test PENDING.
- `BootstrapViewsTest.java`: copied existing zero-extra-exports bootstrap and
  20,000-deep-filter check; current image execution PENDING.
- `CollectionViewsContract.java`: copied original differential suite;
  current JDK image execution PENDING.
- `m3/collections/src/main/java/com/m3/collections/M3CollectionViews.java`:
  CURRENT MASTER UNCHANGED — no destructive restoration of old facade.
- JNI mask sources and java.util Collections/TreeSet/TreeMap implementations:
  UNCHANGED — no claim of native binary or JDK collection replacement.
- `m3/collections/algorithm-catalog/java-base-filtered-sorted-receiver-20261020.tsv`:
  problem-category comparison and explicit acceptance dispositions.

## Receiver gates before merge
Use canonical Synexia Maven/OpenRewrite recipe
`synexia-m3-recipe/recipes/m3-java-util-views-20261017`; pin actual
target preimages, rerun fixed point and refusal tests. Verify the current
M3JDK21 image, not stock JDK + standalone app:
```sh
make images
make test TEST="jtreg:test/jdk/java/util/CollectionViews"
make test TEST="jtreg:test/jdk/jdk/internal/m3"
```
Also qualify current master interactions with array/mask primitives,
newer filtered/sorted wrapper semantics from open #1294 and #1357,
interpreter/C1/C2, GC/serialization/agent/module behavior and independent
CPU/heap/native memory measurements.

Do **not** merge this as complete until existing-first-party Apache-2.0
expression and OpenJDK integration licensing are explicitly admitted.
Do not change OpenJDK GPL-2.0-with-Classpath-exception notices by copying
first-party source. JNI remains separate: the previous #1038 native
`RunSearchJni` was verification-only, not a production JNI accelerator.

The enforced stage order for full-family promotion remains String ->
arrays -> collections -> AST/compiler and other families. This candidate
does not certify closure of the entire collections stage or the full
M3Index/M3String/M3Class migrations.
