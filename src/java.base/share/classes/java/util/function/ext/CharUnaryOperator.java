/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

package java.util.function.ext;

/**
 * A operator with the shape {@code (char) to char}.
 *
 * <p>Primitive positions are primitive in the functional method. Reference arguments and results
 * may be null unless the implementation imposes a restriction. Invocations are caller-driven; this
 * interface implies no scheduling, caching, serialization, purity, resource ownership, or
 * thread-safety guarantee.
 *
 * This is a functional interface whose functional method is {@code applyAsChar}.
 *
 * @since M3JDK 21
 */
@FunctionalInterface
public interface CharUnaryOperator {
    /**
     * Applies this operator to the supplied arguments.
     *
     * @param value1 the argument at position 1
     * @return the result
     */
    char applyAsChar(char value1);

    /**
     * Returns an operator that applies {@code before} before this operator.
     *
     * @param before the other operator
     * @return the composed operator
     * @throws NullPointerException if {@code before} is null
     */
    default CharUnaryOperator compose(CharUnaryOperator before) {
        java.util.Objects.requireNonNull(before);
        return value1 -> applyAsChar(before.applyAsChar(value1));
    }

    /**
     * Returns an operator that applies {@code after} after this operator.
     *
     * @param after the other operator
     * @return the composed operator
     * @throws NullPointerException if {@code after} is null
     */
    default CharUnaryOperator andThen(CharUnaryOperator after) {
        java.util.Objects.requireNonNull(after);
        return value1 -> after.applyAsChar(applyAsChar(value1));
    }

    /**
     * Returns an operator that returns its argument unchanged.
     *
     * @return an identity operator
     */
    static CharUnaryOperator identity() {
        return value -> value;
    }
}
