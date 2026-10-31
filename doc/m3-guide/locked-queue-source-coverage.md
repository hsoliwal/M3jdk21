<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Locked collections and queue source coverage

[Guide](README.md) · [Locked map contracts](specify-locked-map-views.md) · [Queue contracts](specify-blocking-and-transfer-queues.md) · [Coverage ledger](coverage.md)

## Scope and evidence status

Source pin: `1011050da4205a901ad079d7711b6b8efd8a9af7`, inspected on
14 October 2026. The five primary files below were read in full, including
fields, constructors, public/package/private methods, nested entries and
anonymous view/iterator bodies. Their Git blobs were also checked at
`7abdf87cb497d0d94abeb723d70c50f856ecc381` and were unchanged.
This is source-level documentation of five concrete owners, not complete
coverage of `com.m3.collections` or inherited JDK implementations.

**NOT_RUN:** compilation, JUnit, self-tests, allocation probe, Maven,
recipe verification, receiver checks, native builds, jtreg and runtime
qualification. Test descriptions below report inspected assertions, not
successful executions. Complexity/resource remarks in the references are
source deductions rather than measurements.

The physical owners are in
`m3/collections/src/main/java/com/m3/collections/`. They are separately
licensed Apache-2.0 library types, not identically named internal owners in
`jdk.internal.mindex` or replacements for the public `java.util` classes.
The source notices retain Synexia attribution and package/type-adaptation
history. [Module provenance and mapping][mapping] record donor identities;
this pass changes no source, recipe, policy, mapping or receiving status.

## Primary source hashes

The raw SHA-256 values identify exact UTF-8 source bytes; Git blob hashes
include Git's blob header. Each full file has a primary contract route in
one of the two linked references.

| File | Git blob | Raw SHA-256 |
| --- | --- | --- |
| [M3LockedMap.java](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/jdk/m3/collections/M3LockedMap.java) | `d19d5dae538692fbc54b30bb971dcaf708a12120` | `811f21c7a23721afdf85834c6818bbeeddd3c381d4781d5343a04b8ef0286640` |
| [M3LockedTreeMap.java](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/jdk/m3/collections/M3LockedTreeMap.java) | `65c544c78092f4f365c26904aac45b11e574760a` | `d75d122d4c0af398d63bd90518e83e73cdd178d27471115f2b974fdcd802b4a9` |
| [M3LockedTreeSet.java](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/jdk/m3/collections/M3LockedTreeSet.java) | `8a4428a9c535b5e136851f7ca040959a7aff4170` | `c99c46d5a1eae989712bc05cf421276d41afde32349fa5450863360901036ffe` |
| [M3BlockingDeque.java](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/jdk/m3/collections/M3BlockingDeque.java) | `7dac1b0557defd213dc7cb571ff56a3e81c38bc2` | `b45503c20440b44adccf4044789335f5eed00055184d0580f72308c8e6407a8b` |
| [M3TransferQueue.java](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/jdk/m3/collections/M3TransferQueue.java) | `f9f2eac06609acbe5a4c88b02f21ddc4e3c6f0ea` | `0a1de0629f93118093ee54a2fdec878dbbb2ef40da74d4d9085303c67d28d959` |

## M3LockedMap member closure

The full class is [lines 21–151][locked-map]. Its public constructor uses
an insertion-order `M3LinkedHashMap`; its package constructor accepts the
delegate and monitor used by the tree subclass.

| Declared members / state | Source lines | Documented contract |
| --- | --- | --- |
| Fields `map`, `mutex`; constructors `M3LockedMap()`, `M3LockedMap(M3SlotMap,Object)`; `nonnull` | 28–33 | Delegate/monitor identity, constructor ownership, null checks |
| `size`, `isEmpty`, `get`, `getOrDefault`, `containsKey`, `containsValue` | 34–39 | Per-call monitor, absence and null fallback/argument distinction |
| `put`, `putIfAbsent`, both `remove`, both `replace`, `clear` | 40–49 | Locked update/conditional comparison, null refusal and conditional-null false result |
| `putAll` | 50 | Source callbacks and destination per-entry locking; partial completion |
| `computeIfAbsent`, `computeIfPresent`, `compute`, `merge`, `replaceAll`, `forEach` | 51–66 | Locked callbacks, result/null rules, delegate revision checks, exceptions and partial effects |
| `hashCode`, `equals`, `toString` | 67–79 | Lock scope, foreign user code, self-equality fast path and direct self-reference text |
| `snapshot` | 80–86 | Locked key/value reference copy, checked size multiplication |
| `iterator(int)`; anonymous fields `at`, `last`; `hasNext`, `next`, `remove` | 87–103 | Detached traversal, kind selection, exhausted/invalid removal, conditional original-pair removal |
| `SnapshotEntry`; fields `key`, `value`; constructor; `getKey`, `getValue`, `setValue`, `hashCode`, `equals`, `toString` | 104–116 | Local snapshot value and equality/hash; write-through put, stale reinsertion and local old return |
| `keySet` and anonymous `size`, `contains`, `remove`, `clear`, `spliterator`, `iterator` | 117–126 | Backed size/mutations, snapshot key iterator and concurrent distinct adapter |
| `values` and anonymous `size`, `contains`, `clear`, `spliterator`, `iterator` | 127–135 | Backed value view, inherited snapshot-based removals, non-distinct adapter |
| `entrySet` and anonymous `size`, `clear`, `spliterator`, `iterator`, `contains`, `remove` | 136–150 | Backed view, snapshot projections, malformed/null entry handling and conditional mutation |

## M3LockedTreeMap and M3LockedTreeSet member closure

The complete map is [lines 10–50][locked-tree-map], and the complete set is
[lines 12–49][locked-tree-set]. All delegate constructors used here are
eager; the wrapper APIs do not expose the delegate's lazy-value facilities.

| Owner and declared members / state | Source lines | Documented contract |
| --- | --- | --- |
| Map: `tree`; no-arg/comparator/private tree-and-mutex constructors | 12–17 | One delegate plus shared monitor; no nullable keys/values |
| Map: `comparator` | 18 | Fixed view/comparator read outside monitor |
| Map: `firstKey`, `lastKey`, `firstEntry`, `lastEntry`, `pollFirstEntry`, `pollLastEntry` | 19–24 | Locked navigation, empty results, immutable entry snapshots and atomic endpoint removal |
| Map: `lowerKey`, `floorKey`, `ceilingKey`, `higherKey`, `lowerEntry`, `floorEntry`, `ceilingEntry`, `higherEntry` | 25–32 | Non-null search key; comparator/view-relative neighbors |
| Map: `descendingMap`, four-argument `subMap`, two-argument `headMap`/`tailMap` | 33–42 | Shared tree and monitor, bounds and direction |
| Map: two-argument `subMap`, one-argument `headMap`/`tailMap` | 43–45 | Inclusive/exclusive defaults |
| Map: `keySet`, `navigableKeySet`, `descendingKeySet`, `pollKey` | 46–49 | Non-insertable map-key wrappers and key-only endpoint polling operation |
| Set: `map`, `insertable`; no-arg/comparator/package map-and-flag constructors | 14–20 | Standalone Boolean-backed map versus non-insertable map view |
| Set: `size`, `isEmpty`, `contains`, `add`, `remove`, `clear` | 21–29 | Locked delegation, put-if-absent admission, insertion restriction |
| Set: `spliterator`, `iterator`, `descendingIterator` | 30–32 | Shallow key/value snapshot, comparator-preserving sorted adapter, separate size estimate |
| Set: `comparator`, `first`, `last`, `lower`, `floor`, `ceiling`, `higher`, `pollFirst`, `pollLast` | 33–41 | Navigation/empty rules and atomic endpoint removal |
| Set: `descendingSet`, four-argument `subSet`, two-argument `headSet`/`tailSet`, two-argument `subSet`, one-argument `headSet`/`tailSet` | 42–48 | Backed views retain monitor and insertability; exact inclusivity defaults |

The map and set declarations do not wrap every inherited collection method
in a single lock. The reference deliberately distinguishes protected
operations from caller compounds and inherited batches.

## M3BlockingDeque member closure

The complete owner is [lines 19–184][blocking-deque]. Endpoint families
include all overloads and queue/stack aliases.

| Declared members / state | Source lines | Documented contract |
| --- | --- | --- |
| `elements`, `identities`, `head`, `size`, `sequence`, `revision`, `capacity`, `lock`, `notEmpty`, `notFull`; three constructors | 25–42 | Ring representation, positive logical capacity, initial arrays and lock fairness |
| `at`, `value`, `grow` | 43–50 | Ring addressing, typed reads, checked growth and order-preserving copy |
| `insert`, `erase` | 51–67 | Identity exhaustion, head/tail insertion, shifting removal, revision and condition signals |
| Untimed/timed `offerEnd`, `putEnd` | 68–82 | Non-null element, non-interruptible offer versus interruptible blocking/timed acquisition; wait budget |
| Untimed/timed `pollEnd`, `takeEnd`, `peekEnd` | 83–97 | Empty sentinel, waits, endpoint selection and lock release |
| `size`, `remainingCapacity` | 98–99 | Locked observations, no capacity reservation |
| `offerFirst`, `offerLast`, `addFirst`, `addLast`, `putFirst`, `putLast`, timed `offerFirst`/`offerLast` | 100–107 | Endpoint insertion families and full behavior |
| `pollFirst`, `pollLast`, `takeFirst`, `takeLast`, timed `pollFirst`/`pollLast`, `peekFirst`, `peekLast` | 108–115 | Endpoint reads/removals and timed/interruptible variants |
| `required`, `removeFirst`, `removeLast`, `getFirst`, `getLast` | 116–120 | Empty-to-`NoSuchElementException` conversion |
| `offer`, `put`, timed `offer`, `poll`, `peek`, `take`, timed `poll`, `push`, `pop`, `remove(Object)` | 121–130 | Exact queue/stack aliases |
| `removeFirstOccurrence`, `removeLastOccurrence`, `removeOccurrence`, `contains` | 131–146 | Argument equality under lock; null false, direction and linear scan |
| `clear`; both `drainTo` overloads | 147–163 | Reference release, all-producer signal, target-before-erase, revision guard and partial effects |
| `spliterator`, `iterator`, `descendingIterator`, `snapshot`; anonymous `cursor`, `last`, `hasNext`, `next`, `remove` | 164–183 | Snapshot order/reference retention, primitive insertion identities, stale-removal no-op and root adapter flags |

## M3TransferQueue member closure

The complete owner is [lines 19–199][transfer-queue]. Diagnostics and
private ownership transitions are included, not just the interface surface.

| Declared members / state | Source lines | Documented contract |
| --- | --- | --- |
| Constants `DATA`, `TRANSFER`, `REQUEST`, `MATCHED`; lanes `payload`, `before`, `after`, `state`, `identities`; `used`, `free`, chain endpoints/counts, `sequence`, `rendezvous`, `lock`, `changed`; two constructors | 25–40 | Slot state/ownership, buffering choice, initial arrays and lock fairness |
| `allocate` | 41–58 | Growth/free reuse, unique increasing allocation identity and FIFO append |
| `unlink`, `release` | 59–66 | Chain/count changes and cleared free-list ownership |
| `deliver`, `consume`, `cancel` | 67–77 | FIFO matching, DATA release versus matched peer retention, signalling and cancellation |
| `awaitMatch` | 78–89 | Condition loop, timeout cleanup, matched/interrupted race and interrupt restoration |
| `send`, `receive` | 90–109 | Immediate matching, budget checks, waiting-slot allocation, acknowledgement and final release |
| Untimed/timed `offer`, `put`, `transfer`, untimed/timed `tryTransfer` | 110–123 | Buffered versus rendezvous producer behavior, no-enqueue immediate failure, interruption differences |
| Untimed/timed `poll`, `take`, `peek`, `size`, `remainingCapacity` | 124–129 | Receive paths and rendezvous visibility versus available producer data |
| `hasWaitingConsumer`, `getWaitingConsumerCount` | 130–131 | Instantaneous unmatched request counts |
| `contains`, `remove(Object)`, `clear` | 132–145 | Buffered scans/acknowledgement, null false and rendezvous no-ops |
| Both `drainTo` overloads | 146–159 | Any-mode drain, target-before-consume, head-identity guard and partial effects |
| `spliterator`, `iterator`; anonymous `cursor`, `last`, `hasNext`, `next`, `remove` | 160–177 | Buffered snapshot or empty rendezvous snapshot, identity-specific removal and producer acknowledgement |
| `verifyInvariants`, `checkChain` | 178–198 | Explicit lock-protected structural diagnostic, count/link/free/matched ownership assertions |

## Dependency and build reading boundaries

These sources support the references but are not new primary owners in this
five-file closure:

| Source | Read scope and reason |
| --- | --- |
| [M3SlotMap][slot-map] | Complete 235-line body: delegated update/compute/null-result rules, structural checks, immutable snapshots and equality callbacks |
| [M3SlotArrays][slot-arrays] | Complete 24-line body: checked array-growth ceiling and typed array reads |
| [M3SlotSpliterators][slot-spliterators] | Complete 63-line body: unknown-size concurrent adapters; sorted comparator preservation and initial flags |
| [M3LinkedHashMap][linked-map] | Constructor and slot/update/storage dependencies, lines 15–100; detailed primary route remains [the linked-hash reference](collections-reference.md) |
| [M3TreeMap][tree-map] | Eager construction/bounds/find/write/size/clear and navigation/view bodies, lines 24–130 and 225–319; full lazy/AVL owner closure is separate |
| [M3Collections][factories] | Exact factory bodies, lines 128–159, including the JDK set wrapper and true/false rendezvous construction |
| [POM][pom] | Complete descriptor read as text. Standalone `com.m3:m3-collections:0.1.0-SNAPSHOT`, no parent; release 21; JUnit `5.10.2-m3-source-1` in test scope; compiler `3.13.0`, `-Xlint:all,-module`, `-Werror`; Surefire `3.2.5`, module-path tests disabled. Authoring profiles are not runtime dependencies |
| [module-info.java][module] | Complete 5-line descriptor: exports `com.m3.collections`, only implicit `java.base` dependency |
| [Module README][module-readme] | Complete build/receiver instructions read. Historical recipe/test counts are prior receipts, not the result or current test denominator of this pass |

The recorded public mapping relates these owners to Synexia `LockedMap`,
`LockedTreeMap`, `LockedTreeSet`, `PackedBlockingDeque` and
`PackedTransferQueue`. Concrete source, recipe custody and public naming
records do not establish acceptance of a JDK replacement. Repository
[receiving policy][policy] keeps later-family qualification distinct from
this independent module documentation.

## Test and caller evidence: inspected, NOT_RUN

All five test/probe files in this table were read in full. Assertions for
unrelated owners are not counted as coverage of this closure.

| Test source and method(s) | Exact inspected evidence and limitation |
| --- | --- |
| [M3ConcurrentContractTest, 41–51][concurrent-snapshots]: `concurrentStreamsUseSnapshotSizeAndSelfToStringTerminates` | Self-value map string; map keys and both queue spliterators omit SIZED and retain pre-mutation captured values |
| [Same, 52–69][concurrent-maps]: `atomicHashAndTreeCompute` | Six workers each merge 5,000 times over 17 keys; total 30,000; unequal-value update survives stale iterator remove; null put/replaceAll rejection. Finite scenario, not exhaustive race proof |
| [Same, 70–81][concurrent-views]: `lockedRangeAndSetViews` | Backed descending bounded map, out-of-range put, snapshot survives clear, standalone set range insertion and endpoint polling |
| [Same, 82–101][concurrent-deque]: `blockingDequeDifferential` | 30,000 seeded operations against `LinkedBlockingDeque(31)`, order/capacity checks and stale identity after equal-element reinsertion |
| [Same, 102–115][concurrent-waits]: `blockingWakeupTimeoutAndInterrupt` | Bounded offer timeout, producer/consumer wakeups, empty poll timeout and interrupted take |
| [Same, 116–127][concurrent-drain]: `drainFailureRetainsUndeliveredElement` | Both queues retain current/rest elements after target throws on value 2; self-drain rejects. Does not exercise target-add-then-throw, false-return or reentrant callback cases |
| [Same, 128–142][concurrent-transfer]: `transferBufferedDifferentialAndIteratorRecycling` | 10,000 seeded buffered operations against `LinkedTransferQueue`, structural diagnostics and stale identity reuse |
| [Same, 143–161][concurrent-cancel]: `transferWaitersCancellationAndTimeout` | Immediate/timed transfer failure, waiting-consumer count, acknowledgement, producer/consumer interruption and timed receive cleanup |
| [Same, 162–172][concurrent-rendezvous]: `synchronousQueueHasNoCollectionCapacity` | Fair-lock rendezvous mode: no buffering, producer/consumer exchanges and timeout |
| [Same, 173–200][concurrent-mpmc]: `exchange`, `mpmcBlockingDequeHasNoLossOrDuplicates`, `mpmcTransferHasNoLossOrDuplicates`, `mpmcRendezvousHasNoLossOrDuplicates` | Four producers/four consumers, 2,000 items each; each ID asserted once, final emptiness and transfer diagnostics. Not an executed no-loss/duplication finding |
| [M3JdkCollectionDifferentialTest, 84–102][differential-sort]: `everySortedSplitRetainsCustomComparatorAndEncounterOrder`, `collectSorted` | Locked set included with 4,096 elements; recursive splits retain reverse comparator/order, parallel output matches |
| [Same, 213–255][differential-deque]: `boundedDequeWrapDrainAndTimedOperationsMatchJdk`, `blockedProducerInterruptionLeavesNoPhantomElement` | Capacities 1/2/7, 1,800 seeded operations each, zero-timeout paths, drain/null/self checks; blocked virtual-thread producer interruption leaves original item |
| [Same, 257–297][differential-transfer]: `transferAcknowledgementTimeoutAndCancellationMatchJdk`, `zeroCapacityRendezvousPreservesSynchronousQueueContract` | Immediate/timed transfer refusal, waiting consumer delivery, interrupted producer cleanup; rendezvous hides pending producer from size/peek/iterator yet timed poll receives it |
| [Same, 299–314][differential-maps]: `lockedMapAtomicCallbacksLoseNoUpdates` | Both locked maps and JDK `ConcurrentHashMap`: four workers, 2,000 merges each, null rejection and exact final counts |
| [M3FactoryContractTest, 25–55][factory-test]: `allFactoryFamiliesAreUsable` | Locked factory emptiness/set additions; deque offers; buffered transfer true and unmatched synchronous offer false. Smoke calls alone do not prove all owner contracts |
| [M3NoRetainedStructuralEntryTest, 30–58 and 122–161][structural-test] | The roots include both queues, not the three locked wrappers. Reflection checks declared field component types/nested classes for retained structural carriers. Not an exhaustive object-graph or resource proof |
| [M3AllocationProbe, 23–58][allocation-probe] | Prepared ring offer/poll, buffered transfer offer/poll and locked-map put/get/remove labels. Warms 300,000 iterations; best of three million-operation allocated-byte trials must be zero. Unrun; not throughput, contended-wait or retained-heap evidence |

Package production callers were checked in the facade and wrapper
relationships: `M3Collections` constructs all five owners, `M3LockedTreeMap`
extends `M3LockedMap`, and its key sets create `M3LockedTreeSet` wrappers.
No claim of a complete repository-wide inbound caller graph is made.

Source-derived caveats without a dedicated inspected assertion include:
private-monitor versus external synchronization, per-entry `putAll`, stale
entry `setValue` reinsertion, original-pair removal after `setValue`, null
conditional-removal asymmetry, callback partial effects, timeout excluding
initial lock-acquisition delay, identity exhaustion, matched/interrupt race
ordering, maintenance operations acknowledging transfers, false-return or
reentrant drain callbacks, and rendezvous draining a waiting producer.
These remain explicit behavioral test opportunities rather than presumed
passing tests.

## Supporting source hashes

The following exact files anchor the dependency, factory, test and build
statements above. They do not increase the primary-owner count.

| File | Git blob | Raw SHA-256 |
| --- | --- | --- |
| [M3SlotMap.java](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/jdk/m3/collections/M3SlotMap.java) | `a10ee53abb325476ac32c7210712e30cc304182c` | `aff532279cb68435619ef39977f4709125d9061c7a67259df10cfc33c7bfd66f` |
| [M3SlotArrays.java](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/jdk/m3/collections/M3SlotArrays.java) | `e337c69710c871f8caa77a1afe53149f1fd705cc` | `4e978861b03650cec9ab1f064648cc8d342117d8b4f0556152604707c83c2518` |
| [M3SlotSpliterators.java](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/jdk/m3/collections/M3SlotSpliterators.java) | `dab4d257bf82c8dbdf8fec04e1ac0d84ac7e4a83` | `fe7db5946cb21eecac31dc478899c4864bba09b7b302357868af296ea2e0f554` |
| [M3LinkedHashMap.java](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/jdk/m3/collections/M3LinkedHashMap.java) | `e0957cfd8a7a5abfa1c0c5e7bade32997e5ed657` | `236099129854887ba6205be23e42d5e6af9ead09de045419f7c940d0f0bd77e6` |
| [M3TreeMap.java](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/jdk/m3/collections/M3TreeMap.java) | `d306dacbb73d48c955fa0b932acc2c3d1eae5626` | `655ec43b42c95738d042bd762992ba5d97ef6cd07a374fa05244064e46e2c52b` |
| [M3Collections.java](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/jdk/m3/collections/M3Collections.java) | `9b49f723dfa91628978e26db2564e0282a3e78a9` | `ce8bf96283a32f43e60031f9b201ff1a07091b5810303ea096c4c5f58bdfd39a` |
| [M3ConcurrentContractTest.java](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/jdk/m3/collections/M3ConcurrentContractTest.java) | `c4831e0d79dc221070e703f121f1f9ea2febf268` | `1c0135c30a6b5680514c09673817e866047e5099d2db8223ff7ce81a100aad13` |
| [M3JdkCollectionDifferentialTest.java](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/jdk/m3/collections/M3JdkCollectionDifferentialTest.java) | `a6898c609081aea5205e003b0b3d2effa0ebe417` | `2bc7d6873f89b74eaf4b495f8ce97ce581278680ceb03898e1ced99aae3ba5ab` |
| [M3FactoryContractTest.java](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/jdk/m3/collections/M3FactoryContractTest.java) | `d11d43dd29b15a919871e49ed57804af834cb66c` | `ed7c565b4c5f6c9745ad26d994f4d0bc25d4fe84795a091845136ed39c4ddbdd` |
| [M3NoRetainedStructuralEntryTest.java](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/jdk/m3/collections/M3NoRetainedStructuralEntryTest.java) | `effe339fd3945b49307ae97f219e0a2837e752c9` | `05dbc94d16aa7093ff3ac963cd4eac4cd276ead2efac8028cafe96c812683e21` |
| [M3AllocationProbe.java](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/jdk/m3/collections/M3AllocationProbe.java) | `8259f60f7dbc0ba154bb4635a302c4977063740d` | `66048c93155698f1984ac876a03c830c7d10b0d55a0caa00f4d7c970832cf893` |
| [pom.xml](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/pom.xml) | `e82f308c1bc1e05807a6e6bf3104fe2d14664fa2` | `98f6315a27af070fba3d372427b74cec51812ee096940a02d8a4d679eb508064` |
| [module-info.java](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/module-info.java) | `bcb0af11a4d6038bc79ec582e71c63f218468a6e` | `bc1d6962ec48e236346d6ec70d5a1da284276f18bdf5b295cb210f1c2b8fcfc6` |
| [README.md](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/README.md) | `622b61d4b719fd6879ef8df72ffbcfbaa7e54918` | `e72ff3d256fa47d49d5478532974e837af1a7a53d399655283c79fbd482d9dd0` |
| [name-mapping.json](https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/name-mapping.json) | `7b5622619035dbfe12218da878b5b5df21ea2ad0` | `e976621377edae456e5c69308208b9a193d81c2d221cd137afefcd2d07e67b94` |

[locked-map]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/jdk/m3/collections/M3LockedMap.java#L21-L151
[locked-tree-map]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/jdk/m3/collections/M3LockedTreeMap.java#L10-L50
[locked-tree-set]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/jdk/m3/collections/M3LockedTreeSet.java#L12-L49
[blocking-deque]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/jdk/m3/collections/M3BlockingDeque.java#L19-L184
[transfer-queue]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/jdk/m3/collections/M3TransferQueue.java#L19-L199
[slot-map]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/jdk/m3/collections/M3SlotMap.java
[slot-arrays]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/jdk/m3/collections/M3SlotArrays.java
[slot-spliterators]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/jdk/m3/collections/M3SlotSpliterators.java
[linked-map]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/jdk/m3/collections/M3LinkedHashMap.java
[tree-map]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/jdk/m3/collections/M3TreeMap.java
[factories]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/jdk/m3/collections/M3Collections.java#L128-L159
[pom]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/pom.xml
[module]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/module-info.java
[module-readme]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/README.md
[mapping]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/name-mapping.json
[policy]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/docs/m3jdk21-porting-invariant.md
[concurrent-snapshots]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/jdk/m3/collections/M3ConcurrentContractTest.java#L41-L51
[concurrent-maps]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/jdk/m3/collections/M3ConcurrentContractTest.java#L52-L69
[concurrent-views]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/jdk/m3/collections/M3ConcurrentContractTest.java#L70-L81
[concurrent-deque]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/jdk/m3/collections/M3ConcurrentContractTest.java#L82-L101
[concurrent-waits]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/jdk/m3/collections/M3ConcurrentContractTest.java#L102-L115
[concurrent-drain]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/jdk/m3/collections/M3ConcurrentContractTest.java#L116-L127
[concurrent-transfer]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/jdk/m3/collections/M3ConcurrentContractTest.java#L128-L142
[concurrent-cancel]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/jdk/m3/collections/M3ConcurrentContractTest.java#L143-L161
[concurrent-rendezvous]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/jdk/m3/collections/M3ConcurrentContractTest.java#L162-L172
[concurrent-mpmc]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/jdk/m3/collections/M3ConcurrentContractTest.java#L173-L200
[differential-sort]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/jdk/m3/collections/M3JdkCollectionDifferentialTest.java#L84-L102
[differential-deque]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/jdk/m3/collections/M3JdkCollectionDifferentialTest.java#L213-L255
[differential-transfer]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/jdk/m3/collections/M3JdkCollectionDifferentialTest.java#L257-L297
[differential-maps]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/jdk/m3/collections/M3JdkCollectionDifferentialTest.java#L299-L314
[factory-test]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/jdk/m3/collections/M3FactoryContractTest.java#L25-L55
[structural-test]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/jdk/m3/collections/M3NoRetainedStructuralEntryTest.java#L30-L161
[allocation-probe]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/jdk/m3/collections/M3AllocationProbe.java#L23-L58
