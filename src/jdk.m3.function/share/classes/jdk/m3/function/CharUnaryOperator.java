/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Transforms one primitive char into the same primitive type. */
@FunctionalInterface
public interface CharUnaryOperator {
    char applyAsChar(char value);

    /** Invokes the non-null previous operator before this one. */
    default CharUnaryOperator compose(CharUnaryOperator before) {
        java.util.Objects.requireNonNull(before);
        return value -> applyAsChar(before.applyAsChar(value));
    }

    /** Invokes the non-null next operator after this one. */
    default CharUnaryOperator andThen(CharUnaryOperator after) {
        java.util.Objects.requireNonNull(after);
        return value -> after.applyAsChar(applyAsChar(value));
    }

    /** An operation preserving the exact primitive value. */
    static CharUnaryOperator identity() {
        return value -> value;
    }
}
