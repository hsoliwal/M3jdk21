<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 Native Indexed Table: source-sealed Maven recipe

**This is an original M3-owned collection, not an adapter.** It does not implement or invoke
Guava, Apache Commons, Eclipse Collections, or Spring APIs. It is the first-party
indexed two-dimensional primitive-slot/table family, intended for the separately built
hsoliwal/M3JDK21 module. Synexia remains donor/catalogue/recipe owner.

## Concrete Java 21 product

`M3IndexedTable<R,C,V>` stores one GC-visible Object[] payload lane with
primitive int[] per-row, per-column, and insertion-order next/previous indexes,
byte[] physical state masks, and free lists for physical and axis IDs.
Existing `M3LinkedHashMap` interns distinct row/column keys, while
`M3LongLongHashMap` provides collision-free packed (int,int) coordinates and
slot+1 lookup. No per-cell Entry/Node object or copied payload, and null row,
column, value and duplicate values are valid. Axis IDs and physical slots recycle
only after deletion; one fresh owner revision invalidates stale views.

The `IndexView` is a primitive physical-index snapshot descriptor, not a copied
value store: O(1) offset slices, stable sorted-by-value int[] permutation,
reverse, required/forbidden 8-bit state masks, row/column/global views and
detached index export. Default Java selection allocates exact selected physical
offsets. `NATIVE_REQUIRED` forwards **only detached primitive int[] rows and
byte[] precomputed masks** to existing `M3IndexMaskCompactor` C17 JNI;
native callbacks, Java heap pointers and a second native engine are forbidden.
No public JDK java.util class or vendor API is replaced. Single-threaded only.

## Recipe-first qualification

Canonical independent Maven module:
`synexia-m3-recipe/recipes/native-indexed-table-20261017/pom.xml`.
Source generator `com.synexia.m3.M3NativeIndexedTableRecipe` source-seals
three **ABSENT-before** Java21 target paths, preflights all outputs,
refuses symlink paths, drift, duplicates and missing check targets, and reaches
a fixed point upon repeating the same recipe. The exact human-readable templates
are in `src/main/resources/templates`. Fix the templates and re-run, never edit
the emitted target class/test outputs.

```sh
mvn -B -ntp -f synexia-m3-recipe/recipes/native-indexed-table-20261017/pom.xml verify
java -cp synexia-m3-recipe/recipes/native-indexed-table-20261017/target/classes \
  com.synexia.m3.M3NativeIndexedTableRecipe --apply /path/to/M3JDK21
```

**Actually executed locally:** OpenJDK21 `javac --release 21 -proc:none
-Xlint:all -Werror` and 62,986 seeded Java checks / 6,000 randomized operations,
including 8,000 reused axis IDs and physical-slot cycles; the isolated test used
API-shaped stand-ins for two M3 hash/directory owners and the JNI compactor, so
this is **not** real owner/JNI parity or full module evidence.
Actual source-sealed Maven generator Java selftest (missing/drift/fixed point)
**PASS** using stock JDK21. Maven unavailable locally; actual Maven lifecycle,
whole M3 collection module and `-Xcheck:jni` C17/UBSan parity must run in
the target PR workflow before admission.

Donors/categorization: the first-party M3LongLongHashMap, M3LinkedHashMap,
M3SlotArrays and M3IndexMaskCompactor owners are reused; LeetCode
#27/#283/#912, HackerRank BitSet and GeeksforGeeks are classification
references only, **no challenge solutions or third-party source copied**.
All original first-party Apache-2.0 notices preserved.

Full JDK+Guava+Eclipse+Spring+Apache native M3 superset, installed JDK
replacement, GC/native memory-pressure proof, whole JDK/jtreg/HotSpot and
cross-platform ABI/benchmark gates remain open. This additive source family
does not advance String → arrays → collections → AST/compiler migration.
