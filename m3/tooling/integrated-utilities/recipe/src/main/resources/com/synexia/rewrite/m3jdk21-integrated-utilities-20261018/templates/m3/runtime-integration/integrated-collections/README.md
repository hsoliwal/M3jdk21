<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->
# Integrated Collections Java and JNI

This packet adds thirteen factories to the existing `java.util.Collections` owner.
`java.util.M3CollectionViews` and `M3CollectionMasks` are package-private implementation
owners in `java.base`. No public companion package or Synexia runtime service is needed.
The existing public/protected API and all original Collections implementation bytes
are retained; the compiled preimage comparison and compiler-derived atom seals are
in `public-api.json` and `behavioral-atoms.json`.

## Public contracts

| Factory | Result and ownership |
| --- | --- |
| `filteredCollection`, `filteredList`, `filteredSet` | Live views; predicates reevaluate stored values; accepted writes reach the original owner; removal preserves hidden values. |
| `filteredSortedSet`, `filteredNavigableSet` | Original comparator, actual stored representatives, backed ranges, visible navigation/polling, descending and reversed views. |
| `filteredDeque` | Visible endpoint and occurrence operations, backed accepted insertions and reversed traversal. |
| `filteredMap`, `filteredSortedMap`, `filteredNavigableMap` | Checked write-through entries, visible-only mutation, comparator/identity membership, bounded navigation and sorted/navigable endpoints. |
| `sortedList` | Unmodifiable live projection; each observation sorts a captured array, while an iterator retains its own observation. |
| `filteredSnapshot`, `sortedSnapshot`, `filteredSortedSnapshot` | Detached unmodifiable snapshots with shallow element identity, stable ordering and offset/reverse subviews. |

Predicates must be noninterfering and remain stable during an operation or traversal.
Membership is not cached across operations: `List.set`, mutable element fields and
captured predicate state can change visibility without changing a structural modCount.
These views add no synchronization. Generic filtered iterators deliberately reject
`remove`; explicit view removals are available. Filtered lists retain mutable bidirectional
list iterators, including correct last-returned occurrence after lookahead. Sublist
external structural-change restrictions remain those of the backing list.

Set admission rejects a candidate that collides with a hidden existing representative.
Bulk admission validates the complete captured batch before backing writes. This is not
a transaction over failures from a backing collection. Map identity equality/hashing
recognizes IdentityHashMap and the existing JDK checked, unmodifiable and synchronized
wrappers while retaining the original wrapper for all operations. Successful visible
map reads delegate to the backing getter, preserving access order.

## Arrays, masks and offsets

Snapshot filtering captures input once, evaluates each predicate exactly once, records
visibility in `long[]` words, extracts ascending physical `int[]` positions and retains
only the eligible reference array. No retained per-item storage Node/Pair/Entry is added.
Entry objects are created only for the Map.Entry API projection. Snapshot subranges and
reversal share the owned array with offset/length metadata.

A measured refinement removed an extra permanent permutation and custom indirect
mergesort. A detached snapshot already owns its captured values, so it now reuses the
existing `Arrays.sort` stable object sorter. The rejected implementation and measurements
are retained in the canonical recipe qualification packet. Final finite probes still
show mixed CPU results and extra temporary allocation against a straightforward
ArrayList/removeIf/sort baseline. This is a functional API addition with explicit costs,
not a demonstrated general performance improvement. The live sorted projection costs
O(n log n) work and O(n) temporary storage per observation; it retains no stale sorted cache.

## JNI boundary

`M3CollectionMasks.c` adds `Java_java_util_M3CollectionMasks_selected0` to the existing
libjava source directory. `javac -h` supplies its header. The kernel accepts primitive
mask words and an int-indexed half-open range, validates capacity in 64-bit arithmetic,
uses bounded stack scratch and JNI array regions, checks pending exceptions, and returns
an exact detached index array. It does not retain Java heap pointers or execute predicates
through JNI. The Java implementation is the semantic oracle and the production default.

The internal saved-startup diagnostic `jdk.m3.collectionMasks.native=true` requests the
native symbol. A missing symbol is remembered and falls back to Java. Explicit native
qualification entry points still require the symbol. Measurements do not justify a
native dispatch threshold or changing the default.

## Reproduce receiver evidence

First execute the canonical Maven/OpenRewrite source recipe and receive its exact Results.
Then run the emitted verifier from the repository root:

```sh
python3 m3/runtime-integration/integrated-collections/verify.py \
  --repository . --java-home /path/to/jdk21 \
  --temp-directory /path/to/compiler-scratch \
  --native-temp-directory /path/to/executable-scratch \
  --output /path/to/receiver-receipt.json
```

The verifier compiles Java, generates the JNI header, compiles C17 with warnings as errors,
checks the exported symbol, runs the collection corpus in normal and interpreter modes,
and executes raw kernel and public snapshot consumer checks in native-loaded, default-Java
and missing-symbol fallback JVMs with `-Xcheck:jni`. `--large-capacity` adds the isolated
256 MiB fixture covering a mask capacity greater than Integer.MAX_VALUE bits.

Existing JDK collection jtreg tests remain a separate gate. Patch-module evidence does
not constitute a full M3JDK21 image build, tier1 run, cross-platform native qualification
or advancement of the repository's ordered receiving phases. Keep these PRs draft until
those applicable product gates execute. The active ordered phase remains unchanged.

## Recipe and provenance

The canonical source owner is
`synexia-openrewrite-recipes/recipes/m3jdk21-integrated-utilities-20261018` in Synexia;
the target holds an exact pinned proof mirror under `m3/tooling/integrated-utilities/recipe`.
The existing source-seal engine is reused byte-for-byte. Every postimage is emitted by
actual Maven/OpenRewrite execution, admits exact before/after hashes, and must reach a
no-change second execution. Fix the recipe template and regenerate for any subsequent
source correction. The category catalogue records selected/rejected donors and algorithm
choices; `counterexamples.json` identifies the permanent executable regressions.

Product Java and JNI preserve the target GPLv2 plus Classpath exception; tests retain
their target test license. Authoring tooling and first-party metadata use Apache-2.0.
Donor concepts do not relicense donor source, and challenge statements/submissions or
Apache donor implementations have not been pasted into GPL runtime files.
