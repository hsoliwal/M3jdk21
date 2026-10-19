/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 *
 * @test
 * @summary EnumMap bulk forEach, replaceAll and toString use ordinal-indexed values
 * @run main DirectOrdinalTraversal
 */

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public final class DirectOrdinalTraversal {
    private enum K { A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P }
    private static long checks;

    private static void check(boolean condition, String why) {
        checks++;
        if (!condition) throw new AssertionError(why + " #" + checks);
    }

    private static void equal(Object expected, Object actual, String why) {
        check(Objects.equals(expected, actual),
                why + " expected=" + expected + " actual=" + actual);
    }

    /** AbstractMap's old entry-iterator formatting, as an independent oracle. */
    private static <V> String entryString(EnumMap<K, V> source) {
        StringBuilder text = new StringBuilder("{");
        boolean comma = false;
        for (Map.Entry<K, V> entry : source.entrySet()) {
            if (comma) text.append(", ");
            comma = true;
            Object value = entry.getValue();
            text.append(entry.getKey()).append('=')
                    .append(value == source ? "(this Map)" : value);
        }
        return text.append('}').toString();
    }

    private static <V> List<String> entryPairs(EnumMap<K, V> source) {
        List<String> output = new ArrayList<>();
        for (Map.Entry<K, V> entry : source.entrySet())
            output.add(entry.getKey() + "=" + entry.getValue());
        return output;
    }

    private static <V> List<String> directPairs(EnumMap<K, V> source) {
        List<String> output = new ArrayList<>();
        source.forEach((key, value) -> output.add(key + "=" + value));
        return output;
    }

    private static void expectNpe(Runnable work, String what) {
        checks++;
        try {
            work.run();
        } catch (NullPointerException expected) {
            return;
        }
        throw new AssertionError(what + " should throw NPE");
    }

    private static void randomized() {
        Random random = new Random(0x4d33454e554d4cL);
        for (int round = 0; round < 400; round++) {
            EnumMap<K, Integer> map = new EnumMap<>(K.class);
            for (K key : K.values()) {
                if (random.nextInt(4) == 0) continue;
                Integer value = random.nextInt(5) == 0 ? null : random.nextInt(300) - 150;
                map.put(key, value);
            }
            equal(entryPairs(map), directPairs(map), "bulk enumeration");
            equal(entryString(map), map.toString(), "entry-compatible string");
            EnumMap<K, Integer> expected = new EnumMap<>(map);
            EnumMap<K, Integer> actual = new EnumMap<>(map);
            List<String> oldCalls = new ArrayList<>();
            List<String> newCalls = new ArrayList<>();
            BiFunction<K, Integer, Integer> operation = (key, value) ->
                    (value == null || key.ordinal() % 3 == 0) ? null
                            : value + (key.ordinal() & 7);
            for (Map.Entry<K, Integer> entry : expected.entrySet()) {
                oldCalls.add(entry.getKey() + "=" + entry.getValue());
                entry.setValue(operation.apply(entry.getKey(), entry.getValue()));
            }
            actual.replaceAll((key, value) -> {
                newCalls.add(key + "=" + value);
                return operation.apply(key, value);
            });
            equal(oldCalls, newCalls, "replaceAll callback order");
            equal(expected, actual, "replaceAll parallel ordinal values");
            equal(expected.size(), actual.size(), "null sentinel keeps mapping");
            equal(entryString(actual), actual.toString(), "replaceAll rendering");
            check(actual.keySet().equals(expected.keySet()), "live key mask unchanged");
            // Trigger partial-write behavior on callback failure.
            EnumMap<K, Integer> prior = new EnumMap<>(map);
            EnumMap<K, Integer> observed = new EnumMap<>(map);
            final int stopAt = Math.max(0, map.size() / 2);
            AtomicInteger referenceCalls = new AtomicInteger();
            AtomicInteger nativeCalls = new AtomicInteger();
            try {
                for (Map.Entry<K, Integer> entry : prior.entrySet()) {
                    if (referenceCalls.getAndIncrement() == stopAt)
                        throw new IllegalArgumentException("stop");
                    entry.setValue(900 + entry.getKey().ordinal());
                }
                throw new AssertionError("reference should fail");
            } catch (IllegalArgumentException expectedFailure) {
                check("stop".equals(expectedFailure.getMessage()), "reference callback error");
            }
            try {
                observed.replaceAll((key, value) -> {
                    if (nativeCalls.getAndIncrement() == stopAt)
                        throw new IllegalArgumentException("stop");
                    return 900 + key.ordinal();
                });
                throw new AssertionError("optimized callback should fail");
            } catch (IllegalArgumentException expectedFailure) {
                check("stop".equals(expectedFailure.getMessage()), "direct callback error");
            }
            equal(referenceCalls.get(), nativeCalls.get(), "callback count before failure");
            equal(prior, observed, "partial writes before callback failure");
        }
    }

    private static void selfReferenceAndEmpty() {
        EnumMap<K, Object> self = new EnumMap<>(K.class);
        equal("{}", self.toString(), "empty enum map");
        expectNpe(() -> self.forEach(null), "empty forEach");
        expectNpe(() -> self.replaceAll(null), "empty replaceAll");
        self.put(K.A, self);
        self.put(K.C, null);
        equal(entryString(self), self.toString(), "self reference and NULL sentinel");
        check(self.containsKey(K.C) && self.get(K.C) == null, "present null map slot");
        List<K> visited = new ArrayList<>();
        self.forEach((key, value) -> visited.add(key));
        equal(List.of(K.A, K.C), visited, "ordinal traversal order");
    }

    public static void main(String[] args) {
        selfReferenceAndEmpty();
        randomized();
        System.out.println("PASS DirectOrdinalTraversal checks=" + checks);
    }
}
