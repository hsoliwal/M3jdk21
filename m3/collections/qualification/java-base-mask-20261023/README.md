<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 java.base deletion-mask compaction, 2026-10-23

This is an **in-JDK** refinement, not an adapter. The real owners are
`jdk.internal.mindex.M3CollectionMaskCompactor`, `java.util.ArrayList`,
and `java.util.Vector`; the latter two already call this kernel after
their predicate/modCount checks and retain full ownership of tail clearing.

The candidate adjusts only two behavioral atoms in the existing source:
(1) return immediately for a large no-deletion mask and
(2) suppress redundant self-stores in the scalar compaction prefix.
It retains relative long-word mask bits, offsets, duplicates, stable encounter
order, GC-traced Java references, and the same public API and exceptions.
It does **not** move Java object references into JNI or allocate entry objects.

The canonical tested mechanical source transformation and donor catalogue
are in the Synexia convergence repository at
`synexia-m3-recipe/docs/JDK_ARRAY_MASK_COMPACTOR_20261023.md`
and `jdk-array-mask-compactor-20261023.tsv`; product sources carry no recipe
dependency. The source is sealed by SHA-256 from
`1c3f3d2cf129b0ccc44e001fce9f68a251e964ce2c5f7ac5007990cfdaca3216`
to
`812bbae89c04a8b1e812126b751dcc7801216439acb6a9f581e78174bb662f0c`.

## Focused product test

With a built M3JDK21 test image, run the JDK-native jtreg test:

```sh
jtreg -jdk:/absolute/path/to/m3jdk21/build/linux-x86_64-server-release/jdk \
  test/jdk/java/util/ArrayList/M3CollectionMaskCompactorContract.java
```

A local Java 21 compile and `--patch-module java.base` run of the
corresponding contract test completed `2,000,865` checks, including
lengths 0, 511, 512, 513, 1024, randomized bit patterns and mask-word
boundaries, reference identity/order, exceptions, and untouched tails.
This is a **focused local kernel check**, not a full product build or jtreg receipt.

## Compatibility/provenance

Source files in `java.base` preserve their existing OpenJDK GPLv2
Classpath-exception headers. The jtreg test uses OpenJDK GPLv2 terms.
This documentation is Apache-2.0. No LeetCode/HackerRank/GeeksforGeeks
implementation code has been imported. No measured speedup is claimed.

Open item: full image + jtreg + JNI regression suites and benchmarks are
required before promoting the revised JDK implementation. Existing
filtered/sorted collection wrappers and JNI viewport routines are independent
qualification lanes, not automatically validated by this change.
