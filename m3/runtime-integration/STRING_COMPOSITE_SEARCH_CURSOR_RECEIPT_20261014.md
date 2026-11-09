<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 composite-native String literal search — receiver receipt 2026-10-14

This is the M3JDK21 **product receiver** of canonical Synexia recipe
[com.synexia.m3.Jdk21CompositeSearchCursor](https://github.com/hsoliwal/com.synexia/pull/11247),
stacked after M3JDK21 [tuple stream cursor PR #862](https://github.com/hsoliwal/M3jdk21/pull/862).

Base: `bde4aa6f4f30ecbbc919af95252dab573d546eb3`, which already includes the private tuple-leaf stream cursor.
Target sources are exact SHA-256 postimages stored in the Synexia source-sealed recipe:
```tsv
src/java.base/share/classes/java/lang/M3String.java	88716098c89b30502cb6287629259a577608ababfe8154db6090939cc64f0656	d4dcb3f3320e42a977001254cdd9ac58105cea0a427839ec81d945912476943f	M3String.java
src/java.base/share/classes/java/lang/M3StringSearchPrecompute.java	ce6ae17859adb67fefe5fe127db102f0327a99ed89796a1194d4caaf075c46b4	b0cd3f3199cc14e12c69abc34dfba5fd8c15046b06f1faa703b41d7d8c6a6bc1	M3StringSearchPrecompute.java
test/jdk/java/lang/String/M3StringCompositeLiteralSearchCursorTest.java	ABSENT	2d7ea09b8c499f5aa25c3cd698d52aef7c143fe548d55f2038545b72aa8cb1f3	M3StringCompositeLiteralSearchCursorTest.java
```

## Runtime impact
- Permit only `java.lang` package members to reuse `M3String.CodeUnitCursor`;
  public String APIs, layout and JNI/HotSpot symbols remain untouched.
- Composite UTF-16 haystack and needle traversal through the same immutable
  owner+coordinate tuple cursor inside KMP, BMH and reverse KMP.
- Prepared pattern prefix/reverse-prefix/skip facts use the cursor, retain only
  bounded primitive metadata and never own canonical text.
- No flattened source/needle arrays, second payload owner, eager materialization
  or new native allocations are introduced by these operations.

## Evidence and boundaries
- Independent Java 21 tuple/slice search semantic model: **99,820 checks PASS**
  (`--release 21 -Xlint:all -Werror`, `-Xmx32m`).
- New jtreg-shaped Java String API source: **102,291 checks PASS on stock JDK21**;
  this does NOT establish success on the modified M3JDK runtime.
- Exact canonical recipe template SHA-256 and base preimage identities verified at receiver time.
- Maven/OpenRewrite SDK execution, actual modified JDK21 java.base/HotSpot
  build, jtreg with `-XX:+UseM3StringStorage`, JNI/JVMTI/GC/JIT/CDS,
  full reactor, retained-heap and throughput comparisons: **NOT_RUN**.
- Do not merge while any required product gate remains unqualified.

Canonical ownership stays in Synexia; runtime code and acceptance stay in M3JDK21.
The `UseM3StringStorage` feature remains experimental/default-off.
Regex `Pattern/Matcher` full composite-aware precompute remains **P0 OPEN**.
