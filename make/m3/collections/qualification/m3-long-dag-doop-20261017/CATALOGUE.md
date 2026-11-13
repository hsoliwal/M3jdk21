<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 — DOOP collection operator DAG and GoF intake

Source module: `m3/collections`; canonical package: `com.m3.collections` (not `java.util`).

## Taxonomy: 23 GoF patterns, four additional M3 concepts

The original Gang of Four catalogue has **23 patterns**: **5 creational, 7 structural, 11 behavioral**.
A separate fourth section adds **four M3 capabilities** (DOOP, virtual accumulator, virtual collector,
marker/replay interoperability) to form a **27-row catalogue**, without calling them GoF patterns.
Entries marked `REFERENCE` or `FUTURE` are not implemented merely by appearing in this catalogue.

## Architecture, ownership and DAGs

The DAG graph is immutable, parent-owned storage is retained only by `Source` nodes, and each
new `filter/map/skip/limit/distinctBounded/sortedBounded/concat` method produces a **new plan**, never
mutating an old plan or source. `Source` delegates to `M3LongCollection.longStream`.
`M3LongDag.filtered` delegates to **existing** `M3LongFilteredView`, while
`sortedFiltered` delegates to **existing** `M3LongSortedViews.filter`.
Sorted stages materialize a bounded primitive-long snapshot; ordinary stages don't. DAG
construction is lazy; evaluation is a fresh, ordered traversal with owned close semantics.
The graph interpreter uses an explicit stack to avoid recursive graph descent and counts
shared nodes by reference, but arbitrary deep JDK Stream pipelines may still exceed JVM limits.

The separate `M3VirtualAccumulator` is a supplier + primitive fold; its `evaluate` creates new
state for each traversal. `M3VirtualCollector` finishes that state and can `tee` another
collector over the **same exact DAG** in a single pass. No retained per-value objects or
new stream execution engines are created. `collect(Collector<Long,...>)` is an explicit
JDK boxing boundary; primitive accumulator operations remain unboxed.

Live filtered wrappers reflect preterminal owner changes; sorting is a terminal-stage snapshot
and cannot be treated as a live `NavigableSet` view or used to mutate its owner. Callable
predicates/comparators must remain stable during traversal. The existing JNI mask compactor is
unchanged: JAVA oracle always available, NATIVE_REQUIRED must fail if native binary is absent.

## API

```java
M3LongDag view = M3LongDag.filtered(owner, x -> x >= 0)
    .map(x -> x * 2)
    .distinctBounded(4096)
    .sortedBounded(4096);

long count = view.virtualAccumulator(() -> new long[1],
    (state, value) -> state[0]++).finish(state -> state[0]).evaluate();
long max = view.virtualCollector(() -> new long[]{Long.MIN_VALUE},
    (state, value) -> state[0] = Math.max(state[0], value),
    state -> state[0]).evaluate();

try (LongStream s = view.stream()) {
    // ordinary JDK primitive stream operators may follow
    long sum = s.sum();
}
```

## Scope boundaries and verification

No new JNI symbol, transaction boundary, remote microservice EIP route, Spark shuffle,
Flow.Publisher, durable replay/checkpoint or JDK `java.util` replacement is claimed.
DOOP state is compact and parent-owned **where supported by the original source**; a client
accumulator may itself retain arbitrarily much state. A `sortedBounded` invocation allocates
an explicitly bounded materialization and `batches` emits one bounded `long[]` per chunk.

The canonical source-change operator is the **existing**
`M3HashPinnedJavaSnapshotRecipe` with the crate `m3-long-doop-dag-20261017`.
The receipt preserves SHA-256 postimages of all new Java sources and original Git-blob owner
pins. Maven JUnit runs actual OpenRewrite SDK admission/refusal/fixed point, and the module
oracle tests indexed primitive collections against JDK Streams. JNI is checked only through
existing `M3IndexMaskCompactor` with a Java fallback and explicit native opt-in.
