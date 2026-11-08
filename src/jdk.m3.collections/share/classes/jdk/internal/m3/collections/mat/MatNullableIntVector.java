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
 * Int values plus independent presence bitmap: absent is never conflated with zero.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatNullableIntVector.java}
 * blob {@code f66269f55f2cb79c3a383b5b872bb1f919d5a6e7} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatNullableIntVector {
    private final int[] values;
    private final MatSelection present;

    public MatNullableIntVector(int[] values, MatSelection present) {
        this.values = Objects.requireNonNull(values, "values").clone();
        this.present = Objects.requireNonNull(present, "present");
        if (present.rows() != values.length) throw new IllegalArgumentException("universe mismatch");
    }

    public int size() { return values.length; }
    public boolean isPresent(int index) { return present.contains(index); }
    public int get(int index) {
        Objects.checkIndex(index, values.length);
        if (!present.contains(index)) throw new IllegalStateException("absent value");
        return values[index];
    }
    public int getOrDefault(int index, int fallback) {
        Objects.checkIndex(index, values.length);
        return present.contains(index) ? values[index] : fallback;
    }
    public MatSelection present() { return present; }
    public long primitivePayloadBytes() { return 4L * values.length + present.primitivePayloadBytes(); }
}
