/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Maps two reference values to a primitive short result. */
@FunctionalInterface
public interface ToShortBiFunction<T, U> {
    short applyAsShort(T first, U second);
}
