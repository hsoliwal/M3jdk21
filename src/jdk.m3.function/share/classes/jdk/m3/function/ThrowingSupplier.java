/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** A value producer with a declared checked-exception type. */
@FunctionalInterface
public interface ThrowingSupplier<R, E extends Exception> {
    R get() throws E;
}
