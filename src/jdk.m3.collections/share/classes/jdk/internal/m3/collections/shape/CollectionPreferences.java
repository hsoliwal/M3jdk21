/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections.shape;

/**
 * Soft workload weights in 0..1000; ratings are heuristics, not measurements.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/CollectionPreferences.java}
 * blob {@code 8f2bd67cf3b2ef01b9e8e410353dfc6f5e5e7d8e} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public record CollectionPreferences(int expectedSize, int lookup, int mutation,
                                    int traversal, int memory, int growth) {
    public CollectionPreferences {
        if (expectedSize < 0) { throw new IllegalArgumentException("expectedSize"); }
        check(lookup); check(mutation); check(traversal); check(memory); check(growth);
    }
    private static void check(int weight) {
        if (weight < 0 || weight > 1000) { throw new IllegalArgumentException("weight outside 0..1000"); }
    }
    /** Balanced soft preferences; no initial payload allocation is implied. */
    public static CollectionPreferences balanced(int expectedSize) {
        return new CollectionPreferences(expectedSize, 500, 500, 500, 500, 500);
    }
}
