// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import jdk.m3.collections.ObjectStream;

/** Standalone cross-language GDK admission contract; requires jdk.m3.collections. */
public final class M3GroovyGroupByManyTest {
    private static int checks;
    private M3GroovyGroupByManyTest() { }

    public static void main(String[] args) {
        Map<Character, List<String>> result = ObjectStream.snapshot(List.of("ant", "bee", "ape", "cow"))
                .groupByMany(s -> vowels(s), 4, 5, 8);
        eq(result.keySet().toString(), "[a, e, o]", "first-seen key order");
        eq(result.get('a'), List.of("ant", "ape"), "multiple memberships a");
        eq(result.get('e'), List.of("bee", "ape"), "multiple memberships e");
        eq(result.get('o'), List.of("cow"), "multiple memberships o");
        eq(result.size(), 3, "key count");
        result.get('a').add("later");
        eq(result.get('a').size(), 3, "independent mutable result");

        AtomicInteger mappings = new AtomicInteger();
        Map<String, List<Integer>> mapped = ObjectStream.snapshot(List.of(7))
                .groupByMany(n -> mappings.incrementAndGet(), n -> Arrays.asList("x", "x", "y"),
                        1, 2, 3);
        eq(mappings.get(), 3, "mapper invoked per membership");
        eq(mapped.get("x"), List.of(1, 2), "repeated key occurrences retained");
        eq(mapped.get("y"), List.of(3), "second key retained");

        Map<String, List<String>> nullIterable = ObjectStream.snapshot(List.of("skip", "keep"))
                .groupByMany(v -> v.equals("skip") ? null : List.of("ok"), 2, 1, 1);
        eq(nullIterable.get("ok"), List.of("keep"), "null iterable skips element");
        eq(ObjectStream.snapshot(List.<String>of())
                .groupByMany(v -> List.of("x"), 0, 0, 0).size(), 0, "empty source/zero budgets");

        List<String> nullable = new ArrayList<>();
        nullable.add(null);
        nullable.add("x");
        Map<String, List<String>> nullGroup = ObjectStream.snapshot(nullable)
                .groupByMany(v -> Arrays.asList(v == null ? null : "x"), 2, 2, 2);
        eq(nullGroup.get(null).size(), 1, "null group key");
        eq(nullGroup.get(null).get(0), null, "null value preserved");
        eq(nullGroup.get("x"), List.of("x"), "non-null key preserved");

        expect(IllegalStateException.class, () -> ObjectStream.snapshot(List.of(1, 2))
                .groupByMany(i -> List.of(), 1, 0, 0), "source budget with no keys");
        expect(IllegalStateException.class, () -> ObjectStream.snapshot(List.of(1))
                .groupByMany(i -> List.of("a", "b"), 1, 1, 2), "distinct key budget");
        expect(IllegalStateException.class, () -> ObjectStream.snapshot(List.of(1))
                .groupByMany(i -> List.of("a", "a"), 1, 1, 1), "membership budget");
        expect(IllegalStateException.class, () -> ObjectStream.snapshot(List.of(1))
                .groupByMany(i -> List.of("a"), 0, 1, 1), "zero source budget");
        expect(IllegalArgumentException.class, () -> ObjectStream.snapshot(List.of(1))
                .groupByMany(i -> List.of("a"), 1, 1, -1L), "negative budget");
        expect(NullPointerException.class, () -> ObjectStream.snapshot(List.of(1))
                .groupByMany((java.util.function.Function<Integer, Iterable<Integer>>) null,
                        1, 1, 1), "null mapper");

        AtomicBoolean closed = new AtomicBoolean();
        expect(IllegalStateException.class, () -> ObjectStream.once(Stream.of("x")
                .onClose(() -> closed.set(true)))
                .groupByMany(s -> List.of("a", "b"), 1, 1, 2), "source failure");
        eq(closed.get(), true, "stream closes on failure");

        AtomicBoolean closeOnCallback = new AtomicBoolean();
        expect(IllegalArgumentException.class, () -> ObjectStream.once(Stream.of("x")
                .onClose(() -> closeOnCallback.set(true)))
                .groupByMany(s -> { throw new IllegalArgumentException("key error"); }, 1, 1, 1),
                "key callback failure");
        eq(closeOnCallback.get(), true, "stream closes on callback failure");

        Map<String, List<Integer>> replay = ObjectStream.snapshot(List.of(1, 2, 3))
                .groupByMany(i -> List.of("k"), 3, 1, 3);
        eq(replay.get("k"), List.of(1, 2, 3), "encounter order");
        System.out.println("M3_GROOVY_GROUP_BY_MANY_PASS checks=" + checks);
    }

    private static List<Character> vowels(String s) {
        List<Character> out = new ArrayList<>();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ("aeiou".indexOf(ch) >= 0 && !out.contains(ch)) out.add(ch);
        }
        return out;
    }

    private static void eq(Object got, Object expected, String name) {
        if (!java.util.Objects.equals(got, expected)) {
            throw new AssertionError(name + " expected=" + expected + " actual=" + got);
        }
        checks++;
    }

    private static void expect(Class<? extends Throwable> kind, Runnable action, String name) {
        try {
            action.run();
            throw new AssertionError("Expected " + kind.getName() + " in " + name);
        } catch (Throwable error) {
            if (!kind.isInstance(error)) throw error;
        }
        checks++;
    }
}
