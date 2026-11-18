# Native M3 ObjectStream: language-idiom convergence

**Status: draft receiver; module Maven/OpenRewrite/native/image gates pending.**

This additive change belongs to the relocated JDK module `jdk.m3.collections`:

- public API: `src/jdk.m3.collections/share/classes/jdk/m3/collections/ObjectStream.java`;
- one-pass kernels: `src/jdk.m3.collections/share/classes/jdk/m3/collections/ObjectStreamLanguageKernels.java`;
- differential contracts: `test/jdk/jdk/m3/collections/ObjectStreamLanguageContractTest.java`.

Canonical template, source pins and recipe: `hsoliwal/com.synexia`, `M3StreamCrossLanguageRecipe`, crate `m3-stream-cross-language-20261118`. These sources must be admitted by the existing `M3HashPinnedJavaSnapshotRecipe`. Do not manually normalize or merge source changes that fail preimage admission.

## New operators

| Operator | Semantics | Extra retained state |
|---|---|---|
| `mapIndexed(index,value)` | zero-based primitive long index mapping; no Entry or Pair allocation | O(1) |
| `zipWithNext(previous,current)` | map neighboring pairs; zero output for fewer than two inputs | O(1) |
| `runningReduce(reducer)` | emit first value and every incremental reduction | O(1) |
| `chunkBy(key,maxRunLength)` | emit immutable consecutive equal-key runs, not global groups; 1..65536 cap | O(maxRunLength) |
| `distinctUntilChanged(key)` | suppress adjacent duplicate keys, preserve later reappearances | O(1) |
| `intersperse(separator)` | emit separator between elements; null allowed | O(1) |
| `zip(other,mapper)` | truncate at shortest input; close both sources; replay only if both replayable | O(1) |

Unlike eager collection/list helpers, all operators open and traverse upstream only on terminal evaluation; each replay gets fresh cursor state. Null source values are preserved where the downstream contract permits them. The operators supplement the existing `batch`, full `window`, `scan`, `distinctBy`, `partitionBy`, and `groupBy` rather than duplicating them.

## Why these

These common loop idioms are seen in Kotlin Sequences, Python itertools, Rust itertools, C# LINQ and Scala collections. They are independently implemented in Java; no third-party source code was copied. The future catalogue for `groupMapReduce`, `topKBy`, `zipLongest`, and JDK 24 Gatherers-compatible partial sliding windows is maintained in the canonical Synexia crate. Java 24 APIs must not be introduced into `java.base` on the M3JDK21 compatibility path without separate specification/proof.

## Evidence and gates

Java 21 `javac --release 21 -Xlint:all -Werror` and 6 JUnit-shaped contract methods passed against a minimal compatible facade and the actual new kernel, including 100 randomized adjacent-pair reference cases, replay, null, run budgets and two-input closure.

**Not yet run:** a real M3JDK21 module build, provider interop and full JUnit against the actual owner, Synexia Maven/OpenRewrite fixed-point and refusal, JDK image smoke tests, JNI boundary audit, or performance/allocations versus existing stream helpers. The PR remains draft until these gates are satisfied.

## Donor documentation

- Kotlin Sequence: https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.sequences/
- Python itertools: https://docs.python.org/3/library/itertools.html
- Rust itertools: https://docs.rs/itertools/latest/itertools/structs/struct.TupleWindows.html
- .NET LINQ Enumerable: https://learn.microsoft.com/en-us/dotnet/api/system.linq.enumerable
- Scala collections: https://www.scala-lang.org/api/3.1.1/scala/collection/View%24%24ScanLeft.html
- JDK Gatherers (future): https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/util/stream/Gatherers.html
