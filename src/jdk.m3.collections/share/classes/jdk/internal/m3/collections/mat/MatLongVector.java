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

import java.util.Arrays;
import java.util.Objects;
import java.util.function.LongUnaryOperator;

/**
 * Primitive long vector with signed and unsigned bitmap predicates.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatLongVector.java}
 * blob {@code c7800c8a1cf65115f760aa772a492abd39f5b605} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatLongVector {
    private final long[] values;

    public MatLongVector(long[] values) {
        this.values=Objects.requireNonNull(values,"values").clone();
    }

    public int size(){ return values.length; }
    public long get(int index){ return values[Objects.checkIndex(index,values.length)]; }
    long getUnchecked(int index){ return values[index]; }
    public long[] toArray(){ return values.clone(); }
    public long payloadBytes(){ return 8L*values.length; }

    public MatBits equalTo(long target){
        MatBits.Builder bits=MatBits.builder(values.length);
        for(int i=0;i<values.length;i++) if(values[i]==target) bits.set(i);
        return bits.build();
    }
    public MatBits signedGreaterOrEqual(long target){
        MatBits.Builder bits=MatBits.builder(values.length);
        for(int i=0;i<values.length;i++) if(values[i]>=target) bits.set(i);
        return bits.build();
    }
    public MatBits unsignedGreaterOrEqual(long target){
        MatBits.Builder bits=MatBits.builder(values.length);
        for(int i=0;i<values.length;i++) if(Long.compareUnsigned(values[i],target)>=0) bits.set(i);
        return bits.build();
    }
    public MatLongVector map(LongUnaryOperator mapper){
        Objects.requireNonNull(mapper,"mapper");
        long[] result=new long[values.length];
        for(int i=0;i<values.length;i++) result[i]=mapper.applyAsLong(values[i]);
        return new MatLongVector(result);
    }
    @Override public boolean equals(Object other){
        return this==other || other instanceof MatLongVector vector && Arrays.equals(values,vector.values);
    }
    @Override public int hashCode(){ return Arrays.hashCode(values); }
}
