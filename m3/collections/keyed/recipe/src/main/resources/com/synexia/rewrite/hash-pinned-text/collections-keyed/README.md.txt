# M3 keyed execution: explicit semantics, reusable mechanisms

SPDX-License-Identifier: Apache-2.0

Candidate module for Java 21, with optional Linux JNI. Existing collections and
`stream()` / `parallelStream()` are unchanged. The public M3JDK21 receiver is emitted by the Maven/OpenRewrite recipe
`com.synexia.m3jdk.KeyedReceiver20261017`, reusing the same sealed Synexia
source crates and first-party donor atoms. A local replay is not OpenRewrite
SDK qualification: require the exact-head named-recipe and Java/JNI gates
before production promotion.

## Small API, explicit independence

```java
var customers = M3Parallel.by(orders, Order::customerId);
customers.forEach(this::applyOrder);
var invoices = customers.mapOrdered(this::renderInvoice);
var accepted = customers.filter(this::accept);
var totals = customers.foldByKey(() -> 0L, (sum, order) -> sum + order.cents());
var histories = customers.collectByKey(ArrayList<Order>::new, List::add);
customers.forEachPartition((customer, rows) -> processCustomer(customer, rows));
var firstOrders = customers.firstByKey();
var highest = customers.topByKey(3, Comparator.comparingLong(Order::cents).reversed());
```

The client supplies the key for this operation, not a permanent collection layout.
Equal keys execute serially in captured encounter order within one strict terminal
call. Different keys may overlap. Keys use ordinary equality and hashing, including
null. `byInt` accepts a declared dense nonnegative key space. Key objects, membership,
and all key-affecting state must remain stable; source references are captured, not
deep-copied. Concurrent source capture requires external exclusion.

Ordered results do not order cross-key effects. Customer partitioning does not protect
inventory shared between customers. Two terminal calls do not share a lock. Callbacks
must terminate, must not wait for their own key/pool, and must not escape asynchronous
work. Failure/cancellation is cooperative: started callbacks drain before return;
there is no rollback, retry, or deterministic winner among concurrent failures.

`using(ForkJoinPool)` borrows the pool, never shuts it down. `cancelWhen` requires a
thread-safe predicate. `keys()` retains first-key order; `explain()` reports the
captured size, key count, largest partition, backend and primitive index payload.
A hot key limits strict parallelism; scheduling cannot remove this constraint.

## Additional semantic capabilities

### Pure associative reduction, explicitly different from a strict fold

```java
var sums = customers.reduceByKey(() -> 0L, Order::cents, Long::sum);
var tuned = customers.reduceByKey(2048, () -> 0L, Order::cents, Long::sum);
```

`reduceByKey` alone permits concurrent callbacks WITHIN a key. Identity, lift and
combiner must be pure and independent. The identity must hold on both sides, and the
combiner must be associative for the actual value domain. Contiguous chunks merge
in encounter order, so commutativity is unnecessary. Floating-point addition is not
generally associative. The default 1024-row chunk is a documented initial policy,
not a measured crossover. Keep `foldByKey` when these laws do not hold.

### Stable representatives and top-k

`firstByKey()` selects the first original element per key, in first-key order.
Using the identity selector implements stable distinct. `topByKey(k,bestFirst)`
selects the best k per key; a negative comparator result means better. Ties retain
source order. k=0 returns an empty list for every key. The comparator must be pure,
consistent and thread-safe. Nulls depend on its policy. Result lists/maps are
read-only, but elements are not deep copies.

### Key-local neighborhoods and primitive rolling extrema

```java
var neighborhoods = customers.mapWindows(2, 1,
    (center, window) -> window.stream().mapToLong(Order::cents).sum());
long[] maxima = M3LongWindows.max(samples, 32);
long[] minima = M3LongWindows.min(samples, 32);
```

`mapWindows(preceding,following,mapper)` includes the center and clips at that key's
boundaries. Windows are read-only indexed views; output is restored to source
positions. The mapper must not mutate data read by another window. Retaining a view
retains the captured source. These are key-local windows, not global adjacent rows
or a streaming halo across batches.

`M3LongWindows.max` and `min` share an O(n) monotonic primitive ring deque with O(width) scratch
plus output. A positive width larger than the input yields a fresh empty array;
nonpositive width is invalid. It does not mutate input. Java is default; the explicit
`Backend.NATIVE` overload uses the same algorithm through JNI. No native speedup is
claimed. Minimum does not negate signed long values; Long.MIN_VALUE remains safe.
Long extremes, duplicate values and invalid dimensions are tested.

### Bounded, single-use batches

```java
long completed = M3Parallel.batches(iterator, 4096, Order::customerId)
    .using(pool).cancelWhen(cancelled::get).forEach(this::applyOrder);
```

One batch completes before the next is pulled, preserving per-key order across
batch boundaries. At most batchSize elements are prefetched; internal storage is
O(batchSize), excluding upstream buffering and callback retention. Configuration
views share single-use ownership. Iterator and pool remain caller-owned; no closing
is performed. Failure/cancellation drains current work without pulling another batch.
This is synchronous bounded traversal, not an asynchronous stream/backpressure API.

### Conservative multi-resource conflict components

```java
var transfers = M3Parallel.byKeys(events,
    event -> List.of(event.fromAccount(), event.toAccount()));
transfers.forEach(this::applyTransfer);
```

The selector must enumerate ALL touched resources, including reads that can conflict
with writes. Shared resources form transitive connected components; each component
runs serially in source order. Read/read sharing is also conservatively serialized.
Disjoint components may overlap. Empty sets are independent; a null set is invalid,
while null resource keys are supported. Result component keys are first source-row
indices, not resources or threads. This is not a dynamic read/write scheduler or a
cross-call coordination service; connected components may be large.

### Explicit read/write resource dependencies

```java
var work = M3Parallel.byReadWrite(events, Event::readResources, Event::writeResources,
    new M3ReadWrite.Budget(100_000, 500_000, 1_000_000));
work.using(pool).forEach(this::apply);
var results = work.mapOrdered(this::compute);
```

This separate API does NOT change `byKeys`. Read/write and write/write conflicts
retain captured encounter order. Read/read sharing may overlap. Every resource set
must be complete and stable while captured; equality/hashing have the same requirements
as keyed plans. Writes dominate same-row reads; duplicate keys do not duplicate graph
edges. Empty resource sets are independent, null sets invalid, null keys supported.

A linear source pass records the last writer and outstanding readers of each resource.
It constructs only forward dependencies and deduplicates predecessor/row pairs. Runtime
uses a primitive ready queue and publishes successors when their prerequisites COMPLETE,
not when they are dispatched. Unrelated slow work does not impose a global wave barrier.
Monitor handoff publishes prior callback effects to dependent callbacks. The callback
must finish its effects before returning. Read callbacks must not perform undeclared writes.

Admission limits bound captured rows, resource touches (including duplicates), and
unique dependencies before execution. Planning is expected O(rows+touches+edges), apart
from ordinary hash-collision costs. The retained graph payload is 8*(rows+edges) bytes;
source references, boxed resource IDs during capture, headers, allocator slack and
execution scratch are excluded. It is not a total-process memory estimate. The overload
without a budget uses representation ceilings only; use explicit limits in applications.

The existing bounded worker/drain atom is shared with strict keyed execution. One
reusable ManagedBlocker per worker handles internal dependency waits. A polling deadline
allows prompt cancellation checks outside internal monitors; it is not a hard latency
bound. It does not make arbitrary blocking callbacks safe. Failure/cancellation stops
new useful work and drains started callbacks. Plans remain per-execution coordination,
not a shared lock service across calls.

### Asynchronous terminal lifecycle, not escaped asynchronous callbacks

```java
var execution = customers.forEachAsync(coordinator, this::applyOrder);
execution.requestCancellation(); // requests stopping; does not claim work has stopped
execution.completion().toCompletableFuture().join(); // completion follows worker drain
var mapped = customers.mapOrderedAsync(coordinator, this::renderInvoice);
var rwExecution = work.forEachAsync(coordinator, this::apply);
```

`M3Execution` owns neither the coordinator nor worker pool. Both must remain live.
Submission rejection is synchronous. Cancellation before queued start prevents all
callbacks; during execution it is cooperative. The first request accepted before the
terminal completion commit returns true. Completion is published only after the terminal
operation has returned/drained. Concurrent callback failure takes precedence over a
cancellation request; there is no deterministic ordering among independent failures.

The returned minimal CompletionStage cannot be used to complete/cancel the underlying
execution by mutating its `toCompletableFuture()` copy. Cancellation may complete the
stage exceptionally even though some effects already happened. No interruption, timeout,
retry or rollback is implied. A direct Executor may run the operation before submission
returns; this API borrows the supplied Executor semantics. User callbacks still cannot
escape work or wait on their own operation. Continuations must obey executor-lifecycle
and blocking rules as well.

### Explicit memory-pressure-aware retention

```java
var cache = new M3PlanCache<PlanKey, M3Parallel.Plan<Order, Long>>(128, 64L << 20);
cache.put(planKey, customers, estimatedRetainedWeight);
var retained = cache.get(planKey); // access-order promotion
cache.trimTo(16L << 20);          // evict LRU ownership down to a requested weight
cache.clear();                   // also drops this cache's metadata capacity
```

`PlanKey` is a client-defined immutable identity including all semantic discriminators:
source identity/version, key/equality rules, algorithm, backend and configuration where
relevant. The cache does not discover those or make mutable sources safe. It has no
loader; validate freshness before reuse, using the existing version guard where suitable.
The example's key type must match the actual client key type.

Both entry count and caller-declared nonnegative weight are enforced. Weights are
retention estimates, not JVM-measured heap usage; metadata/external references are not
charged automatically. Zero-weight entries remain count-bounded. Oversized replacement
and invalid input are refused before mutation. Eviction releases cache ownership, never
closes values still held elsewhere. `trimTo(0)` can retain zero-weight values; `clear`
releases all cache ownership. Normal key hash/equality calls run under the monitor and
must be stable, non-reentrant and well-behaved. No loader/removal listener runs there.

This owner specializes the existing Synexia PackedHashMap.Table / M3JDK21
M3LinkedHashMap.Table atoms in a private cache implementation. Six method bodies are
checked against pinned donor snapshots (visibility/whitespace only). The cache now
stores keys and actual values in two reference lanes, with primitive weights, hashes,
index positions and LRU links. Released slots reuse their `after` links as the free list.
There is no retained boxed slot-ID lane, map adapter, sequenced view or separate free-link
array. Null keys, value identity, weighted/count admission, recency and synchronized
ownership are unchanged; key/value references remain GC-traced, not native handles.

This is source-level specialization, not a promise about JVM/JIT machine-code inlining.
Prepared replacement/lookup allocation is measured separately by the optional
`M3CacheAllocationProbe`; it is not a throughput benchmark or universal no-allocation
claim. Growth, rehashing, callers and statistics can allocate. No automatic native
resource accounting or atomic rollback after allocation failure is promised.

### Comparator ranges and sorted merge joins

`M3Parallel.byRange(source,key,cuts,order)` accepts strictly increasing cuts.
Equality with a cut belongs to the range on its right. Source order is retained;
this operation does not sort. Nulls require an explicit supporting comparator.

```java
var join = M3SortedJoin.by(left, Left::key, right, Right::key, comparator);
long pairs = join.count();
join.forEach(1_000_000L, this::acceptPair);
```

Both full inputs are validated sorted before callbacks. Comparator equality defines
matches. Duplicate runs yield the Cartesian product in key/left/right encounter
order. Source/key references are captured, not deep copies. A long result budget is
checked BEFORE effects. Pairs are streamed serially; result-sized pair objects are
not allocated. Input references/range metadata are retained, so this is not a
bounded-input streaming join. Inputs and key-affecting values must stay stable.

### Guarded plan reuse

`byVersioned(source,key,versionSupplier)` validates capture, traversal and completion.
The version must cover membership AND all key-affecting values, must not exhibit
ABA, and must be safely readable by worker threads. External exclusion of mutation
is still required. This detects stale reuse; it is not a lock, transactional snapshot,
general cache service, or rollback of already executed callbacks.

## Representation and replaceable atoms

Existing `com.m3.collections.M3LinkedHashMap` supplies the key dictionary. No new map
owner is introduced. One packed int[] stores offsets and stable source-row IDs:
`4 * (elementCount + keyCount + 1)` payload bytes, excluding all references/headers,
outputs and construction temporaries. At most min(work items,pool parallelism)
worker tasks claim partitions/chunks; no task per row or per key is enqueued. The
claim counter saturates instead of overflowing at Integer.MAX_VALUE.

Top-k retains a primitive row-index heap of min(k,group size) per active group, with
O(n log(k+1) + sum(selected log(selected+1))) time. Strict folds reuse the scheduler.
Hot-key reduction retains one partial reference per chunk and merges deterministically.
Conflict discovery uses union-by-size/path compression and the existing map; hash
collision worst cases remain those of the map. Range selection is binary search.
Sorted join scans inputs/runs; callback time is necessarily proportional to pair count.
Generic window cost depends on the mapper; overlapping naive sums are not called O(n).

JNI specializes dense stable scatter and primitive sliding extrema only. It uses
checked dimensions, array-region copies and RAII buffers, no Java callbacks, critical
pins, retained handles, or automatic library loading during Java-only operations.
M3 callbacks and scheduling stay in Java. Native load failure does not disable Java.

## Build, proof and provenance

After actual recipe emission and a qualified local `com.m3:m3-collections` dependency:

```sh
mvn -B -f m3/collections/keyed/pom.xml verify
mvn -B -f m3/collections/keyed/pom.xml -Pnative verify
```

The native profile is Linux JNI with C++17 and UBSan. Windows/macOS and an installed
JDK replacement are not qualified. No GPU implementation, arbitrary blocking/escaped
async callbacks, retry/transactional effects, cross-call lock service, automatic cache
validity inference, outer/unsorted join, or streaming cross-batch halo is claimed.

`provenance.json` records exact dependency/donor identities. Synexia's crate catalogue
separately records GitHub evaluations and LeetCode/HackerRank/GeeksforGeeks category
references. External challenge/editorial code is not copied or relicensed. Guava
TopKSelector was reviewed, not copied: its unstable tie contract does not match this
API. Original first-party additions are Apache-2.0; upstream identities are retained.

Candidate inputs are SHA-256 sealed. The existing Java/text OpenRewrite engines must
emit exact outputs, replay to a fixed point, and pass drift/duplicate/non-AST/path
refusal tests. The post-test Maven verify application step validates the full passing Surefire
inventory, reruns actual recipe emission/fixed point, and preflights every path and
receipt in an exclusively owned workspace; serial writes are not a filesystem transaction.
Always use clean verify in a fresh proof workspace. The XML report is trusted local build
evidence, not a cryptographic attestation against forged or stale reports. ABSENT/exact-final
admission deliberately refuses old or edited candidates. Never patch emitted output.
