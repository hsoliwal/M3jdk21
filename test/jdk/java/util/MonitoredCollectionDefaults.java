/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 *
 * @test
 * @summary Seventeen monitored JDK collection defaults and dual-interface compatibility
 * @run main/othervm MonitoredCollectionDefaults
 */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ProgressMonitor;
import java.util.function.Consumer;

public class MonitoredCollectionDefaults {
    private static int assertions;

    private static void check(boolean ok, String message) {
        assertions++;
        if (!ok) throw new AssertionError(message);
    }

    private static class Counting implements ProgressMonitor {
        long completed;
        long cancelAt = Long.MAX_VALUE;
        int done;
        @Override public void worked(long units) {
            if (units != 1) throw new AssertionError("element work is not one");
            completed += units;
        }
        @Override public boolean isCancelled() { return completed >= cancelAt; }
        @Override public void done() { done++; }
    }

    private static void cancel(Runnable operation) {
        boolean cancelled = false;
        try {
            operation.run();
        } catch (CancellationException expected) {
            cancelled = true;
        }
        check(cancelled, "cancellation not observed");
    }

    /** The old one-argument default already conflicts for a dual-role class. */
    private static final class Dual implements Iterator<Integer>, Spliterator<Integer> {
        int index;
        @Override public boolean hasNext() { return index < 3; }
        @Override public Integer next() { return ++index; }
        @Override public boolean tryAdvance(Consumer<? super Integer> action) {
            if (index == 3) return false;
            action.accept(++index);
            return true;
        }
        @Override public Spliterator<Integer> trySplit() { return null; }
        @Override public long estimateSize() { return 3 - index; }
        @Override public int characteristics() { return ORDERED; }
        @Override public void forEachRemaining(Consumer<? super Integer> action) {
            while (hasNext()) action.accept(next());
        }
    }

    private static PrimitiveIterator.OfInt ints() {
        return new PrimitiveIterator.OfInt() {
            int i;
            @Override public boolean hasNext() { return i < 3; }
            @Override public int nextInt() { return ++i; }
        };
    }

    private static PrimitiveIterator.OfLong longs() {
        return new PrimitiveIterator.OfLong() {
            int i;
            @Override public boolean hasNext() { return i < 3; }
            @Override public long nextLong() { return ++i; }
        };
    }

    private static PrimitiveIterator.OfDouble doubles() {
        return new PrimitiveIterator.OfDouble() {
            int i;
            @Override public boolean hasNext() { return i < 3; }
            @Override public double nextDouble() { return ++i; }
        };
    }

    public static void main(String[] ignored) {
        Counting iterable = new Counting();
        int[] sum = { 0 };
        Iterable<Integer> sequence = List.of(1, 2, 3);
        sequence.forEachMonitored(x -> sum[0] += x, iterable);
        check(sum[0] == 6 && iterable.completed == 3, "Iterable default");

        Counting iterator = new Counting();
        List.of(1, 2, 3).iterator().forEachRemainingMonitored(
                x -> sum[0] += x, iterator);
        check(sum[0] == 12 && iterator.completed == 3, "Iterator default");

        Counting pi = new Counting();
        int[] iSum = { 0 };
        ints().forEachRemainingIntMonitored((int x) -> iSum[0] += x, pi);
        check(iSum[0] == 6 && pi.completed == 3, "primitive iterator int");

        Counting pl = new Counting();
        long[] lSum = { 0 };
        longs().forEachRemainingLongMonitored((long x) -> lSum[0] += x, pl);
        check(lSum[0] == 6 && pl.completed == 3, "primitive iterator long");

        Counting pd = new Counting();
        double[] dSum = { 0 };
        doubles().forEachRemainingDoubleMonitored((double x) -> dSum[0] += x, pd);
        check(dSum[0] == 6 && pd.completed == 3, "primitive iterator double");

        Counting split = new Counting();
        int[] sSum = { 0 };
        List.of(1, 2, 3).spliterator().forEachRemainingSpliteratorMonitored(
                x -> sSum[0] += x, split);
        check(sSum[0] == 6 && split.completed == 3, "generic spliterator");

        Counting spi = new Counting();
        Spliterators.spliterator(new int[] { 1, 2, 3 }, Spliterator.ORDERED)
                .forEachRemainingSpliteratorIntMonitored((int x) -> sSum[0] += x, spi);
        check(sSum[0] == 12 && spi.completed == 3, "spliterator int");

        Counting spl = new Counting();
        Spliterators.spliterator(new long[] { 1, 2, 3 }, Spliterator.ORDERED)
                .forEachRemainingSpliteratorLongMonitored((long x) -> lSum[0] += x, spl);
        check(lSum[0] == 12 && spl.completed == 3, "spliterator long");

        Counting spd = new Counting();
        Spliterators.spliterator(new double[] { 1, 2, 3 }, Spliterator.ORDERED)
                .forEachRemainingSpliteratorDoubleMonitored((double x) -> dSum[0] += x, spd);
        check(dSum[0] == 12 && spd.completed == 3, "spliterator double");

        Collection<Integer> values = new ArrayList<>(List.of(1, 2, 3));
        Counting all = new Counting();
        check(values.containsAllMonitored(List.of(1, 3), all), "containsAll true");
        check(all.completed == 2, "containsAll credit");

        Counting remove = new Counting();
        check(values.removeIfMonitored(x -> x == 2, remove), "removeIf changed");
        check(values.equals(List.of(1, 3)) && remove.completed == 3, "removeIf result");

        Counting add = new Counting();
        check(values.addAllMonitored(List.of(4, 5), add), "addAll changed");
        check(values.equals(List.of(1, 3, 4, 5)) && add.completed == 2, "addAll result");

        List<Integer> repeated = List.of(1, 2, 3, 2);
        Counting index = new Counting();
        check(repeated.indexOfMonitored(2, index) == 1 && index.completed == 2,
                "indexOf contract");
        Counting reverse = new Counting();
        check(repeated.lastIndexOfMonitored(2, reverse) == 3 && reverse.completed == 1,
                "lastIndexOf contract");

        Map<String, Integer> map = new HashMap<>(Map.of("a", 1, "b", 2));
        Counting mapped = new Counting();
        int[] mappedSum = { 0 };
        map.forEachMonitored((key, value) -> mappedSum[0] += value, mapped);
        check(mappedSum[0] == 3 && mapped.completed == 2, "map forEach");

        Counting contains = new Counting();
        check(!map.containsValueMonitored(999, contains) && contains.completed == 2,
                "map containsValue");

        Counting put = new Counting();
        Map<String, Integer> destination = new HashMap<>();
        destination.putAllMonitored(map, put);
        check(destination.equals(map) && put.completed == 2, "map putAll");

        Counting dualIterator = new Counting();
        new Dual().forEachRemainingMonitored(x -> { }, dualIterator);
        check(dualIterator.completed == 3, "dual Iterator resolves");
        Counting dualSplit = new Counting();
        new Dual().forEachRemainingSpliteratorMonitored(x -> { }, dualSplit);
        check(dualSplit.completed == 3, "dual Spliterator resolves");

        Counting earlyCancel = new Counting();
        earlyCancel.cancelAt = 2;
        cancel(() -> List.of(1, 2, 3, 4).forEachMonitored(x -> { }, earlyCancel));
        check(earlyCancel.completed == 2 && earlyCancel.done == 0, "borrowed monitor cancel");

        Counting partial = new Counting();
        partial.cancelAt = 2;
        Collection<Integer> prefix = new ArrayList<>();
        cancel(() -> prefix.addAllMonitored(List.of(1, 2, 3), partial));
        check(prefix.equals(List.of(1, 2)) && partial.completed == 2,
                "addAll successful prefix");

        Counting exceptionMonitor = new Counting();
        IllegalStateException sentinel = new IllegalStateException("sentinel");
        try {
            List.of(1, 2).iterator().forEachRemainingMonitored(x -> {
                throw sentinel;
            }, exceptionMonitor);
            throw new AssertionError("consumer exception suppressed");
        } catch (IllegalStateException observed) {
            check(observed == sentinel, "exception identity");
        }
        check(exceptionMonitor.completed == 0 && exceptionMonitor.done == 0,
                "no false callback credit or owned completion");

        System.out.println("M3_COLLECTION_PROGRESS_DEFAULTS_PASS assertions=" + assertions);
    }
}
