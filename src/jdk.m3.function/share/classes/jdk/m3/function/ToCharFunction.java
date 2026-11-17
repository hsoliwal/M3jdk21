/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Maps a reference to a primitive char result without boxing. */
@FunctionalInterface
public interface ToCharFunction<T> {
    char applyAsChar(T value);
}
