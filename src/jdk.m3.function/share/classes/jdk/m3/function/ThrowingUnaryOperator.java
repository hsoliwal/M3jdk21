/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** A checked-exception-capable same-type unary function. */
@FunctionalInterface
public interface ThrowingUnaryOperator<T, E extends Exception>
        extends ThrowingFunction<T, T, E> {
}
