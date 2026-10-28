<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3JDK21 original-owner superset convergence — 21 October 2026

**M3JDK21 is one product integrating the JDK/HotSpot, M3File/M3AST,
Java/JNI arrays and collections, native/delta build state and MOP recipes.
It is NOT an M3 wrapper layered on an unmodified JDK.**

This branch starts at original **collections/JNI PR #1619** with the
source-launcher parent #1618 and preserves its original Git history and
source modes. Four additional Git merge parents are preserved:

| Source | Original product ownership and qualification |
| --- | --- |
| #1613 (includes #1590) | original M3FileTree, DOOP indexed filesystem, filtered traversal, bounded parallel file/directory/drive work, shadows and original SubProgressMonitor defaults |
| #1615 | append-only MOP correction/guard memory for Hitesh Soliwal / Synexia Atomize → Patternize → Annotate → Document |
| #1597 | one original javac parse → canonical primitive M3ASTIndex JAVAC facts, line/spans/names and sealed syntax identity |
| #1614 | dynamic Object[] removal bit masks and stable physical-index sorting |
| #1619 (base, includes #1618) | java.base filtered/sorted wrappers and JNI mask-window owner |

The exact provenance of **193 non-overlapping Git source paths** is in
`DONOR_BLOBS.tsv`. This includes the 55 inherited #1619 paths; the four
incoming parents contribute 138 more. Files, paths, source modes and
commits must match their real donor revisions. The read-only
`verify_superset.py` proves all five commits are in ancestry, that 192
donor file bytes and modes are unchanged, and that the one reconciled
product file contains the certified combined output.

## The one required three-way product reconciliation

Current master already owns `M3ArrayList.trimToSize(boolean)`. This
overload optionally invalidates existing iterators even when the physical
dynamic array is already at capacity. Replacing the file from #1614
wholesale would silently remove that public method. The exact
reconciliation takes:

1. Original master Java header and exact `trimToSize()` /
   `trimToSize(boolean)` behavioral atom, **unaltered**.
2. Source-qualified #1614 lazy `long[]` removal mask, first-rejection-
   relative physical offsets, `markRemoval`, and stable
   `compactRemovals` atoms, **unaltered**.
3. Otherwise the unchanged #1614 source, including constructor, sort,
   serialization, callback-order and generics contracts.

This produces one `M3ArrayList.java` with no second `Object[]`
payload, no new API other than the already present master overload,
no additional JNI symbol, no comparator running in C, and no
per-entry wrappers. Independent `M3ArrayListSupersetContract` and
JUnit `M3ArrayListSupersetTest` cover both epochs, 64-bit mask
boundaries, stable order/identity, nulls, no-op removal, callback
reentrancy and non-structural callback writes.

### Three exact Maven/OpenRewrite passes (recipe before application)

`m3/tooling/shared-frontend/recipe-custody/datasets/
m3-superset-array-20261021` contains a canonical named, class-backed
`M3HashPinnedJavaSnapshotRecipe` three-pass composition, retaining
original master preimage and exact postimages. Every file has one
semantic atom and independent SHA-256 pre/post seal, with candidate
donor alternatives recorded in `CATEGORY_CATALOG.tsv`.
A failed gate is corrected in the original recipe, then replayed
serially to a fixed point, **never** hand-patching generated output.

## Deliberately not duplicated

- The original #1590/#1613 M3FileTree already has
  `M3FileShadowInputStream`/`M3FileShadowOutputStream`,
  `M3FileProgress` default split, `M3FileChangeProvider` and
  per-file SHA/token/structure shadows. Standalone #1604 proposes
  another independently owned stream/ring abstraction: **do not**
  install that duplicate here. First forward-qualify its missing
  contracts against the existing M3File owner.
- Original M3AST/javac and MIndexAST source identities must remain
  authoritative. Independent Maven/Gradle/JDT/NetBeans processes do
  not magically share live AST objects. Only source generations and
  detached, semantically validated facts can cross process boundaries.
- SQLite/M3FS, Java HotSpot/JNI, M3String composite DAG, immutable
  String vocabulary and JVM ABI remain existing product owners.
  They are not replaced by a new controller, parser or file store.
- Conflicting older source variants from #1577 and #1617 are NOT
  silently selected. Their extra behavioral atoms need independently
  source-sealed forward recipes; this branch does not claim they
  have all been qualified.

## Review and verification

```sh
python3 m3/qualification/unified-superset-20261021/verify_superset.py
mvn -B -ntp -f m3/collections/pom.xml \
  -Dtest=M3ArrayListSupersetTest,M3ArrayRemovalTest,M3MaskedSortTest test
mvn -B -ntp -f m3/file/pom.xml -Dtest=M3FileParallelTest test
bash m3/mop/verify.sh

root="$PWD/m3/tooling/shared-frontend/recipe-custody"
mvn -B -ntp -f "$root/pom.xml" -Pcontract \
  -Dm3.dataset=m3-superset-array-20261021 \
  "-Drecipe.owner.root=$root/sdk-owner" verify
python3 "$root/receive.py" --dataset m3-superset-array-20261021 \
  --recipe-owner "$root/sdk-owner" --target "$PWD" --check
```

**DRAFT and HOLD**: source/Git custody is not JIT/runtime proof. Real
JDK21 source builds, javac/JDT/NetBeans source-identity contracts,
full Maven/Gradle plugin lifecycle, complete JNI/C11 ABI gates,
source/class delta persistence, all Java 21 and platform modes,
measured retained heap and throughput, and all donor-license gates
remain independent acceptance criteria. CI status must be observed,
not inferred from a workflow file or source hash.
