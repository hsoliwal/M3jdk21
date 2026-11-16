/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

package java.util.function.checked;

/**
 * A checked operator with the shape {@code (boolean) to boolean}.
 *
 * <p>Primitive positions are primitive in the functional method. Reference arguments and results
 * may be null unless the implementation imposes a restriction. Invocations are caller-driven; this
 * interface implies no scheduling, caching, serialization, purity, resource ownership, or
 * thread-safety guarantee.
 *
 * <p>The exception parameter preserves the declared throwable type. Default operations relay the
 * original throwable without wrapping or catching it and do not change thread interruption status.
 *
 * This is a functional interface whose functional method is {@code applyAsBoolean}.
 *
 * @param <E> the throwable type
 * @since M3JDK 21
 */
@FunctionalInterface
public interface BooleanUnaryOperator<E extends Throwable> {
    /**
     * Applies this operator to the supplied arguments.
     *
     * @param value1 the argument at position 1
     * @return the result
     * @throws E if the operation fails
     */
    boolean applyAsBoolean(boolean value1) throws E;

    /**
     * Returns an operator that applies {@code before} before this operator.
     *
     * @param before the other operator
     * @return the composed operator
     * @throws NullPointerException if {@code before} is null
     */
    default BooleanUnaryOperator<E> compose(BooleanUnaryOperator<? extends E> before) {
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
    default BooleanUnaryOperator<E> andThen(BooleanUnaryOperator<? extends E> after) {
        java.util.Objects.requireNonNull(after);
        return value1 -> after.applyAsBoolean(applyAsBoolean(value1));
    }

    /**
     * Returns an operator that returns its argument unchanged.
     *
     * @param <E> the throwable type
     * @return an identity operator
     */
    static <E extends Throwable> BooleanUnaryOperator<E> identity() {
        return value -> value;
    }
}
