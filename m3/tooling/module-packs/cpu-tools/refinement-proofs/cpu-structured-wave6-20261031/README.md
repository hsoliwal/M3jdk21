# M3 CPU wave6 — bounded graph, persistent union, suffix and checked sums

This receiving packet contains only M3JDK21 product Java, executable tests,
coverage and passive handoff metadata. It does not contain OpenRewrite recipes.

- M3ShortestPaths: source-sealed primitive CSR nonnegative Dijkstra, indexed
  decrease-key heap, defensive result paths, checked signed-long overflow.
- M3RollbackUnionFind: reversible union-by-size without path compression,
  primitive parent/size/history arrays, depth snapshots, no entry objects.
- M3SuffixArray: immutable UTF-16 charAt-only M3StringPiece-compatible index,
  compact-alphabet rank doubling, stable radix sorting, Kasai LCP, capped
  exact occurrences by original source offsets.
- M3FenwickLong: checked prefix/range sums and failure-atomic point changes.

Both fixtures pass strict stock Java 21 compilation (`-Xlint:all -Werror`),
normal JVM and `-Xint`: 326,118 + 1,051,384 = **1,377,502** checks
per mode. These are bounded operations, not full graph/search/math families.

Canonical source custody is in hsoliwal/com.synexia under
`m3-cpu-structured-wave6-20261031`; delivery stays draft until the actual
OpenRewrite SDK/JUnit, M3JDK21 image/JMOD+jlink, jtreg, and performance gates
pass. Existing contracts and JNI exports remain unchanged (this pass adds no
native symbols). Maintain each FILE as one serial source-sealed owner operation.
