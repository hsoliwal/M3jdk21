/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.SequencedMap;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Independently authored Java 21 oracle tests, separate from the transplanted donor tests. */
@Timeout(20)
final class M3JdkCollectionDifferentialTest {
    private static <E> List<E> drain(Spliterator<E> split) {
        List<E> result = new ArrayList<>(); split.forEachRemaining(result::add); return result;
    }
    private static <K, V> List<Map.Entry<K, V>> entries(Map<K, V> map) {
        return map.entrySet().stream().map(e -> (Map.Entry<K, V>)
                new AbstractMap.SimpleImmutableEntry<>(e)).toList();
    }
    private static <E> void lateBinding(Collection<E> actual, Collection<E> expected,
            Runnable mutateActual, Runnable mutateExpected) {
        Spliterator<E> a = actual.spliterator(), e = expected.spliterator();
        var as = actual.stream(); var es = expected.stream();
        mutateExpected.run(); mutateActual.run();
        assertEquals(drain(e), drain(a));
        assertEquals(es.toList(), as.toList());
    }

    @Test void lateBindingTraversalsFollowListSetAndMapViews() {
        List<Collection<Integer>> actual = List.of(new M3LinkedList<>(), new M3LinkedHashSet<>(),
                new M3TreeSet<>(Comparator.reverseOrder()));
        List<Collection<Integer>> expected = List.of(new LinkedList<>(), new LinkedHashSet<>(),
                new TreeSet<>(Comparator.reverseOrder()));
        for (int i = 0; i < actual.size(); i++) {
            var a = actual.get(i); var e = expected.get(i); a.add(1); e.add(1);
            lateBinding(a, e, () -> a.add(2), () -> e.add(2));
            var bound = a.spliterator(); bound.estimateSize(); a.add(3);
            assertThrows(ConcurrentModificationException.class, () -> bound.tryAdvance(v -> { }));
        }
        for (boolean tree : new boolean[] {false, true}) {
            for (int view = 0; view < 3; view++) {
                SequencedMap<Integer, Integer> a = tree ? new M3TreeMap<>() : new M3LinkedHashMap<>();
                SequencedMap<Integer, Integer> e = tree ? new TreeMap<>() : new LinkedHashMap<>();
                a.put(1, 10); e.put(1, 10);
                if (view == 0) { lateBinding(a.reversed().keySet(), e.reversed().keySet(),
                        () -> a.put(2, 20), () -> e.put(2, 20)); }
                if (view == 1) { lateBinding(a.values(), e.values(),
                        () -> a.put(2, 20), () -> e.put(2, 20)); }
                if (view == 2) { lateBinding(a.entrySet(), e.entrySet(),
                        () -> a.put(2, 20), () -> e.put(2, 20)); }
            }
        }
    }

    @Test void everySortedSplitRetainsCustomComparatorAndEncounterOrder() {
        Comparator<Integer> comparator = Comparator.reverseOrder();
        for (Collection<Integer> set : List.<Collection<Integer>>of(
                new M3TreeSet<>(comparator), new M3LockedTreeSet<>(comparator))) {
            for (int i = 0; i < 4096; i++) { set.add(i); }
            var result = new ArrayList<Integer>();
            collectSorted(set.spliterator(), comparator, 5, result);
            assertEquals(new ArrayList<>(set), result);
            assertEquals(new ArrayList<>(set), set.parallelStream().toList());
        }
    }
    private static void collectSorted(Spliterator<Integer> split, Comparator<Integer> comparator,
            int depth, List<Integer> result) {
        assertTrue(split.hasCharacteristics(Spliterator.SORTED | Spliterator.ORDERED));
        assertSame(comparator, split.getComparator(), "batched child must not claim natural ordering");
        Spliterator<Integer> prefix = depth == 0 ? null : split.trySplit();
        if (prefix != null) { collectSorted(prefix, comparator, depth - 1, result); }
        split.forEachRemaining(result::add);
    }

    @Test void sequencedNullAndAccessOrderOperationsMatchLinkedHashMap() {
        for (boolean access : new boolean[] {false, true}) {
            var a = new M3LinkedHashMap<Integer, Integer>(access);
            var e = new LinkedHashMap<Integer, Integer>(16, 0.75f, access);
            Random random = new Random(0x4d334f52444552L);
            for (int step = 0; step < 1600; step++) {
                Integer key = step % 7 == 0 ? null : random.nextInt(25);
                Integer value = step % 5 == 0 ? null : random.nextInt(10);
                SequencedMap<Integer, Integer> av = (step & 1) == 0 ? a : a.reversed();
                SequencedMap<Integer, Integer> ev = (step & 1) == 0 ? e : e.reversed();
                int operation = random.nextInt(10);
                assertEquals(sequenceOperation(ev, key, value, operation),
                        sequenceOperation(av, key, value, operation));
                assertEquals(entries(e), entries(a), "step " + step + " operation " + operation);
                assertEquals(entries(e.reversed()), entries(a.reversed()));
            }
            a.verifyInvariants();
            a.put(-123, 0);
            assertThrows(UnsupportedOperationException.class, () -> a.sequencedValues().addFirst(1));
            assertThrows(UnsupportedOperationException.class, () -> a.firstEntry().setValue(2));
            var stale = a.entrySet().iterator(); a.put(100, 100);
            assertThrows(ConcurrentModificationException.class, stale::next);
        }
    }
    private static Object sequenceOperation(SequencedMap<Integer, Integer> map,
            Integer key, Integer value, int operation) {
        return switch (operation) {
            case 0 -> map.putFirst(key, value);
            case 1 -> map.putLast(key, value);
            case 2 -> map.computeIfAbsent(key, k -> value);
            case 3 -> map.replace(key, value);
            case 4 -> map.getOrDefault(key, -99);
            case 5 -> map.compute(key, (k, old) -> value);
            case 6 -> map.isEmpty() ? null : map.sequencedValues().reversed().removeFirst();
            case 7 -> map.isEmpty() ? null : map.sequencedKeySet().removeFirst();
            case 8 -> map.isEmpty() ? map.put(key, value) : map.sequencedEntrySet().getFirst().setValue(value);
            default -> map.putIfAbsent(key, value);
        };
    }

    @Test void comparatorEquivalentKeysAndNestedBoundsMatchTreeMap() {
        Comparator<Integer> comparator = Comparator.nullsFirst(Comparator.comparingInt(Math::abs));
        var actual = new M3TreeMap<Integer, Integer>(comparator);
        var expected = new TreeMap<Integer, Integer>(comparator);
        for (Integer key : new Integer[] {null, -4, -3, -2, -1, 0, 1, 2, 3, 4}) {
            assertEquals(expected.put(key, key), actual.put(key, key));
        }
        assertEquals(entries(expected), entries(actual));
        for (boolean low : new boolean[] {false, true}) {
            for (boolean high : new boolean[] {false, true}) {
                NavigableMap<Integer, Integer> a = actual.subMap(-1, low, 4, high).descendingMap();
                NavigableMap<Integer, Integer> e = expected.subMap(-1, low, 4, high).descendingMap();
                for (Integer endpoint : new Integer[] {null, -5, -4, -3, -1, 0, 1, 3, 4, 5}) {
                    assertEquals(e.lowerEntry(endpoint), a.lowerEntry(endpoint));
                    assertEquals(e.floorEntry(endpoint), a.floorEntry(endpoint));
                    assertEquals(e.ceilingEntry(endpoint), a.ceilingEntry(endpoint));
                    assertEquals(e.higherEntry(endpoint), a.higherEntry(endpoint));
                    assertEquals(outcome(() -> e.headMap(endpoint, true)), outcome(() -> a.headMap(endpoint, true)));
                    assertEquals(outcome(() -> e.tailMap(endpoint, false)), outcome(() -> a.tailMap(endpoint, false)));
                }
            }
        }
        actual.subMap(-2, true, 4, false).descendingMap().clear();
        expected.subMap(-2, true, 4, false).descendingMap().clear();
        assertEquals(entries(expected), entries(actual)); actual.verifyInvariants();
        assertThrows(NullPointerException.class, () -> new M3TreeMap<Integer, Integer>().get(null));
    }
    private static Object outcome(Supplier<NavigableMap<Integer, Integer>> operation) {
        try { return entries(operation.get()); } catch (IllegalArgumentException ex) { return ex.getClass(); }
    }

    @Test void reversedSublistAndDefaultListMutationsMatchLinkedList() {
        var actual = new M3LinkedList<Integer>(); var expected = new LinkedList<Integer>();
        Random random = new Random(0x4d334c49535432L);
        for (int step = 0; step < 1800; step++) {
            List<Integer> a = (step & 1) == 0 ? actual : actual.reversed();
            List<Integer> e = (step & 1) == 0 ? expected : expected.reversed();
            Integer v = step % 9 == 0 ? null : random.nextInt(30);
            int index = random.nextInt(e.size() + 1);
            switch (random.nextInt(7)) {
                case 0 -> { a.add(index, v); e.add(index, v); }
                case 1 -> { a.addAll(index, java.util.Arrays.asList(v, null, -1)); e.addAll(index, java.util.Arrays.asList(v, null, -1)); }
                case 2 -> {
                    if (index < e.size()) { assertEquals(e.remove(index), a.remove(index)); }
                }
                case 3 -> {
                    var ai = a.listIterator(index); var ei = e.listIterator(index);
                    ai.add(v); ei.add(v); assertEquals(ei.previous(), ai.previous()); ai.remove(); ei.remove();
                }
                case 4 -> {
                    a.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
                    e.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
                }
                case 5 -> {
                    a.replaceAll(x -> x == null ? null : x & 15); e.replaceAll(x -> x == null ? null : x & 15);
                }
                default -> {
                    if (e.size() > 16) { a.subList(2, 11).reversed().clear(); e.subList(2, 11).reversed().clear(); }
                }
            }
            assertEquals(expected, actual, "step " + step);
            assertArrayEquals(expected.toArray(Integer[]::new), actual.toArray(Integer[]::new));
        }
        actual.verifyInvariants();
        var sub = actual.subList(0, Math.min(2, actual.size()));
        actual.reversed().addFirst(99);
        assertThrows(ConcurrentModificationException.class, sub::size);
    }

    @Test void boundedDequeWrapDrainAndTimedOperationsMatchJdk() throws Exception {
        for (int capacity : new int[] {1, 2, 7}) {
            BlockingDeque<Integer> a = new M3BlockingDeque<>(capacity);
            BlockingDeque<Integer> e = new LinkedBlockingDeque<>(capacity);
            Random random = new Random(0x4d334445515545L + capacity);
            for (int step = 0; step < 1800; step++) {
                int v = random.nextInt(8), operation = random.nextInt(8);
                Object actual; Object expected;
                switch (operation) {
                    case 0 -> { expected = e.offerFirst(v); actual = a.offerFirst(v); }
                    case 1 -> { expected = e.offerLast(v); actual = a.offerLast(v); }
                    case 2 -> { expected = e.pollFirst(); actual = a.pollFirst(); }
                    case 3 -> { expected = e.pollLast(); actual = a.pollLast(); }
                    case 4 -> { expected = e.removeFirstOccurrence(v); actual = a.removeFirstOccurrence(v); }
                    case 5 -> { expected = e.offer(v, 0, TimeUnit.NANOSECONDS); actual = a.offer(v, 0, TimeUnit.NANOSECONDS); }
                    case 6 -> {
                        var ad = new ArrayList<Integer>(); var ed = new ArrayList<Integer>();
                        expected = e.drainTo(ed, v); actual = a.drainTo(ad, v); assertEquals(ed, ad);
                    }
                    default -> { expected = e.poll(0, TimeUnit.NANOSECONDS); actual = a.poll(0, TimeUnit.NANOSECONDS); }
                }
                assertEquals(expected, actual); assertEquals(new ArrayList<>(e), new ArrayList<>(a));
                assertEquals(e.remainingCapacity(), a.remainingCapacity());
            }
            assertThrows(NullPointerException.class, () -> a.offer(null));
            assertThrows(IllegalArgumentException.class, () -> a.drainTo(a));
        }
    }

    @Test void blockedProducerInterruptionLeavesNoPhantomElement() throws Exception {
        for (BlockingDeque<Integer> queue : List.<BlockingDeque<Integer>>of(
                new M3BlockingDeque<>(1), new LinkedBlockingDeque<>(1))) {
            queue.add(1); AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread producer = Thread.ofVirtual().start(() -> {
                try { queue.putLast(2); } catch (Throwable ex) { failure.set(ex); }
            });
            try { await(() -> producer.getState() == Thread.State.WAITING); producer.interrupt(); producer.join(3000); }
            finally { producer.interrupt(); }
            assertFalse(producer.isAlive()); assertInstanceOf(InterruptedException.class, failure.get());
            assertEquals(List.of(1), new ArrayList<>(queue)); assertEquals(1, queue.take());
            assertNull(queue.poll());
        }
    }

    @Test void transferAcknowledgementTimeoutAndCancellationMatchJdk() throws Exception {
        for (TransferQueue<Integer> queue : List.<TransferQueue<Integer>>of(
                new M3TransferQueue<>(), new LinkedTransferQueue<>())) {
            assertFalse(queue.tryTransfer(1));
            assertFalse(queue.tryTransfer(1, 1, TimeUnit.MILLISECONDS)); assertTrue(queue.isEmpty());
            AtomicReference<Integer> received = new AtomicReference<>();
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread consumer = Thread.ofVirtual().start(() -> {
                try { received.set(queue.take()); } catch (Throwable ex) { failure.set(ex); }
            });
            try { await(queue::hasWaitingConsumer); assertTrue(queue.tryTransfer(7)); consumer.join(3000); }
            finally { consumer.interrupt(); }
            assertFalse(consumer.isAlive()); assertNull(failure.get()); assertEquals(7, received.get());
            assertEquals(0, queue.getWaitingConsumerCount());
            Thread producer = Thread.ofVirtual().start(() -> {
                try { queue.transfer(8); } catch (Throwable ex) { failure.set(ex); }
            });
            try { await(() -> queue.contains(8)); producer.interrupt(); producer.join(3000); }
            finally { producer.interrupt(); }
            assertFalse(producer.isAlive()); assertInstanceOf(InterruptedException.class, failure.get());
            assertTrue(queue.isEmpty()); assertNull(queue.poll());
            if (queue instanceof M3TransferQueue<Integer> m3) { m3.verifyInvariants(); }
        }
    }

    @Test void zeroCapacityRendezvousPreservesSynchronousQueueContract() throws Exception {
        for (BlockingQueue<Integer> queue : List.<BlockingQueue<Integer>>of(
                new M3TransferQueue<>(true, false), new SynchronousQueue<>())) {
            assertFalse(queue.offer(1)); assertEquals(0, queue.remainingCapacity());
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread producer = Thread.ofVirtual().start(() -> {
                try { queue.put(3); } catch (Throwable ex) { failure.set(ex); }
            });
            try {
                await(() -> producer.getState() == Thread.State.WAITING);
                assertEquals(0, queue.size()); assertFalse(queue.iterator().hasNext()); assertNull(queue.peek());
                assertEquals(3, queue.poll(3, TimeUnit.SECONDS)); producer.join(3000);
            } finally { producer.interrupt(); }
            assertFalse(producer.isAlive()); assertNull(failure.get()); assertTrue(queue.isEmpty());
        }
    }

    @Test void lockedMapAtomicCallbacksLoseNoUpdates() throws Exception {
        for (ConcurrentMap<Integer, Integer> map : List.<ConcurrentMap<Integer, Integer>>of(
                new M3LockedMap<>(), new M3LockedTreeMap<>(), new ConcurrentHashMap<>())) {
            assertThrows(NullPointerException.class, () -> map.put(null, 1));
            assertThrows(NullPointerException.class, () -> map.put(1, null));
            var failure = new AtomicReference<Throwable>(); var workers = new ArrayList<Thread>();
            for (int worker = 0; worker < 4; worker++) {
                workers.add(Thread.ofVirtual().start(() -> {
                    try { for (int i = 0; i < 2000; i++) { map.merge(i & 3, 1, Integer::sum); } }
                    catch (Throwable ex) { failure.compareAndSet(null, ex); }
                }));
            }
            for (Thread worker : workers) { worker.join(5000); assertFalse(worker.isAlive()); }
            assertNull(failure.get()); assertEquals(Map.of(0, 2000, 1, 2000, 2, 2000, 3, 2000), map);
        }
    }
    private static void await(BooleanSupplier condition) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() >= deadline) { fail("worker did not reach its observable state"); }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
        }
    }
}
