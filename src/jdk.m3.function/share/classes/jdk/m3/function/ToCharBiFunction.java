/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Maps two reference values to a primitive char result. */
@FunctionalInterface
public interface ToCharBiFunction<T, U> {
    char applyAsChar(T first, U second);
}
