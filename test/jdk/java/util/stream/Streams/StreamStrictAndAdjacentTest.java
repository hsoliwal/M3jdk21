/*
 * Copyright 2026 Synexia contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Strict zip and adjacent runs obey source length, ordering, boundedness and close contracts
 * @run main StreamStrictAndAdjacentTest
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.stream.Streams;

public final class StreamStrictAndAdjacentTest {
    private static int checks;

    private static void check(boolean test, String detail) {
        checks++;
        if (!test) throw new AssertionError(detail);
    }

    private static void equal(Object actual, Object expected, String detail) {
        check(Objects.equals(actual, expected), detail + " expected=" + expected + " actual=" + actual);
    }

    private static void expect(Class<? extends Throwable> type, Runnable task, String detail) {
        try {
            task.run();
            throw new AssertionError("accepted " + detail);
        } catch (Throwable result) {
            if (result instanceof AssertionError assertion) throw assertion;
            check(type.isInstance(result), detail + " returned " + result);
        }
    }

    public static void main(String[] arguments) {
        equal(Streams.zipStrict(Stream.of(1, 2), Stream.of(3, 4),
                Integer::sum).toList(), List.of(4, 6), "equal length");
        equal(Streams.zipStrict(Stream.empty(), Stream.empty(),
                (left, right) -> left).count(), 0L, "empty equal length");
        expect(IllegalArgumentException.class, () -> Streams.zipStrict(
                Stream.of(1, 2), Stream.of(3), Integer::sum).count(),
                "mismatch must not be hidden by sized count");
        expect(IllegalArgumentException.class, () -> Streams.zipStrict(
                Stream.of(1), Stream.of(3, 4), Integer::sum).toList(), "right side longer");
        equal(Streams.zipStrict(Stream.of(1, 2), Stream.of(3),
                Integer::sum).limit(1).toList(), List.of(4), "short circuit before mismatch");
        equal(Streams.zipStrict(Stream.of(1, null), Stream.of(3, 4),
                (left, right) -> left == null ? null : left + right).toList(),
                Arrays.asList(4, null), "nullable strict zip");
        Stream<Integer> one = Stream.of(1);
        expect(IllegalArgumentException.class,
                () -> Streams.zipStrict(one, one, Integer::sum), "identical source");
        one.close();
        Spliterator<Integer> zipFlags = Streams.zipStrict(Stream.of(1),
                Stream.of(2), Integer::sum).spliterator();
        check((zipFlags.characteristics() & Spliterator.SIZED) == 0,
                "strict zip must not advertise a known exact size");
        check(zipFlags.trySplit() == null, "strict zip unsplittable");

        AtomicInteger leftClosed = new AtomicInteger();
        AtomicInteger rightClosed = new AtomicInteger();
        try (Stream<Integer> combined = Streams.zipStrict(
                Stream.of(1, 2).onClose(leftClosed::incrementAndGet),
                Stream.of(3, 4).onClose(rightClosed::incrementAndGet), Integer::sum)) {
            equal(combined.findFirst().orElseThrow(), 4, "short close input");
        }
        equal(leftClosed.get(), 1, "left close once");
        equal(rightClosed.get(), 1, "right close once");

        Stream<Integer> failingClose = Streams.zipStrict(
                Stream.of(1).onClose(() -> { throw new IllegalStateException("left"); }),
                Stream.of(2).onClose(() -> { throw new IllegalArgumentException("right"); }),
                Integer::sum);
        try {
            failingClose.close();
            throw new AssertionError("both close exceptions lost");
        } catch (IllegalStateException expected) {
            equal(expected.getMessage(), "left", "first close failure");
            check(expected.getSuppressed().length == 1
                    && expected.getSuppressed()[0] instanceof IllegalArgumentException,
                    "second close is suppressed");
        }

        equal(Streams.groupAdjacent(Stream.<Integer>empty(), value -> value, 2).toList(),
                List.of(), "empty groups");
        equal(Streams.groupAdjacent(Stream.of(1, 1, 2, 2, 1),
                value -> value, 2).toList(),
                List.of(List.of(1, 1), List.of(2, 2), List.of(1)),
                "adjacent groups are not global grouping");
        equal(Streams.groupAdjacent(Stream.of(null, null, 1, null),
                value -> value, 2).toList(),
                Arrays.asList(Arrays.asList(null, null), List.of(1),
                        Arrays.asList((Integer) null)), "null keys and occurrences");
        expect(IllegalStateException.class, () -> Streams.groupAdjacent(
                Stream.of(1, 1, 1), value -> value, 2).toList(), "run budget");
        equal(Streams.groupAdjacent(Stream.of(1, 1, 1), value -> value, 2)
                .limit(0).count(), 0L, "zero-limit grouping is lazy");
        expect(IllegalArgumentException.class,
                () -> Streams.groupAdjacent(Stream.of(1), value -> value, 0), "run width zero");
        expect(IllegalArgumentException.class, () -> Streams.groupAdjacent(
                Stream.of(1), value -> value, 65_537), "run width too large");
        List<List<Integer>> stable = Streams.groupAdjacent(Stream.of(1, 1, 2),
                value -> value, 3).toList();
        expect(UnsupportedOperationException.class,
                () -> stable.get(0).add(9), "each run immutable");
        equal(stable.get(0), List.of(1, 1), "sibling run independent");

        AtomicInteger consumed = new AtomicInteger();
        AtomicInteger keyed = new AtomicInteger();
        AtomicInteger sourceClosed = new AtomicInteger();
        try (Stream<List<Integer>> groups = Streams.groupAdjacent(
                Stream.of(1, 1, 2, 3).peek(value -> consumed.incrementAndGet())
                    .onClose(sourceClosed::incrementAndGet),
                value -> { keyed.incrementAndGet(); return value; }, 4)) {
            equal(consumed.get(), 0, "group construction has zero consumption");
            equal(groups.findFirst().orElseThrow(), List.of(1, 1), "first adjacent run");
            equal(consumed.get(), 3, "one-element lookahead");
            equal(keyed.get(), 3, "key evaluated once for each consumed value");
        }
        equal(sourceClosed.get(), 1, "group close exactly once");

        Random random = new Random(0x5EED_2026L);
        for (int trial = 0; trial < 2_000; trial++) {
            int leftLength = random.nextInt(100);
            int rightLength = random.nextInt(100);
            int cap = 1 + random.nextInt(10);
            ArrayList<Integer> left = new ArrayList<>();
            ArrayList<Integer> right = new ArrayList<>();
            for (int i = 0; i < leftLength; i++)
                left.add(random.nextInt(7) == 0 ? null : random.nextInt(13));
            for (int i = 0; i < rightLength; i++)
                right.add(random.nextInt(7) == 0 ? null : random.nextInt(13));
            if (leftLength != rightLength) {
                expect(IllegalArgumentException.class, () -> Streams.zipStrict(
                        left.stream(), right.stream(), Objects::equals).toList(),
                        "random strict length " + trial);
            } else {
                ArrayList<Boolean> expected = new ArrayList<>();
                for (int i = 0; i < leftLength; i++)
                    expected.add(Objects.equals(left.get(i), right.get(i)));
                equal(Streams.zipStrict(left.stream(), right.stream(),
                        Objects::equals).toList(), expected, "random strict pairs " + trial);
            }

            ArrayList<List<Integer>> expectedRuns = new ArrayList<>();
            int index = 0;
            while (index < leftLength) {
                int next = index + 1;
                while (next < leftLength && Objects.equals(left.get(index), left.get(next)))
                    next++;
                if (next - index > cap) break;
                expectedRuns.add(new ArrayList<>(left.subList(index, next)));
                index = next;
            }
            if (index < leftLength) {
                expect(IllegalStateException.class, () -> Streams.groupAdjacent(
                        left.stream(), value -> value, cap).toList(),
                        "random run budget " + trial);
            } else {
                equal(Streams.groupAdjacent(left.stream(), value -> value, cap).toList(),
                        expectedRuns, "random exact grouping " + trial);
            }
        }
        System.out.println("M3_STREAM_STRICT_ADJACENT_PASS checks=" + checks);
    }
}
