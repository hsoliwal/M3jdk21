/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

package java.util.function.ext;

/**
 * A operator with the shape {@code (boolean) to boolean}.
 *
 * <p>Primitive positions are primitive in the functional method. Reference arguments and results
 * may be null unless the implementation imposes a restriction. Invocations are caller-driven; this
 * interface implies no scheduling, caching, serialization, purity, resource ownership, or
 * thread-safety guarantee.
 *
 * This is a functional interface whose functional method is {@code applyAsBoolean}.
 *
 * @since M3JDK 21
 */
@FunctionalInterface
public interface BooleanUnaryOperator {
    /**
     * Applies this operator to the supplied arguments.
     *
     * @param value1 the argument at position 1
     * @return the result
     */
    boolean applyAsBoolean(boolean value1);

    /**
     * Returns an operator that applies {@code before} before this operator.
     *
     * @param before the other operator
     * @return the composed operator
     * @throws NullPointerException if {@code before} is null
     */
    default BooleanUnaryOperator compose(BooleanUnaryOperator before) {
        java.util.Objects.requireNonNull(before);
        return value1 -> applyAsBoolean(before.applyAsBoolean(value1));
    }

    /**
     * Returns an operator that applies {@code after} after this operator.
     *
     * @param after the other operator
     * @return the composed operator
     * @throws NullPointerException if {@code after} is null
     */
    default BooleanUnaryOperator andThen(BooleanUnaryOperator after) {
        java.util.Objects.requireNonNull(after);
        return value1 -> after.applyAsBoolean(applyAsBoolean(value1));
    }

    /**
     * Returns an operator that returns its argument unchanged.
     *
     * @return an identity operator
     */
    static BooleanUnaryOperator identity() {
        return value -> value;
    }
}
