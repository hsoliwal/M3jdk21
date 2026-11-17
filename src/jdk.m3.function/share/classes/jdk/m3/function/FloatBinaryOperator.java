/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Combines two primitive float values without boxing. */
@FunctionalInterface
public interface FloatBinaryOperator {
    float applyAsFloat(float left, float right);
}
