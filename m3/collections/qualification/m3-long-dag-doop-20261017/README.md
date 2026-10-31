<!-- SPDX-License-Identifier: Apache-2.0 -->
# Receiver: M3JDK21 primitive-long DOOP DAG and virtual reduction

**Target:** `hsoliwal/M3jdk21` / `master@970a34350ebdf6a3f1c5ff5acc8445d0f3515f55`.

**First-party convergence owner:** the existing Synexia `M3HashPinnedJavaSnapshotRecipe`,
not a second source transformer. New named declarative recipe:
`com.synexia.rewrite.M3Jdk21LongDoopDag20261017` in
`m3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/m3-long-dag-doop-20261017.yml`.
Its `crateName` parameter is `m3-long-dag-doop-20261017`.

Five ABSENT-before Java targets are sealed in the classpath manifest by SHA-256.
The reusable OpenRewrite scanning recipe parses reviewed Java LSTs, rejects unexpected
preimages, duplicate targets and foreign representations, and produces a deterministic
fixed point. The companion JUnit tests run **actual OpenRewrite SDK** admissions,
all 32 partial-resume combinations, idempotence and source/representation refusals.
The receiver has no automatic authority to mutate existing JDK or MAT owners;
`OWNER_GUARDS.tsv` preserves ten original Git blobs for their APIs and the unchanged
optional JNI mask backend.

## Direct proof

```sh
mvn -f m3/collections/qualification/m3-long-dag-doop-20261017/pom.xml verify
sh m3/collections/qualification/m3-long-dag-doop-20261017/verify.sh
mvn -f m3/collections/pom.xml test
```

Maven SDK tests run before the standalone Java/JNI verifier. The verifier checks
all SHA-256 postimages, Git blob guards, strict Java 21 compilation, the 291-assertion
contract main under two heaps, C17 strict/UBSan build, and `-Xcheck:jni` parity of the
**existing** `M3IndexMaskCompactor` source. It does not change JNI symbols.

The local isolated test executed strict Java 21 compilation and 291 assertions at
`-Xmx16m`, but used interface-compatible doubles for some original M3 owners.
**That is not real receiver JNI or full module evidence.** The Maven/OpenRewrite SDK,
full module test, C17 native parity, jtreg and whole OpenJDK build remain NOT_RUN here
until independent checks publish receipts. Keep the PR DRAFT until validation.

## GoF / M3 scope

The original GoF corpus contains 23 patterns (5 creational, 7 structural, 11
behavioral). `CATALOGUE.tsv` records 23 distinct entries plus four separately
labeled M3 extensions (DOOP, VirtualAccumulator, VirtualCollector, BarrierReplay),
**not** 27 GoF patterns. The code qualifies the directly used subset; catalogue
presence does not imply complete implementation of all patterns.

This is an opt-in separately built `com.m3.collections` module, not replacement of
JDK `java.util` implementation. Actual filtered/sorted collection wrappers remain
in `M3LongFilteredView`/`M3LongSortedViews` with their existing mutation, ordering,
and source-lifetime contracts. New virtual operations are primitive-first and
materialization occurs only at explicit bounded stages.
