/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/**
 * Maps a primitive char to a primitive long without boxing.
 * The lambda defines any narrowing or overflow behavior.
 */
@FunctionalInterface
public interface CharToLongFunction {
    long applyAsLong(char value);
}
