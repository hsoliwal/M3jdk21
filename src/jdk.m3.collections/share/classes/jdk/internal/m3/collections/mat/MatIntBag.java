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
package jdk.internal.m3.collections.mat;

import java.util.Objects;

/**
 * Immutable primitive multiset frozen as an adaptive primitive value->count map.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatIntBag.java}
 * blob {@code 313731d4ad55f6780f8c975e95fc1b8a9aaa3121} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatIntBag {
    private final MatAdaptiveIntIntMap counts;
    private final long size;

    private MatIntBag(MatAdaptiveIntIntMap counts, long size) { this.counts = counts; this.size = size; }

    public static MatIntBag of(int... values) {
        Objects.requireNonNull(values, "values");
        MatAdaptiveIntIntMap.Builder builder = MatAdaptiveIntIntMap.builder(MatCollectionPolicy.MIN_MEMORY);
        for (int value : values) builder.incrementCount(value);
        return new MatIntBag(builder.build(), values.length);
    }

    public long size() { return size; }
    public int distinctSize() { return counts.size(); }
    public int count(int value) { return counts.getOrDefault(value, 0); }
    public boolean contains(int value) { return count(value) > 0; }
    public int[] distinctValues() { return counts.keys(); }
    public long primitivePayloadBytes() { return counts.primitivePayloadBytes(); }
}
