/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** A checked-exception-capable same-type binary function. */
@FunctionalInterface
public interface ThrowingBinaryOperator<T, E extends Exception>
        extends ThrowingBiFunction<T, T, T, E> {
}
