<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21: direct filtered map entry/key/value projection receiver

Status: **draft, source-sealed candidate only**. Receiver for Synexia
[#11929](https://github.com/hsoliwal/com.synexia/pull/11929), stacked on
M3JDK21 [#985](https://github.com/hsoliwal/M3jdk21/pull/985), base revision
`e81a42da2158801a9358b5371e22563c24f4853f`.

The standalone Apache-2.0 `m3/collections` library already provides the
`M3FilteredMaps` and `M3CollectionViews` public APIs. Only the former's
private mapped-spliterator execution atom changes. No new `java.base`,
`java.util`, `java.lang`, JNI production ABI, HotSpot/JVM integration,
collection owner, or Synexia runtime dependency is introduced. JDK public
contracts remain unchanged, including comparator, null, range, backing
mutations, hidden keys, guarded entries and stable sorted snapshots.

## Recipe authority

The canonical reusable behavior belongs in Synexia. This receiver does not
create an independently evolving refactor engine: reuse unchanged retained
`com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe` from existing
`m3/collections/wrappers/recipe/pom.xml`; target-specific named
`com.synexia.m3jdk.FilteredMapDirectProjectionReceiver20261017`
binds three exact Java targets. Source preimage, target postimage, new
Java contract and JNI proof source templates are committed ahead of product
installation. Fix and rerun the reusable source recipe on any defect, not
the generated target. Source preimage SHA-256:
`1dff497079e768fda574a349cc521732d69cbacbecc19deba6888d909844f1cb`.
Target postimage SHA-256:
`39e3698a6a9e1c494050f04bd9dbd13134dfbb748a075fa6a0535d3325c3dd9d`.

JNI proof C is a test-only bounded `GetIntArrayRegion` SHA/FNV-like primitive
shadow operation, with 256-int temporary blocks, fatal UBSan and `-Xcheck:jni`;
it does **not** call Java predicates or retain native pointers across calls.

## Semantic and algorithm rationale

Previous private helper:
`StreamSupport.stream(source, true).map(map).spliterator()`.
New private helper: one O(1)-state mapped cursor per active traversal or
split, lazy O(n) total mapped consumption, no entry/payload array.
Preserves JDK Stream.map characteristics `ORDERED|SIZED|SUBSIZED`;
deliberately drops `SORTED|DISTINCT|NONNULL|CONCURRENT|IMMUTABLE`.
`getComparator()` fails on the transformed map projection. The existing
specialized `NavigableMap.keySet()` still returns a sorted/distinct
`M3CollectionViews` view with authoritative comparator; it is **not** a
generic projected entry cursor.

Compare JDK/Guava/Eclipse/Apache/Spring contract oracles, see
`CATEGORY_REVIEW.tsv`. LeetCode, HackerRank and GeeksForGeeks supply
problem categories and hostile test ideas, not copied implementation bodies.

## Gates

1. Verify all pinned original and generated SHA256, exact installed template
   identity, target branch lineage and original file preserved.
2. Use actual Maven/OpenRewrite Java recipe engine to generate the three
   outputs, verify fixed point, refuse drift/occupied/missing and non-Java
   input; retained receipt and test accompany source.
3. Compile the actual `m3/collections` runtime pair, test entry/key/value
   projection and sorted/NavigableSet semantics under Java 21.
4. Check generated JNI header, strict C17 native proof, fatal UBSan and
   `-Xcheck:jni`; no new product JNI symbols.
5. Run entire owning `m3/collections` module/reactor and #985 wrapper
   corpus, all-provider ABI/reference/concurrency/serialization and JMH
   before describing the isolated library as admitted.
6. Run source-built OpenJDK/jtreg/JCK and phased String→arrays→collections
   acceptance separately before any JDK runtime promotion.

No new direct GPL/OpenJDK code copying. OpenJDK sources retain original
licenses and this standalone layer remains Apache-2.0.
