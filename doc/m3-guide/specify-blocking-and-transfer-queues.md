<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Blocking deque and transfer queue contracts

[Guide](README.md) · [Locked maps](specify-locked-map-views.md) · [Source and test evidence](locked-queue-source-coverage.md)

This reference covers every declared member of `M3BlockingDeque` and
`M3TransferQueue` at source pin
`1011050da4205a901ad079d7711b6b8efd8a9af7`. Both are public final classes in
the independent `com.m3.collections` module. Both extend `AbstractQueue`;
the former implements `BlockingDeque`, the latter `TransferQueue`.
Complete source bodies and the relevant test assertions were read.
Tests, examples, Maven, recipes and runtime checks are **NOT_RUN**.
Implementing these interfaces is not proof of universal JDK equivalence,
a JDK backend replacement, a stable serialized form or runtime qualification.

## Choose the actual owner and mode

| Constructor/factory | Behavior |
| --- | --- |
| `new M3BlockingDeque<>()`; `lockedQueue()`; `lockedDeque()` | Growable ring with logical capacity `Integer.MAX_VALUE - 8`, non-fair lock |
| `new M3BlockingDeque<>(capacity)`; `blockingQueue(capacity)`; `blockingDeque(capacity)` | Positive logical capacity, non-fair lock |
| `new M3BlockingDeque<>(capacity, fair)` | Same bounded ring with `ReentrantLock(fair)` |
| `new M3TransferQueue<>()`; `transferQueue()` | Buffered FIFO transfer mode with non-fair lock |
| `new M3TransferQueue<>(false, fair)` | Buffered mode with chosen lock fairness |
| `new M3TransferQueue<>(true, fair)`; `synchronousQueue(fair)` | Zero-collection-capacity rendezvous mode |

[Factory bodies][factory] construct these concrete M3 owners.
`arrayBlockingQueue`, `priorityBlockingQueue` and `delayQueue` return other
JDK owners and are not aliases for either class here. The names
`lockedQueue` and `blockingQueue` do not restrict the returned object's
full deque API. There is no collection-copy constructor in either class.

Both store strong Java payload references in arrays with primitive
identity/topology lanes. Payloads are not cloned, immutable or off-heap.
Locks, conditions, array growth, snapshots and JVM wait machinery can
allocate. The absence of per-element structural-node fields establishes
neither zero allocation nor measured memory savings. Removed references are cleared
from live storage, but a previously created iterator can retain its captured
payloads. Neither class has close/shutdown/cancel-all operations.

## Shared locking, time and interruption boundaries

All mutable queue state is protected by a private `ReentrantLock`. The
`fair` constructor argument is passed to that lock, not to a custom
scheduler. Do not infer bounded latency, thread-start ordering, freedom
from starvation or lock-free progress. Even a nonblocking `offer`/`poll`
can wait to acquire the lock. Timed waiting paths use `lockInterruptibly()`,
not timed lock acquisition: their supplied nanoseconds are spent in condition
waits, and time spent first obtaining the lock is not subtracted. They are
not end-to-end wall-clock deadlines. Buffered `M3TransferQueue` timed
`offer` instead follows ordinary non-interruptible `offer` and ignores its
timeout, as detailed below.

[Deque waits][deque-waits] and [transfer matching][transfer-waits] check
predicates in loops and release the lock in `finally` after acquisition.
Condition waits release the lock while waiting and reacquire it before the
method resumes. Timed waiting paths use `TimeUnit.toNanos`; null units throw
`NullPointerException`. A nonpositive timeout can still succeed immediately
if the required space, data or waiting peer is already available. Failure,
interruption or timeout is not a rollback of arbitrary user callbacks.

## M3BlockingDeque: capacity and endpoint operations

[Construction and storage][deque-storage] reject capacity `<= 0` with
`IllegalArgumentException`. Initial payload/identity arrays have
`min(capacity,8)` slots. Growth copies current logical order to new arrays,
resets the head to zero, and uses the smaller of logical capacity and
[`M3SlotArrays.grow`][growth]. The helper grows by roughly 1.5 times plus one,
with a minimum of 8 and array ceiling `Integer.MAX_VALUE - 8`.
A very large explicit logical capacity is accepted even above that ceiling;
actual insertion can fail with `OutOfMemoryError` before `remainingCapacity`
reaches zero. Ordinary allocation failure is also possible.

`size` and `remainingCapacity` each lock and return current size and
`capacity - size`; they do not reserve space. Each insertion rejects null,
assigns a new increasing long identity, increments revision/size and signals
one `notEmpty` waiter. At `Long.MAX_VALUE` identity sequence, a further
insertion throws `IllegalStateException("iterator identity space exhausted")`
before growth or insertion. Neither removal nor `clear` resets the sequence.

The [endpoint API][deque-endpoints] is symmetric:

| Family | Result and waiting |
| --- | --- |
| `offerFirst`, `offerLast` | Non-interruptible lock acquisition; false if logically full, otherwise insert and true |
| `addFirst`, `addLast` | Same insertion attempt; `IllegalStateException("deque full")` if offer returns false |
| `putFirst`, `putLast` | Interruptible acquisition, then wait on `notFull` while full, then insert |
| Timed `offerFirst`, `offerLast` | Interruptible acquisition; wait while full within the condition budget; false on expiry |
| `pollFirst`, `pollLast` | Remove corresponding endpoint or return null when empty |
| `takeFirst`, `takeLast` | Interruptible acquisition and wait on `notEmpty` while empty, then remove |
| Timed `pollFirst`, `pollLast` | Interruptible acquisition; return endpoint or null on empty timeout |
| `peekFirst`, `peekLast` | Read endpoint without removal, or null when empty |
| `removeFirst`, `removeLast` | Poll counterpart, but throw `NoSuchElementException` when empty |
| `getFirst`, `getLast` | Peek counterpart, but throw `NoSuchElementException` when empty |

[Aliases][deque-aliases]: `offer`, `put` and timed `offer` append at the last
end; `poll`, `peek`, `take` and timed `poll` use the first end. `push` is
`addFirst`, `pop` is `removeFirst`; `remove(Object)` is
`removeFirstOccurrence`. The inherited `AbstractQueue` methods use these
primitive operations; the class adds no special atomic batch protocol for
inherited collection operations.

`removeFirstOccurrence` scans forward and `removeLastOccurrence` backward,
using the argument's `equals` against each element under the lock.
`contains` scans similarly. They return false for null. Erasing an interior
element shifts the nearer portion of the ring while moving its identities
with the payloads, clears the vacated slot, increments revision, and signals
one `notFull` waiter. The scan/shift is linear, unlike endpoint removal.
`clear` clears both full arrays, resets head and size, increments revision
even if already empty, and signals all `notFull` waiters. It does not wake
empty consumers with a shutdown result. See [search, clear and drain][deque-bulk].

Blocking insertion/removal uses `lockInterruptibly` and interruptible
condition waits. If a wait throws before insertion/removal, the method
propagates `InterruptedException` with no new element or removal from that
operation. There is no separate per-waiter queue slot to clean up in this
ring. The implementation relies on the lock/condition interruption rules;
it does not implement the transfer queue's explicit matched-versus-interrupt
state machine.

## M3TransferQueue: slots, peers and acknowledgement

[State storage][transfer-storage] starts with arrays of length 8 and grows
through the same checked helper. Primitive links maintain one FIFO data
chain and one FIFO waiting-request chain, plus a free-slot chain. The four
live states are:

| State | Meaning and release owner |
| --- | --- |
| `DATA` | Buffered item whose producer does not await acknowledgement; consumer unlinks and releases its slot |
| `TRANSFER` | Item whose producer waits for matching; consuming unlinks it, clears its payload and marks `MATCHED`; producer releases the slot after wakeup |
| `REQUEST` | Consumer waiting for an item; delivery unlinks it, stores the item and marks `MATCHED`; consumer reads and releases the slot |
| `MATCHED` | Detached acknowledged slot still owned by the waiting sender or receiver until that waiter resumes |

`allocate` assigns one never-reused long identity to every allocated slot,
including a waiting `REQUEST`, then appends it to the appropriate chain.
A freed slot can be reused but receives a new identity. At
`Long.MAX_VALUE`, a further allocation throws the same identity-exhaustion
`IllegalStateException` before modifying the slot. Operations that directly
match an existing peer need not allocate a new slot. No clear operation
resets this sequence.

[`deliver`][transfer-matching] matches the first waiting request and signals
all waiters on `changed`. `consume` takes the selected data slot: DATA is
freed immediately, whereas TRANSFER becomes MATCHED and signals all waiters.
Ordinary receiving chooses the first data slot. Matching is FIFO within
these pending chains; `remove(value)` and iterator removal deliberately can
select a later item. FIFO chain selection does not mean fair scheduling or
that all awakened threads finish in order.

[Producer operations][transfer-producers]:

| Operation | Buffered mode | Rendezvous mode |
| --- | --- | --- |
| `offer(value)` | Deliver to an existing request, else append DATA and true | Deliver to an existing request, else false |
| `put(value)` | Calls ordinary `offer`; no interrupt check or condition wait is added | Send TRANSFER and wait for a match if no consumer is already waiting |
| Timed `offer(value,timeout,unit)` | Checks non-null unit then ordinary `offer`; timeout is ignored | Timed send, false if it cannot match before its wait budget expires |
| `transfer(value)` | Deliver to waiting consumer or enqueue TRANSFER and wait | Same matching/acknowledgement procedure |
| `tryTransfer(value)` | Only immediate delivery to a waiting consumer; false does not enqueue | Same |
| Timed `tryTransfer(value,timeout,unit)` | Timed send, potentially enqueuing TRANSFER until matched, interrupted or expired | Same |

All producer methods reject a null element. Ordinary `offer` and immediate
`tryTransfer` acquire the lock non-interruptibly. Buffered `put` and timed
`offer` also follow that non-interruptible `offer` path despite declaring
`InterruptedException`. Transfer sends and rendezvous waits acquire the
lock interruptibly. Timed send tries immediate delivery before checking a
nonpositive budget; only a positive remaining budget creates a waiting
TRANSFER slot.

[Consumer operations][transfer-consumers]: `poll()` immediately consumes
first data if present, otherwise returns null; it takes the lock
non-interruptibly. `take()` and timed `poll` acquire interruptibly and first
try the data chain. If no data exists, untimed take allocates a REQUEST;
timed poll does so only for a positive wait budget. These requests appear
in `hasWaitingConsumer` / `getWaitingConsumerCount` until matched or
cancelled. Counts are instantaneous observations, not reservations for a
later producer and not a count of already-matched consumers awaiting CPU.

### Timeout and interruption races

[`awaitMatch`][transfer-waits] checks MATCHED before timeout on each loop.
If an unmatched timed request/transfer expires, it unlinks and releases its
slot, then the caller returns null/false. If an interrupt is caught while
the slot is still unmatched, it cancels/releases and rethrows
`InterruptedException`. If matching has already won, it restores the
thread's interrupt flag and reports success; the caller finishes the
handoff and releases its matched slot. Thus a completed match is not
undone by that race. Interruption during lock acquisition occurs before
allocating a waiting slot. Exceptional allocation does not establish a
successful send or receive.

### Buffered collection methods can acknowledge a producer

In buffered mode `size` counts both DATA and still-unmatched TRANSFER
items. `peek`, `contains` and the snapshot iterator expose both kinds.
`remainingCapacity` always reports `Integer.MAX_VALUE`; it is an API
unboundedness indicator, not an allocation guarantee or count of free
array slots. Null `contains`/`remove` return false.

[`remove(value)` and `clear`][transfer-bulk] use `consume`, as do iterator
removal and `drainTo`. Removing a TRANSFER item therefore acknowledges its
waiting producer as successful even when the item was removed by collection
maintenance rather than delivered by `take`. `clear` repeatedly consumes
all pending data; it leaves waiting consumer requests in place. Do not use
it as a cancellation API for either peer kind.

### Rendezvous visibility intentionally differs from availability

When `rendezvous` is true, `size` and `remainingCapacity` return zero,
`peek` returns null, `contains` and `remove(Object)` return false, `clear`
is a no-op, and `iterator()` snapshots zero elements. This remains true
while a producer has a pending TRANSFER slot. Yet `poll`, `take`, timed
`poll` and `drainTo` can consume that producer's item and acknowledge it.
Consequently `isEmpty`, iteration or peek cannot establish that no producer
is waiting. The class exposes waiting-consumer counts, not a waiting-producer
count. A rendezvous `clear` does not unblock or interrupt a producer.

## Drain callbacks and partial failure

Both [`drainTo` implementations][deque-bulk] reject a null target and
`target == this` before draining, even if the requested maximum is
nonpositive. A nonpositive maximum drains zero. The no-maximum overload
uses `Integer.MAX_VALUE`. Both call `target.add` under the source lock
before consuming/removing the current item, and ignore its boolean return.
A target returning false normally still causes source removal. If `add`
throws without modifying the source, that current item is retained and
earlier successfully processed items remain removed. There is no rollback
of the target; if it adds then throws, the current item can exist in both.

The reentrancy checks deliberately differ:

- Deque drain fixes `n = min(initialSize,max(0,maximum))`, records its
  revision before each callback, and throws `ConcurrentModificationException`
  if the callback changes that revision. It then erases the first element
- [Transfer drain][transfer-drain] loops while count is below maximum and
  data exists. It records first-slot index and identity, then requires both
  to remain the first item after the callback. Changing the head or recycling
  it is detected; appending/removing a later item need not be. Reentrant
  additions can extend what this loop drains, up to the requested maximum

A detected mutation has already occurred and is not undone. Calling code
should avoid mutating the source from target callbacks, including aliases
that bypass the direct `target == this` test. Both locks are reentrant;
neither class supplies a general reentrant-mutation guard for element
`equals` during `contains`/occurrence removal either. External callbacks
can also create conflicting lock orders with another collection.

## Iterators, snapshots and removal identity

[Deque iterators][deque-iterator] copy element references and long identities
under one lock in forward or reverse order. [Transfer iterators][transfer-iterator]
copy the buffered data chain in FIFO order, or no entries in rendezvous
mode. Later mutations do not change these shallow snapshots. Their
`hasNext`/`next` run without the owner lock; exhausted `next` throws
`NoSuchElementException`. Sharing one iterator concurrently is not given
an additional synchronization guarantee.

`remove` requires a preceding unremoved `next`, otherwise
`IllegalStateException`. It locks the owner and searches current live items
for the captured identity. If still present, that exact insertion is removed;
if already gone, removal is a no-op. Equal payloads and recycled physical
slots cannot make the old iterator remove a new insertion. The attempt
consumes the iterator's remove permission either way. In transfer queues,
removing a still-pending TRANSFER identity acknowledges the producer.
Traversal is not fail-fast: revision/identity checks protect the specified
mutations, not snapshot `next`.

Both spliterators wrap an already-created snapshot iterator using
[`concurrent(iterator,true,false)`][spliterators]. The root adapter is
unknown-size and advertises `CONCURRENT | NONNULL | ORDERED`, not `SIZED`
or `DISTINCT`. Splitting may allocate batches. These flags do not promise
late binding or visibility of later queue additions. Snapshot creation and
iterator-removal search cost grow with the number of captured/current items.

## Invariant checks, build and test boundaries

Only `M3TransferQueue` declares [`verifyInvariants()`][invariants]. Under
the lock it allocates a `boolean[used]`, traverses data/request/free chains,
checks counts, predecessor links, tails, duplicate ownership and expected
states, permits detached MATCHED slots, and rejects simultaneous unmatched
data and requests. Failure throws `AssertionError` with the relevant queue
message. This is an explicit structural diagnostic, not automatically run
on each operation and not a linearizability, scheduling or performance proof.
`M3BlockingDeque` has no corresponding public diagnostic method.

The [evidence companion](locked-queue-source-coverage.md) maps every declared
member and relevant assertion, including timed/interrupted waits, stale
iterator slot reuse, bounded wraparound, rendezvous visibility and finite
multi-producer/multi-consumer no-loss scenarios. Those tests were **NOT_RUN**;
reading their assertions does not make no-loss/duplication or fairness an
executed result. The prepared allocation probe is likewise unrun and cannot
supply a throughput, retained-heap or contended-wait allocation claim.

The standalone [POM][pom] targets Java 21; the [module descriptor][module]
exports `com.m3.collections` with only implicit `java.base` at runtime.
[Module build/receiver instructions][module-readme] distinguish historical
and current recipe qualification. This documentation executes neither route
and does not advance the repository's receiving phase or qualify a
`java.util` backend.

[factory]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3Collections.java#L128-L159
[deque-waits]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3BlockingDeque.java#L68-L97
[transfer-waits]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3TransferQueue.java#L78-L109
[deque-storage]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3BlockingDeque.java#L25-L67
[growth]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3SlotArrays.java#L14-L17
[deque-endpoints]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3BlockingDeque.java#L98-L120
[deque-aliases]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3BlockingDeque.java#L121-L132
[deque-bulk]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3BlockingDeque.java#L133-L163
[transfer-storage]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3TransferQueue.java#L25-L66
[transfer-matching]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3TransferQueue.java#L67-L77
[transfer-producers]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3TransferQueue.java#L110-L123
[transfer-consumers]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3TransferQueue.java#L124-L131
[transfer-bulk]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3TransferQueue.java#L132-L145
[transfer-drain]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3TransferQueue.java#L146-L159
[deque-iterator]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3BlockingDeque.java#L164-L183
[transfer-iterator]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3TransferQueue.java#L160-L177
[spliterators]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3SlotSpliterators.java#L26-L31
[invariants]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3TransferQueue.java#L178-L198
[pom]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/pom.xml
[module]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/module-info.java
[module-readme]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/README.md
