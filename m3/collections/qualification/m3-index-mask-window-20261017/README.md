<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# M3JDK21 ordered mask-window receiver (2026-10-17)

**Status:** additive Java 21 and JNI **candidate** in the `m3/collections` module; not a
replacement for `java.util`, the existing sorted wrapper families, or a shipped JDK image.
The source of truth for general-purpose convergence and transformation recipes remains
`hsoliwal/com.synexia`. This receiver uses the target's existing Synexia-origin Maven/
OpenRewrite `M3HashPinnedJavaSnapshotRecipe` with a new pinned target crate. It does not
introduce a second rewrite engine, tree, predicate compiler, native buffer manager, or
collection hierarchy.

## Contract, operations and scope

The baseline is `hsoliwal/M3jdk21@a918e588dd11d5ff7e0a6d4efaa89bb1679a9798`.
`M3IndexMaskCompactor` is the canonical existing pure Java/native mask owner.
`M3IndexMaskWindow` is an **optional bounded primitive index projection** for an owner
whose rows are already sorted (or intentionally sequenced). It never sorts, boxes,
creates a `Map.Entry`, or constructs deferred object facades.

```java
// Owner holds a stable index snapshot in its comparator-defined order.
int[] orderedIds = {9, 3, 3, 0};
byte[] flags = {2, 3, 0, 2};
int[] buffer = new int[4];
int first = 0;
while (first < orderedIds.length) {
    long result = M3IndexMaskWindow.scan(orderedIds, flags, first, 2, 0,
            buffer, 0, 2, M3IndexMaskCompactor.Backend.JAVA);
    for (int i = 0; i < M3IndexMaskWindow.written(result); i++) {
        useRowId(buffer[i]);
    }
    first = M3IndexMaskWindow.nextIndex(result);
}
```

The required-mask subset and excluded-mask disjointness are evaluated on **unsigned byte**
flags. Both masks can overlap: that yields an empty selection. Source values, duplicates,
and order are preserved. Nonmatching entries still advance the source cursor. Zero limit
returns the input cursor without reading the arrays. Caller-provided output bounds,
required masks, source/state lengths, source cursor and source/output aliasing are checked
before writes. The caller must not mutate either source during a scan, and must revalidate
parent generation and sorting ownership between scans.

**Output is not a live filtered Collection**: it is a caller-owned detached page of IDs.
Mutable live view/read-only sorted wrapper contracts remain with their existing owners.
The new method does not mutate or retype existing public JDK/M3 interfaces, and it does not
cross the Java object identity/GC/JNI ownership boundary.

## Algorithm/category decision

The sibling [`ALGORITHM_CATALOGUE.tsv`](ALGORITHM_CATALOGUE.tsv) is tab-delimited and
searchable by problem category. It records first-party owners, public donor candidates,
license disposition, complexity, mismatch and chosen action. Public GitHub library revisions
are pinned for **comparison**, not copied. LeetCode, HackerRank and GeeksforGeeks are
problem-shape references only; challenge/editorial code was not imported. No third-party
runtime dependency was introduced.

The chosen Java kernel is **O(visited)** with constant scratch state. JNI uses two
128-element stack arrays plus a 128-element selected buffer; it creates no global JNI
references, caches no Java-array address, and does not call Java predicates or comparators.
Caller-owned output is at most `MAX_WINDOW=4096`. The single optional native addition is a
new entry point in the **existing** `libm3_collections_mask` build. Older libraries are not
silently treated as equivalent under `NATIVE_REQUIRED`.

The full-array `M3IndexMaskCompactor.select()` remains preferable for a detached result
whose cardinality is known to fit in memory. Sorted navigable views belong in the
pending wrapper PR family; a primitive index mask is not interchangeable with `NavigableSet`.

## Deterministic recipe and proof gates

1. Inventory source owner/API and donor licence/contract; consult the TSV before a new atom.
2. Edit **checked-in recipe templates**, never the generated runtime postimages directly.
   `M3HashPinnedJavaSnapshotRecipe` owns Java generation from exact SHA-256 templates;
   `native-manifest.tsv` owns only the companion C source.
3. `python3 m3/collections/qualification/m3-index-mask-window-20261017/verify.py`
   checks all targets, replays onto an empty isolated workspace, proves second-pass fixed
   point, and refuses modified postimages before any file write. An opt-in `--apply`
   generates only absent targets; it never replaces an existing differing source.
4. Run the focused Java/JUnit gate:
   `mvn -f m3/collections/pom-m3-index-mask-window-20261017.xml test`.
5. Run the **actual** Maven/OpenRewrite SDK gate (requires pinned dependencies installed):
   `mvn -f m3/collections/qualification/m3-index-mask-window-20261017/pom.xml test`.
6. Compile strict Java 21 sources and JNI C with `-Wall -Wextra -Werror -Wconversion
   -Wshadow -pedantic` plus optional UndefinedBehaviorSanitizer. Run the same randomized
   standalone oracle under Java-only and native-loaded JVMs. JNI must be loaded explicitly,
   not silently substituted for Java.
7. Qualify whole module/reactor, integrated library distribution, installed JDK image,
   API/JNI ABI, SWT contracts if used in UI, and performance **separately**.

**Current local qualification:** strict Java 21 and C11 compilation, 15,298 Java checks,
30,596 native-loaded checks, and a second native pass with UBSan passed against a
signature-compatible local baseline shim. No full Maven/OpenRewrite SDK build, entire module,
OpenJDK image build, jtreg, SWT screenshot, or measured speedup is claimed by those runs.
Hosted CI remains a required independent gate.

## Licensing and source custody

New first-party receiver text: Apache-2.0, copyright 2026 Hitesh Soliwal and contributors.
OpenJDK `java.base` files are untouched and retain their licensing; no Eclipse, Guava,
Apache Commons, or competitive-programming code is incorporated. The native C implementation
is original and uses the existing primitive Region API pattern in the M3 target.
