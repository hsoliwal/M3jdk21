// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;
import jdk.m3.collections.ObjectStream;

/** Standalone Java 21 contract and seeded differential for M3 countByMany. */
public final class M3GroovyCountByManyTest {
    private static int checks;
    private M3GroovyCountByManyTest() { }

    public static void main(String[] args) {
        Map<String, Long> tally = ObjectStream.snapshot(List.of("ant", "bee", "ape"))
                .countByMany(s -> s.equals("ant") ? List.of("a", "a") : List.of("b", "a"),
                        3, 2, 6);
        eq(tally, Map.of("a", 4L, "b", 2L), "duplicate key memberships count");
        eq(tally.keySet().toString(), "[a, b]", "first-seen key order");
        expect(UnsupportedOperationException.class, () -> tally.put("a", 9L), "read-only result");
        eq(ObjectStream.snapshot(List.<String>of())
                .countByMany(s -> List.of("x"), 0, 0, 0).isEmpty(), true, "zero budget empty");

        List<String> nullable = new ArrayList<>();
        nullable.add(null);
        nullable.add("a");
        nullable.add("skip");
        AtomicInteger calls = new AtomicInteger();
        Map<String, Long> withNull = ObjectStream.snapshot(nullable)
                .countByMany(s -> {
                    calls.incrementAndGet();
                    if ("skip".equals(s)) return null;
                    return Arrays.asList(null, s);
                }, 3, 3, 4);
        eq(calls.get(), 3, "one key-mapper call per item");
        eq(withNull.get(null), 3L, "null-key membership count");
        eq(withNull.get("a"), 1L, "non-null key count");
        eq(withNull.size(), 2, "null iterable does not create groups");
        eq(withNull.keySet().toString(), "[null, a]", "null first-seen key order");

        expect(IllegalStateException.class, () -> ObjectStream.snapshot(List.of(1, 2))
                .countByMany(i -> List.<String>of(), 1, 0, 0), "source budget without memberships");
        expect(IllegalStateException.class, () -> ObjectStream.snapshot(List.of(1))
                .countByMany(i -> List.of("a", "b"), 1, 1, 2), "key cap");
        expect(IllegalStateException.class, () -> ObjectStream.snapshot(List.of(1))
                .countByMany(i -> List.of("a", "a"), 1, 1, 1), "membership cap");
        expect(IllegalStateException.class, () -> ObjectStream.snapshot(List.of(1))
                .countByMany(i -> List.of("a"), 0, 1, 1), "zero source budget");
        expect(IllegalArgumentException.class, () -> ObjectStream.snapshot(List.of(1))
                .countByMany(i -> List.of("a"), 1, 1, -1), "negative membership budget");
        expect(IllegalArgumentException.class, () -> ObjectStream.snapshot(List.of(1))
                .countByMany(i -> List.of("a"), -1, 1, 1), "negative element budget");
        expect(NullPointerException.class, () -> ObjectStream.snapshot(List.of(1))
                .countByMany(null, 1, 1, 1), "null key mapper");

        AtomicBoolean closed = new AtomicBoolean();
        expect(IllegalStateException.class, () -> ObjectStream.once(Stream.of(1)
                .onClose(() -> closed.set(true)))
                .countByMany(i -> List.of("a", "b"), 1, 1, 2), "key limit closes stream");
        eq(closed.get(), true, "closed on budget failure");
        AtomicBoolean callbackClosed = new AtomicBoolean();
        expect(IllegalArgumentException.class, () -> ObjectStream.once(Stream.of(1)
                .onClose(() -> callbackClosed.set(true)))
                .countByMany(i -> { throw new IllegalArgumentException("key failure"); },
                        1, 1, 1), "callback failure propagated");
        eq(callbackClosed.get(), true, "closed on callback failure");

        Random random = new Random(0x636f756e7442794dL);
        for (int trial = 0; trial < 2_500; trial++) {
            ArrayList<Integer> items = new ArrayList<>();
            int n = random.nextInt(128);
            for (int i = 0; i < n; i++) {
                int number = random.nextInt(17);
                items.add(number == 0 ? null : number - 8);
            }
            Function<Integer, Iterable<String>> keys = item -> {
                if (item == null) return null;
                String first = "k" + Math.floorMod(item, 5);
                return Math.floorMod(item, 7) == 0
                        ? Arrays.asList(first, first, null) : List.of(first);
            };
            LinkedHashMap<String, Long> expected = new LinkedHashMap<>();
            long memberships = 0;
            for (Integer item : items) {
                Iterable<String> keySet = keys.apply(item);
                if (keySet == null) continue;
                for (String key : keySet) {
                    expected.merge(key, 1L, Math::addExact);
                    memberships++;
                }
            }
            Map<String, Long> actual = ObjectStream.snapshot(items)
                    .countByMany(keys, n, 6, memberships);
            eq(actual, expected, "random tally #" + trial);
            eq(actual.keySet().toString(), expected.keySet().toString(),
                    "random key order #" + trial);
        }
        System.out.println("M3_GROOVY_COUNT_BY_MANY_PASS checks=" + checks
                + " randomizedTrials=2500 seed=0x636f756e7442794d");
    }

    private static void eq(Object actual, Object expected, String message) {
        if (!java.util.Objects.equals(actual, expected)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
        checks++;
    }

    private static void expect(Class<? extends Throwable> expected, Runnable run, String message) {
        try { run.run(); }
        catch (Throwable problem) {
            if (expected.isInstance(problem)) { checks++; return; }
            throw new AssertionError(message + " wrong exception", problem);
        }
        throw new AssertionError(message + " did not throw " + expected.getName());
    }
}
