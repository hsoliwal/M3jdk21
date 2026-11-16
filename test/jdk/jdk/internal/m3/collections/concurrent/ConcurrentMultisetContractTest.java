/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.internal.m3.collections.concurrent;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Differential, physical-lane and multi-thread proofs for the striped multiset. */
final class ConcurrentMultisetContractTest {
    /** An element whose hash keeps only a few bits, so one stripe fills, packs and forces every region to double. */
    private record Crowded(int id, int bits) {
        @Override public int hashCode() { return id & bits; }
    }

    @Test
    void randomOperationsMatchTheModelAndTheLanesStayConsistent() throws Exception {
        runAgainstModel(40, -1, 0x51ED270BL, 60_000);
        runAgainstModel(3000, -1, 0x9E3779B9L, 120_000);
        runAgainstModel(300, 7, 0x2545F491L, 80_000);
        runAgainstModel(64, 0, 0x7F4A7C15L, 40_000);
    }

    private static void runAgainstModel(int universe, int bits, long seed, int steps) throws Exception {
        ConcurrentMultiset<Object> multiset = new ConcurrentMultiset<>();
        Map<Object, Integer> model = new HashMap<>();
        SplittableRandom random = new SplittableRandom(seed);
        for (int step = 0; step < steps; step++) {
            int id = random.nextInt(universe);
            Object element = bits < 0 ? (Object) Integer.valueOf(id) : new Crowded(id, bits);
            int old = model.getOrDefault(element, 0);
            int amount = random.nextInt(5);
            switch (random.nextInt(10)) {
                case 0, 1, 2 -> {
                    assertEquals(old, multiset.add(element, amount));
                    put(model, element, old + amount);
                }
                case 3, 4 -> {
                    assertEquals(old, multiset.remove(element, amount));
                    put(model, element, Math.max(0, old - amount));
                }
                case 5 -> {
                    assertEquals(old, multiset.setCount(element, amount));
                    put(model, element, amount);
                }
                case 6 -> {
                    int expected = random.nextBoolean() ? old : random.nextInt(4);
                    assertEquals(expected == old, multiset.setCount(element, expected, amount));
                    put(model, element, expected == old ? amount : old);
                }
                case 7 -> clearSometimes(random, multiset, model);
                default -> assertEquals(old, multiset.count(element));
            }
            if (step % 499 == 0) {
                assertSameState(multiset, model);
            }
        }
        assertSameState(multiset, model);
    }

    private static void put(Map<Object, Integer> model, Object element, int count) {
        if (count == 0) {
            model.remove(element);
        } else {
            model.put(element, count);
        }
    }

    private static void clearSometimes(SplittableRandom random, ConcurrentMultiset<Object> multiset, Map<Object, Integer> model) {
        if (random.nextInt(60) == 0) {
            multiset.clear();
            model.clear();
        }
    }

    private static void assertSameState(ConcurrentMultiset<Object> multiset, Map<Object, Integer> model) throws Exception {
        long sum = 0;
        for (Map.Entry<Object, Integer> entry : model.entrySet()) {
            assertEquals(entry.getValue().intValue(), multiset.count(entry.getKey()));
            sum += entry.getValue();
        }
        assertEquals(sum, multiset.size());
        assertEquals(model.isEmpty(), multiset.isEmpty());
        assertEquals(model.keySet(), multiset.elementSet());
        assertEquals(model.size(), multiset.elementSet().size());
        assertLanes(multiset, model);
    }

    /** Physical proof: per stripe region, the state words, the element lane, the count lane and the tallies agree. */
    private static void assertLanes(ConcurrentMultiset<Object> multiset, Map<Object, Integer> model) throws Exception {
        Object[] elements = (Object[]) lane(multiset, "elementLane");
        int[] counts = (int[]) lane(multiset, "countLane");
        long[] states = (long[]) lane(multiset, "stateMaskWords");
        int[] index = (int[]) lane(multiset, "hashIndexLane");
        int region = (int) lane(multiset, "region");
        int[] used = (int[]) lane(multiset, "used");
        int[] live = (int[]) lane(multiset, "live");
        long[] total = (long[]) lane(multiset, "total");
        assertEquals(32 * region, elements.length);
        assertEquals(2 * elements.length, index.length);
        int occupied = 0;
        for (int stripe = 0; stripe < 32; stripe++) {
            int liveHere = 0;
            long totalHere = 0;
            for (int slot = stripe * region; slot < (stripe + 1) * region; slot++) {
                boolean handedOut = slot - stripe * region < used[stripe << 4];
                if (states[slot] == ConcurrentMultiset.STATE_OCCUPIED) {
                    assertTrue(handedOut && counts[slot] > 0, "an occupied slot was handed out and counts something");
                    assertEquals(model.get(elements[slot]).intValue(), counts[slot], "the count lane holds the count");
                    liveHere++;
                    totalHere += counts[slot];
                } else {
                    assertEquals(handedOut ? ConcurrentMultiset.STATE_DELETED : 0L, states[slot], "deleted or never used");
                    assertNull(elements[slot], "a slot that is not occupied holds no element");
                    assertEquals(0, counts[slot]);
                }
            }
            assertEquals(liveHere, live[stripe << 4]);
            assertEquals(totalHere, total[stripe << 3]);
            occupied += liveHere;
            int entries = 0;
            for (int position = stripe * 2 * region; position < (stripe + 1) * 2 * region; position++) {
                entries += index[position] != 0 ? 1 : 0;
            }
            assertEquals(used[stripe << 4], entries, "one index entry per slot handed out, so the index is at most half full");
        }
        assertEquals(model.size(), occupied);
    }

    private static Object lane(ConcurrentMultiset<?> multiset, String name) throws Exception {
        Field field = ConcurrentMultiset.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(multiset);
    }

    @Test
    void argumentsAreCheckedLikeTheProvider() {
        ConcurrentMultiset<String> multiset = new ConcurrentMultiset<>();
        assertEquals(0, multiset.add("a", 3));
        assertEquals(3, multiset.add("a", 0), "zero occurrences only read the count");
        assertEquals(3, multiset.remove("a", 0));
        assertEquals(0, multiset.count(null));
        assertEquals(0, multiset.remove(null, 2));
        assertThrows(NullPointerException.class, () -> multiset.add(null, 1));
        assertThrows(NullPointerException.class, () -> multiset.add(null, 0));
        assertThrows(NullPointerException.class, () -> multiset.setCount(null, 1));
        assertThrows(NullPointerException.class, () -> multiset.setCount(null, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> multiset.add("a", -1));
        assertThrows(IllegalArgumentException.class, () -> multiset.remove("a", -1));
        assertThrows(IllegalArgumentException.class, () -> multiset.remove(null, -1));
        assertThrows(IllegalArgumentException.class, () -> multiset.setCount("a", -1));
        assertThrows(IllegalArgumentException.class, () -> multiset.setCount("a", -1, 1));
        assertThrows(IllegalArgumentException.class, () -> multiset.setCount("a", 3, -1));
        multiset.setCount("a", Integer.MAX_VALUE - 1);
        assertEquals(Integer.MAX_VALUE - 1, multiset.add("a", 1));
        IllegalArgumentException overflow = assertThrows(IllegalArgumentException.class, () -> multiset.add("a", 1));
        assertEquals("Overflow adding 1 occurrences to a count of 2147483647", overflow.getMessage());
        assertEquals(Integer.MAX_VALUE, multiset.count("a"), "a refused add changes nothing");
        multiset.add("b", 5);
        assertEquals(Integer.MAX_VALUE, multiset.size(), "the size stops at the largest int");
        assertEquals(Integer.MAX_VALUE, multiset.remove("a", Integer.MAX_VALUE));
        assertEquals(5, multiset.remove("b", 9), "removing more than there is removes all");
        assertTrue(multiset.isEmpty());
    }

    @Test
    void conditionalSetCountOnlyWritesFromTheExpectedCount() {
        ConcurrentMultiset<String> multiset = new ConcurrentMultiset<>();
        assertTrue(multiset.setCount("a", 0, 0), "absent, expected absent, stays absent");
        assertFalse(multiset.setCount("a", 1, 5), "absent is not count 1");
        assertTrue(multiset.setCount("a", 0, 4));
        assertFalse(multiset.setCount("a", 0, 9));
        assertFalse(multiset.setCount("a", 3, 9));
        assertEquals(4, multiset.count("a"));
        assertTrue(multiset.setCount("a", 4, 4), "the same count is a success");
        assertTrue(multiset.setCount("a", 4, 0), "to zero removes the element");
        assertFalse(multiset.elementSet().contains("a"));
        assertEquals(0, multiset.size());
    }

    @Test
    void theElementSetIsALiveView() {
        ConcurrentMultiset<String> multiset = new ConcurrentMultiset<>();
        Set<String> elements = multiset.elementSet();
        assertSame(elements, multiset.elementSet());
        assertTrue(elements.isEmpty());
        multiset.add("a", 2);
        multiset.add("b", 1);
        multiset.add("c", 7);
        assertEquals(Set.of("a", "b", "c"), elements, "the view follows later additions");
        assertTrue(elements.contains("c"));
        assertFalse(elements.contains("z"));
        assertFalse(elements.contains(null));
        assertTrue(elements.remove("c"), "removing through the view removes every occurrence");
        assertFalse(elements.remove("c"));
        assertFalse(elements.remove(null));
        assertEquals(0, multiset.count("c"));
        assertEquals(3, multiset.size());
        assertThrows(UnsupportedOperationException.class, () -> elements.add("x"));
        Iterator<String> iterator = elements.iterator();
        assertThrows(IllegalStateException.class, iterator::remove);
        Set<String> seen = new HashSet<>();
        while (iterator.hasNext()) {
            seen.add(iterator.next());
            iterator.remove();
        }
        assertEquals(Set.of("a", "b"), seen);
        assertThrows(java.util.NoSuchElementException.class, iterator::next);
        assertTrue(multiset.isEmpty());
        multiset.add("d", 1);
        elements.clear();
        assertEquals(0, multiset.size());
    }

    @Test
    void threadsThatWriteTheSameElementsLoseNothing() throws Exception {
        int threads = 8;
        int rounds = 20_000;
        ConcurrentMultiset<Object> multiset = new ConcurrentMultiset<>();
        List<Object> shared = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            shared.add(i % 2 == 0 ? (Object) ("s" + i) : new Crowded(i, 1));
        }
        runThreads(threads, thread -> {
            for (int round = 0; round < rounds; round++) {
                Object element = shared.get((round + thread) % shared.size());
                multiset.add(element, 2);
                assertTrue(multiset.remove(element, 1) >= 1, "this thread added two before it removes one");
                incrementByCompareAndSet(multiset, element);
                multiset.add(Integer.valueOf(1_000_000 + thread * rounds + round), 1);
            }
        });
        Map<Object, Integer> model = new HashMap<>();
        for (int thread = 0; thread < threads; thread++) {
            for (int round = 0; round < rounds; round++) {
                model.merge(shared.get((round + thread) % shared.size()), 2, Integer::sum);
                model.put(Integer.valueOf(1_000_000 + thread * rounds + round), 1);
            }
        }
        assertSameState(multiset, model);
    }

    @Test
    void threadsThatAddAndRemoveDistinctElementsLeaveNothingBehind() throws Exception {
        int threads = 8;
        ConcurrentMultiset<Object> multiset = new ConcurrentMultiset<>();
        runThreads(threads, thread -> {
            for (int round = 0; round < 30_000; round++) {
                Object element = new Crowded(thread * 1_000_000 + round % 700, 63);
                multiset.add(element, 3);
                assertEquals(3, multiset.setCount(element, 0), "only this thread writes this element");
            }
        });
        assertSameState(multiset, new HashMap<>());
    }

    private static void incrementByCompareAndSet(ConcurrentMultiset<Object> multiset, Object element) {
        int seen = multiset.count(element);
        while (!multiset.setCount(element, seen, seen + 1)) {
            seen = multiset.count(element);
        }
    }

    private interface Work {
        void run(int thread) throws Exception;
    }

    private static void runThreads(int threads, Work work) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        List<Thread> all = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            int thread = i;
            Thread runner = new Thread(() -> {
                try {
                    start.await();
                    work.run(thread);
                } catch (Throwable thrown) {
                    failure.compareAndSet(null, thrown);
                }
            });
            all.add(runner);
            runner.start();
        }
        start.countDown();
        for (Thread runner : all) {
            runner.join();
        }
        if (failure.get() != null) {
            throw new AssertionError("a worker failed", failure.get());
        }
    }

    @Test
    void theDescriptorNamesTheLanes() {
        assertEquals("ConcurrentMultiset|CONCURRENT_MULTISET|OBJECT|OCCUPIED,DELETED|Object[] elementLane;int[] countLane;int[] hashIndexLane",
                ConcurrentMultiset.scaffoldDescriptor());
    }
}
