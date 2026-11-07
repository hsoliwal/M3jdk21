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

import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntUnaryOperator;

/**
 * Primitive int vector: integers remain integers; no boxing and no redundant interning.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatIntVector.java}
 * blob {@code 2ec541a7c4859fabd6c7ad0745a5b76a090f3c59} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatIntVector {
    private final int[] values;

    public M3MatIntVector(int[] values) {
        this.values = Objects.requireNonNull(values, "values").clone();
    }

    public int size() { return values.length; }
    public int get(int index) { return values[Objects.checkIndex(index, values.length)]; }
    int getUnchecked(int index) { return values[index]; }
    public int[] toArray() { return values.clone(); }
    public long payloadBytes() { return 4L * values.length; }

    public M3MatBits equalTo(int target) {
        M3MatBits.Builder bits=M3MatBits.builder(values.length);
        for(int i=0;i<values.length;i++) if(values[i]==target) bits.set(i);
        return bits.build();
    }

    public M3MatBits lessThan(int target) {
        M3MatBits.Builder bits=M3MatBits.builder(values.length);
        for(int i=0;i<values.length;i++) if(values[i]<target) bits.set(i);
        return bits.build();
    }

    public M3MatBits greaterOrEqual(int target) {
        M3MatBits.Builder bits=M3MatBits.builder(values.length);
        for(int i=0;i<values.length;i++) if(values[i]>=target) bits.set(i);
        return bits.build();
    }

    public M3MatIntVector map(IntUnaryOperator mapper) {
        Objects.requireNonNull(mapper,"mapper");
        int[] result=new int[values.length];
        for(int i=0;i<values.length;i++) result[i]=mapper.applyAsInt(values[i]);
        return new M3MatIntVector(result);
    }

    @Override public boolean equals(Object other) {
        return this==other || other instanceof M3MatIntVector vector && Arrays.equals(values,vector.values);
    }
    @Override public int hashCode(){ return Arrays.hashCode(values); }
}
