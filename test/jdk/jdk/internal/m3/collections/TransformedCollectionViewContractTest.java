/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.SplittableRandom;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/** Differential and physical-lane proofs for the write-hooked collection. */
final class TransformedCollectionViewContractTest {
    private static final Function<Integer, Integer> TRIPLE = value -> value == null ? null : value * 3;
    /** A validating hook: the behaviour of a predicated collection. */
    private static final Function<Integer, Integer> EVEN_ONLY = value -> {
        if (value == null || (value & 1) != 0) {
            throw new IllegalArgumentException("odd or null: " + value);
        }
        return value;
    };

    private static Object lane(Object owner, String name) {
        try {
            Field field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(owner);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    private static List<Integer> elements(TransformedCollectionView<Integer> view) {
        List<Integer> list = new ArrayList<>();
        for (Iterator<Integer> it = view.iterator(); it.hasNext();) {
            list.add(it.next());
        }
        return list;
    }

    /** The lane holds the elements in slots 0..size-1, nothing behind them, and exactly those slots are OCCUPIED. */
    private static void checkLanes(TransformedCollectionView<Integer> view, List<Integer> model) {
        Object[] values = (Object[]) lane(view, "valueLane");
        long[] states = (long[]) lane(view, "stateMaskWords");
        assertEquals(model.size(), view.size());
        assertEquals(model, Arrays.asList(Arrays.copyOf(values, model.size())));
        for (int slot = model.size(); slot < values.length; slot++) {
            assertNull(values[slot], "stale reference in slot " + slot);
        }
        assertTrue(states.length * 64 >= values.length, "a state bit for every slot");
        for (int slot = 0; slot < states.length * 64; slot++) {
            boolean occupied = (states[slot >>> 6] >>> slot & TransformedCollectionView.STATE_OCCUPIED) != 0;
            assertEquals(slot < model.size(), occupied, "OCCUPIED bit of slot " + slot);
        }
    }

    @Test
    void identityViewBehavesLikeAList() {
        TransformedCollectionView<Integer> view = new TransformedCollectionView<>();
        assertEquals(0, view.size());
        assertFalse(view.iterator().hasNext());
        assertTrue(view.add(5));
        assertTrue(view.add(null));
        assertTrue(view.add(5));
        assertEquals(Arrays.asList(5, null, 5), elements(view));
        assertTrue(view.remove(null));
        assertTrue(view.remove(5));
        assertFalse(view.remove(7));
        assertEquals(List.of(5), elements(view));
        assertEquals("TransformedCollectionView|TRANSFORMED_COLLECTION|OBJECT|OCCUPIED|Object[] valueLane",
                TransformedCollectionView.scaffoldDescriptor());
        assertThrows(NullPointerException.class, () -> new TransformedCollectionView<Integer>(null));
    }

    @Test
    void hookTransformsWritesOnly() {
        TransformedCollectionView<Integer> view = new TransformedCollectionView<>(TRIPLE);
        assertTrue(view.add(2));
        assertTrue(view.addAll(List.of(1, 4)));
        assertFalse(view.addAll(List.of()));
        assertEquals(List.of(6, 3, 12), elements(view));
        assertFalse(view.remove(2), "remove looks for the stored value, not the hook's argument");
        assertTrue(view.remove(6));
        assertEquals(List.of(3, 12), elements(view));
        assertThrows(NullPointerException.class, () -> view.addAll(null));
    }

    @Test
    void refusingHookLeavesTheCollectionUnchanged() {
        TransformedCollectionView<Integer> view = new TransformedCollectionView<>(EVEN_ONLY);
        view.addAll(List.of(2, 4));
        assertThrows(IllegalArgumentException.class, () -> view.add(3));
        assertThrows(IllegalArgumentException.class, () -> view.addAll(Arrays.asList(6, 8, 9, 10)));
        assertThrows(IllegalArgumentException.class, () -> view.add(null));
        checkLanes(view, List.of(2, 4));
        Iterator<Integer> it = view.iterator();
        assertThrows(IllegalArgumentException.class, () -> view.add(1));
        assertEquals(2, it.next(), "a refused write is not a modification");
    }

    @Test
    void iteratorFollowsTheListRules() {
        TransformedCollectionView<Integer> view = new TransformedCollectionView<>();
        view.addAll(List.of(1, 2, 3, 4));
        Iterator<Integer> it = view.iterator();
        assertThrows(IllegalStateException.class, it::remove);
        assertEquals(1, it.next());
        it.remove();
        assertThrows(IllegalStateException.class, it::remove);
        assertEquals(2, it.next());
        assertEquals(3, it.next());
        it.remove();
        assertEquals(4, it.next());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
        assertEquals(List.of(2, 4), elements(view));
        Iterator<Integer> stale = view.iterator();
        view.add(9);
        assertThrows(ConcurrentModificationException.class, stale::next);
        Iterator<Integer> removed = view.iterator();
        removed.next();
        view.remove(4);
        assertThrows(ConcurrentModificationException.class, removed::remove);
    }

    @Test
    void randomOperationsMatchAnArrayListModel() {
        for (int seed = 0; seed < 8; seed++) {
            SplittableRandom random = new SplittableRandom(seed * 7919L + 31);
            TransformedCollectionView<Integer> view = new TransformedCollectionView<>(TRIPLE);
            List<Integer> model = new ArrayList<>();
            for (int step = 0; step < 4000; step++) {
                Integer value = random.nextInt(20) == 0 ? null : random.nextInt(40);
                switch (random.nextInt(8)) {
                    case 0, 1, 2 -> assertEquals(model.add(TRIPLE.apply(value)), view.add(value));
                    case 3 -> {
                        List<Integer> batch = new ArrayList<>();
                        for (int i = random.nextInt(70); i > 0; i--) {
                            batch.add(random.nextInt(40));
                        }
                        List<Integer> stored = batch.stream().map(TRIPLE).toList();
                        assertEquals(model.addAll(stored), view.addAll(batch));
                    }
                    case 4, 5 -> {
                        Integer target = value == null || random.nextBoolean() ? value : (Integer) (value * 3);
                        assertEquals(model.remove((Object) target), view.remove(target));
                    }
                    case 6 -> removeThroughIterators(random, view, model);
                    default -> assertEquals(model, elements(view));
                }
                if (step % 53 == 0) {
                    checkLanes(view, model);
                }
            }
            checkLanes(view, model);
        }
    }

    private static void removeThroughIterators(SplittableRandom random, TransformedCollectionView<Integer> view,
            List<Integer> model) {
        Iterator<Integer> actual = view.iterator();
        Iterator<Integer> expected = model.iterator();
        while (expected.hasNext()) {
            assertTrue(actual.hasNext());
            assertEquals(expected.next(), actual.next());
            if (random.nextInt(5) == 0) {
                expected.remove();
                actual.remove();
            }
        }
        assertFalse(actual.hasNext());
    }
}
