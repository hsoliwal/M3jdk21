/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Null-filtered stream mapping preserves demand, modes, effects and ownership
 * @run main StreamMapNotNullTest
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.Streams;

public class StreamMapNotNullTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        valuesNullsAndIdentity();
        deterministicDifferentialCases();
        lazinessAndMode();
        shortCircuitDemand();
        callbackAndTraversalFailures();
        closeOwnership();
        validationAndConsumedSources();
        genericVariance();
        System.out.println("StreamMapNotNullTest checks=" + checks);
    }

    private static void valuesNullsAndIdentity() {
        equal(Streams.mapNotNull(Stream.<Integer>empty(), v -> v).toList(),
                List.of(), "empty input");
        equal(Streams.mapNotNull(Stream.of(1, 2, 3), v -> (String) null).toList(),
                List.of(), "all results discarded");
        equal(Streams.mapNotNull(Stream.of(null, "", "x"),
                v -> v == null ? "null-input" : v).toList(),
                List.of("null-input", "", "x"), "mapper receives null input");
        List<Object> values = Arrays.asList(null, false, 0, "", List.of(), "x");
        equal(Streams.mapNotNull(values.stream(), Function.identity()).toList(),
                List.of(false, 0, "", List.of(), "x"), "null filtering is not truth filtering");
        Object shared = new Object();
        List<Object> same = Streams.mapNotNull(Stream.of(1, 2), v -> shared).toList();
        check(same.size() == 2 && same.get(0) == shared && same.get(1) == shared,
                "retain exact mapped references and duplicates");
        AtomicInteger calls = new AtomicInteger();
        long count = Streams.mapNotNull(Stream.of(1, 2, 3, 4), v -> {
            calls.incrementAndGet();
            return v % 2 == 0 ? v : null;
        }).count();
        equal(count, 2L, "count depends on actual mapped results");
        equal(calls.get(), 4, "sized source cannot elide null-filter mapping for count");
    }

    private static void deterministicDifferentialCases() {
        Random random = new Random(0x47524f4f56594cL);
        for (int trial = 0; trial < 250; trial++) {
            List<Integer> values = new ArrayList<>();
            int size = random.nextInt(151);
            for (int i = 0; i < size; i++) {
                values.add(random.nextInt(5) == 0 ? null : random.nextInt(101) - 50);
            }
            List<String> expected = new ArrayList<>();
            for (Integer value : values) {
                String result = transform(value);
                if (result != null) expected.add(result);
            }
            for (boolean parallel : List.of(false, true)) {
                AtomicInteger calls = new AtomicInteger();
                Stream<Integer> input = parallel ? values.parallelStream() : values.stream();
                List<String> actual = Streams.mapNotNull(input, value -> {
                    calls.incrementAndGet();
                    return transform(value);
                }).toList();
                equal(actual, expected, "ordered differential trial " + trial + " parallel=" + parallel);
                equal(calls.get(), values.size(), "one mapping per examined input");
            }
        }
        List<Integer> ordered = Streams.mapNotNull(IntStream.range(0, 100_000)
                .parallel().boxed(), v -> v % 13 == 0 ? v : null).toList();
        List<Integer> expected = new ArrayList<>();
        for (int i = 0; i < 100_000; i += 13) expected.add(i);
        equal(ordered, expected, "large parallel encounter order");
    }

    private static String transform(Integer value) {
        if (value == null) return "null";
        return value % 3 == 0 ? null : "value=" + value;
    }

    private static void lazinessAndMode() {
        for (boolean parallel : List.of(false, true)) {
            AtomicInteger traversed = new AtomicInteger();
            AtomicInteger mapped = new AtomicInteger();
            Stream<Integer> input = Stream.of(5, 3, 1, 4, 2)
                    .peek(v -> traversed.incrementAndGet()).sorted();
            if (parallel) input = input.parallel();
            try (Stream<Integer> result = Streams.mapNotNull(input, v -> {
                mapped.incrementAndGet();
                return v % 2 == 1 ? v : null;
            })) {
                equal(result.isParallel(), parallel, "preserve source execution mode");
                equal(traversed.get(), 0, "construction does not evaluate upstream sorting");
                equal(mapped.get(), 0, "construction does not invoke mapper");
                equal(result.toList(), List.of(1, 3, 5), "stateful upstream values");
                equal(traversed.get(), 5, "upstream evaluated once");
                equal(mapped.get(), 5, "mapper evaluated once per input");
            }
        }
        Spliterator<Integer> ordered = Streams.mapNotNull(Stream.of(1, 2, 3), v -> v).spliterator();
        check(ordered.hasCharacteristics(Spliterator.ORDERED), "ordered source retains order");
        check(!ordered.hasCharacteristics(Spliterator.SIZED), "result cardinality is unknown");
        Spliterator<Integer> unordered = Streams.mapNotNull(Stream.of(1, 2, 3).unordered(), v -> v)
                .spliterator();
        check(!unordered.hasCharacteristics(Spliterator.ORDERED), "do not impose an order");
        equal(Streams.mapNotNull(Stream.of(1, 2, 3), v -> 7).toList(), List.of(7, 7, 7),
                "mapping does not promise distinct output");
        equal(Streams.mapNotNull(Stream.of(1, 2, 3), v -> -v).toList(), List.of(-1, -2, -3),
                "mapping does not promise sorted output");
    }

    private static void shortCircuitDemand() {
        AtomicInteger calls = new AtomicInteger();
        equal(Streams.mapNotNull(Stream.of(1, 2, 3), v -> {
            calls.incrementAndGet(); return v;
        }).limit(0).count(), 0L, "zero limit");
        equal(calls.get(), 0, "zero demand invokes no mapper");
        calls.set(0);
        equal(Streams.mapNotNull(Stream.iterate(0, v -> v + 1), v -> {
            calls.incrementAndGet(); return v % 7 == 0 ? v : null;
        }).limit(3).toList(), List.of(0, 7, 14), "finite demand on infinite input");
        equal(calls.get(), 15, "sequential limit examines only needed prefix");
        calls.set(0);
        equal(Streams.mapNotNull(IntStream.rangeClosed(1, 100).boxed(), v -> {
            calls.incrementAndGet(); return v == 5 ? "found" : null;
        }).findFirst(), Optional.of("found"), "first non-null mapping");
        equal(calls.get(), 5, "first result terminates sequential traversal");
        calls.set(0);
        equal(Streams.mapNotNull(Stream.of(1, 2, 3), v -> {
            calls.incrementAndGet(); return v == 2 ? null : v;
        }).toList(), List.of(1, 3), "a null mapping filters but does not terminate");
        equal(calls.get(), 3, "resume after null mapping");
    }

    private static void callbackAndTraversalFailures() throws Exception {
        RuntimeException mapperFailure = new IllegalArgumentException("mapper");
        List<Integer> observed = new ArrayList<>();
        RuntimeException actual = expect(RuntimeException.class, () ->
                Streams.mapNotNull(Stream.of(1, 2, 3, 4), v -> {
                    observed.add(v);
                    if (v == 3) throw mapperFailure;
                    return v == 1 ? null : v;
                }).toList());
        check(actual == mapperFailure, "mapper failure identity");
        equal(observed, List.of(1, 2, 3), "no callback after sequential failure");
        RuntimeException upstreamFailure = new IllegalStateException("upstream");
        actual = expect(RuntimeException.class, () -> Streams.mapNotNull(
                Stream.of(1).map(v -> { throw upstreamFailure; }), Function.identity()).toList());
        check(actual == upstreamFailure, "upstream failure identity");
        RuntimeException downstreamFailure = new IllegalStateException("downstream");
        actual = expect(RuntimeException.class, () -> Streams.mapNotNull(Stream.of(1, 2), v -> v)
                .forEach(v -> { throw downstreamFailure; }));
        check(actual == downstreamFailure, "downstream failure identity");
    }

    private static void closeOwnership() throws Exception {
        AtomicInteger closed = new AtomicInteger();
        Stream<Integer> result = Streams.mapNotNull(Stream.of(1, 2)
                .onClose(closed::incrementAndGet), v -> v);
        equal(result.toList(), List.of(1, 2), "close ownership values");
        equal(closed.get(), 0, "exhaustion does not implicitly close");
        result.close();
        result.close();
        equal(closed.get(), 1, "close delegates exactly once");
        closed.set(0);
        try (Stream<Integer> unused = Streams.mapNotNull(Stream.of(1)
                .onClose(closed::incrementAndGet), v -> { throw new AssertionError("not demanded"); })) {
            check(!unused.isParallel(), "unused result is open");
        }
        equal(closed.get(), 1, "close without traversal");
        RuntimeException primary = new IllegalStateException("mapper primary");
        RuntimeException cleanup = new IllegalArgumentException("close secondary");
        RuntimeException actual = expect(RuntimeException.class, () -> {
            try (Stream<Integer> stream = Streams.mapNotNull(Stream.of(1)
                    .onClose(() -> { throw cleanup; }), v -> { throw primary; })) {
                stream.toList();
            }
        });
        check(actual == primary, "try-with-resources preserves primary failure");
        check(actual.getSuppressed().length == 1 && actual.getSuppressed()[0] == cleanup,
                "close failure is suppressed on primary");
    }

    private static void validationAndConsumedSources() throws Exception {
        expect(NullPointerException.class, () -> Streams.mapNotNull(null, Function.identity()));
        AtomicInteger closed = new AtomicInteger();
        try (Stream<Integer> input = Stream.of(1, 2).onClose(closed::incrementAndGet)) {
            expect(NullPointerException.class, () -> Streams.mapNotNull(input, null));
            equal(closed.get(), 0, "invalid mapper does not close source");
            equal(input.toList(), List.of(1, 2), "invalid mapper does not consume source");
        }
        equal(closed.get(), 1, "caller still owns invalid-construction source");
        Stream<Integer> consumed = Stream.of(1);
        consumed.count();
        expect(IllegalStateException.class, () -> Streams.mapNotNull(consumed, v -> v));
        Stream<Integer> shut = Stream.of(1);
        shut.close();
        expect(IllegalStateException.class, () -> Streams.mapNotNull(shut, v -> v));
        Stream<Integer> linked = Stream.of(1, 2);
        try (Stream<Integer> output = Streams.mapNotNull(linked, v -> v)) {
            expect(IllegalStateException.class, linked::count);
            equal(output.toList(), List.of(1, 2), "linked result retains ownership");
        }
    }

    private static void genericVariance() {
        Stream<? extends Integer> input = Stream.of(1, 2);
        Function<Number, String> mapper = v -> v.intValue() == 1 ? "one" : null;
        Stream<CharSequence> output = Streams.<Integer, CharSequence>mapNotNull(input, mapper);
        equal(output.toList(), List.of("one"), "receiver and mapper variance");
    }

    private static void equal(Object actual, Object expected, String message) {
        check(Objects.equals(actual, expected), message + ": expected=" + expected + ", actual=" + actual);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static <T extends Throwable> T expect(Class<T> type, Action action) throws Exception {
        checks++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return type.cast(failure);
            throw new AssertionError("expected " + type + ", received " + failure, failure);
        }
        throw new AssertionError("expected " + type);
    }

    @FunctionalInterface
    private interface Action { void run() throws Exception; }
}
