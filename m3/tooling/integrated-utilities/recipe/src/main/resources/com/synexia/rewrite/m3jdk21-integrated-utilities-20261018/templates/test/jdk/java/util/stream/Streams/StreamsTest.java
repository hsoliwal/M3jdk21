/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 */

/*
 * @test
 * @summary Public Streams factories, zip ownership, bounded traversal, and existing internals
 * @run main StreamsTest
 */

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.*;

public class StreamsTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        factories();
        zipValues();
        zipMetadata();
        zipOwnershipAndFailures();
        existingStreamOwners();
        System.out.println("StreamsTest checks=" + checks);
    }

    private static void factories() throws Exception {
        List<Integer> expected = IntStream.range(0, 5000).boxed().toList();
        try (Stream<Integer> values = Streams.stream(expected)) {
            check(!values.isParallel(), "iterable defaults sequential");
            check(values.toList().equals(expected), "iterable values");
        }
        try (Stream<Integer> values = Streams.stream(expected, true)) {
            check(values.isParallel(), "parallel iterable request");
            check(values.toList().equals(expected), "parallel iterable order");
        }
        TreeSet<Integer> sorted = new TreeSet<>(expected);
        Spliterator<Integer> split = Streams.stream(sorted).spliterator();
        check(split.hasCharacteristics(Spliterator.SORTED | Spliterator.ORDERED
                | Spliterator.DISTINCT | Spliterator.SIZED), "iterable characteristics retained");
        check(split.getExactSizeIfKnown() == expected.size() && split.getComparator() == null, "iterable size and comparator");
        CloseableIterator source = new CloseableIterator(expected.iterator());
        Stream<Number> stream = Streams.stream(source);
        check(source.nextCalls == 0 && source.hasNextCalls == 0, "iterator creation is lazy");
        check(stream.limit(3).toList().equals(List.of(0, 1, 2)), "iterator covariance and short circuit");
        check(source.nextCalls == 3, "iterator only consumes requested elements");
        stream.close();
        check(source.closed == 0, "stream does not close iterator");
        try (Stream<Integer> values = Streams.stream(expected.iterator(), true)) {
            check(values.isParallel(), "parallel iterator request");
            check(values.toList().equals(expected), "batched parallel iterator encounter order");
        }
        check(Streams.stream(Collections.enumeration(expected)).toList().equals(expected), "enumeration order");
        check(Streams.stream(Collections.emptyEnumeration()).count() == 0, "empty enumeration");
        Spliterator<Integer> unknown = Streams.stream(expected.iterator()).spliterator();
        check(unknown.hasCharacteristics(Spliterator.ORDERED), "iterator encounter order");
        check(unknown.getExactSizeIfKnown() == -1, "iterator size not invented");
        ArrayList<Integer> late = new ArrayList<>(List.of(1));
        Stream<Integer> lateStream = Streams.stream(late);
        late.add(2);
        check(lateStream.toList().equals(List.of(1, 2)), "source late binding retained");
        expect(NullPointerException.class, () -> Streams.stream((Iterable<Object>) null));
        expect(NullPointerException.class, () -> Streams.stream((Iterator<Object>) null));
        expect(NullPointerException.class, () -> Streams.stream((Enumeration<Object>) null));
    }

    private static void zipValues() throws Exception {
        for (int left : new int[] {0, 1, 3, 17}) {
            for (int right : new int[] {0, 1, 3, 17}) {
                AtomicInteger readLeft = new AtomicInteger();
                AtomicInteger readRight = new AtomicInteger();
                AtomicInteger paired = new AtomicInteger();
                int[] closes = new int[2];
                Stream<Integer> a = IntStream.range(0, left).boxed().parallel()
                        .peek(x -> readLeft.incrementAndGet()).onClose(() -> closes[0]++);
                Stream<Integer> b = IntStream.range(100, 100 + right).boxed().parallel()
                        .peek(x -> readRight.incrementAndGet()).onClose(() -> closes[1]++);
                int minimum = Math.min(left, right);
                List<String> expected = IntStream.range(0, minimum)
                        .mapToObj(i -> i + ":" + (100 + i)).toList();
                try (Stream<String> result = Streams.zip(a, b, (x, y) -> {
                    paired.incrementAndGet();
                    return x + ":" + y;
                })) {
                    check(!result.isParallel(), "zip initially sequential");
                    check(readLeft.get() == 0 && readRight.get() == 0 && paired.get() == 0, "zip creation does not traverse");
                    check(result.parallel().toList().equals(expected), "zip pairs in source order");
                    check(paired.get() == minimum, "one zipper call per result");
                    check(readLeft.get() <= minimum + 1 && readRight.get() <= minimum + 1, "bounded unpaired read ahead");
                    check(closes[0] == 0 && closes[1] == 0, "exhaustion does not close sources");
                }
                check(closes[0] == 1 && closes[1] == 1, "zip closes both sources once");
            }
        }
        try (Stream<Integer> result = Streams.zip(Stream.iterate(0, i -> i + 1),
                Stream.iterate(10, i -> i + 1), Integer::sum)) {
            check(result.limit(4).toList().equals(List.of(10, 12, 14, 16)), "infinite inputs support short circuit");
        }
        check(Streams.zip(Stream.of((String) null), Stream.of("x"),
                (a, b) -> a).toList().equals(Arrays.asList((String) null)), "null inputs/results permitted");
    }

    private static void zipMetadata() throws Exception {
        Stream<Integer> result = Streams.zip(Stream.of(1, 2, 3), Stream.of(4, 5), Integer::sum);
        Spliterator<Integer> split = result.spliterator();
        check(split.trySplit() == null, "zip source is unsplittable");
        check(split.hasCharacteristics(Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED), "zip common metadata");
        check(split.getExactSizeIfKnown() == 2, "zip exact minimum size");
        check(!split.hasCharacteristics(Spliterator.SORTED) && !split.hasCharacteristics(Spliterator.DISTINCT)
                && !split.hasCharacteristics(Spliterator.NONNULL), "zip drops derived characteristics");
        expect(IllegalStateException.class, split::getComparator);
        List<Integer> got = new ArrayList<>();
        check(split.tryAdvance(got::add) && split.estimateSize() == 1, "zip estimate after advance");
        check(split.tryAdvance(got::add) && split.estimateSize() == 0, "zip estimate after final pair");
        check(!split.tryAdvance(got::add) && !split.tryAdvance(got::add), "zip terminal state stable");
        expect(NullPointerException.class, () -> split.tryAdvance(null));
        check(got.equals(List.of(5, 7)), "zip spliterator values");
        result.close();
        Spliterator<Integer> unordered = Streams.zip(Stream.of(1).unordered(), Stream.of(2), Integer::sum).spliterator();
        check(!unordered.hasCharacteristics(Spliterator.ORDERED), "unordered source not upgraded");
        try (Stream<Long> huge = Streams.zip(LongStream.range(0, Long.MAX_VALUE).boxed(),
                LongStream.range(0, Long.MAX_VALUE).boxed(), Long::sum)) {
            Spliterator<Long> exactMaximum = huge.spliterator();
            check(exactMaximum.getExactSizeIfKnown() == Long.MAX_VALUE, "exact maximum size retained");
            check(exactMaximum.tryAdvance(x -> check(x == 0L, "huge zip first pair")), "huge zip advances");
            check(exactMaximum.getExactSizeIfKnown() == Long.MAX_VALUE - 1, "exact maximum size decremented");
        }
        Stream<Integer> unknown = Streams.stream(List.of(1, 2).iterator());
        Spliterator<Integer> notSized = Streams.zip(unknown, Stream.of(4, 5), Integer::sum).spliterator();
        check(notSized.getExactSizeIfKnown() == -1, "unknown zip size not invented");
        check(!notSized.hasCharacteristics(Spliterator.SUBSIZED), "unknown zip subsize not invented");
    }

    private static void zipOwnershipAndFailures() throws Exception {
        RuntimeException first = new RuntimeException("first");
        RuntimeException second = new RuntimeException("second");
        List<String> order = new ArrayList<>();
        Stream<Integer> a = Stream.of(1).onClose(() -> { order.add("first"); throw first; });
        Stream<Integer> b = Stream.of(2).onClose(() -> { order.add("second"); throw second; });
        Stream<Integer> zipped = Streams.zip(a, b, Integer::sum);
        check(expect(RuntimeException.class, zipped::close) == first, "first close exception identity");
        check(Arrays.equals(first.getSuppressed(), new Throwable[] {second}), "second close exception suppressed");
        check(order.equals(List.of("first", "second")), "close order");
        zipped.close();
        check(order.size() == 2, "repeated close remains idempotent");
        RuntimeException same = new RuntimeException("same throwable");
        Stream<Integer> sameFailure = Streams.zip(Stream.of(1).onClose(() -> { throw same; }),
                Stream.of(2).onClose(() -> { throw same; }), Integer::sum);
        check(expect(RuntimeException.class, sameFailure::close) == same, "self-suppression does not replace original failure");
        check(same.getSuppressed().length == 0, "self-suppression avoided by existing owner");
        RuntimeException mapping = new RuntimeException("mapping");
        try (Stream<Object> bad = Streams.zip(Stream.of(1), Stream.of(2), (x, y) -> { throw mapping; })) {
            check(expect(RuntimeException.class, bad::toList) == mapping, "zipper exception identity");
        }
        RuntimeException consuming = new RuntimeException("consumer");
        try (Stream<Integer> values = Streams.zip(Stream.of(1), Stream.of(2), Integer::sum)) {
            check(expect(RuntimeException.class, () -> values.spliterator().tryAdvance(x -> { throw consuming; })) == consuming,
                    "consumer exception identity");
        }
        Stream<Integer> sameInput = Stream.of(1, 2);
        expect(IllegalArgumentException.class, () -> Streams.zip(sameInput, sameInput, Integer::sum));
        check(sameInput.count() == 2, "same-reference refusal precedes consumption");
        Stream<Integer> nullFunctionSource = Stream.of(1);
        expect(NullPointerException.class, () -> Streams.zip(nullFunctionSource, Stream.of(2), null));
        check(nullFunctionSource.count() == 1, "zipper validation precedes linking");
        expect(NullPointerException.class, () -> Streams.zip(null, Stream.of(1), (x, y) -> y));
        expect(NullPointerException.class, () -> Streams.zip(Stream.of(1), null, (x, y) -> x));
        Stream<Integer> used = Stream.of(1);
        used.count();
        expect(IllegalStateException.class, () -> Streams.zip(used, Stream.of(2), Integer::sum));
    }

    private static void existingStreamOwners() {
        check(Arrays.equals(IntStream.range(Integer.MAX_VALUE - 3, Integer.MAX_VALUE).toArray(),
                new int[] {Integer.MAX_VALUE - 3, Integer.MAX_VALUE - 2, Integer.MAX_VALUE - 1}), "existing int range boundary");
        check(Arrays.equals(IntStream.rangeClosed(Integer.MIN_VALUE, Integer.MIN_VALUE + 2).toArray(),
                new int[] {Integer.MIN_VALUE, Integer.MIN_VALUE + 1, Integer.MIN_VALUE + 2}), "existing int closed range");
        check(Arrays.equals(LongStream.rangeClosed(Long.MAX_VALUE - 2, Long.MAX_VALUE).toArray(),
                new long[] {Long.MAX_VALUE - 2, Long.MAX_VALUE - 1, Long.MAX_VALUE}), "existing long closed range boundary");
        check(Stream.<String>builder().add("a").add("b").build().toList().equals(List.of("a", "b")), "existing reference builder");
        check(Arrays.equals(IntStream.builder().add(1).add(2).build().toArray(), new int[] {1, 2}), "existing int builder");
        check(Arrays.equals(LongStream.builder().add(1).add(2).build().toArray(), new long[] {1, 2}), "existing long builder");
        check(Arrays.equals(DoubleStream.builder().add(1).add(2).build().toArray(), new double[] {1, 2}), "existing double builder");
        check(Stream.concat(Stream.of("a"), Stream.of("b")).toList().equals(List.of("a", "b")), "existing reference concat");
        check(Arrays.equals(IntStream.concat(IntStream.of(1), IntStream.of(2)).toArray(), new int[] {1, 2}), "existing int concat");
        check(Arrays.equals(LongStream.concat(LongStream.of(1), LongStream.of(2)).toArray(), new long[] {1, 2}), "existing long concat");
        check(Arrays.equals(DoubleStream.concat(DoubleStream.of(1), DoubleStream.of(2)).toArray(), new double[] {1, 2}), "existing double concat");
    }

    private static final class CloseableIterator implements Iterator<Integer>, Closeable {
        final Iterator<Integer> source;
        int nextCalls;
        int hasNextCalls;
        int closed;
        CloseableIterator(Iterator<Integer> source) { this.source = source; }
        @Override public boolean hasNext() { hasNextCalls++; return source.hasNext(); }
        @Override public Integer next() { nextCalls++; return source.next(); }
        @Override public void close() { closed++; }
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static <T extends Throwable> T expect(Class<T> type, Action action) throws Exception {
        checks++;
        try { action.run(); }
        catch (Throwable actual) {
            if (type.isInstance(actual)) return type.cast(actual);
            throw new AssertionError("expected " + type + ", got " + actual, actual);
        }
        throw new AssertionError("expected " + type);
    }
    @FunctionalInterface private interface Action { void run() throws Exception; }
}
