/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

/**
 * Independent behavioral checks for the current-master immutable predicate
 * coordinate plan. No predicate results or element snapshots may be cached.
 */
class M3FilteredMasterPredicatePlanLifecycleTest {

    @Test
    void concurrentFirstUsePublishesOneValidPlanButBindsIndependentCursors() throws Exception {
        List<Integer> backing = new ArrayList<>();
        for (int i = 0; i < 128; i++) backing.add(i);

        Collection<Integer> view = backing;
        for (int i = 0; i < 192; i++) {
            view = M3FilteredViews.filter(view, value -> value >= 0);
        }
        final Collection<Integer> selected =
                M3FilteredViews.filter(view, value -> (value & 1) == 0);
        List<Integer> expected = new ArrayList<>();
        for (int i = 0; i < 128; i += 2) expected.add(i);

        final int workers = 8;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        try {
            List<Future<List<Integer>>> futures = new ArrayList<>();
            for (int i = 0; i < workers; i++) {
                futures.add(pool.submit(() -> {
                    if (!start.await(20, TimeUnit.SECONDS)) {
                        throw new AssertionError("Concurrent start barrier expired");
                    }
                    return consume(selected);
                }));
            }
            start.countDown();
            for (Future<List<Integer>> result : futures) {
                assertEquals(expected, result.get(30, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }

        // Once the plan exists, changing the backing collection must still
        // change *future* traversals; no stale result or retained element mask.
        backing.remove(Integer.valueOf(0));
        backing.add(128);
        expected.remove(Integer.valueOf(0));
        expected.add(128);
        assertEquals(expected, consume(selected));
    }

    @Test
    void deepMixedSetAndCollectionViewsRetainIdentityAndInsertionOrder() {
        Object first = new Object();
        Object second = new Object();
        Object third = new Object();
        Set<Object> backing = new LinkedHashSet<>(List.of(first, second, third));
        Collection<Object> asCollection = M3FilteredViews.filter(
                backing, value -> value != second);
        Collection<Object> deep = M3FilteredViews.filter(asCollection,
                value -> true);
        assertEquals(2, consume(deep).size());
        assertSame(first, consume(deep).get(0));
        assertSame(third, consume(deep).get(1));
        Object fourth = new Object();
        backing.add(fourth);
        List<Object> current = consume(deep);
        assertEquals(3, current.size());
        assertSame(fourth, current.get(2));
    }

    @Test
    void repeatedHasNextAndShortCircuitKeepExactCallbackOrder() {
        List<String> invocations = new ArrayList<>();
        Collection<Integer> inner = M3FilteredViews.filter(
                List.of(1, 2, 3), value -> {
                    invocations.add("inner:" + value);
                    return value != 2;
                });
        Collection<Integer> outer = M3FilteredViews.filter(inner, value -> {
            invocations.add("outer:" + value);
            return value == 3;
        });
        Iterator<Integer> iterator = outer.iterator();
        assertTrue(iterator.hasNext());
        assertTrue(iterator.hasNext());
        assertEquals(3, iterator.next());
        assertFalse(iterator.hasNext());
        assertFalse(iterator.hasNext());
        assertEquals(List.of("inner:1", "outer:1", "inner:2",
                "inner:3", "outer:3"), invocations);

        invocations.clear();
        assertEquals(List.of(3), consume(outer));
        assertEquals(List.of("inner:1", "outer:1", "inner:2",
                "inner:3", "outer:3"), invocations);
    }

    @Test
    void readOnlyMutationRefusesBeforeUserPredicateCallback() {
        AtomicInteger invoked = new AtomicInteger();
        Predicate<Integer> callback = value -> {
            invoked.incrementAndGet();
            return true;
        };
        Collection<Integer> view = M3FilteredViews.filter(List.of(1, 2), callback);
        assertThrows(UnsupportedOperationException.class, view::clear);
        assertThrows(UnsupportedOperationException.class, () -> view.removeIf(callback));
        assertEquals(0, invoked.get());
    }

    private static <E> List<E> consume(Collection<E> source) {
        List<E> result = new ArrayList<>();
        Iterator<E> cursor = source.iterator();
        while (cursor.hasNext()) result.add(cursor.next());
        return result;
    }
}
