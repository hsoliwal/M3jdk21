# J485 current-tree reconciliation

Status: recipe-first proof/test reconciliation for the already-materialized JEP 485 candidate.

## Inventory finding

PR #177 implemented the final corrected Stream Gatherers lineage and a FILE-atomic recipe/proof lane.
Its ancestry was later attached by a merge whose tree intentionally stayed unchanged. Current master
therefore contains a different recovery payload.

Current product code is **not downgraded** by this packet:

- Gatherer.java and Gatherers.java match the corrected donor state;
- GathererOp retains the Java-21 local VarHandle helper and includes upstream-size propagation;
- AbstractPipeline and ReferencePipeline contain later stream work in addition to Gatherer-required
  integration;
- Stream.gather and package documentation are present.

Those seven product owners are read-only inputs for this reconciliation.

## Residual drift

Seven jtreg files on current master are older than the final/adapted PR #177 corpus. One of them,
GathererShortCircuitTest.java, again contains post-21 unnamed `_` parameters and cannot be a
source-21 proof input. GathererTest.java also lacks the later size-propagation regression from
JDK-8357647.

The exact mutation set is therefore seven test files only. GatherersMapConcurrentTest.java already
matches the final corrected donor and is not rewritten.

## Recipe rule

Each changed test is one FILE-local `M3Jdk21HashPinnedSnapshotRecipe` atom with:

- exact current-master preimage;
- exact final/adapted postimage from PR #177;
- source-21 parse/round-trip;
- drift refusal;
- second-pass fixed point.

Short aggregate recipe: `com.m3.j485.Tests`.

No product source, public API, JNI ABI, dependency, coverage threshold, or Java21 identity policy is
changed by this packet.

## Verification

The existing `M3 JEP 485 Stream Gatherers admission` workflow remains canonical. It is extended to
run:

- the seven FILE-atom OpenRewrite tests;
- the bounded Java-21 patched-module smoke from PR #177;
- existing full JDK image/jtreg/API-smoke gates.

The bounded smoke is not a replacement for the eight-file jtreg family or full image proof.
