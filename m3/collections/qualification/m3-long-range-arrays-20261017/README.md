<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3 dynamic parallel-array long range set: canonical receiving receipt

This additive M3JDK21 product receiving is **not** a wholesale replacement
for Guava RangeSet, Apache Commons, Eclipse Collections, Spring, or JDK
collections. It is one independently reviewable implementation of a primitive
long interval-family mechanism using existing array/index/flag design.

Source/recipe authority: Synexia [PR #12159](https://github.com/hsoliwal/com.synexia/pull/12159),
exact source commit `b353a2d74552334e3ff323853af9d1b41be0827c`.
Named Maven/OpenRewrite recipe:
`com.synexia.m3.Jdk21LongRangeArrays20261017`.
Canonical class-backed generator: `M3HashPinnedJavaSnapshotRecipe`.
Pinned target baseline M3JDK21 `master@e913ab89056657eca4009678cfe9a657c020e79b`.
Three absent-before Java source/test files exactly match Synexia's canonical
Git objects and SHA-256 template manifest. No modified original M3 Java files,
no rewritten vendor interfaces and no new native JNI symbols.

M3LongRangeSet implementation: dynamic parallel `long[] starts` and
`long[] ends`, with contiguous sorted non-overlapping, non-adjacent
half-open signed ranges. Add merges and remove splits interval coordinates
while preserving original references and contracts of all other collections.
Binary-search point membership/intersection, exact BigInteger cardinality,
detached snapshot and no retained Range/Entry/Node. Explicit 
`Long.MAX_VALUE` exclusive endpoint limitation: this is not an unbounded
Guava interval range and is not a full `NavigableSet<Long>`.

Existing M3IndexPathMasks Java/C17 JNI selector consumes primitive 32-bit
required/forbidden state flags on index/offset lanes; never sends Java
comparators, callbacks or object addresses into native code. Native-required
mode must refuse if the library is unavailable; Java mode always works.
No benchmark or native speedup is claimed.

Source files (path relative to `m3/collections`) are pinned in
`SOURCE_RECEIPT.tsv`. The algorithm decision ledger in
`../../algorithm-catalog/m3-long-range-array-20261017.tsv` compares the
original problem category against owner implementations and donor contracts,
retaining source licensing / rejection rationale.

**Executed local finite proof:** strict Java 21 `--release 21 -Xlint:all
-Werror`, 20,000 deterministic operations and 411,913 assertions passed
with a **test-only stub of the JNI mask selector**. This proves interval
arithmetic on that finite corpus, *not* actual native integration.

**Mandatory pending gates:** real-owner Java 21 compilation with original
M3IndexPathMasks; exact-head Maven/JUnit; real C17 JNI `-Xcheck:jni`;
Synexia Maven/OpenRewrite refusal/fixed point, installed-JDK/jtreg,
provider API JAR compatibility, heap/CPU comparison and native platforms.
Keep draft/HOLD until gate evidence exists; preserve Synexia develop and
M3 product master without direct edits/rebases.
