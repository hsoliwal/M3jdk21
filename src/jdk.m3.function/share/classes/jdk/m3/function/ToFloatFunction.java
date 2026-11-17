/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Maps a reference to a primitive float result without boxing. */
@FunctionalInterface
public interface ToFloatFunction<T> {
    float applyAsFloat(T value);
}
