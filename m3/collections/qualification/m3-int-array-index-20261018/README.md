<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Direct M3 primitive-int array list and index-permutation snapshot

Canonical Synexia source/recipe owner; M3JDK21 target runtime owner.
Based on frozen M3JDK21 2026-10-18 master census: M3IntCollection and M3IntIterator
were installed, but M3IntList and M3IntArrayList were not. No full replacement
of java.util, Guava, Eclipse Collections, Spring or Apache interfaces is asserted.

Recipe-first source admission: source identity inventory → safe additive owner
placement → category/donor comparison → Inline → Atomize → Patternize → Comment
→ strict Java21/differential/JNI gates → reviewable PR.

## Four exact generated targets
- M3IntList extends existing M3IntCollection with indexed int[] operations.
- M3IntArrayList owns a single dynamic int[], logical size, fail-fast epochs,
  indexed insert/delete, sort/binary search, stable two-pointer selection,
  sorted dedup, bounded capacity and exception-safe predicate suffix handling.
- M3IntIndexedSnapshot owns detached primitive int[] payload plus int[] logical
  index permutation; immutable slices share array offsets, stable indirect
  ascending/descending sorting permutes index IDs only, and rank/select never
  boxes values. Its optional mask-admission path calls the EXISTING
  M3IndexMaskCompactor.selectPositions (which already owns the C17 JNI ABI).
- M3IntArrayListContractProof runs 9,000 seeded JDK ArrayList comparisons,
  extreme signed int bounds, iterator invalidation, rejected mask inputs,
  stable sort ties, immutable filtered/sliced views and index-only masks.

No per-entry object/node, new JNI symbol, copied third-party algorithms,
Java object pointer transfer or Synexia runtime dependency. Native selection
is deliberately opt-in: NATIVE_REQUIRED refuses a missing library.

## Evidence and commands
The canonical recipe is a Maven-executed exact source generator, using sealed
UTF-8 SHA-256 templates and fail-closed ABSENT-before admission. It is not a
new general OpenRewrite rewrite engine. Maven:
`mvn -B -ntp -f synexia-openrewrite-recipes/recipes/m3-int-array-index-20261018/pom.xml verify`.

Locally run with Java21 strict warnings: new files plus test-only API-shaped
M3IntCollection/M3IntIterator/M3IndexMaskCompactor stand-ins PASSED
5,269,762 assertions for 9,000 seeded operations, with a separate
missing-native refusal. Those stand-ins are not distributed or counted as
real M3 owner/JNI proof. Target CI must compile actual existing M3 sources,
run native code with -Xcheck:jni and independently qualify Maven/JUnit.

The five-provider direct SDK and full M3JDK21 JMOD/image/jtreg/JCK,
cross-platform JNI, latency, retained-memory and GC/JMM proofs remain HOLD.
