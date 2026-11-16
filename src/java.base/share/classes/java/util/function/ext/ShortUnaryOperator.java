/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

package java.util.function.ext;

/**
 * A operator with the shape {@code (short) to short}.
 *
 * <p>Primitive positions are primitive in the functional method. Reference arguments and results
 * may be null unless the implementation imposes a restriction. Invocations are caller-driven; this
 * interface implies no scheduling, caching, serialization, purity, resource ownership, or
 * thread-safety guarantee.
 *
 * This is a functional interface whose functional method is {@code applyAsShort}.
 *
 * @since M3JDK 21
 */
@FunctionalInterface
public interface ShortUnaryOperator {
    /**
     * Applies this operator to the supplied arguments.
     *
     * @param value1 the argument at position 1
     * @return the result
     */
    short applyAsShort(short value1);

    /**
     * Returns an operator that applies {@code before} before this operator.
     *
     * @param before the other operator
     * @return the composed operator
     * @throws NullPointerException if {@code before} is null
     */
    default ShortUnaryOperator compose(ShortUnaryOperator before) {
        java.util.Objects.requireNonNull(before);
        return value1 -> applyAsShort(before.applyAsShort(value1));
    }

    /**
     * Returns an operator that applies {@code after} after this operator.
     *
     * @param after the other operator
     * @return the composed operator
     * @throws NullPointerException if {@code after} is null
     */
    default ShortUnaryOperator andThen(ShortUnaryOperator after) {
        java.util.Objects.requireNonNull(after);
        return value1 -> after.applyAsShort(applyAsShort(value1));
    }

    /**
     * Returns an operator that returns its argument unchanged.
     *
     * @return an identity operator
     */
    static ShortUnaryOperator identity() {
        return value -> value;
    }
}
