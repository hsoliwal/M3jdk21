<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# String-key collection consumers and strict wrapper boundaries

This is a reviewed source/category catalogue for frozen M3JDK21 commit
`a4415af418a9b0f21a06cd5801d238577fbb6eb7`. It introduces no consumer runtime
changes. The accompanying String refinement changes tuple-gated equality and
comparison traversal through `M3String.contentEquals` and
`M3StringPrecomputeBridge.compareTo`. Public `String.equals` and `compareTo`
already call those owners. Flat, identity and hash paths remain unchanged.

## Coverage and interpretation

`CONSUMERS.tsv` contains 78 category/owner decisions, with concrete methods,
SHA-256 source pins, source links, eligibility and contract boundaries.
`SCANNED_FILES.tsv` indexes all 654 Java sources under the frozen `java.util`
(including concurrent/stream) and M3 collection source directories.
`DISPATCH_TEXT_INDEX.tsv` records 1569 textual equality/hash/order symbol
locations. Comments and declarations can match; this index is discovery
support, not a type-resolved closed-program call graph. Rows without runtime
eligibility are retained. A dispatch path means an eligible input can reach
the selected atom; it does not establish an end-to-end performance improvement.

### Inherited String paths

HashMap, LinkedHashMap, their sets, ConcurrentHashMap, Hashtable, Properties,
immutable hash forms and weak maps retain their original hash and collision
checks. When they perform value equality on a qualifying admitted String, they
reach the equality refinement through the existing public String contract.
String-comparable collision tree bins can additionally reach compareTo.
Hash equality is never enough to establish String equality.

TreeMap/TreeSet and concurrent skip-list owners use the existing String
compareTo when their ordering is natural. Any supplied Comparator remains
opaque. Natural-order array/list searches, object sorts, heaps and sorted
streams inherit only the operations they actually call. Grouping and distinct
stream consumers inherit the selected backing collection's path. All mapping,
classifier, comparator and predicate callbacks remain in their existing order.

The actual M3 counterparts already own dynamic Object[] payload lanes, int[]
indices/probe positions/encounter or AVL links, and state metadata. M3LinkedHashMap
and M3TreeMap are existing owners, also used by their sets, locked views,
bidirectional key directories and multivalue maps. Their calls to String
operations need no parallel replacement collection. M3TreeMap's lazy value
provider, deferred states, progress and event callbacks are separate boundaries:
key comparison is not permission to load a value.

### Explicit exclusions and future work

IdentityHashMap, M3ObjectIdentityMap/Set and the identity-keyed M3LFUCache must
continue using object identity. Weak owners must preserve referent lifetimes,
reference-queue handling and stored representative objects. Interning,
coordinate-based replacement or retaining extra strong keys is inadmissible.
Case-insensitive/locale/custom comparator semantics cannot be replaced with
natural String order. A content-equal key inserted later must not silently
replace the original key reference exposed by an existing mapping.

M3PatriciaTrie is an explicit M3NotYetReceived scaffold at this baseline, as are
the other generic scaffolds individually listed. Its primitive bit-index/child
lane design is an implementation opportunity, not working key lookup. Trie,
prefix-directory and extra key metadata work requires a separate contract,
licensed donor comparison, source-sealed recipe, provider oracle and resource
measurement. It is not implied by a cursor refinement.

JNI mask compaction consumes primitive state and index coordinates. Generic
String keys, predicates and comparators are not moved into that ABI. The
selected M3LongIndexProjection atom belongs to immutable long index ordering;
its monotone scan is not automatically transferable to mutable generic values.

## Filtered and sorted wrapper review

`WRAPPER_CANDIDATES.tsv` records eight decisions. Existing wrappers already
preserve backing owners, comparator metadata, stored representatives and live
visibility on their supported paths. Three plausible read shortcuts were
rejected with a small runnable counterexample: replacing tailSet/first with
ceiling changes a user owner dispatch and can suppress the original exception;
a live size cache becomes stale after owner mutation; evaluating the predicate
on an alias loses the stored representative. The diagnostic is a bounded
counterexample set, not proof that no other valid optimization can exist.

The existing donor PRs remain useful accumulated knowledge:

- [PR1630](https://github.com/hsoliwal/M3jdk21/pull/1630), pinned at
  `096096458f564c4e84d1cfe5f81a5393d868889d`, proposes exact COW bulk mutation and
  strict endpoint repairs. Those repairs change success/exception/callback
  observations, and add two declared endpoint overrides. They are excluded
  from this strict-equivalence performance recipe.
- [PR1659](https://github.com/hsoliwal/M3jdk21/pull/1659), pinned at
  `12b16ff9dcf5a2ff4cbd99dfc58b616ae5f76cbe`, repairs comparator-equivalent aliases
  through the general Set overload. Its original Apache-2.0 regression proof
  was reused, compiled and run against the exact current baseline. It fails at
  the expected membership assertion. That records a real remaining defect;
  it is not reclassified as equivalent behavior or silently repaired here.
- [PR1619](https://github.com/hsoliwal/M3jdk21/pull/1619), pinned at
  `4316758d7e59b643d433418a7e440ab16ed1fd29`, provides broader historical
  wrapper/JNI evidence. Its own recovery, license and image qualifications
  remain distinct from this newly executed bounded review.

No wrapper production postimage is included. The only selected collection
performance postimage is the separately documented primitive monotone-window
atom. This preserves the user-required serial, contract-preserving boundary.

## Reproduce the finite diagnostics

Compile the unchanged `M3CollectionViews`, `M3FilteredViews` and
`M3SlotSpliterators` sources plus `m3/collections/src/test/java/com/m3/collections/M3WrapperReadBoundaryProof.java`
with the recorded Java 21 command in `diagnostics/compile.txt`. The mixed and
interpreted commands/logs retain the exact observation output. The proof's
three shortcut rejections and known bug are explicit; a passing diagnostic is
not a repaired-wrapper acceptance.

Compile the unmodified donor `M3FilteredComparatorRepresentativeProof.java`
against that same closure and run it. The expected exit is 1 at
`generic Set must retain comparator membership`; its failure log is retained.
The initial diagnostic-only serialization lint correction is also retained:
a trace-list field was marked transient to satisfy strict javac; assertions
and production sources were not changed. The source hashes and command
receipts permit the next catalogue pass to reuse this result.

## Qualified source layout

The diagnostic Java sources are received in the existing test source tree,
`m3/collections/src/test/java/com/m3/collections/`. Their source bytes and donor
identity are unchanged. Six historical plain-text command receipts use the
`.txt` extension supported by the existing recipe text lane. Receipt contents
retain the original authoring commands and paths; catalogue references name the
received files. No Java source is admitted through the plain-text lane.
