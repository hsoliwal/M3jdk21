/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Combines two primitive boolean values without boxing. */
@FunctionalInterface
public interface BooleanBinaryOperator {
    boolean applyAsBoolean(boolean left, boolean right);
}
