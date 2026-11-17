/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Maps a reference to a primitive short result without boxing. */
@FunctionalInterface
public interface ToShortFunction<T> {
    short applyAsShort(T value);
}
