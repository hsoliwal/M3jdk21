/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Maps two reference values to a primitive float result. */
@FunctionalInterface
public interface ToFloatBiFunction<T, U> {
    float applyAsFloat(T first, U second);
}
