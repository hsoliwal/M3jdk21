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
package jdk.internal.m3.collections;

import java.util.Objects;

/**
 * Linear Z-function index over an int sequence.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/IntZIndex.java}
 * blob {@code 422f73b44d8c8204ec88fc1a8c0c0b1bd7823a0b} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class IntZIndex {
    private final int[] z;

    public IntZIndex(int[] values) {
        Objects.requireNonNull(values, "values");
        z = new int[values.length];
        if (values.length == 0) return;
        z[0] = values.length;
        int left = 0;
        int right = 0;
        for (int i = 1; i < values.length; i++) {
            if (i < right) z[i] = Math.min(right - i, z[i - left]);
            while (i + z[i] < values.length && values[z[i]] == values[i + z[i]]) {
                z[i]++;
            }
            if (i + z[i] > right) {
                left = i;
                right = i + z[i];
            }
        }
    }

    public int length() { return z.length; }
    public int valueAt(int index) { return z[index]; }
    public int[] copy() { return z.clone(); }
    public long payloadBytes() { return (long) z.length * Integer.BYTES; }
}
