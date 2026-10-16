# M3 count totals: borrowed atoms, no new storage hierarchy

This is an additive refinement of the existing `counts` packet. `M3CountTotals`
reuses `M3LongBag` storage and the arithmetic from `M3CountLanes` and the original
Synexia count-lane donor. Existing Java classes and JNI entry points are unchanged.
No result bag, occurrence list, or result count array is needed for a scalar query.

## Public operations

`sum`, `union`, `intersection` and `difference` accept two stable `M3LongBag`
instances and return the same occurrence total as their existing materialized
algebra. Maximum-count union is not distinct-set union. Union subtracts the overlap
before adding the second total, so valid large overlapping unions do not falsely
overflow. Sum is O(1); the other operations use a count scan without result storage.
The intersection scan picks the smaller distinct-key count, not a guaranteed smaller
backing capacity; the existing public bag interface does not expose capacity.

`combine` and `combineNative` reduce already aligned count slices using existing
`M3CountLanes` operation constants. Nulls, bounds, operation, negative counts and
individual/aggregate overflow retain their original validation order. Native result
scratch and `NewLongArray`/`SetLongArrayRegion` calls are removed; the two input
scratch lanes are fixed at 1024 jlongs each (16 KiB). Native loading is explicit.
Java/native exception messages and resource-exhaustion timing are not promised equal.
Inputs must remain unchanged; no concurrency or implicit alignment promise is added.

The original bag count lookup and lane owners remain authoritative. This deliberately
avoids duplicating hash tables, exposing slots, flattening references into native
integers, or replacing concrete Guava/Eclipse/Spring collection contracts. Bag
intersection retains a small callback accumulator, so zero allocation is not claimed.
The native scalar path contains no Java result-array allocation but still runs within
a JVM with its own implementation costs.

## Recipe-first reproduction

The Maven refinement is `m3/collections/counts/totals/recipe/pom.xml`. It compiles
**the already retained** `../../recipe/pinned-engine` providers without modifying or
copying them. Java and module-relative native/text manifests are separate. Sources,
tests, catalogue and this document are generated from actual OpenRewrite results.
Only absent targets or exact outputs are admitted; drift and symlinks are rejected.
An exclusively owned checkout is required; delivery is not a multi-file transaction.

```sh
mvn -B -ntp -f m3/collections/counts/totals/recipe/pom.xml test
mvn -B -ntp -f m3/collections/counts/totals/recipe/pom.xml -Dm3.totals.target="$PWD" test
```

The predecessor count recipe, its receipt and its gate remain unchanged. This
refinement is additive and has its own receipt. Runtime tests compare against an
independent frequency map and BigInteger, as well as existing Java/native array
kernels and materialized bags. JNI verification requires a real library, C17 UBSan
and `-Xcheck:jni`; it cannot silently skip native proof. Existing count and interop
regressions are rerun. The allocation probe compares equivalent array/bag materialized
results with scalar reductions; it records actual thread-allocation counters, not a
JMH throughput claim. Consult exact-head reports for execution status.

`catalogue.tsv` is an eight-column sub-ledger usable by the existing
`com.m3.collections.tooling.M3CountedCatalogue`; no new catalogue tool is added.
Pinned derivation paths/hashes and separate challenge reviews are in `provenance.json`.
This packet is not every collection family, complete object-bag/facade coverage,
installed-JDK admission, or a benchmark-based universal performance claim.
