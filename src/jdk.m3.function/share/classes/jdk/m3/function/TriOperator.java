/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** A three-argument function whose arguments and result share one type. */
@FunctionalInterface
public interface TriOperator<T> extends TriFunction<T, T, T, T> {
}
