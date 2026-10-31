# M3JDK21 keyed stream array/mask completion — 2026-10-19

**Product owner:** `m3/collections/src/main/java/com/m3/collections/M3KeyedParallelismPlan.java` in
`hsoliwal/M3JDK21`, not an external M3 + JDK bridge.

## Contract and problem category

The existing public `M3ObjectStream.parallelByKey(keyExtractor).mapOrdered(...)`
returns a read-only list in source order. Equal keys have one serial lane.
Null input/results are allowed. Source/key extraction, mapper exceptions, bounds,
worker interruption and result ordering retain their established contracts.

The original private `TreeMap<Long,R>` completion reorder buffer had no
retained-state bound relative to `maxInFlight`: if ordinal 0 blocks and
later work finishes, admission can continue while `TreeMap` grows.
The original blocking enqueue can also strand the coordinator when a
worker exits on mapper failure and its lane queue fills.

## Refinement (private atoms, no public API changes)

1. Allocate a private fixed `Object[] pending` ring of next-power-of-two
   `maxInFlight` and a primitive `long[] occupied` bit mask, once per call.
   Each result uses `slot = ordinal & (capacity - 1)`. A set mask bit denotes
   an occupied slot even when the actual Java result is null. Completed
   contiguous source ordinals are appended to the existing final
   `ArrayList`, clearing their slots. A duplicate/out-of-window completion
   is rejected rather than overwriting another result.
2. Bound both outstanding **work** and the **ordinal admission window**:
   `submitted - nextOrdinal < maxInFlight`. This is necessary for the
   ring bound and prevents unbounded retained out-of-order results.
   Head-of-line blocking may reduce throughput; that is the explicit
   resource-bound tradeoff, not a global speedup claim.
3. Publish an `AtomicReference<Throwable>` when a worker fails, before
   publishing its failed completion. Queue admission uses a nonblocking
   `offer` fast path; only a full queue uses timed offers and tests the
   failure signal. Exceptions preserve their original runtime/error type.

No per-entry Node/Entry/Cell owner, new JNI export, source element copy,
mutable shared result cache, or runtime Synexia dependency is introduced.
Existing Work/Completion queue messages remain their original records.
No scalar token→index lookup, String residency or native bulk kernel changes.

## Recipe and source custody

Reusable **existing** OpenRewrite engine:
`com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe`.
New named recipe:
`com.synexia.m3.KeyedOrderRing20261019`.

Recipe resources:
`m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/keyed-ordered-ring-20261019/`.

Actual owner preimage Git blob:
`045e64a2dfc5c395bde5ec211196254ae7a680bb`.
Actual new owner blob:
`492d03df34436cc339142e68d8fdb02c4cf9bd83`.

Preimage SHA-256:
`efaf6160476817518ce6921c336c5b0d7232be7772a312185c503dee5bceb1bd`.
Postimage SHA-256:
`2687afa8040de89110888f0e17a876e6d37ff40230f80cba6d5024bb83f9fe9c`.

Manifest is source-bound, with exact preimage/postimage. The host tests
replay before→after, accept postimage fixed points, preserve unrelated
files, and reject missing, changed or duplicate target sources. This is
`PlainText` source custody, **not proof of OpenRewrite Java semantic/type
equivalence**. Actual compiler, JUnit and runtime gates remain separate.

To run when Maven and required dependencies are installed:

```sh
mvn -f m3/collections/recipe/keyed-ordered-ring-20261019/pom.xml test
mvn -f m3/collections/pom.xml -Dtest=M3KeyedOrderRingTest,M3ObjectStreamContractTest test
```

## Tests and algorithm catalogue

- `M3KeyedOrderRingContract`: 4097 ordered results, null results,
  65-entry non-power-of-two configured window (128 physical slots),
  read-only refusal, slow-head admission bound and dead-worker escape.
- `M3KeyedOrderRingTest`: owning-module JUnit entry point.
- `M3KeyedOrderRingRecipeTest`: source/recipe/refusal proof.
- `CATEGORY_EVALUATION.tsv`: LeetCode 622, HackerRank Java Dequeue,
  GeeksforGeeks circular queue, incumbent M3 TreeMap and native proposal,
  with selection/rejection rationale; **no donor code copied**.

## Remaining qualification

`mvn` is not available in the authoring container. Java 21 `javac`
is installed, but no full Maven/OpenRewrite SDK run, complete collection
module test run, full JDK image, JNI cross-platform build, installed JDK
API/jtreg/JCK or latency/allocation benchmark is claimed here.
Product branch and recipe are review candidates until those gates pass.
