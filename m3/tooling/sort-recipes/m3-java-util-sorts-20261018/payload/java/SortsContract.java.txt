/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */

/*
 * @test
 * @summary Qualify every registered array and index sorting algorithm against JDK21
 * @modules java.base/java.util java.base/java.util.concurrent
 * @run main/othervm SortsContract
 */
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntBinaryOperator;

public class SortsContract {
    private static long checks;
    private static final long SEED = 0x33C05EEDL;
    private record Item(int key, int serial) { }

    public static void main(String[] args) throws Exception {
        boolean smoke = Arrays.asList(args).contains("--smoke");
        int trials = smoke ? 8 : 60;
        for (SortAlgorithm algorithm : SortAlgorithm.values()) {
            exerciseInt(algorithm, trials);
            exerciseByte(algorithm, trials);
            exerciseShort(algorithm, trials);
            exerciseChar(algorithm, trials);
            exerciseLong(algorithm, trials);
            exerciseFloat(algorithm, trials);
            exerciseDouble(algorithm, trials);
            exerciseBoolean(algorithm, trials);
            keyedOrders(algorithm, trials);
            if (!algorithm.requiresNumericKeys()) {
                objects(algorithm, trials);
                indexLanes(algorithm, trials);
                callbackFailure(algorithm);
            }
            cancellation(algorithm);
            System.out.println("ALGORITHM_PASS " + algorithm);
        }
        exhaustive(smoke ? 4 : 6);
        boundaries();
        floatingExtremes();
        structured(smoke ? 127 : 4097);
        concurrentCalls();
        System.out.println("SORTS_CONTRACT_PASS algorithms=" + SortAlgorithm.values().length
                + " checks=" + checks + " seed=" + SEED + " smoke=" + smoke);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("Expected " + type.getName() + ", got " + failure, failure);
        }
        throw new AssertionError("Expected " + type.getName());
    }

    private static void permutation(int[] order, int from, int to, IntBinaryOperator comparison,
                                     SortAlgorithm algorithm) {
        check(order.length == to - from, algorithm + " order length");
        boolean[] seen = new boolean[order.length];
        for (int i = 0; i < order.length; i++) {
            int source = order[i];
            check(source >= from && source < to, algorithm + " index outside range");
            check(!seen[source - from], algorithm + " duplicate source index");
            seen[source - from] = true;
            if (i > 0) {
                int compared = comparison.applyAsInt(order[i - 1], source);
                check(compared <= 0, algorithm + " index order");
                if (algorithm.isStable() && compared == 0) {
                    check(order[i - 1] < source, algorithm + " stability");
                }
            }
        }
    }

    private static void exerciseInt(SortAlgorithm algorithm, int trials) {
        Random random = new Random(SEED + algorithm.ordinal());
        for (int trial = 0; trial < trials; trial++) {
            int length = trial < 8 ? trial : random.nextInt(160);
            int[] original = new int[length + 4];
            for (int i = 0; i < original.length; i++) original[i] = random.nextInt(63) - 31;
            int[] expected = original.clone();
            Arrays.sort(expected, 2, length + 2);
            int[] actual = original.clone();
            Sorts.sort(actual, 2, length + 2, algorithm, null);
            check(Arrays.equals(expected, actual), algorithm + " int interior");
            int[] order = Sorts.order(original, 2, length + 2, algorithm, null);
            permutation(order, 2, length + 2, (left, right) -> Integer.compare(original[left], original[right]), algorithm);
            int[] all = original.clone();
            int[] allExpected = original.clone();
            Arrays.sort(allExpected);
            Sorts.sort(all, algorithm);
            check(Arrays.equals(allExpected, all), algorithm + " int whole");
            int[] allOrder = Sorts.order(original, algorithm);
            permutation(allOrder, 0, original.length, (left, right) -> Integer.compare(original[left], original[right]), algorithm);
            int[] noMonitor = original.clone();
            Sorts.sort(noMonitor, 2, length + 2, algorithm);
            check(Arrays.equals(expected, noMonitor), algorithm + " int range convenience");
            int[] rangeOrder = Sorts.order(original, 2, length + 2, algorithm);
            permutation(rangeOrder, 2, length + 2, (left, right) -> Integer.compare(original[left], original[right]), algorithm);
        }
    }

    private static void exerciseByte(SortAlgorithm algorithm, int trials) {
        Random random = new Random(SEED + algorithm.ordinal());
        for (int trial = 0; trial < trials; trial++) {
            int length = trial < 8 ? trial : random.nextInt(160);
            byte[] original = new byte[length + 4];
            for (int i = 0; i < original.length; i++) original[i] = (byte) random.nextInt(256);
            byte[] expected = original.clone();
            Arrays.sort(expected, 2, length + 2);
            byte[] actual = original.clone();
            Sorts.sort(actual, 2, length + 2, algorithm, null);
            check(Arrays.equals(expected, actual), algorithm + " byte interior");
            int[] order = Sorts.order(original, 2, length + 2, algorithm, null);
            permutation(order, 2, length + 2, (left, right) -> Byte.compare(original[left], original[right]), algorithm);
            byte[] all = original.clone();
            byte[] allExpected = original.clone();
            Arrays.sort(allExpected);
            Sorts.sort(all, algorithm);
            check(Arrays.equals(allExpected, all), algorithm + " byte whole");
            int[] allOrder = Sorts.order(original, algorithm);
            permutation(allOrder, 0, original.length, (left, right) -> Byte.compare(original[left], original[right]), algorithm);
            byte[] noMonitor = original.clone();
            Sorts.sort(noMonitor, 2, length + 2, algorithm);
            check(Arrays.equals(expected, noMonitor), algorithm + " byte range convenience");
            int[] rangeOrder = Sorts.order(original, 2, length + 2, algorithm);
            permutation(rangeOrder, 2, length + 2, (left, right) -> Byte.compare(original[left], original[right]), algorithm);
        }
    }

    private static void exerciseShort(SortAlgorithm algorithm, int trials) {
        Random random = new Random(SEED + algorithm.ordinal());
        for (int trial = 0; trial < trials; trial++) {
            int length = trial < 8 ? trial : random.nextInt(160);
            short[] original = new short[length + 4];
            for (int i = 0; i < original.length; i++) original[i] = (short) random.nextInt(1 << 16);
            short[] expected = original.clone();
            Arrays.sort(expected, 2, length + 2);
            short[] actual = original.clone();
            Sorts.sort(actual, 2, length + 2, algorithm, null);
            check(Arrays.equals(expected, actual), algorithm + " short interior");
            int[] order = Sorts.order(original, 2, length + 2, algorithm, null);
            permutation(order, 2, length + 2, (left, right) -> Short.compare(original[left], original[right]), algorithm);
            short[] all = original.clone();
            short[] allExpected = original.clone();
            Arrays.sort(allExpected);
            Sorts.sort(all, algorithm);
            check(Arrays.equals(allExpected, all), algorithm + " short whole");
            int[] allOrder = Sorts.order(original, algorithm);
            permutation(allOrder, 0, original.length, (left, right) -> Short.compare(original[left], original[right]), algorithm);
            short[] noMonitor = original.clone();
            Sorts.sort(noMonitor, 2, length + 2, algorithm);
            check(Arrays.equals(expected, noMonitor), algorithm + " short range convenience");
            int[] rangeOrder = Sorts.order(original, 2, length + 2, algorithm);
            permutation(rangeOrder, 2, length + 2, (left, right) -> Short.compare(original[left], original[right]), algorithm);
        }
    }

    private static void exerciseChar(SortAlgorithm algorithm, int trials) {
        Random random = new Random(SEED + algorithm.ordinal());
        for (int trial = 0; trial < trials; trial++) {
            int length = trial < 8 ? trial : random.nextInt(160);
            char[] original = new char[length + 4];
            for (int i = 0; i < original.length; i++) original[i] = (char) random.nextInt(1 << 16);
            char[] expected = original.clone();
            Arrays.sort(expected, 2, length + 2);
            char[] actual = original.clone();
            Sorts.sort(actual, 2, length + 2, algorithm, null);
            check(Arrays.equals(expected, actual), algorithm + " char interior");
            int[] order = Sorts.order(original, 2, length + 2, algorithm, null);
            permutation(order, 2, length + 2, (left, right) -> Character.compare(original[left], original[right]), algorithm);
            char[] all = original.clone();
            char[] allExpected = original.clone();
            Arrays.sort(allExpected);
            Sorts.sort(all, algorithm);
            check(Arrays.equals(allExpected, all), algorithm + " char whole");
            int[] allOrder = Sorts.order(original, algorithm);
            permutation(allOrder, 0, original.length, (left, right) -> Character.compare(original[left], original[right]), algorithm);
            char[] noMonitor = original.clone();
            Sorts.sort(noMonitor, 2, length + 2, algorithm);
            check(Arrays.equals(expected, noMonitor), algorithm + " char range convenience");
            int[] rangeOrder = Sorts.order(original, 2, length + 2, algorithm);
            permutation(rangeOrder, 2, length + 2, (left, right) -> Character.compare(original[left], original[right]), algorithm);
        }
    }

    private static void exerciseLong(SortAlgorithm algorithm, int trials) {
        Random random = new Random(SEED + algorithm.ordinal());
        for (int trial = 0; trial < trials; trial++) {
            int length = trial < 8 ? trial : random.nextInt(160);
            long[] original = new long[length + 4];
            for (int i = 0; i < original.length; i++) original[i] = random.nextInt(63) - 31L;
            long[] expected = original.clone();
            Arrays.sort(expected, 2, length + 2);
            long[] actual = original.clone();
            Sorts.sort(actual, 2, length + 2, algorithm, null);
            check(Arrays.equals(expected, actual), algorithm + " long interior");
            int[] order = Sorts.order(original, 2, length + 2, algorithm, null);
            permutation(order, 2, length + 2, (left, right) -> Long.compare(original[left], original[right]), algorithm);
            long[] all = original.clone();
            long[] allExpected = original.clone();
            Arrays.sort(allExpected);
            Sorts.sort(all, algorithm);
            check(Arrays.equals(allExpected, all), algorithm + " long whole");
            int[] allOrder = Sorts.order(original, algorithm);
            permutation(allOrder, 0, original.length, (left, right) -> Long.compare(original[left], original[right]), algorithm);
            long[] noMonitor = original.clone();
            Sorts.sort(noMonitor, 2, length + 2, algorithm);
            check(Arrays.equals(expected, noMonitor), algorithm + " long range convenience");
            int[] rangeOrder = Sorts.order(original, 2, length + 2, algorithm);
            permutation(rangeOrder, 2, length + 2, (left, right) -> Long.compare(original[left], original[right]), algorithm);
        }
    }

    private static void exerciseFloat(SortAlgorithm algorithm, int trials) {
        Random random = new Random(SEED + algorithm.ordinal());
        for (int trial = 0; trial < trials; trial++) {
            int length = trial < 8 ? trial : random.nextInt(160);
            float[] original = new float[length + 4];
            for (int i = 0; i < original.length; i++) original[i] = Float.intBitsToFloat(random.nextInt(127));
            float[] expected = original.clone();
            Arrays.sort(expected, 2, length + 2);
            float[] actual = original.clone();
            Sorts.sort(actual, 2, length + 2, algorithm, null);
            check(Arrays.equals(expected, actual), algorithm + " float interior");
            int[] order = Sorts.order(original, 2, length + 2, algorithm, null);
            permutation(order, 2, length + 2, (left, right) -> Float.compare(original[left], original[right]), algorithm);
            float[] all = original.clone();
            float[] allExpected = original.clone();
            Arrays.sort(allExpected);
            Sorts.sort(all, algorithm);
            check(Arrays.equals(allExpected, all), algorithm + " float whole");
            int[] allOrder = Sorts.order(original, algorithm);
            permutation(allOrder, 0, original.length, (left, right) -> Float.compare(original[left], original[right]), algorithm);
            float[] noMonitor = original.clone();
            Sorts.sort(noMonitor, 2, length + 2, algorithm);
            check(Arrays.equals(expected, noMonitor), algorithm + " float range convenience");
            int[] rangeOrder = Sorts.order(original, 2, length + 2, algorithm);
            permutation(rangeOrder, 2, length + 2, (left, right) -> Float.compare(original[left], original[right]), algorithm);
        }
    }

    private static void exerciseDouble(SortAlgorithm algorithm, int trials) {
        Random random = new Random(SEED + algorithm.ordinal());
        for (int trial = 0; trial < trials; trial++) {
            int length = trial < 8 ? trial : random.nextInt(160);
            double[] original = new double[length + 4];
            for (int i = 0; i < original.length; i++) original[i] = Double.longBitsToDouble(random.nextInt(127));
            double[] expected = original.clone();
            Arrays.sort(expected, 2, length + 2);
            double[] actual = original.clone();
            Sorts.sort(actual, 2, length + 2, algorithm, null);
            check(Arrays.equals(expected, actual), algorithm + " double interior");
            int[] order = Sorts.order(original, 2, length + 2, algorithm, null);
            permutation(order, 2, length + 2, (left, right) -> Double.compare(original[left], original[right]), algorithm);
            double[] all = original.clone();
            double[] allExpected = original.clone();
            Arrays.sort(allExpected);
            Sorts.sort(all, algorithm);
            check(Arrays.equals(allExpected, all), algorithm + " double whole");
            int[] allOrder = Sorts.order(original, algorithm);
            permutation(allOrder, 0, original.length, (left, right) -> Double.compare(original[left], original[right]), algorithm);
            double[] noMonitor = original.clone();
            Sorts.sort(noMonitor, 2, length + 2, algorithm);
            check(Arrays.equals(expected, noMonitor), algorithm + " double range convenience");
            int[] rangeOrder = Sorts.order(original, 2, length + 2, algorithm);
            permutation(rangeOrder, 2, length + 2, (left, right) -> Double.compare(original[left], original[right]), algorithm);
        }
    }

    private static void exerciseBoolean(SortAlgorithm algorithm, int trials) {
        Random random = new Random(SEED);
        for (int trial = 0; trial < trials; trial++) {
            boolean[] original = new boolean[trial + 4];
            for (int i = 0; i < original.length; i++) original[i] = random.nextBoolean();
            boolean[] expected = original.clone();
            int falseCount = 0;
            for (int i = 2; i < trial + 2; i++) if (!original[i]) falseCount++;
            for (int i = 2; i < trial + 2; i++) expected[i] = i >= 2 + falseCount;
            boolean[] actual = original.clone();
            Sorts.sort(actual, 2, trial + 2, algorithm, null);
            check(Arrays.equals(expected, actual), algorithm + " boolean interior");
            permutation(Sorts.order(original, 2, trial + 2, algorithm, null), 2, trial + 2,
                    (left, right) -> Boolean.compare(original[left], original[right]), algorithm);
            boolean[] whole = original.clone();
            Sorts.sort(whole, algorithm);
            for (int i = 1; i < whole.length; i++) {
                check(!whole[i - 1] || whole[i], algorithm + " boolean whole");
            }
            permutation(Sorts.order(original, algorithm), 0, original.length,
                    (left, right) -> Boolean.compare(original[left], original[right]), algorithm);
            boolean[] convenience = original.clone();
            Sorts.sort(convenience, 2, trial + 2, algorithm);
            check(Arrays.equals(expected, convenience), algorithm + " boolean convenience");
            permutation(Sorts.order(original, 2, trial + 2, algorithm), 2, trial + 2,
                    (left, right) -> Boolean.compare(original[left], original[right]), algorithm);
        }
    }

    private static void objects(SortAlgorithm algorithm, int trials) {
        Random random = new Random(SEED);
        Comparator<Item> comparison = (left, right) -> {
            int sign = Integer.compare(left.key(), right.key());
            return sign < 0 ? Integer.MIN_VALUE : sign > 0 ? Integer.MAX_VALUE : 0;
        };
        for (int trial = 0; trial < trials; trial++) {
            Item[] original = new Item[trial + 4];
            for (int i = 0; i < original.length; i++) original[i] = new Item(random.nextInt(9), i);
            Item[] expected = original.clone();
            Arrays.sort(expected, 2, trial + 2, comparison);
            Item[] actual = original.clone();
            Sorts.sort(actual, 2, trial + 2, comparison, algorithm, null);
            check(actual[0] == original[0] && actual[1] == original[1]
                    && actual[trial + 2] == original[trial + 2]
                    && actual[trial + 3] == original[trial + 3], algorithm + " outside reference range");
            Set<Item> identities = Collections.newSetFromMap(new IdentityHashMap<>());
            identities.addAll(Arrays.asList(original));
            for (int i = 0; i < actual.length; i++) {
                check(identities.remove(actual[i]), algorithm + " object identity");
                if (i >= 2 && i < trial + 2) {
                    check(actual[i].key() == expected[i].key(), algorithm + " object order");
                    if (algorithm.isStable()) check(actual[i] == expected[i], algorithm + " object stable");
                }
            }
            check(identities.isEmpty(), algorithm + " object occurrence loss");
            permutation(Sorts.order(original, 2, trial + 2, comparison, algorithm, null),
                    2, trial + 2, (left, right) -> comparison.compare(original[left], original[right]),
                    algorithm);
            Item[] whole = original.clone();
            Sorts.sort(whole, comparison, algorithm);
            for (int i = 1; i < whole.length; i++) {
                check(comparison.compare(whole[i - 1], whole[i]) <= 0, algorithm + " whole object");
            }
            permutation(Sorts.order(original, comparison, algorithm), 0, original.length,
                    (left, right) -> comparison.compare(original[left], original[right]), algorithm);
            permutation(Sorts.order(2, trial + 2,
                    (left, right) -> comparison.compare(original[left], original[right]),
                    algorithm, null), 2, trial + 2,
                    (left, right) -> comparison.compare(original[left], original[right]), algorithm);
        }
        String[] strings = {"z", "beta", "alpha", "beta", "a"};
        String[] expected = strings.clone();
        Arrays.sort(expected);
        Sorts.sort(strings, algorithm);
        check(Arrays.equals(expected, strings), algorithm + " natural whole");
        String[] nullable = {"x", null, "a", null, "b"};
        Comparator<String> nullOrder = Comparator.nullsFirst(Comparator.naturalOrder());
        expected = nullable.clone();
        Arrays.sort(expected, nullOrder);
        Sorts.sort(nullable, nullOrder, algorithm);
        check(Arrays.equals(expected, nullable), algorithm + " explicit null comparator");
        String[] partial = {"keep", "z", "b", "a", "keep"};
        expected = partial.clone();
        Arrays.sort(expected, 1, 4);
        Sorts.sort(partial, 1, 4, algorithm);
        check(Arrays.equals(expected, partial), algorithm + " natural range");
        permutation(Sorts.order(strings, algorithm), 0, strings.length,
                (left, right) -> strings[left].compareTo(strings[right]), algorithm);
        Item repeated = new Item(1, 10);
        Item smaller = new Item(0, 11);
        Item[] repeatedRefs = {repeated, smaller, repeated, repeated};
        Sorts.sort(repeatedRefs, comparison, algorithm);
        check(repeatedRefs[0] == smaller && repeatedRefs[1] == repeated
                && repeatedRefs[2] == repeated && repeatedRefs[3] == repeated,
                algorithm + " repeated reference multiplicity");
    }

    private static void indexLanes(SortAlgorithm algorithm, int trials) {
        IntBinaryOperator comparison = (left, right) ->
                Integer.compare(Math.floorMod(left, 7), Math.floorMod(right, 7));
        Random random = new Random(SEED);
        for (int trial = 0; trial < trials; trial++) {
            int[] indices = new int[trial + 6];
            for (int i = 0; i < indices.length; i++) indices[i] = random.nextInt(31) - 15;
            Integer[] expected = Arrays.stream(indices).boxed().toArray(Integer[]::new);
            Arrays.sort(expected, 2, indices.length - 2, comparison::applyAsInt);
            int[] original = indices.clone();
            Sorts.sortIndices(indices, 2, indices.length - 2, comparison, algorithm, null);
            check(indices[0] == original[0] && indices[1] == original[1]
                    && indices[indices.length - 1] == original[original.length - 1]
                    && indices[indices.length - 2] == original[original.length - 2],
                    algorithm + " index guard");
            int[] expectedMultiset = original.clone();
            int[] actualMultiset = indices.clone();
            Arrays.sort(expectedMultiset);
            Arrays.sort(actualMultiset);
            check(Arrays.equals(expectedMultiset, actualMultiset), algorithm + " index multiset");
            for (int i = 2; i < indices.length - 2; i++) {
                check(comparison.applyAsInt(indices[i], expected[i]) == 0,
                        algorithm + " index duplicate order");
                if (algorithm.isStable()) check(indices[i] == expected[i], algorithm + " index stable");
            }
        }
    }

    private static void keyedOrders(SortAlgorithm algorithm, int trials) {
        Random random = new Random(SEED);
        for (int trial = 0; trial < trials; trial++) {
            int from = 3;
            long[] keys = new long[trial + from];
            for (int i = 0; i < keys.length; i++) keys[i] = random.nextInt(17) - 8;
            int[] reads = new int[keys.length];
            int[] order = Sorts.orderByLong(from, keys.length, index -> {
                reads[index]++;
                return keys[index];
            }, algorithm, null);
            permutation(order, from, keys.length,
                    (left, right) -> Long.compare(keys[left], keys[right]), algorithm);
            for (int i = 0; i < keys.length; i++) {
                check(reads[i] == (i < from ? 0 : 1), algorithm + " key admission count");
            }
        }
    }

    private static void callbackFailure(SortAlgorithm algorithm) {
        Integer[] original = new Integer[80];
        for (int i = 0; i < original.length; i++) original[i] = original.length - i;
        Integer[] values = original.clone();
        AtomicInteger calls = new AtomicInteger();
        expect(IllegalStateException.class, () ->
                Sorts.sort(values, 0, values.length, (left, right) -> {
                    if (calls.incrementAndGet() == 7) throw new IllegalStateException("frozen comparator");
                    return Integer.compare(left, right);
                }, algorithm, null));
        check(Arrays.equals(original, values), algorithm + " comparator exception mutated payload");
        int[] indices = {90, 8, 7, 6, 5, 4, 3, 2, 1, 80};
        int[] before = indices.clone();
        expect(IllegalStateException.class, () ->
                Sorts.sortIndices(indices, 1, 9, (left, right) -> {
                    throw new IllegalStateException("index failure");
                }, algorithm, null));
        check(Arrays.equals(before, indices), algorithm + " index exception mutated lane");
        Object[] invalid = {new Object(), new Object()};
        Object[] saved = invalid.clone();
        expect(ClassCastException.class, () -> Sorts.sort(invalid, algorithm));
        check(Arrays.equals(saved, invalid), algorithm + " natural CCE mutated payload");
    }

    @SuppressWarnings("try")
    private static void cancellation(SortAlgorithm algorithm) {
        int[] original = new int[100];
        for (int i = 0; i < original.length; i++) original[i] = original.length - i;
        int[] values = original.clone();
        ProgressMonitor cancelled = new ProgressMonitor() {
            @Override public boolean isCancelled() { return true; }
        };
        expect(CancellationException.class, () ->
                Sorts.sort(values, 0, values.length, algorithm, cancelled));
        check(Arrays.equals(original, values), algorithm + " precancel mutation");

        ProgressMonitor during = new ProgressMonitor() {
            private boolean stop;
            @Override public void worked(long units) { stop = true; }
            @Override public boolean isCancelled() { return stop; }
            @Override public void begin(String task, long totalWork) { throw new AssertionError("borrowed begin"); }
            @Override public void done() { throw new AssertionError("borrowed done"); }
        };
        expect(CancellationException.class, () ->
                Sorts.sort(values, 0, values.length, algorithm, during));
        check(Arrays.equals(original, values), algorithm + " cancellation after work mutated payload");
        ProgressMonitor broken = new ProgressMonitor() {
            @Override public void worked(long units) { throw new IllegalStateException("monitor"); }
        };
        expect(IllegalStateException.class, () ->
                Sorts.sort(values, 0, values.length, algorithm, broken));
        check(Arrays.equals(original, values), algorithm + " monitor failure mutation");
        try (ProgressMonitors.Scope scope = ProgressMonitors.bind(cancelled)) {
            expect(CancellationException.class, () -> Sorts.sort(values, algorithm));
        }
        Thread.currentThread().interrupt();
        try {
            Sorts.sort(values, 0, values.length, algorithm, ProgressMonitor.NONE);
            check(values[0] == 1, algorithm + " NONE interruption policy");
            final int[] interruptValues = original.clone();
            expect(CancellationException.class, () ->
                    Sorts.sort(interruptValues, 0, interruptValues.length, algorithm, new ProgressMonitor() { }));
        } finally {
            Thread.interrupted();
        }
    }

    private static void exhaustive(int maxLength) {
        for (int length = 0; length <= maxLength; length++) {
            int cases = 1;
            for (int i = 0; i < length; i++) cases *= 3;
            for (int code = 0; code < cases; code++) {
                int[] input = new int[length];
                int digits = code;
                for (int i = 0; i < length; i++) {
                    input[i] = digits % 3 - 1;
                    digits /= 3;
                }
                int[] expected = input.clone();
                Arrays.sort(expected);
                for (SortAlgorithm algorithm : SortAlgorithm.values()) {
                    int[] actual = input.clone();
                    Sorts.sort(actual, algorithm);
                    check(Arrays.equals(expected, actual), algorithm + " exhaustive ternary "
                            + Arrays.toString(input));
                }
            }
        }
    }

    private static void boundaries() {
        int[] original = {4, 3, 2, 1};
        expect(NullPointerException.class, () -> Sorts.sort((int[]) null, SortAlgorithm.HEAP));
        expect(NullPointerException.class, () -> Sorts.sort(original, (SortAlgorithm) null));
        expect(IllegalArgumentException.class, () -> Sorts.sort(original, 3, 2, SortAlgorithm.HEAP));
        expect(ArrayIndexOutOfBoundsException.class, () -> Sorts.sort(original, -1, 2, SortAlgorithm.HEAP));
        expect(ArrayIndexOutOfBoundsException.class, () -> Sorts.sort(original, 0, 5, SortAlgorithm.HEAP));
        check(Arrays.equals(original, new int[]{4, 3, 2, 1}), "range mutation");
        for (SortAlgorithm algorithm : SortAlgorithm.values()) {
            if (algorithm.requiresNumericKeys()) {
                expect(IllegalArgumentException.class, () -> Sorts.sort(new Integer[]{2, 1}, algorithm));
                expect(IllegalArgumentException.class, () -> Sorts.order(0, 2, Integer::compare, algorithm));
                expect(IllegalArgumentException.class, () ->
                        Sorts.sortIndices(new int[]{2, 1}, Integer::compare, algorithm));
            }
            expect(IllegalStateException.class, () -> Sorts.orderByLong(0, 10, index -> {
                if (index == 3) throw new IllegalStateException("key failure");
                return index;
            }, algorithm));
        }
        for (SortAlgorithm algorithm : new SortAlgorithm[]{SortAlgorithm.COUNTING, SortAlgorithm.PIGEONHOLE}) {
            long[] extreme = {Long.MAX_VALUE, Long.MIN_VALUE};
            long[] before = extreme.clone();
            expect(IllegalArgumentException.class, () -> Sorts.sort(extreme, algorithm));
            check(Arrays.equals(before, extreme), algorithm + " signed span refusal");
            long[] tooWide = {0, Sorts.MAX_HISTOGRAM_SPAN};
            expect(IllegalArgumentException.class, () -> Sorts.sort(tooWide, algorithm));
            long[] fits = {Sorts.MAX_HISTOGRAM_SPAN - 1L, 0};
            Sorts.sort(fits, algorithm);
            check(fits[0] == 0 && fits[1] == Sorts.MAX_HISTOGRAM_SPAN - 1L, algorithm + " exact span");
            long[] high = {Long.MAX_VALUE, Long.MAX_VALUE - 1, Long.MAX_VALUE};
            Sorts.sort(high, algorithm);
            check(high[0] == Long.MAX_VALUE - 1 && high[2] == Long.MAX_VALUE, algorithm + " high narrow");
            long[] low = {Long.MIN_VALUE + 1, Long.MIN_VALUE, Long.MIN_VALUE};
            Sorts.sort(low, algorithm);
            check(low[0] == Long.MIN_VALUE && low[2] == Long.MIN_VALUE + 1, algorithm + " low narrow");
        }
    }

    private static void floatingExtremes() {
        float[] floats = {Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY,
                -0.0f, 0.0f, Float.MIN_VALUE, -Float.MIN_VALUE, Float.MIN_NORMAL,
                -Float.MAX_VALUE, Float.MAX_VALUE, Float.intBitsToFloat(0xffc00031),
                Float.intBitsToFloat(0x7f800001), -1, 1};
        double[] doubles = {Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                -0.0d, 0.0d, Double.MIN_VALUE, -Double.MIN_VALUE, Double.MIN_NORMAL,
                -Double.MAX_VALUE, Double.MAX_VALUE, Double.longBitsToDouble(0xfff8000000000031L),
                Double.longBitsToDouble(0x7ff0000000000001L), -1, 1};
        Random random = new Random(SEED);
        floats = Arrays.copyOf(floats, 150);
        doubles = Arrays.copyOf(doubles, 150);
        for (int i = 14; i < floats.length; i++) {
            floats[i] = Float.intBitsToFloat(random.nextInt());
            doubles[i] = Double.longBitsToDouble(random.nextLong());
        }
        final float[] originalFloats = floats;
        final double[] originalDoubles = doubles;
        for (SortAlgorithm algorithm : SortAlgorithm.values()) {
            if (algorithm == SortAlgorithm.COUNTING || algorithm == SortAlgorithm.PIGEONHOLE) continue;
            float[] fa = originalFloats.clone();
            float[] fe = originalFloats.clone();
            double[] da = originalDoubles.clone();
            double[] de = originalDoubles.clone();
            Arrays.sort(fe);
            Arrays.sort(de);
            Sorts.sort(fa, algorithm);
            Sorts.sort(da, algorithm);
            Map<Integer, Integer> fbits = new HashMap<>();
            Map<Long, Integer> dbits = new HashMap<>();
            for (int i = 0; i < fa.length; i++) {
                fbits.merge(Float.floatToRawIntBits(originalFloats[i]), 1, Integer::sum);
                fbits.merge(Float.floatToRawIntBits(fa[i]), -1, Integer::sum);
                dbits.merge(Double.doubleToRawLongBits(originalDoubles[i]), 1, Integer::sum);
                dbits.merge(Double.doubleToRawLongBits(da[i]), -1, Integer::sum);
                check(Float.compare(fe[i], fa[i]) == 0, algorithm + " float total order");
                check(Double.compare(de[i], da[i]) == 0, algorithm + " double total order");
            }
            check(fbits.values().stream().allMatch(count -> count == 0), algorithm + " float payload bits");
            check(dbits.values().stream().allMatch(count -> count == 0), algorithm + " double payload bits");
            permutation(Sorts.order(originalFloats, algorithm), 0, originalFloats.length,
                    (left, right) -> Float.compare(originalFloats[left], originalFloats[right]), algorithm);
            permutation(Sorts.order(originalDoubles, algorithm), 0, originalDoubles.length,
                    (left, right) -> Double.compare(originalDoubles[left], originalDoubles[right]), algorithm);
            long[] extremes = {Long.MIN_VALUE, Long.MAX_VALUE, -1, 0, 1, Long.MIN_VALUE, Long.MAX_VALUE};
            long[] expected = extremes.clone();
            Arrays.sort(expected);
            Sorts.sort(extremes, algorithm);
            check(Arrays.equals(extremes, expected), algorithm + " long full domain");
        }
        for (SortAlgorithm algorithm : new SortAlgorithm[]{SortAlgorithm.COUNTING, SortAlgorithm.PIGEONHOLE}) {
            float[] narrow = {-0.0f, 0.0f, -Float.MIN_VALUE, Float.MIN_VALUE, -0.0f};
            float[] expected = narrow.clone();
            Arrays.sort(expected);
            Sorts.sort(narrow, algorithm);
            check(Arrays.equals(narrow, expected), algorithm + " histogram float signed zero");
            double[] dn = {-0.0d, 0.0d, -Double.MIN_VALUE, Double.MIN_VALUE, -0.0d};
            double[] de = dn.clone();
            Arrays.sort(de);
            Sorts.sort(dn, algorithm);
            check(Arrays.equals(dn, de), algorithm + " histogram double signed zero");
        }
    }

    private static void structured(int size) {
        Set<SortAlgorithm> quadratic = EnumSet.of(SortAlgorithm.INSERTION,
                SortAlgorithm.BINARY_INSERTION, SortAlgorithm.SELECTION, SortAlgorithm.DOUBLE_SELECTION,
                SortAlgorithm.BUBBLE, SortAlgorithm.COCKTAIL, SortAlgorithm.COMB, SortAlgorithm.GNOME,
                SortAlgorithm.ODD_EVEN, SortAlgorithm.CYCLE, SortAlgorithm.PANCAKE,
                SortAlgorithm.BINGO, SortAlgorithm.STRAND, SortAlgorithm.BUCKET);
        for (SortAlgorithm algorithm : SortAlgorithm.values()) {
            int n = quadratic.contains(algorithm) ? Math.min(size, 257) : size;
            for (int shape = 0; shape < 7; shape++) {
                int[] original = new int[n];
                for (int i = 0; i < n; i++) original[i] = switch (shape) {
                    case 0 -> i;
                    case 1 -> n - i;
                    case 2 -> 7;
                    case 3 -> i % 5;
                    case 4 -> Math.min(i, n - i);
                    case 5 -> i % 2 == 0 ? i : n - i;
                    default -> (i / 17) % 2 == 0 ? i : n - i;
                };
                int[] expected = original.clone();
                Arrays.sort(expected);
                int[] actual = original.clone();
                Sorts.sort(actual, algorithm);
                check(Arrays.equals(expected, actual), algorithm + " structured shape=" + shape + " n=" + n);
            }
        }
        for (SortAlgorithm algorithm : new SortAlgorithm[]{SortAlgorithm.BITONIC, SortAlgorithm.ODD_EVEN_MERGE}) {
            for (int n : new int[]{0, 1, 2, 3, 7, 8, 9, 15, 16, 17, 31, 32, 33,
                    63, 64, 65, 127, 128, 129, 255, 256, 257, 511, 512, 513}) {
                int[] input = new Random(SEED + n).ints(n, -4, 5).toArray();
                int[] expected = input.clone();
                Arrays.sort(expected);
                Sorts.sort(input, algorithm);
                check(Arrays.equals(expected, input), algorithm + " network boundary " + n);
            }
        }
        AtomicInteger comparisons = new AtomicInteger();
        int n = Math.max(1000, size);
        int[] equalOrder = Sorts.order(0, n, (left, right) -> {
            comparisons.incrementAndGet();
            return 0;
        }, SortAlgorithm.INTRO, null);
        check(equalOrder.length == n && comparisons.get() < 64L * n, "intro duplicate depth bound");
    }

    private static void concurrentCalls() throws Exception {
        try (ExecutorService executor = Executors.newFixedThreadPool(4)) {
            List<Future<Boolean>> results = new ArrayList<>();
            for (SortAlgorithm algorithm : SortAlgorithm.values()) {
                results.add(executor.submit(() -> {
                    int[] values = new Random(SEED + algorithm.ordinal()).ints(63, -30, 31).toArray();
                    int[] expected = values.clone();
                    Arrays.sort(expected);
                    for (int repeat = 0; repeat < 4; repeat++) {
                        int[] actual = values.clone();
                        Sorts.sort(actual, 0, actual.length, algorithm, null);
                        if (!Arrays.equals(expected, actual)) return false;
                    }
                    return true;
                }));
            }
            for (Future<Boolean> result : results) check(result.get(), "concurrent independent invocation");
        }
    }
}
