/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/**
 * Maps a primitive short to a primitive float without boxing.
 * The lambda defines any narrowing or overflow behavior.
 */
@FunctionalInterface
public interface ShortToFloatFunction {
    float applyAsFloat(short value);
}
