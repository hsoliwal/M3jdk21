/*
 * Copyright (c) 2026, Hitesh Soliwal and Contributors to the Synexia Project.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

/*
 * @test
 * @bug 8292955
 * @summary CheckedMap.merge rejects invalid raw inputs before mutation or callback
 * @run main/othervm CheckedMapMerge8292955Test
 * @run main/othervm -Xint CheckedMapMerge8292955Test
 * @run main/othervm -XX:+CompactStrings CheckedMapMerge8292955Test
 * @run main/othervm -XX:-CompactStrings CheckedMapMerge8292955Test
 */

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CheckedMapMerge8292955Test {
    private static final AtomicInteger assertions = new AtomicInteger();

    private static void check(boolean ok, String message) {
        assertions.incrementAndGet();
        if (!ok) throw new AssertionError(message);
    }

    private static void expect(Class<? extends RuntimeException> kind, Runnable action) {
        assertions.incrementAndGet();
        try {
            action.run();
        } catch (RuntimeException actual) {
            if (actual.getClass() == kind) return;
            throw new AssertionError("unexpected exception", actual);
        }
        throw new AssertionError("missing " + kind.getSimpleName());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void checkView(Map backing, Map raw, String label) {
        expect(ClassCastException.class, () -> raw.merge("wrong-key", 2, (a, b) -> b));
        check(backing.isEmpty(), label + " accepted invalid key");
        expect(ClassCastException.class, () -> raw.merge(7, "wrong-value", (a, b) -> b));
        check(backing.isEmpty(), label + " accepted invalid value");
        check(Integer.valueOf(2).equals(raw.merge(7, 2, (a, b) -> b)), label + " insert");
        int[] callbacks = {0};
        expect(ClassCastException.class, () -> raw.merge(7, "wrong-value", (a, b) -> {
            callbacks[0]++;
            return b;
        }));
        check(callbacks[0] == 0 && Integer.valueOf(2).equals(backing.get(7)),
                label + " invalid merge mutated or invoked callback");
        check(Integer.valueOf(5).equals(raw.merge(7, 3,
                (a, b) -> (Integer) a + (Integer) b)), label + " remap");
        check(Integer.valueOf(5).equals(backing.get(7)), label + " writeback");
        expect(NullPointerException.class, () -> raw.merge("wrong-key", 2, null));
        check(backing.size() == 1, label + " null function mutated map");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void checkDiagnostics() {
        Map raw = Collections.checkedMap(new HashMap<>(), Integer.class, Integer.class);
        String fromPutKey = message(() -> raw.put("bad", 3));
        String fromMergeKey = message(() -> raw.merge("bad", 3, (a, b) -> a));
        check(fromPutKey.equals(fromMergeKey), "wrong-key CCE message changed");
        String fromPutValue = message(() -> raw.put(7, "bad"));
        String fromMergeValue = message(() -> raw.merge(7, "bad", (a, b) -> a));
        check(fromPutValue.equals(fromMergeValue), "wrong-value CCE message changed");
    }

    private static String message(Runnable action) {
        try {
            action.run();
        } catch (ClassCastException expected) {
            return String.valueOf(expected.getMessage());
        }
        throw new AssertionError("expected ClassCastException");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void checkConcurrent() throws Exception {
        Map<Integer, Integer> backing = new ConcurrentHashMap<>();
        Map<Integer, Integer> checked = Collections.checkedMap(backing, Integer.class, Integer.class);
        Map raw = checked;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            Future<?>[] jobs = new Future<?>[4];
            for (int worker = 0; worker < jobs.length; worker++) {
                jobs[worker] = pool.submit(() -> {
                    start.await();
                    for (int i = 0; i < 128; i++) {
                        checked.merge(i % 16, 1, Integer::sum);
                        if ((i % 29) == 0) {
                            expect(ClassCastException.class,
                                    () -> raw.merge("bad-concurrent", 1, (a, b) -> a));
                        }
                    }
                    return null;
                });
            }
            start.countDown();
            for (Future<?> job : jobs) job.get(20, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
            check(pool.awaitTermination(10, TimeUnit.SECONDS), "workers did not stop");
        }
        check(backing.size() == 16, "concurrent invalid key inserted");
        for (int i = 0; i < 16; i++) {
            check(Integer.valueOf(32).equals(backing.get(i)), "concurrent key " + i);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void main(String[] args) throws Exception {
        Map ordinary = new HashMap();
        checkView(ordinary, Collections.checkedMap(ordinary, Integer.class, Integer.class), "map");
        Comparator<Object> loose = (a, b) -> a.toString().compareTo(b.toString());
        Map sorted = new TreeMap(loose);
        checkView(sorted, Collections.checkedSortedMap((java.util.SortedMap) sorted,
                Integer.class, Integer.class), "sorted");
        Map navigable = new TreeMap(loose);
        checkView(navigable, Collections.checkedNavigableMap((java.util.NavigableMap) navigable,
                Integer.class, Integer.class), "navigable");
        Map concurrent = new ConcurrentHashMap();
        checkView(concurrent, Collections.checkedMap(concurrent,
                Integer.class, Integer.class), "concurrent");
        checkDiagnostics();
        checkConcurrent();
        System.out.println("PASS JDK-8292955 checked-map jtreg assertions=" + assertions.get());
    }
}
