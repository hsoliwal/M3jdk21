/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

package java.util.function.ext;

/**
 * A comparator with the shape {@code (short, short) to int}.
 *
 * <p>Primitive positions are primitive in the functional method. Reference arguments and results
 * may be null unless the implementation imposes a restriction. Invocations are caller-driven; this
 * interface implies no scheduling, caching, serialization, purity, resource ownership, or
 * thread-safety guarantee.
 *
 * <p>Implementations must satisfy the sign symmetry, transitivity, and zero consistency
 * requirements of {@link java.util.Comparator}. The result need not be -1, 0, or 1. Consistency
 * with equality is an additional property that must be documented by the implementation.
 *
 * This is a functional interface whose functional method is {@code compare}.
 *
 * @since M3JDK 21
 */
@FunctionalInterface
public interface ShortComparator {
    /**
     * Compares the two arguments using this ordering.
     *
     * @param value1 the argument at position 1
     * @param value2 the argument at position 2
     * @return a negative integer, zero, or a positive integer as the first argument is less than,
     * equal to, or greater than the second
     */
    int compare(short value1, short value2);

    /**
     * Returns the reverse ordering by swapping the arguments.
     * <p>The result is not negated, so {@link Integer#MIN_VALUE} remains valid.
     *
     * @return the reversed comparator
     */
    default ShortComparator reversed() {
        return (value1, value2) -> compare(value2, value1);
    }

    /**
     * Returns a comparator that applies {@code other} only when this comparator returns zero.
     *
     * @param other the secondary comparator
     * @return the combined ordering
     * @throws NullPointerException if {@code other} is null
     */
    default ShortComparator thenComparing(ShortComparator other) {
        java.util.Objects.requireNonNull(other);
        return (value1, value2) -> {
            int result = compare(value1, value2);
            return result == 0 ? other.compare(value1, value2) : result;
        };
    }

    /**
     * Returns the natural primitive ordering.
     * <p>Floating-point order uses the wrapper comparison, including NaN and signed zero.
     *
     * @return the natural-order comparator
     */
    static ShortComparator naturalOrder() {
        return Short::compare;
    }
}
