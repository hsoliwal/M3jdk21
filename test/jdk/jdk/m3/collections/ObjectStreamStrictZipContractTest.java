/*
 * Copyright 2026 Synexia contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary M3 ObjectStream strict zip and original chunkBy compatibility with java.base
 * @modules jdk.m3.collections
 * @run main ObjectStreamStrictZipContractTest
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.Streams;
import jdk.m3.collections.ObjectStream;

public final class ObjectStreamStrictZipContractTest {
    private static int checks;

    private static void check(boolean accepted, String message) {
        checks++;
        if (!accepted) throw new AssertionError(message);
    }

    private static void equal(Object actual, Object expected, String message) {
        check(Objects.equals(actual, expected), message
                + " expected=" + expected + " actual=" + actual);
    }

    private static void expect(Class<? extends Throwable> klass,
            Runnable operation, String name) {
        try {
            operation.run();
            throw new AssertionError("accepted " + name);
        } catch (Throwable outcome) {
            if (outcome instanceof AssertionError assertion) throw assertion;
            check(klass.isInstance(outcome), name + " failed with " + outcome);
        }
    }

    public static void main(String[] args) {
        var left = ObjectStream.snapshot(List.of(1, 2, 3));
        var right = ObjectStream.snapshot(List.of(4, 5, 6));
        var result = left.zipStrict(right, Integer::sum);
        check(result.replayable(), "both snapshot plans must be replayable");
        equal(result.toBoundedList(3), List.of(5, 7, 9), "strict values");
        equal(result.toBoundedList(3), List.of(5, 7, 9), "replay values");
        expect(IllegalArgumentException.class, () -> ObjectStream.snapshot(List.of(1, 2))
                .zipStrict(ObjectStream.snapshot(List.of(3)), Integer::sum).count(),
                "left longer count");
        expect(IllegalArgumentException.class, () -> ObjectStream.snapshot(List.of(1))
                .zipStrict(ObjectStream.snapshot(List.of(3, 4)), Integer::sum).count(),
                "right longer count");
        equal(ObjectStream.snapshot(List.of(1, 2))
                .zipStrict(ObjectStream.snapshot(List.of(3)), Integer::sum)
                .limit(1).toBoundedList(1), List.of(4), "short-circuit prefix");
        equal(ObjectStream.snapshot(Arrays.asList(1, null))
                .zipStrict(ObjectStream.snapshot(Arrays.asList(2, 3)),
                        (a, b) -> a == null ? null : a + b)
                .toBoundedList(2), Arrays.asList(3, null), "null mapper output");

        AtomicInteger closedA = new AtomicInteger();
        AtomicInteger closedB = new AtomicInteger();
        var first = ObjectStream.once(Stream.of(1, 2).onClose(closedA::incrementAndGet));
        var second = ObjectStream.once(Stream.of(4).onClose(closedB::incrementAndGet));
        var unequal = first.zipStrict(second, Integer::sum);
        check(!unequal.replayable(), "one-shot strict pair remains one shot");
        expect(IllegalArgumentException.class, unequal::count, "one-shot unequal lengths");
        equal(closedA.get(), 1, "first resource closed after failure");
        equal(closedB.get(), 1, "second resource closed after failure");
        expect(IllegalStateException.class, unequal::count, "one-shot must not reopen");

        AtomicInteger firstFailureClose = new AtomicInteger();
        var unused = ObjectStream.once(Stream.of(99));
        equal(unused.count(), 1L, "exhaust right first");
        var unopened = ObjectStream.once(Stream.of(1)
                .onClose(firstFailureClose::incrementAndGet));
        expect(IllegalStateException.class,
                () -> unopened.zipStrict(unused, Integer::sum).count(),
                "second opener fails");
        equal(firstFailureClose.get(), 1, "first opener ownership repaired");

        equal(ObjectStream.snapshot(List.of(1, 1, 2, 1))
                .chunkBy(Function.identity(), 2).toBoundedList(4),
                List.of(List.of(1, 1), List.of(2), List.of(1)),
                "chunkBy remains consecutive, not global");
        expect(IllegalStateException.class,
                () -> ObjectStream.snapshot(List.of(1, 1, 1))
                        .chunkBy(Function.identity(), 2).count(), "run width cap");
        equal(ObjectStream.snapshot(Arrays.asList(null, null, 2))
                .chunkBy(Function.identity(), 2).toBoundedList(2),
                Arrays.asList(Arrays.asList(null, null), List.of(2)), "nullable groups");

        Random random = new Random(0x2026_1121L);
        for (int trial = 0; trial < 1500; trial++) {
            int n = random.nextInt(50);
            int m = random.nextInt(50);
            int cap = 1 + random.nextInt(12);
            var a = new ArrayList<Integer>();
            var b = new ArrayList<Integer>();
            for (int i = 0; i < n; i++) a.add(random.nextInt(7) == 0 ? null : random.nextInt(7));
            for (int i = 0; i < m; i++) b.add(random.nextInt(7) == 0 ? null : random.nextInt(7));
            if (n != m) {
                expect(IllegalArgumentException.class,
                        () -> ObjectStream.snapshot(a)
                                .zipStrict(ObjectStream.snapshot(b), Objects::equals).count(),
                        "random mismatch " + trial);
            } else {
                var expected = new ArrayList<Boolean>();
                for (int i = 0; i < n; i++) expected.add(Objects.equals(a.get(i), b.get(i)));
                equal(ObjectStream.snapshot(a).zipStrict(ObjectStream.snapshot(b),
                        Objects::equals).toBoundedList(n), expected,
                        "random exact zip " + trial);
            }

            Object expectedRuns;
            try (Stream<List<Integer>> original = Streams.groupAdjacent(a.stream(),
                    Function.identity(), cap)) {
                expectedRuns = original.toList();
            } catch (IllegalStateException exceeded) {
                expectedRuns = IllegalStateException.class;
            }
            Object actualRuns;
            try {
                actualRuns = ObjectStream.snapshot(a).chunkBy(Function.identity(), cap)
                        .toBoundedList(n);
            } catch (IllegalStateException exceeded) {
                actualRuns = IllegalStateException.class;
            }
            equal(actualRuns, expectedRuns, "adjacent delegation " + trial);
        }

        System.out.println("M3_OBJECT_STREAM_STRICT_ZIP_PASS checks=" + checks);
    }
}
