/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

package java.util.function.checked;

/**
 * A checked comparator with the shape {@code (reference, reference) to int}.
 *
 * <p>Primitive positions are primitive in the functional method. Reference arguments and results
 * may be null unless the implementation imposes a restriction. Invocations are caller-driven; this
 * interface implies no scheduling, caching, serialization, purity, resource ownership, or
 * thread-safety guarantee.
 *
 * <p>The exception parameter preserves the declared throwable type. Default operations relay the
 * original throwable without wrapping or catching it and do not change thread interruption status.
 *
 * <p>Implementations must satisfy the sign symmetry, transitivity, and zero consistency
 * requirements of {@link java.util.Comparator}. The result need not be -1, 0, or 1. Consistency
 * with equality is an additional property that must be documented by the implementation.
 *
 * This is a functional interface whose functional method is {@code compare}.
 *
 * @param <T> an input type
 * @param <E> the throwable type
 * @since M3JDK 21
 */
@FunctionalInterface
public interface Comparator<T, E extends Throwable> {
    /**
     * Compares the two arguments using this ordering.
     *
     * @param value1 the argument at position 1
     * @param value2 the argument at position 2
     * @return a negative integer, zero, or a positive integer as the first argument is less than,
     * equal to, or greater than the second
     * @throws E if the operation fails
     */
    int compare(T value1, T value2) throws E;

    /**
     * Returns the reverse ordering by swapping the arguments.
     * <p>The result is not negated, so {@link Integer#MIN_VALUE} remains valid.
     *
     * @return the reversed comparator
     */
    default Comparator<T, E> reversed() {
        return (value1, value2) -> compare(value2, value1);
    }

    /**
     * Returns a comparator that applies {@code other} only when this comparator returns zero.
     *
     * @param other the secondary comparator
     * @return the combined ordering
     * @throws NullPointerException if {@code other} is null
     */
    default Comparator<T, E> thenComparing(Comparator<? super T, ? extends E> other) {
        java.util.Objects.requireNonNull(other);
        return (value1, value2) -> {
            int result = compare(value1, value2);
            return result == 0 ? other.compare(value1, value2) : result;
        };
    }
}
