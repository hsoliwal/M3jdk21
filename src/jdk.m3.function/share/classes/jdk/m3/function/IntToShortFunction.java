/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/**
 * Maps a primitive int to a primitive short without boxing.
 * The lambda defines any narrowing or overflow behavior.
 */
@FunctionalInterface
public interface IntToShortFunction {
    short applyAsShort(int value);
}
