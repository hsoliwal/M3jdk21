<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Indexed algorithm contracts: prefix facts and exact Fenwick sums

[Guide](README.md) · [Package responsibilities](m3-packages.md) · [Coverage](coverage.md)

This reference reads the public `hsoliwal/M3jdk21` source at `1eac151fe56bccdc33b24a0155cc1ac5b9959005`. It covers every declared public entry point of two separate owners: eight methods on `M3PrefixZ`, and two constructors plus ten methods on `M3LongFenwickTree`. Their dependencies, tests and build routes are distinguished below. Source-derived examples and bounds are **not executed results**; builds, tests, benchmarks and runtime qualification are **NOT_RUN** in this documentation pass.

Both owners produce exact facts when their operations complete. Neither offers probabilistic similarity or approximate sums. Their primitive-array payload counts are exact for the arrays counted, but are not measurements or limits of whole-object retained heap.

## Placement and ownership

| Owner | Role and boundary | Source authority |
| --- | --- | --- |
| `com.m3.algorithm.M3PrefixZ` | Immutable prefix-analysis result over an already admitted sealed `M3StringPiece`; outside `java.base`. Does not own, intern or retain the input text | [Complete implementation](../../m3/algorithms/src/com/m3/algorithm/M3PrefixZ.java), existing `m3.prefix-z` entry in [name mapping](../../m3/docs/name-mapping.json) |
| `com.m3.ds.M3LongFenwickTree` | Mutable fixed-size primitive point values and Fenwick partial sums in the isolated `m3-collection-port-wave1` receiver. Does not implement `java.util` collection APIs | [Complete implementation](../../m3/ports/collections/src/main/java/com/m3/ds/M3LongFenwickTree.java), [receiver README](../../m3/ports/collections/README.md), [public provenance rows](../../m3/ports/collections/PROVENANCE.tsv) |

The existing [porting invariant](../../m3/docs/m3jdk21-porting-invariant.md) and [convergence model](../../m3/docs/SYNEXIA_CONVERGENCE_MODEL.md) remain authoritative. Reusable implementation/recipe convergence remains with Synexia; this reference inspects only the public target. Historical mapping status and provenance do not establish a new target test pass or advance **String → arrays → collections → AST/compiler → remaining families**. Per-file Apache-2.0 notices remain scoped; OpenJDK and third-party licensing is unchanged.

## M3PrefixZ: exact UTF-16 facts

Let `n = value.length()`. At each offset `i`, `prefixLengthAt(i)` is the number of equal UTF-16 code units at the start of the whole input and the suffix beginning at `i`. It stops at the first unequal code unit or the end of that suffix. Thus `0 <= z[i] <= n-i`, and for a nonempty input `z[0] = n`.

This compares Java `char` units literally, including NUL, unpaired surrogates and surrogate pairs divided between leaves. It does not normalize, fold case, count code points, interpret regex or compute a cross-string similarity score. `similaritySum()` sums all these suffix-prefix lengths, including the self-suffix. A proper border is a prefix that is also a suffix but is shorter than the whole input.

For example, source reasoning gives `"ababa"` the lane `[5, 0, 3, 0, 1]`, sum `9` and longest proper border `3`. For an empty input all scalar facts are zero and no prefix offset is valid. These are explanatory examples, not new test receipts.

### Complete public surface

| Entry point | Input, result and refusal behavior |
| --- | --- |
| `analyze(M3StringPiece value)` | Delegates to the explicit overload with `Long.MAX_VALUE` as the metadata-payload budget and an always-false callback. Thread interruption still cancels. No overload accepting a `String`, arbitrary `CharSequence` or progress monitor exists |
| `analyze(value, long maxMetadataPayloadBytes, BooleanSupplier canceled)` | Requires nonnull value and callback, then a nonnegative budget. Reads `n`, rejects if `4L*n` exceeds the budget, polls cancellation and allocates one `int[n]`. Returns a newly computed immutable result on success. Nulls cause `NullPointerException`; negative/insufficient budget causes `IllegalArgumentException`. Allocation failure is not converted into a budget refusal |
| `requiredMetadataPayloadBytes(int codeUnits)` | Returns `4L * codeUnits`; a negative length throws `IllegalArgumentException`. Uses long multiplication: `Integer.MAX_VALUE` yields `8,589,934,588`, rather than wrapping an int. This is an estimator and does not allocate or promise that such an array can be created |
| `length()` | Returns the result lane length in UTF-16 code units |
| `prefixLengthAt(int offset)` | Reads one fact after `Objects.checkIndex(offset, n)`; valid offsets are `[0,n)`. Negative offsets and `n` throw `IndexOutOfBoundsException`. There is no empty-input offset-zero exception |
| `similaritySum()` | Returns the long sum of the entire lane, including `z[0]`. For repeated equal code units it is `n*(n+1)/2`; that mathematical maximum fits in long for every nonnegative int-sized `n`. The implementation accumulates in long without `addExact` |
| `longestProperBorderLength()` | Returns the greatest positive `z[i]` for a nonzero suffix offset where `z[i] == n-i`. Returns zero when no nonempty proper border exists, including length zero or one; never counts the whole value |
| `metadataPayloadBytes()` | Returns `requiredMetadataPayloadBytes(length())`, counting the retained fact lane only |

All result queries are constant-time array/scalar reads. The public API exposes neither the `int[]` nor a mutator, iterator, serializer or close operation.

### Budget, cancellation and progress

Validation order is observable: value null check, callback null check, negative budget, length-based budget refusal, then cancellation. A preexisting interrupt does not take precedence over an invalid argument or insufficient budget.

A cancellation poll evaluates `Thread.currentThread().isInterrupted() || canceled.getAsBoolean()`. Either true result throws `CancellationException`; the interruption flag is not cleared. An already interrupted thread short-circuits the callback. An exception thrown by the callback propagates without wrapping.

For nonempty input, polls occur before allocation, every 1,024 outer-loop indices, at attempted-comparison counts `0, 1024, 2048, ...` inside the equal-prefix scan, and after the work before returning. This includes cancellation during a long repeated-character scan. These are operation-count checkpoints, not a wall-clock deadline; a slow callback or owner access is not preempted. There are no `beginTask`, `worked` or `done` progress callbacks.

The empty-input branch returns immediately after allocating `int[0]`: it has the initial poll but **no final poll**. One-code-unit input has initial and final polls but no comparison-loop poll. Cancellation after allocation abandons the private temporary lane without returning a partial result; it does not change the input. Later result queries do not poll cancellation.

The budget checks only `Integer.BYTES * n`. It excludes result/object headers, array headers and alignment, existing text/backing storage, input descriptors and callback resources. A successful admission check is not a heap-availability guarantee, total-memory cap or CPU/work budget. The result retains one private `int[]`, a long sum and an int border; it retains neither input nor callback, and has no global cache.

### Existing text-owner dependency

The sealed [M3StringPiece interface](../../m3/core/src/com/m3/text/M3StringPiece.java) admits only [LocalM3StringPiece](../../m3/core/src/com/m3/text/LocalM3StringPiece.java) and package-private [JoinedM3StringPiece](../../m3/core/src/com/m3/text/JoinedM3StringPiece.java). The existing [text/storage reference](m3-packages.md#textstorage-api-contracts) covers the wider substrate; these are the dependencies relevant to analysis:

- `LocalM3Arena.copyUtf16` and `copyLatin1` admit private copies; later mutation of the caller array does not mutate an admitted view. [LocalM3Arena](../../m3/core/src/com/m3/text/LocalM3Arena.java) is an allocator, not an interner; [StorageIdentity](../../m3/core/src/com/m3/text/StorageIdentity.java) is descriptive ownership identity, not an address or an input to prefix equality
- A local range retains its whole backing, and reads LATIN1 bytes unsigned or UTF16_LE pairs. Prefix analysis calls only `length` and `charAt`; it neither copies those bytes nor releases that backing
- Joining snapshots the caller's directory, flattens nested leaf directories, omits empty leaves and uses checked cumulative int lengths. A joined `charAt` binary-searches cumulative ends; analysis does not flatten it into a String
- The Z-box algorithm makes O(n) code-unit comparisons and stores O(n) facts. With local constant-time reads this is O(n) work; with a flat joined directory of `s` leaves, the documented worst-case lookup bound is O(n log(s+1)). There is no measured throughput claim

The result's final private state supports immutable sharing after construction. Separate concurrent analyses have independent fact lanes; an application that shares a stateful cancellation callback must provide its own coordination. Neither analysis nor the result supplies a callback concurrency protocol.

## M3LongFenwickTree: fixed-size exact point sums

Public point indices are zero-based. Prefixes and ranges are half-open: `prefixSum(t)` describes `[0,t)` and `rangeSum(a,b)` describes `[a,b)`. Internally `tree[0]` is unused and Fenwick cell indices are one-based; callers must not add one to a point index.

Construction and mutation store exact signed-long point/cell values or throw on checked arithmetic overflow. Negative points are allowed for sums, `add` and `set`, but disallow `lowerBound`. The size cannot grow or shrink.

### Complete public surface

| Entry point | Input, result and refusal behavior |
| --- | --- |
| `M3LongFenwickTree(int size)` | Rejects negative size and `Integer.MAX_VALUE` with `IllegalArgumentException`; otherwise allocates zeroed points, cells and staging scratch. The advertised integer check is not a guarantee that all accepted huge sizes operate correctly; see the index-width gap below |
| `M3LongFenwickTree(long[] values)` | Null throws `NullPointerException`; length `Integer.MAX_VALUE` throws `IllegalArgumentException`. Clones the points, separately builds cells with checked additions and counts negative entries. Constructor arithmetic overflow throws `ArithmeticException` with no constructed owner returned. Requires the input remain stable during construction |
| `size()` | Returns fixed point count |
| `point(int index)` | Returns the point after `Objects.checkIndex(index, size())`; valid indices are `[0,n)`, otherwise `IndexOutOfBoundsException` |
| `total()` | Calls `prefixSum(n)` and inherits its overflow behavior; no separate cached total is retained |
| `payloadBytes()` | Exact byte count of the four retained primitive arrays: `8L * (points.length + (long) tree.length + stagedValues.length) + 4L * stagedIndexes.length`. Not whole-object heap size or an allocation limit |
| `prefixSum(int toExclusive)` | Requires `0 <= toExclusive <= n`, otherwise `IndexOutOfBoundsException`. Walks downward through Fenwick cells with `Math.addExact`; returns zero at endpoint zero. Intermediate arithmetic overflow throws `ArithmeticException` |
| `rangeSum(int fromInclusive, int toExclusive)` | Requires `0 <= fromInclusive <= toExclusive <= n`, otherwise `IndexOutOfBoundsException`. Computes `subtractExact(prefixSum(toExclusive), prefixSum(fromInclusive))`, in that evaluation order. Either prefix or the subtraction can overflow; even an empty range can throw if its prefix cannot be accumulated |
| `add(int index, long delta)` | Bounds-checks the point, checks `points[index] + delta`, stages checked updates for every affected cell, then publishes the point and staged cells. Overflow throws `ArithmeticException` before observable values change |
| `set(int index, long value)` | Bounds-checks the point, computes `subtractExact(value, points[index])`, stages the delta, then publishes. A representable replacement value may still be refused because the difference or an affected cell overflows |
| `lowerBound(long target)` | First rejects any negative point with `IllegalStateException`, including when `target <= 0`. With all points nonnegative: returns `0` if empty or target is nonpositive; otherwise computes `total()`, returns `n` if target exceeds total, or binary-lifts to the smallest zero-based index whose inclusive prefix is at least target. Exact-add overflow propagates; there is no approximate fallback |
| `clear()` | Fills points and cells with zero and resets the negative-point count. Preserves size, all array capacities and staging scratch; it is not a secure erase or release operation |

`size`, `point` and `payloadBytes` are O(1). Construction and clear are O(n); prefix/range/total/add/set/lowerBound are O(log(n+1)) for valid non-wrapping index paths. Empty-tree queries `total`, `prefixSum(0)` and `rangeSum(0,0)` return zero; `lowerBound` returns zero for every long target, serving both the start and `size()` sentinel. All point access/mutation indices are invalid when empty.

### Lower-bound examples and prerequisites

For points `[1,2,3,4,5]`, targets `1,2,3,4,15,16` select `0,1,1,2,4,5` respectively. The final `5` means no qualifying index. Zero-valued points are permitted: for `[0,0,3,0]`, positive target `1` selects index `2`, while target `0` returns `0`. For an all-zero nonempty tree, positive targets return `n`. The positive-vector cases appear in the read test; the zero-vector examples are source-derived, unexecuted illustrations.

Negative-value tracking changes on successful sign crossings in `set` or `add`; failed staging leaves it unchanged. Clearing restores lower-bound eligibility. Nonnegative points ensure monotone mathematical prefix sums, but do not ensure their total fits long; for a positive target, `lowerBound` computes the complete total before searching and can throw even if an early prefix would suffice.

### Checked arithmetic and failure atomicity

“Exact” means checked signed-long arithmetic, not arbitrary precision or saturation. The implementation checks intermediate representation values and traversal order as well as the answer:

- Building cells can fail even when the mathematical total would fit, for example `[Long.MAX_VALUE, 1, -1]`: the first two values overflow a stored cell
- A successfully constructed `[Long.MAX_VALUE, 0, 1]` has a representable point at index 2, but `rangeSum(2,3)` calls an overflowing complete prefix before subtracting; it cannot return the mathematically representable answer `1`
- Even a mathematically representable prefix may overflow during accumulation: `[-1,0,0,0,1,0,Long.MAX_VALUE]` stores representable cells, but `prefixSum(7)` adds the last cell and the preceding two-point cell before the negative four-point cell. `Long.MAX_VALUE + 1` throws before the later `-1` can cancel it
- On a one-point tree containing `Long.MIN_VALUE`, `set(0, Long.MAX_VALUE)` fails while computing the delta, even though the requested point itself is representable

These examples follow the read expressions and are **not runtime-validated counterexamples in this pass**. No rearrangement, wider accumulator or fallback is implemented.

`add` and `set` stage into owner-private `int[]` indices and `long[]` values before publishing. Arithmetic/bounds failures do not partially change the logical points, cells or negative-point count. Scratch slots already written may change on a failed operation; that is not observable through the public queries. A later successful operation uses only its freshly staged count. `clear` does not wipe old staging values.

Failure atomicity is a single-threaded failure contract, not a concurrent transaction or linearizability promise. The class has mutable arrays, a mutable negative counter and shared scratch, with no synchronization. Concurrent reads and writes, competing writers and clear require external synchronization for coherent behavior. There is no callback, cancellation token, interrupt polling, time/work budget or progress reporting; interrupting a thread does not request cooperative cancellation from this API.

### Allocation and retention

The owner retains `long[n]` points, `long[n+1]` cells, and two staging arrays of equal length `k = max(1, bitLength(max(1,n))) + 1`: `int[k]` indices and `long[k]` values. Its counted payload is consequently `16L*n + 8 + 12L*k` bytes. Empty construction still retains 32 payload bytes. Counts exclude the owner, array headers/alignment, scalar fields, caller input and VM/process overhead.

The values constructor clones point storage but reads the original `values` again while populating cells and the negative counter. Callers must not mutate that array during construction; cloning is not a coherent concurrent snapshot of all fields. Mutation after successful construction does not alias the stored arrays. Neither constructor exposes its arrays, and there is no capacity shrink, export-array or close method.

After construction, the successful ordinary method bodies need no new per-operation array, wrapper, stream or entry object. The fixed staging arrays are reused. This source observation is not a zero-allocation measurement: exceptional paths can create exceptions, and JVM behavior and total retained memory were not measured. Construction has no admission budget and can fail due to allocation limits.

### Source-observed index-width gap

The constructor guards allow sizes through `Integer.MAX_VALUE - 1`. However, the one-based parent/path step uses signed-int `i + (i & -i)` without a wrap guard. At `i = 1 << 30` that expression wraps to `Integer.MIN_VALUE`.

If enormous arrays were successfully allocated, the values constructor can then treat the negative parent as within its upper bound and index it, and an update path reaching that cell can continue to a negative index rather than terminate. The zero-filled size constructor itself has no parent-build loop. This is a source-observed gap for admitted sizes at or above `2^30`, not evidence that those allocations or failures were executed. Actual VM/heap limits may prevent construction much earlier. The documented logarithmic mutation behavior must not be read as a verified all-admitted-sizes guarantee.

## Tests, build selection and remaining evidence

### Read test bodies

| File and method | Assertions actually present | What this does not establish |
| --- | --- | --- |
| [PrefixZContractTest](../../m3/algorithms/test/PrefixZContractTest.java), `main`, `compare`, `view` | Independent naive String oracle for every prefix, long sum, border, payload and invalid offsets. Fixed empty/repeated/Unicode/NUL inputs, all cuts and subranges of those inputs, every UTF-16 unit in a five-unit pattern, 12,000 seeded random samples, Latin1 and copied-input mutation. Checks negative length/budgets, insufficient budget, nulls, cancellation, 100,000 equal units with sum `5,000,050,000`, preserved interrupt status, final primitive-only retained fields and 32 tasks on a four-thread executor | No new execution here; no exact empty-input poll-count assertion, callback-exception/short-circuit test, heap limit measurement, GC/reachability proof or whole-JDK compatibility result |
| [ViewInteropContractTest](../../m3/algorithms/test/ViewInteropContractTest.java), `main`, `matches` | Selected `chars`/`codePoints`, regex find/groups/offsets, matches, lookingAt, replacement, split and region-bound behavior on joined views against String across cuts | Does not call PrefixZ. Focused view interoperability is dependency evidence, not a PrefixZ feature or exhaustive regex conformance |
| [M3LongFenwickTreeTest](../../m3/ports/collections/src/test/java/com/m3/ds/M3LongFenwickTreeTest.java), `exactQueriesAndMutationsMatchTheDocumentedFenwickContract` | Five positive points, point/prefix/range/total, six lower-bound targets, successful add/set and clear | No randomized differential suite, empty/bounds/null/zero-target coverage or payload assertion in this file |
| Same Fenwick file, `lowerBoundRejectsNegativePoints` | Setting a point negative and rejecting a positive lower-bound query | Does not exercise negative targets, sign restoration or competing mutation |
| Same Fenwick file, `mutationOverflowIsFailureAtomic` | `[Long.MAX_VALUE,0]`, overflowing `add(1,1)`, unchanged points and total | Does not cover set-delta overflow, constructor/query intermediate overflow, negative underflow, scratch reuse after failure or huge index-path wrap |

The three test files were read completely. Test presence, assertion counts computed by a test, and historical pass text are distinct from running it at this source pin.

### Existing build routes are separate

- [Foundation build](../../m3/build.sh) builds the `com.m3.text` [module](../../m3/core/src/module-info.java) from `core/src` and its selected `core/test` files. It does not compile `algorithms/src` or invoke the two algorithm-directory tests. Its successful execution alone would not establish PrefixZ validation
- [Scoped migration runner](../../m3/migration/run-tests.py) selects the five [pinned P0 dependency files](../../m3/migration/pinned-dependencies.json), PrefixZ, both algorithm test classes, and exact-file recipe code/tests. It requires `java`, `javac` and `javap` on `PATH`, checks that `java -version` contains `version "21.`, and checks dependency hashes, then runs PrefixZ and view tests in normal and `-Xint` modes, plus recipe, Python-validator and String-surface steps. Its stated requirement is a stock Java 21 environment, but that version-string check does not verify the compiler version, vendor or unmodified-JDK provenance. It requires a new non-symlink output directory. Its historical baseline labels and scoped-classpath receipt are not whole-current-tree evidence
- The [migration POM](../../m3/migration/pom.xml) compiles its own `src` and `test` directories and binds `RecipeContractTest` at verify. Do not substitute `mvn -f m3/migration/pom.xml verify` for the scoped PrefixZ runner
- The [collection-port POM](../../m3/ports/collections/pom.xml) is an independent Java 21 Maven project, with compiler `3.13.0`, `-Xlint:all`, `-Werror`, Surefire `3.5.2`, `useModulePath=false`, and test dependency `org.junit.jupiter:junit-jupiter:5.11.4-m3-source-1`. The source-suffixed dependency is not stock `5.11.4`; standalone resolution was not tested here. The two OpenRewrite profiles have no default activation and are separate tooling paths
- The [collection provenance verifier](../../m3/ports/collections/verify_collection_port_wave1.py) reads provenance/recipe declarations, expected source-blob header strings, target packages and forbidden Synexia runtime imports. It does not compile/run the tree or recompute the complete target-body hash against a donor body. Its success would not substitute for Fenwick behavior tests

The [historical prefix acceptance](../../m3/migration/docs/ACCEPTANCE.md) and [resume guide](../../m3/migration/docs/RESUME.md) retain their original dates, revisions and limited proof scope. This reference neither refreshes those historical results nor claims their larger acceptance gates passed. Use the [contributor verification checklist](../../CONTRIBUTING.md#review-and-regression-evidence) for any later execution.

### Closure of this reference

The complete public surfaces of these two owners and their direct ownership/arithmetic/cancellation dependencies are explained here. Other algorithm and collection-port owners, inbound consumer integration, exhaustive test coverage, inline-source documentation changes, target compiler/runtime acceptance and resource measurements remain open. This is two-owner contract coverage, not two complete packages, a complete source family or promotion of an isolated receiver into the JDK.
