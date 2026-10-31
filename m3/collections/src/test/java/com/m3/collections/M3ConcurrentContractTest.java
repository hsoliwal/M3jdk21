// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(30)
class M3ConcurrentContractTest {
    private static void await(BooleanSupplier ready) {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (!ready.getAsBoolean()) {
            if (System.nanoTime() - deadline >= 0) { throw new AssertionError("condition timed out"); }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
        }
    }
    @Test void concurrentStreamsUseSnapshotSizeAndSelfToStringTerminates() {
        var map = new M3LockedMap<String, Object>(); map.put("self", map); assertEquals("{self=(this Map)}", map.toString());
        var keys = map.keySet().spliterator(); map.put("later", 3);
        assertFalse(keys.hasCharacteristics(java.util.Spliterator.SIZED));
        assertEquals(List.of("self"), java.util.stream.StreamSupport.stream(keys, true).toList());
        for (BlockingQueue<Integer> queue : List.of(new M3BlockingDeque<Integer>(), new M3TransferQueue<Integer>())) {
            queue.addAll(List.of(1, 2, 3)); var snapshot = queue.spliterator(); queue.clear(); queue.add(4);
            assertFalse(snapshot.hasCharacteristics(java.util.Spliterator.SIZED));
            assertEquals(List.of(1, 2, 3), java.util.stream.StreamSupport.stream(snapshot, true).toList());
        }
    }
    @Test void atomicHashAndTreeCompute() throws Exception {
        for (M3LockedMap<Integer, Integer> map : List.of(new M3LockedMap<Integer, Integer>(), new M3LockedTreeMap<Integer, Integer>())) {
            try (var pool = Executors.newFixedThreadPool(6)) {
                var start = new CountDownLatch(1); var jobs = new ArrayList<Future<?>>();
                for (int worker = 0; worker < 6; worker++) {
                    jobs.add(pool.submit(() -> { start.await(); for (int i = 0; i < 5000; i++) { map.merge(i % 17, 1, Integer::sum); } return null; }));
                }
                start.countDown(); for (Future<?> job : jobs) { job.get(10, TimeUnit.SECONDS); }
            }
            assertEquals(30000, map.values().stream().mapToInt(Integer::intValue).sum());
            var it = map.entrySet().iterator(); var e = it.next(); map.put(e.getKey(), -1); it.remove();
            assertEquals(-1, map.get(e.getKey()));
            assertThrows(NullPointerException.class, () -> map.put(null, 1));
            assertThrows(NullPointerException.class, () -> map.put(1, null));
            assertThrows(NullPointerException.class, () -> map.replaceAll((k, v) -> null));
            assertFalse(map.containsValue(-900));
        }
    }
    @Test void lockedRangeAndSetViews() {
        var map = new M3LockedTreeMap<Integer, Integer>(); for (int i = 0; i < 100; i++) { map.put(i, i); }
        var view = map.subMap(10, true, 30, false).descendingMap();
        assertEquals(29, view.firstKey()); assertEquals(10, view.lastKey());
        view.compute(15, (k, v) -> v + 1); assertEquals(16, map.get(15));
        view.keySet().remove(16); assertFalse(map.containsKey(16));
        assertThrows(IllegalArgumentException.class, () -> view.put(50, 1));
        var snapshot = view.keySet().iterator(); view.clear(); assertTrue(snapshot.hasNext()); assertEquals(80, map.size());
        var set = new M3LockedTreeSet<Integer>(); set.addAll(List.of(1, 2, 3, 4));
        assertTrue(set.headSet(3, false).add(0)); assertEquals(0, set.pollFirst());
        assertEquals(4, set.descendingSet().pollFirst()); assertEquals(List.of(1, 2, 3), new ArrayList<>(set));
    }
    @Test void blockingDequeDifferential() {
        var a = new M3BlockingDeque<Integer>(31); var b = new LinkedBlockingDeque<Integer>(31); Random r = new Random(41);
        for (int i = 0; i < 30000; i++) {
            int v = r.nextInt(40);
            switch (r.nextInt(8)) {
                case 0 -> assertEquals(b.offerFirst(v), a.offerFirst(v));
                case 1 -> assertEquals(b.offerLast(v), a.offerLast(v));
                case 2 -> assertEquals(b.pollFirst(), a.pollFirst());
                case 3 -> assertEquals(b.pollLast(), a.pollLast());
                case 4 -> assertEquals(b.removeFirstOccurrence(v), a.removeFirstOccurrence(v));
                case 5 -> assertEquals(b.removeLastOccurrence(v), a.removeLastOccurrence(v));
                case 6 -> assertEquals(b.contains(v), a.contains(v));
                default -> {
                    if (!b.isEmpty()) { var ai = a.iterator(); var bi = b.iterator(); assertEquals(bi.next(), ai.next()); ai.remove(); bi.remove(); }
                }
            }
            assertEquals(new ArrayList<>(b), new ArrayList<>(a)); assertEquals(b.remainingCapacity(), a.remainingCapacity());
        }
        a.clear(); a.offer(10); var stale = a.iterator(); stale.next(); a.poll(); a.offer(10); stale.remove(); assertEquals(1, a.size());
    }
    @Test void blockingWakeupTimeoutAndInterrupt() throws Exception {
        var q = new M3BlockingDeque<Integer>(1); q.put(1);
        assertFalse(q.offer(2, 1, TimeUnit.MILLISECONDS));
        try (var pool = Executors.newSingleThreadExecutor()) {
            Future<?> put = pool.submit(() -> { q.putFirst(2); return null; });
            assertEquals(1, q.take()); put.get(5, TimeUnit.SECONDS); assertEquals(2, q.takeLast());
            Future<Integer> take = pool.submit(q::take); q.putLast(3); assertEquals(3, take.get(5, TimeUnit.SECONDS));
        }
        assertNull(q.poll(1, TimeUnit.MILLISECONDS));
        AtomicReference<Throwable> result = new AtomicReference<>();
        Thread thread = new Thread(() -> { try { q.take(); } catch (Throwable error) { result.set(error); } });
        thread.start(); await(() -> thread.getState() == Thread.State.WAITING); thread.interrupt(); thread.join(5000);
        assertFalse(thread.isAlive()); assertTrue(result.get() instanceof InterruptedException); assertTrue(q.isEmpty());
    }
    @Test void drainFailureRetainsUndeliveredElement() {
        for (BlockingQueue<Integer> q : List.of(new M3BlockingDeque<Integer>(), new M3TransferQueue<Integer>())) {
            q.addAll(List.of(1, 2, 3));
            var target = new AbstractCollection<Integer>() {
                @Override public int size() { return 0; }
                @Override public Iterator<Integer> iterator() { return List.<Integer>of().iterator(); }
                @Override public boolean add(Integer value) { if (value == 2) { throw new IllegalStateException("target rejected"); } return true; }
            };
            assertThrows(IllegalStateException.class, () -> q.drainTo(target)); assertEquals(List.of(2, 3), new ArrayList<>(q));
            assertThrows(IllegalArgumentException.class, () -> q.drainTo(q));
        }
    }
    @Test void transferBufferedDifferentialAndIteratorRecycling() {
        var a = new M3TransferQueue<Integer>(); var b = new LinkedTransferQueue<Integer>(); Random r = new Random(738);
        for (int i = 0; i < 10000; i++) {
            int v = r.nextInt(80);
            switch (r.nextInt(5)) {
                case 0 -> assertEquals(b.offer(v), a.offer(v));
                case 1 -> assertEquals(b.poll(), a.poll());
                case 2 -> assertEquals(b.remove(v), a.remove(v));
                case 3 -> assertEquals(b.tryTransfer(v), a.tryTransfer(v));
                default -> assertEquals(b.peek(), a.peek());
            }
            assertEquals(new ArrayList<>(b), new ArrayList<>(a)); a.verifyInvariants();
        }
        a.clear(); a.add(1); var stale = a.iterator(); stale.next(); a.poll(); a.add(1); stale.remove(); assertEquals(1, a.size());
    }
    @Test void transferWaitersCancellationAndTimeout() throws Exception {
        var q = new M3TransferQueue<Integer>();
        assertFalse(q.tryTransfer(5)); assertTrue(q.isEmpty());
        assertFalse(q.tryTransfer(5, 2, TimeUnit.MILLISECONDS)); assertTrue(q.isEmpty()); q.verifyInvariants();
        try (var pool = Executors.newSingleThreadExecutor()) {
            Future<Integer> waiting = pool.submit(q::take); await(q::hasWaitingConsumer);
            assertEquals(1, q.getWaitingConsumerCount()); assertTrue(q.tryTransfer(7)); assertEquals(7, waiting.get(5, TimeUnit.SECONDS));
            Future<?> producer = pool.submit(() -> { q.transfer(8); return null; }); await(() -> q.size() == 1);
            assertFalse(producer.isDone()); assertEquals(8, q.poll()); producer.get(5, TimeUnit.SECONDS);
        }
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread sender = new Thread(() -> { try { q.transfer(9); } catch (Throwable e) { error.set(e); } });
        sender.start(); await(() -> q.size() == 1); sender.interrupt(); sender.join(5000);
        assertTrue(error.get() instanceof InterruptedException); assertTrue(q.isEmpty()); q.verifyInvariants();
        Thread receiver = new Thread(() -> { try { q.take(); } catch (Throwable e) { error.set(e); } });
        receiver.start(); await(q::hasWaitingConsumer); receiver.interrupt(); receiver.join(5000);
        assertFalse(q.hasWaitingConsumer()); assertTrue(q.isEmpty()); q.verifyInvariants();
        assertNull(q.poll(1, TimeUnit.MILLISECONDS)); assertEquals(0, q.getWaitingConsumerCount());
    }
    @Test void synchronousQueueHasNoCollectionCapacity() throws Exception {
        var q = new M3TransferQueue<Integer>(true, true);
        assertFalse(q.offer(1)); assertEquals(0, q.size()); assertEquals(0, q.remainingCapacity()); assertNull(q.peek());
        try (var pool = Executors.newSingleThreadExecutor()) {
            Future<?> sender = pool.submit(() -> { q.put(42); return null; });
            assertEquals(42, q.poll(5, TimeUnit.SECONDS)); sender.get(5, TimeUnit.SECONDS);
            Future<Integer> receiver = pool.submit(q::take); await(q::hasWaitingConsumer);
            assertTrue(q.offer(3)); assertEquals(3, receiver.get(5, TimeUnit.SECONDS));
        }
        assertFalse(q.offer(1, 1, TimeUnit.MILLISECONDS)); q.verifyInvariants();
    }
    private static void exchange(BlockingQueue<Integer> queue, boolean transfer) throws Exception {
        int workers = 4, each = 2000; AtomicIntegerArray seen = new AtomicIntegerArray(workers * each);
        try (var pool = Executors.newFixedThreadPool(workers * 2)) {
            var gate = new CountDownLatch(1); var jobs = new ArrayList<Future<?>>();
            for (int worker = 0; worker < workers; worker++) {
                int base = worker * each;
                jobs.add(pool.submit(() -> {
                    gate.await();
                    for (int i = 0; i < each; i++) {
                        if (transfer && i % 3 == 0) { ((M3TransferQueue<Integer>) queue).transfer(base + i); }
                        else { queue.put(base + i); }
                    }
                    return null;
                }));
                jobs.add(pool.submit(() -> { gate.await(); for (int i = 0; i < each; i++) { int value = queue.take(); assertEquals(1, seen.incrementAndGet(value)); } return null; }));
            }
            gate.countDown(); for (Future<?> job : jobs) { job.get(20, TimeUnit.SECONDS); }
        }
        for (int i = 0; i < seen.length(); i++) { assertEquals(1, seen.get(i)); }
        assertTrue(queue.isEmpty());
    }
    @Test void mpmcBlockingDequeHasNoLossOrDuplicates() throws Exception { exchange(new M3BlockingDeque<>(31), false); }
    @Test void mpmcTransferHasNoLossOrDuplicates() throws Exception {
        var queue = new M3TransferQueue<Integer>(); exchange(queue, true); queue.verifyInvariants();
    }
    @Test void mpmcRendezvousHasNoLossOrDuplicates() throws Exception {
        var queue = new M3TransferQueue<Integer>(true, false); exchange(queue, true); queue.verifyInvariants();
    }
}
