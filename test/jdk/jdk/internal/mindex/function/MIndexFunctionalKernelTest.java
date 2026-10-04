/*
 * Copyright (c) 2026, Contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * @test
 * @summary MIndex JDK-internal functional plans and primitive pipelines
 * @modules java.base/jdk.internal.mindex.function
 * @run main MIndexFunctionalKernelTest
 */

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import java.util.function.LongUnaryOperator;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import jdk.internal.mindex.function.MIndexDoublePipeline;
import jdk.internal.mindex.function.MIndexDoublePredicate;
import jdk.internal.mindex.function.MIndexDoubleUnaryOperator;
import jdk.internal.mindex.function.MIndexFunctionalSupport;
import jdk.internal.mindex.function.MIndexIntPipeline;
import jdk.internal.mindex.function.MIndexIntPredicate;
import jdk.internal.mindex.function.MIndexIntUnaryOperator;
import jdk.internal.mindex.function.MIndexLongPipeline;
import jdk.internal.mindex.function.MIndexLongPredicate;
import jdk.internal.mindex.function.MIndexLongUnaryOperator;

public class MIndexFunctionalKernelTest {
    private static long checks;

    public static void main(String[] args) throws Exception {
        intOperators();
        predicates();
        intPipeline();
        longPipeline();
        doublePipeline();
        functionalSupport();
        ordinaryFallbacks();
        concurrentReuse();
        System.out.println("MIndexFunctionalKernelTest checks=" + checks);
    }

    private static void intOperators() {
        IntUnaryOperator fused =
                MIndexIntUnaryOperator.add(2)
                        .andThen(MIndexIntUnaryOperator.multiply(3))
                        .andThen(MIndexIntUnaryOperator.subtract(1));
        check(fused instanceof MIndexIntUnaryOperator);
        checkEquals(20, fused.applyAsInt(5));
        checkEquals(3, ((MIndexIntUnaryOperator) fused).operationCount());

        IntUnaryOperator composed =
                MIndexIntUnaryOperator.multiply(10)
                        .compose(MIndexIntUnaryOperator.add(1));
        check(composed instanceof MIndexIntUnaryOperator);
        checkEquals(40, composed.applyAsInt(3));

        checkEquals(Integer.MIN_VALUE, MIndexIntUnaryOperator.abs().applyAsInt(Integer.MIN_VALUE));
        expect(ArithmeticException.class,
                () -> MIndexIntUnaryOperator.divide(0).applyAsInt(1));
    }

    private static void predicates() {
        IntPredicate between =
                MIndexIntPredicate.greaterOrEqual(3)
                        .and(MIndexIntPredicate.lessThan(8));
        check(between instanceof MIndexIntPredicate);
        check(between.test(3));
        check(between.test(7));
        check(!between.test(8));

        IntPredicate mixed =
                between.or(MIndexIntPredicate.equalTo(42)).negate();
        check(!mixed.test(42));
        check(mixed.test(2));

        check(MIndexDoublePredicate.isNaN().test(Double.NaN));
        check(!MIndexDoublePredicate.isNaN().test(1.0d));
        check(MIndexDoublePredicate.isFinite().test(-0.0d));
        check(MIndexDoublePredicate.isInfinite().test(Double.NEGATIVE_INFINITY));
        check(MIndexDoublePredicate.equalTo(+0.0d).test(-0.0d));
        check(!MIndexDoublePredicate.equalTo(Double.NaN).test(Double.NaN));
    }

    private static void intPipeline() {
        int[] input = {1, 2, 3, 4, 5, 6, 7};
        int[] expected = Arrays.stream(input)
                .map(value -> value * 2)
                .filter(value -> value > 4)
                .skip(1)
                .limit(2)
                .toArray();

        MIndexIntPipeline pipeline =
                MIndexIntPipeline.builder()
                        .map(MIndexIntUnaryOperator.multiply(2))
                        .filter(MIndexIntPredicate.greaterThan(4))
                        .skip(1)
                        .limit(2)
                        .freeze();

        MIndexIntPipeline.Workspace workspace = pipeline.workspace();
        int[] output = new int[input.length];
        int written = pipeline.transform(input, output, workspace);
        checkEquals(expected.length, written);
        checkArrayEquals(expected, Arrays.copyOf(output, written));

        checkEquals(2, pipeline.count(input, workspace));
        checkEquals(18L, pipeline.sum(input, workspace));
        checkEquals(80, pipeline.reduce(input, 1, (left, right) -> left * right, workspace));
        check(pipeline.anyMatch(input, value -> value == 10, workspace));
        check(pipeline.allMatch(input, value -> value >= 8, workspace));

        long encoded = pipeline.findFirstEncoded(input, workspace);
        check(MIndexIntPipeline.encodedPresent(encoded));
        checkEquals(8, MIndexIntPipeline.encodedInt(encoded));

        int[] collected = new int[2];
        int[] cursor = {0};
        pipeline.forEach(input, value -> collected[cursor[0]++] = value, workspace);
        checkArrayEquals(expected, collected);

        // A second execution with the same workspace must restart all slice state.
        checkEquals(18L, pipeline.sum(input, workspace));
    }

    private static void longPipeline() {
        long[] input = {1, 2, 3, 4, 5, 6};
        long[] expected = LongStream.of(input)
                .map(value -> value + 3)
                .filter(value -> (value & 1L) == 0L)
                .skip(1)
                .limit(2)
                .toArray();

        MIndexLongPipeline pipeline =
                MIndexLongPipeline.builder()
                        .map(MIndexLongUnaryOperator.add(3))
                        .filter(MIndexLongPredicate.equalTo(0)
                                .or(value -> (value & 1L) == 0L))
                        .skip(1)
                        .limit(2)
                        .freeze();

        MIndexLongPipeline.Workspace workspace = pipeline.workspace();
        long[] output = new long[input.length];
        int written = pipeline.transform(input, output, workspace);
        checkEquals(expected.length, written);
        checkArrayEquals(expected, Arrays.copyOf(output, written));

        long[] first = new long[1];
        check(pipeline.findFirst(input, first, workspace));
        checkEquals(expected[0], first[0]);
    }

    private static void doublePipeline() {
        double[] input = {
                -0.0d, 1.0d, 2.0d, 3.0d, Double.NaN, 4.0d
        };
        double[] expected = DoubleStream.of(input)
                .map(value -> value + 0.5d)
                .filter(Double::isFinite)
                .skip(1)
                .limit(3)
                .toArray();

        MIndexDoublePipeline pipeline =
                MIndexDoublePipeline.builder()
                        .map(MIndexDoubleUnaryOperator.add(0.5d))
                        .filter(MIndexDoublePredicate.isFinite())
                        .skip(1)
                        .limit(3)
                        .freeze();

        MIndexDoublePipeline.Workspace workspace = pipeline.workspace();
        double[] output = new double[input.length];
        int written = pipeline.transform(input, output, workspace);
        checkEquals(expected.length, written);
        for (int index = 0; index < written; index++) {
            checkEquals(
                    Double.doubleToLongBits(expected[index]),
                    Double.doubleToLongBits(output[index]));
        }

        double expectedSum = DoubleStream.of(expected).sum();
        double actualSum = pipeline.sum(input, workspace);
        checkEquals(
                Double.doubleToLongBits(expectedSum),
                Double.doubleToLongBits(actualSum));
    }

    private static void functionalSupport() {
        int[] range = new int[5];
        checkEquals(5, MIndexFunctionalSupport.range(1, 11, 2, range));
        checkArrayEquals(new int[] {1, 3, 5, 7, 9}, range);

        int[] descending = new int[4];
        checkEquals(4, MIndexFunctionalSupport.range(7, -1, -2, descending));
        checkArrayEquals(new int[] {7, 5, 3, 1}, descending);

        int[] iterated = new int[5];
        MIndexFunctionalSupport.iterate(
                1, MIndexIntUnaryOperator.multiply(2), 5, iterated);
        checkArrayEquals(new int[] {1, 2, 4, 8, 16}, iterated);

        int[] scan = new int[4];
        MIndexFunctionalSupport.scan(
                new int[] {1, 2, 3, 4}, 0, Integer::sum, scan);
        checkArrayEquals(new int[] {1, 3, 6, 10}, scan);

        checkEquals(
                24L,
                MIndexFunctionalSupport.fold(
                        new long[] {1, 2, 3, 4}, 1L, (left, right) -> left * right));
    }

    private static void ordinaryFallbacks() {
        IntUnaryOperator plan =
                MIndexIntUnaryOperator.add(2).andThen(value -> value * 5);
        checkEquals(35, plan.applyAsInt(5));

        MIndexIntPipeline pipeline =
                MIndexIntPipeline.builder()
                        .map((int value) -> value + 1)
                        .filter((int value) -> (value & 1) == 0)
                        .freeze();
        int[] output = new int[4];
        int written = pipeline.transform(
                new int[] {1, 2, 3, 4}, output, pipeline.workspace());
        checkArrayEquals(new int[] {2, 4}, Arrays.copyOf(output, written));

        LongUnaryOperator longFallback =
                MIndexLongUnaryOperator.add(1).andThen(value -> value * 2);
        checkEquals(8L, longFallback.applyAsLong(3L));

        DoubleUnaryOperator doubleFallback =
                MIndexDoubleUnaryOperator.add(0.5).andThen(Math::sqrt);
        checkEquals(
                Double.doubleToLongBits(Math.sqrt(4.5)),
                Double.doubleToLongBits(doubleFallback.applyAsDouble(4.0)));
    }

    private static void concurrentReuse() throws Exception {
        MIndexIntPipeline pipeline =
                MIndexIntPipeline.builder()
                        .map(MIndexIntUnaryOperator.add(1)
                                .andThen(MIndexIntUnaryOperator.multiply(2)))
                        .filter(MIndexIntPredicate.greaterThan(0))
                        .skip(3)
                        .limit(50)
                        .freeze();
        int[] input = IntStream.range(-20, 100).toArray();
        long expected = pipeline.sum(input, pipeline.workspace());

        int threads = 4;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        for (int thread = 0; thread < threads; thread++) {
            new Thread(() -> {
                try {
                    MIndexIntPipeline.Workspace workspace = pipeline.workspace();
                    ready.countDown();
                    start.await();
                    for (int iteration = 0; iteration < 1_000; iteration++) {
                        if (pipeline.sum(input, workspace) != expected) {
                            throw new AssertionError("concurrent result mismatch");
                        }
                    }
                } catch (Throwable problem) {
                    failure.compareAndSet(null, problem);
                } finally {
                    done.countDown();
                }
            }).start();
        }

        ready.await();
        start.countDown();
        done.await();
        if (failure.get() != null) throw new AssertionError(failure.get());
        checks += 4_000;
    }

    private static void check(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("check failed");
    }

    private static void checkEquals(long expected, long actual) {
        checks++;
        if (expected != actual) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }

    private static void checkArrayEquals(int[] expected, int[] actual) {
        checks++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(
                    "expected=" + Arrays.toString(expected)
                            + " actual=" + Arrays.toString(actual));
        }
    }

    private static void checkArrayEquals(long[] expected, long[] actual) {
        checks++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(
                    "expected=" + Arrays.toString(expected)
                            + " actual=" + Arrays.toString(actual));
        }
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable problem) {
            if (type.isInstance(problem)) return;
            throw new AssertionError("unexpected exception", problem);
        }
        throw new AssertionError("expected " + type.getName());
    }
}
