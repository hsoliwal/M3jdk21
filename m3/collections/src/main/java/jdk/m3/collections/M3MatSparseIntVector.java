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
package jdk.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable sparse int vector: scalar default plus sorted position/value planes.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatSparseIntVector.java}
 * blob {@code ac9cf7aea70009dfc051673144b2046934514718} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatSparseIntVector {
    private final int size,defaultValue;
    private final int[] positions,values;

    public M3MatSparseIntVector(int size,int defaultValue,int[] positions,int[] values){
        if(size<0)throw new IllegalArgumentException("size");
        if(positions.length!=values.length)throw new IllegalArgumentException("length mismatch");
        this.size=size;this.defaultValue=defaultValue;this.positions=positions.clone();this.values=values.clone();
        for(int i=0;i<this.positions.length;i++){
            int p=this.positions[i];
            if(p<0||p>=size||i>0&&p<=this.positions[i-1])throw new IllegalArgumentException("positions must be strict sorted");
            if(this.values[i]==defaultValue)throw new IllegalArgumentException("explicit default wastes sparse slot");
        }
    }

    public static M3MatSparseIntVector fromDense(int[] dense,int defaultValue){
        Objects.requireNonNull(dense,"dense");
        int n=0;for(int v:dense)if(v!=defaultValue)n++;
        int[] p=new int[n],v=new int[n];int k=0;
        for(int i=0;i<dense.length;i++)if(dense[i]!=defaultValue){p[k]=i;v[k++]=dense[i];}
        return new M3MatSparseIntVector(dense.length,defaultValue,p,v);
    }

    public int size(){return size;}
    public int defaultValue(){return defaultValue;}
    public int explicitCount(){return positions.length;}
    public int get(int index){Objects.checkIndex(index,size);int i=Arrays.binarySearch(positions,index);return i<0?defaultValue:values[i];}
    public int[] toArray(){int[] a=new int[size];Arrays.fill(a,defaultValue);for(int i=0;i<positions.length;i++)a[positions[i]]=values[i];return a;}
    public long primitivePayloadBytes(){return 4L+8L*positions.length;}
}
