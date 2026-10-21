/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
package java.util;

import java.util.concurrent.ProgressMonitor;
import java.util.concurrent.ProgressMonitors;
import java.util.function.IntBinaryOperator;
import java.util.function.IntToLongFunction;

/**
 * Explicit, deterministic in-memory sorting algorithms for arrays and index lanes.
 *
 * <p>Every range is half-open: {@code [fromIndex, toIndex)}. Values outside it
 * are untouched. An {@code order} result contains the absolute source index of
 * each successive sorted value and never rearranges the source. This permits
 * sorting metadata over parent-owned arrays and aligned masks without retaining
 * a wrapper object for each element.
 *
 * <p>Sorting prepares a primitive {@code int[]} permutation, then applies it by
 * cycles. An exception from a comparator, key function or monitor occurs before
 * payload writes begin. These callbacks must obey their ordering contracts and
 * must not modify the input. The final permutation commit has no callbacks or
 * cancellation checks. The caller must provide exclusive access throughout the
 * invocation; concurrent or callback mutation is outside this contract.
 *
 * <p>Convenience overloads borrow {@link ProgressMonitors#current()}. An explicit
 * {@code null} monitor or {@link ProgressMonitor#NONE} disables progress and
 * cancellation. A supplied monitor receives bounded batches of work units
 * (comparisons, key admissions and kernel steps), with cancellation checked at
 * admission, every 1024 such steps and before commit. The monitor is borrowed:
 * these methods never call {@code begin} or {@code done}. A single comparator,
 * key function or bulk array copy is not preempted.
 *
 * <p>Object comparators may be {@code null}, requesting natural order as in
 * {@link Arrays#sort(Object[])}. Numeric-only algorithms require a primitive
 * array or {@link #orderByLong(int, int, IntToLongFunction, SortAlgorithm,
 * ProgressMonitor)}. Floating-point order follows {@link Float#compare(float,
 * float)} and {@link Double#compare(double, double)}: all NaNs follow positive
 * infinity, and negative zero precedes positive zero. The permutation copies original
 * values rather than reconstructing them from canonical keys. Signaling-NaN
 * payload preservation remains subject to the platform copying semantics
 * documented by Float and Double.
 *
 * <p>Stability is selectable through {@link SortAlgorithm#isStable()}, rather
 * than promised for every algorithm. Existing {@code Arrays.sort},
 * {@code Arrays.parallelSort} and collection sorting contracts remain unchanged.
 * Even an in-place kernel has O(n) invocation metadata here; numeric algorithms
 * additionally retain one temporary {@code long} ordering key per value.
 * No algorithm silently switches because its workspace domain is unsupported.
 */
public final class Sorts {
    /** Maximum inclusive numeric key span admitted by histogram algorithms. */
    public static final int MAX_HISTOGRAM_SPAN = 1 << 20;

    private Sorts() { }

    /**
     * Sorts an object array in natural order using the selected algorithm.
     *
     * @param <T> the array element type
     * @param values the array to sort
     * @param algorithm the selected sorting algorithm
     */
    public static <T> void sort(T[] values, SortAlgorithm algorithm) {
        sort(values, 0, values.length, null, algorithm, ProgressMonitors.current());
    }

    /**
     * Sorts an object array using a comparator, or natural order when null.
     *
     * @param <T> the array element type
     * @param values the array to sort
     * @param comparator the value comparator, or {@code null} for natural order
     * @param algorithm the selected sorting algorithm
     */
    public static <T> void sort(T[] values, Comparator<? super T> comparator,
                                SortAlgorithm algorithm) {
        sort(values, 0, values.length, comparator, algorithm, ProgressMonitors.current());
    }

    /**
     * Sorts an object range in natural order with the current progress monitor.
     *
     * @param <T> the array element type
     * @param values the array to sort
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     */
    public static <T> void sort(T[] values, int fromIndex, int toIndex,
                                SortAlgorithm algorithm) {
        sort(values, fromIndex, toIndex, null, algorithm, ProgressMonitors.current());
    }

    /**
     * Sorts an object range using the current progress monitor.
     *
     * @param <T> the array element type
     * @param values the array to sort
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param comparator the value comparator, or {@code null} for natural order
     * @param algorithm the selected sorting algorithm
     */
    public static <T> void sort(T[] values, int fromIndex, int toIndex,
                                Comparator<? super T> comparator, SortAlgorithm algorithm) {
        sort(values, fromIndex, toIndex, comparator, algorithm, ProgressMonitors.current());
    }

    /**
     * Sorts an object range with a borrowed, nullable progress monitor.
     *
     * @param <T> the array element type
     * @param values the array to sort
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param comparator the value comparator, or {@code null} for natural order
     * @param algorithm the selected sorting algorithm
     * @param monitor a borrowed monitor, or {@code null} to disable progress and cancellation
     */
    public static <T> void sort(T[] values, int fromIndex, int toIndex,
                                Comparator<? super T> comparator, SortAlgorithm algorithm,
                                ProgressMonitor monitor) {
        int[] order = order(values, fromIndex, toIndex, comparator, algorithm, monitor);
        for (int i = 0; i < order.length; i++) {
            if (order[i] == fromIndex + i) continue;
            T saved = values[fromIndex + i];
            int destination = i;
            while (true) {
                int source = order[destination] - fromIndex;
                order[destination] = fromIndex + destination;
                if (source == i) {
                    values[fromIndex + destination] = saved;
                    break;
                }
                values[fromIndex + destination] = values[fromIndex + source];
                destination = source;
            }
        }
    }

    /**
     * Returns sorted absolute source indices for an object array in natural order.
     *
     * @param <T> the array element type
     * @param values the source array, which is not rearranged
     * @param algorithm the selected sorting algorithm
     * @return absolute source indices in the selected sorted order
     */
    public static <T> int[] order(T[] values, SortAlgorithm algorithm) {
        return order(values, 0, values.length, null, algorithm, ProgressMonitors.current());
    }

    /**
     * Returns sorted absolute source indices using a comparator or natural order.
     *
     * @param <T> the array element type
     * @param values the source array, which is not rearranged
     * @param comparator the value comparator, or {@code null} for natural order
     * @param algorithm the selected sorting algorithm
     * @return absolute source indices in the selected sorted order
     */
    public static <T> int[] order(T[] values, Comparator<? super T> comparator,
                                 SortAlgorithm algorithm) {
        return order(values, 0, values.length, comparator, algorithm, ProgressMonitors.current());
    }

    /**
     * Returns sorted absolute indices for an object range with the current monitor.
     *
     * @param <T> the array element type
     * @param values the source array, which is not rearranged
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param comparator the value comparator, or {@code null} for natural order
     * @param algorithm the selected sorting algorithm
     * @return absolute source indices in the selected sorted order
     */
    public static <T> int[] order(T[] values, int fromIndex, int toIndex,
                                 Comparator<? super T> comparator, SortAlgorithm algorithm) {
        return order(values, fromIndex, toIndex, comparator, algorithm, ProgressMonitors.current());
    }

    /**
     * Returns sorted absolute indices; it never rearranges the object array.
     *
     * @param <T> the array element type
     * @param values the source array, which is not rearranged
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param comparator the value comparator, or {@code null} for natural order
     * @param algorithm the selected sorting algorithm
     * @param monitor a borrowed monitor, or {@code null} to disable progress and cancellation
     * @return absolute source indices in the selected sorted order
     */
    public static <T> int[] order(T[] values, int fromIndex, int toIndex,
                                 Comparator<? super T> comparator, SortAlgorithm algorithm,
                                 ProgressMonitor monitor) {
        int length = values.length;
        checkRange(length, fromIndex, toIndex);
        requireComparison(algorithm);
        IntBinaryOperator comparison = comparator == null
                ? (left, right) -> naturalCompare(values[left], values[right])
                : (left, right) -> comparator.compare(values[left], values[right]);
        return comparisonOrder(fromIndex, toIndex, comparison, algorithm, monitor);
    }

    /**
     * Returns ordered coordinates over a caller-owned indexed source. The comparator
     * receives absolute indices; the values and any aligned masks remain with the caller.
     *
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param comparator the comparison of absolute source indices
     * @param algorithm the selected sorting algorithm
     * @return absolute source indices in the selected sorted order
     */
    public static int[] order(int fromIndex, int toIndex, IntBinaryOperator comparator,
                              SortAlgorithm algorithm) {
        return order(fromIndex, toIndex, comparator, algorithm, ProgressMonitors.current());
    }

    /**
     * Returns ordered coordinates using a borrowed, nullable monitor.
     *
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param comparator the comparison of absolute source indices
     * @param algorithm the selected sorting algorithm
     * @param monitor a borrowed monitor, or {@code null} to disable progress and cancellation
     * @return absolute source indices in the selected sorted order
     */
    public static int[] order(int fromIndex, int toIndex, IntBinaryOperator comparator,
                              SortAlgorithm algorithm, ProgressMonitor monitor) {
        checkRange(Integer.MAX_VALUE, fromIndex, toIndex);
        requireComparison(algorithm);
        Objects.requireNonNull(comparator, "comparator");
        return comparisonOrder(fromIndex, toIndex, comparator, algorithm, monitor);
    }

    /**
     * Returns sorted coordinates after evaluating exactly one signed-long key per
     * input coordinate on successful key admission. Supports every registered algorithm.
     *
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param keys the function supplying a signed-long key for each absolute source index
     * @param algorithm the selected sorting algorithm
     * @return absolute source indices in the selected sorted order
     */
    public static int[] orderByLong(int fromIndex, int toIndex, IntToLongFunction keys,
                                    SortAlgorithm algorithm) {
        return orderByLong(fromIndex, toIndex, keys, algorithm, ProgressMonitors.current());
    }

    /**
     * Returns sorted coordinates using cached keys and a borrowed nullable monitor.
     *
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param keys the function supplying a signed-long key for each absolute source index
     * @param algorithm the selected sorting algorithm
     * @param monitor a borrowed monitor, or {@code null} to disable progress and cancellation
     * @return absolute source indices in the selected sorted order
     */
    public static int[] orderByLong(int fromIndex, int toIndex, IntToLongFunction keys,
                                    SortAlgorithm algorithm, ProgressMonitor monitor) {
        checkRange(Integer.MAX_VALUE, fromIndex, toIndex);
        Objects.requireNonNull(algorithm, "algorithm");
        Objects.requireNonNull(keys, "keys");
        return keyedOrder(fromIndex, toIndex, keys, algorithm, monitor);
    }

    /**
     * Sorts an existing lane of physical indices, including duplicates and negative
     * labels if the caller's comparator supports them. The comparator receives labels.
     *
     * @param indices the index labels to reorder
     * @param comparator the comparison of supplied index labels
     * @param algorithm the selected sorting algorithm
     */
    public static void sortIndices(int[] indices, IntBinaryOperator comparator,
                                    SortAlgorithm algorithm) {
        sortIndices(indices, 0, indices.length, comparator, algorithm, ProgressMonitors.current());
    }

    /**
     * Sorts a range of an index lane using the current monitor.
     *
     * @param indices the index labels to reorder
     * @param fromIndex the first index-lane position, inclusive
     * @param toIndex the last index-lane position, exclusive
     * @param comparator the comparison of supplied index labels
     * @param algorithm the selected sorting algorithm
     */
    public static void sortIndices(int[] indices, int fromIndex, int toIndex,
                                    IntBinaryOperator comparator, SortAlgorithm algorithm) {
        sortIndices(indices, fromIndex, toIndex, comparator, algorithm, ProgressMonitors.current());
    }

    /**
     * Sorts a copied index window, then commits it after all callbacks succeed.
     *
     * @param indices the index labels to reorder
     * @param fromIndex the first index-lane position, inclusive
     * @param toIndex the last index-lane position, exclusive
     * @param comparator the comparison of supplied index labels
     * @param algorithm the selected sorting algorithm
     * @param monitor a borrowed monitor, or {@code null} to disable progress and cancellation
     */
    public static void sortIndices(int[] indices, int fromIndex, int toIndex,
                                    IntBinaryOperator comparator, SortAlgorithm algorithm,
                                    ProgressMonitor monitor) {
        checkRange(indices.length, fromIndex, toIndex);
        requireComparison(algorithm);
        M3SortContext context = new M3SortContext(comparator, monitor);
        int[] work = Arrays.copyOfRange(indices, fromIndex, toIndex);
        M3ComparisonSorts.sort(work, algorithm, context);
        context.finish();
        System.arraycopy(work, 0, indices, fromIndex, work.length);
    }

    /**
     * Sorts a boolean array with the selected algorithm and current monitor.
     *
     * @param values the array to sort
     * @param algorithm the selected sorting algorithm
     */
    public static void sort(boolean[] values, SortAlgorithm algorithm) {
        sort(values, 0, values.length, algorithm, ProgressMonitors.current());
    }

    /**
     * Sorts a half-open boolean range with the current monitor.
     *
     * @param values the array to sort
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     */
    public static void sort(boolean[] values, int fromIndex, int toIndex, SortAlgorithm algorithm) {
        sort(values, fromIndex, toIndex, algorithm, ProgressMonitors.current());
    }

    /**
     * Sorts a boolean range with a borrowed, nullable monitor.
     *
     * @param values the array to sort
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     * @param monitor a borrowed monitor, or {@code null} to disable progress and cancellation
     */
    public static void sort(boolean[] values, int fromIndex, int toIndex,
                             SortAlgorithm algorithm, ProgressMonitor monitor) {
        int[] order = order(values, fromIndex, toIndex, algorithm, monitor);
        for (int i = 0; i < order.length; i++) {
            if (order[i] == fromIndex + i) continue;
            boolean saved = values[fromIndex + i];
            int destination = i;
            while (true) {
                int source = order[destination] - fromIndex;
                order[destination] = fromIndex + destination;
                if (source == i) {
                    values[fromIndex + destination] = saved;
                    break;
                }
                values[fromIndex + destination] = values[fromIndex + source];
                destination = source;
            }
        }
    }

    /**
     * Returns sorted absolute source indices without rearranging a boolean array.
     *
     * @param values the source array, which is not rearranged
     * @param algorithm the selected sorting algorithm
     * @return absolute source indices in the selected sorted order
     */
    public static int[] order(boolean[] values, SortAlgorithm algorithm) {
        return order(values, 0, values.length, algorithm, ProgressMonitors.current());
    }

    /**
     * Returns sorted absolute indices for a half-open boolean range.
     *
     * @param values the source array, which is not rearranged
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     * @return absolute source indices in the selected sorted order
     */
    public static int[] order(boolean[] values, int fromIndex, int toIndex, SortAlgorithm algorithm) {
        return order(values, fromIndex, toIndex, algorithm, ProgressMonitors.current());
    }

    /**
     * Returns sorted absolute indices using a borrowed, nullable monitor.
     *
     * @param values the source array, which is not rearranged
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     * @param monitor a borrowed monitor, or {@code null} to disable progress and cancellation
     * @return absolute source indices in the selected sorted order
     */
    public static int[] order(boolean[] values, int fromIndex, int toIndex,
                              SortAlgorithm algorithm, ProgressMonitor monitor) {
        return primitiveOrder(values.length, fromIndex, toIndex, algorithm, monitor,
                (left, right) -> Boolean.compare(values[left], values[right]), index -> values[index] ? 1L : 0L);
    }

    /**
     * Sorts a byte array with the selected algorithm and current monitor.
     *
     * @param values the array to sort
     * @param algorithm the selected sorting algorithm
     */
    public static void sort(byte[] values, SortAlgorithm algorithm) {
        sort(values, 0, values.length, algorithm, ProgressMonitors.current());
    }

    /**
     * Sorts a half-open byte range with the current monitor.
     *
     * @param values the array to sort
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     */
    public static void sort(byte[] values, int fromIndex, int toIndex, SortAlgorithm algorithm) {
        sort(values, fromIndex, toIndex, algorithm, ProgressMonitors.current());
    }

    /**
     * Sorts a byte range with a borrowed, nullable monitor.
     *
     * @param values the array to sort
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     * @param monitor a borrowed monitor, or {@code null} to disable progress and cancellation
     */
    public static void sort(byte[] values, int fromIndex, int toIndex,
                             SortAlgorithm algorithm, ProgressMonitor monitor) {
        int[] order = order(values, fromIndex, toIndex, algorithm, monitor);
        for (int i = 0; i < order.length; i++) {
            if (order[i] == fromIndex + i) continue;
            byte saved = values[fromIndex + i];
            int destination = i;
            while (true) {
                int source = order[destination] - fromIndex;
                order[destination] = fromIndex + destination;
                if (source == i) {
                    values[fromIndex + destination] = saved;
                    break;
                }
                values[fromIndex + destination] = values[fromIndex + source];
                destination = source;
            }
        }
    }

    /**
     * Returns sorted absolute source indices without rearranging a byte array.
     *
     * @param values the source array, which is not rearranged
     * @param algorithm the selected sorting algorithm
     * @return absolute source indices in the selected sorted order
     */
    public static int[] order(byte[] values, SortAlgorithm algorithm) {
        return order(values, 0, values.length, algorithm, ProgressMonitors.current());
    }

    /**
     * Returns sorted absolute indices for a half-open byte range.
     *
     * @param values the source array, which is not rearranged
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     * @return absolute source indices in the selected sorted order
     */
    public static int[] order(byte[] values, int fromIndex, int toIndex, SortAlgorithm algorithm) {
        return order(values, fromIndex, toIndex, algorithm, ProgressMonitors.current());
    }

    /**
     * Returns sorted absolute indices using a borrowed, nullable monitor.
     *
     * @param values the source array, which is not rearranged
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     * @param monitor a borrowed monitor, or {@code null} to disable progress and cancellation
     * @return absolute source indices in the selected sorted order
     */
    public static int[] order(byte[] values, int fromIndex, int toIndex,
                              SortAlgorithm algorithm, ProgressMonitor monitor) {
        return primitiveOrder(values.length, fromIndex, toIndex, algorithm, monitor,
                (left, right) -> Byte.compare(values[left], values[right]), index -> values[index]);
    }

    /**
     * Sorts a short array with the selected algorithm and current monitor.
     *
     * @param values the array to sort
     * @param algorithm the selected sorting algorithm
     */
    public static void sort(short[] values, SortAlgorithm algorithm) {
        sort(values, 0, values.length, algorithm, ProgressMonitors.current());
    }

    /**
     * Sorts a half-open short range with the current monitor.
     *
     * @param values the array to sort
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     */
    public static void sort(short[] values, int fromIndex, int toIndex, SortAlgorithm algorithm) {
        sort(values, fromIndex, toIndex, algorithm, ProgressMonitors.current());
    }

    /**
     * Sorts a short range with a borrowed, nullable monitor.
     *
     * @param values the array to sort
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     * @param monitor a borrowed monitor, or {@code null} to disable progress and cancellation
     */
    public static void sort(short[] values, int fromIndex, int toIndex,
                             SortAlgorithm algorithm, ProgressMonitor monitor) {
        int[] order = order(values, fromIndex, toIndex, algorithm, monitor);
        for (int i = 0; i < order.length; i++) {
            if (order[i] == fromIndex + i) continue;
            short saved = values[fromIndex + i];
            int destination = i;
            while (true) {
                int source = order[destination] - fromIndex;
                order[destination] = fromIndex + destination;
                if (source == i) {
                    values[fromIndex + destination] = saved;
                    break;
                }
                values[fromIndex + destination] = values[fromIndex + source];
                destination = source;
            }
        }
    }

    /**
     * Returns sorted absolute source indices without rearranging a short array.
     *
     * @param values the source array, which is not rearranged
     * @param algorithm the selected sorting algorithm
     * @return absolute source indices in the selected sorted order
     */
    public static int[] order(short[] values, SortAlgorithm algorithm) {
        return order(values, 0, values.length, algorithm, ProgressMonitors.current());
    }

    /**
     * Returns sorted absolute indices for a half-open short range.
     *
     * @param values the source array, which is not rearranged
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     * @return absolute source indices in the selected sorted order
     */
    public static int[] order(short[] values, int fromIndex, int toIndex, SortAlgorithm algorithm) {
        return order(values, fromIndex, toIndex, algorithm, ProgressMonitors.current());
    }

    /**
     * Returns sorted absolute indices using a borrowed, nullable monitor.
     *
     * @param values the source array, which is not rearranged
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     * @param monitor a borrowed monitor, or {@code null} to disable progress and cancellation
     * @return absolute source indices in the selected sorted order
     */
    public static int[] order(short[] values, int fromIndex, int toIndex,
                              SortAlgorithm algorithm, ProgressMonitor monitor) {
        return primitiveOrder(values.length, fromIndex, toIndex, algorithm, monitor,
                (left, right) -> Short.compare(values[left], values[right]), index -> values[index]);
    }

    /**
     * Sorts a char array with the selected algorithm and current monitor.
     *
     * @param values the array to sort
     * @param algorithm the selected sorting algorithm
     */
    public static void sort(char[] values, SortAlgorithm algorithm) {
        sort(values, 0, values.length, algorithm, ProgressMonitors.current());
    }

    /**
     * Sorts a half-open char range with the current monitor.
     *
     * @param values the array to sort
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     */
    public static void sort(char[] values, int fromIndex, int toIndex, SortAlgorithm algorithm) {
        sort(values, fromIndex, toIndex, algorithm, ProgressMonitors.current());
    }

    /**
     * Sorts a char range with a borrowed, nullable monitor.
     *
     * @param values the array to sort
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     * @param monitor a borrowed monitor, or {@code null} to disable progress and cancellation
     */
    public static void sort(char[] values, int fromIndex, int toIndex,
                             SortAlgorithm algorithm, ProgressMonitor monitor) {
        int[] order = order(values, fromIndex, toIndex, algorithm, monitor);
        for (int i = 0; i < order.length; i++) {
            if (order[i] == fromIndex + i) continue;
            char saved = values[fromIndex + i];
            int destination = i;
            while (true) {
                int source = order[destination] - fromIndex;
                order[destination] = fromIndex + destination;
                if (source == i) {
                    values[fromIndex + destination] = saved;
                    break;
                }
                values[fromIndex + destination] = values[fromIndex + source];
                destination = source;
            }
        }
    }

    /**
     * Returns sorted absolute source indices without rearranging a char array.
     *
     * @param values the source array, which is not rearranged
     * @param algorithm the selected sorting algorithm
     * @return absolute source indices in the selected sorted order
     */
    public static int[] order(char[] values, SortAlgorithm algorithm) {
        return order(values, 0, values.length, algorithm, ProgressMonitors.current());
    }

    /**
     * Returns sorted absolute indices for a half-open char range.
     *
     * @param values the source array, which is not rearranged
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     * @return absolute source indices in the selected sorted order
     */
    public static int[] order(char[] values, int fromIndex, int toIndex, SortAlgorithm algorithm) {
        return order(values, fromIndex, toIndex, algorithm, ProgressMonitors.current());
    }

    /**
     * Returns sorted absolute indices using a borrowed, nullable monitor.
     *
     * @param values the source array, which is not rearranged
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     * @param monitor a borrowed monitor, or {@code null} to disable progress and cancellation
     * @return absolute source indices in the selected sorted order
     */
    public static int[] order(char[] values, int fromIndex, int toIndex,
                              SortAlgorithm algorithm, ProgressMonitor monitor) {
        return primitiveOrder(values.length, fromIndex, toIndex, algorithm, monitor,
                (left, right) -> Character.compare(values[left], values[right]), index -> values[index]);
    }

    /**
     * Sorts a int array with the selected algorithm and current monitor.
     *
     * @param values the array to sort
     * @param algorithm the selected sorting algorithm
     */
    public static void sort(int[] values, SortAlgorithm algorithm) {
        sort(values, 0, values.length, algorithm, ProgressMonitors.current());
    }

    /**
     * Sorts a half-open int range with the current monitor.
     *
     * @param values the array to sort
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     */
    public static void sort(int[] values, int fromIndex, int toIndex, SortAlgorithm algorithm) {
        sort(values, fromIndex, toIndex, algorithm, ProgressMonitors.current());
    }

    /**
     * Sorts a int range with a borrowed, nullable monitor.
     *
     * @param values the array to sort
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     * @param monitor a borrowed monitor, or {@code null} to disable progress and cancellation
     */
    public static void sort(int[] values, int fromIndex, int toIndex,
                             SortAlgorithm algorithm, ProgressMonitor monitor) {
        int[] order = order(values, fromIndex, toIndex, algorithm, monitor);
        for (int i = 0; i < order.length; i++) {
            if (order[i] == fromIndex + i) continue;
            int saved = values[fromIndex + i];
            int destination = i;
            while (true) {
                int source = order[destination] - fromIndex;
                order[destination] = fromIndex + destination;
                if (source == i) {
                    values[fromIndex + destination] = saved;
                    break;
                }
                values[fromIndex + destination] = values[fromIndex + source];
                destination = source;
            }
        }
    }

    /**
     * Returns sorted absolute source indices without rearranging a int array.
     *
     * @param values the source array, which is not rearranged
     * @param algorithm the selected sorting algorithm
     * @return absolute source indices in the selected sorted order
     */
    public static int[] order(int[] values, SortAlgorithm algorithm) {
        return order(values, 0, values.length, algorithm, ProgressMonitors.current());
    }

    /**
     * Returns sorted absolute indices for a half-open int range.
     *
     * @param values the source array, which is not rearranged
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     * @return absolute source indices in the selected sorted order
     */
    public static int[] order(int[] values, int fromIndex, int toIndex, SortAlgorithm algorithm) {
        return order(values, fromIndex, toIndex, algorithm, ProgressMonitors.current());
    }

    /**
     * Returns sorted absolute indices using a borrowed, nullable monitor.
     *
     * @param values the source array, which is not rearranged
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     * @param monitor a borrowed monitor, or {@code null} to disable progress and cancellation
     * @return absolute source indices in the selected sorted order
     */
    public static int[] order(int[] values, int fromIndex, int toIndex,
                              SortAlgorithm algorithm, ProgressMonitor monitor) {
        return primitiveOrder(values.length, fromIndex, toIndex, algorithm, monitor,
                (left, right) -> Integer.compare(values[left], values[right]), index -> values[index]);
    }

    /**
     * Sorts a long array with the selected algorithm and current monitor.
     *
     * @param values the array to sort
     * @param algorithm the selected sorting algorithm
     */
    public static void sort(long[] values, SortAlgorithm algorithm) {
        sort(values, 0, values.length, algorithm, ProgressMonitors.current());
    }

    /**
     * Sorts a half-open long range with the current monitor.
     *
     * @param values the array to sort
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     */
    public static void sort(long[] values, int fromIndex, int toIndex, SortAlgorithm algorithm) {
        sort(values, fromIndex, toIndex, algorithm, ProgressMonitors.current());
    }

    /**
     * Sorts a long range with a borrowed, nullable monitor.
     *
     * @param values the array to sort
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     * @param monitor a borrowed monitor, or {@code null} to disable progress and cancellation
     */
    public static void sort(long[] values, int fromIndex, int toIndex,
                             SortAlgorithm algorithm, ProgressMonitor monitor) {
        int[] order = order(values, fromIndex, toIndex, algorithm, monitor);
        for (int i = 0; i < order.length; i++) {
            if (order[i] == fromIndex + i) continue;
            long saved = values[fromIndex + i];
            int destination = i;
            while (true) {
                int source = order[destination] - fromIndex;
                order[destination] = fromIndex + destination;
                if (source == i) {
                    values[fromIndex + destination] = saved;
                    break;
                }
                values[fromIndex + destination] = values[fromIndex + source];
                destination = source;
            }
        }
    }

    /**
     * Returns sorted absolute source indices without rearranging a long array.
     *
     * @param values the source array, which is not rearranged
     * @param algorithm the selected sorting algorithm
     * @return absolute source indices in the selected sorted order
     */
    public static int[] order(long[] values, SortAlgorithm algorithm) {
        return order(values, 0, values.length, algorithm, ProgressMonitors.current());
    }

    /**
     * Returns sorted absolute indices for a half-open long range.
     *
     * @param values the source array, which is not rearranged
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     * @return absolute source indices in the selected sorted order
     */
    public static int[] order(long[] values, int fromIndex, int toIndex, SortAlgorithm algorithm) {
        return order(values, fromIndex, toIndex, algorithm, ProgressMonitors.current());
    }

    /**
     * Returns sorted absolute indices using a borrowed, nullable monitor.
     *
     * @param values the source array, which is not rearranged
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     * @param monitor a borrowed monitor, or {@code null} to disable progress and cancellation
     * @return absolute source indices in the selected sorted order
     */
    public static int[] order(long[] values, int fromIndex, int toIndex,
                              SortAlgorithm algorithm, ProgressMonitor monitor) {
        return primitiveOrder(values.length, fromIndex, toIndex, algorithm, monitor,
                (left, right) -> Long.compare(values[left], values[right]), index -> values[index]);
    }

    /**
     * Sorts a float array with the selected algorithm and current monitor.
     *
     * @param values the array to sort
     * @param algorithm the selected sorting algorithm
     */
    public static void sort(float[] values, SortAlgorithm algorithm) {
        sort(values, 0, values.length, algorithm, ProgressMonitors.current());
    }

    /**
     * Sorts a half-open float range with the current monitor.
     *
     * @param values the array to sort
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     */
    public static void sort(float[] values, int fromIndex, int toIndex, SortAlgorithm algorithm) {
        sort(values, fromIndex, toIndex, algorithm, ProgressMonitors.current());
    }

    /**
     * Sorts a float range with a borrowed, nullable monitor.
     *
     * @param values the array to sort
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     * @param monitor a borrowed monitor, or {@code null} to disable progress and cancellation
     */
    public static void sort(float[] values, int fromIndex, int toIndex,
                             SortAlgorithm algorithm, ProgressMonitor monitor) {
        int[] order = order(values, fromIndex, toIndex, algorithm, monitor);
        for (int i = 0; i < order.length; i++) {
            if (order[i] == fromIndex + i) continue;
            float saved = values[fromIndex + i];
            int destination = i;
            while (true) {
                int source = order[destination] - fromIndex;
                order[destination] = fromIndex + destination;
                if (source == i) {
                    values[fromIndex + destination] = saved;
                    break;
                }
                values[fromIndex + destination] = values[fromIndex + source];
                destination = source;
            }
        }
    }

    /**
     * Returns sorted absolute source indices without rearranging a float array.
     *
     * @param values the source array, which is not rearranged
     * @param algorithm the selected sorting algorithm
     * @return absolute source indices in the selected sorted order
     */
    public static int[] order(float[] values, SortAlgorithm algorithm) {
        return order(values, 0, values.length, algorithm, ProgressMonitors.current());
    }

    /**
     * Returns sorted absolute indices for a half-open float range.
     *
     * @param values the source array, which is not rearranged
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     * @return absolute source indices in the selected sorted order
     */
    public static int[] order(float[] values, int fromIndex, int toIndex, SortAlgorithm algorithm) {
        return order(values, fromIndex, toIndex, algorithm, ProgressMonitors.current());
    }

    /**
     * Returns sorted absolute indices using a borrowed, nullable monitor.
     *
     * @param values the source array, which is not rearranged
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     * @param monitor a borrowed monitor, or {@code null} to disable progress and cancellation
     * @return absolute source indices in the selected sorted order
     */
    public static int[] order(float[] values, int fromIndex, int toIndex,
                              SortAlgorithm algorithm, ProgressMonitor monitor) {
        return primitiveOrder(values.length, fromIndex, toIndex, algorithm, monitor,
                (left, right) -> Float.compare(values[left], values[right]), index -> floatKey(values[index]));
    }

    /**
     * Sorts a double array with the selected algorithm and current monitor.
     *
     * @param values the array to sort
     * @param algorithm the selected sorting algorithm
     */
    public static void sort(double[] values, SortAlgorithm algorithm) {
        sort(values, 0, values.length, algorithm, ProgressMonitors.current());
    }

    /**
     * Sorts a half-open double range with the current monitor.
     *
     * @param values the array to sort
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     */
    public static void sort(double[] values, int fromIndex, int toIndex, SortAlgorithm algorithm) {
        sort(values, fromIndex, toIndex, algorithm, ProgressMonitors.current());
    }

    /**
     * Sorts a double range with a borrowed, nullable monitor.
     *
     * @param values the array to sort
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     * @param monitor a borrowed monitor, or {@code null} to disable progress and cancellation
     */
    public static void sort(double[] values, int fromIndex, int toIndex,
                             SortAlgorithm algorithm, ProgressMonitor monitor) {
        int[] order = order(values, fromIndex, toIndex, algorithm, monitor);
        for (int i = 0; i < order.length; i++) {
            if (order[i] == fromIndex + i) continue;
            double saved = values[fromIndex + i];
            int destination = i;
            while (true) {
                int source = order[destination] - fromIndex;
                order[destination] = fromIndex + destination;
                if (source == i) {
                    values[fromIndex + destination] = saved;
                    break;
                }
                values[fromIndex + destination] = values[fromIndex + source];
                destination = source;
            }
        }
    }

    /**
     * Returns sorted absolute source indices without rearranging a double array.
     *
     * @param values the source array, which is not rearranged
     * @param algorithm the selected sorting algorithm
     * @return absolute source indices in the selected sorted order
     */
    public static int[] order(double[] values, SortAlgorithm algorithm) {
        return order(values, 0, values.length, algorithm, ProgressMonitors.current());
    }

    /**
     * Returns sorted absolute indices for a half-open double range.
     *
     * @param values the source array, which is not rearranged
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     * @return absolute source indices in the selected sorted order
     */
    public static int[] order(double[] values, int fromIndex, int toIndex, SortAlgorithm algorithm) {
        return order(values, fromIndex, toIndex, algorithm, ProgressMonitors.current());
    }

    /**
     * Returns sorted absolute indices using a borrowed, nullable monitor.
     *
     * @param values the source array, which is not rearranged
     * @param fromIndex the first source index, inclusive
     * @param toIndex the last source index, exclusive
     * @param algorithm the selected sorting algorithm
     * @param monitor a borrowed monitor, or {@code null} to disable progress and cancellation
     * @return absolute source indices in the selected sorted order
     */
    public static int[] order(double[] values, int fromIndex, int toIndex,
                              SortAlgorithm algorithm, ProgressMonitor monitor) {
        return primitiveOrder(values.length, fromIndex, toIndex, algorithm, monitor,
                (left, right) -> Double.compare(values[left], values[right]), index -> doubleKey(values[index]));
    }

    private static int[] primitiveOrder(int length, int fromIndex, int toIndex,
                                         SortAlgorithm algorithm, ProgressMonitor monitor,
                                         IntBinaryOperator comparison, IntToLongFunction keys) {
        checkRange(length, fromIndex, toIndex);
        Objects.requireNonNull(algorithm, "algorithm");
        return algorithm.requiresNumericKeys()
                ? keyedOrder(fromIndex, toIndex, keys, algorithm, monitor)
                : comparisonOrder(fromIndex, toIndex, comparison, algorithm, monitor);
    }

    private static int[] comparisonOrder(int fromIndex, int toIndex,
                                          IntBinaryOperator comparison, SortAlgorithm algorithm,
                                          ProgressMonitor monitor) {
        M3SortContext context = new M3SortContext(comparison, monitor);
        int[] order = new int[toIndex - fromIndex];
        for (int i = 0; i < order.length; i++) {
            order[i] = fromIndex + i;
            context.step();
        }
        M3ComparisonSorts.sort(order, algorithm, context);
        context.finish();
        return order;
    }

    private static int[] keyedOrder(int fromIndex, int toIndex, IntToLongFunction keyFunction,
                                     SortAlgorithm algorithm, ProgressMonitor monitor) {
        if (monitor != null && monitor != ProgressMonitor.NONE) monitor.checkCancelled();
        int length = toIndex - fromIndex;
        long[] keys = new long[length];
        M3SortContext context = new M3SortContext(
                (left, right) -> Long.compare(keys[left - fromIndex], keys[right - fromIndex]),
                monitor);
        int[] order = new int[length];
        for (int i = 0; i < length; i++) {
            context.step();
            keys[i] = keyFunction.applyAsLong(fromIndex + i);
            order[i] = fromIndex + i;
        }
        if (algorithm.requiresNumericKeys()) {
            M3DistributionSorts.sort(order, keys, fromIndex, algorithm, context);
        } else {
            M3ComparisonSorts.sort(order, algorithm, context);
        }
        context.finish();
        return order;
    }

    private static void requireComparison(SortAlgorithm algorithm) {
        if (Objects.requireNonNull(algorithm, "algorithm").requiresNumericKeys()) {
            throw new IllegalArgumentException(algorithm
                    + " requires numeric keys; use a primitive array or orderByLong");
        }
    }

    private static void checkRange(int length, int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException("fromIndex (" + fromIndex
                    + ") exceeds toIndex (" + toIndex + ")");
        }
        if (fromIndex < 0 || toIndex > length) {
            throw new ArrayIndexOutOfBoundsException("Range [" + fromIndex
                    + ", " + toIndex + ") for length " + length);
        }
    }

    @SuppressWarnings("unchecked")
    private static int naturalCompare(Object left, Object right) {
        return ((Comparable<Object>) left).compareTo(right);
    }

    private static long floatKey(float value) {
        int bits = Float.floatToIntBits(value);
        return bits ^ ((bits >> 31) & Integer.MAX_VALUE);
    }

    private static long doubleKey(double value) {
        long bits = Double.doubleToLongBits(value);
        return bits ^ ((bits >> 63) & Long.MAX_VALUE);
    }
}
