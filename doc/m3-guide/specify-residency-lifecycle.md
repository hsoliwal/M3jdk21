<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Parent-owned object residency: lifecycle and failure boundaries

This reference reviews the complete bodies and all declared members of
`com.m3.collections.M3ObjectResidency<T>` and `M3Residency` at
`7abdf87cb497d0d94abeb723d70c50f856ecc381`. Both primary blobs are unchanged
from audit snapshot `1011050da4205a901ad079d7711b6b8efd8a9af7`.
It documents an explicit arena/access API, not transparent object eviction,
compiler transformation, native swapping, or a JVM heap limit guarantee.

The public owners belong to the independently built Apache-2.0
[com.m3.collections module][module]. The [design document][design] supplies
intent; this reference distinguishes the narrower implemented behavior below.
The [recipe packet][recipe] leaves inventory/transformation work separate from
the runtime arena and CLASS-retained marker. Existing Swing/SWT/JDK contracts
and the repository's [receiving phase][policy] are not promoted by this review.
For primitive locations rather than projection lifecycles, see
[atomic storage](specify-atomic-primitive-storage.md) and
[ordinary segmented storage](specify-stable-segmented-storage.md).

## Ownership and construction

An integer slot identifies a logical child **within one arena**. There is no
handle object, generation tag, global namespace, slot allocator, or cross-arena
identity check. Slots do not move or resize. A projection object can disappear
from the arena and later be replaced by another object for the same slot.
External aliases can keep the old object alive; clearing the arena reference
neither revokes those aliases nor forces GC.

The constructor ([source, 77–101][arena]) takes all of these explicitly:

| Argument | Unit / validation / ownership |
| --- | --- |
| `slots` | Nonnegative number of fixed array positions; negative throws `IllegalArgumentException`; Java array/allocation limits apply, not the 28-bit lane bound |
| `flags` | Bitmask restricted to `ALL_FLAGS`; unknown bits throw `IllegalArgumentException` |
| `maxResidentBytes` | Nonnegative configured sum-of-estimates ceiling |
| `reserveBytes` | Nonnegative heap-headroom reserve; effective only with `PRESSURE_AWARE` |
| `Factory<? extends T>` | Retained callback; required at construction for `RECREATE`; an absent lazy slot also needs a factory when first used |
| `Codec<T>`, `SpillStore` | Both retained and required when `SWAP_SECONDARY` is enabled; otherwise may be null |

There is no default arena constructor or implicit budget. Zero slots and zero
budgets are allowed. The constructor eagerly allocates six fixed-length arrays:
`resident` (`Object[]`), `parentBits`, `weights`, `touched` (three `long[]`),
`states` (`byte[]`) and `active` (`int[]`). States start `ABSENT`, words/counters
start zero, and `lastDecision` starts null. Numeric lane payload is 29 bytes per
slot plus one reference slot, before array/object headers and referenced payloads;
this arithmetic is not a measured retained-heap figure. The arena retains the
factory, codec and store even when all projections are absent.

`parentBits` is the built-in canonical 64-bit word per slot. It survives eviction,
swap and restore and is excluded from `residentBytes`. The factory/codec must
honor the application's canonical-state contract; the arena cannot determine
whether they reconstruct correct behavior. Other canonical state may live in an
external parent, as in the tests. State held solely in a discarded projection
is lost on `RECREATE`. Swap preserves only what the supplied codec/store preserve.

## Flags and state values

All constants are in [M3ObjectResidency, 23–36][arena]. They are independent bits;
no combinations silently add missing flags.

| Constant | Value | Actual effect |
| --- | --- | --- |
| `LAZY_CREATE` | 1 | Permits `use` on an `ABSENT` slot to invoke its factory; does not eagerly create when disabled |
| `LRU` | 2 | Enables victim selection; without it no slot is selected for eviction |
| `PRESSURE_AWARE` | 4 | Reduces budget using JVM headroom and enforces on access to an already resident slot |
| `SWAP_SECONDARY` | 8 | On eligible eviction, encode/write before dropping the reference; takes precedence over `RECREATE` |
| `RECREATE` | 16 | Allows dropping an eligible resident projection to `ABSENT` when swap is not enabled |
| `OBSERVE_ONLY` | 32 | Lets admission exceed budget without eviction; pressure enforcement records a warning and returns |
| `STRICT_NO_ESCAPE` | 64 | Rejects the direct identity result `operationResult == projection` after the callback returns |
| `PIN_EXPOSED` | 128 | Pins an installed value, or a projection directly returned by a completed `use`, against future victim selection |
| `ALL_FLAGS` | 255 | Valid construction-bit mask |

There is no implemented `ALLOW_NULL` or `DEFAULT` flag. Factories, decode and
install must produce non-null projections. The design's proposed `DESTROYING`
state is not in the enum. Actual `State` constants, in lane encoding order, are
`ABSENT`, `CREATING`, `RESIDENT`, `SWAPPING`, `SWAPPED`, `RESTORING`.
Private `STATE_MASK` is 7; private `SLOT_PINNED` is a separate bit in the byte
state lane, not the constructor `PIN_EXPOSED` bit value. `stateAt` masks off the
pin and decodes values 0–5; an invalid state value throws `IllegalStateException`.
`setState` preserves the pin bit. There is no public unpin, reset, per-slot delete,
explicit evict, arena clear, or close operation.

`install`'s comment says “Pin,” but pinning is conditional on `PIN_EXPOSED` in its
body. Installing without that flag does not make the object ineligible for LRU.
With both escape flags enabled, a direct-return strict check throws before the
pinning branch runs. A thrown callback also does not perform the later pin check.
Neither flag catches indirect escape through a field, collection, closure,
callback side effect or a wrapper returned by the operation. Neither is deep
escape analysis, and the strict exception cannot undo an escape that already
happened. Keep outside references synchronized and respect the projection scope.

## Public operations and callback effects

All mutable-state public methods below are `synchronized` on the arena. `slots()`
only reads the fixed final array length. Public nested records/interfaces and
exceptions are described separately.

| Declared member | Source lines | Return, refusal and effects |
| --- | --- | --- |
| `slots()` | 103 | Fixed slot count |
| `readParentBits(slot)` | 109–112 | Current canonical word; does not materialize or touch LRU |
| `writeParentBits(slot, value)` | 115–118 | Replaces the canonical word; does not refresh an existing projection |
| `updateParentBits(slot, update)` | 121–127 | Checks slot, rejects null operator, calls it under monitor, writes and returns its result |
| `residentBytes()` | 129 | Current accounted sum of projection estimates, not JVM-measured heap bytes |
| `state(slot)` | 130 | Decoded state, not the separate pin bit |
| `lastDecision()` | 131 | Last immutable decision record, or null initially; not a decision history |
| `use(slot, estimatedBytes, operation)` | 137–157 | Checks slot, null operation, then negative estimate; ensures resident, marks active and touches, invokes callback, applies escape checks, returns its result |
| `install(slot, value, estimatedBytes)` | 160–173 | Checks slot/null/negative estimate, requires `ABSENT`, admits, stores value and weight, optionally pins, increments accounting, marks resident, touches, records `installed`; never invokes factory |
| `signalMemoryPressure()` | 176–178 | Immediately calls budget enforcement with requested slot `-1` and incoming zero; it does not merely defer work to the next access |

Every slot-taking public operation validates `0 <= slot < slots()` and otherwise
throws `IndexOutOfBoundsException`. `use` and `install` reject negative estimates
with `IllegalArgumentException`; zero estimates are permitted. When a slot is
already resident, a nonnegative estimate supplied to `use` does **not** update its
stored weight. A new create/restore uses the estimate supplied for that operation;
there is no automatic reweighing as the object changes.

The `use` callback runs after `active[slot]++` and a touch increment. A `finally`
block decrements active even when the operation or direct-return check throws.
No callback mutation, parent-word update, LRU touch, creation, or earlier eviction
is rolled back. `updateParentBits` stores its returned word only after normal
operator completion. An operator exception propagates, but side effects remain;
a reentrant write to the same word can be overwritten by the outer returned value.

Monitor serialization protects arena lanes across normal calls through this API;
it does not synchronize arbitrary use of an escaped projection. Factory, codec,
store, operator and operation callbacks all run while the arena monitor is held.
A slow/blocking callback delays other arena callers. No timeout, cancellation,
interruptible monitor admission, fairness, asynchronous worker, or callback
sandbox is implemented. Keep callbacks bounded and avoid waiting for another
thread that needs the same arena. These are monitor-based operations, not
lock-free or wait-free operations.

## Materialize, restore, evict

The following transitions are the normal paths in `ensureResident` and `evict`,
not a promise that every arbitrary callback/failure can be recovered.

| Starting state / operation | Ordered actions | Normal completion |
| --- | --- | --- |
| `ABSENT`, `use` | Require `LAZY_CREATE`; admit estimate; require factory; set `CREATING`; call factory and reject null; install reference/weight; add accounting | `RESIDENT`, touch, decision `created` |
| `SWAPPED`, `use` | Admit estimate; set `RESTORING`; `spill.read(slot)`; `codec.decode(slot, bytes)`; reject null; `spill.delete(slot)`; install reference/weight; add accounting | `RESIDENT`, touch, decision `restored` |
| `RESIDENT`, `use` | If pressure-aware, enforce with this slot excluded; keep existing reference/weight | Callback proceeds with active count; no automatic new decision |
| Eligible `RESIDENT`, swap eviction | Set `SWAPPING`; encode non-null bytes; `spill.write`; clear reference; subtract weight and zero it | `SWAPPED`, decision `swapped to secondary store` |
| Eligible `RESIDENT`, recreate eviction | Clear reference; subtract weight and zero it | `ABSENT`, decision `projection released; recreate from canonical parent state` |

`use` on `CREATING`, `RESTORING` or `SWAPPING` throws
`IllegalStateException` for reentrant transition. `use` of an absent slot with
lazy creation disabled throws before admission. If lazy creation is enabled but
factory is null (permitted when `RECREATE` was not selected), it throws after
admission; any evictions already performed are retained. A `SWAPPED` slot can
restore even when `LAZY_CREATE` is disabled. There is no fallback from failed
swap/restore to recreation, even when both strategy bits are set.

Reentrant `use` of an already resident slot is supported by the reentrant monitor
and active counter. Its active projection is excluded from victim selection;
other eligible slots can still be evicted by nested operations. The current
requested slot is separately excluded during admission/enforcement. Transition
callbacks have stronger limitations described below; “recursive use supported”
does not promise arbitrary state-changing reentrancy from every callback.

## Budget, pressure and LRU

`HeapSnapshot.current()` ([48–55][arena]) samples `Runtime.totalMemory()`,
`freeMemory()` and `maxMemory()` without forcing GC. It computes
`used = max(0, committed - free)` and
`estimatedHeadroom = max(0, maxMemory - used)`. These are changing JVM observations,
not an atomic heap snapshot, exact object sizing, native-memory accounting, or
proof that a future allocation will fit. The `HeapSnapshot` record also exposes
its four generated accessors and a public record constructor without additional
validation; constructing a record does not alter arena policy.

Private `targetBudget` (258–265) starts at configured `maxResidentBytes`. With
`PRESSURE_AWARE`, it takes the minimum of that ceiling and
`max(0, estimatedHeadroomBytes - reserveBytes)`. Each `admit`/`enforceBudget` call
samples once; its loop does not resample after every callback/eviction. Private
`wouldExceed` (267–269) uses `incoming > target || residentBytes > target - incoming`
to avoid a direct overflowing sum for the comparison. Accounting itself uses
`Math.addExact` during installation.

Private `leastRecentlyUsed` (271–282) scans every slot when `LRU` is enabled. It
skips the requested slot, null resident references, active slots, pinned slots,
and all slots if neither eviction strategy is configured. It picks the smallest
signed touch tick; a strict `<` comparison leaves the first/lower slot selected
for equal eligible ticks below the initial `Long.MAX_VALUE` sentinel. A slot
whose touch tick equals `Long.MAX_VALUE` is not selected by that scan. Touches
occur on install, creation/restoration and `use`, even if `LRU` is off. There is
no tick-overflow normalization. Victim selection is a full slot scan per victim,
not an O(1) linked LRU queue.

- `admit` (224–237) evicts while the incoming estimate would exceed target. If
  no victim remains, it throws `BudgetExceededException`, including for a zero
  incoming estimate when existing accounting is already over target. With
  `OBSERVE_ONLY`, it breaks and allows over-budget admission without eviction;
  that break itself does not create an observe-only decision.
- `enforceBudget` (239–256), reached with incoming zero by public pressure paths,
  evicts until within target or records `no eligible victim under pressure` and
  returns. It may leave the arena above target. Observe-only enforcement instead
  records `observe-only: budget would be exceeded` and returns. A call already
  within target does not replace the last decision.
- Evicting several victims is not a transaction. An eventual admission failure
  does not restore earlier victims. Budget estimates omit metadata arrays,
  parent state, retained callback/store graphs, spill bytes, encode/decode
  temporaries, escaped aliases and native/process memory.

The `Decision` record (57–58) contains `slot`, `before`, `after`, `residentBytes`,
`targetBytes`, `reason`, with generated accessors/value methods and no custom
validation. Private `decision` (327–329) always stores **configured
`maxResidentBytes`** as `Decision.targetBytes`, not the pressure-reduced target
used by that enforcement pass. Pressure decisions may use slot `-1`, for which
private `stateAtOrAbsent` reports `ABSENT`. Do not read this diagnostic record as
an admission promise or exact effective-target report.

## Store, codec and resource boundary

The nested contracts ([38–47][arena]) are user implementations:

- `Factory.create(int slot)` returns a projection and may throw `Exception`
- `Codec.encode(T value)` returns bytes; `Codec.decode(int slot, byte[] bytes)`
  returns a projection; both declare `IOException`
- `SpillStore.write(int slot, byte[] bytes)`, `read(int slot)`, and `delete(int slot)`
  declare `IOException`

There is no built-in file store, database, encoding, checksum, schema/version,
namespace, quota, native handle, mmap or JNI path in these owners. Slots alone
are passed to the store, so sharing one store across arenas needs a caller-owned
namespace strategy. `encode`'s byte array is passed directly to `write` and
`read`'s return directly to `decode`; the arena does not clone either. Encode
null is rejected; read null is passed to the decoder without an arena null check.
Array aliasing, corruption checks and copy/durability semantics belong to the
codec/store. The test memory store clones on write/read, but that behavior is not
imposed by the interface.

Restore deletes the spill entry before publishing the resident reference. A
caller must account for delete failures or a store that mutates and then throws.
There is no transactional write/delete protocol, rollback of store effects,
automatic recovery of orphaned entries, migration between arenas/stores, or close
hook. The arena does not call `close`/dispose on a discarded projection. External
files and native resources require their own ownership/lifetime protocol; simply
dropping Java references does not establish deterministic release.

## Failure and reentrancy limits

The exception classes are public static final runtime exceptions. Both declare
`serialVersionUID = 1L`. `ResidencyException(String, Throwable)` preserves message
and cause. `BudgetExceededException(long requestedBytes, long residentBytes,
long targetBytes)` stores those public final fields and builds a diagnostic
message; when thrown by admission, `targetBytes` is the effective sampled target,
unlike `Decision.targetBytes`.

| Failure site | Implemented handling |
| --- | --- |
| Creation/restore `IOException` | Reset state enum to previous `ABSENT`/`SWAPPED`; wrap in `ResidencyException` with I/O message |
| Other checked factory `Exception` | Reset state; wrap in `ResidencyException` with creation message |
| Creation/restore `RuntimeException`, including null factory/decode result | Reset state; rethrow the original runtime exception |
| Swap encode/write `IOException` or `RuntimeException`, including null encoded array | Reset state to previous resident state; wrap in `ResidencyException`; normal pre-drop failure retains reference and accounted weight |
| Operation/operator exception | Propagate; `use` decrements its active count in `finally`; callback side effects are not undone |
| `Error`, including allocation failure | Not caught by transition handlers; no general recovery guarantee |

State restoration is not a rollback of every field or external effect. In
particular, the following are source-backed limits, **not newly executed failure
reproductions**:

1. `ensureResident` and `install` assign `resident[slot]` and `weights[slot]`
   before `Math.addExact`. Observe-only admission can let the estimate sum
   overflow. On overflow, `ensureResident` restores only the state enum; `install`
   has no such catch. A reference/weight can remain without matching accounting
   or resident state. Restore may already have deleted the spill entry. Do not
   rely on accounting consistency or retry recovery after this exceptional path.
2. An `Error` from a factory/codec/store can leave `CREATING`, `RESTORING` or
   `SWAPPING` set. The public API provides no repair/reset operation. An error
   after reference/accounting changes can leave additional partial effects.
3. Victim selection tests `resident[i] != null` but does not test that its state
   is `RESIDENT`; `evict` then returns without progress for nonresident state.
   During an encode/write callback, the swapping slot still holds its resident
   reference. A reentrant over-budget admission/pressure call can repeatedly
   select that `SWAPPING` slot and make no progress. Avoid initiating residency
   transitions or pressure enforcement from factory/codec/store callbacks;
   direct same-slot `use` refusal is not a general reentrancy guard.
4. Admission happens before creation marks `CREATING` and before installation
   publishes state; callbacks invoked while evicting another slot can therefore
   see the requested slot as still absent. There is no post-admission recheck
   against callback mutation. No arbitrary reentrant-install atomicity is claimed.

These limits qualify the design document's broad recovery statements. They do
not change the source or introduce a substitute implementation. Keep callback
behavior and estimates within a controlled contract; broader recovery and
concurrency claims require separate tests and fixes through the owning process.

## The annotation is metadata, not an enabled lifecycle

[M3Residency][annotation] has `RetentionPolicy.CLASS` and targets TYPE, FIELD,
METHOD and CONSTRUCTOR. It is inert on stock Java; the arena does not read it.
All members and defaults are:

| Member | Default |
| --- | --- |
| `flags()` | `LAZY_CREATE | LRU | PRESSURE_AWARE` (7) |
| `estimatedBytes()` | `-1L`, metadata placeholder; not a valid estimate for arena `use`/`install` |
| `parent()` | Empty string |
| `strategy()` | `Strategy.RECREATE` |
| Nested `Strategy` | `RECREATE`, `SWAP`, `PIN` enum constants |

The strategy default does not add the `RECREATE` construction bit, and no code
here validates or translates annotation values into an arena. There is no runtime
escape proof, hash guard, bytecode rewrite or automatic adapter in these two
files. The [packet matrix][matrix] marks the inventory recipe/compiler bridge
open and target/JNI adapters blocked at this pinned source. Its statuses are
recorded planning evidence, not evidence of newly run tests or of all other
repository receivers. Distinct `com.m3.residency` metadata annotations and
Swing viewport residency owners are outside this reference's primary ownership.

## Tests, caller and build evidence

All bodies of [M3ObjectResidencyTest][test],
[M3ObjectResidencyRuntimePressureTest][heap-test] and the
[standalone pressure probe][probe] were read. They were **NOT_RUN** here.

| Exact test/caller | What its body actually checks |
| --- | --- |
| `parentBitsRemainCanonicalAcrossProjectionRecreation` | Parent word update survives release of slot 0 and later use |
| `exposedProjectionIsPinnedAndNeverSilentlyReplaced` | Direct-return `PIN_EXPOSED` prevents another 8-byte admission; old object stays resident |
| `exposedInstallIsPinnedFromTheMomentItEntersTheArena` | Installed external reference is pinned with the flag and returned by identity |
| `lazyCreateAndStableIntegerSlot` | Factory invoked once across two uses; state changes absent to resident |
| `swapsToSecondaryAndRestoresWithoutChangingLogicalValue` | Test codec preserves a mutated text field; restore removes slot's memory-store entry |
| `neverEvictsActiveSlotAndReportsUnrecoverableBudget` | An oversized first admission throws; despite its name, it does not enter an active callback or test nested-use eviction |
| `strictScopeRejectsReturningTheProjection` | Direct identity return throws; no indirect escape assertion |
| `rejectsUnknownFlagsAndIncompleteSwapConfiguration` | Unknown flags and missing swap dependencies throw |
| `lruDoesNotSpinWhenNoEvictionStrategyIsConfigured` | Full-budget admission fails with no swap/recreate strategy; no transition-callback reentrancy case |
| `recreatesOnlyFromCanonicalParentState` | Factory sees updated external parent state after previous projection is released |
| `failedSwapLeavesOriginalProjectionResident` | Store write throws `IOException`; original slot state/reference remain usable |
| `residencyAnnotationIsClassRetainedAndExplicit` | Classfile bytes contain the annotation descriptor; null reflection query is on the class although the marker is placed on its field, so that query alone is not field-retention proof |
| `targetUsesTheActualRunningJvmHeap` | Heap snapshot max matches current `Runtime.maxMemory`; nonnegative used/headroom and arithmetic relationship |
| `M3ResidencyPressureProbe.main` | 1,024 slots; declared 16 KiB weights; 24 MiB reserve; external parent word increments survive cold projection eviction/recreation; reports heap facts |

The pressure probe's `Projection` retains a parent array and slot; it does not
allocate a 16 KiB payload for each projection. Its estimates deliberately drive
the budget policy. It is not a per-object sizing or actual 16 KiB reclamation
measurement. The [workflow][workflow] compiles only the arena and probe with
Java 21 strict warnings, then invokes it with `-Xms8m -Xmx32m`. Source existence
and workflow configuration are not a successful run receipt. The
[qualification follow-up][followup] itself retains `ADDED_NOT_YET_RUN` and open
integration gates; its Maven-coordinate statement does not override the actual
POM's test dependency below.

The [standalone POM][pom] declares release 21, JUnit
`5.10.2-m3-source-1`, compiler `-Xlint:all,-module -Werror`, and off-module-path
test compilation/Surefire. The [control reactor][reactor] includes collections
through `m3-extended`, not its default module list. Neither the POM nor the
workflow was run or edited. Dependency availability, recipe replay, compiler,
JUnit, full reactor, native/JNI, installed-runtime, performance and GUI/receiver
gates are separate. The complete module source/test search and named pressure
caller are a bounded caller review, not a whole-repository call-graph proof.

Missing executed evidence includes nested active-use eviction, transition
reentrancy, observe-only overflow, zero-weight limits, changed resident estimates,
failed/null factory/decode/encode/read/delete, hostile store effects, corrupted
snapshots, `Error` recovery, indirect escape, multi-threaded access, tick overflow,
resource disposal and actual retained-heap/native-memory costs.

### Exact source identities

| Source | Git blob at the pinned revision |
| --- | --- |
| [M3ObjectResidency.java][arena] | `0281f85fe543bee202632cae5cf79b536b2a1d70` |
| [M3Residency.java][annotation] | `34c18f45632096c3bd4f0df199fe99a1a59c2f16` |
| [M3ObjectResidencyTest.java][test] | `0deb35e3e8b8a8e44b94b3aab03cac0085dd4c07` |
| [M3ObjectResidencyRuntimePressureTest.java][heap-test] | `19cad07982d1a1222f52cc61fd2168e50ce1f55b` |
| [M3ResidencyPressureProbe.java][probe] | `0b5b432b8646194f389a1494267e4e9242412a57` |
| [Pressure workflow][workflow] | `e57eaacbc57ddeee0344b689f573144c66be470a` |

Only the arena and annotation count as new primary-owner documentation. Nested
interfaces/records/enums/exceptions, private helpers, fields, complete tests and
caller/build bodies were inspected to establish the bounded reference. That is
not package, repository, target-runtime or receiving-phase completion.

[arena]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/java/com/m3/collections/M3ObjectResidency.java
[annotation]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/java/com/m3/collections/M3Residency.java
[test]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/test/java/com/m3/collections/M3ObjectResidencyTest.java
[heap-test]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/test/java/com/m3/collections/M3ObjectResidencyRuntimePressureTest.java
[probe]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/tooling/object-residency/M3ResidencyPressureProbe.java
[workflow]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/.github/workflows/m3-object-residency-pressure.yml
[design]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/docs/m3-parent-owned-object-residency.md
[recipe]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/tooling/object-residency/README.md
[matrix]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/tooling/object-residency/ATOM_MATRIX.tsv
[followup]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/tooling/object-residency/QUALIFICATION_FOLLOWUP.tsv
[module]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/java/module-info.java
[pom]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/pom.xml
[reactor]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/pom.xml
[policy]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/docs/m3jdk21-porting-invariant.md
