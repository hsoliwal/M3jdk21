<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Tree collections and synchronous lazy values

[Guide](README.md) · [Package map](m3-packages.md#package-3) · [Coverage](coverage.md)

This reference covers `M3TreeMap`, `M3TreeSet`, `M3LazyValueProvider`, the
package-private `M3LazyValueState`, and the seven lazy event/operation owners in
`com.m3.collections`. It specifies the inspected implementation, including error
and partial-effect boundaries. It does not qualify all collection families or
install an M3 backend into `java.util`.

Source pin: `1011050da4205a901ad079d7711b6b8efd8a9af7`, inspected 14 October 2026.
All eleven primary production bodies, every declared member, and the named test
bodies below were read. The full 77-file production-package listing was compared
with `7abdf87cb497d0d94abeb723d70c50f856ecc381`: every package blob is unchanged.
**Tests and examples were not compiled or run.** No Maven, recipe, receiver
verifier, native build, jtreg, benchmark or runtime qualification was executed.
Costs below are source-derived, not measurements.

## Module, entry points and comparison

The [module descriptor][module] exports `com.m3.collections`, with only implicit
`java.base` at runtime. The [module POM][pom] targets Java 21 and treats Java lint
warnings other than module-name style as errors. The [control reactor][reactor]
includes collections through `m3-extended`; the [module guide][module-guide]
describes the separate build and distinguishes historical installer evidence from
current receiver qualification. Those historical results are not new test results
from this reference. Factory routing is covered in
[factory and delivery scope](reconcile-factories-and-delivery-scope.md).

| Entry | Result and constraints |
| --- | --- |
| `new M3TreeMap<>()`; `new M3TreeMap<>(comparator)` | Eager, reference-valued AVL map. Null comparator means natural ordering |
| `new M3TreeMap<>(comparator, provider)` | Lazy-capable AVL map; null provider throws `NullPointerException`. Keys are admitted explicitly with `defer`, or with an already-loaded value through `put` |
| `M3Collections.treeMap()` / `treeMap(comparator)` | Eager map constructors; no provider factory overload is declared |
| `new M3TreeSet<>()`; comparator constructor; corresponding `treeSet` factories | Standalone key-only AVL set, with insertion enabled; no dummy-value population or provider |
| Map `keySet`, `navigableKeySet`, `sequencedKeySet`, `descendingKeySet` | Backed `M3TreeSet` facade with insertion disabled; it may refer to a lazy map |

The [tree][tree] identifies a key by comparison result zero, not by `equals` or
reference identity. Updating with a comparator-equivalent key preserves the
original stored key reference. The lazy provider receives that canonical stored
key, even when the query used a different equivalent object. Comparator ordering
must remain consistent while keys are stored; changing key fields or comparator
state can invalidate ordering. Inconsistent-with-`equals` ordering also has the
usual consequences for equality/hash interoperability with other maps and sets.

Lookup and write validation call `compare(key,key)` even on an empty tree. Natural
ordering uses `Objects.requireNonNull(key)` and casts to `Comparable`; null keys
throw `NullPointerException`, and incompatible keys can throw `ClassCastException`.
A supplied comparator determines accepted types and null handling, including
`compare(null,null)`. Comparator exceptions propagate. These calls may execute
caller code; comparator mutation/reentrancy is not made safe by the lazy-provider
guard. Null values are allowed, including explicit null and successfully loaded
null.

## Navigation and nested live ranges

A facade holds the shared private `Tree`, optional ascending low/high bounds,
inclusivity flags, and a descending flag. View creation does not copy mappings.
Changes through any facade are visible through the others when within their
bounds. `descendingMap` and `reversed` each allocate a new facade; double reversal
restores ordering, not object identity. The descending comparator is
`Collections.reverseOrder` of the underlying comparator; an ascending
natural-order map reports null.

| Operation | Ordering, result and errors |
| --- | --- |
| `firstKey`, `lastKey` | Endpoints in this facade's encounter order; empty throws `NoSuchElementException` |
| `lowerKey`, `floorKey`, `ceilingKey`, `higherKey` | Strictly less, less-or-equal, greater-or-equal, strictly greater under the receiving view's order; null if no visible result |
| Corresponding `*Entry`, plus `firstEntry`, `lastEntry` | Same navigation, but resolve the value and create an immutable entry snapshot; null if no result |
| `pollFirstEntry`, `pollLastEntry` | Snapshot the value first, then erase; provider/observer failure before erasure can leave the mapping present |
| `subMap(from,fi,to,ti)` | Endpoints must be valid in the parent range, and ordered in the receiving comparator; inverted range throws `IllegalArgumentException` |
| `headMap(to,inclusive)`, `tailMap(from,inclusive)` | Narrow the appropriate end in the receiving order. In a descending view, head raises the ascending lower bound and tail lowers the ascending upper bound |
| Two-key `subMap`, one-key `headMap`, `tailMap` | Respectively `[from,to)`, exclusive head, inclusive tail in the receiving order |
| `put`, `defer` outside the view | `IllegalArgumentException`, after key validation; they cannot expand the parent's range |
| `get`, membership, removal, state lookup outside the view | Treat as absent after key validation; they do not search beyond the bound |

A nested endpoint equal to an excluded parent endpoint may itself be excluded,
but cannot be included. Endpoints beyond either parent bound are rejected even
when the resulting range would be empty. Equal from/to endpoints are permitted;
both inclusive can select a single comparator-equivalence class, while an
exclusive endpoint makes that equal-bound interval empty. Range validation uses
comparisons, not the current presence of those endpoint keys.

Null-returning navigation is ambiguous when the comparator permits a real null
key. Use entry-returning APIs or membership/emptiness as appropriate; note that
entry-returning APIs can load values. `putFirst`/`putLast` inherit the sorted-map
rejection of explicit positioning and throw `UnsupportedOperationException`.

Unbounded `size` reads the shared count. Bounded `size` traverses visible slots;
`isEmpty` locates the first visible slot. Even an empty-created range stays backed
across first insertion, clearing and reuse. No range caches its own contents or
has an independent structural revision.

## Sets, entries and traversal

[Standalone sets][set] implement membership, add/remove, clear, navigation,
sub/head/tail sets and descending views by delegating to a key-only tree. Duplicate
`add` returns false; new add returns true. Bounds and comparator rules are the
map's rules. A map key view rejects `add` with `UnsupportedOperationException`,
including an already-present key. Set subviews preserve the original insertion
permission. Inherited bulk operations use the ordinary collection algorithms,
so an empty `addAll` need not attempt an unsupported add.

`first`/`last` and inherited `getFirst`/`getLast` throw on empty. `pollFirst` and
`pollLast` return null on empty and erase a present key without resolving its
value. Inherited `NavigableSet.removeFirst`/`removeLast` first reject empty, then
use those polls. Inherited `reversed` calls `descendingSet`. `addFirst`/`addLast`
are unsupported sorted-set positioning operations. These inherited defaults were
checked in the pinned [sorted-set][sorted-set] and [navigable-set][navigable-set]
sources.

**A key-view removal is not uniformly value-free.** `keySet().remove(key)` and
key iterator `remove()` call `eraseAt`, which resolves the old value on a lazy
map. Key-view polls and clear bypass value resolution. Standalone sets never have
a provider, so this distinction only affects key views of lazy maps.

[Shared slot views](collections-reference.md#views-iterators-and-entry-lifetimes)
provide the values and entry APIs; the tree supplies ordered slot traversal.

| Projection | Connection and lazy boundary |
| --- | --- |
| Key iterator | Returns canonical key references without loading values |
| Value iterator | Resolves each returned slot's value |
| Entry iterator, sequenced entry `getFirst/getLast` | Fresh live entry storing its key and facade; creation/getKey does not load. `getValue`, `setValue`, hash, equality and rendering can resolve values |
| Navigational `*Entry`, `firstEntry/lastEntry`, entry polls | Fresh `SimpleImmutableEntry` with resolved references; `setValue` unsupported |
| Sequenced entry `removeFirst/removeLast` | Detached mutable `SimpleEntry`; later `setValue` changes only the entry. Removal still resolves the mapping before erasing |

Live entries find their stored key again on every value access. Absent key means
`getValue` returns null and `setValue` throws `IllegalStateException`. Removal and
reinsertion of a comparator-equivalent key can make an old entry resolve the new
mapping; it is not a generation-tagged row handle. Neither entry snapshots nor
live projections clone payload objects. Retained views/entries can keep the
shared tree and its other mappings reachable.

Iterators capture the shared revision, checking in `hasNext`, `next` through
`hasNext`, and `remove`. A successful iterator remove updates its expected
revision; a second remove without next throws `IllegalStateException`, and
exhausted next throws `NoSuchElementException`. Insertions, removals and nonempty
clear change the revision. Value replacement and lazy state/value publication do
not, so loading during key iteration need not invalidate the iterator. For
value-iterator exceptions, the cursor is advanced before `valueAt`; a failed
next is not a promise that retrying the iterator will revisit that slot.

Tree/set and map-key spliterators use the sorted collection-based helper, with
`ORDERED`, `SORTED`, `DISTINCT`, `SIZED` and `SUBSIZED`, preserving the current
comparator through splits. Values and entry views use the ordered helper;
entries are distinct but neither view declares sorted. Binding occurs on first
traversal, split or size query. Splits can batch into arrays; this is not zero-copy
partitioning of tree lanes. Array-backed child splits do not retain the original
iterator's revision checks. `toArray` materializes references, and entry arrays
hold live projections. See the [shared materialization reference](collections-reference.md#spliterators-streams-and-arrays)
for inherited array contracts. These traversal facilities do not permit
concurrent mutation, including concurrent lazy loading.

## Inherited map operations and value materialization

`M3SlotMap` and `M3OrderedSlotMap` implement the ordinary map/callback surfaces.
Their [full reference](collections-reference.md#map-reads-writes-equality-and-nulls)
applies with tree lookup instead of hash lookup and with the following lazy
resolution effects. `get` returning null alone cannot distinguish absence from
a present loaded-null value; use `containsKey` or `valueFlags`.

| Operation | Lazy effect and partial mutation boundary |
| --- | --- |
| `size`, `isEmpty`, `containsKey`, key navigation/iteration, `valueFlags`, `isValueLoaded` | No value load; comparator work can still run |
| `get`, `getOrDefault`, `touch`, value traversal, live `getValue`, snapshots | Resolve an admitted visible slot; absent key stays absent and does not call the provider. Present loaded null defeats `getOrDefault`'s fallback |
| `containsValue`, equality, hash, `toString`, `forEach` | May resolve many values in encounter order. Equality short-circuits identity/size and catches `ClassCastException`/`NullPointerException` as false; other failures propagate |
| `put` of an absent key | Insert explicit loaded value, including null, without calling the provider |
| `put`/`replace`/live `setValue` of a present key | Resolve old value before replacing it. A failing resolution can leave the mapping present and the new value unapplied |
| `remove(key)`, conditional remove, iterator removal, ordinary key-view remove | Resolve old value before erasing. Conditional removal compares against the resolved value and may access it again when erasing |
| `putIfAbsent` | Resolve a present slot; replace only if resolved value is null. A deferred mapping is not automatically treated as absent |
| `computeIfAbsent`, `computeIfPresent`, `compute`, `merge` | Resolve the old value where the shared algorithm needs it. Present null follows ordinary map-null rules. Provider resolution is distinct from the caller's remapping function |
| `replaceAll`, entry/value view matching/removal | Can resolve values and invoke equality/remapping callbacks; earlier successful loads or writes survive later failure |
| `clear`, including bounded clear | Erase without resolving values; clears only this view's mappings |
| `putAll` | `source.forEach(this::put)` unless exact self; source lazy values may load and target replacements may load. No atomic copy/rollback or alias snapshot |

Some shared algorithms call `valueAt` more than once for one logical operation.
A loaded slot avoids another provider `load`, but can still produce cache-hit
observations and monitor callbacks. Do not infer one event sequence per high-level
map method or use observer invocation count as a provider-call count.

Null remapping/actions are rejected; `merge` also rejects its supplied null value.
Returning null from compute/merge generally removes a present mapping, while
`computeIfAbsent` leaves an existing loaded-null mapping in place when its
function returns null. `replaceAll` may store null. The shared code checks the
structural revision after returning callbacks before applying that callback's
result. Existing callback side effects and earlier updates are not rolled back;
a throwing callback can escape before that check. Nonstructural value changes
are not detected by the revision. Lazy provider restrictions below are stricter
than this general callback fail-fast mechanism.

## Lazy state, retry and reentrancy

The provider is a synchronous caller-owned callback. There is no executor,
background load, future, timeout, cache eviction or invalidation API here. It gets
the canonical stored key and a non-null legacy `M3Progress`; provider code should
check cancellation during long work. Its value lives only in the tree's values
lane. The [state helper][state] stores one short per allocated slot and an active
callback count, not a second value cache or retained exception.

| Flag | Meaning and transitions |
| --- | --- |
| `ABSENT = -1` | Query result for no mapping in this facade, including outside bounds; never a stored flag |
| `UNLOADED = 0` | New deferred slot; erased/free slots are also reset to zero but are not present |
| `LOADING = 1` | A resolution attempt has started; same-slot read reaching this state throws `IllegalStateException` |
| `LOADED = 2` | Explicit value or successfully published provider result, including null; subsequent value reads use it |
| `FAILED = 4` | Attempt failed before publication; values lane is null, key remains present, next value read can retry |

`defer` requires the provider constructor; otherwise it throws
`IllegalStateException`. It never unloads an existing mapping, never replaces its
key and returns false without changing its state. New defer adds a structural
mapping, returning true. `valueFlags` and `isValueLoaded` do not resolve it; every
present eager-map value is reported loaded. State codes are exact alternatives,
not a public promise that combinations of the constants are valid slot states.

A nonloaded read changes state to loading, invokes the provider, checks
cancellation again, stores the result, and publishes loaded. Runtime exceptions
and errors before publication clear the value and mark failed. Cancellation
after provider return but before publication also discards that result and marks
failed. Pre-cancellation before the loading transition leaves the old state as
it was. No Throwable is retained in the tree for later replay; a retry invokes the
provider again. Removing and re-admitting a key likewise allows a new load.

A value published successfully stays loaded if a subsequent `STATE_CHANGED`,
`AFTER_LOAD`, `AFTER_TOUCH` or successful `PROVIDER_END` observer throws. The
operation can therefore throw even though provider work is cached. A later read
can return that cached value without repeating `load`. A replacement/removal
waiting for the old value may still be unapplied when such an observer throws.
Observers that throw `CancellationException` after publication do not undo that
publication either. This is a state-coherence rule, not transactional success.

`read` holds the `activeCalls` guard across event-policy resolution, scope,
provider, cancellation checks and observations. `eventPolicy` and `emit` also
hold the guard when used independently by preload. Mutations reaching
`writable` while this count is nonzero throw `IllegalStateException`, including
mutations through other views, value writes and storage growth/erase/clear.
A no-op operation that never reaches a write need not throw. The count is a
plain field; this is a same-thread callback discipline, not a lock.

Different-key reads are allowed. A provider cycle returning to a slot already
loading fails; uncaught failure unwinds participating attempts into failed state.
There is no general recursion guard for arbitrary event-policy/sink callbacks:
for example, same-key recursion from `BEFORE_TOUCH` occurs before setting loading,
and cache-hit observers can recursively read a loaded key. Keep configuration and
observers nonrecursive and free of mapping mutation. General comparator, equality
and remapping callbacks are not all enclosed by this lazy-state guard.

## Touch, preload and progress boundaries

`touch(key)`, `touch(key,monitor)`, and `touch(key,policy,monitor)` select operation
`TOUCH`. They first resolve a progress scope and check cancellation, then find the
slot. An absent or out-of-view key returns null without provider events, although
scope/cancellation work has already happened. Pre-canceled touch throws before
slot lookup and does not emit the state helper's `CANCELLED` event. Ordinary value
access selects `READ` and reaches the helper's cancellation/event path directly.

`preload()`, `preload(monitor)`, and `preload(policy,monitor)` select `PRELOAD` and
traverse only the receiving view, including descending order. Already-loaded
slots count as completed visits but do not call `load`. Empty/eager views still
use progress; eager trees emit no lazy-provider events.

1. Capture the shared revision and count visible work; resolve a scope weighted
   by `max(1, work)` when no monitor is supplied.
2. Resolve the provider policy and emit `RANGE_BEGIN`, if lazy.
3. Call `beginTask("Load packed tree values", work)` and check the revision.
4. For each visible slot: check cancellation, resolve its value, check revision,
   call `worked(1)`, emit element `PROGRESS` with the current state, check revision.
5. Check cancellation once more, then call `done` on the successful path.
6. After entry to the main try block, emit `RANGE_END` in finally, with the caught
   runtime exception/error or null. Scope close then follows normal Java
   try-with-resources behavior.

Successful loads survive later failure/cancellation; no rollback or clear occurs.
The failing value visit does not reach the loop's `worked(1)`, but provider code
can report work itself, and a failure in worked/progress/done happens after earlier
load effects. `RANGE_BEGIN` is outside the main try/finally: if it throws,
`RANGE_END` is not emitted. `RANGE_END` uses ordinary emit rather than the
failure-preserving helper, so a throwing range-end observer can replace an
original preload failure. The observer receives that original failure parameter.

Outer preload `beginTask`, `isCanceled`, `worked` and `done` callbacks are outside
the lazy value-read guard. Revision checks diagnose certain structural changes
after callbacks return; they do not roll them back or guarantee detection before
a slot is used. For example, worked is followed by progress dispatch before its
revision check, and outer cancellation checking precedes value access without an
intervening revision check. There is no revision check after final `done`.
Callers must not use those callbacks to mutate the traversed map. The regression
fixture covers mutation from worked, not every hostile callback boundary.

An explicit monitor is borrowed: scope close does not complete it. `touch` itself
does not call begin/worked/done on that borrowed monitor; provider code may. A
successful preload does call done explicitly; a failed preload normally skips it.
With null monitor, the owned system-child scope is closed on success **and failure**,
and close calls done, potentially crediting its full weight to the system root
(subject to the progress owner's generation/completion rules). Completion of that
scope is not evidence that every value loaded successfully. Passing
`M3Progress.NULL` explicitly selects the no-op monitor instead of the system
fallback. See [progress scope ownership](specify-progress-ownership.md#scope-and-system-root)
for the full progress contract and cancellation propagation.

## Event policy, dispatch and vocabulary

`M3LazyEventSource<M>` defaults to policy `NONE` and the shared no-op sink.
The provider specializes its monitor type to `M3Progress`; the generic event SPI
otherwise imposes no monitor class. `M3LazyEventSink.onEvent` receives event bit,
long slot, old/new short states, operation, monitor and optional failure. There
is no allocated event-record argument. `NO_SLOT = -1` is used for range events;
real slot numbers can be recycled and are not persistent handles or key IDs.
The sink receives no key/value parameter and must not treat a slot as an index
into an externally exposed tree array.

### Selection and overlay

`M3LazyEventPolicy` is an immutable record of `includeMask`, `excludeMask`,
`watchedStateMask` and non-null `granularity`. Construction rejects event bits
outside `M3LazyEventBits.ALL` in either mask. The watched-state short is not
validated against the provider's state constants. `of(mask,granularity)` uses
zero exclusion/watch masks; `NONE` uses zero masks and granularity none.

- `effectiveMask = includeMask & ~excludeMask`: exclusions win.
- `base.overlay(null)` returns the same base. Otherwise includes are ORed,
  exclusions are ORed, zero override watch mask inherits the base watch mask,
  and override granularity replaces the base unless it is `INHERIT`.
- The overlay cannot clear a base exclusion, and zero cannot clear a base watched
  mask. An override `NONE` disables via granularity; it does not erase base masks.
- Enabled events need a mask intersection and adequate granularity. Rank is
  `OPERATION < RANGE < ELEMENT < STATE_TRANSITION`; `NONE` and unresolved
  `INHERIT` include nothing. This is an observation threshold, not a batching rule.
- For the single event `STATE_CHANGED`, a nonzero watched mask selects bits that
  changed: `(oldState ^ newState) & watchedStateMask != 0`, treating shorts as
  unsigned. It is not a test that the destination state belongs to a set.

`M3LazyEvents.emit` requires a non-null policy before checking selection. If
disabled, it returns without checking sink or operation. If enabled, it requires
both and calls the sink synchronously. It does not validate slot/state values,
monitor, failure, or enforce that the event argument is one known single bit.
Null required granularity can fail when rank evaluation is reached; callers must
supply a real granularity. Sink exceptions propagate unless a specific caller
uses `emitFailure` below.

The tree's helper resolves `provider.lazyEventPolicy().overlay(callPolicy)` per
value access, including cache hits. Preload resolves once for range/progress and
again for each value access, so a changing provider policy may yield different
selection inside one preload. Returning null provider policy fails. The helper
retrieves `provider.lazyEventSink()` before each dispatch helper call, **even for
disabled events**; a getter that throws still affects the operation. Returning
null sink is tolerated only on a disabled dispatch. No callback registration
list, asynchronous queue or guaranteed observer isolation is supplied.

### Declared bits versus tree emission

| Bit position / name | Minimum granularity used by this tree |
| --- | --- |
| 0 `BEFORE_TOUCH`; 1 `AFTER_TOUCH`; 2 `CACHE_HIT`; 3 `CACHE_MISS` | `ELEMENT` |
| 4 `BEFORE_LOAD`; 5 `AFTER_LOAD`; 6 `LOAD_FAILED`; 7 `CANCELLED` | `OPERATION` |
| 8 `STATE_CHANGED` | `STATE_TRANSITION` |
| 9 `INVALIDATED` | Declared, not emitted by this tree |
| 10 `RANGE_BEGIN`; 11 `RANGE_END` | `RANGE` |
| 12 `PROVIDER_BEGIN`; 13 `PROVIDER_END` | `OPERATION` |
| 14 `PROGRESS` | `ELEMENT`, emitted per completed preload visit |
| `NONE = 0`; `ALL = (1L << 15) - 1` | Mask constants, not individual event occurrences |

`M3LazyOperation` declares `READ`, `TOUCH`, `LOAD`, `PRELOAD`, `RENDER`, `SELECT`,
`EXPAND`, `EDIT`, `NAVIGATE`, `PREFETCH`, `API_OBJECT`, `PAGE`, `CHILD_PROBE`,
`ROOT_COUNT`, `ROOT_KEY`, `CHILD_COUNT`, `CHILD_KEY`, `INVALIDATE`. This tree
selects only `READ`, `TOUCH` and `PRELOAD`; the other names do not implement UI,
page-loading or invalidation features. Direct put, defer, erase and clear do not
emit a state-transition/invalidation event merely because flags change.

### Ordering and exception precedence

These are source-order dispatch opportunities; filtering can remove any event,
and an exception or reentrant read can interrupt/interleave the sequence.

| Path | Dispatch order |
| --- | --- |
| Loaded read | `BEFORE_TOUCH`, cancellation check, `CACHE_HIT`, `AFTER_TOUCH`; no provider begin/end |
| Successful unloaded/failed attempt | `BEFORE_TOUCH`, cancellation check, set loading, `CACHE_MISS`, `STATE_CHANGED(old,loading)`, `BEFORE_LOAD`, `PROVIDER_BEGIN`, provider call, cancellation check, publish value/loaded, `STATE_CHANGED(loading,loaded)`, `AFTER_LOAD`, `AFTER_TOUCH`, finally `PROVIDER_END` |
| Runtime exception/error before publication inside attempt | Set failed and clear value, failure-preserving `STATE_CHANGED` then `LOAD_FAILED`, finally failure-preserving `PROVIDER_END`; throw original |
| Cancellation inside attempt before publication | Set failed and clear value, failure-preserving `STATE_CHANGED`, `CANCELLED`, finally `PROVIDER_END`; no `LOAD_FAILED` |
| Helper pre-cancellation before attempt | `BEFORE_TOUCH`, then failure-preserving `CANCELLED`; state unchanged, no provider begin/end |

`BEFORE_TOUCH` is outside the helper's inner exception-handling block. Its failure
leaves the state unchanged and has no automatic failure/end event. Policy/scope
setup failures occur even earlier. Once state becomes loading, an observer
failure before the provider call is still an attempt failure: it can produce
`LOAD_FAILED` and `PROVIDER_END` even though `load` was never entered. A same-slot
loading check itself precedes the inner attempt; the enclosing provider attempt
handles any propagated recursion failure.

`emitFailure` catches runtime exceptions/errors from failure observers, suppresses
them on the original Throwable (unless identical), and preserves that original
exception. After successful publication, runtime observer failure does not emit
`LOAD_FAILED` or revert loaded; provider-end observation gets the failure when
caught inside the attempt. A successful-path provider-end exception propagates
from finally with loaded state retained. The range-end behavior described above
is a distinct boundary and does not use this preservation mechanism. These rules
do not promise exception safety for arbitrary unchecked/sneaky checked throws or
resource failures outside the explicitly caught paths.

## Storage, costs and concurrency limits

The tree retains strong GC-traced key/value references in parallel `Object[]`
lanes, plus `int[] left/right/parent` and `byte[] height`. It stores no structural
node/entry object per mapping. Key-only sets leave the value lane empty. Empty
owners share zero-length arrays; they do not share writable slots. Lazy owners
add one `M3LazyValueState` holding the provider and a `short[]` of slot capacity;
eager and standalone key-only trees allocate no lazy helper/lane.

Insertion uses a free slot or extends the used high-water mark, initializes its
lanes/state, increments size/revision and rebalances AVL ancestors. Growth starts
at at least eight slots, then approximately 1.5 times capacity, capped at
`Integer.MAX_VALUE - 8`; the cap is not an allocation guarantee. Candidate payload
and topology arrays are allocated before lazy flags are grown, then installed.
Deletion relinks topology, including physically relocating the successor's links
rather than copying its payload into the deleted slot; it nulls erased references,
resets flags/height, and uses left as a free-list link. Slot numbers can be reused.
Neither erase nor clear shrinks retained capacity. Unbounded nonempty clear
clears payload/height/state lanes and resets root/free/used/count; stale topology
outside live/free ownership is not itself a mapping.

At capacity `C`, eager map primitive lane payload is `13*C` bytes plus `2*C`
reference slots; key-only set has `13*C` primitive bytes plus `C` references;
lazy map adds `2*C` primitive bytes and helper/provider references. This excludes
headers, alignment, reference width, payload objects, facade/iterator/entry
objects, temporary growth copies and progress/event infrastructure. It is not a
retained-heap result or universal savings claim. Event dispatch avoids an event
record, but overrides can allocate a merged policy; scopes/adapters, caller sinks,
streams and materialized outputs can also allocate.

With stable bounded-cost comparison and excluding callbacks: searches and AVL
updates are logarithmic in live size; growth additionally copies capacity-sized
lanes. Bounded enumeration/size visits visible keys plus tree positioning work.
Unbounded size is constant; endpoint/isEmpty needs tree navigation. Bounded clear
performs one AVL erase per visible key; unbounded nonempty clear touches allocated
lane capacity. Contains-value, hashing and output can visit all visible values
and therefore trigger all their providers. Actual provider/observer cost is
unbounded by the collection. `verifyInvariants` allocates `boolean[used]`, checks
parent/tree/free ownership, counts, strict ordering, AVL heights/balance and lazy
state/payload consistency, and throws `AssertionError` on detected damage. It is
a diagnostic, not a cheap or complete correctness proof.

These owners use plain mutable fields and no synchronization/volatile publication
protocol. Lazy loading is a write even when entered through get, equality, hashing
or rendering. External coordination must cover all views and value reads.
Fail-fast checks and `activeCalls` do not establish concurrent safety, visibility
or lock-free progress. Parallel traversal of deferred values is not independently
qualified. Separate [locked map/set contracts](specify-locked-map-views.md) have
their own null, synchronization and projection rules; the inspected locked tree
constructor creates an eager delegate and does not expose this provider constructor.

There is no raw array/slot API, segmented-address interpretation, stable handle,
close method, native storage, serialization/clone implementation, public capacity
constructor, copy constructor, public trim/compact or invalidation API in these
tree classes. They are final M3 implementations with selected Java collection
interfaces, not subclasses or drop-in concrete-class identities of JDK TreeMap
and TreeSet. Existing porting/phase gates and per-file licenses remain unchanged.

## Small example

Source-checked illustration, **NOT_RUN**:

```java
import com.m3.collections.M3LazyValueProvider;
import com.m3.collections.M3Progress;
import com.m3.collections.M3TreeMap;

var map = new M3TreeMap<Integer, String>(null,
        (key, progress) -> key == 2 ? null : "value-" + key);
map.defer(1);
map.defer(2);
map.defer(3);
var view = map.subMap(1, true, 3, false).descendingMap();
// Keys are 2, 1. Key traversal has not resolved either value.
short before = view.valueFlags(2);              // UNLOADED
view.preload(M3Progress.NULL);                 // synchronous, explicit no-op monitor
boolean loadedNull = view.isValueLoaded(2);    // true
String value = view.getOrDefault(2, "missing"); // null, not the fallback
short outside = view.valueFlags(3);            // ABSENT for this view
short stillDeferred = map.valueFlags(3);       // UNLOADED in the owner
map.clear();                                   // does not load key 3
```

## Focused tests and unqualified edges

All evidence in this table is **test source read, NOT_RUN**. A listed method's
assertions are narrower than its class name; unrelated methods in the same file
are not counted as tree coverage.

| Test body and exact methods | Direct assertion scope |
| --- | --- |
| [M3MapContractTest][map-test]: tree row of `differential`; `treeRotationsAndEveryDeletionOrder` | 30,000 seeded map operations against TreeMap; 80 insertion/deletion-order trials over 500 keys with invariant checks |
| Same: `treeRangesNavigationAndMutation`, `nestedRangeExceptionContractsMatchTreeMap`, `comparatorNullKeysAndEquality` | Inclusive/exclusive/descending bounds and navigation, nested endpoint outcomes, out-of-range writes, immutable snapshots, null-tolerant comparator and canonical comparator-equivalent key |
| Same: tree row of `backedViewsAndCallbacks`; `iteratorDeletionPreservesAllTreeSuccessors`; tree case of `sequencedEndpointsUseLiveAndDetachedEntriesCorrectly` | Value writes through live entries, iterator removal/fail-fast, callback structural side effects, 1,000-key forward/reverse deletion, live versus detached endpoints |
| [M3JdkCollectionDifferentialTest][jdk-test]: tree cases of `lateBindingTraversalsFollowListSetAndMapViews`, `everySortedSplitRetainsCustomComparatorAndEncounterOrder`, `comparatorEquivalentKeysAndNestedBoundsMatchTreeMap` | Pre-binding mutation, bound traversal rejection, recursive split comparator identity/order, comparator-equivalent/null keys and nested range outcomes |
| [M3SequenceContractTest][sequence-test]: tree portions of `streamsRetainEncounterOrderAndSortedComparator`, `treeSetViewsAndPolls` | Reverse comparator/sorted stream order, nullable set elements, subview add/remove/polls, out-of-bound add and map-key insertion rejection |
| [M3LazyTreeMapSelfTest][lazy-test]: `admissionAndNavigation`, `sharedViewsAndEntries`, `canonicalKeys` | No implicit admission or key-navigation loads, loaded-null distinction, shared state and entry snapshots, bounded preload, reused slot reset, stored-key identity and null comparator |
| Same: `failureAndReentrancy`, `progressAndCancellation` | Original failure identity, failed replacement/removal retention and retry, same-key cycles/different-key reads, nine mutation callbacks, partial cancel at two visits, late cancel discard and worked-callback interference |
| Same: `observability`, `eagerAndLazyDifferential`, `storage` | Presence of selected event bits, explicit monitor identity, system-root progress change, 10,000 eager plus 10,000 lazy oracle operations, empty flags/byte heights/int topology/shared tree/cleared references |
| [M3LazyValueStateSelfTest][state-test]: `main` | Direct helper null memoization, growth/reuse/reset, provider failure/retry, recursion/write guard, early/late cancellation, selected event presence and system fallback; it does not assert all event permutations |
| [M3LazyObserverRegressionTest][observer-test]: `successfulValueRemainsCoherentWhenCompletionObserverThrows`, `failureObserversCannotReplaceProviderExceptionOrPoisonRetry` | AFTER_LOAD/AFTER_TOUCH/PROVIDER_END failures preserve loaded value and one provider call; failure observers become ordered suppressed exceptions while retry remains usable |
| Same: `beforeTouchObserverCannotRetargetRecycledSlot`, `rangeAndCachedValueObserversShareMutationGuard`, `deferredKeysPreserveNullFailureCancellationAndViewOrder` | BEFORE_TOUCH/range/cache/end mutation guards, retained unloaded state before attempt, partial borrowed progress, descending retry order and pre-canceled touch state |
| [M3FootprintContractTest][footprint-test]: `treeEmptyChecksStillInvokeComparatorAndRejectNull`, tree parts of `mapViewsCreatedWhileEmptyRemainBackedAndFailFast`, `listAndSetViewsWorkAcrossFirstInsertionClearAndReuse`, `separateEmptyOwnersNeverShareWritableSlots`, `representableAvlHeightFitsSignedByteEvenDuringInsertion` | Empty comparator evaluation, empty-created views/reuse, owner isolation, and an arithmetic AVL height bound; not heap measurement |
| [M3NoRetainedStructuralEntryTest][entry-test]: three declared test methods, tree case | Field/nested-type checks and selected reachable-identity/fresh-entry bursts; finite structural guards, not exhaustive heap analysis |
| [M3LazyTreeMapTest][wrapper-test]: `actualPackedTreeProviderContract` | JUnit wrapper calling `M3LazyTreeMapSelfTest.main`, which also calls the state self-test; no separate independent assertions |

Source-only edges without a dedicated direct assertion in the inspected tests
include policy overlay/exclusion/watch-bit algebra and invalid masks, the full
emission order, disabled-dispatch validation order, throwing sink accessors,
post-publication STATE_CHANGED failure, range-end exception replacement, outer
preload cancellation/done mutation, retained-entry reinsertion, and failure during
allocation. Broader checked/unchecked callback combinations, concurrent use,
serialization compatibility, all aliases/default-method combinations and actual
performance remain unqualified. No successful test run is inferred from source
presence, historical receipts or an example.

## Declared-member review map

The following groups account for every declared field, constructor, method and
nested owner in the eleven primary bodies. Overload counts are explicit; inherited
map/view contracts are dependencies, not additional newly owned source files.
Compiler-generated enum/record methods are not separately declared source bodies.

| Owner/member group | Contract location or implementation role |
| --- | --- |
| `M3TreeMap` fields `tree`, `hasLow`, `low`, `lowInclusive`, `hasHigh`, `high`, `highInclusive`, `descending` | Shared storage and range/direction facade |
| Map constructors: public no-arg, comparator, comparator/provider; package comparator/keyOnly; private tree/bounds/direction | Entry points, provider null validation and view construction |
| `checked`, `within`, `slotWithin`, `endpoint`, `lowest`, `highest`, `firstSlot`, `lastSlot`, `nextSlot`, `findSlot`, `keyAt` | Comparison/range validation, bounded traversal and canonical key lookup |
| `valueAt(int)`, private `valueAt(int,M3Progress)`, private four-argument `valueAt`; `writeAt`, `eraseAt`, `revision` | READ delegation, old-value resolution, guarded mutation and shared revision; the monitor-only private overload has no caller in this body |
| `size`, `isEmpty`, `put`, `clear`, `defer`, `valueFlags`, `isValueLoaded` | Count/membership, explicit/deferred admission, no-load clear and state queries |
| Three `touch` and three `preload` overloads | Synchronous operation/policy/progress routes and partial completion |
| `comparator`, `near`, `nullableKey`, `requiredKey`; first/last key and entry; lower/floor/ceiling/higher key and entry | Receiving-order navigation, null versus throwing endpoint rules, snapshot resolution |
| `poll`, `pollFirstEntry`, `pollLastEntry`, package `pollKey` | Value-first entry erasure versus value-free key polling |
| `reversed`, `descendingMap`, `sequencedKeySet`, `navigableKeySet`, `keySet`, `descendingKeySet`; both overloads each of `subMap`, `headMap`, `tailMap`; `verifyInvariants` | Live facades, insertion-disabled sets, bounds and tree diagnostic |
| Nested `Tree` fields `keys`, `values`, `left`, `right`, `parent`, `height`, `root`, `free`, `used`, `size`, `modifications`, `comparator`, `keyOnly`, `lazy`; constructor | Sole payload/topology/state owner |
| `Tree.writable`, `compare`, `find`, `extreme`, `step`, `near` | Guard, ordering, search, successor/predecessor and nearest search |
| `Tree.h`, `update`, `connect`, `rotate`, `balance` | Byte height maintenance, parent/root reconnection, single/double AVL rotations |
| `Tree.grow`, two `insert` overloads, `erase`, `clear`, both `verify` overloads | Candidate lane allocation, free-slot ownership, structural mutation and invariant recursion |
| `M3TreeSet` fields `map`, `insertable`; no-arg/comparator/package constructors | Standalone key-only storage versus map key facade |
| Set `size`, `isEmpty`, `contains`, `add`, `remove`, `clear`, `spliterator`, `iterator`, `descendingIterator`, `comparator`, `first`, `last`, `lower`, `floor`, `ceiling`, `higher`, `pollFirst`, `pollLast`, `descendingSet`; both overloads each of `subSet`, `headSet`, `tailSet`; `verifyInvariants` | Delegation/permission, value-resolution distinction, sorted traversal and shared diagnostics |
| `M3LazyValueProvider`: five state constants and `load` | Exact states, canonical key, synchronous non-null-monitor callback |
| `M3LazyValueState`: `EMPTY`, `provider`, `flags`, `activeCalls`; constructor; `progress`, `checkCancelled`, `writable`, `grow`, `capacity`, `flags`, `admitted`, `erased`, `cleared` | Optional state lane and guards. `progress(null)` creates a zero-weight system adapter; the tree read path instead uses scopes, and only the self-test directly calls this helper |
| State `read` overloads, `readGuarded`, `eventPolicy`, `emit`, `emitFailure`, `value`, `verifySlot` | Attempt/cache protocol, callback guard, exception suppression, value cast and exact live/free-state consistency |
| `M3LazyEventBits`: NONE, 15 individual bits, ALL; private constructor | Declared vocabulary and mask assignments above |
| `M3LazyEventGranularity`: six enum constants, `includes`, private `rank` | Ranked threshold; INHERIT/NONE disable direct inclusion |
| `M3LazyEventPolicy`: four record components, NONE, compact constructor, `of`, `effectiveMask`, `overlay`, `enabled` | Immutable mask/granularity/watch policy and validation |
| `M3LazyEventSink`: NO_SLOT, `onEvent`, `noop`; nested `NoOpHolder`, INSTANCE and private constructor | Primitive synchronous callback and shared stateless no-op |
| `M3LazyEventSource`: `lazyEventPolicy`, `lazyEventSink` | Default NONE/no-op configuration |
| `M3LazyEvents`: private constructor, generic `emit` | Filter-first dispatch and conditional null validation |
| `M3LazyOperation`: all 18 constants listed above | Cross-owner reason vocabulary; only three selected by this tree |

## Source, dependency and test identities

The complete public production-package directory was reconciled without a
filename filter: 77 physical Java files, comprising 69 public declarations,
seven package-private helpers and package-info. This page owns eleven files
(ten public declarations and one package-private helper), not that whole package.
The module descriptor is outside that package count. Relevant package-local
callers are the factory tree methods, the locked-tree constructor/view delegate,
TreeSet, TreeMap, the state helper and event dispatch chain. Repository-wide code
search returned incomplete results, so no exhaustive repository-wide caller
closure is claimed.

Git blob IDs bind reviewed bytes, not test outcomes. All links below are to
public repository source; licensing and receiver authority are unchanged.

| Primary complete body | Git blob |
| --- | --- |
| [M3TreeMap][tree] | `8a85b86c027bc031ef096ad94de2747b8fd28f9c` |
| [M3TreeSet][set] | `e414f679a341392770fb482377a51fe2f5e6518f` |
| [M3LazyValueProvider][provider] | `5bb4a035f57206bb82dc6b19657a0f39dc2d5fe8` |
| [M3LazyValueState][state] | `d13625a82d623a5aa99fee498e7b5afe793f504a` |
| [M3LazyEventBits][bits] | `e0605534823bd2933b9a3f92f2481a1fef794b33` |
| [M3LazyEventGranularity][granularity] | `7e031ea2e06900aa6326d6d587ad66f290911d6a` |
| [M3LazyEventPolicy][policy] | `aa79f1cc24bc83a5c2db5f7427a3a461715af380` |
| [M3LazyEventSink][sink] | `f0085ec59bbedda0ca18d752996ad489ec976d5c` |
| [M3LazyEventSource][event-source] | `a21d4701650880ae5db7c569427ff64ec2195b92` |
| [M3LazyEvents][events] | `c2a0aa479a4aa89b029ab9d99e22a3a27557b392` |
| [M3LazyOperation][operation] | `9d8939d6d768c3c1861a532d185e8c27afa35994` |

| Dependency / build body and review scope | Git blob |
| --- | --- |
| [M3SlotMap](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3SlotMap.java), complete dependency body | `a7d20300717661c26897f0b69e1e5a83400a62ad` |
| [M3OrderedSlotMap](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3OrderedSlotMap.java), complete dependency body | `e7be2297d19f22cf4dd4ef8b7d1424fe963063d9` |
| [M3SlotArrays](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3SlotArrays.java), complete dependency body | `e337c69710c871f8caa77a1afe53149f1fd705cc` |
| [M3SlotSpliterators](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3SlotSpliterators.java), complete dependency body | `30cf137538353ac8e91514fff7c5db3903da8819` |
| [M3ProgressScope](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3ProgressScope.java), complete dependency body | `591223844f3cfd50fdf5431deb3bc2b1c9d7b5f0` |
| [M3ProgressAdapters](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3ProgressAdapters.java), complete dependency body | `87d3fa14669325d26d0fe1f9a626fce43b357a51` |
| [M3SystemProgress](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3SystemProgress.java), complete dependency body | `66984feed7d22b8e047f69de0862b7ade9083ede` |
| [M3ChildProgressMonitor](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3ChildProgressMonitor.java), complete dependency body | `44a791db2fac00214479e7020ec3c509f70d7ef2` |
| [M3LockedTreeMap](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3LockedTreeMap.java), complete dependency body | `374960e9edfd4c1bbcc0951c4592d3ffd1b31f70` |
| [M3Collections](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3Collections.java), tree factory methods only | `3d2f5aa53bda55293b3254bed3d526b896cbd044` |
| [m3/collections/src/main/java/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/module-info.java), module/build/package declarations | `e6ca30e5a04a0b7c0f1ada4f0659342cd863d78d` |
| [m3/collections/src/main/java/com/m3/collections/package-info.java](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/package-info.java), module/build/package declarations | `3998c9f78a8617aa3b593c353f7d7ac48e93a52b` |
| [m3/collections/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/pom.xml), module/build/package declarations | `3c408446a3841282754f06d82819c285a23acd8a` |
| [m3/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/pom.xml), module/build/package declarations | `693156006961717eccfc649585e724d67682b3df` |
| [m3/collections/README.md](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/README.md), guide reuse | `622b61d4b719fd6879ef8df72ffbcfbaa7e54918` |
| [doc/m3-guide/collections-reference.md](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/doc/m3-guide/collections-reference.md), guide reuse | `ce6038b8276799c9dc15662af916870d7c5b2e39` |
| [SortedSet](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/src/java.base/share/classes/java/util/SortedSet.java), bounded inherited-default/signature review | `3b4612457c6e4cc247601a6d1e0b64a1a7f720d2` |
| [SortedMap](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/src/java.base/share/classes/java/util/SortedMap.java), bounded inherited-default/signature review | `447b0a2c7e79691d8a559feea04cca770777e966` |
| [NavigableSet](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/src/java.base/share/classes/java/util/NavigableSet.java), bounded inherited-default/signature review | `8d7da4eddd32e426e517b741f3b360304d5a7ce8` |
| [NavigableMap](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/src/java.base/share/classes/java/util/NavigableMap.java), bounded inherited-default/signature review | `92abc278dd8beaba253e59633bea3ff0ed328be9` |

| Complete test body read, NOT_RUN | Git blob |
| --- | --- |
| [M3MapContractTest](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/com/m3/collections/M3MapContractTest.java) | `d40c88ec184b90eb057dd859d840c915e4f93df8` |
| [M3JdkCollectionDifferentialTest](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/com/m3/collections/M3JdkCollectionDifferentialTest.java) | `0ca5ef64c7a3fbbc8f869e96affca5dcd8672003` |
| [M3SequenceContractTest](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/com/m3/collections/M3SequenceContractTest.java) | `02ba3c683428a02855517093edbaece60ba7444e` |
| [M3LazyTreeMapSelfTest](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/com/m3/collections/M3LazyTreeMapSelfTest.java) | `9f076ba1d21747e412904b2042510b0ca1e460cd` |
| [M3LazyValueStateSelfTest](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/com/m3/collections/M3LazyValueStateSelfTest.java) | `4c3ce5c828630c7001b9945a1555f5be63c6193f` |
| [M3LazyObserverRegressionTest](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/com/m3/collections/M3LazyObserverRegressionTest.java) | `7d4a21d41dab5a06d52edf8f54689f3118198963` |
| [M3FootprintContractTest](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/com/m3/collections/M3FootprintContractTest.java) | `92183e11d643a12cc797d3d3eb674793d538005a` |
| [M3NoRetainedStructuralEntryTest](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/com/m3/collections/M3NoRetainedStructuralEntryTest.java) | `9e64f4c57ab840b0da744bcff43b9f703fe033f8` |
| [M3LazyTreeMapTest](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/com/m3/collections/M3LazyTreeMapTest.java) | `b101de05ae69a578f273f4e455b796224a2ecf0a` |

[tree]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3TreeMap.java
[set]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3TreeSet.java
[provider]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3LazyValueProvider.java
[state]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3LazyValueState.java
[bits]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3LazyEventBits.java
[granularity]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3LazyEventGranularity.java
[policy]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3LazyEventPolicy.java
[sink]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3LazyEventSink.java
[event-source]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3LazyEventSource.java
[events]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3LazyEvents.java
[operation]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3LazyOperation.java
[module]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/module-info.java
[pom]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/pom.xml
[reactor]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/pom.xml
[module-guide]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/README.md
[map-test]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/com/m3/collections/M3MapContractTest.java
[jdk-test]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/com/m3/collections/M3JdkCollectionDifferentialTest.java
[sequence-test]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/com/m3/collections/M3SequenceContractTest.java
[lazy-test]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/com/m3/collections/M3LazyTreeMapSelfTest.java
[state-test]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/com/m3/collections/M3LazyValueStateSelfTest.java
[observer-test]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/com/m3/collections/M3LazyObserverRegressionTest.java
[footprint-test]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/com/m3/collections/M3FootprintContractTest.java
[entry-test]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/com/m3/collections/M3NoRetainedStructuralEntryTest.java
[wrapper-test]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/com/m3/collections/M3LazyTreeMapTest.java
[sorted-set]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/src/java.base/share/classes/java/util/SortedSet.java
[navigable-set]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/src/java.base/share/classes/java/util/NavigableSet.java
