/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Supplies a primitive short value without boxing. */
@FunctionalInterface
public interface ShortSupplier {
    short getAsShort();
}
