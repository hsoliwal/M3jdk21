/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Maps two reference values to a primitive byte result. */
@FunctionalInterface
public interface ToByteBiFunction<T, U> {
    byte applyAsByte(T first, U second);
}
