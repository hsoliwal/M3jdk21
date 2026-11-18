# Native M3 ObjectStream — compatible stream operator convergence

**Candidate implementation:** current-master source-preserving successor to the draft language waves.
**Do not merge/release without full compiled product and canonical recipe proof.**

## Rationale

A direct merge of the preceding wave-2 and wave-3 drafts is unsafe. One draft redefines the public `IndexedMapper` and `mapIndexed` signatures with reversed argument order, and another collides with the current public Optional-returning `mapWhile` method after Java erasure. This implementation preserves the native APIs:

- `mapIndexed((value, ordinal) -> ...)` remains value-first.
- `mapIndexedOrdinalFirst((ordinal, value) -> ...)` remains ordinal-first.
- `mapWhile(mapperToOptional)` remains defined by modified M3JDK `java.util.stream.Streams.mapWhile`.
- `mapWhileNullable(mapper)` is a separate nullable-sentinel convenience method, not an overload of `mapWhile`.
- `pairwise` retains its canonical M3JDK `java.util.stream.Streams.pairwise` delegate.
- `arrayWindow`, `mapAdjacent`, `distinctAdjacent`, `foldByKey` and other existing methods are left intact.

The original Java 21 type system and explicit source signatures are authoritative. Java `Streams` here means the M3-modified exported `java.util.stream.Streams` class, **not** the package-private class from an unmodified OpenJDK 21 build.

## Six source-sealed Java outputs

`ObjectStream.java` (one exact current-master replacement), `ObjectStreamAdvancedKernels.java`, `ObjectStreamLoopKernels.java`, `ObjectStreamAdvancedContractTest.java`, `ObjectStreamLoopContractTest.java`, and `ObjectStreamCompatibilityGateTest.java` (five absent-before files).

Canonical OpenRewrite owner is Synexia `M3StreamCurrentCompatibilityRecipe`, delegating to the existing `M3HashPinnedJavaSnapshotRecipe`. The product source preimage `ab3376a4585e5926dd3c3d9508d9c4a83b159a2385a55b0f3f6b0b5c471489cf` is replaced by exact postimage `802274f6eb95280742e89ca57d92beb1e6392d8e82e218b752b328e2f0cd8f0d`. The other five output hashes are sealed in the canonical manifest.

The product has a discoverable source receipt test:
```sh
python3 -m unittest -v m3/collections/test_verify_stream_current.py
```
This checks six source hashes plus unique existing method declarations and preserved JDK `Streams` delegates. Existing M3 Collections CI discovers `test_verify_*.py` modules; a green receipt is still **not** a Java compiler, native image, or performance proof.

## Sixteen new operations and storage costs

| Family | Operators | Storage / contract |
|---|---|---|
| Map/fuse | `mapNotNull`, `filterMap`, `mapWhileNullable` | Constant cursor state, callback order and termination explicit |
| Merge/pair | `zipLongest`, `interleave`, `mergeSorted`, `coalesceAdjacent` | O(1) state; stable left-first sorted ties; both sources closed |
| Window/tail | `windowSlidingPartial`, `takeLast`, `dropLast`, `stepBy`, `takeUntilInclusive` | O(width) or O(k) bounded ring; otherwise O(1) |
| Aggregate | `topKBy`, `groupMapReduce`, `runningFoldIndexed`, `partitionMap` | O(k), O(maxKeys), O(1)+client state, O(maxElements) respectively |

The `topKBy` heap holds objects in `Object[]` and ordinals in `long[]`, avoiding per-entry heap wrapper nodes. It uses O(n log k+k log k) time and O(k) retained state, with stable earlier-encounter tie ranking. It is a blocking finite-source operator. No Java object callbacks are sent to JNI.

## Required acceptance gates

- Actual Synexia OpenRewrite execution: preimage refusal, exactly regenerated postimage, fixed-point second pass.
- Real M3JDK21 Java module build, JUnit including compatibility regressions, custom JDK image smoke and donor interop. Stock Java 21 is insufficient for public modified `java.util.stream.Streams` methods.
- Native/JNI boundary verification, allocation and throughput benchmarks under representative pressure, with documented false positives and counterexamples.
- Never change protected Synexia `develop` by rebase. Keep this candidate as a reviewable PR, and repair the canonical recipe rather than hand-edit generated target sources on any test failure.

Operator concepts: Rust Iterators/itertools, Kotlin Sequences, Python itertools/heapq, C# LINQ, Scala Collections and Java Gatherer specifications. No third-party implementation code is copied by these new kernels.
