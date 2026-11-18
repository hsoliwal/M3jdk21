# M3 ObjectStream language-convergence wave 2

**Status:** review candidate only, not released or admitted to an installed JDK image.

The canonical owner remains `jdk.m3.collections.ObjectStream`. This extends the seven wave-1 methods in the existing draft receiver PR; it does not introduce a second stream engine, replace `java.util.stream.Stream`, or require Java 24.

## New operators and compatibility

| Operator | Semantics | Extra state |
|---|---|---|
| `mapNotNull(mapper)` | Apply mapper exactly once, emit only non-null mapped values, fused in one mapMulti stage | O(1) |
| `filterMap(optionalMapper)` | Emit values from present Optional results; reject null Optional from mapper | O(1) |
| `zipLongest(other,mapper)` | Continue until both inputs exhausted; explicit presence booleans distinguish missing and actual null; close both | O(1) |
| `interleave(other)` | Alternate left-first while possible, then drain remaining side; close both | O(1) |
| `windowSlidingPartial(width)` | Full sliding windows, except one immutable partial window when the entire nonempty input is shorter than width; width 1..65536 | O(width) |
| `takeUntilInclusive(predicate)` | Include first matching input value and stop upstream traversal | O(1) |
| `topKBy(k,comparator)` | Descending best-first k items with stable ties; bounded object/ordinal heap; k 0..65536 | O(k) |
| `groupMapReduce(key,maxKeys,value,reducer)` | Ordered sequential per-key fold with first-key order and null key/value support | O(maxKeys) |

`topKBy` uses a worst-root candidate heap and primitive `long[]` encounter ordinals, with O(n log k + k log k) time and no per-element Entry/Pair nodes. It is intentionally a blocking stateful operation on finite input; unlike `windowSlidingPartial` it must exhaust the source before yielding the highest-ranked result. A request for k=0 closes the opened source and returns no values.

The underlying source is opened lazily on terminal evaluation. Replayable plans obtain fresh kernel state each time. One-shot inputs remain one-shot, and two-input results are replayable only when both inputs are replayable. Null mapping/Optional semantics are not conflated with missing zip sides. No Java object callbacks are sent into JNI.

## Programmer-defined loops that can converge

Instead of manually collecting values and then sorting the whole input:

```java
// Reference implementation: full sort and bounded selection
List<Item> ranked = new ArrayList<>(items);
ranked.sort(Comparator.comparingInt(Item::score).reversed());
List<Item> best = ranked.subList(0, Math.min(20, ranked.size()));

// M3: ordered top-K without sorting and retaining the complete input
List<Item> best = ObjectStream.snapshot(items)
    .topKBy(20, Comparator.comparingInt(Item::score))
    .toBoundedList(20);
```

The reverse ordering in the reference is intentional: `topKBy` selects greatest by the supplied comparator and emits greatest first, using earlier encounter order for ties.

Other candidate custom-written shapes are mapped in Synexia's `recipes/m3-stream-language-wave2-20261118/CUSTOM_LOOP_IDIOMS.tsv`. This is a **candidate catalogue**, not an unconditional rewrite of arbitrary loops; compilation, exception, null, side-effect, ordering and allocation behavior must all be proven on a per-file basis.

## Canonical recipe and checks

Synexia `M3StreamLanguageWave2Recipe` delegates to the existing `M3HashPinnedJavaSnapshotRecipe`. Apply only after `M3StreamCrossLanguageRecipe` wave 1; the wave 2 manifest pins the exact wave-1 `ObjectStream` SHA-256 as its sole replacement preimage. It adds two absent-before owners: `ObjectStreamAdvancedKernels.java` and `ObjectStreamAdvancedContractTest.java`.

**Executed locally:** `javac --release 21 -Xlint:all -Werror` and 14 test methods, including 500 seeded top-K vs stable-sort differential cases, using the actual new advanced kernel and a minimal compatible ObjectStream facade. All three Java receiver/template blobs match. **Not run:** actual product-module compiler or JUnit, full Maven/OpenRewrite application and fixed-point/drift refusal, provider interop, JNI/native/image smoke, and resource benchmarks. Keep PR draft until those pass.

## Concept donors (no foreign implementation code copied)

- Kotlin Sequence mapNotNull: https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.sequences/map-not-null.html
- Rust itertools interleave/zip_longest: https://docs.rs/itertools/latest/itertools/
- Python itertools: https://docs.python.org/3/library/itertools.html
- Scala IterableOps: https://www.scala-lang.org/api/current/scala/collection/IterableOps.html
- Microsoft LINQ: https://learn.microsoft.com/en-us/dotnet/api/system.linq.enumerable
- Java 24 Gatherers: https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/util/stream/Gatherers.html
