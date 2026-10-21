<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original `java.util.CollectionViews` stored-occurrence recovery

Integrated M3JDK21 java.base owner, **not** a new M3 wrapper or a second
payload hierarchy. Reuse canonical Synexia source-sealed Maven/OpenRewrite
recipe [#12866](https://github.com/hsoliwal/com.synexia/pull/12866),
commit `9cdce883109e8d302ea2d6a51da01056a48e4702`.
The generated owner bytes here are the exact canonical recipe postimage.
Original first-party donor: M3JDK21 #1116 / Synexia #12099.

The prior `FilteredCollection.contains` evaluated a predicate on an equal
caller probe rather than the stored element. `remove` could delete a hidden
equal element in a `List` or reject a valid `CopyOnWriteArrayList` removal.
This bounded fix restores stored-representative membership, physical-index
list deletion, identity-set equality and conservative generic iteration.
Existing SortedSet/NavigableSet comparator/range contracts and Spliterator
semantics are retained. Dynamic array masks and JNI symbols are untouched.

```sh
bash m3/runtime-integration/stored-occurrence-20261021/verify.sh \
  /absolute/path/to/nonexisting-evidence-directory
```

The verifier seals the complete old and generated Java source, compiles
`java.base` patch module old/new, compares public `javap` descriptors,
reproduces the old oracle failure, then runs the generated first-party
736018-check oracle across default/-Xint/C1/C2 plus existing bootstrap and
collection-view contract tests. This is **patch-module**, not a complete JDK
image/jtreg/JCK or JNI/GC run. No unsupported speedup claim: selected
membership now requires O(n) scanning of original owner occurrences.
Java21 host and native image, resource benchmarks, platform and licensing
remain independent admission gates.
