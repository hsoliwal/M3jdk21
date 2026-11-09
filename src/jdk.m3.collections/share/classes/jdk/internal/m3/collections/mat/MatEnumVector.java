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

import jdk.m3.arrays.SelectionBits;

import java.util.Objects;

/**
 * Compact in-memory enum vector; persistent enum identity remains name-based in MatCodecs.enumCodec.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatEnumVector.java}
 * blob {@code f1e7568420f3947d78e68505dac6531a605e0b69} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatEnumVector<E extends Enum<E>> {
    private final Class<E> type;
    private final E[] constants;
    private final MatPackedLongVector ordinals;

    public MatEnumVector(Class<E> type,int size){
        this.type=Objects.requireNonNull(type,"type");
        this.constants=type.getEnumConstants();
        if(constants==null || constants.length==0) throw new IllegalArgumentException("enum has no constants");
        int width=Math.max(1,32-Integer.numberOfLeadingZeros(constants.length-1));
        this.ordinals=MatPackedLongVector.unsigned(size,width);
    }

    public int size(){ return ordinals.size(); }
    public long payloadBytes(){ return ordinals.payloadBytes(); }
    public E get(int index){ return constants[(int)ordinals.get(index)]; }
    public void set(int index,E value){
        Objects.requireNonNull(value,"value");
        if(value.getDeclaringClass()!=type) throw new IllegalArgumentException("different enum type");
        ordinals.set(index,value.ordinal());
    }
    public SelectionBits equalTo(E value){
        Objects.requireNonNull(value,"value");
        if(value.getDeclaringClass()!=type) throw new IllegalArgumentException("different enum type");
        return ordinals.equalTo(value.ordinal());
    }
}
