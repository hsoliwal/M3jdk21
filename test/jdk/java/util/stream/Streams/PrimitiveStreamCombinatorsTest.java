/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Primitive stream combinators retain lazy serial demand, values, and close ownership
 * @modules java.base/java.util.stream:open
 *          java.base/java.util.function.ext
 * @run main PrimitiveStreamCombinatorsTest
 */

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Random;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.function.UnaryOperator;
import java.util.stream.BaseStream;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;
import java.util.stream.Streams;

public final class PrimitiveStreamCombinatorsTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        differentialValues();
        metadataAndParallelConstruction();
        primitiveTraversalDoesNotBox();
        demandAndTermination();
        doubleTotalOrderAndStableTies();
        argumentValidationAndReuse();
        callbackAndSourceFailures();
        closeOwnership();
        indexedOverflow();
        System.out.println("PrimitiveStreamCombinatorsTest checks=" + checks);
    }

    private static void differentialValues() {
        Random random = new Random(0x2026_1122_51EAL);
        double[] special = { Double.NEGATIVE_INFINITY, -9.5, -0.0, 0.0, 2.25,
                Double.POSITIVE_INFINITY, Double.NaN };
        for (int round = 0; round < 120; round++) {
            int n = round % 29, m = (round * 13) % 31;
            int[] a = new int[n], b = new int[m];
            long[] x = new long[n], y = new long[m];
            double[] p = new double[n], q = new double[m];
            for (int i = 0; i < n; i++) {
                a[i] = random.nextInt(31) - 15;
                x[i] = random.nextLong();
                p[i] = special[random.nextInt(special.length)];
            }
            for (int i = 0; i < m; i++) {
                b[i] = random.nextInt(31) - 15;
                y[i] = random.nextLong();
                q[i] = special[random.nextInt(special.length)];
            }
            int[] indexedInt = new int[n], pairInt = new int[Math.max(0, n - 1)];
            long[] indexedLong = new long[n], pairLong = new long[Math.max(0, n - 1)];
            double[] indexedDouble = new double[n], pairDouble = new double[Math.max(0, n - 1)];
            for (int i = 0; i < n; i++) {
                indexedInt[i] = a[i] + i * 13;
                indexedLong[i] = x[i] - i * 17L;
                indexedDouble[i] = p[i] + i * 0.25;
                if (i > 0) {
                    pairInt[i - 1] = a[i] - a[i - 1];
                    pairLong[i - 1] = x[i] - x[i - 1];
                    pairDouble[i - 1] = p[i] - p[i - 1];
                }
            }
            ints(Streams.mapIndexedInt(IntStream.of(a), (v, i) -> v + (int) i * 13).toArray(), indexedInt);
            longs(Streams.mapIndexedLong(LongStream.of(x), (v, i) -> v - i * 17).toArray(), indexedLong);
            doubles(Streams.mapIndexedDouble(DoubleStream.of(p), (v, i) -> v + i * 0.25).toArray(), indexedDouble);
            ints(Streams.pairwiseInt(IntStream.of(a), (l, r) -> r - l).toArray(), pairInt);
            longs(Streams.pairwiseLong(LongStream.of(x), (l, r) -> r - l).toArray(), pairLong);
            doubles(Streams.pairwiseDouble(DoubleStream.of(p), (l, r) -> r - l).toArray(), pairDouble);

            int pairs = Math.min(n, m);
            int[] zi = new int[pairs]; long[] zl = new long[pairs]; double[] zd = new double[pairs];
            for (int i = 0; i < pairs; i++) { zi[i] = a[i] + b[i]; zl[i] = x[i] + y[i]; zd[i] = p[i] + q[i]; }
            ints(Streams.zipInt(IntStream.of(a), IntStream.of(b), Integer::sum).toArray(), zi);
            longs(Streams.zipLong(LongStream.of(x), LongStream.of(y), Long::sum).toArray(), zl);
            doubles(Streams.zipDouble(DoubleStream.of(p), DoubleStream.of(q), Double::sum).toArray(), zd);

            int size = Math.max(0, 2 * n - 1);
            int[] si = new int[size]; long[] sl = new long[size]; double[] sd = new double[size];
            for (int i = 0; i < n; i++) {
                si[i * 2] = a[i]; sl[i * 2] = x[i]; sd[i * 2] = p[i];
                if (i + 1 < n) { si[i * 2 + 1] = Integer.MIN_VALUE; sl[i * 2 + 1] = Long.MAX_VALUE; sd[i * 2 + 1] = -0.0; }
            }
            ints(Streams.intersperseInt(IntStream.of(a), Integer.MIN_VALUE).toArray(), si);
            longs(Streams.intersperseLong(LongStream.of(x), Long.MAX_VALUE).toArray(), sl);
            doubles(Streams.intersperseDouble(DoubleStream.of(p), -0.0).toArray(), sd);

            int ni = 0, nl = 0, nd = 0;
            while (ni < n && a[ni] >= 0) ni++;
            while (nl < n && x[nl] >= 0) nl++;
            while (nd < n && p[nd] >= 0) nd++;
            ints(Streams.mapWhileInt(IntStream.of(a), v -> v >= 0 ? OptionalInt.of(v) : OptionalInt.empty()).toArray(), Arrays.copyOf(a, ni));
            longs(Streams.mapWhileLong(LongStream.of(x), v -> v >= 0 ? OptionalLong.of(v) : OptionalLong.empty()).toArray(), Arrays.copyOf(x, nl));
            doubles(Streams.mapWhileDouble(DoubleStream.of(p), v -> v >= 0 ? OptionalDouble.of(v) : OptionalDouble.empty()).toArray(), Arrays.copyOf(p, nd));

            int[] mi = new int[n + m]; long[] ml = new long[n + m]; double[] md = new double[n + m];
            System.arraycopy(a, 0, mi, 0, n); System.arraycopy(b, 0, mi, n, m);
            System.arraycopy(x, 0, ml, 0, n); System.arraycopy(y, 0, ml, n, m);
            System.arraycopy(p, 0, md, 0, n); System.arraycopy(q, 0, md, n, m);
            Arrays.sort(mi); Arrays.sort(ml); Arrays.sort(md);
            Arrays.sort(a); Arrays.sort(b); Arrays.sort(x); Arrays.sort(y); Arrays.sort(p); Arrays.sort(q);
            ints(Streams.mergeSortedInt(IntStream.of(a), IntStream.of(b)).toArray(), mi);
            longs(Streams.mergeSortedLong(LongStream.of(x), LongStream.of(y)).toArray(), ml);
            doubles(Streams.mergeSortedDouble(DoubleStream.of(p), DoubleStream.of(q)).toArray(), md);
        }
        ints(Streams.mapIndexedInt(IntStream.range(0, 8).filter(v -> v % 2 != 0), (v, i) -> (int) i).toArray(), 0, 1, 2, 3);
        longs(Streams.mapIndexedLong(LongStream.of(9, 7, 5).skip(1), (v, i) -> v + i).toArray(), 7, 6);
        doubles(Streams.mapIndexedDouble(DoubleStream.of(9, 8, 7).limit(2), (v, i) -> v - i).toArray(), 9, 7);
    }

    private static List<UnaryOperator<IntStream>> intOperations() {
        return List.of(s -> Streams.mapIndexedInt(s, (v, i) -> v),
                s -> Streams.pairwiseInt(s, (l, r) -> l),
                s -> Streams.zipInt(s, IntStream.of(1, 2, 3, 4), (l, r) -> l),
                s -> Streams.intersperseInt(s, 0), s -> Streams.mapWhileInt(s, OptionalInt::of),
                s -> Streams.mergeSortedInt(s, IntStream.empty()));
    }

    private static List<UnaryOperator<LongStream>> longOperations() {
        return List.of(s -> Streams.mapIndexedLong(s, (v, i) -> v),
                s -> Streams.pairwiseLong(s, (l, r) -> l),
                s -> Streams.zipLong(s, LongStream.of(1, 2, 3, 4), (l, r) -> l),
                s -> Streams.intersperseLong(s, 0), s -> Streams.mapWhileLong(s, OptionalLong::of),
                s -> Streams.mergeSortedLong(s, LongStream.empty()));
    }

    private static List<UnaryOperator<DoubleStream>> doubleOperations() {
        return List.of(s -> Streams.mapIndexedDouble(s, (v, i) -> v),
                s -> Streams.pairwiseDouble(s, (l, r) -> l),
                s -> Streams.zipDouble(s, DoubleStream.of(1, 2, 3, 4), (l, r) -> l),
                s -> Streams.intersperseDouble(s, 0), s -> Streams.mapWhileDouble(s, OptionalDouble::of),
                s -> Streams.mergeSortedDouble(s, DoubleStream.empty()));
    }

    private static void metadataAndParallelConstruction() {
        for (int op = 0; op < 6; op++) {
            for (int kind = 0; kind < 3; kind++) {
                AtomicInteger ni = new AtomicInteger(), nl = new AtomicInteger(), nd = new AtomicInteger();
                IntStream si = IntStream.of(1, 2, 2, 3).parallel().peek(v -> ni.incrementAndGet());
                LongStream sl = LongStream.of(1, 2, 2, 3).parallel().peek(v -> nl.incrementAndGet());
                DoubleStream sd = DoubleStream.of(1, 2, 2, 3).parallel().peek(v -> nd.incrementAndGet());
                if (kind == 0) { si = si.sorted(); sl = sl.sorted(); sd = sd.sorted(); }
                else if (kind == 1) { si = si.distinct(); sl = sl.distinct(); sd = sd.distinct(); }
                else { si = si.takeWhile(v -> v < 4); sl = sl.takeWhile(v -> v < 4); sd = sd.takeWhile(v -> v < 4); }
                IntStream ri = intOperations().get(op).apply(si);
                LongStream rl = longOperations().get(op).apply(sl);
                DoubleStream rd = doubleOperations().get(op).apply(sd);
                check(ni.get() == 0 && nl.get() == 0 && nd.get() == 0, "parallel stateful construction traversed");
                metadata(ri, true, op == 5); metadata(rl, true, op == 5); metadata(rd, true, op == 5);
            }
            metadata(intOperations().get(op).apply(IntStream.of(1, 2).unordered()), op == 5, op == 5);
            metadata(longOperations().get(op).apply(LongStream.of(1, 2).unordered()), op == 5, op == 5);
            metadata(doubleOperations().get(op).apply(DoubleStream.of(1, 2).unordered()), op == 5, op == 5);
        }
        AtomicInteger first = new AtomicInteger(), second = new AtomicInteger();
        IntStream zip = Streams.zipInt(IntStream.of(1, 2).parallel().peek(v -> first.incrementAndGet()).sorted(),
                IntStream.of(3, 4).parallel().peek(v -> second.incrementAndGet()).distinct(), Integer::sum);
        check(first.get() == 0 && second.get() == 0, "zip acquired and traversed a stateful input");
        ints(zip.toArray(), 4, 6);
        metadata(Streams.zipLong(LongStream.of(1), LongStream.of(2).unordered(), Long::sum), false, false);
        metadata(Streams.zipDouble(DoubleStream.of(1).unordered(), DoubleStream.of(2), Double::sum), false, false);
    }

    private static void metadata(BaseStream<?, ?> stream, boolean ordered, boolean sorted) {
        check(!stream.isParallel(), "result was initially parallel");
        Spliterator<?> spliterator = stream.parallel().spliterator();
        check(spliterator.trySplit() == null, "primitive source split");
        check(spliterator.estimateSize() == Long.MAX_VALUE, "eager exact-size claim");
        check(spliterator.getExactSizeIfKnown() == -1, "exact size advertised");
        int flags = spliterator.characteristics();
        check((flags & (Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.DISTINCT)) == 0, "unsafe metadata");
        check((flags & Spliterator.NONNULL) != 0, "primitive values lost NONNULL");
        check(((flags & Spliterator.ORDERED) != 0) == ordered, "ORDERED mismatch");
        check(((flags & Spliterator.SORTED) != 0) == sorted, "SORTED mismatch");
        if (sorted) check(spliterator.getComparator() == null, "primitive natural comparator");
        else expect(IllegalStateException.class, spliterator::getComparator);
        drain(spliterator);
        check(spliterator.estimateSize() == 0, "exhausted estimate");
        expect(NullPointerException.class, () -> spliterator.tryAdvance(null));
        stream.close();
    }

    private static void primitiveTraversalDoesNotBox() {
        for (UnaryOperator<IntStream> op : intOperations()) op.apply(StreamSupport.intStream(new IntOnly(), false)).toArray();
        for (UnaryOperator<LongStream> op : longOperations()) op.apply(StreamSupport.longStream(new LongOnly(), false)).toArray();
        for (UnaryOperator<DoubleStream> op : doubleOperations()) op.apply(StreamSupport.doubleStream(new DoubleOnly(), false)).toArray();
        checks += 18;
    }

    private static final class IntOnly implements Spliterator.OfInt {
        private int value;
        @Override public boolean tryAdvance(IntConsumer action) { if (value == 4) return false; action.accept(value++); return true; }
        @Override public boolean tryAdvance(Consumer<? super Integer> action) { throw new AssertionError("boxed int input"); }
        @Override public void forEachRemaining(Consumer<? super Integer> action) { throw new AssertionError("boxed int bulk input"); }
        @Override public OfInt trySplit() { return null; }
        @Override public long estimateSize() { return Long.MAX_VALUE; }
        @Override public int characteristics() { return ORDERED; }
    }

    private static final class LongOnly implements Spliterator.OfLong {
        private long value;
        @Override public boolean tryAdvance(LongConsumer action) { if (value == 4) return false; action.accept(value++); return true; }
        @Override public boolean tryAdvance(Consumer<? super Long> action) { throw new AssertionError("boxed long input"); }
        @Override public void forEachRemaining(Consumer<? super Long> action) { throw new AssertionError("boxed long bulk input"); }
        @Override public OfLong trySplit() { return null; }
        @Override public long estimateSize() { return Long.MAX_VALUE; }
        @Override public int characteristics() { return ORDERED; }
    }

    private static final class DoubleOnly implements Spliterator.OfDouble {
        private int value;
        @Override public boolean tryAdvance(DoubleConsumer action) { if (value == 4) return false; action.accept(value++); return true; }
        @Override public boolean tryAdvance(Consumer<? super Double> action) { throw new AssertionError("boxed double input"); }
        @Override public void forEachRemaining(Consumer<? super Double> action) { throw new AssertionError("boxed double bulk input"); }
        @Override public OfDouble trySplit() { return null; }
        @Override public long estimateSize() { return Long.MAX_VALUE; }
        @Override public int characteristics() { return ORDERED; }
    }

    private static void demandAndTermination() {
        for (int op = 0; op < 6; op++) {
            AtomicInteger ni = new AtomicInteger(), nl = new AtomicInteger(), nd = new AtomicInteger();
            intOperations().get(op).apply(IntStream.iterate(0, v -> v + 1).peek(v -> ni.incrementAndGet())).limit(0).toArray();
            longOperations().get(op).apply(LongStream.iterate(0, v -> v + 1).peek(v -> nl.incrementAndGet())).limit(0).toArray();
            doubleOperations().get(op).apply(DoubleStream.iterate(0, v -> v + 1).peek(v -> nd.incrementAndGet())).limit(0).toArray();
            check(ni.get() == 0 && nl.get() == 0 && nd.get() == 0, "zero demand traversed input");
            int[] ai = intOperations().get(op).apply(IntStream.iterate(0, v -> v + 1).peek(v -> ni.incrementAndGet())).limit(3).toArray();
            long[] al = longOperations().get(op).apply(LongStream.iterate(0, v -> v + 1).peek(v -> nl.incrementAndGet())).limit(3).toArray();
            double[] ad = doubleOperations().get(op).apply(DoubleStream.iterate(0, v -> v + 1).peek(v -> nd.incrementAndGet())).limit(3).toArray();
            check(ai.length == 3 && al.length == 3 && ad.length == 3, "finite demand on infinite source");
            check(ni.get() <= 4 && nl.get() <= 4 && nd.get() <= 4, "demand exceeded constant lookahead");
        }
        AtomicInteger seen = new AtomicInteger();
        Spliterator.OfInt si = Streams.intersperseInt(IntStream.of(4, 8).peek(v -> seen.incrementAndGet()), -1).spliterator();
        check(si.tryAdvance((int v) -> check(v == 4, "first interspersed value")) && seen.get() == 1, "first value lookahead");
        check(si.tryAdvance((int v) -> check(v == -1, "separator")) && seen.get() == 2, "separator lookahead");
        check(si.tryAdvance((int v) -> check(v == 8, "pending value")) && seen.get() == 2, "pending value reread");
        check(!si.tryAdvance((int v) -> { throw new AssertionError("trailing separator"); }), "trailing element");

        for (int boundary : new int[] { 0, 2 }) {
            AtomicInteger ni = new AtomicInteger(), nl = new AtomicInteger(), nd = new AtomicInteger();
            AtomicInteger mi = new AtomicInteger(), ml = new AtomicInteger(), md = new AtomicInteger();
            Spliterator.OfInt pi = Streams.mapWhileInt(IntStream.range(0, 6).peek(v -> ni.incrementAndGet()),
                    v -> { mi.incrementAndGet(); return v < boundary ? OptionalInt.of(v) : OptionalInt.empty(); }).spliterator();
            Spliterator.OfLong pl = Streams.mapWhileLong(LongStream.range(0, 6).peek(v -> nl.incrementAndGet()),
                    v -> { ml.incrementAndGet(); return v < boundary ? OptionalLong.of(v) : OptionalLong.empty(); }).spliterator();
            Spliterator.OfDouble pd = Streams.mapWhileDouble(DoubleStream.of(0, 1, 2, 3, 4, 5).peek(v -> nd.incrementAndGet()),
                    v -> { md.incrementAndGet(); return v < boundary ? OptionalDouble.of(v) : OptionalDouble.empty(); }).spliterator();
            for (int i = 0; i < boundary; i++) {
                final int expected = i;
                check(pi.tryAdvance((int v) -> check(v == expected, "int mapped prefix")), "short int prefix");
                check(pl.tryAdvance((long v) -> check(v == expected, "long mapped prefix")), "short long prefix");
                check(pd.tryAdvance((double v) -> check(v == expected, "double mapped prefix")), "short double prefix");
            }
            for (int i = 0; i < 3; i++) {
                check(!pi.tryAdvance((int v) -> { throw new AssertionError("int stop latch"); }), "int restarted");
                check(!pl.tryAdvance((long v) -> { throw new AssertionError("long stop latch"); }), "long restarted");
                check(!pd.tryAdvance((double v) -> { throw new AssertionError("double stop latch"); }), "double restarted");
            }
            check(ni.get() == boundary + 1 && nl.get() == boundary + 1 && nd.get() == boundary + 1, "rejecting element consumption");
            check(mi.get() == boundary + 1 && ml.get() == boundary + 1 && md.get() == boundary + 1, "mapper called after stop");
        }
        expect(NullPointerException.class, () -> Streams.mapWhileInt(IntStream.of(1), v -> null).toArray());
        expect(NullPointerException.class, () -> Streams.mapWhileLong(LongStream.of(1), v -> null).toArray());
        expect(NullPointerException.class, () -> Streams.mapWhileDouble(DoubleStream.of(1), v -> null).toArray());
    }

    private static void doubleTotalOrderAndStableTies() {
        double leftNaN1 = Double.longBitsToDouble(0x7ff8000000000001L);
        double leftNaN2 = Double.longBitsToDouble(0xfff8000000000002L);
        double rightNaN = Double.longBitsToDouble(0x7ff8000000000003L);
        double[] left = { Double.NEGATIVE_INFINITY, -0.0, +0.0, Double.POSITIVE_INFINITY, leftNaN1, leftNaN2 };
        double[] right = { Double.NEGATIVE_INFINITY, -0.0, +0.0, Double.POSITIVE_INFINITY, rightNaN };
        double[] result = Streams.mergeSortedDouble(DoubleStream.of(left), DoubleStream.of(right)).toArray();
        double[] expected = { Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, -0.0, -0.0, +0.0, +0.0,
                Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, leftNaN1, leftNaN2, rightNaN };
        check(result.length == expected.length, "total-order merge length");
        for (int i = 0; i < expected.length; i++)
            check(Double.doubleToRawLongBits(result[i]) == Double.doubleToRawLongBits(expected[i]), "raw double order at " + i);
        ints(Streams.mergeSortedInt(IntStream.of(Integer.MIN_VALUE, Integer.MAX_VALUE), IntStream.of(-1, 0, 1)).toArray(),
                Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE);
        longs(Streams.mergeSortedLong(LongStream.of(Long.MIN_VALUE, Long.MAX_VALUE), LongStream.of(-1, 0, 1)).toArray(),
                Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE);
        check(Double.doubleToRawLongBits(Streams.mapIndexedDouble(DoubleStream.of(leftNaN2), (v, i) -> v).findFirst().orElseThrow())
                == Double.doubleToRawLongBits(leftNaN2), "indexed double payload");
        check(Double.doubleToRawLongBits(Streams.mapWhileDouble(DoubleStream.of(leftNaN1), OptionalDouble::of).findFirst().orElseThrow())
                == Double.doubleToRawLongBits(leftNaN1), "optional double payload");
    }

    private static List<BiFunction<IntStream, IntStream, IntStream>> binaryIntOperations() {
        return List.of((l, r) -> Streams.zipInt(l, r, Integer::sum), Streams::mergeSortedInt);
    }

    private static List<BiFunction<LongStream, LongStream, LongStream>> binaryLongOperations() {
        return List.of((l, r) -> Streams.zipLong(l, r, Long::sum), Streams::mergeSortedLong);
    }

    private static List<BiFunction<DoubleStream, DoubleStream, DoubleStream>> binaryDoubleOperations() {
        return List.of((l, r) -> Streams.zipDouble(l, r, Double::sum), Streams::mergeSortedDouble);
    }

    private static void argumentValidationAndReuse() {
        for (UnaryOperator<IntStream> op : intOperations()) {
            expect(NullPointerException.class, () -> op.apply(null));
            IntStream input = IntStream.of(1, 2, 3); IntStream result = op.apply(input);
            expect(IllegalStateException.class, input::spliterator); result.close();
            IntStream closed = IntStream.of(1); closed.close();
            expect(IllegalStateException.class, () -> op.apply(closed));
        }
        for (UnaryOperator<LongStream> op : longOperations()) {
            expect(NullPointerException.class, () -> op.apply(null));
            LongStream input = LongStream.of(1, 2, 3); LongStream result = op.apply(input);
            expect(IllegalStateException.class, input::spliterator); result.close();
            LongStream closed = LongStream.of(1); closed.close();
            expect(IllegalStateException.class, () -> op.apply(closed));
        }
        for (UnaryOperator<DoubleStream> op : doubleOperations()) {
            expect(NullPointerException.class, () -> op.apply(null));
            DoubleStream input = DoubleStream.of(1, 2, 3); DoubleStream result = op.apply(input);
            expect(IllegalStateException.class, input::spliterator); result.close();
            DoubleStream closed = DoubleStream.of(1); closed.close();
            expect(IllegalStateException.class, () -> op.apply(closed));
        }
        List<UnaryOperator<IntStream>> invalidInt = List.of(s -> Streams.mapIndexedInt(s, null),
                s -> Streams.pairwiseInt(s, null), s -> Streams.zipInt(s, IntStream.empty(), null), s -> Streams.mapWhileInt(s, null));
        List<UnaryOperator<LongStream>> invalidLong = List.of(s -> Streams.mapIndexedLong(s, null),
                s -> Streams.pairwiseLong(s, null), s -> Streams.zipLong(s, LongStream.empty(), null), s -> Streams.mapWhileLong(s, null));
        List<UnaryOperator<DoubleStream>> invalidDouble = List.of(s -> Streams.mapIndexedDouble(s, null),
                s -> Streams.pairwiseDouble(s, null), s -> Streams.zipDouble(s, DoubleStream.empty(), null), s -> Streams.mapWhileDouble(s, null));
        for (UnaryOperator<IntStream> op : invalidInt) {
            IntStream input = IntStream.of(1, 2).parallel();
            expect(NullPointerException.class, () -> op.apply(input));
            check(input.isParallel(), "invalid mapper changed source mode"); ints(input.toArray(), 1, 2);
        }
        for (UnaryOperator<LongStream> op : invalidLong) {
            LongStream input = LongStream.of(1, 2).parallel();
            expect(NullPointerException.class, () -> op.apply(input));
            check(input.isParallel(), "invalid mapper changed source mode"); longs(input.toArray(), 1, 2);
        }
        for (UnaryOperator<DoubleStream> op : invalidDouble) {
            DoubleStream input = DoubleStream.of(1, 2).parallel();
            expect(NullPointerException.class, () -> op.apply(input));
            check(input.isParallel(), "invalid mapper changed source mode"); doubles(input.toArray(), 1, 2);
        }
        for (BiFunction<IntStream, IntStream, IntStream> op : binaryIntOperations()) {
            IntStream input = IntStream.of(1, 2).parallel();
            expect(IllegalArgumentException.class, () -> op.apply(input, input));
            check(input.isParallel(), "same reference changed source mode"); ints(input.toArray(), 1, 2);
            IntStream other = IntStream.of(3);
            expect(NullPointerException.class, () -> op.apply(other, null)); ints(other.toArray(), 3);
            IntStream used = IntStream.of(1); used.toArray();
            expect(IllegalStateException.class, () -> op.apply(IntStream.of(2), used));
        }
        for (BiFunction<LongStream, LongStream, LongStream> op : binaryLongOperations()) {
            LongStream input = LongStream.of(1, 2).parallel();
            expect(IllegalArgumentException.class, () -> op.apply(input, input));
            check(input.isParallel(), "same reference changed source mode"); longs(input.toArray(), 1, 2);
            LongStream other = LongStream.of(3);
            expect(NullPointerException.class, () -> op.apply(other, null)); longs(other.toArray(), 3);
            LongStream used = LongStream.of(1); used.toArray();
            expect(IllegalStateException.class, () -> op.apply(LongStream.of(2), used));
        }
        for (BiFunction<DoubleStream, DoubleStream, DoubleStream> op : binaryDoubleOperations()) {
            DoubleStream input = DoubleStream.of(1, 2).parallel();
            expect(IllegalArgumentException.class, () -> op.apply(input, input));
            check(input.isParallel(), "same reference changed source mode"); doubles(input.toArray(), 1, 2);
            DoubleStream other = DoubleStream.of(3);
            expect(NullPointerException.class, () -> op.apply(other, null)); doubles(other.toArray(), 3);
            DoubleStream used = DoubleStream.of(1); used.toArray();
            expect(IllegalStateException.class, () -> op.apply(DoubleStream.of(2), used));
        }
    }

    private static void callbackAndSourceFailures() {
        RuntimeException failure = new IllegalStateException("callback sentinel");
        AtomicInteger closed = new AtomicInteger();
        List<BaseStream<?, ?>> mapped = List.of(
                Streams.mapIndexedInt(IntStream.of(1, 2).onClose(closed::incrementAndGet), (v, i) -> { throw failure; }),
                Streams.mapIndexedLong(LongStream.of(1, 2).onClose(closed::incrementAndGet), (v, i) -> { throw failure; }),
                Streams.mapIndexedDouble(DoubleStream.of(1, 2).onClose(closed::incrementAndGet), (v, i) -> { throw failure; }),
                Streams.pairwiseInt(IntStream.of(1, 2).onClose(closed::incrementAndGet), (l, r) -> { throw failure; }),
                Streams.pairwiseLong(LongStream.of(1, 2).onClose(closed::incrementAndGet), (l, r) -> { throw failure; }),
                Streams.pairwiseDouble(DoubleStream.of(1, 2).onClose(closed::incrementAndGet), (l, r) -> { throw failure; }),
                Streams.zipInt(IntStream.of(1, 2).onClose(closed::incrementAndGet), IntStream.of(1), (l, r) -> { throw failure; }),
                Streams.zipLong(LongStream.of(1, 2).onClose(closed::incrementAndGet), LongStream.of(1), (l, r) -> { throw failure; }),
                Streams.zipDouble(DoubleStream.of(1, 2).onClose(closed::incrementAndGet), DoubleStream.of(1), (l, r) -> { throw failure; }),
                Streams.mapWhileInt(IntStream.of(1, 2).onClose(closed::incrementAndGet), v -> { throw failure; }),
                Streams.mapWhileLong(LongStream.of(1, 2).onClose(closed::incrementAndGet), v -> { throw failure; }),
                Streams.mapWhileDouble(DoubleStream.of(1, 2).onClose(closed::incrementAndGet), v -> { throw failure; }));
        int completed = 0;
        for (BaseStream<?, ?> result : mapped) {
            expectSame(failure, () -> consume(result));
            check(closed.get() == completed, "callback failure closed source");
            result.close(); check(closed.get() == ++completed, "failed result lost close ownership");
        }
        for (int op = 0; op < 6; op++) {
            AtomicInteger ci = new AtomicInteger(), cl = new AtomicInteger(), cd = new AtomicInteger();
            IntStream ri = intOperations().get(op).apply(IntStream.of(1, 2).map(v -> { throw failure; }).onClose(ci::incrementAndGet));
            LongStream rl = longOperations().get(op).apply(LongStream.of(1, 2).map(v -> { throw failure; }).onClose(cl::incrementAndGet));
            DoubleStream rd = doubleOperations().get(op).apply(DoubleStream.of(1, 2).map(v -> { throw failure; }).onClose(cd::incrementAndGet));
            expectSame(failure, () -> consume(ri)); expectSame(failure, () -> consume(rl)); expectSame(failure, () -> consume(rd));
            check(ci.get() == 0 && cl.get() == 0 && cd.get() == 0, "source failure closed an input");
            ri.close(); rl.close(); rd.close();
            check(ci.get() == 1 && cl.get() == 1 && cd.get() == 1, "source failure lost ownership");

            IntStream ai = intOperations().get(op).apply(IntStream.of(1, 2));
            LongStream al = longOperations().get(op).apply(LongStream.of(1, 2));
            DoubleStream ad = doubleOperations().get(op).apply(DoubleStream.of(1, 2));
            expectSame(failure, () -> ai.forEach(v -> { throw failure; }));
            expectSame(failure, () -> al.forEach(v -> { throw failure; }));
            expectSame(failure, () -> ad.forEach(v -> { throw failure; }));
            ai.close(); al.close(); ad.close();
        }
    }

    private static void closeOwnership() {
        for (int op = 0; op < 6; op++) {
            for (boolean exhaust : new boolean[] { false, true }) {
                AtomicInteger ni = new AtomicInteger(), nl = new AtomicInteger(), nd = new AtomicInteger();
                IntStream ri = intOperations().get(op).apply(IntStream.of(1, 2, 3).onClose(ni::incrementAndGet));
                LongStream rl = longOperations().get(op).apply(LongStream.of(1, 2, 3).onClose(nl::incrementAndGet));
                DoubleStream rd = doubleOperations().get(op).apply(DoubleStream.of(1, 2, 3).onClose(nd::incrementAndGet));
                if (exhaust) { consume(ri); consume(rl); consume(rd); }
                check(ni.get() == 0 && nl.get() == 0 && nd.get() == 0, "construction or exhaustion closed input");
                ri.close(); rl.close(); rd.close(); ri.close(); rl.close(); rd.close();
                check(ni.get() == 1 && nl.get() == 1 && nd.get() == 1, "close count");
            }
        }
        for (BiFunction<IntStream, IntStream, IntStream> op : binaryIntOperations()) {
            List<String> order = new ArrayList<>();
            RuntimeException left = new IllegalArgumentException("left close"), right = new IllegalStateException("right close");
            IntStream result = op.apply(IntStream.of(1).onClose(() -> { order.add("left"); throw left; }),
                    IntStream.of(2).onClose(() -> { order.add("right"); throw right; }));
            closeFailures(result, order, left, right);
            AtomicInteger first = new AtomicInteger(), second = new AtomicInteger();
            IntStream unused = IntStream.of(1).onClose(first::incrementAndGet);
            IntStream unavailable = IntStream.of(2).onClose(second::incrementAndGet); unavailable.close();
            expect(IllegalStateException.class, () -> op.apply(unused, unavailable));
            check(first.get() == 0 && second.get() == 1, "failed construction took close ownership");
            unused.close(); check(first.get() == 1, "caller could not close first input");
        }
        for (BiFunction<LongStream, LongStream, LongStream> op : binaryLongOperations()) {
            List<String> order = new ArrayList<>();
            RuntimeException left = new IllegalArgumentException("left close"), right = new IllegalStateException("right close");
            LongStream result = op.apply(LongStream.of(1).onClose(() -> { order.add("left"); throw left; }),
                    LongStream.of(2).onClose(() -> { order.add("right"); throw right; }));
            closeFailures(result, order, left, right);
        }
        for (BiFunction<DoubleStream, DoubleStream, DoubleStream> op : binaryDoubleOperations()) {
            List<String> order = new ArrayList<>();
            RuntimeException left = new IllegalArgumentException("left close"), right = new IllegalStateException("right close");
            DoubleStream result = op.apply(DoubleStream.of(1).onClose(() -> { order.add("left"); throw left; }),
                    DoubleStream.of(2).onClose(() -> { order.add("right"); throw right; }));
            closeFailures(result, order, left, right);
        }
    }

    private static void closeFailures(BaseStream<?, ?> result, List<String> order,
            RuntimeException first, RuntimeException second) {
        expectSame(first, result::close);
        check(order.equals(List.of("left", "right")), "binary close order");
        check(first.getSuppressed().length == 1 && first.getSuppressed()[0] == second, "close suppression");
        result.close(); check(order.size() == 2, "close handlers repeated");
    }

    private static void indexedOverflow() throws Exception {
        AtomicInteger ci = new AtomicInteger(), cl = new AtomicInteger(), cd = new AtomicInteger();
        long[] oi = new long[1], ol = new long[1], od = new long[1];
        Spliterator.OfInt pi = Streams.mapIndexedInt(IntStream.of(7, 8), (v, i) -> { ci.incrementAndGet(); oi[0] = i; return v; }).spliterator();
        Spliterator.OfLong pl = Streams.mapIndexedLong(LongStream.of(7, 8), (v, i) -> { cl.incrementAndGet(); ol[0] = i; return v; }).spliterator();
        Spliterator.OfDouble pd = Streams.mapIndexedDouble(DoubleStream.of(7, 8), (v, i) -> { cd.incrementAndGet(); od[0] = i; return v; }).spliterator();
        setLastIndex(pi); setLastIndex(pl); setLastIndex(pd);
        check(pi.tryAdvance((int v) -> check(v == 7, "MAX int value")), "MAX int missing");
        check(pl.tryAdvance((long v) -> check(v == 7, "MAX long value")), "MAX long missing");
        check(pd.tryAdvance((double v) -> check(v == 7, "MAX double value")), "MAX double missing");
        check(oi[0] == Long.MAX_VALUE && ol[0] == Long.MAX_VALUE && od[0] == Long.MAX_VALUE, "long index truncated or wrapped");
        expect(ArithmeticException.class, () -> pi.tryAdvance((int v) -> { throw new AssertionError("overflow int output"); }));
        expect(ArithmeticException.class, () -> pl.tryAdvance((long v) -> { throw new AssertionError("overflow long output"); }));
        expect(ArithmeticException.class, () -> pd.tryAdvance((double v) -> { throw new AssertionError("overflow double output"); }));
        check(ci.get() == 1 && cl.get() == 1 && cd.get() == 1, "overflow mapper invoked");

        Spliterator.OfInt ei = Streams.mapIndexedInt(IntStream.of(1), (v, i) -> v).spliterator();
        Spliterator.OfLong el = Streams.mapIndexedLong(LongStream.of(1), (v, i) -> v).spliterator();
        Spliterator.OfDouble ed = Streams.mapIndexedDouble(DoubleStream.of(1), (v, i) -> v).spliterator();
        setLastIndex(ei); setLastIndex(el); setLastIndex(ed);
        check(ei.tryAdvance((int v) -> {}) && !ei.tryAdvance((int v) -> {}), "MAX int terminal exhaustion");
        check(el.tryAdvance((long v) -> {}) && !el.tryAdvance((long v) -> {}), "MAX long terminal exhaustion");
        check(ed.tryAdvance((double v) -> {}) && !ed.tryAdvance((double v) -> {}), "MAX double terminal exhaustion");
    }

    private static void setLastIndex(Spliterator<?> input) throws Exception {
        Field index = input.getClass().getDeclaredField("index");
        index.setAccessible(true); index.setLong(input, Long.MAX_VALUE);
    }

    private static void consume(BaseStream<?, ?> stream) {
        if (stream instanceof IntStream ints) ints.forEach((int v) -> {});
        else if (stream instanceof LongStream longs) longs.forEach((long v) -> {});
        else if (stream instanceof DoubleStream doubles) doubles.forEach((double v) -> {});
        else throw new AssertionError("not a primitive stream");
    }

    private static void drain(Spliterator<?> input) {
        if (input instanceof Spliterator.OfInt ints) ints.forEachRemaining((int v) -> {});
        else if (input instanceof Spliterator.OfLong longs) longs.forEachRemaining((long v) -> {});
        else if (input instanceof Spliterator.OfDouble doubles) doubles.forEachRemaining((double v) -> {});
        else throw new AssertionError("not a primitive spliterator");
    }

    private static void expectSame(Throwable expected, Runnable body) {
        check(expect(expected.getClass(), body) == expected, "exception identity changed");
    }

    private static Throwable expect(Class<? extends Throwable> type, Runnable body) {
        try { body.run(); }
        catch (Throwable failure) {
            if (!type.isInstance(failure)) throw new AssertionError("Expected " + type.getName(), failure);
            checks++; return failure;
        }
        throw new AssertionError("Missing " + type.getName());
    }

    private static void ints(int[] actual, int... expected) {
        check(Arrays.equals(actual, expected), "int values: " + Arrays.toString(actual) + " expected " + Arrays.toString(expected));
    }

    private static void longs(long[] actual, long... expected) {
        check(Arrays.equals(actual, expected), "long values: " + Arrays.toString(actual) + " expected " + Arrays.toString(expected));
    }

    private static void doubles(double[] actual, double... expected) {
        check(Arrays.equals(actual, expected), "double values: " + Arrays.toString(actual) + " expected " + Arrays.toString(expected));
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
