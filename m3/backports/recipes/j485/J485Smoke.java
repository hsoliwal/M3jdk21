// SPDX-License-Identifier: Apache-2.0
package m3.j485;

import java.util.List;
import java.util.Objects;
import java.util.stream.Gatherer;
import java.util.stream.Gatherers;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/** Bounded Java21 patched-module smoke for the J485 candidate; not a jtreg replacement. */
public final class J485Smoke {
    private J485Smoke() {}

    public static void main(String[] args) {
        List<Integer> source = List.of(1, 2, 3, 4, 5);

        check(
                source.stream().gather(Gatherers.windowFixed(2)).toList(),
                List.of(List.of(1, 2), List.of(3, 4), List.of(5)),
                "windowFixed");
        check(
                source.stream().gather(Gatherers.windowSliding(3)).toList(),
                List.of(List.of(1, 2, 3), List.of(2, 3, 4), List.of(3, 4, 5)),
                "windowSliding");
        check(
                source.stream().gather(Gatherers.fold(() -> 0, Integer::sum)).toList(),
                List.of(15),
                "fold");
        check(
                source.stream().gather(Gatherers.scan(() -> 0, Integer::sum)).toList(),
                List.of(1, 3, 6, 10, 15),
                "scan");

        Gatherer<Integer, Void, Integer> finisher = Gatherer.ofSequential(
                (ignoredState, ignoredElement, ignoredDownstream) -> false,
                (ignoredState, downstream) -> downstream.push(8328316));
        check(source.stream().gather(finisher).toList(), List.of(8328316), "short-circuit finisher");

        List<Integer> expectedSquares = IntStream.range(0, 24).map(x -> x * x).boxed().toList();
        List<Integer> concurrent = IntStream.range(0, 24).boxed()
                .gather(Gatherers.mapConcurrent(4, J485Smoke::delayedSquare))
                .toList();
        check(concurrent, expectedSquares, "mapConcurrent encounter order");

        check(
                IntStream.rangeClosed(1, 100).boxed().parallel()
                        .gather(Gatherers.fold(() -> 0, Integer::sum)).toList(),
                List.of(5050),
                "parallel fold");

        assertThrows(IllegalArgumentException.class, () -> Gatherers.windowFixed(0), "windowFixed zero");
        assertThrows(IllegalArgumentException.class, () -> Gatherers.windowSliding(0), "windowSliding zero");
        assertThrows(IllegalArgumentException.class, () -> Gatherers.mapConcurrent(0, x -> x), "mapConcurrent zero");
        assertThrows(NullPointerException.class, () -> Gatherers.mapConcurrent(1, null), "mapConcurrent null");

        List<List<Integer>> fixed = source.stream().gather(Gatherers.windowFixed(2)).toList();
        assertThrows(UnsupportedOperationException.class, () -> fixed.getFirst().add(99), "window immutable");

        System.out.println("J485_SMOKE_PASS checks=12");
    }

    private static int delayedSquare(int value) {
        try {
            Thread.sleep((23 - value) % 4L);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(interrupted);
        }
        return value * value;
    }

    private static void check(Object actual, Object expected, String label) {
        if (!Objects.equals(actual, expected)) {
            throw new AssertionError(label + ": expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertThrows(
            Class<? extends Throwable> expected, Runnable action, String label) {
        try {
            action.run();
        } catch (Throwable failure) {
            if (expected.isInstance(failure)) {
                return;
            }
            throw new AssertionError(label + ": wrong exception " + failure, failure);
        }
        throw new AssertionError(label + ": expected " + expected.getName());
    }
}
