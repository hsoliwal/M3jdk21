/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/**
 * Maps a primitive float to a primitive long without boxing.
 * The lambda defines any narrowing or overflow behavior.
 */
@FunctionalInterface
public interface FloatToLongFunction {
    long applyAsLong(float value);
}
