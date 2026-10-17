<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3Scale primitive range algebra — serial recipe-first canonical receiving

This is the canonical source/code/test/catalogue handoff for refining the
existing independently-built `M3LongRangeSet` Java/JNI-backed collection
family. Canonical convergence/reusable recipe home is
`hsoliwal/com.synexia`. M3JDK21 remains the product and ABI owner;
this recipe has no Synexia runtime dependency.

Exact **receiver preimage**: M3JDK21 stacked branch PR #1165
`b3ef065b14343fd1aeb3370fa09b0100ead260ab`.
`m3/collections/src/main/java/com/m3/collections/M3LongRangeSet.java`
old Git SHA `5dbc2ec9317c79d801bb4c312f2c0b7ffad6ffbd`,
SHA256 `f865b006888f1d4554b51cf69e7f4d6bac67458982f957f9151af0b4f09eaec8`.
Candidate new Git SHA `03b6d9fad25ce9bdb920ecc7b5e8f975a4638e11`,
SHA256 `17c6db8bf82e5d908a551fe53850a1d4ca00583fa6783bd093af48598038b0c6`.
Two new ABSENT-before Java tests:
`M3LongRangeAlgebraContract.java` SHA256
`da109872c25cf9a9a5af6ee38417c4cf5c7d5fcf7444418e499d03d908ecb6fc`;
`M3LongRangeAlgebraTest.java` SHA256
`fb7de589799041359d6a973496e6f7c87ea87d7bc732e51acebd451c6f2af99f`.

## Sealed behavioral atoms

1. Retain existing half-open signed-long dynamic parallel starts[]/ends[]
   lanes and Java/C17 JNI selector dispatch untouched, including full
   32-bit state masks, source coordinates and preimage mutation rules.
2. Add lazy `long[] cumulative` unsigned prefix-count lane, initialized
   only on rank/select/cardinality and invalidated on actual add/remove/clear.
   One long per range; no retained per-element object or BigInteger graph.
3. Add exact `rank(long)` and `select(BigInteger)`, preserving full
   signed natural order and quantities beyond Long.MAX_VALUE. Reject
   negative/out-of-range ranks before conversion to primitive bit pattern.
4. Add `union`, `intersection`, `difference`, `symmetricDifference`,
   and bounded `complement` as two-pointer compositions of canonical
   sorted intervals. Results are detached; original owners are not mutated.
   Linear time relative to interval counts; no third-party class ABI.
5. No changes to public methods already present, no new JNI symbols,
   no Java callbacks or object pointers in native code. This improves a
   primitive analogue; it **does not** implement Guava RangeSet or a
   universal `java.util` collection replacement.

## Algorithm categories and repeatable evidence

`CATEGORY_REVIEW.tsv` identifies the problem category, current
owner, considered alternative, complexity, contract decision and pinned
donor rights. LeetCode 56, HackerRank Gridland Metro and GFG
Overlapping Intervals informed tests only, not copied solutions.
M3 source and generated recipes remain first-party Apache-2.0.
Upstream Guava/Commons/Spring Apache, Eclipse EPL/EDL and JDK GPLv2/CPE
terms are not changed by catalogue observation.

## Executed and remaining proof

Executed **locally**, selected-source Java 21 strict compiler
`--release 21 -proc:none -Xlint:all -Werror`, existing
`M3LongRangeSetSelfTest` **411,913** oracle assertions, new
`M3LongRangeAlgebraContract` **1,324,090** additional oracle
assertions and 900 seeded BitSet comparisons: PASS at `-Xmx128m`.
The locally compiled `M3IndexPathMasks` was a **test-only
stand-in**, so these counts are NOT a proof of real JDK/Java/C17
integration. Old and new production public methods preserve
their original signatures; only additive public methods are introduced.

Still HOLD: actual canonical Maven/OpenRewrite LST fixed-point/refusal,
real M3 module Maven/JUnit, JNI native parity with -Xcheck:jni, installed
M3JDK21/jtreg/GC/JMM, provider API equivalence, retained heap and CPU
cost comparisons. Keep draft and never rebase `develop`; exact-head
head workflows alone can establish the missing proof.
