# M3 object-stream superset — receiver notes

**Status: draft candidate; not shipped in the installed M3JDK image.**

The shared, sequential-by-default object stream supplements JDK `Stream` and the primitive `M3LongDag`. It lives in `com.m3.collections` and reuses the current `M3IndexedListSnapshot` and `M3IndexMaskCompactor` owners. It does not replace public `java.util.stream` contracts or give donor libraries an M3 runtime dependency.

## Covered contracts

| Family | Operators | Contract |
|---|---|---|
| Source | snapshot, live, once | Snapshot freezes reference order; live reflects source according to its iterator contract; one-shot stream fails on reopen |
| Stateless | filter, map, flatMap, flatMapIterable, mapMulti, peek, skip, limit, takeWhile, dropWhile | Lazy pull pipeline; encounter order preserved |
| Stateful | distinct(max), sorted(comparator,max), sorted(max) | Explicit budgets; stable sorted result uses M3 integer index lane |
| Batch/replay | batch(size, tail), window(width), scan, mark(max), reprocess | Bounded state; window uses a ring; mark is eager bounded output capture, not cursor rewind |
| Array/mask | indexedSnapshot(max), indexedFiltered(predicate,max,backend) | Existing object/index owner holds references; optional JNI sees only byte flags |
| View | virtualCollection | Read-only replaying facade; traversal may re-run functions |
| Reduce/collect | fold, reduce, collect, JDK Collector, tee, count, groupBy, countByKey | Encounter-order sequential semantics; no associativity assumption |
| Terminals | findFirst, findAny, anyMatch, allMatch, noneMatch, min, max, forEach, forEachOrdered, bounded toArray | Short-circuit/match/extrema terminals close their sequential stream; array materialization requires an explicit element budget |
| Key aggregation | groupBy, countByKey | First-key order, null key/value/state support, explicit key cap |
| Parallel | parallelByKey().mapOrdered | Per-key serial workers, bounded in-flight work, source-ordinal merge; different key callbacks can overlap |

## Five-provider qualification

`interop/M3ObjectStreamProviderContractTest.java` uses the optional dependencies already declared in this module: Guava `ArrayListMultimap`, Eclipse `MutableList`, Spring `LinkedMultiValueMap`, and Commons Collections `ListUtils.predicatedList`. JDK `List` is also covered. Tests assert encounter order, duplicates, snapshot-versus-live behavior and the index-mask selection contract. No donor source is copied into M3JDK.

## Provenance / admission

Canonical recipe owner: `hsoliwal/com.synexia`, branch `feature/m3-object-stream-superset-20261018`. The named `M3ObjectStreamSupersetRecipe` uses the incumbent `M3HashPinnedJavaSnapshotRecipe`; its manifest lists absent-before admission and exact SHA-256 postimages for eight outputs: three production owners, four core contract/test sources, and the optional provider interop test. Apply the recipe serially only from a clean target checkout, record the receipt, and verify every resulting hash and the source fixed point.

## Local evidence / open gates

The standalone Java 21 fixture passed `javac --release 21 -Xlint:all -Werror` and 908 deterministic assertions against stand-in implementations of `M3IndexedListSnapshot` and `M3IndexMaskCompactor`. That is not a build against current product helper classes. The new terminal-contract oracle adds 18 deterministic checks for short-circuit results, predicate terminals, extrema, side-effect traversal, null/Optional semantics, typed arrays, and budget refusal; it is committed but **not yet executed**. Full M3JDK Maven tests, actual provider dependency integration, Synexia OpenRewrite engine replay/refusal/fixed-point checks, native/JNI parity, installed-image smoke tests and performance numbers remain **NOT RUN**. Keep this PR draft until they pass on this exact receiver branch.
