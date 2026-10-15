<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Collection progress ownership, cancellation and observation

[Guide](README.md) · [Coverage](coverage.md) · [Factory and delivery scope](reconcile-factories-and-delivery-scope.md)

This reference covers all declared members, nested implementations and state fields
of eight collection progress source owners at
`7abdf87cb497d0d94abeb723d70c50f856ecc381`. Their complete bodies and the named
caller/test bodies were read. The eight source blobs match the earlier collection
inventory at `1011050da4205a901ad079d7711b6b8efd8a9af7`. This is a **source-contract
review**, not executed evidence: compilation, tests, concurrent schedules, recipe
replay and installed-runtime qualification are **NOT_RUN**.

These types live in the independent `com.m3.collections` module, which exports the
package and requires only implicit `java.base`. They are not
`com.m3.precompute.M3Progress`. Progress is accounting and cooperative cancellation;
none of these owners starts a worker, interrupts a thread, rolls back a collection,
opens a resource, or proves that the reported work succeeded.

## Choose the owner before choosing the total

| Owner | Total/work model | Completion and observation |
| --- | --- | --- |
| [`M3ProgressMonitor`][spi] | Long-work SPI; `UNKNOWN = -1L` | Interface defaults provide cancellation check and weighted split; implementations define state transitions |
| [`M3ProgressState`][state] | Immutable eight-component record | A returned record is immutable; consistency of the values used to construct it depends on its producer |
| [`M3Progress`][legacy] | Int-backed legacy monitor; total is at least 1 | Synchronized local snapshots; `done()` fills local total; listener callbacks outside its lock |
| [`M3DefaultProgressMonitor`][root] | Long-work root; unknown/zero totals retained | Atomic fields, not one atomic aggregate; `done()` only changes the flag |
| [`M3ChildProgressMonitor`][child] | Package-private long-work weighted child | Cumulative nondecreasing parent credits; one `beginTask`; parent generation checked separately from credit |
| [`M3ProgressAdapters`][adapters] | Legacy-shaped facade over a long-work monitor | Keeps delegate identity/state; int getters saturate; percentage arithmetic has a large-value limitation |
| [`M3ProgressScope`][scope] | Borrow explicit legacy monitor or own a system child | Only owned scopes call `done()` on close, including exceptional close |
| [`M3SystemProgress`][system] | One static default root per loaded class | Shared mutable task/cancellation state; resolution does not establish exclusive ownership |

## SPI and immutable state

`M3ProgressMonitor` declares `beginTask(String,long)`, `done()`, `worked(long)`,
`setProgress(long,long)`, `setTaskName(String)`, `subTask(String)`, `isCanceled()`,
`setCanceled(boolean)` and `state()`. Its `checkCanceled()` default calls
`isCanceled()` and throws `CancellationException` with the current task name when
true. It does not cache a consistent cancellation/name pair. The default
`split(long)` creates a `M3ChildProgressMonitor` bound to `state().generation()`;
it delegates negative-weight rejection to that constructor. A custom monitor must
supply meaningful generations if it expects restart isolation.

`M3ProgressState` has `taskName`, `subTaskName`, `worked`, `totalWork`, `canceled`,
`done`, `generation` and `startedAtMs` components, with their generated record
accessors/equality/hash/string representation. Its compact constructor normalizes
null names to the empty string and rejects negative worked, totals below `-1`,
or negative generation/start time with `IllegalArgumentException`. It does not
require `worked <= totalWork`, `done` to imply any particular count, or cancellation
to exclude completion. It contains no monitor reference and performs no copying
of a mutable monitor beyond accepting the supplied component values.

## Legacy int monitor

`M3Progress` retains total/work counters, task/subtask strings, canceled/done flags,
generation, start time, and an optional `Consumer<M3Progress>`. Its protected
constructor and both `of(int)` / `of(int,Consumer)` factories use `max(1,total)`;
even a negative int total becomes 1. A null listener is accepted. Construction
starts at generation zero with zero worked and a wall-clock start timestamp.
`PERCENT` is 100. `NULL` is the `NullProgress` singleton discussed below.

| Declared operation | Exact transition / boundary |
| --- | --- |
| `beginTask(String,int)` | Increments generation with `Math.incrementExact`; resets start time, names, worked and done; preserves cancellation; normalizes null task name and total; then notifies listener |
| `beginTask(String,long)` | Rejects `< -1` with `IllegalArgumentException`; `-1`/0 become 1; positive values above int range fail in `Math.toIntExact` with `ArithmeticException`; delegates to int overload |
| `worked(int)` | Rejects negatives even after completion; if not done, adds with long intermediate and clamps to total; zero still reaches work hook/listener; done returns before callbacks |
| `worked(long)` | Rejects negative or `> Integer.MAX_VALUE` with `IllegalArgumentException`; delegates to int overload |
| `setProgress(long,long)` | Rejects negative worked, total `< -1`, or either value above int max; normalizes total to at least 1, clamps worked, and clears done; keeps generation/start/names/cancellation; invokes hook/listener |
| `setTaskName`, `subTask` | Normalize null to empty, set local name, notify listener, including after completion |
| `done()` | Idempotently sets done and fills worked to total, then invokes hook/listener; does not check cancellation |
| `cancel()`, `setCanceled(boolean)` | Set local cancellation; no listener notification, worker interruption, or automatic completion |
| `isCanceled`, `isDone` | Synchronized local flag reads |
| `getTotalWork`, `getWorkedSoFar`, `getTaskName`, `getSubTaskName` | Synchronized local getters, each a separate observation |
| `getPercentComplete()` | Integer floor of `worked * 100 / total`, using a safe long intermediate for the int-backed values |
| `snapshot()` / `state()` | Synchronized coherent local observations; Snapshot has seven components and omits start time; State includes it |
| `split(int)` / `split(long)` | Reject negative weights; long overload also rejects values above int max; capture current generation and return a fresh legacy `SubProgress` |
| `toString()` | Formats `Progress[worked/total DONE\|CANCELED\|RUNNING]`; done takes priority; cancellation is queried separately from its Snapshot |

The public nested `Snapshot` record exposes task/subtask, worked, total, canceled,
done and generation components. It has no validation body of its own; direct
record construction is not constrained like `M3ProgressState`.

`onWorkChanged()` is a protected no-op extension hook. The private `credit(int,long)`
checks parent generation and done in the **same synchronized block** as the count
update, then invokes the hook and listener outside the lock. `notifyListener()`
passes the mutable monitor, not a captured Snapshot. Listener exceptions propagate
**after** state has changed; there is no rollback or suppression here. Reentrant
listeners and concurrent updating threads can observe or create later state, and
notification order is not a transaction log. Configure and safely publish the root
before starting workers; coordinate task restart with direct writers.

### Legacy children and the no-op singleton

The package-private nested `SubProgress` holds its parent, int parent weight,
captured parent generation, cumulative `forwarded` count and a `started` flag. Its
constructor starts local progress with total 100. Its overridden
`beginTask(String,int)` rejects a second start, nonzero existing worked, or done
with `IllegalStateException`. The inherited long overload dispatches to it.

`onWorkChanged()` computes `floor(localWorked * parentTicks / localTotal)`, stores
that cumulative amount, and forwards only a positive delta through the parent's
private generation-checked `credit`. Under **monotone** local progress, many small
updates therefore accumulate rather than each losing its fractional part. Weights
are contributions, not reserved portions of a checked budget; over-allocation is
possible, and the legacy parent clamps to its total. A stale-generation child can
keep changing locally, but its numerical credit is refused by that parent.

The inherited `setProgress` can lower local progress and clear done without
restarting the child. Unlike the long-work child's `advance`, this implementation
also lowers `forwarded`; later increases can credit the same portion again.
Consequently the single-use `beginTask` check is **not** a general prohibition on
reopening/resetting a child. Keep legacy child progress monotone and do not reuse
it through `setProgress`; broader reset behavior is a source-observed limitation,
not an executed regression result.

`SubProgress.isCanceled()` includes parent cancellation; `cancel()` cancels both
child and parent. The inherited `setCanceled(true)` only sets the child's local
flag, and `setCanceled(false)` does not clear the parent. Its inherited
`snapshot()`/`state()` contain only the child's local canceled field, so they may
say false while `isCanceled()` is true due to the parent. Names are local: this
legacy child does not forward `setTaskName`/`subTask` to the parent.

`NullProgress` is final and package-private. It constructs total 1 and overrides
both begin/work/split overloads, `setProgress`, `setTaskName`, `subTask`, `done`,
`cancel`, and `setCanceled` as no-ops; both splits return itself. It deliberately
skips the ordinary argument validation, including negative work/weights. Inherited
getters report the unchanged zero-work, not-done, not-canceled state. Explicitly
passing `M3Progress.NULL` is therefore different from passing null to a resolver.

## Long-work monitor and child

### Default root

The public no-argument `M3DefaultProgressMonitor` constructs separate atomic task
and subtask references, worked/total/start/generation longs, and canceled/done
booleans. Initial total is UNKNOWN, generation/work are zero, names empty, flags
false, and start time is wall-clock milliseconds.

`beginTask` rejects total `< -1`, increments generation, then separately writes the
names, zero worked, supplied total (including zero/UNKNOWN), timestamp and false
done. Cancellation remains sticky. `worked` rejects negatives, returns for zero
or an observed done flag, otherwise atomically adds with `saturatedAdd`; overflow
becomes `Long.MAX_VALUE`. It does **not** clamp to total. `setProgress` validates
nonnegative worked and total `>= -1`, then writes those fields separately without
clearing done or changing generation/start time. `done` only sets its boolean;
it does not fill a total or prevent later `setProgress`.

`setTaskName` and `subTask` normalize null to empty. `isCanceled` and `setCanceled`
read/write the cancellation atomic. `split` captures the current generation and
creates a long-work child. `state` reads each atomic individually. Atomic scalar
updates do not make task restart, worked-vs-done, setProgress, or State construction
one atomic operation: e.g. a writer that saw not-done can add after another thread
sets done. Observers may see components from different updates. Generation uses
`incrementAndGet`, not checked increment; eventual signed wrap would produce a
negative generation rejected by `M3ProgressState`. No practical overflow execution
is claimed. There is no listener in this owner.

### Weighted long-work child

`M3ChildProgressMonitor` has a non-null parent, nonnegative `parentWork`, captured
`expectedParentGeneration`, local strings/counts/start/generation/flags, and a
`forwarded` watermark. Construction rejects null parent or negative weight; initial
total is UNKNOWN. All local compound state operations use the child monitor lock.

| Declared operation/helper | Behavior |
| --- | --- |
| `beginTask(String,long)` | Under lock, first rejects already-started, nonzero-worked or done; rejects total `< -1`; sets started, checked-increments local generation, resets local names/work/time and stores total; then calls parent `subTask` |
| `worked(long)` | Rejects negatives; zero is no-op; ignores completed child; `boundedAdd` saturates on overflow and clamps only when total is positive; computes the cumulative delta under the child lock and attempts parent credit outside it |
| `setProgress(long,long)` | Validates before checking done; if active, stores total and worked clamped only for positive total; can lower local count, but cannot lower forwarded watermark |
| `done()` | Idempotently marks done, fills local worked only for a positive total, advances credit to the full parent weight, then tries to credit parent |
| `setTaskName`, `subTask` | Normalize null, update local field, then propagate the supplied name to parent `subTask`, even for a done or old-generation child |
| `isCanceled`, `setCanceled` | Read local-or-parent cancellation; setting true also calls parent `setCanceled(true)`; clearing local false does not clear parent |
| `split(long)` | Makes another long-work child, capturing this child's current generation; use after this child begins its task |
| `state()` | Coherent locked local fields with inherited cancellation queried from parent; not an atomic snapshot of the whole ancestry |
| `targetParentWork()` | Full weight when done; zero before positive total/work, or for zero weight; full weight when worked reaches total; otherwise scaled floor |
| `scale(value,weight,total)` | Checked long multiplication/division, with exact positive `BigInteger` fallback when multiplication overflows |
| `advance(target)` | Clamps target between existing forwarded and parent weight, stores new watermark, returns nonnegative delta; no retracting prior credit |
| `boundedAdd(current,increment,total)` | Checked addition; overflow becomes positive total when known, otherwise long max; positive known total bounds the result |
| `creditParent(delta)` | For positive delta, checks parent generation and done through separate State calls, then calls parent `worked` |

Unknown or zero child totals accrue local work but forward no fraction until a
positive total is supplied or `done()` forwards the full weight. A child with a
zero/unknown total can be done while its local worked count remains zero or a
previous accumulated count. Parent cancellation is not consulted by `worked`,
`setProgress`, or `done`; callers must choose cancellation checkpoints.

**Coordinate root restart with workers.** `creditParent` does not combine its
expected-generation check with parent mutation. A concurrent root restart between
those calls can admit a credit into the new task. It can also drop a credit when a
parent is done; the child's watermark has already advanced and no retry restores
that credit. Name/cancellation propagation is not generation-filtered. This differs
from the locked generation predicate in legacy `M3Progress.credit`. Do not claim
linearizable cross-generation or whole-tree snapshots from the thread-safe fields.

Exceptions from parent callbacks propagate after local state/watermark changes;
there is no compensating transaction. Parent/child locks and custom implementations
also mean there is no lock-free or wait-free guarantee for this SPI.

## Adapters and percentage limits

`M3ProgressAdapters` has a private constructor. `asLegacy(monitor)` rejects null,
returns an existing `M3Progress` unchanged, otherwise constructs a private
`LegacyAdapter` holding that exact delegate. This shares mutable state; it is not a
copy, reset, new root, or new child. The private adapter constructor initializes
its otherwise unused superclass state with total 1.

The adapter overrides both `beginTask`, both `worked`, both `split`, `setProgress`,
`setTaskName`, `subTask`, `done`, `cancel`, `setCanceled`, `isCanceled`, `isDone`, all
four legacy getters, `getPercentComplete`, `snapshot`, and `state`. Mutators pass
through directly (`cancel` uses delegate `setCanceled(true)`); split calls delegate
split and recursively wraps the result. Long arguments are not subjected to the
ordinary legacy int validation. `isDone` and getters query delegate State; inherited
`toString` uses the overridden Snapshot and cancellation methods.

Private `legacyTotal` maps `<= 0` to 1 and saturates large positive totals at int
max; `legacyWorked` maps `<= 0` to zero and saturates at int max. Snapshot converts
counts with these rules, retains names/flags/generation, and drops start time.
`state()` returns the delegate State directly. Int getters can therefore lose
precision and, for an over-complete delegate, report worked greater than total.

`getPercentComplete()` returns zero for total `<= 0`; otherwise it evaluates
`min(100L, worked * 100L / total)` and casts to int. The multiplication is unchecked:
valid large nonnegative long work can overflow before the cap, producing an
incorrect, potentially negative percentage. This is a source-observed arithmetic
boundary, **not** saturation protection and not a measured/tested failure here.
Do not use that getter as an exact large-counter completion test; read long State
and use appropriate checked arithmetic in the consumer.

## Scope and system root

`M3SystemProgress` has a private constructor and static final `ROOT`. Its private
`createRoot` constructs `M3DefaultProgressMonitor`, begins `"Synexia system"` with
UNKNOWN work, and returns it. `get` exposes the same mutable root. `split` delegates
the supplied weight to the root. `resolve(monitor,systemWork)` returns a non-null
monitor unchanged, without validating the unused weight; null selects a new root
child and thus rejects a negative weight. There is no automatic root replacement,
cancellation reset, scope isolation, or shutdown. “Process-wide” in its source
comment means this loaded class's static root; separate class loaders can load
separate copies.

`M3ProgressScope` retains a legacy monitor, final ownership flag and plain boolean
closed flag. Its private constructor requires a non-null monitor. Static
`resolve(M3Progress,long)` rejects negative systemWork **even with an explicit
monitor**. Non-null monitors, including `M3Progress.NULL`, are borrowed unchanged.
Null creates a system-root child of that weight and wraps it with `asLegacy`, owned
by the scope. `monitor()` and `owned()` return those fields.

`close()` only acts when owned and not closed: it sets closed before calling
`monitor.done()`. Sequential repeated close is a no-op. A borrowed close does
nothing, including leaving its closed flag unchanged. There is no synchronization
or volatile publication on the scope, so do not infer concurrent close-once safety.
If `done()` throws, the owned scope has already marked itself closed and does not
retry. No reference is cleared or external resource released.

Try-with-resources calls close on both normal and exceptional exit. Thus an owned
system child is completed and attempts to credit its remaining weight even after a
provider exception or cancellation. Parent generation/done checks can refuse that
credit after the child has advanced its watermark. A completion flag or increased system count is not a
success receipt. This concrete behavior differs from the legacy `done` comment's
advice that failure/cancellation should not call done; preserve the distinction in
callers rather than treating that comment as an enforced scope rule.

## Caller evidence and failure effects

The [`M3TreeMap` caller][tree] and [`M3LazyValueState`][lazy-state] bodies connect
these monitors to lazy loading:

- Tree `touch` resolves weight 1, checks cancellation, then looks up/loads the
  key. With a missing monitor, even an absent key exits through owned scope close
- Tree `preload` resolves weight `max(1,size)`, begins `"Load packed tree values"`,
  checks cancellation between entries, credits one tick after a successful value
  access, detects structural interference, and calls done on ordinary success.
  An explicit monitor is borrowed by the scope, but the **caller** still invokes
  begin/work/done; borrowed does not mean read-only or preserved state
- Lazy `readGuarded` resolves weight 1, checks cancellation before provider work
  and after its return. A canceled result is not published. Its nested scope
  borrows an explicit outer monitor, avoiding an extra owned child on that route
- `M3LazyValueState.progress(null)` instead returns a zero-weight system child
  adapter. It is a real cancellable monitor, not the NULL singleton
- Owned scope close still happens for failure/cancellation. Borrowed preload's
  exception route does not call done, so successful earlier ticks remain without
  marking that explicit monitor complete

### Tests read, with limits

| Test body | Assertions actually present | What it does not establish |
| --- | --- | --- |
| [`M3LazyValueStateSelfTest.main`][state-test] | Non-null provider monitor; pre-cancel refusal; late cancellation prevents result publication; null fallback differs from NULL; null-read system count increases by at least 1 | Exact weighted rounding, restart races, exceptional owned-close counts, percent overflow |
| [`M3LazyTreeMapSelfTest.progressAndCancellation`][tree-test] | Listener cancels after two ticks; two entries loaded and explicit monitor remains incomplete; late cancellation rejects value; interfering listener triggers concurrent-modification failure | General concurrent monitor linearizability or cancellation propagation through every adapter |
| Same test: `sharedViewsAndEntries`, `observability` | Bounded successful preload marks supplied monitor done; explicit provider monitor is the same object; null touch credits shared root; configured progress events exist | Allocation/resource proof or whole-monitor contract coverage |
| [`M3LazyObserverRegressionTest.deferredKeysPreserveNullFailureCancellationAndViewOrder`][observer-test] | Failed preload leaves explicit progress at 1 and not done; canceled touch does not load deferred value | Null-monitor exceptional completion semantics |
| Other observer regression methods | Completion-observer failures retain loaded values; failure observers do not replace provider exception; mutation guards protect slots/views | Direct progress-listener failure isolation; those listeners are a different callback path |
| [`M3LazyTreeMapTest`][junit-wrapper] | JUnit entry invokes the main self-test | A new run of that self-test in this documentation pass |

The module POM declares Java 21 compilation and JUnit tests. The existing
[`verify_donation.py`][verifier] additionally invokes both main self-tests, packages
the module, and has JNI/allocation gates. Reading its commands is not running them.
The [factory/delivery reference](reconcile-factories-and-delivery-scope.md#delivery-evidence-is-not-a-factory-property)
keeps older receipt inventories distinct from this source pin.

Missing direct tests remain explicit: large-counter adapter arithmetic; coherent
observation under concurrent begin/setProgress/work/done; default-root restart
versus child-credit race; legacy child rewind/reopen; listener exceptions and
reentrancy; NULL invalid-input bypass; nested generation setup; negative-weight
resolver asymmetry; scope close after provider/cancellation failure and concurrent
close. Source body coverage for these eight files does not complete their runtime,
JDK compatibility, or all collection-package contracts.

## Source identity and declared-member accounting

Every method, constructor, field and nested implementation in these eight files is
covered above. The generated accessors of both records are described with their
components. Private constructors prevent instantiating the three utility/scope
facades through ordinary caller construction; the scope factory is the public
construction route. No implementation field is a persistent file format or public
serialization promise.

| Source owner | Git blob | Body/member route |
| --- | --- | --- |
| M3ProgressMonitor | `fbaaf21bb7f013bebbc32d0d43dbf71e47b90059` | SPI and immutable state |
| M3ProgressState | `23e877a0862e2b9a9a83f247e6b0b499a0757d9e` | SPI and immutable state |
| M3Progress | `5728ff52b138121791fd2b12c923a883d52327bc` | Legacy int monitor, Snapshot, SubProgress, NullProgress |
| M3DefaultProgressMonitor | `2c5f52b913ed7d4207645871f522ad63c2bf57df` | Long-work monitor and child |
| M3ChildProgressMonitor | `44a791db2fac00214479e7020ec3c509f70d7ef2` | Long-work monitor and child |
| M3ProgressAdapters | `87d3fa14669325d26d0fe1f9a626fce43b357a51` | Adapters and percentage limits, LegacyAdapter |
| M3ProgressScope | `591223844f3cfd50fdf5431deb3bc2b1c9d7b5f0` | Scope and system root |
| M3SystemProgress | `66984feed7d22b8e047f69de0862b7ade9083ede` | Scope and system root |

[spi]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/java/com/m3/collections/M3ProgressMonitor.java
[state]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/java/com/m3/collections/M3ProgressState.java
[legacy]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/java/com/m3/collections/M3Progress.java
[root]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/java/com/m3/collections/M3DefaultProgressMonitor.java
[child]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/java/com/m3/collections/M3ChildProgressMonitor.java
[adapters]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/java/com/m3/collections/M3ProgressAdapters.java
[scope]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/java/com/m3/collections/M3ProgressScope.java
[system]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/java/com/m3/collections/M3SystemProgress.java
[tree]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/java/com/m3/collections/M3TreeMap.java#L158-L224
[lazy-state]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/java/com/m3/collections/M3LazyValueState.java
[state-test]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/test/java/com/m3/collections/M3LazyValueStateSelfTest.java
[tree-test]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/test/java/com/m3/collections/M3LazyTreeMapSelfTest.java
[observer-test]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/test/java/com/m3/collections/M3LazyObserverRegressionTest.java
[junit-wrapper]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/test/java/com/m3/collections/M3LazyTreeMapTest.java
[verifier]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/verify_donation.py
