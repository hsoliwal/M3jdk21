# Native M3 ObjectStream — third cross-language operator wave

**Status: DRAFT/HOLD. Not yet validated in the actual installed M3JDK21 image.**

This is a stacked successor to the second-wave branch, not a replacement for the standard Java `Stream` and not a new independent `CStream` runtime. Ownership remains `jdk.m3.collections.ObjectStream` and its package-private Java 21 kernels. Existing M3 `arrayWindow`, `mapAdjacent`, `distinctAdjacent` and earlier operator waves stay unchanged.

## New public methods and their exact behavior

| Method | Inspired by | Storage | Important contract |
|---|---|---|---|
| `mapWhile(mapper)` | Rust Iterator.map_while | O(1) | Null mapped result stops after consuming that input; null is not emitted |
| `stepBy(stride)` | Rust Iterator.step_by, Python islice | O(1) | Emitted indices are 0, stride, 2×stride; stride positive |
| `mergeSorted(other, comparator)` | Python heapq.merge | O(1) | Sources must already be sorted by same comparator; equal comparisons emit left first |
| `dropLast(count)` | Kotlin dropLast / deque lookahead | O(count) | Lazy bounded lookahead, 0..65536, null-preserving |
| `takeLast(count)` | Kotlin takeLast / Python deque(maxlen) | O(count) | Finite input required; whole source consumed before first emitted value |
| `runningFoldIndexed(seedSupplier, folder)` | Kotlin runningFoldIndexed | O(1)+client state | Seed emitted even on empty source; 0-based long index for input |
| `coalesceAdjacent(compatible, combine)` | Rust itertools.coalesce | O(1) | Condition compares accumulated merged value with next item; no global grouping |
| `partitionMap(predicate, leftMapper, rightMapper, maxElements)` | Rust itertools.partition_map | O(maxElements) | One predicate and one selected mapper per item; typed immutable output lists; hard materialization bound |

All lazy operations create fresh cursor state on each traversal of a replayable plan; one-shot source semantics are retained. Two-source merges close both upstream streams on terminal closure, including exceptions. Callback exceptions are not swallowed. No per-input Pair/Entry/Either objects are retained, and JNI never receives Java callbacks or object references from these new operators.

## Custom-loop replacements

### Example: sorted merge, not sorting a concatenated list

```java
// Precondition: these two inputs are already sorted ascending
List<Integer> merged = ObjectStream.snapshot(sortedA)
    .mergeSorted(ObjectStream.snapshot(sortedB), Integer::compare)
    .toBoundedList(sortedA.size() + sortedB.size());
```

### Example: indexed running aggregation with an explicit seed

```java
List<Long> totals = ObjectStream.snapshot(values)
    .runningFoldIndexed(() -> 0L,
        (index, accumulated, value) -> Math.addExact(accumulated, value))
    .toBoundedList(values.size() + 1);
```

### Example: two typed result channels

```java
var split = ObjectStream.snapshot(rows).partitionMap(
    Row::valid,
    Row::id,
    Row::validationError,
    100_000);
List<Long> validIds = split.left();
List<String> errors = split.right();
```

Note: `partitionMap` applies an explicit source retention limit; the `Row` examples are illustrative and not added product types.

## Recipe, attribution, acceptance

Canonical authoring in Synexia: `M3StreamLanguageWave3Recipe`, source crate `m3-stream-language-wave3-20261118`. It delegates exact preimage/output SHA-256, parsed Java LST and fixed-point/refusal handling to existing `M3HashPinnedJavaSnapshotRecipe`. Apply **after wave 2**. The M3 module receiver does not own independent transformation logic.

The source template for `ObjectStream.java` is pinned to wave-2 postimage SHA-256 `3348fbf7ed5a61011720a77cd0662d85a98e482ccd9a5108ef3f1cb707b6bc6a` and wave-3 postimage `a0d2f855506234102fbae210f28e08caa38c843a87b93d9731974eb14c3e0630`.

**Local test evidence:** strict Java 21 `javac --release 21 -Xlint:all -Werror` on actual new kernels plus a minimal compatible ObjectStream facade and JUnit stand-ins; 23 contract-test methods across waves 1–3 passed, with seeded differential merge/stride/tail/top-K checks. This does **not** prove that the full actual M3JDK21 module compiles, that the native image works, or that the real OpenRewrite engine has executed the recipes. Pending hard gates are real module and JUnit, Synexia Maven/OpenRewrite apply/refusal/fixed-point, JNI/native image smoke, allocation/performance comparisons and provider integrations. Keep both successors in draft until all pass.

**Concept donors, not copied implementation:** [Rust Iterator](https://doc.rust-lang.org/std/iter/trait.Iterator.html), [Rust itertools](https://docs.rs/itertools/latest/itertools/trait.Itertools.html), [Python heapq](https://docs.python.org/3/library/heapq.html), [Kotlin runningFoldIndexed](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.sequences/running-fold-indexed.html).
