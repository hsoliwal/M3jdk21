/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** A no-argument action with a declared checked-exception type. */
@FunctionalInterface
public interface ThrowingRunnable<E extends Exception> {
    void run() throws E;
}
